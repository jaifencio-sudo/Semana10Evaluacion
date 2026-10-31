# Mejoras de seguridad implementadas (versión 1.1)

Cada mejora responde a un hallazgo de los análisis MobSF (estático y dinámico) del APK `app-debug.apk` v1.0
o a una limitación declarada en `security_tips.md` / `best_practices.md`.

| # | Hallazgo / limitación | Mejora aplicada | Dónde |
|---|---|---|---|
| 1 | Clave de Google Maps en texto plano (`google_maps_key`) | La clave sale del código y del repositorio: se lee de `local.properties` (o de la variable de entorno `MAPS_API_KEY`) y se inyecta con `manifestPlaceholders`. Se eliminó `google_maps_api.xml`. | `app/build.gradle.kts`, `AndroidManifest.xml`, `local.properties.example` |
| 2 | `android:debuggable=true` | La variante **release** no es depurable (no se declara el atributo; AGP lo gestiona por tipo de compilación). | `AndroidManifest.xml` |
| 3 | Firma con certificado *Android Debug* | Firma de producción opcional por `keystore.properties` (no versionado) y scripts para crear el keystore. | `app/build.gradle.kts`, `tools/generar_keystore.*`, `keystore.properties.example` |
| 4 | `allowBackup=true` | `allowBackup=false` + `dataExtractionRules` (Android 12+) que excluyen todos los dominios de backup y transferencia. | `AndroidManifest.xml`, `res/xml/data_extraction_rules.xml` |
| 5 | Receptor exportado `ProfileInstallReceiver` (librería AndroidX) | Se elimina del manifiesto final (`tools:node="remove"`). | `AndroidManifest.xml` |
| 6 | Base SQLite sin cifrar | Cifrado AES-256 con **SQLCipher**. La contraseña es aleatoria (256 bits) y se guarda cifrada con una clave del **Android Keystore** (AES-GCM). Migración automática de la base antigua y borrado del archivo sin cifrar. | `MarkerDbHelper.java`, `DbKeyManager.java` |
| 7 | Estado de autenticación reutilizable al recrear la actividad | El estado "desbloqueado" ya no se guarda en el `Bundle`. Se mantiene en memoria (`AuthSession`): se pide autenticación de nuevo si el proceso muere o la app estuvo más de 30 s en segundo plano. El contenido se oculta al salir. | `AuthSession.java`, `MainActivity.java` |
| 8 | Capturas y miniaturas con ubicaciones | `FLAG_SECURE` en la variante release (en debug queda desactivado para poder tomar evidencias). | `MainActivity.java` |
| 9 | Sin ofuscación / reducción | R8 habilitado en release con reglas para SQLCipher. | `app/build.gradle.kts`, `app/proguard-rules.pro` |
| 10 | Entradas y consultas | Se mantienen la lista blanca y las consultas parametrizadas. Se añade validación de rango de coordenadas y escape de comodines en `LIKE`. | `MarkerDbHelper.java` |

## Pruebas unitarias nuevas
`AuthSessionTest` (5 pruebas) verifica el bloqueo por inactividad y la ventana de autenticación reciente.

## Pasos para compilar y analizar de nuevo

1. **Crear una clave nueva** en Google Cloud Console y **revocar la anterior**. Restringir la nueva por paquete
   `com.example.jaimemaps`, huella SHA-1 y *Maps SDK for Android*.
2. Copiar `local.properties.example` a `local.properties` y pegar la clave nueva en `MAPS_API_KEY`.
3. Crear el keystore: `tools/generar_keystore.bat` (Windows) o `tools/generar_keystore.sh`. Copiar
   `keystore.properties.example` a `keystore.properties` y completar las contraseñas.
4. En Android Studio: *Build > Generate Signed App Bundle or APK > APK > release*, o `gradlew assembleRelease`.
   El archivo queda en `app/build/outputs/apk/release/`.
5. Subir ese **APK release** a MobSF (análisis estático y dinámico). No usar `app-debug.apk`.
6. Guardar los nuevos reportes con fecha y comparar con los del 28/10/2026.

## Resultado esperado en el nuevo análisis (hipótesis a verificar, no resultados)

- `debuggable`: no debería aparecer.
- Certificado de depuración: no debería aparecer si se firmó con el keystore de producción.
- `allowBackup`: no debería aparecer. El receptor de AndroidX tampoco.
- Base de datos: el archivo `jaimemaps_enc.db` no debería ser legible como SQLite sin la contraseña.
- **Clave de Google Maps:** seguirá apareciendo en el APK. El SDK de Maps necesita la clave dentro del manifiesto
  y ninguna técnica la oculta del todo. Lo que cambia es que ya no está en el repositorio, y que la protección real es
  la **restricción por paquete y SHA-1** en Google Cloud. Hay que documentarlo así en el informe.
- Pueden aparecer hallazgos nuevos propios de la librería SQLCipher o del `targetSdk`; deben analizarse con el mismo criterio.

## Limitaciones que siguen vigentes
- No hay API propia: la política de red es preventiva y no se hizo *certificate pinning*.
- La autenticación depende del bloqueo de pantalla del dispositivo.
- Si el usuario cambia o elimina el bloqueo de pantalla y el Keystore invalida la clave, la base cifrada se reinicia (se pierden los marcadores).
