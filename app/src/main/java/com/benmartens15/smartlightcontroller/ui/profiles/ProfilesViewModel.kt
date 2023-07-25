package com.benmartens15.smartlightcontroller.ui.profiles

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfilesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is the profiles fragment"
    }
    val text: LiveData<String> = _text
}