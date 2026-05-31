# Keep serialized state class layouts stable enough for Android save-state files.
-keep class dev.vitorsilverio.gbcemu.** implements java.io.Serializable { *; }

# The Android app only depends on the slf4j API. Some optional bindings are absent by design.
-dontwarn org.slf4j.**
