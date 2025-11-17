package com.store.service;


import com.store.Exceptions.BadRequestException;
import com.store.Exceptions.ResourceNotFoundException;
import com.store.dto.UpdateProfileRequest;
import com.store.dto.UserResponse;
import com.store.entity.User;
import com.store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    public UserResponse getProfile() {
        User user = getCurrentUser();
        return modelMapper.map(user, UserResponse.class);
    }
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = getCurrentUser();
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null && !
                request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        user = userRepository.save(user);
        return modelMapper.map(user, UserResponse.class);
    }
    public User getCurrentUser() {
        String email =
                SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}