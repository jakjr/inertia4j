plugins {
    `java-library`
    `maven-publish`
    id("signing")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

val quarkusPlatformVersion = "3.37.2"

dependencies {
    implementation(project(":inertia4j.core"))
    api(project(":inertia4j.spi"))

    // compileOnly, mirroring inertia4j.spring/inertia4j.ktor: a Quarkus app consuming this adapter
    // already has these on its runtime classpath (it's a Quarkus app), pinned to whatever Quarkus
    // BOM version that app itself chose. Bundling them here instead would risk shipping a second,
    // possibly conflicting copy.
    compileOnly(platform("io.quarkus.platform:quarkus-bom:$quarkusPlatformVersion"))
    compileOnly("io.quarkus:quarkus-core")
    compileOnly("io.quarkus:quarkus-arc")
    compileOnly("io.quarkus:quarkus-rest")
    compileOnly("io.quarkus:quarkus-vertx")
    compileOnly("io.quarkus:quarkus-hibernate-validator")
    compileOnly("io.quarkus:quarkus-redis-client")
    compileOnly("io.vertx:vertx-web-sstore-redis")

    testImplementation(platform("io.quarkus.platform:quarkus-bom:$quarkusPlatformVersion"))
    testImplementation("io.quarkus:quarkus-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

group = "io.github.inertia4j"
version = "1.0.0-SNAPSHOT"

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = "inertia4j-quarkus"

            pom {
                name.set("Inertia4J Quarkus")
                description.set("Inertia4J back-end adapter for Quarkus")
                url.set("https://github.com/jakjr/inertia4j")
                inceptionYear.set("2026")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("edrd-f")
                        name.set("Eduardo Fonseca")
                    }
                    developer {
                        id.set("pefcos")
                        name.set("Pedro Fronchetti Costa da Silva")
                        email.set("pfronchetti@gmail.com")
                    }
                    developer {
                        id.set("jakjr")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/jakjr/inertia4j.git")
                    developerConnection.set("scm:git:ssh://git@github.com:jakjr/inertia4j.git")
                    url.set("https://github.com/jakjr/inertia4j")
                }
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("deploy").get().asFile.toURI()
        }
    }
}

configure<SigningExtension> {
    useGpgCmd()
    if (project.hasProperty("signing.keyId")) {
        useGpgCmd()
    }
}
