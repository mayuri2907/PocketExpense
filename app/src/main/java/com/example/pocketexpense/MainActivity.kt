package com.example.pocketexpense

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.pocketexpense.ui.screens.MainScreen
import com.example.pocketexpense.ui.theme.PocketExpenseTheme
import com.example.pocketexpense.ui.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        val app = application as PocketExpenseApplication
        ExpenseViewModel.provideFactory(app.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketExpenseTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }
}