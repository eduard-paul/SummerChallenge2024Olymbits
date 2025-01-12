import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;

class Player {
    public static class MyLog {

        private static final MyLog INSTANCE = new MyLog();

        public static boolean DEBUG = true;

        public final Map<String, Long> timeitMap = new LinkedHashMap<String, Long>();
        public final Map<String, Long> timeitMapSystem = new LinkedHashMap<String, Long>();
        public final ThreadMXBean mf = ManagementFactory.getThreadMXBean();

        public Scanner in;
        public PrintStream out;
        public PrintStream err;

        private MyLog() {
        }

        public static MyLog log() {
            return INSTANCE;
        }

        public static void out(String s) {
            INSTANCE.out.println(s);
        }

        public static void err(String s) {
            INSTANCE.err.println(s);
        }

        public static void timeit(String name) {
            INSTANCE._timeit(name);
        }

        public void _timeit(String name) {
            if (DEBUG) {
                long time;
                if (timeitMap.containsKey(name)) {
                    time = mf.getCurrentThreadCpuTime() - timeitMap.remove(name);
                    if (time > 1000000L) {
                        MyLog.log().err.println(name + " took cpu " + time / (long)1000000 + "ms");
                    }
                } else {
                    timeitMap.put(name, mf.getCurrentThreadCpuTime());
                }

                if (timeitMapSystem.containsKey(name)) {
                    time = System.nanoTime() - timeitMapSystem.remove(name);
                    if (time > 1000000L) {
                        MyLog.log().err.println(name + " took " + time / (long)1000000 + "ms");
                    }
                } else {
                    timeitMapSystem.put(name, System.nanoTime());
                }
            }

        }
    }

    public static int STEP = 0;
    public static long time = 0;

    public static void main(String args[]) {
        MyLog.log().in = new Scanner(System.in);
        MyLog.log().out = System.out;
        MyLog.log().err = System.err;

        int playerIdx =  MyLog.log().in.nextInt();
        int nbGames =  MyLog.log().in.nextInt();
        if (MyLog.log().in.hasNextLine()) {
            MyLog.log().in.nextLine();
        }

        Strategy strategy = new Strategy();

        while (true) {
            GameState gameState = GameState.read(MyLog.log().in, playerIdx);
            String s = strategy.onTick(gameState);
            MyLog.timeit("step");
            MyLog.out(s);
            MyLog.err("result: " + s);
//            System.gc();
        }
    }

    public static class MonteCarloTreeSearch {
        private static final int MAX_ITERATIONS = 100; // Number of iterations
        private static final double EXPLORATION_PARAM = 1.4;

        public static class Node {
            public GameState state;
            public float score;
            public int simulationsCount;
            public List<Node> children = new ArrayList<>();
            public List<Integer> childrenActions = new ArrayList<>();
            public Node parent;
            public int parentAction;
            public double maxScore;
            public double minScore = Integer.MAX_VALUE;
            public boolean enemyTurn;

            public Node(GameState gameState) {
                state = gameState;
            }

            public boolean isFullyExpanded() {
                return children.size() == 4;
            }

            public Node finalBestChild() {
                return children.stream()
                        .max(Comparator.comparingDouble(c -> c.score / c.simulationsCount))
                        .orElse(null);
            }

            public Node bestChild() {
                return children.stream()
                        .max((c1, c2) -> Double.compare(ucbValue(c1), ucbValue(c2)))
                        .orElse(null);
            }

            private double ucbValue(Node child) {
                if (child.simulationsCount == 0) {
                    return Double.POSITIVE_INFINITY;
                }
                double exploitation;
                if (enemyTurn) {
                    exploitation = (maxScore - (double) child.score / child.simulationsCount) / ((maxScore - minScore));
                } else {
                    exploitation = ((double) child.score / child.simulationsCount - minScore) / ((maxScore - minScore));
                }
                double exploration = EXPLORATION_PARAM * Math.sqrt(
                        Math.log(this.simulationsCount) / child.simulationsCount);
                return exploitation + exploration;
            }
        }

