package com.kaushalyakarnataka.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors

/**
 * A clean SaaS-style background container.
 * Formerly "GalaxyBackground", now updated to a professional minimal dark style.
 */
@Composable
fun GalaxyBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KaushalyaColors.Background)
    ) {
        content()
    }
}
