import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.hendraanggrian.buildconfig.BuildConfigTask
import com.hendraanggrian.packr.PackTask
import com.hendraanggrian.packr.PackrExtension
import com.hendraanggrian.r.RTask

group = RELEASE_GROUP
version = RELEASE_VERSION

plugins {
    kotlin("jvm")
    kotlin("kapt")
    idea
    hendraanggrian("r")
    hendraanggrian("buildconfig")
    hendraanggrian("packr")
    shadow
    application
}

application.mainClassName = "$RELEASE_GROUP.PlanoApp"

sourceSets {
    getByName("main") {
        java.srcDir("src")
        resources.srcDir("res")
    }
}

val configuration = configurations.register("ktlint")

dependencies {
    api(project(":$RELEASE_ARTIFACT"))
    api(kotlin("reflect", VERSION_KOTLIN))
    api(kotlinx("coroutines-javafx", VERSION_COROUTINES))

    api(exposed("core"))
    api(exposed("dao"))
    api(exposed("jdbc"))
    api(sqliteJDBC())

    implementation(hendraanggrian("ktfx", "ktfx", VERSION_KTFX))
    implementation(hendraanggrian("ktfx", "ktfx-jfoenix", VERSION_KTFX))

    implementation(hendraanggrian("prefy", "prefy-jvm", VERSION_PREFY))
    kapt(hendraanggrian("prefy", "prefy-compiler", VERSION_PREFY))

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

    withType<RTask> {
        resourcesDirectory = "res"
        configureCss()
        properties { isWriteResourceBundle = true }
    }

    withType<BuildConfigTask> {
        appName = RELEASE_NAME
        artifactId = RELEASE_ARTIFACT
        debug = RELEASE_DEBUG
        addField("USER", RELEASE_USER)
        addField("WEB", RELEASE_WEB)
    }

    named<Jar>("jar") { manifest { attributes(mapOf("Main-Class" to application.mainClassName)) } }

    named<ShadowJar>("shadowJar") {
        destinationDirectory.set(buildDir.resolve("releases"))
        archiveBaseName.set(RELEASE_ARTIFACT)
        archiveVersion.set(RELEASE_VERSION)
        archiveClassifier.set(null as String?)
    }

    withType<PackTask> {
        dependsOn("installDist")
    }
}

packr {
    executable = RELEASE_NAME
    mainClass = application.mainClassName
    classpath = files("build/install/$RELEASE_ARTIFACT-javafx/lib")
    resources = files("res")
    minimizeJre = PackrExtension.MINIMIZATION_HARD
    macOS {
        name = "$RELEASE_ARTIFACT-$RELEASE_VERSION/$RELEASE_NAME.app"
        icon = rootProject.projectDir.resolve("art/$RELEASE_ARTIFACT.icns")
        bundleId = RELEASE_GROUP
    }
    windows32 {
        name = "$RELEASE_ARTIFACT-$RELEASE_VERSION-x86/$RELEASE_NAME"
        jdk = "/Volumes/Media/Windows JDK/jdk1.8.0_271-x86"
    }
    windows64 {
        name = "$RELEASE_ARTIFACT-$RELEASE_VERSION-x64/$RELEASE_NAME"
        jdk = "/Volumes/Media/Windows JDK/jdk1.8.0_271-x64"
    }
    isVerbose = true
    isAutoOpen = true
}