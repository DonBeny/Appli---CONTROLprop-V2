package org.orgaprop.controlprop.models

import android.os.Parcelable

import kotlinx.parcelize.Parcelize

import java.time.Instant
import java.time.ZoneId

@Parcelize
data class ObjDateCtrl(
    var value: Long = 0,
    var txt: String = ""
) : Parcelable {

    fun isToday(): Boolean {
        return try {
            val controlTime = this.value.toString().toLong()
            val now = Instant.now().epochSecond
            val controlDate = Instant.ofEpochSecond(controlTime).atZone(ZoneId.systemDefault()).toLocalDate()
            val today = Instant.ofEpochSecond(now).atZone(ZoneId.systemDefault()).toLocalDate()
            controlDate == today
        } catch (e: Exception) {
            false
        }
    }

}
