package com.example.karma.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.ScoreBtnBg

@Composable
fun ScorePanel(
    scorePresets: List<Float>,
    selectedScore: Float?,
    onScoreSelected: (Float) -> Unit,
    onEditClick: () -> Unit,
    onCustomScoreChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "分数",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFffd700),
            )
            TextButton(
                onClick = onEditClick,
                modifier = Modifier.padding(0.dp),
            ) {
                Text("编辑", fontSize = 11.sp, color = Color(0xFFa0c4ff))
            }
        }

        // Score grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items(scorePresets.size) { index ->
                val val_ = scorePresets[index]
                val isSelected = selectedScore == val_
                val text = if (val_ > 0) "+$val_" else val_.toString()
                val textColor = if (val_ < 0) Color(0xFFff8a80) else Color(0xFFa0c4ff)

                TextButton(
                    onClick = { onScoreSelected(val_) },
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    2.dp,
                                    if (val_ < 0) Color(0xFFff5252) else Color(0xFFffd700),
                                    RoundedCornerShape(7.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            if (isSelected) {
                                if (val_ < 0) Color(0xFFff5252).copy(alpha = 0.1f)
                                else Color(0xFFffd700).copy(alpha = 0.1f)
                            } else {
                                ScoreBtnBg
                            }
                        )
                        .padding(vertical = 6.dp)
                        .fillMaxWidth()
                        .height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                ) {
                    Text(
                        text = text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            if (val_ < 0) Color(0xFFff5252) else Color(0xFFffd700)
                        } else {
                            textColor
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Custom input
        var customText by remember { mutableStateOf("") }
        OutlinedTextField(
            value = customText,
            onValueChange = {
                customText = it
                onCustomScoreChanged(it)
            },
            placeholder = { Text("自定义分数...", fontSize = 12.sp, color = Color(0xFF666666)) },
            textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
            singleLine = true,
            shape = RoundedCornerShape(7.dp),
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
