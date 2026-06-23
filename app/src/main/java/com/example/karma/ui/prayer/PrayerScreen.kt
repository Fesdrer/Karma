package com.example.karma.ui.prayer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.karma.di.AppContainer
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.ScoreBtnBg

@Composable
fun PrayerScreen(
    appContainer: AppContainer,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val viewModel: PrayerViewModel = viewModel(
        factory = PrayerViewModel.Factory(appContainer.repository)
    )
    val state by viewModel.uiState.collectAsState()

    // Show error as toast
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
    ) {
        // Particle canvas (full screen, no pointer events)
        if (state.isAnimating) {
            ParticleEngineCanvas(
                amount = state.amount.toFloatOrNull() ?: 0f,
                purpose = state.purpose,
                rankLevel = state.rankLevel,
                totalRanks = state.totalRanks,
                onComplete = {
                    onComplete()
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Prayer form (centered)
        if (state.showForm) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF16213e))
                        .padding(32.dp)
                        .fillMaxWidth(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "🙏 祈福",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFffd700),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "诚心祈福，扣减业力值",
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                    )
                    Spacer(Modifier.height(20.dp))

                    // Amount input
                    Text(
                        text = "扣减分数",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = { viewModel.onAmountChanged(it) },
                        placeholder = { Text("输入正数...", color = Color(0xFF666666)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        textStyle = TextStyle(color = Color(0xFFe0e0e0), fontSize = 16.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFffd700),
                            unfocusedBorderColor = BorderSubtle,
                            cursorColor = Color(0xFFffd700),
                            focusedContainerColor = ScoreBtnBg,
                            unfocusedContainerColor = ScoreBtnBg,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))

                    // Purpose input with bracket decoration
                    Text(
                        text = "祈福目的",
                        fontSize = 13.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    // 括号对角布局：「左上角 」右下角
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .background(ScoreBtnBg),
                    ) {
                        Text(
                            text = "「",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFff0000),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(start = 8.dp, top = 4.dp),
                        )
                        OutlinedTextField(
                            value = state.purpose,
                            onValueChange = { viewModel.onPurposeChanged(it) },
                            placeholder = { Text("输入祈福内容...", color = Color(0xFF666666)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                            textStyle = TextStyle(color = Color(0xFFff0000), fontSize = 16.sp),
                            minLines = 1,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = Color(0xFFff0000),
                                focusedTextColor = Color(0xFFff0000),
                                unfocusedTextColor = Color(0xFFff0000),
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                        )
                        Text(
                            text = "」",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFff0000),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 8.dp, bottom = 4.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Button(
                            onClick = onComplete,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF333333),
                                contentColor = Color(0xFF888888),
                            ),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text("取消", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { viewModel.confirmPrayer() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFb8860b),
                                contentColor = Color.White,
                            ),
                            modifier = Modifier.height(44.dp),
                        ) {
                            Text("确认祈福", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
