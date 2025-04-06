package org.orgaprop.controlprop.models

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class Referents(
    val ecdr: ObjAgent,
    val adm: ObjAgent,
    val ent: ObjAgent
) : Parcelable



fun parseReferentsFromJson(jsonObject: JSONObject): Referents {
    val TAG = "parseReferentsFromJson"

    val ecdrJson = jsonObject.getJSONObject("ecdr")

    Log.d(TAG, "parseReferentsFromJson: ecdrJson=$ecdrJson")

    val admJson = jsonObject.getJSONObject("adm")

    Log.d(TAG, "parseReferentsFromJson: admJson=$admJson")

    val entJson = jsonObject.getJSONObject("ent")

    Log.d(TAG, "parseReferentsFromJson: entJson=$entJson")

    val ecdr = ObjAgent(
        id = ecdrJson.getInt("id"),
        name = ecdrJson.getString("txt"),
        type = ecdrJson.getString("type")
    )

    Log.d(TAG, "parseReferentsFromJson: ecdr=$ecdr")

    val adm = ObjAgent(
        id = admJson.getInt("id"),
        name = admJson.getString("txt"),
        type = admJson.getString("type")
    )

    Log.d(TAG, "parseReferentsFromJson: adm=$adm")

    val ent = ObjAgent(
        id = entJson.getInt("id"),
        name = entJson.getString("txt"),
        type = entJson.getString("type")
    )

    Log.d(TAG, "parseReferentsFromJson: ent=$ent")

    return Referents(ecdr, adm, ent)
}
