package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnErrorContainer
import com.example.ui.theme.SleekOnPurpleContainer
import com.example.ui.theme.SleekOnSurfaceVariant
import com.example.ui.theme.SleekOutlineVariant
import com.example.ui.theme.SleekPurple
import com.example.ui.theme.SleekPurpleContainer
import com.example.ui.theme.SleekSurfaceContainer

@Composable
fun ContextualBottomBar(
    selectedCount: Int,
    isRangeMode: Boolean,
    onToggleRangeMode: () -> Unit,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    selectedTotalSize: String? = null
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            color = SleekSurfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = SleekOutlineVariant, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Banner informativo si el modo rango está activo
                    if (isRangeMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SleekPurpleContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LinearScale,
                                contentDescription = null,
                                tint = SleekOnPurpleContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.range_mode_active_banner),
                                fontSize = 12.sp,
                                color = SleekOnPurpleContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Contador de seleccionados + botón deseleccionar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onClearSelection,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_selection_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.action_clear_selection),
                                    tint = SleekOnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = stringResource(R.string.selected_files_count, selectedCount),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPurple
                                )
                                Text(
                                    text = selectedTotalSize ?: stringResource(R.string.selected_files_count, selectedCount),
                                    fontSize = 10.sp,
                                    color = SleekOnSurfaceVariant
                                )
                            }
                        }

                        // Acciones contextuales estilizadas según Sleek Interface
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Acción Rango
                            SleekActionButton(
                                icon = Icons.Filled.LinearScale,
                                label = stringResource(R.string.action_range),
                                containerColor = if (isRangeMode) SleekPurple else SleekPurpleContainer,
                                iconColor = if (isRangeMode) Color.White else SleekOnPurpleContainer,
                                testTag = "toggle_range_mode_button",
                                onClick = onToggleRangeMode
                            )

                            // Acción Copiar
                            SleekActionButton(
                                icon = Icons.Filled.ContentCopy,
                                label = stringResource(R.string.action_copy),
                                containerColor = SleekPurpleContainer,
                                iconColor = SleekOnPurpleContainer,
                                testTag = "copy_action_button",
                                onClick = onCopyClick
                            )

                            // Acción Compartir
                            SleekActionButton(
                                icon = Icons.Filled.Share,
                                label = stringResource(R.string.action_share),
                                containerColor = SleekPurpleContainer,
                                iconColor = SleekOnPurpleContainer,
                                testTag = "share_action_button",
                                onClick = onShareClick
                            )

                            // Acción Borrar
                            SleekActionButton(
                                icon = Icons.Filled.Delete,
                                label = stringResource(R.string.action_delete),
                                containerColor = SleekErrorContainer,
                                iconColor = SleekOnErrorContainer,
                                testTag = "delete_action_button",
                                onClick = onDeleteClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleekActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    iconColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(containerColor)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
