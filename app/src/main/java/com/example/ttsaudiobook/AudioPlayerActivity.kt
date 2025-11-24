package com.example.ttsaudiobook

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ttsaudiobook.databinding.ActivityAudioPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import java.io.File

/**
 * Simple activity that allows the user to play a series of audio files using an
 * [ExoPlayer] hosted inside a [PlayerService].  Playback continues in the
 * background thanks to the service.  Speed adjustments are discrete as
 * requested (1.0×, 1.1×, 1.2×, 1.3×, 1.5×, 1.7×, 2.0×).
 */
class AudioPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAudioPlayerBinding
    private var player: ExoPlayer? = null
    private var playbackSpeeds = floatArrayOf(1f, 1.1f, 1.2f, 1.3f, 1.5f, 1.7f, 2f)
    private var currentSpeedIndex = 0
    private var updateJob: Job? = null
    private var playerServiceBound = false
    private var playerService: PlayerService.PlayerBinder? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerService = service as PlayerService.PlayerBinder
            player = playerService?.getPlayer()
            // Load audio files for demonstration.  In a full app you would
            // retrieve these from the selected book.  Here we load all WAV files
            // in the app’s AudioBooks directory.
            val files = FileUtils.getAppRoot(this@AudioPlayerActivity)
                .walk()
                .filter { it.extension in setOf("wav", "mp3") }
                .map { it.absolutePath }
                .toList()
            if (files.isNotEmpty()) {
                playerService?.loadFiles(files)
                binding.tvTitle.text = Uri.parse(files[0]).lastPathSegment
                // Prepare and play first file
                playerService?.playIndex(0)
            }
            // Attach listeners and UI updates
            attachPlayerListeners()
            playerServiceBound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            playerServiceBound = false
            player = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Start the PlayerService and bind to it.  Starting ensures it runs in
        // the foreground; binding gives us access to the player instance.
        val intent = Intent(this, PlayerService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        // Set up button callbacks.  They refer to the player once bound.
        binding.btnPlayPause.setOnClickListener {
            val p = player ?: return@setOnClickListener
            if (p.isPlaying) {
                p.pause()
            } else {
                p.play()
            }
            updatePlayPauseButton()
        }
        binding.btnPrev.setOnClickListener {
            val p = player ?: return@setOnClickListener
            val index = p.currentMediaItemIndex
            if (index > 0) {
                p.seekTo(index - 1, 0L)
                p.play()
                binding.tvTitle.text = p.currentMediaItem?.mediaId
            }
        }
        binding.btnNext.setOnClickListener {
            val p = player ?: return@setOnClickListener
            val index = p.currentMediaItemIndex
            if (index < (p.mediaItemCount - 1)) {
                p.seekTo(index + 1, 0L)
                p.play()
                binding.tvTitle.text = p.currentMediaItem?.mediaId
            }
        }
        binding.btnSlow.setOnClickListener {
            adjustPlaybackSpeed(-1)
        }
        binding.btnFast.setOnClickListener {
            adjustPlaybackSpeed(1)
        }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val p = player ?: return
                    val duration = p.duration
                    if (duration > 0) {
                        val pos = duration * progress / 1000
                        p.seekTo(pos)
                    }
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun attachPlayerListeners() {
        val p = player ?: return
        // Listen for play/pause state to update button text
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlayPauseButton()
            }
        })
        // Periodically update the seek bar
        updateJob = lifecycleScope.launch {
            while (isActive) {
                val duration = p.duration
                val position = p.currentPosition
                if (duration > 0) {
                    val progress = (position * 1000 / duration).toInt()
                    binding.seekBar.progress = progress
                }
                delay(500)
            }
        }
        // Set initial speed label
        binding.tvSpeed.text = String.format("%.1f×", playbackSpeeds[currentSpeedIndex])
        p.playWhenReady = true
    }

    /**
     * Increase or decrease the playback speed based on the supplied delta.  The
     * speeds available are defined in [playbackSpeeds].  This method updates
     * the ExoPlayer instance and the on‑screen label accordingly.
     */
    private fun adjustPlaybackSpeed(delta: Int) {
        val p = player ?: return
        var newIndex = currentSpeedIndex + delta
        if (newIndex < 0) newIndex = 0
        if (newIndex >= playbackSpeeds.size) newIndex = playbackSpeeds.size - 1
        currentSpeedIndex = newIndex
        val speed = playbackSpeeds[currentSpeedIndex]
        val params = p.playbackParameters.withSpeed(speed)
        p.playbackParameters = params
        binding.tvSpeed.text = String.format("%.1f×", speed)
    }

    private fun updatePlayPauseButton() {
        val p = player
        val isPlaying = p?.isPlaying ?: false
        binding.btnPlayPause.text = if (isPlaying) "⏸" else "▶"
    }

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        if (playerServiceBound) {
            unbindService(connection)
            playerServiceBound = false
        }
        // Do not stop the PlayerService here; we want playback to continue in
        // the background.  If you wish to stop playback when leaving this
        // activity, call stopService(Intent(this, PlayerService::class.java)).
    }
}