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

include(":service1")
include(":service2")

project(":service1").projectDir = File("./services/service1")
project(":service2").projectDir = File("./services/service2")
