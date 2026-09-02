package com.FMS.repositories;

import com.FMS.entity.Invoice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice,String> {
    List<Invoice> findByCustomerId(String customerId);
    List<Invoice> findByStatus(String status);
    List<Invoice> findByDueDateBefore(LocalDate date);
    List<Invoice> findByTrip_Id(String tripId);
    boolean existsByTrip_Id(String tripId);
    boolean existsByInvoiceNumber(String invoiceNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select invoice from Invoice invoice where invoice.id = :id")
    Optional<Invoice> findByIdForUpdate(@Param("id") String id);
}
