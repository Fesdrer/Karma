package com.example.karma.ui.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.karma.KarmaApplication
import com.example.karma.R
import com.example.karma.data.local.entity.DailyMustDoDeed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 每日必做组件的数据服务（集合组件要求）。
 * 数据来自 Room 中 karma_settings 行的 dailyMustDoDeeds，只取 vis == 0（未完成）的条目。
 */
class DailyMustDoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        DailyMustDoRemoteViewsFactory(applicationContext)
}

/**
 * 组件行工厂。
 * onDataSetChanged 在组件数据线程回调（非主线程），此处用 runBlocking 取一次 Room 快照；
 * 刷新由 App 内 notifyAppWidgetViewDataChanged 触发，不依赖系统定时轮询。
 */
class DailyMustDoRemoteViewsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    private var unfinishedDeeds: List<DailyMustDoDeed> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val repository = (context.applicationContext as KarmaApplication).container.repository
        unfinishedDeeds = runBlocking {
            repository.settings.first().dailyMustDoDeeds.filter { it.vis == 0 }
        }
    }

    override fun onDestroy() {
        unfinishedDeeds = emptyList()
    }

    override fun getCount(): Int = unfinishedDeeds.size

    override fun getViewAt(position: Int): RemoteViews? {
        val deed = unfinishedDeeds.getOrNull(position) ?: return null
        return RemoteViews(context.packageName, R.layout.widget_daily_must_do_item).apply {
            setTextViewText(R.id.widget_item_name, deed.name)
            // 点圆圈：携带 deed 名称；与 Provider 的模板 PendingIntent 合并后落到 WidgetClickActivity
            setOnClickFillInIntent(
                R.id.widget_item_circle,
                Intent().putExtra(DailyMustDoWidgetProvider.EXTRA_DEED_NAME, deed.name),
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false
}