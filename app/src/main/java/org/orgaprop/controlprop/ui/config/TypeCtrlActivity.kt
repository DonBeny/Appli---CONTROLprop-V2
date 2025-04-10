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
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.viewmodels.TypeCtrlViewModel

class TypeCtrlActivity : BaseActivity() {

    private val TAG = "TypeCtrlActivity"

    private lateinit var binding: ActivityTypeCtrlBinding
    private val viewModel: TypeCtrlViewModel by viewModel()

    private var canOpenPlanActions: Boolean = false

    companion object {
        const val TYPE_CTRL_ACTIVITY_TAG_EDLE = "edle"
        const val TYPE_CTRL_ACTIVITY_TAG_CTRL = "complet"
        const val TYPE_CTRL_ACTIVITY_TAG_RANDOM = "random"
        const val TYPE_CTRL_ACTIVITY_TAG_LEVEE = "levee"
        const val TYPE_CTRL_ACTIVITY_TAG_EDLS = "edls"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
            Log.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }



    override fun initializeComponents() {
        binding = ActivityTypeCtrlBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()

        if( userData == null ) {
            Log.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            Log.d(TAG, "initializeComponents: UserData is not null")
        }

        val idMbr = userData.idMbr
        val adrMac = userData.adrMac

        Log.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        viewModel.setUserCredentials(idMbr, adrMac)
    }
    override fun setupComponents() {
        val idRsd = getEntrySelected()?.id

        Log.d(TAG, "setupComponents: idRsd: $idRsd")

        if( idRsd == null ) {
            Log.d(TAG, "setupComponents: idRsd is null")
            showToast("Aucune entrée sélectionnée")
            navigateToPrevScreen()
            return
        } else {
            Log.d(TAG, "setupComponents: idRsd is not null")

            setupObservers()

            viewModel.fetchPlanAction(idRsd)

            setupListeners()
        }
    }
    override fun setupListeners() {
        binding.typeCtrlActivityEdleTxt.setOnClickListener { navigateToNextScreen(TYPE_CTRL_ACTIVITY_TAG_EDLE) }
        binding.typeCtrlActivityCpltTxt.setOnClickListener { navigateToNextScreen(TYPE_CTRL_ACTIVITY_TAG_CTRL) }
        binding.typeCtrlActivityRndTxt.setOnClickListener { navigateToNextScreen(TYPE_CTRL_ACTIVITY_TAG_RANDOM) }
        binding.typeCtrlActivityPlanActTxt.setOnClickListener { navigateToPlanActionsActivity() }
        binding.typeCtrlActivityEdlsTxt.setOnClickListener { navigateToNextScreen(TYPE_CTRL_ACTIVITY_TAG_EDLS) }
    }
    override fun setupObservers() {
        viewModel.planActionResult.observe(this, Observer { result ->
            canOpenPlanActions = result

            binding.typeCtrlActivityPlanActTxt.isEnabled = canOpenPlanActions
            binding.typeCtrlActivityPlanActTxt.alpha = if (canOpenPlanActions) 1.0f else 0.5f

            Log.d(TAG, "setupComponents: canOpenPlanActions updated to: $canOpenPlanActions")
        })

        viewModel.error.observe(this, Observer { errorPair ->
            errorPair?.let { (code, message) ->
                showToast(message)
            }
        })
    }



    private fun navigateToPrevScreen() {
        Log.d(TAG, "Navigating to SelectEntryActivity")
        val intent = Intent(this, SelectEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToNextScreen(type: String) {
        Log.d(TAG, "Navigating to ConfigCtrlActivity")

        setTypeCtrl(type)

        val intent = Intent(this, ConfigCtrlActivity::class.java).apply {
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
    private fun navigateToPlanActionsActivity() {
        Log.d(TAG, "Navigating to PlanActionsActivity")

        setTypeCtrl(TYPE_CTRL_ACTIVITY_TAG_LEVEE)

        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
