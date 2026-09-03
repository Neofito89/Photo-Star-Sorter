package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RatingFilter
import com.example.ui.theme.SleekOnSurfaceVariant
import com.example.ui.theme.SleekOutline
import com.example.ui.theme.SleekPurple

@Composable
fun FilterBar(
    currentFilter: RatingFilter,
    ratingCounts: Map<RatingFilter, Int>,
    onFilterSelected: (RatingFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        RatingFilter.All,
        RatingFilter.Exact(5),
        RatingFilter.Exact(4),
        RatingFilter.Exact(3),
        RatingFilter.Exact(2),
        RatingFilter.Exact(1),
        RatingFilter.Exact(0),
        RatingFilter.Unavailable
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { filter ->
            val isSelected = currentFilter == filter
            val count = ratingCounts[filter] ?: 0

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                shape = RoundedCornerShape(50),
                border = if (isSelected) null else BorderStroke(1.dp, SleekOutline.copy(alpha = 0.6f)),
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(filter.labelRes),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "($count)",
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else SleekOnSurfaceVariant
                        )
                    }
                },
                leadingIcon = if (filter is RatingFilter.Exact && filter.stars > 0) {
                    {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else SleekPurple,
                            modifier = Modifier.padding(end = 2.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = SleekOnSurfaceVariant,
                    iconColor = SleekPurple,
                    selectedContainerColor = SleekPurple,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White
                ),
                modifier = Modifier.testTag("filter_chip_${filter.hashCode()}")
            )
        }
    }
}
