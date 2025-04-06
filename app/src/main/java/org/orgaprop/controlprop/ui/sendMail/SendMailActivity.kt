package org.orgaprop.controlprop.ui.sendMail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher

import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.launch

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivitySendMailBinding
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.config.TypeCtrlActivity
import org.orgaprop.controlprop.ui.getMail.GetMailActivity
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.viewmodels.SendMailViewModel

class SendMailActivity : BaseActivity() {

    private val TAG = "SendMailActivity"

    private lateinit var binding: ActivitySendMailBinding
    private val viewModel: SendMailViewModel by viewModel()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var user: LoginData

    private var typeSend: String? = ""
    private var capturedBitmap: Bitmap? = null

    companion object {
        const val SEND_MAIL_ACTIVITY_TYPE = "type"
        const val SEND_MAIL_ACTIVITY_PROBLEM_TECH = "tech"
        const val SEND_MAIL_ACTIVITY_CTRL = "ctrl"
        const val SEND_MAIL_ACTIVITY_PLAN = "plan"
        const val SEND_MAIL_ACTIVITY_AUTO = "auto"

        const val SEND_MAIL_ACTIVITY_TITLE_TECH = "DESORDRE TECHNIQUE"
        const val SEND_MAIL_ACTIVITY_TITLE_CTRL = "RAPPORT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                Log.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
                navigateToPrevScreen()
            }
        } else {
            // Pour les versions inférieures à Android 13
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        }
    }

    override fun initializeComponents() {
        binding = ActivitySendMailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()

        if( userData == null ) {
            Log.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            Log.d(TAG, "initializeComponents: UserData is not null")
            user = userData
        }

        val idMbr = user.idMbr
        val adrMac = user.adrMac

        Log.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        typeSend = intent.getStringExtra(SEND_MAIL_ACTIVITY_TYPE)

        viewModel.setUserCredentials(idMbr, adrMac, typeSend)

        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCameraResult(result.resultCode, result.data)
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) takePhoto() else showToast("Permission caméra refusée")
        }
    }
    override fun setupComponents() {
        if (typeSend != null) {
            when (typeSend) {
                SEND_MAIL_ACTIVITY_PROBLEM_TECH -> {
                    binding.sendMailActivityTitleTxt.text = SEND_MAIL_ACTIVITY_TITLE_TECH
                    binding.sendMailActivityDestInput2.visibility = View.INVISIBLE
                    binding.sendMailActivityDestInput3.visibility = View.INVISIBLE
                    binding.sendMailActivityDestInput4.visibility = View.INVISIBLE
                }
                SEND_MAIL_ACTIVITY_CTRL, SEND_MAIL_ACTIVITY_PLAN -> {
                    binding.sendMailActivityTitleTxt.text = SEND_MAIL_ACTIVITY_TITLE_CTRL
                    binding.sendMailActivityDestInput1.setText(user.mail)
                    binding.sendMailActivityTextInput.visibility = View.INVISIBLE
                    binding.sendMailActivityCaptureBtn.visibility = View.INVISIBLE
                }
                TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_LEVEE -> {
                    binding.sendMailActivityDestInput1.setText(user.mail)
                    binding.sendMailActivityTextInput.visibility = View.INVISIBLE
                    binding.sendMailActivityCaptureBtn.visibility = View.INVISIBLE
                }
            }
        }

        setupObservers()
        setupListeners()
    }
    override fun setupObservers() {
        lifecycleScope.launch {
            viewModel.sendResult.collect { result ->
                result?.fold(
                    onSuccess = {
                        Toast.makeText(this@SendMailActivity, "Mail envoyé", Toast.LENGTH_SHORT).show()
                        navigateToPrevScreen()
                    },
                    onFailure = { e ->
                        Toast.makeText(this@SendMailActivity, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
    override fun setupListeners() {
        binding.sendMailActivityCaptureBtn.setOnClickListener {
            checkCameraPermission()
        }

        binding.sendMailActivitySendBtn.setOnClickListener {
            sendMail()
        }

        binding.sendMailActivityPrevBtn.setOnClickListener {
            navigateToPrevScreen()
        }
    }
    override fun onDestroy() {
        cameraLauncher.unregister()
        super.onDestroy()
    }




    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                showToast("La permission caméra est nécessaire pour prendre des photos")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            showToast("Aucune application de caméra disponible")
        }
    }
    private fun handleCameraResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            capturedBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                data?.extras?.get("data") as? Bitmap
            }
            showToast("Photo capturée")
        } else {
            showToast("Capture annulée")
        }
    }

    private fun sendMail() {
        val destinations = listOf(
            binding.sendMailActivityDestInput1.text.toString(),
            binding.sendMailActivityDestInput2.text.toString(),
            binding.sendMailActivityDestInput3.text.toString(),
            binding.sendMailActivityDestInput4.text.toString()
        ).filter { it.isNotBlank() }

        if (destinations.isEmpty()) {
            showToast("Veuillez saisir au moins un destinataire")
            return
        }

        val dest1 = destinations.getOrElse(0) { "" }
        val dest2 = destinations.getOrElse(1) { "" }
        val dest3 = destinations.getOrElse(2) { "" }
        val dest4 = destinations.getOrElse(3) { "" }
        val message = binding.sendMailActivityTextInput.text.toString()
        val bitmap = capturedBitmap

        viewModel.sendMail(dest1, dest2, dest3, dest4, message, bitmap)
    }



    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToPrevScreen() {
        Log.d(TAG, "Navigating to Prev Activity")

        if (typeSend == SEND_MAIL_ACTIVITY_PROBLEM_TECH) {
            val intent = Intent(this, GetMailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }

        finish()
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: Back Pressed")
            navigateToPrevScreen()
        }
    }

}
