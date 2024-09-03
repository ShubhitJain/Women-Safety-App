package com.shubhit.womensafetyapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shubhit.womensafetyapp.databinding.ActivitySplashBinding
import com.shubhit.womensafetyapp.utills.LocationUtills
import com.shubhit.womensafetyapp.utills.Preferences

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private lateinit var shakeDetector: ShakeDetector
    lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Preferences.setup(this)

        val userId = Preferences.userId


        // Initialize location client
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        shakeDetector = ShakeDetector {
            checkSmsPermissionAndSendHelpMessage()
        }

        LocationUtills.requestLocationPermission(this)
        LocationUtills.getLastKnownLocation(this) { location ->
            location?.let {
                val address = LocationUtills.getAddressFromLocation(this, it)
                Preferences.addressObject = address
            }
        }

        if (userId != null) {
            binding.getStartedButton.text = "Get Started"
        }


        binding.getStartedButton.setOnClickListener {
            if (userId != null) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
                finish()

            }

        }
    }

    private fun checkSmsPermissionAndSendHelpMessage() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.SEND_SMS),
                SEND_SMS_PERMISSION_REQUEST_CODE
            )
        } else {
            sendHelpMessage()
        }
    }

    private fun sendHelpMessage() {
        LocationUtills.getLastKnownLocation(this) { location ->
            if (location != null) {
                val message =
                    "I need help! My location is: https://maps.google.com/?q=${location.latitude},${location.longitude}"

                // Get emergency contacts
                val emergencyContacts = Preferences.emergencyContacts!!

                // Send SMS to each contact
                val smsManager = SmsManager.getDefault()
                for (contact in emergencyContacts) {
                    smsManager.sendTextMessage(contact.number, null, message, null, null)
                }
                Toast.makeText(this, "Help message sent!", Toast.LENGTH_SHORT).show()


            } else {
                Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
            }
        }


    }


    override fun onResume() {
        super.onResume()
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(shakeDetector)
    }

    companion object {
        private const val SEND_SMS_PERMISSION_REQUEST_CODE = 123
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SEND_SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendHelpMessage()
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }

            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationUtills.getLastKnownLocation(this) { location ->
                        location?.let {
                            val address = LocationUtills.getAddressFromLocation(this, it)
                            Preferences.addressObject = address
                        }

                    }
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
