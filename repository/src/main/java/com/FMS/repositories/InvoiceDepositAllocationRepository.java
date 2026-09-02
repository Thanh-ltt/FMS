package com.FMS.repositories;

import com.FMS.entity.InvoiceDepositAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceDepositAllocationRepository extends JpaRepository<InvoiceDepositAllocation, String> {
    List<InvoiceDepositAllocation> findByInvoiceId(String invoiceId);

    List<InvoiceDepositAllocation> findByInvoice_Trip_Id(String tripId);

    boolean existsByDepositId(String depositId);
}
