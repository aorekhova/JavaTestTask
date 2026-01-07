package tests;

import app.Main.AstPrinter;   // ✅ ВАЖНО
import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

class PrinterTest {

    public static void main(String[] args) {
        test_printer_simple_tree();
        System.out.println("PRINTER TEST PASSED ✅");
    }

    static void test_printer_simple_tree() {
        Map<String, String[]> tree = new HashMap<>();
        tree.put("var1", new String[]{"12", "+", "x"});
        tree.put("var2", new String[]{"var1", "*", "23"});

        AstPrinter printer = new AstPrinter(tree, "var2"); // ✅ работает

        String out = captureOut(printer);

        assertContains(out, "*", "operator *");
        assertContains(out, "+", "operator +");
        assertContains(out, "12", "leaf 12");
        assertContains(out, "x", "leaf x");
        assertContains(out, "23", "leaf 23");
    }

    static String captureOut(AstPrinter printer) {
        PrintStream oldOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        try {
            printer.printer();
        } finally {
            System.setOut(oldOut);
        }
        return baos.toString();
    }

    static void assertContains(String text, String expected, String name) {
        if (text == null || !text.contains(expected)) {
            throw new RuntimeException("Missing " + name + "\nOutput was:\n" + text);
        }
    }
}
