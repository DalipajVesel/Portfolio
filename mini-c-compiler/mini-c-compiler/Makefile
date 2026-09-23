all:
	make compile
	./uni-c < demo-correct.txt
compile:
	bison -d uni-c.y
	flex uni-c.l
	gcc -o uni-c uni-c.tab.c lex.yy.c
wrong:
	make compile
	./uni-c < demo-wrong.txt
