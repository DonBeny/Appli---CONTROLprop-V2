package org.orgaprop.controlprop.ui.config

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import org.json.JSONObject

import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orgaprop.controlprop.R

import org.orgaprop.controlprop.databinding.ActivityConfigCtrlBinding
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.HomeActivity
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.ui.selectentry.SelectEntryActivity
import org.orgaprop.controlprop.viewmodels.ConfigCtrlViewModel

class ConfigCtrlActivity: BaseActivity() {

    private val TAG = "ConfigCtrlActivity"

    private lateinit var binding: ActivityConfigCtrlBinding
    private val viewModel: ConfigCtrlViewModel by viewModel()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initializeComponents()
        setupComponents()
    }



    override fun initializeComponents() {
        binding = ActivityConfigCtrlBinding.inflate(layoutInflater)
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
            navigateToSelectEntryActivity()
            return
        } else {
            Log.d(TAG, "setupComponents: idRsd is not null")

            setupListeners()
            setupObservers()
        }
    }
    override fun setupListeners() {
        binding.startCtrlActivityProgChx.setOnClickListener {
            viewModel.setCtrlInopine(false)
        }
        binding.startCtrlActivityInopineChx.setOnClickListener {
            viewModel.setCtrlInopine(true)
        }

        binding.startCtrlActivityNormChx.setOnClickListener {
            viewModel.setMeteoPerturbe(false)
        }
        binding.startCtrlActivityPerturbChx.setOnClickListener {
            viewModel.setMeteoPerturbe(true)
        }

        binding.startCtrlActivityPresentChx.setOnClickListener {
            viewModel.setProdPresent(true)
        }
        binding.startCtrlActivityAbsentChx.setOnClickListener {
            viewModel.setProdPresent(false)
        }

        binding.startCtrlActivityConformChx.setOnClickListener {
            viewModel.setAffConforme(true)
        }
        binding.startCtrlActivityNoConformChx.setOnClickListener {
            viewModel.setAffConforme(false)
        }

        binding.startCtrlActivityStartBtn.setOnClickListener {
            launchMakeCtrlActivity()
        }
        binding.startCtrlActivityCancelBtn.setOnClickListener {
            navigateToSelectEntryActivity()
        }
    }
    override fun setupObservers() {
        viewModel.ctrlInopine.observe(this, Observer { isInopine ->
            updateButtonState(binding.startCtrlActivityProgChx, !isInopine)
            updateButtonState(binding.startCtrlActivityInopineChx, isInopine)
        })

        viewModel.meteoPerturbe.observe(this, Observer { isPerturbe ->
            updateButtonState(binding.startCtrlActivityNormChx, !isPerturbe)
            updateButtonState(binding.startCtrlActivityPerturbChx, isPerturbe)
        })

        viewModel.prodPresent.observe(this, Observer { isPresent ->
            updateButtonState(binding.startCtrlActivityPresentChx, isPresent)
            updateButtonState(binding.startCtrlActivityAbsentChx, !isPresent)
        })

        viewModel.affConforme.observe(this, Observer { isConforme ->
            updateButtonState(binding.startCtrlActivityConformChx, isConforme)
            updateButtonState(binding.startCtrlActivityNoConformChx, !isConforme)
        })
    }



    private fun updateButtonState(button: Button, isSelected: Boolean) {
        button.background = if (isSelected) {
            AppCompatResources.getDrawable(this, R.drawable.button_enabled)
        } else {
            AppCompatResources.getDrawable(this, R.drawable.button_desabled)
        }
        button.setTextColor(
            if (isSelected) ContextCompat.getColor(this, R.color.main_ctrl_prop)
            else ContextCompat.getColor(this, R.color._dark_grey)
        )
    }



    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
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
    private fun navigateToTypeCtrlActivity() {
        Log.d(TAG, "Navigating to TypeCtrlActivity")
        val intent = Intent(this, TypeCtrlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun launchMakeCtrlActivity() {
        Log.d(TAG, "Launching MakeCtrlActivity")

        val donn = JSONObject().apply {
            put(PREF_SAVED_CONFIG_CTRL_VISIT, viewModel.ctrlInopine)
            put(PREF_SAVED_CONFIG_CTRL_METEO, viewModel.meteoPerturbe)
            put(PREF_SAVED_CONFIG_CTRL_PROD, viewModel.prodPresent)
            put(PREF_SAVED_CONFIG_CTRL_AFF, viewModel.affConforme)
        }

        setConfigCtrl(donn)

        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: Back Pressed")
            navigateToTypeCtrlActivity()
        }
    }

}