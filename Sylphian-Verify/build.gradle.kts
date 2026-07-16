// Sylphian-Verify
dependencies {
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
}

val generateBuildConstants by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/sources/buildConstants")
    val pluginVersion = project.version.toString()
    inputs.property("version", pluginVersion)
    outputs.dir(outputDir)
    doLast {
        val pkgDir = outputDir.get().dir("net/sylphian/velocity/verify").asFile
        pkgDir.mkdirs()
        pkgDir.resolve("BuildConstants.java").writeText(
            """
            package net.sylphian.velocity.verify;

            /** Generated build constants. Do not edit by hand. */
            public final class BuildConstants {
                /** The plugin version, sourced from the Gradle build. */
                public static final String VERSION = "$pluginVersion";

                private BuildConstants() {
                }
            }
            """.trimIndent() + "\n"
        )
    }
}

sourceSets {
    main {
        java {
            srcDir(generateBuildConstants)
        }
    }
}

tasks {
    jar {
        archiveVersion.set("")
    }
}
