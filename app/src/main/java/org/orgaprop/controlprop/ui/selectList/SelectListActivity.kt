package org.orgaprop.controlprop.ui.selectList

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.window.OnBackInvokedDispatcher

import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager

import java.io.Serializable

import kotlin.properties.Delegates

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivitySelectListBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.main.MainActivity
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.selectList.adapter.SelectListAdapter
import org.orgaprop.controlprop.viewmodels.SelectListViewModel

class SelectListActivity : BaseActivity(), SelectListAdapter.OnItemClickListener {

    private val TAG = "SelectListActivity"

    companion object {
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
    }

    private lateinit var binding: ActivitySelectListBinding
    private val viewModel: SelectListViewModel by viewModel()
    private lateinit var adapter: SelectListAdapter

    private lateinit var type: String
    private var parentId by Delegates.notNull<Int>()
    private lateinit var searchQuery: String
    private var userData: LoginData? = null
    private var idMbr: Int = 0
    private lateinit var adrMac: String



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT
            ) {
                Log.d(TAG, "handleOnBackPressed: Back Pressed via OnBackInvokedCallback")
                navigateToPrevScreen()
            }
        } else {
            // Pour les versions inférieures à Android 13
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        }
    }



    override fun initializeComponents() {
        binding = ActivitySelectListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        type = intent.getStringExtra(SELECT_LIST_TYPE).toString()
        parentId = intent.getIntExtra(SELECT_LIST_ID, 0)
        searchQuery = intent.getStringExtra(SELECT_LIST_TXT).toString()

        userData = getUserData()

        if( userData == null ) {
            Log.d(TAG, "initializeComponents: UserData is null")
            navigateToMainActivity()
            return
        } else {
            Log.d(TAG, "initializeComponents: UserData is not null")
        }

        idMbr = userData!!.idMbr
        adrMac = userData!!.adrMac

        viewModel.setUserData(userData!!)
        viewModel.fetchData(type, parentId, searchQuery)
    }
    override fun setupComponents() {
        adapter = SelectListAdapter(emptyList(), type, this)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        setupObservers()
    }
    override fun setupListeners() {}
    override fun setupObservers() {
        viewModel.items.observe(this) { items ->
            adapter.updateItems(items)
        }

        viewModel.listAgents.observe(this) { listAgents ->
            setListAgents(listAgents)
        }
        viewModel.listPrestataires.observe(this) { listPrestates ->
            setListPrestates(listPrestates)
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            showToast(errorMessage)
            navigateToPrevScreen()
        }
    }



    override fun onItemClick(item: SelectItem) {
        Log.d(TAG, "onItemClick: $item")

        if( type == SELECT_LIST_TYPE_RSD ) {
            setEntrySelected(item)
            setEntryList(adapter.getCurrentList())
        }

        val resultIntent = Intent().apply {
            putExtra(SELECT_LIST_TYPE, type)
            putExtra(SELECT_LIST_RETURN, item)

            if (type == SELECT_LIST_TYPE_SEARCH) {
                putExtra(SELECT_LIST_COMMENT, item.comment)
            }
            if (type == SELECT_LIST_TYPE_RSD) {
                putExtra(SELECT_LIST_LIST, adapter.getCurrentList() as Serializable)
            }
        }

        Log.d(TAG, "onItemClick: $resultIntent")

        setResult(RESULT_OK, resultIntent)
        finish()
    }



    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java).apply {
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



    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d(TAG, "handleOnBackPressed: Back Pressed")
            navigateToPrevScreen()
        }
    }

}
