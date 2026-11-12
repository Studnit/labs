package com.smartlib.service;

import com.smartlib.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Active subscribers cannot buy again until the last 7 days. */
class SubscriptionRuleTest {
    private PaymentService with(long daysLeft) {
        SubscriptionService subs = mock(SubscriptionService.class);
        when(subs.daysLeft(any())).thenReturn(daysLeft);
        com.smartlib.security.CurrentUser ctx = mock(com.smartlib.security.CurrentUser.class);
        when(ctx.id()).thenReturn(1L);
        return new PaymentService(null, null, subs, null, null, null, null, ctx);
    }
    @Test void blockedWhenManyDaysLeft() { assertThrows(BusinessException.class, () -> with(20).assertCanBuy()); assertThrows(BusinessException.class, () -> with(8).assertCanBuy()); }
    @Test void allowedInLastWeek() { assertDoesNotThrow(() -> with(7).assertCanBuy()); assertDoesNotThrow(() -> with(1).assertCanBuy()); }
    @Test void allowedWithNoSubscription() { assertDoesNotThrow(() -> with(0).assertCanBuy()); }
}
