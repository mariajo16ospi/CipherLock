
package com.example.cipherlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText passwordField;
    private EditText confirmPasswordField;
    private Button saveButton;
    private Button viewWhitelistButton;
    private Switch enableProtectionSwitch;
    private TextView statusText;
    private USBReceiver usbReceiver;

    private static final String PREFS_NAME = "UsbAuthPrefs";
    private static final String PASSWORD_KEY = "password";
    private static final String PROTECTION_ENABLED_KEY = "protection_enabled";

    static {
        // Cargar la biblioteca nativa
        System.loadLibrary("usb_auth_native");
    }

    // Declaración de métodos nativos
    public native boolean blockUsbDataTransfer();
    public native boolean allowUsbDataTransfer();

    /**
     * Inicializa la actividad principal y sus componentes
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar componentes de UI
        passwordField = findViewById(R.id.password_field);
        confirmPasswordField = findViewById(R.id.confirm_password_field);
        saveButton = findViewById(R.id.save_button);
        statusText = findViewById(R.id.status_text);
        enableProtectionSwitch = findViewById(R.id.enable_protection);

        // Añadir botón para ver lista blanca
        viewWhitelistButton = new Button(this);
        viewWhitelistButton.setText("Ver dispositivos en lista blanca");
        viewWhitelistButton.setLayoutParams(new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        ((android.view.ViewGroup) findViewById(android.R.id.content)).addView(viewWhitelistButton);

        // Cargar configuración
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean protectionEnabled = settings.getBoolean(PROTECTION_ENABLED_KEY, false);
        enableProtectionSwitch.setChecked(protectionEnabled);

        // Actualizar texto de estado
        updateStatusText(protectionEnabled);

        // Configurar listeners
        enableProtectionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PROTECTION_ENABLED_KEY, isChecked);
            editor.apply();

            updateStatusText(isChecked);

            if (isChecked) {
                // Iniciar servicio si se activa la protección
                startService(new Intent(MainActivity.this, UsbAuthService.class));

                // Registrar broadcast receiver para eventos USB
                if (usbReceiver == null) {
                    usbReceiver = new USBReceiver();
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
                    registerReceiver(usbReceiver, filter);
                }
            } else {
                // Detener servicio si se desactiva la protección
                stopService(new Intent(MainActivity.this, UsbAuthService.class));

                // Desregistrar broadcast receiver
                if (usbReceiver != null) {
                    try {
                        unregisterReceiver(usbReceiver);
                    } catch (IllegalArgumentException e) {
                        // Receptor no registrado
                    }
                    usbReceiver = null;
                }

                // Permitir transferencias USB si se desactiva la protección
                allowUsbDataTransfer();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePassword();
            }
        });

        viewWhitelistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showWhitelistedDevices();
            }
        });

        // Iniciar servicio si la protección está activada
        if (protectionEnabled) {
            startService(new Intent(this, UsbAuthService.class));
        }
    }

    /**
     * Actualiza el texto de estado según la configuración
     */
    private void updateStatusText(boolean protectionEnabled) {
        if (protectionEnabled) {
            statusText.setText("Estado: Protección activada");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            statusText.setText("Estado: Protección desactivada");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }

    /**
     * Guarda la contraseña si ambos campos coinciden
     */
    private void savePassword() {
        String password = passwordField.getText().toString();
        String confirmPassword = confirmPasswordField.getText().toString();

        if (password.isEmpty()) {
            Toast.makeText(this, "La contraseña no puede estar vacía", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
            return;
        }

        // Guardar contraseña
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PASSWORD_KEY, password);
        editor.apply();

        Toast.makeText(this, "Contraseña guardada correctamente", Toast.LENGTH_SHORT).show();

        // Limpiar campos
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    /**
     * Muestra la lista de dispositivos en lista blanca
     */
    private void showWhitelistedDevices() {
        List<WhitelistedDevice> devices = USBReceiver.getWhitelistedDevices(this);

        if (devices.isEmpty()) {
            Toast.makeText(this, "No hay dispositivos en la lista blanca", Toast.LENGTH_SHORT).show();
            return;
        }

        // Crear lista de nombres de dispositivo
        List<String> deviceNames = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        for (WhitelistedDevice device : devices) {
            String date = dateFormat.format(new Date(device.getAddedTimestamp()));
            deviceNames.add(device.getName() + " (añadido: " + date + ")");
        }

        // Crear diálogo con lista
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dispositivos en lista blanca");

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, deviceNames);

        builder.setAdapter(adapter, null);

        // Añadir opción para eliminar dispositivos
        builder.setPositiveButton("Cerrar", null);

        final AlertDialog dialog = builder.create();

        // Configurar evento de clic para eliminar dispositivos
        ListView listView = dialog.getListView();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Preguntar si se desea eliminar
                AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(MainActivity.this);
                confirmBuilder.setTitle("Eliminar dispositivo");
                confirmBuilder.setMessage("¿Desea eliminar este dispositivo de la lista blanca?");

                confirmBuilder.setPositiveButton("Sí", (dialogInterface, i) -> {
                    // Eliminar dispositivo
                    WhitelistedDevice device = devices.get(position);
                    USBReceiver.removeDeviceFromWhitelist(MainActivity.this, device.getId());

                    Toast.makeText(MainActivity.this,
                            "Dispositivo eliminado de la lista blanca",
                            Toast.LENGTH_SHORT).show();

                    dialog.dismiss();

                    // Actualizar lista
                    showWhitelistedDevices();
                });

                confirmBuilder.setNegativeButton("No", null);
                confirmBuilder.show();
            }
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Desregistrar receptor si existe
        if (usbReceiver != null) {
            try {
                unregisterReceiver(usbReceiver);
            } catch (IllegalArgumentException e) {
                // Receptor no registrado
            }
            usbReceiver = null;
        }
    }
}