package com.unsa.danp.tercerparcial.model

data class Usuario(
    val dni: String,
    val rol: Rol,
    val estadoSalud: EstadoSalud,
    val macAddress: String
)

enum class Rol {
    PERSONA,
    AUTORIDAD
}

enum class EstadoSalud {
    SANO,
    POSIBLE_INFECTADO,
    INFECTADO
} 