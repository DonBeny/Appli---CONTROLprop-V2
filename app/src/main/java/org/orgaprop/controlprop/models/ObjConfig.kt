package org.orgaprop.controlprop.models

import java.io.Serializable

data class ObjConfig(
    var visite: Boolean = false,
    var meteo: Boolean = false,
    var affichage: Boolean = true,
    var produits: Boolean = true,
) : Serializable
