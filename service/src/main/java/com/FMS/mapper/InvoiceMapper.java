package com.FMS.mapper;

import com.FMS.dto.InvoiceDto;
import com.FMS.entity.Invoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    @Mapping(target = "customerUsername", source = "customer.user.username")
    @Mapping(target = "tripId", source = "trip.id")
    @Mapping(target = "paidAmount", expression = "java(invoice.getStatus() == com.FMS.enums.InvoiceStatus.PAID ? invoice.getTotalAmount() : invoice.getPaidAmount())")
    @Mapping(target = "status", expression = "java(invoice.getEffectiveStatus())")
    InvoiceDto toDto(Invoice invoice);
}
