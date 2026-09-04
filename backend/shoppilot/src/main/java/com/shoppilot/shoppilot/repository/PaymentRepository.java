package com.shoppilot.shoppilot.repository;

import com.shoppilot.shoppilot.model.Payment;
import com.shoppilot.shoppilot.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {

    List<Payment> findByOrderId(String orderId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    List<Payment> findByStatus(PaymentStatus status);
}
