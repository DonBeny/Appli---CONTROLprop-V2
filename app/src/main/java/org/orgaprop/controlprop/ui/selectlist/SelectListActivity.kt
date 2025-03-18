package org.orgaprop.controlprop.ui.selectlist

import android.content.Intent
import android.os.Bundle
import android.util.Log

import androidx.recyclerview.widget.LinearLayoutManager

import org.json.JSONObject

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivitySelectListBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.selectlist.adapter.SelectListAdapter
import org.orgaprop.controlprop.viewmodels.SelectListViewModel
import java.io.Serializable
import kotlin.properties.Delegates

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

        initializeComponents()
        setupComponents()

        observeViewModel()
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
            finish()
            return
        }

        idMbr = userData!!.idMbr// (userData as JSONObject).getInt("id")
        adrMac = userData!!.adrMac// (userData as JSONObject).getString("adrMac")

        viewModel.setUserData(userData!!)
    }

    override fun setupComponents() {
        adapter = SelectListAdapter(emptyList(), type ?: "", this)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.fetchData(type, parentId, "")
    }

    override fun onItemClick(item: SelectItem) {
        Log.d(TAG, "onItemClick: $item")

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

    private fun observeViewModel() {
        viewModel.items.observe(this) { items ->
            adapter.updateItems(items)
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            // TODO: Gérer les erreurs
            // Afficher un message d'erreur à l'utilisateur (Toast, Snackbar, etc.)
        }
    }

}
