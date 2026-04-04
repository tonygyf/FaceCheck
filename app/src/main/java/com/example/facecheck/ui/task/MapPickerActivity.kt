package com.example.facecheck.ui.task

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.amap.api.maps2d.AMap
import com.amap.api.maps2d.CameraUpdateFactory
import com.amap.api.maps2d.MapView
import com.amap.api.maps2d.model.LatLng
import com.amap.api.maps2d.model.MarkerOptions
import com.amap.api.maps2d.model.CircleOptions
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.GeocodeResult
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.example.facecheck.ui.theme.FaceCheckTheme
import android.widget.Toast
import com.google.android.gms.location.LocationServices

class MapPickerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val readonly = intent.getBooleanExtra("readonly", false)
        val presetLat = intent.getDoubleExtra("preset_lat", Double.NaN)
        val presetLng = intent.getDoubleExtra("preset_lng", Double.NaN)
        val presetRadius = intent.getIntExtra("preset_radius_m", -1)
        setContent {
            FaceCheckTheme {
                MapPickerScreen(onLocationSelected = { lat, lng, address ->
                    val result = Intent().apply {
                        putExtra("latitude", lat)
                        putExtra("longitude", lng)
                        putExtra("address", address)
                    }
                    setResult(Activity.RESULT_OK, result)
                    finish()
                }, onBack = { finish() }, readonly = readonly, presetLat = presetLat, presetLng = presetLng, presetRadiusM = presetRadius)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    onLocationSelected: (Double, Double, String) -> Unit,
    onBack: () -> Unit,
    readonly: Boolean = false,
    presetLat: Double = Double.NaN,
    presetLng: Double = Double.NaN,
    presetRadiusM: Int = -1
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var aMap by remember { mutableStateOf<AMap?>(null) }
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val defaultLatLng = remember { LatLng(39.90923, 116.397428) }

    val moveToCurrentLocation: () -> Unit = {
        try {
            val mapLocation = aMap?.myLocation
            if (mapLocation != null) {
                val currentLatLng = LatLng(mapLocation.latitude, mapLocation.longitude)
                aMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        aMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                    } else {
                        Toast.makeText(context, "暂未获取到当前位置", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "缺少定位权限", Toast.LENGTH_SHORT).show()
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            aMap?.isMyLocationEnabled = true
            moveToCurrentLocation()
        } else {
            Toast.makeText(context, "未授予定位权限，无法定位到当前位置", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(Bundle())
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(aMap) {
        aMap = mapView.map
        aMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 12f))
        aMap?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isMyLocationButtonEnabled = true
            isCompassEnabled = true
            isScaleControlsEnabled = true
        }

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                if (!readonly) {
                    aMap?.isMyLocationEnabled = true
                }
                if (!readonly) {
                    moveToCurrentLocation()
                }
            }
            else -> {
                if (!readonly) {
                    requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }

        if (readonly && !presetLat.isNaN() && !presetLng.isNaN()) {
            val target = LatLng(presetLat, presetLng)
            selectedLatLng = target
            selectedAddress = "签到范围中心点"
            aMap?.clear()
            aMap?.addMarker(MarkerOptions().position(target))
            if (presetRadiusM > 0) {
                aMap?.addCircle(
                    CircleOptions()
                        .center(target)
                        .radius(presetRadiusM.toDouble())
                        .strokeWidth(3f)
                        .strokeColor(0x552196F3)
                        .fillColor(0x222196F3)
                )
            }
            aMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(target, if (presetRadiusM > 0) 16f else 15f))
            return@LaunchedEffect
        }

        aMap?.setOnMapClickListener { latLng ->
            if (readonly) return@setOnMapClickListener
            isLoading = true
            selectedLatLng = latLng
            aMap?.clear()
            aMap?.addMarker(MarkerOptions().position(latLng))

            val geocoderSearch = GeocodeSearch(context)
            geocoderSearch.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                override fun onRegeocodeSearched(result: RegeocodeResult?, rCode: Int) {
                    isLoading = false
                    selectedAddress = if (rCode == 1000) {
                        result?.regeocodeAddress?.formatAddress
                    } else {
                        "无法获取地址"
                    }
                }

                override fun onGeocodeSearched(result: GeocodeResult?, rCode: Int) {}
            })
            val query = RegeocodeQuery(LatLonPoint(latLng.latitude, latLng.longitude), 200f, GeocodeSearch.AMAP)
            geocoderSearch.getFromLocationAsyn(query)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择签到位置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView({ mapView })
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                SmallFloatingActionButton(onClick = {
                    if (readonly) return@SmallFloatingActionButton
                    when (PackageManager.PERMISSION_GRANTED) {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                            moveToCurrentLocation()
                        }
                        else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }) {
                    Icon(Icons.Default.LocationOn, contentDescription = "定位到当前位置")
                }
                if (!readonly && selectedLatLng != null && !isLoading) {
                    FloatingActionButton(onClick = {
                        onLocationSelected(
                            selectedLatLng!!.latitude,
                            selectedLatLng!!.longitude,
                            selectedAddress ?: "未知位置"
                        )
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "确定")
                    }
                }
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
