package com.example.karma.ui.widget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.karma.KarmaApplication
import com.example.karma.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 桌面组件「点圆圈」的中转 Activity（v4.3）。
 *
 * 职责只有一件事，顺序固定：
 *   ① vis = 1（标记该每日必做今天已完成）
 *   ② 加分（按善业默认分，并写一条历史）
 *   ③ 判断是否到达新的阶位
 *   ④ 只有到达新阶位时才打开 App，让 App 弹被动自证窗；否则直接结束，App 完全不出现
 *
 * 为什么需要这个中转 Activity：
 * 若把②③④放进广播接收器的 `onReceive` 里，第④步的 `startActivity()` 属于**后台启动 Activity**，
 * Android 10 起被系统限制（广播返回后更没有任何豁免），在华为/Android 12+ 上会被直接拦截，
 * 结果就是「分数加了、自证窗不弹」。而「桌面通过 PendingIntent.getActivity 拉起 Activity」
 * 是系统允许的，本 Activity 处于前台，再由它 `startActivity(MainActivity)` 必然允许。
 *
 * 本 Activity 全透明、不 setContentView、无窗口动画（见 Theme.Karma.WidgetClick）：
 * 不跨阶时它做完写库立刻 finish()，用户看不到任何东西（App 不出现是需求的一部分）。
 */
class WidgetClickActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 重建（旋屏 / 被系统回收后恢复）走到这里说明写库已由上一次实例执行过，直接退出防重复加分
        if (savedInstanceState != null) {
            finish()
            return
        }

        val deedName = intent?.getStringExtra(DailyMustDoWidgetProvider.EXTRA_DEED_NAME)
        // 立刻清空 extra，避免 Activity 重建时重复消费同一次点击
        intent?.removeExtra(DailyMustDoWidgetProvider.EXTRA_DEED_NAME)
        if (deedName == null) {
            finish()
            return
        }

        val repository = (application as KarmaApplication).container.repository
        lifecycleScope.launch {
            // NonCancellable：本 Activity 声明了 noHistory，连点两次圆圈时第一个实例会被系统提前结束；
            // 若让销毁取消协程，可能停在「vis=1 已写、加分还没写」中间态，这一分就永远丢了。
            // 写库一旦开始就必须跑完。
            withContext(NonCancellable) {
                try {
                    // ①②③ 的顺序在 Repository 内保证：先 vis=1，再加分，最后用写库后的总分判断新阶位
                    val crossing = withContext(Dispatchers.IO) {
                        repository.completeDailyMustDoFromWidget(deedName)
                    }
                    // 该行已完成，刷新组件列表让它从桌面上消失
                    DailyMustDoWidgetProvider.refreshAll(this@WidgetClickActivity)
                    // ④ 只有跨入更高正阶位才打开 App 弹被动自证窗；不跨阶则什么都不做
                    if (crossing != null) {
                        startActivity(
                            Intent(this@WidgetClickActivity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .putExtra(DailyMustDoWidgetProvider.EXTRA_PROOF_FROM, crossing.first)
                                .putExtra(DailyMustDoWidgetProvider.EXTRA_PROOF_TO, crossing.second)
                        )
                    }
                } finally {
                    // 不跨阶时到这里就是「什么都没发生」，透明窗口没有任何内容可看
                    finish()
                }
            }
        }
    }
}
