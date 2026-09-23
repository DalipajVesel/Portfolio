from ortools.sat.python import cp_model

model = cp_model.CpModel()

S = model.NewIntVar(0, 9, "S")
E = model.NewIntVar(0, 9, "E")
N = model.NewIntVar(0, 9, "N")
D = model.NewIntVar(0, 9, "D")
M = model.NewIntVar(0, 9, "M")
O = model.NewIntVar(0, 9, "O")
R = model.NewIntVar(0, 9, "R")
Y = model.NewIntVar(0, 9, "Y")

model.AddAllDifferent([S, E, N, D, M, O, R, Y])
model.Add(S != 0)
model.Add(M != 0)

SEND = 1000 * S + 100 * E + 10 * N + D
MORE = 1000 * M + 100 * O + 10 * R + E
MONEY = 10000 * M + 1000 * O + 100 * N + 10 * E + Y

model.Add(SEND + MORE == MONEY)

solver = cp_model.CpSolver()
status = solver.Solve(model)

print("CP-SAT")
if status in (cp_model.OPTIMAL, cp_model.FEASIBLE):
    s = solver.Value(S)
    e = solver.Value(E)
    n = solver.Value(N)
    d = solver.Value(D)
    m = solver.Value(M)
    o = solver.Value(O)
    r = solver.Value(R)
    y = solver.Value(Y)

    send = 1000 * s + 100 * e + 10 * n + d
    more = 1000 * m + 100 * o + 10 * r + e
    money = 10000 * m + 1000 * o + 100 * n + 10 * e + y

    print({"S": s, "E": e, "N": n, "D": d, "M": m, "O": o, "R": r, "Y": y})
    print(send, "+", more, "=", money)
else:
    print("No solution")

print()
print("BACKTRACKING")

# backtracking: δοκιμάζουμε ψηφία ένα ένα για κάθε γράμμα
letters = ["S", "E", "N", "D", "M", "O", "R", "Y"]
vals = [-1] * 8
used = [False] * 10
next_try = [0] * 8
i = 0
found = False

while i >= 0 and not found:
    digit = next_try[i]
    chosen = -1
    while digit <= 9:
        if not used[digit]:
            if digit == 0 and (letters[i] == "S" or letters[i] == "M"):
                digit += 1
                continue
            chosen = digit
            break
        digit += 1

    if chosen == -1:
        next_try[i] = 0
        if vals[i] != -1:
            used[vals[i]] = False
            vals[i] = -1
        i -= 1
        if i >= 0:
            used[vals[i]] = False
            vals[i] = -1
        continue

    vals[i] = chosen
    used[chosen] = True
    next_try[i] = chosen + 1

    if i == 7:
        s, e, n, d, m, o, r, y = vals
        send = 1000 * s + 100 * e + 10 * n + d
        more = 1000 * m + 100 * o + 10 * r + e
        money = 10000 * m + 1000 * o + 100 * n + 10 * e + y
        if send + more == money:
            print({"S": s, "E": e, "N": n, "D": d, "M": m, "O": o, "R": r, "Y": y})
            print(send, "+", more, "=", money)
            found = True
        used[vals[i]] = False
        vals[i] = -1
    else:
        i += 1
        next_try[i] = 0

if not found:
    print("No solution")
