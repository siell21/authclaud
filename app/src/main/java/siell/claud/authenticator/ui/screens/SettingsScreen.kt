package siell.claud.authenticator.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import siell.claud.authenticator.ui.viewmodels.AuthViewModel
import siell.claud.authenticator.utils.PrefsUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isAppLockEnabled by authViewModel.isAppLockEnabled.collectAsState()
    val syncStatus by authViewModel.syncStatus.collectAsState()
    
    val lastBackup by PrefsUtils.getLastBackupTime(context).collectAsState(initial = 0L)
    val lastRestore by PrefsUtils.getLastRestoreTime(context).collectAsState(initial = 0L)
    
    var showLockDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    
    val format = SimpleDateFormat("EEEE, dd MMMM yyyy — HH:mm:ss", Locale("id", "ID"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            // App Lock
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Kunci Aplikasi (App Lock)", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showLockDialog = true
                        } else {
                            authViewModel.setAppLock(false, "", "")
                        }
                    }
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Sync
            Text("Sinkronisasi Google Drive", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { authViewModel.performManualBackup() }) {
                    Text("Backup Sekarang")
                }
                Button(onClick = { showRestoreDialog = true }) {
                    Text("Pulihkan Sekarang")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val backupText = if (lastBackup > 0) format.format(Date(lastBackup)) else "Belum pernah"
            val restoreText = if (lastRestore > 0) format.format(Date(lastRestore)) else "Belum pernah"
            
            Text("Backup terakhir: $backupText", style = MaterialTheme.typography.bodyMedium)
            Text("Restore terakhir: $restoreText", style = MaterialTheme.typography.bodyMedium)
            
            if (syncStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status: $syncStatus",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (showLockDialog) {
            var selectedMethod by remember { mutableStateOf("biometric") }
            var pinInput by remember { mutableStateOf("") }
            
            AlertDialog(
                onDismissRequest = { showLockDialog = false },
                title = { Text("Pilih Metode Keamanan") },
                text = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedMethod == "biometric", onClick = { selectedMethod = "biometric" })
                            Text("Biometric (Sidik Jari / Face Unlock)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedMethod == "pin", onClick = { selectedMethod = "pin" })
                            Text("Manual PIN")
                        }
                        if (selectedMethod == "pin") {
                            OutlinedTextField(
                                value = pinInput,
                                onValueChange = { pinInput = it },
                                label = { Text("Masukkan PIN") },
                                singleLine = true
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            authViewModel.setAppLock(true, selectedMethod, pinInput)
                            showLockDialog = false
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLockDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
        
        if (showRestoreDialog) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = false },
                title = { Text("Konfirmasi Restore") },
                text = { Text("Data akun saat ini akan digantikan dengan data dari backup terakhir. Lanjutkan?") },
                confirmButton = {
                    Button(
                        onClick = {
                            authViewModel.performManualRestore()
                            showRestoreDialog = false
                        }
                    ) {
                        Text("Ya, Lanjutkan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
