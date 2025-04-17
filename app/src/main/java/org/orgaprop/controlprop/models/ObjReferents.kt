package org.orgaprop.controlprop.models

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class ObjReferents(
    val ecdr: ObjAgent,
    val adm: ObjAgent,
    val ent: ObjAgent
) : Parcelable {

    companion object {
        private const val TAG = "ObjReferents"

        fun fromJson(json: JSONObject): ObjReferents {
            val ecdrJson = json.getJSONObject("ecdr")
            val admJson = json.getJSONObject("adm")
            val entJson = json.getJSONObject("ent")

            val ecdr = ObjAgent.fromJson(ecdrJson)
            val adm = ObjAgent.fromJson(admJson)
            val ent = ObjAgent.fromJson(entJson)

            Log.d(TAG, "fromJson: ecdr=$ecdr, adm=$adm, ent=$ent")

            return ObjReferents(ecdr, adm, ent)
        }
    }

}



fun parseReferentsFromJson(jsonObject: JSONObject): ObjReferents {
    return ObjReferents.fromJson(jsonObject)
}
