#!/usr/bin/env bash
# Genera el keystore de PRODUCCIÓN (RSA 4096, validez ~27 años). Guárdalo fuera de Git.
# Requiere keytool (viene con el JDK de Android Studio).
set -e
keytool -genkeypair -v \
  -keystore release-keystore.jks \
  -alias jaimemaps \
  -keyalg RSA -keysize 4096 -validity 10000
echo
echo "Listo. Ahora copia keystore.properties.example a keystore.properties y completa las contraseñas."
echo "Respalda release-keystore.jks en un lugar seguro: si lo pierdes no podrás actualizar la app."
