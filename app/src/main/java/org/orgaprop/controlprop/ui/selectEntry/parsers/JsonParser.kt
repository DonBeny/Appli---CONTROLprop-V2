package org.orgaprop.controlprop.ui.selectEntry.parsers

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import org.orgaprop.controlprop.models.ObjConfig
import org.orgaprop.controlprop.models.ObjDateCtrl
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.parseReferentsFromJson
import org.orgaprop.controlprop.utils.LogUtils

object JsonParser {

    private const val TAG = "JsonParser"



    /**
     * Parse une réponse JSON pour extraire les données de groupement
     * @param jsonResponse Réponse JSON du serveur
     * @param type Type de liste
     * @return Liste d'éléments SelectItem
     */
    fun parseResponseGrp(jsonResponse: JSONObject, type: String): List<SelectItem> {
        val dataArray = jsonResponse.getJSONObject("data").getJSONArray("grps")
        val newItems = mutableListOf<SelectItem>()

        LogUtils.json(TAG, "parseResponseGrp:", dataArray)

        for (i in 0 until dataArray.length()) {
            val jsonItem = dataArray.getJSONObject(i)

            LogUtils.json(TAG, "parseResponseGrp:", jsonItem)

            val item = SelectItem(
                id = jsonItem.getInt("id"),
                name = jsonItem.getString("txt"),
                type = type
            )

            LogUtils.json(TAG, "parseResponseGrp:", item)

            newItems.add(item)
        }

        LogUtils.json(TAG, "parseResponseGrp:", newItems)

        return newItems.toList()
    }

    /**
     * Extrait les listes d'agents et prestataires
     * @param jsonResponse Réponse JSON
     * @return Paire de JSONObject (agents, prestataires)
     */
    fun extractAgentsAndPrestataires(jsonResponse: JSONObject): Pair<JSONObject, JSONObject> {
        val dataAgents = jsonResponse.getJSONObject("data").getJSONObject("agts")
        val dataPrestates = jsonResponse.getJSONObject("data").getJSONObject("prestas")

        LogUtils.json(TAG, "extractAgentsAndPrestataires:", dataAgents)
        LogUtils.json(TAG, "extractAgentsAndPrestataires:", dataPrestates)

        return Pair(dataAgents, dataPrestates)
    }

    /**
     * Parse une réponse JSON pour extraire les données de résidence
     * @param jsonResponse Réponse JSON du serveur
     * @param type Type de liste
     * @return Liste d'éléments SelectItem
     */
    fun parseResponseRsd(jsonResponse: JSONObject, type: String): List<SelectItem> {
        val dataArray = jsonResponse.getJSONArray("data")
        val newItems = mutableListOf<SelectItem>()

        LogUtils.json(TAG, "parseResponseRsd dataArray:", dataArray)

        for (i in 0 until dataArray.length()) {
            val jsonItem = dataArray.getJSONObject(i)

            LogUtils.json(TAG, "parseResponseRsd jsonItem:", jsonItem)

            val prop = jsonItem.optJSONObject("prop")?.let { propJson ->
                SelectItem.Prop(
                    zones = parseRsdZones(propJson),
                    ctrl = parseRsdCtrl(propJson)
                )
            }

            LogUtils.json(TAG, "parseResponseRsd prop:", prop)

            val referentsJson = jsonItem.optJSONObject("aff")

            LogUtils.json(TAG, "parseResponseRsd referentsJson:", referentsJson)

            val referents = referentsJson?.let { parseReferentsFromJson(it) }

            LogUtils.json(TAG, "parseResponseRsd referents:", referents)

            val item = SelectItem(
                id = jsonItem.getInt("id"),
                ref = jsonItem.getString("ref"),
                name = jsonItem.getString("name"),
                entry = jsonItem.getString("entry"),
                agency = jsonItem.getInt("agency"),
                group = jsonItem.getInt("group"),
                address = jsonItem.getJSONObject("adr").getString("rue"),
                postalCode = jsonItem.getJSONObject("adr").getString("cp"),
                city = jsonItem.getJSONObject("adr").getString("city"),
                last = jsonItem.getString("last"),
                delay = jsonItem.getBoolean("delay"),
                comment = jsonItem.getString("comment"),
                type = type,
                prop = prop,
                referents = referents
            )

            LogUtils.json(TAG, "parseResponseRsd item:", item)

            newItems.add(item)
        }

        LogUtils.json(TAG, "parseResponseRsd newItems:", newItems)

        return newItems.toList()
    }

