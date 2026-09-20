package com.israel.hebrewcalendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HebrewYearAdapter
        extends RecyclerView.Adapter<HebrewYearAdapter.MonthViewHolder> {

    private final List<HebrewMonth> months;

    public HebrewYearAdapter(List<HebrewMonth> months) {
        this.months = months;
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hebrew_month, parent, false);

        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MonthViewHolder holder,
            int position) {

        HebrewMonth month = months.get(position);

        holder.monthName.setText(month.name);

        holder.sunday.setText(month.sunday);
        holder.monday.setText(month.monday);
        holder.tuesday.setText(month.tuesday);
        holder.wednesday.setText(month.wednesday);
        holder.thursday.setText(month.thursday);
        holder.friday.setText(month.friday);
        holder.saturday.setText(month.saturday);
    }

    @Override
    public int getItemCount() {
        return months.size();
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {

        TextView monthName;

        TextView sunday;
        TextView monday;
        TextView tuesday;
        TextView wednesday;
        TextView thursday;
        TextView friday;
        TextView saturday;

        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);

            monthName = itemView.findViewById(R.id.monthName);

            sunday = itemView.findViewById(R.id.sunday);
            monday = itemView.findViewById(R.id.monday);
            tuesday = itemView.findViewById(R.id.tuesday);
            wednesday = itemView.findViewById(R.id.wednesday);
            thursday = itemView.findViewById(R.id.thursday);
            friday = itemView.findViewById(R.id.friday);
            saturday = itemView.findViewById(R.id.saturday);
        }
    }
}