        public int run(GameState initialState) {
            MyLog.timeit("run");
            Node root = new Node(initialState);

            int i = 0;
            while (true) {
                i++;
                Node selectedNode = selection(root);
                Node expandedNode = expansion(selectedNode);
                float result = simulation(expandedNode);
                backpropagation(expandedNode, result);
                if (expandedNode.state.isGameOver() || i % 10 == 0 && System.nanoTime() - time >= 40_000_000L) {
                    MyLog.err("Needed iterations: " + i);
                    break;
                }
            }

//            Node node = root;
//            List<Node> path = new ArrayList<>();
//            while(node != null) {
//                path.add(node);
//                node = node.finalBestChild();
//            }
            Node bestChild = root.finalBestChild();
            MyLog.timeit("run");
            return bestChild == null ? 0 : bestChild.parentAction; // Return the best move
        }

        private Node selection(Node node) {
            while (!node.children.isEmpty() && node.isFullyExpanded()) {
                node = node.bestChild();
            }
            return node;
        }

        private Node expansion(Node node) {
            if (node.state.isGameOver()) {
                return node; // Terminal state, no expansion needed
            }

            for (int i = 0; i < 4; i++) {
                GameState childState;
                if (node.enemyTurn) {
                    childState = (new Action(-1, i, i)).applyEnemy(node.state);
                } else {
                    childState = (new Action(i, -1, -1)).applyPlayer(node.state);
                }
                boolean alreadyExpanded = false;
                for (int childAction: node.childrenActions) {
                    if (childAction == i) {
                        alreadyExpanded = true;
                        break;
                    }
                }
                if (!alreadyExpanded) {
                    Node childNode = new Node(childState);
                    childNode.parent = node;
                    childNode.parentAction = i;
                    childNode.enemyTurn = !node.enemyTurn;
                    node.children.add(childNode);
                    node.childrenActions.add(i);
                    return childNode; // Return the newly expanded node
                }
            }
            return node; // All children already expanded
        }

        public static float simulation(Node node) {
            GameState state = node.state;
            GameState newState;
            if (node.enemyTurn) {
                Action enemyAction = new Action(0, PerfectStrategy.getRandomAction(), PerfectStrategy.getRandomAction());
                newState = enemyAction.applyEnemy(state);
            } else {
                newState = state;
            }

            return simulationInternal(newState);
        }

        private static float simulationInternal(GameState state) {
            while (!state.isGameOver()) {
                int action1 = PerfectStrategy.getRandomAction();
                int action2 = PerfectStrategy.getRandomAction();
                int action = PerfectStrategy.getRandomAction();
                state = (new Action(action, action1, action2)).apply(state);
            }
            return (state.scores[state.playerId].getGameScore(0) + state.games[0].getScore())
                    * (state.scores[state.playerId].getGameScore(1) + state.games[1].getScore())
                    * (state.scores[state.playerId].getGameScore(2) + state.games[2].getScore())
                    * (state.scores[state.playerId].getGameScore(3) + state.games[3].getScore())
                    + (state.games[0].getScore()
                    + state.games[1].getScore()
                    + state.games[2].getScore()
                    + state.games[3].getScore()) / 9f;
        }

        private void backpropagation(Node node, float result) {
            while (node != null) {
                node.simulationsCount++;
                node.score += result;
                if (result > node.maxScore) {
                    node.maxScore = result;
                }
                if (result < node.minScore) {
                    node.minScore = result;
                }
                node = node.parent;
            }
        }

    }

    public static class PerfectStrategy {
        private static int q = 0;

        public static int getRandomAction() {
            return q++ % 4;
        }

        public static int getPerfectAction(DivingState state) {
            return state.goals[0];
        }

