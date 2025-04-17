package org.orgaprop.controlprop.managers

import android.content.SharedPreferences
import android.util.Log
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.models.ObjComment
import org.orgaprop.controlprop.models.ObjCriter
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.utils.LogUtils

class CtrlZoneManager(private val sharedPrefs: SharedPreferences) {

    companion object {
        const val TAG = "CtrlZoneManager"
    }



    /**
     * Charge les données de zone pour une entrée et un ID de zone spécifiques
     *
     * @param userData Les données de l'utilisateur contenant la structure
     * @param entrySelected L'entrée sélectionnée
     * @param zoneId L'ID de la zone à charger
     * @return Une paire contenant le nom de la zone et les éléments chargés
     */
     fun loadZoneData(userData: LoginData, entrySelected: SelectItem, zoneId: Int): Pair<String, List<ObjElement>> {
        LogUtils.d(TAG, "loadZoneData: Loading data for zone $zoneId")

        try {
            val structureZone = userData.structure[zoneId.toString()]
                ?: throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Zone $zoneId non trouvée dans la structure")

            val zoneName = structureZone.name
            LogUtils.d(TAG, "loadZoneData: Zone name: $zoneName")

            val elements = structureZone.elmts.mapNotNull { (elementId, element) ->
                LogUtils.json(TAG, "loadZoneData: Processing element $elementId", element)

                if (element.coef > 0) {
                    LogUtils.d(TAG, "loadZoneData: Adding element $elementId to elements list")

                    ObjElement(
                        id = elementId.toInt(),
                        coef = element.coef
                    ).apply {
                        element.critrs.forEach { (critterId, critter) ->
                            LogUtils.json(TAG, "loadZoneData: Processing critter $critterId", critter)

                            if (critter.coef > 0) {
                                LogUtils.d(TAG, "loadZoneData: Adding critter $critterId to criter map")

                                this.addCriter(
                                    ObjCriter(
                                        id = critterId.toInt(),
                                        coefProduct = element.coef * critter.coef
                                    )
                                )
                            }
                        }
                    }.takeIf { it.criterMap.isNotEmpty() }
                } else null
            }
            LogUtils.json(TAG, "loadZoneData: Elements list size: ${elements.size}", elements)

            val savedElements = entrySelected.getZoneElements(zoneId)

            LogUtils.json(TAG, "loadZoneData: Saved elements", savedElements)

            if (savedElements != null) {
                LogUtils.d(TAG, "loadZoneData: Found saved elements for zone $zoneId")
                elements.forEach { element ->
                    savedElements.find { it.id == element.id }?.let { savedElement ->
                        element.note = savedElement.note

                        savedElement.criterMap.forEach { (critterId, savedCritter) ->
                            element.criterMap[critterId]?.let { currentCritter ->
                                currentCritter.note = savedCritter.note
                                currentCritter.comment = savedCritter.comment
                            }
                        }
                    }
                }
            }

            return Pair(zoneName, elements)
        } catch (e: BaseException) {
            LogUtils.e(TAG, "Error in loadZoneData: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            LogUtils.e(TAG, "Unexpected error in loadZoneData", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors du chargement des données de la zone", e)
        }
    }

    /**
     * Calcule la note d'un élément en fonction des critères évalués
     *
     * @param element L'élément à évaluer
     * @param meteoMajoration Si une majoration météo doit être appliquée
     * @return La note calculée de l'élément (0-100)
     */
    fun calculateElementNote(element: ObjElement, meteoMajoration: Boolean): Int {
        try {
            var sumValues = 0
            var sumCoefs = 0
            var hasEvaluatedCriteria = false

            element.criterMap.values.forEach { critter ->
                when (critter.note) {
                    1 -> {
                        hasEvaluatedCriteria = true
                        sumValues += critter.coefProduct
                        sumCoefs += critter.coefProduct
                    }
                    -1 -> {
                        hasEvaluatedCriteria = true
                        sumCoefs += critter.coefProduct
                    }
                }
            }

            if (!hasEvaluatedCriteria) {
                return -1
            }

            if (meteoMajoration && sumValues > 0) {
                sumValues = (sumValues * 1.1).toInt()
            }

            return if (sumCoefs > 0) {
                ((sumValues.toDouble() / sumCoefs) * 100).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error calculating element note", e)
            return 0
        }
    }

    /**
     * Met à jour la valeur d'un critère et recalcule la note de l'élément
     *
     * @param elements La liste des éléments
     * @param elementPosition L'index de l'élément à mettre à jour
     * @param critterPosition L'ID du critère à mettre à jour
     * @param value La nouvelle valeur du critère (1: bon, -1: mauvais, 0: non évalué)
     * @param meteoMajoration Si une majoration météo doit être appliquée
     * @return La liste mise à jour des éléments
     */
    fun updateCritterValue(
        elements: List<ObjElement>,
        elementPosition: Int,
        critterPosition: Int,
        value: Int,
        meteoMajoration: Boolean
    ): List<ObjElement> {
        LogUtils.d(TAG, "updateCritterValue: Updating element $elementPosition, critter $critterPosition to value $value")

        try {
            if (elementPosition >= elements.size) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Position d'élément invalide: $elementPosition")
            }

            val updatedElements = elements.toMutableList()
            val element = updatedElements[elementPosition]
            val critter = element.criterMap[critterPosition]
                ?: throw BaseException(ErrorCodes.INVALID_DATA, "Critère non trouvé à la position $critterPosition")

            critter.note = value

            if (value == -1) {
                critter.comment = ObjComment("", "")
            }

            element.note = calculateElementNote(element, meteoMajoration)

            return updatedElements
        } catch (e: BaseException) {
            LogUtils.e(TAG, "Error in updateCritterValue: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            LogUtils.e(TAG, "Unexpected error in updateCritterValue", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la mise à jour du critère", e)
        }
    }

    /**
     * Met à jour le commentaire d'un critère
     *
     * @param elements La liste des éléments
     * @param elementIndex L'index de l'élément
     * @param critterIndex L'ID du critère
     * @param comment Le texte du commentaire
     * @param imagePath Le chemin de l'image associée au commentaire
     * @return La liste mise à jour des éléments
     */
    fun updateCritterComment(
        elements: List<ObjElement>,
        elementIndex: Int,
        critterIndex: Int,
        comment: String,
        imagePath: String
    ): List<ObjElement> {
        LogUtils.d(TAG, "updateCritterComment: Updating comment for element $elementIndex, critter $critterIndex")

        try {
            if (elementIndex >= elements.size) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Position d'élément invalide: $elementIndex")
            }

            val updatedElements = elements.toMutableList()
            val element = updatedElements[elementIndex]
            val critter = element.criterMap[critterIndex]
                ?: throw BaseException(ErrorCodes.INVALID_DATA, "Critère non trouvé à la position $critterIndex")

            critter.comment = ObjComment(comment, imagePath)

            return updatedElements
        } catch (e: BaseException) {
            LogUtils.e(TAG, "Error in updateCritterComment: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            LogUtils.e(TAG, "Unexpected error in updateCritterComment", e)
            throw BaseException(ErrorCodes.UNKNOWN_ERROR, "Erreur lors de la mise à jour du commentaire", e)
        }
    }

}
