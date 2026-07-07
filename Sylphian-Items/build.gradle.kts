// Sylphian-Items
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.command.api)
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
