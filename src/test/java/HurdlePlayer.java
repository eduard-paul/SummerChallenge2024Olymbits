import java.util.*;
        import java.io.*;
        import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class HurdlePlayer {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int playerIdx = in.nextInt();
        int nbGames = in.nextInt();
        if (in.hasNextLine()) {
            in.nextLine();
        }

        // game loop
        while (true) {
            int[] r = new int[] {0,0,0,0};
            for (int i = 0; i < 3; i++) {
                String scoreInfo = in.nextLine();
            }
            for (int i = 0; i < nbGames; i++) {
                String gpu = in.next();
                int reg0 = in.nextInt();
                int reg1 = in.nextInt();
                int reg2 = in.nextInt();
                int reg3 = in.nextInt();
                int reg4 = in.nextInt();
                int reg5 = in.nextInt();
                int reg6 = in.nextInt();

                int reg = reg0;
                if (playerIdx == 0) {
                    reg = reg0;
                } else if (playerIdx == 1) {
                    reg = reg1;
                } else {
                    reg = reg2;
                }

                if (i == 0) {
                    if (reg < gpu.length() - 1 && gpu.charAt(reg + 1) == '#') {
                        r[0]++;
                    } else if (reg < gpu.length() - 2 && gpu.charAt(reg + 2) == '#') {
                        r[1]++;
                    } else if (reg < gpu.length() - 3 && gpu.charAt(reg + 3) == '#') {
                        r[2]++;
                    } else {
                        r[3]++;
                    }
                }
            }
            in.nextLine();

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            if (r[0] > 0) {
                System.out.println("up");
            } else if(r[1] > 0) {
                System.out.println("left");
            } else if(r[2] > 0) {
                System.out.println("down");
            } else if(r[3] > 0) {
                System.out.println("right");
            }
        }
    }
}