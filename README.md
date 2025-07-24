# COVID Tracker App

Aplicación Android para rastreo de contactos cercanos mediante Bluetooth para la prevención de COVID-19.

## Características

### ✅ Requerimientos Funcionales Implementados

1. **Inicio de sesión**
   - Login con solo DNI
   - Detección automática de rol (PERSONA/AUTORIDAD)
   - Redirección a interfaz correspondiente

2. **Detección de cercanía mediante Bluetooth**
   - Escaneo Bluetooth automático al iniciar
   - Escaneos intermitentes cada 30-60 minutos
   - Detección de dispositivos en radio de 30 metros (RSSI > -70)
   - Almacenamiento de registros de cercanía

3. **Almacenamiento en Firebase**
   - Datos de cercanía subidos a Firestore
   - Estados de salud actualizados en tiempo real
   - Acceso restringido solo para autoridades

4. **Gestión de estados de salud**
   - Cambio automático a "posiblemente infectado" por contacto
   - Actualización manual por autoridades sanitarias
   - Lógica automatizada basada en contactos recientes

5. **Interfaz de usuario**
   - **Persona común**: Noticias, información útil, control manual de Bluetooth
   - **Autoridad de salud**: Tabla resumen con todos los usuarios y control de estados

6. **Privacidad**
   - No se registra ubicación geográfica exacta
   - Solo autoridades acceden a datos detallados
   - Personas comunes solo ven cantidad de contactos

## Configuración

### 1. Firebase Setup

1. Crear proyecto en [Firebase Console](https://console.firebase.google.com/)
2. Habilitar Firestore Database
3. Descargar `google-services.json` y reemplazar el archivo en `app/google-services.json`
4. Configurar reglas de seguridad en Firestore:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Usuarios pueden leer solo su propio documento
    match /usuarios/{dni} {
      allow read: if request.auth != null && resource.data.dni == request.auth.uid;
      allow write: if request.auth != null;
    }
    
    // Contactos solo para autoridades
    match /contactos/{dni} {
      allow read, write: if request.auth != null && 
        get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.rol == 'AUTORIDAD';
    }
  }
}
```

### 2. Permisos Requeridos

La aplicación solicita automáticamente:
- Permisos Bluetooth (SCAN, ADVERTISE, CONNECT)
- Permisos de ubicación (para estimar cercanía)
- Permisos de internet y red

### 3. Configuración de Bluetooth

- Bluetooth debe estar habilitado
- Ubicación debe estar habilitada
- La app usa el nombre "COVID_TRACKER" como identificador único

## Estructura del Proyecto

```
app/src/main/java/com/unsa/danp/tercerparcial/
├── model/                    # Modelos de datos
│   ├── Usuario.kt
│   └── ContactoCercano.kt
├── repository/               # Repositorio Firebase
│   └── FirebaseRepository.kt
├── service/                  # Servicio Bluetooth
│   └── BluetoothScanService.kt
├── receiver/                 # Receptor de alarmas
│   └── BluetoothScanReceiver.kt
├── viewmodel/                # ViewModel principal
│   └── MainViewModel.kt
├── ui/screens/               # Pantallas de la UI
│   ├── LoginScreen.kt
│   ├── PersonaScreen.kt
│   └── AutoridadScreen.kt
└── MainActivity.kt
```

## Uso

### Para Personas Comunes:
1. Ingresar DNI en la pantalla de login
2. La app inicia automáticamente el escaneo Bluetooth
3. Ver noticias e información útil sobre COVID-19
4. Control manual del escaneo Bluetooth
5. Ver cantidad de contactos recientes (sin detalles)

### Para Autoridades de Salud:
1. Ingresar DNI de autoridad
2. Ver tabla completa de todos los usuarios
3. Cambiar estados de salud de los usuarios
4. Ver estadísticas de contagios
5. Acceso completo a datos de contactos

## Estrategia de Escaneo Bluetooth

- **Escaneo inicial**: 10 segundos al abrir la app
- **Escaneos intermitentes**: Cada 30-60 minutos (aleatorio)
- **Ahorro de batería**: Bluetooth se activa solo durante escaneos
- **Filtrado**: Solo dispositivos con nombre "COVID_TRACKER"
- **Cercanía**: RSSI > -70 (aproximadamente 30 metros)

## Base de Datos Firebase

### Colección: usuarios
```json
{
  "dni": "12345678",
  "rol": "PERSONA",
  "estadoSalud": "SANO"
}
```

### Colección: contactos
```json
{
  "contactos": [
    {
      "dni": "87654321",
      "fechaHora": "2025-07-23T14:00:00Z",
      "rssi": -65
    }
  ]
}
```

## Notas Importantes

1. **Reemplazar google-services.json**: Usar el archivo real de tu proyecto Firebase
2. **Configurar reglas de seguridad**: Implementar las reglas de Firestore mostradas arriba
3. **Pruebas**: Usar múltiples dispositivos para probar la detección Bluetooth
4. **Producción**: Considerar optimizaciones adicionales para batería y rendimiento

## Tecnologías Utilizadas

- **Kotlin** + **Jetpack Compose**
- **Firebase Firestore**
- **Bluetooth Low Energy (BLE)**
- **WorkManager** para tareas en segundo plano
- **DataStore** para almacenamiento local
- **Coroutines** para operaciones asíncronas 