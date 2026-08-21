package siell.claud.authenticator.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onBack: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(2) } // Default to Manual for now
    
    val tabs = listOf("Scan QR", "Upload Image", "Manual Input")
    
    val handleScannedUrl = { url: String ->
        try {
            val uri = Uri.parse(url)
            if (uri.scheme == "otpauth" && uri.host == "totp") {
                val secret = uri.getQueryParameter("secret") ?: ""
                var issuer = uri.getQueryParameter("issuer") ?: ""
                var name = uri.path?.removePrefix("/") ?: ""
                
                if (name.contains(":")) {
                    val parts = name.split(":", limit = 2)
                    if (issuer.isEmpty()) issuer = parts[0]
                    name = parts[1].trim()
                }
                
                if (secret.isNotBlank()) {
                    onSave(name, issuer, secret)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Akun") },
                navigationIcon = {
                    Button(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (selectedTab) {
                0 -> QrScannerView(onQrScanned = handleScannedUrl)
                1 -> UploadImageView(context, onQrScanned = handleScannedUrl)
                2 -> ManualInputForm(onSave)
            }
        }
    }
}

@Composable
fun ManualInputForm(onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var issuer by remember { mutableStateOf("") }
    var secretKey by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Account Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = issuer,
            onValueChange = { issuer = it },
            label = { Text("Issuer (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = secretKey,
            onValueChange = { secretKey = it },
            label = { Text("Secret Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onSave(name, issuer, secretKey.replace(" ", "").uppercase()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && secretKey.isNotBlank()
        ) {
            Text("Simpan")
        }
    }
}
