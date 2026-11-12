package com.smartlib;

import com.smartlib.domain.Feature;
import com.smartlib.feature.FeatureInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureInterceptorTest {
    @Test
    void mapsEveryModulePrefixToItsFeature() {
        assertEquals(Feature.CHAT, FeatureInterceptor.match("/chat"));
        assertEquals(Feature.CHAT, FeatureInterceptor.match("/chat/api/rooms/1/messages"));
        assertEquals(Feature.COMMUNITY_FEED, FeatureInterceptor.match("/feed/api/5/like"));
        assertEquals(Feature.COMMUNITY_FEED, FeatureInterceptor.match("/files/feed/x.png"));
        assertEquals(Feature.STUDY_GROUPS, FeatureInterceptor.match("/groups/materials/3"));
        assertEquals(Feature.QR_ATTENDANCE, FeatureInterceptor.match("/attendance/scan/abc"));
        assertEquals(Feature.REPORTS, FeatureInterceptor.match("/reports/export/payments/xlsx"));
    }

    @Test
    void doesNotMatchUnrelatedOrLookalikePaths() {
        assertNull(FeatureInterceptor.match("/dashboard"));
        assertNull(FeatureInterceptor.match("/chatroom"));      // prefix must end at a path boundary
        assertNull(FeatureInterceptor.match("/announcements"));
        assertNull(FeatureInterceptor.match("/moderation/report"));
    }
}
