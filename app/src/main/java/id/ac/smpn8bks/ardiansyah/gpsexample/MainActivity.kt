package id.ac.smpn8bks.ardiansyah.gpsexample

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.method.ScrollingMovementMethod
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import id.ac.smpn8bks.ardiansyah.gpsexample.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private var trackingLocation = false

    private val TAG = "MainActivityGPS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvLocation.text = ""
        binding.tvLocation.movementMethod = ScrollingMovementMethod()

        // ============================================================
        // 1. REQUEST PERMISSIONS
        // ============================================================
        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {}
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {}
                else -> binding.btnUpdate.isEnabled = false
            }
        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // ============================================================
        // 2. LOCATION REQUEST SETTINGS
        // ============================================================
        val builder = LocationSettingsRequest.Builder()
        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {

            locationRequest = LocationRequest.create().apply {
                interval = TimeUnit.SECONDS.toMillis(20)
                fastestInterval = TimeUnit.SECONDS.toMillis(10)
                maxWaitTime = TimeUnit.SECONDS.toMillis(40)
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    if (locationResult.locations.isNotEmpty()) {
                        val location: Location = locationResult.lastLocation!!
                        logResults("${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this@MainActivity, 100)
                } catch (_: IntentSender.SendIntentException) { }
            }
        }

        // ============================================================
        // 3. GET LAST KNOWN LOCATION
        // ============================================================
        if (checkPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    logResults("${location?.latitude}, ${location?.longitude}")
                }
        }

        // ============================================================
        // 4. BUTTON START / STOP GPS UPDATES
        // ============================================================
        binding.btnUpdate.setOnClickListener {
            if (!trackingLocation) {
                startLocationUpdates()
            } else {
                stopLocationUpdates()
            }
            trackingLocation = !trackingLocation
            updateButton()
        }
    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun logResults(msg: String) {
        binding.tvLocation.text = "$msg\n${binding.tvLocation.text}"
    }

    private fun updateButton() {
        binding.btnUpdate.text =
            if (trackingLocation) getString(R.string.stop_update)
            else getString(R.string.start_update)
    }

    // ============================================================
    // 5. START GPS UPDATE
    // ============================================================
    private fun startLocationUpdates() {
        if (!checkPermission()) return

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        Log.d(TAG, "Location Updates Started")
    }

    // ============================================================
    // 6. STOP GPS UPDATE
    // ============================================================
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnCompleteListener {
                Log.d(TAG, "Location Updates Stopped")
            }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
}
