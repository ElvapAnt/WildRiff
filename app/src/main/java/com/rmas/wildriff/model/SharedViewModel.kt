package com.rmas.wildriff.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel:ViewModel() {
    val _userId: MutableLiveData<String> = MutableLiveData()
    var userId:LiveData<String> = _userId

    fun setUserID(id:String){
        _userId.value = id
    }
}