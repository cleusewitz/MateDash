package com.soooool.matedash.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun LeafletMapView(lat: Double, lng: Double, modifier: Modifier) {
    key(lat, lng) {
        UIKitView(
            factory = {
                MKMapView().also { mapView ->
                    val coordinate = CLLocationCoordinate2DMake(lat, lng)
                    val region = MKCoordinateRegionMakeWithDistance(coordinate, 1000.0, 1000.0)
                    mapView.setRegion(region, animated = false)
                    val annotation = MKPointAnnotation()
                    annotation.setCoordinate(coordinate)
                    mapView.addAnnotation(annotation)
                }
            },
            modifier = modifier,
        )
    }
}
