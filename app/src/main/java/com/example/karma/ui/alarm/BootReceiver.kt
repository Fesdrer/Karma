package com.example.karma.ui.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.karma.KarmaApplication
import kotlinx.coroutines.runBlocking

/**
 * 设备重启后恢复闹钟：重新注册所有开启的闹钟（照手机闹钟）。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repository = (context.applicationContext as? KarmaApplication)?.container?.repository ?: return
        runBlocking {
            repository.getEnabledAlarms().forEach { alarm ->
                AlarmScheduler.schedule(context, alarm)
            }
        }
    }
}
