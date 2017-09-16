package assignment;

import java.io.*;
import java.util.*;

public class RandomWordWriter implements TextProcessor {

    public static void main(String[] args) {

        //making sure the number of arguments is correct
        if (args.length != 4) {
            System.err.println("Incorrect number of arguments. Proper usage:" +
                    "\njava RandomWordWriter [source file] [result file] [level of analysis, k] [length of output, in words]\n" +
                    "\nsource file should be a valid file for reading, and contain more characters than k" +
                    "\nresult flie should be an valid file for output" +
                    "\nlevel of analysis should be a whole number" +
                    "\nlength of output should be a whole number");
            System.exit(1);
        }

        //making sure K is an integer, then non-negative
        int K = -1;
        try {
            K = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("level of analysis (k) must be a whole number");
            System.exit(1);
        }
        if (K < 0) {
            System.err.println("level of analysis (k) must be a non-negative number");
            System.exit(1);
        }

        //making sure length is an integer, then non-negative
        int length = -1;
        try {
            length = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.err.println("length of output must be a whole number");
            System.exit(1);
        }
        if (length < 0) {
            System.err.println("length must be a non-negative number");
            System.exit(1);
        }

        //level of analysis K = 0 is a special case, handle it here
        //need to open file, make sure the length is greater than K
        if (K == 0) {
            try {
                Scanner in = new Scanner(new FileReader(args[0]));
                String text = "";

                //try to get first line (if it exists - if file not empty)
                if(in.hasNext()) text = in.nextLine();

                //add the entire file to "text"
                while (in.hasNext())
                    text += " \n " + in.nextLine();

                //split the text into words by [spaces]
                String[] words = text.split(" ");

                //make sure length of input is greater than k
                if (words.length <= K)
                    throw new IOException("Input file doesn't contain more  than the specified level of analysis k = " + K);

                //output the number of characters specified
                FileWriter out = new FileWriter(args[1]);
                for (int i = 0; i < length; i++)
                    out.write(words[(int) (Math.random() * words.length)] + " "); //adding a space since all spaces were removed

                in.close();
                out.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        } else {
            //typical code for when K > 0
            try {
                TextProcessor processor = createProcessor(K);

                processor.readText(args[0]);
                processor.writeText(args[1], length);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
    }

    //copied over from other class
    public static TextProcessor createProcessor(int level) {
        return new RandomWordWriter(level);
    }

    private RandomWordWriter(int level) {
        this.level = level;
        analyzer = new HashMap<String, StringCounter>();
    }

    private HashMap<String, StringCounter> analyzer;
    private int level;

    /**
     * This method goes through the input file and populates
     * the HashMap analyzer mapping from seeds of k words to
     * a custom StringCounter class that keeps track of
     * the different words following a certain seed.
     * <p>
     * This method catches situations where the number of words is less
     * than the specified level of analysis k (throws an Exception).
     * Does not handle k = 0, however.
     *
     * @param inputFilename source text file
     * @throws IOException if file doesn't exist or is not readable
     *                     or if length of the file is smaller than
     *                     the level of analysis
     */
    public void readText(String inputFilename) throws IOException {
        Scanner in = new Scanner(new FileReader(inputFilename));
        String text = "";

        //try to get first line (if it exists - if file not empty)
        if(in.hasNext()) text = in.nextLine();

        //add the entire file to "text"
        while (in.hasNext())
            text += " \n " + in.nextLine();

        //split the text into words by [spaces]
        String[] words = text.split(" ");

        //make sure length of input is greater than k
        if (words.length <= level)
            throw new IOException("Input file doesn't contain more words than the specified level of analysis k = " + level);

        //get the first k words as the initial seed
        String seed = "";
        for (int i = 0; i < level; i++)
            seed += words[i] + " "; //there will be a space at the end!

        analyzer.put(seed, new StringCounter());
        analyzer.get(seed).addCount(words[level]);

        //goes to words.length - 1 because the last k characters have no character after them
        for (int i = level; i < words.length - 1; i++) {
            //take off the front of the seed, add one more to the back
            seed = seed.substring(seed.indexOf(" ") + 1) + words[i] + " ";

            //if first time seeing this specific seed
            if (!analyzer.containsKey(seed))
                analyzer.put(seed, new StringCounter());

            //add this situation to the analyzer
            analyzer.get(seed).addCount(words[i + 1]);
        }

        in.close();
    }

    /**
     * This method takes the HashMap populated by readText() and
     * generates "length"-number words based on the previous
     * k words (the seed).
     *
     * @param outputFilename destination text file
     * @param length         word length of text to generate (non-negative)
     * @throws IOException if file cannot be opened for writing
     */
    public void writeText(String outputFilename, int length) throws IOException {
        FileWriter out = new FileWriter(outputFilename);

        //randomly choose a beginning seed from the key-set of previous seeds
        int numOfSeeds = analyzer.keySet().size();
        String seed = analyzer.keySet().toArray()[(int) (Math.random() * numOfSeeds)].toString();

        //write out "length" characters
        for (int i = 0; i < length; i++) {
            //handle case if current seed has not been seen before
            if (!analyzer.keySet().contains(seed)) {
                //choose a new, random seed
                seed = analyzer.keySet().toArray()[(int) (Math.random() * numOfSeeds)].toString();
            }
            String str = analyzer.get(seed).generate();
            out.write(str + " ");
            //moving the seed over by a word
            seed = seed.substring(seed.indexOf(" ") + 1) + str + " ";
        }

        out.close();
    }

    /**
     * This class is mapped to by each String seed. It holds
     * information about frequency for certain words (Strings).
     * Using the data it generates random words based on
     * the previous distribution of word frequencies.
     */
    private class StringCounter {
        HashMap<String, Integer> counts;
        int total;

        public StringCounter() {
            counts = new HashMap<String, Integer>();
            total = 0;
        }

        public void addCount(String str) {
            //if first time encountering this specific character, add entry to the hashmap
            counts.putIfAbsent(str, 0);

            counts.put(str, counts.get(str) + 1);
            total++;
        }

        /**
         * This method returns a weighted-random String word
         * based on previous frequency of words
         *
         * @return a random word whose probability is based on the frequency of past data
         */
        public String generate() {
            //chooses a random element [1, total] from the hashmap to return as the next word
            int randomIndex = (int) (Math.random() * total) + 1;

            for (String str : counts.keySet()) {
                //decrease index until the nth character is reached
                randomIndex -= counts.get(str);

                if (randomIndex < 1)
                    return str;
            }
            return null;
        }
    }
}
