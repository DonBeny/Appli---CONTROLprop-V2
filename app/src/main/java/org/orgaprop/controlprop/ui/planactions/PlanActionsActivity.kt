package org.orgaprop.controlprop.ui.planactions

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.Observer

import org.koin.androidx.viewmodel.ext.android.viewModel
import org.orgaprop.controlprop.databinding.ActivityPlanActionsBinding

import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.ui.selectentry.SelectEntryActivity
import org.orgaprop.controlprop.viewmodels.PlanActionsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PlanActionsActivity : BaseActivity() {

    private val TAG = "PlanActionsActivity"

    private lateinit var binding: ActivityPlanActionsBinding
    private val viewModel: PlanActionsViewModel by viewModel()
    private var idPlanActions: Int = -1



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        initializeComponents()
        setupComponents()

        viewModel.getPlanActions()
    }



    override fun initializeComponents() {
        binding = ActivityPlanActionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userData = getUserData()
        val entrySelect = getEntrySelected()

        if( userData == null ) {
            Log.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else if (entrySelect == null) {
            Log.d(TAG, "initializeComponents: EntrySelect is null")
            navigateToSelectEntryActivity()
            return
        } else {
            Log.d(TAG, "initializeComponents: UserData is not null")
        }

        val idMbr = userData.idMbr
        val adrMac = userData.adrMac
        val idRsd = entrySelect.id

        Log.d(TAG, "initializeComponents: idMbr: $idMbr, adrMac: $adrMac")

        viewModel.setUserCredentials(idMbr, adrMac, idRsd)

        updateButtonStates()
    }
    override fun setupComponents() {
        setupObservers()
        setupListeners()
    }
    override fun setupObservers() {
        viewModel.planActions.observe(this, Observer { planActions ->
            if (planActions != null && planActions.id > 0) {
                idPlanActions = planActions.id
                binding.addPlanActionDateTxt.setText(planActions.limit)
                binding.addPlanActionPlanTxt.setText(planActions.txt)

                updateButtonStates()
            }
        })

        viewModel.savePlanActionResult.observe(this, Observer { success ->
            if (success) {
                navigateToPrevActivity()
            } else {
                showToast("Erreur lors de la sauvegarde du plan d'actions")
            }
        })

        viewModel.openCalendarEvent.observe(this, Observer { shouldOpenCalendar ->
            if (shouldOpenCalendar) {
                val rsd = getEntrySelected()
                val date = binding.addPlanActionDateTxt.text.toString()
                val planText = binding.addPlanActionPlanTxt.text.toString()

                if (rsd != null && date.isNotEmpty() && planText.isNotEmpty()) {
                    val txt = rsd.ref + " -- " + rsd.name + " -- " + rsd.address + " " + rsd.city
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, "Échéance Plan d'actions")
                        putExtra(CalendarContract.Events.DESCRIPTION, txt)
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, parseDateToMillis(date))
                    }
                    startActivity(intent)
                }
            }
        })

        viewModel.validatePlanActionResult.observe(this, Observer { success ->
            if (success) {
                binding.addPlanActionDateTxt.text.clear() // Vide le champ de la date
                binding.addPlanActionPlanTxt.text.clear() // Vide le champ du texte

                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val formattedDate = String.format(
                    Locale.FRENCH,
                    "%02d/%02d/%04d",
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.YEAR)
                )

                binding.addPlanActionDateTxt.setText(formattedDate)

                idPlanActions = -1
                updateButtonStates()
            } else {
                showToast("Erreur lors de la validation du plan d'actions")
            }
        })

        viewModel.errorMessage.observe(this, Observer { errorMessage ->
            showToast(errorMessage)
        })
    }
    override fun setupListeners() {
        binding.addPlanActionPrevBtn.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.addPlanActionSaveBtn.setOnClickListener {
            if (idPlanActions < 0) {
                val date = binding.addPlanActionDateTxt.text.toString()
                val planText = binding.addPlanActionPlanTxt.text.toString()

                if (date.isNotEmpty() && planText.isNotEmpty()) {
                    viewModel.savePlanAction(idPlanActions, date, planText)
                } else {
                    showToast("Veuillez remplir tous les champs")
                }
            }
        }

        binding.addPlanActionAlertBtn.setOnClickListener {
            if (idPlanActions < 0) {
                val rsd = getEntrySelected()
                val date = binding.addPlanActionDateTxt.text.toString()
                val planText = binding.addPlanActionPlanTxt.text.toString()

                if (rsd != null && date.isNotEmpty() && planText.isNotEmpty()) {
                    viewModel.savePlanActionAndOpenCalendar(idPlanActions, date, planText)
                } else {
                    showToast("Veuillez remplir tous les champs")
                }
            }
        }

        binding.addPlanActionValidBtn.setOnClickListener {
            if (idPlanActions > 0) {
                viewModel.validatePlanAction(idPlanActions)
            }
        }

        binding.addPlanActionDateTxt.setOnClickListener {
            if (idPlanActions < 0) {
                val calendar = Calendar.getInstance()
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
        }
    }



    private fun updateButtonStates() {
        if (idPlanActions < 0) {
            // Mode création : les boutons "Sauvegarder" et "Ajouter une alerte" sont actifs
            binding.addPlanActionSaveBtn.isEnabled = true
            binding.addPlanActionSaveBtn.alpha = 1.0f

            binding.addPlanActionAlertBtn.isEnabled = true
            binding.addPlanActionAlertBtn.alpha = 1.0f

            // Le bouton "Valider" est désactivé
            binding.addPlanActionValidBtn.isEnabled = false
            binding.addPlanActionValidBtn.alpha = 0.5f
        } else {
            // Mode édition : le bouton "Valider" est actif
            binding.addPlanActionValidBtn.isEnabled = true
            binding.addPlanActionValidBtn.alpha = 1.0f

            // Les boutons "Sauvegarder" et "Ajouter une alerte" sont désactivés
            binding.addPlanActionSaveBtn.isEnabled = false
            binding.addPlanActionSaveBtn.alpha = 0.5f

            binding.addPlanActionAlertBtn.isEnabled = false
            binding.addPlanActionAlertBtn.alpha = 0.5f
        }
    }

    private fun parseDateToMillis(date: String): Long {
        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return format.parse(date)?.time ?: System.currentTimeMillis()
    }



    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun navigateToSelectEntryActivity() {
        Log.d(TAG, "Navigating to SelectEntryActivity")
        val intent = Intent(this, SelectEntryActivity::class.java)
        startActivity(intent)
        finish()
    }
    private fun navigateToPrevActivity() {
        Log.d(TAG, "Navigating to activity précédente")

        setResult(RESULT_OK)
        finish()
    }



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: Back Pressed")
            navigateToPrevActivity()
        }
    }

}
