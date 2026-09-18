package com.craftinginterpreters.lox;

import java.util.List;

import static com.craftinginterpreters.lox.TokenType.*;

/**
 * Recursive descent parser for the expression grammar built through chapter
 * 6, extended with the three challenges at the end of that chapter:
 *
 * Final grammar, from lowest to highest precedence:
 *
 *   expression -> comma ;
 *   comma      -> ternary ( "," ternary )* ;
 *   ternary    -> equality ( "?" expression ":" ternary )? ;
 *   equality   -> comparison ( ( "!=" | "==" ) comparison )* ;
 *   comparison -> term ( ( ">" | ">=" | "<" | "<=" ) term )* ;
 *   term       -> factor ( ( "-" | "+" ) factor )* ;
 *   factor     -> unary ( ( "/" | "*" ) unary )* ;
 *   unary      -> ( "!" | "-" ) unary | primary ;
 *   primary    -> NUMBER | STRING | "true" | "false" | "nil"
 *               | "(" expression ")" ;
 */
class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    // ----- Challenge 6.1: the comma operator -----------------------------
    // expression -> comma ;
    // comma      -> ternary ( "," ternary )* ;
    //
    // Same precedence and associativity as C: lowest precedence of all,
    // and left associative. At runtime the left operand is evaluated and
    // discarded, and the value of the right operand is returned (see
    // Interpreter.visitBinaryExpr, case COMMA).
    //
    // Note: if/when a function call argument list is added later in the
    // book, the argument parser should call ternary() directly instead of
    // expression(), otherwise "foo(1, 2)" would be parsed as a single
    // argument built from the comma operator instead of two arguments.
    private Expr expression() {
        return comma();
    }

    private Expr comma() {
        Expr expr = ternary();

        while (match(COMMA)) {
            Token operator = previous();
            Expr right = ternary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    // ----- Challenge 6.2: the C style conditional ("ternary") operator ---
    // ternary -> equality ( "?" expression ":" ternary )? ;
    //
    // The condition sits at the equality level (so it binds tighter than
    // "?:" but looser than comparison), which mirrors where "?:" sits in
    // C's precedence table: just above assignment and comma, below
    // everything else.
    //
    // Between "?" and ":" the middle operand is parsed as a full
    // "expression". This is safe (unlike parsing a bare operand at the
    // ternary's own precedence) because the ":" token unambiguously marks
    // where the middle operand ends, so it can contain even comma
    // expressions without any ambiguity: "a ? b, c : d" is unambiguous.
    //
    // The operator itself is right associative: the branch after ":" is
    // parsed by recursing back into ternary() rather than looping, so
    // "a ? b : c ? d : e" parses as "a ? b : (c ? d : e)", matching C.
    private Expr ternary() {
        Expr expr = equality();

        if (match(QUESTION)) {
            Expr thenBranch = expression();
            consume(COLON, "Expect ':' after then branch of ternary expression.");
            Expr elseBranch = ternary();
            expr = new Expr.Ternary(expr, thenBranch, elseBranch);
        }

        return expr;
    }

    private Expr equality() {
        Expr expr = comparison();

        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        Expr expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        Expr expr = factor();

        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        Expr expr = unary();

        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }

        return primary();
    }

    // ----- Challenge 6.3: error productions for missing left operand -----
    // Every binary operator (other than unary minus, which is legal on its
    // own) is checked for here. If one of these turns up where a primary
    // expression was expected, we report the error, then parse and throw
    // away an operand at the matching precedence level so the parser can
    // keep going and look for more errors instead of losing its place.
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        if (match(COMMA)) {
            error(previous(), "Missing left-hand operand for ','.");
            ternary();
            return new Expr.Literal(null);
        }

        if (match(QUESTION)) {
            error(previous(), "Missing left-hand operand for '?:'.");
            expression();
            if (match(COLON)) {
                ternary();
            }
            return new Expr.Literal(null);
        }

        if (match(BANG_EQUAL, EQUAL_EQUAL)) {
            error(previous(), "Missing left-hand operand for '" + previous().lexeme + "'.");
            comparison();
            return new Expr.Literal(null);
        }

        if (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            error(previous(), "Missing left-hand operand for '" + previous().lexeme + "'.");
            term();
            return new Expr.Literal(null);
        }

        if (match(PLUS)) {
            error(previous(), "Missing left-hand operand for '+'.");
            factor();
            return new Expr.Literal(null);
        }

        if (match(SLASH, STAR)) {
            error(previous(), "Missing left-hand operand for '" + previous().lexeme + "'.");
            unary();
            return new Expr.Literal(null);
        }

        throw error(peek(), "Expect expression.");
    }

    // ----- Helpers ---------------------------------------------------------

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) return;

            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    break;
            }

            advance();
        }
    }
}
