package org.orgaprop.controlprop.ui.getmail

import android.os.Bundle
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orgaprop.controlprop.databinding.ActivityGetMailBinding
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.viewmodels.GetMailViewModel

class GetMailActivity : BaseActivity() {

    private val TAG = "GetMailActivity"

    private lateinit var binding: ActivityGetMailBinding
    private val viewModel: GetMailViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeComponents()
        setupComponents()
    }

    override fun initializeComponents() {
        binding = ActivityGetMailBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setupComponents() {
        binding.getMailActivityMailBtn.setOnClickListener {
            val email = binding.getMailActivityMailTxt.text.toString()
            viewModel.submitEmail(email)
        }

        viewModel.response.observe(this) { response ->
            // Update UI with the response
        }

        viewModel.error.observe(this) { error ->
            // Show error message
        }
    }

}
