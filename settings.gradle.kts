import dev.scaffoldit.hytale.wire.HytaleManifest

rootProject.name = "BloodMagic"

plugins {
    // See documentation on https://scaffoldit.dev
    id("dev.scaffoldit") version "0.2.+"
}

// Would you like to do a split project?
// Create a folder named "common", then configure details with `common { }`

hytale {
    usePatchline("release")
    useVersion("latest")

    repositories {
        // Any external repositories besides: MavenLocal, MavenCentral, HytaleMaven, and CurseMaven
    }

    dependencies {
        // Any external dependency you also want to include
    }

    manifest {
        Group = "Exotik850"
        Name = "[Exotik's] Blood Magic"
        Main = "dev.byt3.bloodmagic.BloodMagicPlugin"
        Description = "Adds a magical system based on blood and rituals."
        Version = "0.1.0"
        Authors = listOf(
            HytaleManifest.Author("Exotik850")
        )
    }
}