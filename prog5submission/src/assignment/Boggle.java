package assignment;

import java.io.IOException;
import java.util.*;

public class Boggle {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=== Welcome to Boggle! ===");
        BoggleDictionary dict = new GameDictionary();
        GameManager game = new GameManager();

        try {
            dict.loadDictionary("words.txt");
        } catch (IOException e) {
            System.out.println("Error loading dictionary: " + e.getMessage());
            return;
        }

        while (true) {
            System.out.print("Enter number of players: ");
            int numPlayers = 1;
            try {
                numPlayers = Integer.parseInt(scanner.nextLine().trim());
                if (numPlayers < 1)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println("Invalid input; defaulting to 1 player.");
                numPlayers = 1;
            }

            try {
                game.newGame(4, numPlayers, "cubes.txt", dict);
            } catch (IOException e) {
                System.out.println("Error initializing game: " + e.getMessage());
                return;
            }

            playRound(game, numPlayers);

            System.out.print("\nPlay again? (y/n): ");
            String again = scanner.nextLine().trim().toLowerCase(Locale.ENGLISH);
            if (!(again.equals("y") || again.equals("yes")))
                break;
        }

        System.out.println("Thanks for playing!");
    }

    private static void playRound(GameManager game, int numPlayers) {
        System.out.println("\nHere's your Boggle board:\n");
        printBoard(game.getBoard());

        for (int player = 0; player < numPlayers; player++) {
            System.out.println("\n--- Player " + (player + 1) + "'s Turn ---");
            System.out.println("Type words (press Enter on empty line to finish):");

            while (true) {
                System.out.print("Word: ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty())
                    break;

                String reason = getInvalidReason(input, player, game);
                if (reason != null) {
                    System.out.println("Incorrect: " + reason);
                    continue;
                }

                int points = game.addWord(input, player);
                if (points > 0) {
                    System.out.println("Correct, +" + points + " points! (" + input.toUpperCase() + ")");
                    printHighlightedBoard(game.getBoard(), game.getLastAddedWord());
                }

                // Re-fetch the score from GameManager to ensure it's up-to-date
                int currentScore = game.getScores()[player];
                System.out.println("Current score: " + currentScore);
            }

            int finalScore = game.getScores()[player];
            System.out.println("Player " + (player + 1) + " finished their turn with " + finalScore + " points.\n");
        }

        System.out.println("Finding all words the computer can make...");
        game.setSearchTactic(GameManager.SearchTactic.SEARCH_BOARD);
        Collection<String> computerWords = game.getAllWords();

        int computerScore = computerWords.stream().mapToInt(w -> w.length() - 3).sum();

        System.out.println("\n=== Round Summary ===");
        for (int i = 0; i < numPlayers; i++)
            System.out.println("Player " + (i + 1) + " final score: " + game.getScores()[i]);
        System.out.println("\nComputer found " + computerWords.size() + " words.");
        System.out.println("Computer score (for reference): " + computerScore);

        System.out.println("\n--- Words Found by Each Player ---");
        for (int i = 0; i < numPlayers; i++)
            System.out.println("Player " + (i + 1) + ": " + String.join(", ", game.getPlayedWords().get(i)));

        System.out.println("\n--- Words Found by Computer ---");
        System.out.println(String.join(", ", computerWords));

        int maxScore = Arrays.stream(game.getScores()).max().orElse(0);
        List<String> winners = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++)
            if (game.getScores()[i] == maxScore)
                winners.add("Player " + (i + 1));

        System.out.print("\nWinner");
        if (winners.size() > 1)
            System.out.print("s");
        System.out.print(": ");
        System.out.println(String.join(", ", winners) + " with " + maxScore + " points!");
    }

    private static void printBoard(char[][] board) {
        for (char[] row : board) {
            for (char c : row)
                System.out.print(c + " ");
            System.out.println();
        }
    }

    private static void printHighlightedBoard(char[][] board, List<java.awt.Point> path) {
        if (path == null || path.isEmpty()) {
            printBoard(board);
            return;
        }

        char[][] temp = new char[board.length][board.length];
        for (int i = 0; i < board.length; i++)
            temp[i] = board[i].clone();

        for (java.awt.Point p : path)
            temp[p.x][p.y] = Character.toLowerCase(temp[p.x][p.y]);

        System.out.println("\nUpdated Board:");
        printBoard(temp);
    }

    private static String getInvalidReason(String word, int player, GameManager game) {
        String up = word.toUpperCase(Locale.ENGLISH);
        if (up.length() < 4)
            return "Word too short (must be 4+ letters).";
        if (!game.getdictionary().contains(up))
            return "Not found in dictionary.";
        if (game.getPlayedWords().get(player).contains(up))
            return "Duplicate word for this player.";
        if (!game.isWordOnBoard(up))
            return "Word not found on board.";
        return null;
    }
}
