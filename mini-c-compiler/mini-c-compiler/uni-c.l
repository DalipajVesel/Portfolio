/* Reads only one file and terminates the program at the first EOF. */
%option noyywrap
%x error

/* C code to declare all the necessary header files and variables. */
%{

#include <stdio.h>
#include <string.h>
#include <stdlib.h>

/* Header file which contains the token list. */
#include "uni-c.tab.h"

/* The various counters that we got from the bison file */
extern int line;
extern int correct_words;
extern int wrong_words;

/* Declaring the keywords. */

const char *keywords[] = {"break","case","func","const","continue",
                      "do","double","else","float","for",
                        "if","int","long","return","short",
                        "sizeof","struct","switch","void","while"};

 /* Declaring the operators. */
const char *operators[] = {"+","-","*","/","%","=","!","<",">","&"};

void yyerror(const char *s); /* Error handling */
void prn(char *s); /* Token found printer */
int check(const char *array[],char *yytext,int array_size); /* Checking if we got a match of the 2 previous declared arrays */

%}

/* Declaring the names and arguments(in the form of regular expressions) to be used.
   After this we can use the names instead of the regular expressions. */
WHITE_SPACE     [ \t]+
SCAN            (scan) 
LEN             (len)
CMP             (cmp)
PRINT           (print)
STRING          ["]([^\\\"\n]|\\\\|\\\"|\\n)*["]
FLOAT_NUM       ([0]|[1-9][0-9]*)+((\.[0-9]+)([eE][-]?([0]|[1-9][0-9]*))?|([eE][-]?([0]|[1-9][0-9]*)))
COMMENT_ROW     (\/\/)(.*)
COMMENT_LINE    [\/][\*]+([^*][^\/]*)[\*]+[\/]
ZERO            0+  
INTEGER         (0[xX][0-9A-F]+)|(0[0-7]*)|([1-9][0-9]*)
VARIABLE        [A-Za-z_]+[A-Za-z_0-9]*
            


/* For each pattern that matches execute the code inside the right side of the brackets.  */

%%

