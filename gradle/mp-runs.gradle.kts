// Compile proof support separately: it is available to dev runs and absent from release jars.
val proofSources = extensions.getByType<SourceSetContainer>()
val runtimeMain = proofSources.named("main").get()
runtimeMain.java.exclude("dev/quarkos/emotes/harness/**")
val proof = proofSources.create("proof") {
    java.srcDir(layout.buildDirectory.dir("generated/stonecutter/main/java"))
    java.include("dev/quarkos/emotes/harness/**")
    compileClasspath += runtimeMain.output + runtimeMain.compileClasspath
    runtimeClasspath += output + compileClasspath
}
// Stonecutter configures every source set after evaluation; proof deliberately
// reads the already-processed main tree, with an independent compile destination.
gradle.projectsEvaluated {
    runtimeMain.java.exclude("dev/quarkos/emotes/harness/**")
    proof.java.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/main/java").get().asFile))
    proof.java.setIncludes(setOf("dev/quarkos/emotes/harness/**"))
    proof.resources.setSrcDirs(emptyList<String>())
}
tasks.named(proof.compileJavaTaskName) { dependsOn("stonecutterGenerate", "compileJava") }

// Shared proof properties for both loaders. Client arguments never reach the dedicated server.
tasks.withType<JavaExec>().configureEach {
    val isClient = name == "runClient"
    val isServer = name == "runServer"
    if (isClient || isServer) {
        dependsOn(proof.classesTaskName)
        classpath(proof.output)
        providers.gradleProperty("runDir").orNull?.let { workingDir(rootProject.file(it)) }
        if (isClient) {
            systemProperty("quarkos_emotes.quick", providers.gradleProperty("quick").orNull ?: "false")
            for (key in listOf("joinServer", "role", "vanilla", "late", "life")) {
                providers.gradleProperty(key).orNull?.let { systemProperty("quarkos_emotes.$key", it) }
            }
            if (providers.gradleProperty("harness").orNull != "true") {
                providers.gradleProperty("username").orNull?.let { args("--username", it) }
                if (providers.gradleProperty("joinServer").isPresent) {
                    args("--width", providers.gradleProperty("width").orNull ?: "960",
                         "--height", providers.gradleProperty("height").orNull ?: "540")
                }
            }
        }
        if (isServer) {
            jvmArgs("-Xmx${providers.gradleProperty("gameMemory").orNull ?: "1536M"}")
            args("nogui")
            systemProperty("quarkos_emotes.serverHarness", providers.gradleProperty("serverHarness").orNull ?: "false")
            systemProperty("quarkos_emotes.cleanNames", providers.gradleProperty("cleanNames").orNull ?: "false")
            systemProperty("quarkos_emotes.disableServer",
                providers.gradleProperty("disableServer").orNull
                    ?: providers.systemProperty("quarkos_emotes.disableServer").orNull ?: "false")
            doFirst {
                workingDir.mkdirs()
                workingDir.resolve("eula.txt").writeText("eula=true\n")
                val port = (providers.gradleProperty("port").orNull ?: "25599").toInt()
                require(port in 1..65535) { "port must be between 1 and 65535" }
                workingDir.resolve("server.properties").writeText("""
                    max-tick-time=-1
                    online-mode=false
                    server-ip=127.0.0.1
                    server-port=$port
                    level-type=minecraft:flat
                    generate-structures=false
                    spawn-protection=0
                    gamemode=creative
                    difficulty=peaceful
                    allow-flight=true
                    view-distance=6
                    simulation-distance=6
                    max-players=4
                    enable-status=true
                """.trimIndent() + "\n")
            }
            standardInput = System.`in`
        }
    }
}
