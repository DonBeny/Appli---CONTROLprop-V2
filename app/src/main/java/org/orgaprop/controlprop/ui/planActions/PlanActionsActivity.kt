package org.orgaprop.controlprop.ui.planActions

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import android.window.OnBackInvokedDispatcher
import androidx.lifecycle.Observer

import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orgaprop.controlprop.databinding.ActivityPlanActionsBinding
import org.orgaprop.controlprop.models.ObjPlanActions

import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.ui.selectEntry.SelectEntryActivity
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.viewmodels.PlanActionsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Activity permettant de gérer les plans d'actions.
 *
 * Flux principal:
 * 1. Au chargement, récupération du plan en cours s'il existe
 * 2. Si un plan existe (mode EDITION):
 *    - Les champs sont remplis avec les données du plan
 *    - L'utilisateur peut uniquement lever le plan
 * 3. Si aucun plan n'existe (mode CREATION):
 *    - L'utilisateur peut remplir les champs et enregistrer un nouveau plan
 *    - L'utilisateur peut également ajouter un rappel au calendrier
 */
class PlanActionsActivity : BaseActivity() {

    private val TAG = "PlanActionsActivity"

    private lateinit var binding: ActivityPlanActionsBinding
    private val viewModel: PlanActionsViewModel by viewModel()
    private var idPlanActions: Int = -1



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT) {
            LogUtils.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }



    override fun initializeComponents() {
        binding = ActivityPlanActionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()
        val entrySelect = getEntrySelected()

        if( userData == null ) {
            LogUtils.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else if (entrySelect == null) {
            LogUtils.d(TAG, "initializeComponents: EntrySelect is null")
            navigateToSelectEntryActivity()
            return
        } else {
            LogUtils.d(TAG, "initializeComponents: UserData is not null")
        }

        val idMbr = userData.idMbr
        val adrMac = userData.adrMac

        LogUtils.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        viewModel.setUserCredentials(idMbr, adrMac, entrySelect)
        viewModel.setTypeCtrl(getTypeCtrl())
        viewModel.getPlanActions()
    }
    override fun setupComponents() {
        setupObservers()
        setupListeners()
    }
    override fun setupObservers() {
        viewModel.state.observe(this, Observer { state ->
            when (state) {
                is PlanActionsViewModel.PlanActionState.Loading -> {
                    showLoading(true)
                }
                is PlanActionsViewModel.PlanActionState.PlanLoaded -> {
                    showLoading(false)
                    handlePlanLoaded(state.plan)
                }
                is PlanActionsViewModel.PlanActionState.Success -> {
                    showLoading(false)
                    showSuccessMessage(state.message)
                }
                is PlanActionsViewModel.PlanActionState.Error -> {
                    showLoading(false)
                    showErrorMessage(state.message)
                }
            }
        })

        viewModel.mode.observe(this, Observer { mode ->
            updateUIBasedOnMode(mode)
        })

        viewModel.event.observe(this, Observer { event ->
            event?.let {
                when (it) {
                    is PlanActionsViewModel.PlanActionEvent.NavigateBack -> {
                        navigateToPrevScreen()
                    }
                    is PlanActionsViewModel.PlanActionEvent.NavigateTo -> {
                        when (it.destination) {
                            PlanActionsViewModel.DESTINATION_SELECT_ENTRY -> {
                                navigateToSelectEntryActivity()
                            }
                            PlanActionsViewModel.DESTINATION_BACK -> {
                                navigateToPrevScreen()
                            }
                        }
                    }
                    is PlanActionsViewModel.PlanActionEvent.OpenCalendar -> {
                        openCalendarWithReminder()
                        navigateToPrevScreen()
                    }
                    is PlanActionsViewModel.PlanActionEvent.ShowMessage -> {
                        UiUtils.showInfoSnackbar(binding.root, it.message)
                    }
                    is PlanActionsViewModel.PlanActionEvent.ResetForm -> {
                        resetForm()
                    }
                    is PlanActionsViewModel.PlanActionEvent.StayOnScreen -> {}
                }
                viewModel.eventHandled()
            }
        })

        viewModel.currentPlan.observe(this, Observer { plan ->
            plan?.let {
                binding.addPlanActionDateTxt.setText(it.limit)
                binding.addPlanActionPlanTxt.setText(it.txt)
            }
        })
    }
    override fun setupListeners() {
        binding.addPlanActionPrevBtn.setOnClickListener {
            navigateToPrevScreen()
        }

        binding.addPlanActionSaveBtn.setOnClickListener {
            val date = binding.addPlanActionDateTxt.text.toString()
            val planText = binding.addPlanActionPlanTxt.text.toString()

            if (viewModel.mode.value == PlanActionsViewModel.PlanActionMode.CREATION) {
                if (date.isNotEmpty() && planText.isNotEmpty()) {
                    viewModel.savePlanAction(-1, date, planText)
                } else {
                    showErrorMessage("Veuillez remplir tous les champs")
                }
            } else {
                showErrorMessage("Impossible de modifier un plan d'actions déjà enregistré")
            }
        }

        binding.addPlanActionAlertBtn.setOnClickListener {
            val date = binding.addPlanActionDateTxt.text.toString()
            val planText = binding.addPlanActionPlanTxt.text.toString()

            if (viewModel.mode.value == PlanActionsViewModel.PlanActionMode.CREATION) {
                if (date.isNotEmpty() && planText.isNotEmpty()) {
                    viewModel.savePlanActionAndOpenCalendar(-1, date, planText)
                } else {
                    showErrorMessage("Veuillez remplir tous les champs")
                }
            } else {
                showErrorMessage("Impossible de modifier un plan d'actions déjà enregistré")
            }
        }

        binding.addPlanActionValidBtn.setOnClickListener {
            val currentPlan = viewModel.currentPlan.value

            LogUtils.json(TAG, "currentPlan:", currentPlan)
            LogUtils.d(TAG, "currentPlan != null: ${currentPlan != null}")
            LogUtils.d(TAG, "currentPlan.id: ${currentPlan?.id}")
            LogUtils.d(TAG, "currentPlan.id > 0: ${(currentPlan?.id ?: -1) > 0}")

            if (currentPlan != null && currentPlan.id > 0) {
                viewModel.validatePlanAction(currentPlan.id)
            } else {
                showErrorMessage("Aucun plan d'actions à lever")
            }
        }

        binding.addPlanActionDateTxt.setOnClickListener {
            if (viewModel.mode.value == PlanActionsViewModel.PlanActionMode.CREATION) {
                showDatePickerDialog()
            } else {
                showErrorMessage("Impossible de modifier un plan d'actions déjà enregistré")
            }
        }
    }



    /**
     * Traite un plan d'actions chargé.
     */
    private fun handlePlanLoaded(plan: ObjPlanActions?) {
        if (plan == null) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(calendar.time)
            binding.addPlanActionDateTxt.setText(formattedDate)
            binding.addPlanActionPlanTxt.setText("")
        }
    }

    /**
     * Réinitialise le formulaire après une levée de plan.
     */
    private fun resetForm() {
        binding.addPlanActionDateTxt.text.clear()
        binding.addPlanActionPlanTxt.text.clear()

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH).format(calendar.time)

        binding.addPlanActionDateTxt.setText(formattedDate)
    }



    /**
     * Met à jour l'interface utilisateur en fonction du mode actuel.
     *
     * @param mode Mode d'affichage (création ou édition)
     */
    private fun updateUIBasedOnMode(mode: PlanActionsViewModel.PlanActionMode) {
        val creationMode = mode == PlanActionsViewModel.PlanActionMode.CREATION

        with(binding) {
            addPlanActionSaveBtn.isEnabled = creationMode
            addPlanActionSaveBtn.alpha = if (creationMode) 1.0f else 0.5f

            addPlanActionAlertBtn.isEnabled = creationMode
            addPlanActionAlertBtn.alpha = if (creationMode) 1.0f else 0.5f

            addPlanActionValidBtn.isEnabled = !creationMode
            addPlanActionValidBtn.alpha = if (creationMode) 0.5f else 1.0f

            addPlanActionDateTxt.isEnabled = creationMode
            addPlanActionPlanTxt.isEnabled = creationMode
        }
    }



    private fun parseDateToMillis(date: String): Long {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(date)?.time
                ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    private fun showDatePickerDialog() {
        val currentDate = binding.addPlanActionDateTxt.text.toString()
        val calendar = Calendar.getInstance()

        if (currentDate.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH)
                val date = dateFormat.parse(currentDate)
                if (date != null) {
                    calendar.time = date
                }
            } catch (e: Exception) {
                LogUtils.e(TAG, "Erreur lors du parsing de la date : ${e.message}")
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format(
                    Locale.FRENCH,
                    "%02d/%02d/%04d",
                    selectedDay,
                    selectedMonth + 1,
                    selectedYear
                )
                binding.addPlanActionDateTxt.setText(formattedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }



    private fun navigateToMainActivity() {
        LogUtils.d(TAG, "Navigating to MainActivity")
        Intent(this, LoginActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(it)
            finish()
        }
    }
    private fun navigateToSelectEntryActivity() {
        LogUtils.d(TAG, "Navigating to SelectEntryActivity")
        Intent(this, SelectEntryActivity::class.java).also {
            startActivity(it)
            finish()
        }
    }
    private fun navigateToPrevScreen() {
        LogUtils.d(TAG, "Navigating to activity précédente")

        setResult(RESULT_OK)
        finish()
    }

    /**
     * Ouvre le calendrier pour ajouter un rappel.
     */
    private fun openCalendarWithReminder() {
        val rsd = getEntrySelected()
        val date = binding.addPlanActionDateTxt.text.toString()
        val planText = binding.addPlanActionPlanTxt.text.toString()

        if (rsd != null && date.isNotEmpty() && planText.isNotEmpty()) {
            val txt = "${rsd.ref} -- ${rsd.name} -- ${rsd.address} ${rsd.city}"
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, "Échéance Plan d'actions")
                putExtra(CalendarContract.Events.DESCRIPTION, txt)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, parseDateToMillis(date))
            }
            startActivity(intent)
        }
    }



    /**
     * Affiche un message de succès sous forme de Snackbar.
     *
     * @param message Le message de succès à afficher
     */
    private fun showSuccessMessage(message: String) {
        UiUtils.showSuccessSnackbar(binding.root, message)
    }

    /**
     * Affiche un message d'erreur sous forme de Snackbar.
     *
     * @param message Le message d'erreur à afficher
     */
    private fun showErrorMessage(message: String) {
        UiUtils.showErrorSnackbar(binding.root, message)
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

        with(binding) {
            addPlanActionSaveBtn.isEnabled = !isLoading
            addPlanActionAlertBtn.isEnabled = !isLoading
            addPlanActionValidBtn.isEnabled = !isLoading
            addPlanActionPrevBtn.isEnabled = !isLoading
            addPlanActionDateTxt.isEnabled = !isLoading
            addPlanActionPlanTxt.isEnabled = !isLoading
        }
    }

}
