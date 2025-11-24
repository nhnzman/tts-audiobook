package com.example.ttsaudiobook

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * A foreground service that performs long‑running text‑to‑speech conversions in the
 * background.  When the user starts a conversion from the UI, this service is
 * launched with the relevant parameters.  Running as a foreground service with a
 * persistent notification prevents the system from killing the process when the app
 * is not in the foreground.
 */
class ConversionService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        // This service does not provide binding; return null.
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Extract conversion parameters from the intent.  The calling activity must
        // include these extras when starting the service.
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: ""
        val subject = intent?.getStringExtra(EXTRA_SUBJECT) ?: "Default"
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Untitled"
        // Build a minimal persistent notification to keep the service alive.  On
        // Android 8.0 (API 26) and above a notification channel is required.
        val channelId = createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Converting text to audio…")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        // Start the service in the foreground.  The ID of 1 is arbitrary and can
        // be any unique integer.  The notification will remain until
        // stopForeground(true) is called.
        startForeground(NOTIFICATION_ID, notification)
        // Perform conversion on a background thread to avoid blocking the main
        // thread.  In a production app you might use a coroutine or a WorkManager.
        Thread {
            // Split the text according to the smart hyphen rule
            val chunks = Splitter.split(text)
            // Prepare output directory and file names
            val outputDir = FileUtils.getSubjectDir(this, subject)
            val fileNames = FileUtils.generateFileNames(title, chunks.size)
            // Select the default voice; for simplicity we do not choose here.  To
            // customise, you could pass in the voice information via intent extras.
            val generator = AudioGenerator(this, null)
            // Synthesize all chunks.  We ignore progress updates in this service; a
            // full implementation could broadcast progress via a LocalBroadcastManager.
            generator.synthesizeChunks(chunks, outputDir, title, { _, _ -> }, {
                // When done, stop the foreground state and stop this service.
                stopForeground(true)
                stopSelf()
            })
        }.start()
        // If the system kills the service after onStartCommand returns, restart it
        return START_NOT_STICKY
    }

    /**
     * Create a notification channel on Android O and above.  Notification
     * channels are required to post notifications.  This method returns the
     * channel ID which is used when building notifications.
     */
    private fun createNotificationChannel(): String {
        val channelId = "conversion_service"
        val channelName = "Conversion Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
        return channelId
    }

    companion object {
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_SUBJECT = "extra_subject"
        const val EXTRA_TITLE = "extra_title"
        private const val NOTIFICATION_ID = 1
    }
}