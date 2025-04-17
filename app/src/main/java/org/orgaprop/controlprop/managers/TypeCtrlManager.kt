package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes
import java.io.IOException

class TypeCtrlManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    companion object {
        private const val TAG = "TypeCtrlManager"
    }



    suspend fun fetchPlanAction(rsdId: Int, idMbr: Int, adrMac: String): JSONObject {
        return try {
            val paramGet = "mod=" + HttpTaskConstantes.HTTP_TASK_ACT_PROP_MOD_GET
            val paramsPost = JSONObject().apply {
                put("mbr", idMbr)
                put("mac", adrMac)
                put("rsd", rsdId)
            }.toString()

            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_PROP, HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_PLAN_ACTIONS, paramGet, paramsPost)

            LogUtils.json(TAG, "fetchPlanAction: response:", response)

            val jsonObject = JSONObject(response)

            LogUtils.json(TAG, "fetchPlanAction: jsonObject", jsonObject)

            jsonObject
        } catch (e: IOException) {
            throw TypeCtrlException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw TypeCtrlException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw TypeCtrlException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }



    class TypeCtrlException(
        code: Int,
        cause: Throwable? = null
    ) : BaseException(code, getMessageForCode(code), cause) {

        companion object {
            /**
             * Retourne un message par défaut en fonction du code d'erreur.
             *
             * @param code Le code d'erreur.
             * @return Le message correspondant au code d'erreur.
             */
            private fun getMessageForCode(code: Int): String {
                return when (code) {
                    ErrorCodes.NETWORK_ERROR -> "Erreur réseau lors de la connexion"
                    ErrorCodes.INVALID_RESPONSE -> "Réponse serveur invalide"
                    else -> "Une erreur inconnue s'est produite"
                }
            }
        }

    }

}
