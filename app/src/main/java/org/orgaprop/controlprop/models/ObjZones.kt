package org.orgaprop.controlprop.models

import org.json.JSONArray
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjZones(
    var proxi: List<String> = emptyList(),
    var contra: List<String> = emptyList()
) : Serializable {

    companion object {
        fun fromJson(json: JSONObject): ObjZones {
            try {
                val proxiArray = json.optJSONArray("proxi") ?: JSONArray()
                val contraArray = json.optJSONArray("contra") ?: JSONArray()

                val proxiList = (0 until proxiArray.length()).map { proxiArray.getString(it) }
                val contraList = (0 until contraArray.length()).map { contraArray.getString(it) }

                return ObjZones(proxiList, contraList)
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjZones", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("proxi", JSONArray(proxi))
                put("contra", JSONArray(contra))
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjZones vers JSON", e)
        }
    }

}
