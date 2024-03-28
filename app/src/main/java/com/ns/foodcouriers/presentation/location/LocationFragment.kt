package com.ns.foodcouriers.presentation.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.ns.foodcouriers.BuildConfig
import com.ns.foodcouriers.R
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignTop


class LocationFragment : Fragment(), OnMapReadyCallback {

    // The entry point to the Fused Location Provider.
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var mMap: GoogleMap
    private lateinit var tvAddress: TextView
    private lateinit var autocompleteSupportFragment: AutocompleteSupportFragment

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Places.initialize(requireContext(), BuildConfig.MAPS_API_KEY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Checking the permissions before the view created.
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    "android.permission.ACCESS_COARSE_LOCATION",
                    "android.permission.ACCESS_FINE_LOCATION"
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        return inflater.inflate(R.layout.fragment_location, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvAddress = view.findViewById(R.id.tvAddress)
        autocompleteSupportFragment =
            childFragmentManager.findFragmentById(R.id.autoCompleteFragment) as AutocompleteSupportFragment

        autocompleteSupportFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )
        autocompleteSupportFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                Toast.makeText(requireContext(), "Some error", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    tvAddress.text = place.address
                    placeMarkerOnMap(it, mMap.cameraPosition.zoom)
                }
            }
        })

        // To display the map on the screen
        val mapFragment: SupportMapFragment =
            childFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // We need for get the current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        onClickMarker(view)

        customizeAutoCompleteSupportFragmentComponents()
    }

    private fun customizeAutoCompleteSupportFragmentComponents() {
        autocompleteSupportFragment.requireView()
            .findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_button)?.visibility =
            View.GONE
        autocompleteSupportFragment.requireView()
            .findViewById<EditText>(com.google.android.libraries.places.R.id.places_autocomplete_search_input)
            ?.setHintTextColor(Color.BLACK)


        autocompleteSupportFragment.requireView()
            .findViewById<EditText>(com.google.android.libraries.places.R.id.places_autocomplete_search_input)
            ?.setHintTextColor(Color.BLACK)

        val typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_black)

        autocompleteSupportFragment.requireView()
            .findViewById<EditText>(com.google.android.libraries.places.R.id.places_autocomplete_search_input)
            ?.setTypeface(typeface)

        autocompleteSupportFragment.requireView()
            .findViewById<EditText>(com.google.android.libraries.places.R.id.places_autocomplete_search_input)
            ?.textSize = 18f


    }


    private fun setAddressToTextView(addresses: List<Address>?) {
        addresses?.let {
            if (it.isNotEmpty()) {
                val address = addresses[0]
                val addressText = address.getAddressLine(0)
                tvAddress.text = addressText

                Log.d("Main Activity", "Current Location: $address")
            } else {
                Toast.makeText(
                    requireContext(),
                    "No address found",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setupMap()
    }

    private fun setupMap() {

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mMap.isMyLocationEnabled = false


        getLocation()
        // Set marker to map when we click
        mMap.setOnMapLongClickListener {
            val geocoder = Geocoder(requireContext())
            val currentAddress: MutableList<Address>? =
                geocoder.getFromLocation(
                    it.latitude,
                    it.longitude,
                    1
                )
            placeMarkerOnMap(it, mMap.cameraPosition.zoom)
            setAddressToTextView(currentAddress)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(isFirst: Boolean = true) {
        // Get current location info
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {

                currentLocation = location

                // Well, we need to get addresses to display on UI right??
                val geocoder = Geocoder(requireContext())

                // getFromLocation is deprecated but I didn't change it because it requires
                // higher API level.
                val addresses: MutableList<Address>? =
                    geocoder.getFromLocation(
                        currentLocation.latitude,
                        currentLocation.longitude,
                        1
                    )

                // We're getting the address of the current location
                addresses?.let {
                    if (it.isNotEmpty()) {
                        val address = addresses[0]
                        val addressText = address.getAddressLine(0)
                        tvAddress.text = addressText

                        Log.d("Main Activity", "Current Location: $address")
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No address found",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }

                // Mark the location
                if (isFirst) {
                    placeMarkerOnMap(LatLng(location.latitude, location.longitude))
                } else {
                    placeMarkerOnMap(LatLng(location.latitude, location.longitude), zoomLevel = 12f)

                }
            }
        }
    }

    private fun onClickMarker(view: View) {
        val ivMarkerCard = view.findViewById<ImageView>(R.id.ivMarkerCard)
        val balloon = Balloon.Builder(requireContext())
            .setWidth(BalloonSizeSpec.WRAP)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("Click to see your location!")
            .setTextColorResource(R.color.black)
            .setTextSize(15f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(10)
            .setArrowPosition(0.5f)
            .setArrowColorResource(R.color.arrow_color)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundDrawableResource(R.drawable.indicator_gradient)
            .setBalloonAnimation(BalloonAnimation.ELASTIC)
            .build()
        ivMarkerCard.showAlignTop(balloon)
        ivMarkerCard.setOnClickListener {
            getLocation(isFirst = false)
        }
    }

    private fun placeMarkerOnMap(currentLatLong: LatLng, zoomLevel: Float = 5f) {

        mMap.clear()

        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("Current Location")
        mMap.addMarker(markerOptions)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, zoomLevel))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("onRequestPermissionsResult", "permission granted")

                getLocation()
            } else {
                Log.d(
                    "onRequestPermissionsResult",
                    "YOOOOU DIDN'TTTT GIVE A PERMISSION. YOOOU SHALL NOT PASSSSS!!!!"
                )
            }
        }
    }
}


