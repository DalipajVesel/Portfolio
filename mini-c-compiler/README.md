# Mini C Compiler

Lexer and parser for a small C-like language: declarations, arrays,
if/while/for, functions and four built-in functions. Flex does the
tokenizing, bison does the grammar, and both correct input and syntax errors
are reported line by line.

Compilers course, group project. The report is from Part A2 and covers the
automata for numbers, strings and identifiers.

## Files

- uni-c.l is the tokenizer, uni-c.y the grammar and main, token.h the token constants
- demo-correct.txt uses every construct of the grammar, demo-wrong.txt has a few mistakes in it

## Build and run

Needs flex and bison.

    make compile
    ./uni-c < demo-correct.txt
    ./uni-c < demo-wrong.txt
