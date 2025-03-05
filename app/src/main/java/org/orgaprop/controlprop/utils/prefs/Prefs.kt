package org.orgaprop.controlprop.utils.prefs

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import org.orgaprop.controlprop.utils.prefs.models.Pref
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Gestionnaire des préférences de l'application.
 */
class Prefs(private val context: Context) {

    companion object {
        private const val TAG = "Prefs"
    }

    // ************ SETTERS ************

    fun setMbr(idMbr: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Looper.prepare()

            val values = ContentValues().apply {
                put("id", ConfigPrefDatabase.PREF_ROW_ID_MBR_NUM.toLong())
                put("param", ConfigPrefDatabase.PREF_ROW_ID_MBR)
                put("value", idMbr)
            }

            PrefDatabase.getInstance(context).mPrefDao().updatePref(Pref.fromContentValues(values))
            Looper.loop()
        }
    }

    fun setAdrMac(adrMac: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Looper.prepare()

            val values = ContentValues().apply {
                put("id", ConfigPrefDatabase.PREF_ROW_ADR_MAC_NUM.toLong())
                put("param", ConfigPrefDatabase.PREF_ROW_ADR_MAC)
                put("value", adrMac)
            }

            PrefDatabase.getInstance(context).mPrefDao().updatePref(Pref.fromContentValues(values))
            Looper.loop()
        }
    }

    fun setAgency(agency: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Looper.prepare()

            val values = ContentValues().apply {
                put("id", ConfigPrefDatabase.PREF_ROW_AGENCY_NUM.toLong())
                put("param", ConfigPrefDatabase.PREF_ROW_AGENCY)
                put("value", agency)
            }

            PrefDatabase.getInstance(context).mPrefDao().updatePref(Pref.fromContentValues(values))
            Looper.loop()
        }
    }

    fun setGroup(group: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Looper.prepare()

            val values = ContentValues().apply {
                put("id", ConfigPrefDatabase.PREF_ROW_GROUP_NUM.toLong())
                put("param", ConfigPrefDatabase.PREF_ROW_GROUP)
                put("value", group)
            }

            PrefDatabase.getInstance(context).mPrefDao().updatePref(Pref.fromContentValues(values))
            Looper.loop()
        }
    }

    fun setResidence(residence: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Looper.prepare()

            val values = ContentValues().apply {
                put("id", ConfigPrefDatabase.PREF_ROW_RESIDENCE_NUM.toLong())
                put("param", ConfigPrefDatabase.PREF_ROW_RESIDENCE)
                put("value", residence)
            }

            PrefDatabase.getInstance(context).mPrefDao().updatePref(Pref.fromContentValues(values))
            Looper.loop()
        }
    }

    // ************ GETTERS ************

    fun getMbr(callback: Callback<String>) {
        Executors.newSingleThreadExecutor().execute {
            Looper.prepare()

            var result = "new"
            val cursor: Cursor? = PrefDatabase.getInstance(context).mPrefDao()
                .getPrefFromParamWithCursor(ConfigPrefDatabase.PREF_ROW_ID_MBR)

            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(2)
                cursor.close()
            }

            val finalResult = result
            Handler(Looper.getMainLooper()).post { callback.onResult(finalResult) }
        }
    }

    fun getAdrMac(callback: Callback<String>) {
        Executors.newSingleThreadExecutor().execute {
            Looper.prepare()

            var result = "new"
            val cursor: Cursor? = PrefDatabase.getInstance(context).mPrefDao()
                .getPrefFromParamWithCursor(ConfigPrefDatabase.PREF_ROW_ADR_MAC)

            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(2)
                cursor.close()
            }

            val finalResult = result
            Handler(Looper.getMainLooper()).post { callback.onResult(finalResult) }
        }
    }

    fun getAgency(callback: Callback<String>) {
        Executors.newSingleThreadExecutor().execute {
            Looper.prepare()

            var result = ""
            val cursor: Cursor? = PrefDatabase.getInstance(context).mPrefDao()
                .getPrefFromParamWithCursor(ConfigPrefDatabase.PREF_ROW_AGENCY)

            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(2)
                cursor.close()
            }

            val finalResult = result
            Handler(Looper.getMainLooper()).post { callback.onResult(finalResult) }
        }
    }

    fun getGroup(callback: Callback<String>) {
        Executors.newSingleThreadExecutor().execute {
            Looper.prepare()

            var result = ""
            val cursor: Cursor? = PrefDatabase.getInstance(context).mPrefDao()
                .getPrefFromParamWithCursor(ConfigPrefDatabase.PREF_ROW_GROUP)

            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(2)
                cursor.close()
            }

            val finalResult = result
            Handler(Looper.getMainLooper()).post { callback.onResult(finalResult) }
        }
    }

    fun getResidence(callback: Callback<String>) {
        Executors.newSingleThreadExecutor().execute {
            Looper.prepare()

            var result = ""
            val cursor: Cursor? = PrefDatabase.getInstance(context).mPrefDao()
                .getPrefFromParamWithCursor(ConfigPrefDatabase.PREF_ROW_RESIDENCE)

            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(2)
                cursor.close()
            }

            val finalResult = result
            Handler(Looper.getMainLooper()).post { callback.onResult(finalResult) }
        }
    }

    // ************ INTERFACES ************

    interface Callback<T> {
        fun onResult(result: T)
    }
}