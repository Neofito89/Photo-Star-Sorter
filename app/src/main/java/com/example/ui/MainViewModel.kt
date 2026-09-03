package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.db.PhotoDatabase
import com.example.data.model.ColumnVisibility
import com.example.data.model.OperationState
import com.example.data.model.PhotoItem
import com.example.data.model.RatingFilter
import com.example.data.model.RatingStatus
import com.example.data.model.SortDirection
import com.example.data.model.SortField
import com.example.data.model.SortOrder
import com.example.data.repository.PhotoRepository
import com.example.util.AppLanguage
import com.example.util.LocaleHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

data class MainUiState(
    val currentTreeUri: Uri? = null,
    val folderDisplayName: String? = null,
    val isScanning: Boolean = false,
    val scanStatusText: String = "",
    val allPhotos: List<PhotoItem> = emptyList(),
    val filteredPhotos: List<PhotoItem> = emptyList(),
    val selectedUris: Set<String> = emptySet(),
    val ratingFilter: RatingFilter = RatingFilter.All,
    val sortOrder: SortOrder = SortOrder(SortField.RATING, SortDirection.DESCENDING),
    val columnVisibility: ColumnVisibility = ColumnVisibility(),
    val isRangeModeActive: Boolean = false,
    val rangeAnchorUri: String? = null,
    val operationState: OperationState = OperationState.Idle,
    val isSettingsOpen: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val ratingCounts: Map<RatingFilter, Int> = emptyMap(),
    val appLanguage: AppLanguage = AppLanguage.SYSTEM
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("photo_star_sorter_prefs", Context.MODE_PRIVATE)
    private val database = PhotoDatabase.getInstance(application)
    private val repository = PhotoRepository(application, database.photoCacheDao())

    private val _uiState = MutableStateFlow(
        MainUiState(
            appLanguage = LocaleHelper.getSavedAppLanguage(application)
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Cargar URI persistida previamente si existe
        val savedUriStr = prefs.getString("saved_tree_uri", null)
        if (!savedUriStr.isNullOrBlank()) {
            val uri = Uri.parse(savedUriStr)
            val hasPermission = checkUriPermission(uri)
            if (hasPermission) {
                _uiState.update { it.copy(currentTreeUri = uri, folderDisplayName = getDisplayName(uri)) }
                startScan(uri)
            }
        }
    }

    private fun checkUriPermission(uri: Uri): Boolean {
        val persistedList = getApplication<Application>().contentResolver.persistedUriPermissions
        return persistedList.any { it.uri == uri && (it.isReadPermission || it.isWritePermission) }
    }

    private fun getDisplayName(uri: Uri): String {
        return uri.lastPathSegment?.substringAfterLast(':') ?: uri.lastPathSegment ?: "SD Card"
    }

    fun onFolderSelected(uri: Uri) {
        val app = getApplication<Application>()
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            app.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {}

        prefs.edit().putString("saved_tree_uri", uri.toString()).apply()

        _uiState.update {
            it.copy(
                currentTreeUri = uri,
                folderDisplayName = getDisplayName(uri),
                selectedUris = emptySet(),
                rangeAnchorUri = null,
                isRangeModeActive = false
            )
        }
        startScan(uri)
    }

    fun rescan() {
        _uiState.value.currentTreeUri?.let { startScan(it) }
    }

    private fun startScan(uri: Uri) {
        val context = getApplication<Application>()
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanning = true,
                    scanStatusText = context.getString(R.string.scan_starting),
                    allPhotos = emptyList(),
                    filteredPhotos = emptyList(),
                    selectedUris = emptySet()
                )
            }

            try {
                repository.scanDirectory(uri) { count, name ->
                    _uiState.update {
                        it.copy(
                            scanStatusText = context.getString(R.string.scan_progress, count, name)
                        )
                    }
                }.collect { discoveredList ->
                    updatePhotosList(discoveredList)
                }
            } catch (e: Exception) {
                val mediaDisconnected = context.getString(R.string.scan_media_disconnected)
                val errMsg = e.localizedMessage ?: mediaDisconnected
                _uiState.update {
                    it.copy(
                        scanStatusText = context.getString(R.string.scan_error, errMsg)
                    )
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        scanStatusText = context.getString(R.string.scan_completed, _uiState.value.allPhotos.size)
                    )
                }
            }
        }
    }

    private fun updatePhotosList(rawList: List<PhotoItem>) {
        val filter = _uiState.value.ratingFilter
        val sort = _uiState.value.sortOrder
        val filtered = applyFilterAndSort(rawList, filter, sort)
        val counts = calculateRatingCounts(rawList)
        _uiState.update {
            it.copy(
                allPhotos = rawList,
                filteredPhotos = filtered,
                ratingCounts = counts
            )
        }
    }

    fun setRatingFilter(filter: RatingFilter) {
        _uiState.update {
            val filtered = applyFilterAndSort(it.allPhotos, filter, it.sortOrder)
            it.copy(ratingFilter = filter, filteredPhotos = filtered)
        }
    }

    fun toggleSort(field: SortField) {
        _uiState.update { current ->
            val newDirection = if (current.sortOrder.field == field) {
                if (current.sortOrder.direction == SortDirection.ASCENDING) SortDirection.DESCENDING else SortDirection.ASCENDING
            } else {
                if (field == SortField.RATING) SortDirection.DESCENDING else SortDirection.ASCENDING
            }
            val newSort = SortOrder(field, newDirection)
            val filtered = applyFilterAndSort(current.allPhotos, current.ratingFilter, newSort)
            current.copy(sortOrder = newSort, filteredPhotos = filtered)
        }
    }

    fun toggleColumnVisibility(
        showType: Boolean? = null,
        showSize: Boolean? = null,
        showDate: Boolean? = null,
        showPath: Boolean? = null
    ) {
        _uiState.update {
            it.copy(
                columnVisibility = it.columnVisibility.copy(
                    showType = showType ?: it.columnVisibility.showType,
                    showSize = showSize ?: it.columnVisibility.showSize,
                    showDate = showDate ?: it.columnVisibility.showDate,
                    showPath = showPath ?: it.columnVisibility.showPath
                )
            )
        }
    }

    fun toggleRangeMode() {
        _uiState.update {
            val newMode = !it.isRangeModeActive
            it.copy(
                isRangeModeActive = newMode,
                rangeAnchorUri = if (newMode) it.selectedUris.lastOrNull() else null
            )
        }
    }

    fun toggleSelectPhoto(photo: PhotoItem) {
        _uiState.update { state ->
            val uriStr = photo.uriString
            if (state.isRangeModeActive && state.rangeAnchorUri != null && state.rangeAnchorUri != uriStr) {
                val anchorIndex = state.filteredPhotos.indexOfFirst { it.uriString == state.rangeAnchorUri }
                val targetIndex = state.filteredPhotos.indexOfFirst { it.uriString == uriStr }
                if (anchorIndex != -1 && targetIndex != -1) {
                    val minIdx = min(anchorIndex, targetIndex)
                    val maxIdx = max(anchorIndex, targetIndex)
                    val blockUris = state.filteredPhotos.subList(minIdx, maxIdx + 1).map { it.uriString }.toSet()
                    return@update state.copy(
                        selectedUris = state.selectedUris + blockUris,
                        rangeAnchorUri = uriStr,
                        isRangeModeActive = false
                    )
                }
            }

            val currentSelected = state.selectedUris
            val newSelected = if (currentSelected.contains(uriStr)) {
                currentSelected - uriStr
            } else {
                currentSelected + uriStr
            }

            state.copy(
                selectedUris = newSelected,
                rangeAnchorUri = if (newSelected.contains(uriStr)) uriStr else state.rangeAnchorUri
            )
        }
    }

    fun toggleSelectAllFiltered() {
        _uiState.update { state ->
            val currentFilteredUris = state.filteredPhotos.map { it.uriString }.toSet()
            val allSelected = currentFilteredUris.isNotEmpty() && currentFilteredUris.all { state.selectedUris.contains(it) }
            val newSelected = if (allSelected) {
                state.selectedUris - currentFilteredUris
            } else {
                state.selectedUris + currentFilteredUris
            }
            state.copy(selectedUris = newSelected)
        }
    }

    fun clearSelection() {
        _uiState.update {
            it.copy(selectedUris = emptySet(), rangeAnchorUri = null, isRangeModeActive = false)
        }
    }

    fun requestDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmDialog = true) }
    }

    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmDialog = false) }
    }

    fun executeDeleteSelected() {
        dismissDeleteConfirmation()
        val context = getApplication<Application>()
        val selectedPhotos = _uiState.value.allPhotos.filter { _uiState.value.selectedUris.contains(it.uriString) }
        if (selectedPhotos.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    operationState = OperationState.Progress(
                        currentItem = 0,
                        totalItems = selectedPhotos.size,
                        currentFileName = "",
                        isDelete = true
                    )
                )
            }

            val result = repository.deletePhotos(selectedPhotos) { current, total, name ->
                _uiState.update {
                    it.copy(
                        operationState = OperationState.Progress(
                            currentItem = current,
                            totalItems = total,
                            currentFileName = name,
                            isDelete = true
                        )
                    )
                }
            }

            val deletedUris = selectedPhotos.take(result.successCount).map { it.uriString }.toSet()
            val remainingAll = _uiState.value.allPhotos.filterNot { deletedUris.contains(it.uriString) }
            val remainingSelected = _uiState.value.selectedUris - deletedUris

            _uiState.update { state ->
                val filtered = applyFilterAndSort(remainingAll, state.ratingFilter, state.sortOrder)
                state.copy(
                    allPhotos = remainingAll,
                    filteredPhotos = filtered,
                    selectedUris = remainingSelected,
                    ratingCounts = calculateRatingCounts(remainingAll),
                    operationState = OperationState.Summary(
                        successCount = result.successCount,
                        failedCount = result.failedCount,
                        failureMessages = result.errorMessages,
                        actionType = "ELIMINAR"
                    )
                )
            }
        }
    }

    fun executeCopySelected(destinationTreeUri: Uri) {
        val selectedPhotos = _uiState.value.allPhotos.filter { _uiState.value.selectedUris.contains(it.uriString) }
        if (selectedPhotos.isEmpty()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    operationState = OperationState.Progress(
                        currentItem = 0,
                        totalItems = selectedPhotos.size,
                        currentFileName = "",
                        isDelete = false
                    )
                )
            }

            val result = repository.copyPhotos(selectedPhotos, destinationTreeUri) { current, total, name ->
                _uiState.update {
                    it.copy(
                        operationState = OperationState.Progress(
                            currentItem = current,
                            totalItems = total,
                            currentFileName = name,
                            isDelete = false
                        )
                    )
                }
            }

            _uiState.update {
                it.copy(
                    operationState = OperationState.Summary(
                        successCount = result.successCount,
                        failedCount = result.failedCount,
                        failureMessages = result.errorMessages,
                        actionType = "COPIAR"
                    )
                )
            }
        }
    }

    fun dismissOperationDialog() {
        _uiState.update { it.copy(operationState = OperationState.Idle) }
    }

    fun setSettingsOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
    }

    fun setAppLanguage(language: AppLanguage) {
        val app = getApplication<Application>()
        LocaleHelper.setAppLanguage(app, language)
        _uiState.update { it.copy(appLanguage = language) }
    }

    fun clearCacheAndRescan() {
        val uri = _uiState.value.currentTreeUri ?: return
        viewModelScope.launch {
            repository.clearCacheForTree(uri)
            setSettingsOpen(false)
            startScan(uri)
        }
    }

    companion object {
        fun calculateRatingCounts(photos: List<PhotoItem>): Map<RatingFilter, Int> {
            val counts = mutableMapOf<RatingFilter, Int>()
            counts[RatingFilter.All] = photos.size
            counts[RatingFilter.Exact(5)] = photos.count { it.rating == 5 }
            counts[RatingFilter.Exact(4)] = photos.count { it.rating == 4 }
            counts[RatingFilter.Exact(3)] = photos.count { it.rating == 3 }
            counts[RatingFilter.Exact(2)] = photos.count { it.rating == 2 }
            counts[RatingFilter.Exact(1)] = photos.count { it.rating == 1 }
            counts[RatingFilter.Exact(0)] = photos.count { it.rating == 0 || it.ratingStatus == RatingStatus.UNRATED }
            counts[RatingFilter.Unavailable] = photos.count { it.ratingStatus == RatingStatus.UNAVAILABLE || it.ratingStatus == RatingStatus.ERROR }
            return counts
        }

        fun applyFilterAndSort(
            photos: List<PhotoItem>,
            filter: RatingFilter,
            sort: SortOrder
        ): List<PhotoItem> {
            // 1. Filtrado completo
            val filtered = when (filter) {
                is RatingFilter.All -> photos
                is RatingFilter.Exact -> {
                    if (filter.stars == 0) {
                        photos.filter { it.rating == 0 || it.ratingStatus == RatingStatus.UNRATED }
                    } else {
                        photos.filter { it.rating == filter.stars }
                    }
                }
                is RatingFilter.Unavailable -> {
                    photos.filter { it.ratingStatus == RatingStatus.UNAVAILABLE || it.ratingStatus == RatingStatus.ERROR }
                }
            }

            // 2. Ordenamiento
            val comparator = when (sort.field) {
                SortField.RATING -> compareBy<PhotoItem> { it.rating ?: -1 }
                    .thenBy { it.fileName.lowercase() }
                SortField.NAME -> compareBy<PhotoItem> { it.fileName.lowercase() }
                SortField.DATE -> compareBy<PhotoItem> { it.captureDate ?: it.lastModified }
                    .thenBy { it.fileName.lowercase() }
                SortField.SIZE -> compareBy<PhotoItem> { it.fileSize }
                    .thenBy { it.fileName.lowercase() }
            }

            return if (sort.direction == SortDirection.ASCENDING) {
                filtered.sortedWith(comparator)
            } else {
                filtered.sortedWith(comparator.reversed())
            }
        }
    }
}
