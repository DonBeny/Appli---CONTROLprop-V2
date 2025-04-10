package org.orgaprop.controlprop.ui.grille

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import android.window.OnBackInvokedDispatcher

import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.json.JSONObject

import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orgaprop.controlprop.R

import org.orgaprop.controlprop.databinding.ActivityGrilleCtrlBinding
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.managers.GrilleCtrlManager
import org.orgaprop.controlprop.models.ObjAgent
import org.orgaprop.controlprop.models.ObjBtnZone
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.toObjAgentList
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.config.ConfigCtrlActivity
import org.orgaprop.controlprop.ui.finish.FinishCtrlActivity
import org.orgaprop.controlprop.ui.grille.adapters.AgentSpinnerAdapter
import org.orgaprop.controlprop.ui.grille.adapters.BtnZoneAdapter
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.ui.sendMail.SendMailActivity
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.viewmodels.GrilleCtrlViewModel

class GrilleCtrlActivity : BaseActivity() {

    private val TAG = "GrilleCtrlActivity"

    private lateinit var binding: ActivityGrilleCtrlBinding
    private val viewModel: GrilleCtrlViewModel by viewModel()
    private lateinit var btnZoneAdapter: BtnZoneAdapter
    private val grilleCtrlManager: GrilleCtrlManager by inject()

    private var progressDialog: AlertDialog? = null

    private lateinit var user: LoginData
    private var idRsd: Int = 0
    private lateinit var entrySelected: SelectItem

    private var typeCtrl: String = ""
    private lateinit var confCtrl: JSONObject

    private var withProxi: Boolean = false
    private var withContract: Boolean = false
    private var listAgents: List<ObjAgent> = emptyList()
    private var listPrestates: List<ObjAgent> = emptyList()

    private var isInitialDataSetupDone = false
    private var isSpinnerSetupDone = false

    private val ctrlZoneLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                val zoneId = intent.getIntExtra("zoneId", -1)
                if (zoneId != -1) {
                    Log.d(TAG, "Received zoneId: $zoneId")

                    val controlledElements = intent.getStringExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_EXTRA_CONTROLLED_ELEMENTS)

                    Log.d(TAG, "Received controlled elements: $controlledElements")

