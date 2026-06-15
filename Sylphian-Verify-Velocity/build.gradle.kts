// Sylphian-Verify-Velocity
dependencies {
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
}

tasks {
    jar {
        archiveVersion.set("")
    }
}