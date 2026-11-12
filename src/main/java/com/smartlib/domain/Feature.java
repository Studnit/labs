package com.smartlib.domain;

/** Library modules that each Library Admin can switch on/off independently. */
public enum Feature {
    QR_ATTENDANCE("QR Attendance", "bi-qr-code-scan", "Check-in/check-out via QR code and manual attendance"),
    SUBSCRIPTIONS("Subscriptions", "bi-card-checklist", "Monthly plans, renewals and expiry management"),
    PAYMENTS("Payments", "bi-cash-coin", "Cash, UPI and online payments with receipts"),
    SEAT_BOOKING("Seat Booking", "bi-grid-3x3-gap", "Seat availability and booking"),
    BOOK_MANAGEMENT("Book Management", "bi-book", "Inventory, issue and return"),
    COMMUNITY_FEED("Community Feed", "bi-chat-square-heart", "Posts, images, likes and comments"),
    DISCUSSION_FORUM("Discussion Forum", "bi-question-circle", "Categories, questions and answers"),
    CHAT("Chat", "bi-chat-dots", "Private messages and group chat"),
    STUDY_GROUPS("Study Groups", "bi-people", "Study groups and shared materials"),
    GUEST_MANAGEMENT("Guest Management", "bi-person-badge", "Visitor registration and logs"),
    REPORTS("Reports", "bi-graph-up", "Analytics and Excel/PDF exports");

    private final String label, icon, description;
    Feature(String label, String icon, String description) { this.label = label; this.icon = icon; this.description = description; }
    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getDescription() { return description; }
}
