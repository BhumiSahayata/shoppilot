package com.shoppilot.shoppilot.repository;

import com.shoppilot.shoppilot.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByActiveTrue();

    List<Product> findByCategoryIgnoreCaseAndActiveTrue(String category);

    List<Product> findByPriceLessThanEqualAndActiveTrue(double maxPrice);

    @Query("{ 'active': true, '$or': [ " +
           "{ 'name': { '$regex': ?0, '$options': 'i' } }, " +
           "{ 'description': { '$regex': ?0, '$options': 'i' } }, " +
           "{ 'category': { '$regex': ?0, '$options': 'i' } }, " +
           "{ 'tags': { '$regex': ?0, '$options': 'i' } } " +
           "] }")
    List<Product> searchProducts(String keyword);
}
