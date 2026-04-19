package com.soooool.matedash.ui.map

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private val CartoDarkTiles = XYTileSource(
    "CartoDB.DarkMatter",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/dark_all/",
        "https://b.basemaps.cartocdn.com/dark_all/",
        "https://c.basemaps.cartocdn.com/dark_all/",
        "https://d.basemaps.cartocdn.com/dark_all/",
    ),
    "© OpenStreetMap contributors © CARTO",
)

@SuppressLint("ClickableViewAccessibility")
@Composable
actual fun LeafletMapView(lat: Double, lng: Double, modifier: Modifier, interactive: Boolean) {
    AndroidView(
        factory = { context ->
            Configuration.getInstance().userAgentValue = "MateDash/1.0"
            Configuration.getInstance().osmdroidBasePath = context.cacheDir
            MapView(context).apply {
                setTileSource(CartoDarkTiles)
                setMultiTouchControls(interactive)
                @Suppress("DEPRECATION")
                setBuiltInZoomControls(false)
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                controller.setZoom(16.0)
                if (interactive) {
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false
                    }
                } else {
                    setOnTouchListener { _, _ -> true }
                }
            }
        },
        update = { mapView ->
            val point = GeoPoint(lat, lng)
            mapView.controller.setCenter(point)
            mapView.overlays.clear()
            val marker = Marker(mapView)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = "Tesla"
            mapView.overlays.add(marker)
            mapView.invalidate()
        },
        modifier = modifier,
    )
}
