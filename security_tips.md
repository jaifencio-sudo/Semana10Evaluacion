# Security Tips implementados

Aplicación: **Jaimemaps** (Android, Java) · Actividad 2.5 – Manejo de elementos de seguridad

Este documento detalla los *Security Tips* aplicados a la app y explica **cómo cada uno mejora la seguridad**.
Los tres tips pedidos en la pauta son: protección contra inyección SQL, autenticación/autorización seguras y protección contra ataques de red (MITM).

| # | Security Tip | Dónde está implementado |
|---|---|---|
| 1 | Protección contra inyección SQL | `MarkerDbHelper.java`, `InputValidator.java` |
| 2 | Autenticación y autorización seguras | `AuthHelper.java`, `MainActivity.java` |
| 3 | Protección contra ataques de red (MITM) | `res/xml/network_security_config.xml`, `AndroidManifest.xml` |
| 4 | Medidas complementarias | `AndroidManifest.xml`, `MainActivity.java` |

---

## 1. Protección contra inyección SQL

**Riesgo.** La app permite al usuario escribir el nombre de un marcador, y ese texto se guarda en una base de datos SQLite local. Si el texto se concatenara dentro de una sentencia SQL, un atacante podría escribir algo como `'; DROP TABLE markers;--` y alterar, leer o borrar datos (CWE-89, OWASP Mobile M4 / Top 10 A03:2021).

**Qué se implementó.**

1. **Consultas parametrizadas.** En `MarkerDbHelper.java` ningún dato del usuario se concatena en el SQL:
   - `INSERT` mediante `ContentValues` (`db.insert(TABLE, null, values)`).
   - `SELECT ... WHERE title LIKE ?` con `selectionArgs` (método `searchByTitle`).
   - `DELETE` mediante la API `db.delete(...)`.
   - Las únicas sentencias construidas como texto (`CREATE TABLE`, `DROP TABLE`) son constantes fijas sin datos del usuario.
2. **Validación y sanitización con lista blanca** (`InputValidator.sanitizeTitle`): solo se aceptan letras, números, espacios y los símbolos `. , - _`, con un máximo de 40 caracteres. Se recortan espacios y se normalizan los espacios repetidos. Comillas, `;`, `--`, `<`, `>` y similares provocan el rechazo de la entrada.
3. **Límite de longitud en el campo** (`InputFilter.LengthFilter`) para evitar entradas desproporcionadas.

**Cómo mejora la seguridad.** Se aplica *defensa en profundidad*: aunque una de las capas fallara, la otra sigue protegiendo. La validación rechaza el payload antes de llegar a la base de datos, y la parametrización garantiza que, aun si llegara, SQLite lo trate como **dato** y nunca como **código SQL**.

**Evidencia / cómo probarlo.**
- Prueba unitaria `InputValidatorTest` (`app/src/test/...`): verifica que `'; DROP TABLE markers;--`, `' OR '1'='1`, `<script>…</script>`, cadenas vacías y cadenas demasiado largas son rechazadas.
- Prueba manual: tocar el mapa → escribir `'; DROP TABLE markers;--` → la app muestra *"Nombre inválido"* y no guarda nada. Luego escribir `Casa` → el marcador se guarda y sigue ahí al cerrar y reabrir la app.

---

## 2. Autenticación y autorización seguras

**Riesgo.** Cualquier persona con el teléfono desbloqueado por un instante (o prestado) podría ver la ubicación y los marcadores guardados, o borrarlos (OWASP Mobile M3: autenticación/autorización insegura).

**Qué se implementó.**

1. **Autenticación al abrir la app** (`AuthHelper` + `BiometricPrompt` de AndroidX): el usuario debe verificarse con **huella/rostro (biometría clase fuerte)** o, como alternativa, con el **PIN/patrón/contraseña del dispositivo**.
   - Mientras no se autentique, el contenido de la app permanece **oculto** (`rootLayout` en `INVISIBLE`) y **no se solicita ni se usa la ubicación**.
   - Si la autenticación falla o se cancela, la app se **cierra** (`finish()`).
   - **Fail-closed:** si el dispositivo no tiene ningún bloqueo de pantalla configurado, el acceso se **deniega** con un mensaje explicativo, en lugar de dejar pasar al usuario.
2. **Autorización de acciones sensibles** (`requireRecentAuth` en `MainActivity`): eliminar todos los marcadores es una acción destructiva, por lo que exige una **autenticación reciente (máx. 30 s)**. Si pasó más tiempo, se vuelve a pedir la verificación antes de ejecutar la acción.
3. **Las credenciales nunca pasan por la app:** la verificación la realiza el sistema operativo. La app no almacena contraseñas, PINs ni plantillas biométricas, por lo que no hay nada que filtrar.
4. **Principio de mínimo privilegio en permisos:** solo se piden `ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` (en tiempo de ejecución); la cámara se usa mediante la app de cámara del sistema (`TakePicturePreview`), por lo que la app **no necesita** el permiso `CAMERA`.

**Cómo mejora la seguridad.** Evita el acceso no autorizado a datos de ubicación y marcadores, y protege las operaciones destructivas contra uso accidental o malicioso. Delegar en `BiometricPrompt` es más seguro que construir un login propio (almacenamiento de contraseñas, fuerza bruta, etc.).

**Evidencia / cómo probarlo.**
- Abrir la app → aparece el diálogo del sistema y el mapa no se ve hasta autenticarse.
- Cancelar el diálogo → la app se cierra.
- Esperar más de 30 s y presionar el botón de eliminar → vuelve a pedir autenticación.
- Dispositivo sin bloqueo de pantalla → mensaje *"Configura un bloqueo de pantalla…"* y la app no abre.