        public static int getPerfectAction(HurdleRaceState state, int idx) {
            int[] r = new int[4];

            int pos = state.positions[idx];

            if (pos < state.track.length - 1 && state.track[pos + 1] == HurdleRaceState.HURDLE) {
                r[0]++;
            } else if (pos < state.track.length - 2 && state.track[pos + 2] == HurdleRaceState.HURDLE) {
                r[1]++;
            } else if (pos < state.track.length - 3 && state.track[pos + 3] == HurdleRaceState.HURDLE) {
                r[2]++;
            } else {
                r[3]++;
            }

            if (r[0] > 0) {
                return Action.UP;
            } else if(r[1] > 0) {
                return Action.LEFT;
            } else if(r[2] > 0) {
                return Action.DOWN;
            } else if(r[3] > 0) {
                return Action.RIGHT;
            }
            return Action.RIGHT;
        }
    }

    public static class Action {
        public static final String[] ACTIONS = new String[] {"up", "right", "down", "left"};
        public static final int UP = 0;
        public static final int RIGHT = 1;
        public static final int DOWN = 2;
        public static final int LEFT = 3;

        public int[] actions = new int[3];

        public Action(int playerAction, int enemyAction1, int enemyAction2) {
            actions[0] = playerAction;
            actions[1] = enemyAction1;
            actions[2] = enemyAction2;
        }

        public GameState apply(GameState state) {
            GameState newState = new GameState(state.playerId);
            newState.games[0] = state.games[0].next(this);
            newState.games[1] = state.games[1].next(this);
            newState.games[2] = state.games[2].next(this);
            newState.games[3] = state.games[3].next(this);
            newState.scores = state.scores;
            return newState;
        }

        public GameState applyPlayer(GameState state) {
            GameState newState = new GameState(state.playerId);
            newState.games[0] = state.games[0].playerNext(this);
            newState.games[1] = state.games[1].playerNext(this);
            newState.games[2] = state.games[2].playerNext(this);
            newState.games[3] = state.games[3].playerNext(this);
            newState.scores = state.scores;
            return newState;
        }

        public GameState applyEnemy(GameState state) {
            GameState newState = new GameState(state.playerId);
            newState.games[0] = state.games[0].enemyNext(this);
            newState.games[1] = state.games[1].enemyNext(this);
            newState.games[2] = state.games[2].enemyNext(this);
            newState.games[3] = state.games[3].enemyNext(this);
            newState.scores = state.scores;
            return newState;
        }

        public ArcheryState apply(ArcheryState state) {
            return state;
        }

        public DivingState apply(DivingState state) {
            return state;
        }

        public RollersState apply(RollersState state) {
            return state;
        }
    }

    public static class Strategy {
        public String onTick(GameState state) {
            STEP++;
            MonteCarloTreeSearch mcts = new MonteCarloTreeSearch();
            int result = mcts.run(state);
            return Action.ACTIONS[result];
        }
    }

    public static class GameState {
        public MiniGameState[] games = new MiniGameState[4];
        public Score[] scores = new Score[3];
        public int playerId;
        public int enemyId1;
        public int enemyId2;

        public GameState(int id) {
            playerId = id;
            switch (playerId) {
                case 0:
                    enemyId1 = 1;
                    enemyId2 = 2;
                    break;
                case 1:
                    enemyId1 = 0;
                    enemyId2 = 2;
                    break;
                default:
                    enemyId1 = 0;
                    enemyId2 = 1;
                    break;
            }
        }

        public boolean isGameOver() {
            return games[0].isFinished() && games[1].isFinished() && games[2].isFinished() && games[3].isFinished();
        }

        public static GameState read(Scanner in, int id) {
            GameState gameState = new GameState(id);
            for (int i = 0; i < 3; i++) {
                gameState.scores[i] = Score.read(in);
                if (i == 0) {
                    MyLog.timeit("step");
                    time = System.nanoTime();
                }
            }
            for (int i = 0; i < 4; i++) {
                gameState.games[i] = MiniGameState.read(in, id, i);
                MyLog.err("Game " + (i + 1));
                MyLog.err(gameState.games[i].toString());
            }

            return gameState;
        }

        public static class Score {
            int finalScore;
            int[][] medals = new int[4][3];

