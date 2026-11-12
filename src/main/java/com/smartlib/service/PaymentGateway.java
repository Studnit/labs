package com.smartlib.service;

import com.smartlib.domain.Payment;

/** Pluggable online payment provider (Razorpay, Stripe, PayU...). Implement and mark as a Spring bean. */
public interface PaymentGateway {
    record Result(boolean success, String reference) {}
    Result charge(Payment payment);
}
