import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    kotlin("jvm") version libs.versions.kotlin
    alias(libs.plugins.pluginyml)
    alias(libs.plugins.shadow)
    alias(libs.plugins.ksp)
}

group = "me.santio.npe"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation(libs.packetevents.spigot)
    implementation(libs.netty)

    implementation(libs.bundles.cloud)
    compileOnly(libs.paper)

    implementation(libs.gson)

    ksp(libs.autoservice.ksp)
    compileOnly(libs.autoservice.google)
}

ksp {
    arg("autoserviceKsp.verify", "true")
}

paper {
    name = "NullPointerException"
    version = "1.0"
    description = "Paper exploit prevention against malformed packets"
    author = "Santio71"
    main = "me.santio.npe.NPE"
    apiVersion = "1.19"

    foliaSupported = true
    generateLibrariesJson = true

    prefix = "NPE"
    provides = listOf("NPE")

    serverDependencies {
        register("ViaVersion") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        javaParameters = true
    }
}

tasks {
    shadowJar {
        mergeServiceFiles()
        minimize()
        archiveFileName.set("NullPointerException.jar")

        exclude("org.intellij.lang.annotations")
        exclude("")
    }

    build {
        dependsOn(shadowJar)
    }

}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}