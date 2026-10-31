import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// ---------------------------------------------------------------------------
// SECRETOS FUERA DEL CÓDIGO FUENTE
//  - MAPS_API_KEY: se lee de local.properties (ignorado por Git) o de la
//    variable de entorno MAPS_API_KEY (útil en CI).
//  - keystore.properties: datos del keystore de producción (ignorado por Git).
// ---------------------------------------------------------------------------
fun loadProps(name: String): Properties = Properties().apply {
    val f = rootProject.file(name)
    if (f.exists()) f.inputStream().use { load(it) }
}

val localProps = loadProps("local.properties")
val keystoreProps = loadProps("keystore.properties")

val mapsApiKey: String = localProps.getProperty("MAPS_API_KEY")
    ?: System.getenv("MAPS_API_KEY")
    ?: ""

if (mapsApiKey.isBlank()) {
    logger.warn(
        "ADVERTENCIA: MAPS_API_KEY no está definida. Agrégala en local.properties " +
            "(ver local.properties.example); el mapa no cargará sin ella."
    )
}

val hasReleaseKeystore = keystoreProps.getProperty("storeFile") != null

android {
    namespace = "com.example.jaimemaps"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.jaimemaps"
        minSdk = 30
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"

        // La clave se inyecta en el manifiesto en tiempo de compilación.
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true // BuildConfig.DEBUG: FLAG_SECURE solo en release
    }

    // Firma de producción: solo si existe keystore.properties (nunca en Git).
    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: reducción + ofuscación del código de la variante release.
            // La variante release NO es depurable (valor por defecto de AGP).
            optimization {
                enable = true
                keepRules {
                    files.add(getDefaultProguardFile("proguard-android-optimize.txt"))
                    files.add(rootProject.file("app/proguard-rules.pro"))
                }
            }
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Google Maps + ubicación
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    // Autenticación biométrica / credencial del dispositivo
    implementation(libs.biometric)

    // Cifrado de la base de datos SQLite (SQLCipher, AES-256)
    implementation("net.zetetic:sqlcipher-android:4.6.1@aar")
    implementation(libs.androidx.sqlite)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
