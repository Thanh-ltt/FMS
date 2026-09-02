package com.FMS.services.impl;


import com.FMS.dto.ContractDto;
import com.FMS.dto.request.ContractCreationRequest;
import com.FMS.entity.Contract;
import com.FMS.entity.Customer;
import com.FMS.enums.ContractStatus;
import com.FMS.enums.ContractValueMode;
import com.FMS.enums.DepositScope;
import com.FMS.enums.DepositType;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.ContractMapper;
import com.FMS.repositories.ContractRepository;
import com.FMS.repositories.CustomerRepository;
import com.FMS.repositories.CargoRateRepository;
import com.FMS.repositories.DepositRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.services.ContractService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor // generates a constructor with required arguments (final fields)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // all fields are private and final by default
@Slf4j
public class ContractServiceImpl implements ContractService {
    public static final Pattern CONTRACT_CODE_PATTERN = Pattern.compile("^HD(\\d+)$", Pattern.CASE_INSENSITIVE);

    public static final Comparator<Contract> CONTRACT_ORDER_COMPARATOR = (c1, c2) -> {
        if (c1 == null && c2 == null) return 0;
        if (c1 == null) return 1;
        if (c2 == null) return -1;

        String code1 = c1.getContractCode();
        String code2 = c2.getContractCode();

        if (code1 == null && code2 == null) {
            return compareTimestamps(c1, c2);
        }
        if (code1 == null) return 1;
        if (code2 == null) return -1;

        String trimmed1 = code1.trim();
        String trimmed2 = code2.trim();

        Matcher m1 = CONTRACT_CODE_PATTERN.matcher(trimmed1);
        Matcher m2 = CONTRACT_CODE_PATTERN.matcher(trimmed2);

        if (m1.matches() && m2.matches()) {
            try {
                long n1 = Long.parseLong(m1.group(1));
                long n2 = Long.parseLong(m2.group(1));
                int cmp = Long.compare(n1, n2);
                if (cmp != 0) return cmp;
            } catch (NumberFormatException ignored) {
            }
        } else if (m1.matches()) {
            return -1;
        } else if (m2.matches()) {
            return 1;
        }

        int strCmp = trimmed1.compareToIgnoreCase(trimmed2);
        if (strCmp != 0) return strCmp;

        return compareTimestamps(c1, c2);
    };

    static final List<TripStatus> OPEN_TRIP_STATUSES = List.of(
            TripStatus.CREATED,
            TripStatus.ASSIGNED,
            TripStatus.IN_PROGRESS
    );

    ContractRepository contractRepository;
    CustomerRepository customerRepository;
    CargoRateRepository cargoRateRepository;
    TripRepository tripRepository;
    DepositRepository depositRepository;
    ContractMapper contractMapper;

    @Transactional
    public ContractDto create(ContractCreationRequest request) {
        validateContractDates(request.getSignedDate(), request.getStartDate(), request.getEndDate());
        String contractCode = resolveContractCode(request.getContractCode());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        Double freightRatePerTonKm = resolveFreightRate(request.getCargoType(), request.getFreightRatePerTonKm());
        ContractValueMode valueMode = resolveValueMode(request);

        Contract contract = Contract.builder()
                .contractCode(contractCode)
                .customer(customer)
                .signedDate(request.getSignedDate())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .cargoDescription(request.getCargoDescription())
                .cargoType(request.getCargoType())
                .freightRatePerTonKm(freightRatePerTonKm)
                .estimatedDistanceKm(request.getEstimatedDistanceKm())
                .estimatedCargoWeightTon(request.getEstimatedCargoWeightTon())
                .valueMode(valueMode)
                .contractValue(resolveContractValue(request, valueMode))
                .status(ContractStatus.DRAFT)
                .build();

        applyDepositPolicy(contract, request);

        contract = contractRepository.save(contract);

        return contractMapper.toDto(contract);
    }


