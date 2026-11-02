package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CharacterLimitIndicator(
    currentLength: Int,
    maxLimit: Int,
    modifier: Modifier = Modifier
) {
    val progress = (currentLength.toFloat() / maxLimit).coerceIn(0f, 1f)
    val isNearLimit = currentLength > maxLimit * 0.85f
    val isOverLimit = currentLength > maxLimit

    val barColor = when {
        isOverLimit -> MaterialTheme.colorScheme.error
        isNearLimit -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isOverLimit) "⚠️ Превышен лимит описания!" else "Лимит описания персонажа",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isOverLimit) FontWeight.Bold else FontWeight.Normal,
                color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$currentLength / $maxLimit симв.",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
        if (isOverLimit) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Лимит превышен на ${currentLength - maxLimit} симв.! Сохранение заблокировано. Сократите описание.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        } else if (isNearLimit) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Осталось ${maxLimit - currentLength} симв.",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFF59E0B),
                fontSize = 11.sp
            )
        }
    }
}
