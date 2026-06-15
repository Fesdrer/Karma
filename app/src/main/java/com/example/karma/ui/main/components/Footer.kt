package com.example.karma.ui.main.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.PanelBg

@Composable
fun Footer(
    confirmEnabled: Boolean,
    prayerEnabled: Boolean,
    onConfirm: () -> Unit,
    onPrayer: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Confirm button
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier
                .weight(1f)
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4a90d9),
                disabledContainerColor = Color(0xFF333333),
                contentColor = Color.White,
                disabledContentColor = Color(0xFF666666),
            ),
        ) {
            Text(
                text = "确认",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Prayer button
        Button(
            onClick = onPrayer,
            enabled = prayerEnabled,
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFb8860b),
                disabledContainerColor = Color(0xFF333333),
                contentColor = Color.White,
                disabledContentColor = Color(0xFF666666),
            ),
        ) {
            Text(
                text = "祈福",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // History button
        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier
                .weight(0.7f)
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFa0c4ff),
            ),
            border = BorderStroke(1.dp, Color(0xFF334444)),
        ) {
            Text(
                text = "历史",
                fontSize = 13.sp,
            )
        }

        // ⚙ Settings button
        OutlinedButton(
            onClick = onSettings,
            modifier = Modifier
                .weight(0.5f)
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFa0c4ff),
            ),
            border = BorderStroke(1.dp, Color(0xFF334444)),
        ) {
            Text(
                text = "⚙",
                fontSize = 16.sp,
            )
        }
    }
}
