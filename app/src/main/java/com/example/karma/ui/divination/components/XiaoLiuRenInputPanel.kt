package com.example.karma.ui.divination.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.divination.InputMode
import com.example.karma.ui.divination.model.LunarCalendarHelper
import com.example.karma.ui.divination.model.ShiChen
import com.example.karma.ui.theme.BlueLight
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.PanelBg
import com.example.karma.ui.theme.ScoreBtnBg
import com.example.karma.ui.theme.TextMuted
import com.example.karma.ui.theme.TextPrimary
import com.example.karma.ui.theme.TextSecondary

/**
 * 小六壬输入面板
 * 无状态设计，所有状态由父组件（ViewModel）管理
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XiaoLiuRenInputPanel(
    inputMode: InputMode,
    month: Int,
    day: Int,
    shiChen: ShiChen,
    number1: String,
    number2: String,
    number3: String,
    onInputModeChanged: (InputMode) -> Unit,
    onMonthChanged: (Int) -> Unit,
    onDayChanged: (Int) -> Unit,
    onShiChenChanged: (ShiChen) -> Unit,
    onNumber1Changed: (String) -> Unit,
    onNumber2Changed: (String) -> Unit,
    onNumber3Changed: (String) -> Unit,
    onStartClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(20.dp))
                .background(PanelBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 标题
            Text(
                text = "🙏 小六壬占卜",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Gold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "诚心叩问，以决吉凶",
                fontSize = 14.sp,
                color = TextSecondary,
            )
            Spacer(Modifier.height(16.dp))

            // 模式切换 Toggle
            InputModeToggle(
                currentMode = inputMode,
                onModeChanged = onInputModeChanged,
                enabled = enabled,
            )
            Spacer(Modifier.height(16.dp))

            // 输入区域
            when (inputMode) {
                InputMode.TRADITIONAL -> TraditionalInputs(
                    month = month,
                    day = day,
                    shiChen = shiChen,
                    onMonthChanged = onMonthChanged,
                    onDayChanged = onDayChanged,
                    onShiChenChanged = onShiChenChanged,
                    enabled = enabled,
                )
                InputMode.ARBITRARY -> ArbitraryInputs(
                    number1 = number1,
                    number2 = number2,
                    number3 = number3,
                    onNumber1Changed = onNumber1Changed,
                    onNumber2Changed = onNumber2Changed,
                    onNumber3Changed = onNumber3Changed,
                    enabled = enabled,
                )
            }

            Spacer(Modifier.height(16.dp))

            // 开始按钮
            Button(
                onClick = onStartClick,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFb8860b),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFF333333),
                    disabledContentColor = Color(0xFF666666),
                ),
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(48.dp),
            ) {
                Text(
                    text = "开始推算",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ====== 模式切换 Toggle ======

@Composable
private fun InputModeToggle(
    currentMode: InputMode,
    onModeChanged: (InputMode) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ScoreBtnBg)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp)),
    ) {
        ToggleOption(
            text = "传统农历",
            selected = currentMode == InputMode.TRADITIONAL,
            onClick = { if (enabled) onModeChanged(InputMode.TRADITIONAL) },
            modifier = Modifier.weight(1f),
        )
        ToggleOption(
            text = "任意数字",
            selected = currentMode == InputMode.ARBITRARY,
            onClick = { if (enabled) onModeChanged(InputMode.ARBITRARY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ToggleOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) Color(0xFFb8860b) else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else TextMuted,
        )
    }
}

// ====== 传统输入 ======

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TraditionalInputs(
    month: Int,
    day: Int,
    shiChen: ShiChen,
    onMonthChanged: (Int) -> Unit,
    onDayChanged: (Int) -> Unit,
    onShiChenChanged: (ShiChen) -> Unit,
    enabled: Boolean,
) {
    // 月份选择
    var monthExpanded by remember { mutableStateOf(false) }
    TraditionalLabel("月份")
    Spacer(Modifier.height(4.dp))
    ExposedDropdownMenuBox(
        expanded = monthExpanded,
        onExpandedChange = { if (enabled) monthExpanded = it },
    ) {
        OutlinedTextField(
            value = "${LunarCalendarHelper.monthNames[month]}（$month）",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
            shape = RoundedCornerShape(10.dp),
            colors = dropdownFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = monthExpanded,
            onDismissRequest = { monthExpanded = false },
        ) {
            for (m in 1..12) {
                DropdownMenuItem(
                    text = { Text("${LunarCalendarHelper.monthNames[m]}（$m）") },
                    onClick = {
                        onMonthChanged(m)
                        monthExpanded = false
                    },
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))

    // 日期选择
    var dayExpanded by remember { mutableStateOf(false) }
    TraditionalLabel("日期")
    Spacer(Modifier.height(4.dp))
    ExposedDropdownMenuBox(
        expanded = dayExpanded,
        onExpandedChange = { if (enabled) dayExpanded = it },
    ) {
        OutlinedTextField(
            value = "${LunarCalendarHelper.dayNames[day]}（$day）",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded) },
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
            shape = RoundedCornerShape(10.dp),
            colors = dropdownFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = dayExpanded,
            onDismissRequest = { dayExpanded = false },
        ) {
            for (d in 1..30) {
                DropdownMenuItem(
                    text = { Text("${LunarCalendarHelper.dayNames[d]}（$d）") },
                    onClick = {
                        onDayChanged(d)
                        dayExpanded = false
                    },
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))

    // 时辰选择
    var scExpanded by remember { mutableStateOf(false) }
    TraditionalLabel("时辰")
    Spacer(Modifier.height(4.dp))
    ExposedDropdownMenuBox(
        expanded = scExpanded,
        onExpandedChange = { if (enabled) scExpanded = it },
    ) {
        OutlinedTextField(
            value = "${shiChen.label} ${shiChen.timeRange}",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scExpanded) },
            textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp),
            shape = RoundedCornerShape(10.dp),
            colors = dropdownFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = scExpanded,
            onDismissRequest = { scExpanded = false },
        ) {
            for (sc in ShiChen.entries) {
                DropdownMenuItem(
                    text = { Text("${sc.label} ${sc.timeRange}") },
                    onClick = {
                        onShiChenChanged(sc)
                        scExpanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TraditionalLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun dropdownFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Gold,
    unfocusedBorderColor = BorderSubtle,
    cursorColor = Gold,
    focusedContainerColor = ScoreBtnBg,
    unfocusedContainerColor = ScoreBtnBg,
    disabledContainerColor = ScoreBtnBg,
    disabledBorderColor = BorderSubtle,
    disabledTextColor = TextMuted,
)

// ====== 任意数字输入 ======

@Composable
private fun ArbitraryInputs(
    number1: String,
    number2: String,
    number3: String,
    onNumber1Changed: (String) -> Unit,
    onNumber2Changed: (String) -> Unit,
    onNumber3Changed: (String) -> Unit,
    enabled: Boolean,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        NumberField(
            value = number1,
            onValueChange = onNumber1Changed,
            label = "数字一",
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        NumberField(
            value = number2,
            onValueChange = onNumber2Changed,
            label = "数字二",
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        NumberField(
            value = number3,
            onValueChange = onNumber3Changed,
            label = "数字三",
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text("...", color = TextMuted) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        textStyle = TextStyle(color = TextPrimary, fontSize = 15.sp, textAlign = TextAlign.Center),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            unfocusedBorderColor = BorderSubtle,
            cursorColor = Gold,
            focusedContainerColor = ScoreBtnBg,
            unfocusedContainerColor = ScoreBtnBg,
            disabledContainerColor = ScoreBtnBg,
            disabledBorderColor = BorderSubtle,
            disabledTextColor = TextMuted,
            focusedLabelColor = BlueLight,
            unfocusedLabelColor = TextSecondary,
        ),
        modifier = modifier,
    )
}
