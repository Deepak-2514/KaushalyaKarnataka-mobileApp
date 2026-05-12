package com.kaushalyakarnataka.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kaushalyakarnataka.app.BuildConfig
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.util.findActivity
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    strings: Strings,
    repo: KaushalyaRepository,
    onSignedIn: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val activity = LocalContext.current.findActivity()
    val scope = rememberCoroutineScope()
    var isSigningIn by remember { mutableStateOf(false) }

    val googleSignInClient = remember(activity) {
        val webId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webId)
            .requestEmail()
            .requestProfile()
            .build()
        activity?.let { GoogleSignIn.getClient(it, gso) }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val token = account?.idToken
            
            if (token.isNullOrBlank()) {
                isSigningIn = false
                onMessage("Error: Missing token. Check Firebase config.")
            } else {
                scope.launch {
                    runCatching { repo.signInWithGoogleIdToken(token) }
                        .onSuccess { 
                            onSignedIn() 
                        }
                        .onFailure { 
                            isSigningIn = false
                            onMessage(it.message ?: "Login failed") 
                        }
                }
            }
        } catch (e: ApiException) {
            isSigningIn = false
            val msg = when (e.statusCode) {
                12501 -> "Sign-in cancelled."
                7 -> "Network error. Try again."
                10 -> "Developer Error (10): Likely SHA-1 mismatch in Firebase."
                else -> "Login failed (Code: ${e.statusCode})."
            }
            onMessage(msg)
        }
    }

    val webIdConfigured = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineMuted = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)

    GalaxyBackground {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                strings.appName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                strings.tagline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        enabled = !isSigningIn && activity != null && webIdConfigured,
                        onClick = {
                            val client = googleSignInClient ?: return@OutlinedButton
                            isSigningIn = true
                            launcher.launch(client.signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.38f),
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.alpha(if (isSigningIn) 0f else 1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White,
                                ) {
                                    Text(
                                        "G",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Spacer(Modifier.size(16.dp))
                                Text(
                                    strings.googleSignIn,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (!webIdConfigured) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Google sign-in isn't configured for this install yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text(
                strings.safeLogin,
                style = MaterialTheme.typography.labelMedium,
                color = outlineMuted,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
