package com.example.garapro.ui.emergencies

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.garapro.R

class EmergencyListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_list)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val swipe = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
        val empty = findViewById<View>(R.id.emptyState)
        val rv = findViewById<RecyclerView>(R.id.rvEmergencies)
        rv.layoutManager = LinearLayoutManager(this)
        val adapter = EmergencySummaryAdapter()
        rv.adapter = adapter
        

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra("force_new", true)
            startActivity(intent)
        }

        val data = intent.getParcelableArrayListExtra<EmergencySummary>("emergency_list")
        if (data != null && data.isNotEmpty()) {
            adapter.submitList(data)
            empty.visibility = View.GONE
        } else {
            val vm: EmergencyListViewModel by viewModels()
            vm.items.observe(this) {
                adapter.submitList(it)
                empty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
                swipe.isRefreshing = false
            }
            val userPrefs = getSharedPreferences(com.example.garapro.utils.Constants.USER_PREFERENCES, android.content.Context.MODE_PRIVATE)
            val authPrefs = getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
            val userId = userPrefs.getString("user_id", null) ?: authPrefs.getString("user_id", null)
            if (!userId.isNullOrBlank()) vm.loadByCustomer(userId) else vm.loadPending()
            swipe.setOnRefreshListener {
                if (!userId.isNullOrBlank()) vm.loadByCustomer(userId) else vm.loadPending()
            }

            
        }
    }
}

data class EmergencySummary(
    val id: String,
    val vehicleTitle: String,
    val issue: String,
    val status: String,
    val time: String,
    val garageName: String,
    val technicianName: String? = null,
    val technicianPhone: String? = null
) : android.os.Parcelable {
    constructor(parcel: android.os.Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString(),
        parcel.readString()
    )
    override fun describeContents(): Int = 0
    override fun writeToParcel(parcel: android.os.Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(vehicleTitle)
        parcel.writeString(issue)
        parcel.writeString(status)
        parcel.writeString(time)
        parcel.writeString(garageName)
        parcel.writeString(technicianName)
        parcel.writeString(technicianPhone)
    }
    companion object CREATOR : android.os.Parcelable.Creator<EmergencySummary> {
        override fun createFromParcel(parcel: android.os.Parcel): EmergencySummary = EmergencySummary(parcel)
        override fun newArray(size: Int): Array<EmergencySummary?> = arrayOfNulls(size)
    }
}

class EmergencySummaryAdapter : RecyclerView.Adapter<EmergencySummaryVH>() {
    private val items = mutableListOf<EmergencySummary>()
    fun submitList(list: List<EmergencySummary>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmergencySummaryVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_emergency_card, parent, false)
        return EmergencySummaryVH(v)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: EmergencySummaryVH, position: Int) {
        holder.bind(items[position])
    }
}

    class EmergencySummaryVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvIssue: TextView = itemView.findViewById(R.id.tvIssue)
        private val chipStatus: com.google.android.material.chip.Chip = itemView.findViewById(R.id.chipStatus)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val btnView: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnView)
        fun bind(item: EmergencySummary) {
            tvTitle.text = item.vehicleTitle
            tvIssue.text = item.issue
            chipStatus.text = item.status
            val ctx = itemView.context
            val color = when (item.status.lowercase()) {
                "accepted" -> androidx.core.content.ContextCompat.getColor(ctx, R.color.warning)
                "inprogress", "in_progress" -> androidx.core.content.ContextCompat.getColor(ctx, R.color.info)
                "arrived" -> androidx.core.content.ContextCompat.getColor(ctx, R.color.success)
                "cancelled", "canceled" -> androidx.core.content.ContextCompat.getColor(ctx, R.color.error)
                else -> androidx.core.content.ContextCompat.getColor(ctx, R.color.chip_selector)
            }
            chipStatus.chipBackgroundColor = android.content.res.ColorStateList.valueOf(color)
            tvTime.text = item.time
            tvAddress.text = item.garageName
            val lower = item.status.lowercase()
            btnView.visibility = if (lower == "inprogress" || lower == "in_progress") View.VISIBLE else View.GONE
            btnView.setOnClickListener {
                android.util.Log.d("EmergencyList", "Open clicked id=" + item.id + ", status=" + item.status)
                val intent = Intent(ctx, MapActivity::class.java)
                intent.putExtra("emergency_id", item.id)
                intent.putExtra("status", item.status)
                item.technicianName?.let { intent.putExtra("technician_name", it) }
                item.technicianPhone?.let { intent.putExtra("technician_phone", it) }
                ctx.startActivity(intent)
            }
        }
    }
