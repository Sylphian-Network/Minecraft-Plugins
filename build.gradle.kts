plugins {
    java
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
        maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
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
}
