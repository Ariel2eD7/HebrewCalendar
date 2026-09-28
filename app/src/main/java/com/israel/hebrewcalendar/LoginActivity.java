package com.israel.hebrewcalendar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        Button loginButton = findViewById(R.id.loginButton);
        Button registerButton = findViewById(R.id.registerButton);
        Button googleButton = findViewById(R.id.googleButton);


        loginButton.setOnClickListener(v -> loginWithEmail());

        registerButton.setOnClickListener(v -> registerWithEmail());

        googleButton.setOnClickListener(v -> loginWithGoogle());
    }

    private void loginWithEmail() {

        String email = getEmail();
        String password = getPassword();

        if (email.isEmpty()) {
            showMessage("נא להזין כתובת אימייל");
            return;
        }

        if (password.isEmpty()) {
            showMessage("נא להזין סיסמה");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        showMessage("התחברת בהצלחה");

                        openCalendar();

                    } else {

                        showMessage(
                                "ההתחברות נכשלה: " +
                                        getFirebaseError(task.getException())
                        );
                    }
                });
    }

    private void registerWithEmail() {

        String email = getEmail();
        String password = getPassword();

        if (email.isEmpty()) {
            showMessage("נא להזין כתובת אימייל");
            return;
        }

        if (password.isEmpty()) {
            showMessage("נא להזין סיסמה");
            return;
        }

        if (password.length() < 6) {
            showMessage("הסיסמה חייבת להכיל לפחות 6 תווים");
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        showMessage("החשבון נוצר בהצלחה");

                        openCalendar();

                    } else {

                        showMessage(
                                "יצירת החשבון נכשלה: " +
                                        getFirebaseError(task.getException())
                        );
                    }
                });
    }

    private void loginWithGoogle() {

        /*
         * את Google Sign-In נחבר בשלב הבא.
         */
        showMessage("Google Sign-In יחובר בשלב הבא");
    }

    private String getEmail() {

        android.widget.EditText emailEditText =
                findViewById(R.id.emailEditText);

        String email = emailEditText.getText().toString().trim();

        return email;
    }

    private String getPassword() {

        android.widget.EditText passwordEditText =
                findViewById(R.id.passwordEditText);

        String password =
                passwordEditText.getText().toString();

        return password;
    }

    private String getFirebaseError(Exception exception) {

        if (exception == null) {
            return "שגיאה לא ידועה";
        }

        String message = exception.getMessage();

        if (message == null || message.isEmpty()) {
            return "שגיאה לא ידועה";
        }

        return message;
    }

    private void showMessage(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void openCalendar() {

        Intent intent =
                new Intent(LoginActivity.this, MainActivity.class);

        startActivity(intent);

        finish();
    }
}
