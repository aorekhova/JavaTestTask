package app;

import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Map<Character, Integer> priorities = new HashMap<>();
        priorities.put('+', 1); priorities.put('-', 1);
        priorities.put('*', 2); priorities.put('/', 2);
        priorities.put('(', 3); priorities.put(')', 3);

        System.out.println("Enter expression (example: (12 + x) * 23 + y):");
        String expression = scanner.nextLine().trim();

        PrepareResult prepareTokens;
        try {
            prepareTokens = prepare_expression(expression, priorities);
        } catch (IllegalArgumentException e) {
            System.out.println("ERROR: " + e.getMessage());
            return;
        }

        FinalData finalExp = SortingExp(prepareTokens.tokens, prepareTokens.priorityMap);

        System.out.println("\nRandom init + first calculation:");
        finalExp.calc.initRandomAndCalc();

        System.out.println("\nCommands:");
        System.out.println("  print          - print AST");
        System.out.println("  calc           - calculate expression");
        System.out.println("  vars           - show variables");
        System.out.println("  x1 = 20        - set variable value");
        System.out.println("  exit           - quit");

        while (true) {
            System.out.print("\n> ");
            String line = scanner.nextLine().trim();

            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Bye!");
                break;
            }

            if (line.equalsIgnoreCase("print")) {
                finalExp.tree.printer();
                continue;
            }

            if (line.equalsIgnoreCase("calc")) {
                try {
                    finalExp.calc.calc_result();
                } catch (RuntimeException e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
                continue;
            }

            if (line.equalsIgnoreCase("vars")) {
                System.out.println(finalExp.calc.getVariables());
                continue;
            }

            if (line.contains("=")) {
                String[] parts = line.split("=");
                if (parts.length != 2) {
                    System.out.println("ERROR: use format: x = 20");
                    continue;
                }

                String name = parts[0].trim();
                String valueStr = parts[1].trim();

                if (!name.matches("[A-Za-z][A-Za-z0-9]*")) {
                    System.out.println("ERROR: invalid variable name");
                    continue;
                }

                if (!valueStr.matches("-?\\d+")) {
                    System.out.println("ERROR: invalid integer value");
                    continue;
                }

                int value = Integer.parseInt(valueStr);
                finalExp.calc.setVariable(name, value);
                System.out.println(name + " = " + value);
                continue;
            }

            System.out.println("ERROR: unknown command");
        }
    }

    // prepare expression
    public static PrepareResult prepare_expression(String exp, Map<Character, Integer> priorities) {
        List<String[]> tokens = new ArrayList<>();
        Map<Integer, List<Integer>> priority_map = new HashMap<>();

        String lastType = "none";
        StringBuilder var = new StringBuilder();
        int balance = 0;
        int level_priority = 0;

        for (int i = 0; i < exp.length(); i++) {
            char symbol = exp.charAt(i);

            if (Character.isWhitespace(symbol)) continue;

            String type = detect_symbol_type(symbol, priorities); // indify type
            if (type.equals("nun")) {
                throw new IllegalArgumentException("Expression incorrect: unexpected symbol");
            }

            // bracket
            if (type.equals("bracket")) {
                if (!var.isEmpty()) {
                    tokens.add(new String[]{var.toString(), lastType});
                    var.setLength(0);
                }

                if (symbol == '(') {
                    balance++; level_priority += 10;
                }
                if (symbol == ')') {
                    balance--; level_priority -= 10;
                }

                // error with brackets
                if (balance < 0) {
                    throw new IllegalArgumentException("Expression incorrect: ')' before '('");
                }
                if ((symbol == ')') && (exp.charAt(i - 1) == '(')) {
                    throw new IllegalArgumentException("Expression incorrect: empty brackets");
                }
                if (lastType.equals("letter") && symbol == '(') {
                    throw new IllegalArgumentException("Expression incorrect: brackets after letter");
                }
                if (lastType.equals("digit") && symbol == '(') {
                    throw new IllegalArgumentException("Expression incorrect: brackets after digit");
                }
                if (lastType.equals("signs") && symbol == ')') {
                    throw new IllegalArgumentException("Expression incorrect: bracket after operation");
                }

                tokens.add(new String[]{String.valueOf(symbol), type});
                lastType = type;
                continue;
            }

            // signs
            if (type.equals("signs")) {
                // error with incorrect start
                if (tokens.isEmpty() && var.isEmpty()) {
                    throw new IllegalArgumentException("Expression incorrect: starts with operator");
                }
                // error with 2 signs
                if (lastType.equals("signs")) {
                    throw new IllegalArgumentException("Expression incorrect: two operators");
                }

                if (!var.isEmpty()) {
                    tokens.add(new String[]{var.toString(), lastType});
                    var.setLength(0);
                }

                tokens.add(new String[]{String.valueOf(symbol), type});
                int token_index = tokens.size() - 1;

                lastType = type;

                int sign_priority = level_priority + priorities.get(symbol);
                priority_map.computeIfAbsent(sign_priority, k -> new ArrayList<>()).add(token_index);

                continue;
            }

            // digit and letters
            if (lastType.equals("digit") && type.equals("letter")) {
                throw new IllegalArgumentException("Expression incorrect: number followed by letter");
            }

            if (var.isEmpty()) {
                lastType = type;
            }

            var.append(symbol);
        }

        // last token
        if (!var.isEmpty()) {
            tokens.add(new String[]{var.toString(), lastType});
        }

        if (balance != 0) {
            throw new IllegalArgumentException("Expression incorrect: unbalanced brackets");
        }
        if (lastType.equals("signs")) {
            throw new IllegalArgumentException("Expression incorrect: ends with operator");
        }


        return new PrepareResult(tokens, priority_map);
    }

    static String detect_symbol_type(char symbol, Map<Character, Integer> signs) {
        if ((symbol >= 'A' && symbol <= 'Z') || (symbol >= 'a' && symbol <= 'z')) {
            return "letter";
        } else if (symbol >= '0' && symbol <= '9') {
            return "digit";
        } else if (symbol == '(' || symbol == ')') {
            return "bracket";
        } else if (signs.get(symbol) != null) {
            return "signs";
        }

        return "nun";
    }

    public static class PrepareResult {
        public final List<String[]> tokens;
        public final Map<Integer, List<Integer>> priorityMap;

        PrepareResult(List<String[]> tokens, Map<Integer, List<Integer>> priorityMap) {
            this.tokens = tokens;
            this.priorityMap = priorityMap;
        }
    }


    // sort expression
    static FinalData SortingExp(List<String[]> tokens, Map<Integer, List<Integer>> priorityMap) {
        Map<Integer, List<Integer>> sorting_priority = new TreeMap<>(Collections.reverseOrder());
        sorting_priority.putAll(priorityMap);

        List<String[]> sort_expression = new ArrayList<>();
        Map<String, String[]> tree_map = new HashMap<>();

        int count_exp = 0;
        String var_name = "";
        for (Map.Entry<Integer, List<Integer>> entry : sorting_priority.entrySet()) {
            List<Integer> indexes = entry.getValue();

            for (Integer idx : indexes) {
                count_exp ++;
                var_name = "var" + count_exp;
                String sign = tokens.get(idx)[0];
                // int priority = entry.getKey();

                String first_sym = tokens.get(idx - 1)[0];
                String second_sym = tokens.get(idx + 1)[0];


                int[] range = findRange(tokens, idx);
                replaceRange(tokens, range[0], range[1], var_name);


                String[] list = {first_sym, sign, second_sym};
                tree_map.put(var_name, list);
                sort_expression.add(list);
            }

        }

        return new FinalData(tree_map, var_name);
    }

    static int[] findRange(List<String[]> tokens, int startIdx) {
        // int n = tokens.size();
        int start_id = 0; int finish_id = 0;
        String start_id_trigger = ""; String finish_id_trigger = "";
        boolean hasSign = false;

        for (int i = startIdx - 1; i >= 0; i--){
            if (tokens.get(i)[1].equals("bracket")) {
                start_id = i; start_id_trigger = "bracket";
                break;
            }
            if (tokens.get(i)[1].equals("signs")) {
                start_id = i + 1; start_id_trigger = "signs";
                hasSign = true;
                break;
            }
            start_id = i;
        }

        for (int i = startIdx + 1; i < tokens.size(); i++){
            if (tokens.get(i)[1].equals("bracket")) {
                if (hasSign){
                    finish_id = i - 1;
                } else {
                    finish_id = i;
                }
                break;
            }
            if (tokens.get(i)[1].equals("signs")) {
                finish_id = i - 1;
                hasSign = true;
                break;
            }
            finish_id = i;
        }

        if (hasSign) {
            if (start_id_trigger.equals("bracket")) {
                start_id ++;
            }
        }

        return new int[]{start_id, finish_id};
    }

    static void replaceRange(List<String[]> tokens, int left, int right, String varName) {
        for (int i = left; i <= right; i++) {
            tokens.get(i)[0] = varName;
            tokens.get(i)[1] = "letter";
        }
    }

    static class FinalData {
        final AstPrinter tree;
        final Calc calc;

        FinalData(Map<String, String[]> tree_map, String tree_root) {
            this.tree = new AstPrinter(tree_map, tree_root);
            this.calc = new Calc(tree_map);
        }
    }


    // tree printer
    public static class AstPrinter {

        private final Map<String, String[]> tree;
        private final String root;

        public AstPrinter(Map<String, String[]> tree, String root) {
            this.tree = tree;
            this.root = root;
        }

        public void printer() {
            printNode(root, "", true);
        }

        private void printNode(String node, String indent, boolean last) {

            System.out.print(indent);
            System.out.print(last ? "└── " : "├── ");

            if (!tree.containsKey(node)) {
                System.out.println(node);
                return;
            }

            String[] t = tree.get(node); // [left, op, right]
            if (t == null || t.length < 3) {
                System.out.println("(bad node)");
                return;
            }

            System.out.println(t[1]); // operation

            String newIndent = indent + (last ? "    " : "│   ");
            printNode(t[0], newIndent, false);
            printNode(t[2], newIndent, true);
        }
    }


    // calc exp
    public static class Calc {
        private final Map<String, Integer> variables = new HashMap<>();
        private final Map<String, String[]> expression;

        public Calc(Map<String, String[]> expression) {
            this.expression = expression;
        }

        public void setVariable(String name, int value) {
            variables.put(name, value);
        }

        public Map<String, Integer> getVariables() {
            return new HashMap<>(variables);
        }

        public void initRandomAndCalc() {
            Random r = new Random();

            for (String[] exp : expression.values()) {
                initVar(exp[0], r);
                initVar(exp[2], r);
            }

            System.out.println("Variables:");
            for (Map.Entry<String, Integer> e : variables.entrySet()) {
                System.out.println("  " + e.getKey() + " = " + e.getValue());
            }


            calc_result();
        }

        private void initVar(String token, Random r) {
            token = token.trim();

            if (token.matches("-?\\d+")) return;      // число
            if (expression.containsKey(token)) return; // var1, var2...
            if (!variables.containsKey(token)) {
                variables.put(token, 1 + r.nextInt(100)); // случайное число
            }
        }

        public void calc_result() {
            int count = expression.size();
            Map<String, Integer> step_result = new HashMap<>();

            for (int i = 1; i <= count; i++) {
                String key_name = "var" + i;
                String[] exp = expression.get(key_name);

                int left = token_type(exp[0], step_result);
                int right = token_type(exp[2], step_result);

                String op = exp[1];
                switch (op) {
                    case "+" -> step_result.put(key_name, left + right);
                    case "-" -> step_result.put(key_name, left - right);
                    case "*" -> step_result.put(key_name, left * right);
                    case "/" -> {
                        if (right == 0) {
                            System.out.println("Error: division by zero in " + key_name);
                            return;
                        }
                        step_result.put(key_name, left / right);
                    }
                }
            }

            System.out.println("Result = " + step_result.get("var" + count));
        }

        private int token_type(String token, Map<String, Integer> step_result) {
            token = token.trim();

            // number
            if (token.matches("-?\\d+")) {
                return Integer.parseInt(token);
            }

            // already computed varK
            if (step_result.containsKey(token)) {
                return step_result.get(token);
            }

            // input var like x, y
            if (variables.containsKey(token)) {
                return variables.get(token);
            }

            throw new RuntimeException("Unknown var: " + token);
        }
    }

}

