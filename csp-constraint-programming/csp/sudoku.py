from ortools.sat.python import cp_model

model = cp_model.CpModel()

grid = [[model.NewIntVar(1, 9, "x_%d_%d" % (i, j))
         for j in range(9)]
        for i in range(9)]

# κάθε γραμμή και κάθε στήλη με διαφορετικά ψηφία
for i in range(9):
    model.AddAllDifferent(grid[i])
    model.AddAllDifferent([grid[r][i]
                           for r in range(9)])

# κάθε 3x3 κουτί με διαφορετικά ψηφία
for bi in range(0, 9, 3):
    for bj in range(0, 9, 3):
        block = [grid[bi + r][bj + c]
                 for r in range(3)
                 for c in range(3)]
        model.AddAllDifferent(block)

model.Add(grid[0][1] == 6)
model.Add(grid[1][5] == 1)
model.Add(grid[1][6] == 4)

solver = cp_model.CpSolver()
status = solver.Solve(model)
for i in range(9):
    print([solver.Value(grid[i][j])
           for j in range(9)])
