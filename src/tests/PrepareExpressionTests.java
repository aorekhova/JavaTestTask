package tests;

import app.Main;
import java.util.*;

class PrepareExpressionManyTests {

    public static void main(String[] args) {

        ok("1+2");
        ok("12 + x");
        ok("(12 + x) * 23 + y");
        ok("((12 + x1) * (y - 5) / 3) + (z2 * (7 + a) - b / (c + 2))");
        ok("  12+ x1*( y-5 ) ");
        ok("x1 + y2 * 3");
        ok("(x1 + y2) * 3");
        ok("((1+2)*3)");
        ok("a1+b2-c3*d4/5");

        err("12 + $x", "Expression incorrect: unexpected symbol");
        err("+12", "Expression incorrect: starts with operator");
        err("12 + * 3", "Expression incorrect: two operators");
        err("12 ++ 3", "Expression incorrect: two operators");
        err("(12 + x) *", "Expression incorrect: ends with operator");
        err("(12 + x", "Expression incorrect: unbalanced brackets");
        err("12 + x)", "Expression incorrect: ')' before '('");
        err(")12 + x(", "Expression incorrect: ')' before '('");
        err("() + 1", "Expression incorrect: empty brackets");
        err("(())", "Expression incorrect: empty brackets");      // inner empty
        err("x(1+2)", "Expression incorrect: brackets after letter");
        err("12(3+4)", "Expression incorrect: brackets after digit");
        err("(1 + )2", "Expression incorrect: bracket after operation");
        err("12x + 1", "Expression incorrect: number followed by letter");
        err("1a", "Expression incorrect: number followed by letter");

    }


    static Map<Character, Integer> priorities() {
        Map<Character, Integer> p = new HashMap<>();
        p.put('+', 1); p.put('-', 1);
        p.put('*', 2); p.put('/', 2);
        p.put('(', 3); p.put(')', 3);
        return p;
    }

    static void ok(String expr) {
        try {
            Main.prepare_expression(expr, priorities());
            System.out.println("OK  : " + expr);
        } catch (Exception e) {
            throw new RuntimeException("OK FAILED for: " + expr + "\n" + e);
        }
    }

    static void err(String expr, String expectedMessage) {
        try {
            Main.prepare_expression(expr, priorities());
            throw new RuntimeException("ERR FAILED (no exception) for: " + expr);
        } catch (IllegalArgumentException e) {
            if (!expectedMessage.equals(e.getMessage())) {
                throw new RuntimeException(
                        "ERR FAILED (wrong message) for: " + expr +
                                "\nExpected: " + expectedMessage +
                                "\nActual  : " + e.getMessage()
                );
            }
            System.out.println("ERR : " + expr + " -> " + e.getMessage());
        }
    }
}
