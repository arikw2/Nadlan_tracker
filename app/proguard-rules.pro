# Keep kotlinx.serialization serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class il.arik.nadlantracker.**$$serializer { *; }
-keepclassmembers class il.arik.nadlantracker.** {
    *** Companion;
}
-keepclasseswithmembers class il.arik.nadlantracker.** {
    kotlinx.serialization.KSerializer serializer(...);
}
