import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.3.0.BUILD-SNAPSHOT"
	id("io.spring.dependency-management") version "1.0.8.RELEASE"
	kotlin("jvm") version "1.3.70"
	kotlin("plugin.spring") version "1.3.70"
}

group = "com.y"
version = "0.2.7-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_12

//configurations {
//	implementation {
//		exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
//	}
//}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
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
		jvmTarget = "12"
	}
}

//defaultTasks "run"
//mainClassName = "com.y.et.EtApplicationKt"

// gradle wrapper --gradle-version 5.6.3 --distribution-type all

tasks.wrapper {
    gradleVersion = "6.2.2"
    distributionType = Wrapper.DistributionType.ALL
}
