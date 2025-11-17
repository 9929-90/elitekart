package com.store.service;


import com.store.Exceptions.BadRequestException;
import com.store.dto.AuthResponse;
import com.store.dto.LoginRequest;
import com.store.dto.RegisterRequest;
import com.store.entity.Cart;
import com.store.entity.User;
import com.store.enums.Role;
import com.store.repository.CartRepository;
import com.store.repository.UserRepository;
import com.store.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(Role.USER);
        user = userRepository.save(user);
        // Create cart for user
        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);
        // Authenticate and generate token
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),
                        request.getPassword())
        );
        String token = tokenProvider.generateToken(authentication);
        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(),
                user.getName(), user.getRole().name());
    }
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(),
                        request.getPassword())
        );
        String token = tokenProvider.generateToken(authentication);
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));
        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(),
                user.getName(), user.getRole().name());
    }
}