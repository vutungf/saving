# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class vn.sotietkiem.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class vn.sotietkiem.**$$serializer { *; }

# Credential Manager (đăng nhập Google)
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
