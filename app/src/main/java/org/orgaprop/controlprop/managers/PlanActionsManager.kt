package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.models.ObjPlanActions
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes
import java.io.IOException

class PlanActionsManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    private val TAG = "PlanActionsManager"

    suspend fun fetchPlanActions(idRsd: Int, idMbr: Int, adrMac: String): JSONObject {
        return try {
            val paramGet = "mod=" + HttpTaskConstantes.HTTP_TASK_ACT_PROP_MOD_GET
            val paramsPost = JSONObject().apply {
                put("mbr", idMbr)
                put("mac", adrMac)
                put("rsd", idRsd)
            }.toString()

            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_PROP, HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_PLAN_ACTIONS, paramGet, paramsPost)

            Log.d(TAG, "fetchPlanAction: response = $response")

            val jsonObject = JSONObject(response)

            Log.d(TAG, "fetchPlanAction: jsonObject = $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw PlanActionsException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw PlanActionsException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw PlanActionsException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

    suspend fun savePlanAction(planAction: ObjPlanActions, idRsd: Int, idMbr: Int, adrMac: String): JSONObject {
        return try {
            val paramGet = "mod=" + HttpTaskConstantes.HTTP_TASK_ACT_PROP_MOD_SET
            val paramsPost = JSONObject().apply {
                put("mbr", idMbr)
                put("mac", adrMac)
                put("rsd", idRsd)
                put("limitPlan", planAction.limit)
                put("txtPlan", planAction.txt)
            }.toString()

            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_PROP, HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_PLAN_ACTIONS, paramGet, paramsPost)

            Log.d(TAG, "savePlanAction: response = $response")

            val jsonObject = JSONObject(response)

            Log.d(TAG, "savePlanAction: jsonObject = $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw PlanActionsException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw PlanActionsException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw PlanActionsException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

    suspend fun validatePlanAction(idPlan: Int, idMbr: Int, adrMac: String): JSONObject {
        return try {
            val paramGet = "mod=" + HttpTaskConstantes.HTTP_TASK_ACT_PROP_MOD_SET
            val paramsPost = JSONObject().apply {
                put("mbr", idMbr)
                put("mac", adrMac)
                put("plan", idPlan)
            }.toString()

            val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_PROP, HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_PLAN_ACTIONS, paramGet, paramsPost)

            Log.d(TAG, "validatePlanAction: response = $response")

            val jsonObject = JSONObject(response)

            Log.d(TAG, "validatePlanAction: jsonObject = $jsonObject")

            jsonObject
        } catch (e: IOException) {
            throw PlanActionsException(ErrorCodes.NETWORK_ERROR, e)
        } catch (e: JSONException) {
            throw PlanActionsException(ErrorCodes.INVALID_RESPONSE, e)
        } catch (e: Exception) {
            throw PlanActionsException(ErrorCodes.UNKNOWN_ERROR, e)
        }
    }

    class PlanActionsException(
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