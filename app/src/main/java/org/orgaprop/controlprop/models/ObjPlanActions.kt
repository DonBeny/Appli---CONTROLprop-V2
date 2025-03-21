package org.orgaprop.controlprop.models

import java.io.Serializable

data class ObjPlanActions(
    var id: Int,
    var limit: String,
    var txt: String
) : Serializable
