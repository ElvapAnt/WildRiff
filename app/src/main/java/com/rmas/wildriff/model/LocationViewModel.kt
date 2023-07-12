package com.rmas.wildriff.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LocationViewModel: ViewModel() {
    private val _longitude = MutableLiveData<Double>()
    val longitude : LiveData<Double>get()=_longitude
    private val _latitude = MutableLiveData<Double>()
    val latitude : LiveData<Double>get()=_latitude

    var setLocation:Boolean=false

    fun setLocation(lat:Double,lon:Double){
        _longitude.value = lon
        _latitude.value = lat
        setLocation = false
    }

}