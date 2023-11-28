import java.util.Comparator;

import components.map.Map;
import components.map.Map1L;
import components.set.Set;
import components.set.Set1L;
import components.simplereader.SimpleReader;
import components.simplereader.SimpleReader1L;
import components.simplewriter.SimpleWriter;
import components.simplewriter.SimpleWriter1L;
import components.sortingmachine.SortingMachine;
import components.sortingmachine.SortingMachine1L;

/**
 * Counts the word occurrences in a given input file and outputs an HTML
 * document of a alphabetized tag cloud using a user-input quantity of the most
 * commonly occuring words. Project built from a direct copy of the SW2
 * WordCounter Project.
 *
 * @author Nicholas McCracken and Jack Mikesell
 *
 */
public final class TagCloudGen {

    /**
     * Compare keys of {@code Map.Pair<String, Integer>}s in lexicographic order
     * while ignoring case.
     */
    private static class KeyLT
            implements Comparator<Map.Pair<String, Integer>> {
        @Override
        public int compare(Map.Pair<String, Integer> o1,
                Map.Pair<String, Integer> o2) {

            /*
             * Ensure a zero is only returned when both pairs, which includes
             * their key and value, are equal to one another to be consistent
             * with equals.
             */
            int compare = o1.key().compareToIgnoreCase(o2.key());
            if (compare == 0) {
                compare = o2.value().compareTo(o1.value());
            }
            return compare;
        }
    }

    /**
     * Compare values of {@code Map.Pair<String, Integer>}s in descending order.
     */
    private static class ValueLT
            implements Comparator<Map.Pair<String, Integer>> {
        @Override
        public int compare(Map.Pair<String, Integer> o1,
                Map.Pair<String, Integer> o2) {

            /*
             * Ensure a zero is only returned when both pairs, which includes
             * their key and value, are equal to one another to be consistent
             * with equals.
             */
            int compare = o2.value().compareTo(o1.value());
            if (compare == 0) {
                compare = o1.key().compareToIgnoreCase(o2.key());
            }
            return compare;
        }
    }

    /**
     * Private constructor so this utility class cannot be instantiated.
     */
    private TagCloudGen() {
    }

