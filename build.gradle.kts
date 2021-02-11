import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.publish.maven.MavenPom

plugins {
    `java-library`
    `maven-publish`
}

val major = "0"
val minor = "6"
val patch = "0"

group = "io.oengus"
version = "$major.$minor.$patch"

val tokens = mapOf(
    "MAJOR" to major,
    "MINOR" to minor,
    "PATCH" to patch,
    "VERSION" to version
)

repositories {
    mavenCentral()
    jcenter() // Legacy :(
}

val versions = mapOf(
    "slf4j" to "1.7.25",
    "okhttp" to "3.14.9",
    "json" to "20180813",
    "junit" to "4.12",
    "mockito" to "3.6.28",
    "powermock" to "2.0.9",
    "logback" to "1.2.3"
)

dependencies {
    api("org.slf4j:slf4j-api:${versions["slf4j"]}")
    api("com.squareup.okhttp3:okhttp:${versions["okhttp"]}")
    api("org.json:json:${versions["json"]}")
    api("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.jetbrains:annotations:16.0.1")

    testImplementation("junit:junit:${versions["junit"]}")
    testImplementation("org.mockito:mockito-core:${versions["mockito"]}")
    testImplementation("org.powermock:powermock-module-junit4:${versions["powermock"]}")
    testImplementation("org.powermock:powermock-api-mockito2:${versions["powermock"]}")
    //testCompile("ch.qos.logback:logback-classic:${versions["logback"]}")
}

fun getProjectProperty(name: String) = project.properties[name] as? String

val javadoc: Javadoc by tasks
val jar: Jar by tasks

val sources = tasks.create("sources", Copy::class.java) {
    from("src/main/java")
    into("$buildDir/sources")
    filter<ReplaceTokens>("tokens" to tokens)
}

javadoc.dependsOn(sources)
javadoc.source = fileTree(sources.destinationDir)
javadoc.isFailOnError = false
if (!System.getProperty("java.version").startsWith("1.8"))
    (javadoc.options as CoreJavadocOptions).addBooleanOption("html5", true)

val javadocJar = tasks.create("javadocJar", Jar::class.java) {
    dependsOn(javadoc)
    from(javadoc.destinationDir)
    classifier = "javadoc"
}

val sourcesJar = tasks.create("sourcesJar", Jar::class.java) {
    dependsOn(sources)
    from(sources.destinationDir)
    classifier = "sources"
}

val compileJava: JavaCompile by tasks
compileJava.options.isIncremental = true
compileJava.source = fileTree(sources.destinationDir)
compileJava.dependsOn(sources)

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val test: Task by tasks
val build: Task by tasks
build.apply {
    dependsOn(javadocJar)
    dependsOn(sourcesJar)
    dependsOn(jar)
    dependsOn(test)
}

// Generate pom file for maven central

fun generatePom(): MavenPom.() -> Unit {
    return {
        packaging = "jar"
        name.set(project.name)
        description.set("Provides easy to use bindings for the Discord Webhook API")
        url.set("https://github.com/MinnDevelopment/discord-webhooks")
        scm {
            url.set("https://github.com/MinnDevelopment/discord-webhooks")
            connection.set("scm:git:git://github.com/MinnDevelopment/discord-webhooks")
            developerConnection.set("scm:git:ssh:git@github.com:MinnDevelopment/discord-webhooks")
        }
        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("Minn")
                name.set("Florian Spieß")
                email.set("business@minnced.club")
            }
        }
    }
}


// Publish

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = project.name
            groupId = project.group as String
            version = project.version as String

            artifact(sourcesJar)
            artifact(javadocJar)

            pom.apply(generatePom())
        }
    }
}
