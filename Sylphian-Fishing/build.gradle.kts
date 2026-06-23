// Sylphian-Fishing
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":Sylphian-Database"))
    compileOnly(project(":Sylphian-Items"))
    compileOnly(project(":Sylphian-Scoreboard"))
    compileOnly(project(":Sylphian-Crates"))
    compileOnly(project(":Sylphian-Skills"))
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
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
