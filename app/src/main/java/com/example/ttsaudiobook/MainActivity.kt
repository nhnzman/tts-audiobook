package com.example.ttsaudiobook

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ttsaudiobook.databinding.ActivityMainBinding

/**
 * Entry point of the application.  Presents three primary actions: paste text,
 * import a text file and view existing audio books.  When the user chooses to
 * convert text to audio, this activity starts the [ConversionService] in the
 * foreground so that conversion continues even if the app is backgrounded.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // Register an activity result launcher for file picking.  This new API
    // simplifies requesting a document via the system file picker.
    private val pickTxtFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let { handleImportedFile(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Paste text button: show dialog for manual entry.  When confirmed we
        // prompt for subject and title and start conversion.
        binding.btnPasteText.setOnClickListener {
            showPasteTextDialog()
        }

        // Import text file button: launch the system file picker.  We restrict
        // MIME type to text/plain to filter for .txt files.
        binding.btnImportText.setOnClickListener {
            pickTxtFile.launch(arrayOf("text/plain"))
        }

        // View books: for brevity we reuse the AudioPlayerActivity as a simple
        // file browser; in a full app this would be a dedicated list activity.
        binding.btnViewBooks.setOnClickListener {
            val intent = Intent(this, AudioPlayerActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Display a dialog containing a multi‑line text box for the user to paste
     * arbitrary text.  When the user taps OK we ask for a subject and title
     * and launch the conversion.
     */
    private fun showPasteTextDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 4
            hint = "여기에 텍스트를 붙여넣으세요"
        }
        AlertDialog.Builder(this)
            .setTitle("텍스트 입력")
            .setView(input)
            .setPositiveButton("확인") { _, _ ->
                val text = input.text.toString()
                if (text.isNotBlank()) {
                    promptSubjectAndConvert(text)
                } else {
                    Toast.makeText(this, "텍스트가 비어 있습니다", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * Prompt the user to enter a subject and title for the conversion.  Once
     * provided we start the [ConversionService] with the full text.
     */
    private fun promptSubjectAndConvert(text: String) {
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_subject_title, null)
        val etSubject = dialogView.findViewById<EditText>(R.id.etSubject)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        AlertDialog.Builder(this)
            .setTitle("과목 및 제목 입력")
            .setView(dialogView)
            .setPositiveButton("시작") { _, _ ->
                val subject = etSubject.text.toString().ifBlank { "General" }
                val title = etTitle.text.toString().ifBlank { "Untitled" }
                startConversionService(text, subject, title)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    /**
     * Start the [ConversionService] in the foreground with the given text,
     * subject and title.  The service will handle splitting and synthesising
     * asynchronously.  Because it's a foreground service the conversion can
     * continue while the user switches to another app.
     */
    private fun startConversionService(text: String, subject: String, title: String) {
        val serviceIntent = Intent(this, ConversionService::class.java).apply {
            putExtra(ConversionService.EXTRA_TEXT, text)
            putExtra(ConversionService.EXTRA_SUBJECT, subject)
            putExtra(ConversionService.EXTRA_TITLE, title)
        }
        // Start the service.  On Android 8.0+ startForegroundService must be
        // used to launch a foreground service.  Once started, the service
        // becomes responsible for calling startForeground().
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    /**
     * Handle a file selected from the system file picker.  We read its
     * contents into a string and prompt the user for subject/title before
     * launching the conversion service.  This method runs on the main thread
     * because file reading for small .txt files is usually quick; for very
     * large files you may wish to offload to a background thread.
     */
    private fun handleImportedFile(uri: Uri) {
        // Query the display name of the file to use as default title
        var name = "Imported"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
                // Remove extension if present
                name = name.substringBeforeLast('.')
            }
        }
        // Read the entire file into a String.  Note: this may block the UI
        // thread for very large files; consider using a coroutine for production.
        val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        if (text == null) {
            Toast.makeText(this, "파일을 읽을 수 없습니다", Toast.LENGTH_SHORT).show()
            return
        }
        // Ask for subject and optional override of title
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_subject_title, null)
        val etSubject = dialogView.findViewById<EditText>(R.id.etSubject)
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        etTitle.setText(name)
        AlertDialog.Builder(this)
            .setTitle("과목 및 제목 입력")
            .setView(dialogView)
            .setPositiveButton("시작") { _, _ ->
                val subject = etSubject.text.toString().ifBlank { "General" }
                val title = etTitle.text.toString().ifBlank { name }
                startConversionService(text, subject, title)
            }
            .setNegativeButton("취소", null)
            .show()
    }
}