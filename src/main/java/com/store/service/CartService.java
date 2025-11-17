package com.store.service;

import com.store.Exceptions.BadRequestException;
import com.store.Exceptions.ResourceNotFoundException;
import com.store.dto.CartItemRequest;
import com.store.dto.CartItemResponse;
import com.store.dto.CartResponse;
import com.store.entity.Cart;
import com.store.entity.CartItem;
import com.store.entity.Product;
import com.store.entity.User;
import com.store.repository.CartItemRepository;
import com.store.repository.CartRepository;
import com.store.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final ModelMapper modelMapper;

    // Explicit constructor with debug logging
    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       ProductRepository productRepository,
                       UserService userService,
                       ModelMapper modelMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userService = userService;
        this.modelMapper = modelMapper;
        System.out.println("✅ CartService SUCCESSFULLY CREATED!");
    }

    // Helper method to get or create cart
    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    System.out.println("Creating new cart for user: " + user.getId());
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public CartResponse addItemToCart(CartItemRequest request) {
        User user = userService.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getStock() < request.getQuantity()) {
            throw new BadRequestException("Insufficient stock");
        }

        CartItem existingItem =
                cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                        .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            cartItemRepository.save(existingItem);
        } else {
            CartItem cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
            cartItemRepository.save(cartItem);
        }

        return getCart();
    }

    @Transactional
    public CartResponse updateCartItem(Long itemId, Integer quantity) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }

        if (cartItem.getProduct().getStock() < quantity) {
            throw new BadRequestException("Insufficient stock");
        }

        cartItem.setQuantity(quantity);
        cartItemRepository.save(cartItem);

        return getCart();
    }

    @Transactional
    public CartResponse removeItemFromCart(Long itemId) {
        if (!cartItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Cart item not found");
        }
        cartItemRepository.deleteById(itemId);
        return getCart();
    }

    public CartResponse getCart() {
        User user = userService.getCurrentUser();
        Cart cart = getOrCreateCart(user);

        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setItems(cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList()));

        BigDecimal total = response.getItems().stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        response.setTotalAmount(total);

        return response;
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        CartItemResponse response = modelMapper.map(item, CartItemResponse.class);
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        response.setSubtotal(subtotal);
        return response;
    }
}