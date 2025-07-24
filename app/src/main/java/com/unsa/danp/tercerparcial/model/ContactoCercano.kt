package com.unsa.danp.tercerparcial.model

data class ContactoCercano(
    val dniUsuario: String,
    val macAddressUsuario: String,
    val macAddressContacto: String,
    val fechaHora: Long,
    val intensidadRssi: Int
)

data class RegistroDeCercania(
    val dniUsuario: String,
    val contactos: List<ContactoFirebase>
)

data class ContactoFirebase(
    val macAddressContacto: String,
    val dniContacto: String,
    val fechaHora: String,
    val rssi: Int
) 