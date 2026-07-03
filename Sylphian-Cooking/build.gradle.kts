// Sylphian-Cooking
dependencies {
    compileOnly(project(":Sylphian-Skills"))
    compileOnly(project(":Sylphian-Database"))
    compileOnly(libs.paper.api)
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
    compileOnly(project(":Sylphian-Items"))
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
