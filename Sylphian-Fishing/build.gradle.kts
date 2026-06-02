// Sylphian-Fishing
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly(project(":Sylphian-Database"))
    compileOnly(project(":Sylphian-Scoreboard"))
    compileOnly("org.jdbi:jdbi3-core:3.47.0")
    compileOnly("org.jdbi:jdbi3-sqlobject:3.47.0")
}

tasks {
    processResources {
        val props = mapOf("version" to version )
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
