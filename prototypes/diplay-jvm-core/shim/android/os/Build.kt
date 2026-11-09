// Desktop stand-in for android.os.Build. Reports a modern SDK so version gates take
// the newest code path. Our code, not DiPlay's.
package android.os

object Build {
    @JvmField val MANUFACTURER: String = "linux"
    @JvmField val BRAND: String = "linux"
    @JvmField val MODEL: String = System.getProperty("os.name") ?: "linux"
    @JvmField val DEVICE: String = "desktop"
    @JvmField val PRODUCT: String = "desktop-jvm"
    @JvmField val HARDWARE: String = System.getProperty("os.arch") ?: "x86_64"

    object VERSION {
        @JvmField val SDK_INT: Int = 35
        @JvmField val RELEASE: String = "15"
    }

    object VERSION_CODES {
        const val N = 24; const val N_MR1 = 25; const val O = 26; const val O_MR1 = 27
        const val P = 28; const val Q = 29; const val R = 30; const val S = 31
        const val S_V2 = 32; const val TIRAMISU = 33; const val UPSIDE_DOWN_CAKE = 34
        const val VANILLA_ICE_CREAM = 35
    }
}
