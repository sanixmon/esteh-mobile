package com.evrenhouse.esteh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.evrenhouse.esteh.ui.PosScreen
import com.evrenhouse.esteh.ui.theme.EstehTheme
import com.evrenhouse.esteh.viewmodel.ClockViewModel
import com.evrenhouse.esteh.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as EstehApp).container
        val vm: PosViewModel by viewModels { container.vmFactory }
        val clockVm: ClockViewModel by viewModels { container.vmFactory }
        setContent {
            EstehTheme {
                PosScreen(vm, clockVm)
            }
        }
    }
}