            public Score(int fs, int g1, int s1, int b1, int g2, int s2, int b2, int g3, int s3, int b3, int g4, int s4, int b4) {
                finalScore = fs;
                medals[0][0] = g1;
                medals[0][1] = s1;
                medals[0][2] = b1;
                medals[1][0] = g2;
                medals[1][1] = s2;
                medals[1][2] = b2;
                medals[2][0] = g3;
                medals[2][1] = s3;
                medals[2][2] = b3;
                medals[3][0] = g4;
                medals[3][1] = s4;
                medals[3][2] = b4;
            }

            public int getGameScore(int gameIdx) {
                return medals[gameIdx][0] * 3 + medals[gameIdx][1];
            }

            public static Score read(Scanner in) {
                return new Score(in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt(), in.nextInt());
            }
        }
    }

    public static class HurdleRaceState extends MiniGameState {
        public static final char EMPTY = '.';
        public static final char HURDLE = '#';

        public int[] positions = new int[3];
        public int[] stunTimers = new int[3];
        public char[] track;

        public HurdleRaceState(int playerId, String gpu, int reg0, int reg1, int reg2, int reg3, int reg4, int reg5, int reg6) {
            this(playerId, gpu.toCharArray(), new int[] {reg0, reg1, reg2}, new int[]{reg3, reg4, reg5});
        }

        public HurdleRaceState(int playerId, char[] gpu, int[] pos, int[] reg3) {
            super(playerId);
            positions[0] = pos[0];
            positions[1] = pos[1];
            positions[2] = pos[2];
            stunTimers[0] = reg3[0];
            stunTimers[1] = reg3[1];
            stunTimers[2] = reg3[2];
            track = gpu;
        }

        @Override
        public int stepsLeft() {
            return isFinished() ? Integer.MAX_VALUE : (30 - Math.max(Math.max(positions[0], positions[1]), positions[2])) / 2;
        }

        public boolean isFinished() {
            return positions[0] >= track.length - 1 || positions[1] >= track.length - 1 || positions[2] >= track.length - 1;
        }

        public int getScore() {
            if (positions[playerId] >= positions[enemyId1] && positions[playerId] >= positions[enemyId2]) {
                return 3;
            } else if (positions[playerId] >= positions[enemyId1] || positions[playerId] >= positions[enemyId2]) {
                return 1;
            }
            return 0;
        }

        @Override
        public HurdleRaceState next(Action action) {
            if (!isFinished()) {
                HurdleRaceState newState = new HurdleRaceState(playerId, track, positions, stunTimers);
                applyPlayerAction(newState, newState.playerId, action.actions[0]);
                applyPlayerAction(newState, newState.enemyId1, action.actions[1]);
                applyPlayerAction(newState, newState.enemyId2, action.actions[2]);
                return newState;
            }
            return this;
        }

        @Override
        public MiniGameState playerNext(Action action) {
            if (!isFinished()) {
                HurdleRaceState newState = new HurdleRaceState(playerId, track, positions, stunTimers);
                applyPlayerAction(newState, newState.playerId, action.actions[0]);
                return newState;
            }
            return this;
        }

        @Override
        public MiniGameState enemyNext(Action action) {
            if (!isFinished()) {
                HurdleRaceState newState = new HurdleRaceState(playerId, track, positions, stunTimers);
                applyPlayerAction(newState, newState.enemyId1, action.actions[1]);
                applyPlayerAction(newState, newState.enemyId2, action.actions[2]);
                return newState;
            }
            return this;
        }

        private void applyPlayerAction(HurdleRaceState state, int id, int action) {
            if (state.stunTimers[id] > 0) {
                state.stunTimers[id]--;
                return;
            }
            switch (action) {
                case Action.UP:
                    state.positions[id] += 2;
                    if (state.positions[id] < state.track.length && state.track[state.positions[id]] == HurdleRaceState.HURDLE) {
                        state.stunTimers[id] = 3;
                    }
                    break;
                case Action.RIGHT:
                    state.positions[id]++;
                    if (state.positions[id] < state.track.length && state.track[state.positions[id]] == HurdleRaceState.HURDLE) {
                        state.stunTimers[id] = 3;
                        break;
                    }
                case Action.DOWN:
                    state.positions[id]++;
                    if (state.positions[id] < state.track.length && state.track[state.positions[id]] == HurdleRaceState.HURDLE) {
                        state.stunTimers[id] = 3;
                        break;
                    }
                default:
                    state.positions[id]++;
                    if (state.positions[id] < state.track.length && state.track[state.positions[id]] == HurdleRaceState.HURDLE) {
                        state.stunTimers[id] = 3;
                        break;
                    }
            }
        }
    }

