// Sylphian-Entities
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.command.api)
    compileOnly(project(":Sylphian-Items"))
}

tasks {
    jar {
        archiveVersion.set("")
    }

    processResources {
        val props = mapOf("version" to version )
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
