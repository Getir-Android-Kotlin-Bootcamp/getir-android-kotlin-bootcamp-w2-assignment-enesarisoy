package com.ns.foodcouriers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.textfield.TextInputLayout
import com.ns.foodcouriers.presentation.chat.ChatFragment
import com.ns.foodcouriers.presentation.location.LocationFragment

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var mMap: GoogleMap
    private lateinit var autoCompleteFragment: AutocompleteSupportFragment
    private lateinit var tvAddress: TextView

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvAddress = findViewById(R.id.tvAddress)

        // Initialize places api with api key of course :)
        Places.initialize(this, BuildConfig.MAPS_API_KEY)

        // So, we need to use AutoCompleteFragment for searching places
        autoCompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autoCompleteFragment) as AutocompleteSupportFragment

        // To display the map on the screen
        val mapFragment: SupportMapFragment =
            supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Need for get the current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        searchLocation()

    }

    private fun searchLocation() {
        autoCompleteFragment.setHint("Where is your location")

        // TODO icon problem
        autoCompleteFragment.requireActivity()
            .findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_button)?.visibility =
            View.GONE


        autoCompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )

        // Triggers when selecting a place
        autoCompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                Toast.makeText(this@MainActivity, "Some Error in Search", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(it, 5f)
                    mMap.animateCamera(newLatLngZoom)
                    placeMarkerOnMap(it)
                }
            }
        })
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMap()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupMap()
            } else {
                Log.d("Main Activity", "İzin reddildi")
            }
        }
    }

    private fun setupMap() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mMap.isMyLocationEnabled = true

        // Get current location info
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {

                currentLocation = location

                // Well, we need to get addresses to display on UI right??
                val geocoder = Geocoder(this@MainActivity)
                val addresses: MutableList<Address>? =
                    geocoder.getFromLocation(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        1
                    )

                addresses?.let {
                    if (it.isNotEmpty()) {
                        val address = addresses[0]
                        val addressText = address.getAddressLine(0)
                        tvAddress.text = addressText

                        Log.d("Main Activity", "Current Location: $address")
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No address found",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }

                // Mark the location
                placeMarkerOnMap(LatLng(location.latitude, location.longitude))
            }
        }
    }

    private fun placeMarkerOnMap(currentLatLong: LatLng) {

        mMap.clear()

        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("Current Location")
        mMap.addMarker(markerOptions)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, 5f))
    }


}