package com.food_delivery.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.food_delivery.backend.dto.CreateUserRequest;
import com.food_delivery.backend.entity.*;
import com.food_delivery.backend.enums.RestaurantStatus;
import com.food_delivery.backend.repository.*;
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
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ========================= HELPER =========================

    private Restaurant buildApprovedRestaurant(User owner, String name) {
        return restaurantRepository.save(
                Restaurant.builder()
                        .name(name)
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .status(RestaurantStatus.APPROVED)
                        .active(true)
                        .build()
        );
    }

    private Restaurant buildPendingRestaurant(User owner, String name) {
        return restaurantRepository.save(
                Restaurant.builder()
                        .name(name)
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .status(RestaurantStatus.PENDING)
                        .active(true)
                        .build()
        );
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

    private String bearerToken(String email) {
        return "Bearer " + jwtUtil.generateToken(email, "USER", 0);
    }

    private JsonNode login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response);
    }

    // ========================= TESTS =========================

    @Test
    void publicRestaurantListingOnlyReturnsApprovedRestaurants() throws Exception {
        User owner1 = saveUser("owner1@test.com", Role.RESTAURANT_OWNER, true);
        User owner2 = saveUser("owner2@test.com", Role.RESTAURANT_OWNER, true);

        buildApprovedRestaurant(owner1, "Approved Kitchen");
        buildPendingRestaurant(owner2, "Pending Kitchen");

        mockMvc.perform(get("/api/public/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void publicMenuOnlyReturnsAvailableItemsFromApprovedRestaurant() throws Exception {
        User owner = saveUser("owner@test.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = buildApprovedRestaurant(owner, "Menu Kitchen");

        menuItemRepository.save(MenuItem.builder()
                .name("Visible Item")
                .price(100.0)
                .available(true)
                .restaurant(restaurant)
                .build());

        menuItemRepository.save(MenuItem.builder()
                .name("Hidden Item")
                .price(120.0)
                .available(false)
                .restaurant(restaurant)
                .build());

        mockMvc.perform(get("/api/public/restaurants/{id}/menu", restaurant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void customerCannotAddItemsFromArchivedRestaurant() throws Exception {
        User owner = saveUser("owner@test.com", Role.RESTAURANT_OWNER, true);

        Restaurant restaurant = restaurantRepository.save(
                Restaurant.builder()
                        .name("Archived")
                        .location("City")
                        .cuisineType("Indian")
                        .owner(owner)
                        .status(RestaurantStatus.APPROVED)
                        .active(false)
                        .build()
        );

        MenuItem item = menuItemRepository.save(MenuItem.builder()
                .name("Burger")
                .price(100)
                .available(true)
                .restaurant(restaurant)
                .build());

        User customer = saveUser("cust@test.com", Role.USER, true);

        mockMvc.perform(post("/api/user/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "restaurantId": %d,
                                  "menuItemId": %d,
                                  "quantity": 1
                                }
                                """.formatted(restaurant.getId(), item.getId()))
                        .header("Authorization", bearerToken(customer.getEmail())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archivedRestaurantIsRemovedFromPublicAccess() throws Exception {
        User owner = saveUser("owner@test.com", Role.RESTAURANT_OWNER, true);
        Restaurant restaurant = buildApprovedRestaurant(owner, "Archive Kitchen");

        mockMvc.perform(delete("/api/restaurant/{id}", restaurant.getId())
                        .header("Authorization", bearerToken(owner.getEmail())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/restaurants/{id}", restaurant.getId()))
                .andExpect(status().isNotFound());
    }
}