package org.orgaprop.controlprop.utils.extentions

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

/**
 * Extension pour Bundle qui permet de récupérer un objet Parcelable de manière compatible
 * avec les versions anciennes et récentes d'Android.
 *
 * @param key La clé utilisée pour récupérer l'objet Parcelable.
 * @return L'objet Parcelable de type T, ou null si l'objet n'existe pas ou si le cast échoue.
 */
inline fun <reified T : Parcelable> Bundle.getParcelableCompat(key: String): T? {

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Pour les versions TIRAMISU et supérieures
        getParcelable(key, T::class.java)
    } else {
        // Pour les versions antérieures à TIRAMISU
        @Suppress("DEPRECATION")
        getParcelable(key) as? T
    }

}
