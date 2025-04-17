package org.orgaprop.controlprop.utils

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import org.orgaprop.controlprop.R

object UiUtils {

    private var currentDialog: AlertDialog? = null

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
    fun showAlert(
        context: Context,
        message: String,
        title: String? = null
    ) {
        runOnMainThread {
            currentDialog?.dismiss()
            currentDialog = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .apply { title?.let { setTitle(it) } }
                .setMessage(message)
                .setPositiveButton(R.string.btn_close, ) { dialog, _ -> dialog.dismiss() }
                .create()
                .also { dialog ->
                    dialog.show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                        setTextColor(context.getColor(R.color.main_ctrl_prop))
                        isAllCaps = false
                    }
                }
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
            currentDialog = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .apply { titleId?.let { setTitle(it) } }
                .setMessage(messageId)
                .setPositiveButton(R.string.btn_close, ) { dialog, _ -> dialog.dismiss() }
                .create()
                .also { dialog ->
                    dialog.show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                        isAllCaps = false
                    }
                }
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
            currentDialog = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .apply { title?.let { setTitle(it) } }
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    positiveAction?.invoke()
                }
                .create()
                .also { dialog ->
                    dialog.show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                        isAllCaps = false
                    }
                }
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
            currentDialog = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .apply { titleId?.let { setTitle(it) } }
                .setMessage(messageId)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    positiveAction?.invoke()
                }
                .create()
                .also { dialog ->
                    dialog.show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                        isAllCaps = false
                    }
                }
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
            currentDialog = AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
                .apply { title?.let { setTitle(it) } }
                .setMessage(message)
                .setPositiveButton(positiveButtonText) { _, _ -> positiveAction() }
                .setNegativeButton(negativeButtonText) { _, _ -> negativeAction?.invoke() }
                .setCancelable(false)
                .create()
                .also { dialog ->
                    dialog.show()

                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.apply {
                        setTextColor(context.getColor(R.color.main_ctrl_prop))
                        isAllCaps = false
                    }

                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.apply {
                        setTextColor(context.getColor(R.color.text_secondary_light))
                        isAllCaps = false
                    }
                }
        }
    }



    @JvmStatic
    fun showProgressDialog(
        context: Context,
        message: String,
        title: String? = null,
        cancelable: Boolean = false,
    ): AlertDialog {
        return AlertDialog.Builder(context).apply {
            title?.let { setTitle(it) }
            setView(R.layout.dialog_progress)
            setCancelable(cancelable)

            if (cancelable) {
                setPositiveButton(R.string.btn_close) { dialog, _ -> dialog.dismiss() }
            }
        }.show().also { dialog ->
            dialog.show()
            currentDialog = dialog

            dialog.findViewById<TextView>(R.id.progress_message)?.apply {
                text = message
                setTextColor(context.getColor(R.color.text_primary_light))
            }

            dialog.findViewById<ProgressBar>(R.id.progress_bar)?.apply {
                indeterminateTintList = ColorStateList.valueOf(context.getColor(R.color.main_ctrl_prop))
            }

            dialog.window?.apply {
                setBackgroundDrawableResource(R.drawable.progress_dialog_background)
            }
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
        actionText: String? = view.context.getString(R.string.btn_close),
        action: (() -> Unit)? = null,
    ) {
        val durationValue = if (action == null) duration else Snackbar.LENGTH_INDEFINITE
        val snackbar = Snackbar.make(view, message, durationValue)

        if (action != null) {
            snackbar.setAction(actionText) { action.invoke() }
            snackbar.setActionTextColor(view.context.getColor(R.color._white))
        } else {
            snackbar.setAction(actionText) { snackbar.dismiss() }
            snackbar.setActionTextColor(view.context.getColor(R.color._white))
        }

        snackbar.setBackgroundTint(view.context.getColor(R.color.design_default_color_error))
        snackbar.setTextColor(view.context.getColor(R.color._white))

        val snackbarView = snackbar.view
        snackbarView.elevation = 6f

        snackbarView.background = ContextCompat.getDrawable(view.context, R.drawable.snackbar_background)

        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = 3

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

        snackbar.setAction(R.string.btn_close) { snackbar.dismiss() }
        snackbar.setActionTextColor(view.context.getColor(R.color._white))

        snackbar.setBackgroundTint(view.context.getColor(R.color.text_secondary_light))
        snackbar.setTextColor(view.context.getColor(R.color._white))

        val snackbarView = snackbar.view
        snackbarView.elevation = 6f

        snackbarView.background = ContextCompat.getDrawable(view.context, R.drawable.snackbar_background)

        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.maxLines = 3

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

        snackbar.setAction(R.string.btn_close) { snackbar.dismiss() }
        snackbar.setActionTextColor(view.context.getColor(R.color._white))

        snackbar.setBackgroundTint(view.context.getColor(R.color._light_green))
        snackbar.setTextColor(view.context.getColor(R.color._white))

        val snackbarView = snackbar.view
        snackbarView.elevation = 6f

        snackbarView.background = ContextCompat.getDrawable(view.context, R.drawable.snackbar_background)

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