{WHITE_SPACE}   { }
{SCAN}          {prn("BUILT-IN-FUNCTION"); correct_words++; return SCAN;}
{LEN}           {prn("BUILT-IN-FUNCTION"); correct_words++; return LEN;}
{CMP}           {prn("BUILT-IN-FUNCTION"); correct_words++; return CMP;}
{PRINT}         {prn("BUILT-IN-FUNCTION"); correct_words++; return PRINT;}
{ZERO}          {prn("ZERO"); correct_words++; return ZERO;}
{INTEGER}       {prn("INTEGER"); correct_words++; return INTEGER;}
";"             {prn("SEMICOLON"); correct_words++; return SEMICOLON;}
"("             {prn("LEFT PARENTHESES"); correct_words++; return LEFT_PARENTHESES;}
")"             {prn("RIGHT PARENTHESES"); correct_words++; return RIGHT_PARENTHESES;}
"["             {prn("LEFT SQUARE BRACKET"); correct_words++; return LEFT_SQUARE_BRACKET;}
"]"             {prn("RIGHT SQUARE BRACKET"); correct_words++; return RIGHT_SQUARE_BRACKET;}
"{"             {prn("LEFT BRACKET"); correct_words++; return LEFT_BRACKET;}
"}"             {prn("RIGHT BRACKET"); correct_words++; return RIGHT_BRACKET;}
","             {prn("COMMA"); correct_words++; return COMMA;}
{FLOAT_NUM}         { prn("FLOAT"); correct_words++; return FLOAT_NUM;}
{STRING}        { prn("STRING"); correct_words++; return STRING;}
{VARIABLE}    {
        int i = check(keywords, yytext, 20); /* Checking if the word is a keyword */
        if( i > -1)
        {
            switch(i)
            {
                case 0:
                    prn("KEYWORD");
                    correct_words++;
                    correct_words++;
                    break;
                case 1:
                    prn("KEYWORD");
                    correct_words++;
                    return CASE;
                    break;
                case 2:
                    prn("KEYWORD");
                    correct_words++;
                    return FUNC;
                    break;
                case 3:
                    prn("KEYWORD");
                    correct_words++;
                    return CONST;
                    break;
                case 4:
                    prn("KEYWORD");
                    correct_words++;
                    return CONTINUE;
                    break;
                case 5:
                    prn("KEYWORD");
                    correct_words++;
                    return DO;
                    break;
                case 6:
                    prn("KEYWORD");
                    correct_words++;
                    return DOUBLE;
                    break;
                case 7:
                    prn("KEYWORD");
                    correct_words++;
                    return ELSE;
                    break;
                case 8:
                    prn("KEYWORD");
                    correct_words++;
                    return FLOAT;
                    break;
                case 9:
                    prn("KEYWORD");
                    correct_words++;
                    return FOR;
                    break;
                case 10:
                    prn("KEYWORD");
                    correct_words++;
                    return IF;
                    break;
                case 11:
                    prn("KEYWORD");
                    correct_words++;
                    return INT;
                    break;
                case 12:
                    prn("KEYWORD");
                    correct_words++;
                    return LONG;
                    break;
                case 13:
                    prn("KEYWORD");
                    correct_words++;
                    return RETURN;
                    break;
                case 14:
                    prn("KEYWORD");
                    correct_words++;
                    return SHORT;
                    break;
                case 15:
                    prn("KEYWORD");
                    correct_words++;
                    return SIZEOF;
                    break;
                case 16:
                    prn("KEYWORD");
                    correct_words++;
                    return STRUCT;
                    break;
                case 17:
                    prn("KEYWORD");
                    correct_words++;
                    return SWITCH;
                    break;
                case 18:
                    prn("KEYWORD");
                    correct_words++;
                    return VOID;
                    break;
                case 19:
                    prn("KEYWORD");
                    correct_words++;
                    return WHILE;
                    break;
            }
        }
        else
        {
            prn("VARIABLE");
            correct_words++;
            return VARIABLE;
        }
    }
{COMMENT_ROW}   { }
{COMMENT_LINE}  { }
. { 
		int i = check(operators, yytext, 10); /* Checking if the symbol is an operator */
        if( i > -1 )
        {
			switch(i)
			{
				case 0:
                    prn("OPERATOR");
                    correct_words++;
					return PLUS;
					break;
				case 1:
                    prn("OPERATOR");
                    correct_words++;
					return MINUS;
					break;
				case 2:
                    prn("OPERATOR");
                    correct_words++;
					return MULTIPLY;
					break;
				case 3:
					prn("OPERATOR");
                    correct_words++;
					return DIVISION;
					break;
				case 4:
                    prn("OPERATOR");
                    correct_words++;
                    return MODULO;
					break;
                case 5:
                    prn("OPERATOR");
                    correct_words++;
                    return ASSIGN;
                    break;
                case 6:
                    prn("OPERATOR");
                    correct_words++;
                    return NOT;
                    break;
                case 7:
                    prn("OPERATOR");
                    correct_words++;
                    return LESSTHAN;
                    break;
                case 8:
                    prn("OPERATOR");
                    correct_words++;
                    return MORETHAN;
                    break;
                case 9:
                    prn("OPERATOR");
                    correct_words++;
                    return MEMORYADDRESS;
                    break;
			}
        }
        else
        {
            yyerror("UNKNOWN_TOKEN");
            wrong_words++;
            BEGIN(error);
            return UNKNOWN_TOKEN;
        }
    }
<error>[ \t]+ {BEGIN(0); printf("\n\t %d characters ignored so far\n", wrong_words); }
<error>. {}
<error>\n {BEGIN(0); printf("\n\t%d characters ignored so far\n", wrong_words);  line++;}
(\\[ \t]*\n)?   { line++; }
\n              { line++; }
<<EOF>>		{ return 0; }

%%

/* Function check checks if a string(yytext) is similar to another string in an array(array).
   In our case we use the function check to check if the returned value is a KEYWORD or an OPERATOR 
   If the strings are similar it returns the value 1(TRUE) if not it returns 0(FALSE)*/

int check(const char *array[],char *yytext,int array_size){
	int i;
	for (i=0;i<array_size;i++){
		if(strcmp(yytext,array[i])==0)
			return i;
	}
	return -1;
}

/* Prints the found token */
void prn(char *s)
{
	printf("\n\t%s: %s", s, yytext);
	return;
}

/* Messages the user about the error */
void yyerror (const char *msg)
{
    fprintf(yyout, "\tError, line %d at lexeme \'%s\': %s\n", line, yytext, msg);
    getchar();
    return;
}
