package ngo.xnet.aiope.core.terminal.backend;

import android.util.Log;
import java.lang.reflect.Method;

/**
 * JNI bridge — delegates to com.termux.terminal.JNI via reflection
 * since that class is package-private.
 *
 * Graceful init: if the native library or reflection fails, the bridge
 * marks itself unavailable instead of crashing the process. Consumer
 * code should check {@link #isAvailable()} before invoking any method.
 */
final class JNI {
    private static final String TAG = "TerminalJNI";

    private static final Method sCreateSubprocess;
    private static final Method sSetPtyWindowSize;
    private static final Method sWaitFor;
    private static final Method sClose;

    private static volatile boolean sAvailable = false;

    static {
        Method create = null, setSize = null, wait = null, close = null;
        try {
            Class<?> c = Class.forName("com.termux.terminal.JNI");
            create  = c.getDeclaredMethod("createSubprocess", String.class, String.class, String[].class, String[].class, int[].class, int.class, int.class);
            setSize = c.getDeclaredMethod("setPtyWindowSize", int.class, int.class, int.class);
            wait    = c.getDeclaredMethod("waitFor", int.class);
            close   = c.getDeclaredMethod("close", int.class);
            create.setAccessible(true);
            setSize.setAccessible(true);
            wait.setAccessible(true);
            close.setAccessible(true);
            sAvailable = true;
        } catch (Exception e) {
            Log.e(TAG, "JNI bridge unavailable — terminal will not function", e);
        }
        sCreateSubprocess   = create;
        sSetPtyWindowSize   = setSize;
        sWaitFor            = wait;
        sClose              = close;
    }

    /** Returns {@code true} if the native PTY bridge was loaded successfully. */
    static boolean isAvailable() { return sAvailable; }

    static int createSubprocess(String cmd, String cwd, String[] args, String[] envVars, int[] processId, int rows, int columns) {
        try { return (int) sCreateSubprocess.invoke(null, cmd, cwd, args, envVars, processId, rows, columns); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    static void setPtyWindowSize(int fd, int rows, int cols) {
        try { sSetPtyWindowSize.invoke(null, fd, rows, cols); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    static int waitFor(int processId) {
        try { return (int) sWaitFor.invoke(null, processId); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    static void close(int fileDescriptor) {
        try { sClose.invoke(null, fileDescriptor); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
