// Desktop stand-in for android.util.Log: forwards to stderr. Our code, not DiPlay's.
package android.util

object Log {
    const val VERBOSE = 2; const val DEBUG = 3; const val INFO = 4; const val WARN = 5; const val ERROR = 6
    private fun out(level: String, tag: String?, msg: String?, tr: Throwable? = null): Int {
        System.err.println("$level/$tag: $msg")
        tr?.printStackTrace()
        return 0
    }
    @JvmStatic fun v(tag: String?, msg: String): Int = out("V", tag, msg)
    @JvmStatic fun d(tag: String?, msg: String): Int = out("D", tag, msg)
    @JvmStatic fun i(tag: String?, msg: String): Int = out("I", tag, msg)
    @JvmStatic fun w(tag: String?, msg: String): Int = out("W", tag, msg)
    @JvmStatic fun e(tag: String?, msg: String): Int = out("E", tag, msg)
    @JvmStatic fun v(tag: String?, msg: String?, tr: Throwable?): Int = out("V", tag, msg, tr)
    @JvmStatic fun d(tag: String?, msg: String?, tr: Throwable?): Int = out("D", tag, msg, tr)
    @JvmStatic fun i(tag: String?, msg: String?, tr: Throwable?): Int = out("I", tag, msg, tr)
    @JvmStatic fun w(tag: String?, msg: String?, tr: Throwable?): Int = out("W", tag, msg, tr)
    @JvmStatic fun w(tag: String?, tr: Throwable?): Int = out("W", tag, tr?.toString(), tr)
    @JvmStatic fun e(tag: String?, msg: String?, tr: Throwable?): Int = out("E", tag, msg, tr)
    @JvmStatic fun isLoggable(tag: String?, level: Int): Boolean = level >= INFO
}
