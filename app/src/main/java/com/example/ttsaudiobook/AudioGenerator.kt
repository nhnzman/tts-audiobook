package com.example.ttsaudiobook

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.io.File
import java.util.Locale

/**
 * Handles conversion of text chunks into audio files using the Android system’s
 * Text‑To‑Speech (TTS) service.  A single instance of [TextToSpeech] is reused to
 * minimise initialization overhead.  The [voice] property determines which TTS
 * engine/voice will be used; this is assigned externally after the user chooses a voice.
 *
 * Note: For simplicity this implementation synthesises directly to WAV files via
 * [TextToSpeech.synthesizeToFile].  If you require MP3 output you can post‑process
 * the WAV files with a library such as FFmpeg (e.g. using the ffmpeg‑kit wrapper).
 */
class AudioGenerator(
    private val context: Context,
    private val voice: Voice? = null
) {
    // Underlying TextToSpeech engine.  It is initialised lazily in the init block.
    private val tts: TextToSpeech
    // Track how many chunks have been processed and total chunks to allow progress callbacks.
    private var processedCount = 0
    private var totalCount = 0
    // Callback to invoke after each chunk is synthesised.  It receives the current
    // processed count and total chunk count.
    private var progressCallback: ((Int, Int) -> Unit)? = null
    // Callback invoked once all synthesis jobs have completed.
    private var doneCallback: (() -> Unit)? = null

    init {
        // Initialise the TextToSpeech engine.  It may take a moment to return the
        // SUCCESS status.  We use applicationContext to avoid leaking the Activity.
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // If the user selected a specific voice then assign it; otherwise
                // leave the default voice.  When a voice is set that belongs to a
                // different TTS engine, Android automatically switches engines.
                voice?.let { tts.voice = it }
                // Optionally set the language to Korean to improve pronunciation.  The
                // return value indicates whether the language is supported.
                val res = tts.setLanguage(Locale.KOREAN)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fall back to default language if Korean is not available.
                    tts.setLanguage(Locale.getDefault())
                }
                // Attach a progress listener.  We use a single listener for all
                // utterances; each utterance is identified by its utteranceId which we
                // derive from the index of the chunk.
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // Nothing to do when a chunk starts.
                    }
                    override fun onDone(utteranceId: String?) {
                        // Increase the processed count and report progress on the main thread.
                        processedCount++
                        progressCallback?.invoke(processedCount, totalCount)
                        // When all chunks are done, invoke completion callback.
                        if (processedCount == totalCount) {
                            doneCallback?.invoke()
                        }
                    }
                    override fun onError(utteranceId: String?) {
                        // Errors are logged but do not stop the loop; increment count.
                        processedCount++
                        progressCallback?.invoke(processedCount, totalCount)
                        if (processedCount == totalCount) {
                            doneCallback?.invoke()
                        }
                    }
                })
            }
        }
    }

    /**
     * Synthesize a list of chunks into audio files.  The output files are named
     * sequentially using [baseName] and saved into [outputDir].  Each chunk is
     * synthesised asynchronously; progress and completion are reported via the
     * callback parameters.
     *
     * @param chunks the list of text chunks to synthesise
     * @param outputDir the directory in which to save output files
     * @param baseName the common prefix for output file names (without extension)
     * @param onProgress invoked after each chunk is completed, with (current, total)
     * @param onDone invoked once all chunks have been synthesised
     */
    fun synthesizeChunks(
        chunks: List<String>,
        outputDir: File,
        baseName: String,
        onProgress: (current: Int, total: Int) -> Unit,
        onDone: () -> Unit
    ) {
        // Store callbacks and counters for use inside the progress listener.
        this.totalCount = chunks.size
        this.processedCount = 0
        this.progressCallback = onProgress
        this.doneCallback = onDone
        // Ensure the output directory exists.
        if (!outputDir.exists()) outputDir.mkdirs()
        // Loop through all chunks and schedule synthesis.  Each utteranceId
        // corresponds to its index so we can correlate progress if needed.
        for ((index, chunk) in chunks.withIndex()) {
            // Create a temporary WAV file name for this chunk.  Android’s TTS will
            // write PCM WAV by default when using the File parameter.  You can
            // change the extension to ".wav" if you plan to convert later.
            val outFile = File(outputDir, "$baseName-${index + 1}.wav")
            val params = Bundle()
            // Provide the utterance ID so the progress listener can track this chunk.
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
            // The synthesizeToFile method queues the job immediately.  It returns an
            // int status code synchronously but we rely on the progress listener for
            // completion.
            tts.synthesizeToFile(chunk, params, outFile, index.toString())
        }
    }

    /**
     * Release the underlying TextToSpeech resources.  Should be called when the
     * generating activity is destroyed to free system resources.
     */
    fun shutdown() {
        tts.shutdown()
    }
}