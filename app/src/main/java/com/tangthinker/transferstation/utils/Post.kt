package com.tangthinker.transferstation.utils

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

object Post {

    fun sendPostRequest(url: String, jsonData: JSONObject) {
        val client = OkHttpClient()

        // Use the new way to create MediaType
        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaType(),
            jsonData.toString()
        )

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    println("Response: $responseData")
                } else {
                    println("Request failed with code: ${response.code}")
                }
            }
        })
    }
}