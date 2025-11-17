package com.store.service;

import com.store.Exceptions.ResourceNotFoundException;
import com.store.dto.ProductRequest;
import com.store.dto.ProductResponse;
import com.store.entity.Category;
import com.store.entity.Product;
import com.store.repository.CategoryRepository;
import com.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;
    private final FileStorageService fileStorageService;

    @Value("${server.port:8081}")
    private String serverPort;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        System.out.println("🔥 Creating product: " + request.getName());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        // Handle image upload
        MultipartFile file = request.getImage();
        if (file != null && !file.isEmpty()) {
            String imageUrl = fileStorageService.saveFile(file);
            product.setImageUrl(imageUrl);
            System.out.println("✅ Image saved: " + imageUrl);
        }

        product.setCategory(category);

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        System.out.println("🔥 Updating product ID: " + id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());

        // If a new image is uploaded, replace it
        MultipartFile file = request.getImage();
        if (file != null && !file.isEmpty()) {
            // Delete old image if exists
            if (product.getImageUrl() != null) {
                fileStorageService.deleteFile(product.getImageUrl());
            }
            String imageUrl = fileStorageService.saveFile(file);
            product.setImageUrl(imageUrl);
            System.out.println("✅ Image updated: " + imageUrl);
        }

        product.setCategory(category);

        product = productRepository.save(product);
        return mapToResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Delete image file if exists
        if (product.getImageUrl() != null) {
            fileStorageService.deleteFile(product.getImageUrl());
            System.out.println("🗑️ Deleted image for product: " + id);
        }

        productRepository.deleteById(id);
        System.out.println("🗑️ Deleted product: " + id);
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToResponse(product);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Helper method to map Product to ProductResponse with absolute image URL
    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = modelMapper.map(product, ProductResponse.class);

        // Convert relative URL to absolute URL for frontend
        if (product.getImageUrl() != null) {
            String imageUrl = product.getImageUrl();

            // If it's a relative path, convert to absolute URL
            if (imageUrl.startsWith("/uploads/") || imageUrl.startsWith("uploads/")) {
                if (!imageUrl.startsWith("/")) {
                    imageUrl = "/" + imageUrl;
                }
                String absoluteUrl = "http://localhost:" + serverPort + imageUrl;
                response.setImageUrl(absoluteUrl);
                System.out.println("🖼️ Image URL: " + absoluteUrl);
            } else {
                // Already an absolute URL or external URL
                response.setImageUrl(imageUrl);
            }
        }

        return response;
    }
}