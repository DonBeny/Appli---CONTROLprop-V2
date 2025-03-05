package org.orgaprop.controlprop.utils

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes

object UiUtils {

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

    @JvmStatic
    fun showToast(context: Context, @StringRes messageId: Int) {
        runOnMainThread { Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show() }
    }

    @JvmStatic
    fun toggleWaitingState(waitingView: View, mainLayout: View, show: Boolean) {
        runOnMainThread {
            waitingView.visibility = if (show) View.VISIBLE else View.GONE
            mainLayout.isEnabled = !show
        }
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            Handler(Looper.getMainLooper()).post(action)
        }
    }

}
