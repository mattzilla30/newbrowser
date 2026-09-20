package com.newbrowser.app.ui

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Full-screen gate shown over private tabs after the app returns from the background, when
 * "Lock private tabs" is on. Auto-prompts for biometrics/PIN on first composition; the
 * buttons below cover the case where the user dismisses that system prompt without acting.
 */
@Composable
fun LockScreen(onUnlocked: () -> Unit, onCloseTabsInstead: () -> Unit) {
    val activity = LocalContext.current as FragmentActivity

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Private tabs locked")
            .setSubtitle("Verify it's you to continue")
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
    }

    val biometricPrompt = remember {
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            },
        )
    }

    fun authenticate() = biometricPrompt.authenticate(promptInfo)

    LaunchedEffect(Unit) { authenticate() }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text(
                text = "Private tabs are locked",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Verify it's you to see your private tabs.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )
            Button(onClick = ::authenticate) {
                Text("Unlock")
            }
            TextButton(onClick = onCloseTabsInstead, modifier = Modifier.padding(top = 8.dp)) {
                Text("Close private tabs instead")
            }
        }
    }
}
