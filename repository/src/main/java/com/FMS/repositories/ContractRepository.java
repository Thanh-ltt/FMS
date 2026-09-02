package com.FMS.repositories;

import com.FMS.entity.Contract;
import com.FMS.enums.ContractStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract,String> {
    Optional<Contract> findByContractCode(String contractCode);
    List<Contract> findByStatus(ContractStatus status);
    List<Contract> findByCustomerId(String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select contract from Contract contract where contract.id = :id")
    Optional<Contract> findByIdForUpdate(@Param("id") String id);
}
