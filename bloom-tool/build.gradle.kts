import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    application
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(project(":bloom-filter"))

    implementation("com.offbytwo:docopt:0.6.0.20150202")    // CLI parsing, see: https://github.com/docopt/docopt.java
    implementation("com.google.guava:guava:22.0")

    testImplementation("junit:junit:4.12")
    testImplementation("pl.pragmatists:JUnitParams:1.0.6")
    testImplementation("org.assertj:assertj-core:3.6.2")

    testRuntimeOnly("org.junit.vintage", "junit-vintage-engine","5.3.2")
}

application {
    mainClassName = "eu.xword.nixer.bloom.BloomToolMain"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
