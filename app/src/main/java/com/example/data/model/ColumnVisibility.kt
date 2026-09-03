package com.example.data.model

data class ColumnVisibility(
    val showDate: Boolean = false,
    val showSize: Boolean = false,
    val showType: Boolean = false,
    val showPath: Boolean = false
)

enum class OptionalColumn {
    DATE,
    SIZE,
    TYPE,
    PATH
}
