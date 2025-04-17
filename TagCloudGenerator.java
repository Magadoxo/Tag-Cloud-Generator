import java.util.Comparator;

import components.map.Map;
import components.map.Map1L;
import components.queue.Queue;
import components.queue.Queue1L;
import components.set.Set;
import components.set.Set1L;
import components.simplereader.SimpleReader;
import components.simplereader.SimpleReader1L;
import components.simplewriter.SimpleWriter;
import components.simplewriter.SimpleWriter1L;
import components.sortingmachine.SortingMachine;
import components.sortingmachine.SortingMachine2;

/**
 * Generates a tag cloud from a text file based on word frequencies.
 *
 * @author Ayub Abdi and Mark Wang
 */
public final class TagCloudGenerator {

    /**
     * Private constructor so this utility class cannot be instantiated.
     */
    private TagCloudGenerator() {
    }

    /**
     * Maximum font value for tags in tag cloud.
     */
    private static final int FONT_MAX = 48;

    /**
     * Minimum font value for tags in tag cloud.
     */
    private static final int FONT_MIN = 11;

    /**
     * Outputs the opening tags in the generated HTML file.
     *
     * @param title
     *            the title of the page
     * @param out
     *            the output stream
     * @updates out.content
     * @requires out.isOpen
     * @ensures out.content = #out.content * [the HTML opening tags]
     */
    private static void outputHeader(String title, SimpleWriter out) {
        assert out != null : "Violation of: out is not null";
        assert out.isOpen() : "Violation of: out.isOpen";

        // Print HTML header with CSS links for styling
        out.println("<html>\n\t<head>\n\t<title>" + title
                + "</title>\n<link href=\"https://cse22x1.engineering.osu.edu/2231/web-sw2/assignments/projects/tag-cloud-generator/data/tagcloud.css\" rel=\"stylesheet\" "
                + "type=\"text/css\">\n<link href=\"tagcloud.css\" "
                + "rel=\"stylesheet\" type=\"text/css\">\n" + "</head>\n<body>\n" + "<h2>"
                + title + "</h2>\r\n" + "<hr>\r\n" + "<div class=\"cdiv\">\r\n"
                + "<p class=\"cbox\">");
    }

    /**
     * Acquires a valid number of words for the tag cloud.
     *
     * @param words
     *            the map of unique words and their counts
     * @param out
     *            the output stream for error messages
     * @param in
     *            the input stream for user inputs
     * @return the valid number of words for the tag cloud
     */
    private static int getValidInput(Map<String, Integer> words, SimpleWriter out,
            SimpleReader in) {
        // Prompt user for number of words to include
        out.print("Input number of words to include in tag cloud: ");
        int n = in.nextInteger();

        // Validate input - must be positive and not exceed unique word count
        while (n < 0 || n > words.size()) {
            if (n < 0) {
                out.println("Must be a positive integer.");
            } else {
                out.println(
                        "Must not exceed the number of unique words in the input file.");
            }
            out.print("Input number of words to include in tag cloud: ");
            n = in.nextInteger();
        }
        return n;
    }

    /**
     * Comparator to sort Strings in alphabetical order (case-insensitive).
     */
    private static class StringLT implements Comparator<String> {
        @Override
        public int compare(String a, String b) {
            return a.compareToIgnoreCase(b);
        }
    }

    /**
     * Comparator to sort Map.Pairs by integer values in descending order.
     */
    private static class CountLT implements Comparator<Map.Pair<String, Integer>> {
        @Override
        public int compare(Map.Pair<String, Integer> a, Map.Pair<String, Integer> b) {
            return Integer.compare(b.value(), a.value());
        }
    }

    /**
     * Returns the first word or separator string found in text starting at the
     * given position.
     *
     * @param text
     *            the string to parse
     * @param position
     *            the starting position in the string
     * @param separatorSet
     *            the set of separator characters
     * @return the first word or separator string found
     * @requires 0 <= position < |text|
     * @ensures nextWordOrSeparator is the next word or separator string in text
     */
    private static String nextWordOrSeparator(String text, int position,
            Set<Character> separatorSet) {
        StringBuilder word = new StringBuilder();
        // Determine if current character is a separator
        boolean isSeparator = separatorSet.contains(text.charAt(position));

        // Continue building the word or separator string until a different type is found
        while (position < text.length()
                && separatorSet.contains(text.charAt(position)) == isSeparator) {
            word.append(text.charAt(position));
            position++;
        }

        return word.toString();
    }

