package org.orgaprop.controlprop.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.orgaprop.controlprop.BuildConfig


class LogUtils {

    companion object {
        private const val TAG = "LogUtils"

        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        fun d(message: String) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, message)
            }
        }

        fun d(tag: String, message: String) {
            if (BuildConfig.DEBUG) {
                Log.d(tag, message)
            }
        }



        fun e(tag: String, message: String) {
            if (BuildConfig.DEBUG) {
                Log.e(tag, message)
            }
        }

        fun e(tag: String, message: String, throwable: Throwable) {
            if (BuildConfig.DEBUG) {
                Log.e(tag, message, throwable)
            }
        }



        fun json(`object`: Any?) {
            if (BuildConfig.DEBUG) {
                val jsonString: String = gson.toJson(`object`)
                val maxLogSize = 2000

                for (i in 0..jsonString.length / maxLogSize) {
                    val start = i * maxLogSize
                    var end = (i + 1) * maxLogSize
                    end = if (end > jsonString.length) jsonString.length else end
                    Log.d(TAG, jsonString.substring(start, end))
                }
            }
        }

        fun json(tag : String, `object`: Any?) {
            if (BuildConfig.DEBUG) {
                val jsonString: String = gson.toJson(`object`)
                val maxLogSize = 2000

                for (i in 0..jsonString.length / maxLogSize) {
                    val start = i * maxLogSize
                    var end = (i + 1) * maxLogSize
                    end = if (end > jsonString.length) jsonString.length else end
                    Log.d(tag, jsonString.substring(start, end))
                }
            }
        }

        fun json(tag : String, message : String, `object`: Any?) {
            if (BuildConfig.DEBUG) {
                val jsonString: String = gson.toJson(`object`)
                val maxLogSize = 2000

                Log.d(tag, message)

                for (i in 0..jsonString.length / maxLogSize) {
                    val start = i * maxLogSize
                    var end = (i + 1) * maxLogSize
                    end = if (end > jsonString.length) jsonString.length else end
                    Log.d(tag, jsonString.substring(start, end))
                }
            }
        }
    }

}
