from ortools.sat.python import cp_model

N = 8

model = cp_model.CpModel()
q = [model.NewIntVar(0, N - 1, "q_%d" % i)
     for i in range(N)]

# όχι στην ίδια γραμμή και όχι στην ίδια διαγώνιο
for i in range(N):
    for j in range(i + 1, N):
        model.Add(q[i] != q[j])
        d = i - j
        model.Add(q[i] - q[j] != d)
        model.Add(q[i] - q[j] != -d)

solver = cp_model.CpSolver()
status = solver.Solve(model)
print([solver.Value(q[i]) + 1 for i in range(N)])

# η ίδια άσκηση με backtracking, βρίσκει όλες τις λύσεις
q_bt = [0] * N
solutions = []

col = 0
while col >= 0:
    q_bt[col] += 1

    while q_bt[col] <= N:
        conflict = False
        for pc in range(col):
            if q_bt[pc] == q_bt[col] or abs(q_bt[pc] - q_bt[col]) == abs(pc - col):
                conflict = True
                break
        if not conflict:
            break
        q_bt[col] += 1

    if q_bt[col] <= N:
        if col == N - 1:
            solutions.append(q_bt.copy())
            col -= 1
        else:
            col += 1
            q_bt[col] = 0
    else:
        q_bt[col] = 0
        col -= 1

first_solution = solutions[0]
total_solutions = len(solutions)

print(first_solution)
print(total_solutions)

for r in range(1, N + 1):
    line = ""
    for c in range(N):
        line += "Q " if first_solution[c] == r else ". "
    print(line)
