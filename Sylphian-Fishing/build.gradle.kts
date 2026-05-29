// Sylphian-Fishing
plugins {
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly(project(":Sylphian-Database"))
    compileOnly("org.jdbi:jdbi3-core:3.47.0")
    compileOnly("org.jdbi:jdbi3-sqlobject:3.47.0")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G", "-Dcom.mojang.eula.agree=true")
    }

    processResources {
        val props = mapOf("version" to version )
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
