package com.example.karma.ui.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.karma.KarmaApplication
import com.example.karma.MainActivity
import com.example.karma.R
import com.example.karma.data.model.TimerState
import com.example.karma.data.model.TimerStatus
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

        /**
         * 从 Room 恢复计时状态。
         * 如果 timer 之前在 RUNNING 状态，回填离线经过的时间。
         * v4.3：改用墙钟时间（System.currentTimeMillis），设备重启后仍连续。
         */
        fun restoreTimerState(saved: TimerState, currentWallMs: Long) {
            var restored = saved
            if (saved.status == TimerStatus.RUNNING) {
                // 回填离线时间（含设备重启场景）
                val offlineMs = currentWallMs - saved.resumeElapsed
                if (offlineMs > 0 && offlineMs < 365L * 24 * 60 * 60 * 1000L) {
                    restored = saved.copy(accumulatedMs = saved.accumulatedMs + offlineMs)
                }
                restored = restored.copy(resumeElapsed = currentWallMs)
            }
            _timerState.value = restored
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
            "RESTORE" -> {
                // 恢复计时：不重置状态，只启动前台通知 + 计时 tick
                if (_timerState.value.status == TimerStatus.RUNNING) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                    startTick()
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startTiming(score: Float, event: String) {
        if (_timerState.value.status == TimerStatus.RUNNING || _timerState.value.status == TimerStatus.PAUSED) return
        val now = System.currentTimeMillis()   // v4.3：墙钟时间戳
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
            resumeElapsed = System.currentTimeMillis(),   // v4.3：墙钟时间戳
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

    // ===== 持久化 =====
    // 数据库的读写移到 UI 层（MainScreen）通过 ViewModel scope 执行，
    // 避免 Service 生命周期（stopSelf/onDestroy → scope.cancel）导致写入被取消。

    /** 将当前计时状态写入 Room，支持跨进程存活 */
    fun saveToRoom() {
        val app = application as? KarmaApplication ?: return
        val state = _timerState.value
        kotlinx.coroutines.runBlocking {
            app.container.repository.saveTimerState(
                status = state.status.name,
                startElapsed = state.startElapsed,
                resumeElapsed = state.resumeElapsed,
                accumulatedMs = state.accumulatedMs,
                selectedScore = state.selectedScore,
                selectedEvent = state.selectedEvent,
            )
        }
    }

    /** 清除 Room 中的计时状态（停止时调用） */
    fun clearFromRoom() {
        val app = application as? KarmaApplication ?: return
        kotlinx.coroutines.runBlocking {
            app.container.repository.clearTimerState()
        }
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
