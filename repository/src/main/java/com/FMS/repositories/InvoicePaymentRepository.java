package com.FMS.repositories;

import com.FMS.entity.InvoicePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, String> {
    List<InvoicePayment> findByInvoice_IdOrderByPaymentDateDescCreatedAtDesc(String invoiceId);

    boolean existsByInvoice_Id(String invoiceId);
}
