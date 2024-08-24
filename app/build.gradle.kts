import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
}

application {
    mainClass.set("test.engine.logic.AppKt")
}

tasks.getByName<JavaCompile>("compileJava") {
    targetCompatibility = Version.jvmTarget
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = Version.jvmTarget
}

tasks.getByName<JavaExec>("run") {
    doFirst {
        val os = DefaultNativePlatform.getCurrentOperatingSystem()
        when {
            os.isMacOsX -> {
                jvmArgs = listOf("-XstartOnFirstThread")
            }
        }
    }
}

dependencies {
    implementation("com.github.kepocnhh:KotlinExtension.Lwjgl:0.2.0u-SNAPSHOT")
    implementation("com.github.kepocnhh:KotlinExtension.Math:0.7.3-SNAPSHOT")
    implementation(platform("${Lwjgl.group}:lwjgl-bom:${Version.lwjgl}"))
    val classifier = Lwjgl.requireNativesName()
    Lwjgl.modules.forEach { name ->
        implementation(group = Lwjgl.group, name = name)
        runtimeOnly(group = Lwjgl.group, name = name, classifier = classifier)
    }
}
