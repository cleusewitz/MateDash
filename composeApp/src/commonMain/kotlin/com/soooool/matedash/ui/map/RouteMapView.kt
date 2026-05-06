package com.soooool.matedash.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.soooool.matedash.data.api.PositionPoint

@Composable
expect fun RouteMapView(
    route: List<PositionPoint>,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
)
