package org.orgaprop.controlprop.ui.finish

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.window.OnBackInvokedDispatcher

import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivityFinishCtrlBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.config.TypeCtrlActivity
import org.orgaprop.controlprop.ui.grille.GrilleCtrlActivity
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.ui.planActions.PlanActionsActivity
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.ui.sendMail.SendMailActivity
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.utils.UiUtils.showToast
import org.orgaprop.controlprop.viewmodels.FinishCtrlViewModel



class FinishCtrlActivity : BaseActivity() {

    private val TAG = "FinishCtrlActivity"

    private lateinit var binding: ActivityFinishCtrlBinding
    private val viewModel: FinishCtrlViewModel by viewModel()

    private val signatureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        handleSignatureResult(result.resultCode)
    }

    private lateinit var user: LoginData
    private lateinit var entrySelected: SelectItem



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
            Log.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }
    override fun onResume() {
        super.onResume()
        viewModel.refreshControlState()
    }



    override fun initializeComponents() {
        binding = ActivityFinishCtrlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()

        if( userData == null ) {
            Log.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            user = userData
            Log.d(TAG, "initializeComponents: UserData is not null")
        }

        val idMbr = user.idMbr
        val adrMac = user.adrMac

        Log.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        viewModel.setUserData(user)

        val savedEntry = getEntrySelected()

        if( savedEntry == null ) {
            Log.d(TAG, "initializeComponents: Entry is null")
            showToast(this, "Aucune résidence sélectionnée")
            navigateToSelectEntryActivity()
            return
        } else {
            Log.d(TAG, "initializeComponents: Entry is not null")
            entrySelected = savedEntry
            viewModel.setEntrySelected(entrySelected)
        }
    }
    override fun setupComponents() {
        setupObservers()
        setupListeners()
    }
    override fun setupObservers() {
        viewModel.controlState.observe(this, Observer { state ->
            updateButtonStates(state)
        })
    }
    override fun setupListeners() {
        binding.finishCtrlActivityPrevBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on previous button")
            navigateToPrevScreen()
        }

        binding.finishCtrlActivityPlanBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on plan button")
            navigateToPlanActionsActivity()
        }

        binding.finishCtrlActivitySignBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on sign button")
            navigateToSignatureActivity()
        }

        binding.finishCtrlActivitySendBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on send button")
            navigateToSendMailActivity()
        }

        binding.finishCtrlActivityEndBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on end button")
            confirmCloseControl()
        }
    }



    /**
     * Met à jour l'état des boutons en fonction de l'état du contrôle
     */
    private fun updateButtonStates(state: FinishCtrlViewModel.ControlState) {
        binding.finishCtrlActivitySignBtn.isEnabled = !state.isSigned
        binding.finishCtrlActivitySignBtn.alpha = if (state.isSigned) 0.5f else 1.0f

        binding.finishCtrlActivityPlanBtn.isEnabled = !state.isSigned
        binding.finishCtrlActivityPlanBtn.alpha = if (state.isSigned) 0.5f else 1.0f
    }

    private fun confirmCloseControl() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Terminer le contrôle")
            .setMessage("Êtes-vous sûr de vouloir terminer ce contrôle ?")
            .setPositiveButton("Oui") { _, _ ->
                handleCloseCtrl()
            }
            .setNegativeButton("Non", null)
            .show()
    }



    private fun handleSignatureResult(resultCode: Int) {
        when (resultCode) {
            RESULT_OK -> {
                viewModel.refreshControlState()
                UiUtils.showSuccessSnackbar(binding.root, "Signature enregistrée avec succès")
            }
            RESULT_CANCELED -> {
                showToast(this, "Signature annulée")
            }
            else -> {
                UiUtils.showErrorSnackbar(binding.root, "Erreur lors de la signature")
            }
        }
    }
    private fun handleCloseCtrl() {
        val entry = getEntrySelected()
        if (entry?.type == TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_RANDOM) {
            // Pour les contrôles aléatoires, action spécifique à implémenter plus tard
            Log.d(TAG, "handleCloseCtrl: Random control - Special handling to be implemented")
            showToast(this, "Contrôle aléatoire terminé")
        } else {
            Log.d(TAG, "handleCloseCtrl: Standard control - Navigating to SelectEntryActivity")
            showToast(this, "Contrôle terminé avec succès")
        }
        navigateToSelectEntryActivity()
    }



    private fun navigateToPrevScreen() {
        Log.d(TAG, "navigateToPrevScreen: Navigating to previous screen")

        val intent = Intent(this, GrilleCtrlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToSelectEntryActivity() {
        Log.d(TAG, "Navigating to SelectEntryActivity")
        val intent = Intent(this, SelectEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToPlanActionsActivity() {
        Log.d(TAG, "Navigating to PlanActionsActivity")
        val intent = Intent(this, PlanActionsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }
    private fun navigateToSendMailActivity() {
        Log.d(TAG, "Navigating to SendMailActivity")
        val intent = Intent(this, SendMailActivity::class.java).apply {
            putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TYPE, SendMailActivity.SEND_MAIL_ACTIVITY_CTRL)
            putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TAG_RSD, entrySelected.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }
    private fun navigateToSignatureActivity() {
        Log.d(TAG, "Navigating to SignatureActivity")
        val intent = Intent(this, SignatureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        signatureLauncher.launch(intent)
    }

}
