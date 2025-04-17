package org.orgaprop.controlprop.models

import android.os.Parcelable
import android.util.Log

import kotlinx.parcelize.Parcelize
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.utils.LogUtils
import java.text.SimpleDateFormat

import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

@Parcelize
data class ObjDateCtrl(
    var value: Long = 0,
    var txt: String = ""
) : Parcelable {

    companion object {
        private const val TAG = "ObjDateCtrl"

        fun fromJson(json: JSONObject): ObjDateCtrl {
            try {
                val dateValue = if (!json.isNull("val") && json.optLong("val", 0) > 0)
                    json.optLong("val", 0)
                else
                    System.currentTimeMillis()

                return ObjDateCtrl(
                    txt = json.optString("txt", ""),
                    value = dateValue
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de la date", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de date invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de la date", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de la date", e)
            }
        }

        fun getCurrentDate(): ObjDateCtrl {
            try {
                val now = System.currentTimeMillis()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                val formattedDate = dateFormat.format(Date(now))

                return ObjDateCtrl(
                    txt = formattedDate,
                    value = now
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la création de la date courante", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la création de la date", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("txt", txt)
                put("val", value)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors de la sérialisation de la date", e)
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion de la date en JSON", e)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors de la sérialisation de la date", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la conversion de la date", e)
        }
    }

    fun isToday(): Boolean {
        try {
            var controlTime = this.value

            LogUtils.d(TAG, "isToday: controlTime: $controlTime")

            if (controlTime < 10000000000) {
                controlTime *= 1000
            }

            val now = Instant.now().toEpochMilli()

            LogUtils.d(TAG, "isToday: now: $now")

            val controlDate = Instant.ofEpochMilli(controlTime).atZone(ZoneId.systemDefault()).toLocalDate()

            LogUtils.d(TAG, "isToday: controlDate: $controlDate")

            val today = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate()

            LogUtils.d(TAG, "isToday: today: $today")

            return controlDate == today
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la vérification de la date", e)
            return false
        }
    }

    private fun validate() {
        if (value <= 0) {
            Log.w(TAG, "Date invalide, utilisation de l'heure actuelle")
            value = System.currentTimeMillis()
        }
        if (txt.isBlank()) {
            Log.w(TAG, "Texte de date manquant, génération automatique")
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
            txt = dateFormat.format(Date(value))
        }
    }


}
