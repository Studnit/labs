package com.smartlib.service;

import com.smartlib.config.AppProperties;
import com.smartlib.domain.Payment;
import com.smartlib.web.BusinessException;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Development gateway that always succeeds. Disabled with GATEWAY_MOCK=false; add a real PaymentGateway bean annotated @Primary before go-live. */
@Component

public class MockPaymentGateway implements PaymentGateway {
    private final AppProperties props;


    public MockPaymentGateway(AppProperties props) {
        this.props = props;
    }
    @Override
    public Result charge(Payment payment) {
        if (!props.gatewayMock()) throw new BusinessException("Online payments are not configured for this deployment");
        return new Result(true, "MOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
}
