package com.unsa.danp.tercerparcial.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unsa.danp.tercerparcial.model.ContactoCercano
import com.unsa.danp.tercerparcial.model.EstadoSalud
import com.unsa.danp.tercerparcial.model.Usuario
import com.unsa.danp.tercerparcial.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonaScreen(
    viewModel: MainViewModel,
    currentUser: Usuario,
    onLogout: () -> Unit
) {
    val contactos by viewModel.contactos.collectAsStateWithLifecycle()
    val isScanning by viewModel.isBluetoothScanning.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        TopAppBar(
            title = { Text("COVID Tracker - Persona") },
            actions = {
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información del usuario
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Tu información",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("DNI: ${currentUser.dni}")
                        Text("Estado: ${getEstadoSaludText(currentUser.estadoSalud)}")
                    }
                }
            }
            
            // Control de Bluetooth
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Control de Escaneo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Escaneo Bluetooth")
                            Switch(
                                checked = isScanning,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        viewModel.iniciarEscaneoManual()
                                    } else {
                                        viewModel.detenerEscaneoManual()
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // Noticias COVID-19
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Noticias COVID-19",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Mantén una distancia de al menos 2 metros con otras personas\n" +
                                    "• Usa mascarilla en espacios cerrados\n" +
                                    "• Lava tus manos frecuentemente\n" +
                                    "• Si tienes síntomas, aíslate y consulta a un médico",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Información útil
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Información Útil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Síntomas comunes:",
                            fontWeight = FontWeight.Bold
                        )
                        Text("• Fiebre o escalofríos\n• Tos\n• Dificultad para respirar\n• Fatiga\n• Pérdida del gusto u olfato")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Protocolos:",
                            fontWeight = FontWeight.Bold
                        )
                        Text("• Si tienes síntomas, aíslate inmediatamente\n• Contacta a las autoridades sanitarias\n• Sigue las recomendaciones médicas")
                    }
                }
            }
            
            // Contactos recientes (solo cantidad, no detalles)
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Contactos Recientes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Se han detectado ${contactos.size} contactos cercanos en los últimos 14 días")
                        Text(
                            text = "Nota: Por privacidad, no se muestran detalles de los contactos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getEstadoSaludText(estado: EstadoSalud): String {
    return when (estado) {
        EstadoSalud.SANO -> "Sano"
        EstadoSalud.POSIBLE_INFECTADO -> "Posiblemente Infectado"
        EstadoSalud.INFECTADO -> "Infectado"
    }
} 