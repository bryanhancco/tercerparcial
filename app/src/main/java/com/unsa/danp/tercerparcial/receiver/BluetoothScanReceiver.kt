package com.unsa.danp.tercerparcial.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.unsa.danp.tercerparcial.service.BluetoothScanService

class BluetoothScanReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarma de escaneo Bluetooth recibida")
        
        val serviceIntent = Intent(context, BluetoothScanService::class.java)
        
        // Obtener datos del usuario desde el intent
        val userDni = intent.getStringExtra(EXTRA_USER_DNI)
        val userMacAddress = intent.getStringExtra(EXTRA_USER_MAC_ADDRESS)
        
        if (userDni != null && userMacAddress != null) {
            serviceIntent.putExtra(BluetoothScanService.EXTRA_USER_DNI, userDni)
            serviceIntent.putExtra(BluetoothScanService.EXTRA_USER_MAC_ADDRESS, userMacAddress)
        }
        
        // Usar startForegroundService solo en API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        // Programar siguiente escaneo aleatorio (30-60 minutos)
        scheduleNextScan(context, userDni, userMacAddress)
    }
    
    companion object {
        private const val TAG = "BluetoothScanReceiver"
        private const val ACTION_SCAN_BLUETOOTH = "com.unsa.danp.tercerparcial.SCAN_BLUETOOTH"
        private const val EXTRA_USER_DNI = "user_dni"
        private const val EXTRA_USER_MAC_ADDRESS = "user_mac_address"
        
        fun scheduleNextScan(context: Context, userDni: String?, userMacAddress: String?) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, BluetoothScanReceiver::class.java).apply {
                action = ACTION_SCAN_BLUETOOTH
                putExtra(EXTRA_USER_DNI, userDni)
                putExtra(EXTRA_USER_MAC_ADDRESS, userMacAddress)
            }
            
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            // Tiempo aleatorio entre 30 y 60 minutos
            val randomMinutes = (30..60).random()
            val triggerTime = System.currentTimeMillis() + (randomMinutes * 60 * 1000L)
            
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            
            Log.d(TAG, "Próximo escaneo programado en $randomMinutes minutos")
        }
        
        fun cancelScheduledScans(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, BluetoothScanReceiver::class.java).apply {
                action = ACTION_SCAN_BLUETOOTH
            }
            
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                0,
                intent,
                android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }
} 