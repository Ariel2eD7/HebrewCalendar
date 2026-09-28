package com.israel.hebrewcalendar;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return insets;
                }
        );

        // חיבור ל-Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        checkUser();
    }

    private void checkUser() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            // המשתמש כבר מחובר
            showCalendar();

        } else {

            // אין משתמש מחובר
            // עוברים למסך התחברות
            openLogin();
        }
    }

    private void openLogin() {

        Intent intent =
                new Intent(MainActivity.this, LoginActivity.class);

        startActivity(intent);

        // כדי שלא יהיה אפשר לחזור למסך הראשי
        // לפני התחברות
        finish();
    }

    private void showCalendar() {

        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (getSupportFragmentManager().findFragmentById(R.id.main) == null) {

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.main,
                            new HebrewCalendarFragment()
                    )
                    .commit();
        }
    }
}
