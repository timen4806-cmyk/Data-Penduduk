package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Penduduk
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResidentFormDialog(
    resident: Penduduk?,
    onDismiss: () -> Unit,
    onSave: (Penduduk) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = resident != null

    var nik by remember { mutableStateOf(resident?.nik ?: "") }
    var nama by remember { mutableStateOf(resident?.nama ?: "") }
    var tempatLahir by remember { mutableStateOf(resident?.tempatLahir ?: "") }
    var tanggalLahir by remember { mutableStateOf(resident?.tanggalLahir ?: "") }
    var jenisKelamin by remember { mutableStateOf(resident?.jenisKelamin ?: "") }
    var alamat by remember { mutableStateOf(resident?.alamat ?: "") }
    var rt by remember { mutableStateOf(resident?.rt ?: "") }
    var rw by remember { mutableStateOf(resident?.rw ?: "") }
    var kelurahan by remember { mutableStateOf(resident?.kelurahan ?: "") }
    var kecamatan by remember { mutableStateOf(resident?.kecamatan ?: "") }
    var agama by remember { mutableStateOf(resident?.agama ?: "") }
    var statusPerkawinan by remember { mutableStateOf(resident?.statusPerkawinan ?: "") }
    var pekerjaan by remember { mutableStateOf(resident?.pekerjaan ?: "") }
    var kewarganegaraan by remember { mutableStateOf(resident?.kewarganegaraan ?: "WNI") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dropdown expanded states
    var isGenderExpanded by remember { mutableStateOf(false) }
    var isAgamaExpanded by remember { mutableStateOf(false) }
    var isStatusExpanded by remember { mutableStateOf(false) }
    var isWnExpanded by remember { mutableStateOf(false) }

    // DatePicker setup
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val format = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                tanggalLahir = format.format(selectedCal.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isEditMode) "Edit Data Penduduk" else "Tambah Penduduk Baru",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // NIK field
                    OutlinedTextField(
                        value = nik,
                        onValueChange = {
                            if (it.length <= 16 && it.all { c -> c.isDigit() }) {
                                nik = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Nomor Induk Kependudukan (NIK) *") },
                        supportingText = { Text("${nik.length}/16 digit angka") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resident_form_nik_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Nama lengkap
                    OutlinedTextField(
                        value = nama,
                        onValueChange = {
                            nama = it
                            errorMessage = null
                        },
                        label = { Text("Nama Lengkap *") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resident_form_nama_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Jenis Kelamin Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isGenderExpanded,
                        onExpandedChange = { isGenderExpanded = !isGenderExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = jenisKelamin,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Jenis Kelamin") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isGenderExpanded,
                            onDismissRequest = { isGenderExpanded = false }
                        ) {
                            Penduduk.GENDER_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        jenisKelamin = option
                                        isGenderExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tempat Lahir & Tanggal Lahir
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tempatLahir,
                            onValueChange = { tempatLahir = it },
                            label = { Text("Tempat Lahir") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("resident_form_tempat_lahir_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = tanggalLahir,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tanggal Lahir") },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { datePickerDialog.show() }
                                .testTag("resident_form_tanggal_lahir_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Alamat jalan
                    OutlinedTextField(
                        value = alamat,
                        onValueChange = { alamat = it },
                        label = { Text("Alamat (Jalan / Dusun / Gang)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resident_form_alamat_input"),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // RT and RW
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = rt,
                            onValueChange = { if (it.length <= 4) rt = it },
                            label = { Text("RT") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = rw,
                            onValueChange = { if (it.length <= 4) rw = it },
                            label = { Text("RW") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kelurahan & Kecamatan
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = kelurahan,
                            onValueChange = { kelurahan = it },
                            label = { Text("Kelurahan / Desa") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = kecamatan,
                            onValueChange = { kecamatan = it },
                            label = { Text("Kecamatan") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Agama Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isAgamaExpanded,
                        onExpandedChange = { isAgamaExpanded = !isAgamaExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = agama,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Agama") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAgamaExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isAgamaExpanded,
                            onDismissRequest = { isAgamaExpanded = false }
                        ) {
                            Penduduk.AGAMA_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        agama = option
                                        isAgamaExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Perkawinan Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isStatusExpanded,
                        onExpandedChange = { isStatusExpanded = !isStatusExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = statusPerkawinan,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Status Perkawinan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isStatusExpanded,
                            onDismissRequest = { isStatusExpanded = false }
                        ) {
                            Penduduk.STATUS_PERKAWINAN_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        statusPerkawinan = option
                                        isStatusExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pekerjaan
                    OutlinedTextField(
                        value = pekerjaan,
                        onValueChange = { pekerjaan = it },
                        label = { Text("Pekerjaan") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("resident_form_pekerjaan_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Kewarganegaraan Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isWnExpanded,
                        onExpandedChange = { isWnExpanded = !isWnExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = kewarganegaraan,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kewarganegaraan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isWnExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = isWnExpanded,
                            onDismissRequest = { isWnExpanded = false }
                        ) {
                            Penduduk.KEWARGANEGARAAN_OPTIONS.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        kewarganegaraan = option
                                        isWnExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (nik.length != 16 || !nik.all { it.isDigit() }) {
                                errorMessage = "NIK harus terdiri dari 16 digit angka."
                                return@Button
                            }
                            if (nama.isBlank()) {
                                errorMessage = "Nama lengkap wajib diisi."
                                return@Button
                            }

                            val toSave = resident?.copy(
                                nik = nik.trim(),
                                nama = nama.trim(),
                                tempatLahir = tempatLahir.trim(),
                                tanggalLahir = tanggalLahir.trim(),
                                jenisKelamin = jenisKelamin.trim(),
                                alamat = alamat.trim(),
                                rt = rt.trim(),
                                rw = rw.trim(),
                                kelurahan = kelurahan.trim(),
                                kecamatan = kecamatan.trim(),
                                agama = agama.trim(),
                                statusPerkawinan = statusPerkawinan.trim(),
                                pekerjaan = pekerjaan.trim(),
                                kewarganegaraan = kewarganegaraan.trim(),
                                updatedAt = System.currentTimeMillis()
                            ) ?: Penduduk(
                                nik = nik.trim(),
                                nama = nama.trim(),
                                tempatLahir = tempatLahir.trim(),
                                tanggalLahir = tanggalLahir.trim(),
                                jenisKelamin = jenisKelamin.trim(),
                                alamat = alamat.trim(),
                                rt = rt.trim(),
                                rw = rw.trim(),
                                kelurahan = kelurahan.trim(),
                                kecamatan = kecamatan.trim(),
                                agama = agama.trim(),
                                statusPerkawinan = statusPerkawinan.trim(),
                                pekerjaan = pekerjaan.trim(),
                                kewarganegaraan = kewarganegaraan.trim()
                            )

                            onSave(toSave)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("resident_form_save_button")
                    ) {
                        Text("Simpan Data")
                    }
                }
            }
        }
    }
}
