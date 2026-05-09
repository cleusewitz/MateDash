package com.soooool.matedash.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.soooool.matedash.data.api.PositionPoint
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

private fun buildRouteHtml(route: List<PositionPoint>): String {
    if (route.size < 2) return "<html><body style='background:#050505'></body></html>"

    val latMin = route.minOf { it.latitude }
    val latMax = route.maxOf { it.latitude }
    val lngMin = route.minOf { it.longitude }
    val lngMax = route.maxOf { it.longitude }

    val coordsJson = route.joinToString(",") { "[${it.latitude},${it.longitude}]" }
    val start = route.first()
    val end = route.last()

    return """
    <!DOCTYPE html>
    <html><head>
    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
    <style>
      html,body,#map{margin:0;padding:0;width:100%;height:100%;background:#EFEFEF}
      .leaflet-control-attribution{display:none!important}
    </style>
    </head><body>
    <div id="map"></div>
    <script>
      var map = L.map('map',{zoomControl:false});
      // CartoDB Voyager — Apple Maps Standard와 가장 비슷한 라이트 테마, 한글 라벨 포함
      L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png',{
        subdomains:'abcd',maxZoom:20
      }).addTo(map);

      var coords = [$coordsJson];
      var polyline = L.polyline(coords,{color:'#0A84FF',weight:5,opacity:0.95}).addTo(map);

      L.circleMarker([${start.latitude},${start.longitude}],{
        radius:7,fillColor:'#34C759',fillOpacity:1,color:'#fff',weight:2
      }).addTo(map).bindPopup('출발');

      L.circleMarker([${end.latitude},${end.longitude}],{
        radius:7,fillColor:'#FF3B30',fillOpacity:1,color:'#fff',weight:2
      }).addTo(map).bindPopup('도착');

      map.fitBounds([[$latMin,$lngMin],[$latMax,$lngMax]],{padding:[30,30]});
    </script>
    </body></html>
    """.trimIndent()
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun RouteMapView(route: List<PositionPoint>, modifier: Modifier, interactive: Boolean) {
    val lastRouteSize = remember { intArrayOf(-1) }

    UIKitView(
        factory = {
            val config = WKWebViewConfiguration()
            WKWebView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0), configuration = config).apply {
                scrollView.scrollEnabled = interactive
                scrollView.bounces = false
                opaque = false
                setBackgroundColor(platform.UIKit.UIColor.blackColor)
            }
        },
        update = { webView ->
            if (lastRouteSize[0] != route.size) {
                lastRouteSize[0] = route.size
                val html = buildRouteHtml(route)
                webView.loadHTMLString(html, baseURL = null)
            }
        },
        modifier = modifier,
    )
}
