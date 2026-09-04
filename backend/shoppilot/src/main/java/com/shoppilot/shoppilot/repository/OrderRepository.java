package com.shoppilot.shoppilot.repository;

import com.shoppilot.shoppilot.model.Order;
import com.shoppilot.shoppilot.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByAiAssistedTrue();

    List<Order> findAllByOrderByCreatedAtDesc();
}
