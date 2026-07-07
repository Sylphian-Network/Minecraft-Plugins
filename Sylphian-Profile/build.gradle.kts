// Sylphian-Profile
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":Sylphian-Database"))
    compileOnly(project(":Sylphian-Scoreboard"))
    compileOnly(libs.placeholderapi)
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
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
