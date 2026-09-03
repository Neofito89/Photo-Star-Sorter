package com.example.data.model

sealed interface OperationState {
    data object Idle : OperationState

    data class Progress(
        val title: String = "",
        val currentItem: Int,
        val totalItems: Int,
        val currentFileName: String,
        val isIndeterminate: Boolean = false,
        val isDelete: Boolean = false
    ) : OperationState {
        val percentage: Float
            get() = if (totalItems > 0) currentItem.toFloat() / totalItems.toFloat() else 0f
    }

    data class Summary(
        val title: String = "",
        val successCount: Int,
        val failedCount: Int,
        val failureMessages: List<String> = emptyList(),
        val actionType: String // "COPIAR" o "ELIMINAR"
    ) : OperationState
}
