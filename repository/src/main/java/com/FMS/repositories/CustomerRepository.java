package com.FMS.repositories;

import com.FMS.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer,String> {

    boolean existsByUser_Id(String userId);

    Optional<Customer> findByUser_Username(String username);
}