    public static class RollersState extends MiniGameState {
        public int[] spaces = new int[3];
        public int[] risks = new int[3];
        public int turnsLeft;
        public String riskOrder;

        public RollersState(int playerId, String gpu, int reg0, int reg1, int reg2, int reg3, int reg4, int reg5, int reg6) {
            super(playerId);
            spaces[0] = reg0;
            spaces[1] = reg1;
            spaces[2] = reg2;
            risks[0] = reg3;
            risks[1] = reg4;
            risks[2] = reg5;
            turnsLeft = reg6;
            riskOrder = gpu;
        }
    }

    public static class DivingState extends MiniGameState {
        public int[] points = new int[3];
        public int[] combo = new int[3];
        public int[] goals;
        private int pos;

        public DivingState(int playerId, int[] goals, int[] points, int[] combo, int pos) {
            super(playerId);
            this.goals = goals;
            this.points = points;
            this.combo = combo;
            this.pos = pos;
        }

        public DivingState(int playerId, String gpu, int reg0, int reg1, int reg2, int reg3, int reg4, int reg5, int reg6) {
            super(playerId);
            points[0] = reg0;
            points[1] = reg1;
            points[2] = reg2;
            combo[0] = reg3;
            combo[1] = reg4;
            combo[2] = reg5;
            goals = new int[gpu.length()];
            for (int i = 0; i < gpu.length(); i++) {
                goals[i] = switch (gpu.charAt(i)) {
                    case 'U' -> 0;
                    case 'R' -> 1;
                    case 'D' -> 2;
                    default -> 3;
                };
            }
        }

        @Override
        public int stepsLeft() {
            return isFinished() ? Integer.MAX_VALUE : goals.length - pos;
        }

        @Override
        public boolean isFinished() {
            return pos == goals.length;
        }

        @Override
        public int getScore() {
            if (points[playerId] >= points[enemyId1] && points[playerId] >= points[enemyId2]) {
                return 3;
            } else if (points[playerId] >= points[enemyId1] || points[playerId] >= points[enemyId2]) {
                return 1;
            }
            return 0;
        }

        @Override
        public MiniGameState next(Action action) {
            if (isFinished()) return this;

            int[] newPoints = new int[3];
            int[] newCombo = new int[3];
            newCombo[playerId] = action.actions[0] == goals[pos] ? combo[playerId] + 1 : 0;
            newCombo[enemyId1] = action.actions[1] == goals[pos] ? combo[enemyId1] + 1 : 0;
            newCombo[enemyId2] = action.actions[2] == goals[pos] ? combo[enemyId2] + 1 : 0;

            newPoints[playerId] = points[playerId] + newCombo[playerId];
            newPoints[enemyId1] = points[enemyId1] + newCombo[enemyId1];
            newPoints[enemyId2] = points[enemyId2] + newCombo[enemyId2];

            return new DivingState(playerId, goals, newPoints, newCombo, pos + 1);
        }

        @Override
        public MiniGameState playerNext(Action action) {
            if (isFinished()) return this;

            int[] newPoints = new int[3];
            int[] newCombo = new int[3];
            newCombo[playerId] = action.actions[0] == goals[pos] ? combo[playerId] + 1 : 0;
            newCombo[enemyId1] = combo[enemyId1];
            newCombo[enemyId2] = combo[enemyId2];

            newPoints[playerId] = points[playerId] + newCombo[playerId];
            newPoints[enemyId1] = points[enemyId1];
            newPoints[enemyId2] = points[enemyId2];

            return new DivingState(playerId, goals, newPoints, newCombo, pos);
        }