                    controlledElements?.let { elements ->
                        try {
                            val typeToken = object : TypeToken<List<ObjElement>>() {}.type
                            val newControlledElements = Gson().fromJson<List<ObjElement>>(elements, typeToken)
                            updateZoneNotes(zoneId, newControlledElements)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing controlled elements", e)
                            showErrorMessage("Erreur lors de l'analyse des éléments contrôlés")
                        }
                    }
                }
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
            Log.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        progressDialog?.dismiss()
        progressDialog = null
    }



    override fun initializeComponents() {
        binding = ActivityGrilleCtrlBinding.inflate(layoutInflater)
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

        viewModel.setUserData(user, withProxi(), withContract())
    }
    override fun setupComponents() {
        val entrySaved = getEntrySelected()
        val typeCtrlSaved = getTypeCtrl()
        val confCtrlSaved = getConfigCtrl()
        val proxiSaved = withProxi()
        val contractSaved = withContract()
        val listAgentsSaved = getListAgents()
        val listPrestatesSaved = getListPrestates()

        if (entrySaved == null || typeCtrlSaved == null || confCtrlSaved == null || listAgentsSaved == null || listPrestatesSaved == null) {
            Log.e(TAG, "setupComponents: Missing required data, navigating to SelectEntryActivity")

            if (entrySaved == null) Log.e(TAG, "setupComponents: entrySaved is null")
            if (typeCtrlSaved == null) Log.e(TAG, "setupComponents: typeCtrlSaved is null")
            if (confCtrlSaved == null) Log.e(TAG, "setupComponents: confCtrlSaved is null")
            if (listAgentsSaved == null) Log.e(TAG, "setupComponents: listAgentsSaved is null")
            if (listPrestatesSaved == null) Log.e(TAG, "setupComponents: listPrestatesSaved is null")

            navigateToSelectEntryActivity()
            return
        } else {
            Log.d(TAG, "setupComponents: All is ok")

            entrySelected = entrySaved
            idRsd = entrySaved.id
            confCtrl = confCtrlSaved
            typeCtrl = typeCtrlSaved
            withProxi = proxiSaved
            withContract = contractSaved

            try {
                loadSavedGrilleData()

                Log.d(TAG, "setupComponents: Entry selected ID: ${entrySelected.id}")

                viewModel.setEntrySelected(entrySelected)
                viewModel.refreshAllNotes()

                listAgents = listAgentsSaved.toObjAgentList()
                listPrestates = listPrestatesSaved.toObjAgentList()

                setupZoneRecyclerView()

                if (!isInitialDataSetupDone) {
                    initializeUI()
                    isInitialDataSetupDone = true
                }

                setupObservers()
                setupListeners()

                viewModel.refreshAllNotes()
            } catch (e: BaseException) {
                Log.e(TAG, "setupComponents: Error loading saved grille data", e)
                showErrorMessage(e.message ?: ErrorCodes.getMessageForCode(e.code))
            } catch (e: Exception) {
                Log.e(TAG, "setupComponents: Unexpected error", e)
                showErrorMessage("Une erreur inattendue s'est produite lors du chargement des données")
            }
        }
    }
    override fun setupObservers() {
        Log.d(TAG, "setupObservers: Setting up observers")

        viewModel.noteCtrl.observe(this) { note ->
            binding.grilleCtrlActivityNoteCtrlTxt.text = note

            Log.d(TAG, "noteCtrl: $note")

            val noteValue = if (note == "S O") -1 else note.removeSuffix("%").toIntOrNull() ?: -1

            val backgroundRes = when {
                noteValue < 0 -> R.drawable.ctrl_note_grey
                noteValue < user.limits.down -> R.drawable.ctrl_note_red
                noteValue >= user.limits.top -> R.drawable.ctrl_note_green
                else -> R.drawable.ctrl_note_orange
            }

            binding.grilleCtrlActivityNoteCtrlTxt.setBackgroundResource(backgroundRes)
        }

        viewModel.navigateToNext.observe(this) { success ->
            if (success) {
                UiUtils.showSuccessSnackbar(binding.root, "Contrôle finalisé avec succès")
                navigateToNextScreen()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            showLoading(isLoading)
        }

        viewModel.error.observe(this) { errorData ->
            errorData?.let { (code, message) ->
                showErrorMessage(message)
                viewModel.clearError()
            }
        }
    }
    override fun setupListeners() {
        Log.d(TAG, "setupListeners: Setting up listeners")

        binding.grilleCtrlActivityPrevBtn.setOnClickListener {
            navigateToPrevScreen()
        }

        binding.grilleCtrlActivityTechBtn.setOnClickListener {
            val intent = Intent(this, SendMailActivity::class.java).apply {
                putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TYPE, SendMailActivity.SEND_MAIL_ACTIVITY_PROBLEM_TECH)
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }

        binding.grilleCtrlActivityCommentBtn.setOnClickListener {
            // TODO : Quoi en faire ?
        }

        binding.grilleCtrlActivityAgtTxt.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val item = parent.getItemAtPosition(position)
                if (item is ObjAgent) {
                    if (entrySelected.prop == null || entrySelected.prop?.ctrl == null) {
                        Log.e(TAG, "prop or ctrl is null, cannot update prestate")
                        return
                    }

                    val isAgent = listAgents.any { it.id == item.id }
                    val prestateValue = if (isAgent) {
                        item.id
                    } else {
                        -item.id
                    }

                    try {
                        val updatedEntry = entrySelected.copy(
                            prop = entrySelected.prop?.copy(
                                ctrl = entrySelected.prop!!.ctrl.copy(
                                    prestate = prestateValue
                                )
                            )
                        )
                        entrySelected = updatedEntry
                        setEntrySelected(updatedEntry)

                        // MODIFICATION: Sauvegarde après modification
                        try {
                            grilleCtrlManager.saveControlProgress(updatedEntry)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error saving control after agent selection", e)
                            // Ne pas bloquer l'UI pour cette erreur
                        }

                        Log.d(TAG, "Selected agent: ${item.name} with ID ${item.id}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating prestate", e)
                        showErrorMessage("Erreur lors de la mise à jour de l'agent")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ne rien faire
            }
        }

        binding.grilleCtrlActivityEndBtn.setOnClickListener {
            Log.d(TAG, "End button clicked")
            UiUtils.showAlert(
                this,
                "Voulez-vous finaliser ce contrôle ?",
                "Confirmation"
            )
            viewModel.finishCtrl()
        }
    }



    private fun loadSavedGrilleData() {
        Log.d(TAG, "loadSavedGrilleData: Loading saved grille data")

        try {
            entrySelected = grilleCtrlManager.loadResidenceData(
                entrySelected,
                typeCtrl,
                confCtrl
            )

            grilleCtrlManager.saveControlProgress(entrySelected)

            Log.d(TAG, "loadSavedGrilleData: Data loaded for entry ${entrySelected.id}")
        } catch (e: BaseException) {
            Log.e(TAG, "loadSavedGrilleData: Error with code ${e.code}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "loadSavedGrilleData: Unexpected error", e)
            throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Erreur lors du chargement des données de la grille", e)
        }
    }
    private fun initializeUI() {
        Log.d(TAG, "initializeUI: Initializing UI")

        viewModel.residenceData.value?.let { residence ->
            val entry = "Entrée ${residence.entry}"

            Log.d(TAG, "initializeUI: residence: $residence")

            binding.grilleCtrlActivityRefRsdTxt.text = residence.ref
            binding.grilleCtrlActivityNameRsdTxt.text = residence.name
            binding.grilleCtrlActivityEntryRsdTxt.text = entry
            binding.grilleCtrlActivityAdrRsdTxt.text = residence.address
            binding.grilleCtrlActivityEcdrTxt.text = residence.referents?.ecdr?.name
            binding.grilleCtrlActivityAdmTxt.text = residence.referents?.adm?.name

            setupSpinner()
        }
    }

    private fun setupSpinner() {
        Log.d(TAG, "setupSpinner: Setting up spinner")

        if (isSpinnerSetupDone) return

        Log.d(TAG, "setupSpinner: listAgents: $listAgents")
        Log.d(TAG, "setupSpinner: listPrestates: $listPrestates")

        val adapter = AgentSpinnerAdapter(this, listAgents, listPrestates)
        binding.grilleCtrlActivityAgtTxt.adapter = adapter

        val selectedAgentId = if ((viewModel.residenceData.value?.prop?.ctrl?.prestate ?: 0) != 0) {
            viewModel.residenceData.value?.prop?.ctrl?.prestate
        } else {
            viewModel.residenceData.value?.referents?.ent?.id
        }
        if (selectedAgentId != null) {
            val position = findAgentPositionById(selectedAgentId, listAgents, listPrestates)
            if (position != -1) {
                binding.grilleCtrlActivityAgtTxt.setSelection(position)
            }
        }

        Log.d(TAG, "setupSpinner: Spinner setup done")

        isSpinnerSetupDone = true
    }
    private fun findAgentPositionById(agentId: Int, agents: List<ObjAgent>, prestates: List<ObjAgent>): Int {
        Log.d(TAG, "findAgentPositionById: agentId: $agentId")

        val agentIndex = agents.indexOfFirst { it.id == agentId }
        if (agentIndex != -1) {
            return agentIndex + 1
        }

        val prestateIndex = prestates.indexOfFirst { it.id == agentId }
        if (prestateIndex != -1) {
            return prestateIndex + agents.size + 2
        }

        return -1
    }

    private fun setupZoneRecyclerView() {
        Log.d(TAG, "setupZoneRecyclerView: Setting up zone recycler view")

        binding.grilleCtrlActivityListZoneLyt.layoutManager = GridLayoutManager(this, 3)

        val minLimit = user.limits.down
        val maxLimit = user.limits.top

        btnZoneAdapter = BtnZoneAdapter(emptyList(), minLimit, maxLimit) { zone ->
            onBtnZoneClicked(zone)
        }
        binding.grilleCtrlActivityListZoneLyt.adapter = btnZoneAdapter

        viewModel.btnZones.observe(this) { zones ->
            if (zones.isNotEmpty()) {
                Log.d(TAG, "Setting up adapter with ${zones.size} zones")
                btnZoneAdapter.updateZones(zones)
            } else {
                Log.d(TAG, "No zones to display")
            }
        }
    }
    private fun onBtnZoneClicked(zone: ObjBtnZone) {
        Log.d(TAG, "Zone clicked: ${zone.id}")

        val intent = Intent(this, CtrlZoneActivity::class.java).apply {
            putExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_EXTRA_ZONE_ID, zone.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        ctrlZoneLauncher.launch(intent)
    }

    private fun updateZoneNotes(zoneId: Int, elements: List<ObjElement>) {
        Log.d(TAG, "updateZoneNotes: Updating notes for zone $zoneId")

        try {
            // Délégation au ViewModel qui gère maintenant toute la logique
            viewModel.updateZoneNote(zoneId, elements)

            // La mise à jour de entrySelected est maintenant gérée par l'observateur de residenceData
            viewModel.residenceData.value?.let { updatedEntry ->
                entrySelected = updatedEntry
                // La sauvegarde est maintenant gérée dans le ViewModel
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateZoneNotes: Error updating zone notes", e)
            showErrorMessage("Erreur lors de la mise à jour des notes de zone")
        }
    }



    private fun showErrorMessage(message: String) {
        UiUtils.showErrorSnackbar(
            binding.root,
            message,
            actionText = "OK",
            action = { /* Rien à faire ici */ }
        )
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            progressDialog?.dismiss()
            progressDialog = UiUtils.showProgressDialog(
                this,
                "Traitement en cours...",
                cancelable = false
            )
        } else {
            progressDialog?.dismiss()
            progressDialog = null
        }

        binding.grilleCtrlActivityPrevBtn.isEnabled = !show
        binding.grilleCtrlActivityEndBtn.isEnabled = !show
    }
    private fun showAlert(message: String, title: String? = null) {
        UiUtils.showAlert(this, message, title)
    }




    private fun navigateToPrevScreen() {
        Log.d(TAG, "Navigating to ConfigCtrlActivity")
        val intent = Intent(this, ConfigCtrlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToNextScreen() {
        Log.d(TAG, "Navigating to FinishCtrlActivity")
        val intent = Intent(this, FinishCtrlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
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