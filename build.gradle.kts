import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

group = "no.nav.felles"

fun normalizeVersion(raw: String): String = raw.trim().removePrefix("v").removePrefix("V")

version = providers
    .gradleProperty("releaseVersion")
    .orElse(providers.environmentVariable("RELEASE_VERSION"))
    .orElse("0.1.0-SNAPSHOT")
    .map(::normalizeVersion)
    .get()

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

publishing {
    publications {
        create<MavenPublication>("github") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = rootProject.name
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/sikkerhetstjenesten-lib")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "x-access-token"
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Spring Boot BOM to align versions of version-less starter artifacts
    api(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    // OpenTelemetry instrumentation BOM to align versions of version-less instrumentation artifacts
    api(platform(libs.opentelemetry.instrumentation.bom))

    // Exposed in public API (constructors/interfaces implemented by classes in this library)
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.webclient)
    api(libs.logbook.spring.boot.starter)
    api(libs.jackson.module.kotlin)
    api(libs.spring.boot.starter.data.redis)
    api(libs.bundles.observability)
    api(libs.spring.boot.starter.actuator)
    api(libs.spring.boot.starter.aspectj)
    api(libs.httpclient5)

    // Used internally only
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.slack)
    implementation(libs.boot.conditionals)
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.springframework:spring-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
}

tasks.test {
    useJUnitPlatform()
}
