package com.israel.hebrewcalendar;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EventActivity extends AppCompatActivity {

    private EditText titleEditText;
    private EditText locationEditText;
    private EditText linkEditText;
    private EditText descriptionEditText;

    private Button startDateButton;
    private Button startTimeButton;
    private Button endDateButton;
    private Button endTimeButton;

    private CheckBox allDayCheckBox;

    private Spinner reminderSpinner;

    private Button saveButton;
    private Button cancelButton;
    private Button deleteButton;

    private final Calendar startCalendar =
            Calendar.getInstance();

    private final Calendar endCalendar =
            Calendar.getInstance();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String gregorianDate;
    private String hebrewDate;

    private String eventId;

    private static final String FIRESTORE_DATE_FORMAT =
            "yyyy-MM-dd";

    private static final String DISPLAY_DATE_FORMAT =
            "dd/MM/yyyy";

    private static final String TIME_FORMAT =
            "HH:mm";


    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.event);


        mAuth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();


        titleEditText =
                findViewById(R.id.titleEditText);

        locationEditText =
                findViewById(R.id.locationEditText);

        linkEditText =
                findViewById(R.id.linkEditText);

        descriptionEditText =
                findViewById(R.id.descriptionEditText);

        startDateButton =
                findViewById(R.id.startDateButton);

        startTimeButton =
                findViewById(R.id.startTimeButton);

        endDateButton =
                findViewById(R.id.endDateButton);

        endTimeButton =
                findViewById(R.id.endTimeButton);

        allDayCheckBox =
                findViewById(R.id.allDayCheckBox);

        reminderSpinner =
                findViewById(R.id.reminderSpinner);

        saveButton =
                findViewById(R.id.saveButton);

        cancelButton =
                findViewById(R.id.cancelButton);

        deleteButton =
                findViewById(R.id.deleteButton);


        gregorianDate =
                getIntent().getStringExtra(
                        "gregorian_date"
                );

        hebrewDate =
                getIntent().getStringExtra(
                        "hebrew_date"
                );

        eventId =
                getIntent().getStringExtra(
                        "event_id"
                );


        setupReminderSpinner();

        initializeDates();

        updateDateButtons();

        updateTimeButtons();


        startDateButton.setOnClickListener(
                v -> showStartDatePicker()
        );

        endDateButton.setOnClickListener(
                v -> showEndDatePicker()
        );

        startTimeButton.setOnClickListener(
                v -> showStartTimePicker()
        );

        endTimeButton.setOnClickListener(
                v -> showEndTimePicker()
        );


        allDayCheckBox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    startTimeButton.setVisibility(
                            isChecked
                                    ? View.GONE
                                    : View.VISIBLE
                    );

                    endTimeButton.setVisibility(
                            isChecked
                                    ? View.GONE
                                    : View.VISIBLE
                    );
                }
        );


        cancelButton.setOnClickListener(
                v -> finish()
        );


        saveButton.setOnClickListener(
                v -> saveEvent()
        );


        /*
         * אם eventId קיים,
         * אנחנו במצב עריכה ולכן מציגים
         * את כפתור המחיקה.
         */
        if (eventId != null &&
                !eventId.trim().isEmpty()) {

            saveButton.setText(
                    "עדכון"
            );

            deleteButton.setVisibility(
                    View.VISIBLE
            );

            deleteButton.setOnClickListener(
                    v -> confirmDeleteEvent()
            );

            loadEvent();

        } else {

            saveButton.setText(
                    "שמירה"
            );

            /*
             * ביצירת אירוע חדש אין מה למחוק.
             */
            deleteButton.setVisibility(
                    View.GONE
            );
        }
    }


    /*
     * ============================================================
     * אישור מחיקת אירוע
     * ============================================================
     */

    private void confirmDeleteEvent() {

        new AlertDialog.Builder(this)
                .setTitle("מחיקת אירוע")
                .setMessage(
                        "האם אתה בטוח שברצונך למחוק את האירוע?"
                )
                .setNegativeButton(
                        "ביטול",
                        null
                )
                .setPositiveButton(
                        "מחיקה",
                        (dialog, which) ->
                                deleteEvent()
                )
                .show();
    }


    /*
     * ============================================================
     * מחיקת אירוע מ-Firestore
     * ============================================================
     */

    private void deleteEvent() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "אין משתמש מחובר",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        if (eventId == null ||
                eventId.trim().isEmpty()) {

            Toast.makeText(
                    this,
                    "לא נמצא מזהה אירוע",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        String userId =
                currentUser.getUid();


        /*
         * מונעים לחיצות נוספות בזמן המחיקה.
         */
        deleteButton.setEnabled(false);
        saveButton.setEnabled(false);


        db.collection("users")
                .document(userId)
                .collection("calendarEntries")
                .document(eventId)
                .delete()
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "האירוע נמחק בהצלחה",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            deleteButton.setEnabled(true);
                            saveButton.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    "מחיקת האירוע נכשלה: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    /*
     * ============================================================
     * אתחול תאריכים
     * ============================================================
     */

    private void initializeDates() {

        boolean initialized = false;


        if (gregorianDate != null &&
                !gregorianDate.trim().isEmpty()) {

            initialized =
                    setCalendarFromDate(
                            startCalendar,
                            gregorianDate
                    );

            if (initialized) {

                endCalendar.setTime(
                        startCalendar.getTime()
                );
            }
        }


        if (!initialized) {

            Calendar now =
                    Calendar.getInstance();

            startCalendar.setTime(
                    now.getTime()
            );

            endCalendar.setTime(
                    now.getTime()
            );
        }


        startCalendar.set(
                Calendar.HOUR_OF_DAY,
                14
        );

        startCalendar.set(
                Calendar.MINUTE,
                0
        );

        startCalendar.set(
                Calendar.SECOND,
                0
        );

        startCalendar.set(
                Calendar.MILLISECOND,
                0
        );


        endCalendar.set(
                Calendar.HOUR_OF_DAY,
                15
        );

        endCalendar.set(
                Calendar.MINUTE,
                0
        );

        endCalendar.set(
                Calendar.SECOND,
                0
        );

        endCalendar.set(
                Calendar.MILLISECOND,
                0
        );
    }


    /*
     * ============================================================
     * Spinner התראות
     * ============================================================
     */

    private void setupReminderSpinner() {

        String[] reminders = {

                "ללא התראה",
                "5 דקות לפני",
                "10 דקות לפני",
                "15 דקות לפני",
                "30 דקות לפני",
                "שעה לפני",
                "יום לפני"
        };


        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        reminders
                );


        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );


        reminderSpinner.setAdapter(adapter);

        reminderSpinner.setSelection(0);
    }


    /*
     * ============================================================
     * בחירת תאריך התחלה
     * ============================================================
     */

    private void showStartDatePicker() {

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {

                            startCalendar.set(
                                    Calendar.YEAR,
                                    year
                            );

                            startCalendar.set(
                                    Calendar.MONTH,
                                    month
                            );

                            startCalendar.set(
                                    Calendar.DAY_OF_MONTH,
                                    day
                            );


                            if (dateOnly(endCalendar)
                                    .before(
                                            dateOnly(startCalendar)
                                    )) {

                                copyDate(
                                        endCalendar,
                                        startCalendar
                                );
                            }


                            updateDateButtons();
                        },
                        startCalendar.get(
                                Calendar.YEAR
                        ),
                        startCalendar.get(
                                Calendar.MONTH
                        ),
                        startCalendar.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        dialog.show();
    }


    /*
     * ============================================================
     * בחירת תאריך סיום
     * ============================================================
     */

    private void showEndDatePicker() {

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {

                            endCalendar.set(
                                    Calendar.YEAR,
                                    year
                            );

                            endCalendar.set(
                                    Calendar.MONTH,
                                    month
                            );

                            endCalendar.set(
                                    Calendar.DAY_OF_MONTH,
                                    day
                            );


                            if (dateOnly(endCalendar)
                                    .before(
                                            dateOnly(startCalendar)
                                    )) {

                                Toast.makeText(
                                        this,
                                        "תאריך הסיום לא יכול להיות לפני תאריך ההתחלה",
                                        Toast.LENGTH_SHORT
                                ).show();

                                copyDate(
                                        endCalendar,
                                        startCalendar
                                );
                            }


                            updateDateButtons();
                        },
                        endCalendar.get(
                                Calendar.YEAR
                        ),
                        endCalendar.get(
                                Calendar.MONTH
                        ),
                        endCalendar.get(
                                Calendar.DAY_OF_MONTH
                        )
                );

        dialog.show();
    }


    /*
     * ============================================================
     * בחירת שעת התחלה
     * ============================================================
     */

    private void showStartTimePicker() {

        TimePickerDialog dialog =
                new TimePickerDialog(
                        this,
                        (view, hour, minute) -> {

                            startCalendar.set(
                                    Calendar.HOUR_OF_DAY,
                                    hour
                            );

                            startCalendar.set(
                                    Calendar.MINUTE,
                                    minute
                            );

                            startCalendar.set(
                                    Calendar.SECOND,
                                    0
                            );


                            if (isSameDate(
                                    startCalendar,
                                    endCalendar
                            ) &&
                                    !endCalendar.after(
                                            startCalendar
                                    )) {

                                endCalendar.setTime(
                                        startCalendar.getTime()
                                );

                                endCalendar.add(
                                        Calendar.HOUR_OF_DAY,
                                        1
                                );
                            }


                            updateTimeButtons();
                        },
                        startCalendar.get(
                                Calendar.HOUR_OF_DAY
                        ),
                        startCalendar.get(
                                Calendar.MINUTE
                        ),
                        true
                );

        dialog.show();
    }


    /*
     * ============================================================
     * בחירת שעת סיום
     * ============================================================
     */

    private void showEndTimePicker() {

        TimePickerDialog dialog =
                new TimePickerDialog(
                        this,
                        (view, hour, minute) -> {

                            endCalendar.set(
                                    Calendar.HOUR_OF_DAY,
                                    hour
                            );

                            endCalendar.set(
                                    Calendar.MINUTE,
                                    minute
                            );

                            endCalendar.set(
                                    Calendar.SECOND,
                                    0
                            );

                            updateTimeButtons();
                        },
                        endCalendar.get(
                                Calendar.HOUR_OF_DAY
                        ),
                        endCalendar.get(
                                Calendar.MINUTE
                        ),
                        true
                );

        dialog.show();
    }


    /*
     * ============================================================
     * שמירת אירוע
     * ============================================================
     */

    private void saveEvent() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "אין משתמש מחובר",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        String title =
                titleEditText
                        .getText()
                        .toString()
                        .trim();

        String location =
                locationEditText
                        .getText()
                        .toString()
                        .trim();

        String link =
                linkEditText
                        .getText()
                        .toString()
                        .trim();

        String description =
                descriptionEditText
                        .getText()
                        .toString()
                        .trim();

        boolean allDay =
                allDayCheckBox.isChecked();


        if (title.isEmpty()) {

            titleEditText.setError(
                    "נא להזין כותרת"
            );

            titleEditText.requestFocus();

            return;
        }


        if (dateOnly(endCalendar)
                .before(
                        dateOnly(startCalendar)
                )) {

            Toast.makeText(
                    this,
                    "תאריך הסיום לא יכול להיות לפני תאריך ההתחלה",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        if (!allDay &&
                isSameDate(
                        startCalendar,
                        endCalendar
                ) &&
                !endCalendar.after(
                        startCalendar
                )) {

            Toast.makeText(
                    this,
                    "שעת הסיום חייבת להיות אחרי שעת ההתחלה",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }


        String startDate =
                formatFirestoreDate(
                        startCalendar
                );

        String endDate =
                formatFirestoreDate(
                        endCalendar
                );


        String startTime =
                allDay
                        ? ""
                        : formatTime(
                        startCalendar
                );

        String endTime =
                allDay
                        ? ""
                        : formatTime(
                        endCalendar
                );


        String reminder =
                reminderSpinner
                        .getSelectedItem()
                        .toString();


        Map<String, Object> event =
                new HashMap<>();


        event.put(
                "type",
                "event"
        );

        event.put(
                "title",
                title
        );

        event.put(
                "startDate",
                startDate
        );

        event.put(
                "endDate",
                endDate
        );

        event.put(
                "startTime",
                startTime
        );

        event.put(
                "endTime",
                endTime
        );

        event.put(
                "allDay",
                allDay
        );

        event.put(
                "location",
                location
        );

        event.put(
                "link",
                link
        );

        event.put(
                "reminder",
                reminder
        );

        event.put(
                "description",
                description
        );

        event.put(
                "hebrewDate",
                hebrewDate
        );


        String userId =
                currentUser.getUid();


        if (eventId != null &&
                !eventId.trim().isEmpty()) {

            updateEvent(
                    userId,
                    event
            );

        } else {

            createEvent(
                    userId,
                    event
            );
        }
    }


    /*
     * ============================================================
     * יצירת אירוע
     * ============================================================
     */

    private void createEvent(
            String userId,
            Map<String, Object> event) {

        event.put(
                "createdAt",
                FieldValue.serverTimestamp()
        );


        db.collection("users")
                .document(userId)
                .collection("calendarEntries")
                .add(event)
                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    this,
                                    "האירוע נשמר בהצלחה",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "שמירת האירוע נכשלה: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        )
                );
    }


    /*
     * ============================================================
     * עדכון אירוע
     * ============================================================
     */

    private void updateEvent(
            String userId,
            Map<String, Object> event) {

        db.collection("users")
                .document(userId)
                .collection("calendarEntries")
                .document(eventId)
                .update(event)
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "האירוע עודכן בהצלחה",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "עדכון האירוע נכשל: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        )
                );
    }


    /*
     * ============================================================
     * טעינת אירוע לעריכה
     * ============================================================
     */

    private void loadEvent() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        if (currentUser == null) {
            return;
        }


        db.collection("users")
                .document(
                        currentUser.getUid()
                )
                .collection("calendarEntries")
                .document(eventId)
                .get()
                .addOnSuccessListener(
                        this::fillFormFromDocument
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "טעינת האירוע נכשלה: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        )
                );
    }


    /*
     * ============================================================
     * מילוי הטופס
     * ============================================================
     */

    private void fillFormFromDocument(
            DocumentSnapshot document) {

        if (!document.exists()) {

            Toast.makeText(
                    this,
                    "האירוע לא נמצא",
                    Toast.LENGTH_LONG
            ).show();

            finish();

            return;
        }


        titleEditText.setText(
                safe(
                        document.getString(
                                "title"
                        )
                )
        );


        locationEditText.setText(
                safe(
                        document.getString(
                                "location"
                        )
                )
        );


        linkEditText.setText(
                safe(
                        document.getString(
                                "link"
                        )
                )
        );


        descriptionEditText.setText(
                safe(
                        document.getString(
                                "description"
                        )
                )
        );


        String startDate =
                document.getString(
                        "startDate"
                );

        String endDate =
                document.getString(
                        "endDate"
                );


        if (startDate == null) {

            startDate =
                    document.getString(
                            "gregorianDate"
                    );
        }

        if (endDate == null) {
            endDate = startDate;
        }


        setCalendarFromDate(
                startCalendar,
                startDate
        );

        setCalendarFromDate(
                endCalendar,
                endDate
        );


        setTimeFromString(
                startCalendar,
                document.getString(
                        "startTime"
                )
        );

        setTimeFromString(
                endCalendar,
                document.getString(
                        "endTime"
                )
        );


        Boolean allDay =
                document.getBoolean(
                        "allDay"
                );


        allDayCheckBox.setChecked(
                allDay != null &&
                        allDay
        );


        setReminderSpinner(
                document.getString(
                        "reminder"
                )
        );


        updateDateButtons();

        updateTimeButtons();
    }


    /*
     * ============================================================
     * התראה
     * ============================================================
     */

    private void setReminderSpinner(
            String reminder) {

        if (reminder == null) {
            return;
        }


        for (int i = 0;
             i < reminderSpinner.getCount();
             i++) {

            Object item =
                    reminderSpinner
                            .getItemAtPosition(i);


            if (item != null &&
                    reminder.equals(
                            item.toString()
                    )) {

                reminderSpinner.setSelection(i);

                return;
            }
        }
    }


    /*
     * ============================================================
     * עדכון תצוגה
     * ============================================================
     */

    private void updateDateButtons() {

        startDateButton.setText(
                formatDisplayDate(
                        startCalendar
                )
        );

        endDateButton.setText(
                formatDisplayDate(
                        endCalendar
                )
        );
    }


    private void updateTimeButtons() {

        startTimeButton.setText(
                formatTime(
                        startCalendar
                )
        );

        endTimeButton.setText(
                formatTime(
                        endCalendar
                )
        );
    }


    /*
     * ============================================================
     * תאריכים
     * ============================================================
     */

    private boolean setCalendarFromDate(
            Calendar calendar,
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return false;
        }


        Date date =
                parseDate(
                        value,
                        FIRESTORE_DATE_FORMAT
                );


        if (date == null) {

            date =
                    parseDate(
                            value,
                            DISPLAY_DATE_FORMAT
                    );
        }


        if (date == null) {
            return false;
        }


        Calendar temp =
                Calendar.getInstance();

        temp.setTime(date);


        calendar.set(
                Calendar.YEAR,
                temp.get(Calendar.YEAR)
        );

        calendar.set(
                Calendar.MONTH,
                temp.get(Calendar.MONTH)
        );

        calendar.set(
                Calendar.DAY_OF_MONTH,
                temp.get(Calendar.DAY_OF_MONTH)
        );


        return true;
    }


    private Date parseDate(
            String value,
            String pattern) {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        pattern,
                        Locale.US
                );

        format.setLenient(false);


        try {

            return format.parse(value);

        } catch (ParseException e) {

            return null;
        }
    }


    private void setTimeFromString(
            Calendar calendar,
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return;
        }


        Date date =
                parseDate(
                        value,
                        TIME_FORMAT
                );


        if (date == null) {
            return;
        }


        Calendar temp =
                Calendar.getInstance();

        temp.setTime(date);


        calendar.set(
                Calendar.HOUR_OF_DAY,
                temp.get(Calendar.HOUR_OF_DAY)
        );

        calendar.set(
                Calendar.MINUTE,
                temp.get(Calendar.MINUTE)
        );
    }


    private Date dateOnly(
            Calendar calendar) {

        Calendar copy =
                Calendar.getInstance();

        copy.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
        );

        copy.set(
                Calendar.MILLISECOND,
                0
        );

        return copy.getTime();
    }


    private void copyDate(
            Calendar target,
            Calendar source) {

        target.set(
                Calendar.YEAR,
                source.get(Calendar.YEAR)
        );

        target.set(
                Calendar.MONTH,
                source.get(Calendar.MONTH)
        );

        target.set(
                Calendar.DAY_OF_MONTH,
                source.get(Calendar.DAY_OF_MONTH)
        );
    }


    private boolean isSameDate(
            Calendar first,
            Calendar second) {

        return first.get(Calendar.YEAR)
                ==
                second.get(Calendar.YEAR)
                &&
                first.get(Calendar.DAY_OF_YEAR)
                        ==
                        second.get(Calendar.DAY_OF_YEAR);
    }


    /*
     * ============================================================
     * פורמטים
     * ============================================================
     */

    private String formatFirestoreDate(
            Calendar calendar) {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        FIRESTORE_DATE_FORMAT,
                        Locale.US
                );

        return format.format(
                calendar.getTime()
        );
    }


    private String formatDisplayDate(
            Calendar calendar) {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        DISPLAY_DATE_FORMAT,
                        Locale.US
                );

        return format.format(
                calendar.getTime()
        );
    }


    private String formatTime(
            Calendar calendar) {

        SimpleDateFormat format =
                new SimpleDateFormat(
                        TIME_FORMAT,
                        Locale.US
                );

        return format.format(
                calendar.getTime()
        );
    }


    private String safe(
            String value) {

        return value == null
                ? ""
                : value;
    }

}
