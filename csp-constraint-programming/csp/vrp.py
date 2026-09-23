from ortools.sat.python import cp_model

N = 5
M = 3
capacity = 10
demand = [0, 2, 3, 4, 5, 2]
distance = [
    [0, 4, 8, 6, 7, 3],
    [4, 0, 5, 9, 2, 6],
    [8, 5, 0, 3, 4, 7],
    [6, 9, 3, 0, 8, 5],
    [7, 2, 4, 8, 0, 6],
    [3, 6, 7, 5, 6, 0],
]

nodes = range(N + 1)
customers = range(1, N + 1)
vehicles = range(M)

model = cp_model.CpModel()

# x[v][i][j] = 1 όταν το όχημα v πηγαίνει από τον κόμβο i στον j
x = [[[model.NewBoolVar("x_%d_%d_%d" % (v, i, j))
       for j in nodes]
      for i in nodes]
     for v in vehicles]

# οι u εμποδίζουν τις υποδιαδρομές που δεν περνάνε από την αποθήκη
u = [model.NewIntVar(0, N, "u_%d" % i)
     for i in nodes]
model.Add(u[0] == 0)

for v in vehicles:
    for i in nodes:
        model.Add(x[v][i][i] == 0)

for i in customers:
    model.Add(sum(x[v][i][j]
                  for v in vehicles
                  for j in nodes
                  if j != i) == 1)

    model.Add(sum(x[v][j][i]
                  for v in vehicles
                  for j in nodes
                  if j != i) == 1)

# κάθε όχημα ξεκινάει και τελειώνει στην αποθήκη (κόμβος 0)
for v in vehicles:
    model.Add(sum(x[v][0][j] for j in customers) == 1)
    model.Add(sum(x[v][i][0] for i in customers) == 1)

for v in vehicles:
    for i in customers:
        model.Add(
            sum(x[v][i][j] for j in nodes if j != i) ==
            sum(x[v][j][i] for j in nodes if j != i)
        )

for v in vehicles:
    model.Add(
        sum(demand[i] * sum(x[v][i][j] for j in nodes if j != i) for i in customers) <= capacity
    )

for v in vehicles:
    for i in customers:
        for j in customers:
            if i != j:
                model.Add(u[i] - u[j] + N * x[v][i][j] <= N - 1)

model.Minimize(sum(distance[i][j] * x[v][i][j] for v in vehicles for i in nodes for j in nodes))

solver = cp_model.CpSolver()
status = solver.Solve(model)

if status not in (cp_model.OPTIMAL, cp_model.FEASIBLE):
    print("No feasible solution")
else:
    print("Total Distance:", int(solver.ObjectiveValue()))
    for v in vehicles:
        route = [0]
        cur = 0
        while True:
            nxt = None
            for j in nodes:
                if j != cur and solver.Value(x[v][cur][j]) == 1:
                    nxt = j
                    break
            route.append(nxt)
            cur = nxt
            if cur == 0:
                break
        load = sum(demand[i] for i in route if i != 0)
        print("Vehicle", v + 1, "load:", load, "route:", route)
