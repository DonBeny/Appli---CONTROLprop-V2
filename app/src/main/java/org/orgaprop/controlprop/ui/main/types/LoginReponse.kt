package org.orgaprop.controlprop.ui.main.types

data class LoginResponse(
    val status: Boolean,
    val data: LoginData? = null,
    val error: LoginError? = null
)

data class LoginData(
    val agences: List<AgenceType>,
    val version: Int,
    val idMbr: Int,
    val adrMac: String,
    val mail: String,
    val hasContrat: Boolean,
    val info: InfoConf,
    val limits: Limits,
    val planActions: String,
    val structure: Map<String, StructureZone>
)



data class AgenceType(
    // Définir les propriétés de l'agence
    val id: Int,
    val nom: String,
    val tech: String,
    val contact: String
)

data class InfoConf(
    val aff: String,
    val prod: String
)

data class Limits(
    val top: Int,
    val down: Int,
    val rapport: Rapport
)

data class Rapport(
    val value: Int,
    val dest: String
)

data class StructureZone(
    val coef: Int,
    val name: String,
    val elmts: Map<String, StructureElement>
)
data class StructureElement(
    val coef: Int,
    val name: String,
    val critrs: Map<String, StructureCriter>
)
data class StructureCriter(
    val coef: Int,
    val name: String
)



data class LoginError(
    val code: Int,
    val txt: String
)
