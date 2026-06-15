// Sylphian-Verify-Paper
plugins {
    id("com.gradleup.shadow") version "9.4.1"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly(project(":Sylphian-Database"))
    compileOnly(project(":Sylphian-Profile"))
    compileOnly("org.jdbi:jdbi3-core:3.47.0")
    compileOnly("org.jdbi:jdbi3-sqlobject:3.47.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
}

tasks {
    jar {
        enabled = false
    }

    shadowJar {
        dependsOn(":Sylphian-Database:shadowJar")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version )
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
