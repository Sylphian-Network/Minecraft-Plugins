// Sylphian-Logging
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.command.api)
    compileOnly(project(":Sylphian-Items"))
    compileOnly(project(":Sylphian-Gathering"))
    compileOnly(project(":Sylphian-Skills"))
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