    @Transactional
    public ContractDto update(String id, ContractCreationRequest request) {
        Contract contract = contractRepository.findByIdForUpdate(id).orElseThrow(() ->
                        new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new AppException(ErrorCode.CONTRACT_CANNOT_EDIT);
        }
        if (contract.getCustomer() == null
                || !contract.getCustomer().getId().equals(request.getCustomerId())) {
            throw new AppException(ErrorCode.CONTRACT_CUSTOMER_MISMATCH);
        }
        validateContractDates(request.getSignedDate(), request.getStartDate(), request.getEndDate());

        contract.setSignedDate(request.getSignedDate());
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setCargoDescription(request.getCargoDescription());
        contract.setCargoType(request.getCargoType());
        Double freightRatePerTonKm = resolveFreightRate(request.getCargoType(), request.getFreightRatePerTonKm());
        contract.setFreightRatePerTonKm(freightRatePerTonKm);
        contract.setEstimatedDistanceKm(request.getEstimatedDistanceKm());
        contract.setEstimatedCargoWeightTon(request.getEstimatedCargoWeightTon());
        ContractValueMode valueMode = resolveValueMode(request);
        contract.setValueMode(valueMode);
        contract.setContractValue(resolveContractValue(request, valueMode));
        applyDepositPolicy(contract, request);
        Contract updatedContract = contractRepository.save(contract);

        return contractMapper.toDto(updatedContract);
    }

    public ContractDto getById(String id) {
        Contract contract = contractRepository.findById(id).orElseThrow(() ->
                        new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        return contractMapper.toDto(contract);
    }

    public List<ContractDto> getAll() {
        return contractRepository.findAll()
                .stream()
                .sorted(CONTRACT_ORDER_COMPARATOR)
                .map(contractMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(String id) {
        Contract contract = contractRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (tripRepository.existsByContractId(id) || depositRepository.existsByContractId(id)) {
            throw new AppException(ErrorCode.DELETE_BLOCKED_BY_RELATED_DATA);
        }
        if (contract.getStatus() != ContractStatus.DRAFT
                && contract.getStatus() != ContractStatus.CANCELLED) {
            throw new AppException(ErrorCode.CONTRACT_CANNOT_DELETE);
        }

        contractRepository.delete(contract);
    }

    @Transactional
    public void cancelContract(String id) {
        Contract contract = contractRepository.findByIdForUpdate(id).orElseThrow(() ->
                        new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() == ContractStatus.COMPLETED
                || contract.getStatus() == ContractStatus.CANCELLED) {
            throw new AppException(ErrorCode.CONTRACT_CANNOT_CANCELLED);
        }
        ensureNoOpenTrips(contract.getId());

        contract.setStatus(ContractStatus.CANCELLED);

        contractRepository.save(contract);
    }

    @Transactional
    public void activateContract(String id) {
        Contract contract = contractRepository.findByIdForUpdate(id).orElseThrow(() ->
                        new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new AppException(ErrorCode.CONTRACT_CANNOT_ACTIVATED);
        }
        validateContractDates(contract.getSignedDate(), contract.getStartDate(), contract.getEndDate());
        if (contract.getEndDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.CONTRACT_OUTSIDE_VALIDITY);
        }

        contract.setStatus(ContractStatus.ACTIVE);

        contractRepository.save(contract);
    }

    @Transactional
    public void completeContract(String id) {
        Contract contract = contractRepository.findByIdForUpdate(id).orElseThrow(() ->
                        new AppException(ErrorCode.CONTRACT_NOT_FOUND));

        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new AppException(ErrorCode.CONTRACT_CANNOT_COMPLETED);
        }
        ensureNoOpenTrips(contract.getId());

        contract.setStatus(ContractStatus.COMPLETED);

        contractRepository.save(contract);
    }

    public List<ContractDto> findByStatus(ContractStatus status) {
        return contractRepository.findByStatus(status)
                .stream()
                .sorted(CONTRACT_ORDER_COMPARATOR)
                .map(contractMapper::toDto)
                .toList();
    }

    private synchronized String resolveContractCode(String requestedCode) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            String trimmed = requestedCode.trim();
            if (contractRepository.findByContractCode(trimmed).isPresent()) {
                throw new AppException(ErrorCode.CONTRACT_ALREADY_EXISTS);
            }
            return trimmed;
        }

