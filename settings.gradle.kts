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
include(":auth")
include(":userData")
include(":activity")
include(":echo")
include(":api_gw")
include(":weight_history")
include(":social")
include(":nutrition")

project(":auth").projectDir = File("./services/auth")
project(":userData").projectDir = File("./services/userData")
project(":activity").projectDir = File("./services/activity")
project(":echo").projectDir = File("./services/echo")
project(":api_gw").projectDir = File("./services/api_gw")
project(":weight_history").projectDir = File("./services/weight_history")
project(":social").projectDir = File("./services/social")
project(":nutrition").projectDir = File("./services/nutrition")