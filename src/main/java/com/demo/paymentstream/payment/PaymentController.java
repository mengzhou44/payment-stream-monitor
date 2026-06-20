package com.demo.paymentstream.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.demo.paymentstream.payment.model.Payment;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentDlqService dlqService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment createPayment(@RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request.amount());
    }

    @GetMapping("/{id}")
    public Payment getPayment(@PathVariable String id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/dlq/count")
    public String dlqCount() {
        return dlqService.count() + " message(s) in DLQ";
    }

    @PostMapping("/dlq/replay")
    public String replayDlq() {
        int count = dlqService.replay();
        return count + " message(s) replayed from DLQ to original topic";
    }

    public record CreatePaymentRequest(BigDecimal amount) {}
}
