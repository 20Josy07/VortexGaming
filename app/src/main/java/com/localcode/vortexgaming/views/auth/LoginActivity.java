package com.localcode.vortexgaming.views.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.localcode.vortexgaming.MainActivity;
import com.localcode.vortexgaming.R;
import com.localcode.vortexgaming.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText email = findViewById(R.id.et_email);
        Button login = findViewById(R.id.btn_login);
        android.widget.TextView register = findViewById(R.id.tv_go_to_register);

        login.setOnClickListener(v -> {
            if (TextUtils.isEmpty(email.getText())) {
                email.setError("Este campo es obligatorio");
            } else {
                new SessionManager(this).setLoggedIn(true);
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });

        register.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }
}
