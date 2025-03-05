package org.orgaprop.controlprop.utils.prefs.dao

import org.orgaprop.controlprop.utils.prefs.configPrefDatabase

object DatabaseQueries {

    // Requêtes pour la table Pref
    const val SELECT_PREF_BY_PARAM = "SELECT * FROM ${configPrefDatabase.PREF_TABLE_NAME} WHERE ${configPrefDatabase.PREF_COL_PARAM_NAME} = :param"
    const val SELECT_PREF_BY_ID = "SELECT * FROM ${configPrefDatabase.PREF_TABLE_NAME} WHERE ${configPrefDatabase.PREF_COL_ID_NAME} = :paramId"
    const val DELETE_PREF_BY_PARAM = "DELETE FROM ${configPrefDatabase.PREF_TABLE_NAME} WHERE ${configPrefDatabase.PREF_COL_PARAM_NAME} = :param"
    const val DELETE_PREF_BY_ID = "DELETE FROM ${configPrefDatabase.PREF_TABLE_NAME} WHERE ${configPrefDatabase.PREF_COL_ID_NAME} = :paramId"

    // Requêtes pour la table Contact
    const val SELECT_ALL_CONTACTS = "SELECT * FROM ${configPrefDatabase.CONTACT_TABLE_NAME}"
    const val SELECT_CONTACT_BY_ADDRESS = "SELECT * FROM ${configPrefDatabase.CONTACT_TABLE_NAME} WHERE ${configPrefDatabase.CONTACT_COL_ADR} LIKE '%' || :txt || '%'"
    const val DELETE_CONTACT_BY_ID = "DELETE FROM ${configPrefDatabase.CONTACT_TABLE_NAME} WHERE ${configPrefDatabase.CONTACT_COL_ID} = :addressId"

    // Requêtes pour la table Storage
    const val SELECT_ALL_STORAGE = "SELECT * FROM ${configPrefDatabase.STORAGE_TABLE_NAME}"
    const val SELECT_STORAGE_BY_ID = "SELECT * FROM ${configPrefDatabase.STORAGE_TABLE_NAME} WHERE ${configPrefDatabase.STORAGE_COL_ID} = :storageId"
    const val SELECT_STORAGE_BY_RSD = "SELECT * FROM ${configPrefDatabase.STORAGE_TABLE_NAME} WHERE ${configPrefDatabase.STORAGE_COL_RESID} = :storageRsd"
    const val DELETE_STORAGE_BY_ID = "DELETE FROM ${configPrefDatabase.STORAGE_TABLE_NAME} WHERE ${configPrefDatabase.STORAGE_COL_ID} = :storageId"
    const val DELETE_STORAGE_BY_RSD = "DELETE FROM ${configPrefDatabase.STORAGE_TABLE_NAME} WHERE ${configPrefDatabase.STORAGE_COL_RESID} = :storageRsd"
    const val DELETE_ALL_STORAGE = "DELETE FROM ${configPrefDatabase.STORAGE_TABLE_NAME}"

}
