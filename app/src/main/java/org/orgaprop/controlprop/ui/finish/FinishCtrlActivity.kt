package org.orgaprop.controlprop.ui.finish

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivityFinishCtrlBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.viewmodels.FinishCtrlViewModel



class FinishCtrlActivity : BaseActivity() {

    private val TAG = "FinishCtrlActivity"

    private lateinit var binding: ActivityFinishCtrlBinding
    private val viewModel: FinishCtrlViewModel by viewModel()

    private lateinit var user: LoginData
    private lateinit var entrySelected: SelectItem



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT
        ) {
            Log.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
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

    }
    override fun setupListeners() {
        binding.finishCtrlActivityPrevBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on previous button")
            navigateToPrevScreen()
        }

        binding.finishCtrlActivityPlanBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on plan button")
        }

        binding.finishCtrlActivitySignBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on sign button")
        }

        binding.finishCtrlActivitySendBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on send button")
        }

        binding.finishCtrlActivityEndBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: Clicked on end button")
        }
    }



    private fun navigateToPrevScreen() {
        Log.d(TAG, "navigateToPrevScreen: Navigating to previous screen")
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



    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: Back Pressed")
            navigateToPrevScreen()
        }
    }

}