    /**
     * Parse les zones d'une propriété
     * @param jsonObj Objet JSON de propriété
     * @return Objet Zones
     */
    private fun parseRsdZones(jsonObj: JSONObject): SelectItem.Prop.Zones {
        val proxiArray = jsonObj.getJSONObject("zones").getJSONArray("proxi")
        val contraArray = jsonObj.getJSONObject("zones").getJSONArray("contra")

        LogUtils.json(TAG, "parseRsdZones proxiArray:", proxiArray)
        LogUtils.json(TAG, "parseRsdZones contraArray:", contraArray)

        val proxiList = (0 until proxiArray.length()).map { proxiArray.getString(it) }
        val contraList = (0 until contraArray.length()).map { contraArray.getString(it) }

        LogUtils.json(TAG, "parseRsdZones proxiList:", proxiList)
        LogUtils.json(TAG, "parseRsdZones contraList:", contraList)

        return SelectItem.Prop.Zones(proxiList, contraList)
    }

    /**
     * Parse les données de contrôle d'une propriété
     * @param jsonObj Objet JSON de propriété
     * @return Objet Ctrl
     */
    private fun parseRsdCtrl(jsonObj: JSONObject): SelectItem.Prop.Ctrl {
        val jsonCtrl = jsonObj.getJSONObject("ctrl")

        LogUtils.json(TAG, "parseRsdCtrl jsonCtrl:", jsonCtrl)

        val note = jsonCtrl.getInt("note")
        val conf = parseRsdConfCtrl(jsonCtrl)
        val date = parseRsdDateCtrl(jsonCtrl)
        val grille = jsonCtrl.optJSONArray("grill") ?: JSONArray()

        val ctrlObj = SelectItem.Prop.Ctrl.fromJsonArray(
            note = note,
            conf = conf,
            date = date,
            grille = grille
        )

        LogUtils.json(TAG, "parseRsdCtrl ctrlObj:", ctrlObj)

        return ctrlObj
    }

    /**
     * Parse la configuration d'un contrôle
     * @param jsonObj Objet JSON de contrôle
     * @return Objet ObjConfig
     */
    private fun parseRsdConfCtrl(jsonObj: JSONObject): ObjConfig {
        val confJson = jsonObj.getJSONObject("conf")
        val confObj = ObjConfig(
            visite = confJson.getBoolean("visite"),
            meteo = confJson.getBoolean("meteo"),
            affichage = confJson.getBoolean("aff"),
            produits = confJson.getBoolean("prod")
        )

        LogUtils.json(TAG, "parseRsdConfCtrl confObj:", confObj)

        return confObj
    }

    /**
     * Parse la date d'un contrôle
     * @param jsonObj Objet JSON de contrôle
     * @return Objet ObjDateCtrl
     */
    private fun parseRsdDateCtrl(jsonObj: JSONObject): ObjDateCtrl {
        val dateJson = jsonObj.getJSONObject("date")
        // Utiliser la date actuelle si la valeur est nulle ou 0
        val dateValue = if (!dateJson.isNull("val") && dateJson.getLong("val") > 0)
            dateJson.getLong("val")
        else
            System.currentTimeMillis()

        LogUtils.json(TAG, "parseRsdDateCtrl dateValue:", dateValue)

        val dateObj = ObjDateCtrl(
            txt = dateJson.getString("txt"),
            value = dateValue
        )

        LogUtils.json(TAG, "parseRsdDateCtrl dateObj:", dateObj)

        return dateObj
    }

}
