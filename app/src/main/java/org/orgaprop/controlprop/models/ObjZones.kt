package org.orgaprop.controlprop.models

import org.json.JSONArray
import java.io.Serializable

data class ObjZones(
    var proxi: JSONArray = JSONArray(),
    var contrat: JSONArray = JSONArray()
) : Serializable
