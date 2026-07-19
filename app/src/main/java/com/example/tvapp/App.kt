package com.example.tvapp

import android.app.Application
import com.example.tvapp.data.ConfigManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ConfigManager.init(this)
        ConfigManager.restore()
    }
}
