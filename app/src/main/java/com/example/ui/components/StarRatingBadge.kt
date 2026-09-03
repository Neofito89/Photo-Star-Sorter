package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.RatingStatus
import com.example.ui.theme.SleekErrorContainer
import com.example.ui.theme.SleekOnErrorContainer
import com.example.ui.theme.SleekOnPurpleContainer
import com.example.ui.theme.SleekOnSurfaceVariant
import com.example.ui.theme.SleekPurple
import com.example.ui.theme.SleekPurpleContainer

@Composable
fun StarRatingBadge(
    rating: Int?,
    ratingStatus: RatingStatus,
    modifier: Modifier = Modifier
) {
    when (ratingStatus) {
        RatingStatus.RATED -> {
            val stars = (rating ?: 0).coerceIn(1, 5)
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SleekPurpleContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Número de estrellas
                Text(
                    text = "$stars",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekOnPurpleContainer
                )

                // Estrellas visuales en el color púrpura característico del tema Sleek Interface
                Row(
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 1..5) {
                        if (i <= stars) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = SleekPurple,
                                modifier = Modifier.size(13.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = SleekPurple.copy(alpha = 0.25f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        RatingStatus.UNRATED -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.rating_unrated),
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = SleekOnSurfaceVariant
                )
            }
        }

        RatingStatus.UNAVAILABLE -> {
            Row(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.rating_unavailable),
                    tint = SleekOnSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = stringResource(R.string.filter_unavailable),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = SleekOnSurfaceVariant
                )
            }
        }

        RatingStatus.ERROR -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SleekErrorContainer)
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stringResource(R.string.rating_error),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleekOnErrorContainer
                )
            }
        }
    }
}
