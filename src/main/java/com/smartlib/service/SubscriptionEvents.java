package com.smartlib.service;

import com.smartlib.domain.Subscription;

/** Spring application events so seat allocation can react to subscriptions without a service cycle. */
public final class SubscriptionEvents {
    private SubscriptionEvents() {}
    public record Activated(Subscription subscription) {}
    public record Cancelled(Subscription subscription) {}
}
