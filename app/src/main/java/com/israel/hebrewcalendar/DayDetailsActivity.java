package com.israel.hebrewcalendar;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class DayDetailsActivity extends AppCompatActivity {

    private Spinner typeSpinner;

    private EditText titleEditText;
    private EditText timeEditText;
    private EditText descriptionEditText;

    private Button saveButton;
    private Button cancelButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String gregorianDate;
    private String hebrewDate;

    private String editingDocumentId = null;

    private LinearLayout entriesContainer;

    private final List<CalendarEntry> entries =
            new ArrayList<>();


    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.day_details);


        mAuth =
                FirebaseAuth.getInstance();

        db =
                FirebaseFirestore.getInstance();


        typeSpinner =
                findViewById(R.id.typeSpinner);

        titleEditText =
                findViewById(R.id.titleEditText);

        timeEditText =
                findViewById(R.id.timeEditText);

        descriptionEditText =
                findViewById(R.id.descriptionEditText);

        saveButton =
                findViewById(R.id.saveButton);

        cancelButton =
                findViewById(R.id.cancelButton);


        gregorianDate =
                getIntent().getStringExtra(
                        "gregorian_date"
                );


        hebrewDate =
                getIntent().getStringExtra(
                        "hebrew_date"
                );


        /*
         * סוגי נתונים.
         *
         * אין יותר amount / הכנסה בתוך אירוע.
         * הכנסה היא סוג נפרד.
         */
        String[] types = {

                "אירוע",
                "הכנסה",
                "הוצאה",
                "הערה"
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


        typeSpinner.setAdapter(adapter);


        /*
         * מוצאים את ה־ViewGroup הראשי
         * כדי להוסיף אליו את רשימת האירועים.
         */
        View content =
                findViewById(android.R.id.content);


        if (content instanceof ViewGroup) {

            ViewGroup root =
                    (ViewGroup) content;


            entriesContainer =
                    new LinearLayout(this);


            entriesContainer.setOrientation(
                    LinearLayout.VERTICAL
            );


            entriesContainer.setPadding(
                    20,
                    20,
                    20,
                    20
            );


            TextView title =
                    new TextView(this);


            title.setText(
                    "הנתונים הקיימים ביום"
            );


            title.setTextSize(18);

            title.setTextColor(
                    Color.BLACK
            );

            title.setGravity(
                    Gravity.CENTER
            );


            entriesContainer.addView(
                    title,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );


            /*
             * הוספת הרשימה בתחתית המסך.
             */
            root.addView(
                    entriesContainer,
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }


        /*
         * ביטול.
         */
        cancelButton.setOnClickListener(
                v -> finish()
        );


        /*
         * שמירה.
         */
        saveButton.setOnClickListener(
                v -> saveDataToFirebase()
        );


        /*
         * טעינת הנתונים הקיימים.
         */
        loadEntries();
    }


    /*
     * ============================================================
     * מודל אירוע
     * ============================================================
     */
    private static class CalendarEntry {

        String id;

        String type;
        String title;
        String time;
        String description;
    }


    /*
     * ============================================================
     * טעינת כל הנתונים של היום
     * ============================================================
     */
    private void loadEntries() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();


        if (currentUser == null) {
            return;
        }


        String userId =
                currentUser.getUid();


        db.collection("users")
                .document(userId)
                .collection("calendarEntries")
                .whereEqualTo(
                        "gregorianDate",
                        gregorianDate
                )
                .get()
                .addOnSuccessListener(
                        querySnapshot -> {

                            entries.clear();


                            for (DocumentSnapshot document :
                                    querySnapshot.getDocuments()) {

                                CalendarEntry entry =
                                        new CalendarEntry();


                                entry.id =
                                        document.getId();


                                entry.type =
                                        document.getString(
                                                "type"
                                        );


                                entry.title =
                                        document.getString(
                                                "title"
                                        );


                                entry.time =
                                        document.getString(
                                                "time"
                                        );


                                entry.description =
                                        document.getString(
                                                "description"
                                        );


                                entries.add(entry);
                            }


                            displayEntries();
                        }
                )
                .addOnFailureListener(
                        e -> Toast.makeText(
                                this,
                                "טעינת הנתונים נכשלה: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        )
                );
    }


    /*
     * ============================================================
     * הצגת האירועים הקיימים
     * ============================================================
     */
    private void displayEntries() {

        if (entriesContainer == null) {
            return;
        }


        /*
         * שומרים את הכותרת הראשונה.
         */
        while (entriesContainer.getChildCount() > 1) {

            entriesContainer.removeViewAt(1);
        }


        for (CalendarEntry entry : entries) {

            LinearLayout item =
                    new LinearLayout(this);


            item.setOrientation(
                    LinearLayout.VERTICAL
            );


            item.setPadding(
                    20,
                    16,
                    20,
                    16
            );


            item.setBackgroundColor(
                    getTypeColor(entry.type)
            );


            TextView title =
                    new TextView(this);


            title.setText(
                    safe(entry.title)
            );


            title.setTextSize(17);

            title.setTextColor(
                    Color.BLACK
            );


            TextView details =
                    new TextView(this);


            StringBuilder text =
                    new StringBuilder();


            String typeName =
                    getTypeName(entry.type);


            text.append(typeName);


            if (entry.time != null &&
                    !entry.time.trim().isEmpty()) {

                text.append("  •  ");
                text.append(entry.time);
            }


            if (entry.description != null &&
                    !entry.description.trim().isEmpty()) {

                text.append("\n");
                text.append(entry.description);
            }


            details.setText(
                    text.toString()
            );


            details.setTextSize(14);

            details.setTextColor(
                    Color.DKGRAY
            );


            item.addView(
                    title,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );


            item.addView(
                    details,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );


            /*
             * לחיצה על אירוע קיים =
             * טעינה לטופס לעריכה.
             */
            item.setOnClickListener(
                    v -> loadEntryForEditing(entry)
            );


            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    );


            params.setMargins(
                    0,
                    8,
                    0,
                    8
            );


            entriesContainer.addView(
                    item,
                    params
            );
        }


        /*
         * אם אין נתונים.
         */
        if (entries.isEmpty()) {

            TextView empty =
                    new TextView(this);


            empty.setText(
                    "אין עדיין נתונים ביום הזה"
            );


            empty.setTextSize(15);

            empty.setTextColor(
                    Color.GRAY
            );


            empty.setGravity(
                    Gravity.CENTER
            );


            entriesContainer.addView(
                    empty,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }
    }


    /*
     * ============================================================
     * טעינת אירוע קיים לעריכה
     * ============================================================
     */
    private void loadEntryForEditing(
            CalendarEntry entry) {

        editingDocumentId =
                entry.id;


        titleEditText.setText(
                safe(entry.title)
        );


        timeEditText.setText(
                safe(entry.time)
        );


        descriptionEditText.setText(
                safe(entry.description)
        );


        int spinnerPosition =
                getTypeSpinnerPosition(
                        entry.type
                );


        if (spinnerPosition >= 0) {

            typeSpinner.setSelection(
                    spinnerPosition
            );
        }


        saveButton.setText(
                "עדכון"
        );


        Toast.makeText(
                this,
                "האירוע נטען לעריכה",
                Toast.LENGTH_SHORT
        ).show();
    }


    /*
     * ============================================================
     * שמירה / עדכון
     * ============================================================
     */
    private void saveDataToFirebase() {

        String title =
                titleEditText.getText()
                        .toString()
                        .trim();


        String time =
                timeEditText.getText()
                        .toString()
                        .trim();


        String description =
                descriptionEditText.getText()
                        .toString()
                        .trim();


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


        if (title.isEmpty()) {

            titleEditText.setError(
                    "נא להזין כותרת"
            );


            titleEditText.requestFocus();

            return;
        }


        String selectedType =
                typeSpinner
                        .getSelectedItem()
                        .toString();


        String type =
                convertType(
                        selectedType
                );


        String userId =
                currentUser.getUid();


        Map<String, Object> entry =
                new HashMap<>();


        entry.put(
                "gregorianDate",
                gregorianDate
        );


        entry.put(
                "hebrewDate",
                hebrewDate
        );


        entry.put(
                "type",
                type
        );


        entry.put(
                "title",
                title
        );


        entry.put(
                "time",
                time
        );


        entry.put(
                "description",
                description
        );


        if (editingDocumentId != null) {

            /*
             * עדכון אירוע קיים.
             */
            db.collection("users")
                    .document(userId)
                    .collection("calendarEntries")
                    .document(editingDocumentId)
                    .update(entry)
                    .addOnSuccessListener(
                            unused -> {

                                Toast.makeText(
                                        this,
                                        "הנתונים עודכנו בהצלחה",
                                        Toast.LENGTH_SHORT
                                ).show();


                                editingDocumentId =
                                        null;


                                saveButton.setText(
                                        "שמירה"
                                );


                                clearForm();


                                loadEntries();
                            }
                    )
                    .addOnFailureListener(
                            e -> Toast.makeText(
                                    this,
                                    "עדכון הנתונים נכשל: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            )
                    );


        } else {

            /*
             * יצירת אירוע חדש.
             */
            entry.put(
                    "createdAt",
                    FieldValue.serverTimestamp()
            );


            db.collection("users")
                    .document(userId)
                    .collection("calendarEntries")
                    .add(entry)
                    .addOnSuccessListener(
                            documentReference -> {

                                Toast.makeText(
                                        this,
                                        "הנתונים נשמרו בהצלחה",
                                        Toast.LENGTH_SHORT
                                ).show();


                                clearForm();

                                loadEntries();
                            }
                    )
                    .addOnFailureListener(
                            e -> Toast.makeText(
                                    this,
                                    "שמירת הנתונים נכשלה: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            )
                    );
        }
    }


    /*
     * ============================================================
     * המרת סוג
     * ============================================================
     */
    private String convertType(
            String selectedType) {

        switch (selectedType) {

            case "הכנסה":
                return "income";

            case "הוצאה":
                return "expense";

            case "הערה":
                return "note";

            default:
                return "event";
        }
    }


    /*
     * ============================================================
     * מיקום Spinner
     * ============================================================
     */
    private int getTypeSpinnerPosition(
            String type) {

        if ("income".equals(type)) {
            return 1;
        }


        if ("expense".equals(type)) {
            return 2;
        }


        if ("note".equals(type)) {
            return 3;
        }


        return 0;
    }


    /*
     * ============================================================
     * שם סוג בעברית
     * ============================================================
     */
    private String getTypeName(
            String type) {

        if ("income".equals(type)) {
            return "הכנסה";
        }


        if ("expense".equals(type)) {
            return "הוצאה";
        }


        if ("note".equals(type)) {
            return "הערה";
        }


        return "אירוע";
    }


    /*
     * ============================================================
     * צבע לפי סוג
     * ============================================================
     */
    private int getTypeColor(
            String type) {

        if ("income".equals(type)) {

            return Color.rgb(
                    220,
                    245,
                    220
            );
        }


        if ("expense".equals(type)) {

            return Color.rgb(
                    255,
                    225,
                    225
            );
        }


        if ("note".equals(type)) {

            return Color.rgb(
                    255,
                    245,
                    205
            );
        }


        return Color.rgb(
                220,
                235,
                255
        );
    }


    /*
     * ============================================================
     * ניקוי הטופס
     * ============================================================
     */
    private void clearForm() {

        editingDocumentId =
                null;


        titleEditText.setText("");

        timeEditText.setText("");

        descriptionEditText.setText("");


        typeSpinner.setSelection(0);


        saveButton.setText(
                "שמירה"
        );
    }


    private String safe(String value) {

        return value == null
                ? ""
                : value;
    }
}