
package com.israel.hebrewcalendar;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import android.os.CancellationSignal;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        credentialManager =
                CredentialManager.create(this);

        Button loginButton =
                findViewById(R.id.loginButton);

        Button registerButton =
                findViewById(R.id.registerButton);

        Button googleButton =
                findViewById(R.id.googleButton);


        loginButton.setOnClickListener(
                v -> loginWithEmail()
        );


        registerButton.setOnClickListener(
                v -> registerWithEmail()
        );


        googleButton.setOnClickListener(
                v -> loginWithGoogle()
        );
    }


    /*
     * ============================================================
     * התחברות עם Email + Password
     * ============================================================
     */
    private void loginWithEmail() {

        String email =
                getEmail();

        String password =
                getPassword();


        if (email.isEmpty()) {

            showMessage(
                    "נא להזין כתובת אימייל"
            );

            return;
        }


        if (password.isEmpty()) {

            showMessage(
                    "נא להזין סיסמה"
            );

            return;
        }


        mAuth.signInWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                showMessage(
                                        "התחברת בהצלחה"
                                );

                                openCalendar();

                            } else {

                                showMessage(
                                        "ההתחברות נכשלה: "
                                                + getFirebaseError(
                                                task.getException()
                                        )
                                );
                            }
                        }
                );
    }


    /*
     * ============================================================
     * יצירת חשבון עם Email + Password
     * ============================================================
     */
    private void registerWithEmail() {

        String email =
                getEmail();

        String password =
                getPassword();


        if (email.isEmpty()) {

            showMessage(
                    "נא להזין כתובת אימייל"
            );

            return;
        }


        if (password.isEmpty()) {

            showMessage(
                    "נא להזין סיסמה"
            );

            return;
        }


        if (password.length() < 6) {

            showMessage(
                    "הסיסמה חייבת להכיל לפחות 6 תווים"
            );

            return;
        }


        mAuth.createUserWithEmailAndPassword(
                        email,
                        password
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                showMessage(
                                        "החשבון נוצר בהצלחה"
                                );

                                openCalendar();

                            } else {

                                showMessage(
                                        "יצירת החשבון נכשלה: "
                                                + getFirebaseError(
                                                task.getException()
                                        )
                                );
                            }
                        }
                );
    }


    /*
     * ============================================================
     * Google Sign-In
     * ============================================================
     */
    private void loginWithGoogle() {

        /*
         * חשוב:
         *
         * default_web_client_id הוא ה-Web OAuth Client ID
         * שנוצר עבור פרויקט Firebase/Google שלך.
         */
        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()

                        /*
                         * מציג גם חשבונות Google
                         * שלא השתמשו באפליקציה בעבר.
                         */
                        .setFilterByAuthorizedAccounts(false)

                        /*
                         * זה צריך להיות ה-Web Client ID,
                         * לא Android Client ID.
                         */
                        .setServerClientId(
                                getString(
                                        R.string.default_web_client_id
                                )
                        )

                        .build();


        GetCredentialRequest request =
                new GetCredentialRequest.Builder()

                        .addCredentialOption(
                                googleIdOption
                        )

                        .build();


        credentialManager
                .getCredentialAsync(
                        this,
                        request,
                        new android.os.CancellationSignal(),
                        getMainExecutor(),
                        new androidx.credentials.CredentialManagerCallback<
                                androidx.credentials.GetCredentialResponse,
                                androidx.credentials.exceptions.GetCredentialException>() {

                            @Override
                            public void onResult(
                                    androidx.credentials.GetCredentialResponse result) {

                                handleGoogleCredential(
                                        result.getCredential()
                                );
                            }


                            @Override
                            public void onError(
                                    @NonNull androidx.credentials.exceptions.GetCredentialException e) {

                                showMessage(
                                        "התחברות עם Google נכשלה: "
                                                + e.getMessage()
                                );
                            }
                        }
                );
    }


    /*
     * ============================================================
     * קבלת Credential מ-Google
     * ============================================================
     */
    private void handleGoogleCredential(
            Credential credential) {

        if (!(credential instanceof CustomCredential)) {

            showMessage(
                    "פרטי Google לא תקינים"
            );

            return;
        }


        CustomCredential customCredential =
                (CustomCredential) credential;


        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(customCredential.getType())) {

            showMessage(
                    "פרטי Google לא תקינים"
            );

            return;
        }


        try {

            GoogleIdTokenCredential googleIdTokenCredential =
                    GoogleIdTokenCredential
                            .createFrom(credential.getData());

            String idToken =
                    googleIdTokenCredential.getIdToken();

            AuthCredential firebaseCredential =
                    GoogleAuthProvider.getCredential(
                            idToken,
                            null
                    );

            mAuth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this, task -> {

                        if (task.isSuccessful()) {

                            showMessage(
                                    "התחברת בהצלחה"
                            );

                            openCalendar();

                        } else {

                            showMessage(
                                    "ההתחברות נכשלה: "
                                            + getFirebaseError(
                                            task.getException()
                                    )
                            );
                        }
                    });

        } catch (Exception e) {

            showMessage(
                    "התחברות עם Google נכשלה: "
                            + e.getMessage()
            );
        }
    }


    /*
     * ============================================================
     * חיבור Google ל-Firebase
     * ============================================================
     */
    private void firebaseAuthWithGoogle(
            String idToken) {

        AuthCredential credential =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null
                );


        mAuth.signInWithCredential(
                        credential
                )
                .addOnCompleteListener(
                        this,
                        task -> {

                            if (task.isSuccessful()) {

                                showMessage(
                                        "התחברת בהצלחה עם Google"
                                );

                                openCalendar();

                            } else {

                                showMessage(
                                        "ההתחברות עם Google נכשלה: "
                                                + getFirebaseError(
                                                task.getException()
                                        )
                                );
                            }
                        }
                );
    }


    /*
     * ============================================================
     * Email
     * ============================================================
     */
    private String getEmail() {

        android.widget.EditText emailEditText =
                findViewById(
                        R.id.emailEditText
                );


        return emailEditText
                .getText()
                .toString()
                .trim();
    }


    /*
     * ============================================================
     * Password
     * ============================================================
     */
    private String getPassword() {

        android.widget.EditText passwordEditText =
                findViewById(
                        R.id.passwordEditText
                );


        return passwordEditText
                .getText()
                .toString();
    }


    /*
     * ============================================================
     * Firebase error
     * ============================================================
     */
    private String getFirebaseError(
            Exception exception) {

        if (exception == null) {

            return "שגיאה לא ידועה";
        }


        String message =
                exception.getMessage();


        if (message == null ||
                message.isEmpty()) {

            return "שגיאה לא ידועה";
        }


        return message;
    }


    /*
     * ============================================================
     * הודעה
     * ============================================================
     */
    private void showMessage(
            String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }


    /*
     * ============================================================
     * מעבר ללוח השנה
     * ============================================================
     */
    private void openCalendar() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        MainActivity.class
                );


        startActivity(intent);

        finish();
    }
}
