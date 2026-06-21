package com.demo.paymentstream.payment;

import com.demo.paymentstream.payment.model.PaymentDocument;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceElasticsearch {

    private final PaymentRepositoryElasticsearch esRepository;

    public void upsert(JsonNode row) {
        PaymentDocument doc = new PaymentDocument();
        doc.setId(row.get("id").asText());
        // Debezium sends TIMESTAMP WITHOUT TIME ZONE as microseconds since epoch
        long microsSinceEpoch = row.get("payment_date").asLong();
        doc.setPaymentDate(Instant.EPOCH.plus(microsSinceEpoch, ChronoUnit.MICROS));
        doc.setAmount(new BigDecimal(row.get("amount").asText()));
        doc.setCustomerId(row.get("customer_id").asText());
        doc.setCountryCode(row.get("country_code").asText());
        esRepository.save(doc);
        log.info("Upserted payment {} customerId={} countryCode={} amount={}", doc.getId(), doc.getCustomerId(), doc.getCountryCode(), doc.getAmount());
    }

    public void delete(String id) {
        esRepository.deleteById(id);
        log.info("Deleted payment {}", id);
    }
}
