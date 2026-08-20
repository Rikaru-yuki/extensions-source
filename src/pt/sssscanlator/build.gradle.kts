import io.github.keiyoushi.gradle.api.ContentWarning

plugins {
    alias(kei.plugins.extension)
}

keiyoushi {
    name = "Yomu Comics"
<<<<<<< HEAD
    versionCode = 54
    contentWarning = ContentWarning.MIXED
    libVersion = "1.6"
=======
    versionCode = 55
    contentWarning = ContentWarning.NSFW // or MIXED, please confirm
    libVersion = "1.4"
>>>>>>> upstream/main

    source {
        lang = "pt-BR"
        baseUrl = "https://yomu.com.br"
        id = 1497838059713668619L
    }
}

dependencies {
    implementation(project(":lib:cryptoaes"))
}
