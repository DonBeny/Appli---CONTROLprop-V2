package org.orgaprop.controlprop.ui.finish

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.AlertDialog

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
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.utils.UiUtils.showUiAlert
import org.orgaprop.controlprop.viewmodels.SignatureViewModel

class SignatureActivity : BaseActivity() {

    private val TAG = "SignatureActivity"

    private lateinit var binding: ActivitySignatureBinding
    private val viewModel: SignatureViewModel by viewModel()

    private lateinit var user: LoginData
    private lateinit var entrySelected: SelectItem

    private var ctrlHasSigned = false
    private var agtHasSigned = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
            Log.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }

    override fun initializeComponents() {
        binding = ActivitySignatureBinding.inflate(layoutInflater)
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
                            Log.d(TAG, "Signatures enregistrées avec succès")
                            onSignaturesSaved()
                        }
                        is SignatureManager.SignatureResult.PartialSuccess -> {
                            if (!result.failedIds.contains(entrySelected.id)) {
                                Log.d(TAG, "Synchronisation partielle réussie pour cet élément")
                                onSignaturesSaved()
                            } else {
                                Log.e(TAG, "Échec de synchronisation pour cet élément")
                                showUiAlert("Signatures enregistrées localement, mais échec de synchronisation avec le serveur.")
                            }
                        }
                        is SignatureManager.SignatureResult.Error -> {
                            Log.e(TAG, "Erreur lors de l'enregistrement des signatures: ${result.message}")
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
            // Si aucune signature n'a été effectuée, simplement retourner à l'écran précédent
            if (!ctrlHasSigned && !agtHasSigned) {
                setResult(RESULT_CANCELED)
                finish()
                return
            }

            showLoading(true)

            // Récupérer les signatures existantes (si présentes)
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

            // Enregistrer les signatures
            viewModel.saveSignatures(entrySelected, signatureData)
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la validation des signatures", e)
            showLoading(false)
            showUiAlert("Une erreur est survenue lors de l'enregistrement des signatures")
        }
    }
    private fun onSignaturesSaved() {
        setResult(RESULT_OK)
        finish()
    }

    private fun setupPrestate() {
        // Récupérer la valeur de prestate
        val prestate = entrySelected.prop?.ctrl?.prestate ?: 0

        try {
            Log.d(TAG, "setupPrestate: prestate = $prestate")

            // Si prestate est positif, utiliser la liste des agents
            if (prestate > 0) {
                val listAgents = getListAgents()
                listAgents?.let {
                    Log.d(TAG, "setupPrestate: listAgents = $listAgents")
                    // Trouver l'agent correspondant à prestate dans la liste
                    val agentObj = it.optJSONObject(prestate.toString())
                    val agentName = agentObj?.optString("txt", "") ?: ""
                    Log.d(TAG, "setupPrestate: agentName from agents = $agentName")
                    binding.signatureActivityAgtNameInput.setText(agentName)
                }
            }
            // Si prestate est négatif, utiliser la liste des prestataires
            else if (prestate < 0) {
                val listPrestates = getListPrestates()
                listPrestates?.let {
                    Log.d(TAG, "setupPrestate: listPrestates = $listPrestates")
                    // Utiliser la valeur absolue de prestate comme clé
                    val prestateObj = it.optJSONObject(Math.abs(prestate).toString())
                    val prestateName = prestateObj?.optString("txt", "") ?: ""
                    Log.d(TAG, "setupPrestate: prestateName from prestates = $prestateName")
                    binding.signatureActivityAgtNameInput.setText(prestateName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du préchargement du nom de l'agent", e)
        }
    }



    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun updateSaveButtonState() {
        // Activer le bouton de sauvegarde uniquement si au moins une signature est présente
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
                "Veuillez patienter",
                false
            )
        } else {
            UiUtils.dismissCurrentDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirmer l'abandon")
            .setMessage("Voulez-vous quitter sans enregistrer les signatures ?")
            .setPositiveButton("Oui") { _, _ ->
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
            .setNegativeButton("Non", null)
            .show()
    }



    private fun navigateToPrevScreen() {
        Log.d(TAG, "navigateToPrevScreen: Navigating to previous screen")
        if( ctrlHasSigned || agtHasSigned ) {
            showExitConfirmationDialog()
        } else {
            viewModel.clearResult()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, LoginActivity::class.java).apply {
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

}
