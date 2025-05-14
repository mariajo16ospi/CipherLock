import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class USBReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "UsbAuthPrefs";
    private static final String PROTECTION_ENABLED_KEY = "protection_enabled";
    private static final String WHITELIST_KEY = "whitelist_devices";
    private static final String TAG = "USBReceiver";

    /**
     * Recibe eventos de conexión USB y verifica si el dispositivo necesita autenticación
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
            SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
            boolean protectionEnabled = settings.getBoolean(PROTECTION_ENABLED_KEY, false);

            if (protectionEnabled) {
                // Obtener información del dispositivo conectado
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (device != null) {
                    // Generar ID único para el dispositivo conectado
                    String deviceId = generateDeviceId(device);
                    Log.d(TAG, "Dispositivo detectado: " + deviceId);

                    // Verificar si el dispositivo está en la lista blanca
                    if (!isDeviceWhitelisted(context, deviceId)) {
                        // No está en lista blanca, lanzar diálogo de contraseña
                        Intent dialogIntent = new Intent(context, PasswordDialogActivity.class);
                        dialogIntent.putExtra("device_id", deviceId);
                        dialogIntent.putExtra("device_name", getDeviceName(device));
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(dialogIntent);
                    } else {
                        Log.d(TAG, "Dispositivo en lista blanca, acceso permitido");
                        // El dispositivo está en lista blanca, permitir acceso automáticamente
                        // Usar la clase MainActivity para acceder al método nativo
                        MainActivity mainActivity = new MainActivity();
                        mainActivity.allowUsbDataTransfer();
                    }
                }
            }
        }
    }

    /**
     * Genera un ID único para el dispositivo USB conectado
     */
    private String generateDeviceId(UsbDevice device) {
        StringBuilder builder = new StringBuilder();
        builder.append(device.getVendorId())
                .append(":")
                .append(device.getProductId())
                .append(":");

        // Intentar obtener número de serie si está disponible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String serialNumber = device.getSerialNumber();
            if (serialNumber != null) {
                builder.append(serialNumber);
            }
        }

        return builder.toString();
    }

    /**
     * Obtiene un nombre descriptivo para el dispositivo
     */
    private String getDeviceName(UsbDevice device) {
        String deviceName = device.getDeviceName();

        // Intentar obtener nombre más descriptivo si está disponible
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String manufacturerName = device.getManufacturerName();
            String productName = device.getProductName();

            if (manufacturerName != null && productName != null) {
                return manufacturerName + " " + productName;
            }
        }

        // Nombre genérico con IDs si no hay información descriptiva
        return "Dispositivo " + device.getVendorId() + ":" + device.getProductId();
    }

    /**
     * Verifica si un dispositivo está en la lista blanca
     */
    public static boolean isDeviceWhitelisted(Context context, String deviceId) {
        List<WhitelistedDevice> whitelistedDevices = getWhitelistedDevices(context);

        for (WhitelistedDevice device : whitelistedDevices) {
            if (device.getId().equals(deviceId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Obtiene la lista de dispositivos en lista blanca almacenados
     */
    public static List<WhitelistedDevice> getWhitelistedDevices(Context context) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String jsonWhitelist = settings.getString(WHITELIST_KEY, "");

        if (jsonWhitelist.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            Gson gson = new Gson();
            Type type = new TypeToken<List<WhitelistedDevice>>(){}.getType();
            return gson.fromJson(jsonWhitelist, type);
        } catch (Exception e) {
            Log.e(TAG, "Error al deserializar lista blanca", e);
            return new ArrayList<>();
        }
    }

    /**
     * Añade un dispositivo a la lista blanca
     */
    public static void addDeviceToWhitelist(Context context, String deviceId, String deviceName) {
        List<WhitelistedDevice> whitelistedDevices = getWhitelistedDevices(context);

        // Verificar si ya existe
        for (WhitelistedDevice device : whitelistedDevices) {
            if (device.getId().equals(deviceId)) {
                // Ya está en la lista
                return;
            }
        }

        // Añadir nuevo dispositivo
        WhitelistedDevice newDevice = new WhitelistedDevice(deviceId, deviceName, System.currentTimeMillis());
        whitelistedDevices.add(newDevice);

        // Guardar la lista actualizada
        saveWhitelistedDevices(context, whitelistedDevices);
    }

    /**
     * Elimina un dispositivo de la lista blanca
     */
    public static void removeDeviceFromWhitelist(Context context, String deviceId) {
        List<WhitelistedDevice> whitelistedDevices = getWhitelistedDevices(context);
        List<WhitelistedDevice> updatedList = new ArrayList<>();

        for (WhitelistedDevice device : whitelistedDevices) {
            if (!device.getId().equals(deviceId)) {
                updatedList.add(device);
            }
        }

        saveWhitelistedDevices(context, updatedList);
    }

    /**
     * Guarda la lista de dispositivos en las preferencias
     */
    private static void saveWhitelistedDevices(Context context, List<WhitelistedDevice> devices) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        Gson gson = new Gson();
        String jsonWhitelist = gson.toJson(devices);

        editor.putString(WHITELIST_KEY, jsonWhitelist);
        editor.apply();
    }
}
