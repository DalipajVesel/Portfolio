%{
	//./uni-c < input.txt
/* In this part of the program we include the C libraries 
and define the variables that will be needed later on.*/
        #include <stdio.h>
	    #include <stdlib.h>
		#include <string.h>

		int line=1; /* Line counter */
		int errflag=0; /* Error counter */
		int correct_words=0; /* Correct tokens from the input */
		int correct_expr=0;	/* Correct syntax expressions */
		int wrong_words=0; /* Wrong tokens from the input */

		extern char *yytext;
		int yylex(void);
		void yyerror(const char *s);
		#define YYSTYPE char *
%}

// Declaring tokens.
%token BREAK CASE FUNC CONST CONTINUE DO DOUBLE ELSE
%token FOR IF FLOAT INT LONG RETURN SHORT SIZEOF STRUCT SWITCH VOID WHILE
%token PLUS MINUS MULTIPLY DIVISION MODULO ASSIGN
%token NOT AND OR PLUSPLUS
%token MINUSMINUS LESSTHAN MORETHAN LESSEQUAL MOREEQUAL MEMORYADDRESS
%token VARIABLE SEMICOLON FLOAT_NUM ZERO INTEGER LEFT_SQUARE_BRACKET RIGHT_SQUARE_BRACKET COMMA
%token LEFT_BRACKET RIGHT_BRACKET UNKNOWN_TOKEN STRING LEFT_PARENTHESES RIGHT_PARENTHESES
%token SCAN LEN CMP PRINT NEWLINE

%%

/* In this part of the program we declare our grammar rules. Each time we match
a grammar rule from the input we execute the code inside the brackets. */
program:
    	program expr SEMICOLON
	| 	program expr {printf("\n\tWarning: expected semicolon (;) !!!\n"); errflag++;} // B3 Warning for Semicolon
	|  	program loops
	| 	program error SEMICOLON{printf("\n\t Error in line: %d \n",line); yyerrok; errflag++;}
	|	program expr UNKNOWN_TOKEN {printf("\n\t Token Error!\n");}
	|	
	;
loops: if { printf("\n\t### Line %d IF DECLARATION.\n",line); correct_expr++;} 
	|while {printf("\n\t### Line %d WHILE DECLARATION.\n",line); correct_expr++;} 
	|for {printf("\n\t### Line %d FOR DECLARATION.\n",line); correct_expr++;} 
	|if SEMICOLON {printf("\n\tWarning:  unexpected semicolon (;) after if !!!\n"); errflag++;}
	|while SEMICOLON {printf("\n\tWarning: unexpected semicolon (;) after while !!!\n"); errflag++;}
	|for SEMICOLON {printf("\n\tWarning: unexpected semicolon (;) after for !!!\n"); errflag++;}
expr:
	var_decl {printf("\n\t### Line %d VARIABLE DECLARATION.\n",line);  correct_expr++;} 
	|var_as   {printf("\n\t### Line %d VARIABLE ASSIGNMENT.\n",line); correct_expr++;}
	|array_access {printf("\n\t### Line %d ARRAY ACCESS.\n",line); correct_expr++;}
	|array_as {printf("\n\t### Line %d ARRAY ASSIGNMENT.\n",line); correct_expr++;}
	|scan {printf("\n\t### Line %d SCAN FUNCTION.\n",line); correct_expr++;} 
	|len {printf("\n\t### Line %d LEN FUNCTION.\n",line); correct_expr++;}
	|cmp {printf("\n\t### Line %d CMP FUNCTION.\n",line); correct_expr++;}
	|print {printf("\n\t### Line %d PRINT FUNCTION.\n",line); correct_expr++;}
	|function  {printf("\n\t### Line %d FUNCTION DECLARATION.\n",line); correct_expr++;}
	|function_call {printf("\n\t### Line %d FUNCTION CALL.\n",line); correct_expr++;}
	|num_expr {printf("\n\t### Line %d NUMERICAL EXPRESSION.\n",line); correct_expr++;}
	|comparison {printf("\n\t### Line %d COMPARISON.\n",line); correct_expr++;} 
	|array_merg {printf("\n\t### Line %d ARRAY MERGE.\n",line); correct_expr++;} 
	;
nums:		//
	|	INTEGER 
	|	FLOAT_NUM //
	;
var_decl:
		INT VARIABLE
	|	DOUBLE VARIABLE
	|	FLOAT VARIABLE
	|	LONG VARIABLE
	|	SHORT VARIABLE
	|   INT VARIABLE ASSIGN INTEGER 	//
	| 	DOUBLE VARIABLE ASSIGN INTEGER //
	| 	FLOAT VARIABLE ASSIGN FLOAT_NUM //
	|	LONG VARIABLE ASSIGN INTEGER //
	|	SHORT VARIABLE ASSIGN INTEGER //
	;
builtin_function_out:
		len
	|	cmp
	;
array_exp:
	nums
	|	STRING
	|	builtin_function_out
	|	array_exp COMMA array_exp
	;
array_access:
		VARIABLE LEFT_SQUARE_BRACKET VARIABLE RIGHT_SQUARE_BRACKET
	| 	VARIABLE LEFT_SQUARE_BRACKET INTEGER RIGHT_SQUARE_BRACKET
	| 	VARIABLE LEFT_SQUARE_BRACKET ZERO RIGHT_SQUARE_BRACKET
	;
