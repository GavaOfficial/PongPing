package context;

import java.awt.*;

public class RankContext {

    public static String currentRank = "NOVICE";
    public static Color rankColor = Color.WHITE;

    // Ranking system for single player
    public static String finalRank = "";
    public static boolean showRankScreen = false;
    public static int rankAnimationFrame = 0;

    // Rank screen animation phases
    public static boolean rankPaddleTransitionComplete = false;
    public static boolean rankTextTransitionStarted = false;
    public static double rankPaddleProgress = 0.0; // 0.0 = posizione gioco, 1.0 = posizione rank
    public static double rankTextProgress = 0.0; // 0.0 = fuori schermo destra, 1.0 = posizione finale

}
