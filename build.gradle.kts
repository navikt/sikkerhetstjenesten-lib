import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.1.21"
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
            url = uri("https://maven.pkg.github.com/navikt")
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
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.4.1"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("org.zalando:logbook-spring-boot-starter:3.7.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.micrometer:micrometer-core")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.springframework:spring-test")
}

tasks.test {
    useJUnitPlatform()
}
