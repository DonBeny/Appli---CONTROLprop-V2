package org.orgaprop.controlprop.ui.selectentry

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.orgaprop.controlprop.databinding.ActivitySelectEntryBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.selectlist.SelectListActivity
import org.orgaprop.controlprop.viewmodels.SelectEntryViewModel

class SelectEntryActivity : BaseActivity() {

    private val TAG = "SelectEntryActivity"

    private lateinit var binding: ActivitySelectEntryBinding
    private val viewModel: SelectEntryViewModel by viewModels()

    private val selectionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val selectedItem = data?.getSerializableExtra(SelectListActivity.SELECT_LIST_LIST) as? SelectItem
            selectedItem?.let {
                viewModel.handleSelectedItem(it)
            }
        }
    }

    companion object {
        private const val SELECT_LIST_REQUEST_CODE = 1001
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

        val userData = getData("userData") as? JSONObject
        val idMbr = userData?.optInt("idMbr", -1) ?: -1
        val adrMac = userData?.optString("adrMac", "") ?: ""

        viewModel.setUserCredentials(idMbr, adrMac)

        // Désactiver la case à cocher "Contrat" si userData.hasContrat est false
        val hasContrat = userData?.optBoolean("hasContrat", true) ?: true
        binding.selectEntryContraChk.isEnabled = hasContrat

        // Si hasContrat est false, décocher la case par défaut
        if (!hasContrat) {
            binding.selectEntryContraChk.isChecked = false
        }

        val selectedResidence = viewModel.selectedResidence.value
        binding.selectEntryNextBtn.isEnabled = !selectedResidence.isNullOrEmpty()
    }

    override fun setupComponents() {
        setupListeners()
        observeViewModel()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigateToMainActivity()
    }


    private fun setupListeners() {
        // Bouton Précédent
        binding.selectEntryPrevBtn.setOnClickListener {
            navigateToMainActivity()
        }

        // Bouton Déconnexion
        binding.selectEntryDecoBtn.setOnClickListener {
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
        binding.selectEntryAgcBlk.setOnClickListener {
            openSelectionList(SelectListActivity.SELECT_LIST_TYPE_AGC)
        }
        binding.selectEntryAgcSpinner.setOnClickListener {
            openSelectionList(SelectListActivity.SELECT_LIST_TYPE_AGC)
        }

        // Bloc Groupement
        binding.selectEntryGrpBlk.setOnClickListener {
            openSelectionList(SelectListActivity.SELECT_LIST_TYPE_GRP)
        }
        binding.selectEntryGrpSpinner.setOnClickListener {
            openSelectionList(SelectListActivity.SELECT_LIST_TYPE_GRP)
        }

        // Bloc Résidence
        binding.selectEntryRsdBlk.setOnClickListener {
            openSelectionList(SelectListActivity.SELECT_LIST_TYPE_RSD)
        }
        binding.selectEntryRsdSpinner.setOnClickListener {
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
            viewModel.onNextButtonClicked()
        }
    }

    private fun observeViewModel() {
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
                    // Afficher un message d'erreur si parentId n'est pas valide
                    viewModel.setErrorMessage("Veuillez d'abord sélectionner une agence (pour GRP) ou un groupement (pour RSD)")
                    return
                }

                // Lancer SelectListActivity avec le type et le parentId
                val intent = Intent(this, SelectListActivity::class.java).apply {
                    putExtra(SelectListActivity.SELECT_LIST_TYPE, type)
                    putExtra(SelectListActivity.SELECT_LIST_ID, parentId)
                }
                selectionLauncher.launch(intent)
            }
            else -> {
                // Pour les autres types (AGC, SEARCH), pas besoin de parentId
                val intent = Intent(this, SelectListActivity::class.java).apply {
                    putExtra(SelectListActivity.SELECT_LIST_TYPE, type)
                }
                selectionLauncher.launch(intent)
            }
        }
    }

    private fun navigateToMainActivity() {
        finish()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateToMainActivity()
        }
    }

}
