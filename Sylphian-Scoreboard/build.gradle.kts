// Sylphian-Scoreboard
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
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
