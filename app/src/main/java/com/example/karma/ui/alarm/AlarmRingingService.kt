package com.example.karma.ui.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.karma.KarmaApplication
import kotlinx.coroutines.runBlocking

/**
 * 闹钟响铃前台服务：播放铃声 + 震动 + 全屏通知（贪睡/关闭）。
 * 一直响直到用户贪睡或关闭（与系统闹钟一致）。
 */
class AlarmRingingService : Service() {

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AlarmNotifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> {
                val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
                startRinging(alarmId)
            }
            ACTION_STOP -> {
                stopRinging()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRinging(alarmId: Long) {
        stopRinging()   // 若已在响其他闹钟，先停
        val alarm = runBlocking {
            (application as? KarmaApplication)?.container?.repository?.getAlarmById(alarmId)
        }
        if (alarm == null) {
            // 闹钟已被删除：不响铃，直接结束（startForegroundService 后允许以 stopSelf 终止）
            stopSelf()
            return
        }

        // 前台通知（全屏 intent → 响铃页，带贪睡/关闭）
        startForeground(
            AlarmNotifications.NOTIFICATION_ID + alarmId.toInt(),
            AlarmNotifications.buildRingingNotification(this, alarm),
        )

        // 铃声：用户自选 URI，空则系统默认闹钟铃声（无则退化为通知音）
        val uri = alarm.ringtoneUri.ifEmpty {
            (RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).toString()
        }
        ringtone = RingtoneManager.getRingtone(this, Uri.parse(uri))?.apply { play() }

        // 震动：无限循环（每 1s 震 0.8s）
        if (alarm.vibrate) {
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 800), 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 800), 0)
            }
        }
    }

    private fun stopRinging() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val ACTION_RING = "com.example.karma.alarm.SERVICE_RING"
        const val ACTION_STOP = "com.example.karma.alarm.SERVICE_STOP"

        /** 停止响铃（不结束服务本身，由 dismiss/snooze 后续处理）。 */
        fun stopRinging(context: Context) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
