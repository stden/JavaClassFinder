import org.junit.Assert;
import org.junit.Test;
import util.FullName;

import java.io.IOException;
import java.util.stream.Stream;

public class ClassFinderTest extends Assert {
    /**
     * Search pattern `<pattern>` must include class name camelcase upper case letters
     * in the right order and it may contain lower case letters to narrow down the search results,
     * for example `'FB'`, `'FoBa'` and `'FBar'` searches must all match
     * `a.b.FooBarBaz` and `c.d.FooBar` classes.
     */
    @Test
    public void testSearchPatternMustIncludeClassNameUpperCaseLettersInRightOrder() {
        ClassFinder FB = new ClassFinder("FB");
        assertEquals("*F*B*", FB.classWildCard);
        ClassFinder FoBa = new ClassFinder("FoBa");
        ClassFinder FBar = new ClassFinder("FBar");
        assertEquals("*F*Bar*", FBar.classWildCard);
        assertTrue(FB.match("a.b.FooBarBaz"));
        assertTrue(FoBa.match("a.b.FooBarBaz"));
        assertTrue(FBar.match("a.b.FooBarBaz"));
        assertTrue(FB.match("c.d.FooBar"));
        assertTrue(FoBa.match("c.d.FooBar"));
        assertTrue(FBar.match("c.d.FooBar"));

        ClassFinder HaMa = new ClassFinder("HaMa");
        assertTrue(HaMa.match("HashMap"));
        assertTrue(HaMa.match("java.util.HashMap"));
        assertTrue(HaMa.match("ConcurrentHashMap"));
        assertTrue(HaMa.match("java.util.concurrent.ConcurrentHashMap"));

        ClassFinder cf = new ClassFinder("apac.util.Hash");
        assertTrue(cf.match("apache.xml.utils.HashMap"));
    }

    @Test
    public void testMissedLettersInPattern() {
        ClassFinder fb = new ClassFinder("FilBuider");
        assertFalse(fb.match("FileBuilder"));
        assertTrue(fb.match("FileBuider"));

        ClassFinder hm = new ClassFinder("ConcHhMap");
        assertFalse(hm.match("ConcurrentHashMap"));
    }

    @Test
    public void testPatternContainsFirstLettersOfPackages() {
        ClassFinder cf = new ClassFinder("a.u.Hash");
        assertTrue(cf.match("apache.xml.utils.HelperHash"));
        assertFalse(cf.match("xml.utils.HelperHash"));
    }

    /**
     * Upper case letters written in the wrong order will not find any results, for example
     * `'BF'` will not find `c.d.FooBar`.
     */
    @Test
    public void testUpperCaseLettersInWrongOrder() {
        ClassFinder BF = new ClassFinder("BF");
        assertFalse(BF.match("c.d.FooBar"));
        assertTrue(BF.match("c.d.BF"));
        assertTrue(BF.match("c.d.SomeBarFoo"));
    }

    /**
     * If the search pattern consists of only lower case characters then the search becomes
     * case insensitive (`'fbb'` finds `FooBarBaz` but `'fBb'` will not).
     */
    @Test
    public void testCaseSensitive() {
        ClassFinder fbb = new ClassFinder("fbb");
        assertFalse(fbb.caseSensitive);
        assertTrue(fbb.match("FooBarBaz"));

        ClassFinder fBb = new ClassFinder("fBb");
        assertTrue(fBb.caseSensitive);
        assertFalse(fBb.match("FooBarBaz"));

        ClassFinder list = new ClassFinder("list");
        assertTrue(list.match("List"));

        ClassFinder hama = new ClassFinder("hama");
        assertFalse(hama.caseSensitive);
        assertTrue(hama.match("HashMap"));
    }

    /**
     * If the search pattern ends with a space `' '` then the last word in the pattern must
     * also be the last word of the found class name (`'FBar '` finds `FooBar` but not `FooBarBaz`).
     */
    @Test
    public void testPatternEndsWithSpaceLastWordMustMatch() {
        ClassFinder space = new ClassFinder("FBar ");
        assertEquals("*F*Bar", space.classWildCard);
        assertEquals("*", space.packageWildCard);
        assertTrue(space.match("FooBar"));
        assertFalse(space.match("FooBarBaz"));
        assertTrue(space.match("java.util.FooBar"));
    }

    /**
     * The search pattern may include wildcard characters `'*'` which match missing letters
     * (`'B*rBaz'` finds `FooBarBaz` but `BrBaz` does not).
     */
    @Test
    public void testWildcardAsteriskMatchMissingLetters() {
        ClassFinder oneAsterisk = new ClassFinder("B*rBaz");
        assertTrue(oneAsterisk.match("FooBarBaz"));
        assertFalse(oneAsterisk.match("BrBaz"));

        ClassFinder twoAsterisk = new ClassFinder("B*rB*z");
        assertTrue(twoAsterisk.match("FooBarBaz"));
        assertFalse(twoAsterisk.match("BarBz"));

        ClassFinder w1 = new ClassFinder("u*l.Hash");
        assertTrue(w1.match("util.Hash"));
        assertTrue(w1.match("java.util.Hash"));
        assertFalse(w1.match("til.Hash"));
        assertFalse(w1.match("ul.Hash"));

        ClassFinder w2 = new ClassFinder("f*tRCl");
        assertTrue(w2.match("firstRichClient"));
    }

