package context;

public class AIContext {

    // AI variables
    public static double aiTargetY = 250;
    public static double aiCurrentVelocity = 0.0;
    public static double aiPaddleY = 250.0; // Smooth AI paddle position
    public static int aiDifficulty = 3; // 1-5, higher = harder
    public static double aiMaxSpeed = 4.0;
    public static double aiAcceleration = 0.4;
    public static double aiDeceleration = 0.85;
    public static long lastAIUpdate = 0;
    public static long lastBallDirectionChange = 0;
    public static double aiReactionDelay = 0.2; // seconds

}
