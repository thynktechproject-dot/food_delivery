package com.food_delivery.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.food_delivery.backend.dto.CreateUserRequest;
import com.food_delivery.backend.entity.MenuItem;
import com.food_delivery.backend.entity.Order;
import com.food_delivery.backend.entity.OrderStatus;
import com.food_delivery.backend.entity.Restaurant;
import com.food_delivery.backend.entity.Role;
import com.food_delivery.backend.entity.User;
import com.food_delivery.backend.repository.CartRepository;
import com.food_delivery.backend.repository.MenuItemRepository;
import com.food_delivery.backend.repository.OrderRepository;
import com.food_delivery.backend.repository.OtpCodeRepository;
import com.food_delivery.backend.repository.RefreshTokenRepository;
import com.food_delivery.backend.repository.RestaurantRepository;
import com.food_delivery.backend.repository.UserRepository;
import com.food_delivery.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private OtpCodeRepository otpCodeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        otpCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerAdminRequiresAuthentication() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Admin");
        request.setEmail("admin@example.com");
        request.setPassword("Password123");

        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void publicRegistrationRejectsPrivilegedRoles() throws Exception {
        CreateUserRequest request = new CreateUserRequest();
        request.setName("Owner");
        request.setEmail("owner-register@example.com");
        request.setPassword("Password123");
        request.setRole(Role.RESTAURANT_OWNER);

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Public registration is only available for USER accounts"));
    }

    @Test
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        User user = saveUser("plain-user@example.com", Role.USER, true);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearerToken(user.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to perform this action"));
    }

    @Test
    void blockedUsersCannotUseExistingJwt() throws Exception {
        User blockedUser = saveUser("blocked@example.com", Role.USER, false);

        mockMvc.perform(get("/api/user/cart")
                        .header("Authorization", bearerToken(blockedUser.getEmail())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotPayForAnotherUsersOrder() throws Exception {
        User owner = saveUser("owner@example.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .build()
        );

        User orderOwner = saveUser("customer1@example.com", Role.USER, true);
        User otherUser = saveUser("customer2@example.com", Role.USER, true);

        Order order = orderRepository.save(
                Order.builder()
                        .userId(orderOwner.getId())
                        .restaurantId(restaurant.getId())
                        .totalAmount(350.0)
                        .orderStatus(OrderStatus.CREATED)
                        .build()
        );

        mockMvc.perform(post("/api/user/payments/{orderId}", order.getId())
                        .header("Authorization", bearerToken(otherUser.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only pay for your own orders"));
    }

    @Test
    void restaurantOwnerCannotAccessDeliveryEndpoints() throws Exception {
        User owner = saveUser("owner-no-delivery@example.com", Role.RESTAURANT_OWNER, true);

        mockMvc.perform(get("/api/delivery/orders")
                        .header("Authorization", bearerToken(owner.getEmail())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not allowed to perform this action"));
    }

    @Test
    void publicRestaurantListingOnlyReturnsApprovedRestaurants() throws Exception {
        User ownerOne = saveUser("approved-owner@example.com", Role.RESTAURANT_OWNER, true);
        User ownerTwo = saveUser("pending-owner@example.com", Role.RESTAURANT_OWNER, true);

        restaurantRepository.save(
                Restaurant.builder()
                        .name("Approved Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(ownerOne)
                        .approved(true)
                        .build()
        );

        restaurantRepository.save(
                Restaurant.builder()
                        .name("Pending Kitchen")
                        .location("City")
                        .cuisineType("Italian")
                        .owner(ownerTwo)
                        .approved(false)
                        .build()
        );

        mockMvc.perform(get("/api/public/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Approved Kitchen"));
    }

    @Test
    void publicMenuOnlyReturnsAvailableItemsFromApprovedRestaurant() throws Exception {
        User owner = saveUser("menu-owner@example.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Public Menu")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .build()
        );

        menuItemRepository.save(
                MenuItem.builder()
                        .name("Visible Item")
                        .price(100.0)
                        .available(true)
                        .restaurant(restaurant)
                        .build()
        );

        menuItemRepository.save(
                MenuItem.builder()
                        .name("Hidden Item")
                        .price(120.0)
                        .available(false)
                        .restaurant(restaurant)
                        .build()
        );

        mockMvc.perform(get("/api/public/restaurants/{id}/menu", restaurant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Visible Item"));
    }

    @Test
    void restaurantOwnerCannotModifyAnotherOwnersMenu() throws Exception {
        User ownerOne = saveUser("owner1@example.com", Role.RESTAURANT_OWNER, true);
        User ownerTwo = saveUser("owner2@example.com", Role.RESTAURANT_OWNER, true);

        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Owner One Restaurant")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(ownerOne)
                        .approved(true)
                        .build()
        );

        String body = """
                {
                  "name": "Paneer Tikka",
                  "price": 199.0
                }
                """;

        mockMvc.perform(post("/menu/{restaurantId}", restaurant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", bearerToken(ownerTwo.getEmail())))
                .andExpect(status().isNotFound());
    }

    @Test
    void restaurantOwnerCanFetchOwnOrderQueue() throws Exception {
        User owner = saveUser("queue-owner@example.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Queue Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .build()
        );

        User customer = saveUser("queue-customer@example.com", Role.USER, true);
        orderRepository.save(
                Order.builder()
                        .userId(customer.getId())
                        .restaurantId(restaurant.getId())
                        .totalAmount(420.0)
                        .orderStatus(OrderStatus.PAID)
                        .build()
        );

        mockMvc.perform(get("/api/restaurant/orders")
                        .header("Authorization", bearerToken(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].restaurantId").value(restaurant.getId()));
    }

    @Test
    void ownerCanToggleMenuAvailability() throws Exception {
        User owner = saveUser("availability-owner@example.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Availability Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .build()
        );

        MenuItem menuItem = menuItemRepository.save(
                MenuItem.builder()
                        .name("Samosa")
                        .price(80.0)
                        .available(true)
                        .restaurant(restaurant)
                        .build()
        );

        mockMvc.perform(put("/menu/item/{menuItemId}/availability", menuItem.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "available": false
                                }
                                """)
                        .header("Authorization", bearerToken(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void customerCannotAddItemsFromArchivedRestaurant() throws Exception {
        User owner = saveUser("archived-cart-owner@example.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Archived Cart Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .active(false)
                        .build()
        );

        MenuItem menuItem = menuItemRepository.save(
                MenuItem.builder()
                        .name("Unavailable Burger")
                        .price(150.0)
                        .available(true)
                        .restaurant(restaurant)
                        .build()
        );

        User customer = saveUser("archived-cart-customer@example.com", Role.USER, true);

        mockMvc.perform(post("/api/user/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": %d,
                                  "menuItemId": %d,
                                  "quantity": 1
                                }
                                """.formatted(restaurant.getId(), menuItem.getId()))
                        .header("Authorization", bearerToken(customer.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Menu item belongs to a restaurant that is not available"));
    }

    @Test
    void customerCanClearCartWithoutPersistingInvalidRestaurantId() throws Exception {
        User owner = saveUser("cart-owner@example.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Cart Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .build()
        );

        MenuItem menuItem = menuItemRepository.save(
                MenuItem.builder()
                        .name("Burger")
                        .price(150.0)
                        .available(true)
                        .restaurant(restaurant)
                        .build()
        );

        User customer = saveUser("cart-customer@example.com", Role.USER, true);

        mockMvc.perform(post("/api/user/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": %d,
                                  "menuItemId": %d,
                                  "quantity": 1
                                }
                                """.formatted(restaurant.getId(), menuItem.getId()))
                        .header("Authorization", bearerToken(customer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1));

        mockMvc.perform(delete("/api/user/cart/clear")
                        .header("Authorization", bearerToken(customer.getEmail())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/cart")
                        .header("Authorization", bearerToken(customer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.restaurantId").isEmpty())
                .andExpect(jsonPath("$.data.totalAmount").value(0.0));
    }

    @Test
    void restaurantOwnerCannotReadAnotherOwnersMenu() throws Exception {
        User ownerOne = saveUser("menu-read-owner1@example.com", Role.RESTAURANT_OWNER, true);
        User ownerTwo = saveUser("menu-read-owner2@example.com", Role.RESTAURANT_OWNER, true);

        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Private Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(ownerOne)
                        .approved(true)
                        .build()
        );

        mockMvc.perform(get("/menu/{restaurantId}", restaurant.getId())
                        .header("Authorization", bearerToken(ownerTwo.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Restaurant not found"));
    }

    @Test
    void deletedUserTokenBecomesInvalidAndEmailCanBeReused() throws Exception {
        User user = saveUser("delete-me@example.com", Role.USER, true);

        mockMvc.perform(delete("/api/user/profile/me")
                        .header("Authorization", bearerToken(user.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile deleted"));

        mockMvc.perform(get("/api/user/profile/me")
                        .header("Authorization", bearerToken(user.getEmail())))
                .andExpect(status().isUnauthorized());

        CreateUserRequest request = new CreateUserRequest();
        request.setName("Recreated User");
        request.setEmail("delete-me@example.com");
        request.setPassword("Password123");

        mockMvc.perform(post("/api/auth/register/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("delete-me@example.com"));
    }

    @Test
    void restaurantOwnerCannotDeleteAccountWhileRestaurantExists() throws Exception {
        User owner = saveUser("owner-delete@example.com", Role.RESTAURANT_OWNER, true);
        restaurantRepository.save(
                Restaurant.builder()
                        .name("Delete Guard Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .build()
        );

        mockMvc.perform(delete("/api/user/profile/me")
                        .header("Authorization", bearerToken(owner.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Restaurant owners must remove or transfer their restaurant before deleting the account"));
    }

    @Test
    void placingOrderClearsCartRestaurantContext() throws Exception {
        User owner = saveUser("checkout-owner@example.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Checkout Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .build()
        );

        MenuItem menuItem = menuItemRepository.save(
                MenuItem.builder()
                        .name("Wrap")
                        .price(220.0)
                        .available(true)
                        .restaurant(restaurant)
                        .build()
        );

        User customer = saveUser("checkout-user@example.com", Role.USER, true);

        mockMvc.perform(post("/api/user/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": %d,
                                  "menuItemId": %d,
                                  "quantity": 2
                                }
                                """.formatted(restaurant.getId(), menuItem.getId()))
                        .header("Authorization", bearerToken(customer.getEmail())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/user/orders")
                        .header("Authorization", bearerToken(customer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CREATED"));

        mockMvc.perform(get("/api/user/cart")
                        .header("Authorization", bearerToken(customer.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.restaurantId").isEmpty());
    }

    @Test
    void passwordChangeInvalidatesOldAccessAndRefreshTokens() throws Exception {
        User user = saveUser("security-user@example.com", Role.USER, true);

        JsonNode loginResponse = login("security-user@example.com", "Password123");
        String accessToken = loginResponse.get("token").asText();
        String refreshToken = loginResponse.get("refreshToken").asText();

        mockMvc.perform(put("/api/user/profile/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentPassword": "Password123",
                                  "newPassword": "Password456"
                                }
                                """)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/user/profile/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is invalid"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "security-user@example.com",
                                  "password": "Password456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refreshAndLogoutManageSessionTokens() throws Exception {
        saveUser("refresh-user@example.com", Role.USER, true);

        JsonNode loginResponse = login("refresh-user@example.com", "Password123");
        String refreshToken = loginResponse.get("refreshToken").asText();

        JsonNode refreshResponse = refresh(refreshToken);
        String rotatedRefreshToken = refreshResponse.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refresh token is invalid"));
    }

    @Test
    void archivedRestaurantIsRemovedFromPublicAccessAndOwnerCanDeleteAccountAfterwards() throws Exception {
        User owner = saveUser("archived-owner@example.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Archive Kitchen")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .approved(true)
                        .active(true)
                        .build()
        );

        mockMvc.perform(delete("/api/restaurant/{id}", restaurant.getId())
                        .header("Authorization", bearerToken(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restaurant deleted"));

        mockMvc.perform(get("/api/public/restaurants/{id}", restaurant.getId()))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/user/profile/me")
                        .header("Authorization", bearerToken(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile deleted"));
    }

    private User saveUser(String email, Role role, boolean active) {
        return userRepository.save(
                User.builder()
                        .name(email)
                        .email(email)
                        .password(passwordEncoder.encode("Password123"))
                        .role(role)
                        .active(active)
                        .build()
        );
    }

    private JsonNode login(String email, String password) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody);
    }

    private JsonNode refresh(String refreshToken) throws Exception {
        String responseBody = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(responseBody);
    }

    private String bearerToken(String email) {
        return "Bearer " + jwtUtil.generateToken(email);
    }

    @Test
    void generateOtpReturnsSixDigitOtpForUser() throws Exception {
        saveUser("customer@test.com", Role.USER, true);

        String response = mockMvc.perform(post("/api/auth/generate-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "customer@test.com",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.identifier").value("customer@test.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertTrue(json.get("otp").asText().matches("\\d{6}"));
    }

    @Test
    void generateOtpReturnsSixDigitOtpForRestaurantOwner() throws Exception {
        saveUser("owner@test.com", Role.RESTAURANT_OWNER, true);

        String response = mockMvc.perform(post("/api/auth/generate-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner@test.com",
                                  "role": "RESTAURANT_OWNER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RESTAURANT_OWNER"))
                .andExpect(jsonPath("$.identifier").value("owner@test.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertTrue(json.get("otp").asText().matches("\\d{6}"));
    }
}
