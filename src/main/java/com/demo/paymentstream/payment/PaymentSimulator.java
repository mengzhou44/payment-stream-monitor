package com.demo.paymentstream.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.demo.paymentstream.payment.model.Payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentSimulator {

    private final PaymentService paymentService;
    private final Random random = new Random();

    @Scheduled(fixedRate = 2000)
    public void simulatePayment() {
        BigDecimal amount = BigDecimal.valueOf(10 + (random.nextDouble() * 990))
                .setScale(4, RoundingMode.HALF_UP);
        Payment payment = paymentService.createPayment(amount);
        log.debug("Inserted payment {} amount={}", payment.getId(), amount);
    }
}
