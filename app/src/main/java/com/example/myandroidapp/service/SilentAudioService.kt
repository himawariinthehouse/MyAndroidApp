package com.example.myandroidapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
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

    private fun startSilentPlayback() {
        if (mediaPlayer != null) return
        mediaPlayer = MediaPlayer.create(this, R.raw.silent_audio)?.apply {
            isLooping = true
            setVolume(0f, 0f)
            start()
        }
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
        isRunning = false
        super.onDestroy()
    }
}
