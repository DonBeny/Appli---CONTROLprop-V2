package org.orgaprop.controlprop.models

import java.io.Serializable

data class ObjElement(
    var id: Int = 0,
    var note: Int = -1,
    var coef: Int = 0,
    var criterMap: MutableMap<Int, ObjCriter> = mutableMapOf()
) : Serializable {
    fun addCriter(criter: ObjCriter) {
        criterMap[criterMap.size] = criter
    }
}
