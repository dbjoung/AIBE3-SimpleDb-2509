plugins {
    java
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "SimpleDb-2509"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
    // ✅ Spring Boot 기본 의존성
    implementation("org.springframework.boot:spring-boot-starter")

    // ✅ Lombok
    compileOnly("org.projectlombok:lombok:1.18.38") // 버전 명시
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    // ✅ DB (MySQL)
    runtimeOnly("com.mysql:mysql-connector-j:9.3.0")

    // ✅ JSON 처리
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")

    // ✅ 테스트 관련
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}