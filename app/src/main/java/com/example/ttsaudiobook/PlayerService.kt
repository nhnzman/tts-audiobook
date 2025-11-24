package com.example.ttsaudiobook

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem

/**
 * A foreground service that hosts an [ExoPlayer] instance for background audio
 * playback.  By moving playback into a service with a persistent notification,
 * audio continues seamlessly when the app moves to the background.  The
 * [PlayerService] exposes a simple binder for controlling the player from the
 * activity.
 */
class PlayerService : Service() {

    private val binder = PlayerBinder()
    private lateinit var player: ExoPlayer

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        // Initialise ExoPlayer.  Do not pass a context associated with an
        // Activity to avoid leaks.
        player = ExoPlayer.Builder(this).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If this service is started (rather than bound) we prepare a basic
        // notification.  In a full implementation you would update the
        // notification with media controls.
        val channelId = createChannel()
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Playing audio")
            .setContentText("Background playback")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
        startForeground(2, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }

    /**
     * Expose control methods via a Binder.  Activities that bind to this
     * service can call [loadFiles] and [playIndex] to drive playback.
     */
    inner class PlayerBinder : Binder() {
        fun getPlayer(): ExoPlayer = player
        /**
         * Prepare the player with a list of media items pointing to file paths.
         */
        fun loadFiles(paths: List<String>) {
            val items = paths.map { path ->
                MediaItem.fromUri(android.net.Uri.parse(path))
            }
            player.setMediaItems(items)
            player.prepare()
        }
        /**
         * Start playback from the specified index in the playlist.
         */
        fun playIndex(index: Int) {
            player.seekTo(index, 0L)
            player.playWhenReady = true
        }
    }

    /**
     * Create notification channel required for foreground service on Android O+.
     */
    private fun createChannel(): String {
        val channelId = "player_service"
        val channelName = "Audio Playback"
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
}