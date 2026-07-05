plugins {
    java
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://repo.extendedclip.com/releases/") }
    }
}

subprojects {
    plugins.apply("java-library")

    java {
        toolchain.languageVersion = JavaLanguageVersion.of(25)
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

tasks.register<Javadoc>("combineJavadoc") {
    description = "Generates combined Javadoc for all subprojects."
    group = "documentation"

    destinationDir = file("$rootDir/docs")

    val javaSubprojects = subprojects.filter {
        it.plugins.hasPlugin("java") || it.plugins.hasPlugin("java-library")
    }

    dependsOn(javaSubprojects.map { it.tasks.named("compileJava") })

    source(javaSubprojects.map { it.sourceSets["main"].allJava })
    classpath = files(javaSubprojects.map { it.sourceSets["main"].compileClasspath })

    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        isAuthor = true
        isVersion = true
        windowTitle = "Sylphian Network - Plugin API Documentation"
        docTitle = "Sylphian Network Plugin Documentation"
        addBooleanOption("Xdoclint:none", true)
    }
}
