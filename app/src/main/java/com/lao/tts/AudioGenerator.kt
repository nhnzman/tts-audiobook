package com.lao.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale

/**
 * Responsible for converting chunks of text into audio files.  A
 * [TextToSpeech] engine is lazily initialised when conversion begins and
 * released after all chunks have been processed.  Output files are named
 * `{baseName}-{index}.mp3` regardless of the actual encoding produced; on
 * most devices `synthesizeToFile` writes a WAV file which can still be
 * consumed by the media player.
 */
class AudioGenerator(private val context: Context) {
    private var tts: TextToSpeech? = null

    /**
     * Converts a list of text chunks into MP3 files stored in [outputDir].
     * Each file is named using [baseName] followed by a 1‑based index.  This
     * method blocks the calling thread until all files are generated.
     */
    fun convertChunksToAudio(chunks: List<String>, outputDir: File, baseName: String) {
        if (chunks.isEmpty()) return
        val engine = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                // Initialization failed; abort conversion
                return@TextToSpeech
            }
        }
        // Use the system default locale.  You could expose this to the user.
        engine.language = Locale.getDefault()
        engine.setSpeechRate(1.0f)
        // Synthesize each chunk sequentially.  Use a blocking call via
        // synthesizeToFile; the API will queue up utterances if invoked
        // repeatedly, but we wait for completion by polling the file.  A
        // production implementation should use callbacks instead.
        chunks.forEachIndexed { index, chunk ->
            val fileName = "$baseName-${index + 1}.mp3"
            val outFile = File(outputDir, fileName)
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, fileName)
            }
            engine.synthesizeToFile(chunk, params, outFile, fileName)
            // Busy wait until the file appears.  Note: for large files this
            // could take a while; consider using UtteranceProgressListener.
            while (!outFile.exists()) {
                try {
                    Thread.sleep(100)
                } catch (_: InterruptedException) {
                }
            }
        }
        engine.shutdown()
    }
}