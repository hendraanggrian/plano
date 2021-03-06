plugins {
    android("application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}

android {
    compileSdkVersion(SDK_TARGET)
    defaultConfig {
        minSdkVersion(SDK_MIN)
        targetSdkVersion(SDK_TARGET)
        multiDexEnabled = true
        applicationId = RELEASE_GROUP
        versionName = RELEASE_VERSION
        buildConfigField("String", "VERSION_NAME", "\"$RELEASE_VERSION\"") // probably Android gradle plugin 4.1.0 bug
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
            res.srcDir("res")
            resources.srcDir("src")
        }
    }
    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            buildConfigField("String", "NAME", "\"$RELEASE_NAME\"")
            buildConfigField("String", "WEB", "\"$RELEASE_WEB\"")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            buildConfigField("String", "NAME", "\"$RELEASE_NAME\"")
            buildConfigField("String", "WEB", "\"$RELEASE_WEB\"")
        }
    }
    lintOptions {
        isAbortOnError = false
    }
    dexOptions {
        javaMaxHeapSize = "2g"
    }
    packagingOptions {
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/kotlinx-io.kotlin_module")
        exclude("META-INF/kotlinx-coroutines-core.kotlin_module")
        exclude("META-INF/kotlinx-coroutines-io.kotlin_module")
        exclude("META-INF/atomicfu.kotlin_module")
    }
}

val configuration = configurations.register("ktlint")

dependencies {
    api(project(":$RELEASE_ARTIFACT"))
    api(kotlinx("coroutines-android", VERSION_COROUTINES))

    implementation(hendraanggrian("prefy", "prefy-android", VERSION_PREFY))
    kapt(hendraanggrian("prefy", "prefy-compiler", VERSION_PREFY))
    implementation(hendraanggrian("bundler", "bundler", VERSION_BUNDLER))
    kapt(hendraanggrian("bundler", "bundler-compiler", VERSION_BUNDLER))

    // implementation(androidx("multidex", version = VERSION_MULTIDEX))
    implementation(androidx("lifecycle", "lifecycle-extensions", VERSION_LIFECYCLE))
    implementation(androidx("lifecycle", "lifecycle-viewmodel-ktx", VERSION_LIFECYCLE))
    implementation(androidx("lifecycle", "lifecycle-livedata-ktx", VERSION_LIFECYCLE))
    implementation(androidx("core", "core-ktx"))
    implementation(androidx("appcompat"))
    implementation(androidx("preference"))
    implementation(androidx("coordinatorlayout"))
    implementation(androidx("recyclerview"))
    implementation(androidx("room", "room-ktx", VERSION_ROOM))
    kapt(androidx("room", "room-compiler", VERSION_ROOM))
    implementation(material())

    implementation(processPhoenix())

    debugImplementation(leakCanary())

    configuration {
        invoke(ktlint())
    }
}

tasks {
    val ktlint by registering(JavaExec::class) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        inputs.dir("src")
        outputs.dir("src")
        description = "Check Kotlin code style."
        classpath(configuration.get())
        main = "com.pinterest.ktlint.Main"
        args("src/**/*.kt")
    }
    "check" {
        dependsOn(ktlint.get())
    }
    register<JavaExec>("ktlintFormat") {
        group = "formatting"
        inputs.dir("src")
        outputs.dir("src")
        description = "Fix Kotlin code style deviations."
        classpath(configuration.get())
        main = "com.pinterest.ktlint.Main"
        args("-F", "src/**/*.kt")
    }
}