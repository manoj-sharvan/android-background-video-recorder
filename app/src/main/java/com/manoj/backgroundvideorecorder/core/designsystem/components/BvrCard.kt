package com.manoj.backgroundvideorecorder.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.manoj.backgroundvideorecorder.core.designsystem.theme.DarkDanger
import com.manoj.backgroundvideorecorder.core.designsystem.theme.DarkSuccess
import com.manoj.backgroundvideorecorder.core.designsystem.theme.DarkSurface

enum class CardVariant {
    NORMAL, WARNING, SUCCESS, DANGER
}

@Composable
fun BvrCard(
    modifier: Modifier = Modifier,
    variant: CardVariant = CardVariant.NORMAL,
    content: @Composable () -> Unit
) {
    val containerColor = when (variant) {
        CardVariant.NORMAL -> DarkSurface
        CardVariant.WARNING -> Color(0xFFF57C00).copy(alpha = 0.2f)
        CardVariant.SUCCESS -> DarkSuccess.copy(alpha = 0.2f)
        CardVariant.DANGER  -> DarkDanger.copy(alpha = 0.2f)
        else -> DarkSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}
