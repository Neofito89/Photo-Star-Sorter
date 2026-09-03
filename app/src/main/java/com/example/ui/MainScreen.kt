package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.OperationState
import com.example.data.model.PhotoItem
import com.example.ui.components.ContextualBottomBar
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FilterBar
import com.example.ui.components.OperationProgressDialog
import com.example.ui.components.OperationSummaryDialog
import com.example.ui.components.PhotoTableView
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.SleekOutlineVariant
import com.example.ui.theme.SleekPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Launcher para seleccionar la tarjeta SD / carpeta origen (ACTION_OPEN_DOCUMENT_TREE)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        }
    }

    // Launcher para seleccionar carpeta de destino para copiar
    val copyDestinationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { destUri: Uri? ->
        if (destUri != null) {
            viewModel.executeCopySelected(destUri)
        }
    }

    // Si no hay carpeta seleccionada en el lanzamiento, abrir automáticamente el selector SAF
    LaunchedEffect(Unit) {
        if (uiState.currentTreeUri == null) {
            folderPickerLauncher.launch(null)
        }
    }

    var showColumnsMenu by remember { mutableStateOf(false) }

    val selectedBytes = remember(uiState.selectedUris, uiState.allPhotos) {
        uiState.allPhotos.filter { it.uriString in uiState.selectedUris }.sumOf { it.fileSize }
    }
    val formattedBytes = if (uiState.selectedUris.isNotEmpty()) {
        android.text.format.Formatter.formatFileSize(context, selectedBytes)
    } else ""
    val selectedTotalSize = if (uiState.selectedUris.isNotEmpty()) {
        stringResource(R.string.selected_total_size, formattedBytes)
    } else null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(19.dp))
                                    .background(SleekPurple)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = uiState.folderDisplayName ?: stringResource(R.string.app_subtitle_connect_sd),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        // Botón para seleccionar o cambiar tarjeta SD
                        IconButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            modifier = Modifier.testTag("select_folder_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderOpen,
                                contentDescription = stringResource(R.string.action_choose_sd),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Botón refrescar / re-escanear
                        IconButton(
                            onClick = { viewModel.rescan() },
                            enabled = uiState.currentTreeUri != null && !uiState.isScanning,
                            modifier = Modifier.testTag("refresh_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.action_rescan),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Menú de Columnas adicionales
                        Box {
                            IconButton(
                                onClick = { showColumnsMenu = true },
                                modifier = Modifier.testTag("columns_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ViewColumn,
                                    contentDescription = stringResource(R.string.action_columns_menu),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            DropdownMenu(
                                expanded = showColumnsMenu,
                                onDismissRequest = { showColumnsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.col_capture_date)) },
                                    trailingIcon = {
                                        if (uiState.columnVisibility.showDate) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        viewModel.toggleColumnVisibility(showDate = !uiState.columnVisibility.showDate)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.col_size)) },
                                    trailingIcon = {
                                        if (uiState.columnVisibility.showSize) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        viewModel.toggleColumnVisibility(showSize = !uiState.columnVisibility.showSize)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.col_type)) },
                                    trailingIcon = {
                                        if (uiState.columnVisibility.showType) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        viewModel.toggleColumnVisibility(showType = !uiState.columnVisibility.showType)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.col_relative_path)) },
                                    trailingIcon = {
                                        if (uiState.columnVisibility.showPath) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        viewModel.toggleColumnVisibility(showPath = !uiState.columnVisibility.showPath)
                                    }
                                )
                            }
                        }

                        // Botón ajustes
                        IconButton(
                            onClick = { viewModel.setSettingsOpen(true) },
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.action_settings),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                HorizontalDivider(color = SleekOutlineVariant, thickness = 1.dp)
            }
        },
        bottomBar = {
            ContextualBottomBar(
                selectedCount = uiState.selectedUris.size,
                isRangeMode = uiState.isRangeModeActive,
                onToggleRangeMode = { viewModel.toggleRangeMode() },
                onCopyClick = { copyDestinationLauncher.launch(null) },
                onShareClick = {
                    val selected = uiState.allPhotos.filter { uiState.selectedUris.contains(it.uriString) }
                    shareSelectedPhotos(context, selected)
                },
                onDeleteClick = { viewModel.requestDeleteConfirmation() },
                onClearSelection = { viewModel.clearSelection() },
                selectedTotalSize = selectedTotalSize
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Barra de progreso y estado de escaneo si está activo
            AnimatedVisibility(visible = uiState.isScanning) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = SleekPurple,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = uiState.scanStatusText,
                        fontSize = 11.sp,
                        color = SleekPurple,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Filtros de estrellas (Todas, 5, 4, 3, 2, 1, 0, No disponible)
            FilterBar(
                currentFilter = uiState.ratingFilter,
                ratingCounts = uiState.ratingCounts,
                onFilterSelected = { viewModel.setRatingFilter(it) }
            )

            HorizontalDivider(color = SleekOutlineVariant, thickness = 1.dp)

            // Tabla interactiva de fotos
            PhotoTableView(
                photos = uiState.filteredPhotos,
                selectedUris = uiState.selectedUris,
                sortOrder = uiState.sortOrder,
                columnVisibility = uiState.columnVisibility,
                isRangeModeActive = uiState.isRangeModeActive,
                rangeAnchorUri = uiState.rangeAnchorUri,
                onSortClick = { viewModel.toggleSort(it) },
                onPhotoToggle = { viewModel.toggleSelectPhoto(it) },
                onSelectAllFiltered = { viewModel.toggleSelectAllFiltered() },
                onOpenFolderPicker = { folderPickerLauncher.launch(null) }
            )
        }
    }

    // Diálogos de operación
    when (val op = uiState.operationState) {
        is OperationState.Progress -> {
            OperationProgressDialog(progressState = op)
        }
        is OperationState.Summary -> {
            OperationSummaryDialog(
                summary = op,
                onDismiss = { viewModel.dismissOperationDialog() }
            )
        }
        OperationState.Idle -> {}
    }

    if (uiState.showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            count = uiState.selectedUris.size,
            onConfirm = { viewModel.executeDeleteSelected() },
            onDismiss = { viewModel.dismissDeleteConfirmation() }
        )
    }

    if (uiState.isSettingsOpen) {
        SettingsDialog(
            currentFolderUri = uiState.currentTreeUri?.toString(),
            folderDisplayName = uiState.folderDisplayName,
            currentAppLanguage = uiState.appLanguage,
            onLanguageSelected = { lang ->
                viewModel.setAppLanguage(lang)
            },
            onChangeFolderClick = { folderPickerLauncher.launch(null) },
            onClearCacheClick = { viewModel.clearCacheAndRescan() },
            onDismiss = { viewModel.setSettingsOpen(false) }
        )
    }
}

/**
 * Comparte las fotos seleccionadas a través de la hoja nativa de Android (ACTION_SEND_MULTIPLE).
 */
private fun shareSelectedPhotos(context: Context, selectedPhotos: List<PhotoItem>) {
    if (selectedPhotos.isEmpty()) return

    val uris = ArrayList(selectedPhotos.map { it.uri })
    val mimeType = if (selectedPhotos.all { it.mimeType.startsWith("image/") }) {
        if (selectedPhotos.all { it.mimeType == selectedPhotos.first().mimeType }) {
            selectedPhotos.first().mimeType
        } else {
            "image/*"
        }
    } else {
        "*/*"
    }

    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    val title = context.getString(R.string.share_chooser_title, uris.size)
    val chooser = Intent.createChooser(intent, title)
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(chooser)
    } catch (_: Exception) {}
}
