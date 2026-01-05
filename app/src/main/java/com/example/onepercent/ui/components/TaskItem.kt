package com.example.onepercent.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onepercent.local.entity.TaskEntity

@Composable
fun TaskItem(
    task: TaskEntity,
    index: Int,
    totalCount: Int,
    onDelete: () -> Unit,
    onToggleComplete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .then(
                if (task.isPriority && task.isCompleted) {
                    Modifier.alpha(0.6f)
                } else {
                    Modifier
                }
            ),
        shape = groupedCardShape(index, totalCount),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isPriority)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onToggleComplete != null) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() }
                )
            }
            Text(
                text = task.name,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
                style = if (task.isCompleted) {
                    MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    MaterialTheme.typography.bodyLarge
                }
            )
            TextButton(onClick = onDelete) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}


fun groupedCardShape(
    index: Int,
    size: Int,
    radius: Dp = 24.dp,
    innerRadius: Dp = 1.dp
): Shape {
    return when {
        size == 1 -> RoundedCornerShape(radius)
        index == 0 -> RoundedCornerShape(
            topStart = radius,
            topEnd = radius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
        index == size - 1 -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = radius,
            bottomEnd = radius
        )
        else -> RoundedCornerShape(innerRadius)
    }
}
