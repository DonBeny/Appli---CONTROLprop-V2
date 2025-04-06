package org.orgaprop.controlprop.managers

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes

class GetMailManager(private val context: Context, private val httpTask: HttpTask) {

    suspend fun submitEmail(email: String): JSONObject {
        val response = httpTask.executeHttpTask(HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION, HttpTaskConstantes.HTTP_TASK_ACT_CONNEXION_CBL_MAIL, "", "email=$email")
        val responseJson = JSONObject(response)

        Log.d("GetMailManager", "Response: $response")
        Log.d("GetMailManager", "Response JSON: $responseJson")

        return responseJson
    }

}
