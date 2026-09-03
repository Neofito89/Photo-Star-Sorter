package com.example.data.model

enum class SortField {
    RATING,
    NAME,
    DATE,
    SIZE
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

data class SortOrder(
    val field: SortField = SortField.RATING,
    val direction: SortDirection = SortDirection.DESCENDING
)
