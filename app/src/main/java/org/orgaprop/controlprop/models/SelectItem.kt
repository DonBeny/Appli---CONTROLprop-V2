package org.orgaprop.controlprop.models

import android.os.Parcelable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import org.json.JSONArray

@Parcelize
data class SelectItem(
    val id: Int,
    val agency: Int = 0,
    val group: Int = 0,
    val ref: String = "",
    val name: String = "",
    val entry: String = "",
    val address: String = "",
    val postalCode: String = "",
    val city: String = "",
    val last: String = "",
    val delay: Boolean = false,
    val comment: String = "",
    val type: String = "",
    val prop: Prop? = null,
    val referents: Referents? = null,
    val saved: Boolean = false,
    val signed: Boolean = false
) : Parcelable {

    fun hasPendingChanges(): Boolean {
        return if (prop?.ctrl?.grille != null) {
            val grille = prop.ctrl.grille
            grille != "[]" && isGrilleValid(grille)
        } else {
            false
        }
    }

    private fun isGrilleValid(grilleJson: String): Boolean {
        return try {
            val grille = JSONArray(grilleJson)
            grille.length() > 0 && hasValidZoneData(grille)
        } catch (e: Exception) {
            false
        }
    }

    private fun hasValidZoneData(grille: JSONArray): Boolean {
        return (0 until grille.length()).any { i ->
            try {
                val zoneObj = grille.getJSONObject(i)
                zoneObj.has("zoneId") &&
                        zoneObj.has("elements") &&
                        zoneObj.getJSONArray("elements").toString().isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
    }

    @Parcelize
    data class Prop(
        val zones: Zones,
        val ctrl: Ctrl
    ) : Parcelable {

        @Parcelize
        data class Zones(
            val proxi: List<String>,
            val contra: List<String>
        ) : Parcelable

        @Parcelize
        data class Ctrl(
            val note: Int = -1,
            val conf: ObjConfig,
            val date: ObjDateCtrl,
            var prestate: Int = 0,
            val grille: String = "[]"
        ) : Parcelable {

            fun getGrilleAsJsonArray(): JSONArray {
                return try {
                    JSONArray(grille)
                } catch (e: Exception) {
                    JSONArray()
                }
            }

            companion object {
                fun fromJsonArray(grille: JSONArray, note: Int, conf: ObjConfig, date: ObjDateCtrl): Ctrl {
                    return Ctrl(
                        note = note,
                        conf = conf,
                        date = date,
                        grille = grille.toString()
                    )
                }
            }
        }
    }

    fun getZoneElements(zoneId: Int): List<ObjElement>? {
        return try {
            prop?.ctrl?.getGrilleAsJsonArray()?.let { grille ->
                (0 until grille.length())
                    .mapNotNull { i ->
                        grille.optJSONObject(i)?.takeIf { it.optInt("zoneId") == zoneId }
                    }
                    .firstOrNull()
                    ?.optJSONArray("elements")
                    ?.toString()
                    ?.let { elementsJson ->
                        Gson().fromJson(
                            elementsJson,
                            object : TypeToken<List<ObjElement>>() {}.type
                        )
                    }
            }
        } catch (e: Exception) {
            null
        }
    }
}
