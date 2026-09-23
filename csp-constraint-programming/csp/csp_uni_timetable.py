from ortools.sat.python import cp_model

# λιστα φοιτητών
S = {"Student1", "Student2", "Student3", "Student4", "Student5"}

# λίστα μαθημάτων 
L = {"Course1_Lec", "Course1_Lab", "Course2", "Course3", "Course4", "Course5", "Course6"}

# μαθήματα που παρακολουθεί κάθε φοιτητής
student_courses = {
    "Student1": {"Course1_Lec", "Course1_Lab", "Course2"},
    "Student2": {"Course2", "Course3"},
    "Student3": {"Course1_Lec", "Course1_Lab", "Course4"},
    "Student4": {"Course3", "Course5"},
    "Student5": {"Course4", "Course6"}
}

# λίστα αιθουσών και χωρητικότητες
R = {"Room1", "Room2", "Room3"}
room_capacities = {"Room1": 20, "Room2": 30, "Room3": 40}

# λίστα διδασκόντων
teachers = {"Teacher1", "Teacher2", "Teacher3", "Teacher4"}

# ανάθεση διδασκόντων σε μαθήματα
course_teachers = {
    "Course1_Lec": "Teacher1",
    "Course1_Lab": "Teacher1",
    "Course2": "Teacher2",
    "Course3": "Teacher3",
    "Course4": "Teacher4",
    "Course5": "Teacher1",
    "Course6": "Teacher2"
}

# διαθέσιμες χρονικές στιγμές
timeslots = [
    "Monday 08:00", "Monday 10:00",
    "Tuesday 09:00", "Tuesday 11:00",
    "Wednesday 08:00", "Wednesday 10:00",
]
T = range(len(timeslots))

# διαθεσιμότητα διδασκόντων ανά χρονική στιγμή
teacher_availability = {
    "Teacher1": {0, 1, 2},
    "Teacher2": {1, 2, 3},
    "Teacher3": {2, 3, 4},
    "Teacher4": {3, 4, 5},
}

# περιορισμοί προτεραιότητας 
precedence = [("Course1_Lec", "Course1_Lab")]

# Ταξινομημένες λίστες για σταθερή σειρά επεξεργασίας
S_list = sorted(S)
L_list = sorted(L)
R_list = sorted(R)
teachers_list = sorted(teachers)

# υπολογισμός μεγέθους κάθε μαθήματος
course_size = {}
for c in L_list:
    cnt = 0
    for s in S_list:
        if c in student_courses[s]:
            cnt += 1
    course_size[c] = cnt

model = cp_model.CpModel()

x = {}
# μεταβλητές ανάθεσης μαθήματος σε χρονική στιγμή
for c in L_list:
    for t in T:
        x[(c, t)] = model.NewBoolVar("x_%s_%d" % (c, t))

y = {}
# μεταβλητές ανάθεσης μαθήματος σε αίθουσα
for c in L_list:
    for r in R_list:
        y[(c, r)] = model.NewBoolVar("y_%s_%s" % (c, r))

z = {}
# μεταβλητές για συνδυασμό μαθήματος, χρονικής στιγμής και αίθουσας
for c in L_list:
    for t in T:
        for r in R_list:
            z[(c, t, r)] = model.NewBoolVar("z_%s_%d_%s" % (c, t, r))
            model.Add(z[(c, t, r)] <= x[(c, t)])
            model.Add(z[(c, t, r)] <= y[(c, r)])
            model.Add(z[(c, t, r)] >= x[(c, t)] + y[(c, r)] - 1)

# περιορισμοί ανάθεσης μαθημάτων σε χρονικές στιγμές και αίθουσες
for c in L_list:
    model.Add(sum(x[(c, t)] for t in T) == 1)
    model.Add(sum(y[(c, r)] for r in R_list) == 1)

# περιορισμοί χωρητικότητας αιθουσών
for c in L_list:
    for r in R_list:
        if room_capacities[r] < course_size[c]:
            model.Add(y[(c, r)] == 0)

# περιορισμοί μαθημάτων για φοιτητές και αίθουσες
for s in S_list:
    for t in T:
        model.Add(sum(x[(c, t)] for c in student_courses[s]) <= 1)

# Περιορισμοί αιθουσών
for t in T:
    for r in R_list:
        model.Add(sum(z[(c, t, r)] for c in L_list) <= 1)

# περιορισμοί διαθεσιμότητας διδασκόντων
for c in L_list:
    teacher = course_teachers[c]
    allowed = teacher_availability[teacher]
    for t in T:
        if t not in allowed:
            model.Add(x[(c, t)] == 0)

# περιορισμοί διδασκόντων για μαθήματα
for teacher in teachers_list:
    taught = [c for c in L_list if course_teachers[c] == teacher]
    for t in T:
        model.Add(sum(x[(c, t)] for c in taught) <= 1)

# περιορισμοί προτεραιότητας μαθημάτων
time_of = {}
for c in L_list:
    tv = model.NewIntVar(0, len(timeslots) - 1, "t_%s" % c)
    model.Add(tv == sum(t * x[(c, t)] for t in T))
    time_of[c] = tv

# Προτεραιότητες μαθημάτων
for a, b in precedence:
    model.Add(time_of[a] < time_of[b])

solver = cp_model.CpSolver()
status = solver.Solve(model)

# Εκτύπωση αποτελεσμάτων
if status not in (cp_model.OPTIMAL, cp_model.FEASIBLE):
    print("No feasible timetable")
else:
    print("University timetable\n")
    for t in T:
        print(timeslots[t])
        for r in R_list:
            placed = "-"
            for c in L_list:
                if solver.Value(z[(c, t, r)]) == 1:
                    placed = c
                    break
            print(" ", r, ":", placed)
        print()
