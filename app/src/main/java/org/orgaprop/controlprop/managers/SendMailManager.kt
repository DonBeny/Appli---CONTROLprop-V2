package org.orgaprop.controlprop.managers

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.orgaprop.controlprop.ui.sendMail.SendMailActivity
import org.orgaprop.controlprop.utils.FileUtils
import org.orgaprop.controlprop.utils.HttpTask
import org.orgaprop.controlprop.utils.network.HttpTaskConstantes

class SendMailManager(
    private val httpTask: HttpTask,
    private val context: Context
) {

    private val TAG = "SendMailManager"



    suspend fun sendMail(
        idMbr: Int,
        adrMac: String,
        typeSend: String?,
        dest1: String,
        dest2: String,
        dest3: String,
        dest4: String,
        message: String,
        bitmap: Bitmap?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val imageFile = bitmap?.let { FileUtils.saveBitmapToCache(context, it, "mail_attachment.jpg") }
            val paramsPost = mutableListOf(
                "dest1" to dest1,
                "dest2" to dest2,
                "dest3" to dest3,
                "dest4" to dest4,
                "msg" to message,
                "idMbr" to idMbr,
                "adrMac" to adrMac
            )
            val cblParam = when (typeSend) {
                SendMailActivity.SEND_MAIL_ACTIVITY_PROBLEM_TECH -> HttpTaskConstantes.HTTP_TASK_ACT_SEND_CBL_TECH
                SendMailActivity.SEND_MAIL_ACTIVITY_CTRL -> HttpTaskConstantes.HTTP_TASK_ACT_SEND_CBL_CTRL
                SendMailActivity.SEND_MAIL_ACTIVITY_PLAN -> HttpTaskConstantes.HTTP_TASK_ACT_SEND_CBL_PLAN
                SendMailActivity.SEND_MAIL_ACTIVITY_AUTO -> HttpTaskConstantes.HTTP_TASK_ACT_SEND_CBL_AUTO
                else -> ""
            }

            imageFile?.let { file ->
                paramsPost.add("image" to file.readBytes().toString(Charsets.ISO_8859_1))
                file.delete()
            }

            val response = httpTask.executeHttpTask(
                HttpTaskConstantes.HTTP_TASK_ACT_SEND,
                cblParam,
                "",
                paramsPost.joinToString("&") { "${it.first}=${it.second}" },
            )

            Log.d(TAG, "logout: response = $response")

            val jsonObject = JSONObject(response)

            Log.d(TAG, "logout: jsonObject = $jsonObject")

            if (jsonObject.getBoolean("status")) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(jsonObject.getJSONObject("error").getString("txt")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
