package com.demo.paymentstream.payment;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.demo.paymentstream.payment.model.PaymentDocument;

import java.util.List;

public interface PaymentRepositoryElasticsearch extends ElasticsearchRepository<PaymentDocument, String> {

    List<PaymentDocument> findByCustomerId(String customerId);

    List<PaymentDocument> findByCountryCode(String countryCode);

    List<PaymentDocument> findByCustomerIdAndCountryCode(String customerId, String countryCode);
}
