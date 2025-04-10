package org.orgaprop.controlprop.ui.sendMail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import android.window.OnBackInvokedDispatcher

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import org.koin.androidx.viewmodel.ext.android.viewModel

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.databinding.ActivitySendMailBinding
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.config.TypeCtrlActivity
import org.orgaprop.controlprop.ui.getMail.GetMailActivity
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.viewmodels.SendMailViewModel

class SendMailActivity : BaseActivity() {

    private lateinit var binding: ActivitySendMailBinding
    private val viewModel: SendMailViewModel by viewModel()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var user: LoginData
    private var photoUri: Uri? = null
    private var currentPhotoPath: String? = null
    private var capturedBitmap: Bitmap? = null
    private var typeSend: String? = ""
    private var idEntry: Int? = null

    companion object {
        private const val TAG = "SendMailActivity"

        const val SEND_MAIL_ACTIVITY_TYPE = "type"
        const val SEND_MAIL_ACTIVITY_PROBLEM_TECH = "tech"
        const val SEND_MAIL_ACTIVITY_CTRL = "ctrl"
        const val SEND_MAIL_ACTIVITY_PLAN = "plan"
        const val SEND_MAIL_ACTIVITY_AUTO = "auto"

        const val SEND_MAIL_ACTIVITY_TITLE_TECH = "DESORDRE TECHNIQUE"
        const val SEND_MAIL_ACTIVITY_TITLE_RAPPORT = "RAPPORT"

        const val SEND_MAIL_ACTIVITY_TAG_RSD = "idRsd"

        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val AUTHORITY_SUFFIX = ".fileprovider"
    }



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

        if (typeSend == SEND_MAIL_ACTIVITY_CTRL) {
            idEntry = intent.getIntExtra(SEND_MAIL_ACTIVITY_TAG_RSD, -1)
        }

        viewModel.setUserCredentials(idMbr, adrMac, typeSend)

