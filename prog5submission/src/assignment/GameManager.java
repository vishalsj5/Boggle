package assignment;

import java.awt.Point;
import java.io.*;
import java.util.*;

public class GameManager implements BoggleGame {

    private int size;
    private int numPlayers;
    private char[][] board;
    private BoggleDictionary dictionary;
    private Random rand = new Random();
    private List<String> cubes;

    private int[] scores;
    private List<Set<String>> playedWords;

    private List<Point> lastWordPath;

    private SearchTactic currentTactic = SearchTactic.SEARCH_BOARD;
    private static final EnumSet<SearchTactic> VALID = EnumSet.of(SearchTactic.SEARCH_BOARD, SearchTactic.SEARCH_DICT);

    public BoggleDictionary getdictionary() {
        return dictionary;
    }

    public int getNumPlayers() {
        return numPlayers;
    }

    public List<Set<String>> getPlayedWords() {
        return playedWords;
    }

    @Override
    public void newGame(int size, int numPlayers, String cubeFile, BoggleDictionary dict) throws IOException {
        if (size <= 0)
            throw new IllegalArgumentException("Board size must be a positive integer.");
        if (numPlayers <= 0)
            throw new IllegalArgumentException("Number of players must be a positive integer.");
        if (cubeFile == null)
            throw new IllegalArgumentException("Cube file is null.");
        File file = new File(cubeFile);
        if (!file.exists() || !file.isFile())
            throw new IOException("Cube file not found: " + cubeFile);
        if (dict == null)
            throw new IllegalArgumentException("Dictionary is null.");

        this.size = size;
        this.numPlayers = numPlayers;
        this.dictionary = dict;
        this.scores = new int[numPlayers];
        playedWords = new ArrayList<>();
        for (int i = 0; i < numPlayers; i++)
            playedWords.add(new HashSet<>());
        this.lastWordPath = Collections.emptyList();

        loadCubes(cubeFile);

        if (cubes.size() < size * size)
            throw new IOException("Not enough cubes for board size.");

        Collections.shuffle(cubes, rand);

        char[][] generatedBoard = new char[size][size];
        for (int i = 0; i < size * size; i++) {
            int row = i / size;
            int col = i % size;
            String cube = cubes.get(i);
            generatedBoard[row][col] = cube.charAt(rand.nextInt(cube.length()));
        }

        setGame(generatedBoard);
    }

