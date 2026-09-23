package com.vesel.expensetrackerapi.repository;

import com.vesel.expensetrackerapi.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Integer> {

    List<Expense> findByUserIdOrderByTimestampDesc(int userId);

    // between takes both ends, so the same method works for all the filters
    List<Expense> findByUserIdAndTimestampBetweenOrderByTimestampDesc(int userId, long from, long to);
}
