package org.orgaprop.controlprop.ui.config

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer
import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivityTypeCtrlBinding
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.HomeActivity
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.ui.planActions.PlanActionsActivity
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.viewmodels.TypeCtrlViewModel

class TypeCtrlActivity : BaseActivity() {

    private lateinit var binding: ActivityTypeCtrlBinding
    private val viewModel: TypeCtrlViewModel by viewModel()

    private var canOpenPlanActions: Boolean = false

    companion object {
        private const val TAG = "TypeCtrlActivity"

        const val TYPE_CTRL_ACTIVITY_TAG_EDLE = "edle"
        const val TYPE_CTRL_ACTIVITY_TAG_CTRL = "complet"
        const val TYPE_CTRL_ACTIVITY_TAG_RANDOM = "random"
        const val TYPE_CTRL_ACTIVITY_TAG_LEVEE = "levee"
        const val TYPE_CTRL_ACTIVITY_TAG_EDLS = "edls"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
            LogUtils.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }



    override fun initializeComponents() {
        binding = ActivityTypeCtrlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()

        if( userData == null ) {
            LogUtils.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            LogUtils.d(TAG, "initializeComponents: UserData is not null")
        }

        val idMbr = userData.idMbr
        val adrMac = userData.adrMac

        LogUtils.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        viewModel.setUserCredentials(idMbr, adrMac)
    }
    override fun setupComponents() {
        val idRsd = getEntrySelected()?.id

        LogUtils.d(TAG, "setupComponents: idRsd: $idRsd")

        if( idRsd == null ) {
            LogUtils.d(TAG, "setupComponents: idRsd is null")
            UiUtils.showErrorSnackbar(binding.root, "Aucune entrée sélectionnée")
            navigateToPrevScreen()
            return
        } else {
            LogUtils.d(TAG, "setupComponents: idRsd is not null")

            setupObservers()

            viewModel.fetchPlanAction(idRsd)

            setupListeners()
        }
    }
    override fun setupListeners() {
        binding.typeCtrlActivityEdleTxt.setOnClickListener { navigateToNextScreen(TYPE_CTRL_ACTIVITY_TAG_EDLE) }
        binding.typeCtrlActivityCpltTxt.setOnClickListener { navigateToNextScreen(TYPE_CTRL_ACTIVITY_TAG_CTRL) }
        binding.typeCtrlActivityRndTxt.setOnClickListener {
            val selectedEntry = getEntrySelected()
            if (selectedEntry == null) {
                UiUtils.showErrorSnackbar(
                    binding.root,
                    "Aucune entrée sélectionnée"
                )
                return@setOnClickListener
            }

            UiUtils.showProgressDialog(
                this,
                "Génération du contrôle aléatoire...",
                "Veuillez patienter"
            )

            val entryList = getEntryList()

            LogUtils.json(TAG, "setupListeners: entryList", entryList)

            if (entryList.isEmpty()) {
                UiUtils.dismissCurrentDialog()
                UiUtils.showErrorSnackbar(
                    binding.root,
                    "Aucune entrée disponible"
                )
            }

            viewModel.generateRandomControl(
                selectedEntry,
                entryList,
                withProxi(),
                withContract()
            )
        }
        binding.typeCtrlActivityPlanActTxt.setOnClickListener { navigateToPlanActionsActivity() }
        binding.typeCtrlActivityEdlsTxt.setOnClickListener { navigateToNextScreen(TYPE_CTRL_ACTIVITY_TAG_EDLS) }
    }
    override fun setupObservers() {
        viewModel.planActionResult.observe(this, Observer { result ->
            canOpenPlanActions = result

            binding.typeCtrlActivityPlanActTxt.isEnabled = canOpenPlanActions
            binding.typeCtrlActivityPlanActTxt.alpha = if (canOpenPlanActions) 1.0f else 0.5f

            LogUtils.d(TAG, "setupComponents: canOpenPlanActions updated to: $canOpenPlanActions")
        })

        viewModel.error.observe(this, Observer { errorPair ->
            errorPair?.let { (code, message) ->
                UiUtils.showErrorSnackbar(binding.root, message)
            }
        })

        viewModel.randomGenerationCompleted.observe(this, Observer { success ->
            UiUtils.dismissCurrentDialog()

            if (success) {
                val randomList = viewModel.getRandomControlList()

                if (randomList.isNotEmpty()) {
                    setRandomList(randomList)

                    val selectedEntry = getEntrySelected()
                    val modifiedSelectedEntry = randomList.find { it.id == selectedEntry?.id }

                    if (modifiedSelectedEntry != null) {
                        LogUtils.d(TAG, "Mise à jour de l'entrée sélectionnée avec la version modifiée")

                        setEntrySelected(modifiedSelectedEntry)
                    } else {
                        LogUtils.d(TAG, "Entrée sélectionnée non trouvée dans la liste aléatoire")
                    }

                    navigateToNextScreen(TYPE_CTRL_ACTIVITY_TAG_RANDOM)
                } else {
                    UiUtils.showErrorSnackbar(
                        binding.root,
                        "Aucune zone disponible pour le contrôle aléatoire"
                    )
                }
            } else {
                viewModel.error.value?.let { (_, message) ->
                    UiUtils.showErrorSnackbar(binding.root, message)
                } ?: UiUtils.showErrorSnackbar(
                    binding.root,
                    "Impossible de générer le contrôle aléatoire"
                )
            }
        })
    }



    private fun navigateToPrevScreen() {
        LogUtils.d(TAG, "Navigating to SelectEntryActivity")
        val intent = Intent(this, SelectEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToNextScreen(type: String) {
        LogUtils.d(TAG, "Navigating to ConfigCtrlActivity")

        setTypeCtrl(type)

        val intent = Intent(this, ConfigCtrlActivity::class.java).apply {
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
    private fun navigateToPlanActionsActivity() {
        LogUtils.d(TAG, "Navigating to PlanActionsActivity")

        setTypeCtrl(TYPE_CTRL_ACTIVITY_TAG_LEVEE)

        val intent = Intent(this, PlanActionsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

}
