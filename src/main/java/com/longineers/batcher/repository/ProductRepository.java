package com.longineers.batcher.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.longineers.batcher.model.Product;

public interface ProductRepository extends JpaRepository<Product, String> {}
