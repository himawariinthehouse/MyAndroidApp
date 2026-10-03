package com.example.myandroidapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.example.myandroidapp.MainActivity
import com.example.myandroidapp.R

/**
 * 前台服务：循环播放静音音频，使应用持续在后台运行。
 *
 * 基于 MediaBrowserServiceCompat + MediaSessionCompat 实现系统媒体控制中心集成：
 * - MediaStyle 前台通知绑定 MediaSession token，播放时系统媒体控制中心会出现播放卡片；
 * - 通过 MediaSessionCompat.Callback 响应播放/暂停/切歌等系统媒体按键
 *   （耳机线控、蓝牙设备、控制中心卡片、锁屏控制器）；
 * - MediaButtonReceiver 负责接收并转发媒体按键广播。
 *
 * 注意：MEDIA_CONTENT_CONTROL 权限为 signature|privileged 级别，仅系统应用可获得，
 * 第三方应用声明无效，本媒体控制中心功能并不依赖该权限。
 */
class SilentAudioService : MediaBrowserServiceCompat() {

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
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 媒体按键（耳机线控/蓝牙/系统按键）由 MediaButtonReceiver 转发到此处处理
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            mediaSession?.let { MediaButtonReceiver.handleIntent(it, intent) }
            return START_STICKY
        }
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

    /** 初始化 MediaSessionCompat，使播放器显示在系统媒体控制中心 */
    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "SilentAudioSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
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
            // 注册媒体按键接收器：API 26+ 系统直接向本会话派发媒体按键事件
            setMediaButtonReceiver(
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this@SilentAudioService,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE
                )
            )
            isActive = true
            setMetadata(buildMediaMetadata())
            setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING))
        }
        // 将会话 token 关联到浏览服务，供客户端（系统控制中心/车机等）发现并控制播放
        mediaSession?.let { setSessionToken(it.sessionToken) }
    }

    /** 会话回调：响应控制中心卡片、锁屏、耳机线控等系统媒体按键 */
    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            startSilentPlayback()
        }

        override fun onPause() {
            mediaPlayer?.pause()
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        }

        override fun onStop() {
            stopSelf()
        }

        override fun onSkipToNext() {
            // 当前只有单曲静音音频，切歌仅重置播放进度并继续播放
            restartPlayback()
        }

        override fun onSkipToPrevious() {
            // 当前只有单曲静音音频，切歌仅重置播放进度并继续播放
            restartPlayback()
        }
    }

    private fun buildMediaMetadata() = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "保持后台运行")
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getString(R.string.app_name))
        .build()

    private fun buildPlaybackState(state: Int) = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP
        )
        .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
        .build()

    /** 同步播放状态到 MediaSession，并刷新前台通知按钮与状态一致 */
    private fun updatePlaybackState(state: Int) {
        mediaSession?.setPlaybackState(buildPlaybackState(state))
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification())
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
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.silent_audio)?.apply {
                isLooping = true
                setVolume(0f, 0f)
            }
        }
        mediaPlayer?.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    /** 单曲模式下切歌：重置播放进度并继续播放 */
    private fun restartPlayback() {
        mediaPlayer?.let {
            it.seekTo(0)
            it.start()
        }
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    /**
     * 构建 MediaStyle 前台通知。
     * 绑定 MediaSession token 后，播放时系统媒体控制中心即出现播放卡片；
     * 通知按钮通过 MediaButtonReceiver 广播转发回服务，最终派发到会话回调。
     */
    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // 右上角关闭按钮：停止服务（等价于应用内“停止”按钮）
        val cancelIntent = PendingIntent.getService(
            this,
            0,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // 根据当前播放状态动态切换播放/暂停按钮
        val playing = mediaPlayer?.isPlaying == true
        val style = MediaNotificationCompat.MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(cancelIntent)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("保持后台运行")
            .setContentText("正在播放静音音频")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            // 锁屏界面需要 PUBLIC 可见性才能展示媒体卡片与控制按钮
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_previous,
                "上一首",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "暂停" else "播放",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    if (playing) PlaybackStateCompat.ACTION_PAUSE else PlaybackStateCompat.ACTION_PLAY
                )
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "下一首",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )
            .setStyle(style)
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

    // 本服务仅供应用内部播放使用，不对外提供媒体浏览内容

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot =
        BrowserRoot("silent_audio_root", null)

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        // 无媒体浏览内容，返回空列表
        result.sendResult(emptyList())
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
