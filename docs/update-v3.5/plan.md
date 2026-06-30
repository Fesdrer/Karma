# 运气增幅值 实现计划

## Context

在 Karma 安卓应用中新增"运气增幅值"功能。该值综合总业力(K)和近期行为波动(K')来量化业力对运气的影响。公式：

$$\text{运气} = \frac{K + c \int_0^T K' \cdot e^{-at^2} dt}{b}$$

核心计算原理：历史记录之间的 K 是折线变化，K' 分段恒定。积分退化为每段 K' 乘以该段的 ∫e^{-at²}dt，利用 erf 函数求解。

---

## 修改/新建文件清单

### 1. 新建 `util/LuckAmplifier.kt` — 核心计算

- **erf(x)**：Abramowitz & Stegun 7.1.26 近似（误差 < 1.5×10⁻⁷）
- **integralExpMinusAt2(a, from, to)**：∫_{from}^{to} e^{-at²} dt = (√π)/(2√a) · [erf(√a·to) - erf(√a·from)]
- **computeLuckAmplification(totalScore, historyEntries, T, b, W)**：
  1. 从 T、W 计算 a = 9/(2T²)、c = W / (10 · integralExpMinusAt2(a, 0, 1))
  2. 以最后一条记录时间为 t=0，算每条记录的 t = 距最后记录的天数
  3. 取 t ≤ T 的记录，按 t 升序排列
  4. 对每对相邻记录 (t_i, K_i) → (t_{i+1}, K_{i+1})，其中 t_i < t_{i+1}：
     - K' = (K_{i+1} - K_i) / (t_{i+1} - t_i)
     - 若 t_{i+1} > T，在 T 处截断：K_T = K_i + K' · (T - t_i)，段为 [t_i, T]
     - 贡献 = K' · integralExpMinusAt2(a, t_i, min(t_{i+1}, T))
  5. 总和 integral = Σ 贡献
  6. 返回 (totalScore + c * integral) / b，保留 2 位小数
- 仅一条记录或无记录时 integral = 0，直接返回 K/b

### 2. 修改 `data/local/entity/KarmaSettingsEntity.kt` — 新增字段

```kotlin
val luckEnabled: Boolean = false,   // 是否启用运气增幅
val luckT: Float = 7f,              // T（天）
val luckB: Float = 1f,              // b（普通好事分值）
val luckW: Float = 100f,            // W（对应总分）
```

### 3. 修改 `data/local/KarmaDatabase.kt` — 版本 7→8 迁移

```kotlin
private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE karma_settings ADD COLUMN luckEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE karma_settings ADD COLUMN luckT REAL NOT NULL DEFAULT 7.0")
        db.execSQL("ALTER TABLE karma_settings ADD COLUMN luckB REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE karma_settings ADD COLUMN luckW REAL NOT NULL DEFAULT 100.0")
    }
}
```

### 4. 修改 `data/repository/KarmaRepository.kt` — 新增计算方法

- 新增 `suspend fun computeLuckValue(): Float`：
  - 读取 settings 和全部 history entries
  - 若 `!luckEnabled` 返回 `totalScore / luckB`
  - 否则调用 `LuckAmplifier.computeLuckAmplification(...)`

### 5. 修改 `ui/main/MainViewModel.kt` — UI 状态新增 luckValue

- `MainUiState` 新增 `luckValue: Float? = null`（null 表示未启用/不显示）
- 新增 `_luckValue: MutableStateFlow<Float?>` 
- `uiState` combine 中调用 repository 计算（或在 viewModelScope 中单独 launch）
- 由于需要 history entries 参与计算，需要新增对 `repository.allHistory` 的监听

注意：运气值依赖 history entries + settings，需要 combine 两者。在 `uiState` 的 combine 中加入 `repository.allHistory`。

### 6. 修改 `ui/main/components/Header.kt` — 显示运气值

在总分和 rank 下方新增一行小字显示运气值：
```
运气增幅: +12.50
```
- 字体 13sp，颜色 TextMuted
- 仅当 `luckValue != null` 时显示
- 正值绿色、负值红色

### 7. 修改 `ui/settings/SettingsViewModel.kt` — 运气参数编辑

新增方法：
- `updateLuckEnabled(Boolean)`
- `updateLuckT(Float)`
- `updateLuckB(Float)`
- `updateLuckW(Float)`

### 8. 修改 `ui/settings/SettingsScreen.kt` — 新增运气设置卡片

新增 `LuckSettingsCard` Composable，放在 DecaySettingsCard 之后：
- 启用开关
- T 滑块（1~365 天）
- b 输入框（0.1~100）
- W 输入框（1~1000）
- 显示导出的 a、c 值（只读，灰色小字）

---

## 验证

1. `cd app && ./gradlew assembleDebug` 编译通过
2. 在设置中开启运气增幅，调整 T/b/W，回到主页观察运气值变化
3. 添加一条新记录后，运气值应随之更新
4. 关闭运气增幅后，主页不再显示运气值
