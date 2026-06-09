package com.localcode.vortexgaming.views.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.localcode.vortexgaming.MainActivity;
import com.localcode.vortexgaming.R;
import com.localcode.vortexgaming.api.RetrofitClient;
import com.localcode.vortexgaming.api.VortexApiService;
import com.localcode.vortexgaming.models.LoginRequest;
import com.localcode.vortexgaming.models.VortexAuthResponse;
import com.localcode.vortexgaming.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private TextView tvLoginError;
    private MaterialButton btnLogin;

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        session = new SessionManager(this);

        tilEmail     = findViewById(R.id.tilEmail);
        tilPassword  = findViewById(R.id.tilPassword);
        etEmail      = findViewById(R.id.etEmail);
        etPassword   = findViewById(R.id.etPassword);
        tvLoginError = findViewById(R.id.tvLoginError);
        btnLogin     = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        clearErrors();

        String email    = text(etEmail);
        String password = text(etPassword);
        boolean valid   = true;

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("El correo es obligatorio");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Ingresa un correo válido");
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("La contraseña es obligatoria");
            valid = false;
        }

        if (!valid) return;

        btnLogin.setEnabled(false);
        btnLogin.setText("Iniciando sesión...");

        VortexApiService api = RetrofitClient.getVortexClient().create(VortexApiService.class);
        api.login(new LoginRequest(email, password)).enqueue(new Callback<VortexAuthResponse>() {
            @Override
            public void onResponse(Call<VortexAuthResponse> call, Response<VortexAuthResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar sesión");

                if (response.isSuccessful() && response.body() != null && response.body().token != null) {
                    VortexAuthResponse body = response.body();
                    session.setLoggedIn(true);
                    session.saveToken(body.token);
                    session.saveUsername(body.user.name);
                    session.saveEmail(body.user.email);
                    session.saveDob(body.user.dob != null ? body.user.dob : "");
                    goToMain();
                } else if (response.code() == 401) {
                    tilPassword.setError("Contraseña incorrecta");
                } else if (response.code() == 404) {
                    showError("Usuario no encontrado. ¿Ya tienes una cuenta?");
                } else {
                    showError("Error del servidor. Intenta de nuevo.");
                }
            }

            @Override
            public void onFailure(Call<VortexAuthResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Iniciar sesión");
                showError(t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void showError(String msg) {
        tvLoginError.setText(msg);
        tvLoginError.setVisibility(View.VISIBLE);
    }

    private void clearErrors() {
        tilEmail.setError(null);
        tilPassword.setError(null);
        tvLoginError.setVisibility(View.GONE);
    }

    private String text(TextInputEditText f) {
        return f.getText() != null ? f.getText().toString().trim() : "";
    }
}
