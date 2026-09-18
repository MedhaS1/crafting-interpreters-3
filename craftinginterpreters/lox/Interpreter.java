package com.craftinginterpreters.lox;

class Interpreter implements Expr.Visitor<Object> {

    void interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;
            default:
                break;
        }

        // Unreachable.
        return null;
    }

    // ----- Challenge 6.2: ternary operator ---------------------------------
    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object condition = evaluate(expr.condition);
        if (isTruthy(condition)) {
            return evaluate(expr.thenBranch);
        } else {
            return evaluate(expr.elseBranch);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {

        // ----- Challenge 6.1: the comma operator ---------------------------
        // Evaluate the left operand purely for its side effects and throw
        // the result away, then evaluate and return the right operand.
        if (expr.operator.type == TokenType.COMMA) {
            evaluate(expr.left);
            return evaluate(expr.right);
        }

        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                // ----- Challenge 7.1: comparisons on strings --------------
                if (left instanceof Double && right instanceof Double) {
                    return (double) left > (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) > 0;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case GREATER_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left >= (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) >= 0;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case LESS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left < (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) < 0;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");
            case LESS_EQUAL:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left <= (double) right;
                }
                if (left instanceof String && right instanceof String) {
                    return ((String) left).compareTo((String) right) <= 0;
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings.");

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;

            case PLUS:
                // Number + number: arithmetic addition.
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }
                // String + string: concatenation (already supported).
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                // ----- Challenge 7.2: string + non-string concatenation ---
                // If either operand is a string, convert the other operand
                // to its string representation and concatenate. This lets
                // "scone" + 4 produce "scone4", and 4 + "scone" produce
                // "4scone".
                if (left instanceof String || right instanceof String) {
                    return stringify(left) + stringify(right);
                }
                throw new RuntimeError(expr.operator,
                        "Operands must be two numbers or two strings, or at least one operand must be a string.");

            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                // ----- Challenge 7.3: division by zero ----------------------
                if ((double) right == 0) {
                    throw new RuntimeError(expr.operator, "Division by zero.");
                }
                return (double) left / (double) right;

            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;

            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);

            default:
                break;
        }

        // Unreachable.
        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }
}
