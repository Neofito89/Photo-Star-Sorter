package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ColumnVisibility
import com.example.data.model.PhotoItem
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.SortOrder
import com.example.ui.theme.SleekOnPurpleContainer
import com.example.ui.theme.SleekOnSurfaceVariant
import com.example.ui.theme.SleekOutline
import com.example.ui.theme.SleekOutlineVariant
import com.example.ui.theme.SleekPurple
import com.example.ui.theme.SleekPurpleContainer
import com.example.ui.theme.SleekSurfaceContainer
import com.example.ui.theme.SleekSurfaceContainerHigh

@Composable
fun PhotoTableView(
    photos: List<PhotoItem>,
    selectedUris: Set<String>,
    sortOrder: SortOrder,
    columnVisibility: ColumnVisibility,
    isRangeModeActive: Boolean,
    rangeAnchorUri: String?,
    onSortClick: (SortField) -> Unit,
    onPhotoToggle: (PhotoItem) -> Unit,
    onSelectAllFiltered: () -> Unit,
    onOpenFolderPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) {
        EmptyTableView(
            onOpenFolderPicker = onOpenFolderPicker,
            modifier = modifier
        )
        return
    }

    val horizontalScrollState = rememberScrollState()
    val allFilteredSelected = photos.isNotEmpty() && photos.all { selectedUris.contains(it.uriString) }

    Column(modifier = modifier.fillMaxSize()) {
        // Encabezado de la tabla con scroll horizontal sincronizado
        Surface(
            tonalElevation = 2.dp,
            color = SleekSurfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox para seleccionar todos los resultados filtrados
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onSelectAllFiltered() }
                        .padding(end = 8.dp)
                        .testTag("select_all_header_button")
                ) {
                    Checkbox(
                        checked = allFilteredSelected,
                        onCheckedChange = { onSelectAllFiltered() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = SleekPurple,
                            checkmarkColor = Color.White,
                            uncheckedColor = SleekOutline.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.testTag("select_all_checkbox")
                    )
                    Text(
                        text = stringResource(R.string.col_select_all),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SleekOnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Columna 1: Nombre del archivo
                TableHeaderCell(
                    title = stringResource(R.string.col_file_name),
                    sortField = SortField.NAME,
                    currentSort = sortOrder,
                    onSortClick = onSortClick,
                    modifier = Modifier.width(220.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Columna 2: Calificación por estrellas
                TableHeaderCell(
                    title = stringResource(R.string.col_rating),
                    sortField = SortField.RATING,
                    currentSort = sortOrder,
                    onSortClick = onSortClick,
                    modifier = Modifier.width(180.dp),
                    isProminent = true
                )

                // Columnas opcionales
                if (columnVisibility.showType) {
                    Spacer(modifier = Modifier.width(12.dp))
                    TableHeaderCell(
                        title = stringResource(R.string.col_type),
                        sortField = null,
                        currentSort = sortOrder,
                        onSortClick = {},
                        modifier = Modifier.width(70.dp)
                    )
                }

                if (columnVisibility.showSize) {
                    Spacer(modifier = Modifier.width(12.dp))
                    TableHeaderCell(
                        title = stringResource(R.string.col_size),
                        sortField = SortField.SIZE,
                        currentSort = sortOrder,
                        onSortClick = onSortClick,
                        modifier = Modifier.width(90.dp)
                    )
                }

                if (columnVisibility.showDate) {
                    Spacer(modifier = Modifier.width(12.dp))
                    TableHeaderCell(
                        title = stringResource(R.string.col_capture_date),
                        sortField = SortField.DATE,
                        currentSort = sortOrder,
                        onSortClick = onSortClick,
                        modifier = Modifier.width(140.dp)
                    )
                }

                if (columnVisibility.showPath) {
                    Spacer(modifier = Modifier.width(12.dp))
                    TableHeaderCell(
                        title = stringResource(R.string.col_relative_path),
                        sortField = null,
                        currentSort = sortOrder,
                        onSortClick = {},
                        modifier = Modifier.width(180.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = SleekOutlineVariant, thickness = 1.dp)

        // Filas de fotos
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(horizontalScrollState),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            itemsIndexed(
                items = photos,
                key = { _, item -> item.uriString }
            ) { index, photo ->
                val isSelected = selectedUris.contains(photo.uriString)
                val isAnchor = isRangeModeActive && rangeAnchorUri == photo.uriString

                PhotoTableRow(
                    photo = photo,
                    isSelected = isSelected,
                    isAnchor = isAnchor,
                    columnVisibility = columnVisibility,
                    onClick = { onPhotoToggle(photo) },
                    modifier = Modifier.testTag("photo_row_$index")
                )

                HorizontalDivider(color = SleekOutlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun TableHeaderCell(
    title: String,
    sortField: SortField?,
    currentSort: SortOrder,
    onSortClick: (SortField) -> Unit,
    modifier: Modifier = Modifier,
    isProminent: Boolean = false
) {
    val isCurrentSorted = sortField != null && currentSort.field == sortField

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(enabled = sortField != null) {
                if (sortField != null) onSortClick(sortField)
            }
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .testTag(if (sortField != null) "sort_header_${sortField.name.lowercase()}" else "header_cell"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            fontWeight = if (isProminent || isCurrentSorted) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isCurrentSorted) SleekPurple else SleekOnSurfaceVariant
        )

        if (isCurrentSorted) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (currentSort.direction == SortDirection.ASCENDING) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                contentDescription = if (currentSort.direction == SortDirection.ASCENDING) "Orden ascendente" else "Orden descendente",
                tint = SleekPurple,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PhotoTableRow(
    photo: PhotoItem,
    isSelected: Boolean,
    isAnchor: Boolean,
    columnVisibility: ColumnVisibility,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isAnchor -> SleekPurpleContainer.copy(alpha = 0.7f)
            isSelected -> SleekPurpleContainer.copy(alpha = 0.35f)
            else -> Color.Transparent
        },
        label = "rowBackground"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .then(
                if (isAnchor) {
                    Modifier.border(1.dp, SleekPurple, RoundedCornerShape(4.dp))
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Columna Selección (Checkbox)
        Box(modifier = Modifier.padding(end = 8.dp)) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = SleekPurple,
                    checkmarkColor = Color.White,
                    uncheckedColor = SleekOutline.copy(alpha = 0.7f)
                ),
                modifier = Modifier.testTag("checkbox_${photo.fileName}")
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Columna 1: Nombre del archivo
        Row(
            modifier = Modifier.width(220.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = photo.fileName,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) SleekPurple else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Columna 2: Calificación por estrellas
        Box(
            modifier = Modifier.width(180.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            StarRatingBadge(
                rating = photo.rating,
                ratingStatus = photo.ratingStatus
            )
        }

        // Columna: Tipo
        if (columnVisibility.showType) {
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier.width(70.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                FileTypeBadge(extension = photo.fileExtension)
            }
        }

        // Columna: Tamaño
        if (columnVisibility.showSize) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = photo.formattedSize,
                fontSize = 12.sp,
                color = SleekOnSurfaceVariant,
                modifier = Modifier.width(90.dp),
                maxLines = 1
            )
        }

        // Columna: Fecha
        if (columnVisibility.showDate) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = photo.captureDateFormatted ?: "-",
                fontSize = 12.sp,
                color = SleekOnSurfaceVariant,
                modifier = Modifier.width(140.dp),
                maxLines = 1
            )
        }

        // Columna: Ruta
        if (columnVisibility.showPath) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = photo.relativePath,
                fontSize = 11.sp,
                color = SleekOnSurfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.width(180.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FileTypeBadge(extension: String) {
    val isRaw = extension.equals("cr2", ignoreCase = true) ||
            extension.equals("cr3", ignoreCase = true) ||
            extension.equals("raw", ignoreCase = true)

    val badgeBg = if (isRaw) {
        SleekPurpleContainer
    } else {
        SleekSurfaceContainer
    }

    val textColor = if (isRaw) {
        SleekOnPurpleContainer
    } else {
        SleekOnSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(badgeBg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = extension.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun EmptyTableView(
    onOpenFolderPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(SleekPurpleContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PhotoLibrary,
                contentDescription = null,
                tint = SleekPurple,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.empty_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.empty_message),
            fontSize = 14.sp,
            color = SleekOnSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenFolderPicker,
            colors = ButtonDefaults.buttonColors(
                containerColor = SleekPurple,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(50),
            modifier = Modifier.testTag("open_folder_empty_button")
        ) {
            Icon(
                imageVector = Icons.Filled.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.empty_action_open))
        }
    }
}
