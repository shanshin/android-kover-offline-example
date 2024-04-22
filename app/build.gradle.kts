import com.android.build.gradle.internal.tasks.DeviceProviderInstrumentTestTask
import com.android.build.gradle.internal.tasks.JacocoTask
import kotlinx.kover.features.jvm.ClassFilters
import kotlinx.kover.features.jvm.KoverFeatures
import kotlinx.kover.features.jvm.KoverLegacyFeatures
import java.util.Base64


buildscript {
    dependencies {
        // Kover: adding dependency to perform instrumentation and generate reports
        classpath(libs.kover.feautures)
    }
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "org.jetbrains.example.kover"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.jetbrains.example.kover"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        // Kover: enable coverage measurement
        debug {
            enableAndroidTestCoverage = true
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }


}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// =====
// Kover: Kover-only settings
// =====
dependencies {
    // dependency to Kover runtime (required for applications, instrumented offline)
    implementation(libs.kover.offline)
}


// After JaCoCo instrumentation is performed - replace these classes by classes instrumented with Kover
tasks.withType<JacocoTask>().configureEach {
    doLast {
        val instrumentedClassesDir = this@configureEach.outputForDirs.get().asFile
        val originalClassesDir = this@configureEach.classesDir

        // perform instrumentation
        val instrumenter = KoverFeatures.createOfflineInstrumenter()

        originalClassesDir.forEach { originalRootDir ->
            originalRootDir.walk().forEach { originFile ->
                if (originFile.isFile && originFile.name.endsWith(".class")) {
                    // instrument classfile by Kover and rewrite file content
                    val koverInstrumentedBytes = instrumenter.instrument(
                        originFile.readBytes().inputStream(),
                        originFile.name
                    )

                    val relativePath = originFile.toRelativeString(originalRootDir)
                    val fileToRewrite = instrumentedClassesDir.resolve(relativePath)
                    fileToRewrite.writeBytes(koverInstrumentedBytes)
                }
            }
        }

    }
}

tasks.register("koverHtmlReport") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP

    dependsOn("connectedDebugAndroidTest")

    doLast {
        val koverDir = layout.buildDirectory.dir("kover").get()
        koverDir.asFile.mkdirs()

        // get binary report file
        val testTask =
            tasks.withType<DeviceProviderInstrumentTestTask>().named("connectedDebugAndroidTest")
                .get()
        val testResultDir: File = testTask.resultsDir.get().asFile
        val reportBytes = testResultDir.findLogFile()?.extractKoverBinaryReport()
        assert(reportBytes != null) { "Kover binary report wasn't found in logs" }
        val reportFile = koverDir.file("report.ic").asFile
        reportFile.writeBytes(reportBytes!!)

        // collect sources dirs
        val sourcesDirs =
            android.applicationVariants.first { it.name == "debug" }.sourceSets.flatMap {
                it.javaDirectories + it.kotlinDirectories
            }

        // collect classfiles dirs
        val jacoco = tasks.withType<JacocoTask>().named("jacocoDebug").get()
        val classesDirs = jacoco.classesDir.toList()

        // generate report
        val htmlDir = koverDir.dir("html").asFile
        KoverLegacyFeatures.generateHtmlReport(
            htmlDir,
            null,
            listOf(reportFile),
            classesDirs,
            sourcesDirs,
            "Example report",
            ClassFilters(emptySet(), emptySet(), emptySet())
        )

        logger.quiet("Kover HTML report file://${htmlDir.absolutePath}/index.html")
    }
}


fun File.findLogFile(): File? {
    walk().forEach { file ->
        if (file.isFile && file.name.startsWith("logcat-")) {
            return file
        }
    }
    return null
}

fun File.extractKoverBinaryReport(): ByteArray? {
    val bytesString = readLines().map { line ->
        line.substringAfter("KOVER DUMP=", "")
    }.firstOrNull { it.isNotEmpty() } ?: return null

    return Base64.getDecoder().decode(bytesString)
}
