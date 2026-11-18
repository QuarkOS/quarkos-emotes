plugins {
    id("dev.kikugie.loom-back-compat")
}

version = "${property("mod.version")}+${sc.current.version}"
base.archivesName = "quarkos_emotes-fabric"

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
    modCompileOnly("maven.modrinth:modmenu:${sc.properties.get<String>("deps.modmenu")}")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.code.gson:gson:2.11.0")
}

loom {
    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = providers.gradleProperty("runDir").orNull?.let { rootProject.file(it) } ?: if (sc.current.version == "1.21.11") {
            rootProject.file("run")
        } else {
            rootProject.file("run/${sc.current.version}")
        }
    }
}

java {
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val chiseledMain = layout.buildDirectory.dir("generated/stonecutter/main")
gradle.projectsEvaluated {
    val main = sourceSets.named("main").get()
    val generated = chiseledMain.get()
    main.java.setSrcDirs(listOf(generated.dir("java").asFile))
    main.resources.setSrcDirs(listOf(generated.dir("resources").asFile))
}
tasks.named("compileJava") { dependsOn("stonecutterGenerate") }
tasks.named("processResources") { dependsOn("stonecutterGenerate") }
tasks.named("compileTestJava") { dependsOn("stonecutterGenerate") }
tasks.named("sourcesJar") { dependsOn("stonecutterGenerate") }

val writePack = tasks.register("writePackMeta") {
    val out = layout.buildDirectory.file("generated/pack/pack.mcmeta")
    outputs.file(out)
    val meta = if (sc.current.parsed >= "1.21.9") {
        """{"pack":{"description":"QuarkOS Emotes","min_format":[75,0],"max_format":[75,0]}}""" + "\n"
    } else {
        """{"pack":{"description":"QuarkOS Emotes","pack_format":34}}""" + "\n"
    }
    doLast {
        val file = out.get().asFile
        file.parentFile.mkdirs()
        file.writeText(meta)
    }
}

tasks {
    processResources {
        dependsOn(writePack)
        val minecraftVersion = sc.current.version
        val props = mapOf(
            "id" to sc.properties.get<String>("mod.id"),
            "name" to sc.properties.get<String>("mod.name"),
            "version" to "${sc.properties.get<String>("mod.version")}+$minecraftVersion",
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
        )
        inputs.properties(props)
        exclude("META-INF/mods.toml")
        exclude("pack.mcmeta")
        filesMatching("fabric.mod.json") {
            expand(props)
        }
        from(layout.buildDirectory.dir("generated/pack"))
    }

    test {
        useJUnitPlatform()
    }

    withType<JavaExec>().configureEach {
        val shots = providers.gradleProperty("shots").orNull
            ?: rootProject.file("build/shots/fabric/${sc.current.version}").absolutePath
        systemProperty("quarkos_emotes.shots", shots)
        systemProperty("quarkos_emotes.loader", "fabric")
        systemProperty("quarkos_emotes.guiScale", providers.gradleProperty("guiScale").orNull ?: "0")
        if (name == "runClient" && providers.gradleProperty("harness").orNull == "true") {
            systemProperty("quarkos_emotes.harness", "true")
            val wide = providers.gradleProperty("wide").orNull == "true"
            val width = providers.gradleProperty("width").orNull ?: if (wide) "1920" else "960"
            val height = providers.gradleProperty("height").orNull ?: if (wide) "1080" else "540"
            args("--width", width, "--height", height)
            // Same offline player as the other head shots, so the default skin does not change.
            args("--username", providers.gradleProperty("username").orNull ?: "Player546")
        }
        if (providers.gradleProperty("store").orNull == "true") {
            systemProperty("quarkos_emotes.store", "true")
        }
        val only = providers.gradleProperty("only").orNull
        if (!only.isNullOrBlank()) {
            systemProperty("quarkos_emotes.only", only)
        }
        val display = providers.gradleProperty("display").orNull
        val xauth = providers.gradleProperty("xauth").orNull
        if (!display.isNullOrBlank()) {
            environment("DISPLAY", display)
        }
        if (!xauth.isNullOrBlank()) {
            environment("XAUTHORITY", xauth)
        }
    }

    named<JavaExec>("runClient") {
        jvmArgs("-Xmx${providers.gradleProperty("gameMemory").orNull ?: "1536M"}")
        doFirst {
            val eula = workingDir.resolve("eula.txt")
            eula.parentFile?.mkdirs()
            eula.writeText("eula=true\n")
        }
    }
}

apply(from = rootProject.file("gradle/mp-runs.gradle.kts"))

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE")) { into("META-INF") }
    exclude("**/*.bbmodel", "**/*.py", "**/*.sh", "**/*Test.class", "**/test/**", "dev/quarkos/emotes/harness/**")
}
