package com.example.ttsaudiobook

import android.content.Context
import java.io.File

/**
 * A collection of helper functions for dealing with file and directory management.  All
 * audio books and their associated text files are stored under the app’s private
 * external storage directory in a folder named `AudioBooks`.  Within that folder
 * subjects are represented as subdirectories.  Each text import and its generated
 * chunks share a common base name (e.g. `민법01`), and the individual MP3 files are
 * enumerated as `민법01-1.mp3`, `민법01-2.mp3`, etc.
 */
object FileUtils {

    /**
     * Return the root directory used to store all audio book content.  This folder
     * resides in the app’s private external files area, which means it is visible to
     * the user via a file manager but is deleted when the app is uninstalled.
     *
     * @param context the current activity or application context
     */
    fun getAppRoot(context: Context): File {
        // getExternalFilesDir(null) returns a path like /storage/emulated/0/Android/data/…/files
        val root = File(context.getExternalFilesDir(null), "AudioBooks")
        if (!root.exists()) {
            // Create the directory if it does not yet exist.  mkdirs() creates
            // intermediate directories as needed and returns true if successful.
            root.mkdirs()
        }
        return root
    }

    /**
     * Obtain a File object representing a specific subject folder.  The folder will be
     * created if it does not already exist.
     *
     * @param context the context used to resolve the external files directory
     * @param subject the name of the subject (e.g. "민법")
     * @return a File pointing to the subject directory
     */
    fun getSubjectDir(context: Context, subject: String): File {
        // Sanitize the subject string to avoid illegal characters in file names.
        val sanitized = subject.replace(Regex("[^a-zA-Z0-9가-힣 _-]"), "_")
        val dir = File(getAppRoot(context), sanitized)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generate sequential file names for MP3 outputs based on a common base name.  For
     * example, calling generateFileNames("민법01", 3) returns a list containing
     * `["민법01-1.mp3", "민법01-2.mp3", "민법01-3.mp3"]`.
     *
     * @param base the common prefix of the file names, typically derived from the
     *             imported text file’s name without extension
     * @param count the number of chunks / files to generate
     */
    fun generateFileNames(base: String, count: Int): List<String> {
        val names = mutableListOf<String>()
        for (i in 1..count) {
            names.add("$base-$i.mp3")
        }
        return names
    }
}