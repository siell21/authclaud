package siell.claud.authenticator.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import siell.claud.authenticator.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import siell.claud.authenticator.utils.PrefsUtils

@Composable
fun LockScreen(authViewModel: AuthViewModel, onUnlockSuccess: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val method by authViewModel.appLockMethod.collectAsState()
    
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var failedAttempts by remember { mutableStateOf(0) }
    var savedPin by remember { mutableStateOf("") }
    var showBiometricPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(method) {
        if (method == "pin") {
            savedPin = PrefsUtils.getAppLockPin(context).first()
        } else if (method == "biometric") {
            showBiometricPrompt = true
        }
    }

    if (showBiometricPrompt && context is FragmentActivity) {
        LaunchedEffect(Unit) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(context, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onUnlockSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        errorMessage = errString.toString()
                        showBiometricPrompt = false
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        errorMessage = "Biometric authentication failed."
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Cloud Authenticator")
                .setSubtitle("Use your biometric credential")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()

            biometricPrompt.authenticate(promptInfo)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (method == "pin") {
            Text("Enter PIN", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pinInput,
                onValueChange = { pinInput = it },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (pinInput == savedPin) {
                    onUnlockSuccess()
                } else {
                    failedAttempts++
                    errorMessage = "Incorrect PIN"
                    pinInput = ""
                }
            }) {
                Text("Unlock")
            }
            
            if (failedAttempts >= 3) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = {
                    // Reset PIN by logging out
                    authViewModel.logout()
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Reset PIN (Log Out)")
                }
            }
        } else if (method == "biometric") {
            if (!showBiometricPrompt) {
                Button(onClick = { showBiometricPrompt = true }) {
                    Text("Retry Biometric")
                }
            }
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
        }
    }
}
