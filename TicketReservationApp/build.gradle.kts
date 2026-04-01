// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("org.sonarqube") version "7.2.3.7755"
     id("com.google.gms.google-services") version "4.4.4" apply false
}

sonar {
  properties {
    property ("sonar.projectKey", "Extinctable_soen-345")
    property ("sonar.organization", "extinctable")
  }
}
