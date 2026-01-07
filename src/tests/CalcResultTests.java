package tests;

import app.Main;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

class CalcResultTests {

    public static void main(String[] args) {
        test_ok_result_74();
        test_unknown_variable();
        test_division_by_zero();
    }

    static void test_ok_result_74() {
        Main.Calc c = new Main.Calc(buildExpression());

        c.setVariable("x1", 6);
        c.setVariable("y", 11);
        c.setVariable("z2", 4);
        c.setVariable("a", 3);
        c.setVariable("b", 10);
        c.setVariable("c", 3);

        String out = captureOut(c);

        assertContains(out, "Result = 74", "test_ok_result_74");
    }

    static void test_unknown_variable() {
        Main.Calc c = new Main.Calc(buildExpression());
        // НЕ задаём x1
        c.setVariable("y", 11);
        c.setVariable("z2", 4);
        c.setVariable("a", 3);
        c.setVariable("b", 10);
        c.setVariable("c", 3);

        try {
            captureOut(c);
            fail("test_unknown_variable FAILED: expected RuntimeException, but none thrown");
        } catch (RuntimeException e) {
            // token_type() кидает RuntimeException("Unknown var: ...")
            assertContains(String.valueOf(e.getMessage()), "Unknown var", "test_unknown_variable");
        }
    }

    static void test_division_by_zero() {
        Main.Calc c = new Main.Calc(buildExpression());

        c.setVariable("x1", 6);
        c.setVariable("y", 11);
        c.setVariable("z2", 4);
        c.setVariable("a", 3);
        c.setVariable("b", 10);
        c.setVariable("c", -2); // c+2=0 -> division by zero

        String out = captureOut(c);

        assertContains(out, "division by zero", "test_division_by_zero");
        assertNotContains(out, "Result =", "test_division_by_zero (should not print result)");
    }

    // -------- expression as Map<String, String[]> --------
    static Map<String, String[]> buildExpression() {
        Map<String, String[]> e = new HashMap<>();

        // left: ((12+x1)*(y-5)/3)
        e.put("var1",  new String[]{"12", "+", "x1"});
        e.put("var2",  new String[]{"y",  "-", "5"});
        e.put("var3",  new String[]{"var1", "*", "var2"});
        e.put("var4",  new String[]{"var3", "/", "3"});

        // right: (z2*(7+a) - b/(c+2))
        e.put("var5",  new String[]{"7",  "+", "a"});
        e.put("var6",  new String[]{"z2", "*", "var5"});
        e.put("var7",  new String[]{"c",  "+", "2"});
        e.put("var8",  new String[]{"b",  "/", "var7"});
        e.put("var9",  new String[]{"var6", "-", "var8"});

        // whole: left + right
        e.put("var10", new String[]{"var4", "+", "var9"});

        return e;
    }

    // -------- capture stdout --------
    static String captureOut(Main.Calc c) {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            c.calc_result();
        } finally {
            System.setOut(oldOut);
        }
        return baos.toString();
    }

    // -------- asserts --------
    static void assertContains(String haystack, String needle, String testName) {
        if (haystack == null) haystack = "";
        if (!haystack.contains(needle)) {
            fail(testName + " FAILED:\nExpected to contain: " + needle + "\nActual:\n" + haystack);
        }
        System.out.println("PASS: " + testName);
    }

    static void assertNotContains(String haystack, String needle, String testName) {
        if (haystack == null) haystack = "";
        if (haystack.contains(needle)) {
            fail(testName + " FAILED:\nExpected NOT to contain: " + needle + "\nActual:\n" + haystack);
        }
        System.out.println("PASS: " + testName);
    }

    static void fail(String msg) {
        throw new RuntimeException(msg);
    }
}
