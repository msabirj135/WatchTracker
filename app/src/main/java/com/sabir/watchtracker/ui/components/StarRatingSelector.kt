package com.sabir.watchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StarRatingSelector(
    rating: Double?,
    onRatingChange: (Double?) -> Unit,
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(10) { index ->
                val value = index + 1
                Text(
                    text = if ((rating ?: 0.0) >= value) "★" else "☆",
                    modifier = Modifier
                        .size(27.dp)
                        .clickable(enabled = enabled) {
                            onRatingChange(value.toDouble())
                        },
                    color = if ((rating ?: 0.0) >= value) {
                        Color(0xFFFFC857)
                    } else {
                        Color(0xFF6F727D)
                    },
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rating?.let { "${it.toInt()} / 10" } ?: "Not rated",
                color = Color(0xFF9A9DA8),
                fontSize = 12.sp
            )

            if (rating != null) {
                TextButton(
                    onClick = { onRatingChange(null) },
                    enabled = enabled
                ) {
                    Text("Clear", color = Color(0xFFE63946))
                }
            }
        }
    }
}
