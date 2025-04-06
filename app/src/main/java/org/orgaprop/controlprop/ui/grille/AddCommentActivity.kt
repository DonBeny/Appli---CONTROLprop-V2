package org.orgaprop.controlprop.ui.grille

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.window.OnBackInvokedDispatcher

import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlinx.coroutines.launch

import org.orgaprop.controlprop.databinding.ActivityAddCommentBinding
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.utils.FileUtils
import org.orgaprop.controlprop.viewmodels.AddCommentViewModel

class AddCommentActivity : BaseActivity() {

    private lateinit var binding: ActivityAddCommentBinding
    private lateinit var viewModel: AddCommentViewModel
    private lateinit var user: LoginData

    private var elementIndex: Int = -1
    private var critterIndex: Int = -1

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val thumbnailBitmap = result.data?.let { data ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    data.extras?.getParcelable("data", Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    data.extras?.getParcelable("data") as? Bitmap
                }
            }

            thumbnailBitmap?.let { viewModel.setImageBitmap(it) }
        }
    }

    companion object {
        const val TAG = "AddCommentActivity"

        const val ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_TEXT = "commentText"
        const val ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_IMAGE = "commentImage"
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                Log.d(CtrlZoneActivity.TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
                navigateToPrevScreen()
            }
        } else {
            // Pour les versions inférieures à Android 13
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        }
    }



    override fun initializeComponents() {
        binding = ActivityAddCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(AddCommentViewModel::class.java)

        getUserData()?.let { userData ->
            user = userData
            viewModel.setUserData(user)

            elementIndex = intent.getIntExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_EXTRA_ELEMENT_POSITION, -1)
            critterIndex = intent.getIntExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_EXTRA_CRITTER_POSITION, -1)

            if (elementIndex == -1 || critterIndex == -1) {
                navigateToPrevScreen()
                return
            }

            intent.getStringExtra(ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_TEXT)?.let {
                binding.addCommentActivityCommentInput.setText(it)
                viewModel.setCommentText(it)
            }

            intent.getStringExtra(ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_IMAGE)?.let { imageString ->
                FileUtils.base64ToBitmap(imageString)?.let { bitmap ->
                    viewModel.setImageBitmap(bitmap)
                    binding.addCommentActivityCaptureImg.setImageBitmap(bitmap)
                }
            }

            showKeyboard()
        } ?: run {
            navigateToMainActivity()
        }
    }
    override fun setupComponents() {
        setupObservers()
        setupListeners()
    }
    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.imageBitmap.collect { bitmap ->
                        bitmap?.let {
                            binding.addCommentActivityCaptureImg.setImageBitmap(it)
                        }
                    }
                }

                launch {
                    viewModel.navigationEvent.collect { event ->
                        event?.let { handleNavigationEvent(it) }
                    }
                }
            }
        }
    }
    override fun setupListeners() {
        binding.addCommentActivityPrevBtn.setOnClickListener {
            viewModel.cancelComment()
        }

        binding.addCommentActivitySaveBtn.setOnClickListener {
            viewModel.prepareCommentData()
        }

        binding.addCommentActivityCaptureBtn.setOnClickListener {
            takePicture()
        }

        binding.addCommentActivityCommentInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showKeyboard()
            } else {
                hideKeyboard()
            }
        }

        binding.addCommentActivityCommentInput.doAfterTextChanged { text ->
            viewModel.updateCommentText(text?.toString() ?: "")
        }
    }



    private fun showKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        binding.addCommentActivityCommentInput.apply {
            requestFocus()
            postDelayed({
                imm.showSoftInput(this, 0)
            }, 100)
        }
    }
    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.addCommentActivityCommentInput.windowToken, 0)
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            takePictureLauncher.launch(intent)
        }
    }

    private fun handleNavigationEvent(event: AddCommentViewModel.NavigationEvent) {
        Log.d(CtrlZoneActivity.TAG, "handleNavigationEvent: $event")

        when (event) {
            is AddCommentViewModel.NavigationEvent.SaveComment -> {
                Log.d(TAG, "handleNavigationEvent: SaveComment")
                Log.d(TAG, "handleNavigationEvent: Comment text: ${event.commentText}")
                Log.d(TAG, "handleNavigationEvent: Image base64: ${event.imageBase64}")

                val resultIntent = Intent().apply {
                    putExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_EXTRA_ELEMENT_POSITION, elementIndex)
                    putExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_EXTRA_CRITTER_POSITION, critterIndex)
                    putExtra(ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_TEXT, event.commentText)
                    putExtra(ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_IMAGE, event.imageBase64)
                }

                setResult(RESULT_OK, resultIntent)
                finish()
            }
            AddCommentViewModel.NavigationEvent.Cancel -> {
                navigateToPrevScreen()
            }
        }
    }



    private fun navigateToPrevScreen() {
        setResult(RESULT_CANCELED)
        finish()
    }
    private fun navigateToMainActivity() {
        Log.d(CtrlZoneActivity.TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(CtrlZoneActivity.TAG, "handleOnBackPressed: Back Pressed")
            navigateToPrevScreen()
        }
    }

}