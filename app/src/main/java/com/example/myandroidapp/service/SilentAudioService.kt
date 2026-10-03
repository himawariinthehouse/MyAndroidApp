package com.example.myandroidapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.example.myandroidapp.MainActivity
import com.example.myandroidapp.R

/**
 * 前台服务：循环播放静音音频，使应用持续在后台运行。
 *
 * 为使播放会话映射到鸿蒙媒体控制中心（卓易通容器），同时满足三要素：
 * 1. AudioManager 音频焦点：申请焦点并处理焦点变化回调；
 * 2. MediaSessionCompat：注册 callback → 设置 PlaybackState/Metadata → setActive(true)；
 * 3. MediaStyle 前台通知：startForeground 并绑定 sessionToken，紧凑视图提供播放/暂停按钮。
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

        /** 播放进度同步到会话的间隔（毫秒） */
        private const val PROGRESS_UPDATE_INTERVAL_MS = 1000L

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
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    private val handler = Handler(Looper.getMainLooper())

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

    /** 初始化 MediaSessionCompat，严格按顺序：callback → PlaybackState/Metadata → isActive */
    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "SilentAudioSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            // 1. 注册回调，响应控制中心卡片、锁屏、耳机按键
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
            // 2. 设置元数据与初始播放状态
            setMetadata(buildMediaMetadata())
            setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_NONE))
            // 3. 最后激活会话，使播放器进入媒体控制中心
            isActive = true
        }
        // 将会话 token 关联到浏览服务，供客户端（系统控制中心/车机等）发现并控制播放
        mediaSession?.let { setSessionToken(it.sessionToken) }
    }

    /** 会话回调：响应控制中心卡片、锁屏、耳机线控等系统媒体按键 */
    private val sessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() {
            resumePlayback()
        }

        override fun onPause() {
            pausePlayback()
            // 用户主动暂停：释放音频焦点，将播放让给其他应用
            abandonAudioFocus()
        }

        override fun onSeekTo(pos: Long) {
            // 拖动进度条：定位到新位置并同步最新进度
            mediaPlayer?.seekTo(pos.toInt())
            mediaSession?.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING))
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

    private fun buildMediaMetadata(duration: Long = 0L) = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "保持后台运行")
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getString(R.string.app_name))
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, loadAlbumArt())
        .build()

    /** 用应用图标作为专辑封面 */
    private fun loadAlbumArt() = applicationInfo?.loadIcon(packageManager)?.toBitmap(512, 512)

    /** 播放器就绪后补充元数据（音频时长） */
    private fun updateMediaMetadata() {
        mediaSession?.setMetadata(buildMediaMetadata(mediaPlayer?.duration?.toLong() ?: 0L))
    }

    private fun buildPlaybackState(state: Int) = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO
        )
        .setState(
            state,
            mediaPlayer?.currentPosition?.toLong() ?: PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
            if (state == PlaybackStateCompat.STATE_PLAYING) 1f else 0f,
            SystemClock.elapsedRealtime()
        )
        .build()

    /** 同步播放状态到 MediaSession，并刷新前台通知按钮与状态一致 */
    private fun updatePlaybackState(state: Int) {
        mediaSession?.setPlaybackState(buildPlaybackState(state))
        getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, buildNotification())
    }

    /** 播放中周期性同步播放进度（只更新会话，不重建通知避免频繁刷新） */
    private val progressUpdater = object : Runnable {
        override fun run() {
            if (mediaPlayer?.isPlaying == true) {
                mediaSession?.setPlaybackState(buildPlaybackState(PlaybackStateCompat.STATE_PLAYING))
                handler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun startProgressUpdates() {
        handler.removeCallbacks(progressUpdater)
        handler.postDelayed(progressUpdater, PROGRESS_UPDATE_INTERVAL_MS)
    }

    private fun stopProgressUpdates() {
        handler.removeCallbacks(progressUpdater)
    }

    // ---------- 音频焦点管理 ----------

    /** 申请音频焦点，成功返回 true；已持有焦点时直接返回 true */
    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        val am = audioManager ?: getSystemService(AudioManager::class.java)?.also { audioManager = it }
            ?: return false
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    /** 释放音频焦点 */
    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return
        val am = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    /** 焦点变化回调：处理来电、其他应用抢占焦点等场景 */
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久失去焦点：暂停播放并放弃焦点
                hasAudioFocus = false
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 暂时失去焦点（如来电）：暂停，待 AUDIOFOCUS_GAIN 时恢复
                hasAudioFocus = false
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 重新获得焦点：恢复播放
                resumePlayback()
            }
        }
    }

    // ---------- 播放控制 ----------

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
            // 播放器就绪后补充元数据（时长、封面）
            updateMediaMetadata()
        }
        if (!requestAudioFocus()) return
        mediaPlayer?.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        startProgressUpdates()
    }

    private fun pausePlayback() {
        stopProgressUpdates()
        mediaPlayer?.pause()
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }

    private fun resumePlayback() {
        if (!requestAudioFocus()) return
        if (mediaPlayer == null) {
            startSilentPlayback()
            return
        }
        mediaPlayer?.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        startProgressUpdates()
    }

    /** 单曲模式下切歌：重置播放进度并继续播放 */
    private fun restartPlayback() {
        mediaPlayer?.let {
            it.seekTo(0)
            it.start()
        }
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        startProgressUpdates()
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
        // 右上角关闭按钮：停止服务（等价于应用内"停止"按钮）
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
        stopProgressUpdates()
        mediaPlayer?.release()
        mediaPlayer = null
        // 释放音频焦点与会话资源
        abandonAudioFocus()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        isRunning = false
        super.onDestroy()
    }
}
