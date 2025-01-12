import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.codingame.gameengine.runner.MultiplayerGameRunner;
import com.codingame.gameengine.runner.simulate.GameResult;

public class Main {
	public static void main(String[] args) {

//		MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();
//
//		// Set seed here (leave empty for random)
//		gameRunner.setSeed(8787933113005275327l);
//
//		// Select agents here
////		gameRunner.addAgent("python3 bots/depth_one.py", "DepthOne");
////		gameRunner.addAgent("python3 bots/julien.py 10", "Julien");
//		gameRunner.addAgent(Player.class, "Player");
//		gameRunner.addAgent(PlayerArena.class, "PlayerArena");
////		gameRunner.addAgent(Player.class, "Player");
////		gameRunner.addAgent(HurdlePlayer.class, "HurdlePlayer");
//		gameRunner.addAgent(DivingPlayer.class, "DivingPlayer");
////		gameRunner.addAgent(DivingPlayer.class, "DivingPlayer");
////		gameRunner.addAgent(Player.class, "Enemy 2");
//
//		Properties params = new Properties();
//		// Set params here
//		gameRunner.setGameParameters(params);
//		gameRunner.setLeagueLevel(19);
//
//
//		gameRunner.start(8888);

		runSeed(-1717375063034309773l);

		runTests(1000, true, Player.class, PlayerArena.class, PlayerArena2.class);
	}

	private static void runSeed(long seed) {
		MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();
		gameRunner.setSeed(seed);
		gameRunner.setLeagueLevel(19);

//        gameRunner.addAgent(PlayerArena.class, "Arena Agent");
//        gameRunner.addAgent(PlayerTest.class, "Arena Agent");
		gameRunner.addAgent(PlayerArena.class, "Arena Agent");
		gameRunner.addAgent(Player.class, "IDE Agent");
		gameRunner.addAgent(PlayerArena2.class, "Arena Agent");
//        gameRunner.addAgent(SimpleAgent.class, "Simple Agent");

		Properties params = new Properties();
		gameRunner.setGameParameters(params);

		gameRunner.start(8888);
	}

	private static void runTests(int count, boolean fullLog, Class<?> agent1, Class<?> agent2, Class<?> agent3) {
		Random rand = new Random(System.nanoTime());
		int summaryScoreIde = 0;
		int summaryScoreArena1 = 0;
		int summaryScoreArena2 = 0;
		int summaryScoreTie = 0;

		for (int i = 0; i < count; i++) {
			long seed = rand.nextLong();

			MultiplayerGameRunner gameRunner2 = new MultiplayerGameRunner();
			gameRunner2.setLeagueLevel(19);
			gameRunner2.setSeed(seed);

			if (i % 3 == 0) {
				gameRunner2.addAgent(agent1, agent1.getSimpleName());
				gameRunner2.addAgent(agent2, agent2.getSimpleName());
				gameRunner2.addAgent(agent3, agent3.getSimpleName());
			} else if (i % 3 == 1) {
				gameRunner2.addAgent(agent2, agent2.getSimpleName());
				gameRunner2.addAgent(agent3, agent3.getSimpleName());
				gameRunner2.addAgent(agent1, agent1.getSimpleName());
			} else {
				gameRunner2.addAgent(agent3, agent3.getSimpleName());
				gameRunner2.addAgent(agent1, agent1.getSimpleName());
				gameRunner2.addAgent(agent2, agent2.getSimpleName());
			}

			GameResult result = gameRunner2.simulate();

			int ideAgentIndex = result.agents.get(0).name.equals(agent1.getSimpleName()) ? result.agents.get(0).index : result.agents.get(1).name.equals(agent1.getSimpleName()) ? result.agents.get(1).index : result.agents.get(2).index;
			int arenaAgentIndex1 = result.agents.get(0).name.equals(agent2.getSimpleName()) ? result.agents.get(0).index : result.agents.get(1).name.equals(agent2.getSimpleName()) ? result.agents.get(1).index : result.agents.get(2).index;
			int arenaAgentIndex2 = result.agents.get(0).name.equals(agent3.getSimpleName()) ? result.agents.get(0).index : result.agents.get(1).name.equals(agent3.getSimpleName()) ? result.agents.get(1).index : result.agents.get(2).index;

			int ideScore = result.scores.get(ideAgentIndex);
			int arenaScore1 = result.scores.get(arenaAgentIndex1);
			int arenaScore2 = result.scores.get(arenaAgentIndex2);

			String winner = "";

			if (ideScore >= arenaScore1 && ideScore >= arenaScore2) {
				summaryScoreIde++;
				winner = agent1.getSimpleName();
			}
			if (arenaScore1 >= ideScore && arenaScore1 >= arenaScore2) {
				summaryScoreArena1++;
				winner = agent2.getSimpleName();
			}
			if (arenaScore2 >= ideScore && arenaScore2 >= arenaScore1) {
				summaryScoreArena2++;
				winner = agent3.getSimpleName();
			}

			if (fullLog) System.out.println((i+1) +"/" + count+ " winner: " + winner
					+ ", " + result.agents.get(ideAgentIndex).name + ": " + result.scores.get(ideAgentIndex)
					+ ", " + result.agents.get(arenaAgentIndex1).name + ": " + result.scores.get(arenaAgentIndex1)
					+ ", " + result.agents.get(arenaAgentIndex2).name + ": " + result.scores.get(arenaAgentIndex2) + ", seed: " + seed);
			if (i % 20 == 19) {
				System.out.println("\nSummary: " + ((100*summaryScoreIde/(i+1-summaryScoreTie))) + "% "
						+ agent1.getSimpleName() + " wins " + summaryScoreIde
						+ ", " + agent2.getSimpleName() + " wins " + summaryScoreArena1
						+ ", " + agent3.getSimpleName() + " wins " + summaryScoreArena2
						+ ", ties " + summaryScoreTie);
			}
		}
		System.out.println("\nFinal Summary: "
				+ agent1.getSimpleName() + " wins " + summaryScoreIde
				+ ", " + agent2.getSimpleName() + " wins " + summaryScoreArena1
				+ ", " + agent3.getSimpleName() + " wins " + summaryScoreArena2
				+ ", ties " + summaryScoreTie);
	}
}
