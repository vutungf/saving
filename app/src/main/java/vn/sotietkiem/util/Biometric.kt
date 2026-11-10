package vn.sotietkiem.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Máy có vân tay / khuôn mặt đã đăng ký và dùng được không. */
fun canUseBiometric(ctx: Context): Boolean =
    BiometricManager.from(ctx).canAuthenticate(BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS

/** Hiện hộp thoại vân tay / khuôn mặt của hệ thống; gọi [onSuccess] khi xác thực thành công. */
fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit) {
    if (!canUseBiometric(activity)) return
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Mở Sổ tiết kiệm")
        .setSubtitle("Dùng vân tay hoặc khuôn mặt")
        .setNegativeButtonText("Nhập mã PIN")
        .setAllowedAuthenticators(BIOMETRIC_WEAK)
        .build()
    prompt.authenticate(info)
}
