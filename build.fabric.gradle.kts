import com.google.protobuf.gradle.id

plugins {
    id("fabric-loom")
    id("com.google.protobuf")
    id("com.gradleup.shadow")
}

version = "${property("mod.version")}+${stonecutter.current.version}-fabric"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    stonecutter.eval(stonecutter.current.version, ">=1.20.6") -> JavaVersion.VERSION_21
    stonecutter.eval(stonecutter.current.version, ">=1.18") -> JavaVersion.VERSION_17
    stonecutter.eval(stonecutter.current.version, ">=1.17") -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

// Libraries bundled into the mod jar and relocated so they can't conflict
// with copies from other mods. Minecraft already provides guava and gson.
val shade: Configuration by configurations.creating {
    exclude(group = "com.google.guava")
    exclude(group = "com.google.code.gson")
    exclude(group = "com.google.code.findbugs")
    exclude(group = "com.google.errorprone")
    exclude(group = "com.google.j2objc")
    exclude(group = "com.google.android")
    exclude(group = "org.checkerframework")
    exclude(group = "org.codehaus.mojo")
}

repositories {
    mavenCentral()

    maven {
        name = "Architectury"
        url = uri("https://maven.architectury.dev/")
        content {
            includeGroupAndSubgroups("dev.architectury")
            includeGroupAndSubgroups("me.shedaniel")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${property("deps.protoc")}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${property("deps.grpc")}"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                id("grpc") { }
            }
        }
    }
}

dependencies {
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("dev.architectury:architectury-fabric:${property("deps.architectury_api")}")

    fun shadeDep(dep: String) {
        implementation(dep)
        shade(dep)
    }

    shadeDep("com.google.protobuf:protobuf-java:${property("deps.protoc")}")
    shadeDep("io.grpc:grpc-protobuf:${property("deps.grpc")}")
    shadeDep("io.grpc:grpc-stub:${property("deps.grpc")}")
    shadeDep("io.grpc:grpc-okhttp:${property("deps.grpc")}")
    implementation("javax.annotation:javax.annotation-api:1.3.2")

    //protobuf(files("src/main/proto/"))

    minecraft("com.mojang:minecraft:${stonecutter.current.version}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep_fabric"))
        inputs.property("architectury_api", project.property("deps.architectury_api"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep_fabric"),
            "architectury_api" to project.property("deps.architectury_api")
        )

        filesMatching("fabric.mod.json") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    jar {
        archiveClassifier = "dev"
    }

    shadowJar {
        configurations.set(listOf(shade))
        archiveClassifier = "dev-shadow"

        relocate("io.grpc", "chat.seabird.vendor.io.grpc")
        relocate("io.perfmark", "chat.seabird.vendor.io.perfmark")
        relocate("okio", "chat.seabird.vendor.okio")
        relocate("com.google.protobuf", "chat.seabird.vendor.com.google.protobuf")
        // proto-google-common-protos
        for (pkg in listOf("api", "cloud", "geo", "logging", "longrunning", "rpc", "type")) {
            relocate("com.google.$pkg", "chat.seabird.vendor.com.google.$pkg")
        }

        mergeServiceFiles()
    }

    remapJar {
        inputFile = shadowJar.flatMap { it.archiveFile }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}
