package org.orgaprop.controlprop.ui.grille.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.orgaprop.controlprop.models.ObjAgent

class AgentSpinnerAdapter(
    context: Context,
    private val agents: List<ObjAgent>,
    private val prestates: List<ObjAgent>
) : ArrayAdapter<Any>(context, android.R.layout.simple_spinner_item) {

    override fun getCount(): Int {
        // Nombre total d'éléments (agents + prestataires + 2 en-têtes)
        return agents.size + prestates.size + 2
    }

    override fun getItem(position: Int): Any {
        return when {
            position == 0 -> "Agents de proximité"
            position <= agents.size -> agents[position - 1]
            position == agents.size + 1 -> "Prestataires"
            else -> prestates[position - agents.size - 2]
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val item = getItem(position)
        view.text = when (item) {
            is String -> item // En-tête
            is ObjAgent -> item.name // Nom de l'agent ou du prestataire
            else -> ""
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        val item = getItem(position)
        view.text = when (item) {
            is String -> item // En-tête
            is ObjAgent -> item.name // Nom de l'agent ou du prestataire
            else -> ""
        }
        // Désactiver les en-têtes pour qu'ils ne soient pas sélectionnables
        view.isEnabled = item !is String
        return view
    }
}