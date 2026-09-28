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
import com.example.karma.R

/**
 * 每日必做桌面组件（v4.3，集合组件）。
 *
 * 点某行圆圈 → 落到全透明的 [WidgetClickActivity]：
 * **先 vis=1 → 再加分 → 再判断是否到达新的阶位**；只有跨入更高的正阶位时才打开 App 弹被动自证窗。
 *
 * 为什么点击目标不是广播接收器：
 * 组件点击若要「条件性地」打开 App，广播接收器里 `startActivity()` 属于后台启动 Activity，
 * Android 10+ 起被限制（`onReceive` 返回后更无豁免），华为/Android 12+ 上会被直接拦截，
 * 表现为「分数加了、但自证窗不弹」。而「桌面通过 PendingIntent.getActivity 拉起 Activity」
 * 是系统允许的，所以让点击先落到中转 Activity，再由它在前台 `startActivity(MainActivity)`。
 */
class DailyMustDoWidgetProvider : AppWidgetProvider() {

    companion object {
        /** 组件行携带的事件名（fill-in intent 填充）。 */
        const val EXTRA_DEED_NAME = "com.example.karma.extra.WIDGET_DEED_NAME"
        /** 跨阶时附带：加分前 / 加分后阶位（供 App 补弹被动自证）。 */
        const val EXTRA_PROOF_FROM = "com.example.karma.extra.WIDGET_PROOF_FROM"
        const val EXTRA_PROOF_TO = "com.example.karma.extra.WIDGET_PROOF_TO"

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
        // 必须是 getActivity —— 目标 [WidgetClickActivity] 全透明无界面，负责写库并在跨阶时拉起 App。
        // fill-in intent 要求模板 PendingIntent 为 MUTABLE（Android 12+）。
        val mutableFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val template = PendingIntent.getActivity(
            context,
            0,
            Intent(context, WidgetClickActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag,
        )
        views.setPendingIntentTemplate(R.id.widget_list, template)

        return views
    }
}
