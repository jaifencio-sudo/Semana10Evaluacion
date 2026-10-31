@echo off
REM Genera el keystore de PRODUCCION. Requiere keytool (JDK de Android Studio en el PATH).
keytool -genkeypair -v -keystore release-keystore.jks -alias jaimemaps -keyalg RSA -keysize 4096 -validity 10000
echo.
echo Listo. Copia keystore.properties.example a keystore.properties y completa las contrasenas.
echo Respalda release-keystore.jks en un lugar seguro y NO lo subas a Git.
pause
