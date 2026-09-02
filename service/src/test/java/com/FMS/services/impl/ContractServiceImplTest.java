package com.FMS.services.impl;

import com.FMS.dto.request.ContractCreationRequest;
import com.FMS.entity.Contract;
import com.FMS.entity.Customer;
import com.FMS.enums.ContractStatus;
import com.FMS.enums.ContractValueMode;
import com.FMS.enums.DepositScope;
import com.FMS.enums.DepositType;
import com.FMS.enums.DepositUsage;
import com.FMS.enums.TripStatus;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.ContractMapper;
import com.FMS.repositories.CargoRateRepository;
import com.FMS.repositories.ContractRepository;
import com.FMS.repositories.CustomerRepository;
import com.FMS.repositories.DepositRepository;
import com.FMS.repositories.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest {
    @Mock
    private ContractRepository contractRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CargoRateRepository cargoRateRepository;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private DepositRepository depositRepository;
    @Mock
    private ContractMapper contractMapper;

    private ContractServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ContractServiceImpl(
                contractRepository,
                customerRepository,
                cargoRateRepository,
                tripRepository,
                depositRepository,
                contractMapper
        );
    }

    @Test
    void create_rejectsInvalidDateOrder() {
        ContractCreationRequest request = ContractCreationRequest.builder()
                .contractCode("HD-001")
                .customerId("customer-1")
                .signedDate(LocalDate.now())
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .cargoType("DRY")
                .cargoDescription("Hàng khô")
                .build();

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CONTRACT_DATE);
    }

    @Test
    void create_allowsPerTripContractWithoutTotalValue() {
        ContractCreationRequest request = validRequest();
        request.setValueMode(ContractValueMode.PER_TRIP);

        prepareCreate(request);
        service.create(request);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getValueMode()).isEqualTo(ContractValueMode.PER_TRIP);
        assertThat(captor.getValue().getContractValue()).isNull();
        assertThat(captor.getValue().getFreightRatePerTonKm()).isEqualTo(20_000D);
    }

    @Test
    void create_requiresTotalValueForAgreedValueContract() {
        ContractCreationRequest request = validRequest();
        request.setValueMode(ContractValueMode.AGREED_VALUE);
        request.setEstimatedDistanceKm(50D);
        request.setEstimatedCargoWeightTon(5D);

        when(contractRepository.findByContractCode(request.getContractCode())).thenReturn(Optional.empty());
        when(customerRepository.findById(request.getCustomerId()))
                .thenReturn(Optional.of(Customer.builder().id(request.getCustomerId()).build()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CONTRACT_INPUT);
    }

    @Test
    void create_allowsPercentageDepositPerTripWithoutTotalValue() {
        ContractCreationRequest request = validRequest();
        request.setValueMode(ContractValueMode.PER_TRIP);
        request.setDepositRequired(true);
        request.setDepositScope(DepositScope.TRIP);
        request.setDepositType(DepositType.PERCENTAGE);
        request.setDepositValue(25D);
        request.setDepositUsage(DepositUsage.APPLY_TO_INVOICE);
        request.setDepositDueDays(0);

        prepareCreate(request);
        service.create(request);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getContractValue()).isNull();
        assertThat(captor.getValue().getDepositScope()).isEqualTo(DepositScope.TRIP);
        assertThat(captor.getValue().getDepositType()).isEqualTo(DepositType.PERCENTAGE);
        assertThat(captor.getValue().getDepositValue()).isEqualTo(25D);
    }

    @Test
    void create_rejectsContractPercentageDepositWithoutAgreedValue() {
        ContractCreationRequest request = validRequest();
        request.setValueMode(ContractValueMode.PER_TRIP);
        request.setDepositRequired(true);
        request.setDepositScope(DepositScope.CONTRACT);
        request.setDepositType(DepositType.PERCENTAGE);
        request.setDepositValue(20D);
        request.setDepositUsage(DepositUsage.APPLY_TO_INVOICE);

        when(contractRepository.findByContractCode(request.getContractCode())).thenReturn(Optional.empty());
        when(customerRepository.findById(request.getCustomerId()))
                .thenReturn(Optional.of(Customer.builder().id(request.getCustomerId()).build()));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DEPOSIT_POLICY);
    }

    @Test
    void update_rejectsActiveContract() {
        Customer customer = Customer.builder().id("customer-1").build();
        Contract contract = Contract.builder()
                .id("contract-1")
                .customer(customer)
                .status(ContractStatus.ACTIVE)
                .build();
        when(contractRepository.findByIdForUpdate("contract-1")).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> service.update("contract-1", ContractCreationRequest.builder()
                .customerId("customer-1")
                .build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONTRACT_CANNOT_EDIT);
    }

    @Test
    void activate_rejectsExpiredContract() {
        Contract contract = Contract.builder()
                .id("contract-1")
                .signedDate(LocalDate.now().minusMonths(2))
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().minusDays(1))
                .status(ContractStatus.DRAFT)
                .build();
        when(contractRepository.findByIdForUpdate("contract-1")).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> service.activateContract("contract-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONTRACT_OUTSIDE_VALIDITY);
    }

    @Test
    void complete_rejectsContractWithOpenTrip() {
        Contract contract = Contract.builder()
                .id("contract-1")
                .status(ContractStatus.ACTIVE)
                .build();
        when(contractRepository.findByIdForUpdate("contract-1")).thenReturn(Optional.of(contract));
        when(tripRepository.existsByContractIdAndStatusIn(
                "contract-1",
                List.of(TripStatus.CREATED, TripStatus.ASSIGNED, TripStatus.IN_PROGRESS)
        )).thenReturn(true);

        assertThatThrownBy(() -> service.completeContract("contract-1"))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONTRACT_HAS_OPEN_TRIPS);
    }

    @Test
    void create_generatesHD01WhenNoContractsExist() {
        ContractCreationRequest request = validRequest();
        request.setContractCode(null);

        when(contractRepository.findAll()).thenReturn(List.of());
        when(contractRepository.findByContractCode("HD01")).thenReturn(Optional.empty());
        when(customerRepository.findById(request.getCustomerId()))
                .thenReturn(Optional.of(Customer.builder().id(request.getCustomerId()).build()));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getContractCode()).isEqualTo("HD01");
    }

    @Test
    void create_generatesNextCodeInSequence() {
        ContractCreationRequest request = validRequest();
        request.setContractCode("");

        Contract c1 = Contract.builder().contractCode("HD01").build();
        Contract c2 = Contract.builder().contractCode("HD09").build();

        when(contractRepository.findAll()).thenReturn(List.of(c1, c2));
        when(contractRepository.findByContractCode("HD10")).thenReturn(Optional.empty());
        when(customerRepository.findById(request.getCustomerId()))
                .thenReturn(Optional.of(Customer.builder().id(request.getCustomerId()).build()));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<Contract> captor = ArgumentCaptor.forClass(Contract.class);
        verify(contractRepository).save(captor.capture());
        assertThat(captor.getValue().getContractCode()).isEqualTo("HD10");
    }

    @Test
    void getAll_sortsContractsByContractOrder() {
        Contract c1 = Contract.builder().id("1").contractCode("HD10").build();
        Contract c2 = Contract.builder().id("2").contractCode("HD02").build();
        Contract c3 = Contract.builder().id("3").contractCode("HD01").build();
        Contract c4 = Contract.builder().id("4").contractCode("HD100").build();

        when(contractRepository.findAll()).thenReturn(List.of(c1, c2, c3, c4));
        when(contractMapper.toDto(any(Contract.class))).thenAnswer(inv -> {
            Contract c = inv.getArgument(0);
            return com.FMS.dto.ContractDto.builder().id(c.getId()).contractCode(c.getContractCode()).build();
        });

        List<com.FMS.dto.ContractDto> result = service.getAll();

        assertThat(result)
                .extracting(com.FMS.dto.ContractDto::getContractCode)
                .containsExactly("HD01", "HD02", "HD10", "HD100");
    }

    private ContractCreationRequest validRequest() {
        return ContractCreationRequest.builder()
                .contractCode("HD-PER-TRIP-001")
                .customerId("customer-1")
                .signedDate(LocalDate.now())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .cargoType("DRY")
                .cargoDescription("Hàng khô đóng thùng")
                .freightRatePerTonKm(20_000D)
                .build();
    }

    private void prepareCreate(ContractCreationRequest request) {
        when(contractRepository.findByContractCode(request.getContractCode())).thenReturn(Optional.empty());
        when(customerRepository.findById(request.getCustomerId()))
                .thenReturn(Optional.of(Customer.builder().id(request.getCustomerId()).build()));
        when(contractRepository.save(any(Contract.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
