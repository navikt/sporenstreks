import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val mainClass = "no.nav.helse.sporenstreks.web.AppKt"
val kotlinVersion = "1.5.30"
val ktorVersion = "1.6.6"
val logbackVersion = "1.2.7"
val logbackContribVersion = "0.1.5"
val jacksonVersion = "2.13.0"
val prometheusVersion = "0.12.0"
val hikariVersion = "5.0.0"
val vaultJdbcVersion = "1.3.7"
val junitJupiterVersion = "5.8.1"
val assertJVersion = "3.21.0"
val mockKVersion = "1.12.1"
val tokenSupportVersion = "1.3.9"
val mockOAuth2ServerVersion = "0.3.6"
val koinVersion = "3.1.4"
val valiktorVersion = "0.12.0"
val cxfVersion = "3.4.1"
val jaxwsVersion = "2.3.1"
val jaxwsToolsVersion = "2.3.3"
val apachePoiVersion = "5.0.0"
val influxVersion = "2.21"
val githubPassword: String by project

plugins {
    application
    kotlin("jvm") version "1.5.30"
    id("org.sonarqube") version "3.3"
    id("io.snyk.gradle.plugin.snykplugin") version "0.4"
    id("com.github.ben-manes.versions") version "0.27.0"
    jacoco
}

application {
    mainClass.set("no.nav.helse.sporenstreks.web.AppKt")
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_sporenstreks")
        property("sonar.organization", "navit")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.exclusions", "**/Koin*,**Mock**,**/App**")
    }
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

dependencies {
    // SNYK-fikser - Disse kan fjernes etterhver som våre avhengigheter oppdaterer sine versjoner
    // Forsøk å fjerne en og en og kjør snyk test --configuration-matching=runtimeClasspath
    implementation("commons-collections:commons-collections:3.2.2") // overstyrer transiente 3.2.1
    implementation("io.netty:netty-codec:4.1.59.Final") // overstyrer transiente 4.1.44
    implementation("io.netty:netty-codec-http:4.1.59.Final") // overstyrer transiente 4.1.51.Final
    implementation("io.netty:netty-codec-http2:4.1.59.Final") // overstyrer transiente 4.1.51.Final
    implementation("io.netty:netty-transport-native-epoll:4.1.59.Final")
    implementation("org.glassfish.jersey.media:jersey-media-jaxb:2.31") // overstyrer transiente 2.30.1
    implementation("junit:junit:4.13.1") // overstyrer transiente 4.12
    implementation("org.apache.httpcomponents:httpclient:4.5.13") // overstyrer transiente 4.5.6 via ktor-client-apache
    implementation("com.google.guava:guava:30.0-jre") // overstyrer transiente 29.0-jre
    implementation("org.eclipse.jetty:jetty-server:11.0.7")
    // -- end snyk fixes
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("org.valiktor:valiktor-core:$valiktorVersion")
    implementation("org.valiktor:valiktor-javatime:$valiktorVersion")
    implementation("org.apache.pdfbox:pdfbox:2.0.24")
    implementation("org.apache.poi:poi:$apachePoiVersion")
    implementation("org.apache.poi:poi-ooxml:$apachePoiVersion")
    implementation("javax.xml.ws:jaxws-api:$jaxwsVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion")
    implementation("org.apache.ws.xmlschema:xmlschema-core:2.2.5") // Force newer version of XMLSchema to fix illegal reflective access warning
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }
    implementation("com.sun.activation:javax.activation:1.2.0")
    implementation("io.insert-koin:koin-core-jvm:$koinVersion")
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    implementation("no.nav.security:token-validation-ktor:$tokenSupportVersion")
    implementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")
    implementation("no.nav.common:log:2.2021.11.15_14.58-d7a174cfb6a8")
    implementation(kotlin("stdlib", kotlinVersion))
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("ch.qos.logback.contrib:logback-jackson:$logbackContribVersion")
    implementation("ch.qos.logback.contrib:logback-json-classic:$logbackContribVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:7.0")
    implementation("org.codehaus.janino:janino:3.1.6")
    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:1.2019.09.25-00.21-49b69f0625e0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("org.postgresql:postgresql:42.3.1")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("org.influxdb:influxdb-java:$influxVersion")
    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:2021.06.28-09-42-e08ae")
    testImplementation("io.mockk:mockk:$mockKVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "11"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    mavenCentral()
    google()
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/helse-arbeidsgiver-felles-backend")
    }
}

tasks.jar {
    archiveBaseName.set("app")
    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }
    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<Test>("test") {
    include("no/nav/helse/**")
    exclude("no/nav/helse/slowtests/**")
}

task<Test>("slowTests") {
    include("no/nav/helse/slowtests/**")
    outputs.upToDateWhen { false }
    group = "verification"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

tasks.withType<JacocoReport> {
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude("**/Koin**", "**/App**", "**Mock**")
        }
    )
}

configure<io.snyk.gradle.plugin.SnykExtension> {
    setSeverity("high")
    setAutoDownload(true)
    setAutoUpdate(true)
    setArguments("--all-sub-projects")
}

tasks.withType<Wrapper> {
    gradleVersion = "7.3"
}
