package com.lao.tts

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Utility functions for file access, including reading imported text
 * documents and determining where to save generated audio.  The app stores
 * audio under a user‑selectable base directory; if the user has not yet
 * chosen one, a default location within the app's external files directory
 * is used.  You can extend these helpers to integrate with the Storage
 * Access Framework and allow the user to change the base directory at
 * runtime.
 */
object FileUtils {

    private const val PREFS_NAME = "rao_tts_prefs"
    private const val KEY_BASE_DIR = "base_save_dir"

    /**
     * Reads the contents of the given URI as UTF‑8 text.  The caller is
     * responsible for catching exceptions.  This method opens an
     * [InputStreamReader] on the content resolver and reads it into a
     * [StringBuilder].
     */
    fun readTextFromUri(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                val builder = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    builder.appendLine(line)
                    line = reader.readLine()
                }
                return builder.toString()
            }
        }
    }

    /**
     * Returns the base directory into which audio and subjects are saved.  If
     * the user has previously selected a directory via the settings screen
     * (not yet implemented), that directory is returned.  Otherwise the
     * default external files directory `/RaoTTS` is used.
     */
    fun getBaseSaveDirectory(context: Context): File? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_BASE_DIR, null)
        val dir = if (path != null) File(path) else {
            // Use app‑specific external directory as a safe default
            File(context.getExternalFilesDir(null), "RaoTTS")
        }
        return dir
    }

    /**
     * Prompts the user to choose a directory to save audio files.  This uses
     * the Storage Access Framework to obtain a tree URI and stores it in
     * SharedPreferences.  For brevity this implementation is a stub; you
     * should call this from an Activity and handle the result via
     * `ActivityResultContracts.OpenDocumentTree`.
     */
    fun promptForSaveDirectory(context: Context) {
        // In a full implementation you would launch an Intent here and persist
        // the returned URI.  For this sample we simply inform the user and
        // continue using the default directory.
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.using_default_save_dir),
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Returns the directory for a given subject.  A subject corresponds to
     * a subdirectory under the base save location.  The returned directory
     * may not exist; callers should ensure it is created before use.
     */
    fun getSubjectDirectory(context: Context, subject: String): File {
        val base = getBaseSaveDirectory(context) ?: context.filesDir
        return File(base, subject)
    }

    /**
     * Lists available subjects by scanning subdirectories of the base save
     * location.  Only directories are returned; files are ignored.  If
     * none exist, an empty list is returned.
     */
    fun getAvailableSubjects(context: Context): List<String> {
        val base = getBaseSaveDirectory(context) ?: return emptyList()
        val files = base.listFiles() ?: return emptyList()
        return files.filter { it.isDirectory }.map { it.name ?: "" }
    }
}