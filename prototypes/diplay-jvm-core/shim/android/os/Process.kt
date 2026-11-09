// Desktop stand-in for android.os.Process: thread priorities are a no-op on the JVM.
package android.os

object Process {
    const val THREAD_PRIORITY_DEFAULT = 0
    const val THREAD_PRIORITY_AUDIO = -16
    const val THREAD_PRIORITY_URGENT_AUDIO = -19
    const val THREAD_PRIORITY_DISPLAY = -4
    @JvmStatic fun setThreadPriority(priority: Int) {}
    @JvmStatic fun setThreadPriority(tid: Int, priority: Int) {}
    @JvmStatic fun myPid(): Int = ProcessHandle.current().pid().toInt()
    @JvmStatic fun myTid(): Int = Thread.currentThread().id.toInt()
}
