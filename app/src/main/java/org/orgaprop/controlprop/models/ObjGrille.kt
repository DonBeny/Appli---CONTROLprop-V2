package org.orgaprop.controlprop.models

import java.io.Serializable

data class ObjGrille(
    var zoneMap: MutableMap<Int, ObjZone> = mutableMapOf()
) : Serializable {
    fun addZone(zone: ObjZone) {
        zoneMap[zoneMap.size] = zone
    }
}
