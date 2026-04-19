package com.soooool.matedash.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun LeafletMapView(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
)
