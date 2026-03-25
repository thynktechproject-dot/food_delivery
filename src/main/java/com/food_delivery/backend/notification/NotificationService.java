package com.food_delivery.backend.notification;

import com.food_delivery.backend.entity.Order;
import com.food_delivery.backend.entity.OrderItem;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.entity.Restaurant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    @Autowired
    private EmailNotificationService emailNotificationService;

    public void notifyUserOrderPaid(User user, Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemsHtml.append("<tr><td>")
                    .append(item.getName())
                    .append("</td><td>")
                    .append(item.getQuantity())
                    .append("</td><td>")
                    .append(String.format("%.2f", item.getPrice()))
                    .append("</td></tr>");
        }
        String html = """
            <h2>Order Confirmation</h2>
            <p>Thank you, <b>""" + user.getName() + "</b>, for your payment!</p>"
            + "<p><b>Order ID:</b> " + order.getId() + "</p>"
            + "<table border='1' cellpadding='5' style='border-collapse:collapse;'>"
            + "<tr><th>Item</th><th>Qty</th><th>Price</th></tr>"
            + itemsHtml
            + "</table>"
            + "<p><b>Total:</b> $" + String.format("%.2f", order.getTotalAmount()) + "</p>"
            + "<p>We hope you enjoy your meal!</p>";
        emailNotificationService.sendEmail(user.getEmail(), "Order Paid - Confirmation", html);
    }

    public void notifyRestaurantOrderPaid(Restaurant restaurant, User user, Order order) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemsHtml.append("<tr><td>")
                    .append(item.getName())
                    .append("</td><td>")
                    .append(item.getQuantity())
                    .append("</td><td>")
                    .append(String.format("%.2f", item.getPrice()))
                    .append("</td></tr>");
        }
        String html = """
            <h2>New Paid Order Received</h2>
            <p><b>User:</b> """ + user.getName() + " (" + user.getEmail() + ")</p>"
            + "<p><b>Address:</b> " + (user.getAddress() != null ? user.getAddress() : "N/A") + "</p>"
            + "<p><b>Order ID:</b> " + order.getId() + "</p>"
            + "<table border='1' cellpadding='5' style='border-collapse:collapse;'>"
            + "<tr><th>Item</th><th>Qty</th><th>Price</th></tr>"
            + itemsHtml
            + "</table>"
            + "<p><b>Total:</b> $" + String.format("%.2f", order.getTotalAmount()) + "</p>";
        emailNotificationService.sendEmail(restaurant.getOwner().getEmail(), "New Paid Order Received", html);
    }

    public void notifyDeliveryAgentAssigned(User agent, Order order, User user, Restaurant restaurant) {
        StringBuilder itemsHtml = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            itemsHtml.append("<tr><td>")
                    .append(item.getName())
                    .append("</td><td>")
                    .append(item.getQuantity())
                    .append("</td><td>")
                    .append(String.format("%.2f", item.getPrice()))
                    .append("</td></tr>");
        }
        String html = """
            <h2>New Delivery Assigned</h2>
            <p><b>Order ID:</b> """ + order.getId() + "</p>"
            + "<p><b>User:</b> " + user.getName() + " (" + user.getEmail() + ")</p>"
            + "<p><b>Address:</b> " + (user.getAddress() != null ? user.getAddress() : "N/A") + "</p>"
            + "<p><b>Restaurant:</b> " + restaurant.getName() + "</p>"
            + "<table border='1' cellpadding='5' style='border-collapse:collapse;'>"
            + "<tr><th>Item</th><th>Qty</th><th>Price</th></tr>"
            + itemsHtml
            + "</table>"
            + "<p><b>Total:</b> $" + String.format("%.2f", order.getTotalAmount()) + "</p>";
        emailNotificationService.sendEmail(agent.getEmail(), "New Delivery Assigned", html);
    }

    public void notifyUserOrderDelivered(String userEmail, String receiptDetails) {
        String html = "<h2>Order Delivered</h2><p>Your order has been delivered! Receipt: " + receiptDetails + "</p>";
        emailNotificationService.sendEmail(userEmail, "Order Delivered", html);
    }
}
