package com.unsa.danp.tercerparcial.service

import android.Manifest
import android.app.*
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.unsa.danp.tercerparcial.R
import com.unsa.danp.tercerparcial.model.ContactoCercano
import com.unsa.danp.tercerparcial.model.EstadoSalud
import com.unsa.danp.tercerparcial.repository.FirebaseRepository
import kotlinx.coroutines.*
import java.util.*

class BluetoothScanService : Service() {
    private val binder = LocalBinder()
    private val firebaseRepository = FirebaseRepository()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var isScanning = false
    private var isAdvertising = false
    private var currentUserDni: String? = null
    private var currentUserMacAddress: String? = null
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            processScanResult(result)
        }
        
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Error en escaneo Bluetooth: $errorCode")
        }
    }
    
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "BLE Advertising iniciado exitosamente")
            isAdvertising = true
        }
        
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Error al iniciar BLE Advertising: $errorCode")
            isAdvertising = false
        }
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): BluetoothScanService = this@BluetoothScanService
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentUserDni = intent?.getStringExtra(EXTRA_USER_DNI)
        currentUserMacAddress = intent?.getStringExtra(EXTRA_USER_MAC_ADDRESS)
        Log.d(TAG, "Servicio iniciado con DNI: $currentUserDni, MAC: $currentUserMacAddress")
        
        // Iniciar BLE Advertising para publicar nuestra MAC
        iniciarBLEAdvertising()
        
        // Iniciar escaneo automáticamente cuando se inicia el servicio
        iniciarEscaneo()
        
        return START_STICKY
    }
    
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT])
    private fun iniciarBLEAdvertising() {
        if (isAdvertising) {
            Log.d(TAG, "BLE Advertising ya en progreso")
            return
        }
        
        Log.d(TAG, "Iniciando BLE Advertising...")
        
        // Verificar permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permisos Bluetooth no concedidos para advertising")
                return
            }
        }
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth no disponible en este dispositivo")
            return
        }
        
        if (!bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "Bluetooth deshabilitado")
            return
        }
        
        bluetoothLeAdvertiser = bluetoothAdapter!!.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            Log.w(TAG, "Bluetooth LE Advertiser no disponible")
            return
        }
        
        // Configurar settings de advertising
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0) // Sin timeout
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
        
        // Crear datos de advertising con nuestra MAC address
        val macBytes = currentUserMacAddress?.let { mac ->
            mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
        } ?: ByteArray(6)
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addManufacturerData(MANUFACTURER_ID, macBytes) // Incluir MAC en manufacturer data
            .build()
        
        try {
            bluetoothLeAdvertiser!!.startAdvertising(settings, data, advertiseCallback)
            Log.d(TAG, "BLE Advertising iniciado con MAC: $currentUserMacAddress")
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar BLE Advertising: ${e.message}")
        }
    }
    
    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
    fun iniciarEscaneo() {
        if (isScanning) {
            Log.d(TAG, "Escaneo ya en progreso")
            return
        }
        
        Log.d(TAG, "Iniciando escaneo Bluetooth...")
        
        // Verificar permisos antes de usar Bluetooth
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permisos Bluetooth no concedidos")
                return
            }
        }
        
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth no disponible en este dispositivo")
            return
        }
        
        if (!bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "Bluetooth deshabilitado")
            return
        }
        
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.w(TAG, "Bluetooth LE Scanner no disponible")
            return
        }
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        // Escanear todos los dispositivos, no solo los que tienen nombre específico
        try {
            bluetoothLeScanner!!.startScan(null, scanSettings, scanCallback)
            isScanning = true
            Log.d(TAG, "Escaneo Bluetooth iniciado exitosamente")
            
            // Detener escaneo después de 10 segundos
            CoroutineScope(Dispatchers.Main).launch {
                delay(10000)
                detenerEscaneo()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar escaneo: ${e.message}")
        }
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun detenerEscaneo() {
        if (!isScanning) {
            Log.d(TAG, "No hay escaneo activo para detener")
            return
        }
        
        // Verificar permisos antes de detener
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Permisos Bluetooth no concedidos")
                return
            }
        }
        
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            isScanning = false
            Log.d(TAG, "Escaneo Bluetooth detenido")
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener escaneo: ${e.message}")
        }
    }
    
    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val rssi = result.rssi
        val deviceName = device.name ?: ""
        val deviceAddress = device.address
        
        Log.d(TAG, "Dispositivo detectado: $deviceName ($deviceAddress), RSSI: $rssi")
        
        // Extraer MAC address real desde manufacturer data
        val realMacAddress = extractMacFromManufacturerData(result)
        
        if (realMacAddress != null) {
            Log.d(TAG, "MAC real extraída: $realMacAddress")
            
            // Solo procesar dispositivos con señal fuerte (cercanos)
            if (rssi > -70) {
                Log.d(TAG, "Dispositivo cercano con MAC real: $realMacAddress, RSSI: $rssi")
                
                // Buscar usuario por MAC address real en Firebase
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val usuarioContacto = firebaseRepository.obtenerUsuarioPorMac(realMacAddress, this@BluetoothScanService).getOrNull()
                        
                        if (usuarioContacto != null && currentUserDni != null && currentUserMacAddress != null) {
                            Log.d(TAG, "Usuario contacto encontrado: ${usuarioContacto.estadoSalud}")
                            
                            val contacto = ContactoCercano(
                                dniUsuario = currentUserDni!!,
                                macAddressUsuario = currentUserMacAddress!!,
                                macAddressContacto = realMacAddress,
                                fechaHora = System.currentTimeMillis(),
                                intensidadRssi = rssi
                            )
                            
                            // Guardar el contacto
                            val saveResult = firebaseRepository.guardarContactos(
                                currentUserDni!!,
                                listOf(contacto),
                                this@BluetoothScanService
                            )
                            
                            if (saveResult.isSuccess) {
                                Log.d(TAG, "Contacto guardado exitosamente")
                                
                                // Aplicar la regla: si el contacto está infectado o posiblemente infectado,
                                // cambiar el estado del usuario actual a posiblemente infectado
                                if (usuarioContacto.estadoSalud == EstadoSalud.INFECTADO || 
                                    usuarioContacto.estadoSalud == EstadoSalud.POSIBLE_INFECTADO) {
                                    
                                    Log.d(TAG, "Contacto infectado detectado, marcando usuario como posiblemente infectado")
                                    firebaseRepository.marcarComoPosiblementeInfectado(currentUserDni!!, this@BluetoothScanService)
                                } else {
                                    Log.d(TAG, "Contacto sano, no se cambia el estado")
                                }
                            } else {
                                Log.e(TAG, "Error al guardar contacto: ${saveResult.exceptionOrNull()?.message}")
                            }
                        } else {
                            Log.d(TAG, "Usuario contacto no encontrado en Firebase o datos de usuario actual incompletos")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al procesar contacto: ${e.message}")
                    }
                }
            }
        } else {
            Log.d(TAG, "No se pudo extraer MAC real del dispositivo")
        }
    }
    
    private fun extractMacFromManufacturerData(result: ScanResult): String? {
        try {
            val scanRecord = result.scanRecord
            if (scanRecord != null) {
                // Obtener los bytes del scan record
                val scanRecordBytes = scanRecord.bytes
                
                // Buscar manufacturer data en los bytes del scan record
                val manufacturerData = extractManufacturerDataFromBytes(scanRecordBytes, MANUFACTURER_ID)
                
                if (manufacturerData != null && manufacturerData.size >= 6) {
                    // Los primeros 6 bytes contienen la MAC address
                    val macBytes = manufacturerData.take(6).toByteArray()
                    return macBytes.joinToString(":") { "%02X".format(it) }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extrayendo MAC de manufacturer data: ${e.message}")
        }
        return null
    }
    
    private fun extractManufacturerDataFromBytes(scanRecordBytes: ByteArray, manufacturerId: Int): ByteArray? {
        var i = 0
        while (i < scanRecordBytes.size - 1) {
            val length = scanRecordBytes[i].toInt() and 0xFF
            if (length == 0) break
            
            if (i + length + 1 > scanRecordBytes.size) break
            
            val type = scanRecordBytes[i + 1].toInt() and 0xFF
            
            // Type 0xFF es manufacturer specific data
            if (type == 0xFF && length >= 3) {
                val dataLength = length - 3 // Restar 1 byte de type y 2 bytes de manufacturer ID
                val receivedManufacturerId = ((scanRecordBytes[i + 2].toInt() and 0xFF) or 
                                           ((scanRecordBytes[i + 3].toInt() and 0xFF) shl 8))
                
                if (receivedManufacturerId == manufacturerId && dataLength > 0) {
                    return scanRecordBytes.copyOfRange(i + 4, i + 4 + dataLength)
                }
            }
            
            i += length + 1
        }
        return null
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "COVID Tracker Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Servicio de rastreo de contactos cercanos"
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("COVID Tracker")
            .setContentText("Monitoreando contactos cercanos...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        detenerEscaneo()
        detenerBLEAdvertising()
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun detenerBLEAdvertising() {
        if (isAdvertising) {
            try {
                bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
                isAdvertising = false
                Log.d(TAG, "BLE Advertising detenido")
            } catch (e: Exception) {
                Log.e(TAG, "Error al detener BLE Advertising: ${e.message}")
            }
        }
    }
    
    companion object {
        private const val TAG = "BluetoothScanService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "covid_tracker_service"
        private const val MANUFACTURER_ID = 0x1234 // ID personalizado para nuestra app
        const val EXTRA_USER_DNI = "user_dni"
        const val EXTRA_USER_MAC_ADDRESS = "user_mac_address"
    }
} 