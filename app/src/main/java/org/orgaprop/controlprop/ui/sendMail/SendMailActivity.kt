package org.orgaprop.controlprop.ui.sendMail

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Patterns
import android.view.View
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
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.ui.grille.GrilleCtrlActivity
import org.orgaprop.controlprop.utils.FileUtils
import org.orgaprop.controlprop.utils.LogUtils
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
    private var idCtrl: Long? = null

    companion object {
        private const val TAG = "SendMailActivity"

        const val SEND_MAIL_ACTIVITY_TYPE = "type"
        const val SEND_MAIL_ACTIVITY_PROBLEM_TECH = "tech"
        const val SEND_MAIL_ACTIVITY_CTRL = "ctrl"
        const val SEND_MAIL_ACTIVITY_PLAN = "plan"
        const val SEND_MAIL_ACTIVITY_AUTO = "auto"

        const val SEND_MAIL_ACTIVITY_TITLE_TECH = "DESORDRE TECHNIQUE"
        const val SEND_MAIL_ACTIVITY_TITLE_RAPPORT = "RAPPORT"

        const val SEND_MAIL_ACTIVITY_TAG_RSD_ID = "idRsd"
        const val SEND_MAIL_ACTIVITY_TAG_CTRL_DATE = "ctrlDate"

        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT
        ) {
            LogUtils.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }

    override fun onDestroy() {
        try {
            cameraLauncher.unregister()
            requestPermissionLauncher.unregister()
        } catch (e: Exception) {
            LogUtils.e(TAG, "Error unregistering launchers", e)
        }
        super.onDestroy()
    }



    override fun initializeComponents() {
        binding = ActivitySendMailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()

        if( userData == null ) {
            LogUtils.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            LogUtils.d(TAG, "initializeComponents: UserData is not null")
            user = userData
        }

        val idMbr = user.idMbr
        val adrMac = user.adrMac

        LogUtils.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        typeSend = intent.getStringExtra(SEND_MAIL_ACTIVITY_TYPE)

        LogUtils.d(TAG, "initializeComponents: typeSend: $typeSend")

        idEntry = intent.getIntExtra(SEND_MAIL_ACTIVITY_TAG_RSD_ID, -1)
        idCtrl = intent.getLongExtra(SEND_MAIL_ACTIVITY_TAG_CTRL_DATE, -1)

        LogUtils.d(TAG, "initializeComponents: idEntry: $idEntry")

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



    private fun initializeLaunchers() {
        LogUtils.d(TAG, "initializeLaunchers: launcher camera")
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCameraResult(result.resultCode, result.data)
        }

        LogUtils.d(TAG, "initializeLaunchers: launcher permission")
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                showPermissionExplanation()
            }
        }

        LogUtils.d(TAG, "initializeLaunchers: launcher finished")
    }
    private fun configureUIBasedOnType() {
        LogUtils.d(TAG, "configureUIBasedOnType: typeSend: $typeSend")

        when (typeSend) {
            SEND_MAIL_ACTIVITY_PROBLEM_TECH -> {
                LogUtils.d(TAG, "configureUIBasedOnType: SEND_MAIL_ACTIVITY_PROBLEM_TECH")
                binding.sendMailActivityTitleTxt.text = SEND_MAIL_ACTIVITY_TITLE_TECH
                binding.sendMailActivityDestInput2.visibility = View.GONE
                binding.sendMailActivityDestInput3.visibility = View.GONE
                binding.sendMailActivityDestInput4.visibility = View.GONE
            }
            SEND_MAIL_ACTIVITY_CTRL, SEND_MAIL_ACTIVITY_PLAN -> {
                LogUtils.d(TAG, "configureUIBasedOnType: SEND_MAIL_ACTIVITY_CTRL or SEND_MAIL_ACTIVITY_PLAN")
                binding.sendMailActivityTitleTxt.text = SEND_MAIL_ACTIVITY_TITLE_RAPPORT
                binding.sendMailActivityDestInput1.setText(user.mail)
                binding.sendMailActivityTextInput.visibility = View.GONE
                binding.sendMailActivityCaptureBtn.visibility = View.GONE
            }
            TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_LEVEE -> {
                LogUtils.d(TAG, "configureUIBasedOnType: TypeCtrlActivity.TYPE_CTRL_ACTIVITY_TAG_LEVEE")
                binding.sendMailActivityDestInput1.setText(user.mail)
                binding.sendMailActivityTextInput.visibility = View.GONE
                binding.sendMailActivityCaptureBtn.visibility = View.GONE
            }
        }

        LogUtils.d(TAG, "configureUIBasedOnType: finished")
    }
    private fun updateUIForState(state: SendMailViewModel.UiState) {
        LogUtils.d(TAG, "updateUIForState: state: $state")

        when (state) {
            is SendMailViewModel.UiState.Idle -> {
                LogUtils.d(TAG, "updateUIForState: Idle")
                showLoading(false)
                binding.sendMailActivitySendBtn.isEnabled = true
            }
            is SendMailViewModel.UiState.Loading -> {
                LogUtils.d(TAG, "updateUIForState: Loading")
                showLoading(true)
                binding.sendMailActivitySendBtn.isEnabled = false
            }
            is SendMailViewModel.UiState.Success -> {
                LogUtils.d(TAG, "updateUIForState: Success")
                showLoading(false)
                binding.sendMailActivitySendBtn.isEnabled = true
                UiUtils.showSuccessSnackbar(binding.root, state.message)
                Handler(Looper.getMainLooper()).postDelayed({
                    navigateToPrevScreen()
                }, 1500)
            }
            is SendMailViewModel.UiState.Error -> {
                LogUtils.d(TAG, "updateUIForState: Error")
                showLoading(false)
                binding.sendMailActivitySendBtn.isEnabled = true
                UiUtils.showErrorSnackbar(binding.root, state.message)
            }
        }

        LogUtils.d(TAG, "updateUIForState: finished")
    }

    private fun validateInputs(): Boolean {
        LogUtils.d(TAG, "validateInputs: typeSend: $typeSend")

        val destinations = getDestinations()

        LogUtils.d(TAG, "validateInputs: destinations: $destinations")

        if (destinations.isEmpty()) {
            LogUtils.d(TAG, "validateInputs: destinations is empty")
            UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_no_recipient))
            return false
        }

        val invalidEmails = destinations.filter { !isValidEmail(it) }

        LogUtils.d(TAG, "validateInputs: invalidEmails: $invalidEmails")

        if (invalidEmails.isNotEmpty()) {
            LogUtils.d(TAG, "validateInputs: invalidEmails is not empty")

            UiUtils.showErrorSnackbar(
                binding.root,
                getString(R.string.error_invalid_emails, invalidEmails.joinToString())
            )
            return false
        }

        if (typeSend == SEND_MAIL_ACTIVITY_PROBLEM_TECH &&
            binding.sendMailActivityTextInput.text.toString().isBlank()) {
            LogUtils.d(TAG, "validateInputs: message is empty")

            UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_empty_message))
            return false
        }

        LogUtils.d(TAG, "validateInputs: finished true")

        return true
    }
    private fun isValidEmail(email: String): Boolean {
        LogUtils.d(TAG, "isValidEmail: email: $email")
        LogUtils.d(TAG, "isValidEmail: return: ${Patterns.EMAIL_ADDRESS.matcher(email).matches()}")

        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    private fun getDestinations(): List<String> {
        LogUtils.d(TAG, "getDestinations: dest1: ${binding.sendMailActivityDestInput1.text}")
        LogUtils.d(TAG, "getDestinations: dest2: ${binding.sendMailActivityDestInput2.text}")
        LogUtils.d(TAG, "getDestinations: dest3: ${binding.sendMailActivityDestInput3.text}")
        LogUtils.d(TAG, "getDestinations: dest4: ${binding.sendMailActivityDestInput4.text}")

        return listOf(
            binding.sendMailActivityDestInput1.text.toString(),
            binding.sendMailActivityDestInput2.text.toString(),
            binding.sendMailActivityDestInput3.text.toString(),
            binding.sendMailActivityDestInput4.text.toString()
        ).filter { it.isNotBlank() }
    }



    private fun checkCameraPermission() {
        LogUtils.d(TAG, "checkCameraPermission: checking camera permission")

        when {
            ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED -> {
                LogUtils.d(TAG, "checkCameraPermission: permission granted")

                takePhoto()
            }
            shouldShowRequestPermissionRationale(CAMERA_PERMISSION) -> {
                LogUtils.d(TAG, "checkCameraPermission: permission denied")

                showPermissionExplanation()
            }
            else -> {
                LogUtils.d(TAG, "checkCameraPermission: request permission")

                requestPermissionLauncher.launch(CAMERA_PERMISSION)
            }
        }

        LogUtils.d(TAG, "checkCameraPermission: finished")
    }
    private fun showPermissionExplanation() {
        LogUtils.d(TAG, "showPermissionExplanation: showing permission explanation")

        UiUtils.showAlert(
            this,
            getString(R.string.camera_permission_explanation),
            getString(R.string.permission_required)
        ) {
            requestPermissionLauncher.launch(CAMERA_PERMISSION)
        }
    }



    private fun takePhoto() {
        LogUtils.d(TAG, "takePhoto: taking photo")

        try {
            val photoFile = createImageFile()
            val authority = "${applicationContext.packageName}.fileprovider"

            LogUtils.d(TAG, "takePhoto: using authority: $authority")

            photoUri = FileProvider.getUriForFile(
                this,
                authority,
                photoFile
            )

            LogUtils.d(TAG, "takePhoto: photoUri: $photoUri")

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            }

            if (intent.resolveActivity(packageManager) != null) {
                LogUtils.d(TAG, "takePhoto: starting camera activity")

                cameraLauncher.launch(intent)
            } else {
                LogUtils.d(TAG, "takePhoto: no camera app found")

                UiUtils.showErrorSnackbar(binding.root, getString(R.string.no_camera_app))
            }
        } catch (e: IOException) {
            LogUtils.e(TAG, "Error creating image file", e)
            UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_creating_file))
        }
    }
    @Throws(IOException::class)
    private fun createImageFile(): File {
        LogUtils.d(TAG, "createImageFile: creating image file")

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        LogUtils.d(TAG, "createImageFile: imageFileName: $imageFileName")
        LogUtils.d(TAG, "createImageFile: storageDir: $storageDir")

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }
    private fun handleCameraResult(resultCode: Int, data: Intent?) {
        LogUtils.d(TAG, "handleCameraResult: resultCode: $resultCode")

        if (resultCode == RESULT_OK) {
            photoUri?.let { uri ->
                try {
                    LogUtils.d(TAG, "handleCameraResult: uri: $uri")

                    val source = ImageDecoder.createSource(contentResolver, uri)
                    capturedBitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }

                    capturedBitmap = FileUtils.fixBitmapOrientation(this, capturedBitmap, uri)

                    UiUtils.showInfoSnackbar(binding.root, getString(R.string.photo_captured_success))
                } catch (e: Exception) {
                    LogUtils.e(TAG, "Error handling camera result", e)
                    UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_processing_photo))
                }
            } ?: run {
                LogUtils.d(TAG, "handleCameraResult: photoUri is null")

                capturedBitmap = data?.extras?.getParcelable("data", Bitmap::class.java)

                if (capturedBitmap != null && (capturedBitmap!!.width < 500 || capturedBitmap!!.height < 500)) {
                    LogUtils.d(TAG, "handleCameraResult: only thumbnail received, quality may be poor")
                    UiUtils.showInfoSnackbar(binding.root, getString(R.string.thumbnail_captured_low_quality))
                } else if (capturedBitmap != null) {
                    UiUtils.showInfoSnackbar(binding.root, getString(R.string.thumbnail_captured))
                } else {
                    UiUtils.showErrorSnackbar(binding.root, getString(R.string.error_no_photo_data))
                }
            }
        } else {
            LogUtils.d(TAG, "handleCameraResult: photo capture cancelled")

            UiUtils.showInfoSnackbar(binding.root, getString(R.string.photo_capture_cancelled))
        }
    }

    private fun sendMail() {
        LogUtils.d(TAG, "sendMail: sending mail")

        val destinations = getDestinations()
        val destArray = Array(4) { index -> destinations.getOrElse(index) { "" } }
        val message = binding.sendMailActivityTextInput.text.toString()
        val entryId = idEntry?.toString() ?: ""
        val idCtrl = idCtrl?.toString() ?: ""

        LogUtils.d(TAG, "sendMail: destArray: $destArray")
        LogUtils.d(TAG, "sendMail: message: $message")
        LogUtils.d(TAG, "sendMail: entryId: $entryId")

        viewModel.sendMail(
            dest1 = destArray[0],
            dest2 = destArray[1],
            dest3 = destArray[2],
            dest4 = destArray[3],
            message = message,
            photoUri = photoUri,
            currentPhotoPath = currentPhotoPath,
            entry = entryId,
            timer = idCtrl
        )
    }



    private fun navigateToMainActivity() {
        LogUtils.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToPrevScreen() {
        LogUtils.d(TAG, "Navigating to Prev Activity")

        currentPhotoPath?.let { path ->
            try {
                File(path).delete()
            } catch (e: Exception) {
                LogUtils.e(TAG, "Error deleting temp file", e)
            }
        }

        if (typeSend == SEND_MAIL_ACTIVITY_PROBLEM_TECH) {
            LogUtils.d(TAG, "Navigating to GrilleCtrlActivity")

            val intent = Intent(this, GrilleCtrlActivity::class.java).apply {
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
