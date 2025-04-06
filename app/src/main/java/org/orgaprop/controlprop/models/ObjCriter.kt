package org.orgaprop.controlprop.models

import java.io.Serializable

data class ObjCriter(
    var id: Int = 0,
    var note: Int = 0,
    var coefProduct: Int = 0,
    var comment: ObjComment = ObjComment()
) : Serializable
