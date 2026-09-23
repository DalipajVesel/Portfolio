from ortools.sat.python import cp_model
import time

states = ["WA", "NT", "SA", "Q", "NSW", "V", "T"]
colors = ["Red", "Green", "Blue"]

edges = [
    ("WA", "NT"), ("WA", "SA"),
    ("NT", "SA"), ("NT", "Q"),
    ("SA", "Q"), ("SA", "NSW"), ("SA", "V"),
    ("Q", "NSW"),
    ("NSW", "V"),
]

idx = {s: i for i, s in enumerate(states)}


# μετράει όλες τις λύσεις και κρατάει την πρώτη
class Counter(cp_model.CpSolverSolutionCallback):
    def __init__(self, vars_list):
        cp_model.CpSolverSolutionCallback.__init__(self)
        self.vars_list = vars_list
        self.count = 0
        self.first = None

    def OnSolutionCallback(self):
        self.count += 1
        if self.first is None:
            self.first = [self.Value(v) for v in self.vars_list]


print("1) DEFAULT_toggle")
m1 = cp_model.CpModel()
c1 = [m1.NewIntVar(0, 2, states[i]) for i in range(len(states))]
for a, b in edges:
    m1.Add(c1[idx[a]] != c1[idx[b]])
s1 = cp_model.CpSolver()
cb1 = Counter(c1)
t0 = time.perf_counter()
s1.SearchForAllSolutions(m1, cb1)
t1 = time.perf_counter()
print("solutions:", cb1.count, "time:", t1 - t0)
for i in range(len(states)):
    print(states[i], "=", colors[cb1.first[i]])

print("\n2) MRV (AddDecisionStrategy)")
m2 = cp_model.CpModel()
c2 = [m2.NewIntVar(0, 2, states[i]) for i in range(len(states))]
for a, b in edges:
    m2.Add(c2[idx[a]] != c2[idx[b]])
m2.AddDecisionStrategy(c2, cp_model.CHOOSE_MIN_DOMAIN_SIZE, cp_model.SELECT_MIN_VALUE)
s2 = cp_model.CpSolver()
s2.parameters.search_branching = cp_model.FIXED_SEARCH
cb2 = Counter(c2)
t0 = time.perf_counter()
s2.SearchForAllSolutions(m2, cb2)
t1 = time.perf_counter()
print("solutions:", cb2.count, "time:", t1 - t0)
for i in range(len(states)):
    print(states[i], "=", colors[cb2.first[i]])

print("\n3) DEGREE order (static order)")
m3 = cp_model.CpModel()
c3 = [m3.NewIntVar(0, 2, states[i]) for i in range(len(states))]
for a, b in edges:
    m3.Add(c3[idx[a]] != c3[idx[b]])

# πρώτα οι περιοχές με τους περισσότερους γείτονες
deg = {s: 0 for s in states}
for a, b in edges:
    deg[a] += 1
    deg[b] += 1

order_states = sorted(states, key=lambda s: -deg[s])
order_vars = [c3[idx[s]] for s in order_states]

m3.AddDecisionStrategy(order_vars, cp_model.CHOOSE_FIRST, cp_model.SELECT_MIN_VALUE)
s3 = cp_model.CpSolver()
s3.parameters.search_branching = cp_model.FIXED_SEARCH
cb3 = Counter(c3)
t0 = time.perf_counter()
s3.SearchForAllSolutions(m3, cb3)
t1 = time.perf_counter()
print("solutions:", cb3.count, "time:", t1 - t0)
for i in range(len(states)):
    print(states[i], "=", colors[cb3.first[i]])