> **Nota para el emulador:** configurar primero un PIN en *Ajustes → Seguridad → Bloqueo de pantalla* (y, si se desea probar huella, *Extended controls → Fingerprint*).

---

## 3. Protección contra ataques de red (MITM)

**Riesgo.** En un ataque *Man-in-the-Middle* (p. ej. una red Wi-Fi pública maliciosa) el atacante intercepta o modifica el tráfico. Es especialmente grave si la app envía datos sin cifrar o si acepta certificados falsos (CWE-319 / CWE-295, OWASP Mobile M5).

**Qué se implementó.**

1. **Network Security Config** (`res/xml/network_security_config.xml`, referenciado en el manifiesto):
   - `cleartextTrafficPermitted="false"`: se **prohíbe todo tráfico HTTP** sin cifrar; solo se permite HTTPS/TLS.
   - `trust-anchors` con `src="system"`: solo se confía en los certificados raíz del sistema. **No se aceptan certificados instalados por el usuario**, que es la técnica habitual de proxies interceptores (Burp, ZAP, mitmproxy) y de atacantes.
2. **`android:usesCleartextTraffic="false"`** en `<application>`: refuerzo explícito de la misma política para compatibilidad con librerías.
3. **Sin desactivación de validaciones TLS:** el código no contiene `TrustManager` personalizados, `HostnameVerifier` permisivos ni `setSSLSocketFactory` inseguros.
4. **Tráfico de Google Maps:** el SDK de Maps y los servicios de ubicación se comunican únicamente por HTTPS con los servidores de Google, y quedan cubiertos por esta política.

**Decisión sobre *certificate pinning*.** No se fijaron (*pin*) certificados de los dominios de Google porque estos rotan sus certificados con frecuencia; un pin desactualizado dejaría la app inutilizable. Si en el futuro la app consume una API propia, se recomienda agregar un `<domain-config>` con `<pin-set>` (con fecha de expiración y un pin de respaldo) únicamente para ese dominio.

**Cómo mejora la seguridad.** Garantiza que cualquier comunicación futura de la app viaje cifrada y autenticada, y que un certificado falso o instalado manualmente no sea aceptado, lo que neutraliza la interceptación pasiva y activa del tráfico.

**Evidencia / cómo probarlo.**
- Revisar el reporte estático de MobSF (secciones *Network Security* y *Manifest Analysis*): no debe aparecer la alerta de tráfico *cleartext* permitido.
- Prueba opcional: configurar un proxy (p. ej. OWASP ZAP) con su certificado instalado como certificado de usuario; el tráfico HTTPS de la app **no** debe ser descifrado por el proxy.

---

## 4. Medidas complementarias

| Medida | Dónde | Beneficio |
|---|---|---|
| `android:allowBackup="false"` | `AndroidManifest.xml` | Evita que la base de datos de marcadores se extraiga mediante copias de seguridad (`adb backup` / nube). |
| Única *Activity* exportada = la de lanzamiento | `AndroidManifest.xml` | Reduce la superficie de ataque: no hay componentes expuestos a otras apps. |
| Guard de `mMap == null` antes de usar el mapa | `MainActivity.getCurrentLocation` | Evita un cierre inesperado (error de robustez) si se pulsa el botón antes de que cargue el mapa. |
| Sin registros (`Log`) de ubicación ni nombres de marcadores | Todo el código | Evita filtrar datos sensibles por `logcat`. |

### Recomendación pendiente (API Key de Google Maps)

La API Key de Google Maps está escrita en `res/values/google_maps_api.xml` y por tanto es visible en el repositorio y dentro del APK. Se recomienda:

1. **Restringirla** en Google Cloud Console por *nombre de paquete* + *huella SHA-1* y limitarla a *Maps SDK for Android*.
2. **Revocarla y regenerar** una nueva, ya que la actual quedó publicada.
3. Sacarla del repositorio (por ejemplo `local.properties` + `manifestPlaceholders`) y dejarla fuera de Git.

Este punto se trata también en el documento de *Best Practices* / *Vulnerabilities*.

---

## Limitaciones conocidas

- La base de datos SQLite local **no está cifrada** (el cifrado de datos sensibles corresponde a la sección *Best Practices*; una opción es SQLCipher o `EncryptedSharedPreferences`).
- La app actualmente **no consume APIs propias**, por lo que las protecciones de red son preventivas; se activarán automáticamente ante cualquier conexión futura.
- La autenticación depende del bloqueo de pantalla del dispositivo: la app no gestiona usuarios ni roles propios.


---

## Actualización v1.1: medidas añadidas tras los análisis MobSF

Se resuelven las "limitaciones conocidas" y la "recomendación pendiente" de este documento:

- **API Key fuera del código:** se inyecta desde `local.properties` con `manifestPlaceholders`; `google_maps_api.xml` fue eliminado.
  Sigue visible dentro del APK (requisito del SDK), por lo que debe estar **restringida por paquete + SHA-1** y la anterior **revocada**.
- **Cifrado de SQLite:** SQLCipher (AES-256). Contraseña aleatoria protegida con Android Keystore (`DbKeyManager`). Migra y borra la base antigua.
- **Sesión de autenticación:** `AuthSession` en memoria; re-autenticación tras 30 s en segundo plano o muerte del proceso; contenido oculto al salir.
- **FLAG_SECURE** en release; **R8** habilitado; **firma de producción** por `keystore.properties`.
- **Backup:** `allowBackup=false` + `data_extraction_rules.xml`. Se elimina el receptor exportado de `ProfileInstallReceiver`.

Ver `MEJORAS_SEGURIDAD_V2.md` para el detalle y los pasos de verificación.
