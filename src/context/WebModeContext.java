package context;

/**
 * Context for detecting if the game is running in web mode (via CheerpJ)
 * Use this to disable features that don't work well in the browser
 */
public class WebModeContext {

    private static final String WEB_MODE_PROPERTY = "pongping.webmode";

    /**
     * Check if the game is running in web mode (via CheerpJ)
     * @return true if running in browser, false if running as desktop app
     */
    public static boolean isWebMode() {
        return "true".equals(System.getProperty(WEB_MODE_PROPERTY));
    }

    /**
     * Check if the game is running as a desktop application
     * @return true if running as desktop app, false if in browser
     */
    public static boolean isDesktopMode() {
        return !isWebMode();
    }

    /**
     * Print debug information about the current mode
     */
    public static void printModeInfo() {
        if (isWebMode()) {
            System.out.println("[WebMode] Running in browser via CheerpJ");
            System.out.println("[WebMode] Some features may be disabled for web compatibility");
        } else {
            System.out.println("[DesktopMode] Running as native desktop application");
        }
    }
}
