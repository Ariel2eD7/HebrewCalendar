package com.israel.hebrewcalendar;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HebrewYearAdapter
        extends RecyclerView.Adapter<HebrewYearAdapter.MonthViewHolder> {


    private final List<HebrewMonth> months;

    // רוחב של תא אחד
    private static final int CELL_WIDTH_DP = 40;

    public HebrewYearAdapter(List<HebrewMonth> months) {
        this.months = months;
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_hebrew_month,
                        parent,
                        false
                );

        return new MonthViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull MonthViewHolder holder,
            int position) {

        HebrewMonth month = months.get(position);

        // שם החודש
        holder.monthName.setText(month.name);

        // מנקים את התאים הקודמים
        holder.daysContainer.removeAllViews();

        // רווחים לפני היום הראשון של החודש
        for (int i = 0; i < month.startDayOfWeek; i++) {

            TextView emptyCell =
                    createDayCell(holder.itemView);

            emptyCell.setText("");

            holder.daysContainer.addView(emptyCell);
        }

        // ימי החודש
        for (int day = 1; day <= month.daysInMonth; day++) {

            TextView dayCell =
                    createDayCell(holder.itemView);

            dayCell.setText(String.valueOf(day));

            holder.daysContainer.addView(dayCell);
        }
    }

    /**
     * יצירת תא אחד של יום.
     */

    private TextView createDayCell(View parent) {

        TextView textView = new TextView(
                parent.getContext()
        );

        textView.setLayoutParams(
                new LinearLayout.LayoutParams(
                        dpToPx(parent, CELL_WIDTH_DP),
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        textView.setGravity(
                android.view.Gravity.CENTER
        );

        textView.setTextSize(13);

        textView.setTextColor(
                Color.BLACK
        );

// גבול של תא בגיליון
        textView.setBackgroundResource(
                R.drawable.calendar_cell_border
        );

        return textView;

    }


    /**
     * המרה מ-dp ל-pixel.
     */
    private int dpToPx(
            View view,
            int dp) {

        float density =
                view.getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (dp * density + 0.5f);
    }

    @Override
    public int getItemCount() {
        return months.size();
    }

    static class MonthViewHolder
            extends RecyclerView.ViewHolder {

        TextView monthName;

        LinearLayout daysContainer;

        public MonthViewHolder(
                @NonNull View itemView) {

            super(itemView);

            monthName =
                    itemView.findViewById(
                            R.id.monthName
                    );

            daysContainer =
                    itemView.findViewById(
                            R.id.daysContainer
                    );
        }
    }

}
