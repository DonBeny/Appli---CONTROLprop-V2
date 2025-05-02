package org.orgaprop.controlprop.ui.finish

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.window.OnBackInvokedDispatcher

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import kotlinx.coroutines.launch

import com.github.gcacace.signaturepad.views.SignaturePad

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivitySignatureBinding
import org.orgaprop.controlprop.managers.SignatureManager
import org.orgaprop.controlprop.models.ObjSignature
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.utils.FileUtils.bitmapToBase64
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.utils.UiUtils.showUiAlert
import org.orgaprop.controlprop.viewmodels.SignatureViewModel
import kotlin.math.abs

class SignatureActivity : BaseActivity() {

    companion object {
        private const val TAG = "SignatureActivity"
    }

    private lateinit var binding: ActivitySignatureBinding
    private val viewModel: SignatureViewModel by viewModel()

    private lateinit var user: LoginData
    private lateinit var entrySelected: SelectItem

    private var ctrlHasSigned = false
    private var agtHasSigned = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
            LogUtils.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }

    override fun initializeComponents() {
        binding = ActivitySignatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()

        if( userData == null ) {
            LogUtils.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            user = userData
            LogUtils.d(TAG, "initializeComponents: UserData is not null")
        }

        val idMbr = user.idMbr
        val adrMac = user.adrMac

        LogUtils.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        viewModel.setUserData(user)

        val savedEntry = getEntrySelected()

        if( savedEntry == null ) {
            LogUtils.d(TAG, "initializeComponents: Entry is null")
            navigateToSelectEntryActivity()
            return
        } else {
            LogUtils.d(TAG, "initializeComponents: Entry is not null")
            entrySelected = savedEntry
            viewModel.setEntrySelected(entrySelected)
        }
    }
    override fun setupComponents() {
        binding.signatureActivityCtrlSignaturePad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {}
            override fun onSigned() {
                ctrlHasSigned = true
                binding.signatureActivityCtrlClearBtn.isEnabled = true
                binding.signatureActivityAgtSignaturePad.isEnabled = true
                updateSaveButtonState()
            }
            override fun onClear() {
                ctrlHasSigned = false
                binding.signatureActivityCtrlClearBtn.isEnabled = false
                if (!agtHasSigned) {
                    binding.signatureActivityAgtSignaturePad.isEnabled = false
                }
                updateSaveButtonState()
            }
        })

        binding.signatureActivityAgtSignaturePad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {}
            override fun onSigned() {
                agtHasSigned = true
                binding.signatureActivityAgtClearBtn.isEnabled = true
                updateSaveButtonState()
            }
            override fun onClear() {
                agtHasSigned = false
                binding.signatureActivityAgtClearBtn.isEnabled = false
                updateSaveButtonState()
            }
        })

        binding.signatureActivityCtrlClearBtn.isEnabled = false
        binding.signatureActivityAgtClearBtn.isEnabled = false
        binding.signatureActivityAgtSignaturePad.isEnabled = false
        binding.signatureActivityCtrlSaveBtn.isEnabled = false

        setupPrestate()
        updateSaveButtonState()

        setupObservers()
        setupListeners()
    }
    override fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.signatureResult.collect { result ->
                    showLoading(false)

                    when (result) {
                        is SignatureManager.SignatureResult.Success -> {
                            LogUtils.d(TAG, "Signatures enregistrées avec succès")
                            onSignaturesSaved()
                        }
                        is SignatureManager.SignatureResult.PartialSuccess -> {
                            if (!result.failedIds.contains(entrySelected.id)) {
                                LogUtils.d(TAG, "Synchronisation partielle réussie pour cet élément")
                                onSignaturesSaved()
                            } else {
                                LogUtils.e(TAG, "Échec de synchronisation pour cet élément")
                                showUiAlert("Signatures enregistrées localement, mais échec de synchronisation avec le serveur.")
                            }
                        }
                        is SignatureManager.SignatureResult.Error -> {
                            LogUtils.e(TAG, "Erreur lors de l'enregistrement des signatures: ${result.message}")
                            showUiAlert(result.message)
                        }
                        null -> Unit
                    }
                }
            }
        }
    }
    override fun setupListeners() {
        binding.apply {
            signatureActivityPrevBtn.setOnClickListener { navigateToPrevScreen() }
            signatureActivityCtrlClearBtn.setOnClickListener { clearPadCtrl() }
            signatureActivityAgtClearBtn.setOnClickListener { clearPadAgt() }
            signatureActivityCtrlSaveBtn.setOnClickListener { validateSignatures() }

            signatureActivityAgtNameInput.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard()
                    true
                } else {
                    false
                }
            }
        }
    }



    private fun clearPadCtrl() {
        binding.signatureActivityCtrlSignaturePad.clear()
    }
    private fun clearPadAgt() {
        binding.signatureActivityAgtSignaturePad.clear()
    }

    private fun validateSignatures() {
        try {
            if (!ctrlHasSigned && !agtHasSigned) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }

            showLoading(true)

            val controlSignature = if (ctrlHasSigned) {
                binding.signatureActivityCtrlSignaturePad.signatureBitmap?.let { bitmapToBase64(it) } ?: ""
            } else ""

            val agentSignature = if (agtHasSigned) {
                binding.signatureActivityAgtSignaturePad.signatureBitmap?.let { bitmapToBase64(it) } ?: ""
            } else ""

            val agentName = binding.signatureActivityAgtNameInput.text.toString()

            val signatureData = ObjSignature(
                controlSignature = controlSignature,
                agentSignature = agentSignature,
                agentName = agentName
            )

            viewModel.saveSignatures(entrySelected, signatureData)
        } catch (e: Exception) {
            LogUtils.e(TAG, "Erreur lors de la validation des signatures", e)
            showLoading(false)
            showUiAlert("Une erreur est survenue lors de l'enregistrement des signatures")
        }
    }
    private fun onSignaturesSaved() {
        val updatedEntry = entrySelected.copy(
            signed = true
        )

        setEntrySelected(updatedEntry)

        setResult(RESULT_OK)
        finish()
    }

    private fun setupPrestate() {
        val prestate = entrySelected.prop?.ctrl?.prestate ?: 0

        try {
            LogUtils.d(TAG, "setupPrestate: prestate = $prestate")

            if (prestate > 0) {
                val listAgents = getListAgents()

                LogUtils.d(TAG, "setupPrestate: idPrestate = $prestate")

                listAgents?.let {
                    LogUtils.json(TAG, "setupPrestate: listAgents", listAgents)

                    val agentObj = it.optJSONObject(prestate.toString())
                    val agentName = agentObj?.optString("name", "")

                    LogUtils.json(TAG, "setupPrestate: agentName from agents:", agentObj)
                    LogUtils.d(TAG, "setupPrestate: agentName from agents = $agentName")

                    binding.signatureActivityAgtNameInput.setText(agentName)
                }
            }
            else if (prestate < 0) {
                val listPrestates = getListPrestates()
                listPrestates?.let {
                    LogUtils.json(TAG, "setupPrestate: listPrestates", listPrestates)

                    val prestateObj = it.optJSONObject(abs(prestate).toString())
                    val prestateName = prestateObj?.optString("name", "") ?: ""

                    LogUtils.d(TAG, "setupPrestate: prestateName from prestates = $prestateName")

                    binding.signatureActivityAgtNameInput.setText(prestateName)
                }
            }
            else {
                val entId = entrySelected.referents?.ent?.id
                val entType = entrySelected.referents?.ent?.type

                Log.d(TAG, "setupPrestate: prestate = 0, checking referents: entId = $entId, entType = $entType")

                if (entId != null) {
                    if (entType == "agent") {
                        val listAgents = getListAgents()
                        listAgents?.let {
                            val agentObj = it.optJSONObject(entId.toString())
                            val agentName = agentObj?.optString("name", "") ?: ""
                            Log.d(TAG, "setupPrestate: agentName from referents = $agentName")
                            binding.signatureActivityAgtNameInput.setText(agentName)
                        }
                    } else {
                        val listPrestates = getListPrestates()
                        listPrestates?.let {
                            val prestateObj = it.optJSONObject(entId.toString())
                            val prestateName = prestateObj?.optString("name", "") ?: ""
                            Log.d(TAG, "setupPrestate: prestateName from referents = $prestateName")
                            binding.signatureActivityAgtNameInput.setText(prestateName)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "Erreur lors du préchargement du nom de l'agent", e)
        }
    }



    private fun updateSaveButtonState() {
        binding.signatureActivityCtrlSaveBtn.isEnabled = ctrlHasSigned && agtHasSigned
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
                "Veuillez patienter"
            )
        } else {
            UiUtils.dismissCurrentDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        UiUtils.showConfirmationDialog(
            context = this,
            title = "Confirmer l'abandon",
            message = "Voulez-vous quitter sans enregistrer les signatures ?",
            positiveButtonText = "Oui",
            negativeButtonText = "Non",
            positiveAction = {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        )
    }



    private fun navigateToPrevScreen() {
        LogUtils.d(TAG, "navigateToPrevScreen: Navigating to previous screen")
        if( ctrlHasSigned || agtHasSigned ) {
            showExitConfirmationDialog()
        } else {
            viewModel.clearResult()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun navigateToMainActivity() {
        LogUtils.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToSelectEntryActivity() {
        LogUtils.d(TAG, "Navigating to SelectEntryActivity")
        val intent = Intent(this, SelectEntryActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }

}
