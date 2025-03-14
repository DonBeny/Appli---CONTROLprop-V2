package org.orgaprop.controlprop.models

import java.io.Serializable

data class ObjZone(
    var id: Int = 0,
    var note: Int = -1,
    var elementMap: MutableMap<Int, ObjElement> = mutableMapOf()
) : Serializable {
    fun addElement(element: ObjElement) {
        elementMap[elementMap.size] = element
    }
}
