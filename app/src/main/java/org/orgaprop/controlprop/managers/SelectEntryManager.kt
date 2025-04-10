package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.BuildConfig
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes
import java.io.IOException

class SelectEntryManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    companion object {
        private const val TAG = "SelectEntryManager"
    }



    /**
     * Effectue la déconnexion de l'utilisateur
     * @param idMbr ID de l'utilisateur
     * @param adrMac Adresse MAC de l'appareil
     * @return JSONObject contenant la réponse du serveur
     * @throws BaseException en cas d'erreur
     */
    suspend fun logout(idMbr: Int, adrMac: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val paramsPost = JSONObject().apply {
                    put("mbr", idMbr)
                    put("mac", adrMac)
                }.toString()

                val response = httpTask.executeHttpTask(
                    HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION,
                    HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION_CBL_LOGOUT,
                    "",
                    paramsPost
                )

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "logout: response = $response")
                }

                val jsonObject = JSONObject(response)

                // Vérifier si la déconnexion a réussi
                if (!jsonObject.optBoolean("status", false)) {
                    throw BaseException(ErrorCodes.LOGOUT_FAILED)
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "logout: jsonObject = $jsonObject")
                }

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

}
