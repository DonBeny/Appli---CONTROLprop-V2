package org.orgaprop.controlprop.ui.grille.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

import org.orgaprop.controlprop.R
import org.orgaprop.controlprop.models.ObjBtnZone

class BtnZoneAdapter(
    var zones: List<ObjBtnZone>,
    private val minLimit: Int,
    private val maxLimit: Int,
    private val onZoneClicked: (ObjBtnZone) -> Unit
) : RecyclerView.Adapter<BtnZoneAdapter.BtnZoneViewHolder>() {

    fun updateZones(newZones: List<ObjBtnZone>) {
        zones = newZones
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BtnZoneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_btn_zone, parent, false)
        return BtnZoneViewHolder(view, minLimit, maxLimit)
    }

    override fun onBindViewHolder(holder: BtnZoneViewHolder, position: Int) {
        val zone = zones[position]
        holder.bind(zone)
        holder.itemView.setOnClickListener { onZoneClicked(zone) }
    }

    override fun getItemCount(): Int = zones.size

    class BtnZoneViewHolder(
        itemView: View,
        private val minLimit: Int,
        private val maxLimit: Int
    ) : RecyclerView.ViewHolder(itemView) {
        private val zoneButton: TextView = itemView.findViewById(R.id.item_btn_zone_text)
        private val zoneNote: TextView = itemView.findViewById(R.id.item_btn_zone_note)
        private val zoneImg: ImageView = itemView.findViewById(R.id.item_btn_zone_img)

        fun bind(zone: ObjBtnZone) {
            zoneButton.text = zone.txt
            zoneNote.text = zone.note

            val noteValue = zone.note.removeSuffix("%").toIntOrNull() ?: -1
            val textColor = when {
                noteValue < 0 -> Color.GRAY
                noteValue < minLimit -> Color.RED
                noteValue >= maxLimit -> Color.GREEN
                else -> Color.parseColor("#FFA500")
            }

            zoneNote.setTextColor(textColor)
            zoneNote.background = null

            Glide.with(itemView.context)
                .load(getDrawableIdForZone(zone.icon))
                .placeholder(R.drawable.localisation_vert)
                .error(R.drawable.localisation_vert)
                .into(zoneImg)
        }

        private fun getDrawableIdForZone(iconName: String): Int {
            return itemView.context.resources.getIdentifier(
                iconName,
                "drawable",
                itemView.context.packageName
            )
        }
    }

}
