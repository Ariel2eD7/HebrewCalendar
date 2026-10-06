package com.israel.hebrewcalendar;

import android.app.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.text.*;
import java.util.*;
import android.widget.*;


public class EventActivity extends AppCompatActivity {

    private EditText titleEditText, locationEditText, linkEditText, descriptionEditText;
    private Button startDateButton, startTimeButton, endDateButton, endTimeButton,
            saveButton, cancelButton, deleteButton;
    private CheckBox allDayCheckBox;
    private Spinner reminderSpinner;

    private Spinner recurrenceSpinner;
    private EditText recurrenceDayEditText;
    private LinearLayout monthlyRecurrenceLayout;
    private CheckBox noRecurrenceEndCheckBox;
    private Button recurrenceEndDateButton;

    private final Calendar recurrenceEndCalendar = Calendar.getInstance();


    private final Calendar startCalendar = Calendar.getInstance(), endCalendar = Calendar.getInstance();
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String gregorianDate, hebrewDate, eventId;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.event);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        titleEditText = findViewById(R.id.titleEditText);
        locationEditText = findViewById(R.id.locationEditText);
        linkEditText = findViewById(R.id.linkEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        startDateButton = findViewById(R.id.startDateButton);
        startTimeButton = findViewById(R.id.startTimeButton);
        endDateButton = findViewById(R.id.endDateButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        allDayCheckBox = findViewById(R.id.allDayCheckBox);
        reminderSpinner = findViewById(R.id.reminderSpinner);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        deleteButton = findViewById(R.id.deleteButton);

        recurrenceSpinner = findViewById(R.id.recurrenceSpinner);
        recurrenceDayEditText = findViewById(R.id.recurrenceDayEditText);
        monthlyRecurrenceLayout = findViewById(R.id.monthlyRecurrenceLayout);
        noRecurrenceEndCheckBox = findViewById(R.id.noRecurrenceEndCheckBox);
        recurrenceEndDateButton = findViewById(R.id.recurrenceEndDateButton);



        gregorianDate = getIntent().getStringExtra("gregorian_date");
        hebrewDate = getIntent().getStringExtra("hebrew_date");
        eventId = getIntent().getStringExtra("event_id");

        setupReminderSpinner();

        setupRecurrenceSpinner();


        initializeDates();
        updateDateButtons();
        updateTimeButtons();

        startDateButton.setOnClickListener(v -> showStartDatePicker());
        endDateButton.setOnClickListener(v -> showEndDatePicker());
        startTimeButton.setOnClickListener(v -> showStartTimePicker());
        endTimeButton.setOnClickListener(v -> showEndTimePicker());
        cancelButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveEvent());

        allDayCheckBox.setOnCheckedChangeListener((v, checked) -> {
            startTimeButton.setVisibility(checked ? View.GONE : View.VISIBLE);
            endTimeButton.setVisibility(checked ? View.GONE : View.VISIBLE);
        });

        boolean editing = eventId != null && !eventId.trim().isEmpty();
        saveButton.setText(editing ? "עדכון" : "שמירה");
        deleteButton.setVisibility(editing ? View.VISIBLE : View.GONE);

        if (editing) {
            deleteButton.setOnClickListener(v -> confirmDeleteEvent());
            loadEvent();
        }
    }

    private void setupRecurrenceSpinner() {
        String[] recurrenceOptions = {
                "ללא חזרה",
                "כל חודש"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                recurrenceOptions
        );

        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        recurrenceSpinner.setAdapter(adapter);
        recurrenceSpinner.setSelection(0);

        recurrenceSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        boolean monthly = position == 1;

                        monthlyRecurrenceLayout.setVisibility(
                                monthly ? View.VISIBLE : View.GONE
                        );
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

        noRecurrenceEndCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    recurrenceEndDateButton.setVisibility(
                            isChecked ? View.GONE : View.VISIBLE
                    );
                });

        recurrenceEndDateButton.setOnClickListener(
                v -> showRecurrenceEndDatePicker()
        );
    }



    private void showRecurrenceEndDatePicker() {

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {

                    recurrenceEndCalendar.set(
                            year,
                            month,
                            dayOfMonth
                    );

                    recurrenceEndDateButton.setText(
                            format(recurrenceEndCalendar, "dd/MM/yyyy")
                    );
                },
                recurrenceEndCalendar.get(Calendar.YEAR),
                recurrenceEndCalendar.get(Calendar.MONTH),
                recurrenceEndCalendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }



    private void confirmDeleteEvent() {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת אירוע")
                .setMessage("האם אתה בטוח שברצונך למחוק את האירוע?")
                .setNegativeButton("ביטול", null)
                .setPositiveButton("מחיקה", (d, w) -> deleteEvent())
                .show();
    }

    private void deleteEvent() {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            msg("אין משתמש מחובר");
            return;
        }

        if (eventId == null || eventId.trim().isEmpty()) {
            msg("לא נמצא מזהה אירוע");
            return;
        }

        deleteButton.setEnabled(false);
        saveButton.setEnabled(false);

        db.collection("users").document(user.getUid())
                .collection("calendarEntries").document(eventId).delete()
                .addOnSuccessListener(v -> {
                    msg("האירוע נמחק בהצלחה");
                    finish();
                })
                .addOnFailureListener(e -> {
                    deleteButton.setEnabled(true);
                    saveButton.setEnabled(true);
                    msg("מחיקת האירוע נכשלה: " + e.getMessage());
                });
    }

    private void initializeDates() {
        boolean ok = gregorianDate != null && !gregorianDate.trim().isEmpty()
                && setCalendarFromDate(startCalendar, gregorianDate);

        if (ok) endCalendar.setTime(startCalendar.getTime());
        else {
            startCalendar.setTimeInMillis(System.currentTimeMillis());
            endCalendar.setTime(startCalendar.getTime());
        }

        startCalendar.set(Calendar.HOUR_OF_DAY, 14);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        endCalendar.set(Calendar.HOUR_OF_DAY, 15);
        endCalendar.set(Calendar.MINUTE, 0);
        endCalendar.set(Calendar.SECOND, 0);
        endCalendar.set(Calendar.MILLISECOND, 0);

        recurrenceEndCalendar.setTime(startCalendar.getTime());

        recurrenceDayEditText.setText(
                String.valueOf(startCalendar.get(Calendar.DAY_OF_MONTH))
        );

    }

    private void setupReminderSpinner() {
        String[] reminders = {
                "ללא התראה", "5 דקות לפני", "10 דקות לפני", "15 דקות לפני",
                "30 דקות לפני", "שעה לפני", "יום לפני"
        };

        ArrayAdapter<String> a = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, reminders);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        reminderSpinner.setAdapter(a);
        reminderSpinner.setSelection(0);
    }

    private void showStartDatePicker() {
        new DatePickerDialog(this, (v, y, m, d) -> {
            startCalendar.set(y, m, d);
            if (dateOnly(endCalendar).before(dateOnly(startCalendar)))
                copyDate(endCalendar, startCalendar);
            updateDateButtons();
        }, startCalendar.get(Calendar.YEAR), startCalendar.get(Calendar.MONTH),
                startCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showEndDatePicker() {
        new DatePickerDialog(this, (v, y, m, d) -> {
            endCalendar.set(y, m, d);
            if (dateOnly(endCalendar).before(dateOnly(startCalendar))) {
                msg("תאריך הסיום לא יכול להיות לפני תאריך ההתחלה");
                copyDate(endCalendar, startCalendar);
            }
            updateDateButtons();
        }, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH),
                endCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showStartTimePicker() {
        new TimePickerDialog(this, (v, h, m) -> {
            startCalendar.set(Calendar.HOUR_OF_DAY, h);
            startCalendar.set(Calendar.MINUTE, m);
            startCalendar.set(Calendar.SECOND, 0);

            if (isSameDate(startCalendar, endCalendar)
                    && !endCalendar.after(startCalendar)) {
                endCalendar.setTime(startCalendar.getTime());
                endCalendar.add(Calendar.HOUR_OF_DAY, 1);
            }
            updateTimeButtons();
        }, startCalendar.get(Calendar.HOUR_OF_DAY),
                startCalendar.get(Calendar.MINUTE), true).show();
    }

    private void showEndTimePicker() {
        new TimePickerDialog(this, (v, h, m) -> {
            endCalendar.set(Calendar.HOUR_OF_DAY, h);
            endCalendar.set(Calendar.MINUTE, m);
            endCalendar.set(Calendar.SECOND, 0);
            updateTimeButtons();
        }, endCalendar.get(Calendar.HOUR_OF_DAY),
                endCalendar.get(Calendar.MINUTE), true).show();
    }

    private void saveEvent() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            msg("אין משתמש מחובר");
            return;
        }

        String title = titleEditText.getText().toString().trim();
        boolean allDay = allDayCheckBox.isChecked();

        if (title.isEmpty()) {
            titleEditText.setError("נא להזין כותרת");
            titleEditText.requestFocus();
            return;
        }

        if (dateOnly(endCalendar).before(dateOnly(startCalendar))) {
            msg("תאריך הסיום לא יכול להיות לפני תאריך ההתחלה");
            return;
        }

        if (!allDay && isSameDate(startCalendar, endCalendar)
                && !endCalendar.after(startCalendar)) {
            msg("שעת הסיום חייבת להיות אחרי שעת ההתחלה");
            return;
        }

        Map<String, Object> event = new HashMap<>();
        event.put("type", "event");
        event.put("title", title);
        event.put("startDate", format(startCalendar, "yyyy-MM-dd"));
        event.put("endDate", format(endCalendar, "yyyy-MM-dd"));
        event.put("startTime", allDay ? "" : format(startCalendar, "HH:mm"));
        event.put("endTime", allDay ? "" : format(endCalendar, "HH:mm"));
        event.put("allDay", allDay);
        event.put("location", locationEditText.getText().toString().trim());
        event.put("link", linkEditText.getText().toString().trim());
        event.put("reminder", reminderSpinner.getSelectedItem().toString());
        event.put("description", descriptionEditText.getText().toString().trim());
        event.put("hebrewDate", hebrewDate);

        String recurrenceType = "none";

        if (recurrenceSpinner.getSelectedItem() != null) {
            if ("כל חודש".equals(
                    recurrenceSpinner.getSelectedItem().toString())) {
                recurrenceType = "monthly";
            }
        }

        event.put("recurrenceType", recurrenceType);

        if ("monthly".equals(recurrenceType)) {

            String dayText =
                    recurrenceDayEditText.getText().toString().trim();

            if (dayText.isEmpty()) {
                recurrenceDayEditText.setError(
                        "נא להזין את היום בחודש"
                );
                recurrenceDayEditText.requestFocus();
                return;
            }

            int recurrenceDay;

            try {
                recurrenceDay = Integer.parseInt(dayText);
            } catch (NumberFormatException e) {
                recurrenceDayEditText.setError(
                        "יום לא תקין"
                );
                recurrenceDayEditText.requestFocus();
                return;
            }

            if (recurrenceDay < 1 || recurrenceDay > 31) {
                recurrenceDayEditText.setError(
                        "יש להזין יום בין 1 ל־31"
                );
                recurrenceDayEditText.requestFocus();
                return;
            }

            event.put("recurrenceDay", recurrenceDay);

            if (noRecurrenceEndCheckBox.isChecked()) {
                event.put("recurrenceEndDate", "");
            } else {

                if (dateOnly(recurrenceEndCalendar)
                        .before(dateOnly(startCalendar))) {

                    msg("תאריך סיום החזרה לא יכול להיות לפני תאריך ההתחלה");
                    return;
                }

                event.put(
                        "recurrenceEndDate",
                        format(recurrenceEndCalendar, "yyyy-MM-dd")
                );
            }

        } else {

            event.put("recurrenceDay", 0);
            event.put("recurrenceEndDate", "");
        }



        if (eventId != null && !eventId.trim().isEmpty())
            updateEvent(user.getUid(), event);
        else
            createEvent(user.getUid(), event);
    }

    private void createEvent(String userId, Map<String, Object> event) {
        event.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(userId).collection("calendarEntries").add(event)
                .addOnSuccessListener(v -> {
                    msg("האירוע נשמר בהצלחה");
                    finish();
                })
                .addOnFailureListener(e -> msg("שמירת האירוע נכשלה: " + e.getMessage()));
    }

    private void updateEvent(String userId, Map<String, Object> event) {
        db.collection("users").document(userId).collection("calendarEntries")
                .document(eventId).update(event)
                .addOnSuccessListener(v -> {
                    msg("האירוע עודכן בהצלחה");
                    finish();
                })
                .addOnFailureListener(e -> msg("עדכון האירוע נכשל: " + e.getMessage()));
    }

    private void loadEvent() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users").document(user.getUid()).collection("calendarEntries")
                .document(eventId).get()
                .addOnSuccessListener(this::fillFormFromDocument)
                .addOnFailureListener(e -> msg("טעינת האירוע נכשלה: " + e.getMessage()));
    }

    private void fillFormFromDocument(DocumentSnapshot d) {
        if (!d.exists()) {
            msg("האירוע לא נמצא");
            finish();
            return;
        }

        titleEditText.setText(safe(d.getString("title")));
        locationEditText.setText(safe(d.getString("location")));
        linkEditText.setText(safe(d.getString("link")));
        descriptionEditText.setText(safe(d.getString("description")));

        String start = d.getString("startDate");
        String end = d.getString("endDate");
        if (start == null) start = d.getString("gregorianDate");
        if (end == null) end = start;

        setCalendarFromDate(startCalendar, start);
        setCalendarFromDate(endCalendar, end);
        setTimeFromString(startCalendar, d.getString("startTime"));
        setTimeFromString(endCalendar, d.getString("endTime"));

        Boolean allDay = d.getBoolean("allDay");
        allDayCheckBox.setChecked(allDay != null && allDay);
        setReminderSpinner(d.getString("reminder"));

        loadRecurrenceFromDocument(d);

        updateDateButtons();
        updateTimeButtons();
    }


    private void loadRecurrenceFromDocument(DocumentSnapshot d) {

        String recurrenceType =
                d.getString("recurrenceType");

        if ("monthly".equals(recurrenceType)) {

            recurrenceSpinner.setSelection(1);

            Long day = d.getLong("recurrenceDay");

            if (day != null) {
                recurrenceDayEditText.setText(
                        String.valueOf(day)
                );
            } else {
                recurrenceDayEditText.setText(
                        String.valueOf(
                                startCalendar.get(Calendar.DAY_OF_MONTH)
                        )
                );
            }

            String endDate =
                    d.getString("recurrenceEndDate");

            if (endDate == null || endDate.trim().isEmpty()) {

                noRecurrenceEndCheckBox.setChecked(true);

            } else {

                noRecurrenceEndCheckBox.setChecked(false);

                if (setCalendarFromDate(
                        recurrenceEndCalendar,
                        endDate)) {

                    recurrenceEndDateButton.setText(
                            format(
                                    recurrenceEndCalendar,
                                    "dd/MM/yyyy"
                            )
                    );
                }
            }

        } else {

            recurrenceSpinner.setSelection(0);
            monthlyRecurrenceLayout.setVisibility(View.GONE);
        }
    }


    private void setReminderSpinner(String reminder) {
        if (reminder == null) return;

        for (int i = 0; i < reminderSpinner.getCount(); i++)
            if (reminder.equals(reminderSpinner.getItemAtPosition(i).toString())) {
                reminderSpinner.setSelection(i);
                return;
            }
    }

    private void updateDateButtons() {
        startDateButton.setText(format(startCalendar, "dd/MM/yyyy"));
        endDateButton.setText(format(endCalendar, "dd/MM/yyyy"));
    }

    private void updateTimeButtons() {
        startTimeButton.setText(format(startCalendar, "HH:mm"));
        endTimeButton.setText(format(endCalendar, "HH:mm"));
    }

    private boolean setCalendarFromDate(Calendar c, String value) {
        if (value == null || value.trim().isEmpty()) return false;

        Date d = parse(value, "yyyy-MM-dd");
        if (d == null) d = parse(value, "dd/MM/yyyy");
        if (d == null) return false;

        Calendar t = Calendar.getInstance();
        t.setTime(d);
        c.set(t.get(Calendar.YEAR), t.get(Calendar.MONTH), t.get(Calendar.DAY_OF_MONTH));
        return true;
    }

    private void setTimeFromString(Calendar c, String value) {
        if (value == null || value.trim().isEmpty()) return;

        Date d = parse(value, "HH:mm");
        if (d == null) return;

        Calendar t = Calendar.getInstance();
        t.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
        c.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
    }

    private Date parse(String value, String pattern) {
        SimpleDateFormat f = new SimpleDateFormat(pattern, Locale.US);
        f.setLenient(false);
        try {
            return f.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    private String format(Calendar c, String pattern) {
        return new SimpleDateFormat(pattern, Locale.US).format(c.getTime());
    }

    private Date dateOnly(Calendar c) {
        Calendar x = Calendar.getInstance();
        x.clear();
        x.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        return x.getTime();
    }

    private void copyDate(Calendar target, Calendar source) {
        target.set(source.get(Calendar.YEAR), source.get(Calendar.MONTH),
                source.get(Calendar.DAY_OF_MONTH));
    }

    private boolean isSameDate(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void msg(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
