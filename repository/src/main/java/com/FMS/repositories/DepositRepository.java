package com.FMS.repositories;

import com.FMS.entity.Deposit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, String> {
    boolean existsByReceiptNumber(String receiptNumber);

    boolean existsByCustomerId(String customerId);

    boolean existsByContractId(String contractId);

    boolean existsByTripId(String tripId);

    List<Deposit> findAllByOrderByReceivedDateDescCreatedAtDesc();

    List<Deposit> findByCustomerIdOrderByReceivedDateAscCreatedAtAsc(String customerId);

    List<Deposit> findByContractIdOrderByReceivedDateDescCreatedAtDesc(String contractId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deposit d where d.id = :id")
    Optional<Deposit> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Deposit d where d.customer.id = :customerId order by d.receivedDate asc, d.createdAt asc")
    List<Deposit> findByCustomerIdForUpdate(@Param("customerId") String customerId);
}
