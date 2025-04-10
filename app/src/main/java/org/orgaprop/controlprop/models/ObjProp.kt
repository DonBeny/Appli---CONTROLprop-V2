package org.orgaprop.controlprop.models

import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import java.io.Serializable

data class ObjProp(
    var objConfig: ObjConfig = ObjConfig(),
    var objZones: ObjZones? = null,
    var objDateCtrl: ObjDateCtrl? = null,
    var note: Int = 0,
    var grille: ObjGrille? = null
) : Serializable {

    companion object {
        fun fromJson(json: JSONObject): ObjProp {
            try {
                val objProp = ObjProp(
                    note = json.optInt("note", 0)
                )

                json.optJSONObject("objConfig")?.let {
                    objProp.objConfig = ObjConfig.fromJson(it)
                }

                json.optJSONObject("objZones")?.let {
                    objProp.objZones = ObjZones.fromJson(it)
                }

                json.optJSONObject("objDateCtrl")?.let {
                    objProp.objDateCtrl = ObjDateCtrl.fromJson(it)
                }

                json.optJSONObject("grille")?.let {
                    objProp.grille = ObjGrille.fromJson(it)
                }

                return objProp
            } catch (e: Exception) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion JSON vers ObjProp", e)
            }
        }
    }

    fun toJson(): JSONObject {
        try {
            return JSONObject().apply {
                put("objConfig", objConfig.toJson())
                put("note", note)
                objZones?.let { put("objZones", it.toJson()) }
                objDateCtrl?.let { put("objDateCtrl", it.toJson()) }
                grille?.let { put("grille", it.toJsonArray()) }
            }
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.INVALID_DATA, "Erreur lors de la conversion ObjProp vers JSON", e)
        }
    }

}
