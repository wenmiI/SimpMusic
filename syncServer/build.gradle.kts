// syncServer — the account sync host as a program of its own, for a machine that does not run the
// app (a home server behind the VPN the devices are on). It owns nothing but `main`: accounts, rounds
// and audio are `AccountSyncHost` in :data, the HTTP side `AccountSyncServer` in :loginSync.
//
//   ./gradlew :syncServer:installDist   →  syncServer/build/install/simpmusic-sync-host/bin/simpmusic-sync-host
plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    applicationName = "simpmusic-sync-host"
    mainClass.set("com.maxrave.simpmusic.syncserver.MainKt")
}

dependencies {
    implementation(projects.data)
    // AccountSyncHost implements this module's server handler, so its types are on :data's surface.
    implementation(projects.loginSync)
}

// The YouTube extractor forks reach :data twice under one file name; either copy will do.
tasks.withType<AbstractCopyTask>().configureEach { duplicatesStrategy = DuplicatesStrategy.EXCLUDE }
