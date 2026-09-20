package com.israel.hebrewcalendar;




import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HebrewCalendarFragment extends Fragment {

    private RecyclerView monthsRecyclerView;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_hebrew_calendar,
                container,
                false
        );

        monthsRecyclerView =
                view.findViewById(R.id.monthsRecyclerView);

        monthsRecyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        List<HebrewMonth> months = createTestYear();

        HebrewYearAdapter adapter =
                new HebrewYearAdapter(months);

        monthsRecyclerView.setAdapter(adapter);

        return view;
    }

    private List<HebrewMonth> createTestYear() {

        List<HebrewMonth> months = new ArrayList<>();

        months.add(new HebrewMonth(
                "תשרי",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "חשוון",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "כסלו",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "טבת",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "שבט",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "אדר",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "ניסן",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "אייר",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "סיוון",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "תמוז",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "אב",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        months.add(new HebrewMonth(
                "אלול",
                "1,8,15,22,29",
                "2,9,16,23,30",
                "3,10,17,24",
                "4,11,18,25",
                "5,12,19,26",
                "6,13,20,27",
                "7,14,21,28"
        ));

        return months;
    }
}