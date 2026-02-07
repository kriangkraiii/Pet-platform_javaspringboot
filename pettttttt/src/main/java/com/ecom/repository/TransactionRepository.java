package com.ecom.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecom.model.Transaction;
import com.ecom.model.UserDtls;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserOrderByCreatedAtDesc(UserDtls user);

    List<Transaction> findByUserAndStatusOrderByCreatedAtDesc(UserDtls user, Transaction.Status status);

    List<Transaction> findByStatusOrderByCreatedAtDesc(Transaction.Status status);

    boolean existsByRefTransactionId(String refTransactionId);
}
