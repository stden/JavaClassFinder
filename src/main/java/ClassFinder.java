import util.FullName;

import java.io.IOException;

import static java.lang.System.out;
import static java.nio.file.Files.readAllLines;
import static java.nio.file.Paths.get;

public class ClassFinder {
    final boolean caseSensitive;
    final String classWildCard;
    final String packageWildCard;

    public ClassFinder(String pattern) {
        if (pattern.length() < 1 || pattern.equals(" "))
            throw new IllegalArgumentException("Pattern format: '<pattern>' \"" + pattern + "\"");
        caseSensitive = isCamelCase(pattern);

        FullName fn = new FullName(pattern);

        packageWildCard = toWildCard(fn.packageName);
        classWildCard = toWildCard(fn.className);
    }

    private static boolean isCamelCase(String pattern) {
        for (char c : pattern.toCharArray()) {
            if (c >= 'A' && c <= 'Z')
                return true;
        }
        return false;
    }

    public static void main(String... args) throws IOException {
        if (args.length != 2) {
            System.out.println("Usage: ./class-finder-java classes.txt 'FooBar'");
            return;
        }
        try {
            ClassFinder finder = new ClassFinder(args[1]);
            readAllLines(get(args[0])).stream().
                    filter(finder::match).
                    map(FullName::new).
                    sorted().forEach(out::println);
        } catch (Exception ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    /**
     * @param wc   wildcard with * ?
     * @param text text
     * @return match
     */
    static boolean wildcard(String wc, String text) {
        int wcStarPos = -1; // Last star position in wildcard
        int textPos = -1; // Position in text

        int j = 0;
        for (int i = 0; i < text.length(); ) {
            char c = text.charAt(i);
            if (j < wc.length() && (wc.charAt(j) == '?' || wc.charAt(j) == c)) {
                ++i;
                ++j;
            } else if (j < wc.length() && wc.charAt(j) == '*') {
                wcStarPos = j;
                textPos = i;
                j++;
            } else if (wcStarPos != -1) {
                j = wcStarPos + 1;
                i = textPos + 1;
                textPos++;
            } else {
                return false;
            }
        }
        // Skip all stars
        while (j < wc.length() && wc.charAt(j) == '*') {
            ++j;
        }
        return j == wc.length();
    }

    private String toWildCard(String pattern) {
        StringBuilder wc = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char p = pattern.charAt(i);
            if ((p >= 'A' && p <= 'Z') || (p == '.') || !caseSensitive) {
                wc.append("*");
            }
            if (wc.length() == 0)
                wc.append("*");
            if (p == '*')
                wc.append('?');
            if (p != ' ')
                wc.append(p);
        }
        if (!pattern.endsWith(" "))
            wc.append("*");
        return wc.toString();
    }

    /**
     * @param fullClassName class name
     * @return true in match the pattern
     */
    boolean match(String fullClassName) {
        if (!caseSensitive)
            fullClassName = fullClassName.toLowerCase();

        FullName q = new FullName(fullClassName);
        return wildcard(packageWildCard, q.packageName) &&
                wildcard(classWildCard, q.className);
    }
}