    @Test
    public void testIfWrongPatternFormatThrowsException() {
        String[] wrongPatterns = {"", " "};
        for (String pattern : wrongPatterns) {
            try {
                new ClassFinder(pattern);
                fail("Expected IllegalArgumentException for \"" + pattern + "\"");
            } catch (IllegalArgumentException ex) {
                assertEquals("Pattern format: '<pattern>' \"" + pattern + "\"", ex.getMessage());
            }
        }
    }

    @Test
    public void testSplitFullClassName() {
        FullName fn = new FullName("java.util.List");
        assertEquals("java.util", fn.packageName);
        assertEquals("List", fn.className);
        assertEquals("java.util.List", fn.toString());
    }

    @Test
    public void testSplitEmptyClassName() {
        FullName empty = new FullName("");
        assertEquals("", empty.packageName);
        assertEquals("", empty.className);
        assertEquals("", empty.toString());
    }

    @Test
    public void testSplitClassNameWithoutPackage() {
        FullName r2 = new FullName("MyClass");
        assertEquals("", r2.packageName);
        assertEquals("MyClass", r2.className);
        assertEquals("MyClass", r2.toString());
    }

    /**
     * The found class names must be sorted in alphabetical order ignoring package names
     * (package names must still be included in the output).
     */
    @Test
    public void testClassNamesMustBeSortedInAlphabeticalOrderIgnoringPackageNames() {
        Stream<String> list = Stream.of("java.util.List", "a.ListB", "jdk.ListAdapter");
        // Sort items
        Stream<FullName> sorted = list.map(FullName::new).sorted();
        // Convert to array
        String[] results = sorted.map(FullName::toString).toArray(String[]::new);
        // Verify array
        assertEquals(3, results.length);
        assertEquals("java.util.List", results[0]);
        assertEquals("jdk.ListAdapter", results[1]);
        assertEquals("a.ListB", results[2]);
    }

    /**
     * "Smoke" test for "main" call
     */
    @Test
    public void testCommandLineCall() throws IOException {
        ClassFinder.main("classes.txt", "FooBar");
        ClassFinder.main("classes.txt", " ");
        ClassFinder.main("classes.txt", "");
        ClassFinder.main("", "");
    }

    @Test
    public void testWithPackageName() {
        ClassFinder cf = new ClassFinder("util.List");
        assertTrue(cf.match("java.util.List"));
    }

    @Test
    public void testPackageNameInPattern() {
        ClassFinder utilHashMap = new ClassFinder("util.HashMap");
        assertEquals("*Hash*Map*", utilHashMap.classWildCard);
        assertTrue(utilHashMap.match("java.util.HashMap"));

        ClassFinder utilHashMapNeg = new ClassFinder("utilHashMap");
        assertFalse(utilHashMapNeg.match("java.util.HashMap"));

        ClassFinder utilHashMapWrongPackage = new ClassFinder("util2.HashMap");
        assertFalse(utilHashMapWrongPackage.match("java.util.HashMap"));
        assertTrue(utilHashMapWrongPackage.match("java.util2.HashMap"));
    }

    @Test
    public void testWildcardMatching() {
        assertTrue(ClassFinder.wildcard("g*ks", "geeks"));
        assertTrue(ClassFinder.wildcard("geeks*", "geeksforgeeks"));
        assertFalse("No because 'k' is not in second", ClassFinder.wildcard("g*k", "gee"));
        assertFalse("No because 't' is not in first", ClassFinder.wildcard("*pqrs", "pqrst"));
        assertTrue(ClassFinder.wildcard("abc*bcd", "abcdhghgbcd"));
        assertFalse("No because second must have 2 instances of 'c'",
                ClassFinder.wildcard("abc*c?d", "abcd"));
        assertTrue(ClassFinder.wildcard("*c*d", "abcd"));
        assertTrue(ClassFinder.wildcard("*bc*d", "abcd"));
        assertTrue(ClassFinder.wildcard("*c****************************************d",
                "abbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbcd"));
    }

    @Test(timeout = 1000)
    public void testWildcardsPerformance() {
        String wildCardSegment = "*aabb***aa**a******aa*";
        String textSegment = "abbabbbaabaaabbbbbabbabbabbbabbaaabbbababbabaaabbab";

        assertTrue(ClassFinder.wildcard(wildCardSegment, textSegment));

        StringBuilder wildCard = new StringBuilder();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            wildCard.append(wildCardSegment);
            text.append(textSegment);
        }
        assertTrue(ClassFinder.wildcard(wildCard.toString(), text.toString()));
    }

}
