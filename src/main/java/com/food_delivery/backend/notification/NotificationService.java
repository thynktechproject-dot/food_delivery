package com.food_delivery.backend.notification;

import com.food_delivery.backend.entity.Order;
import com.food_delivery.backend.entity.OrderItem;
import com.food_delivery.backend.entity.Payment;
import com.food_delivery.backend.entity.Restaurant;
import com.food_delivery.backend.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final EmailNotificationService emailNotificationService;

    public NotificationService(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    public void notifyUserOrderPaid(User user, Order order, Payment payment) {
        String html = emailShell(
                "Payment Confirmation",
                "Dear " + safe(user.getName()) + ", your payment was received successfully.",
                section("Order Details", orderDetailsHtml(order))
                        + section("Payment Details", paymentDetailsHtml(payment))
                        + section("Items", itemsTable(order))
        );

        emailNotificationService.sendEmail(user.getEmail(), "Your Order is Confirmed", html);
    }

    public void notifyRestaurantOrderPaid(Restaurant restaurant, User user, Order order, Payment payment) {
        String userDetails = row("Name", safe(user.getName()))
                + row("Email", safe(user.getEmail()))
                + row("Phone", safe(user.getPhone()))
                + row("Address", safe(user.getAddress()));

        String html = emailShell(
                "New Paid Order",
                "A customer has placed and paid for an order.",
                section("Restaurant", table(row("Name", safe(restaurant.getName()))
                        + row("Location", safe(restaurant.getLocation()))))
                        + section("Customer Details", table(userDetails))
                        + section("Order Details", orderDetailsHtml(order))
                        + section("Payment Details", paymentDetailsHtml(payment))
                        + section("Order Items", itemsTable(order))
        );

        emailNotificationService.sendEmail(restaurant.getOwner().getEmail(), "New Paid Order Received", html);
    }

    public void notifyDeliveryAgentAssigned(User agent, Order order, User user, Restaurant restaurant) {
        String pickupAndDrop = row("Pickup Restaurant", safe(restaurant.getName()))
                + row("Pickup Location", safe(restaurant.getLocation()))
                + row("Customer Name", safe(user.getName()))
                + row("Customer Phone", safe(user.getPhone()))
                + row("Delivery Address", safe(user.getAddress()));

        String html = emailShell(
                "Delivery Assignment",
                "You have been assigned a delivery order.",
                section("Assignment Details", table(row("Delivery Agent", safe(agent.getName()))
                        + row("Agent Email", safe(agent.getEmail()))
                        + row("Order ID", String.valueOf(order.getId()))
                        + row("Order Status", order.getOrderStatus().name())))
                        + section("Pickup and Customer", table(pickupAndDrop))
                        + section("Order Items", itemsTable(order))
                        + section("Order Summary", orderDetailsHtml(order))
        );

        emailNotificationService.sendEmail(agent.getEmail(), "New Delivery Assignment", html);
    }

    public void notifyUserOrderDelivered(String userEmail, String receiptDetails) {
        String html = emailShell(
                "Order Delivered",
                "Your order has been delivered successfully.",
                section("Receipt", "<p style='margin:0;line-height:1.6;'>" + safe(receiptDetails) + "</p>")
        );
        emailNotificationService.sendEmail(userEmail, "Order Delivered", html);
    }

    private String orderDetailsHtml(Order order) {
        return table(
                row("Order ID", String.valueOf(order.getId()))
                        + row("Status", order.getOrderStatus().name())
                        + row("Total Amount", formatAmount(order.getTotalAmount()))
                        + row("Placed At", formatDateTime(order.getCreatedAt()))
        );
    }

    private String paymentDetailsHtml(Payment payment) {
        return table(
                row("Payment Status", payment.getStatus().name())
                        + row("Amount", formatAmount(payment.getAmount()))
                        + row("Razorpay Order ID", safe(payment.getRazorpayOrderId()))
                        + row("Razorpay Payment ID", safe(payment.getRazorpayPaymentId()))
                        + row("Updated At", formatDateTime(payment.getUpdatedAt()))
        );
    }

    private String itemsTable(Order order) {
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            double lineTotal = item.getQuantity() * item.getPrice();
            rows.append("<tr>")
                    .append("<td style='padding:10px;border:1px solid #dfe3e8;'>").append(safe(item.getName())).append("</td>")
                    .append("<td style='padding:10px;border:1px solid #dfe3e8;text-align:center;'>").append(item.getQuantity()).append("</td>")
                    .append("<td style='padding:10px;border:1px solid #dfe3e8;text-align:right;'>").append(formatAmount(item.getPrice())).append("</td>")
                    .append("<td style='padding:10px;border:1px solid #dfe3e8;text-align:right;'>").append(formatAmount(lineTotal)).append("</td>")
                    .append("</tr>");
        }

        return "<table style='width:100%;border-collapse:collapse;font-size:14px;'>"
                + "<thead><tr style='background:#f4f6f8;'>"
                + "<th style='padding:10px;border:1px solid #dfe3e8;text-align:left;'>Item</th>"
                + "<th style='padding:10px;border:1px solid #dfe3e8;text-align:center;'>Qty</th>"
                + "<th style='padding:10px;border:1px solid #dfe3e8;text-align:right;'>Unit Price</th>"
                + "<th style='padding:10px;border:1px solid #dfe3e8;text-align:right;'>Line Total</th>"
                + "</tr></thead><tbody>"
                + rows
                + "</tbody></table>";
    }

    private String emailShell(String title, String intro, String body) {
        return "<div style='font-family:Arial,Helvetica,sans-serif;background:#f5f7fb;padding:24px;'>"
                + "<div style='max-width:720px;margin:0 auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:10px;padding:24px;'>"
                + "<h2 style='margin:0 0 12px 0;color:#1f2937;'>" + title + "</h2>"
                + "<p style='margin:0 0 20px 0;color:#374151;line-height:1.6;'>" + intro + "</p>"
                + body
                + "<p style='margin:20px 0 0 0;color:#6b7280;font-size:12px;'>This is an automated notification from Food Delivery.</p>"
                + "</div></div>";
    }

    private String section(String heading, String content) {
        return "<div style='margin-top:18px;'>"
                + "<h3 style='margin:0 0 8px 0;color:#111827;font-size:16px;'>" + heading + "</h3>"
                + content
                + "</div>";
    }

    private String table(String rows) {
        return "<table style='width:100%;border-collapse:collapse;font-size:14px;'>" + rows + "</table>";
    }

    private String row(String key, String value) {
        return "<tr>"
                + "<td style='width:38%;padding:10px;border:1px solid #dfe3e8;background:#f9fafb;font-weight:600;'>" + key + "</td>"
                + "<td style='padding:10px;border:1px solid #dfe3e8;'>" + safe(value) + "</td>"
                + "</tr>";
    }

    private String formatAmount(double amount) {
        return "INR " + String.format("%.2f", amount);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    private String safe(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value;
    }
}
