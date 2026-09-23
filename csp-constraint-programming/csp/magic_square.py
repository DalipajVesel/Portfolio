from ortools.sat.python import cp_model
from collections import deque

n = 3
# το άθροισμα που πρέπει να έχει κάθε γραμμή, στήλη και διαγώνιος
S = n * (n * n + 1) // 2

model = cp_model.CpModel()
x = [[model.NewIntVar(1, n * n, "x_%d_%d" % (i, j)) for j in range(n)] for i in range(n)]

model.AddAllDifferent([x[i][j] for i in range(n) for j in range(n)])

for i in range(n):
    model.Add(sum(x[i][j] for j in range(n)) == S)
    model.Add(sum(x[j][i] for j in range(n)) == S)

model.Add(sum(x[i][i] for i in range(n)) == S)
model.Add(sum(x[i][n - 1 - i] for i in range(n)) == S)

solver = cp_model.CpSolver()
status = solver.Solve(model)

print("Magic Square n=%d" % n)
if status in (cp_model.OPTIMAL, cp_model.FEASIBLE):
    for i in range(n):
        print([solver.Value(x[i][j]) for j in range(n)])
else:
    print("Δεν βρέθηκε λύση.")

print()
print("AC-3 domains")

domains = {
    "A": {1, 3, 7, 8, 9},
    "B": {1, 3, 7, 8, 9},
    "C": {1, 3, 7, 8, 9},
    "D": {1, 3, 7, 8, 9},
    "E": {1, 3, 7, 8, 9},
}

constraints = {("E", "A"), ("B", "C"), ("B", "D"), ("E", "D")}
known_values = {("E", "A"): 5, ("E", "D"): 5, ("B", "D"): 6, ("B", "C"): 5}

# AC-3: κρατάμε σε κάθε domain μόνο τις τιμές που έχουν ταίρι στον γείτονα
queue = deque(constraints)
ok = True

while queue and ok:
    xv, yv = queue.popleft()
    revised = False
    k = known_values.get((xv, yv), None)

    if k is not None:
        for vx in list(domains[xv]):
            supported = False
            for vy in domains[yv]:
                if vx + vy + k == 15:
                    supported = True
                    break
            if not supported:
                domains[xv].remove(vx)
                revised = True

    if revised:
        if not domains[xv]:
            ok = False
            break
        for z, v in constraints:
            if v == xv and z != yv:
                queue.append((z, xv))

if ok:
    for var in ["A", "B", "C", "D", "E"]:
        print(var + ":", sorted(domains[var]))
else:
    print("Δεν υπάρχει λύση.")
