package com.soooool.matedash.ui.map

import android.annotation.SuppressLint
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.soooool.matedash.data.api.PositionPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

// Apple Maps Standard와 가장 비슷한 라이트 테마, 한글 라벨 포함
private val CartoVoyagerTiles = XYTileSource(
    "CartoDB.Voyager",
    0, 20, 256, ".png",
    arrayOf(
        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://c.basemaps.cartocdn.com/rastertiles/voyager/",
        "https://d.basemaps.cartocdn.com/rastertiles/voyager/",
    ),
    "© OpenStreetMap contributors © CARTO",
)

@SuppressLint("ClickableViewAccessibility")
@Composable
actual fun RouteMapView(route: List<PositionPoint>, modifier: Modifier, interactive: Boolean) {
    AndroidView(
        factory = { context ->
            Configuration.getInstance().userAgentValue = "MateDash/1.0"
            Configuration.getInstance().osmdroidBasePath = context.cacheDir
            MapView(context).apply {
                setTileSource(CartoVoyagerTiles)
                setMultiTouchControls(interactive)
                @Suppress("DEPRECATION")
                setBuiltInZoomControls(false)
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
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
            mapView.overlays.clear()
            if (route.size >= 2) {
                val geoPoints = route.map { GeoPoint(it.latitude, it.longitude) }

                val polyline = Polyline()
                polyline.setPoints(geoPoints)
                polyline.outlinePaint.color = android.graphics.Color.rgb(0, 199, 255)
                polyline.outlinePaint.strokeWidth = 6f
                polyline.outlinePaint.isAntiAlias = true
                mapView.overlays.add(polyline)

                val startMarker = Marker(mapView)
                startMarker.position = geoPoints.first()
                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                startMarker.title = "출발"
                mapView.overlays.add(startMarker)

                val endMarker = Marker(mapView)
                endMarker.position = geoPoints.last()
                endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                endMarker.title = "도착"
                mapView.overlays.add(endMarker)

                val bbox = BoundingBox.fromGeoPoints(geoPoints)
                mapView.zoomToBoundingBox(bbox, false, 60)
            } else if (route.size == 1) {
                mapView.controller.setZoom(15.0)
                mapView.controller.setCenter(GeoPoint(route[0].latitude, route[0].longitude))
            }
            mapView.invalidate()
        },
        modifier = modifier,
    )
}
