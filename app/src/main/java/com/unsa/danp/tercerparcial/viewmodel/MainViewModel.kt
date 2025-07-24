package com.unsa.danp.tercerparcial.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager

import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.unsa.danp.tercerparcial.model.*
import com.unsa.danp.tercerparcial.repository.FirebaseRepository
import com.unsa.danp.tercerparcial.receiver.BluetoothScanReceiver
import com.unsa.danp.tercerparcial.service.BluetoothScanService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val firebaseRepository = FirebaseRepository()
    private val context = getApplication<Application>()
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _currentUser = MutableStateFlow<Usuario?>(null)
    val currentUser: StateFlow<Usuario?> = _currentUser.asStateFlow()
    
    private val _usuarios = MutableStateFlow<List<Usuario>>(emptyList())
    val usuarios: StateFlow<List<Usuario>> = _usuarios.asStateFlow()
    
    private val _contactos = MutableStateFlow<List<ContactoCercano>>(emptyList())
    val contactos: StateFlow<List<ContactoCercano>> = _contactos.asStateFlow()
    
    private val _isBluetoothScanning = MutableStateFlow(false)
    val isBluetoothScanning: StateFlow<Boolean> = _isBluetoothScanning.asStateFlow()

    fun getEthMac(): String {
        var macAddress = "Not able to read the MAC address"
        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader("/sys/class/net/eth0/address"))
            macAddress = br.readLine().uppercase()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return macAddress
    }

    private fun obtenerMacAddress(): String {
        // Método 1: Usar BluetoothAdapter (más confiable)
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter != null) {
            val bluetoothMac = bluetoothAdapter.address
            if (bluetoothMac != null && bluetoothMac != "02:00:00:00:00:00") {
                Log.d("MAC Address", "Obtenida via Bluetooth: $bluetoothMac")
                return bluetoothMac
            }
        }
        
        // Método 2: Leer desde archivo del sistema (para dispositivos rooteados)
        try {
            val ethMac = getEthMac()
            if (ethMac != "Not able to read the MAC address" && ethMac != "02:00:00:00:00:00") {
                Log.d("MAC Address", "Obtenida via archivo: $ethMac")
                return ethMac
            }
        } catch (e: Exception) {
            Log.e("MAC Address", "Error leyendo archivo: ${e.message}")
        }
        
        // Método 3: Generar MAC única basada en características del dispositivo
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        )
        
        val uniqueMac = generateMacFromDeviceId(deviceId)
        Log.d("MAC Address", "Generada única: $uniqueMac")
        return uniqueMac
    }
    
    private fun generateMacFromDeviceId(deviceId: String): String {
        val hash = deviceId.hashCode()
        val macBytes = ByteArray(6)
        macBytes[0] = 0x02.toByte() // MAC local
        macBytes[1] = (hash shr 16).toByte()
        macBytes[2] = (hash shr 8).toByte()
        macBytes[3] = hash.toByte()
        macBytes[4] = (hash shr 24).toByte()
        macBytes[5] = (hash shr 16).toByte()
        
        return macBytes.joinToString(":") { "%02X".format(it) }
    }
    
    fun login(dni: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            
            try {
                val macAddress = obtenerMacAddress()
                Log.d("1------------------------", obtenerMacAddress().toString())
                Log.d("12------------------------", getEthMac().toString())
                val usuario = firebaseRepository.obtenerUsuarioPorMac(macAddress, context).getOrNull()
                
                if (usuario != null) {
                    _currentUser.value = usuario
                    _uiState.value = UiState.Success
                    
                    // Iniciar servicio de escaneo Bluetooth
                    iniciarServicioBluetooth(dni, macAddress)
                    
                    // Cargar datos según el rol
                    when (usuario.rol) {
                        Rol.AUTORIDAD -> cargarDatosAutoridad()
                        Rol.PERSONA -> cargarDatosPersona()
                    }
                } else {
                    // Crear nuevo usuario (por defecto como PERSONA)
                    val nuevoUsuario = Usuario(dni, Rol.PERSONA, EstadoSalud.SANO, macAddress)
                    Log.d("--------------------------------", "a--------------------------------")
                    firebaseRepository.guardarUsuario(nuevoUsuario, context)
                    _currentUser.value = nuevoUsuario
                    _uiState.value = UiState.Success
                    
                    iniciarServicioBluetooth(dni, macAddress)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al iniciar sesión: ${e.message}")
            }
        }
    }
    
    fun cargarDatosAutoridad() {
        viewModelScope.launch {
            try {
                val usuarios = firebaseRepository.obtenerTodosLosUsuarios(context).getOrNull() ?: emptyList()
                _usuarios.value = usuarios
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al cargar usuarios: ${e.message}")
            }
        }
    }
    
    fun cargarDatosPersona() {
        val currentUser = _currentUser.value ?: return
        viewModelScope.launch {
            try {
                val contactos = firebaseRepository.obtenerContactos(currentUser.dni, context).getOrNull() ?: emptyList()
                _contactos.value = contactos
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al cargar contactos: ${e.message}")
            }
        }
    }
    
    fun actualizarEstadoSalud(dni: String, nuevoEstado: EstadoSalud) {
        viewModelScope.launch {
            try {
                firebaseRepository.actualizarEstadoSalud(dni, nuevoEstado, context)
                cargarDatosAutoridad() // Recargar datos
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al actualizar estado: ${e.message}")
            }
        }
    }
    
    fun iniciarEscaneoManual() {
        Log.d("MainViewModel", "Iniciando escaneo manual...")
        _isBluetoothScanning.value = true
        
        val currentUser = _currentUser.value ?: return
        
        // Iniciar el servicio de escaneo Bluetooth
        val intent = Intent(context, BluetoothScanService::class.java).apply {
            putExtra(BluetoothScanService.EXTRA_USER_DNI, currentUser.dni)
            putExtra(BluetoothScanService.EXTRA_USER_MAC_ADDRESS, currentUser.macAddress)
        }
        context.startForegroundService(intent)
        
        // Detener el indicador de escaneo después de 10 segundos
        viewModelScope.launch {
            kotlinx.coroutines.delay(10000)
            _isBluetoothScanning.value = false
            Log.d("MainViewModel", "Escaneo manual completado")
        }
    }
    
    fun detenerEscaneoManual() {
        Log.d("MainViewModel", "Deteniendo escaneo manual...")
        _isBluetoothScanning.value = false
        val intent = Intent(context, BluetoothScanService::class.java)
        context.stopService(intent)
    }
    
    private fun iniciarServicioBluetooth(dni: String, macAddress: String) {
        Log.d("MainViewModel", "Iniciando servicio Bluetooth para DNI: $dni, MAC: $macAddress")
        
        // Programar escaneos intermitentes
        BluetoothScanReceiver.scheduleNextScan(context, dni, macAddress)
        
        // Iniciar primer escaneo
        val intent = Intent(context, BluetoothScanService::class.java).apply {
            putExtra(BluetoothScanService.EXTRA_USER_DNI, dni)
            putExtra(BluetoothScanService.EXTRA_USER_MAC_ADDRESS, macAddress)
        }
        context.startForegroundService(intent)
    }
    
    fun verificarPermisos(): Boolean {
        // Verificar permisos Bluetooth
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            _uiState.value = UiState.Error("Bluetooth no disponible en este dispositivo")
            return false
        }
        
        if (!bluetoothAdapter.isEnabled) {
            _uiState.value = UiState.Error("Bluetooth debe estar habilitado")
            return false
        }
        
        // Verificar permisos de ubicación
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            _uiState.value = UiState.Error("Ubicación debe estar habilitada")
            return false
        }
        
        return true
    }
    
    fun logout() {
        _currentUser.value = null
        _usuarios.value = emptyList()
        _contactos.value = emptyList()
        _uiState.value = UiState.Idle
        
        // Detener servicios
        BluetoothScanReceiver.cancelScheduledScans(context)
        val intent = Intent(context, BluetoothScanService::class.java)
        context.stopService(intent)
    }
    
    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        object Success : UiState()
        data class Error(val message: String) : UiState()
    }
} 