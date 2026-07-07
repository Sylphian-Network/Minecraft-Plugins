// Sylphian-Economy
dependencies {
    compileOnly(libs.paper.api)
    compileOnly(project(":Sylphian-Database"))
    compileOnly(libs.jdbi.core)
    compileOnly(libs.jdbi.sqlobject)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.command.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testRuntimeOnly(libs.slf4j.simple)
    testImplementation(libs.assertj.core)
    testImplementation(libs.h2)
    testImplementation(libs.jdbi.core)
    testImplementation(libs.jdbi.sqlobject)
    testImplementation(project(":Sylphian-Database")) {
        attributes {
            attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                objects.named(LibraryElements::class.java, LibraryElements.CLASSES)
            )
        }
    }
}

tasks {
    jar {
        archiveVersion.set("")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
