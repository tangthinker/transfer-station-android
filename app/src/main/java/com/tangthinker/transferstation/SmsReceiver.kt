package com.tangthinker.transferstation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import android.widget.Toast
import com.tangthinker.transferstation.utils.Post
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException


class SmsReceiver : BroadcastReceiver() {

    private val StorageKey = "sms-prefs"
    private val WebhookKey = "webhook-key"

    override fun onReceive(context: Context, intent: Intent) {
        val sharedPreferences = context.getSharedPreferences(StorageKey, Context.MODE_PRIVATE)
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            for (smsMessage in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                // 处理接收到的短信
                Log.d("SmsReceiver", "Sender: $sender, Message: $messageBody")
                // 处理短信内容
                Toast.makeText(context, "Sender: $sender\nMessage: $messageBody", Toast.LENGTH_LONG).show()
                val message = "Sender: $sender\nMessage:$messageBody"
                val jsonData = JSONObject().apply {
                    put("msg_type", "text")
                    put("content", JSONObject().apply {
                        put("text", message)
                    })
                }

                val webhook = sharedPreferences.getString(WebhookKey, "")
                if (!webhook.isNullOrEmpty()) {
                    Post.sendPostRequest(webhook, jsonData)
                }
            }
        }
    }

}
