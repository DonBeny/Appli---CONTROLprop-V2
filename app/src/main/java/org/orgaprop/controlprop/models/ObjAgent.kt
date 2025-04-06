package org.orgaprop.controlprop.models

import org.json.JSONException

import android.os.Parcelable
import android.util.Log
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class ObjAgent(
    var id: Int = 0,
    var name: String = "",
    var type: String = "",
) : Parcelable



fun JSONObject.toObjAgentList(): List<ObjAgent> {
    val list = mutableListOf<ObjAgent>()
    val keys = this.keys()

    //Log.d("JSONObject", "toObjAgentList: $this")

    while (keys.hasNext()) {
        val key = keys.next()

        //Log.d("JSONObject", "toObjAgentList: $key")

        try {
            val jsonObject = this.getJSONObject(key)

            //Log.d("JSONObject", "toObjAgentList: $jsonObject")

            val objAgent = ObjAgent(
                id = jsonObject.getInt("id"),
                name = jsonObject.getString("name"),
                type = jsonObject.optString("type", "agent") // Si "type" est absent, utilise "agent" par défaut
            )

            //Log.d("JSONObject", "toObjAgentList: $objAgent")

            list.add(objAgent)
        } catch (e: JSONException) {
            Log.e("JSONObject", "Erreur lors du parsing de l'agent $key", e)
        }
    }

    //Log.d("JSONObject", "toObjAgentList: $list")

    return list
}
