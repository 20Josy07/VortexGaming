package com.localcode.vortexgaming.views.auth;

import android.app.DatePickerDialog;
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
import com.localcode.vortexgaming.models.RegisterRequest;
import com.localcode.vortexgaming.models.VortexAuthResponse;
import com.localcode.vortexgaming.utils.SessionManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilRegEmail, tilRegPassword, tilConfirmPassword, tilDob;
    private TextInputEditText etName, etRegEmail, etRegPassword, etConfirmPassword, etDob;
    private TextView tvRegisterError;
    private MaterialButton btnRegister;

    private final Calendar calendar = Calendar.getInstance();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        session = new SessionManager(this);

        tilName            = findViewById(R.id.tilName);
        tilRegEmail        = findViewById(R.id.tilRegEmail);
        tilRegPassword     = findViewById(R.id.tilRegPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilDob             = findViewById(R.id.tilDob);

        etName            = findViewById(R.id.etName);
        etRegEmail        = findViewById(R.id.etRegEmail);
        etRegPassword     = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etDob             = findViewById(R.id.etDob);
        tvRegisterError   = findViewById(R.id.tvRegisterError);
        btnRegister       = findViewById(R.id.btnRegister);

        View.OnClickListener dobPicker = v -> showDatePicker();
        etDob.setOnClickListener(dobPicker);
        tilDob.setEndIconOnClickListener(dobPicker);

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> finish());
    }

    private void showDatePicker() {
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    calendar.set(year, month, day);
                    etDob.setText(DATE_FORMAT.format(calendar.getTime()));
                    tilDob.setError(null);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void attemptRegister() {
        clearErrors();

        String name    = text(etName);
        String email   = text(etRegEmail);
        String pass    = text(etRegPassword);
        String confirm = text(etConfirmPassword);
        String dob     = text(etDob);

        boolean valid = true;

        if (TextUtils.isEmpty(name)) {
            tilName.setError("El nombre es obligatorio");
            valid = false;
        }
        if (TextUtils.isEmpty(email)) {
            tilRegEmail.setError("El correo es obligatorio");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilRegEmail.setError("Ingresa un correo válido");
            valid = false;
        }
        if (TextUtils.isEmpty(pass)) {
            tilRegPassword.setError("La contraseña es obligatoria");
            valid = false;
        } else if (pass.length() < 8) {
            tilRegPassword.setError("Mínimo 8 caracteres");
            valid = false;
        }
        if (TextUtils.isEmpty(confirm)) {
            tilConfirmPassword.setError("Confirma tu contraseña");
            valid = false;
        } else if (!confirm.equals(pass)) {
            tilConfirmPassword.setError("Las contraseñas no coinciden");
            valid = false;
        }
        if (TextUtils.isEmpty(dob)) {
            tilDob.setError("La fecha de nacimiento es obligatoria");
            valid = false;
        } else {
            Date parsed = parseDob(dob);
            if (parsed == null) {
                tilDob.setError("Formato inválido. Usa DD/MM/AAAA");
                valid = false;
            } else if (!isOldEnough(parsed)) {
                tilDob.setError("Debes tener al menos 13 años");
                valid = false;
            }
        }

        if (!valid) return;

        btnRegister.setEnabled(false);
        btnRegister.setText("Creando cuenta...");

        VortexApiService api = RetrofitClient.getVortexClient().create(VortexApiService.class);
        api.register(new RegisterRequest(name, email, pass, dob)).enqueue(new Callback<VortexAuthResponse>() {
            @Override
            public void onResponse(Call<VortexAuthResponse> call, Response<VortexAuthResponse> response) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Crear cuenta");

                if (response.isSuccessful() && response.body() != null && response.body().token != null) {
                    VortexAuthResponse body = response.body();
                    session.setLoggedIn(true);
                    session.saveToken(body.token);
                    session.saveUsername(body.user.name);
                    session.saveEmail(body.user.email);
                    session.saveDob(body.user.dob != null ? body.user.dob : dob);
                    goToMain();
                } else if (response.code() == 409) {
                    tilRegEmail.setError("Este correo ya está registrado");
                } else {
                    showError("Error del servidor. Intenta de nuevo.");
                }
            }

            @Override
            public void onFailure(Call<VortexAuthResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                btnRegister.setText("Crear cuenta");
                showError("No se pudo conectar al servidor.\nVerifica que la API esté corriendo.");
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private Date parseDob(String dob) {
        try {
            DATE_FORMAT.setLenient(false);
            return DATE_FORMAT.parse(dob);
        } catch (ParseException e) {
            return null;
        }
    }

    private boolean isOldEnough(Date dob) {
        Calendar minAge = Calendar.getInstance();
        minAge.add(Calendar.YEAR, -13);
        return !dob.after(minAge.getTime());
    }

    private void clearErrors() {
        tilName.setError(null);
        tilRegEmail.setError(null);
        tilRegPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilDob.setError(null);
        tvRegisterError.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        tvRegisterError.setText(msg);
        tvRegisterError.setVisibility(View.VISIBLE);
    }

    private String text(TextInputEditText f) {
        return f.getText() != null ? f.getText().toString().trim() : "";
    }
}
