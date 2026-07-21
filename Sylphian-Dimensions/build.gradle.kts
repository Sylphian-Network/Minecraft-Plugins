// Sylphian-Dimensions
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.command.api)
    compileOnly(libs.placeholderapi)
    compileOnly(project(":Sylphian-Clans"))
    compileOnly(project(":Sylphian-Entities"))
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
