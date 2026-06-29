// Sylphian-Cooking
dependencies {
    compileOnly(project(":Sylphian-Skills"))
    compileOnly(project(":Sylphian-Database"))
    compileOnly(libs.paper.api)
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
    compileOnly(project(":Sylphian-Items"))
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
