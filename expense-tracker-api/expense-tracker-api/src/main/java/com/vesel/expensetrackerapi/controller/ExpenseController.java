package com.vesel.expensetrackerapi.controller;

import com.vesel.expensetrackerapi.entity.Expense;
import com.vesel.expensetrackerapi.service.ExpenseService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    // principal holds the username that came out of the token
    // the app sends from and to for every filter, without them the list comes back whole
    @GetMapping
    public List<Expense> getAll(Principal principal,
                                @RequestParam(required = false) Long from,
                                @RequestParam(required = false) Long to) {
        if (from == null || to == null) {
            return service.getAll(principal.getName());
        }
        return service.getInRange(principal.getName(), from, to);
    }

    @GetMapping("/{id}")
    public Expense getOne(Principal principal, @PathVariable int id) {
        return service.getOne(principal.getName(), id);
    }

    @PostMapping
    public Expense add(Principal principal, @RequestBody Expense expense) {
        return service.add(principal.getName(), expense);
    }

    @PutMapping("/{id}")
    public Expense update(Principal principal, @PathVariable int id, @RequestBody Expense expense) {
        return service.update(principal.getName(), id, expense);
    }

    @DeleteMapping("/{id}")
    public void delete(Principal principal, @PathVariable int id) {
        service.delete(principal.getName(), id);
    }
}
