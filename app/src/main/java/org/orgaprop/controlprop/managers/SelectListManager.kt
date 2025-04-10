package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException

import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.ui.selectEntry.SelectListActivity
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes

class SelectListManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    companion object {
        private const val TAG = "SelectListManager"
    }



    /**
     * Récupère les données de la liste en fonction du type et des paramètres
     * @param type Type de liste (agc, grp, rsd, search)
     * @param parentId ID du parent (agence ou groupement)
     * @param searchQuery Termes de recherche (pour le type search)
     * @param idMbr ID de l'utilisateur
     * @param adrMac Adresse MAC de l'appareil
     * @return JSONObject contenant les données de la liste
     * @throws BaseException en cas d'erreur
     */
    suspend fun fetchData(
        type: String,
        parentId: Int,
        searchQuery: String,
        idMbr: Int,
        adrMac: String
    ): JSONObject {
        val getString = "val=$parentId"
        var postString = "mbr=$idMbr&mac=$adrMac"

        if (type == SelectListActivity.SELECT_LIST_TYPE_SEARCH) {
            postString += "&search=$searchQuery"
        }

        return try {
            val response = httpTask.executeHttpTask(
                HttpTaskConstantes.HTTP_TASK_ACT_LIST,
                type,
                getString,
                postString
            )

            Log.d(TAG, "fetchData response : $response")

            val jsonObject = JSONObject(response)

            if (!jsonObject.optBoolean("status", false) && jsonObject.has("error")) {
                val errorObj = jsonObject.getJSONObject("error")
                val errorMsg = errorObj.optString("txt", "Erreur inconnue")
                throw BaseException(ErrorCodes.DATA_NOT_FOUND, errorMsg)
            }

            Log.d(TAG, "fetchData jsonObject : $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw BaseException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw BaseException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: BaseException) {
            throw e
        } catch (e: Exception) {
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

}
