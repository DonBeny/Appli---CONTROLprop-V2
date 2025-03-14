package org.orgaprop.controlprop.models

import java.io.Serializable

data class ObjProp(
    var objConfig: ObjConfig = ObjConfig(),
    var objZones: ObjZones? = null,
    var objDateCtrl: ObjDateCtrl? = null,
    var note: Int = 0,
    var grille: ObjGrille? = null
) : Serializable
