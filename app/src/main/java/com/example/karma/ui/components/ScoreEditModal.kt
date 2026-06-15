package com.example.karma.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.karma.ui.theme.BorderSubtle
import com.example.karma.ui.theme.ScoreBtnBg

@Composable
fun ScoreEditModal(
    initialPresets: List<Float>,
    onDismiss: () -> Unit,
    onSave: (List<Float>) -> Unit,
) {
    val presets = remember { mutableStateListOf<Float>().apply { addAll(initialPresets) } }
    var addText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16213e),
        titleContentColor = Color(0xFFffd700),
        textContentColor = Color(0xFFe0e0e0),
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("编辑预设分数", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                ) {
                    itemsIndexed(presets) { index, value ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Move up
                            TextButton(
                                onClick = {
                                    if (index > 0) {
                                        val temp = presets[index]
                                        presets[index] = presets[index - 1]
                                        presets[index - 1] = temp
                                    }
                                },
                                modifier = Modifier.width(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            ) { Text("▲", fontSize = 12.sp, color = Color(0xFF888888)) }

                            // Move down
                            TextButton(
                                onClick = {
                                    if (index < presets.size - 1) {
                                        val temp = presets[index]
                                        presets[index] = presets[index + 1]
                                        presets[index + 1] = temp
                                    }
                                },
                                modifier = Modifier.width(36.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            ) { Text("▼", fontSize = 12.sp, color = Color(0xFF888888)) }

                            Spacer(Modifier.width(8.dp))

                            // Editable value
                            var editText by remember(value) {
                                mutableStateOf(value.toString())
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                    .background(ScoreBtnBg, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = {
                                        editText = it
                                        val parsed = it.toFloatOrNull()
                                        if (parsed != null && parsed != 0f) {
                                            presets[index] = parsed
                                        }
                                    },
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = Color(0xFFe0e0e0),
                                        fontSize = 15.sp,
                                    ),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        cursorColor = Color(0xFFffd700),
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            // Delete
                            TextButton(
                                onClick = { presets.removeAt(index) },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
                            ) { Text("✕", fontSize = 16.sp, color = Color(0xFFff5252)) }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Add row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = addText,
                        onValueChange = { addText = it },
                        placeholder = { Text("输入新分数...", fontSize = 14.sp, color = Color(0xFF666666)) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = Color(0xFFe0e0e0),
                            fontSize = 14.sp,
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFffd700),
                            unfocusedBorderColor = BorderSubtle,
                            cursorColor = Color(0xFFffd700),
                            focusedContainerColor = ScoreBtnBg,
                            unfocusedContainerColor = ScoreBtnBg,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val v = addText.toFloatOrNull()
                            if (v != null && v != 0f) {
                                presets.add(v)
                                addText = ""
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4a90d9)),
                    ) {
                        Text("添加", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(presets.toList()) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4a90d9)),
            ) {
                Text("保存", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF888888))
            }
        },
    )
}

