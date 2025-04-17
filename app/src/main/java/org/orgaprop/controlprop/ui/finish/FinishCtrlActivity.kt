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
import org.orgaprop.controlprop.ui.config.ConfigCtrlActivity
import org.orgaprop.controlprop.ui.planActions.PlanActionsActivity
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.ui.sendMail.SendMailActivity
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.utils.UiUtils.showToast
import org.orgaprop.controlprop.viewmodels.FinishCtrlViewModel



class FinishCtrlActivity : BaseActivity() {

    companion object {
        private const val TAG = "FinishCtrlActivity"
    }

    private lateinit var binding: ActivityFinishCtrlBinding
    private val viewModel: FinishCtrlViewModel by viewModel()

    private val signatureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        handleSignatureResult(result.resultCode)
    }

    private lateinit var user: LoginData
    private lateinit var entrySelected: SelectItem



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
            LogUtils.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
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
            LogUtils.d(TAG, "initializeComponents: UserData is null")
            UiUtils.showErrorSnackbar(binding.root, "Session expirée, veuillez vous reconnecter")
            navigateToMainActivity()
            return
        } else {
            user = userData
            LogUtils.d(TAG, "initializeComponents: UserData is not null")
        }

        val idMbr = user.idMbr
        val adrMac = user.adrMac

        LogUtils.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        viewModel.setUserData(user)

        val savedEntry = getEntrySelected()

        if( savedEntry == null ) {
            LogUtils.d(TAG, "initializeComponents: Entry is null")
            UiUtils.showErrorSnackbar(binding.root, "Aucune résidence sélectionnée")
            navigateToSelectEntryActivity()
            return
        } else {
            LogUtils.d(TAG, "initializeComponents: Entry is not null")
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
            LogUtils.d(TAG, "setupListeners: Clicked on previous button")
            navigateToPrevScreen()
        }

        binding.finishCtrlActivityPlanBtn.setOnClickListener {
            LogUtils.d(TAG, "setupListeners: Clicked on plan button")
            if (viewModel.controlState.value?.isSigned == true) {
                UiUtils.showInfoSnackbar(binding.root, "Impossible d'ajouter un plan d'actions après la signature")
            } else {
                navigateToPlanActionsActivity()
            }
        }

        binding.finishCtrlActivitySignBtn.setOnClickListener {
            LogUtils.d(TAG, "setupListeners: Clicked on sign button")
            if (viewModel.controlState.value?.isSigned == true) {
                UiUtils.showInfoSnackbar(binding.root, "Le contrôle est déjà signé")
            } else {
                navigateToSignatureActivity()
            }
        }

        binding.finishCtrlActivitySendBtn.setOnClickListener {
            LogUtils.d(TAG, "setupListeners: Clicked on send button")
            navigateToSendMailActivity()
        }

        binding.finishCtrlActivityEndBtn.setOnClickListener {
            LogUtils.d(TAG, "setupListeners: Clicked on end button")
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
        UiUtils.showConfirmationDialog(
            context = this,
            title = "Terminer le contrôle",
            message = "Êtes-vous sûr de vouloir terminer ce contrôle ?",
            positiveButtonText = "Oui",
            negativeButtonText = "Non",
            positiveAction = {
                handleCloseCtrl()
            }
        )
    }



    private fun handleSignatureResult(resultCode: Int) {
        when (resultCode) {
            RESULT_OK -> {
                viewModel.refreshControlState()

                binding.finishCtrlActivitySignBtn.isEnabled = false
                binding.finishCtrlActivitySignBtn.alpha = 0.5f
                binding.finishCtrlActivityPlanBtn.isEnabled = false
                binding.finishCtrlActivityPlanBtn.alpha = 0.5f

                UiUtils.showSuccessSnackbar(binding.root, "Signature enregistrée avec succès")
            }
            RESULT_CANCELED -> {
                UiUtils.showInfoSnackbar(binding.root, "Signature annulée")
            }
            else -> {
                UiUtils.showErrorSnackbar(binding.root, "Erreur lors de la signature")
            }
        }
    }
    private fun handleCloseCtrl() {
        val entry = getEntrySelected()

        if (entry?.type == TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_RANDOM) {
            LogUtils.d(TAG, "handleCloseCtrl: Random control - Special handling to be implemented")

            val randomList = getRandomList().toMutableList()
            LogUtils.json(TAG, "handleCloseCtrl: Random list size: ${randomList.size}", randomList)

            if (randomList.isNotEmpty()) {
                var nextEntry = randomList.first()
                LogUtils.json(TAG, "handleCloseCtrl: Next random entry:", nextEntry)

                randomList.removeAt(0)

                if (nextEntry.id == entrySelected.id) {
                    nextEntry = randomList.first()
                    randomList.removeAt(0)

                    if (randomList.isEmpty()) {
                        setRandomList(emptyList())

                        LogUtils.d(TAG, "handleCloseCtrl: No more random controls")
                        UiUtils.showSuccessSnackbar(binding.root, "Tous les contrôles aléatoires sont terminés")
                        navigateToSelectEntryActivity()
                    }
                }

                setRandomList(randomList)
                LogUtils.d(TAG, "handleCloseCtrl: Updated random list size: ${randomList.size}")

                setEntrySelected(nextEntry)

                UiUtils.showInfoSnackbar(binding.root, "Passage au contrôle aléatoire suivant")
                navigateToConfigCtrlActivity()
            } else {
                LogUtils.d(TAG, "handleCloseCtrl: No more random controls")
                UiUtils.showSuccessSnackbar(binding.root, "Tous les contrôles aléatoires sont terminés")
                navigateToSelectEntryActivity()
            }
        } else {
            LogUtils.d(TAG, "handleCloseCtrl: Standard control - Navigating to SelectEntryActivity")
            UiUtils.showSuccessSnackbar(binding.root, "Contrôle terminé avec succès")
            navigateToSelectEntryActivity()
        }
    }



    private fun navigateToPrevScreen() {
        LogUtils.d(TAG, "navigateToPrevScreen: Navigating to previous screen")

        val intent = Intent(this, GrilleCtrlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun navigateToMainActivity() {
        LogUtils.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToSelectEntryActivity() {
        LogUtils.d(TAG, "Navigating to SelectEntryActivity")
        val intent = Intent(this, SelectEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToPlanActionsActivity() {
        LogUtils.d(TAG, "Navigating to PlanActionsActivity")
        val intent = Intent(this, PlanActionsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }
    private fun navigateToSendMailActivity() {
        LogUtils.d(TAG, "Navigating to SendMailActivity")
        val intent = Intent(this, SendMailActivity::class.java).apply {
            putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TYPE, SendMailActivity.SEND_MAIL_ACTIVITY_CTRL)
            putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TAG_RSD_ID, entrySelected.id)
            putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TAG_CTRL_DATE, entrySelected.prop?.ctrl?.date?.value)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }
    private fun navigateToSignatureActivity() {
        LogUtils.d(TAG, "Navigating to SignatureActivity")
        val intent = Intent(this, SignatureActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        signatureLauncher.launch(intent)
    }
    private fun navigateToConfigCtrlActivity() {
        LogUtils.d(TAG, "Navigating to ConfigCtrlActivity")
        val intent = Intent(this, ConfigCtrlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

}
