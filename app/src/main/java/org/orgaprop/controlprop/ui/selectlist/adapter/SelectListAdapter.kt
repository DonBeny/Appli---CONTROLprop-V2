package org.orgaprop.controlprop.ui.selectlist.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.databinding.AgenceItemBinding
import org.orgaprop.controlprop.databinding.ResidItemBinding
import org.orgaprop.controlprop.models.SelectItem
import org.orgaprop.controlprop.ui.selectlist.SelectListActivity

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
                val binding = AgenceItemBinding.inflate(inflater, parent, false)
                AgenceViewHolder(binding)
            }
            TYPE_RSD, TYPE_SEARCH -> {
                val binding = ResidItemBinding.inflate(inflater, parent, false)
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

    fun getCurrentList(): List<SelectItem> {
        return items
    }

    fun updateItems(newItems: List<SelectItem>) {
        val diffResult = DiffUtil.calculateDiff(SelectItemDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

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

    class AgenceViewHolder(private val binding: AgenceItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SelectItem, listener: OnItemClickListener) {
            binding.agenceItemName.text = item.name
            binding.root.setOnClickListener { listener.onItemClick(item) }
        }
    }

    class ResidenceViewHolder(private val binding: ResidItemBinding) : RecyclerView.ViewHolder(binding.root) {
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

            val shape = GradientDrawable()
            shape.shape = GradientDrawable.RECTANGLE
            shape.cornerRadius = 8f
            shape.setStroke(8, borderColor)
            shape.setColor(Color.TRANSPARENT)

            binding.root.background = shape

            binding.root.setOnClickListener { listener.onItemClick(item) }
        }
    }

}
