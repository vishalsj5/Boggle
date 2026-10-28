package assignment;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GameDictionary implements BoggleDictionary {

    private static class Node {
        Node[] letters = new Node[28];
        boolean end = false;
    }

    private Node root;
    private Set<String> wordSet;

    public GameDictionary() {
        root = new Node();
        wordSet = new HashSet<>();
    }

    private int getIndex(char c) {
        if (c >= 'A' && c <= 'Z')
            return c - 'A';
        if (c == ' ')
            return 26;
        if (c == '\t')
            return 27;
        return -1;
    }

    @Override
    public boolean contains(String word) {
        if (word == null || word.isEmpty())
            return false;
        return wordSet.contains(word.toUpperCase(Locale.ENGLISH));
    }

    @Override
    public boolean isPrefix(String prefix) {
        if (prefix == null)
            return false;
        String upperPrefix = prefix.toUpperCase(Locale.ENGLISH);
        Node current = root;
        for (char c : upperPrefix.toCharArray()) {
            int index = getIndex(c);
            if (index < 0 || current.letters[index] == null)
                return false;
            current = current.letters[index];
        }
        return true;
    }

    @Override
    public void loadDictionary(String filename) throws IOException {
        if (filename == null)
            throw new IOException("Dictionary filename cannot be null.");

        Node newRoot = new Node();
        Set<String> newWordSet = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String word = line.toUpperCase(Locale.ENGLISH);
                if (word.isEmpty() || newWordSet.contains(word))
                    continue;

                Node current = newRoot;
                boolean valid = true;
                for (char c : word.toCharArray()) {
                    int index = getIndex(c);
                    if (index < 0) {
                        valid = false;
                        break;
                    }
                    if (current.letters[index] == null)
                        current.letters[index] = new Node();
                    current = current.letters[index];
                }

                if (valid) {
                    current.end = true;
                    newWordSet.add(word);
                }
            }
        }

        root = newRoot;
        wordSet = newWordSet;
    }

    @Override
    public Iterator<String> iterator() {
        return new Iterator<>() {
            private Stack<Pair<Node, StringBuilder>> stack = new Stack<>();
            private String nextWord = null;

            {
                stack.push(new Pair<>(root, new StringBuilder()));
                advance();
            }

            private void advance() {
                nextWord = null;
                while (!stack.isEmpty()) {
                    Pair<Node, StringBuilder> pair = stack.pop();
                    Node node = pair.getKey();
                    StringBuilder sb = pair.getValue();

                    for (int i = 27; i >= 0; i--) {
                        if (node.letters[i] != null) {
                            char c = (i < 26) ? (char) ('A' + i) : (i == 26 ? ' ' : '\t');
                            StringBuilder newSb = new StringBuilder(sb);
                            newSb.append(c);
                            stack.push(new Pair<>(node.letters[i], newSb));
                        }
                    }

                    if (node.end) {
                        nextWord = sb.toString();
                        return;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                return nextWord != null;
            }

            @Override
            public String next() {
                if (nextWord == null)
                    throw new NoSuchElementException();
                String result = nextWord;
                advance();
                return result;
            }
        };
    }

    private static class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}