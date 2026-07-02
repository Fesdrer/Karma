# 后续修改（图片背景起）

## 1. 全局背景装饰图

`docs/update-v3.6/image.png` → `drawable-nodpi/theme_bg.png`，`Theme.kt` 渐变层和内容之间插入该图片，`statusBarsPadding()` + `FillBounds` 使顶端贴状态栏下边、底端贴屏幕底。

## 2. 计时页按钮

暂停/继续和停止按钮：字体改衬线体；暂停/继续底色由蓝色 `#4a90d9` 改暗金 `#b8860b`，停止底色由红色 `#d32f2f` 改深灰 `#333333`，文字改灰 `#888888`。

## 3. 善果事件选中效果

`EventSection` 中善果未选中项的 item 背景从通用 `ScoreBtnBg` 改为 `Gold.copy(alpha=0.08)`（淡黄底），与善业（淡绿）、恶业（淡红）对称。

## 4. 占卜结果页背景

小六壬和大衍筮法结果面板外层遮罩从半透明黑改为透明，让主题渐变透出，启示框内保持纯黑不变。

## 5. 阶位加减按钮颜色

`+`/`−` 文字从蓝色 `#a0c4ff` 改金色，`−` 按钮激活态底色从蓝色 `#4a90d9` 改暗金 `#b8860b`。

## 6. 时间选择器数字居中

`ScrollPicker` 每项从 Text 改 Box 包裹 + `Alignment.Center`，使数字水平和垂直都居中。

## 7. 时间选择器吸附

根源：`LaunchedEffect(isScrollInProgress)` 的 key 绑定导致 `animateScrollToItem` 开始后 key 变化、协程被杀、动画中断。修复：改用 `LaunchedEffect(Unit)` + `snapshotFlow` 保持协程存活，吸附完整执行。配合 `animateScrollToItem(closest - 2)` 使目标落到第 3 项（5 项视口的中心）。

## 8. 历史页返回按钮变窄

水平内边距从 16dp 减到 10dp。

## 9. JSON 导出/导入修复

`importJson()` 原先 `Map` 中间步骤把所有数字变 `Double`，再序列化回 JSON 时 Long/Int 类型不匹配。改为 `JsonParser.parseString()` 直接解析 `JsonObject`，保留原始类型。