        initializeLaunchers()
    }
    override fun setupComponents() {
        configureUIBasedOnType()

        setupObservers()
        setupListeners()
    }
    override fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                updateUIForState(state)
            }
        }
    }
    override fun setupListeners() {
        binding.sendMailActivityCaptureBtn.setOnClickListener {
            checkCameraPermission()
        }

        binding.sendMailActivitySendBtn.setOnClickListener {
            if (validateInputs()) {
                sendMail()
            }
        }

        binding.sendMailActivityPrevBtn.setOnClickListener {
            navigateToPrevScreen()
        }
    }
    override fun onDestroy() {
        try {
            cameraLauncher.unregister()
            requestPermissionLauncher.unregister()
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering launchers", e)
        }
        super.onDestroy()
    }



    private fun initializeLaunchers() {
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCameraResult(result.resultCode, result.data)
        }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                showPermissionExplanation()
            }
        }
    }
    private fun configureUIBasedOnType() {
        when (typeSend) {
            SEND_MAIL_ACTIVITY_PROBLEM_TECH -> {
                binding.sendMailActivityTitleTxt.text = SEND_MAIL_ACTIVITY_TITLE_TECH
                binding.sendMailActivityDestInput2.visibility = View.INVISIBLE
                binding.sendMailActivityDestInput3.visibility = View.INVISIBLE
                binding.sendMailActivityDestInput4.visibility = View.INVISIBLE
            }
            SEND_MAIL_ACTIVITY_CTRL, SEND_MAIL_ACTIVITY_PLAN -> {
                binding.sendMailActivityTitleTxt.text = SEND_MAIL_ACTIVITY_TITLE_RAPPORT
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
    private fun updateUIForState(state: SendMailViewModel.UiState) {
        when (state) {
            is SendMailViewModel.UiState.Idle -> {
                showLoading(false)
                binding.sendMailActivitySendBtn.isEnabled = true
            }
            is SendMailViewModel.UiState.Loading -> {
                showLoading(true)
                binding.sendMailActivitySendBtn.isEnabled = false
            }
            is SendMailViewModel.UiState.Success -> {
                showLoading(false)
                binding.sendMailActivitySendBtn.isEnabled = true
                UiUtils.showSuccessSnackbar(binding.root, state.message)
                navigateToPrevScreen()
            }
            is SendMailViewModel.UiState.Error -> {
                showLoading(false)
                binding.sendMailActivitySendBtn.isEnabled = true
                UiUtils.showErrorSnackbar(binding.root, state.message)
            }
        }
    }

    private fun validateInputs(): Boolean {
        val destinations = getDestinations()

        if (destinations.isEmpty()) {
            UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_no_recipient))
            return false
        }

        // Vérification des adresses email
        val invalidEmails = destinations.filter { !isValidEmail(it) }
        if (invalidEmails.isNotEmpty()) {
            UiUtils.showErrorSnackbar(
                binding.root,
                getString(R.string.error_invalid_emails, invalidEmails.joinToString())
            )
            return false
        }

        // Si c'est un rapport technique et qu'aucun message n'est saisi
        if (typeSend == SEND_MAIL_ACTIVITY_PROBLEM_TECH &&
            binding.sendMailActivityTextInput.text.toString().isBlank()) {
            UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_empty_message))
            return false
        }

        return true
    }
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    private fun getDestinations(): List<String> {
        return listOf(
            binding.sendMailActivityDestInput1.text.toString(),
            binding.sendMailActivityDestInput2.text.toString(),
            binding.sendMailActivityDestInput3.text.toString(),
            binding.sendMailActivityDestInput4.text.toString()
        ).filter { it.isNotBlank() }
    }



    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) ==
                    PackageManager.PERMISSION_GRANTED -> {
                takePhoto()
            }
            shouldShowRequestPermissionRationale(CAMERA_PERMISSION) -> {
                showPermissionExplanation()
            }
            else -> {
                requestPermissionLauncher.launch(CAMERA_PERMISSION)
            }
        }
    }
    private fun showPermissionExplanation() {
        UiUtils.showAlert(
            this,
            getString(R.string.camera_permission_explanation),
            getString(R.string.permission_required)
        ) {
            requestPermissionLauncher.launch(CAMERA_PERMISSION)
        }
    }



    private fun takePhoto() {
        try {
            val photoFile = createImageFile()
            photoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}$AUTHORITY_SUFFIX",
                photoFile
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }

            if (intent.resolveActivity(packageManager) != null) {
                cameraLauncher.launch(intent)
            } else {
                UiUtils.showErrorSnackbar(binding.root, getString(R.string.no_camera_app))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error creating image file", e)
            UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_creating_file))
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(null)

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }
    private fun handleCameraResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            photoUri?.let { uri ->
                try {
                    // Pour une miniature (si nécessaire pour prévisualisation)
                    capturedBitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

                    UiUtils.showInfoSnackbar(binding.root, getString(R.string.photo_captured_success))
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling camera result", e)
                    UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_processing_photo))
                }
            } ?: run {
                // Aucun URI, tenter de récupérer la miniature du Intent
                capturedBitmap = data?.extras?.getParcelable("data")

                if (capturedBitmap != null) {
                    UiUtils.showInfoSnackbar(binding.root, getString(R.string.thumbnail_captured))
                } else {
                    UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_no_photo_data))
                }
            }
        } else {
            UiUtils.showInfoSnackbar(binding.root, getString(R.string.photo_capture_cancelled))
        }
    }

    private fun sendMail() {
        val destinations = getDestinations()
        val destArray = Array(4) { index -> destinations.getOrElse(index) { "" } }
        val message = binding.sendMailActivityTextInput.text.toString()
        val entryId = idEntry?.toString() ?: ""

        viewModel.sendMail(
            dest1 = destArray[0],
            dest2 = destArray[1],
            dest3 = destArray[2],
            dest4 = destArray[3],
            message = message,
            photoUri = photoUri,
            currentPhotoPath = currentPhotoPath,
            entry = entryId
        )
    }



    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToPrevScreen() {
        Log.d(TAG, "Navigating to Prev Activity")

        currentPhotoPath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting temp file", e)
            }
        }

        if (typeSend == SEND_MAIL_ACTIVITY_PROBLEM_TECH) {
            val intent = Intent(this, GetMailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }

        finish()
    }



    /**
     * Affiche ou masque l'indicateur de chargement.
     *
     * @param isLoading Indique si l'état de chargement est actif
     */
    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            UiUtils.showProgressDialog(
                this,
                "Chargement en cours...",
                "Veuillez patienter",
                false
            )
        } else {
            UiUtils.dismissCurrentDialog()
        }
    }

}
