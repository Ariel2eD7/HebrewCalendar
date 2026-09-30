package com.israel.hebrewcalendar;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.*;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        Button login = findViewById(R.id.loginButton);
        Button register = findViewById(R.id.registerButton);
        Button google = findViewById(R.id.googleButton);

        login.setOnClickListener(v -> login(false));
        register.setOnClickListener(v -> login(true));
        google.setOnClickListener(v -> loginWithGoogle());
    }

    private void login(boolean register) {
        String email = ((EditText) findViewById(R.id.emailEditText))
                .getText().toString().trim();
        String password = ((EditText) findViewById(R.id.passwordEditText))
                .getText().toString();

        if (email.isEmpty()) {
            showMessage("נא להזין כתובת אימייל");
            return;
        }

        if (password.isEmpty()) {
            showMessage("נא להזין סיסמה");
            return;
        }

        if (register && password.length() < 6) {
            showMessage("הסיסמה חייבת להכיל לפחות 6 תווים");
            return;
        }

        (register
                ? mAuth.createUserWithEmailAndPassword(email, password)
                : mAuth.signInWithEmailAndPassword(email, password))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        showMessage(register ? "החשבון נוצר בהצלחה" : "התחברת בהצלחה");
                        openCalendar();
                    } else {
                        showMessage(
                                (register ? "יצירת החשבון נכשלה: " : "ההתחברות נכשלה: ")
                                        + getFirebaseError(task.getException()));
                    }
                });
    }

    private void loginWithGoogle() {
        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build();

        credentialManager.getCredentialAsync(
                this, request, new android.os.CancellationSignal(),
                getMainExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredential(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        showMessage("התחברות עם Google נכשלה: " + e.getMessage());
                    }
                });
    }

    private void handleGoogleCredential(Credential credential) {
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(credential.getType())) {
            showMessage("פרטי Google לא תקינים");
            return;
        }

        try {
            String token = GoogleIdTokenCredential
                    .createFrom(credential.getData())
                    .getIdToken();

            mAuth.signInWithCredential(
                            GoogleAuthProvider.getCredential(token, null))
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            showMessage("התחברת בהצלחה");
                            openCalendar();
                        } else {
                            showMessage("ההתחברות נכשלה: "
                                    + getFirebaseError(task.getException()));
                        }
                    });
        } catch (Exception e) {
            showMessage("התחברות עם Google נכשלה: " + e.getMessage());
        }
    }

    private String getFirebaseError(Exception e) {
        return e != null && e.getMessage() != null && !e.getMessage().isEmpty()
                ? e.getMessage()
                : "שגיאה לא ידועה";
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void openCalendar() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
