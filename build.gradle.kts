import me.champeau.jmh.*
import org.springframework.boot.gradle.tasks.bundling.*
import org.springframework.boot.gradle.tasks.run.*

plugins {
    id("java")
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("me.champeau.jmh") version "0.7.2"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

repositories {
    mavenCentral()
}

group = "org.example"
version = "0.0.1-SNAPSHOT"

val jacksonVersion = "2.17.1"
configurations {
    all {
        exclude(module = "spring-boot-starter-tomcat")

        resolutionStrategy {
            cacheChangingModulesFor(0, TimeUnit.SECONDS)
            force("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
            force("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
            force("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
            force("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
            force("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
            force("com.fasterxml.jackson.module:jackson-module-parameter-names:$jacksonVersion")
        }
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-undertow")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:$jacksonVersion")

    jmh("org.springframework.boot:spring-boot")
}

tasks.withType<JavaCompile> {
    options.compilerArgs?.add("--enable-preview")
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

tasks.withType<Test> {
    jvmArgs(
        "--enable-preview"

//        "-Djdk.traceVirtualThreadLocals"
    )
//    systemProperty("spring.security.strategy", "MODE_INHERITABLETHREADLOCAL")
    useJUnitPlatform()
}

tasks.withType<JmhBytecodeGeneratorTask> {
    jvmArgs.add("--enable-preview")
}

tasks.withType<BootJar> {
//    loaderImplementation = org.springframework.boot.loader.tools.LoaderImplementation.CLASSIC
}

tasks.withType<BootRun> {
    mainClass.set("org.example.loom.ThreadLocalApplication")
}

jmh {
    jvmArgs.set(
        jvmArgs.get() + setOf(
            "--enable-preview",
            "-XX:+UnlockExperimentalVMOptions",
            "-Xms4G",
            "-Xmx4G",
            "-XX:+AlwaysPreTouch",
//            "-XX:TieredStopAtLevel=1",
//            "-Dspring.output.ansi.enabled=always",
//            "-Dcom.sun.management.jmxremote",
//            "-Dspring.jmx.enabled=true",
//            "-Dspring.liveBeansView.mbeanDomain",
//            "-Dspring.application.admin.enabled=true",
//            "-Dmanagement.endpoints.jmx.exposure.include=*",
        )
    )
    resultFormat.set("CSV")
//    forceGC.set(true)
    profilers.set(
        listOf("gc")
    )
}
