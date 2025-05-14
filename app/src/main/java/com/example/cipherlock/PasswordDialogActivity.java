package com.example.cipherlock;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class PasswordDialogActivity extends Activity {
    private EditText passwordInput;
    private Button unlockButton;
    private Button cancelButton;
    private CheckBox rememberDeviceCheckbox;
    private TextView timerTextView;

    private String deviceId;
    private String deviceName;
    private CountDownTimer countDownTimer;

    private static final String PREFS_NAME = "UsbAuthPrefs";
    private static final String PASSWORD_KEY = "password";
    private static final int TIMER_DURATION = 30000; // 30 segundos
    private static final int TIMER_INTERVAL = 1000; // 1 segundo

    static {
        System.loadLibrary("usb_auth_native");
    }

    // Declaración de método nativo
    public native boolean allowUsbDataTransfer();
    public native boolean blockUsbDataTransfer();

    /**
     * Inicializa la interfaz y componentes del diálogo de contraseña.
     * Comienza un temporizador de 30 segundos.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_dialog);

        // Obtener información del dispositivo conectado
        deviceId = getIntent().getStringExtra("device_id");
        deviceName = getIntent().getStringExtra("device_name");
        if (deviceName == null) {
            deviceName = "Dispositivo desconocido";
        }

        // Inicializar componentes de UI
        passwordInput = findViewById(R.id.password_input);
        unlockButton = findViewById(R.id.unlock_button);
        cancelButton = findViewById(R.id.cancel_button);
        rememberDeviceCheckbox = findViewById(R.id.remember_device_checkbox);
        timerTextView = findViewById(R.id.timer_text);

        // Mostrar nombre del dispositivo
        TextView deviceNameText = findViewById(R.id.device_name_text);
        deviceNameText.setText("Dispositivo: " + deviceName);

        // Bloquear transferencia al mostrar el diálogo
        blockUsbDataTransfer();

        // Configurar listeners de botones
        unlockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyPassword();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAuthentication();
            }
        });

        // Iniciar temporizador de 30 segundos
        startTimer();
    }

    /**
     * Inicia un temporizador de 30 segundos. Si se acaba el tiempo,
     * se cancela la autenticación automáticamente.
     */
    private void startTimer() {
        countDownTimer = new CountDownTimer(TIMER_DURATION, TIMER_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Actualizar texto del temporizador
                int secondsLeft = (int) (millisUntilFinished / 1000);
                timerTextView.setText("Tiempo restante: " + secondsLeft + " segundos");

                // Cambiar color cuando queden pocos segundos
                if (secondsLeft <= 5) {
                    timerTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }
            }

            @Override
            public void onFinish() {
                timerTextView.setText("¡Tiempo agotado!");
                Toast.makeText(PasswordDialogActivity.this,
                        "Tiempo agotado. Acceso denegado.",
                        Toast.LENGTH_SHORT).show();
                cancelAuthentication();
            }
        }.start();
    }

    /**
     * Verifica si la contraseña ingresada es correcta.
     * Si es correcta y el checkbox está marcado, añade el dispositivo a la lista blanca.
     */
    private void verifyPassword() {
        String enteredPassword = passwordInput.getText().toString();
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String savedPassword = settings.getString(PASSWORD_KEY, "");

        if (enteredPassword.equals(savedPassword)) {
            // Detener temporizador
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            // Contraseña correcta, permitir acceso
            allowUsbDataTransfer();

            // Si está marcado "recordar dispositivo", añadir a lista blanca
            if (rememberDeviceCheckbox.isChecked() && deviceId != null) {
                USBReceiver.addDeviceToWhitelist(this, deviceId, deviceName);
                Toast.makeText(this,
                        "Dispositivo añadido a lista blanca",
                        Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(this, "Acceso concedido", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Contraseña incorrecta", Toast.LENGTH_SHORT).show();
            passwordInput.setText("");
        }
    }

    /**
     * Cancela la autenticación y bloquea el acceso al dispositivo
     */
    private void cancelAuthentication() {
        // Detener temporizador
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Asegurar que transferencia queda bloqueada
        blockUsbDataTransfer();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Detener temporizador si existe
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Si se cierra el diálogo sin verificar, mantener bloqueado
        if (!isFinishing()) {
            blockUsbDataTransfer();
        }
    }
}