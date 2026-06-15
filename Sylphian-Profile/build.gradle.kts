// Sylphian-Profile
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":Sylphian-Database"))
    compileOnly(project(":Sylphian-Scoreboard"))
    compileOnly(project(":Sylphian-Economy"))
    compileOnly(project(":Sylphian-Clans"))
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
