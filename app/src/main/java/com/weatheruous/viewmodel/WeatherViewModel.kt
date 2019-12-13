package com.weatheruous.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.weatheruous.data.WeatherAPI
import com.weatheruous.data.WeatherDatabaseDao
import com.weatheruous.data.WeatherUrls
import com.weatheruous.model.WeatherData
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DateFormat

class WeatherViewModel(
    val database: WeatherDatabaseDao,
    application: Application
) : AndroidViewModel(application) {

    private val _currentTemp = MutableLiveData<String>()
    private val _currentTempHi = MutableLiveData<String>()
    private val _currentTempLow = MutableLiveData<String>()
    private val _refreshed = MutableLiveData<String>()
    private val _conditions = MutableLiveData<String>()
    private val _icon = MutableLiveData<String>()

    //  Jobs
    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private val urls = MutableLiveData<WeatherUrls?>()

    val currentTemperature: LiveData<String>
        get() = _currentTemp

    val currentHi: LiveData<String>
        get() = _currentTempHi

    val currentLow: LiveData<String>
        get() = _currentTempLow

    val refreshed: LiveData<String>
        get() = _refreshed

    val conditions: LiveData<String>
        get() = _conditions

    init {
        Log.i("WeatherViewModel", "Weather ViewModel created.")
        getWeatherData()
        getCurrentWeather()
        generateUpdatedTime()
    }

    private suspend fun getUrlsFromDatabase(): WeatherUrls? {
        return withContext(Dispatchers.IO) {
            var weatherUrls = database.getWeatherUrls()
            weatherUrls
        }
    }

    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

    private suspend fun update(location: WeatherUrls) {
        withContext(Dispatchers.IO) {
            database.update(location)
        }
    }

    private suspend fun insert(location: WeatherUrls) {
        withContext(Dispatchers.IO) {
            database.insert(location)
        }
    }

    fun onSaveLocation() {
        uiScope.launch {
            val newLocation = WeatherUrls()
            insert(newLocation)
            urls.value = getUrlsFromDatabase()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun refreshData() {
        getCurrentWeather()
        getWeatherData()
        generateUpdatedTime()
        uiScope.launch { urls.value = getUrlsFromDatabase() }
            .also { Log.i("What????", urls.value.toString()) }
    }

    fun generateUpdatedTime() {
        val currentTimestamp = System.currentTimeMillis()
        val formatted = DateFormat.getTimeInstance().format(currentTimestamp)

        _refreshed.value = "refreshed at: $formatted"
    }

    private fun getWeatherData() {
        WeatherAPI.retrofitService.getCurrentWeather().enqueue(object : Callback<WeatherData> {
            override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                Log.i("WeatherViewModel", "Calling failure response on getting current weather.")
                //  todo handle failure
            }

            override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {

                _currentTemp.value =
                    response.body()?.properties?.periods?.get(0)?.temperature.toString()
                _conditions.value =
                    response.body()?.properties?.periods?.get(0)?.shortForecast.toString()
            }
        })
    }

    private fun getCurrentWeather() {
        WeatherAPI.retrofitService.getWeatherForecast().enqueue(object : Callback<WeatherData> {
            override fun onFailure(call: Call<WeatherData>, t: Throwable) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onResponse(call: Call<WeatherData>, response: Response<WeatherData>) {

                _currentTempHi.value =
                    response.body()?.properties?.periods?.get(0)?.temperature.toString()
                _currentTempLow.value =
                    response.body()?.properties?.periods?.get(1)?.temperature.toString()
            }
        })
    }
}