package org.orgaprop.controlprop.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.orgaprop.controlprop.R

object UiUtils {

    private var currentDialog: AlertDialog? = null

    @JvmStatic
    fun showWait(activity: Activity, waitImage: ImageView, show: Boolean) {
        runOnMainThread { waitImage.visibility = if (show) View.VISIBLE else View.INVISIBLE }
    }

    @JvmStatic
    fun disableOption(vararg views: View) {
        views.forEach { view ->
            view.isEnabled = false
            view.isClickable = false
        }
    }

    /**
     * Ferme le dialogue actuellement affiché.
     */
    @JvmStatic
    fun dismissCurrentDialog() {
        runOnMainThread {
            currentDialog?.dismiss()
            currentDialog = null
        }
    }



    @JvmStatic
    fun showToast(context: Context, @StringRes messageId: Int) {
        runOnMainThread { Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show() }
    }
    @JvmStatic
    fun showToast(context: Context, message: String) {
        runOnMainThread { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    @JvmStatic
    fun toggleWaitingState(waitingView: View, mainLayout: View, show: Boolean) {
        runOnMainThread {
            waitingView.visibility = if (show) View.VISIBLE else View.GONE
            mainLayout.isEnabled = !show
        }
    }



    @JvmStatic
    fun showAlert(
        context: Context,
        message: String,
        title: String? = null
    ) {
        runOnMainThread {
            currentDialog?.dismiss()
            currentDialog = AlertDialog.Builder(context)
                .apply { title?.let { setTitle(it) } }
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    @JvmStatic
    fun showAlert(
        context: Context,
        @StringRes messageId: Int,
        @StringRes titleId: Int? = null
    ) {
        runOnMainThread {
            currentDialog?.dismiss()
            currentDialog = AlertDialog.Builder(context)
                .apply { titleId?.let { setTitle(it) } }
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    @JvmStatic
    fun showAlert(
        context: Context,
        message: String,
        title: String? = null,
        positiveAction: (() -> Unit)? = null
    ) {
        runOnMainThread {
            currentDialog?.dismiss()
            currentDialog = AlertDialog.Builder(context)
                .apply { title?.let { setTitle(it) } }
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    positiveAction?.invoke()
                }
                .show()
        }
    }

    @JvmStatic
    fun showAlert(
        context: Context,
        @StringRes messageId: Int,
        @StringRes titleId: Int? = null,
        positiveAction: (() -> Unit)? = null
    ) {
        runOnMainThread {
            currentDialog?.dismiss()
            currentDialog = AlertDialog.Builder(context)
                .apply { titleId?.let { setTitle(it) } }
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    positiveAction?.invoke()
                }
                .show()
        }
    }

    fun Fragment.showUiAlert(message: String, title: String? = null) {
        showAlert(requireContext(), message, title)
    }
    fun Activity.showUiAlert(message: String, title: String? = null) {
        showAlert(this, message, title)
    }



    /**
     * Affiche une boîte de dialogue d'alerte avec boutons OK et Annuler.
     *
     * @param context Contexte de l'application
     * @param message Message à afficher
     * @param title Titre de la boîte de dialogue (optionnel)
     * @param positiveButtonText Texte du bouton positif (OK par défaut)
     * @param negativeButtonText Texte du bouton négatif (Annuler par défaut)
     * @param positiveAction Action à exécuter lors du clic sur le bouton positif
     * @param negativeAction Action à exécuter lors du clic sur le bouton négatif (optionnel)
     */
    @JvmStatic
    fun showConfirmationDialog(
        context: Context,
        message: String,
        title: String? = null,
        positiveButtonText: String = context.getString(android.R.string.ok),
        negativeButtonText: String = context.getString(android.R.string.cancel),
        positiveAction: () -> Unit,
        negativeAction: (() -> Unit)? = null
    ) {
        runOnMainThread {
            currentDialog?.dismiss()
            currentDialog = AlertDialog.Builder(context)
                .apply { title?.let { setTitle(it) } }
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { _, _ -> positiveAction() }
                .setNegativeButton(negativeButtonText) { _, _ -> negativeAction?.invoke() }
                .setCancelable(false)
                .show()
        }
    }



    @JvmStatic
    fun showProgressDialog(
        context: Context,
        message: String,
        title: String? = null,
        cancelable: Boolean = false
    ): AlertDialog {
        return AlertDialog.Builder(context).apply {
            title?.let { setTitle(it) }
            setView(R.layout.dialog_progress).apply {
                // Layout personnalisé à créer (voir ci-dessous)
            }
            setCancelable(cancelable)
        }.show().also {
            currentDialog = it
            // Optionnel: Configurer le message ici si besoin
            it.findViewById<TextView>(R.id.progress_message)?.text = message
        }
    }



    /**
     * Affiche un message d'erreur sous forme de Snackbar
     *
     * @param view La vue sur laquelle afficher le Snackbar
     * @param message Le message d'erreur
     * @param duration La durée d'affichage
     * @param actionText Le texte du bouton d'action (optionnel)
     * @param action L'action à effectuer lors du clic sur le bouton (optionnel)
     */
    fun showErrorSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, duration)

        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }

        //snackbar.setBackgroundTint(view.context.getColor(R.color.error_background))
        //snackbar.setTextColor(view.context.getColor(R.color.error_text))
        snackbar.show()
    }
    /**
     * Affiche un message d'information sous forme de Snackbar
     *
     * @param view La vue sur laquelle afficher le Snackbar
     * @param message Le message d'information
     * @param duration La durée d'affichage
     */
    fun showInfoSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.show()
    }

    /**
     * Affiche un message de succès sous forme de Snackbar
     *
     * @param view La vue sur laquelle afficher le Snackbar
     * @param message Le message de succès
     * @param duration La durée d'affichage
     */
    fun showSuccessSnackbar(
        view: View,
        message: String,
        duration: Int = Snackbar.LENGTH_SHORT
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        //snackbar.setBackgroundTint(view.context.getColor(R.color.success_background))
        //snackbar.setTextColor(view.context.getColor(R.color.success_text))
        snackbar.show()
    }




    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            Handler(Looper.getMainLooper()).post(action)
        }
    }

}
