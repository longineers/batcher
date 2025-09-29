package com.longineers.batcher.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Entity
@Table(name = "products")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID uuid;
    private String name;
    private String brand;
    private String category;
    private String subcategory;
    private String description;
    private Double price;
    private String currency;
    private Double discountPercent;
    private Double finalPrice;
    private Double rating;
    private Integer reviewCount;
    private Integer stockQuantity;
    private Boolean inStock;
    private String sku;
    private String barcode;
    private Double weightKg;
    private String tags;
    private String imageUrl;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
    private Boolean featured;
    private Double lengthCm;
    private Double widthCm;
    private Double heightCm;
    private Boolean freeShipping;
    private Double shippingCost;
    private Integer estimatedDays;
}