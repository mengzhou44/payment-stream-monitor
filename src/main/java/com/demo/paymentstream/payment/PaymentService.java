package com.demo.paymentstream.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.demo.paymentstream.payment.model.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment createPayment(BigDecimal amount, String customerId, String countryCode) {
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setPaymentDate(LocalDateTime.now());
        payment.setAmount(amount);
        payment.setCustomerId(customerId);
        payment.setCountryCode(countryCode);
        return paymentRepository.save(payment);
    }

    public Payment getPayment(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
