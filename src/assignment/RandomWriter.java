package assignment;

import java.io.*;
import java.util.*;

/*
 * CS 314H Assignment 2 - Random Writing
 *
 * Your task is to implement this RandomWriter class
 */
public class RandomWriter implements TextProcessor {

    public static void main(String[] args) {

        //making sure the number of arguments is correct
        if (args.length != 4) {
            System.err.println("Incorrect number of arguments. Proper usage:" +
                    "\njava RandomWriter [source file] [result file] [level of analysis, k] [length of output]\n" +
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

                //try to get the first line, if it exists (file not empty)
                if(in.hasNext()) text = in.nextLine();

                //add the rest of the file to "text"
                while (in.hasNext())
                    text += "\n" + in.nextLine();

                //make sure length of input is greater than k
                if (text.length() <= K)
                    throw new IOException("Input file doesn't contain more characters than the specified level of analysis k = " + K);

                //output the number of characters specified
                FileWriter out = new FileWriter(args[1]);
                for (int i = 0; i < length; i++)
                    out.write(text.charAt((int) (Math.random() * text.length())));

                in.close();
                out.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        } else {
            //typical code for when K > 0
            try {
                TextProcessor machine = createProcessor(K);

                machine.readText(args[0]);
                machine.writeText(args[1], length);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
    }

    // Unless you need extra logic here, you might not have to touch this method
    public static TextProcessor createProcessor(int level) {
        return new RandomWriter(level);
    }

    private RandomWriter(int level) {
        this.level = level;
        analyzer = new HashMap<String, CharacterCounter>();
    }

    private HashMap<String, CharacterCounter> analyzer;
    private int level;

    /**
     * This method goes through the input file and populates
     * the HashMap analyzer mapping from seeds of length k to
     * a custom CharacterCounter class that keeps track of
     * the different characters following a certain seed.
     * <p>
     * This method catches situations where the length is less
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

        //try to get the first line, if it exists (file not empty)
        if(in.hasNext()) text = in.nextLine();

        //add the rest of the file to "text"
        while (in.hasNext())
            text += "\n" + in.nextLine();

        //make sure length of input is greater than k
        if (text.length() <= level)
            throw new IOException("Input file doesn't contain more characters than the specified level of analysis k = " + level);

        char[] charText = text.toCharArray();
        String seed = text.substring(0, level);

        //add the initial seed and first data point to the hash-map
        analyzer.put(seed, new CharacterCounter());
        analyzer.get(seed).addCount(charText[level]);

        //goes to charText.length - 1 because the last k characters have no character after them
        for (int i = level; i < charText.length - 1; i++) {
            //take off the front of the seed, add one more to the back
            seed = seed.substring(1) + charText[i];

            //if first time seeing this specific seed
            if (!analyzer.containsKey(seed))
                analyzer.put(seed, new CharacterCounter());

            //add this situation to the analyzer
            analyzer.get(seed).addCount(charText[i + 1]);
        }

        in.close();
    }

    /**
     * This method takes the HashMap populated by readText() and
     * generates "length"-number characters based on the previous
     * k characters (the seed).
     *
     * @param outputFilename destination text file
     * @param length         length of text to generate (non-negative)
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
            char c = analyzer.get(seed).generate();
            out.write(c);
            //moving the seed down by a character
            seed = seed.substring(1) + c;
        }

        out.close();
    }

    /**
     * This class is mapped to by each String seed. It holds
     * information about frequency for certain characters.
     * Using the data it generates random characters based on
     * the previous distribution of character frequencies.
     */
    private class CharacterCounter {
        HashMap<Character, Integer> counts;
        int total;

        public CharacterCounter() {
            counts = new HashMap<Character, Integer>();
            total = 0;
        }

        public void addCount(char c) {
            //if first time encountering this specific character, add entry to the hashmap
            counts.putIfAbsent(c, 0);

            counts.put(c, counts.get(c) + 1);
            total++;
        }

        /**
         * This method returns a weighted-random character
         * based on previous frequency of characters
         *
         * @return a random character whose probability is based on the frequency of past data
         */
        public char generate() {
            //chooses a random element [1, total] from the hashmap to return as the next character
            int randomIndex = (int) (Math.random() * total) + 1;

            for (Character c : counts.keySet()) {
                //decrease index until the nth character is reached
                randomIndex -= counts.get(c);

                if (randomIndex < 1)
                    return c;
            }
            return 0;
        }
    }
}
