package com.tangthinker.transferstation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tangthinker.transferstation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val StorageKey = "sms-prefs"
    private val WebhookKey = "webhook-key"

    private var StartStatus = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = this.getSharedPreferences(StorageKey, Context.MODE_PRIVATE)

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val existWebhook = sharedPreferences.getString(WebhookKey, "")

        if (existWebhook != "") {
            binding.inputEt.setText(existWebhook)
            binding.startBtn.text = "运行中"
            StartStatus = true
        }

        binding.startBtn.setOnClickListener  {
            if (!StartStatus) {
                val input = binding.inputEt.text.toString()
                Toast.makeText(this, "start service at: $input", Toast.LENGTH_SHORT).show()
                sharedPreferences.edit().putString(WebhookKey, input).apply()
                binding.startBtn.text = "运行中"
                StartStatus = true
            } else {
                Toast.makeText(this, "stop service", Toast.LENGTH_SHORT).show()
                binding.startBtn.text = "启动"
                StartStatus = false
            }

        }

        requestSmsPermissions()
    }


    private val REQUEST_SMS_PERMISSIONS = 123

    private fun requestSmsPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                REQUEST_SMS_PERMISSIONS)
        } else {
            Toast.makeText(this, "permission is authed!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        println("call onRequestPermissionsResult")
        if (requestCode == REQUEST_SMS_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "permission is authed!", Toast.LENGTH_SHORT).show()
                println("is authed")
            } else {
                Toast.makeText(this, "permission is rejected!", Toast.LENGTH_SHORT).show()
                println("is rejected!")


            }
        }
    }
}