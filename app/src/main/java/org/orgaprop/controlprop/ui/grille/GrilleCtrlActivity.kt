package org.orgaprop.controlprop.ui.grille

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.window.OnBackInvokedDispatcher

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager

import org.json.JSONArray
import org.json.JSONObject

import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.databinding.ActivityGrilleCtrlBinding
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.exceptions.ErrorCodes
import org.orgaprop.controlprop.managers.GrilleCtrlManager
import org.orgaprop.controlprop.models.ObjAgent
import org.orgaprop.controlprop.models.ObjBtnZone
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.toObjAgentList
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.config.ConfigCtrlActivity
import org.orgaprop.controlprop.ui.finish.FinishCtrlActivity
import org.orgaprop.controlprop.ui.grille.adapters.AgentSpinnerAdapter
import org.orgaprop.controlprop.ui.grille.adapters.BtnZoneAdapter
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.models.ObjGrilleElement
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.ui.sendMail.SendMailActivity
import org.orgaprop.controlprop.utils.LogUtils
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
                    LogUtils.d(TAG, "Received zoneId: $zoneId")

                    val controlledElements = intent.getStringExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_EXTRA_CONTROLLED_ELEMENTS)

                    LogUtils.json(TAG, "Received controlled elements:", controlledElements)

                    controlledElements?.let { elements ->
                        try {
                            val typeToken = object : TypeToken<List<ObjGrilleElement>>() {}.type
                            val newControlledElements = Gson().fromJson<List<ObjGrilleElement>>(elements, typeToken)

                            LogUtils.json(TAG, "Parsed controlled elements:", newControlledElements)

                            updateZoneNotes(zoneId, newControlledElements)
                        } catch (e: Exception) {
                            LogUtils.e(TAG, "Error parsing controlled elements", e)
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
            LogUtils.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UiUtils.dismissCurrentDialog()
        progressDialog?.dismiss()
        progressDialog = null
    }



    override fun initializeComponents() {
        binding = ActivityGrilleCtrlBinding.inflate(layoutInflater)
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
            LogUtils.e(TAG, "setupComponents: Missing required data, navigating to SelectEntryActivity")

            if (entrySaved == null) LogUtils.e(TAG, "setupComponents: entrySaved is null")
            if (typeCtrlSaved == null) LogUtils.e(TAG, "setupComponents: typeCtrlSaved is null")
            if (confCtrlSaved == null) LogUtils.e(TAG, "setupComponents: confCtrlSaved is null")
            if (listAgentsSaved == null) LogUtils.e(TAG, "setupComponents: listAgentsSaved is null")
            if (listPrestatesSaved == null) LogUtils.e(TAG, "setupComponents: listPrestatesSaved is null")

            navigateToSelectEntryActivity()
            return
        } else {
            LogUtils.d(TAG, "setupComponents: All is ok")

            entrySelected = entrySaved
            idRsd = entrySaved.id
            confCtrl = confCtrlSaved
            typeCtrl = typeCtrlSaved
            withProxi = proxiSaved
            withContract = contractSaved

            try {
                loadSavedGrilleData()

                LogUtils.json(TAG, "setupComponents: Entry selected ID:", entrySelected)

                viewModel.setEntrySelected(entrySelected)

                listAgents = listAgentsSaved.toObjAgentList()
                listPrestates = listPrestatesSaved.toObjAgentList()

                setupZoneRecyclerView()

                if (!isInitialDataSetupDone) {
                    initializeUI()
                    isInitialDataSetupDone = true
                }

                setupObservers()
                setupListeners()
            } catch (e: BaseException) {
                LogUtils.e(TAG, "setupComponents: Error loading saved grille data", e)
                showErrorMessage(e.message ?: ErrorCodes.getMessageForCode(e.code))
            } catch (e: Exception) {
                LogUtils.e(TAG, "setupComponents: Unexpected error", e)
                showErrorMessage("Une erreur inattendue s'est produite lors du chargement des données")
            }
        }
    }
    override fun setupObservers() {
        LogUtils.d(TAG, "setupObservers: Setting up observers")

        viewModel.noteCtrl.observe(this) { note ->
            binding.grilleCtrlActivityNoteCtrlTxt.text = note

            LogUtils.d(TAG, "noteCtrl: $note")

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
                if (code == ErrorCodes.NETWORK_ERROR || message.contains("connexion", ignoreCase = true)) {
                    UiUtils.showConfirmationDialog(
                        this,
                        "Problème de connexion",
                        "Votre contrôle est sauvegardé. Pour sa synchronisation, rétablissez une connexion ou reconnectez-vous à un réseau.",
                        "Continuer",
                        positiveAction = {
                            navigateToNextScreen()
                        },
                        negativeAction = null
                    )
                } else {
                    showErrorMessage(message)
                }
                viewModel.clearError()
            }
        }
    }
    override fun setupListeners() {
        LogUtils.d(TAG, "setupListeners: Setting up listeners")

        binding.grilleCtrlActivityPrevBtn.setOnClickListener {
            navigateToPrevScreen()
        }

        binding.grilleCtrlActivityTechBtn.setOnClickListener {
            val intent = Intent(this, SendMailActivity::class.java).apply {
                putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TYPE, SendMailActivity.SEND_MAIL_ACTIVITY_PROBLEM_TECH)
                putExtra(SendMailActivity.SEND_MAIL_ACTIVITY_TAG_RSD_ID, idRsd)
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
                        LogUtils.e(TAG, "prop or ctrl is null, cannot update prestate")
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

                        try {
                            grilleCtrlManager.saveControlProgress(updatedEntry)
                        } catch (e: Exception) {
                            LogUtils.e(TAG, "Error saving control after agent selection", e)
                        }

                        LogUtils.d(TAG, "Selected agent: ${item.name} with ID ${item.id}")
                    } catch (e: Exception) {
                        LogUtils.e(TAG, "Error updating prestate", e)
                        showErrorMessage("Erreur lors de la mise à jour de l'agent")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ne rien faire
            }
        }

        binding.grilleCtrlActivityEndBtn.setOnClickListener {
            LogUtils.d(TAG, "End button clicked")

            val hasControlledZones = viewModel.residenceData.value?.let { entry ->
                val grille = entry.prop?.ctrl?.grille ?: "[]"
                return@let grille != "[]" && JSONArray(grille).length() > 0
            } ?: false

            if (!hasControlledZones) {
                navigateToSelectEntryActivity()
            } else {
                UiUtils.showConfirmationDialog(
                    this,
                    "Voulez-vous finaliser ce contrôle ?",
                    "Confirmation",
                    "Oui",
                    "Non",
                    { viewModel.finishCtrl() },
                    { UiUtils.dismissCurrentDialog() }
                )
            }
        }
    }



    private fun loadSavedGrilleData() {
        LogUtils.d(TAG, "loadSavedGrilleData: Loading saved grille data")

        try {
            entrySelected = grilleCtrlManager.loadResidenceData(
                entrySelected,
                typeCtrl,
                confCtrl
            )

            //grilleCtrlManager.saveControlProgress(entrySelected)

            LogUtils.json(TAG, "loadSavedGrilleData: Data loaded for entry", entrySelected)
        } catch (e: BaseException) {
            LogUtils.e(TAG, "loadSavedGrilleData: Error with code ${e.code}", e)
            throw e
        } catch (e: Exception) {
            LogUtils.e(TAG, "loadSavedGrilleData: Unexpected error", e)
            throw BaseException(ErrorCodes.DATA_NOT_FOUND, "Erreur lors du chargement des données de la grille", e)
        }
    }
    private fun initializeUI() {
        LogUtils.d(TAG, "initializeUI: Initializing UI")

        viewModel.residenceData.value?.let { residence ->
            val entry = "Entrée ${residence.entry}"

            LogUtils.json(TAG, "initializeUI: residence:", residence)

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
        LogUtils.d(TAG, "setupSpinner: Setting up spinner")

        if (isSpinnerSetupDone) return

        LogUtils.json(TAG, "setupSpinner: listAgents:", listAgents)
        LogUtils.json(TAG, "setupSpinner: listPrestates:", listPrestates)

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

        LogUtils.d(TAG, "setupSpinner: Spinner setup done")

        isSpinnerSetupDone = true
    }
    private fun findAgentPositionById(agentId: Int, agents: List<ObjAgent>, prestates: List<ObjAgent>): Int {
        LogUtils.d(TAG, "findAgentPositionById: agentId: $agentId")

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
        LogUtils.d(TAG, "setupZoneRecyclerView: Setting up zone recycler view")

        binding.grilleCtrlActivityListZoneLyt.layoutManager = GridLayoutManager(this, 3)

        val minLimit = user.limits.down
        val maxLimit = user.limits.top

        btnZoneAdapter = BtnZoneAdapter(emptyList(), minLimit, maxLimit) { zone ->
            onBtnZoneClicked(zone)
        }
        binding.grilleCtrlActivityListZoneLyt.adapter = btnZoneAdapter

        viewModel.btnZones.observe(this) { zones ->
            if (zones.isNotEmpty()) {
                LogUtils.json(TAG, "Setting up adapter with ${zones.size} zones", zones)
                btnZoneAdapter.updateZones(zones)
            } else {
                LogUtils.d(TAG, "No zones to display")
            }
        }
    }
    private fun onBtnZoneClicked(zone: ObjBtnZone) {
        LogUtils.json(TAG, "Zone clicked:", zone)

        val intent = Intent(this, CtrlZoneActivity::class.java).apply {
            putExtra(CtrlZoneActivity.CTRL_ZONE_ACTIVITY_EXTRA_ZONE_ID, zone.id)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        ctrlZoneLauncher.launch(intent)
    }

    private fun updateZoneNotes(zoneId: Int, elements: List<ObjGrilleElement>) {
        LogUtils.json(TAG, "updateZoneNotes: Updating notes for zone $zoneId", elements)

        try {
            if (elements.isEmpty() || elements.all { element -> element.critters.all { critter -> critter.note == 0 } }) {
                viewModel.removeZone(zoneId)
            } else {
                viewModel.updateZoneNote(zoneId, elements)
            }

            viewModel.residenceData.value?.let { updatedEntry ->
                entrySelected = updatedEntry
                setEntrySelected(updatedEntry)
            }
        } catch (e: Exception) {
            LogUtils.e(TAG, "updateZoneNotes: Error updating zone notes", e)
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



    private fun navigateToPrevScreen() {
        LogUtils.d(TAG, "Navigating to ConfigCtrlActivity")
        val intent = Intent(this, ConfigCtrlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToNextScreen() {
        LogUtils.d(TAG, "Navigating to FinishCtrlActivity")
        val intent = Intent(this, FinishCtrlActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
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