    /**
     * Constructs a map of word-count pairs from the input text file.
     *
     * @param input
     *            the input stream to read from
     * @return a map of unique words and their counts
     * @requires input.isOpen
     * @ensures input.isOpen and the map contains word-count pairs from the
     *          input
     */
    private static Map<String, Integer> wordCountMap(SimpleReader input) {
        assert input.isOpen() : "Violation of: input.isOpen";

        Map<String, Integer> countMap = new Map1L<>();
        Set<Character> separators = new Set1L<>();
        // Define what characters count as separators
        String separatorChars = " `'|\t\n\r,-.!?[]\";:/()_*";

        // Add all separator characters to the set
        for (char c : separatorChars.toCharArray()) {
            separators.add(c);
        }

        // Process each line of the input file
        while (!input.atEOS()) {
            String line = input.nextLine();
            int position = 0;
            // Process each word/separator in the line
            while (position < line.length()) {
                String term = nextWordOrSeparator(line, position, separators)
                        .toLowerCase();

                if (!term.trim().isEmpty() && !countMap.hasKey(term)) {
                    if (!separators.contains(term.charAt(0))) {
                        countMap.add(term, 1);
                    }
                    // Add new word to map with count 1

                } else if (!term.trim().isEmpty()) {
                    // Increment count for existing word
                    countMap.replaceValue(term, countMap.value(term) + 1);
                }
                position += term.length();
            }
        }

        return countMap;
    }

    /**
     * Outputs an HTML formatted tag for a word in the tag cloud.
     *
     * @param out
     *            the output stream for HTML
     * @param fSize
     *            the font size for the word
     * @param count
     *            the word's frequency
     * @param word
     *            the word itself
     */
    private static void printTagInCloud(SimpleWriter out, int fSize, int count,
            String word) {
        // Output span element with appropriate styling and title (shows count on hover)
        out.println("<span style=\"cursor:default\" class=\"f" + fSize
                + "\" title=\"count: " + count + "\">" + word + "</span>");
    }

    /**
     * Calculates an appropriate font size for a word based on its frequency.
     *
     * @param max
     *            the maximum word frequency
     * @param min
     *            the minimum word frequency
     * @param count
     *            the word's frequency
     * @return the calculated font size
     */
    private static int fontSize(int max, int min, float count) {
        float range = FONT_MAX - FONT_MIN;
        if (max > min) {
            // Linear scaling of font size based on word frequency
            return (int) (range * (count - min) / (max - min) + FONT_MIN);
        } else {
            // All words have same frequency, use max font size
            return FONT_MAX;
        }
    }

    /**
     * Main method.
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
        // Initialize input/output streams
        SimpleReader in = new SimpleReader1L();
        SimpleWriter out = new SimpleWriter1L();

        // Get input file name from user
        out.print("Input name of text file to count words from: ");
        String inputFile = in.nextLine();
        SimpleReader inFile = new SimpleReader1L(inputFile);

        // Get output file name from user
        out.print("Input name for the HTML output file: ");
        String outputFile = in.nextLine();
        SimpleWriter htmlOut = new SimpleWriter1L(outputFile);

        // Generate word count map from input file
        Map<String, Integer> data = wordCountMap(inFile);
        // Get valid number of words to include from user
        int n = getValidInput(data, out, in);

        if (n > 0) {
            Queue<String> ordered = new Queue1L<>();
            // Sorting machine to sort by word count (descending)
            SortingMachine<Map.Pair<String, Integer>> cSort = new SortingMachine2<>(
                    new CountLT());

            // Add all word-count pairs to sorting machine
            for (Map.Pair<String, Integer> pair : data) {
                cSort.add(pair);
            }
            cSort.changeToExtractionMode();

            // Extract top n words by frequency
            for (int i = 0; i < n; i++) {
                ordered.enqueue(cSort.removeFirst().key());
            }

            // Find max and min counts for font scaling
            int maxCount = data.value(ordered.front());
            int minCount = maxCount;
            for (String word : ordered) {
                minCount = data.value(word);
            }

            // Sorting machine to sort words alphabetically
            SortingMachine<String> sSort = new SortingMachine2<>(new StringLT());
            while (ordered.length() > 0) {
                sSort.add(ordered.dequeue());
            }
            sSort.changeToExtractionMode();

            // Output HTML header
            outputHeader("Top " + n + " words in " + inputFile, htmlOut);

            // Output each word in the tag cloud with appropriate font size
            while (sSort.size() > 0) {
                String word = sSort.removeFirst();
                int size = fontSize(maxCount, minCount, data.value(word));
                printTagInCloud(htmlOut, size, data.value(word), word);
            }
        } else {
            // Special case for 0 words
            outputHeader("Top 0 words in " + inputFile, htmlOut);
        }

        // Close HTML tags and all streams
        htmlOut.println("</p>\r\n" + "</div>" + "</body>\n</html>");
        in.close();
        out.close();
        inFile.close();
        htmlOut.close();
    }
}
