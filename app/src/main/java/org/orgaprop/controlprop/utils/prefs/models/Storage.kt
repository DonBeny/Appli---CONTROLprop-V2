package org.orgaprop.controlprop.utils.prefs.models

import android.content.ContentValues
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.utils.prefs.configPrefDatabase

@Entity(tableName = configPrefDatabase.STORAGE_TABLE_NAME)
data class Storage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_ID)
    var id: Long = 0,

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_RESID)
    var resid: Int = 0,

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_DATE)
    var date: Int = 0,

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_CONFIG)
    var config: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_TYPE_CTRL)
    var typeCtrl: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_CTRL_TYPE)
    var ctrl_type: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_CTRL_CTRL)
    var ctrl_ctrl: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_CTRL_SIG1)
    var ctrl_sig1: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_CTRL_SIG2)
    var ctrl_sig2: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_CTRL_SIG)
    var ctrl_sig: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_PLAN_END)
    var plan_end: Int = 0,

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_PLAN_CONTENT)
    var plan_content: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_PLAN_VALIDATE)
    var plan_validate: Boolean = false,

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_SEND_DEST)
    var send_dest: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_SEND_ID_PLAN)
    var send_idPlan: Int = 0,

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_SEND_DATE_CTRL)
    var send_dateCtrl: Int = 0,

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_SEND_TYPE_CTRL)
    var send_typeCtrl: String = "",

    @ColumnInfo(name = configPrefDatabase.STORAGE_COL_SEND_SRC)
    var send_src: String = ""
) {
    @Ignore
    constructor() : this(0)

    fun isValid(): Boolean {
        return resid > 0 && config.isNotBlank() && typeCtrl.isNotBlank()
    }

    companion object {
        fun fromContentValues(values: ContentValues): Storage {
            val storage = Storage()

            if (values.containsKey("id")) {
                storage.id = values.getAsLong("id")
            }

            if (values.containsKey("resid")) {
                storage.resid = values.getAsInteger("resid")
            }
            if (values.containsKey("rsd")) {
                storage.resid = values.getAsInteger("rsd")
            }

            if (values.containsKey("date")) {
                storage.date = values.getAsInteger("date")
            }
            if (values.containsKey("ctrl_date")) {
                storage.date = values.getAsInteger("ctrl_date")
            }
            if (values.containsKey("date_ctrl")) {
                storage.date = values.getAsInteger("date_ctrl")
            }

            if (values.containsKey("conf")) {
                storage.config = values.getAsString("conf") ?: ""
            }
            if (values.containsKey("config")) {
                storage.config = values.getAsString("config") ?: ""
            }

            if (values.containsKey("type")) {
                storage.typeCtrl = values.getAsString("type") ?: ""
                storage.send_src = values.getAsString("type") ?: ""
            }

            if (values.containsKey("type_ctrl")) {
                storage.ctrl_type = values.getAsString("type_ctrl") ?: ""
                storage.send_typeCtrl = values.getAsString("type_ctrl") ?: ""
            }
            if (values.containsKey("ctrl_type")) {
                storage.ctrl_type = values.getAsString("ctrl_type") ?: ""
                storage.send_typeCtrl = values.getAsString("ctrl_type") ?: ""
            }

            if (values.containsKey("grill")) {
                storage.ctrl_ctrl = values.getAsString("grill") ?: ""
            }
            if (values.containsKey("grille")) {
                storage.ctrl_ctrl = values.getAsString("grille") ?: ""
            }
            if (values.containsKey("ctrl")) {
                storage.ctrl_ctrl = values.getAsString("ctrl") ?: ""
            }

            if (values.containsKey("sig1")) {
                storage.ctrl_sig1 = values.getAsString("sig1") ?: ""
            }
            if (values.containsKey("sig_controleur")) {
                storage.ctrl_sig1 = values.getAsString("sig_controleur") ?: ""
            }
            if (values.containsKey("signature")) {
                storage.ctrl_sig1 = values.getAsString("signature") ?: ""
            }
            if (values.containsKey("sign")) {
                storage.ctrl_sig1 = values.getAsString("sign") ?: ""
            }
            if (values.containsKey("sig_controleur")) {
                storage.ctrl_sig1 = values.getAsString("sig_controleur") ?: ""
            }
            if (values.containsKey("sign_controleur")) {
                storage.ctrl_sig1 = values.getAsString("sign_controleur") ?: ""
            }
            if (values.containsKey("signature_controleur")) {
                storage.ctrl_sig1 = values.getAsString("signature_controleur") ?: ""
            }

            if (values.containsKey("sig2")) {
                storage.ctrl_sig2 = values.getAsString("sig2") ?: ""
            }
            if (values.containsKey("sig_agent")) {
                storage.ctrl_sig2 = values.getAsString("sig_agent") ?: ""
            }
            if (values.containsKey("sig_contradictoir")) {
                storage.ctrl_sig2 = values.getAsString("sig_contradictoir") ?: ""
            }
            if (values.containsKey("sig_contradictoire")) {
                storage.ctrl_sig2 = values.getAsString("sig_contradictoire") ?: ""
            }
            if (values.containsKey("sign_contradictoir")) {
                storage.ctrl_sig2 = values.getAsString("sign_contradictoir") ?: ""
            }
            if (values.containsKey("sign_contradictoire")) {
                storage.ctrl_sig2 = values.getAsString("sign_contradictoire") ?: ""
            }
            if (values.containsKey("signature_contradictoir")) {
                storage.ctrl_sig2 = values.getAsString("signature_contradictoir") ?: ""
            }
            if (values.containsKey("signature_contradictoire")) {
                storage.ctrl_sig2 = values.getAsString("signature_contradictoire") ?: ""
            }

            if (values.containsKey("sig")) {
                storage.ctrl_sig = values.getAsString("sig") ?: ""
            }
            if (values.containsKey("agent")) {
                storage.ctrl_sig = values.getAsString("agent") ?: ""
            }
            if (values.containsKey("nom_agent")) {
                storage.ctrl_sig = values.getAsString("nom_agent") ?: ""
            }
            if (values.containsKey("contradicteur")) {
                storage.ctrl_sig = values.getAsString("contradicteur") ?: ""
            }
            if (values.containsKey("contradictoir")) {
                storage.ctrl_sig = values.getAsString("contradictoir") ?: ""
            }
            if (values.containsKey("contradictoire")) {
                storage.ctrl_sig = values.getAsString("contradictoire") ?: ""
            }

            if (values.containsKey("plan_end")) {
                storage.plan_end = values.getAsInteger("plan_end")
            }
            if (values.containsKey("plan_fin")) {
                storage.plan_end = values.getAsInteger("plan_fin")
            }
            if (values.containsKey("echeance")) {
                storage.plan_end = values.getAsInteger("echeance")
            }

            if (values.containsKey("plan_content")) {
                storage.plan_content = values.getAsString("plan_content") ?: ""
            }
            if (values.containsKey("plan_txt")) {
                storage.plan_content = values.getAsString("plan_txt") ?: ""
            }

            if (values.containsKey("plan_valid")) {
                storage.plan_validate = values.getAsBoolean("plan_valid")
            }
            if (values.containsKey("plan_validate")) {
                storage.plan_validate = values.getAsBoolean("plan_validate")
            }

            if (values.containsKey("dest")) {
                storage.send_dest = values.getAsString("dest") ?: ""
            }
            if (values.containsKey("send_dest")) {
                storage.send_dest = values.getAsString("send_dest") ?: ""
            }
            if (values.containsKey("dest_send")) {
                storage.send_dest = values.getAsString("dest_send") ?: ""
            }
            if (values.containsKey("destinataire")) {
                storage.send_dest = values.getAsString("destinataire") ?: ""
            }

            if (values.containsKey("send_idPlan")) {
                storage.send_idPlan = values.getAsInteger("send_idPlan")
            }
            if (values.containsKey("send_plan")) {
                storage.send_idPlan = values.getAsInteger("send_plan")
            }

            if (values.containsKey("send_dateCtrl")) {
                storage.send_dateCtrl = values.getAsInteger("send_dateCtrl")
            }
            if (values.containsKey("send_date")) {
                storage.send_dateCtrl = values.getAsInteger("send_date")
            }

            return storage
        }
    }

    fun toJSON(): JSONObject {
        val result = JSONObject()
        val objCtrl = JSONObject()
        val objPlan = JSONObject()
        val objSend = JSONObject()

        try {
            result.put("rsd", resid)
            result.put("date", date)
            result.put("type", typeCtrl)
            result.put("conf", config)

            objCtrl.put("type", ctrl_type)
            objCtrl.put("grill", ctrl_ctrl)
            objCtrl.put("sig1", ctrl_sig1)
            objCtrl.put("sig2", ctrl_sig2)
            objCtrl.put("agt", ctrl_sig)
            result.put("ctrl", objCtrl)

            objPlan.put("end", plan_end)
            objPlan.put("txt", plan_content)
            objPlan.put("close", plan_validate)
            result.put("plan", objPlan)

            objSend.put("dest", send_dest)
            objSend.put("plan", send_idPlan)
            objSend.put("ctrl", send_dateCtrl)
            objSend.put("type", send_typeCtrl)
            objSend.put("src", send_src)
            result.put("send", objSend)
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return result
    }
}