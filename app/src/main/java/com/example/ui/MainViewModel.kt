package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DataWargaApp
import com.example.data.model.Penduduk
import com.example.data.model.User
import com.example.notification.NotificationHelper
import com.example.repository.AppThemeMode
import com.example.repository.AuthResult
import com.example.repository.ExportFileResult
import com.example.repository.ExportFormat
import com.example.repository.SaveResult
import com.example.repository.StorageDestination
import com.example.repository.SyncResult
import com.example.repository.UserPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Checking : AuthUiState()
    data object SetupAdmin : AuthUiState()
    data object LoggedOut : AuthUiState()
    data class TwoFactorChallenge(val user: User) : AuthUiState()
    data class LoggedIn(val user: User) : AuthUiState()
}

data class DashboardStats(
    val totalCount: Int = 0,
    val maleCount: Int = 0,
    val femaleCount: Int = 0,
    val wniCount: Int = 0,
    val pendingSyncCount: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DataWargaApp
    private val pendudukRepository = app.pendudukRepository
    private val authRepository = app.authRepository
    private val preferencesRepository = app.preferencesRepository
    private val syncRepository = app.syncRepository
    val exportRepository = app.exportRepository

    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Checking)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _genderFilter = MutableStateFlow<String?>(null)
    val genderFilter: StateFlow<String?> = _genderFilter.asStateFlow()

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UserPreferences(
                themeMode = AppThemeMode.SYSTEM,
                isDailyReminderEnabled = true,
                reminderHour = 9,
                reminderMinute = 0,
                isAutoCloudSyncEnabled = true,
                cloudSyncIntervalMinutes = 60,
                lastSyncTimestamp = 0L
            )
        )

    private val allResidentsFlow = pendudukRepository.getAllResidentsFlow()

    val filteredResidents: StateFlow<List<Penduduk>> = combine(
        allResidentsFlow,
        _searchQuery,
        _genderFilter
    ) { residents, query, gender ->
        var result = residents
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                it.nik.lowercase().contains(q) ||
                        it.nama.lowercase().contains(q) ||
                        it.alamat.lowercase().contains(q) ||
                        it.kelurahan.lowercase().contains(q) ||
                        it.kecamatan.lowercase().contains(q)
            }
        }
        if (gender != null) {
            result = result.filter { it.jenisKelamin.equals(gender, ignoreCase = true) }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = allResidentsFlow.combine(filteredResidents) { all, _ ->
        DashboardStats(
            totalCount = all.size,
            maleCount = all.count { it.jenisKelamin.equals("Laki-laki", ignoreCase = true) },
            femaleCount = all.count { it.jenisKelamin.equals("Perempuan", ignoreCase = true) },
            wniCount = all.count { it.kewarganegaraan.equals("WNI", ignoreCase = true) },
            pendingSyncCount = all.count { it.syncStatus == Penduduk.SYNC_STATUS_PENDING }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Dialog & UI State
    var selectedResidentForDetail = MutableStateFlow<Penduduk?>(null)
    var residentForForm = MutableStateFlow<Penduduk?>(null)
    var isFormOpen = MutableStateFlow(false)
    var residentToDelete = MutableStateFlow<Penduduk?>(null)
    var isExportDialogOpen = MutableStateFlow(false)
    var isSyncDialogOpen = MutableStateFlow(false)
    var isSecurityDialogOpen = MutableStateFlow(false)
    var isSettingsDialogOpen = MutableStateFlow(false)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncResult = MutableStateFlow<SyncResult?>(null)
    val lastSyncResult: StateFlow<SyncResult?> = _lastSyncResult.asStateFlow()

    private val _lastExportResult = MutableStateFlow<ExportFileResult?>(null)
    val lastExportResult: StateFlow<ExportFileResult?> = _lastExportResult.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    init {
        checkInitialAuthState()
    }

    fun checkInitialAuthState() {
        viewModelScope.launch {
            if (!authRepository.hasAdmin()) {
                _authUiState.value = AuthUiState.SetupAdmin
            } else {
                _authUiState.value = AuthUiState.LoggedOut
            }
        }
    }

    fun createAdmin(username: String, password: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (username.isBlank() || password.length < 8) {
                onComplete(false, "Username wajib diisi dan password minimal 8 karakter.")
                return@launch
            }
            val success = authRepository.createAdmin(username, password)
            if (success) {
                val admin = authRepository.getAdminUser()
                if (admin != null) {
                    _authUiState.value = AuthUiState.LoggedIn(admin)
                    _snackbarEvent.emit("Akun admin berhasil dibuat. Selamat datang di DataWarga!")
                    onComplete(true, "Berhasil")
                } else {
                    _authUiState.value = AuthUiState.LoggedOut
                    onComplete(true, "Berhasil")
                }
            } else {
                onComplete(false, "Gagal membuat akun admin. Coba username lain.")
            }
        }
    }

    fun login(username: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.authenticate(username, password)) {
                is AuthResult.Success -> {
                    _authUiState.value = AuthUiState.LoggedIn(result.user)
                    _snackbarEvent.emit("Login berhasil sebagai ${result.user.username}")
                    onResult(true, "Berhasil")
                }
                is AuthResult.RequiresTwoFactor -> {
                    _authUiState.value = AuthUiState.TwoFactorChallenge(result.user)
                    onResult(true, "Masukkan kode 2FA")
                }
                is AuthResult.Error -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    fun verifyTwoFactor(pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val current = _authUiState.value
            if (current is AuthUiState.TwoFactorChallenge) {
                val isValid = authRepository.verifyTwoFactorPin(current.user, pin)
                if (isValid) {
                    _authUiState.value = AuthUiState.LoggedIn(current.user)
                    _snackbarEvent.emit("Verifikasi 2FA berhasil!")
                    onResult(true, "Berhasil")
                } else {
                    onResult(false, "PIN 2FA salah. Silakan periksa kembali.")
                }
            }
        }
    }

    fun loginWithBiometricSuccess() {
        viewModelScope.launch {
            val admin = authRepository.getAdminUser()
            if (admin != null) {
                _authUiState.value = AuthUiState.LoggedIn(admin)
                _snackbarEvent.emit("Autentikasi biometrik berhasil!")
            }
        }
    }

    fun logout() {
        _authUiState.value = AuthUiState.LoggedOut
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun setGenderFilter(gender: String?) {
        _genderFilter.value = gender
    }

    fun openAddResident() {
        residentForForm.value = null
        isFormOpen.value = true
    }

    fun openEditResident(penduduk: Penduduk) {
        residentForForm.value = penduduk
        isFormOpen.value = true
    }

    fun closeForm() {
        isFormOpen.value = false
        residentForForm.value = null
    }

    fun saveResident(penduduk: Penduduk, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = pendudukRepository.saveResident(penduduk)) {
                is SaveResult.Success -> {
                    closeForm()
                    val msg = if (penduduk.id == 0L) "Data penduduk berhasil ditambahkan!" else "Data penduduk berhasil diperbarui!"
                    _snackbarEvent.emit(msg)
                    onResult(true, msg)
                }
                is SaveResult.Error -> {
                    onResult(false, result.message)
                }
            }
        }
    }

    fun confirmDeleteResident(penduduk: Penduduk) {
        residentToDelete.value = penduduk
    }

    fun executeDeleteResident() {
        viewModelScope.launch {
            val toDelete = residentToDelete.value
            if (toDelete != null) {
                pendudukRepository.deleteResident(toDelete)
                residentToDelete.value = null
                if (selectedResidentForDetail.value?.id == toDelete.id) {
                    selectedResidentForDetail.value = null
                }
                _snackbarEvent.emit("Data penduduk ${toDelete.nama} berhasil dihapus.")
            }
        }
    }

    fun cancelDeleteResident() {
        residentToDelete.value = null
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = syncRepository.performCloudSync()
            _lastSyncResult.value = result
            _isSyncing.value = false
            when (result) {
                is SyncResult.Success -> _snackbarEvent.emit(result.message)
                is SyncResult.Offline -> _snackbarEvent.emit(result.message)
                is SyncResult.Error -> _snackbarEvent.emit(result.message)
            }
        }
    }

    fun exportData(
        format: ExportFormat,
        destination: StorageDestination,
        onResult: (Boolean, String, ExportFileResult?) -> Unit
    ) {
        viewModelScope.launch {
            val residents = pendudukRepository.getAllResidentsList()
            if (residents.isEmpty()) {
                onResult(false, "Tidak ada data penduduk untuk diekspor.", null)
                return@launch
            }

            val result = exportRepository.exportData(residents, format, destination)
            result.onSuccess { exportResult ->
                _lastExportResult.value = exportResult
                val destLabel = if (destination == StorageDestination.INTERNAL) "Internal App" else "Folder Downloads"
                val formatLabel = if (format == ExportFormat.EXCEL) "Excel (.csv)" else "PDF"
                val msg = "Berhasil mengekspor $formatLabel ke $destLabel:\n${exportResult.filename}"
                _snackbarEvent.emit(msg)
                onResult(true, msg, exportResult)
            }.onFailure { err ->
                onResult(false, "Gagal mengekspor: ${err.localizedMessage}", null)
            }
        }
    }

    fun restoreBackupJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = syncRepository.restoreEncryptedBackup(jsonString)
            result.onSuccess { count ->
                _snackbarEvent.emit("Berhasil memulihkan $count data penduduk dari file cadangan terenkripsi!")
                onResult(true, "Berhasil memulihkan $count data.")
            }.onFailure { err ->
                onResult(false, "Gagal memulihkan cadangan: ${err.localizedMessage}")
            }
        }
    }

    fun getEncryptedBackupPackage(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val list = pendudukRepository.getAllResidentsList()
            val backupJson = syncRepository.createEncryptedBackupJson(list)
            onResult(backupJson)
        }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun updateDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesRepository.setDailyReminder(enabled, hour, minute)
            if (enabled) {
                NotificationHelper.scheduleDailyReminder(app, hour, minute)
                _snackbarEvent.emit("Pengingat harian dijadwalkan pukul %02d:%02d".format(hour, minute))
            } else {
                NotificationHelper.cancelDailyReminder(app)
                _snackbarEvent.emit("Pengingat harian dinonaktifkan.")
            }
        }
    }

    fun sendTestNotification() {
        NotificationHelper.showReminderNotification(
            app,
            "Pengingat Uji Coba DataWarga",
            "Sistem notifikasi pengingat harian aktif dan berfungsi dengan baik!"
        )
    }

    fun updateTwoFactor(enabled: Boolean, pin: String?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val user = (authUiState.value as? AuthUiState.LoggedIn)?.user ?: return@launch
            if (enabled && (pin.isNullOrBlank() || pin.length != 6 || !pin.all { it.isDigit() })) {
                onResult(false, "PIN 2FA harus berupa 6 digit angka.")
                return@launch
            }
            authRepository.updateTwoFactorSettings(user, enabled, pin)
            val updatedUser = user.copy(isTwoFactorEnabled = enabled, twoFactorPin = if (enabled) pin else null)
            _authUiState.value = AuthUiState.LoggedIn(updatedUser)
            val msg = if (enabled) "Otentikasi Dua Faktor (2FA) berhasil diaktifkan!" else "2FA telah dinonaktifkan."
            _snackbarEvent.emit(msg)
            onResult(true, msg)
        }
    }

    fun updateBiometric(enabled: Boolean) {
        viewModelScope.launch {
            val user = (authUiState.value as? AuthUiState.LoggedIn)?.user ?: return@launch
            authRepository.updateBiometricSettings(user, enabled)
            val updatedUser = user.copy(isBiometricEnabled = enabled)
            _authUiState.value = AuthUiState.LoggedIn(updatedUser)
            _snackbarEvent.emit(if (enabled) "Autentikasi sidik jari diaktifkan." else "Autentikasi sidik jari dinonaktifkan.")
        }
    }
}
