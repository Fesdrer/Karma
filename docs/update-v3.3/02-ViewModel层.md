# 阶段 2：ViewModel 层 — DivinationViewModel.kt

## 目标

创建 `app/src/main/java/com/example/karma/ui/divination/DivinationViewModel.kt`，管理小六壬占卜的全部状态和推算逻辑。

## 文件路径

```
app/src/main/java/com/example/karma/ui/divination/DivinationViewModel.kt
```

---

## 详细设计

### 1. 枚举定义

```kotlin
// 输入模式
enum class InputMode { TRADITIONAL, ARBITRARY }

// 动画阶段
enum class AnimationPhase {
    IDLE,             // 等待输入
    COUNTING_MONTH,   // 金线数月步（0-1500ms）
    MONTH_PAUSE,      // 月步暂停（1500-2000ms）
    COUNTING_DAY,     // 金线数日步（2000-4000ms）
    DAY_PAUSE,        // 日步暂停（4000-4500ms）
    COUNTING_HOUR,    // 金线数时步（4500-6000ms）
    RESULT_GLOW,      // 结果高亮（6000-7000ms）
    COMPLETE,         // 动画完成
}
```

### 2. UI 状态数据类

```kotlin
data class DivinationUiState(
    // 输入
    val inputMode: InputMode = InputMode.TRADITIONAL,
    val month: Int = 1,              // 传统模式：农历月 1-12
    val day: Int = 1,                // 传统模式：农历日 1-30
    val shiChen: ShiChen = ShiChen.ZI, // 传统模式：时辰
    val number1: String = "",         // 任意数字模式
    val number2: String = "",
    val number3: String = "",
    // 动画
    val animationPhase: AnimationPhase = AnimationPhase.IDLE,
    val isAnimating: Boolean = false,
    // 推算结果
    val resultPalace: PalaceRevelation? = null,
    val fullPath: List<Int> = emptyList(), // 完整推算路径（宫索引序列，用于金线动画）
    // 错误
    val errorMessage: String? = null,
)
```

### 3. DivinationViewModel 类

