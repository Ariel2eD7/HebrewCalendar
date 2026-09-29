package com.israel.hebrewcalendar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayDetailsActivity extends AppCompatActivity {

     private TextView dayTitleTextView;

    private LinearLayout entriesContainer;

    private Button addButton;

    private FirebaseAuth mAuth;

    private FirebaseFirestore db;

    private String gregorianDate;

    private String hebrewDate;

    private final List<CalendarEntry> entries =
            new ArrayList<>();


    private static final String FIRESTORE_DATE_FORMAT =
            "yyyy-MM-dd";


    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.day_details
        );


        mAuth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();


        dayTitleTextView =
                findViewById(
                        R.id.dayTitleTextView
                );


        entriesContainer =
                findViewById(
                        R.id.entriesContainer
                );


        addButton =
                findViewById(
                        R.id.addButton
                );


        gregorianDate =
                getIntent().getStringExtra(
                        "gregorian_date"
                );


        hebrewDate =
                getIntent().getStringExtra(
                        "hebrew_date"
                );


        updateDayTitle();


        addButton.setOnClickListener(
                v -> showAddDialog()
        );
    }


    @Override
    protected void onResume() {

        super.onResume();

        loadEntries();
    }


    /*
     * ============================================================
     * כותרת היום
     * ============================================================
     */

    private void updateDayTitle() {

        if (gregorianDate == null) {

            dayTitleTextView.setText(
                    "אירועי היום"
            );

            return;
        }


        dayTitleTextView.setText(
                "אירועים ביום " +
                        formatDateForDisplay(
                                gregorianDate
                        )
        );
    }


    /*
     * ============================================================
     * טעינת האירועים
     * ============================================================
     */

    private void loadEntries() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        if (currentUser == null) {

            displayEntries();

            return;
        }


        db.collection("users")
                .document(
                        currentUser.getUid()
                )
                .collection("calendarEntries")
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            entries.clear();


                            String selectedDate =
                                    normalizeDate(
                                            gregorianDate
                                    );


                            if (selectedDate == null) {

                                displayEntries();

                                return;
                            }


                            for (DocumentSnapshot document :
                                    querySnapshot.getDocuments()) {

                                String startDate =
                                        document.getString(
                                                "startDate"
                                        );


                                String endDate =
                                        document.getString(
                                                "endDate"
                                        );


                                /*
                                 * תמיכה באירועים ישנים.
                                 */

                                if (startDate == null ||
                                        startDate.trim().isEmpty()) {

                                    String oldDate =
                                            document.getString(
                                                    "gregorianDate"
                                            );


                                    if (oldDate != null &&
                                            !oldDate.trim().isEmpty()) {

                                        startDate =
                                                normalizeDate(
                                                        oldDate
                                                );

                                        endDate =
                                                startDate;
                                    }
                                }


                                if (startDate == null ||
                                        endDate == null) {

                                    continue;
                                }


                                if (!isDateInsideRange(
                                        selectedDate,
                                        startDate,
                                        endDate
                                )) {

                                    continue;
                                }


                                CalendarEntry entry =
                                        new CalendarEntry();


                                entry.id =
                                        document.getId();


                                entry.title =
                                        document.getString(
                                                "title"
                                        );


                                entry.type =
                                        document.getString(
                                                "type"
                                        );


                                entry.startDate =
                                        startDate;


                                entry.endDate =
                                        endDate;


                                entry.startTime =
                                        document.getString(
                                                "startTime"
                                        );


                                entry.endTime =
                                        document.getString(
                                                "endTime"
                                        );


                                Boolean allDay =
                                        document.getBoolean(
                                                "allDay"
                                        );


                                entry.allDay =
                                        allDay != null &&
                                                allDay;


                                entry.location =
                                        document.getString(
                                                "location"
                                        );


                                entry.description =
                                        document.getString(
                                                "description"
                                        );


                                entries.add(
                                        entry
                                );
                            }


                            displayEntries();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "טעינת האירועים נכשלה: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        )
                );
    }


    /*
     * ============================================================
     * הצגת האירועים
     * ============================================================
     */

    private void displayEntries() {

        entriesContainer.removeAllViews();


        if (entries.isEmpty()) {

            TextView empty =
                    new TextView(this);


            empty.setText(
                    "אין אירועים ביום הזה"
            );


            empty.setTextSize(17);

            empty.setTextColor(
                    Color.GRAY
            );

            empty.setGravity(
                    android.view.Gravity.CENTER
            );


            entriesContainer.addView(
                    empty
            );

            return;
        }


        for (CalendarEntry entry :
                entries) {

            LinearLayout item =
                    new LinearLayout(this);


            item.setOrientation(
                    LinearLayout.VERTICAL
            );


            item.setPadding(
                    20,
                    18,
                    20,
                    18
            );


            item.setBackgroundColor(
                    Color.rgb(
                            220,
                            235,
                            255
                    )
            );


            TextView title =
                    new TextView(this);


            title.setText(
                    safe(entry.title)
            );


            title.setTextSize(18);

            title.setTextColor(
                    Color.BLACK
            );


            title.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
            );


            TextView details =
                    new TextView(this);


            StringBuilder text =
                    new StringBuilder();


            if (entry.allDay) {

                text.append(
                        "כל היום"
                );

            } else {

                if (entry.startTime != null &&
                        !entry.startTime.isEmpty()) {

                    text.append(
                            entry.startTime
                    );


                    if (entry.endTime != null &&
                            !entry.endTime.isEmpty()) {

                        text.append(
                                " → "
                        );

                        text.append(
                                entry.endTime
                        );
                    }
                }
            }


            if (entry.startDate != null &&
                    entry.endDate != null &&
                    !entry.startDate.equals(
                            entry.endDate
                    )) {

                if (text.length() > 0) {
                    text.append("\n");
                }

                text.append(
                        formatDateForDisplay(
                                entry.startDate
                        )
                );

                text.append(
                        " → "
                );

                text.append(
                        formatDateForDisplay(
                                entry.endDate
                        )
                );
            }


            if (entry.location != null &&
                    !entry.location.trim().isEmpty()) {

                text.append("\n📍 ");

                text.append(
                        entry.location
                );
            }


            if (entry.description != null &&
                    !entry.description.trim().isEmpty()) {

                text.append("\n");

                text.append(
                        entry.description
                );
            }


            details.setText(
                    text.toString()
            );


            details.setTextSize(14);

            details.setTextColor(
                    Color.DKGRAY
            );


            item.addView(
                    title
            );


            item.addView(
                    details
            );


            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );


            params.setMargins(
                    0,
                    0,
                    0,
                    12
            );


            entriesContainer.addView(
                    item,
                    params
            );


            /*
             * לחיצה על אירוע =
             * עריכה.
             */

            item.setOnClickListener(
                    v -> openEventForEditing(
                            entry.id
                    )
            );
        }
    }


    /*
     * ============================================================
     * חלון "מה להוסיף?"
     * ============================================================
     */

    private void showAddDialog() {

        final Spinner spinner =
                new Spinner(this);


        String[] types = {

                "אירוע"
        };


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        types
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        spinner.setAdapter(
                adapter
        );


        LinearLayout container =
                new LinearLayout(this);


        container.setOrientation(
                LinearLayout.VERTICAL
        );


        int padding =
                (int) (
                        24 *
                                getResources()
                                        .getDisplayMetrics()
                                        .density
                );


        container.setPadding(
                padding,
                0,
                padding,
                0
        );


        TextView label =
                new TextView(this);


        label.setText(
                "מה תרצה להוסיף?"
        );


        label.setTextSize(16);


        container.addView(
                label
        );


        container.addView(
                spinner
        );


        new AlertDialog.Builder(this)
                .setTitle(
                        "הוספת נתון"
                )
                .setView(
                        container
                )
                .setNegativeButton(
                        "ביטול",
                        null
                )
                .setPositiveButton(
                        "המשך",
                        (dialog, which) -> {

                            String selected =
                                    spinner
                                            .getSelectedItem()
                                            .toString();


                            if ("אירוע".equals(
                                    selected
                            )) {

                                openNewEvent();
                            }
                        }
                )
                .show();
    }


    /*
     * ============================================================
     * אירוע חדש
     * ============================================================
     */

    private void openNewEvent() {

        Intent intent =
                new Intent(
                        this,
                        EventActivity.class
                );


        intent.putExtra(
                "gregorian_date",
                gregorianDate
        );


        intent.putExtra(
                "hebrew_date",
                hebrewDate
        );


        startActivity(
                intent
        );
    }


    /*
     * ============================================================
     * עריכת אירוע
     * ============================================================
     */

    private void openEventForEditing(
            String eventId) {

        Intent intent =
                new Intent(
                        this,
                        EventActivity.class
                );


        intent.putExtra(
                "event_id",
                eventId
        );


        startActivity(
                intent
        );
    }


    /*
     * ============================================================
     * בדיקה האם היום בתוך טווח
     * ============================================================
     */

    private boolean isDateInsideRange(
            String selectedDate,
            String startDate,
            String endDate) {

        Date selected =
                parseFirestoreDate(
                        selectedDate
                );


        Date start =
                parseFirestoreDate(
                        startDate
                );


        Date end =
                parseFirestoreDate(
                        endDate
                );


        if (selected == null ||
                start == null ||
                end == null) {

            return false;
        }


        return !selected.before(start)
                &&
                !selected.after(end);
    }


    /*
     * ============================================================
     * נרמול תאריך
     * ============================================================
     */

    private String normalizeDate(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return null;
        }


        if (value.matches(
                "\\d{4}-\\d{2}-\\d{2}"
        )) {

            return value;
        }


        try {

            SimpleDateFormat oldFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.US
                    );


            oldFormat.setLenient(false);


            Date date =
                    oldFormat.parse(
                            value
                    );


            if (date == null) {
                return null;
            }


            SimpleDateFormat newFormat =
                    new SimpleDateFormat(
                            FIRESTORE_DATE_FORMAT,
                            Locale.US
                    );


            return newFormat.format(
                    date
            );

        } catch (ParseException e) {

            return null;
        }
    }


    /*
     * ============================================================
     * Parse Firestore date
     * ============================================================
     */

    private Date parseFirestoreDate(
            String value) {

        try {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            FIRESTORE_DATE_FORMAT,
                            Locale.US
                    );


            format.setLenient(false);


            return format.parse(
                    value
            );

        } catch (Exception e) {

            return null;
        }
    }


    /*
     * ============================================================
     * הצגת תאריך
     * ============================================================
     */

    private String formatDateForDisplay(
            String value) {

        Date date =
                parseFirestoreDate(
                        value
                );


        if (date == null) {
            return value;
        }


        SimpleDateFormat format =
                new SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.US
                );


        return format.format(
                date
        );
    }


    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }


    /*
     * ============================================================
     * מודל אירוע
     * ============================================================
     */

    private static class CalendarEntry {

        String id;

        String title;

        String type;

        String startDate;

        String endDate;

        String startTime;

        String endTime;

        boolean allDay;

        String location;

        String description;
    }

}
