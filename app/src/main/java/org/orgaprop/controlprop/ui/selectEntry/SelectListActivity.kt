package org.orgaprop.controlprop.ui.selectEntry

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.window.OnBackInvokedDispatcher
import androidx.appcompat.app.AlertDialog

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import kotlin.properties.Delegates

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivitySelectListBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.login.LoginActivity
import org.orgaprop.controlprop.models.LoginData
import org.orgaprop.controlprop.ui.selectList.adapter.SelectListAdapter
import org.orgaprop.controlprop.utils.LogUtils
import org.orgaprop.controlprop.utils.UiUtils
import org.orgaprop.controlprop.viewmodels.SelectListViewModel

class SelectListActivity : BaseActivity(), SelectListAdapter.OnItemClickListener {

    companion object {
        private const val TAG = "SelectListActivity"

        const val SELECT_LIST_TYPE = "SELECT_LIST_TYPE"
        const val SELECT_LIST_RETURN = "SELECT_LIST_RETURN"
        const val SELECT_LIST_ID = "SELECT_LIST_ID"
        const val SELECT_LIST_TXT = "SELECT_LIST_TXT"
        const val SELECT_LIST_LIST = "SELECT_LIST_LIST"
        const val SELECT_LIST_COMMENT = "SELECT_LIST_COMMENT"

        const val SELECT_LIST_TYPE_AGC = "agc"
        const val SELECT_LIST_TYPE_GRP = "grp"
        const val SELECT_LIST_TYPE_RSD = "rsd"
        const val SELECT_LIST_TYPE_SEARCH = "search"
        const val SELECT_LIST_TYPE_PRESTATES = "prestates"
    }

    private lateinit var binding: ActivitySelectListBinding
    private val viewModel: SelectListViewModel by viewModel()
    private lateinit var adapter: SelectListAdapter
    private var progressDialog: AlertDialog? = null

    private lateinit var type: String
    private var parentId by Delegates.notNull<Int>()
    private lateinit var searchQuery: String
    private var userData: LoginData? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackInvokedDispatcher.registerOnBackInvokedCallback(
            OnBackInvokedDispatcher.PRIORITY_DEFAULT
        ) {
            Log.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
            navigateToPrevScreen()
        }
    }
    override fun onDestroy() {
        dismissLoadingDialog()
        super.onDestroy()
    }



    override fun initializeComponents() {
        binding = ActivitySelectListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        type = intent.getStringExtra(SELECT_LIST_TYPE) ?: ""
        parentId = intent.getIntExtra(SELECT_LIST_ID, 0)
        searchQuery = intent.getStringExtra(SELECT_LIST_TXT) ?: ""

        userData = getUserData()

        if( userData == null ) {
            Log.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            Log.d(TAG, "initializeComponents: UserData is not null")
            viewModel.setUserData(userData!!)
        }

        showLoadingDialog()

        if (type == SELECT_LIST_TYPE_RSD && getEntryList().isNotEmpty()) {
            val cachedList = getEntryList()
            LogUtils.json(TAG, "initializeComponents: Using cached entry list", cachedList)
            viewModel.setCachedItems(cachedList)
        } else {
            // Sinon, charger les données depuis le serveur
            viewModel.fetchData(type, parentId, searchQuery)
        }
    }
    override fun setupComponents() {
        adapter = SelectListAdapter(emptyList(), type, this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@SelectListActivity.adapter
            addItemDecoration(createItemDecoration())
        }

        setupObservers()
    }
    override fun setupObservers() {
        viewModel.items.observe(this) { items ->
            adapter.updateItems(items)

            if (items.isEmpty()) {
                UiUtils.showInfoSnackbar(
                    binding.root,
                    "Aucun élément trouvé"
                )
            }

            dismissLoadingDialog()
        }

        viewModel.isLoading.observe(this) { isLoading ->
            if (isLoading) {
                showLoadingDialog()
            } else {
                dismissLoadingDialog()
            }
        }

        viewModel.listAgents.observe(this) { listAgents ->
            setListAgents(listAgents)
        }
        viewModel.listPrestataires.observe(this) { listPrestates ->
            setListPrestates(listPrestates)
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            dismissLoadingDialog()
            UiUtils.showErrorSnackbar(
                binding.root,
                errorMessage,
                actionText = "Retour",
                action = { navigateToPrevScreen() }
            )
            navigateToPrevScreen()
        }
    }
    override fun setupListeners() {}



    private fun createItemDecoration(): RecyclerView.ItemDecoration {
        return object : RecyclerView.ItemDecoration() {
            // Implémentation de votre décorateur
        }
    }



    override fun onItemClick(item: SelectItem) {
        Log.d(TAG, "onItemClick: $item")

        if( type == SELECT_LIST_TYPE_RSD ) {
            setEntrySelected(item)
            setEntryList(adapter.getCurrentList())
        } else if (type == SELECT_LIST_TYPE_SEARCH) {
            showLoadingDialog()


            viewModel.fetchComplementaryData(item.agency)

            viewModel.complementaryDataReady.observe(this) { isReady ->
                if (isReady) {
                    setEntrySelected(item)
                    setEntryList(adapter.getCurrentList())

                    dismissLoadingDialog()

                    val resultIntent = Intent().apply {
                        putExtra(SELECT_LIST_TYPE, type)
                        putExtra(SELECT_LIST_RETURN, item)
                        putExtra(SELECT_LIST_COMMENT, item.comment)
                    }

                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }

            viewModel.complementaryDataError.observe(this) { error ->
                dismissLoadingDialog()
                UiUtils.showErrorSnackbar(
                    binding.root,
                    error,
                    actionText = "Réessayer",
                    action = { onItemClick(item) }
                )
            }

            return
        }

        val resultIntent = Intent().apply {
            putExtra(SELECT_LIST_TYPE, type)
            putExtra(SELECT_LIST_RETURN, item)

            if (type == SELECT_LIST_TYPE_SEARCH) {
                putExtra(SELECT_LIST_COMMENT, item.comment)
            }
        }

        Log.d(TAG, "onItemClick: $resultIntent")

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun showLoadingDialog() {
        runOnUiThread {
            progressDialog?.dismiss()
            progressDialog = UiUtils.showProgressDialog(
                this,
                message = "Veuillez patienter pendant la récupération des données...",
                cancelable = false
            )
        }
    }
    private fun dismissLoadingDialog() {
        runOnUiThread {
            progressDialog?.dismiss()
            progressDialog = null
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
    private fun navigateToPrevScreen() {
        Log.d(TAG, "Navigating to SelectEntryActivity")

        setResult(RESULT_CANCELED)
        finish()
    }

}
