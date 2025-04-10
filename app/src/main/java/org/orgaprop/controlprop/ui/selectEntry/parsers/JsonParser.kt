package org.orgaprop.controlprop.ui.selectEntry.parsers

import org.json.JSONArray
import org.json.JSONObject
import org.orgaprop.controlprop.models.ObjConfig
import org.orgaprop.controlprop.models.ObjDateCtrl
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.parseReferentsFromJson

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

        for (i in 0 until dataArray.length()) {
            val jsonItem = dataArray.getJSONObject(i)

            val item = SelectItem(
                id = jsonItem.getInt("id"),
                name = jsonItem.getString("txt"),
                type = type
            )

            newItems.add(item)
        }

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

        for (i in 0 until dataArray.length()) {
            val jsonItem = dataArray.getJSONObject(i)

            val prop = jsonItem.optJSONObject("prop")?.let { propJson ->
                SelectItem.Prop(
                    zones = parseRsdZones(propJson),
                    ctrl = parseRsdCtrl(propJson)
                )
            }

            val referentsJson = jsonItem.optJSONObject("aff")
            val referents = referentsJson?.let { parseReferentsFromJson(it) }

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

            newItems.add(item)
        }

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

        val proxiList = (0 until proxiArray.length()).map { proxiArray.getString(it) }
        val contraList = (0 until contraArray.length()).map { contraArray.getString(it) }

        return SelectItem.Prop.Zones(proxiList, contraList)
    }

    /**
     * Parse les données de contrôle d'une propriété
     * @param jsonObj Objet JSON de propriété
     * @return Objet Ctrl
     */
    private fun parseRsdCtrl(jsonObj: JSONObject): SelectItem.Prop.Ctrl {
        val jsonCtrl = jsonObj.getJSONObject("ctrl")

        val note = jsonCtrl.getInt("note")
        val conf = parseRsdConfCtrl(jsonCtrl)
        val date = parseRsdDateCtrl(jsonCtrl)
        val grille = jsonCtrl.optJSONArray("grille") ?: JSONArray()

        return SelectItem.Prop.Ctrl.fromJsonArray(
            note = note,
            conf = conf,
            date = date,
            grille = grille
        )
    }

    /**
     * Parse la configuration d'un contrôle
     * @param jsonObj Objet JSON de contrôle
     * @return Objet ObjConfig
     */
    private fun parseRsdConfCtrl(jsonObj: JSONObject): ObjConfig {
        val confJson = jsonObj.getJSONObject("conf")

        return ObjConfig(
            visite = confJson.getBoolean("visite"),
            meteo = confJson.getBoolean("meteo"),
            affichage = confJson.getBoolean("aff"),
            produits = confJson.getBoolean("prod")
        )
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

        return ObjDateCtrl(
            txt = dateJson.getString("txt"),
            value = dateValue
        )
    }

}
