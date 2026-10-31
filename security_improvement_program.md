# Programa de mejora continua de seguridad --- jaimemaps

## 1. Objetivo

Establecer un proceso de mejora continua para reducir los riesgos de
seguridad de jaimemaps, verificar las medidas ya incorporadas y mantener
evidencia de cada corrección.

El programa se basa en los hallazgos del análisis estático y dinámico
entregados, el informe técnico y la revisión de las medidas descritas en
el proyecto.

## 2. Estado inicial

Los reportes entregados registran un análisis estático MobSF de **45/100
(riesgo medio, grado B)**. Entre los hallazgos se encuentran una clave
de Google Maps expuesta, `android:debuggable=true`, firma con
certificado Android Debug y `android:allowBackup=true`.

El análisis dinámico se realizó en un emulador Pixel 5 con Android API
30. Se observaron llamadas de geolocalización y actividad de componentes
de Google Maps. El reporte atribuye algunas consultas de propiedades del
sistema al SDK de mapas y las describe como comportamiento rutinario.
Estos registros no deben presentarse como vulnerabilidades explotables
por sí solos.

El informe técnico también indica que la base SQLite local no está
cifrada y que la aplicación no dispone de una API propia. Por tanto, la
política de red es preventiva y los reportes disponibles no acreditan
pruebas TLS exhaustivas ni certificate pinning.

## 3. Acciones priorizadas

  -----------------------------------------------------------------------------------------
  Prioridad      Acción               Responsable     Plazo sugerido Criterio de
                                      sugerido                       verificación
  -------------- -------------------- --------------- -------------- ----------------------
  Alta           Revocar o rotar la   Desarrollador   Inmediato /    La clave anterior
                 clave de Google Maps responsable de  antes de       queda revocada o
                 expuesta y           la              publicar       rotada; la nueva tiene
                 restringir la nueva  configuración                  restricciones y no se
                 clave por paquete,   Google Cloud                   filtra en el
                 certificado y API                                   repositorio.
                 necesaria.                                          

  Alta           Generar APK          Desarrollador   Antes de la    Inspección del
                 `release` con        Android         entrega final  manifiesto del APK
                 `debuggable=false` y                 o distribución release y verificación
                 firma de producción.                                del certificado de
                                                                     firma.

  Media          Cifrar la base de    Desarrollador   Próxima        Prueba de
                 datos SQLite que     Android         iteración      lectura/escritura y
                 contiene marcadores                                 verificación de que
                 y ubicaciones.                                      los datos persistidos
                                                                     no quedan en texto
                                                                     legible.

  Media          Desactivar backup o  Desarrollador   Próxima        Revisar manifiesto y
                 definir exclusiones  Android         iteración      reglas de backup en el
                 explícitas para                                     APK final; comprobar
                 datos sensibles.                                    el comportamiento de
                                                                     extracción aplicable a
                                                                     la versión Android
                                                                     utilizada.

  Media          Revisar el flujo de  Desarrollador   Próxima        Pruebas de
                 autenticación ante   Android         iteración      rotación/recreación,
                 recreación de la                                    reanudación y
                 actividad y exigir                                  eliminación de
                 autenticación válida                                marcadores sin
                 para acciones                                       autenticación válida.
                 sensibles.                                          

  Media          Repetir análisis     Responsable de  Después de las Guardar los reportes
                 estático y dinámico  pruebas de      correcciones   MobSF con fecha,
                 sobre el APK         seguridad                      versión del APK y
                 corregido.                                          hashes; comparar
                                                                     hallazgos
                                                                     antes/después.

  Baja           Evaluar reducción y  Desarrollador   Iteración      Compilación release y
                 ofuscación de la     Android         posterior      pruebas funcionales
                 variante release con                                satisfactorias.
                 R8/ProGuard, sin                                    
                 afectar el                                          
                 funcionamiento.                                     
  -----------------------------------------------------------------------------------------

Los plazos son propuestas de planificación y deben ajustarse al
calendario real del equipo.

## 4. Proceso de mejora continua

1.  **Identificar:** revisar reportes MobSF, dependencias, configuración
    Android y comentarios de pruebas.
2.  **Priorizar:** ordenar los hallazgos por severidad, exposición de
    datos e impacto para las personas usuarias.
3.  **Corregir:** implementar cambios pequeños y mantenerlos bajo
    control de versiones.
4.  **Probar:** ejecutar pruebas unitarias, funcionales y de seguridad
    relacionadas con cada cambio.
5.  **Verificar:** volver a analizar el APK y comprobar que el hallazgo
    se resolvió sin introducir regresiones.
6.  **Documentar:** actualizar `vulnerabilities.md`,
    `best_practices.md`, este programa y el reporte PDF.
7.  **Revisar periódicamente:** repetir la revisión ante cambios
    relevantes de código, dependencias, permisos o configuración.

## 5. Métricas de seguimiento

-   Número de hallazgos críticos y altos abiertos.
-   Número de hallazgos corregidos y verificados con un nuevo análisis.
-   Porcentaje de pruebas de seguridad planificadas que cuentan con
    evidencia.
-   Estado de la clave de Google Maps: expuesta, rotada/restringida y
    verificada.
-   Estado de la base de datos: sin cifrar o cifrada y comprobada.
-   Estado del APK de distribución: depurable/no depurable y certificado
    de depuración/producción.
-   Número de dependencias con actualizaciones de seguridad pendientes.

Las métricas deben actualizarse con resultados observados; no asignar
valores de cumplimiento sin ejecutar las verificaciones.

## 6. Evidencias y registro

Para cada acción registrar: - Fecha y responsable. - Versión del código
y del APK. - Descripción del cambio. - Prueba ejecutada y resultado
obtenido. - Captura o reporte que respalde el resultado. - Riesgos que
continúan pendientes.

No incluir claves, contraseñas, tokens, keystores privados ni datos
personales en los reportes o en GitHub.

## 7. Revisión y mantenimiento

El equipo debe revisar el estado del programa al finalizar cada
iteración de desarrollo y antes de generar una versión de distribución.
También debe repetir el análisis cuando se modifiquen componentes de
autenticación, almacenamiento, permisos, red o dependencias de Google
Maps.

## 8. Estado actual y conclusión

El proyecto ya documenta medidas de validación de entradas, consultas
SQL parametrizadas, autenticación mediante credenciales del dispositivo
y configuración de seguridad de red. Sin embargo, la clave de Google
Maps expuesta, el cifrado pendiente de SQLite y los hallazgos de
configuración del APK requieren atención.

Este programa define las acciones necesarias para abordar esos riesgos y
comprobar las correcciones. Una tarea solo debe marcarse como completada
cuando exista evidencia de implementación y verificación.
