package com.example.karma.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import com.example.karma.KarmaApplication
import com.example.karma.MainActivity
import com.example.karma.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 每日必做桌面组件（v4.3，集合组件）。
 *
 * 点某行圆圈 → 本类 onReceive 直接写库（标记完成 + 按善业默认分加分 + 写历史），**不打开 App**。
 * 仅当本次加分**跨入更高的正阶位**时，才打开 App 弹被动自证窗——自证窗必须由 App UI 呈现。
 */
class DailyMustDoWidgetProvider : AppWidgetProvider() {

    companion object {
        /** 组件行携带的事件名（fill-in intent 填充）。 */
        const val EXTRA_DEED_NAME = "com.example.karma.extra.WIDGET_DEED_NAME"
        /** 跨阶时附带：加分前 / 加分后阶位（供 App 补弹被动自证）。 */
        const val EXTRA_PROOF_FROM = "com.example.karma.extra.WIDGET_PROOF_FROM"
        const val EXTRA_PROOF_TO = "com.example.karma.extra.WIDGET_PROOF_TO"
        /** 点圆圈广播动作（模板 PendingIntent 用）。 */
        const val ACTION_COMPLETE_DEED = "com.example.karma.action.WIDGET_COMPLETE_DEED"

        /**
         * 刷新所有组件实例的列表数据。
         * App 内每日必做 vis 发生变化（打分完成 / 衰减跨天重置 / 设置变更）后调用。
         * 无组件实例时直接返回，不做任何事。
         */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, DailyMustDoWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context))
        }
        // 首次添加后立刻拉一次列表数据
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
    }

    /**
     * 点圆圈：直接写库。
     * goAsync 让写库在 onReceive 返回后进行，避免阻塞主线程被判定超时。
     */
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_COMPLETE_DEED) return
        val deedName = intent.getStringExtra(EXTRA_DEED_NAME) ?: return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            try {
                // 写库（IO 线程）：返回非 null 表示本次加分跨入了更高的正阶位
                val crossing = withContext(Dispatchers.IO) {
                    (appContext as KarmaApplication).container.repository
                        .completeDailyMustDoFromWidget(deedName)
                }
                refreshAll(appContext)
                if (crossing != null) {
                    // 只有跨阶才打开 App（弹被动自证窗）；不跨阶则全程不打扰
                    appContext.startActivity(
                        Intent(appContext, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(EXTRA_PROOF_FROM, crossing.first)
                            .putExtra(EXTRA_PROOF_TO, crossing.second)
                    )
                }
            } catch (e: Exception) {
                Log.e("KarmaWidget", "组件点击处理失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildRemoteViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_daily_must_do)

        // 列表数据源：RemoteViewsService（必须带唯一 data，不同组件实例各自取数）
        val serviceIntent = Intent(context, DailyMustDoWidgetService::class.java).apply {
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        // 全部完成时显示占位文案
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)

        // 行的统一模板：真正的点击 Intent 由每行的 fill-in intent 补齐（携带 deed 名称），
        // 直接发广播给本 Provider，由 onReceive 处理（不拉起 App）。
        // fill-in intent 要求模板 PendingIntent 为 MUTABLE（Android 12+）。
        val mutableFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val template = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, DailyMustDoWidgetProvider::class.java).setAction(ACTION_COMPLETE_DEED),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
        )
        views.setPendingIntentTemplate(R.id.widget_list, template)

        return views
    }
}