package org.orgaprop.controlprop.ui.selectList.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.databinding.ItemAgencyBinding
import org.orgaprop.controlprop.databinding.ItemResidBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.selectEntry.SelectListActivity

/**
 * Adaptateur pour afficher les listes de sélection
 */
class SelectListAdapter(
    private var items: List<SelectItem>,
    private val type: String,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_AGC = 0
        private const val TYPE_GRP = 1
        private const val TYPE_RSD = 2
        private const val TYPE_SEARCH = 3
    }

    interface OnItemClickListener {
        fun onItemClick(item: SelectItem)
    }

    override fun getItemViewType(position: Int): Int {
        return when (type) {
            SelectListActivity.SELECT_LIST_TYPE_AGC -> TYPE_AGC
            SelectListActivity.SELECT_LIST_TYPE_GRP -> TYPE_GRP
            SelectListActivity.SELECT_LIST_TYPE_RSD -> TYPE_RSD
            SelectListActivity.SELECT_LIST_TYPE_SEARCH -> TYPE_SEARCH
            else -> throw IllegalArgumentException("Type de liste inconnu : $type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            TYPE_AGC, TYPE_GRP -> {
                val binding = ItemAgencyBinding.inflate(inflater, parent, false)
                AgenceViewHolder(binding)
            }
            TYPE_RSD, TYPE_SEARCH -> {
                val binding = ItemResidBinding.inflate(inflater, parent, false)
                ResidenceViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Type de vue inconnu : $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]

        when (holder) {
            is AgenceViewHolder -> holder.bind(item, listener)
            is ResidenceViewHolder -> holder.bind(item, listener)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * Obtient une copie de la liste actuelle
     * @return Copie de la liste d'éléments
     */
    fun getCurrentList(): List<SelectItem> {
        return items.toList() // Retourne une copie défensive
    }

    /**
     * Met à jour la liste d'éléments
     * @param newItems Nouvelle liste d'éléments
     */
    fun updateItems(newItems: List<SelectItem>) {
        val diffResult = DiffUtil.calculateDiff(SelectItemDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    /**
     * Classe pour calculer les différences entre deux listes
     */
    private class SelectItemDiffCallback(
        private val oldList: List<SelectItem>,
        private val newList: List<SelectItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    /**
     * ViewHolder pour les agences et groupements
     */
    class AgenceViewHolder(private val binding: ItemAgencyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SelectItem, listener: OnItemClickListener) {
            binding.agenceItemName.text = item.name
            binding.root.setOnClickListener { listener.onItemClick(item) }
        }
    }

    /**
     * ViewHolder pour les résidences
     */
    class ResidenceViewHolder(private val binding: ItemResidBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SelectItem, listener: OnItemClickListener) {
            val adrText = "${item.address}, ${item.postalCode}"

            binding.itemResidRef.text = item.ref
            binding.itemResidName.text = item.name
            binding.itemResidEntry.text = item.entry
            binding.itemResidAdr.text = adrText
            binding.itemResidCity.text = item.city
            binding.itemResidLast.text = item.last

            val borderColor = if (item.delay) {
                itemView.context.getColor(R.color.text_secondary_light) // Couleur grise
            } else {
                itemView.context.getColor(R.color._light_green) // Couleur verte
            }

            setupItemBackground(borderColor)

            binding.root.setOnClickListener { listener.onItemClick(item) }
        }

        /**
         * Configure l'arrière-plan de l'élément
         * @param borderColor Couleur de la bordure
         */
        private fun setupItemBackground(borderColor: Int) {
            val shape = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f
                setStroke(8, borderColor)
                setColor(Color.TRANSPARENT)
            }

            binding.root.background = shape
        }
    }
}