    /**
     * Generates the set of characters in the given {@code str} into the given
     * {@code charSet}. Reused from SW1 Glossary.
     *
     * @param str
     *            the given {@code String}
     * @param charSet
     *            the {@code Set} to be replaced
     * @replaces charSet
     * @ensures charSet = entries(str)
     */
    private static void generateElements(String str, Set<Character> charSet) {
        assert str != null : "Violation of: str is not null";
        assert charSet != null : "Violation of: charSet is not null";

        /*
         * Input each character of the string as a separate element in the
         * temporary set, then replace the formal parameter charSet with the
         * elements from the temporary set.
         */
        Set<Character> strEntries = charSet.newInstance();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            // Ensure duplicate characters are not accidentally added.
            if (!strEntries.contains(c)) {
                strEntries.add(c);
            }
        }
        charSet.transferFrom(strEntries);
    }

    /**
     * Returns the first "word" (maximal length string of characters not in
     * {@code separators}) or "separator string" (maximal length string of
     * characters in {@code separators}) in the given {@code text} starting at
     * the given {@code position}. Reused from SW1 Glossary.
     *
     * @param text
     *            the {@code String} from which to get the word or separator
     *            string
     * @param position
     *            the starting index
     * @param separators
     *            the {@code Set} of separator characters
     * @return the first word or separator string found in {@code text} starting
     *         at index {@code position}
     * @requires 0 <= position < |text|
     * @ensures <pre>
     * nextWordOrSeparator =
     *   text[position, position + |nextWordOrSeparator|)  and
     * if entries(text[position, position + 1)) intersection separators = {}
     * then
     *   entries(nextWordOrSeparator) intersection separators = {}  and
     *   (position + |nextWordOrSeparator| = |text|  or
     *    entries(text[position, position + |nextWordOrSeparator| + 1))
     *      intersection separators /= {})
     * else
     *   entries(nextWordOrSeparator) is subset of separators  and
     *   (position + |nextWordOrSeparator| = |text|  or
     *    entries(text[position, position + |nextWordOrSeparator| + 1))
     *      is not subset of separators)
     * </pre>
     */
    private static String nextWordOrSeparator(String text, int position,
            Set<Character> separators) {
        assert text != null : "Violation of: text is not null";
        assert separators != null : "Violation of: separators is not null";
        assert 0 <= position : "Violation of: 0 <= position";
        assert position < text.length() : "Violation of: position < |text|";

        /*
         * Determine if first character is word or separator which determines if
         * the string wordOrSeparator will contain a word or separators.
         */
        String wordOrSeparator = "";
        boolean initialCharacterIsSeparator = separators
                .contains(text.charAt(position));
        int i = position;

        /*
         * Continuously add characters to the string wordOrSeparator until the
         * end of the formal parameter text is reached or the current character
         * is not of the same type (word or separator) as the initial character.
         */
        while (i < text.length() && initialCharacterIsSeparator == separators
                .contains(text.charAt(i))) {
            wordOrSeparator += text.charAt(i);
            i++;
        }

        /*
         * Convert word to lowercase so the same word with different
         * capitalization does not occur multiple times in the tag cloud.
         */
        return wordOrSeparator.toLowerCase();
    }

    /**
     * Inputs a list of terms and their definitions from the given file and
     * stores them in the given {@code wordCountMap}. Redesigned from SW1
     * Glossary.
     *
     * @param inputFile
     *            the name of the input file
     * @param wordCountMap
     *            the {@code Map} of unique words and their number of
     *            occurrences
     * @replaces wordCountMap
     * @requires <pre>
     * [file named inputFile exists but is not open, and consists of English
     *  words following standard grammatical convention]
     * </pre>
     * @ensures [wordCountMap contains unique words and their number of
     *          occurrences from the file]
     */
    private static void countWords(String inputFile,
            Map<String, Integer> wordCountMap) {
        assert inputFile != null : "Violation of: inputFile is not null";
        assert wordCountMap != null : "Violation of: termMap is not null";

        /*
         * Open an input stream to read from the file and a set to store all
         * characters that are not present in valid words.
         */
        SimpleReader inFile = new SimpleReader1L(inputFile);
        Set<Character> separators = new Set1L<Character>();
        generateElements(" \"\t\n\r,-.!?[]'`~@#$%^&*-=+{}|<>;:/()", separators);

        /*
         * Store each word and it's number of occurrences in the map until the
         * end of the file is reached.
         */
        while (!inFile.atEOS()) {
            String text = inFile.nextLine();
            int position = 0;

            while (position < text.length()) {
                String wordOrSeparator = nextWordOrSeparator(text, position,
                        separators);
                position += wordOrSeparator.length();

                /*
                 * Determine if return from wordOrSeparator is a valid word that
                 * should be stored by checking that it's first character is not
                 * a separator.
                 */
                if (!separators.contains(wordOrSeparator.charAt(0))) {
                    /*
                     * Update the occurrence count of the word if it already
                     * exists in the map, and add to the map if it does not.
                     */
                    if (wordCountMap.hasKey(wordOrSeparator)) {
                        int count = wordCountMap.value(wordOrSeparator);
                        wordCountMap.replaceValue(wordOrSeparator, count + 1);
                    } else {
                        wordCountMap.add(wordOrSeparator, 1);
                    }
                }
            }
        }
        // Close file input stream.
        inFile.close();
    }

    /**
     * Stores a list of unique words of up to wordQuantity and their number of
     * occurrences from the {@code Map} alphabetically in the given
     * {@code SortingMachine}.
     *
     * @param wordQuantity
     *            the maximum number of unique words to store in topNWords
     * @param topNWords
     *            the {@code SortingMachine} of words in descending order based
     *            on their number of occurrences
     * @param alphabetized
     *            the {@code SortingMachine} of up to wordQuantity unique words
     *            in alphabetical order
     * @param wordCountMap
     *            the {@code Map} of unique words and their number of
     *            occurrences
     *
     * @replaces alphabetized
     * @requires <pre>
     * [file named fileName exists but is not open] and 0 <= wordQuantity
     * </pre>
     * @ensures [SortingMachine contains up to wordQuantity terms ordered
     *          alphabetically -> term queueing from Map]
     */
    private static void alphabetizeTopNWords(int wordQuantity,
            SortingMachine<Map.Pair<String, Integer>> topNWords,
            SortingMachine<Map.Pair<String, Integer>> alphabetized,
            Map<String, Integer> wordCountMap) {
        assert 0 <= wordQuantity : "Violation of: 0 <= wordQuantity";
        assert topNWords != null : "Violation of: topNWords is not null";
        assert alphabetized != null : "Violation of: alphabetized is not null";
        assert wordCountMap != null : "Violation of: wordCountMap is not null";

        //Store each word count pair from the map in the sorting machine.
        for (Map.Pair<String, Integer> pair : wordCountMap) {
            topNWords.add(pair);
        }

        // Store pairs up to wordQuantity and sort them alphabetically.
        topNWords.changeToExtractionMode();
        int counter = 0;
        while (topNWords.size() > 0 && counter < wordQuantity) {
            alphabetized.add(topNWords.removeFirst());
            counter++;
        }
    }

    /**
     * Returns the minimum and maximum number of occurences in the given
     * {@code wordCounts}.
     *
     * @param wordCounts
     *            the {@code SortingMachine} to be searched through containing
     *            pairs of their unique words and their number of occurrences
     * @return minimum and maximum number of occurences in {@code wordCounts}
     * @ensures findMinMaxCounts = [min(values in {@code wordCounts}),
     *          max(values in {@code wordCounts})]
     */
    private static int[] findMinMaxCounts(
            SortingMachine<Map.Pair<String, Integer>> wordCounts) {
        assert wordCounts != null : "Violation of: wordCountMap is not null";

        // Initialize array with seed values to ensure correct comparsion.
        int[] minMax = new int[2];
        minMax[0] = Integer.MAX_VALUE;
        minMax[1] = -1;

        // Iterate through the sorting machine to find the min and max.
        for (Map.Pair<String, Integer> wordCount : wordCounts) {
            int count = wordCount.value();
            minMax[0] = Math.min(minMax[0], count);
            minMax[1] = Math.max(minMax[1], count);
        }

        /*
         * Explicitly guard against one word edge case to ensure formula for
         * calculating font size does not divide by zero.
         */
        if (minMax[0] == minMax[1]) {
            minMax[0] = 0;
        }

        return minMax;
    }

    /**
     * Generates an HTML file which lists each word from an input file in an
     * alphabetized table, along with it's number of occurrences in said file.
     *
     * @param heading
     *            the {@code String} heading for the html file
     * @param outputFile
     *            the name of the output file
     * @param alphabetized
     *            the {@code SortingMachine} of top N occuring words in
     *            alphabetical order
     * @clears alphabetized
     * @ensures <pre>
     * [generates HTML file with each unique word from the input file in an
     * alphabetized tag cloud with it's font size corresponding to it's number
     * of occurrences in the file relative to other words]
     * </pre>
     */
    private static void generateWordCountTable(String heading,
            String outputFile,
            SortingMachine<Map.Pair<String, Integer>> alphabetized) {
        assert heading != null : "Violation of: heading is not null";
        assert outputFile != null : "Violation of: outputFile is not null";
        assert alphabetized != null : "Violation of: alphabetized is not null";

        // Open an output stream to write to a file stored in folder.
        SimpleWriter fileOut = new SimpleWriter1L(outputFile);

        /*
         * Create opening tags including a title, linked css stylesheet, head,
         * and div to store the tag cloud.
         */
        fileOut.println("<html>");
        fileOut.println("<head>");
        fileOut.println(
                "<link rel=\"stylesheet\" href=\"http://web.cse.ohio-state.edu"
                        + "/software/2231/web-sw2/assignments/projects"
                        + "/tag-cloud-generator/data/tagcloud.css\" type=\"text/css\">");
        fileOut.println(
                "<link href=\"data/tagcloud.css\" rel=\"stylesheet\" type=\"text/css\">");
        fileOut.println("<title>" + heading + "</title>");
        fileOut.println("</head>");
        fileOut.println("<body>");
        fileOut.println("<h2>" + heading + "</h2>");
        fileOut.println("<hr>");
        fileOut.println("<div class=\"cdiv\">");
        fileOut.println("<p class=\"cbox\">");

        /*
         * For each word stored in alphabetized, calculate it's font size
         * corresponding to it's number of occurrences in the file and place it
         * within the tag cloud div.
         */
        final int maxFont = 37, minFont = 11;
        final int[] minMax = findMinMaxCounts(alphabetized);
        alphabetized.changeToExtractionMode();

        while (alphabetized.size() > 0) {
            Map.Pair<String, Integer> wordCount = alphabetized.removeFirst();

            int count = wordCount.value();
            // Determine class corresponding to font size with given formula
            int fontSize = minFont
                    + (maxFont * (count - minMax[0])) / (minMax[1] - minMax[0]);

            fileOut.print("<span style=\"cursor:default\" ");
            fileOut.print("class=\"" + "f" + fontSize + "\" ");
            fileOut.print("title=\"count: " + count + "\">");
            fileOut.print(wordCount.key());
            fileOut.println("</span>");
        }

        // Close all opened tags and output stream.
        fileOut.println("</p>");
        fileOut.println("</div>");
        fileOut.println("</body>");
        fileOut.println("</html>");
        fileOut.close();
    }

    /**
     * Main method.
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        // Open input and output streams to console.
        SimpleReader in = new SimpleReader1L();
        SimpleWriter out = new SimpleWriter1L();

        /*
         * Prompt user for the name of an input file to read words from and an
         * output file to generate a tag cloud in, as well as the number of
         * words to be included in the tag cloud.
         */
        out.print("Enter the name "
                + "of an input file and it's path with a .txt extension: ");
        String inputFile = in.nextLine();
        out.print("Enter the name "
                + "of an output file and it's path with a .html extension: ");
        String outputFile = in.nextLine();
        out.print("Enter the quantity of words for the tag cloud: ");
        int wordQuantity = Integer.parseInt(in.nextLine());

        // Store all words and their respective counts from input file in a map.
        Map<String, Integer> wordCountMap = new Map1L<String, Integer>();
        countWords(inputFile, wordCountMap);

        /*
         * Store all words from map in an alphabetized sorting machine. Note:
         * Line length cannot be fixed as the line is saved like this by
         * checkstyle.
         */
        SortingMachine<Map.Pair<String, Integer>> topNWords = new SortingMachine1L<Map.Pair<String, Integer>>(
                new ValueLT()),
                alphabetized = new SortingMachine1L<Map.Pair<String, Integer>>(
                        new KeyLT());
        alphabetizeTopNWords(wordQuantity, topNWords, alphabetized,
                wordCountMap);

        /*
         * Generates an HTML file with a heading stating the file name and
         * number of words included along with a tag cloud of up to the most
         * commonly occuring words.
         */
        String heading = "Top " + alphabetized.size() + " words in "
                + inputFile;
        generateWordCountTable(heading, outputFile, alphabetized);

        // Close input and output streams.
        in.close();
        out.close();
    }
}
