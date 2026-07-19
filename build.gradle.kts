import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("io.quarkus")
    id("com.diffplug.spotless") version "8.8.0"
    id("net.ltgt.errorprone") version "5.1.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-rest-client-jackson")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-hibernate-reactive-panache")
    implementation("io.quarkus:quarkus-reactive-pg-client")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer")
    implementation("io.quarkus:quarkus-opentelemetry")

    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.osgi:org.osgi.annotation.versioning:1.1.2")
    errorprone("com.google.errorprone:error_prone_core:2.50.0")
    errorprone("com.uber.nullaway:nullaway:0.13.7")

    testImplementation("org.jspecify:jspecify:1.0.0")
    testCompileOnly("org.osgi:org.osgi.annotation.versioning:1.1.2")
    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.quarkus:quarkus-test-vertx")
    testImplementation("io.quarkus:quarkus-test-hibernate-reactive-panache")
    testImplementation("io.rest-assured:rest-assured")
}

group = "com.alexastudillo"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat("1.35.0")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Werror"))
    options.errorprone {
        error("NullAway")
        option("NullAway:OnlyNullMarked", "true")
        option("NullAway:JSpecifyMode", "true")
    }
}
