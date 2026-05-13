package com.kaushalyakarnataka.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import kotlin.random.Random

@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val starPositions = remember {
        List(40) {
            Offset(Random.nextFloat(), Random.nextFloat())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = KaushalyaColors.BackgroundGradient
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            starPositions.forEach { pos ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = 1.5f,
                    center = Offset(pos.x * size.width, pos.y * size.height)
                )
            }
        }
        content()
    }
}
