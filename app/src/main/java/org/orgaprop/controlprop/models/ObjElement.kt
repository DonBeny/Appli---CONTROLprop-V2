package org.orgaprop.controlprop.models

import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjElement(
    var id: Int = 0,
    var note: Int = -1,
    var coef: Int = 0,
    var criterMap: MutableMap<Int, ObjCriter> = mutableMapOf()
) : Serializable {

    fun addCriter(criter: ObjCriter) {
        criterMap[criterMap.size] = criter
    }

    companion object {
        fun fromJson(json: JSONObject): ObjElement {
            try {
                val objElement = ObjElement(
                    id = json.optInt("id", 0),
                    note = json.optInt("note", -1),
                    coef = json.optInt("coef", 0)
                )

                val criterMapJson = json.optJSONObject("criterMap")
                criterMapJson?.let {
                    val keys = it.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val criterJson = it.getJSONObject(key)
                        objElement.criterMap[key.toInt()] = ObjCriter.fromJson(criterJson)
                    }
                }

                return objElement
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjElement", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            val criterMapJson = JSONObject()
            for ((key, value) in criterMap) {
                criterMapJson.put(key.toString(), value.toJson())
            }

            return JSONObject().apply {
                put("id", id)
                put("note", note)
                put("coef", coef)
                put("criterMap", criterMapJson)
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjElement vers JSON", e)
        }
    }

}
