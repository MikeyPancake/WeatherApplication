package com.udemy.weatherapplication.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants{

    // Add Open Weather Map values
    const val APP_ID : String = "2d7fdd29d2e471e42b1e49966fff5a1b"
    const val BASE_URL : String = "https://api.openweathermap.org/data/"
    const val IMPERIAL_UNIT : String = "imperial"
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA = "weather_response_data"

    // Checks for internet
    fun isNetworkAvailable(context : Context) : Boolean{

        // It answers the queries about the state of network connectivity.
        val connectivityManager = context.
        getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        // Checks if SDK is sdk vers. 23 or newer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // If network is not available, return false
            val network = connectivityManager.activeNetwork ?: return false
            // If active network is not connected, return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            // If active network has Wifi, Cellular, or ethernet connection, return false
            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true

                // If active network does not have any of the three capabilities, return false
                else -> false
            }
        }
        else{
            // Old method of checking SDK that checks vers. 23 or older
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }

    }

}