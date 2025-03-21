package org.orgaprop.controlprop.ui.selectentry

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.launch

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivitySelectEntryBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.config.TypeCtrlActivity
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.ui.selectlist.SelectListActivity
import org.orgaprop.controlprop.utils.extentions.getParcelableCompat
import org.orgaprop.controlprop.viewmodels.SelectEntryViewModel

class SelectEntryActivity : BaseActivity() {

    private val TAG = "SelectEntryActivity"

    private lateinit var binding: ActivitySelectEntryBinding
    private val viewModel: SelectEntryViewModel by viewModel()

    private val selectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d(TAG, "selectionLauncher: Result code: ${result.resultCode}")

        if (result.resultCode == RESULT_OK) {
            Log.d(TAG, "selectionLauncher: Result data: ${result.data}")

            val data = result.data
            val selectedItem = data?.extras?.getParcelableCompat<SelectItem>(SelectListActivity.SELECT_LIST_RETURN)

            Log.d(TAG, "selectionLauncher: Selected item: $selectedItem")

            selectedItem?.let {
                viewModel.handleSelectedItem(it)
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initializeComponents()
        setupComponents()
    }



    override fun initializeComponents() {
        binding = ActivitySelectEntryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()

        if( userData == null ) {
            Log.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            Log.d(TAG, "initializeComponents: UserData is not null")
        }

        val idMbr = userData.idMbr// (userData as JSONObject).optInt("idMbr", -1)
        val adrMac = userData.adrMac// (userData as JSONObject).optString("adrMac", "")

        Log.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        viewModel.setUserCredentials(idMbr, adrMac)

        val hasContrat = userData.hasContrat// (userData as JSONObject).optBoolean("hasContrat", true) ?: true
        binding.selectEntryContraChk.isEnabled = hasContrat

        Log.d(TAG, "initializeComponents: hasContrat: $hasContrat")

        // Si hasContrat est false, décocher la case par défaut
        if (!hasContrat) {
            binding.selectEntryContraChk.isChecked = false
        }

        val selectedResidence = viewModel.selectedResidence.value
        binding.selectEntryNextBtn.isEnabled = !selectedResidence.isNullOrEmpty()
    }
    override fun setupComponents() {
        setupListeners()
        setupObservers()
    }
    override fun setupListeners() {
        // Bouton Précédent
        binding.selectEntryPrevBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: PrevBtn pressed")
            navigateToMainActivity()
        }

        // Bouton Déconnexion
        binding.selectEntryDecoBtn.setOnClickListener {
            Log.d(TAG, "setupListeners: DecoBtn pressed")
            viewModel.onLogoutButtonClicked()
        }

        // Bouton Recherche (icône loupe)
        binding.selectEntrySearchImg.setOnClickListener {
            val searchQuery = binding.selectEntrySearchInput.text.toString()
            viewModel.onSearchButtonClicked(searchQuery)
        }

        // Gestion de la touche "Entrée" du clavier
        binding.selectEntrySearchInput.setOnEditorActionListener { _, _, _ ->
            val searchQuery = binding.selectEntrySearchInput.text.toString()
            viewModel.onSearchButtonClicked(searchQuery)
            true
        }

        // Bloc Agence
        binding.selectEntryAgcLyt.setOnClickListener {
            openSelectionList(SelectListActivity.SELECT_LIST_TYPE_AGC)
        }

        // Bloc Groupement
        binding.selectEntryGrpLyt.setOnClickListener {
            openSelectionList(SelectListActivity.SELECT_LIST_TYPE_GRP)
        }

        // Bloc Résidence
        binding.selectEntryRsdLyt.setOnClickListener {
            openSelectionList(SelectListActivity.SELECT_LIST_TYPE_RSD)
        }

        // Case à cocher Proximité
        binding.selectEntryProxiChk.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onProximityCheckChanged(isChecked)
        }
        // Case à cocher Contrat
        binding.selectEntryContraChk.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onContractCheckChanged(isChecked)
        }

        // Bouton Suivant
        binding.selectEntryNextBtn.setOnClickListener {
            navigateToNextScreen()
        }
    }
    override fun setupObservers() {
        // Observers pour les sélections (Agence, Groupement, Résidence)
        viewModel.selectedAgence.observe(this, Observer { agence ->
            binding.selectEntryAgcSpinner.text = agence
        })
        viewModel.selectedGroupement.observe(this, Observer { groupement ->
            binding.selectEntryGrpSpinner.text = groupement
        })
        viewModel.selectedResidence.observe(this, Observer { residence ->
            binding.selectEntryRsdSpinner.text = residence

            binding.selectEntryNextBtn.isEnabled = !residence.isNullOrEmpty()
        })

        // Observers pour les cases à cocher
        viewModel.isProximityChecked.observe(this, Observer { isChecked ->
            binding.selectEntryProxiChk.isChecked = isChecked
        })
        viewModel.isContractChecked.observe(this, Observer { isChecked ->
            binding.selectEntryContraChk.isChecked = isChecked
        })

        // Observer pour fermer l'application
        viewModel.navigateToCloseApp.observe(this, Observer { shouldClose ->
            if (shouldClose) {
                clearUserData()
                finishAffinity() // Fermer l'application
            }
        })

        // Observer pour les erreurs
        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show(    )
        })

        // Observer pour la navigation vers la recherche
        lifecycleScope.launch {
            viewModel.navigateToSearch.collect { query ->
                val intent = Intent(this@SelectEntryActivity, SelectListActivity::class.java).apply {
                    putExtra(SelectListActivity.SELECT_LIST_TYPE, SelectListActivity.SELECT_LIST_TYPE_SEARCH)
                    putExtra(SelectListActivity.SELECT_LIST_TXT, query)
                }
                selectionLauncher.launch(intent)
            }
        }
    }



    private fun openSelectionList(type: String) {
        when (type) {
            SelectListActivity.SELECT_LIST_TYPE_GRP, SelectListActivity.SELECT_LIST_TYPE_RSD -> {
                // Vérifier que parentId est valide
                val parentId = when (type) {
                    SelectListActivity.SELECT_LIST_TYPE_GRP -> viewModel.selectedAgenceId.value
                    SelectListActivity.SELECT_LIST_TYPE_RSD -> viewModel.selectedGroupementId.value
                    else -> null
                }

                if (parentId == null) {
                    viewModel.setErrorMessage("Veuillez d'abord sélectionner une agence (pour GRP) ou un groupement (pour RSD)")
                    return
                }

                val intent = Intent(this, SelectListActivity::class.java).apply {
                    putExtra(SelectListActivity.SELECT_LIST_TYPE, type)
                    putExtra(SelectListActivity.SELECT_LIST_ID, parentId)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                selectionLauncher.launch(intent)
            }
            else -> {
                val intent = Intent(this, SelectListActivity::class.java).apply {
                    putExtra(SelectListActivity.SELECT_LIST_TYPE, type)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                selectionLauncher.launch(intent)
            }
        }
    }
    private fun navigateToMainActivity() {
        Log.d(TAG, "navigateToMainActivity: Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
        finish()
    }
    private fun navigateToNextScreen() {
        Log.d(TAG, "navigateToNextScreen: Navigating to Next Screen")

        val selectedEntry = getEntrySelected()
        val entryList = getEntryList()
        val proxi = viewModel.isProximityChecked.value ?: false
        val contract = viewModel.isContractChecked.value ?: false
        val b = proxi || contract

        if (selectedEntry != null && entryList != null && b) {
            setProxi(proxi)
            setContract(contract)

            val intent = Intent(this, TypeCtrlActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            startActivity(intent)
        } else {
            Log.e(TAG, "navigateToNextScreen: Données manquantes (selectedEntry ou entryList ou Chekbox)")

            if( !b ) {
                Toast.makeText(this, "Veuillez sélectionner au moins une option (Proximité ou Contrat)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Veuillez sélectionner une entrée", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: Back Pressed")
            navigateToMainActivity()
        }
    }

}
