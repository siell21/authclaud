package siell.claud.authenticator.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import siell.claud.authenticator.data.AuthAccount
import siell.claud.authenticator.ui.theme.*
import siell.claud.authenticator.ui.viewmodels.AuthViewModel
import siell.claud.authenticator.utils.TotpUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    onNavigateToAddAccount: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val accounts by authViewModel.allAccounts.collectAsState(initial = emptyList())
    val profilePicUrl by authViewModel.userProfilePicUrl.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var revealedAccountId by remember { mutableStateOf<String?>(null) }
    
    val filteredAccounts = if (searchQuery.isEmpty()) accounts else accounts.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.issuer.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddAccount,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            // Custom Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cloud Authenticator",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "siell.claud.authenticator",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onNavigateToProfile() }
                    ) {
                        if (profilePicUrl != null) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }

            // Sync and Search section
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${accounts.size} accounts secured",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 14.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF4ADE80))) // Pulse green dot
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYNC ACTIVE",
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("Search accounts...", color = MaterialTheme.colorScheme.secondary, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }
            
            // List
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAccounts, key = { it.id }) { account ->
                    AccountItem(
                        account = account,
                        isRevealed = revealedAccountId == account.id,
                        onToggleReveal = {
                            revealedAccountId = if (revealedAccountId == account.id) null else account.id
                        },
                        onDelete = { authViewModel.deleteAccount(account.id) },
                        onEdit = { newName -> authViewModel.updateAccount(account.copy(name = newName)) }
                    )
                }
            }
        }
    }
}

@Composable
fun AccountItem(
    account: AuthAccount,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit
) {
    val context = LocalContext.current
    var currentCode by remember { mutableStateOf("••••••") }
    var progress by remember { mutableStateOf(1f) }
    
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            val decryptedSecret = siell.claud.authenticator.utils.CryptoUtils.decryptLocal(account.secretKeyEncrypted)
            while (true) {
                val time = System.currentTimeMillis()
                currentCode = TotpUtils.generateTotp(decryptedSecret, time)
                val timeStep = 30000L
                val timeRemaining = timeStep - (time % timeStep)
                progress = timeRemaining.toFloat() / timeStep.toFloat()
                delay(100) // Update progress bar every 100ms
            }
        } else {
            currentCode = "••••••"
            progress = 0f
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isRevealed) {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("TOTP Code", currentCode))
                    // Optional toast
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left purple border indicator for revealed state
            if (isRevealed) {
                Box(modifier = Modifier.width(4.dp).height(80.dp).background(MaterialTheme.colorScheme.primary))
            } else {
                Box(modifier = Modifier.width(4.dp).height(80.dp))
            }
            
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp).weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = account.issuer.uppercase(),
                            color = if (isRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• ${account.name}",
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isRevealed) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${currentCode.substring(0, 3)} ${currentCode.substring(3, 6)}",
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Light,
                                letterSpacing = 4.sp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            // Progress bar pill
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .width(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(progress)
                                        .align(Alignment.BottomCenter)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "••••••",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            fontSize = 32.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 4.sp
                        )
                    }
                }
                
                IconButton(onClick = onToggleReveal) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (isRevealed) "Hide code" else "Reveal code",
                        tint = if (isRevealed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
                
                var showEditDialog by remember { mutableStateOf(false) }
            var editName by remember { mutableStateOf(account.name) }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            showEditDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
            
            if (showEditDialog) {
                AlertDialog(
                    onDismissRequest = { showEditDialog = false },
                    title = { Text("Edit Name") },
                    text = {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text("Account Name") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (editName.isNotBlank()) {
                                onEdit(editName)
                            }
                            showEditDialog = false
                        }) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}
}

