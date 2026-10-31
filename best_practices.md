# Buenas prácticas de seguridad --- jaimemaps

## Objetivo

Este documento describe las buenas prácticas de desarrollo seguro
relacionadas con la aplicación jaimemaps, utilizando como base el
código, el informe técnico y los reportes de análisis estático y
dinámico entregados.

## 1. Validación de entradas

**Riesgo:** entradas inesperadas o manipuladas pueden causar errores de
aplicación o utilizarse para alterar operaciones que guardan marcadores.

**Medida documentada:** el proyecto incorpora `InputValidator.java` y
valida el nombre del marcador antes de guardarlo. El informe técnico
indica que se rechazan entradas vacías, entradas con contenido no
permitido y textos que superan el límite establecido (40 caracteres).

**Evidencia indicada en el informe:** pruebas unitarias de
`InputValidatorTest` y pruebas manuales de entradas inválidas. El
informe registra el rechazo de una cadena de prueba con sintaxis de
inyección SQL.

**Impacto:** reduce la posibilidad de que datos no válidos lleguen a la
capa de persistencia y mejora la consistencia de los marcadores.

## 2. Consultas SQL parametrizadas

**Riesgo:** concatenar directamente texto ingresado por el usuario en
una consulta SQL puede permitir inyección SQL.

**Medida documentada:** las operaciones SQLite utilizan mecanismos
parametrizados, como `ContentValues` y `selectionArgs`, en lugar de
construir consultas incorporando directamente el texto del usuario.

**Componente:** `MarkerDbHelper.java`.

**Evidencia indicada:** el informe técnico describe pruebas con entradas
de inyección y pruebas unitarias de validación.

**Impacto:** los valores ingresados se tratan como datos y no como parte
ejecutable de la consulta SQL.

## 3. Autenticación y autorización de acciones sensibles

**Riesgo:** una persona con acceso al dispositivo podría abrir la
aplicación o eliminar marcadores sin autorización suficiente.

**Medida documentada:** el proyecto utiliza AndroidX Biometric /
`BiometricPrompt` para solicitar autenticación mediante biometría o
credencial del dispositivo. El informe señala que se solicita
autenticación al abrir la aplicación y nuevamente para eliminar
marcadores.

**Evidencia indicada:** las pruebas P-05 y P-06 del informe técnico
describen el inicio de la aplicación, la solicitud de autenticación y la
reautenticación al ejecutar la acción de eliminación.

**Impacto:** añade una barrera de acceso a la información y a las
operaciones sensibles.

**Limitación:** la autenticación depende de los mecanismos de bloqueo y
credenciales configurados en el dispositivo. Debe verificarse que el
estado de autenticación no pueda reutilizarse indebidamente al recrear
la actividad.

## 4. Comunicaciones de red

**Riesgo:** el tráfico HTTP sin cifrar puede quedar expuesto a
observación o modificación durante el tránsito.

**Medida documentada:** el proyecto incluye
`network_security_config.xml` y lo referencia desde el manifiesto para
declarar una política que impide el tráfico en texto claro y confía en
certificados del sistema.

**Evidencia indicada:** el informe técnico registra que la configuración
está referenciada desde el manifiesto y que la aplicación carga el mapa
con esa política activa.

**Impacto:** evita que la aplicación permita accidentalmente conexiones
HTTP en texto claro mediante la configuración de seguridad de red.

**Limitación:** el informe indica que la aplicación no dispone de una
API propia. Esta medida es preventiva y no equivale a demostrar que
todas las conexiones TLS hayan sido sometidas a pruebas exhaustivas. No
afirmar que certificate pinning fue implementado o validado.

## 5. Protección de credenciales y claves de API

**Riesgo detectado:** el análisis estático encontró una clave de Google
Maps en texto plano dentro de los recursos del APK.

**Estado:** pendiente de corrección según los reportes entregados.

**Acciones recomendadas:** - Revocar o rotar la clave expuesta. - Crear
o configurar una clave restringida para Android. - Restringirla por
nombre de paquete y huella SHA-1 del certificado correspondiente. -
Limitar la clave a las API necesarias y configurar cuotas. - Mantener
archivos locales con secretos fuera del repositorio. - Revisar el APK
final para comprobar que no se publique una clave antigua o sin
restricciones.

**Impacto esperado:** reduce el riesgo de uso no autorizado y consumo de
cuotas o cargos en Google Cloud.

## 6. Almacenamiento local y copias de seguridad

**Riesgo detectado:** el informe técnico indica que los marcadores se
almacenan en SQLite y que la base de datos local no está cifrada. El APK
analizado también tenía `android:allowBackup=true`.

**Estado:** pendiente de mejora.

**Acciones recomendadas:** - Evaluar e implementar cifrado de la base de
datos local. - Proteger las claves criptográficas mediante Android
Keystore o una solución compatible. - Desactivar el respaldo si no es
necesario o definir reglas explícitas que excluyan datos sensibles. -
Verificar la configuración efectiva en el APK de distribución.

**Impacto esperado:** reduce la exposición de ubicaciones y marcadores
si se obtiene acceso a los datos locales o a una copia de respaldo.

## 7. Permisos de ubicación

**Riesgo:** la ubicación precisa es información sensible y puede revelar
movimientos o lugares frecuentados.

**Medida y criterio:** los permisos `ACCESS_FINE_LOCATION` y
`ACCESS_COARSE_LOCATION` son coherentes con la función de mapas, pero
deben solicitarse en tiempo de ejecución y solo cuando la función lo
requiera. La persona usuaria debe poder denegar el permiso sin que la
aplicación falle.

**Evidencia disponible:** los reportes confirman el uso de APIs de
geolocalización durante el recorrido dinámico. La existencia del permiso
en el manifiesto, por sí sola, no demuestra una gestión temporal
incorrecta.

## 8. Configuración de compilación y firma

**Riesgos detectados en el APK analizado:** -
`android:debuggable=true`. - Firma con certificado Android Debug.

**Acciones recomendadas:** generar una variante `release`, comprobar que
no sea depurable y firmarla con un keystore de producción protegido. No
guardar el keystore ni sus contraseñas en GitHub.

**Impacto esperado:** evita distribuir una compilación de desarrollo y
mejora la autenticidad del paquete publicado.

## Resumen de estado

  -----------------------------------------------------------------------
  Práctica                            Estado según la documentación
                                      disponible
  ----------------------------------- -----------------------------------
  Validación de entradas              Implementada y con pruebas
                                      descritas

  Consultas SQL parametrizadas        Implementadas

  Autenticación para acceso y         Implementada; revisar ciclo de vida
  eliminación                         de la actividad

  Política de red sin tráfico en      Configurada; pruebas TLS
  texto claro                         exhaustivas no acreditadas

  Protección de clave Google Maps     Pendiente: clave expuesta en APK
                                      analizado

  Cifrado de SQLite                   Pendiente

  Política de backup                  Pendiente de corregir/verificar en
                                      APK

  Firma release y depuración          Pendiente de verificar en APK
  deshabilitada                       release
  -----------------------------------------------------------------------

## Conclusión

El proyecto incorpora medidas concretas para validar entradas, proteger
consultas SQLite, autenticar acciones sensibles y declarar una política
de red. Sin embargo, los reportes también identifican riesgos pendientes
en la clave de Google Maps, el cifrado de la base de datos, las copias
de seguridad y la configuración de distribución. Estas medidas deben
corregirse y verificarse antes de considerar la aplicación lista para
publicación.
