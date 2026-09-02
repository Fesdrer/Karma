package com.example.karma

import android.app.Application
import android.util.Log
import com.example.karma.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class KarmaApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

        // 全新安装/数据被清后：确保默认设置行存在（否则 updateTotalScore 更新 0 行，总分永远无法累计）
        applicationScope.launch {
            try {
                container.repository.ensureSettingsRow()
            } catch (e: Exception) {
                Log.e("KarmaInit", "初始化默认设置行失败", e)
            }
        }

        // 应用启动时检查业力衰减
        applicationScope.launch {
            try {
                val deducted = container.repository.applyDecay()
                if (deducted > 0f) {
                    Log.i("KarmaDecay", "业力衰减：本次共扣除 ${String.format("%.1f", deducted)} 分")
                }
            } catch (e: Exception) {
                Log.e("KarmaDecay", "衰减检查失败", e)
            }
        }
    }
}
