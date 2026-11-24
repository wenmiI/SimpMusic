plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    applicationName = "simpmusic-stream-host"
    mainClass.set("com.maxrave.simpmusic.streamserver.MainKt")
}

dependencies {
    implementation(projects.kotlinYtmusicScraper)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
}

tasks.withType<AbstractCopyTask>().configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
