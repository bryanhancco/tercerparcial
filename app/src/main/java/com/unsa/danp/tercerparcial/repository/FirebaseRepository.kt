package com.unsa.danp.tercerparcial.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.firestore.FirebaseFirestore
import com.unsa.danp.tercerparcial.model.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
    
    // Verificar conectividad
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
    
    // Colección de usuarios
    suspend fun guardarUsuario(usuario: Usuario, context: Context): Result<Unit> {
        return try {
            if (!isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión a internet"))
            }
            
            val usuarioMap = mapOf(
                "dni" to usuario.dni,
                "rol" to usuario.rol.name,
                "estadoSalud" to usuario.estadoSalud.name,
                "macAddress" to usuario.macAddress
            )
            // Usar la MAC address como ID del documento
            db.collection("usuarios").document(usuario.macAddress).set(usuarioMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun obtenerUsuario(dni: String, context: Context): Result<Usuario?> {
        return try {
            if (!isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión a internet"))
            }
            
            val document = db.collection("usuarios").document(dni).get().await()
            if (document.exists()) {
                val data = document.data!!
                val usuario = Usuario(
                    dni = data["dni"] as String,
                    rol = Rol.valueOf(data["rol"] as String),
                    estadoSalud = EstadoSalud.valueOf(data["estadoSalud"] as String),
                    macAddress = data["macAddress"] as String
                )
                Result.success(usuario)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun obtenerUsuarioPorMac(macAddress: String, context: Context): Result<Usuario?> {
        return try {
            if (!isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión a internet"))
            }
            
            val document = db.collection("usuarios").document(macAddress).get().await()
            if (document.exists()) {
                val data = document.data!!
                val usuario = Usuario(
                    dni = data["dni"] as String,
                    rol = Rol.valueOf(data["rol"] as String),
                    estadoSalud = EstadoSalud.valueOf(data["estadoSalud"] as String),
                    macAddress = data["macAddress"] as String
                )
                Result.success(usuario)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun actualizarEstadoSalud(dni: String, estadoSalud: EstadoSalud, context: Context): Result<Unit> {
        return try {
            if (!isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión a internet"))
            }
            
            db.collection("usuarios").document(dni)
                .update("estadoSalud", estadoSalud.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun obtenerTodosLosUsuarios(context: Context): Result<List<Usuario>> {
        return try {
            if (!isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión a internet"))
            }
            
            val snapshot = db.collection("usuarios").get().await()
            val usuarios = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    Usuario(
                        dni = data["dni"] as String,
                        rol = Rol.valueOf(data["rol"] as String),
                        estadoSalud = EstadoSalud.valueOf(data["estadoSalud"] as String),
                        macAddress = data["macAddress"] as String
                    )
                } else null
            }
            Result.success(usuarios)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Colección de contactos
    suspend fun guardarContactos(dniUsuario: String, contactos: List<ContactoCercano>, context: Context): Result<Unit> {
        return try {
            if (!isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión a internet"))
            }
            
            val contactosFirebase = contactos.map { contacto ->
                mapOf(
                    "macAddressContacto" to contacto.macAddressContacto,
                    "dniContacto" to contacto.dniUsuario, // Aquí guardamos el DNI del contacto encontrado
                    "fechaHora" to dateFormat.format(Date(contacto.fechaHora)),
                    "rssi" to contacto.intensidadRssi
                )
            }
            
            val registro = mapOf(
                "contactos" to contactosFirebase
            )
            
            db.collection("contactos").document(dniUsuario).set(registro).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun obtenerContactos(dniUsuario: String, context: Context): Result<List<ContactoCercano>> {
        return try {
            if (!isNetworkAvailable(context)) {
                return Result.failure(Exception("Sin conexión a internet"))
            }
            
            val document = db.collection("contactos").document(dniUsuario).get().await()
            if (document.exists()) {
                val data = document.data!!
                val contactosList = data["contactos"] as? List<*> ?: emptyList<Any>()
                
                val contactos = contactosList.mapNotNull { contacto ->
                    if (contacto is Map<*, *>) {
                        val contactoMap = contacto as Map<String, Any>
                        try {
                            ContactoCercano(
                                dniUsuario = dniUsuario,
                                macAddressUsuario = "", // Este campo se puede obtener del usuario actual
                                macAddressContacto = contactoMap["macAddressContacto"] as String,
                                fechaHora = dateFormat.parse(contactoMap["fechaHora"] as String)?.time ?: 0L,
                                intensidadRssi = when (val rssi = contactoMap["rssi"]) {
                                    is Long -> rssi.toInt()
                                    is Int -> rssi
                                    else -> 0
                                }
                            )
                        } catch (e: Exception) {
                            null
                        }
                    } else null
                }
                Result.success(contactos)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Lógica para marcar como posiblemente infectado
    suspend fun marcarComoPosiblementeInfectado(dniUsuario: String, context: Context): Result<Unit> {
        return try {
            val usuario = obtenerUsuario(dniUsuario, context).getOrNull()
            if (usuario != null && usuario.estadoSalud == EstadoSalud.SANO) {
                actualizarEstadoSalud(dniUsuario, EstadoSalud.POSIBLE_INFECTADO, context)
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Obtener contactos recientes para análisis
    suspend fun obtenerContactosRecientes(dniUsuario: String, context: Context, diasAtras: Int = 14): Result<List<ContactoCercano>> {
        return try {
            val contactos = obtenerContactos(dniUsuario, context).getOrNull() ?: emptyList()
            val tiempoLimite = System.currentTimeMillis() - (diasAtras * 24 * 60 * 60 * 1000L)
            
            val contactosRecientes = contactos.filter { contacto ->
                contacto.fechaHora > tiempoLimite
            }
            
            Result.success(contactosRecientes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 