package com.demo.paymentstream.payment;

import com.demo.paymentstream.payment.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentSimulator {

    private final PaymentService paymentService;

    private static final String[] CUSTOMER_IDS = {
            "CUST-001", "CUST-002", "CUST-003", "CUST-004", "CUST-005",
            "CUST-006", "CUST-007", "CUST-008", "CUST-009", "CUST-010"
    };

    private static final String[] COUNTRY_CODES = {"US", "GB", "DE", "FR", "JP", "CA", "AU", "SG"};

    @Scheduled(fixedRate = 2000)
    public void simulatePayment() {
        BigDecimal amount = BigDecimal.valueOf(10 + (ThreadLocalRandom.current().nextDouble() * 990))
                .setScale(4, RoundingMode.HALF_UP);
        String customerId = CUSTOMER_IDS[ThreadLocalRandom.current().nextInt(CUSTOMER_IDS.length)];
        String countryCode = COUNTRY_CODES[ThreadLocalRandom.current().nextInt(COUNTRY_CODES.length)];
        Payment payment = paymentService.createPayment(amount, customerId, countryCode);
        log.debug("Inserted payment {} customerId={} countryCode={} amount={}", payment.getId(), customerId, countryCode, amount);
    }
}