        @Override
        public MiniGameState enemyNext(Action action) {
            if (isFinished()) return this;

            int[] newPoints = new int[3];
            int[] newCombo = new int[3];
            newCombo[playerId] = combo[playerId];
            newCombo[enemyId1] = action.actions[1] == goals[pos] ? combo[enemyId1] + 1 : 0;
            newCombo[enemyId2] = action.actions[2] == goals[pos] ? combo[enemyId2] + 1 : 0;

            newPoints[playerId] = points[playerId];
            newPoints[enemyId1] = points[enemyId1] + newCombo[enemyId1];
            newPoints[enemyId2] = points[enemyId2] + newCombo[enemyId2];

            return new DivingState(playerId, goals, newPoints, newCombo, pos + 1);
        }
    }

    public static class ArcheryState extends MiniGameState {
        private static int[][] actionMap = new int[][] {new int[]{0, -1}, new int[]{1, 0}, new int[]{0, 1}, new int[]{-1, 0}};
        public int[][] positions = new int[3][2];
        public int[] wind;
        public int pos;

        public ArcheryState(int playerId, int[] wind, int[][] positions, int pos) {
            super(playerId);
            this.playerId = playerId;
            this.wind = wind;
            this.positions[0][0] = positions[0][0];
            this.positions[0][1] = positions[0][1];
            this.positions[1][0] = positions[1][0];
            this.positions[1][1] = positions[1][1];
            this.positions[2][0] = positions[2][0];
            this.positions[2][1] = positions[2][1];
            this.pos = pos;
        }

        public ArcheryState(int playerId, String gpu, int reg0, int reg1, int reg2, int reg3, int reg4, int reg5, int reg6) {
            super(playerId);
            positions[0][0] = reg0;
            positions[0][1] = reg1;
            positions[1][0] = reg2;
            positions[1][1] = reg3;
            positions[2][0] = reg4;
            positions[2][1] = reg5;
            wind = new int[gpu.length()];
            for (int i = 0; i < gpu.length(); i++) {
                wind[i] = gpu.charAt(i) - '0';
            }
        }

        @Override
        public boolean isFinished() {
            return wind.length == pos;
        }

        @Override
        public int getScore() {
            double playerDist = Math.sqrt(positions[playerId][0] * positions[playerId][0] + positions[playerId][1]*positions[playerId][1]);
            double enemyDist1 = Math.sqrt(positions[enemyId1][0] * positions[enemyId1][0] + positions[enemyId1][1]*positions[enemyId1][1]);
            double enemyDist2 = Math.sqrt(positions[enemyId2][0] * positions[enemyId2][0] + positions[enemyId2][1]*positions[enemyId2][1]);
            if (playerDist <= enemyDist1 && playerDist <= enemyDist2) {
                return 3;
            } else if (playerDist <= enemyDist1 || playerDist <= enemyDist2) {
                return 1;
            }
            return 0;
        }

        @Override
        public MiniGameState next(Action action) {
            if (!isFinished()) {
                int[][] newPositions = new int[3][2];
                newPositions[playerId][0] = Math.clamp(this.positions[playerId][0] + (long) actionMap[action.actions[0]][0] * wind[pos], -20, 20);
                newPositions[playerId][1] = Math.clamp(this.positions[playerId][1] + (long) actionMap[action.actions[0]][1] * wind[pos], -20, 20);
                newPositions[enemyId1][0] = Math.clamp(this.positions[enemyId1][0] + (long) actionMap[action.actions[1]][0] * wind[pos], -20, 20);
                newPositions[enemyId1][1] = Math.clamp(this.positions[enemyId1][1] + (long) actionMap[action.actions[1]][1] * wind[pos], -20, 20);
                newPositions[enemyId2][0] = Math.clamp(this.positions[enemyId2][0] + (long) actionMap[action.actions[2]][0] * wind[pos], -20, 20);
                newPositions[enemyId2][1] = Math.clamp(this.positions[enemyId2][1] + (long) actionMap[action.actions[2]][1] * wind[pos], -20, 20);
                return new ArcheryState(playerId, wind, newPositions, pos + 1);
            }
            return this;
        }

