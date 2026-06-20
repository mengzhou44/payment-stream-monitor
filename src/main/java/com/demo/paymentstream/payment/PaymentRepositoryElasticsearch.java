package com.demo.paymentstream.payment;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.demo.paymentstream.payment.model.PaymentDocument;

public interface PaymentRepositoryElasticsearch extends ElasticsearchRepository<PaymentDocument, String> {
}
