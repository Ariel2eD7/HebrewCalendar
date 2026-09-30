package com.israel.hebrewcalendar;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, i) -> {
            Insets x = i.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(x.left, x.top, x.right, x.bottom);
            return i;
        });

        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main, new HebrewCalendarFragment())
                    .commit();
        else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
