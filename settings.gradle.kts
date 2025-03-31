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

include(":api_gw")
include(":echo")
include(":auth")
include(":userData")
include(":activity")
include(":nutrition")
include(":weight_history")

project(":api_gw").projectDir = File("./services/api_gw")
project(":echo").projectDir = File("./services/echo")
project(":auth").projectDir = File("./services/auth")
project(":userData").projectDir = File("./services/userData")
project(":activity").projectDir = File("./services/activity")
project(":nutrition").projectDir = File("./services/nutrition")
project(":weight_history").projectDir = File("./services/weight_history")
