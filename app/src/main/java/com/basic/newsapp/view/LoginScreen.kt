package com.basic.newsapp.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.basic.newsapp.viewModel.AuthViewModel
import androidx.compose.material3.TextButton


@Composable
fun LoginApp(authViewModel: AuthViewModel = viewModel()) {
    val isLoggedIn by authViewModel.isLoggedIn
    val isLoginMode by authViewModel.isLoginMode

    if (isLoggedIn) {
        ArticleApp(authViewModel.email)
    } else {
        LoginScreen(
            isLoginMode = isLoginMode,
            onAuthClicked = { email, password ->
                if (isLoginMode) {
                    authViewModel.signIn(email, password)
                } else {
                    authViewModel.signUp(email, password)
                }
            },
            onToggleMode = {
                authViewModel.changeAuthMode()
            }
        )
    }
}


@Composable
fun LoginScreen(
    isLoginMode: Boolean,
    onAuthClicked: (String, String) -> Unit,
    onToggleMode: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isLoginMode) "Logowanie" else "Rejestracja",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            TextField(email, onValueChange = { email = it }, placeholder = { Text("Email") })
            Spacer(modifier = Modifier.height(8.dp))
            TextField(password, onValueChange = { password = it }, placeholder = { Text("Hasło") })
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { onAuthClicked(email, password) }) {
                Text(if (isLoginMode) "Zaloguj" else "Zarejestruj")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onToggleMode) {
                Text(
                    if (isLoginMode) "Nie masz konta? Zarejestruj się"
                    else "Masz już konto? Zaloguj się"
                )
            }
        }
    }
}