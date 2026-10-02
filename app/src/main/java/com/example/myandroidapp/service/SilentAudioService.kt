package com.example.myandroidapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.myandroidapp.MainActivity
import com.example.myandroidapp.R

/**
 * 前台服务：循环播放静音音频，使应用持续在后台运行
 */
class SilentAudioService : Service() {

    companion object {
        private const val ACTION_START = "com.example.myandroidapp.action.START_AUDIO"
        private const val ACTION_STOP = "com.example.myandroidapp.action.STOP_AUDIO"
        private const val CHANNEL_ID = "silent_audio_channel"
        private const val NOTIFICATION_ID = 1

        /** 服务是否正在运行（供界面同步按钮状态） */
        @Volatile
        var isRunning = false
            private set

        fun startIntent(context: Context): Intent =
            Intent(context, SilentAudioService::class.java).setAction(ACTION_START)

        fun stopIntent(context: Context): Intent =
            Intent(context, SilentAudioService::class.java).setAction(ACTION_STOP)
    }

    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundCompat()
                startSilentPlayback()
                return START_STICKY
            }
        }
    }

    private fun startForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        isRunning = true
    }

    /** 初始化 MediaSession，使播放器显示在系统控制中心 */
    private fun initMediaSession() {
        mediaSession = MediaSession(this, "SilentAudioSession").apply {
            setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(sessionCallback)
            setSessionActivity(
                PendingIntent.getActivity(
                    this@SilentAudioService,
                    0,
                    Intent(this@SilentAudioService, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            isActive = true
            setMetadata(buildMediaMetadata())
            setPlaybackState(buildPlaybackState(PlaybackState.STATE_PLAYING))
        }
    }

    private val sessionCallback = object : MediaSession.Callback() {
        override fun onPlay() {
            startSilentPlayback()
        }

        override fun onPause() {
            mediaPlayer?.pause()
            updatePlaybackState(PlaybackState.STATE_PAUSED)
        }

        override fun onStop() {
            stopSelf()
        }
    }

    private fun buildMediaMetadata() = MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_TITLE, "保持后台运行")
        .putString(MediaMetadata.METADATA_KEY_ARTIST, getString(R.string.app_name))
        .build()

    private fun buildPlaybackState(state: Int) = PlaybackState.Builder()
        .setActions(
            PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_STOP
        )
        .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
        .build()

    private fun updatePlaybackState(state: Int) {
        mediaSession?.setPlaybackState(buildPlaybackState(state))
    }

    private fun startSilentPlayback() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.silent_audio)?.apply {
                isLooping = true
                setVolume(0f, 0f)
            }
        }
        mediaPlayer?.start()
        updatePlaybackState(PlaybackState.STATE_PLAYING)
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("保持后台运行")
            .setContentText("正在播放静音音频")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .addAction(0, "停止", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "后台运行服务",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "用于保持应用后台运行的静音音频服务"
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        isRunning = false
        super.onDestroy()
    }
}
