// Sylphian-Verify-Paper
plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":Sylphian-Database"))
    compileOnly(project(":Sylphian-Profile"))
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
    implementation(libs.caffeine)
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
