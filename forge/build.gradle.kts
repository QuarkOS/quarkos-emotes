buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.fabricmc.net/")
    }
    dependencies {
        classpath("net.minecraftforge.gradle:net.minecraftforge.gradle.gradle.plugin:7.0.40")
    }
}

data class ForgeNode(
    val forgeCoordinate: String,
    val forgeDep: String,
    val packMeta: String,
)

val minecraftVersion = project.name
val forgeNode = when (minecraftVersion) {
    "1.21.1" -> ForgeNode(
        "1.21.1-52.1.20",
        "52.1.20",
        """{"pack":{"description":"QuarkOS Emotes","pack_format":34}}""" + "\n",
    )
    "1.21.11" -> ForgeNode(
        "1.21.11-61.2.4",
        "61.2.4",
        """{"pack":{"description":"QuarkOS Emotes","min_format":[75,0],"max_format":[75,0]}}""" + "\n",
    )
    else -> null
}

if (minecraftVersion == "forge" || forgeNode == null) {
    tasks.configureEach {
        if (name != "build") {
            enabled = false
        }
    }
    tasks.named("build") {
        enabled = true
        setDependsOn(emptyList<Any>())
        doLast {
            if (forgeNode == null && minecraftVersion != "forge") {
                throw GradleException("No Forge coordinate is configured for Minecraft $minecraftVersion.")
            }
        }
    }
} else {
    val node = forgeNode
    apply(plugin = "java")
    apply(plugin = "net.minecraftforge.gradle")

    version = "1.0.0+$minecraftVersion"
    base.archivesName.set("quarkos_emotes-forge")
    group = "dev.quarkos"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    val minecraftExt = extensions.getByName("minecraft")
    val fg = extensions.getByName("fg")
    val forgeMinecraft = minecraftExt as net.minecraftforge.gradle.MinecraftExtensionForProject
    forgeMinecraft.mappings("official", minecraftVersion)
    val runs = (minecraftExt as groovy.lang.GroovyObject).getProperty("runs")
        as org.gradle.api.NamedDomainObjectContainer<net.minecraftforge.gradle.SlimeLauncherOptions>
    val runDir = rootProject.layout.projectDirectory.dir("run/forge/$minecraftVersion")
    runs.configureEach {
        workingDir.set(runDir)
        systemProperty("forge.logging.console.level", "info")
    }
    val sourceSets = extensions.getByType(org.gradle.api.tasks.SourceSetContainer::class.java)
    val mainSource = sourceSets.named("main")
    val chiseledMain = layout.buildDirectory.dir("generated/stonecutter/main")
    val syncBranchSources = tasks.register<Sync>("syncBranchSources") {
        from(rootProject.file("src"))
        into(rootProject.file("forge/src"))
    }
    tasks.named("stonecutterPrepare") { dependsOn(syncBranchSources) }
    tasks.matching { it.name == "stonecutterPrepareTest" || it.name == "processTestResources" || it.name == "compileTestJava" }.configureEach { dependsOn(syncBranchSources) }
    // Both Forge versions sync the shared tree into forge/src. Order the writes
    // and the reads so a combined compile does not race that directory.
    gradle.projectsEvaluated {
        val thisSync = syncBranchSources.get()
        rootProject.subprojects.forEach { other ->
            val otherSync = other.tasks.findByName("syncBranchSources") ?: return@forEach
            if (otherSync == thisSync) {
                return@forEach
            }
            tasks.named("stonecutterPrepare").configure { mustRunAfter(otherSync) }
            tasks.matching { it.name == "stonecutterPrepareTest" || it.name == "processTestResources" || it.name == "compileTestJava" }.configureEach { mustRunAfter(otherSync) }
            if (project.path > other.path) {
                thisSync.mustRunAfter(otherSync)
            }
        }
    }
    gradle.projectsEvaluated {
        val main = mainSource.get()
        main.java.setSrcDirs(listOf(chiseledMain.get().dir("java").asFile))
        main.resources.setSrcDirs(listOf(chiseledMain.get().dir("resources").asFile))
        main.output.setResourcesDir(main.java.destinationDirectory)
    }
    tasks.named("compileJava") { dependsOn("stonecutterGenerate") }
    tasks.named("processResources") { dependsOn("stonecutterGenerate") }
    runs.register("client") {
        mods {
            register("quarkos_emotes") {
                source(mainSource.get())
            }
        }
    }

    repositories {
        forgeMinecraft.mavenizer(this)
        maven((fg as net.minecraftforge.gradle.ForgeGradleExtension).forgeMaven)
        maven((fg as net.minecraftforge.gradle.ForgeGradleExtension).minecraftLibsMaven)
        maven("https://maven.minecraftforge.net/")
        mavenCentral()
    }

    dependencies {
        add("implementation", forgeMinecraft.dependency("net.minecraftforge:forge:${node.forgeCoordinate}"))
        // Same pure-Java unit tests as the Fabric targets.
        add("testImplementation", platform("org.junit:junit-bom:5.11.4"))
        add("testImplementation", "org.junit.jupiter:junit-jupiter")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        add("testImplementation", "com.google.code.gson:gson:2.11.0")
    }

    tasks.named<Jar>("jar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
        exclude("META-INF/MANIFEST.MF")
        manifest {
            attributes("MixinConfigs" to "quarkos_emotes.mixins.json")
        }
    }

    // Mixin 0.8 on Forge reads MixinConfigs from the jar manifest. mods.toml
    // [[mixins]] is not consulted, and the userdev mod is a classes directory,
    // so the same manifest has to land next to the classes or the client mixins
    // never apply and player poses remain vanilla.
    val forgeManifest = layout.buildDirectory.dir("generated/forge-manifest")
    val writeForgeManifest = tasks.register("writeForgeManifest") {
        outputs.dir(forgeManifest)
        doLast {
            val file = forgeManifest.get().asFile.resolve("META-INF/MANIFEST.MF")
            file.parentFile.mkdirs()
            file.writeText(
                "Manifest-Version: 1.0\n" +
                    "MixinConfigs: quarkos_emotes.mixins.json\n" +
                    "\n"
            )
        }
    }

    val forgePack = layout.buildDirectory.dir("generated/forge-pack")
    val writeForgePack = tasks.register("writeForgePack") {
        outputs.dir(forgePack)
        doLast {
            val file = forgePack.get().asFile.resolve("pack.mcmeta")
            file.parentFile.mkdirs()
            file.writeText(node.packMeta)
        }
    }

    tasks.named<ProcessResources>("processResources") {
        dependsOn(writeForgePack, writeForgeManifest)
        val props = mapOf(
            "version" to "1.0.0+$minecraftVersion",
            "forge_dep" to node.forgeDep,
            "minecraft" to minecraftVersion,
        )
        inputs.property("forge_dep", node.forgeDep)
        inputs.property("minecraft", minecraftVersion)
        filesMatching("META-INF/mods.toml") {
            expand(props)
        }
        exclude("fabric.mod.json")
        exclude("pack.mcmeta")
        val packText = node.packMeta
        doLast {
            val dest = destinationDir
            dest.resolve("pack.mcmeta").writeText(packText)
            // This Mixin build's Forge agent ignores mods.toml, so the config has to be on the manifest.
            val manifest = dest.resolve("META-INF/MANIFEST.MF")
            manifest.parentFile.mkdirs()
            manifest.writeText("Manifest-Version: 1.0\r\nMixinConfigs: quarkos_emotes.mixins.json\r\n\r\n")
        }
    }

    tasks.withType<JavaExec>().configureEach {
        val shots = providers.gradleProperty("shots").orNull
            ?: rootProject.file("build/shots/forge/$minecraftVersion").absolutePath
        systemProperty("quarkos_emotes.shots", shots)
        systemProperty("quarkos_emotes.loader", "forge")
        systemProperty("quarkos_emotes.guiScale", providers.gradleProperty("guiScale").orNull ?: "0")
        if (providers.gradleProperty("harness").orNull == "true") {
            systemProperty("quarkos_emotes.harness", "true")
            val width = providers.gradleProperty("width").orNull ?: "960"
            val height = providers.gradleProperty("height").orNull ?: "540"
            args("--width", width, "--height", height)
            // Same offline player as the other head shots, so the default skin does not change.
            args("--username", "Player546")
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
        environment("vblank_mode", "0")
        environment("__GL_SYNC_TO_VBLANK", "0")
        if (name == "runClient") {
            jvmArgs("-Xmx1536M")
            doFirst {
                val dir = workingDir
                dir.mkdirs()
                dir.resolve("eula.txt").writeText("eula=true\n")
                if (providers.gradleProperty("harness").orNull == "true") {
                    val options = dir.resolve("options.txt")
                    val existing = if (options.exists()) options.readText() else ""
                    options.writeText(seedHarnessOptions(existing, providers.gradleProperty("guiScale").orNull ?: "0"))
                }
            }
        }
    }
}

fun seedHarnessOptions(existing: String, guiScale: String): String {
    val wanted = linkedMapOf(
        "pauseOnLostFocus" to "false",
        "onboardAccessibility" to "false",
        "menuBackgroundBlurriness" to "0",
        "guiScale" to guiScale,
        "maxFps" to "30",
        "enableVsync" to "false",
        "renderDistance" to "8",
        "simulationDistance" to "6",
        "fov" to "54",
        "mipmapLevels" to "0",
        "entityShadows" to "false",
        "renderClouds" to "\"false\"",
        "particles" to "2",
        "graphicsPreset" to "\"fast\"",
        "inactivityFpsLimit" to "\"minimized\"",
        "fullscreen" to "false",
        "narrator" to "0",
    )
    val lines = existing.lines().toMutableList()
    val seen = mutableSetOf<String>()
    for (i in lines.indices) {
        val line = lines[i]
        val key = line.substringBefore(':')
        val value = wanted[key] ?: continue
        lines[i] = "$key:$value"
        seen += key
    }
    for ((key, value) in wanted) {
        if (key !in seen) {
            lines += "$key:$value"
        }
    }
    return lines.filter { it.isNotBlank() }.joinToString("\n") + "\n"
}

if (project.name != "forge" && project.plugins.hasPlugin("java")) {
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}
