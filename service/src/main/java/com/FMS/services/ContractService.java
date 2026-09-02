package com.FMS.services;

import com.FMS.dto.ContractDto;
import com.FMS.dto.request.ContractCreationRequest;
import com.FMS.enums.ContractStatus;

import java.util.List;

public interface ContractService {
    ContractDto create(ContractCreationRequest request);

    ContractDto update(String id, ContractCreationRequest request);

    ContractDto getById(String id);

    List<ContractDto> getAll();

    void delete(String id);

    void cancelContract(String id);

    void activateContract(String id);

    void completeContract(String id);

    List<ContractDto> findByStatus(ContractStatus status);
}
