package com.soooool.matedash.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.soooool.matedash.data.api.PositionPoint
import com.soooool.matedash.mapkit.MateDashAddAnnotation
import com.soooool.matedash.mapkit.MateDashAddOverlay
import com.soooool.matedash.mapkit.MateDashClearAll
import com.soooool.matedash.mapkit.MateDashMakeAnnotation
import com.soooool.matedash.mapkit.MateDashMakePolyline
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKOverlayProtocol
import platform.MapKit.MKOverlayRenderer
import platform.MapKit.MKPolyline
import platform.MapKit.MKPolylineRenderer
import platform.UIKit.UIColor
import platform.UIKit.UIUserInterfaceStyle
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class RouteMapDelegate : NSObject(), MKMapViewDelegateProtocol {
    override fun mapView(
        mapView: MKMapView,
        rendererForOverlay: MKOverlayProtocol,
    ): MKOverlayRenderer {
        if (rendererForOverlay is MKPolyline) {
            return MKPolylineRenderer(polyline = rendererForOverlay).apply {
                strokeColor = UIColor(red = 0.04, green = 0.52, blue = 1.0, alpha = 0.95) // system blue
                lineWidth = 5.0
            }
        }
        return MKOverlayRenderer(overlay = rendererForOverlay)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@Composable
actual fun RouteMapView(route: List<PositionPoint>, modifier: Modifier, interactive: Boolean) {
    val delegate = remember { RouteMapDelegate() }
    val lastRouteSize = remember { intArrayOf(-1) }

    UIKitView(
        factory = {
            MKMapView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
                setDelegate(delegate)
                setShowsCompass(false)
                setShowsScale(false)
                setShowsTraffic(false)
                setZoomEnabled(interactive)
                setScrollEnabled(interactive)
                setRotateEnabled(interactive)
                setPitchEnabled(interactive)
                // 시스템 다크모드 따라가지 않게 라이트 고정 (Apple Maps Standard 톤)
                overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
            }
        },
        update = { mapView ->
            if (lastRouteSize[0] == route.size) return@UIKitView
            lastRouteSize[0] = route.size

            // 기존 overlay/annotation 정리 (cinterop helper 통해)
            MateDashClearAll(mapView)
            if (route.size < 2) return@UIKitView

            // 좌표 두 개의 double 배열로 분리해서 ObjC 헬퍼에 전달 (CLLocationCoordinate2D 직접 만들기 회피)
            memScoped {
                val lats = allocArray<DoubleVar>(route.size)
                val lngs = allocArray<DoubleVar>(route.size)
                route.forEachIndexed { i, p ->
                    lats[i] = p.latitude
                    lngs[i] = p.longitude
                }
                val polyline = MateDashMakePolyline(lats, lngs, route.size.toULong())
                MateDashAddOverlay(mapView, polyline)
            }

            // 출발/도착 핀
            val start = route.first()
            val end = route.last()
            MateDashAddAnnotation(mapView, MateDashMakeAnnotation(start.latitude, start.longitude, "출발"))
            MateDashAddAnnotation(mapView, MateDashMakeAnnotation(end.latitude, end.longitude, "도착"))

            // 카메라 맞춤
            val latMin = route.minOf { it.latitude }
            val latMax = route.maxOf { it.latitude }
            val lngMin = route.minOf { it.longitude }
            val lngMax = route.maxOf { it.longitude }
            val centerLat = (latMin + latMax) / 2.0
            val centerLng = (lngMin + lngMax) / 2.0
            val spanLat = (latMax - latMin).coerceAtLeast(0.001)
            val spanLng = (lngMax - lngMin).coerceAtLeast(0.001)
            val distanceMeters = (maxOf(spanLat, spanLng) * 111_000 * 1.4).coerceAtLeast(500.0)
            val region = MKCoordinateRegionMakeWithDistance(
                centerCoordinate = CLLocationCoordinate2DMake(centerLat, centerLng),
                latitudinalMeters = distanceMeters,
                longitudinalMeters = distanceMeters,
            )
            mapView.setRegion(region, animated = false)
        },
        modifier = modifier,
    )
}
