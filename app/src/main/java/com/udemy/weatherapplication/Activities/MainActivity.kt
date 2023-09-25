package com.udemy.weatherapplication.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.udemy.weatherapplication.Models.WeatherResponse
import com.udemy.weatherapplication.Network.WeatherService
import com.udemy.weatherapplication.R
import com.udemy.weatherapplication.utils.Constants
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

// OpenWeather Link : https://openweathermap.org/api
/**
 * The useful link or some more explanation for this app you can checkout this link :
 * https://medium.com/@sasude9/basic-android-weather-app-6a7c0855caf4
 */
class MainActivity : AppCompatActivity() {


    // A fused location client variable which is further used to get the user's current location
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    // Progress bar variable
    var mCustomProgressDialog : Dialog? = null

    // Variable for shared Preference storage
    private lateinit var mSharedPreferences : SharedPreferences

    //UI Variables
    private lateinit var tvMain : TextView
    private lateinit var ivMain : ImageView
    private lateinit var tvMainDescription : TextView
    private lateinit var tvTemp : TextView
    private lateinit var tvHumidity : TextView
    private lateinit var tvMinTemp : TextView
    private lateinit var tvMaxTemp : TextView
    private lateinit var tvSpeed : TextView
    private lateinit var tvName : TextView
    private lateinit var tvCountry : TextView
    private lateinit var tvSunriseTime : TextView
    private lateinit var tvSunsetTime : TextView

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initializes the shared preferences
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)

        // UI Views
        tvMain = findViewById(R.id.tv_main)
        ivMain = findViewById(R.id.iv_main)
        tvMainDescription = findViewById(R.id.tv_main_description)
        tvTemp = findViewById(R.id.tv_temp)
        tvHumidity = findViewById(R.id.tv_humidity)
        tvMinTemp = findViewById(R.id.tv_min)
        tvMaxTemp = findViewById(R.id.tv_max)
        tvSpeed = findViewById(R.id.tv_speed)
        tvName = findViewById(R.id.tv_name)
        tvCountry = findViewById(R.id.tv_country)
        tvSunriseTime = findViewById(R.id.tv_sunrise_time)
        tvSunsetTime = findViewById(R.id.tv_sunset_time)

        // Set UI
        setUI()


        // If location services is not automatically enabled, display message
        if (!isLocationEnabled()) {
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            // This will redirect you to settings from where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
        else {
            // Asking the location permission on runtime
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {

                            // Call the location request function here.
                            requestLocationData()
                        }

                        // If permissions are denied, show a toast
                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. It is mandatory to use this application's location features.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    // Displays Rational Dialog to request permissions if permissions are not granted
                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
        }
    }

    /**
     * A function which is used to verify that the location or GPS is enable or not of the user's device.
     */
    private fun isLocationEnabled(): Boolean {

        // This provides access to the system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)

                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri

                    startActivity(intent)

                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") {
                    dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location = locationResult.lastLocation!!
            val latitude = mLastLocation.latitude
            Log.i("Current Latitude", "$latitude")

            val longitude = mLastLocation.longitude
            Log.i("Current Longitude", "$longitude")

            // TODO (STEP 6: Pass the latitude and longitude as parameters in function)
            getLocationWeatherDetails(latitude, longitude)
        }
    }

    /**
     * Function that retrieves weather and is called in the MLocationCallBack function once
     * location is retrieved
     */
    private fun getLocationWeatherDetails(latitude: Double, longitude: Double){

        // Check if there is a internet connection for this activity using the constants function
        if(Constants.isNetworkAvailable(this@MainActivity)){

            // TODO (STEP 1: Make an api call using retrofit.)
            // START
            /**
             * Add the built-in converter factory first. This prevents overriding its
             * behavior but also ensures correct behavior when using converters that consume all types.
             */
            val retrofit: Retrofit = Retrofit.Builder()
                // API base URL.
                .baseUrl(Constants.BASE_URL)
                /** Add converter factory for serialization and deserialization of objects. */
                /**
                 * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
                 * decoding from JSON (when no charset is specified by a header) will use UTF-8.
                 */
                .addConverterFactory(GsonConverterFactory.create())
                /** Create the Retrofit instances. */
                .build()
            // END

            // TODO (STEP 5: Further step for API call)
            // START
            /**
             * Here we map the service interface in which we declares the end point and the API type
             *i.e GET, POST and so on along with the request parameter which are required.
             */
            val service: WeatherService =
                retrofit.create(WeatherService::class.java)

            /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
             * Here we pass the required param in the service
             */
            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.IMPERIAL_UNIT, Constants.APP_ID
            )

            // Displays custom progress dialog before retrieving weather data
            showProgressDialog()

            // Callback methods are executed using the Retrofit callback executor. Enqueue waits for call back
            listCall.enqueue(object : Callback<WeatherResponse> {

                @RequiresApi(Build.VERSION_CODES.N)
                @SuppressLint("SetTextI18n")
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    // Check weather the response is success or not.
                    if (response!!.isSuccessful) {

                        // Hides Progress dialog once response is successful
                        hideProgressDialog()

                        /** The de-serialized response body of a successful response. */
                        val weatherList: WeatherResponse? = response.body()

                        // converted the model class in to Json String to store it in the SharedPreferences
                        val weatherResponseJsonString = Gson().toJson(weatherList)

                        // Save the converted string to shared preferences
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()

                        // Remove the weather detail object as we will be getting
                        // the object in form of a string in the setup UI method
                        setUI()

                        Log.i("Response Result", "$weatherList")

                    } else {
                        // If the response is not success then we check the response code.
                        val responseCode = response.code()
                        when (responseCode) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }
                    }
                }
                override fun onFailure(
                    call: Call<WeatherResponse>,
                    t: Throwable) {
                    Log.e("Call Error", t!!.message.toString())
                    // Hides Progress dialog once response is unsuccessful
                    hideProgressDialog()
                }
            })
            // END
        }
        else{
            // TODO replace with code to be actioned when there is NO connection
            Toast.makeText(
                this@MainActivity,
                "You have no internet connection",
                Toast.LENGTH_SHORT
            ).show()

        }
    }

    /**
     * Function that sets refresh button
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when(item.itemId){
            R.id.action_refresh -> {
                requestLocationData()
                true
            }else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Function that will display a progress bar while weather data is being retrieved
     */
    private fun showProgressDialog() {
        mCustomProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mCustomProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        mCustomProgressDialog!!.show()
    }

    /**
     * Function is used to dismiss the progress dialog if it is visible to user.
     */
    private fun hideProgressDialog(){
        if (mCustomProgressDialog != null){
            mCustomProgressDialog!!.dismiss()
            mCustomProgressDialog = null
        }
    }

    /**
     * Function to set up UI that requires the weather list from the weather response
     * This function will take all objects created in the weather response model and display
     * them in the UI
     */
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n")
    private fun setUI(){

        /**
         * Here we get the stored response from SharedPreferences and again convert back to data object
         * to populate the data in the UI
         */
        val weatherResponseJsonString = mSharedPreferences
            .getString(Constants.WEATHER_RESPONSE_DATA, "")

        if(!weatherResponseJsonString.isNullOrEmpty()){

            val weatherList = Gson().fromJson(weatherResponseJsonString,
                WeatherResponse::class.java)

            // Iterates through the weather list
            for(i in weatherList.weather.indices){
                Log.i("Weather Name", weatherList.weather.toString())

                // Determine the unit based on the user's location
                val unit = getUnit(weatherList.sys.country)

                // Set data to UI Cards
                tvMain.text = weatherList.weather[i].main
                tvMainDescription.text = weatherList.weather[i].description
                tvHumidity.text = weatherList.main.humidity.toString() + " % RH"
                tvTemp.text = weatherList.main.temp.toString() + unit
                tvMinTemp.text = weatherList.main.temp_min.toString() + " min"
                tvMaxTemp.text = weatherList.main.temp_max.toString() + " max"
                tvSpeed.text = weatherList.wind.speed.toString()
                tvName.text = weatherList.name
                tvCountry.text = weatherList.sys.country
                tvSunriseTime.text = unixTime(weatherList.sys.sunrise.toLong())
                tvSunsetTime.text = unixTime(weatherList.sys.sunset.toLong())

                // Here we update the main icon
                when (weatherList.weather[i].icon) {
                    "01d" -> ivMain.setImageResource(R.drawable.sunny)
                    "02d" -> ivMain.setImageResource(R.drawable.cloud)
                    "03d" -> ivMain.setImageResource(R.drawable.cloud)
                    "04d" -> ivMain.setImageResource(R.drawable.cloud)
                    "04n" -> ivMain.setImageResource(R.drawable.cloud)
                    "10d" -> ivMain.setImageResource(R.drawable.rain)
                    "11d" -> ivMain.setImageResource(R.drawable.storm)
                    "13d" -> ivMain.setImageResource(R.drawable.snowflake)
                    "01n" -> ivMain.setImageResource(R.drawable.cloud)
                    "02n" -> ivMain.setImageResource(R.drawable.cloud)
                    "03n" -> ivMain.setImageResource(R.drawable.cloud)
                    "10n" -> ivMain.setImageResource(R.drawable.cloud)
                    "11n" -> ivMain.setImageResource(R.drawable.rain)
                    "13n" -> ivMain.setImageResource(R.drawable.snowflake)
                }
            }
        }
    }

    /**
     * Function to set degrees unit of either C or F
     */
    private fun getUnit(value: String): String {

        return if ("US" == value || "LR" == value || "MM" == value) {
            "°F"
        }
        else{
            "°C"
        }
    }

    /**
     * The function is used to get the formatted time based on the Format and the LOCALE we pass to it.
     */
    private fun unixTime(timex: Long): String? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat")
        val sdf = SimpleDateFormat("HH:mm")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }
}