package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricHelper
import com.example.ui.AuthUiState
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request Notification Permission for Android 13+ with 16-bit compliant requestCode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        setContent {
            val userPrefs by viewModel.userPreferences.collectAsState()
            val authState by viewModel.authUiState.collectAsState()

            MyApplicationTheme(themeMode = userPrefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (authState) {
                        is AuthUiState.LoggedIn -> {
                            DashboardScreen(viewModel = viewModel)
                        }
                        else -> {
                            LoginScreen(
                                viewModel = viewModel,
                                authState = authState,
                                onBiometricClick = { triggerBiometricUnlock() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun triggerBiometricUnlock() {
        val status = BiometricHelper.checkBiometricAvailability(this)
        when (status) {
            BiometricHelper.BiometricStatus.AVAILABLE -> {
                BiometricHelper.showBiometricPrompt(
                    activity = this,
                    title = "Autentikasi DataWarga",
                    subtitle = "Gunakan sidik jari atau biometrik Anda untuk membuka panel",
                    negativeButtonText = "Batal",
                    onSuccess = {
                        viewModel.loginWithBiometricSuccess()
                    },
                    onError = { err ->
                        Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
                    }
                )
            }
            BiometricHelper.BiometricStatus.NOT_ENROLLED -> {
                Toast.makeText(
                    this,
                    "Belum ada sidik jari terdaftar pada perangkat. Silakan login dengan password.",
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Sensor biometrik tidak tersedia pada perangkat ini. Silakan gunakan password.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
