plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("com.gradle.develocity").version("3.17.1")
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("no")
    }
}

rootProject.name = "happ-backend"

include("shared")

include(":nutrition")

project(":nutrition").projectDir = File("./services/nutrition")
