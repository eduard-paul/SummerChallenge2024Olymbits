import java.util.Scanner;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class DivingPlayer {

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
            String gpu = "";
            for (int i = 0; i < nbGames; i++) {
                gpu = in.next();
                int reg0 = in.nextInt();
                int reg1 = in.nextInt();
                int reg2 = in.nextInt();
                int reg3 = in.nextInt();
                int reg4 = in.nextInt();
                int reg5 = in.nextInt();
                int reg6 = in.nextInt();


            }
            in.nextLine();

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            String res = switch (gpu.charAt(0)) {
                case 'U' -> "up";
                case 'R' -> "right";
                case 'L' -> "left";
                default -> "down";
            };
            System.out.println(res);
        }
    }
}