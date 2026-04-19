package com.soooool.matedash.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun LeafletMapView(lat: Double, lng: Double, modifier: Modifier, interactive: Boolean) {
    val lastCoord = remember { doubleArrayOf(Double.NaN, Double.NaN) }

    UIKitView(
        factory = {
            MKMapView().also { mapView ->
                mapView.scrollEnabled = interactive
                mapView.zoomEnabled = interactive
                mapView.rotateEnabled = interactive
                mapView.pitchEnabled = interactive
                mapView.userInteractionEnabled = interactive
            }
        },
        update = { mapView ->
            if (lastCoord[0] != lat || lastCoord[1] != lng) {
                lastCoord[0] = lat
                lastCoord[1] = lng
                val coordinate = CLLocationCoordinate2DMake(lat, lng)
                val region = MKCoordinateRegionMakeWithDistance(coordinate, 1000.0, 1000.0)
                mapView.setRegion(region, animated = false)
                mapView.removeAnnotations(mapView.annotations)
                val annotation = MKPointAnnotation()
                annotation.setCoordinate(coordinate)
                mapView.addAnnotation(annotation)
            }
        },
        modifier = modifier,
    )
}
