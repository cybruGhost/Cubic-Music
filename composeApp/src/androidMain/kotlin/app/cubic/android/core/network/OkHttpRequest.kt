package app.cubic.android.core.network

import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request

class OkHttpRequest {
    private val client = NetworkClientFactory.getClient()

    fun POST(url: String, parameters: Map<String, String>, callback: Callback): Call {
        val builder = FormBody.Builder()
        for ((key, value) in parameters) {
            builder.add(key, value)
        }

        val formBody = builder.build()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    fun GET(url: String, callback: Callback): Call {
        val request = Request.Builder()
            .url(url)
            .build()

        val call = client.newCall(request)
        call.enqueue(callback)
        return call
    }

    companion object {
        val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}
