// Sylphian-Cooking
dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly(project(":Sylphian-Core"))
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
