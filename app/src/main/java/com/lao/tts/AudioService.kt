package com.lao.tts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager

/**
 * Foreground service responsible for audio playback using ExoPlayer.  It
 * exposes simple controls via a notification and keeps the player alive
 * while the app is in the background.  Activities bind to this service to
 * control playback and observe player state.
 */
class AudioService : Service() {

    private lateinit var player: ExoPlayer
    private var notificationManager: PlayerNotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        createNotificationChannel()
        // Set up a notification manager that attaches to the player.  It
        // handles showing the notification and responding to user actions.
        notificationManager = PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        ).setMediaDescriptionAdapter(object : PlayerNotificationManager.MediaDescriptionAdapter {
            override fun getCurrentContentTitle(player: Player): CharSequence {
                return player.mediaMetadata.title ?: getString(R.string.app_name)
            }

            override fun createCurrentContentIntent(player: Player): android.app.PendingIntent? {
                val intent = Intent(this@AudioService, AudioPlayerActivity::class.java)
                return android.app.PendingIntent.getActivity(
                    this@AudioService,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) android.app.PendingIntent.FLAG_IMMUTABLE else 0
                )
            }

            override fun getCurrentContentText(player: Player): CharSequence? {
                return null
            }

            override fun getCurrentLargeIcon(
                player: Player,
                callback: PlayerNotificationManager.BitmapCallback
            ): android.graphics.Bitmap? = null
        }).setChannelImportance(NotificationManagerCompat.IMPORTANCE_LOW)
            .build()
        notificationManager?.setPlayer(player)
        notificationManager?.setUseNextAction(true)
        notificationManager?.setUsePreviousAction(true)
        notificationManager?.setUsePlayPauseActions(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val playlist = it.getStringArrayListExtra(EXTRA_PLAYLIST)
            val startIndex = it.getIntExtra(EXTRA_START_INDEX, 0)
            if (playlist != null && playlist.isNotEmpty()) {
                val mediaItems = playlist.map { path ->
                    MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path)))
                }
                player.setMediaItems(mediaItems, startIndex, 0L)
                player.prepare()
                player.play()
            }
        }
        // Start the service in the foreground to ensure it stays alive.
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.playing_audio))
                .setSmallIcon(R.drawable.ic_stat_name)
                .build()
        )
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager?.setPlayer(null)
        player.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playback_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.playback_channel_desc)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "rao_tts_playback"
        private const val NOTIFICATION_ID = 1
        const val EXTRA_PLAYLIST = "extra_playlist"
        const val EXTRA_START_INDEX = "extra_start_index"
    }
}