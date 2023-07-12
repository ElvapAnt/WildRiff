package com.rmas.wildriff.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rmas.wildriff.data.Riff

class SelectedRiffViewModel:ViewModel() {

    private val _selectedRiff = MutableLiveData<Riff>()
    val selectedRiff : LiveData<Riff>get() = _selectedRiff

    fun setRiff(riff:Riff){
        _selectedRiff.value = riff
    }
}