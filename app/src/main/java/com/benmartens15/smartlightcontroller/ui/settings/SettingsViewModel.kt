package com.benmartens15.smartlightcontroller.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is the settings fragment"
    }
    val text: LiveData<String> = _text
}