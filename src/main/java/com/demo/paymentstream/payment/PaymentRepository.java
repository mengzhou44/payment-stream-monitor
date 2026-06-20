package com.demo.paymentstream.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import com.demo.paymentstream.payment.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, String> {
}
