package com.example.garapro.ui.promotions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R
import com.example.garapro.data.model.quotations.CustomerPromotion

class PromotionListAdapter(
    private val items: List<CustomerPromotion>,
    private var selectedId: String?,
    private val onItemSelected: (CustomerPromotion) -> Unit
) : RecyclerView.Adapter<PromotionListAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rbSelected: RadioButton = view.findViewById(R.id.rbSelected)
        val tvName: TextView = view.findViewById(R.id.tvPromotionName)
        val tvDescription: TextView = view.findViewById(R.id.tvPromotionDescription)
        val tvEligibilityMessage: TextView = view.findViewById(R.id.tvEligibilityMessage)
        val tvDisplay: TextView = view.findViewById(R.id.tvPromotionDisplay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_promotion_option, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val promo = items[position]

        holder.tvName.text = promo.name
        holder.tvDescription.text = promo.description
        holder.tvDisplay.text = promo.discountDisplayText
        holder.tvEligibilityMessage.text = promo.eligibilityMessage

        holder.rbSelected.setOnCheckedChangeListener(null)
        holder.rbSelected.isChecked = promo.id == selectedId

        val onClick = {
            selectedId = promo.id
            onItemSelected(promo)
            notifyDataSetChanged()
        }

        holder.itemView.setOnClickListener { onClick() }
        holder.rbSelected.setOnClickListener { onClick() }
    }
}