    private void loadCubes(String cubeFile) throws IOException {
        cubes = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(cubeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.toUpperCase(Locale.ENGLISH);
                if (line.length() != 6)
                    throw new IllegalArgumentException("Cube line must be 6 letters");
                for (char ch : line.toCharArray()) {
                    if (!((ch >= 'A' && ch <= 'Z') || ch == ' ' || ch == '\t'))
                        throw new IllegalArgumentException("Invalid character in cube file");
                }

                cubes.add(line);
            }
        }
    }

    @Override
    public int addWord(String word, int player) {
        if (dictionary == null || board == null)
            throw new IllegalStateException("Game not initialized.");
        if (player < 0 || player >= numPlayers || word == null || word.isEmpty())
            return 0;

        String upWord = word.toUpperCase(Locale.ENGLISH);
        if (upWord.length() < 4)
            return 0;
        if (playedWords.get(player).contains(upWord))
            return 0;
        if (!dictionary.contains(upWord))
            return 0;
        if (!findWordOnBoard(upWord))
            return 0;

        int score = upWord.length() - 3;
        scores[player] += score;
        playedWords.get(player).add(upWord);
        return score;
    }

    @Override
    public int[] getScores() {
        return scores;
    }

    @Override
    public List<Point> getLastAddedWord() {
        return new ArrayList<>(lastWordPath);
    }

    @Override
    public char[][] getBoard() {
        return board;
    }

    @Override
    public void setGame(char[][] board) {
        if (board == null || board.length == 0)
            throw new IllegalArgumentException("Board invalid");

        int len = board.length;
        this.board = new char[len][len];

        for (int i = 0; i < len; i++) {
            if (board[i] == null || board[i].length != len)
                throw new IllegalArgumentException("Board rows invalid");

            for (int j = 0; j < len; j++) {
                char c = Character.toUpperCase(board[i][j]);
                if (!((c >= 'A' && c <= 'Z') || c == ' ' || c == '\t'))
                    throw new IllegalArgumentException("Invalid board character: " + c);

                this.board[i][j] = c;
            }
        }

        this.size = len;
        this.lastWordPath = Collections.emptyList();
        if (scores != null)
            Arrays.fill(scores, 0);
        if (playedWords != null)
            for (Set<String> s : playedWords)
                s.clear();
    }

    @Override
    public void setSearchTactic(SearchTactic tactic) {
        if (tactic == null || !VALID.contains(tactic))
            this.currentTactic = SearchTactic.SEARCH_BOARD;
        else
            this.currentTactic = tactic;
    }

    @Override
    public Collection<String> getAllWords() {
        if (dictionary == null || board == null)
            throw new IllegalStateException("Game not initialized.");

        Set<String> results = (currentTactic == SearchTactic.SEARCH_BOARD)
                ? findAllWordsBoardDriven()
                : findAllWordsDictDriven();

        for (Set<String> played : playedWords)
            results.removeAll(played);
        return results;
    }

    private Set<String> findAllWordsBoardDriven() {
        Set<String> results = new HashSet<>();
        for (int r = 0; r < size; r++)
            for (int c = 0; c < size; c++)
                dfsFindAll(r, c, new boolean[size][size], new StringBuilder(), results);
        return results;
    }

    private void dfsFindAll(int r, int c, boolean[][] visited, StringBuilder current, Set<String> results) {
        if (r < 0 || r >= size || c < 0 || c >= size || visited[r][c])
            return;

        visited[r][c] = true;
        current.append(board[r][c]);
        String word = current.toString();

        if (!dictionary.isPrefix(word)) {
            current.deleteCharAt(current.length() - 1);
            visited[r][c] = false;
            return;
        }

        if (word.length() >= 4 && dictionary.contains(word))
            results.add(word);

        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++)
                if (!(dr == 0 && dc == 0))
                    dfsFindAll(r + dr, c + dc, visited, current, results);

        current.deleteCharAt(current.length() - 1);
        visited[r][c] = false;
    }

    private Set<String> findAllWordsDictDriven() {
        Set<String> found = new HashSet<>();
        for (String word : dictionary)
            if (word.length() >= 4 && findWordOnBoard(word))
                found.add(word);
        return found;
    }

    private boolean findWordOnBoard(String word) {
        this.lastWordPath = Collections.emptyList();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == word.charAt(0)) {
                    boolean[][] visited = new boolean[size][size];
                    List<Point> path = new ArrayList<>();
                    if (dfsFindWord(word, 0, r, c, visited, path)) {
                        this.lastWordPath = new ArrayList<>(path);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean dfsFindWord(String word, int index, int r, int c, boolean[][] visited, List<Point> path) {
        if (r < 0 || r >= size || c < 0 || c >= size)
            return false;
        if (visited[r][c])
            return false;
        if (board[r][c] != word.charAt(index))
            return false;

        visited[r][c] = true;
        path.add(new Point(r, c));

        if (index == word.length() - 1)
            return true;

        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++)
                if (!(dr == 0 && dc == 0) && dfsFindWord(word, index + 1, r + dr, c + dc, visited, path))
                    return true;

        visited[r][c] = false;
        path.remove(path.size() - 1);
        return false;
    }

    private boolean findWordOnBoardNoTouch(String word) {
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (board[r][c] == word.charAt(0)) {
                    boolean[][] visited = new boolean[size][size];
                    if (dfsFindWordNoTouch(word, 0, r, c, visited))
                        return true;
                }
            }
        }
        return false;
    }

    private boolean dfsFindWordNoTouch(String word, int index, int r, int c, boolean[][] visited) {
        if (r < 0 || r >= size || c < 0 || c >= size)
            return false;
        if (visited[r][c])
            return false;
        if (board[r][c] != word.charAt(index))
            return false;

        visited[r][c] = true;

        if (index == word.length() - 1)
            return true;

        for (int dr = -1; dr <= 1; dr++)
            for (int dc = -1; dc <= 1; dc++)
                if (!(dr == 0 && dc == 0) && dfsFindWordNoTouch(word, index + 1, r + dr, c + dc, visited))
                    return true;

        visited[r][c] = false;
        return false;
    }

    public boolean isWordOnBoard(String word) {
        return findWordOnBoardNoTouch(word.toUpperCase(Locale.ENGLISH));
    }
}
