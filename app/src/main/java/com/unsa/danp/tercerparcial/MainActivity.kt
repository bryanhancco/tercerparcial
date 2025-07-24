package com.unsa.danp.tercerparcial

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.unsa.danp.tercerparcial.model.Rol
import com.unsa.danp.tercerparcial.ui.screens.AutoridadScreen
import com.unsa.danp.tercerparcial.ui.screens.LoginScreen
import com.unsa.danp.tercerparcial.ui.screens.PersonaScreen
import com.unsa.danp.tercerparcial.ui.theme.TercerparcialTheme
import com.unsa.danp.tercerparcial.viewmodel.MainViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            viewModel.verificarPermisos()
        }
    }
    
    private val requestBluetoothEnable = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // Bluetooth habilitado exitosamente
            viewModel.verificarPermisos()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Solicitar permisos al iniciar
        requestPermissions()
        
        setContent {
            TercerparcialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppContent(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
    
    @Composable
    fun AppContent(
        modifier: Modifier = Modifier,
        viewModel: MainViewModel
    ) {
        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
        when {
            currentUser == null -> {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        // Permisos ya verificados en onCreate
                    }
                )
            }
            currentUser?.rol == Rol.AUTORIDAD -> {
                AutoridadScreen(
                    viewModel = viewModel,
                    currentUser = currentUser!!,
                    onLogout = {
                        viewModel.logout()
                    }
                )
            }
            else -> {
                PersonaScreen(
                    viewModel = viewModel,
                    currentUser = currentUser!!,
                    onLogout = {
                        viewModel.logout()
                    }
                )
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        // Permisos Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            permissions.addAll(
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }
        
        // Permisos de ubicación
        permissions.addAll(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        
        // Verificar si ya están concedidos
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkBluetoothEnabled()
        }
    }
    
    private fun checkBluetoothEnabled() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            // Verificar permisos antes de solicitar habilitar Bluetooth
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    requestBluetoothEnable.launch(enableBtIntent)
                }
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetoothEnable.launch(enableBtIntent)
            }
        } else {
            viewModel.verificarPermisos()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Solo verificar Bluetooth si ya tenemos permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                checkBluetoothEnabled()
            }
        } else {
            checkBluetoothEnabled()
        }
    }
}