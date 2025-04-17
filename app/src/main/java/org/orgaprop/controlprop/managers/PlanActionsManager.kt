package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.models.ObjPlanActions
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes
import java.io.IOException

class PlanActionsManager(
    private val context: Context,
    private val httpTask: HttpTask
) {

    companion object {
        private const val TAG = "PlanActionsManager"
    }



    /**
     * Récupère les plans d'actions pour une résidence donnée.
     *
     * @param idRsd Identifiant de la résidence
     * @param idMbr Identifiant du membre
     * @param adrMac Adresse MAC de l'appareil
     * @return JSONObject contenant la réponse du serveur
     * @throws PlanActionsException en cas d'erreur
     */
    suspend fun fetchPlanActions(idRsd: Int, idMbr: Int, adrMac: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val paramGet = "mod=${HttpTaskConstantes.HTTP_TASK_ACT_PROP_MOD_GET}"
                val paramsPost = JSONObject().apply {
                    put("mbr", idMbr)
                    put("mac", adrMac)
                    put("rsd", idRsd)
                }.toString()

                val response = httpTask.executeHttpTask(
                    HttpTaskConstantes.HTTP_TASK_ACT_PROP,
                    HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_PLAN_ACTIONS,
                    paramGet,
                    paramsPost
                )

                LogUtils.json(TAG,"Response for fetchPlanAction:", response)
                JSONObject(response)
            } catch (e: IOException) {
                LogUtils.e(TAG, "Erreur réseau lors de la récupération du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.NETWORK_ERROR, e)
            } catch (e: JSONException) {
                LogUtils.e(TAG, "Réponse invalide lors de la récupération du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.INVALID_RESPONSE, e)
            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur inconnue lors de la récupération du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.UNKNOWN_ERROR, e)
            }
        }
    }

    /**
     * Sauvegarde un plan d'actions.
     *
     * @param planAction Plan d'actions à sauvegarder
     * @param idRsd Identifiant de la résidence
     * @param idMbr Identifiant du membre
     * @param adrMac Adresse MAC de l'appareil
     * @return JSONObject contenant la réponse du serveur
     * @throws PlanActionsException en cas d'erreur
     */
    suspend fun savePlanAction(planAction: ObjPlanActions, idRsd: Int, idMbr: Int, adrMac: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val paramGet = "mod=${HttpTaskConstantes.HTTP_TASK_ACT_PROP_MOD_SET}"
                val paramsPost = JSONObject().apply {
                    put("mbr", idMbr)
                    put("mac", adrMac)
                    put("rsd", idRsd)
                    put("limitPlan", planAction.limit)
                    put("txtPlan", planAction.txt)
                }.toString()

                val response = httpTask.executeHttpTask(
                    HttpTaskConstantes.HTTP_TASK_ACT_PROP,
                    HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_PLAN_ACTIONS,
                    paramGet,
                    paramsPost
                )

                LogUtils.json(TAG, "Response for savePlanAction:", response)
                JSONObject(response)
            } catch (e: IOException) {
                LogUtils.e(TAG, "Erreur réseau lors de la sauvegarde du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.NETWORK_ERROR, e)
            } catch (e: JSONException) {
                LogUtils.e(TAG, "Réponse invalide lors de la sauvegarde du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.INVALID_RESPONSE, e)
            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur inconnue lors de la sauvegarde du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.UNKNOWN_ERROR, e)
            }
        }
    }

    /**
     * Valide (lève) un plan d'actions.
     *
     * @param idPlan Identifiant du plan d'actions
     * @param idMbr Identifiant du membre
     * @param adrMac Adresse MAC de l'appareil
     * @return JSONObject contenant la réponse du serveur
     * @throws PlanActionsException en cas d'erreur
     */
    suspend fun validatePlanAction(idPlan: Int, idMbr: Int, adrMac: String): JSONObject {
        return withContext(Dispatchers.IO) {
            try {
                val paramGet = "mod=${HttpTaskConstantes.HTTP_TASK_ACT_PROP_MOD_SET}"
                val paramsPost = JSONObject().apply {
                    put("mbr", idMbr)
                    put("mac", adrMac)
                    put("plan", idPlan)
                }.toString()

                val response = httpTask.executeHttpTask(
                    HttpTaskConstantes.HTTP_TASK_ACT_PROP,
                    HttpTaskConstantes.HTTP_TASK_ACT_PROP_CBL_PLAN_ACTIONS,
                    paramGet,
                    paramsPost
                )

                LogUtils.json(TAG, "Response for validatePlanAction:", response)
                JSONObject(response)
            } catch (e: IOException) {
                LogUtils.e(TAG, "Erreur réseau lors de la validation du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.NETWORK_ERROR, e)
            } catch (e: JSONException) {
                LogUtils.e(TAG, "Réponse invalide lors de la validation du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.INVALID_RESPONSE, e)
            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur inconnue lors de la validation du plan d'actions", e)
                throw PlanActionsException(ErrorCodes.UNKNOWN_ERROR, e)
            }
        }
    }

    /**
     * Exception spécifique pour les opérations liées aux plans d'actions.
     */
    class PlanActionsException(
        code: Int,
        cause: Throwable? = null
    ) : BaseException(code, getMessageForCode(code), cause) {

        companion object {
            /**
             * Retourne un message spécifique pour les erreurs de plan d'actions.
             *
             * @param code Le code d'erreur.
             * @return Le message correspondant au code d'erreur.
             */
            private fun getMessageForCode(code: Int): String {
                return when (code) {
                    ErrorCodes.NETWORK_ERROR -> "Erreur réseau lors de la connexion au serveur"
                    ErrorCodes.INVALID_RESPONSE -> "Réponse du serveur invalide ou incomplète"
                    ErrorCodes.DATA_NOT_FOUND -> "Aucun plan d'actions trouvé"
                    ErrorCodes.INVALID_DATA -> "Données du plan d'actions invalides"
                    ErrorCodes.PLAN_ACTION_NOT_AVAILABLE -> "Le plan d'actions n'est pas disponible"
                    else -> "Une erreur est survenue lors du traitement du plan d'actions"
                }
            }
        }

    }

}