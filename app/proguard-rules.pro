# app/proguard-rules.pro

# Protege solo los miembros serializables de los modelos de datos
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Mantiene el método serializer generado por el compilador
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Mantiene la integridad de los modelos en el módulo compartido
-keep,allowobfuscation class com.neurogenesis.shared_network.models.** { *; }

# Conserva anotaciones necesarias para la infraestructura de Ktor y Koin
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault, Signature
-keepclassmembers class * {
    @org.koin.core.annotation.KoinInternalApi <methods>;
}