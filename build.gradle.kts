plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "me.soknight.minigram"
version = "0.0.1-SNAPSHOT"
description = "chats-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.bundles.spring.boot.starters)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.springdoc.openapi.webmvc.ui)
    compileOnly(libs.lombok)
    runtimeOnly(libs.h2)
    runtimeOnly(libs.postgresql)
    annotationProcessor(libs.lombok)
    testImplementation(libs.bundles.spring.boot.test.starters)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}