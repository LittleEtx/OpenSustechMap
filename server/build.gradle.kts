plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "com.littleetx.sustechnav"
version = "1.0.0"
application {
    mainClass.set("com.littleetx.sustechnav.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.crypt)
    implementation(libs.exposed.datetime)
    implementation(libs.exposed.money)
    implementation(libs.hikari.cp)
    implementation(libs.database.driver.sqlite)
    implementation(libs.reflections)
    implementation(libs.spatialk.geojson)

    testImplementation(libs.kotlin.test.junit)
}