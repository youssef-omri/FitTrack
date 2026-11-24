package com.example.fittrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etRegisterUsername;
    private EditText etRegisterPassword;
    private EditText etRegisterConfirmPassword;
    private Button btnRegisterSubmit;
    private TextView tvBackToLogin;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        databaseHelper = new DatabaseHelper(this);
        sessionManager = new SessionManager(this);

        etRegisterUsername = findViewById(R.id.etRegisterUsername);
        etRegisterPassword = findViewById(R.id.etRegisterPassword);
        etRegisterConfirmPassword = findViewById(R.id.etRegisterConfirmPassword);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnRegisterSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegistration();
            }
        });

        tvBackToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void handleRegistration() {
        String username = etRegisterUsername.getText().toString().trim();
        String password = etRegisterPassword.getText().toString().trim();
        String confirmPassword = etRegisterConfirmPassword.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(username)) {
            etRegisterUsername.setError(getString(R.string.error_empty_username));
            etRegisterUsername.requestFocus();
            return;
        }

        if (username.length() < 3) {
            etRegisterUsername.setError(getString(R.string.error_short_username));
            etRegisterUsername.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etRegisterPassword.setError(getString(R.string.error_empty_password));
            etRegisterPassword.requestFocus();
            return;
        }

        if (password.length() < 4) {
            etRegisterPassword.setError(getString(R.string.error_short_password));
            etRegisterPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etRegisterConfirmPassword.setError(getString(R.string.error_password_mismatch));
            etRegisterConfirmPassword.requestFocus();
            return;
        }

        // Check if username already exists in SQLite
        if (databaseHelper.checkUserExists(username)) {
            etRegisterUsername.setError(getString(R.string.error_user_exists));
            etRegisterUsername.requestFocus();
            Toast.makeText(this, getString(R.string.error_user_exists), Toast.LENGTH_LONG).show();
            return;
        }

        // Register user and pre-populate default workouts
        long userId = databaseHelper.registerUser(username, password);
        if (userId != -1) {
            // Automatically log in the user upon registration
            sessionManager.createLoginSession(userId, username);

            Toast.makeText(this, getString(R.string.registration_success), Toast.LENGTH_SHORT).show();

            // Redirect to MainActivity
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Registration failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
