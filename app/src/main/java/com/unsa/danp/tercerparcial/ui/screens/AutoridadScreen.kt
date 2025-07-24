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
import com.unsa.danp.tercerparcial.model.EstadoSalud
import com.unsa.danp.tercerparcial.model.Usuario
import com.unsa.danp.tercerparcial.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoridadScreen(
    viewModel: MainViewModel,
    currentUser: Usuario,
    onLogout: () -> Unit
) {
    val usuarios by viewModel.usuarios.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        TopAppBar(
            title = { Text("COVID Tracker - Autoridad") },
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
            // Resumen estadístico
            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Resumen Estadístico",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val totalUsuarios = usuarios.size
                        val sanos = usuarios.count { it.estadoSalud == EstadoSalud.SANO }
                        val posiblementeInfectados = usuarios.count { it.estadoSalud == EstadoSalud.POSIBLE_INFECTADO }
                        val infectados = usuarios.count { it.estadoSalud == EstadoSalud.INFECTADO }
                        
                        Text("Total de usuarios: $totalUsuarios")
                        Text("Sanos: $sanos")
                        Text("Posiblemente infectados: $posiblementeInfectados")
                        Text("Infectados: $infectados")
                    }
                }
            }
            
            // Tabla de usuarios
            item {
                Text(
                    text = "Lista de Usuarios",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(usuarios) { usuario ->
                UsuarioCard(
                    usuario = usuario,
                    onEstadoChange = { nuevoEstado ->
                        viewModel.actualizarEstadoSalud(usuario.dni, nuevoEstado)
                    }
                )
            }
        }
    }
}

@Composable
fun UsuarioCard(
    usuario: Usuario,
    onEstadoChange: (EstadoSalud) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DNI: ${usuario.dni}",
                        fontWeight = FontWeight.Bold
                    )
                    Text("Rol: ${usuario.rol.name}")
                    Text(
                        text = "Estado: ${getEstadoSaludText(usuario.estadoSalud)}",
                        color = getEstadoSaludColor(usuario.estadoSalud)
                    )
                }
                
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expandir"
                    )
                }
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Cambiar Estado:",
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onEstadoChange(EstadoSalud.SANO) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (usuario.estadoSalud == EstadoSalud.SANO) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Sano")
                    }
                    
                    Button(
                        onClick = { onEstadoChange(EstadoSalud.POSIBLE_INFECTADO) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (usuario.estadoSalud == EstadoSalud.POSIBLE_INFECTADO) 
                                MaterialTheme.colorScheme.tertiary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Posible")
                    }
                    
                    Button(
                        onClick = { onEstadoChange(EstadoSalud.INFECTADO) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (usuario.estadoSalud == EstadoSalud.INFECTADO) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text("Infectado")
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

private fun getEstadoSaludColor(estado: EstadoSalud): androidx.compose.ui.graphics.Color {
    return when (estado) {
        EstadoSalud.SANO -> androidx.compose.ui.graphics.Color.Green
        EstadoSalud.POSIBLE_INFECTADO -> androidx.compose.ui.graphics.Color.Yellow
        EstadoSalud.INFECTADO -> androidx.compose.ui.graphics.Color.Red
    }
} 