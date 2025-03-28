package org.orgaprop.controlprop.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectItem(
    val id: Int,
    val agency: Int = 0,
    val group: Int = 0,
    val ref: String = "",
    val name: String = "",
    val entry: String = "",
    val address: String = "",
    val postalCode: String = "",
    val city: String = "",
    val last: String = "",
    val delay: Boolean = false,
    val comment: String = "",
    val type: String = ""
) : Parcelable
