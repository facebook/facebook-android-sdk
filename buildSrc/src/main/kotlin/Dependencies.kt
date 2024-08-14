object Config {
    const val minSdk = 15
    const val compileSdk = 31
    const val targetSdk = 31
}

object Versions {
    const val acra = "5.5.0"
    const val analytics = "4.+"
    const val android_billingclient = "6.0.0"
    const val android_installreferrer =
        "1.0" // there are issues with 1.1 regarding permissions, asks for unnecessary permissions
    const val androidx = "1.1.0"
    const val androidxActivity = "1.2.0"
    const val androidxAnnotation = "1.1.0"
    const val androidxBrowser = "1.0.0"
    const val androidxCardview = "1.0.0"
    const val androidxConstraintLayout = "1.1.3"
    const val androidxCore = "1.0.0"
    const val androidxCoreKtx = "1.3.2"
    const val androidxEspressoCore = "3.1.0"
    const val androidxFragment = "1.3.0"
    const val androidxLegacy = "1.0.0"
    const val androidxTest = "1.4.0"
    const val assertj = "3.15.0"
    const val dexmaker = "1.2"
    const val dokka = "1.4.30"
    const val firebaseBom = "29.3.1"
    const val firebaseMessaging = "23.0.3"
    const val fresco = "2.6.0"
    const val glide = "4.13.0"
    const val googleServices = "4.3.5"
    const val gradle = "4.2.0" // Android gradle plugin version
    const val gson = "2.8.8"
    const val guava = "18.0"
    const val junit = "4.13"
    const val jacoco = "0.8.7"
    const val json = "20180130"
    const val kotlin = "1.5.10"
    const val leakcanaryAndroid = "2.9.1"
    const val liquidcore = "0.6.2"
    const val material = "1.0.0"
    const val mockitoInline = "2.26.0"
    const val mockitoKotlin = "2.2.11"
    const val mockwebserver = "4.9.0"
    const val playServicesAuth = "16.0.0"
    const val playServicesGcm = "17.0.0"
    const val powerMock = "2.0.2"
    const val robolectric = "4.4"
    const val stetho = "1.5.0"
    const val testng = "6.9.6"
    const val zxing = "3.3.3"
}

object Plugins {
    const val android_gradle = "com.android.tools.build:gradle:${Versions.gradle}"
    const val kotlin_gradle = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Versions.dokka}"
    const val google_services = "com.google.gms:google-services:${Versions.googleServices}"
    const val jacoco = "org.jacoco:org.jacoco.core:${Versions.jacoco}"
}

object Libs {

    // android
    const val android_billingclient =
        "com.android.billingclient:billing:${Versions.android_billingclient}"
    const val android_installreferrer =
        "com.android.installreferrer:installreferrer:${Versions.android_installreferrer}" // https://developer.android.com/google/play/installreferrer/library.html#java
    const val androidx_activity = "androidx.activity:activity:${Versions.androidxActivity}"
    const val androidx_annotation = "androidx.annotation:annotation:${Versions.androidxAnnotation}"
    const val androidx_appcompat = "androidx.appcompat:appcompat:${Versions.androidx}"
    const val androidx_browser = "androidx.browser:browser:${Versions.androidxBrowser}"
    const val androidx_cardview = "androidx.cardview:cardview:${Versions.androidxCardview}"
    const val androidx_constraintlayout =
        "androidx.constraintlayout:constraintlayout:${Versions.androidxConstraintLayout}"
    const val androidx_core = "androidx.core:core:${Versions.androidxCore}"
    const val androidx_core_ktx = "androidx.core:core-ktx:${Versions.androidxCoreKtx}"
    const val androidx_espresso_core =
        "androidx.test.espresso:espresso-core:${Versions.androidxEspressoCore}"
    const val androidx_fragment = "androidx.fragment:fragment:${Versions.androidxFragment}"
    const val androidx_legacy_support_v4 =
        "androidx.legacy:legacy-support-v4:${Versions.androidxLegacy}"
    const val androidx_legacy_support_core_utils =
        "androidx.legacy:legacy-support-core-utils:${Versions.androidxLegacy}"
    const val androidx_recyclerview = "androidx.recyclerview:recyclerview:${Versions.androidx}"
    const val androidx_test_core = "androidx.test:core:${Versions.androidxTest}"

    // google
    const val dexmaker = "com.google.dexmaker:dexmaker:${Versions.dexmaker}"
    const val dexmaker_mockito = "com.google.dexmaker:dexmaker-mockito:${Versions.dexmaker}"
    const val firebase_bom = "com.google.firebase:firebase-bom:${Versions.firebaseBom}"
    const val firebase_analytics = "com.google.firebase:firebase-analytics"
    const val firebase_messaging =
        "com.google.firebase:firebase-messaging:${Versions.firebaseMessaging}"
    const val gson = "com.google.code.gson:gson:${Versions.gson}"
    const val guava = "com.google.guava:guava:${Versions.guava}"
    const val material = "com.google.android.material:material:${Versions.material}"
    const val play_services_auth =
        "com.google.android.gms:play-services-auth:${Versions.playServicesAuth}"
    const val play_services_gcm =
        "com.google.android.gms:play-services-gcm:${Versions.playServicesGcm}"
    const val zxing = "com.google.zxing:core:${Versions.zxing}"

    // 1st-party
    const val fresco = "com.facebook.fresco:fresco:${Versions.fresco}"
    const val stetho = "com.facebook.stetho:stetho:${Versions.stetho}"

    const val acra = "ch.acra:acra-mail:${Versions.acra}"
    const val analytics = "com.segment.analytics.android:analytics:${Versions.analytics}"
    const val assertj_core = "org.assertj:assertj-core:${Versions.assertj}"
    const val glide = "com.github.bumptech.glide:glide:${Versions.glide}"
    const val glide_compiler = "com.github.bumptech.glide:compiler:${Versions.glide}"
    const val json = "org.json:json:${Versions.json}"
    const val junit = "junit:junit:${Versions.junit}"
    const val kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
    const val kotlin_stdlib_jdk7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val kotlin_test_junit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
    const val leakcanary_android =
        "com.squareup.leakcanary:leakcanary-android:${Versions.leakcanaryAndroid}"
    const val leakcanary_object_watcher =
        "com.squareup.leakcanary:leakcanary-object-watcher-android:${Versions.leakcanaryAndroid}"
    const val liquidcore = "com.github.LiquidPlayer:LiquidCore:${Versions.liquidcore}"
    const val mockito_inline = "org.mockito:mockito-inline:${Versions.mockitoInline}"
    const val mockito_kotlin = "org.mockito.kotlin:mockito-kotlin:${Versions.mockitoKotlin}"
    const val mockwebserver = "com.squareup.okhttp3:mockwebserver:${Versions.mockwebserver}"
    const val powermock_core = "org.powermock:powermock-core:${Versions.powerMock}"
    const val powermock_api_mockito2 = "org.powermock:powermock-api-mockito2:${Versions.powerMock}"
    const val powermock_junit4 = "org.powermock:powermock-module-junit4:${Versions.powerMock}"
    const val powermock_junit4_rule =
        "org.powermock:powermock-module-junit4-rule:${Versions.powerMock}"
    const val powermock_classloading_xstream =
        "org.powermock:powermock-classloading-xstream:${Versions.powerMock}"
    const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val testng = "org.testng:testng:${Versions.testng}"
}
