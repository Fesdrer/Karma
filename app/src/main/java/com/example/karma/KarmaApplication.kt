package com.example.karma

import android.app.Application
import android.util.Log
import com.example.karma.di.AppContainer
import java.io.File

class KarmaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        // 捕获未处理异常并写入文件，便于开发阶段定位闪退原因
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashLog = File(filesDir, "crash_log.txt")
                crashLog.writeText(
                    "Time: ${System.currentTimeMillis()}\n" +
                    "Thread: ${thread.name}\n" +
                    Log.getStackTraceString(throwable)
                )
            } catch (_: Exception) {
                // 日志写入失败不影响原有崩溃处理
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate()
        container = AppContainer(this)
    }
}
