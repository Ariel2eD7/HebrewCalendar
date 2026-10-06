package com.israel.hebrewcalendar;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.text.*;
import java.util.*;

public class DayDetailsActivity extends AppCompatActivity {

    private TextView dayTitleTextView;
    private LinearLayout entriesContainer;
    private Button addButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String gregorianDate, hebrewDate;
    private final List<HebrewCalendarFragment.CalendarEntry> entries = new ArrayList<>();
    private static final String FIRESTORE_DATE_FORMAT = "yyyy-MM-dd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.day_details);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dayTitleTextView = findViewById(R.id.dayTitleTextView);
        entriesContainer = findViewById(R.id.entriesContainer);
        addButton = findViewById(R.id.addButton);
        gregorianDate = getIntent().getStringExtra("gregorian_date");
        hebrewDate = getIntent().getStringExtra("hebrew_date");

        updateDayTitle();
        addButton.setOnClickListener(v -> showAddDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEntries();
    }

    private void updateDayTitle() {
        dayTitleTextView.setText(gregorianDate == null
                ? "אירועי היום"
                : "אירועים ביום " + formatDateForDisplay(gregorianDate));
    }

    private void loadEntries() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            displayEntries();
            return;
        }

        db.collection("users").document(user.getUid())
                .collection("calendarEntries").get()
                .addOnSuccessListener(snapshot -> {
                    entries.clear();
                    String selectedDate = normalizeDate(gregorianDate);

                    if (selectedDate == null) {
                        displayEntries();
                        return;
                    }

                    for (DocumentSnapshot d : snapshot) {
                        String start = d.getString("startDate");
                        String end = d.getString("endDate");

                        if (start == null || start.trim().isEmpty()) {
                            start = normalizeDate(d.getString("gregorianDate"));
                            end = start;
                        }


                        String recurrenceType =
                                d.getString("recurrenceType");

                        boolean matches;

                        if ("monthly".equals(recurrenceType)) {

                            Long recurrenceDayValue =
                                    d.getLong("recurrenceDay");

                            int recurrenceDay =
                                    recurrenceDayValue == null
                                            ? getDayOfDate(start)
                                            : recurrenceDayValue.intValue();

                            String recurrenceEndDate =
                                    d.getString("recurrenceEndDate");

                            matches = isMonthlyOccurrence(
                                    selectedDate,
                                    start,
                                    recurrenceDay,
                                    recurrenceEndDate
                            );

                        } else {

                            matches =
                                    isDateInsideRange(
                                            selectedDate,
                                            start,
                                            end
                                    );
                        }

                        if (!matches) {
                            continue;
                        }


                        HebrewCalendarFragment.CalendarEntry e = new HebrewCalendarFragment.CalendarEntry();
                        e.id = d.getId();
                        e.title = d.getString("title");

                        e.startDate = selectedDate;
                        e.endDate = selectedDate;

                        e.startTime = d.getString("startTime");
                        e.endTime = d.getString("endTime");
                        Boolean allDay = d.getBoolean("allDay");
                        e.allDay = allDay != null && allDay;
                        e.location = d.getString("location");
                        e.description = d.getString("description");
                        entries.add(e);
                    }

                    displayEntries();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "טעינת האירועים נכשלה: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    private boolean isMonthlyOccurrence(
            String selectedDate,
            String startDate,
            int recurrenceDay,
            String recurrenceEndDate) {

        Date selected = parseFirestoreDate(selectedDate);
        Date start = parseFirestoreDate(startDate);

        if (selected == null || start == null) {
            return false;
        }

        if (selected.before(start)) {
            return false;
        }

        if (recurrenceEndDate != null &&
                !recurrenceEndDate.trim().isEmpty()) {

            Date end =
                    parseFirestoreDate(recurrenceEndDate);

            if (end != null && selected.after(end)) {
                return false;
            }
        }

        Calendar s = Calendar.getInstance();
        s.setTime(start);

        Calendar d = Calendar.getInstance();
        d.setTime(selected);

        int maxDay =
                d.getActualMaximum(Calendar.DAY_OF_MONTH);

        int actualDay =
                Math.min(recurrenceDay, maxDay);

        return d.get(Calendar.DAY_OF_MONTH) == actualDay;
    }


    private int getDayOfDate(String date) {

        Date d = parseFirestoreDate(date);

        if (d == null) {
            return 1;
        }

        Calendar c = Calendar.getInstance();
        c.setTime(d);

        return c.get(Calendar.DAY_OF_MONTH);
    }



    private void displayEntries() {
        entriesContainer.removeAllViews();

        if (entries.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("אין אירועים ביום הזה");
            empty.setTextSize(17);
            empty.setTextColor(Color.GRAY);
            empty.setGravity(android.view.Gravity.CENTER);
            entriesContainer.addView(empty);
            return;
        }

        for (HebrewCalendarFragment.CalendarEntry e : entries) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(20, 18, 20, 18);
            item.setBackgroundColor(Color.rgb(220, 235, 255));

            TextView title = new TextView(this);
            title.setText(safe(e.title));
            title.setTextSize(18);
            title.setTextColor(Color.BLACK);
            title.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView details = new TextView(this);
            StringBuilder text = new StringBuilder();

            if (e.allDay) {
                text.append("כל היום");
            } else if (e.startTime != null && !e.startTime.isEmpty()) {
                text.append(e.startTime);
                if (e.endTime != null && !e.endTime.isEmpty())
                    text.append(" → ").append(e.endTime);
            }

            if (e.startDate != null && e.endDate != null &&
                    !e.startDate.equals(e.endDate)) {
                if (text.length() > 0) text.append("\n");
                text.append(formatDateForDisplay(e.startDate))
                        .append(" → ")
                        .append(formatDateForDisplay(e.endDate));
            }

            if (e.location != null && !e.location.trim().isEmpty())
                text.append("\n📍 ").append(e.location);

            if (e.description != null && !e.description.trim().isEmpty())
                text.append("\n").append(e.description);

            details.setText(text);
            details.setTextSize(14);
            details.setTextColor(Color.DKGRAY);

            item.addView(title);
            item.addView(details);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 12);
            entriesContainer.addView(item, params);

            item.setOnClickListener(v -> openEventForEditing(e.id));
        }
    }

    private void showAddDialog() {
        Spinner spinner = new Spinner(this);
        String[] types = {"אירוע"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, types);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources()
                .getDisplayMetrics().density);
        container.setPadding(padding, 0, padding, 0);

        TextView label = new TextView(this);
        label.setText("מה תרצה להוסיף?");
        label.setTextSize(16);
        container.addView(label);
        container.addView(spinner);

        new AlertDialog.Builder(this)
                .setTitle("הוספת נתון")
                .setView(container)
                .setNegativeButton("ביטול", null)
                .setPositiveButton("המשך", (dialog, which) -> {
                    if ("אירוע".equals(spinner.getSelectedItem().toString()))
                        openNewEvent();
                })
                .show();
    }

    private void openNewEvent() {
        Intent intent = new Intent(this, EventActivity.class);
        intent.putExtra("gregorian_date", gregorianDate);
        intent.putExtra("hebrew_date", hebrewDate);
        startActivity(intent);
    }

    private void openEventForEditing(String eventId) {
        Intent intent = new Intent(this, EventActivity.class);
        intent.putExtra("event_id", eventId);
        startActivity(intent);
    }

    private boolean isDateInsideRange(String selected, String start, String end) {
        Date s = parseFirestoreDate(selected);
        Date a = parseFirestoreDate(start);
        Date b = parseFirestoreDate(end);
        return s != null && a != null && b != null &&
                !s.before(a) && !s.after(b);
    }

    private String normalizeDate(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) return value;

        try {
            SimpleDateFormat oldFormat =
                    new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            oldFormat.setLenient(false);
            Date date = oldFormat.parse(value);
            return date == null ? null :
                    new SimpleDateFormat(FIRESTORE_DATE_FORMAT, Locale.US)
                            .format(date);
        } catch (ParseException e) {
            return null;
        }
    }

    private Date parseFirestoreDate(String value) {
        try {
            SimpleDateFormat format =
                    new SimpleDateFormat(FIRESTORE_DATE_FORMAT, Locale.US);
            format.setLenient(false);
            return format.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDateForDisplay(String value) {
        Date date = parseFirestoreDate(value);
        return date == null ? value :
                new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(date);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}