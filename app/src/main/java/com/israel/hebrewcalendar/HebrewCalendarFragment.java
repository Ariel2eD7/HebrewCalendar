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

        // 0 = א
        // 1 = ב
        // 2 = ג
        // 3 = ד
        // 4 = ה
        // 5 = ו
        // 6 = ש

        months.add(new HebrewMonth(
                "תשרי",
                3,
                30
        ));

        months.add(new HebrewMonth(
                "חשוון",
                6,
                29
        ));

        months.add(new HebrewMonth(
                "כסלו",
                1,
                30
        ));

        months.add(new HebrewMonth(
                "טבת",
                3,
                29
        ));

        months.add(new HebrewMonth(
                "שבט",
                5,
                30
        ));

        months.add(new HebrewMonth(
                "אדר",
                0,
                29
        ));

        months.add(new HebrewMonth(
                "ניסן",
                2,
                30
        ));

        months.add(new HebrewMonth(
                "אייר",
                4,
                29
        ));

        months.add(new HebrewMonth(
                "סיוון",
                6,
                30
        ));

        months.add(new HebrewMonth(
                "תמוז",
                1,
                29
        ));

        months.add(new HebrewMonth(
                "אב",
                3,
                30
        ));

        months.add(new HebrewMonth(
                "אלול",
                5,
                29
        ));

        return months;
    }

}
