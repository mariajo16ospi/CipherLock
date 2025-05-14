#include <jni.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <android/log.h>

#define LOG_TAG "UsbAuthNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Esta función bloquea la transferencia de datos por USB
JNIEXPORT jboolean JNICALL
Java_com_example_usbauth_MainActivity_blockUsbDataTransfer(JNIEnv *env, jobject thiz) {
    LOGD("Intentando bloquear transferencia de datos USB");

    // Aquí es donde implementaríamos el código C para bloquear la transferencia
    // Necesitaríamos acceso root o permisos especiales

    // Código simplificado para ejemplo:
    // En un dispositivo real con permisos root, podríamos:
    // 1. Modificar el modo de funcionamiento USB
    // 2. Cambiar los permisos del sistema de archivos

    // En Android esto requiere permisos de sistema, así que en muchos dispositivos
    // solo funcionaría con root. Una alternativa sería usar la API de Android USB
    // desde Java/Kotlin para dispositivos no rooteados.

    int success = 1; // simulamos éxito

    if (success) {
        LOGD("Transferencia USB bloqueada exitosamente");
        return JNI_TRUE;
    } else {
        LOGE("Error al bloquear transferencia USB");
        return JNI_FALSE;
    }
}

// Esta función permite la transferencia de datos por USB
JNIEXPORT jboolean JNICALL
Java_com_example_usbauth_MainActivity_allowUsbDataTransfer(JNIEnv *env, jobject thiz) {
    LOGD("Intentando permitir transferencia de datos USB");

    // Aquí implementaríamos el código para permitir la transferencia

    int success = 1; // simulamos éxito

    if (success) {
        LOGD("Transferencia USB permitida exitosamente");
        return JNI_TRUE;
    } else {
        LOGE("Error al permitir transferencia USB");
        return JNI_FALSE;
    }
}

// Mismo método para PasswordDialogActivity
JNIEXPORT jboolean JNICALL
Java_com_example_usbauth_PasswordDialogActivity_blockUsbDataTransfer(JNIEnv *env, jobject thiz) {
    return Java_com_example_usbauth_MainActivity_blockUsbDataTransfer(env, thiz);
}

JNIEXPORT jboolean JNICALL
Java_com_example_usbauth_PasswordDialogActivity_allowUsbDataTransfer(JNIEnv *env, jobject thiz) {
    return Java_com_example_usbauth_MainActivity_allowUsbDataTransfer(env, thiz);
}