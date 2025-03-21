package org.orgaprop.controlprop.ui.getmail

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orgaprop.controlprop.databinding.ActivityGetMailBinding
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.viewmodels.GetMailViewModel

class GetMailActivity : BaseActivity() {

    private val TAG = "GetMailActivity"

    private lateinit var binding: ActivityGetMailBinding
    private val viewModel: GetMailViewModel by viewModel()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initializeComponents()
        setupComponents()
    }



    override fun initializeComponents() {
        binding = ActivityGetMailBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
    override fun setupComponents() {
        setupListeners()
        setupObservers()
    }
    override fun setupListeners() {
        binding.getMailActivityMailBtn.setOnClickListener {
            val email = binding.getMailActivityMailTxt.text.toString()
            viewModel.submitEmail(email)
        }
    }
    override fun setupObservers() {
        viewModel.response.observe(this) { response ->
            Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
            navigateToMainActivity()
        }

        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }



    private fun navigateToMainActivity() {
        Log.d(TAG, "navigateToMainActivity: Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }



    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: Back Pressed")
            navigateToMainActivity()
        }
    }

}
