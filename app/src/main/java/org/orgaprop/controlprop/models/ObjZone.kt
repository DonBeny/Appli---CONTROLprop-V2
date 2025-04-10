package org.orgaprop.controlprop.models

import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjZone(
    var id: Int = 0,
    var note: Int = -1,
    var elementMap: MutableMap<Int, ObjElement> = mutableMapOf()
) : Serializable {

    fun addElement(element: ObjElement) {
        elementMap[elementMap.size] = element
    }

    companion object {
        fun fromJson(json: JSONObject): ObjZone {
            try {
                val objZone = ObjZone(
                    id = json.optInt("id", 0),
                    note = json.optInt("note", -1)
                )

                val elementMapJson = json.optJSONObject("elementMap")
                elementMapJson?.let {
                    val keys = it.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val elementJson = it.getJSONObject(key)
                        objZone.elementMap[key.toInt()] = ObjElement.fromJson(elementJson)
                    }
                }

                return objZone
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjZone", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            val elementMapJson = JSONObject()
            for ((key, value) in elementMap) {
                elementMapJson.put(key.toString(), value.toJson())
            }

            return JSONObject().apply {
                put("id", id)
                put("note", note)
                put("elementMap", elementMapJson)
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjZone vers JSON", e)
        }
    }

}
