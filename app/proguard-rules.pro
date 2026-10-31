# Reglas R8/ProGuard de la variante release de Jaimemaps.
# Las reglas de Google Maps, Play Services y AndroidX las aportan sus propias
# librerías (consumer rules). Solo se agregan las de SQLCipher.

# SQLCipher usa JNI: no se debe renombrar ni eliminar su API.
-keep class net.zetetic.database.** { *; }
-keep interface net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# Conservar números de línea para poder leer reportes de fallos,
# ocultando el nombre real del archivo fuente.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