```kotlin
class DivinationViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DivinationUiState())
    val uiState: StateFlow<DivinationUiState> = _uiState.asStateFlow()

    // ====== 输入更新方法 ======

    fun setInputMode(mode: InputMode) {
        _uiState.update { it.copy(inputMode = mode) }
    }

    fun setMonth(month: Int) {
        _uiState.update { it.copy(month = month.coerceIn(1, 12)) }
    }

    fun setDay(day: Int) {
        _uiState.update { it.copy(day = day.coerceIn(1, 30)) }
    }

    fun setShiChen(sc: ShiChen) {
        _uiState.update { it.copy(shiChen = sc) }
    }

    fun setNumber1(value: String) {
        _uiState.update { it.copy(number1 = value.filter { c -> c.isDigit() }) }
    }

    fun setNumber2(value: String) {
        _uiState.update { it.copy(number2 = value.filter { c -> c.isDigit() }) }
    }

    fun setNumber3(value: String) {
        _uiState.update { it.copy(number3 = value.filter { c -> c.isDigit() }) }
    }

    // ====== 核心推算 ======

    fun startDivination() {
        val state = _uiState.value
        val month: Int
        val day: Int
        val hour: Int

        when (state.inputMode) {
            InputMode.TRADITIONAL -> {
                month = state.month
                day = state.day
                hour = state.shiChen.index
            }
            InputMode.ARBITRARY -> {
                val n1 = state.number1.toIntOrNull()
                val n2 = state.number2.toIntOrNull()
                val n3 = state.number3.toIntOrNull()
                if (n1 == null || n2 == null || n3 == null || n1 <= 0 || n2 <= 0 || n3 <= 0) {
                    _uiState.update { it.copy(errorMessage = "请输入三个正整数") }
                    return
                }
                month = n1
                day = n2
                hour = n3
            }
        }

        val result = calculate(month, day, hour)
        _uiState.update {
            it.copy(
                resultPalace = result.finalPalace,
                fullPath = result.fullPath,
                animationPhase = AnimationPhase.COUNTING_MONTH,
                isAnimating = true,
                errorMessage = null,
            )
        }
    }

    // ====== 推算算法 ======

    /**
     * 小六壬核心算法
     *
     * 从大安(索引0)起正月，顺时针数至目标月
     * 从月落点起初一，顺时针数至目标日
     * 从日落点起子时，顺时针数至目标时辰
     *
     * @return CalculationResult 包含最终结果宫和完整推算路径
     */
    private fun calculate(month: Int, day: Int, hour: Int): CalculationResult {
        // 第1步：数月
        val afterMonth = (month - 1) % 6
        val monthPath = generatePath(0, month)   // 从大安(0)开始数 month 步

        // 第2步：数日
        val afterDay = (afterMonth + day - 1) % 6
        val dayPath = generatePath(afterMonth, day)

        // 第3步：数时
        val afterHour = (afterDay + hour - 1) % 6
        val hourPath = generatePath(afterDay, hour)

        return CalculationResult(
            finalPalace = XiaoLiuRenPalaces.getPalace(PalaceIndex.fromIndex(afterHour)),
            fullPath = monthPath + dayPath + hourPath,
            monthEnd = afterMonth,
            dayEnd = afterDay,
            hourEnd = afterHour,
        )
    }

    /**
     * 生成从 startIndex 开始、顺时针移动 steps 步的宫索引序列
     * 例如：startIndex=0, steps=3 → [0, 1, 2]（大安→留连→速喜）
     */
    private fun generatePath(startIndex: Int, steps: Int): List<Int> {
        if (steps <= 0) return listOf(startIndex)
        return (0 until steps).map { (startIndex + it) % 6 }
    }

    // ====== 动画回调 ======

    fun onPhaseComplete(phase: AnimationPhase) {
        _uiState.update {
            when (phase) {
                AnimationPhase.COUNTING_MONTH -> it.copy(animationPhase = AnimationPhase.MONTH_PAUSE)
                AnimationPhase.COUNTING_DAY -> it.copy(animationPhase = AnimationPhase.DAY_PAUSE)
                AnimationPhase.COUNTING_HOUR -> it.copy(animationPhase = AnimationPhase.RESULT_GLOW)
                AnimationPhase.RESULT_GLOW -> it.copy(animationPhase = AnimationPhase.COMPLETE, isAnimating = false)
                else -> it  // MONTH_PAUSE/DAY_PAUSE 由动画组件自行管理
            }
        }
    }

    // ====== 重置 ======

    fun reset() {
        _uiState.value = DivinationUiState()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ====== Factory ======

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DivinationViewModel() as T
        }
    }
}

/**
 * 推算结果
 */
data class CalculationResult(
    val finalPalace: PalaceRevelation,
    val fullPath: List<Int>,    // 完整路径（月步 + 日步 + 时步）
    val monthEnd: Int,
    val dayEnd: Int,
    val hourEnd: Int,
)
```

---

## 关键算法验证

| 输入 | 计算过程 | 预期结果 |
|------|---------|---------|
| 月=1, 日=1, 时=1 | (0+0)%6=0, (0+0)%6=0, (0+0)%6=0 | 大安 |
| 月=3, 日=5, 时=5 | (0+2)%6=2, (2+4)%6=0, (0+4)%6=4 | 小吉 |
| 月=1, 日=1, 时=1（任意数）| 同上 | 大安 |
| 月=12, 日=30, 时=12 | (0+11)%6=5, (5+29)%6=4, (4+11)%6=3 | 赤口 |

---

## 与现有代码的一致性

- ViewModel 不依赖 Repository（小六壬数据自包含）
- 使用 `MutableStateFlow` + `asStateFlow()` 模式（与 PrayerViewModel 一致）
- 使用 `update {}` 扩展函数进行原子状态更新
- Factory 模式（内部类 + ViewModelProvider.Factory）

---

## 验证检查点

- [ ] 编译通过（依赖 XiaoLiuRenData.kt）
- [ ] 传统模式：输入月=3,日=5,时=5 → 结果为小吉
- [ ] 任意模式：输入 1,1,1 → 结果为大安
- [ ] 空输入验证：全空或非数字 → errorMessage 不为 null
- [ ] fullPath 路径列表长度 = month + day + hour
- [ ] reset() 后状态恢复到初始值

---

🤖 Generated with [Claude Code](https://claude.com/claude-code)
