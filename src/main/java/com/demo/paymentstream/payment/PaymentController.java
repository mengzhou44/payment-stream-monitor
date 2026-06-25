package com.demo.paymentstream.payment;

import com.demo.paymentstream.payment.model.Payment;
import com.demo.paymentstream.payment.model.PaymentDocument;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentDlqService dlqService;
    private final PaymentRepositoryElasticsearch esRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Payment createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request.amount(), request.customerId(), request.countryCode());
    }

    @GetMapping("/search")
    public List<PaymentDocument> search(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String countryCode) {
        if (customerId != null && countryCode != null) {
            return esRepository.findByCustomerIdAndCountryCode(customerId, countryCode);
        } else if (customerId != null) {
            return esRepository.findByCustomerId(customerId);
        } else if (countryCode != null) {
            return esRepository.findByCountryCode(countryCode);
        }
        List<PaymentDocument> result = new ArrayList<>();
        esRepository.findAll().forEach(result::add);
        return result;
    }

    @GetMapping("/{id}")
    public Payment getPayment(@PathVariable String id) {
        return paymentService.getPayment(id);
    }

    @GetMapping("/es/{id}")
    public ResponseEntity<PaymentDocument> getPaymentFromEs(@PathVariable String id) {
        return esRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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

    public record CreatePaymentRequest(
            @NotNull @Positive BigDecimal amount,
            @NotBlank String customerId,
            @NotBlank String countryCode
    ) {}
}
