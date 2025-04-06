package org.orgaprop.controlprop.ui.grille

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.window.OnBackInvokedDispatcher

import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import org.json.JSONObject

import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orgaprop.controlprop.R

import org.orgaprop.controlprop.databinding.ActivityGrilleCtrlBinding
import org.orgaprop.controlprop.managers.GrilleCtrlManager
import org.orgaprop.controlprop.models.ObjAgent
import org.orgaprop.controlprop.models.ObjBtnZone
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.toObjAgentList
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.HomeActivity
import org.orgaprop.controlprop.ui.config.ConfigCtrlActivity
import org.orgaprop.controlprop.ui.grille.adapters.AgentSpinnerAdapter
import org.orgaprop.controlprop.ui.grille.adapters.BtnZoneAdapter
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.ui.sendMail.SendMailActivity
import org.orgaprop.controlprop.viewmodels.GrilleCtrlViewModel

class GrilleCtrlActivity : BaseActivity() {

    private val TAG = "GrilleCtrlActivity"

    private lateinit var binding: ActivityGrilleCtrlBinding
    private val viewModel: GrilleCtrlViewModel by viewModel()
    private lateinit var btnZoneAdapter: BtnZoneAdapter
    private val grilleCtrlManager: GrilleCtrlManager by inject()

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
                        val typeToken = object : TypeToken<List<ObjElement>>() {}.type
                        val controlledElements = Gson().fromJson<List<ObjElement>>(elements, typeToken)

                        Log.d(TAG, "Parsed controlled elements: $controlledElements")

                        updateZoneNotes(zoneId, controlledElements)
                    }
                }
            }
        }
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
            if (entrySaved == null) {
                Log.d(TAG, "setupComponents: entrySaved is null")
            }
            if (typeCtrlSaved == null) {
                Log.d(TAG, "setupComponents: typeCtrlSaved is null")
            }
            if (confCtrlSaved == null) {
                Log.d(TAG, "setupComponents: confCtrlSaved is null")
            }
            if (listAgentsSaved == null) {
                Log.d(TAG, "setupComponents: listAgentsSaved is null")
            }
            if (listPrestatesSaved == null) {
                Log.d(TAG, "setupComponents: listPrestatesSaved is null")
            }
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

            Log.d(TAG, "setupComponents: confCtrl: $confCtrl")

            loadSavedGrilleData()

            viewModel.setEntrySelected(entrySelected)
            viewModel.refreshAllNotes()

            Log.d(TAG, "setupComponents: idRsd: $idRsd")
            //Log.d(TAG, "setupComponents: listAgentsSaved: $listAgentsSaved")

            listAgents = listAgentsSaved.toObjAgentList()

            //Log.d(TAG, "setupComponents: listAgents: $listAgents")
            //Log.d(TAG, "setupComponents: listPrestatesSaved: $listPrestatesSaved")

            listPrestates = listPrestatesSaved.toObjAgentList()

            //Log.d(TAG, "setupComponents: listPrestates: $listPrestates")

            setupZoneRecyclerView()

            if (!isInitialDataSetupDone) {
                initializeUI()
                isInitialDataSetupDone = true
            }

            setupObservers()
            setupListeners()
        }
    }
    override fun setupObservers() {
        val myTag = "$TAG::setupObservers"

        Log.d(myTag, "setupObservers: Setting up observers")

        viewModel.noteCtrl.observe(this) { note ->
            binding.grilleCtrlActivityNoteCtrlTxt.text = note

            Log.d(myTag, "noteCtrl: $note")

            val noteValue = if (note == "S O") -1 else note.removeSuffix("%").toIntOrNull() ?: -1

            Log.d(myTag, "noteValue: $noteValue")

            val backgroundRes = when {
                noteValue < 0 -> R.drawable.ctrl_note_grey
                noteValue < user.limits.down -> R.drawable.ctrl_note_red
                noteValue >= user.limits.top -> R.drawable.ctrl_note_green
                else -> R.drawable.ctrl_note_orange
            }

            binding.grilleCtrlActivityNoteCtrlTxt.setBackgroundResource(backgroundRes)
        }

        viewModel.navigateToNext.observe(this) { success ->
            if (success) navigateToNextScreen()
        }
    }
    override fun setupListeners() {
        Log.d(TAG, "setupListeners: Setting up listeners")

        binding.grilleCtrlActivityPrevBtn.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
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

                    entrySelected.prop!!.ctrl.prestate = prestateValue

                    setEntrySelected(entrySelected)

                    Log.d(TAG, "Selected agent: ${item.name}")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Ne rien faire
            }
        }

        binding.grilleCtrlActivityEndBtn.setOnClickListener {
            Log.d(TAG, "End button clicked")
            viewModel.finishCtrl()
        }
    }



    private fun loadSavedGrilleData() {
        Log.d(TAG, "loadSavedGrilleData: Loading saved grille data")

        entrySelected = grilleCtrlManager.loadResidenceData(
            entrySelected,
            typeCtrl,
            confCtrl
        )

        grilleCtrlManager.saveControlProgress(entrySelected)

        Log.d(TAG, "loadSavedGrilleData: entrySelected: $entrySelected")
        Log.d(TAG, "loadSavedGrilleData: typeCtrl: ${entrySelected.type}")
        Log.d(TAG, "loadSavedGrilleData: confCtrl: ${entrySelected.prop?.ctrl?.conf}")
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

        binding.grilleCtrlActivityListZoneLyt.layoutManager = GridLayoutManager(this, 4)

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
        viewModel.updateZoneNote(zoneId, elements)
        entrySelected = viewModel.residenceData.value ?: entrySelected
        addPendingControl(entrySelected)
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
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        //startActivity(intent)
        //finish()
        Log.d(TAG, "Navigating to HomeActivity")
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
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



    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: Back Pressed")
            navigateToPrevScreen()
        }
    }

}