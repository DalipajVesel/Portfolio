# Expense Tracker API

A REST API in Spring Boot for my expense tracker, together with the Android
app that uses it. The server is in expense-tracker-api and the app in
ExpenseTracker. It is the same app as in expense-tracker, with Room replaced
by Retrofit and a login screen.

Register and login take only a username. Login gives back a JWT, and
everything under /api/expenses needs that token, so every user only sees his
own expenses. The list takes a from and a to in milliseconds, which covers all
the filters of the app. Amounts are stored as cents in a long.

Personal project.

## Endpoints

| Method | Path |
|---|---|
| POST | /api/auth/register |
| POST | /api/auth/login |
| GET | /api/expenses?from=&to= |
| GET, PUT, DELETE | /api/expenses/{id} |
| POST | /api/expenses |

## Build and run

The server needs Java 25 and PostgreSQL. Make an empty database called
expensetracker and put your own user and password in
expense-tracker-api/src/main/resources/application.properties.

    ./gradlew bootRun

It starts on port 8080. Hibernate makes the tables on the first run.

The ExpenseTracker folder opens straight in Android Studio. I ran the app on
the Medium Phone emulator with API 36, with the server on the same computer.
It finds it at 10.0.2.2:8080, set in ApiClient.java. Min SDK 24, target 37.
