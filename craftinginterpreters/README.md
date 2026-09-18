# Crafting Interpreters: Chapter 6 and 7 Challenges

This repository contains a working `jlox` parser and tree walk interpreter
(Java) that implements the challenge exercises from the end of Chapter 6
("Parsing Expressions") and Chapter 7 ("Evaluating Expressions") of Robert
Nystrom's *Crafting Interpreters* (https://craftinginterpreters.com/).

At this stage of the book, jlox only understands expressions (statements,
variables, and control flow come in later chapters), so the program reads
one expression at a time, either from a file or from the interactive
prompt, evaluates it, and prints the result.

## Chapter 6 challenges

1. **Comma operator** — implemented in `Parser.java` (`comma()`) and
   `Interpreter.java` (`visitBinaryExpr`, the `COMMA` case). Same
   precedence and associativity as C: lowest precedence, left associative.
2. **Ternary operator `?:`** — implemented as a new `Expr.Ternary` node.
   See `Parser.java` (`ternary()`) and `Interpreter.java`
   (`visitTernaryExpr`).
3. **Error productions for a missing left-hand operand** — implemented in
   `Parser.java` (`primary()`). Detects a binary operator at the start of
   an expression, reports an error, and still parses (and discards) a
   right-hand operand so parsing can continue.

## Chapter 7 challenges

1. **Comparisons on other types** — a discussion question, answered in the
   submitted write-up. The code additionally extends `>`, `>=`, `<`, and
   `<=` to work on two strings (lexicographic order), while intentionally
   not allowing comparisons between mixed types.
2. **`+` with mixed string / non-string operands** — implemented in
   `Interpreter.java` (`visitBinaryExpr`, the `PLUS` case). If either
   operand is a string, the other is converted to its string
   representation and the two are concatenated.
3. **Division by zero** — implemented in `Interpreter.java`
   (`visitBinaryExpr`, the `SLASH` case). Dividing by zero now raises a
   `RuntimeError` ("Division by zero.") instead of silently producing
   `Infinity` or `NaN`.

## Building and running

Requires a JDK (17+). From the repository root:

```
javac com/craftinginterpreters/lox/*.java -d out
java -cp out com.craftinginterpreters.lox.Lox            # interactive prompt
java -cp out com.craftinginterpreters.lox.Lox script.lox  # run a file
```

## Project layout

```
com/craftinginterpreters/lox/
  TokenType.java    Token kinds, including the new QUESTION and COLON
  Token.java        Token record
  Scanner.java      Hand written lexer
  Expr.java         Expression AST node classes, including Expr.Ternary
  Parser.java       Recursive descent parser (chapter 6 challenges)
  Interpreter.java  Tree walk evaluator (chapter 7 challenges)
  Lox.java          Entry point, REPL / script runner, error reporting
```
