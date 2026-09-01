package com.example.karma.ui.divination.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.karma.data.ai.AiAnalysisClient
import com.example.karma.data.ai.AiConfigStore
import com.example.karma.ui.theme.ChartBg
import com.example.karma.ui.theme.Gold
import com.example.karma.ui.theme.RedNegative
import com.example.karma.ui.theme.TextPrimary
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** AI 分析弹窗状态 */
private sealed interface AiAnalysisUiState {
    data object Hidden : AiAnalysisUiState
    data object Loading : AiAnalysisUiState
    data class Content(val text: String) : AiAnalysisUiState
    data class Error(val message: String) : AiAnalysisUiState
}

/**
 * 占卜结果页末尾的「AI 分析」按钮 + 结果弹窗。
 *
 * @param divinationType 占卜类型（"大衍筮法"/"小六壬"）
 * @param topic 求占事项（来自占卜输入面板）
 * @param resultSummary 占卜结果文本摘要（卦象/宫位等，由各结果面板拼装）
 */
@Composable
fun AiAnalysisSection(
    divinationType: String,
    topic: String,
    resultSummary: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uiState by remember { mutableStateOf<AiAnalysisUiState>(AiAnalysisUiState.Hidden) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun startAnalysis() {
        if (uiState == AiAnalysisUiState.Loading) return
        uiState = AiAnalysisUiState.Loading
        job = scope.launch {
            try {
                val text = AiAnalysisClient.analyze(context, divinationType, topic, resultSummary)
                uiState = AiAnalysisUiState.Content(text)
            } catch (e: Exception) {
                uiState = AiAnalysisUiState.Error(e.message ?: "AI 分析失败，请稍后重试")
            }
        }
    }

    Button(
        onClick = {
            if (!AiConfigStore(context.applicationContext).isConfigured()) {
                Toast.makeText(
                    context,
                    "请先在「设置 → AI 分析」中配置 API 地址与 Key",
                    Toast.LENGTH_LONG,
                ).show()
                return@Button
            }
            startAnalysis()
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1a2e1a),
            contentColor = Color(0xFF9dff9d),
        ),
        modifier = modifier.fillMaxWidth(0.6f).height(44.dp),
    ) {
        Text("🤖 AI 分析", fontFamily = FontFamily.Serif, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

    if (uiState != AiAnalysisUiState.Hidden) {
        AiAnalysisDialog(
            state = uiState,
            onDismiss = {
                job?.cancel()
                uiState = AiAnalysisUiState.Hidden
            },
        )
    }
}

/** AI 分析结果弹窗（独立窗口层，覆盖在结果面板之上） */
@Composable
private fun AiAnalysisDialog(
    state: AiAnalysisUiState,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC000000)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ChartBg)
                    .border(1.5.dp, Gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (state) {
                    is AiAnalysisUiState.Loading -> {
                        Spacer(Modifier.height(24.dp))
                        CircularProgressIndicator(color = Gold)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "🤖 AI 正在解读卦象，请稍候…",
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    is AiAnalysisUiState.Content -> {
                        Text(
                            text = "【 AI 分 析 】",
                            fontFamily = FontFamily.Serif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = state.text,
                            fontFamily = FontFamily.Serif,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            lineHeight = 24.sp,
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    is AiAnalysisUiState.Error -> {
                        Text(
                            text = "⚠️ AI 分析失败",
                            fontFamily = FontFamily.Serif,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedNegative,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = state.message,
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    AiAnalysisUiState.Hidden -> Unit
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFb8860b),
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.height(42.dp),
                ) {
                    Text("关闭", fontFamily = FontFamily.Serif, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
