package com.israel.hebrewcalendar;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class DayDetailsActivity extends AppCompatActivity {

    private TextView gregorianDateText;
    private TextView hebrewDateText;

    private EditText titleEditText;
    private EditText descriptionEditText;

    private Button saveButton;
    private Button cancelButton;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String gregorianDate;
    private String hebrewDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.day_details);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // חיבור רכיבי ה-XML
        gregorianDateText = findViewById(R.id.gregorianDateText);
        hebrewDateText = findViewById(R.id.hebrewDateText);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);

        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        // קבלת הנתונים שנשלחו מהלוח
        gregorianDate =
                getIntent().getStringExtra("gregorian_date");

        hebrewDate =
                getIntent().getStringExtra("hebrew_date");

        // הצגת התאריך הלועזי
        if (gregorianDate != null && !gregorianDate.isEmpty()) {
            gregorianDateText.setText(gregorianDate);
        }

        // הצגת התאריך העברי
        if (hebrewDate != null && !hebrewDate.isEmpty()) {
            hebrewDateText.setText(hebrewDate);
        }

        // כפתור ביטול
        cancelButton.setOnClickListener(v -> {
            finish();
        });

        // כפתור שמירה
        saveButton.setOnClickListener(v -> {
            saveDataToFirebase();
        });
    }

    private void saveDataToFirebase() {

        String title = titleEditText.getText()
                .toString()
                .trim();

        String description = descriptionEditText.getText()
                .toString()
                .trim();

        // בדיקה שיש משתמש מחובר
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "אין משתמש מחובר",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // בדיקה שהמשתמש הזין כותרת
        if (title.isEmpty()) {
            titleEditText.setError("נא להזין כותרת");
            titleEditText.requestFocus();
            return;
        }

        String userId = currentUser.getUid();

        /*
         * יצירת מסמך חדש.
         *
         * Firestore יוצר ID ייחודי אוטומטית.
         */
        String entryId = db.collection("users")
                .document(userId)
                .collection("calendarEntries")
                .document()
                .getId();

        // יצירת הנתונים
        Map<String, Object> entry = new HashMap<>();

        entry.put("gregorianDate", gregorianDate);
        entry.put("hebrewDate", hebrewDate);
        entry.put("title", title);
        entry.put("description", description);
        entry.put("createdAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        // שמירת הנתונים
        db.collection("users")
                .document(userId)
                .collection("calendarEntries")
                .document(entryId)
                .set(entry)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "הנתונים נשמרו בהצלחה",
                            Toast.LENGTH_SHORT
                    ).show();

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "שמירת הנתונים נכשלה: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

}
