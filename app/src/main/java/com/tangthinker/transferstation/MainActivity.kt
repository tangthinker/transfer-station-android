package com.tangthinker.transferstation

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tangthinker.transferstation.databinding.ActivityMainBinding
import com.tangthinker.transferstation.utils.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val storageKey = "sms-prefs"
    private val webhookKey = "webhook-key"

    private var startStatus = false

    private var lastSMSReceiveTime = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lastSMSReceiveTime = System.currentTimeMillis()

        val sharedPreferences = this.getSharedPreferences(storageKey, Context.MODE_PRIVATE)

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val existWebhook = sharedPreferences.getString(webhookKey, "")

        var readSMSJob : Job? = null

        if (existWebhook.isNullOrEmpty()) {
            binding.inputEt.setText(existWebhook)
            binding.startTxt.text = "运行中"
            startStatus = true
            readSMSJob = GlobalScope.launch (Dispatchers.IO) {
                if (existWebhook != null) {
                    readSMSInterval(contentResolver, existWebhook)
                }
            }
        }


        binding.startBtn.setOnClickListener  {
            if (!startStatus) {
                val input = binding.inputEt.text.toString()
                Toast.makeText(this, "start service at: $input", Toast.LENGTH_SHORT).show()
                sharedPreferences.edit().putString(webhookKey, input).apply()
                 readSMSJob = GlobalScope.launch (Dispatchers.IO) {
                    readSMSInterval(contentResolver, input)
                }
                binding.startTxt.text = "运行中"
                startStatus = true
            } else {
                Toast.makeText(this, "stop service", Toast.LENGTH_SHORT).show()
                sharedPreferences.edit().remove(webhookKey).apply()
                readSMSJob?.cancel()
                binding.startTxt.text = "启动"
                startStatus = false
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

    private fun readSMSInterval(contentResolver: ContentResolver, webhook: String) {
        // 读取短信
        readSMS(contentResolver, webhook)
        // 读取间隔
        Thread.sleep(3000)
        // 递归调用
        readSMSInterval(contentResolver, webhook)
    }

    private fun readSMS(contentResolver: ContentResolver, webhook: String) {
        val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        val sortOrder = "${Telephony.Sms.DATE} DESC"
        val selection = "${Telephony.Sms.DATE} > ?"
        val selectionArgs = arrayOf(lastSMSReceiveTime.toString())
        val cursor = contentResolver.query(Telephony.Sms.CONTENT_URI, projection, selection, selectionArgs, sortOrder)
        var curLastTime = lastSMSReceiveTime
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndexOrThrow(Telephony.Sms._ID))
                val address = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val date = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                // 处理短信数据
                println("read from content: id: $id, address: $address, body: $body date: $date")
                val message = "Sender: $address\nMessage:$body\nDate: $date"
                val jsonData = JSONObject().apply {
                    put("msg_type", "text")
                    put("content", JSONObject().apply {
                        put("text", message)
                    })
                }

                if (webhook.isNotEmpty()) {
                    Post.sendPostRequest(webhook, jsonData)
                }
                if (date.toLong() > curLastTime) {
                    curLastTime = date.toLong()
                }
            }
        }
        lastSMSReceiveTime = curLastTime
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