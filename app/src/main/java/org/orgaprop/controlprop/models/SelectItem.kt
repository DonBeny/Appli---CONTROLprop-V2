package org.orgaprop.controlprop.models

import android.os.Parcelable
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.ui.selectEntry.SelectListActivity

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
    val signed: Boolean = false,
    val signatures: ObjSignature? = null,
    val planActions: ObjPlanActions? = null
) : Parcelable {

    companion object {
        private const val TAG = "SelectItem"

        fun fromAgencyJson(json: JSONObject, type: String): SelectItem {
            try {
                return SelectItem(
                    id = json.optInt("id", 0),
                    name = json.optString("txt", ""),
                    type = type
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de l'agence", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format d'agence invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de l'agence", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de l'agence", e)
            }
        }

        fun fromGroupJson(json: JSONObject, type: String): SelectItem {
            try {
                return SelectItem(
                    id = json.optInt("id", 0),
                    name = json.optString("txt", ""),
                    type = type
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing du groupe", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de groupe invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing du groupe", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse du groupe", e)
            }
        }

        fun fromResidenceJson(json: JSONObject, type: String): SelectItem {
            try {
                val prop = json.optJSONObject("prop")?.let { propJson ->
                    try {
                        Prop.fromJson(propJson)
                    } catch (e: BaseException) {
                        Log.e(TAG, "Erreur lors du parsing des propriétés", e)
                        null
                    }
                }

                val referentsJson = json.optJSONObject("aff")
                val referents = referentsJson?.let {
                    try {
                        parseReferentsFromJson(it)
                    } catch (e: BaseException) {
                        Log.e(TAG, "Erreur lors du parsing des référents", e)
                        null
                    }
                }

                return SelectItem(
                    id = json.optInt("id", 0),
                    ref = json.optString("ref", ""),
                    name = json.optString("name", ""),
                    entry = json.optString("entry", ""),
                    agency = json.optInt("agency", 0),
                    group = json.optInt("group", 0),
                    address = json.optJSONObject("adr")?.optString("rue", "") ?: "",
                    postalCode = json.optJSONObject("adr")?.optString("cp", "") ?: "",
                    city = json.optJSONObject("adr")?.optString("city", "") ?: "",
                    last = json.optString("last", ""),
                    delay = json.optBoolean("delay", false),
                    comment = json.optString("comment", ""),
                    type = type,
                    prop = prop,
                    referents = referents
                ).also { it.validate() }
            } catch (e: JSONException) {
                Log.e(TAG, "Erreur lors du parsing de la résidence", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de résidence invalide", e)
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur inattendue lors du parsing de la résidence", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse de la résidence", e)
            }
        }

        fun fromSearchJson(json: JSONObject, type: String): SelectItem {
            try {
                val item = fromResidenceJson(json, type)
                // Traitement spécifique aux résultats de recherche si nécessaire
                return item
            } catch (e: BaseException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du parsing du résultat de recherche", e)
                throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse du résultat de recherche", e)
            }
        }

        fun parseListFromJsonArray(jsonArray: JSONArray, type: String): List<SelectItem> {
            try {
                val items = mutableListOf<SelectItem>()

                for (i in 0 until jsonArray.length()) {
                    try {
                        val jsonItem = jsonArray.getJSONObject(i)

                        val item = when (type) {
                            SelectListActivity.SELECT_LIST_TYPE_AGC -> fromAgencyJson(jsonItem, type)
                            SelectListActivity.SELECT_LIST_TYPE_GRP -> fromGroupJson(jsonItem, type)
                            SelectListActivity.SELECT_LIST_TYPE_RSD -> fromResidenceJson(jsonItem, type)
                            SelectListActivity.SELECT_LIST_TYPE_SEARCH -> fromSearchJson(jsonItem, type)
                            else -> {
                                Log.e(TAG, "Type inconnu: $type")
                                throw BaseException(ErrorCodes.INVALID_DATA, "Type de liste inconnu: $type")
                            }
                        }

                        items.add(item)
                    } catch (e: BaseException) {
                        Log.e(TAG, "Erreur lors du parsing de l'élément à l'index $i", e)
                        // On continue avec les autres éléments
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur inattendue lors du parsing de l'élément à l'index $i", e)
                        // On continue avec les autres éléments
                    }
                }

                return items
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors du parsing de la liste", e)
                throw BaseException(ErrorCodes.INVALID_RESPONSE, "Erreur lors de l'analyse de la liste", e)
            }
        }
    }

    private fun isGrilleValid(grilleJson: String): Boolean {
        return try {
            val grille = JSONArray(grilleJson)
            grille.length() > 0 && hasValidZoneData(grille)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la validation de la grille", e)
            false
        }
    }

    private fun hasValidZoneData(grille: JSONArray): Boolean {
        return try {
            (0 until grille.length()).any { i ->
                try {
                    val zoneObj = grille.getJSONObject(i)
                    zoneObj.has("zoneId") &&
                            zoneObj.has("elements") &&
                            zoneObj.getJSONArray("elements").toString().isNotEmpty()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la validation des données de zone à l'index $i", e)
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la validation des données de zone", e)
            false
        }
    }

    fun getZoneElements(zoneId: Int): List<ObjElement>? {
        return try {
            prop?.ctrl?.getGrilleAsJsonArray()?.let { grille ->
                (0 until grille.length()).firstNotNullOfOrNull { i ->
                    grille.optJSONObject(i)?.takeIf { it.optInt("zoneId") == zoneId }
                }
                    ?.optJSONArray("elements")
                    ?.toString()
                    ?.let { elementsJson ->
                        try {
                            Gson().fromJson(
                                elementsJson,
                                object : TypeToken<List<ObjElement>>() {}.type
                            )
                        } catch (e: JsonSyntaxException) {
                            Log.e(TAG, "Erreur lors de la conversion des éléments de zone", e)
                            throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format d'éléments de zone invalide", e)
                        }
                    }
            }
        } catch (e: BaseException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des éléments de zone", e)
            null
        }
    }

    private fun validate() {
        if (id <= 0) {
            throw BaseException(ErrorCodes.INVALID_DATA, "ID d'élément invalide: $id")
        }

        when (type) {
            SelectListActivity.SELECT_LIST_TYPE_AGC, SelectListActivity.SELECT_LIST_TYPE_GRP -> {
                if (name.isBlank()) {
                    throw BaseException(ErrorCodes.INVALID_DATA, "Nom manquant")
                }
            }
            SelectListActivity.SELECT_LIST_TYPE_RSD, SelectListActivity.SELECT_LIST_TYPE_SEARCH -> {
                if (name.isBlank()) {
                    throw BaseException(ErrorCodes.INVALID_DATA, "Nom de résidence manquant")
                }
                if (ref.isBlank()) {
                    throw BaseException(ErrorCodes.INVALID_DATA, "Référence manquante")
                }
                if (entry.isBlank()) {
                    throw BaseException(ErrorCodes.INVALID_DATA, "Entrée manquante")
                }
            }
        }
    }

    @Parcelize
    data class Prop(
        val zones: Zones,
        val ctrl: Ctrl
    ) : Parcelable {

        companion object {
            private const val TAG = "SelectItem.Prop"

            fun fromJson(json: JSONObject): Prop {
                try {
                    return Prop(
                        zones = Zones.fromJson(json.optJSONObject("zones") ?: JSONObject()),
                        ctrl = Ctrl.fromJson(json.optJSONObject("ctrl") ?: JSONObject())
                    )
                } catch (e: JSONException) {
                    Log.e(TAG, "Erreur lors du parsing des propriétés", e)
                    throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de propriétés invalide", e)
                } catch (e: BaseException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur inattendue lors du parsing des propriétés", e)
                    throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse des propriétés", e)
                }
            }
        }

        @Parcelize
        data class Zones(
            val proxi: List<String>,
            val contra: List<String>
        ) : Parcelable {

            companion object {
                private const val TAG = "SelectItem.Prop.Zones"

                fun fromJson(json: JSONObject): Zones {
                    try {
                        val proxiArray = json.optJSONArray("proxi") ?: JSONArray()
                        val contraArray = json.optJSONArray("contra") ?: JSONArray()

                        val proxiList = (0 until proxiArray.length()).map { proxiArray.optString(it, "") }
                        val contraList = (0 until contraArray.length()).map { contraArray.optString(it, "") }

                        return Zones(proxiList, contraList)
                    } catch (e: JSONException) {
                        Log.e(TAG, "Erreur lors du parsing des zones", e)
                        throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de zones invalide", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur inattendue lors du parsing des zones", e)
                        throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse des zones", e)
                    }
                }
            }

        }

        @Parcelize
        data class Ctrl(
            val note: Int = -1,
            val conf: ObjConfig,
            val date: ObjDateCtrl,
            var prestate: Int = 0,
            val grille: String = "[]"
        ) : Parcelable {

            companion object {
                private const val TAG = "SelectItem.Prop.Ctrl"

                fun fromJson(json: JSONObject): Ctrl {
                    try {
                        val note = json.optInt("note", -1)
                        val conf = parseConfCtrl(json)
                        val date = parseDateCtrl(json)
                        val grille = json.optJSONArray("grille")?.toString() ?: "[]"

                        return Ctrl(
                            note = note,
                            conf = conf,
                            date = date,
                            prestate = json.optInt("prestate", 0),
                            grille = grille
                        )
                    } catch (e: JSONException) {
                        Log.e(TAG, "Erreur lors du parsing du contrôle", e)
                        throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de contrôle invalide", e)
                    } catch (e: BaseException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur inattendue lors du parsing du contrôle", e)
                        throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de l'analyse du contrôle", e)
                    }
                }

                fun fromJsonArray(grille: JSONArray, note: Int, conf: ObjConfig, date: ObjDateCtrl): Ctrl {
                    try {
                        return Ctrl(
                            note = note,
                            conf = conf,
                            date = date,
                            grille = grille.toString()
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors de la création du contrôle à partir du tableau", e)
                        throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la création du contrôle", e)
                    }
                }

                private fun parseConfCtrl(json: JSONObject): ObjConfig {
                    try {
                        val confJson = json.optJSONObject("conf")
                        return if (confJson != null) {
                            ObjConfig.fromJson(confJson)
                        } else {
                            ObjConfig()
                        }
                    } catch (e: BaseException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors du parsing de la configuration", e)
                        throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de configuration invalide", e)
                    }
                }

                private fun parseDateCtrl(json: JSONObject): ObjDateCtrl {
                    try {
                        val dateJson = json.optJSONObject("date")
                        return if (dateJson != null) {
                            ObjDateCtrl.fromJson(dateJson)
                        } else {
                            ObjDateCtrl.getCurrentDate()
                        }
                    } catch (e: BaseException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur lors du parsing de la date", e)
                        throw BaseException(ErrorCodes.INVALID_RESPONSE, "Format de date invalide", e)
                    }
                }
            }

            fun getGrilleAsJsonArray(): JSONArray {
                return try {
                    JSONArray(grille)
                } catch (e: JSONException) {
                    Log.e(TAG, "Erreur lors de la conversion de la grille en JSONArray", e)
                    JSONArray()
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur inattendue lors de la conversion de la grille", e)
                    JSONArray()
                }
            }
        }
    }

}
