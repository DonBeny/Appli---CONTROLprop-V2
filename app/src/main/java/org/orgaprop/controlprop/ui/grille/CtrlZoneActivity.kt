package org.orgaprop.controlprop.ui.grille

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import org.json.JSONObject
import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.databinding.ActivityCtrlZoneBinding
import org.orgaprop.controlprop.models.ObjElement
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.viewmodels.CtrlZoneViewModel

class CtrlZoneActivity : BaseActivity() {

    private lateinit var binding: ActivityCtrlZoneBinding
    private lateinit var viewModel: CtrlZoneViewModel
    private lateinit var user: LoginData

    private var zoneId = -1
    private lateinit var entrySelected: SelectItem

    private var progressDialog: AlertDialog? = null

    companion object {
        const val TAG = "CtrlZoneActivity"

        const val CTRL_ZONE_ACTIVITY_EXTRA_ZONE_ID = "zoneId"

        const val CTRL_ZONE_ACTIVITY_EXTRA_ELEMENT_POSITION = "elementPosition"
        const val CTRL_ZONE_ACTIVITY_EXTRA_CRITTER_POSITION = "critterPosition"
        const val CTRL_ZONE_ACTIVITY_EXTRA_CONTROLLED_ELEMENTS = "commentText"

        const val CTRL_ZONE_ACTIVITY_ABORD = 1
        const val CTRL_ZONE_ACTIVITY_HALL = 2
        const val CTRL_ZONE_ACTIVITY_ASCENSEUR = 3
        const val CTRL_ZONE_ACTIVITY_ESCALIER = 4
        const val CTRL_ZONE_ACTIVITY_PALIER = 5
        const val CTRL_ZONE_ACTIVITY_OM = 6
        const val CTRL_ZONE_ACTIVITY_VELO = 7
        const val CTRL_ZONE_ACTIVITY_CAVE = 8
        const val CTRL_ZONE_ACTIVITY_PARK_INT = 9
        const val CTRL_ZONE_ACTIVITY_INT = 10
        const val CTRL_ZONE_ACTIVITY_PARK_EXT = 11
        const val CTRL_ZONE_ACTIVITY_EXT = 12
        const val CTRL_ZONE_ACTIVITY_OFFICE = 13
        const val CTRL_ZONE_ACTIVITY_REUNION = 14
        const val CTRL_ZONE_ACTIVITY_WASHING = 15
        const val CTRL_ZONE_ACTIVITY_ASCENSEUR2 = 16
        const val CTRL_ZONE_ACTIVITY_LOCAL_OM2 = 17
        const val CTRL_ZONE_ACTIVITY_LOCAL_POUSSETTE = 18
        const val CTRL_ZONE_ACTIVITY_PALIER_CENTRAL = 19

        private val zoneIcons = mapOf(
            CTRL_ZONE_ACTIVITY_ABORD to R.drawable.abords_acces_immeubles_2_blanc,
            CTRL_ZONE_ACTIVITY_HALL to R.drawable.hall_blanc,
            CTRL_ZONE_ACTIVITY_ASCENSEUR to R.drawable.ascenseur_blanc,
            CTRL_ZONE_ACTIVITY_ESCALIER to R.drawable.escalier_blanc,
            CTRL_ZONE_ACTIVITY_PALIER to R.drawable.paliers_coursives_blanc,
            CTRL_ZONE_ACTIVITY_OM to R.drawable.local_om_blanc,
            CTRL_ZONE_ACTIVITY_VELO to R.drawable.local_velo_blanc,
            CTRL_ZONE_ACTIVITY_CAVE to R.drawable.cave_blanc,
            CTRL_ZONE_ACTIVITY_PARK_INT to R.drawable.parking_sous_sol_blanc,
            CTRL_ZONE_ACTIVITY_INT to R.drawable.cour_interieure_blanc,
            CTRL_ZONE_ACTIVITY_PARK_EXT to R.drawable.parking_exterieur_blanc,
            CTRL_ZONE_ACTIVITY_EXT to R.drawable.espaces_exterieurs_blanc,
            CTRL_ZONE_ACTIVITY_OFFICE to R.drawable.icone_bureau_blanc,
            CTRL_ZONE_ACTIVITY_REUNION to R.drawable.salle_commune_blanc,
            CTRL_ZONE_ACTIVITY_WASHING to R.drawable.buanderie_blanc,
            CTRL_ZONE_ACTIVITY_ASCENSEUR2 to R.drawable.ascenseur_blanc,
            CTRL_ZONE_ACTIVITY_LOCAL_OM2 to R.drawable.local_om_blanc,
            CTRL_ZONE_ACTIVITY_LOCAL_POUSSETTE to R.drawable.local_poussette_blanc,
            CTRL_ZONE_ACTIVITY_PALIER_CENTRAL to R.drawable.paliers_coursives_blanc,
        )
    }

