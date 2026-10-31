# jaimemaps

Aplicación Android desarrollada en Java que utiliza Google Maps para
mostrar la ubicación del dispositivo y gestionar marcadores. El proyecto
pertenece a la asignatura **Programación Android**.

## Descripción

La aplicación permite visualizar un mapa, consultar la ubicación actual
y guardar marcadores con nombres definidos por el usuario. Para
persistir los marcadores utiliza SQLite. También incorpora autenticación
mediante biometría o la credencial de desbloqueo del dispositivo para
controlar el acceso y proteger acciones sensibles, como eliminar
marcadores.

## Tecnologías y componentes

-   Android Studio y Java.
-   Google Maps SDK for Android.
-   SQLite para el almacenamiento local de marcadores.
-   AndroidX Biometric para autenticación biométrica o credencial del
    dispositivo.
-   Android Network Security Configuration.
-   Pruebas unitarias para la validación de entradas.

> Completar las versiones exactas de Android Gradle Plugin, Gradle, SDK
> y dependencias desde el entorno utilizado antes de publicar la
> entrega.

## Vulnerabilidades y riesgos identificados

Los reportes de análisis estático y dinámico entregados para el proyecto
describen los siguientes hallazgos:

  ----------------------------------------------------------------------------
  Hallazgo                     Severidad reportada /   Observación
                               evaluación              
  ---------------------------- ----------------------- -----------------------
  Clave de Google Maps         Alta                    Debe revocarse o
  expuesta en recursos del APK                         rotarse y restringirse
                                                       en Google Cloud por
                                                       aplicación y huella de
                                                       firma. No publicar la
                                                       clave en el
                                                       repositorio.

  `android:debuggable=true`    Alta                    El APK analizado
                                                       permitía depuración.
                                                       Verificar que la
                                                       variante de
                                                       distribución tenga
                                                       `debuggable=false`.

  Firma con certificado        Alta                    El APK analizado estaba
  Android Debug                                        firmado con el
                                                       certificado de
                                                       depuración. Generar una
                                                       variante release
                                                       firmada con un keystore
                                                       de producción.

  `android:allowBackup=true`   Media / advertencia     El APK analizado
                                                       permitía respaldo.
                                                       Revisar y aplicar una
                                                       política de respaldo
                                                       apropiada para los
                                                       datos de la aplicación.

  Permisos de ubicación        Media / informativa     Son necesarios para la
                                                       función de mapa; deben
                                                       solicitarse en tiempo
                                                       de ejecución y solo
                                                       cuando se requieran.

  Ejecución de `getprop` por   Baja / informativa      El reporte la atribuye
  Google Maps SDK                                      al SDK y la describe
                                                       como consulta rutinaria
                                                       de propiedades del
                                                       sistema.
  ----------------------------------------------------------------------------

Los hallazgos corresponden al APK analizado en los reportes
proporcionados; no deben interpretarse como un nuevo análisis de una
compilación posterior.

## Medidas de seguridad implementadas en el código

De acuerdo con el código y la documentación técnica proporcionada:

-   **Protección frente a inyección SQL:** uso de consultas
    parametrizadas para las operaciones SQLite.
-   **Validación de entradas:** validación del nombre del marcador,
    incluyendo rechazo de entradas vacías, inválidas o que superen el
    límite definido por la aplicación.
-   **Autenticación y autorización:** uso de BiometricPrompt con
    biometría o credencial del dispositivo; se solicita autenticación
    para acceder y para acciones sensibles como eliminar marcadores.
-   **Política de red:** configuración explícita de seguridad de red
    para impedir tráfico en texto claro.

La aplicación no cuenta con una API propia según el informe técnico. Por
ello, la configuración de red es una medida preventiva; los reportes
disponibles no demuestran una prueba completa de validación TLS ni de
certificate pinning.

## Riesgos y tareas pendientes

-   Revocar o rotar la clave de Google Maps que apareció expuesta y
    restringir la nueva clave. La clave anterior no se reproduce en este
    README.
-   Cifrar la base de datos SQLite, que el informe técnico identifica
    como almacenamiento local actualmente no cifrado.
-   Corregir la configuración de compilación para que la variante
    release no sea depurable.
-   Generar y verificar un APK release firmado con un certificado de
    producción.
-   Revisar la configuración de `allowBackup` y las reglas de extracción
    de datos.
-   Repetir el análisis estático y dinámico sobre el APK actualizado y
    conservar los reportes resultantes.

## Documentación de seguridad

Los documentos complementarios de la raíz del repositorio deben incluir:

-   `vulnerabilities.md`: hallazgos, impacto, evidencia y mitigaciones.
-   `best_practices.md`: prácticas de desarrollo seguro aplicadas y sus
    limitaciones.
-   `security_tips.md`: recomendaciones y medidas de seguridad
    implementadas.
-   `security_improvement_program.md`: plan de mejora continua.
-   `vulnerability_report.pdf`: reporte consolidado de análisis estático
    y dinámico.

Los reportes MobSF existentes describen un análisis con puntuación
estática de **45/100 (riesgo medio, grado B)** y un análisis dinámico en
emulador Pixel 5 con Android API 30. Las llamadas observadas a
componentes de Google Maps deben interpretarse según el contexto
descrito en el informe; no constituyen por sí solas una vulnerabilidad
explotable.

## Requisitos y ejecución

1.  Instalar Android Studio y los componentes del Android SDK requeridos
    por el proyecto.
2.  Clonar el repositorio o extraer el proyecto en una carpeta local.
3.  Abrir en Android Studio la carpeta que contiene el proyecto Gradle.
4.  Esperar la sincronización de Gradle y resolver cualquier dependencia
    que el IDE solicite.
5.  Configurar la clave de Google Maps de forma segura para el entorno
    local. No incluir claves ni secretos en Git. Restringir la clave en
    Google Cloud Console.
6.  Seleccionar un emulador compatible o un dispositivo Android de
    prueba.
7.  Ejecutar la configuración `app` desde Android Studio.

Los nombres exactos de las tareas Gradle y las versiones de SDK deben
confirmarse en el proyecto y en el entorno de desarrollo. No se afirma
aquí que la compilación de esta copia haya sido verificada.

## Pruebas

El informe técnico proporcionado registra pruebas de validación de
entradas, ejecución de la aplicación, autenticación y configuración de
red. Antes de la entrega final, conservar las capturas y resultados
originales que respalden cada prueba. No completar resultados faltantes
sin ejecutarlas.

## Publicación en GitHub

Antes de subir el proyecto:

-   Retirar secretos y archivos locales de configuración.
-   Revocar o rotar cualquier clave que haya quedado expuesta.
-   Comprobar que el repositorio no incluya keystores privados,
    contraseñas ni tokens.
-   Incluir los documentos de seguridad y los reportes sanitizados.
-   Verificar que el README y los reportes correspondan a la misma
    versión del código/APK.
