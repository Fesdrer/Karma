package com.example.karma.ui.divination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.karma.ui.divination.model.HexagramLibrary
import com.example.karma.ui.divination.model.HexagramLine
import com.example.karma.ui.divination.model.IntegrationEngine
import com.example.karma.ui.divination.model.YarrowResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class YarrowPhase {
    WAITING,            // 开始前等待按钮点击
    INTRO, IDLE,
    SPLITTING,          // a[1..num]左移, a[num+1..n]右移
    HANGING_ONE,        // a[num+1]缩短下移
    GROUP_LEFT,         // 左堆每4根一组右移
    COLLECT_LEFT,       // 左余缩短移到挂一左边
    GROUP_RIGHT,        // 右堆每4根一组左移
    COLLECT_RIGHT,      // 右余缩短移到挂一右边
    STORING,            // 归奇堆伸长下移（b空→最左, b不空→贴b右侧）
    FLASH_GROUPS,       // 每组4根闪出扩散型红光
    MERGING,            // T<=2: a合并居中
    LINE_END,           // T=3: 画爻, b→a, 49居中
    COMPLETE,
}

data class YarrowUiState(
    val n: Int = 49,                             // a.size()
    val num: Int = 0,                            // 分界点
    val ln: Int = 0,                             // 左余
    val rn: Int = 0,                             // 右余
    val bSize: Int = 0,                          // b.size()
    val phase: YarrowPhase = YarrowPhase.WAITING,
    val changeNumber: Int = 1,                   // T (1..18)
    val lines: List<HexagramLine> = emptyList(),
    val showResult: Boolean = false,
    val result: YarrowResult? = null,
)

class YarrowViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(YarrowUiState())
    val uiState: StateFlow<YarrowUiState> = _uiState.asStateFlow()

    // 用户点击 → 加载卦库 → 开始SPLITTING
    fun startDivination() {
        HexagramLibrary.ensureLoaded()
        _uiState.update { if (it.phase == YarrowPhase.WAITING) it.copy(phase = YarrowPhase.INTRO) else it }
    }

    fun onSplitTap(num: Int) {
        val s = _uiState.value
        if (s.phase != YarrowPhase.IDLE) return
        val n = s.n
        // ln=num%4; if(ln==0) ln=4;
        val ln0 = num % 4; val ln = if (ln0 == 0) 4 else ln0
        // rn=(n-num-1)%4; if(rn==0) rn=4;
        val rn0 = (n - num - 1) % 4; val rn = if (rn0 == 0) 4 else rn0
        _uiState.update { it.copy(num = num, ln = ln, rn = rn, phase = YarrowPhase.SPLITTING) }
    }

    fun advancePhase() {
        _uiState.update { s ->
            when (s.phase) {
                YarrowPhase.INTRO -> s.copy(phase = YarrowPhase.IDLE)
                YarrowPhase.SPLITTING -> s.copy(phase = YarrowPhase.HANGING_ONE)
                YarrowPhase.HANGING_ONE -> s.copy(phase = YarrowPhase.GROUP_LEFT)
                YarrowPhase.GROUP_LEFT -> s.copy(phase = YarrowPhase.COLLECT_LEFT)
                YarrowPhase.COLLECT_LEFT -> s.copy(phase = YarrowPhase.GROUP_RIGHT)
                YarrowPhase.GROUP_RIGHT -> s.copy(phase = YarrowPhase.COLLECT_RIGHT)
                YarrowPhase.COLLECT_RIGHT -> s.copy(phase = YarrowPhase.STORING)
                YarrowPhase.STORING -> {
                    val T = s.changeNumber % 3
                    if (T == 0) s.copy(phase = YarrowPhase.FLASH_GROUPS)
                    else {
                        val collected = s.ln + 1 + s.rn
                        s.copy(n = s.n - collected, bSize = s.bSize + collected, phase = YarrowPhase.MERGING)
                        // 注意：保留num/ln/rn，MERGING需要它们计算分组位置
                    }
                }
                YarrowPhase.FLASH_GROUPS -> {
                    // x = (num-ln)/4 + (n-num-1-rn)/4  // 数出来的组数=6,7,8,9
                    val x = (s.num - s.ln) / 4 + (s.n - s.num - 1 - s.rn) / 4
                    val line = HexagramLine(value = x, isYang = x % 2 != 0, isChanging = x == 6 || x == 9)
                    val newLines = s.lines + line
                    if (newLines.size >= 6) {
                        // 计算卦象启示
                        val primary = HexagramLibrary.fromHexagramLines(newLines)
                        val movingIndices = newLines.mapIndexedNotNull { idx, ln ->
                            if (ln.isChanging) idx + 1 else null  // 1-based: 1=初爻
                        }
                        // 变卦：动爻阴阳翻转
                        val transformedLines = newLines.map { ln ->
                            if (ln.isChanging) HexagramLine(
                                value = if (ln.value == 6) 8 else 7,  // 老阴变少阴，老阳变少阳
                                isYang = !ln.isYang,
                                isChanging = false
                            ) else ln
                        }
                        val transformed = HexagramLibrary.fromHexagramLines(transformedLines)
                        val integration = IntegrationEngine.calculate(primary, transformed)
                        val result = YarrowResult(
                            lines = newLines,
                            primaryHexagram = primary,
                            transformedHexagram = transformed,
                            movingLines = movingIndices,
                            integration = integration,
                        )
                        s.copy(lines = newLines, phase = YarrowPhase.COMPLETE,
                            showResult = true, result = result)
                    } else {
                        s.copy(lines = newLines, phase = YarrowPhase.LINE_END, bSize = 0)
                    }
                }
                YarrowPhase.MERGING -> {
                    s.copy(phase = YarrowPhase.IDLE, changeNumber = s.changeNumber + 1)
                }
                YarrowPhase.LINE_END -> {
                    // b中所有元素移动到a中，49个排列在中间
                    s.copy(n = 49, num = 0, ln = 0, rn = 0, bSize = 0,
                        phase = YarrowPhase.IDLE, changeNumber = s.changeNumber + 1)
                }
                else -> s
            }
        }
    }

    fun reset() {
        HexagramLibrary.release()
        _uiState.value = YarrowUiState(phase = YarrowPhase.WAITING)
    }

    override fun onCleared() {
        super.onCleared()
        HexagramLibrary.release()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = YarrowViewModel() as T
    }
}
