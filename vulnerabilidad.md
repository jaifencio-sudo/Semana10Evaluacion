# Vulnerabilities Report

## Summary
- Total vulnerabilities found: 6
- Critical: 0
- High: 3
- Medium: 2
- Low: 1

## Detailed Findings

### 1. Exposición de Clave de API en el Código (Hardcoded Secret)
- **Description:** Se detectó una clave de la API de Google Maps expuesta directamente en texto plano dentro de los recursos de configuración y metadatos del paquete APK (`google_maps_key: AlzaSyBQRYrRcNx5TiufBI-CAIUJZOS2-wxopeU`).
- **Severity:** High
- **Impact:** Cualquier atacante puede extraer la credencial mediante ingeniería inversa (descompilando el APK) y utilizarla en servicios externos de Google Cloud, generando consumos no autorizados, agotamiento de cuotas o cargos económicos al desarrollador.
- **Steps to Reproduce:** 
  1. Extraer el APK utilizando MobSF o una herramienta de descompilación como JADX.
  2. Inspeccionar los archivos de recursos (`AndroidManifest.xml` o archivos de configuración XML).
  3. Localizar la cadena de texto plano correspondiente a la llave de Google Maps.
- **Remediation:** Extraer la credencial del código fuente, almacenarla en propiedades de entorno locales (`local.properties`) y aplicar restricciones estrictas de uso en la consola de Google Cloud (restringiendo por huella SHA-1 y nombre de paquete).

### 2. Habilitación de Depuración en el Manifiesto (android:debuggable=true)
- **Description:** El archivo de manifiesto (`AndroidManifest.xml`) contiene el atributo explícito `android:debuggable="true"`.
- **Severity:** High
- **Impact:** Permite que cualquier herramienta de depuración o instrumentación externa (JDB, Frida, Android Studio) se conecte en caliente al proceso de la aplicación en ejecución, facilitando la extracción de datos en memoria y la alteración de la lógica de negocio.
- **Steps to Reproduce:**
  1. Analizar el archivo `AndroidManifest.xml` mediante análisis estático en MobSF.
  2. Verificar la presencia de la bandera `android:debuggable="true"` en la etiqueta `<application>`.
  3. Intentar conectar un depurador o inyectar Frida Gadget al proceso de la app.
- **Remediation:** Eliminar el atributo del manifiesto o configurar el sistema de compilación de Gradle para que gestione esta bandera automáticamente según el tipo de entorno (*debug* o *release*).

### 3. Firma con Certificado de Depuración (Debug Keystore)
- **Description:** El paquete APK se encuentra firmado utilizando el almacén de claves de depuración estándar provisto por el SDK de Android (`CN=Android Debug`).
- **Severity:** High
- **Impact:** Compromete la autenticidad e integridad del software en entornos de producción, permitiendo que terceros firmen actualizaciones maliciosas aceptadas por el sistema operativo.
- **Steps to Reproduce:**
  1. Revisar los metadatos de firma del APK en MobSF (bloque `META-INF/CERT.RSA`).
  2. Identificar el emisor del certificado (`CN=Android Debug, O=Android, C=US`).
- **Remediation:** Generar un almacén de claves de producción (*Release Keystore*) seguro, protegido con contraseñas robustas, y aplicarlo en la configuración de compilación firmada.

### 4. Respaldo de Datos Activo (allowBackup=true)
- **Description:** El archivo de manifiesto configura la directiva `android:allowBackup="true"`, permitiendo respaldar los datos de la aplicación mediante la interfaz ADB.
- **Severity:** Medium
- **Impact:** Un atacante con acceso físico o conexión USB habilitada puede extraer bases de datos SQLite, archivos de preferencias compartidas y cachés locales mediante comandos de respaldo.
- **Steps to Reproduce:**
  1. Revisar la configuración del manifiesto en el análisis estático.
  2. Ejecutar el comando de respaldo ADB (`adb backup com.example.jaimemaps`) para comprobar la extracción de datos.
- **Remediation:** Establecer explícitamente `android:allowBackup="false"` en el archivo `AndroidManifest.xml` si la app maneja información sensible.

### 5. Solicitud de Permisos de Ubicación Sensibles (Fine & Coarse Location)
- **Description:** La aplicación declara el uso de los permisos `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` sin restricciones explícitas de control temporal.
- **Severity:** Medium
- **Impact:** Exposición de coordenadas geográficas de precisión si no se gestionan adecuadamente mediante solicitudes en tiempo real adaptadas a la privacidad del usuario.
- **Steps to Reproduce:**
  1. Revisar el listado de permisos declarados en MobSF.
  2. Ejecutar la app y verificar en tiempo de ejecución (análisis dinámico con Frida) las llamadas a `Location.getLatitude()` y `getLongitude()`.
- **Remediation:** Validar la solicitud de estos permisos estrictamente en tiempo de ejecución (*runtime permissions*) y solo cuando la funcionalidad de mapas lo requiera de forma activa.

### 6. Ejecución de Comandos del Sistema por Componentes Externos
- **Description:** Durante el análisis dinámico, se detectó la ejecución nativa de comandos del sistema operativo (`['/system/bin/getprop', 'debug.mapview.renderer.label']`) por parte de las librerías del SDK de Google Maps.
- **Severity:** Low
- **Impact:** Consulta rutinaria de propiedades del sistema para renderizado gráfico. Aunque es un comportamiento interno esperado del SDK, expone trazas de ejecución en entornos virtualizados.
- **Steps to Reproduce:**
  1. Iniciar la aplicación en el emulador bajo el *Dynamic Analyzer* de MobSF.
  2. Interactuar con la interfaz del mapa.
  3. Revisar los logs del monitor de procesos y comandos (`ProcessBuilder` / `Runtime.exec`).
- **Remediation:** Mantener actualizadas las dependencias de Google Play Services para asegurar que utilicen canales de consulta optimizados y seguros.