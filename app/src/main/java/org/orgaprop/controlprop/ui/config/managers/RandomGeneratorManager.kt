package org.orgaprop.controlprop.ui.config.managers

import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.utils.LogUtils

class RandomGeneratorManager {

    companion object {
        private const val TAG = "RandomGeneratorManager"

        private const val MAX_ZONES_PER_ENTRY = 4


        /**
         * Génère une liste de contrôles aléatoires
         *
         * @param entries Liste complète des entrées disponibles
         * @param useProxi Indique si les zones proxi doivent être incluses
         * @param useContract Indique si les zones contrat doivent être incluses
         * @return Liste des entrées avec zones sélectionnées aléatoirement
         */
        fun generateRandomControl(
            selectedEntry: SelectItem,
            entryList: List<SelectItem>,
            useProxi: Boolean,
            useContract: Boolean
        ): List<SelectItem> {
            try {
                LogUtils.d(TAG, "Génération de contrôle aléatoire pour l'entrée ${selectedEntry.id} -- ${selectedEntry.name} - Proxi: $useProxi, Contract: $useContract")

                val nameToSearch = selectedEntry.name.trim().lowercase()
                val sameNameEntries = entryList.filter { it.name.trim().lowercase() == nameToSearch }

                LogUtils.json(TAG, "Entrées avec le même nom: ${sameNameEntries.size}", sameNameEntries)

                if (sameNameEntries.isEmpty()) {
                    LogUtils.d(TAG, "Aucune entrée avec le même nom trouvée")
                    return emptyList()
                }

                val availableZones = collectAvailableZones(sameNameEntries, useProxi, useContract)

                LogUtils.json(TAG, "Zones disponibles: ${availableZones.size}", availableZones)

                if (availableZones.isEmpty()) {
                    LogUtils.d(TAG, "Aucune zone disponible")
                    return emptyList()
                }

                val result = selectRandomZones(sameNameEntries, availableZones)

                LogUtils.json(TAG, "Contrôles générés: ${result.size}", result)

                return result
            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur lors de la génération des contrôles aléatoires", e)
                return emptyList()
            }
        }

        /**
         * Collecte toutes les zones disponibles pour un groupe d'entrées
         */
        private fun collectAvailableZones(
            entries: List<SelectItem>,
            useProxi: Boolean,
            useContract: Boolean
        ): List<Pair<String, SelectItem>> {
            val zones = mutableListOf<Pair<String, SelectItem>>()

            entries.forEach { entry ->
                entry.prop?.zones?.let { entryZones ->
                    if (useProxi) {
                        entryZones.proxi.forEach { zoneId ->
                            zones.add(Pair(zoneId, entry))
                        }
                    }

                    if (useContract) {
                        entryZones.contra.forEach { zoneId ->
                            zones.add(Pair(zoneId, entry))
                        }
                    }
                }
            }

            return zones.shuffled()
        }

        /**
         * Sélectionne aléatoirement des zones et crée les entrées de contrôle
         */
        private fun selectRandomZones(
            entries: List<SelectItem>,
            availableZones: List<Pair<String, SelectItem>>
        ): List<SelectItem> {
            val result = mutableListOf<SelectItem>()
            val selectedZones = mutableMapOf<Int, MutableList<String>>()
            val allPossibleZones = availableZones.map { it.first }.distinct()

            if (allPossibleZones.isEmpty()) {
                LogUtils.d(TAG, "Aucune zone disponible")
                return emptyList()
            }

            entries.forEach { entry ->
                selectedZones[entry.id] = mutableListOf()
            }

            entries.forEach { entry ->
                if (allPossibleZones.isEmpty()) return@forEach

                val randomZone = allPossibleZones.random()

                selectedZones[entry.id]?.add(randomZone)
            }

            val idealZonesPerEntry = minOf(
                MAX_ZONES_PER_ENTRY,
                maxOf(1, allPossibleZones.size / entries.size)
            )

            LogUtils.d(TAG, "Nombre idéal de zones par entrée: $idealZonesPerEntry")

            entries.forEach { entry ->
                val currentZones = selectedZones[entry.id] ?: mutableListOf()
                val neededZones = idealZonesPerEntry - currentZones.size

                if (neededZones > 0) {
                    val zonesToPickFrom = allPossibleZones.toMutableList()

                    zonesToPickFrom.removeAll(currentZones)

                    if (zonesToPickFrom.size < neededZones) {
                        LogUtils.d(TAG, "Pas assez de zones différentes, réutilisation autorisée")

                        for (zone in zonesToPickFrom) {
                            currentZones.add(zone)
                        }

                        val remaining = neededZones - zonesToPickFrom.size

                        for (i in 1..remaining) {
                            val possibleZones = allPossibleZones.filter { it !in currentZones }

                            if (possibleZones.isNotEmpty()) {
                                val zoneToAdd = possibleZones.random()

                                currentZones.add(zoneToAdd)
                            } else {
                                break
                            }
                        }
                    } else {
                        val zonesToAdd = zonesToPickFrom.shuffled().take(neededZones)

                        currentZones.addAll(zonesToAdd)
                    }
                }
            }

            LogUtils.d(TAG, "Distribution finale des zones par entrée:")

            selectedZones.forEach { (entryId, zones) ->
                LogUtils.json(TAG, "Entrée $entryId: ${zones.size} zones", zones)
            }

            entries.forEach { entry ->
                val zones = selectedZones[entry.id] ?: return@forEach

                if (zones.isNotEmpty()) {
                    val modifiedEntry = createEntryWithFilteredZones(entry, zones)

                    result.add(modifiedEntry)
                }
            }

            return result
        }

        /**
         * Crée une copie de l'entrée avec uniquement les zones spécifiées
         */
        private fun createEntryWithFilteredZones(
            originalEntry: SelectItem,
            selectedZoneIds: List<String>
        ): SelectItem {
            // Ici, nous créons une nouvelle instance de SelectItem avec uniquement les zones sélectionnées
            // Note: La structure exacte dépend de votre implémentation de SelectItem

            // Dans un cas réel, vous devriez filtrer les éléments de la grille de contrôle
            // pour ne garder que ceux correspondant aux zones sélectionnées

            return originalEntry.copy(
                // Vous devrez adapter cette partie en fonction de votre modèle de données
                // prop = originalEntry.prop?.copy(
                //     zones = Zones(
                //         proxi = originalEntry.prop.zones.proxi.filter { it in selectedZoneIds },
                //         contra = originalEntry.prop.zones.contra.filter { it in selectedZoneIds }
                //     )
                // )
            )
        }
    }

}
