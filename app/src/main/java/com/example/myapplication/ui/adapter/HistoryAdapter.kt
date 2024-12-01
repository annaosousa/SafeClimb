package com.example.myapplication.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.ui.item.HistoryItem

class HistoryAdapter(private var historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMonth: TextView = itemView.findViewById(R.id.textMonth)
        val textDay: TextView = itemView.findViewById(R.id.textDay)
        val textComma: TextView = itemView.findViewById(R.id.textComma)
        val textYear: TextView = itemView.findViewById(R.id.textYear)
        val textAt: TextView = itemView.findViewById(R.id.textAt)
        val textHour: TextView = itemView.findViewById(R.id.textHour)
        val textPeriodOfDay: TextView = itemView.findViewById(R.id.textPeriodOfDay)

        val imageWindy: ImageView = itemView.findViewById(R.id.imageWindy)
        val textWindy: TextView = itemView.findViewById(R.id.textWindy)
        val imageHumidity: ImageView = itemView.findViewById(R.id.imageHumidity)
        val textHumidity: TextView = itemView.findViewById(R.id.textHumidity)
        val imageTemperature: ImageView = itemView.findViewById(R.id.imageTemperature)
        val textTemperature: TextView = itemView.findViewById(R.id.textTemperature)
        val precipitation: TextView = itemView.findViewById(R.id.textPrecipitation)
        val soil: TextView = itemView.findViewById(R.id.textSoil)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = historyList[position]

        // Bind date and time
        holder.textMonth.text = historyItem.month
        holder.textDay.text = historyItem.day.toString()
        holder.textYear.text = historyItem.year.toString()
        holder.textHour.text = historyItem.hour
        holder.textPeriodOfDay.text = historyItem.periodOfDay

        holder.textWindy.text = historyItem.windSpeed
        holder.textHumidity.text = historyItem.humidity
        holder.textTemperature.text = historyItem.temperature
        holder.precipitation.text = historyItem.precipitation
        holder.soil.text = historyItem.soil
    }

    override fun getItemCount(): Int {
        return historyList.size
    }

    fun updateData(newHistoryList: List<HistoryItem>) {
        historyList = newHistoryList
        notifyDataSetChanged()
    }
}
