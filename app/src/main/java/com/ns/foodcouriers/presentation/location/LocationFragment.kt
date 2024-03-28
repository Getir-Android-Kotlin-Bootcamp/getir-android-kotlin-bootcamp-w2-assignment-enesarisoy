package com.ns.foodcouriers.presentation.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ns.foodcouriers.R


class LocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var mMap: GoogleMap
    private lateinit var tvAddress: TextView

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvAddress = view.findViewById(R.id.tvAddress)
        val searchView = view.findViewById<SearchView>(R.id.searchView)

        val searchEditText =
            searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)

        // Change the color of the editText inside searchView
        val textColor = ContextCompat.getColor(requireContext(), R.color.black)
        searchEditText.setTextColor(textColor)

        val hintTextColor = ContextCompat.getColor(requireContext(), R.color.black)
        searchEditText.setHintTextColor(hintTextColor)

        // Search the place
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                val searchText = query.toString()
                searchPlace(searchText, requireContext())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        // To display the map on the screen
        val mapFragment: SupportMapFragment =
            childFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // We need for get the current location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())


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

        mMap.isMyLocationEnabled = true

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
                placeMarkerOnMap(LatLng(location.latitude, location.longitude))
            }
        }

        // Set marker to map when we click
        mMap.setOnMapLongClickListener {
            placeMarkerOnMap(it, mMap.cameraPosition.zoom)
        }
    }

    private fun placeMarkerOnMap(currentLatLong: LatLng, zoomLevel: Float = 5f) {

        mMap.clear()

        val markerOptions = MarkerOptions().position(currentLatLong)
        markerOptions.title("Current Location")
        mMap.addMarker(markerOptions)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLong, zoomLevel))
    }

    private fun searchPlace(query: String, context: Context) {
        Log.d("Fragment Location", "search: $query")

        var addressList: List<Address>? = null

        val geocoder = Geocoder(context)

        try {
            // As I said, getFromLocation is deprecated but I didn't change it because it requires
            // higher API level.
            addressList = geocoder.getFromLocationName(query, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val address = addressList?.get(0)
        address?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            placeMarkerOnMap(latLng)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                setupMap()
            } else {
                Log.d(
                    "Main Activity",
                    "YOOOOU DIDN'TTTT GIVE A PERMISSION. YOOOU SHALL NOT PASSSSS!!!!"
                )
            }
        }
    }
}



