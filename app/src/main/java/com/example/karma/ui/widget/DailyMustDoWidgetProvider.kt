package com.example.karma.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.example.karma.MainActivity
import com.example.karma.R

/**
 * 每日必做桌面组件（v4.3，集合组件）。
 *
 * 组件本身**不写数据库**：仅列出未完成的每日必做。
 * 点某行圆圈 → 通过 PendingIntent 唤起 MainActivity（携带 deed 名称），
 * 由 App 内执行「标记完成 + 按善业默认分加分 + 写历史」，自证也由 App 内 checkProof 正常判定。
 * （组件进程无法弹自证窗，故一律走 App。）
 */
class DailyMustDoWidgetProvider : AppWidgetProvider() {

    companion object {
        /** 组件行携带的事件名 extra（fill-in intent 填充，由 MainActivity 读取）。 */
        const val EXTRA_DEED_NAME = "com.example.karma.extra.WIDGET_DEED_NAME"

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

    private fun buildRemoteViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_daily_must_do)

        // 列表数据源：RemoteViewsService（必须带唯一 data，不同组件实例各自取数）
        val serviceIntent = Intent(context, DailyMustDoWidgetService::class.java).apply {
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        // 全部完成时显示占位文案
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)

        // 行的统一模板：真正的点击 Intent 由每行的 fill-in intent 补齐（携带 deed 名称）。
        // fill-in intent 要求模板 PendingIntent 为 MUTABLE（Android 12+）。
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val template = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            flags,
        )
        views.setPendingIntentTemplate(R.id.widget_list, template)

        return views
    }
}