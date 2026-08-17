package com.example.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.widget.Toast
import java.util.concurrent.Executors

object BiometricAuthHelper {

    fun isBiometricSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        return try {
            val packageManager = context.packageManager
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT)
        } catch (e: Exception) {
            false
        }
    }

    fun authenticate(
        activity: Activity,
        title: String = "تسجيل الدخول بالبصمة",
        subtitle: String = "المصادقة للوصول إلى الحساب المحاسبي",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val executor = Executors.newSingleThreadExecutor()
                val cancellationSignal = CancellationSignal()

                val prompt = android.hardware.biometrics.BiometricPrompt.Builder(activity)
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setDescription("ضع إصبعك على مستشعر البصمة للمتابعة")
                    .setNegativeButton("إلغاء واستخدام الرمز", executor) { _, _ ->
                        activity.runOnUiThread {
                            onError("تم الإلغاء")
                        }
                    }
                    .build()

                prompt.authenticate(
                    cancellationSignal,
                    executor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) {
                            super.onAuthenticationSucceeded(result)
                            activity.runOnUiThread {
                                onSuccess()
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                            super.onAuthenticationError(errorCode, errString)
                            activity.runOnUiThread {
                                onError(errString?.toString() ?: "فشلت المصادقة بالبصمة")
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            activity.runOnUiThread {
                                onError("لم يتم التعرف على البصمة، حاول مرة أخرى")
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                onError("خدمة البصمة غير متاحة: ${e.localizedMessage}")
            }
        } else {
            onError("البصمة غير مدعومة على هذا الإصدار من النظام")
        }
    }
}
