package com.vesel.expensetrackerapi.service;

import com.vesel.expensetrackerapi.entity.Expense;
import com.vesel.expensetrackerapi.entity.User;
import com.vesel.expensetrackerapi.repository.ExpenseRepository;
import com.vesel.expensetrackerapi.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class ExpenseService {

    private final ExpenseRepository repository;
    private final UserRepository userRepository;

    public ExpenseService(ExpenseRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<Expense> getAll(String username) {
        return repository.findByUserIdOrderByTimestampDesc(findUserId(username));
    }

    public List<Expense> getInRange(String username, long from, long to) {
        return repository.findByUserIdAndTimestampBetweenOrderByTimestampDesc(findUserId(username), from, to);
    }

    public Expense getOne(String username, int id) {
        return findOwn(username, id);
    }

    public Expense add(String username, Expense expense) {
        // id 0 makes save() add a new row instead of changing an old one
        expense.id = 0;
        expense.userId = findUserId(username);
        return repository.save(expense);
    }

    public Expense update(String username, int id, Expense expense) {
        Expense saved = findOwn(username, id);

        saved.name = expense.name;
        saved.amountCents = expense.amountCents;
        saved.timestamp = expense.timestamp;
        saved.street = expense.street;
        return repository.save(saved);
    }

    public void delete(String username, int id) {
        Expense saved = findOwn(username, id);
        repository.delete(saved);
    }

    // the username comes from the token, the expenses are saved with the id of the user
    private int findUserId(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return user.get().id;
    }

    // an expense of another user is answered as not found
    private Expense findOwn(String username, int id) {
        Optional<Expense> found = repository.findById(id);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Expense expense = found.get();
        if (expense.userId != findUserId(username)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return expense;
    }
}
