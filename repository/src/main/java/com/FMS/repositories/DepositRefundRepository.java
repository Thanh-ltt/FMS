package com.FMS.repositories;

import com.FMS.entity.DepositRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositRefundRepository extends JpaRepository<DepositRefund, String> {
    List<DepositRefund> findByDepositIdOrderByRefundDateDescCreatedAtDesc(String depositId);

    boolean existsByDepositId(String depositId);
}
