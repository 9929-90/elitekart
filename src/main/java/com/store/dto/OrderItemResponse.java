package com.store.dto;

import lombok.Data;
import java.math.BigDecimal;
@Data
public class OrderItemResponse {
    private Long id;
    private ProductResponse product;
    private Integer quantity;
    private BigDecimal price;
}