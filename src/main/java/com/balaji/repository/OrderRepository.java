package com.balaji.repository;

import com.balaji.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerPhoneOrderByCreatedAtDesc(String phone);
    List<Order> findAllByOrderByCreatedAtDesc();
}