array_body:
		LEFT_SQUARE_BRACKET RIGHT_SQUARE_BRACKET
	|	LEFT_SQUARE_BRACKET array_exp RIGHT_SQUARE_BRACKET
	;
array_merg:
	  array_body PLUS array_body
	| array_merg PLUS array_body
	;
array_as:
	array_access ASSIGN values
	;
scan:
		SCAN LEFT_PARENTHESES VARIABLE RIGHT_PARENTHESES
	;
str_var:
		STRING
	| 	VARIABLE
	;
len:
		LEN LEFT_PARENTHESES str_var RIGHT_PARENTHESES
	|	LEN LEFT_PARENTHESES LEFT_SQUARE_BRACKET array_exp RIGHT_SQUARE_BRACKET RIGHT_PARENTHESES
	;
cmp:
		CMP LEFT_PARENTHESES str_var COMMA str_var RIGHT_PARENTHESES
	;
print_epxr:
		array_exp
	|	array_access
	|	print_epxr COMMA print_epxr
	;	
print:
		PRINT LEFT_PARENTHESES print_epxr RIGHT_PARENTHESES	
	;
func_decl:
	var_decl
	| var_decl COMMA var_decl
	| func_decl COMMA func_decl
	|
	;
function:
	FUNC VARIABLE LEFT_PARENTHESES func_decl RIGHT_PARENTHESES LEFT_BRACKET program RIGHT_BRACKET
	;
function_call:
		VARIABLE LEFT_PARENTHESES array_exp RIGHT_PARENTHESES
	|	VARIABLE LEFT_PARENTHESES RIGHT_PARENTHESES
	;
vars:
		VARIABLE
	|	VARIABLE COMMA VARIABLE
	| 	vars COMMA VARIABLE 
	;

values:
		array_exp
	|	nums_neg
	| 	num_expr
	|	array_access
	| 	values COMMA values
	;
var_as:
	nums_neg ASSIGN vars {printf("\n\tWarning: Left side of equation should be a variable !!!\n"); errflag++;}
	| num_expr ASSIGN vars {printf("\n\tWarning: Left side of equation should be a variable !!!\n"); errflag++;}
	| vars ASSIGN values
	;
nums_neg:
		MINUS nums
	| 	nums
	|	ZERO
	;
div_nums:
		MINUS nums
	| 	nums
	;
num_vars: 
		nums_neg
	| 	vars
	;

num_expr:
		num_vars DIVISION ZERO {printf("\n\n\tWarning: Division by zero !!!\n");errflag++;}
	| 	num_vars DIVISION ASSIGN ZERO {printf("\n\n\tWarning: Division by zero !!!\n");errflag++;}
	|	num_vars PLUS ASSIGN num_vars
	| 	num_vars MINUS ASSIGN num_vars
	| 	num_vars MINUS MINUS  //
	| 	num_vars PLUS PLUS //
	| 	num_vars MULTIPLY ASSIGN num_vars
	| 	num_vars DIVISION ASSIGN div_nums
	| 	num_vars MODULO ASSIGN num_vars
	|	num_vars PLUS num_vars
	|	num_vars MINUS num_vars
	|	num_vars MULTIPLY num_vars
	|	num_vars DIVISION div_nums
	|	num_vars MODULO num_vars
	;
comparison:
		num_vars ASSIGN ASSIGN num_vars
	|	num_vars NOT ASSIGN num_vars
	| 	num_vars MORETHAN num_vars
	| 	num_vars MORETHAN ASSIGN num_vars
	| 	num_vars LESSTHAN num_vars
	| 	num_vars LESSTHAN ASSIGN num_vars
	| 	num_vars OR num_vars
	| 	num_vars AND num_vars
	|	comparison ASSIGN ASSIGN num_vars
	| 	comparison NOT ASSIGN num_vars
	| 	comparison MORETHAN num_vars
	| 	comparison MORETHAN ASSIGN num_vars
	| 	comparison LESSTHAN num_vars
	| 	comparison LESSTHAN ASSIGN num_vars
	| 	comparison OR num_vars
	| 	comparison AND num_vars
	;
if:
		IF LEFT_PARENTHESES comparison RIGHT_PARENTHESES LEFT_BRACKET program RIGHT_BRACKET
	;
while:
		WHILE LEFT_PARENTHESES comparison RIGHT_PARENTHESES LEFT_BRACKET program RIGHT_BRACKET
	;
for:
		FOR LEFT_PARENTHESES var_as SEMICOLON comparison SEMICOLON var_as RIGHT_PARENTHESES LEFT_BRACKET program RIGHT_BRACKET
	;

%%

FILE *yyinput;
/* Function main is the starting point of our program.
In our case it opens two files, calls the function yyparse to execute
the syntax analyzer and closes the two previous files. */
int main(int argc,char **argv)
{
	/* Checking if we got an argument and if we did we open that file argument to read from */
	if(argc == 2)
		yyinput=fopen(argv[1],"r");
	else
		yyinput=stdin;
	int parse = yyparse(); /* Calling yyparse and return the results to integer parse */

	/* Check if yyparse ran successfully and if we had any errors */ 
	if(parse==0 && errflag==0)
		printf("\nSuccessful parsing.\n");
	else
		printf("\nError found.\n");

	/* Print the results */
	printf("\nCorrect words: %d",correct_words);
	printf("\nWrong words: %d",wrong_words);
	printf("\nCorrect expressions: %d", correct_expr);
	printf("\nWrong expressions: %d\n", errflag);

	return 0;
}
