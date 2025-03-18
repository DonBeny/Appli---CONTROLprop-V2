package org.orgaprop.controlprop.utils

import android.content.Context
import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter

import java.net.URL
import javax.net.ssl.HttpsURLConnection

import org.orgaprop.controlprop.utils.network.NetworkConfig
import org.orgaprop.controlprop.utils.network.NetworkMonitor

class HttpTask(private val context: Context) {

    companion object {

        private const val TAG = "HttpTask"

    }

    suspend fun executeHttpTask(vararg params: String): String = withContext(Dispatchers.IO) {
        var retryCount = 0

        while (retryCount < NetworkConfig.TIME_OUT) {
            val isNetworkAvailable = NetworkMonitor.isNetworkAvailable.value

            Log.d(TAG, "executeHttpTask isNetworkAvailable: $isNetworkAvailable => "+NetworkMonitor.isNetworkAvailable.value)

            if (!isNetworkAvailable) {
                return@withContext "No internet connection"
            }

            Log.d(TAG, "executeHttpTask: $params")

            val paramsAct = params.getOrNull(0) ?: return@withContext "Missing action parameter"
            val paramsCbl = params.getOrNull(1) ?: return@withContext "Missing callback parameter"
            val paramsGet = params.getOrNull(2) ?: ""
            val paramsPost = params.getOrNull(3) ?: ""

            val stringUrl = buildUrl(paramsAct, paramsCbl, paramsGet)

            Log.d(TAG, "executeHttpTask stringUrl: $retryCount => $stringUrl")
            Log.d(TAG, "executeHttpTask paramsPost: $retryCount => $paramsPost")

            var urlConnection: HttpsURLConnection? = null
            try {
                val url = URL(stringUrl)
                urlConnection = url.openConnection() as HttpsURLConnection
                urlConnection.apply {
                    readTimeout = NetworkConfig.LAPS_TIME_TEST_CONNECT
                    connectTimeout = NetworkConfig.LAPS_TIME_TEST_CONNECT
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                }

                urlConnection.outputStream.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                        writer.write(paramsPost)
                    }
                }

                if (urlConnection.responseCode == HttpsURLConnection.HTTP_OK) {
                    retryCount = NetworkConfig.TIME_OUT

                    return@withContext urlConnection.inputStream.use { inputStream ->
                        readStream(BufferedInputStream(inputStream))
                    }
                }

                retryCount++
            } catch (e: IOException) {
                retryCount++
                Log.d(TAG, "IOException occurred: ${e.message}", e)
                kotlinx.coroutines.delay(NetworkConfig.RETRY_DELAY_MS)
            } finally {
                retryCount = NetworkConfig.TIME_OUT
                urlConnection?.disconnect()
            }
        }

        return@withContext "Request timed out"
    }

    private fun buildUrl(action: String, callback: String, getParams: String): String {
        val urlBuilder = StringBuilder(NetworkConfig.HTTP_ADDRESS_SERVER)
            .append(NetworkConfig.ACCESS_CODE)
            .append(".php")
            .append("?act=")
            .append(action)
            .append("&cbl=")
            .append(callback)

        if (getParams.isNotEmpty()) {
            urlBuilder.append("&").append(getParams)
        }

        return urlBuilder.toString()
    }

    private fun readStream(inputStream: BufferedInputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val result = StringBuilder()
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                result.append(line).append("\n")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading stream", e)
        } finally {
            reader.close()
        }
        return result.toString().trim()
    }

}
