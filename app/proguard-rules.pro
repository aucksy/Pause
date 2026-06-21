# Pause — release ProGuard/R8 rules.
#
# The app is small and uses Hilt (codegen) + Compose + DataStore, all of which ship
# their own consumer rules. We add only the project-specific keeps below.

# Keep the AccessibilityService entry point — it is referenced by the framework via the
# manifest by name and must survive shrinking/renaming.
-keep class com.pause.app.service.PauseAccessibilityService { *; }

# Keep Kotlin metadata for reflection-free coroutines/serialization edge cases.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes Signature, InnerClasses, EnclosingMethod
