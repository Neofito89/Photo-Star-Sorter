package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.OperationState
import com.example.ui.theme.CanonRed
import com.example.ui.theme.SuccessGreen
import com.example.util.AppLanguage

@Composable
fun OperationProgressDialog(
    progressState: OperationState.Progress
) {
    val progressTitle = if (progressState.isDelete) {
        stringResource(R.string.op_delete_progress_title)
    } else {
        stringResource(R.string.op_copy_progress_title)
    }

    AlertDialog(
        onDismissRequest = { /* No cancelable por toque externo para proteger la I/O */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Text(
                text = progressTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (progressState.totalItems > 0) {
                    LinearProgressIndicator(
                        progress = { progressState.percentage },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.op_progress_items, progressState.currentItem, progressState.totalItems),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(progressState.percentage * 100).toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }

                if (progressState.currentFileName.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.op_progress_file, progressState.currentFileName),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun DeleteConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = CanonRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_delete_title, count),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = stringResource(R.string.dialog_delete_message, count),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CanonRed),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(stringResource(R.string.dialog_delete_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun OperationSummaryDialog(
    summary: OperationState.Summary,
    onDismiss: () -> Unit
) {
    val isDelete = summary.actionType.equals("ELIMINAR", ignoreCase = true)
    val titleText = if (isDelete) {
        stringResource(R.string.op_summary_delete_title)
    } else {
        stringResource(R.string.op_summary_copy_title)
    }
    val actionText = if (isDelete) {
        stringResource(R.string.op_type_delete)
    } else {
        stringResource(R.string.op_type_copy)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (summary.failedCount == 0) Icons.Filled.CheckCircle else Icons.Filled.Info,
                contentDescription = null,
                tint = if (summary.failedCount == 0) SuccessGreen else CanonRed,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(text = titleText, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.op_summary_message, actionText),
                    fontSize = 14.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.op_summary_completed),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${summary.successCount}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.op_summary_failed),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${summary.failedCount}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.failedCount > 0) CanonRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (summary.failureMessages.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.op_summary_errors_title),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CanonRed
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    ) {
                        items(summary.failureMessages) { msg ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = CanonRed,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = msg,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("summary_dismiss_button")
            ) {
                Text(stringResource(R.string.dialog_ok))
            }
        }
    )
}

@Composable
fun SettingsDialog(
    currentFolderUri: String?,
    folderDisplayName: String?,
    currentAppLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onChangeFolderClick: () -> Unit,
    onClearCacheClick: () -> Unit,
    onDismiss: () -> Unit
) {
    var showLanguagePicker by remember { mutableStateOf(false) }

    if (showLanguagePicker) {
        LanguageSelectionDialog(
            currentLanguage = currentAppLanguage,
            onSelect = { lang ->
                onLanguageSelected(lang)
                showLanguagePicker = false
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Selector de Idioma de la aplicación
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.clickable { showLanguagePicker = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_app_language),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val currentLanguageLabel = if (currentAppLanguage == AppLanguage.SYSTEM) {
                                    stringResource(R.string.settings_system_default)
                                } else {
                                    currentAppLanguage.nativeName
                                }
                                Text(
                                    text = currentLanguageLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        TextButton(
                            onClick = { showLanguagePicker = true },
                            modifier = Modifier.testTag("open_language_picker_button")
                        ) {
                            Text(stringResource(R.string.action_settings))
                        }
                    }
                }

                // Tarjeta actual
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.settings_selected_folder),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = folderDisplayName ?: stringResource(R.string.settings_no_folder),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!currentFolderUri.isNullOrBlank()) {
                            Text(
                                text = currentFolderUri,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Botón cambiar carpeta
                Button(
                    onClick = {
                        onDismiss()
                        onChangeFolderClick()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("change_folder_button")
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_change_folder))
                }

                // Botón vaciar caché
                OutlinedButton(
                    onClick = onClearCacheClick,
                    modifier = Modifier.fillMaxWidth().testTag("clear_cache_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.settings_clear_cache))
                }

                // Explicación técnica Canon RAW
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = stringResource(R.string.canon_raw_title),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.canon_raw_explanation),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_settings_button")
            ) {
                Text(stringResource(R.string.settings_dialog_close))
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        AppLanguage.SYSTEM,
        AppLanguage.ENGLISH,
        AppLanguage.SPANISH,
        AppLanguage.ITALIAN,
        AppLanguage.GALICIAN,
        AppLanguage.GERMAN
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_app_language),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                languages.forEach { lang ->
                    val isSelected = currentLanguage == lang
                    val label = if (lang == AppLanguage.SYSTEM) {
                        stringResource(R.string.settings_system_default)
                    } else {
                        lang.nativeName
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(lang) }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .testTag("language_option_${lang.code}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(lang) },
                            modifier = Modifier.testTag("language_radio_${lang.code}")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_language_dialog_button")
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
