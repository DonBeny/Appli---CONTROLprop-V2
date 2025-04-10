package org.orgaprop.controlprop.ui.login.mappers

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.models.ObjAgency
import org.orgaprop.controlprop.models.ObjInfo
import org.orgaprop.controlprop.models.ObjLimits
import org.orgaprop.controlprop.models.ObjRapport
import org.orgaprop.controlprop.models.ObjStructureCritter
import org.orgaprop.controlprop.models.ObjStructureElement
import org.orgaprop.controlprop.models.ObjStructureZone
import org.orgaprop.controlprop.ui.login.types.LoginError
import org.orgaprop.controlprop.ui.login.types.LoginResponse

class LoginResponseMapper {

    companion object {
        private const val TAG = "LoginResponseMapper"
    }

    /**
     * Transforme la réponse JSON de login/checkLogin en objet LoginResponse
     *
     * @param responseJson La réponse JSON à parser
     * @return Un objet LoginResponse typé
     * @throws BaseException Si le parsing échoue
     */
    fun parseLoginResponse(responseJson: JSONObject): LoginResponse {
        return try {
            val status = responseJson.getBoolean("status")

            if (status) {
                val data = responseJson.getJSONObject("data")
                LoginResponse(
                    status = true,
                    data = parseLoginData(data),
                    error = null
                )
            } else {
                val error = responseJson.getJSONObject("error")
                LoginResponse(
                    status = false,
                    data = null,
                    error = LoginError(
                        code = error.getInt("code"),
                        txt = error.getString("txt")
                    )
                )
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors du parsing de la réponse de login", e)
            throw BaseException(ErrorCodes.INVALID_RESPONSE, "Réponse JSON invalide", e)
        }
    }

    /**
     * Parse la partie data de la réponse JSON
     */
    private fun parseLoginData(data: JSONObject): LoginData {
        return LoginData(
            agencies = parseAgences(data.getJSONArray("agences")),
            version = data.getInt("version"),
            idMbr = data.getInt("idMbr"),
            adrMac = data.getString("adrMac"),
            mail = data.getString("mail"),
            hasContract = data.getBoolean("hasContrat"),
            info = parseInfoConf(data.getJSONObject("info")),
            limits = parseLimits(data.getJSONObject("limits")),
            planActions = data.getString("planActions"),
            structure = parseStructure(data.getJSONObject("structure"))
        )
    }

    /**
     * Transforme la réponse JSON de logout en objet LoginResponse
     *
     * @param responseJson La réponse JSON à parser
     * @return Un objet LoginResponse typé
     * @throws BaseException Si le parsing échoue
     */
    fun parseLogoutResponse(responseJson: JSONObject): LoginResponse {
        return try {
            val status = responseJson.getBoolean("status")

            if (status) {
                LoginResponse(status = true)
            } else {
                val error = responseJson.getJSONObject("error")
                LoginResponse(
                    status = false,
                    error = LoginError(
                        code = error.getInt("code"),
                        txt = error.getString("txt")
                    )
                )
            }
        } catch (e: JSONException) {
            Log.e(TAG, "Erreur lors du parsing de la réponse de logout", e)
            throw BaseException(ErrorCodes.INVALID_RESPONSE, "Réponse JSON invalide", e)
        }
    }

    /**
     * Parse la liste des agences depuis un JSONArray
     */
    private fun parseAgences(agencesArray: JSONArray): List<ObjAgency> {
        return List(agencesArray.length()) { i ->
            val agence = agencesArray.getJSONObject(i)
            ObjAgency(
                id = agence.getInt("id"),
                nom = agence.getString("nom"),
                tech = agence.getString("tech"),
                contact = agence.getString("contact")
            )
        }
    }

    /**
     * Parse les informations de configuration
     */
    private fun parseInfoConf(infoObject: JSONObject): ObjInfo {
        return ObjInfo(
            aff = infoObject.getString("aff"),
            prod = infoObject.getString("prod")
        )
    }

    /**
     * Parse les informations de limites
     */
    private fun parseLimits(limitsObject: JSONObject): ObjLimits {
        return ObjLimits(
            top = limitsObject.getInt("top"),
            down = limitsObject.getInt("down"),
            rapport = parseRapport(limitsObject.getJSONObject("rapport"))
        )
    }

    /**
     * Parse les informations de rapport
     */
    private fun parseRapport(rapportObject: JSONObject): ObjRapport {
        return ObjRapport(
            value = rapportObject.getInt("value"),
            dest = rapportObject.getString("dest")
        )
    }

    /**
     * Parse la structure complète de zones
     */
    private fun parseStructure(structureObject: JSONObject): Map<String, ObjStructureZone> {
        val structureMap = mutableMapOf<String, ObjStructureZone>()

        val keys = structureObject.keys()
        while (keys.hasNext()) {
            val sectionId = keys.next()
            val sectionJson = structureObject.getJSONObject(sectionId)

            val name = sectionJson.getString("name")
            val coef = sectionJson.getInt("coef")

            val elmtsJson = sectionJson.getJSONObject("elmts")
            val elmtsMap = parseStructureElements(elmtsJson)

            structureMap[sectionId] = ObjStructureZone(coef, name, elmtsMap)
        }

        return structureMap
    }

    /**
     * Parse les éléments de structure
     */
    private fun parseStructureElements(elmtsJson: JSONObject): Map<String, ObjStructureElement> {
        val elmtsMap = mutableMapOf<String, ObjStructureElement>()

        val keys = elmtsJson.keys()
        while (keys.hasNext()) {
            val elementId = keys.next()
            val elementJson = elmtsJson.getJSONObject(elementId)

            val name = elementJson.getString("name")
            val coef = elementJson.getInt("coef")

            val critrsJson = elementJson.getJSONObject("critrs")
            val critrsMap = parseCriters(critrsJson)

            elmtsMap[elementId] = ObjStructureElement(coef, name, critrsMap)
        }

        return elmtsMap
    }

    /**
     * Parse les critères
     */
    private fun parseCriters(critrsJson: JSONObject): Map<String, ObjStructureCritter> {
        val critrsMap = mutableMapOf<String, ObjStructureCritter>()

        val keys = critrsJson.keys()
        while (keys.hasNext()) {
            val critreId = keys.next()
            val critreJson = critrsJson.getJSONObject(critreId)

            val name = critreJson.getString("name")
            val coef = critreJson.getInt("coef")

            critrsMap[critreId] = ObjStructureCritter(coef, name)
        }

        return critrsMap
    }

}
