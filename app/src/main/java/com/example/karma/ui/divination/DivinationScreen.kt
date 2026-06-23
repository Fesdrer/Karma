package com.example.karma.ui.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

@Composable
fun DivinationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var result by remember { mutableStateOf<Int?>(null) }
    var backHandled by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // Back button - larger touch target
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 32.dp)
                .clickable {
                    if (!backHandled) {
                        backHandled = true
                        onBack()
                    }
                },
        ) {
            Text(
                text = "← 返回",
                fontSize = 18.sp,
                color = Color(0xFFa0c4ff),
                modifier = Modifier.padding(12.dp),
            )
        }

        // Center content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Result number
            Text(
                text = result?.toString() ?: "?",
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFffd700),
                textAlign = TextAlign.Center,
            )
        }

        // Bottom button
        Button(
            onClick = {
                var cnt = 0
                for (i in 1..1000) {
                    if (Random.nextInt(1, 1001) <= 490) {
                        cnt++
                    }
                }
                result = cnt
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4a90d9),
                contentColor = Color.White,
            ),
        ) {
            Text(
                text = if (result == null) "开始" else "再来一次",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
