import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

//val gradle_version = "6.8-milestone-1"
val gradle_version = "6.7"

buildscript {
    extra.apply {
        set("kotlinVersion", "1.4.20-M1")
    }

    repositories {
        mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
	maven ("https://dl.bintray.com/kotlin/kotlin-eap")
	maven ("https://kotlin.bintray.com/kotlinx")
    }
}

plugins {
	id("org.springframework.boot") version "2.4.0-SNAPSHOT"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	kotlin("jvm") version "${property("kotlinVersion")}"
	kotlin("plugin.spring") version "${property("kotlinVersion")}"
}

group = "com.y"
version = "0.2.7-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_15

//configurations {
//	implementation {
//		exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
//	}
//}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
	maven ("https://dl.bintray.com/kotlin/kotlin-eap")
	maven ("https://kotlin.bintray.com/kotlinx")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.web3j:core:+")
	implementation("org.web3j:crypto:+")
	implementation("org.web3j:infura:+")
	implementation("com.google.guava:guava:+")
	implementation("org.bouncycastle:bcprov-jdk15on:+")
	implementation("org.bouncycastle:bcpkix-jdk15on:+")
	implementation("com.xenomachina:kotlin-argparser:+")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
	testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "14"
	}
}

//defaultTasks "run"
//mainClassName = "com.y.et.EtApplicationKt"

// gradle wrapper --gradle-version 5.6.3 --distribution-type all

tasks.wrapper {
    gradleVersion = gradle_version
    distributionType = Wrapper.DistributionType.ALL
}
