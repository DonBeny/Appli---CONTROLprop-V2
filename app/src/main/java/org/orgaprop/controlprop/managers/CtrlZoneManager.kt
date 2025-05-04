package org.orgaprop.controlprop.managers

import android.content.SharedPreferences

import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.models.ObjComment
import org.orgaprop.controlprop.models.ObjGrilleCritter
import org.orgaprop.controlprop.models.ObjGrilleElement
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
     fun loadZoneData(userData: LoginData, entrySelected: SelectItem, zoneId: Int): Pair<String, List<ObjGrilleElement>> {
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

                    val critters = element.critrs.mapNotNull { (critterId, critter) ->
                        LogUtils.json(TAG, "loadZoneData: Processing critter $critterId", critter)

                        if (critter.coef > 0) {
                            LogUtils.d(TAG, "loadZoneData: Adding critter $critterId to criter list")

                            ObjGrilleCritter(
                                id = critterId.toInt(),
                                name = critter.name,
                                coef = critter.coef,
                                note = 0,
                                comment = ObjComment()
                            )
                        } else null
                    }

                    ObjGrilleElement(
                        id = elementId.toInt(),
                        name = element.name,
                        coef = element.coef,
                        note = -1,
                        critters = critters
                    ).takeIf { it.critters.isNotEmpty() }
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

                        savedElement.critters.forEach { savedCritter ->
                            element.critters.find { it.id == savedCritter.id }?.let { currentCritter ->
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
    fun calculateElementNote(element: ObjGrilleElement, meteoMajoration: Boolean): Int {
        try {
            var sumValues = 0
            var sumCoefs = 0
            var hasEvaluatedCriteria = false

            element.critters.forEach { critter ->
                when (critter.note) {
                    1 -> {
                        hasEvaluatedCriteria = true
                        sumValues += critter.coef * element.coef
                        sumCoefs += critter.coef * element.coef
                    }
                    -1 -> {
                        hasEvaluatedCriteria = true
                        sumCoefs += critter.coef * element.coef
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
        elements: List<ObjGrilleElement>,
        elementPosition: Int,
        critterPosition: Int,
        value: Int,
        meteoMajoration: Boolean
    ): List<ObjGrilleElement> {
        LogUtils.d(TAG, "updateCritterValue: Updating element $elementPosition, critter $critterPosition to value $value")

        try {
            if (elementPosition >= elements.size) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Position d'élément invalide: $elementPosition")
            }

            val updatedElements = elements.toMutableList()
            val element = updatedElements[elementPosition]
            val critter = element.critters.find { it.id == critterPosition }
                ?: throw BaseException(ErrorCodes.INVALID_DATA, "Critère non trouvé à la position $critterPosition")

            val updatedCritters = element.critters.map { crit ->
                if (crit.id == critterPosition) {
                    val updatedComment = if (value == -1) {
                        // Si on passe à -1, on garde le commentaire existant
                        crit.comment
                    } else {
                        // Si on change de -1 à autre chose, on supprime le commentaire
                        ObjComment("", "")
                    }
                    crit.copy(note = value, comment = updatedComment)
                } else {
                    crit
                }
            }

            val updatedElement = element.copy(
                critters = updatedCritters,
                note = calculateElementNote(element.copy(critters = updatedCritters), meteoMajoration)
            )

            updatedElements[elementPosition] = updatedElement

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
        elements: List<ObjGrilleElement>,
        elementIndex: Int,
        critterIndex: Int,
        comment: String,
        imagePath: String
    ): List<ObjGrilleElement> {
        LogUtils.d(TAG, "updateCritterComment: Updating comment for element $elementIndex, critter $critterIndex")

        try {
            if (elementIndex >= elements.size) {
                throw BaseException(ErrorCodes.INVALID_DATA, "Position d'élément invalide: $elementIndex")
            }

            val updatedElements = elements.toMutableList()
            val element = updatedElements[elementIndex]
            val critter = element.critters[critterIndex]

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
