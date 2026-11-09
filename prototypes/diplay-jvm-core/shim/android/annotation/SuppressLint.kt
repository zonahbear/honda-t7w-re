// Desktop stand-in for android.annotation.SuppressLint (Android lint has no desktop meaning).
package android.annotation
@Retention(AnnotationRetention.SOURCE)
annotation class SuppressLint(vararg val value: String)
