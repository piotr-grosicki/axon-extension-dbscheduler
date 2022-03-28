import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.spring") version "1.6.10"
}

group = "pl.curiosoft"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.springframework:spring-context:5.3.17")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:2.6.5")
    compileOnly("com.github.kagkarlsson:db-scheduler-spring-boot-starter:11.0")
    compileOnly("io.github.microutils:kotlin-logging-jvm:2.1.21")
    compileOnly("org.axonframework:axon-spring-boot-starter:4.5.8") {
        exclude(group = "org.axonframework", module = "axon-server-connector")
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}