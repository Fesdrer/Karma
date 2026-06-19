# 步骤 05：EventPanel 重写为三段式

## 目标

将右侧事件栏从扁平列表改为三个区段（善业/恶业/善果）紧邻排列的可滚动列表。

## 文件

`ui/main/components/EventPanel.kt`

## 核心设计

- 不再使用 `LazyColumn`，改用 `Column` + `verticalScroll`，使三个区段在一个滚动容器中
- 自定义输入框固定在底部，不随区段滚动
- 区段标题使用对应的语义颜色

## 新布局结构

```
┌─────────────────────────────┐
│  事件（金色标题）             │
│                             │
│  ── 善业 ──                  │  ← 绿色
│  帮助他人  □                  │
│  早起早睡  □                  │
│  锻炼身体  ■  ← 选中状态      │
│  日行一善  □                  │
│  孝敬父母  □                  │
│                             │
│  ── 恶业 ──                  │  ← 红色
│  发脾气    □                  │
│  浪费粮食  □                  │
│  ...                         │
│                             │
│  ── 善果 ──                  │  ← 金色
│  (暂无预设事件)               │
│                             │
├─────────────────────────────┤
│  [自定义事件输入框]           │  ← 固定底部
└─────────────────────────────┘
```

## 新函数签名

```kotlin
@Composable
fun EventPanel(
    goodDeedPresets: List<String>,
    badDeedPresets: List<String>,
    goodResultPresets: List<String>,
    selectedEvent: String?,
    onEventSelected: (String) -> Unit,
    onCustomEventChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
)
```

## 完整实现

```kotlin
@Composable
fun EventPanel(
    goodDeedPresets: List<String>,
    badDeedPresets: List<String>,
    goodResultPresets: List<String>,
    selectedEvent: String?,
    onEventSelected: (String) -> Unit,
    onCustomEventChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
    ) {
        // Title
        Text(
            text = "事件",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFffd700),
        )

        Spacer(Modifier.height(6.dp))

        // Scrollable event sections
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // — 善业 —
            EventSection(
                title = "善业",
                titleColor = Color(0xFF69f0ae),
                events = goodDeedPresets,
                selectedEvent = selectedEvent,
                onEventSelected = onEventSelected,
            )

            // — 恶业 —
            EventSection(
                title = "恶业",
                titleColor = Color(0xFFff5252),
                events = badDeedPresets,
                selectedEvent = selectedEvent,
                onEventSelected = onEventSelected,
            )

            // — 善果 —
            EventSection(
                title = "善果",
                titleColor = Color(0xFFffd700),
                events = goodResultPresets.ifEmpty { listOf("（暂无预设事件）") },
                selectedEvent = selectedEvent,
                onEventSelected = { event ->
                    // 善果目前只有提示文字，不允许选中
                },
            )
        }

        Spacer(Modifier.height(6.dp))

        // Custom event input (fixed at bottom)
        var customText by remember { mutableStateOf("") }
        OutlinedTextField(
            value = customText,
            onValueChange = {
                customText = it
                onCustomEventChanged(it)
            },
            placeholder = { Text("自定义事件...", fontSize = 14.sp, color = Color(0xFF666666)) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFffd700),
                unfocusedBorderColor = BorderSubtle,
                cursorColor = Color(0xFFffd700),
                focusedContainerColor = ScoreBtnBg,
                unfocusedContainerColor = ScoreBtnBg,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EventSection(
    title: String,
    titleColor: Color,
    events: List<String>,
    selectedEvent: String?,
    onEventSelected: (String) -> Unit,
) {
    Column {
        // Section title
        Text(
            text = "── $title ──",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            modifier = Modifier.padding(vertical = 2.dp),
        )

        // Section items
        events.forEach { event ->
            val isSelected = selectedEvent == event
            val canSelect = !event.startsWith("（") // 不能选中提示文字

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .then(
                        if (isSelected) {
                            Modifier.border(2.dp, Color(0xFFffd700), RoundedCornerShape(7.dp))
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        when {
                            isSelected -> Color(0xFFffd700).copy(alpha = 0.1f)
                            title == "善业" -> Color(0xFF69f0ae).copy(alpha = 0.08f)
                            title == "恶业" -> Color(0xFFff5252).copy(alpha = 0.08f)
                            else -> ScoreBtnBg
                        }
                    )
                    .padding(horizontal = 8.dp)
                    .defaultMinSize(minHeight = 13.dp)
                    .fillMaxWidth()
                    .then(
                        if (canSelect) {
                            Modifier.clickable { onEventSelected(event) }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = event,
                    fontSize = 12.sp,
                    color = when {
                        isSelected -> Color(0xFFffd700)
                        !canSelect -> Color(0xFF666666)
                        else -> Color(0xFFa0c4ff)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
```

## 关键点

- `verticalScroll` 配合 `weight(1f)` 使三个区段占满可用空间并一起滚动
- `EventSection` 提取为辅助 composable，三个区段共享同一渲染逻辑
- 善果区段目前无预设事件时显示"（暂无预设事件）"灰色提示，不可点击选中
- 注意需要新增 import：`rememberScrollState`
- 善业/恶业/善果的不可点击提示文字需要用括号包围（例如"（暂无预设事件）"），这样 `!canSelect` 逻辑会禁止选中
