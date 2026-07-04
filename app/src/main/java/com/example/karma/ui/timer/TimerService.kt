package com.example.karma.ui.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.karma.MainActivity
import com.example.karma.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val startElapsed: Long = 0L,       // SystemClock.elapsedRealtime() 记录的开始点
    val resumeElapsed: Long = 0L,      // 最近一次恢复时的 elapsedRealtime
    val accumulatedMs: Long = 0L,      // 暂停期间累计的已计时毫秒数
    val selectedScore: Float = 0f,
    val selectedEvent: String = "",
) {
    /** 当前总已计时毫秒数 */
    fun currentElapsedMs(): Long {
        return when (status) {
            TimerStatus.RUNNING -> accumulatedMs + (SystemClock.elapsedRealtime() - resumeElapsed)
            TimerStatus.PAUSED -> accumulatedMs
            TimerStatus.IDLE, TimerStatus.STOPPED -> accumulatedMs
        }
    }
}

enum class TimerStatus { IDLE, RUNNING, PAUSED, STOPPED }

class TimerService : Service() {

    companion object {
        private val _timerState = MutableStateFlow(TimerState())
        val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

        private const val CHANNEL_ID = "karma_timer"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_NAME = "计时器"

        fun start(context: Context, score: Float, event: String) {
            val intent = Intent(context, TimerService::class.java).apply {
                putExtra("score", score)
                putExtra("event", event)
                action = "START"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = "PAUSE" }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = "RESUME" }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply { action = "STOP" }
            context.startService(intent)
        }

        /** 格式化毫秒为 HH:MM:SS */
        fun formatTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null
    private var notificationJob: Job? = null
    /** 缓存的 PendingIntent，避免 buildNotification() 每次调用都 new Intent + PendingIntent */
    private val cachedPendingIntent: PendingIntent by lazy {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START" -> {
                val score = intent.getFloatExtra("score", 0f)
                val event = intent.getStringExtra("event") ?: ""
                startTiming(score, event)
            }
            "PAUSE" -> pauseTiming()
            "RESUME" -> resumeTiming()
            "STOP" -> stopTiming()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTiming(score: Float, event: String) {
        val now = SystemClock.elapsedRealtime()
        _timerState.value = TimerState(
            status = TimerStatus.RUNNING,
            startElapsed = now,
            resumeElapsed = now,
            accumulatedMs = 0L,
            selectedScore = score,
            selectedEvent = event,
        )
        startForeground(NOTIFICATION_ID, buildNotification())
        startTick()
    }

    private fun pauseTiming() {
        val state = _timerState.value
        if (state.status != TimerStatus.RUNNING) return
        val elapsed = state.currentElapsedMs()
        _timerState.value = state.copy(
            status = TimerStatus.PAUSED,
            accumulatedMs = elapsed,
        )
        tickJob?.cancel()
        updateNotification()
    }

    private fun resumeTiming() {
        val state = _timerState.value
        if (state.status != TimerStatus.PAUSED) return
        _timerState.value = state.copy(
            status = TimerStatus.RUNNING,
            resumeElapsed = SystemClock.elapsedRealtime(),
        )
        startTick()
    }

    private fun stopTiming() {
        val state = _timerState.value
        val elapsed = state.currentElapsedMs()
        _timerState.value = state.copy(
            status = TimerStatus.STOPPED,
            accumulatedMs = elapsed,
        )
        tickJob?.cancel()
        notificationJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTick() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(200)
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val state = _timerState.value
        val elapsedMs = state.currentElapsedMs()
        val timeText = formatTime(elapsedMs)
        val statusText = when (state.status) {
            TimerStatus.RUNNING -> "计时中"
            TimerStatus.PAUSED -> "已暂停"
            else -> ""
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Karma 计时")
            .setContentText("$timeText  $statusText")
            .setSubText("事件：${state.selectedEvent}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(cachedPendingIntent)
            .setOngoing(state.status == TimerStatus.RUNNING || state.status == TimerStatus.PAUSED)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "计时器通知"
                setShowBadge(false)
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        tickJob?.cancel()
        notificationJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
