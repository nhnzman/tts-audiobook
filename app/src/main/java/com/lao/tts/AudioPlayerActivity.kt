package com.lao.tts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.lao.tts.databinding.ActivityAudioPlayerBinding

/**
 * Full screen playback activity.  This activity binds to [AudioService] to
 * control ExoPlayer and exposes UI to change playback speed.  It expects a
 * playlist of absolute file paths via the `EXTRA_PLAYLIST` extra.
 */
class AudioPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioPlayerBinding
    private lateinit var player: ExoPlayer
    private val speeds = floatArrayOf(1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.5f, 3.0f)
    private var speedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve playlist from intent.  If none provided, build one from the
        // current subject directory.  This allows the user to open the player
        // directly from the mini player without passing extras.
        val playlist = intent.getStringArrayListExtra(AudioService.EXTRA_PLAYLIST)
            ?: run {
                val subject = intent.getStringExtra("subject") ?: "기본"
                val dir = FileUtils.getSubjectDirectory(this, subject)
                val list = dir.listFiles { file -> file.extension.equals("mp3", ignoreCase = true) }
                    ?.sortedBy { it.name }?.map { it.absolutePath } ?: emptyList()
                ArrayList(list)
            }
        val startIndex = intent.getIntExtra(AudioService.EXTRA_START_INDEX, 0)

        // Start playback via the foreground service
        Intent(this, AudioService::class.java).also { svcIntent ->
            svcIntent.putStringArrayListExtra(AudioService.EXTRA_PLAYLIST, playlist)
            svcIntent.putExtra(AudioService.EXTRA_START_INDEX, startIndex)
            startService(svcIntent)
        }

        // Set up a local player for UI display.  We let the service own the
        // actual playback; this player simply attaches to the same playlist
        // for position updates.  Because both players read from the same files
        // there is negligible overhead.
        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player
        val mediaItems = playlist.map { path -> MediaItem.fromUri(android.net.Uri.fromFile(java.io.File(path))) }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()

        // Speed control button cycles through predefined values
        binding.btnSpeed.setOnClickListener {
            speedIndex = (speedIndex + 1) % speeds.size
            val speed = speeds[speedIndex]
            player.setPlaybackSpeed(speed)
            binding.btnSpeed.text = "${speed}x"
        }
    }

    override fun onStop() {
        super.onStop()
        player.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}