package com.example.data.model

import androidx.annotation.StringRes
import com.example.R

sealed interface RatingFilter {
    data object All : RatingFilter
    data class Exact(val stars: Int) : RatingFilter // 1..5 o 0
    data object Unavailable : RatingFilter

    val labelRes: Int
        get() = when (this) {
            is All -> R.string.filter_all
            is Exact -> when (stars) {
                0 -> R.string.filter_unrated
                1 -> R.string.filter_star_1
                2 -> R.string.filter_star_2
                3 -> R.string.filter_star_3
                4 -> R.string.filter_star_4
                5 -> R.string.filter_star_5
                else -> R.string.filter_all
            }
            is Unavailable -> R.string.filter_unavailable
        }

    val labelSpanish: String
        get() = when (this) {
            is All -> "Todas"
            is Exact -> when (stars) {
                0 -> "0 ★ (Sin calificar)"
                1 -> "1 ★"
                2 -> "2 ★"
                3 -> "3 ★"
                4 -> "4 ★"
                5 -> "5 ★"
                else -> "$stars ★"
            }
            is Unavailable -> "No disponible"
        }
}
