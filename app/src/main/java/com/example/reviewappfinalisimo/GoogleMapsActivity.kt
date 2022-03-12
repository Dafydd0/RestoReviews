package com.example.reviewappfinalisimo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.reviewappfinalisimo.Common.Common
import com.example.reviewappfinalisimo.Model.MyPlaces
import com.example.reviewappfinalisimo.Remote.IGoogleAPIService
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.SphericalUtil
import kotlinx.android.synthetic.main.activity_google_maps.*
import kotlinx.android.synthetic.main.dialog_view.view.*
import retrofit2.Call
import retrofit2.Response

class GoogleMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private var latitude: Double = 0.toDouble()
    private var longitude: Double = 0.toDouble()

    private lateinit var mLastLocation: Location
    private lateinit var latLng: LatLng
    private var mMarker: Marker? = null
    private lateinit var email: String
    private var displayWithoutZoom = false

    //Location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    private lateinit var placesClient: PlacesClient
    private val browserKey = "YOUR_API_KEY"
    private val apiKey = "YOUR_API_KEY"


    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }


    private lateinit var mService: IGoogleAPIService

    lateinit var currentPlace: MyPlaces

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_maps)
        createFragment()

        //Get the different values which we will need to work properly
        val bundle: Bundle? = intent.extras
        email = bundle?.getString("email").toString()
        displayWithoutZoom = bundle?.getBoolean("displayWithoutZoom")!!

        //Init service
        mService = Common.googleApiService


        //To check is the user has an username or not
        fetchUser(email)
        { result ->
            if (!result) {
                showAlertProfile()
            }
        }

        //Request runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationPermission()) {

                buildLocationRequest()
                buildLocationCallBack()

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallBack,
                    Looper.myLooper()
                )
            } else {
                buildLocationRequest()
                buildLocationCallBack()

                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallBack,
                    Looper.myLooper()
                )
            }

            //This part of code is for Google Place Autocomplete API, which will autocomplete with places at 1km around
            // Setup Places Client
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }
            placesClient = Places.createClient(this)

            val autocompleteFragment =
                supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                        as AutocompleteSupportFragment             // Initialize the AutocompleteSupportFragment

            if (isLocationPermissionGranted()) {

                fetchLocation { result ->
                    latitude = result.latitude
                    longitude = result.longitude

                    val bound = buildRectangleBounds(LatLng(latitude, longitude), 1000.0)

                    // Specify the types of place data to return.
                    autocompleteFragment.setPlaceFields(listOf(Place.Field.ID,
                        Place.Field.LAT_LNG,
                        Place.Field.NAME)).setLocationBias(bound)

                    // Set up a PlaceSelectionListener to handle the response.
                    autocompleteFragment.setOnPlaceSelectedListener(
                        object : PlaceSelectionListener {
                            override fun onPlaceSelected(place: Place) {
                                nearByJustOne(place)
                            }

                            override fun onError(status: Status) {
                            }
                        })
                }
            } else {

                autocompleteFragment.setPlaceFields(listOf(Place.Field.ID,
                    Place.Field.LAT_LNG,
                    Place.Field.NAME))

                // Set up a PlaceSelectionListener to handle the response.
                autocompleteFragment.setOnPlaceSelectedListener(
                    object : PlaceSelectionListener {
                        override fun onPlaceSelected(place: Place) {
                            nearByJustOne(place)
                        }

                        override fun onError(status: Status) {
                        }
                    })
            }

            //Set the actions to do when clicking on the navigation bar
            bottom_navigation_view.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.action_restaurant -> nearByPlace("restaurant")
                    R.id.action_reviews -> {
                        if (email == "guest_email") {
                            showAlertAuth()
                        } else {
                            val intent =
                                Intent(this, UserReviewsRecyclerViewActivity::class.java).apply {
                                    putExtra("email", email)
                                }
                            startActivity(intent)
                        }
                    }
                    R.id.action_bookmarks -> {
                        if (email == "guest_email") {
                            showAlertAuth()
                        } else {
                            val intent =
                                Intent(this, UserBookmarksRecyclerViewActivity::class.java).apply {
                                    putExtra("email", email)
                                }
                            startActivity(intent)
                        }
                    }
                    R.id.action_profile -> {
                        if (email == "guest_email") {
                            showAlertAuth()
                        } else {
                            val intent =
                                Intent(this, ProfileActivity::class.java).apply {
                                    putExtra("email", email)
                                }
                            startActivity(intent)
                        }
                    }

                }
                true
            }
        }
    }

    //If the activity resumes we want that everything still works
    override fun onResume() {
        super.onResume()

        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment)
                    as AutocompleteSupportFragment

        if (isLocationPermissionGranted()) {

            fetchLocation { result ->
                latitude = result.latitude
                longitude = result.longitude

                val bound = buildRectangleBounds(LatLng(latitude, longitude), 1000.0)

                // Specify the types of place data to return.
                autocompleteFragment.setPlaceFields(listOf(Place.Field.ID,
                    Place.Field.LAT_LNG,
                    Place.Field.NAME)).setLocationBias(bound)

                // Set up a PlaceSelectionListener to handle the response.
                autocompleteFragment.setOnPlaceSelectedListener(
                    object : PlaceSelectionListener {
                        override fun onPlaceSelected(place: Place) {
                            nearByJustOne(place)
                        }

                        override fun onError(status: Status) {
                        }
                    })
            }
        } else {

            autocompleteFragment.setPlaceFields(listOf(Place.Field.ID,
                Place.Field.LAT_LNG,
                Place.Field.NAME))

            // Set up a PlaceSelectionListener to handle the response.
            autocompleteFragment.setOnPlaceSelectedListener(
                object : PlaceSelectionListener {
                    override fun onPlaceSelected(place: Place) {
                        nearByJustOne(place)
                    }

                    override fun onError(status: Status) {
                    }
                })
        }
    }

    private fun checkLocationPermission(): Boolean {
        var toReturn = false
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION
                )

            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE_LOCATION
                )
                toReturn = false
            }
        } else {
            toReturn = true
        }
        return toReturn
    }


    private fun buildLocationCallBack() {
        locationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                mLastLocation =
                    locationResult.locations[locationResult.locations.size - 1] //Get last location

                if (mMarker != null) {
                    mMarker!!.remove()
                }
                for (location in locationResult.locations) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    latLng = LatLng(latitude, longitude)
                }
                latitude = mLastLocation.latitude
                longitude = mLastLocation.longitude
            }
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10f
    }

    //Create map
    private fun createFragment() {
        val mapFragment: SupportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    //When map is ready, enable location, check permissions and if a marker is clicked, go to the ViewPlaceActivity of that place
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMyLocationButtonEnabled = false
        enableLocation()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
            }
        } else {
            mMap.isMyLocationEnabled = true
        }

        //Edit initial zoom and makes zoom to current position
        fetchLocation { result ->
            latitude = result.latitude
            longitude = result.longitude
        }

        //Make event click on Marker
        mMap.setOnMarkerClickListener { marker ->

            var i = 0
            var indexFind = 0

            for (x in currentPlace.results!!) {
                if (x.name.toString() == marker.title.toString()) {
                    indexFind = i
                }
                ++i
            }

            //Store data of place which will be used vi ViewPlaceActivity
            Common.currentResult = currentPlace.results!![indexFind]

            //Different parameters will be passed depending if the user has the place in bookmarks or not
            fetchData(marker.title.toString()) { result ->
                if (result) {
                    val intent = Intent(this, ViewPlaceActivity::class.java).apply {
                        putExtra("email", email)
                        putExtra("calledByReview", true)
                        putExtra("bookmark", userBookmark!!.bookmark)
                    }
                    startActivity(intent)
                } else {
                    val intent = Intent(this, ViewPlaceActivity::class.java).apply {
                        putExtra("email", email)
                        putExtra("calledByReview", true)
                    }
                    startActivity(intent)
                }
            }
            true
        }
    }

    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (!::mMap.isInitialized) return
        if (isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = false
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
        ) { //Has already rejected the permissions
            Toast.makeText(this,
                "Please, go to settings and accept the permissions",
                Toast.LENGTH_SHORT)
                .show()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION)
        }
    }

    @SuppressLint("MissingSuperCall", "MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        when (requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
            } else {
                Toast.makeText(
                    this,
                    "Please, go to settings and accept the permissions",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
            }
        }
    }


    @SuppressLint("MissingPermission")
    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::mMap.isInitialized) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
            }
        } else {
            mMap.isMyLocationEnabled = true
        }
        if (autocomplete_fragment.view?.equals("") == true) {
            if (!isLocationPermissionGranted()) {
                mMap.isMyLocationEnabled = false
                Toast.makeText(
                    this,
                    "Please, go to settings and accept the permissions",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    //Check if an user has a place in bookmarks or not
    private var userBookmark: UserBookmark? = null
    private fun fetchData(restaurantName: String, callback: (Boolean) -> Unit) {
        var exist = false
        FirebaseFirestore.getInstance().collection("user_bookmark")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if (document.id.contains(email) && document.id.contains(restaurantName)) {
                        userBookmark = document.toObject(UserBookmark::class.java)
                        exist = true
                    }
                }
                callback.invoke(exist)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error trying to fetch data", Toast.LENGTH_LONG).show()
            }
    }

    //Check if an user has an username or not
    private fun fetchUser(
        email: String,
        callback: (Boolean) -> Unit,
    ) {
        var toReturn = false
        FirebaseFirestore.getInstance().collection("user")
            .get()
            .addOnSuccessListener { result ->
                if (email != "guest_email") {
                    if (!result.isEmpty) {
                        for (document in result) {
                            if (document.id.contains(email)) {
                                toReturn = true
                            }
                        }
                        if (toReturn) { //The user has already an username
                            callback.invoke(true)
                        } else { //The user doesn't have an username yet
                            callback.invoke(false)
                        }
                    } else { //If there isn't "user" collection means that the user hasn't an username yet
                        callback.invoke(false)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error trying to fetch user", Toast.LENGTH_LONG).show()
            }
    }

    private fun showAlertAuth() {

        val view = View.inflate(this, R.layout.dialog_view, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)


        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAlertProfile() {

        val view = View.inflate(this, R.layout.dialog_view_profile, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)


        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("email", email)
            }
            startActivity(intent)
        }
    }

    private fun showAlertNoResponse() {

        val view = View.inflate(this, R.layout.dialog_view_error_noresponse, null)

        val builder = AlertDialog.Builder(this)
        builder.setView(view)


        val dialog: AlertDialog = builder.create()
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        view.btn_confirm.setOnClickListener {
            dialog.dismiss()
        }
    }

    //Fetch user location and move the camera there with or without zoom
    private fun fetchLocation(callback: (LatLng) -> Unit) {
        val task = fusedLocationProviderClient.lastLocation
        var latLng: LatLng

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
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
            return
        }
        task.addOnCompleteListener {
            if (it != null) {
                latLng = LatLng(it.result.latitude, it.result.longitude)

                if (!displayWithoutZoom) { //Called when the app runs for first time
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latLng, 15.5f
                        ), 3000, null
                    )
                } else { //Called when an activity has been posted
                    mMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            latLng, 15.5f
                        ), 1, null
                    )
                }
                callback.invoke(latLng)
            }
        }
    }

    //Create a rectangle for the autocomplete google places
    private fun buildRectangleBounds(from: LatLng, distance: Double): RectangularBounds {
        val southWest = SphericalUtil.computeOffset(from, distance, 225.0)
        val northEast = SphericalUtil.computeOffset(from, distance, 45.0)

        return RectangularBounds.newInstance(southWest, northEast)
    }

    //Search a place when is searched by the searching bar and move camera there
    private fun nearByJustOne(place: Place) {

        val url = getUrlJustOne(place.latLng.latitude, place.latLng.longitude)
        var checkIfNoResults = 0
        mMap.clear()

        mService.getNearbyPlaces(url).enqueue(object : retrofit2.Callback<MyPlaces> {
            override fun onResponse(call: Call<MyPlaces>?, response: Response<MyPlaces>?) {
                currentPlace = response!!.body()!!

                if (response.isSuccessful) {
                    for (element in response.body()!!.results!!) {
                        if (element.name.toString() == place.name.toString()) {
                            val markerOptions = MarkerOptions()
                            val lat = element.geometry!!.location!!.lat
                            val lng = element.geometry!!.location!!.lng
                            val placeName = element.name
                            val latLng = LatLng(lat, lng)

                            markerOptions.position(latLng)
                            markerOptions.title(placeName)

                            //Add marker to map
                            checkIfNoResults++

                            mMap.addMarker(markerOptions)
                            mMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    latLng, 15.5f
                                ), 3000, null
                            )
                        }
                    }
                }
                if (checkIfNoResults == 0) {
                    showAlertNoResponse()
                }
            }

            override fun onFailure(call: Call<MyPlaces>, t: Throwable) {
                Toast.makeText(baseContext, "" + t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    //Find nearby places
    private fun nearByPlace(typePlace: String) {
        //Clear all marker on mMap
        if (checkLocationPermission()) {
            fetchLocation { result ->
                latitude = result.latitude
                longitude = result.longitude
            }

            mMap.clear()

            //Build URL request base on location
            val url = getUrl(latitude, longitude, typePlace)

            mService.getNearbyPlaces(url).enqueue(object : retrofit2.Callback<MyPlaces> {
                override fun onResponse(call: Call<MyPlaces>?, response: Response<MyPlaces>?) {
                    currentPlace = response!!.body()!!

                    if (response.isSuccessful) {
                        for (element in response.body()!!.results!!) {
                            val markerOptions = MarkerOptions()
                            val lat = element.geometry!!.location!!.lat
                            val lng = element.geometry!!.location!!.lng
                            val placeName = element.name
                            val latLng = LatLng(lat, lng)

                            markerOptions.position(latLng)
                            markerOptions.title(placeName)

                            //Add marker to map
                            mMap.addMarker(markerOptions)
                            //Move camera
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLng(
                                    LatLng(
                                        latitude,
                                        longitude
                                    )
                                )
                            ) //Makes zoom to current position
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(15.5f))
                        }
                    }
                }

                override fun onFailure(call: Call<MyPlaces>, t: Throwable) {
                    Toast.makeText(baseContext, "" + t.message, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    //Get URL request to Google Places API radius = 5m
    private fun getUrlJustOne(latitude: Double, longitude: Double): String {
        val googlePlaceUrl =
            StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")

        googlePlaceUrl.append("?location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=5") //5m
        googlePlaceUrl.append("&type=restaurant")
        googlePlaceUrl.append("&key=${browserKey}")

        return googlePlaceUrl.toString()
    }

    //Get URL request to Google Places API  radius = 100m
    private fun getUrl(latitude: Double, longitude: Double, typePlace: String): String {
        val googlePlaceUrl =
            StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")

        googlePlaceUrl.append("?location=$latitude,$longitude")
        googlePlaceUrl.append("&radius=500") //500m
        googlePlaceUrl.append("&type=$typePlace")
        googlePlaceUrl.append("&key=${browserKey}")

        return googlePlaceUrl.toString()
    }
}