    private val addCommentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                val elementIndex = intent.getIntExtra(CTRL_ZONE_ACTIVITY_EXTRA_ELEMENT_POSITION, -1)
                val critterIndex = intent.getIntExtra(CTRL_ZONE_ACTIVITY_EXTRA_CRITTER_POSITION, -1)
                val commentText = intent.getStringExtra(AddCommentActivity.ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_TEXT) ?: ""
                val imagePath = intent.getStringExtra(AddCommentActivity.ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_IMAGE) ?: ""

                Log.d(TAG, "addCommentLauncher::elementIndex => $elementIndex")
                Log.d(TAG, "addCommentLauncher::critterIndex => $critterIndex")
                Log.d(TAG, "addCommentLauncher::commentText => $commentText")
                Log.d(TAG, "addCommentLauncher::imagePath => $imagePath")

                if (elementIndex != -1 && critterIndex != -1) {
                    viewModel.updateCritterComment(elementIndex, critterIndex, commentText, imagePath)
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
        binding = ActivityCtrlZoneBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            getUserData()?.let { userData ->
                user = userData
                viewModel.setUserData(user)

                zoneId = intent.getIntExtra(CTRL_ZONE_ACTIVITY_EXTRA_ZONE_ID, -1)
                if (zoneId == -1) {
                    Log.e(TAG, "initializeComponents: Zone ID is missing")
                    showErrorMessage("Zone non spécifiée")
                    navigateToPrevScreen()
                    return
                }

                viewModel.setConfigCtrl(getConfigCtrl() ?: JSONObject())

            } ?: run {
                Log.e(TAG, "initializeComponents: User data is null")
                navigateToMainActivity()
                return
            }

            getEntrySelected()?.let {
                entrySelected = it
                viewModel.setEntrySelected(it)

                viewModel.loadZone(zoneId)
            } ?: run {
                Log.e(TAG, "initializeComponents: Selected entry is null")
                showErrorMessage("Entrée non sélectionnée")
                setResult(RESULT_CANCELED)
                finish()
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "initializeComponents: Unexpected error", e)
            showErrorMessage("Erreur lors de l'initialisation")
            setResult(RESULT_CANCELED)
            finish()
        }
    }
    override fun setupComponents() {
        setupObservers()
        setupListeners()
        setupZoneIcon()
    }
    override fun setupObservers() {
        Log.d(TAG, "setupObservers: Setting up observers")

        viewModel.elements.observe(this) { elements ->
            elements?.let { createElementsViews(it) }
        }

        viewModel.zoneName.observe(this) { name ->
            binding.ctrlZoneActivityTitleZoneLbl.text = name
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

        binding.ctrlZoneActivityPrevBtn.setOnClickListener {
            navigateToPrevScreen()
        }
    }



    private fun setupZoneIcon() {
        zoneIcons[zoneId]?.let {
            binding.ctrlZoneActivityTitleZoneImg.setImageResource(it)
        } ?: run {
            binding.ctrlZoneActivityTitleZoneImg.setImageResource(R.drawable.localisation_blanc)
        }
    }
    @SuppressLint("InflateParams")
    private fun createElementsViews(elements: List<ObjElement>) {
        binding.ctrlZoneActivityGrillList.removeAllViews()

        elements.forEachIndexed { elementIndex, element ->
            val viewElement = LayoutInflater.from(this).inflate(R.layout.item_element, null).apply {
                val elementText = user.structure[zoneId.toString()]?.elmts?.get(element.id.toString())?.name
                    ?: "Élément ${element.id}"

                findViewById<TextView>(R.id.element_item_text_txt).text = elementText

                val noteView = findViewById<TextView>(R.id.element_item_note_txt)
                updateNoteView(noteView, element.note)
                noteView.tag = element.id

                val gridCritters = findViewById<LinearLayout>(R.id.element_item_grill_lyt)

                element.criterMap.forEach { (critterIndex, critter) ->
                    val viewCritter = LayoutInflater.from(this@CtrlZoneActivity)
                        .inflate(R.layout.item_criter, null).apply {
                            val critterText = user.structure[zoneId.toString()]?.elmts?.get(element.id.toString())?.critrs?.get(critter.id.toString())?.name
                                ?: "Critter ${critter.id}"

                            findViewById<TextView>(R.id.criter_item_text_txt).text = critterText

                            val buttonOk = findViewById<Button>(R.id.criter_item_ok_btn)
                            val buttonBad = findViewById<Button>(R.id.criter_item_bad_btn)
                            val buttonComment = findViewById<ImageButton>(R.id.criter_item_com_btn)

                            setupCritterButtons(
                                buttonOk,
                                buttonBad,
                                buttonComment,
                                elementIndex,
                                critterIndex,
                                critter.note
                            )
                        }

                    gridCritters.addView(viewCritter)
                }
            }

            binding.ctrlZoneActivityGrillList.addView(viewElement)
        }
    }

    private fun setupCritterButtons(
        buttonOk: Button,
        buttonBad: Button,
        buttonComment: ImageButton,
        elementIndex: Int,
        critterIndex: Int,
        currentValue: Int
    ) {
        val critter = viewModel.elements.value?.get(elementIndex)?.criterMap?.get(critterIndex)
        val hasComment = !critter?.comment?.txt.isNullOrEmpty()

        when (currentValue) {
            1 -> { // Cas "Nom"
                buttonOk.setBackgroundResource(R.drawable.button_selected_green)
                buttonOk.tag = "1"
                buttonComment.isEnabled = false
                buttonComment.alpha = 0.5f
                buttonComment.setBackgroundResource(R.drawable.button_desabled)
            }
            -1 -> { // Cas "Oui"
                buttonBad.setBackgroundResource(R.drawable.button_selected_red)
                buttonBad.tag = "1"
                buttonComment.isEnabled = true
                buttonComment.setBackgroundResource(
                    if (hasComment) R.drawable.button_selected_green
                    else R.drawable.button_desabled
                )
            }
            else -> {
                buttonOk.tag = "0"
                buttonBad.tag = "0"
                buttonComment.isEnabled = false
                buttonComment.alpha = 0.5f
                buttonComment.setBackgroundResource(R.drawable.button_desabled)
            }
        }

        buttonOk.setOnClickListener {
            handleCritterClick(buttonOk, buttonBad, buttonComment, elementIndex, critterIndex, 1)
        }

        buttonBad.setOnClickListener {
            handleCritterClick(buttonBad, buttonOk, buttonComment, elementIndex, critterIndex, -1)
        }

        buttonComment.setOnClickListener {
            openAddCommentActivity(elementIndex, critterIndex)
        }
    }

    private fun handleCritterClick(
        clickedButton: Button,
        otherButton: Button,
        commentButton: ImageButton,
        elementIndex: Int,
        critterIndex: Int,
        value: Int
    ) {
        try {
            Log.d(TAG, "handleCritterClick: Element $elementIndex, Critter $critterIndex, Value $value")

            val isSelected = clickedButton.tag == "1"
            val newValue = if (isSelected) 0 else value

            viewModel.updateCritterValue(elementIndex, critterIndex, newValue)

            updateButtonState(clickedButton, otherButton, commentButton, elementIndex, critterIndex, newValue)
        } catch (e: Exception) {
            Log.e(TAG, "handleCritterClick: Error updating critter value", e)
            showErrorMessage("Erreur lors de la mise à jour du critère")
        }
    }

    private fun updateButtonState(
        clickedButton: Button,
        otherButton: Button,
        commentButton: ImageButton,
        elementIndex: Int,
        critterIndex: Int,
        value: Int
    ) {
        otherButton.setBackgroundResource(R.drawable.button_desabled)
        otherButton.tag = "0"

        val critter = viewModel.elements.value?.get(elementIndex)?.criterMap?.get(critterIndex)
        val hasComment = !critter?.comment?.txt.isNullOrEmpty()

        if (value != 0) {
            clickedButton.setBackgroundResource(
                if (value > 0) R.drawable.button_selected_green else R.drawable.button_selected_red
            )
            clickedButton.tag = "1"

            commentButton.isEnabled = (value == -1)
            commentButton.alpha = if (value == -1) 1.0f else 0.5f
            commentButton.setBackgroundResource(
                if (value == -1 && hasComment) R.drawable.button_selected_green
                else R.drawable.button_desabled
            )
        } else {
            clickedButton.setBackgroundResource(R.drawable.button_desabled)
            clickedButton.tag = "0"

            commentButton.isEnabled = false
            commentButton.alpha = 0.5f
            commentButton.setBackgroundResource(R.drawable.button_desabled)
        }
    }

    private fun updateNoteView(noteView: TextView, noteValue: Int) {
        noteView.text = if (noteValue >= 0) "$noteValue %" else getString(R.string.txt_so)

        viewModel.limits.value?.let { (max, min) ->
            noteView.background = when {
                noteValue < 0 -> AppCompatResources.getDrawable(this, R.drawable.ctrl_note_grey)
                noteValue < min -> AppCompatResources.getDrawable(this, R.drawable.ctrl_note_red)
                noteValue >= max -> AppCompatResources.getDrawable(this, R.drawable.ctrl_note_green)
                else -> AppCompatResources.getDrawable(this, R.drawable.ctrl_note_orange)
            }
        } ?: run {
            noteView.setBackgroundColor(ContextCompat.getColor(this, R.color._dark_grey))
        }
    }

    private fun openAddCommentActivity(elementIndex: Int, critterIndex: Int) {
        val intent = Intent(this, AddCommentActivity::class.java).apply {
            putExtra(CTRL_ZONE_ACTIVITY_EXTRA_ELEMENT_POSITION, elementIndex)
            putExtra(CTRL_ZONE_ACTIVITY_EXTRA_CRITTER_POSITION, critterIndex)

            val critter = viewModel.elements.value?.get(elementIndex)?.criterMap?.get(critterIndex)
            critter?.comment?.let { comment ->
                putExtra(AddCommentActivity.ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_TEXT, comment.txt)
                putExtra(AddCommentActivity.ADD_COMMENT_ACTIVITY_EXTRA_COMMENT_IMAGE, comment.img)
            }
        }
        addCommentLauncher.launch(intent)
    }



    private fun showLoading(show: Boolean) {
        if (show) {
            progressDialog?.dismiss()
            progressDialog = UiUtils.showProgressDialog(
                this,
                "Chargement de la zone...",
                cancelable = false
            )
        } else {
            progressDialog?.dismiss()
            progressDialog = null
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



    private fun navigateToPrevScreen() {
        Log.d(TAG, "Navigating to previous screen with controlled elements")

        try {
            val resultIntent = Intent().apply {
                val controlledElements = viewModel.getControlledElements()
                val jsonString = Gson().toJson(controlledElements)

                Log.d(TAG, "navigateToPrevScreen: Returning ${controlledElements.size} elements")

                putExtra(CTRL_ZONE_ACTIVITY_EXTRA_ZONE_ID, zoneId)
                putExtra(CTRL_ZONE_ACTIVITY_EXTRA_CONTROLLED_ELEMENTS, jsonString)
            }

            setResult(RESULT_OK, resultIntent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "navigateToPrevScreen: Error preparing result", e)
            showErrorMessage("Erreur lors de la préparation des données de retour")

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

}
