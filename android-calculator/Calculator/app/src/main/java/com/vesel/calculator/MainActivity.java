package com.vesel.calculator;

import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import net.objecthunter.exp4j.ExpressionBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;

public class MainActivity extends AppCompatActivity {

    // Symbols shown on the buttons. exp4j needs * / - instead.
    // MINUS is not the keyboard hyphen, it only looks like it.
    private static final char MULT = '×';
    private static final char DIV = '÷';
    private static final char MINUS = '−';
    // What the user has typed so far, as shown on the screen
    private final StringBuilder expr = new StringBuilder();
    private TextView tvExpression;
    private TextView tvResult;
    // How many brackets are still open
    private int openParens = 0;
    // True right after '=', the next key decides if we start over
    private boolean justEvaluated = false;

    // The four operators as they look on the buttons
    private static boolean isOperator(char c) {
        return c == '+' || c == MINUS || c == MULT || c == DIV;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Top line shows what is typed, bottom line shows the result while typing
        tvExpression = findViewById(R.id.tvExpression);
        tvResult = findViewById(R.id.tvResult);

        GridLayout keypad = findViewById(R.id.keypad);

        // Same listener for all keys
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Object tag = v.getTag();
                if (tag != null) {
                    onKey(tag.toString());
                }
            }
        };

        // Attach it to every key of the keypad
        for (int i = 0; i < keypad.getChildCount(); i++) {
            keypad.getChildAt(i).setOnClickListener(listener);
        }

        render();
    }

    // Every key sends its tag here, digits, operators, dot and % go to append()
    private void onKey(String tag) {
        switch (tag) {
            case "clear":
                expr.setLength(0);
                openParens = 0;
                justEvaluated = false;
                break;
            case "del":
                delete();
                break;
            case "eq":
                evaluate();
                break;
            case "(":
                openParen();
                break;
            case ")":
                closeParen();
                break;
            default:
                append(tag);
        }
        render();
    }

    private void append(String key) {
        char c = key.charAt(0);

        if (justEvaluated) {
            // After '=' a digit starts over, an operator keeps the result.
            if (Character.isDigit(c) || c == '.') {
                expr.setLength(0);
                openParens = 0;
            }
            justEvaluated = false;
        }

        // An operator can't be first or right after '('. A second operator replaces the first one
        if (isOperator(c)) {
            if (expr.length() == 0) {
                return;
            }
            char last = expr.charAt(expr.length() - 1);
            if (last == '(') {
                return;
            }
            if (isOperator(last)) {
                expr.setCharAt(expr.length() - 1, c);
                return;
            }
        }

        // Only one dot per number, and a dot with no number before it becomes "0."
        if (c == '.') {
            if (currentNumberHasDot()) {
                return;
            }
            if (expr.length() == 0 || !Character.isDigit(expr.charAt(expr.length() - 1))) {
                expr.append('0');
            }
        }

        // % only right after a digit
        if (c == '%') {
            if (expr.length() == 0) {
                return;
            }
            if (!Character.isDigit(expr.charAt(expr.length() - 1))) {
                return;
            }
        }

        expr.append(c);
    }

    // Goes back from the end until the number stops and checks if it has a dot
    private boolean currentNumberHasDot() {
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == '.') {
                return true;
            }
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return false;
    }

    private void openParen() {
        // After '=' a bracket starts a new expression
        if (justEvaluated) {
            expr.setLength(0);
            openParens = 0;
            justEvaluated = false;
        }
        // 2( means 2×(
        if (expr.length() > 0) {
            char last = expr.charAt(expr.length() - 1);
            if (Character.isDigit(last) || last == ')') {
                expr.append(MULT);
            }
        }
        expr.append('(');
        openParens++;
    }

    // ')' only when a bracket is open and not right after '(' or an operator
    private void closeParen() {
        if (openParens == 0 || expr.length() == 0) {
            return;
        }
        char last = expr.charAt(expr.length() - 1);
        if (last == '(' || isOperator(last)) {
            return;
        }
        expr.append(')');
        openParens--;
    }

    private void delete() {
        // After '=' backspace clears everything
        if (justEvaluated) {
            expr.setLength(0);
            openParens = 0;
            justEvaluated = false;
            return;
        }
        if (expr.length() == 0) {
            return;
        }
        char removed = expr.charAt(expr.length() - 1);
        // Keep openParens right when a bracket is deleted
        if (removed == '(') {
            openParens--;
        }
        if (removed == ')') {
            openParens++;
        }
        expr.deleteCharAt(expr.length() - 1);
    }

    // '=' puts the result in place of the expression, so the user can go on from it
    private void evaluate() {
        if (expr.length() == 0) {
            return;
        }
        String value = compute();
        if (value == null) {
            return;
        }
        expr.setLength(0);
        expr.append(value);
        openParens = 0;
        justEvaluated = true;
    }

    // Updates both lines of the display after every key
    private void render() {
        if (expr.length() == 0) {
            tvExpression.setText("0");
        } else {
            tvExpression.setText(expr.toString());
        }

        // No live result right after '=' or when nothing is typed
        if (justEvaluated || expr.length() == 0) {
            tvResult.setText("");
            return;
        }

        String value = compute();
        if (value == null) {
            tvResult.setText("");
        } else {
            tvResult.setText(value);
        }
    }

    // Returns null if it can't be calculated
    private String compute() {
        try {
            StringBuilder math = new StringBuilder(toMath(expr.toString()));
            // Close open brackets so the preview still works inside them.
            for (int i = 0; i < openParens; i++) {
                math.append(')');
            }
            // exp4j does the actual math on the converted string
            double result = new ExpressionBuilder(math.toString()).build().evaluate();
            if (Double.isNaN(result) || Double.isInfinite(result)) {
                return null;
            }
            return format(result);
        } catch (Exception e) {
            return null;
        }
    }

    // Changes the button symbols to what exp4j understands and expands every %
    private String toMath(String s) {
        StringBuilder out = new StringBuilder();

        // Start position of each open bracket
        Deque<Integer> parenStack = new ArrayDeque<>();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == MULT) {
                out.append('*');
            } else if (c == DIV) {
                out.append('/');
            } else if (c == MINUS) {
                out.append('-');
            } else if (c == '(') {
                parenStack.push(out.length());
                out.append('(');
            } else if (c == ')') {
                if (!parenStack.isEmpty()) {
                    parenStack.pop();
                }
                out.append(')');
            } else if (c == '%') {
                applyPercent(out, parenStack);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    // exp4j reads % as modulo, so we expand it here.
    // Like the Huawei calculator: a + b% is a + a*b/100 and a − b% is a − a*b/100,
    // but a × b% and a ÷ b% only divide b by 100.
    private void applyPercent(StringBuilder out, Deque<Integer> parenStack) {
        // Find where the number before % starts
        int numStart = out.length();
        while (numStart > 0) {
            char c = out.charAt(numStart - 1);
            if (Character.isDigit(c) || c == '.') {
                numStart--;
            } else {
                break;
            }
        }
        if (numStart == out.length()) {
            return;
        }

        String b = out.substring(numStart);

        char opBefore = 0;
        if (numStart > 0) {
            opBefore = out.charAt(numStart - 1);
        }

        // "a" is everything on the left, but only inside the current bracket
        int scopeStart = 0;
        if (!parenStack.isEmpty()) {
            scopeStart = parenStack.peek() + 1;
        }

        if ((opBefore == '+' || opBefore == '-') && numStart - 1 >= scopeStart) {
            String left = out.substring(scopeStart, numStart - 1);
            out.setLength(numStart);
            out.append('(').append(left).append(")*").append(b).append("/100");
        } else {
            out.setLength(numStart);
            out.append('(').append(b).append("/100)");
        }
    }

    // Rounds to 10 decimals and drops the zeros at the end, so 0.1 + 0.2 shows 0.3 and 20.0 shows 20
    private String format(double value) {
        return new BigDecimal(value)
                .setScale(10, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
