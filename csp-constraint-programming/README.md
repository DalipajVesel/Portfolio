# CSP Constraint Programming

Eight constraint satisfaction problems with OR-Tools CP-SAT: a university
timetable, vehicle routing, N-Queens, map colouring, Einstein's riddle,
SEND + MORE = MONEY, sudoku and a magic square.

Map colouring compares the default search with two decision strategies, MRV
and a degree order, and times all three. N-Queens and SEND + MORE = MONEY are
also solved with plain backtracking, and the magic square also runs an AC-3
example.

Constraint Modelling and Programming course.

Every script runs on its own, for example:

    pip install ortools
    python csp_uni_timetable.py
