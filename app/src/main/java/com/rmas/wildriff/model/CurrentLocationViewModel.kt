package com.rmas.wildriff.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CurrentLocationViewModel:ViewModel() {
    private val _currLongitude = MutableLiveData<Double>()
    val currLongitude : LiveData<Double>
        get()=_currLongitude
    private val _currLatitude = MutableLiveData<Double>()
    val currLatitude : LiveData<Double>
        get()=_currLatitude

    var setLocation:Boolean=false

    fun setLocation(lat:Double,lon:Double){
        _currLongitude.value = lon
        _currLatitude.value = lat
        setLocation = false
    }
}