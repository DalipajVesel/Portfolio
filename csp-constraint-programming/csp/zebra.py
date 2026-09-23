from ortools.sat.python import cp_model

model = cp_model.CpModel()

# A εθνικότητα, B τσιγάρα, C ποτό, D ζώο, E χρώμα, η τιμή είναι το σπίτι (1 έως 5)
A = [model.NewIntVar(1, 5, "A%d" % i) for i in range(1, 6)]
B = [model.NewIntVar(1, 5, "B%d" % i) for i in range(1, 6)]
C = [model.NewIntVar(1, 5, "C%d" % i) for i in range(1, 6)]
D = [model.NewIntVar(1, 5, "D%d" % i) for i in range(1, 6)]
E = [model.NewIntVar(1, 5, "E%d" % i) for i in range(1, 6)]

for g in (A, B, C, D, E):
    model.AddAllDifferent(g)

A1, A2, A3, A4, A5 = A
B1, B2, B3, B4, B5 = B
C1, C2, C3, C4, C5 = C
D1, D2, D3, D4, D5 = D
E1, E2, E3, E4, E5 = E

model.Add(A1 == E1)
model.Add(A2 == D1)
model.Add(A3 == C2)
model.Add(E2 - E3 == 1)
model.Add(E2 == C1)
model.Add(B1 == D2)
model.Add(E4 == B2)
model.Add(C3 == 3)
model.Add(A4 == 1)

# |x - y| == 1 σημαίνει διπλανά σπίτια
t10 = model.NewIntVar(0, 4, "t10")
model.AddAbsEquality(t10, B3 - D3)
model.Add(t10 == 1)

t11 = model.NewIntVar(0, 4, "t11")
model.AddAbsEquality(t11, D4 - B2)
model.Add(t11 == 1)

model.Add(B4 == C4)
model.Add(A5 == B5)

t14 = model.NewIntVar(0, 4, "t14")
model.AddAbsEquality(t14, A4 - E5)
model.Add(t14 == 1)

t15 = model.NewIntVar(0, 4, "t15")
model.AddAbsEquality(t15, B3 - C5)
model.Add(t15 == 1)

solver = cp_model.CpSolver()
status = solver.Solve(model)

pos_nat = {"Άγγλος": solver.Value(A1), "Σουηδός": solver.Value(A2), "Δανός": solver.Value(A3),
           "Νορβηγός": solver.Value(A4), "Γερμανός": solver.Value(A5)}
pos_smoke = {"Pall Mall": solver.Value(B1), "Dunhill": solver.Value(B2), "Blends": solver.Value(B3),
             "Blue Master": solver.Value(B4), "Prince": solver.Value(B5)}
pos_drink = {"Καφές": solver.Value(C1), "Τσάι": solver.Value(C2), "Γάλα": solver.Value(C3), "Μπύρα": solver.Value(C4),
             "Νερό": solver.Value(C5)}
pos_pet = {"Σκύλος": solver.Value(D1), "Πουλιά": solver.Value(D2), "Γάτες": solver.Value(D3), "Άλογο": solver.Value(D4),
           "Ζέβρα": solver.Value(D5)}
pos_color = {"Κόκκινο": solver.Value(E1), "Πράσινο": solver.Value(E2), "Λευκό": solver.Value(E3),
             "Κίτρινο": solver.Value(E4), "Μπλε": solver.Value(E5)}

color_at = {v: k for k, v in pos_color.items()}
nat_at = {v: k for k, v in pos_nat.items()}
drink_at = {v: k for k, v in pos_drink.items()}
smoke_at = {v: k for k, v in pos_smoke.items()}
pet_at = {v: k for k, v in pos_pet.items()}

print("House | Color | Nationality | Drink | Smoke | Pet")
for h in range(1, 6):
    print(h, color_at[h], nat_at[h], drink_at[h], smoke_at[h], pet_at[h], sep=" | ")

print("\nΖέβρα έχει ο:", nat_at[pos_pet["Ζέβρα"]])
print("Νερό πίνει ο:", nat_at[pos_drink["Νερό"]])
