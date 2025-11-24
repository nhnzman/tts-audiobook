package com.lao.tts

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.lao.tts.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The entry point of the Rao TTS application.  This activity allows the user to
 * paste or import text, choose a subject folder, and initiate conversion to
 * audio files.  Once conversion is complete the user can tap the bottom
 * mini‑player to open [AudioPlayerActivity] for playback.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var importFilesLauncher: ActivityResultLauncher<Intent>

    /** Currently selected subject folder.  All imported files and generated
     * audio files will be stored under this subject.  The subject name is
     * appended to the base save directory chosen by the user on first run.
     */
    private var currentSubject: String = "기본"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialise the ActivityResultLauncher for file import.  This uses
        // ACTION_OPEN_DOCUMENT with the ability to select multiple files.  The
        // selected URIs are passed to [handleImportedFiles].
        importFilesLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data: Intent? = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val clipData = data.clipData
                val uris = mutableListOf<Uri>()
                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        uris.add(clipData.getItemAt(i).uri)
                    }
                } else {
                    data.data?.let { uris.add(it) }
                }
                handleImportedFiles(uris)
            }
        }

        // Subject selector opens a simple dialog with a list of existing subjects
        // or allows the user to enter a new subject name.  This could be
        // replaced with a dedicated screen for more complex organisation.
        binding.btnChooseSubject.setOnClickListener {
            showSubjectDialog()
        }

        // Button to paste text from clipboard into the input field.  Long
        // pressing in the EditText also allows paste via the context menu.
        binding.btnPaste.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)
            if (!text.isNullOrEmpty()) {
                binding.editTextInput.setText(text)
            } else {
                Toast.makeText(this, getString(R.string.no_clipboard_data), Toast.LENGTH_SHORT).show()
            }
        }

        // Launch the system file picker to import one or more .txt files.  We
        // restrict the MIME type to plain text to avoid unsupported formats.
        binding.btnImport.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            importFilesLauncher.launch(intent)
        }

        // Start conversion of the entered/imported text.  If the user has not
        // yet selected a save directory then prompt for one via [FileUtils].
        binding.btnConvert.setOnClickListener {
            val text = binding.editTextInput.text.toString()
            if (text.isBlank()) {
                Toast.makeText(this, getString(R.string.please_enter_text), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val saveDir = FileUtils.getBaseSaveDirectory(this)
            if (saveDir == null) {
                FileUtils.promptForSaveDirectory(this)
            } else {
                convertTextToAudio(text)
            }
        }

        // Mini player opens the full player.  The mini player itself is
        // automatically updated by [AudioService].
        binding.miniPlayer.root.setOnClickListener {
            startActivity(Intent(this, AudioPlayerActivity::class.java))
        }
    }

    /**
     * Reads imported text files and appends their contents into the input
     * field.  This method runs on a background thread to avoid blocking the
     * UI during file I/O operations.  Currently selected subject remains
     * unchanged; subjects apply to generated audio, not imported text.
     */
    private fun handleImportedFiles(uris: List<Uri>) {
        CoroutineScope(Dispatchers.IO).launch {
            val builder = StringBuilder()
            for (uri in uris) {
                try {
                    val content = FileUtils.readTextFromUri(this@MainActivity, uri)
                    builder.appendLine(content)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val importedText = builder.toString()
            CoroutineScope(Dispatchers.Main).launch {
                binding.editTextInput.append(importedText)
                Toast.makeText(this@MainActivity, getString(R.string.files_imported, uris.size), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Splits the provided text using [Splitter] and delegates to
     * [AudioGenerator] for conversion.  Conversion occurs on a background
     * coroutine.  A progress dialog is shown to keep the user informed.
     */
    private fun convertTextToAudio(text: String) {
        val saveDir = FileUtils.getBaseSaveDirectory(this) ?: return
        val subjectDir = FileUtils.getSubjectDirectory(this, currentSubject)
        // Ensure subject folder exists
        subjectDir.mkdirs()
        val chunks = Splitter.split(text)
        // Show a basic progress dialog
        val progressDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.converting))
            .setMessage(getString(R.string.please_wait))
            .setCancelable(false)
            .create()
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            AudioGenerator(this@MainActivity).convertChunksToAudio(
                chunks,
                subjectDir,
                currentSubject
            )
            CoroutineScope(Dispatchers.Main).launch {
                progressDialog.dismiss()
                Toast.makeText(this@MainActivity, getString(R.string.conversion_complete), Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Displays a dialog allowing the user to select an existing subject or
     * create a new one.  Subjects correspond to directories under the base
     * save location.  Changing the subject does not move existing files.
     */
    private fun showSubjectDialog() {
        val subjects = FileUtils.getAvailableSubjects(this).toMutableList()
        subjects.add(0, getString(R.string.new_subject))
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, subjects)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_subject))
            .setAdapter(adapter) { dialog, which ->
                dialog.dismiss()
                if (which == 0) {
                    // New subject: prompt for a name
                    val inputView = layoutInflater.inflate(R.layout.dialog_new_subject, null)
                    val input = inputView.findViewById<android.widget.EditText>(R.id.editSubjectName)
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.enter_subject_name))
                        .setView(inputView)
                        .setPositiveButton(android.R.string.ok) { d, _ ->
                            val newName = input.text.toString().trim()
                            if (newName.isNotEmpty()) {
                                currentSubject = newName
                                binding.txtCurrentSubject.text = newName
                            }
                            d.dismiss()
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                } else {
                    currentSubject = subjects[which]
                    binding.txtCurrentSubject.text = currentSubject
                }
            }
            .show()
    }
}