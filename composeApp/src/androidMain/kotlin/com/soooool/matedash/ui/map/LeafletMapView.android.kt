package com.soooool.matedash.ui.map

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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

@Composable
actual fun LeafletMapView(lat: Double, lng: Double, modifier: Modifier) {
    key(lat, lng) {
        AndroidView(
            factory = { context ->
                Configuration.getInstance().userAgentValue = "MateDash/1.0"
                Configuration.getInstance().osmdroidBasePath = context.cacheDir
                MapView(context).apply {
                    setTileSource(CartoDarkTiles)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    isHorizontalMapRepetitionEnabled = false
                    isVerticalMapRepetitionEnabled = false
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(lat, lng))
                    val marker = Marker(this)
                    marker.position = GeoPoint(lat, lng)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    marker.title = "Tesla"
                    overlays.add(marker)
                    // 부모 ScrollView가 터치 이벤트를 가로채지 않도록 처리
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                            MotionEvent.ACTION_UP,
                            MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        false // 지도가 터치 이벤트를 직접 처리
                    }
                }
            },
            modifier = modifier,
        )
    }
}