        @Override
        public int stepsLeft() {
            return wind.length - pos;
        }

        @Override
        public MiniGameState playerNext(Action action) {
            if (!isFinished()) {
                int[][] newPositions = new int[3][2];
                newPositions[playerId][0] = Math.clamp(this.positions[playerId][0] + (long) actionMap[action.actions[0]][0] * wind[pos], -20, 20);
                newPositions[playerId][1] = Math.clamp(this.positions[playerId][1] + (long) actionMap[action.actions[0]][1] * wind[pos], -20, 20);
                newPositions[enemyId1][0] = this.positions[enemyId1][0];
                newPositions[enemyId1][1] = this.positions[enemyId1][1];
                newPositions[enemyId2][0] = this.positions[enemyId2][0];
                newPositions[enemyId2][1] = this.positions[enemyId2][1];
                return new ArcheryState(playerId, wind, newPositions, pos);
            }
            return this;
        }

        @Override
        public MiniGameState enemyNext(Action action) {
            if (!isFinished()) {
                int[][] newPositions = new int[3][2];
                newPositions[playerId][0] = this.positions[playerId][0];
                newPositions[playerId][1] = this.positions[playerId][1];
                newPositions[enemyId1][0] = Math.clamp(this.positions[enemyId1][0] + (long) actionMap[action.actions[1]][0] * wind[pos], -20, 20);
                newPositions[enemyId1][1] = Math.clamp(this.positions[enemyId1][1] + (long) actionMap[action.actions[1]][1] * wind[pos], -20, 20);
                newPositions[enemyId2][0] = Math.clamp(this.positions[enemyId2][0] + (long) actionMap[action.actions[2]][0] * wind[pos], -20, 20);
                newPositions[enemyId2][1] = Math.clamp(this.positions[enemyId2][1] + (long) actionMap[action.actions[2]][1] * wind[pos], -20, 20);
                return new ArcheryState(playerId, wind, newPositions, pos + 1);
            }
            return this;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(positions[0][0]).append(", ");
            sb.append(positions[0][1]).append(" ; ");
            sb.append(positions[1][0]).append(", ");;
            sb.append(positions[1][1]).append(" ; ");;
            sb.append(positions[2][0]).append(", ");;
            sb.append(positions[2][1]).append(" ; ");;
            for (int j : wind) {
                sb.append(j);
            }
            return sb.toString();
        }
    }

    public static class MiniGameState {
        public int playerId;
        public int enemyId1;
        public int enemyId2;

        public MiniGameState(int playerId) {
            this.playerId = playerId;
            switch (playerId) {
                case 0:
                    enemyId1 = 1;
                    enemyId2 = 2;
                    break;
                case 1:
                    enemyId1 = 0;
                    enemyId2 = 2;
                    break;
                default:
                    enemyId1 = 0;
                    enemyId2 = 1;
                    break;
            }
        }

        public int stepsLeft() {return Integer.MAX_VALUE;}

        public boolean isFinished() {return true;}

        public int getScore() {return 0;}

        public MiniGameState next(Action action) {return this;}
        public MiniGameState playerNext(Action action) {return this;}
        public MiniGameState enemyNext(Action action) {return this;}

        public static MiniGameState read(Scanner in, int playerId, int gameType) {
            String gpu = in.next();
            int reg0 = in.nextInt();
            int reg1 = in.nextInt();
            int reg2 = in.nextInt();
            int reg3 = in.nextInt();
            int reg4 = in.nextInt();
            int reg5 = in.nextInt();
            int reg6 = in.nextInt();

            return switch (gameType) {
                case 0 -> new HurdleRaceState(playerId, gpu, reg0, reg1, reg2, reg3, reg4, reg5, reg6);
                case 1 -> new ArcheryState(playerId, gpu, reg0, reg1, reg2, reg3, reg4, reg5, reg6);
                case 2 -> new RollersState(playerId, gpu, reg0, reg1, reg2, reg3, reg4, reg5, reg6);
                default -> new DivingState(playerId, gpu, reg0, reg1, reg2, reg3, reg4, reg5, reg6);
            };
        }
    }
}
