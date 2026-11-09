// Desktop stand-in for androidx.annotation.RequiresApi (no API-level gating off Android).
package androidx.annotation
@Retention(AnnotationRetention.SOURCE)
annotation class RequiresApi(val value: Int = -1, val api: Int = -1)