        return generateNextContractCode();
    }

    private synchronized String generateNextContractCode() {
        List<Contract> contracts = contractRepository.findAll();
        long maxIndex = 0;
        for (Contract contract : contracts) {
            String code = contract.getContractCode();
            if (code != null) {
                Matcher matcher = CONTRACT_CODE_PATTERN.matcher(code.trim());
                if (matcher.matches()) {
                    try {
                        long val = Long.parseLong(matcher.group(1));
                        if (val > maxIndex) {
                            maxIndex = val;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        long nextIndex = maxIndex + 1;
        String generated;
        do {
            generated = String.format("HD%02d", nextIndex++);
        } while (contractRepository.findByContractCode(generated).isPresent());

        return generated;
    }

    private static int compareTimestamps(Contract c1, Contract c2) {
        if (c1.getCreatedAt() != null && c2.getCreatedAt() != null) {
            int createdCmp = c1.getCreatedAt().compareTo(c2.getCreatedAt());
            if (createdCmp != 0) return createdCmp;
        }
        if (c1.getId() != null && c2.getId() != null) {
            return c1.getId().compareTo(c2.getId());
        }
        return 0;
    }

    private ContractValueMode resolveValueMode(ContractCreationRequest request) {
        if (request.getValueMode() != null) {
            return request.getValueMode();
        }

        return request.getContractValue() == null
                ? ContractValueMode.PER_TRIP
                : ContractValueMode.AGREED_VALUE;
    }

    private Double resolveContractValue(ContractCreationRequest request, ContractValueMode valueMode) {
        if (valueMode == ContractValueMode.PER_TRIP) {
            return null;
        }

        Double contractValue = request.getContractValue();
        if (contractValue == null || contractValue <= 0) {
            throw new AppException(ErrorCode.INVALID_CONTRACT_INPUT);
        }

        return contractValue;
    }

    private Double resolveFreightRate(String cargoType, Double requestedRate) {
        if (requestedRate != null && requestedRate > 0) {
            return requestedRate;
        }

        if (cargoType == null || cargoType.isBlank()) {
            throw new AppException(ErrorCode.INVALID_FREIGHT_INPUT);
        }

        Double configuredRate = cargoRateRepository.findByCargoType(cargoType)
                .map(rate -> rate.getRatePerTonKm())
                .orElse(null);
        if (configuredRate == null || configuredRate <= 0) {
            throw new AppException(ErrorCode.INVALID_FREIGHT_INPUT);
        }
        return configuredRate;
    }

    private void applyDepositPolicy(Contract contract, ContractCreationRequest request) {
        if (!Boolean.TRUE.equals(request.getDepositRequired())) {
            contract.setDepositRequired(false);
            contract.setDepositScope(null);
            contract.setDepositType(null);
            contract.setDepositValue(null);
            contract.setDepositUsage(null);
            contract.setDepositDueDays(null);
            contract.setDepositTerms(null);
            return;
        }

        boolean invalid = request.getDepositScope() == null
                || request.getDepositType() == null
                || request.getDepositUsage() == null
                || request.getDepositValue() == null
                || request.getDepositValue() <= 0
                || (request.getDepositType() == DepositType.PERCENTAGE && request.getDepositValue() > 100)
                || (request.getDepositDueDays() != null && request.getDepositDueDays() < 0)
                || (request.getDepositScope() == DepositScope.CONTRACT
                    && request.getDepositType() == DepositType.PERCENTAGE
                    && (contract.getValueMode() != ContractValueMode.AGREED_VALUE
                        || contract.getContractValue() == null
                        || contract.getContractValue() <= 0));
        if (invalid) {
            throw new AppException(ErrorCode.INVALID_DEPOSIT_POLICY);
        }

        contract.setDepositRequired(true);
        contract.setDepositScope(request.getDepositScope());
        contract.setDepositType(request.getDepositType());
        contract.setDepositValue(request.getDepositValue());
        contract.setDepositUsage(request.getDepositUsage());
        contract.setDepositDueDays(request.getDepositDueDays() == null ? 0 : request.getDepositDueDays());
        contract.setDepositTerms(request.getDepositTerms() == null || request.getDepositTerms().isBlank()
                ? null : request.getDepositTerms().trim());
    }

    private void validateContractDates(LocalDate signedDate, LocalDate startDate, LocalDate endDate) {
        if (signedDate == null
                || startDate == null
                || endDate == null
                || signedDate.isAfter(LocalDate.now())
                || signedDate.isAfter(startDate)
                || startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.INVALID_CONTRACT_DATE);
        }
    }

    private void ensureNoOpenTrips(String contractId) {
        if (tripRepository.existsByContractIdAndStatusIn(contractId, OPEN_TRIP_STATUSES)) {
            throw new AppException(ErrorCode.CONTRACT_HAS_OPEN_TRIPS);
        }
    }
}
