import com.google.protobuf.gradle.id

plugins {
    id("net.neoforged.moddev")
    id("com.google.protobuf")
}

version = "${property("mod.version")}+${stonecutter.current.version}-neoforge"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    stonecutter.eval(stonecutter.current.version, ">=1.20.6") -> JavaVersion.VERSION_21
    stonecutter.eval(stonecutter.current.version, ">=1.18") -> JavaVersion.VERSION_17
    stonecutter.eval(stonecutter.current.version, ">=1.17") -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    google()

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
        //artifact = "com.google.protobuf:protoc:${property("deps.protoc")}"
        path = "/opt/homebrew/bin/protoc"
    }
    plugins {
        id("grpc") {
            //artifact = "io.grpc:protoc-gen-grpc-java:${property("deps.grpc")}"
            path = "/opt/homebrew/bin/protoc-gen-grpc-java"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc") { }

            }
        }
    }
}

dependencies {
    implementation("dev.architectury:architectury-neoforge:${property("deps.architectury_api")}")

    implementation("com.google.protobuf:protobuf-java:${property("deps.protoc")}")
    implementation("io.grpc:grpc-protobuf:${property("deps.grpc")}")
    implementation("io.grpc:grpc-stub:${property("deps.grpc")}")
    implementation("io.grpc:grpc-netty-shaded:${property("deps.grpc")}")
    //implementation("com.google.guava:guava:29.0-jre")
    jarJar("com.google.protobuf:protobuf-java:${property("deps.protoc")}")
    jarJar("io.grpc:grpc-protobuf:${property("deps.grpc")}")
    jarJar("io.grpc:grpc-stub:${property("deps.grpc")}")
    jarJar("io.grpc:grpc-netty-shaded:${property("deps.grpc")}")
    //jarJar("com.google.guava:guava:29.0-jre")

    implementation("javax.annotation:javax.annotation-api:1.3.2")

    //protobuf(files("src/main/proto/"))
}

neoForge {
    version = property("deps.neoforge") as String

    runs {
        register("client") {
            gameDirectory = file("../../run/")
            client()
        }

        register("server") {
            gameDirectory = file("../../run/")
            server()
        }
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
        inputs.property("minecraft", project.property("mod.mc_dep_forgelike"))
        inputs.property("architectury_api", project.property("deps.architectury_api"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep_forgelike"),
            "architectury_api" to project.property("deps.architectury_api")
        )

        filesMatching("META-INF/neoforge.mods.toml") { expand(props) }

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register("listrepos") {
        println("Repositories:")
        project.repositories.forEach { it -> println("Name: " + it.name) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}
