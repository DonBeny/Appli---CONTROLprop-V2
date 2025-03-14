package org.orgaprop.controlprop.ui.selectlist

import android.content.Intent
import android.os.Bundle

import androidx.recyclerview.widget.LinearLayoutManager

import org.json.JSONObject

import org.koin.androidx.viewmodel.ext.android.viewModel

import org.orgaprop.controlprop.databinding.ActivitySelectListBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.BaseActivity
import org.orgaprop.controlprop.ui.selectlist.adapter.SelectListAdapter
import org.orgaprop.controlprop.viewmodels.SelectListViewModel
import java.io.Serializable
import kotlin.properties.Delegates

class SelectListActivity : BaseActivity(), SelectListAdapter.OnItemClickListener {

    companion object {
        const val SELECT_LIST_TYPE = "SELECT_LIST_TYPE"
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
    private lateinit var userData: Any
    private var idMbr: Int = 0
    private lateinit var adrMac: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initializeComponents()
        setupComponents()

        viewModel.fetchData(type, parentId, searchQuery, idMbr, adrMac)

        observeViewModel()
    }

    override fun initializeComponents() {
        binding = ActivitySelectListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        type = intent.getStringExtra(SELECT_LIST_TYPE).toString()
        parentId = intent.getIntExtra(SELECT_LIST_ID, 0)
        searchQuery = intent.getStringExtra(SELECT_LIST_TXT).toString()
        userData = getData("userData")!!
        idMbr = (userData as JSONObject).getInt("id")
        adrMac = (userData as JSONObject).getString("adrMac")

        viewModel.setUserData(userData as JSONObject)
    }

    override fun setupComponents() {
        adapter = SelectListAdapter(emptyList(), type ?: "", this)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onItemClick(item: SelectItem) {
        val resultIntent = Intent().apply {
            putExtra(SELECT_LIST_TYPE, type)
            putExtra(SELECT_LIST_ID, item.id)
            putExtra(SELECT_LIST_TXT, item.name)

            if (type == SELECT_LIST_TYPE_SEARCH) {
                putExtra(SELECT_LIST_COMMENT, item.comment)
            } else {
                putExtra(SELECT_LIST_LIST, adapter.getCurrentList() as Serializable) // Utiliser adapter.currentList

                if (type == SELECT_LIST_TYPE_RSD) {
                    putExtra(SELECT_LIST_COMMENT, item)
                }
            }
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun observeViewModel() {
        viewModel.items.observe(this) { items ->
            adapter.updateItems(items)
        }

        viewModel.errorMessage.observe(this) { errorMessage ->
            // Afficher un message d'erreur à l'utilisateur (Toast, Snackbar, etc.)
        }
    }

    private fun onItemSelected(item: SelectItem) {
        val resultIntent = Intent().apply {
            putExtra(SELECT_LIST_TYPE, type)
            putExtra(SELECT_LIST_ID, item.id)
            putExtra(SELECT_LIST_TXT, item.name)

            if (type == SELECT_LIST_TYPE_SEARCH) {
                putExtra(SELECT_LIST_COMMENT, item.comment)
            } else {
                putExtra(SELECT_LIST_LIST, adapter.getCurrentList() as Serializable)

                if (type == SELECT_LIST_TYPE_RSD) {
                    putExtra(SELECT_LIST_COMMENT, item)
                }
            }
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }

}
