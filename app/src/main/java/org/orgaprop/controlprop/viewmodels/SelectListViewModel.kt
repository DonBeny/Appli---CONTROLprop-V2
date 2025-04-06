package org.orgaprop.controlprop.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.orgaprop.controlprop.exceptions.BaseException
import org.orgaprop.controlprop.managers.SelectListManager
import org.orgaprop.controlprop.models.ObjConfig
import org.orgaprop.controlprop.models.ObjDateCtrl
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.models.parseReferentsFromJson
import org.orgaprop.controlprop.ui.main.types.LoginData
import org.orgaprop.controlprop.ui.selectList.SelectListActivity

class SelectListViewModel(
    private val selectListManager: SelectListManager
): ViewModel() {

    private val TAG = "SelectListViewModel"

    private var idMbr: Int = -1
    private var adrMac: String = ""

    private val _items = MutableLiveData<List<SelectItem>>()
    val items: LiveData<List<SelectItem>> get() = _items

    private val _listAgents = MutableLiveData<JSONObject>()
    val listAgents: LiveData<JSONObject> get() = _listAgents

    private val _listPrestataires = MutableLiveData<JSONObject>()
    val listPrestataires: LiveData<JSONObject> get() = _listPrestataires

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private lateinit var userData: LoginData

    fun fetchData(type: String, parentId: Int, searchQuery: String) {
        viewModelScope.launch {
            try {
                val items = when (type) {
                    SelectListActivity.SELECT_LIST_TYPE_AGC -> fetchAgenciesFromUserData()
                    else -> {
                        val jsonResponse = selectListManager.fetchData(
                            type,
                            parentId,
                            searchQuery,
                            idMbr,
                            adrMac
                        )
                        parseJsonResponse(jsonResponse, type)
                    }
                }
                _items.value = items
            } catch (e: BaseException) {
                _errorMessage.value = e.message
            } catch (e: JSONException) {
                _errorMessage.value = "Erreur de traitement des données"
            } catch (e: Exception) {
                _errorMessage.value = "Une erreur inconnue s'est produite"
            }
        }
    }

    private fun fetchAgenciesFromUserData(): List<SelectItem> {
        return userData.let {
            val agencesArray = it.agences
            val agences = mutableListOf<SelectItem>()
            for (i in agencesArray.indices) {
                val agence = agencesArray.get(i)

                agences.add(
                    SelectItem(
                        id = agence.id,
                        name = agence.nom,
                        type = SelectListActivity.SELECT_LIST_TYPE_AGC
                    )
                )
            }
            agences
        }
    }

    private fun parseJsonResponse(jsonResponse: JSONObject, type: String): List<SelectItem> {
        Log.d(TAG, "parseJsonResponse: $jsonResponse")
        Log.d(TAG, "parseJsonResponse: type=$type")

        if (jsonResponse.getBoolean("status")) {
            if (type == SelectListActivity.SELECT_LIST_TYPE_GRP) {
                Log.d(TAG, "parseJsonResponse: type=SelectListActivity.SELECT_LIST_TYPE_GRP")

                return parseResponseGrp(jsonResponse, type)
            } else {
                Log.d(TAG, "parseJsonResponse: type!=SelectListActivity.SELECT_LIST_TYPE_GRP")

                return parseResponseRsd(jsonResponse, type)
            }
        } else {
            _errorMessage.value = jsonResponse.getJSONObject("error").getString("txt")
        }

        return emptyList()
    }
    private fun parseResponseGrp(jsonResponse: JSONObject, type: String): List<SelectItem> {
        val dataArray = jsonResponse.getJSONObject("data").getJSONArray("grps")
        val newItems = mutableListOf<SelectItem>()

        for (i in 0 until dataArray.length()) {
            val jsonItem = dataArray.getJSONObject(i)

            Log.d(TAG, "parseResponseGrp: jsonItem=$jsonItem")

            val item = SelectItem(
                id = jsonItem.getInt("id"),
                name = jsonItem.getString("txt"),
                type = type
            )

            Log.d(TAG, "parseResponseGrp: item=$item")

            newItems.add(item)
        }

        Log.d(TAG, "parseResponseGrp: items=$items")

        val dataAgents = jsonResponse.getJSONObject("data").getJSONObject("agts")

        Log.d(TAG, "parseResponseGrp: dataAgents=$dataAgents")

        val dataPrestates = jsonResponse.getJSONObject("data").getJSONObject("prestas")

        Log.d(TAG, "parseResponseGrp: dataPrestates=$dataPrestates")

        _listAgents.value = dataAgents
        _listPrestataires.value = dataPrestates

        return newItems.toList()
    }
    private fun parseResponseRsd(jsonResponse: JSONObject, type: String): List<SelectItem> {
        val dataArray = jsonResponse.getJSONArray("data")
        val newItems = mutableListOf<SelectItem>()

        Log.d(TAG, "parseResponseRsd: dataArray=$dataArray")

        for (i in 0 until dataArray.length()) {
            val jsonItem = dataArray.getJSONObject(i)

            val prop = jsonItem.optJSONObject("prop")?.let { propJson ->
                Log.d(TAG, "parseResponseRsd: propJson=$propJson")

                SelectItem.Prop(
                    zones = parseRsdZones(propJson),
                    /*SelectItem.Prop.Zones(
                        proxi = propJson.getJSONObject("zones").getJSONArray("proxi").let { array ->
                            (0 until array.length()).map { array.getString(it) }
                        },
                        contra = propJson.getJSONObject("zones").getJSONArray("contra").let { array ->
                            (0 until array.length()).map { array.getString(it) }
                        }
                    ),*/
                    ctrl = parseRsdCtrl(propJson),
                    /*SelectItem.Prop.Ctrl.fromJsonArray(
                        grille = propJson.getJSONObject("ctrl").getJSONArray("grille"),
                        note = propJson.getJSONObject("ctrl").getInt("note"),
                        conf = ObjConfig(
                            visite = propJson.getJSONObject("ctrl").getJSONObject("conf").getBoolean("visite"),
                            meteo = propJson.getJSONObject("ctrl").getJSONObject("conf").getBoolean("meteo"),
                            affichage = propJson.getJSONObject("ctrl").getJSONObject("conf").getBoolean("aff"),
                            produits = propJson.getJSONObject("ctrl").getJSONObject("conf").getBoolean("prod")
                        ),
                        date = ObjDateCtrl(
                            txt = propJson.getJSONObject("ctrl").getJSONObject("date").getString("txt"),
                            value = propJson.getJSONObject("ctrl").getJSONObject("date").getInt("val")
                        )
                    )*/
                )
            }

            Log.d(TAG, "parseResponseRsd: prop=$prop")

            val referentsJson = jsonItem.optJSONObject("aff")

            Log.d(TAG, "parseResponseRsd: referentsJson=$referentsJson")

            val referents = referentsJson?.let { parseReferentsFromJson(it) }

            Log.d(TAG, "parseResponseRsd: referents=$referents")

            val item = SelectItem(
                id = jsonItem.getInt("id"),
                ref = jsonItem.getString("ref"),
                name = jsonItem.getString("name"),
                entry = jsonItem.getString("entry"),
                agency = jsonItem.getInt("agency"),
                group = jsonItem.getInt("group"),
                address = jsonItem.getJSONObject("adr").getString("rue"),
                postalCode = jsonItem.getJSONObject("adr").getString("cp"),
                city = jsonItem.getJSONObject("adr").getString("city"),
                last = jsonItem.getString("last"),
                delay = jsonItem.getBoolean("delay"),
                comment = jsonItem.getString("comment"),
                type = type,
                prop = prop,
                referents = referents
            )

            Log.d(TAG, "parseResponseRsd: item=$item")

            newItems.add(item)
        }

        Log.d(TAG, "parseResponseRsd: newItems=$newItems")

        return newItems.toList()
    }
    private fun parseRsdZones(jsonObj: JSONObject): SelectItem.Prop.Zones {
        Log.d(TAG, "parseRsdZones: jsonObj=$jsonObj")

        val proxiArray = jsonObj.getJSONObject("zones").getJSONArray("proxi")
        val contraArray = jsonObj.getJSONObject("zones").getJSONArray("contra")

        Log.d(TAG, "parseRsdZones: proxiArray=$proxiArray")
        Log.d(TAG, "parseRsdZones: contraArray=$contraArray")

        val proxiList = (0 until proxiArray.length()).map { proxiArray.getString(it) }
        val contraList = (0 until contraArray.length()).map { contraArray.getString(it) }

        Log.d(TAG, "parseRsdZones: proxiList=$proxiList")
        Log.d(TAG, "parseRsdZones: contraList=$contraList")

        return SelectItem.Prop.Zones(proxiList, contraList)
    }
    private fun parseRsdCtrl(jsonObj: JSONObject): SelectItem.Prop.Ctrl {
        val jsonCtrl = jsonObj.getJSONObject("ctrl")
        Log.d(TAG, "parseRsdCtrl: jsonCtrl=$jsonCtrl")

        val note = jsonCtrl.getInt("note")
        Log.d(TAG, "parseRsdCtrl: note=$note")

        val conf = parseRsdConfCtrl(jsonCtrl)
        Log.d(TAG, "parseRsdCtrl: conf=$conf")

        val date = parseRsdDateCtrl(jsonCtrl)
        Log.d(TAG, "parseRsdCtrl: date=$date")

        val grille = jsonCtrl.optJSONArray("grille")?.toString() ?: "[]"
        Log.d(TAG, "parseRsdCtrl: grille=$grille")

        return SelectItem.Prop.Ctrl.fromJsonArray(
            note = note,
            conf = conf,
            date = date,
            grille = JSONArray(grille)
        )
    }
    private fun parseRsdConfCtrl(jsonObj: JSONObject): ObjConfig {
        Log.d(TAG, "parseRsdConfCtrl: jsonObj=$jsonObj")

        val confJson = jsonObj.getJSONObject("conf")

        Log.d(TAG, "parseRsdConfCtrl: confJson=$confJson")

        val conf = ObjConfig(
            visite = confJson.getBoolean("visite"),
            meteo = confJson.getBoolean("meteo"),
            affichage = confJson.getBoolean("aff"),
            produits = confJson.getBoolean("prod")
        )

        Log.d(TAG, "parseRsdConfCtrl: conf=$conf")

        return conf
    }
    private fun parseRsdDateCtrl(jsonObj: JSONObject): ObjDateCtrl {
        Log.d(TAG, "parseRsdDateCtrl: jsonObj=$jsonObj")

        val dateJson = jsonObj.getJSONObject("date")

        Log.d(TAG, "parseRsdDateCtrl: dateJson=$dateJson")

        val date = ObjDateCtrl(
            txt = dateJson.getString("txt"),
            value = dateJson.getLong("val")
        )

        Log.d(TAG, "parseRsdDateCtrl: date=$date")

        return date
    }

    fun setUserData(userData: LoginData) {
        this.userData = userData
        idMbr = userData.idMbr
        adrMac = userData.adrMac
    }

}
