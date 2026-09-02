package com.FMS.services.impl;

import com.FMS.dto.ContractDto;
import com.FMS.dto.CustomerDto;
import com.FMS.dto.CustomerPortalDto;
import com.FMS.dto.InvoiceDto;
import com.FMS.dto.TripDto;
import com.FMS.dto.request.CustomerAccountCreationRequest;
import com.FMS.dto.request.CustomerAccountLinkRequest;
import com.FMS.dto.request.CustomerProfileRequest;
import com.FMS.entity.Customer;
import com.FMS.entity.User;
import com.FMS.enums.Role;
import com.FMS.exception.AppException;
import com.FMS.exception.ErrorCode;
import com.FMS.mapper.ContractMapper;
import com.FMS.mapper.CustomerMapper;
import com.FMS.mapper.InvoiceMapper;
import com.FMS.mapper.TripMapper;
import com.FMS.repositories.ContractRepository;
import com.FMS.repositories.CustomerRepository;
import com.FMS.repositories.InvoiceRepository;
import com.FMS.repositories.DepositRepository;
import com.FMS.repositories.TripRepository;
import com.FMS.repositories.UserRepository;
import com.FMS.services.CustomerService;
import com.FMS.services.DepositService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor // generates a constructor with required arguments (final fields)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true) // all fields are private and final by default
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    CustomerRepository customerRepository;
    ContractRepository contractRepository;
    InvoiceRepository invoiceRepository;
    TripRepository tripRepository;
    DepositRepository depositRepository;
    UserRepository userRepository;
    CustomerMapper customerMapper;
    ContractMapper contractMapper;
    InvoiceMapper invoiceMapper;
    TripMapper tripMapper;
    PasswordEncoder passwordEncoder;
    DepositService depositService;

    @Override
    public CustomerDto create(CustomerProfileRequest request) {
        Customer customer = buildCustomer(
                request.getName(),
                request.getPhone(),
                request.getIdNumber(),
                request.getDob(),
                request.getAddress()
        );
        customer = customerRepository.save(customer);

        return customerMapper.toDto(customer);
    }

    @Override
    @Transactional
    public CustomerDto createWithNewAccount(CustomerAccountCreationRequest request) {
        String username = normalizeUsername(request.getUsername());
        validatePassword(request.getPassword());
        if (userRepository.existsByUsername(username)) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        userRepository.save(user);

        Customer customer = buildCustomer(
                request.getName(),
                request.getPhone(),
                request.getIdNumber(),
                request.getDob(),
                request.getAddress()
        );
        customer.setUser(user);

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Override
    @Transactional
    public CustomerDto createWithExistingAccount(CustomerAccountLinkRequest request) {
        User user = userRepository.findByUsername(normalizeUsername(request.getUsername()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != Role.CUSTOMER) {
            throw new AppException(ErrorCode.INVALID_CUSTOMER_ACCOUNT);
        }

        if (customerRepository.existsByUser_Id(user.getId())) {
            throw new AppException(ErrorCode.CUSTOMER_ACCOUNT_ALREADY_LINKED);
        }

        Customer customer = buildCustomer(
                request.getName(),
                request.getPhone(),
                request.getIdNumber(),
                request.getDob(),
                request.getAddress()
        );
        customer.setUser(user);

        return customerMapper.toDto(customerRepository.save(customer));
    }

    @Override
    public CustomerDto update(String id, CustomerProfileRequest request) {
        Customer customer = customerRepository.findById(id).orElseThrow(() ->
                        new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        customer.setName(request.getName());
        customer.setPhone(request.getPhone());
        customer.setIdNumber(request.getIdNumber());
        customer.setDob(request.getDob());
        customer.setAddress(request.getAddress());
        normalizeCustomer(customer);

        return customerMapper.toDto(
                customerRepository.save(customer));
    }

    @Override
    public CustomerDto getById(String id) {
        Customer customer = customerRepository.findById(id).orElseThrow(() ->
                        new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        return customerMapper.toDto(customer);
    }

    @Override
    public List<CustomerDto> getAll() {
        return customerRepository.findAll()
                .stream()
                .map(customerMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(String id) {
        Customer customer = customerRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        if (!contractRepository.findByCustomerId(id).isEmpty()
                || !invoiceRepository.findByCustomerId(id).isEmpty()
                || !tripRepository.findByCustomerId(id).isEmpty()
                || depositRepository.existsByCustomerId(id)) {
            throw new AppException(ErrorCode.DELETE_BLOCKED_BY_RELATED_DATA);
        }

        User linkedUser = customer.getUser();
        customerRepository.delete(customer);
        customerRepository.flush();
        if (linkedUser != null) {
            userRepository.delete(linkedUser);
        }
    }

    @Override
    public List<ContractDto> getContracts(String customerId) {
        customerRepository.findById(customerId).orElseThrow(() ->
                        new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        return contractRepository.findByCustomerId(customerId)
                .stream()
                .sorted(ContractServiceImpl.CONTRACT_ORDER_COMPARATOR)
                .map(contractMapper::toDto)
                .toList();
    }

    @Override
    public List<InvoiceDto> getInvoices(String customerId) {
        customerRepository.findById(customerId).orElseThrow(() ->
                        new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        return invoiceRepository.findByCustomerId(customerId)
                .stream()
                .map(invoiceMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPortalDto getPortalByUsername(String username) {
        Customer customer = customerRepository.findByUser_Username(username).orElseThrow(() ->
                new AppException(ErrorCode.CUSTOMER_NOT_FOUND));

        String customerId = customer.getId();

        return CustomerPortalDto.builder()
                .profile(customerMapper.toDto(customer))
                .contracts(contractRepository.findByCustomerId(customerId)
                        .stream()
                        .sorted(ContractServiceImpl.CONTRACT_ORDER_COMPARATOR)
                        .map(contractMapper::toDto)
                        .toList())
                .trips(tripRepository.findByCustomerId(customerId)
                        .stream()
                        .map(this::toPortalTripDto)
                        .toList())
                .invoices(invoiceRepository.findByCustomerId(customerId)
                        .stream()
                        .map(invoiceMapper::toDto)
                        .toList())
                .deposits(depositService.getByCustomer(customerId))
                .build();
    }

    private TripDto toPortalTripDto(com.FMS.entity.Trip trip) {
        TripDto dto = tripMapper.toDto(trip);
        dto.setDepositSummary(depositService.getSummaryForTrip(trip.getId()));
        return dto;
    }

    private Customer buildCustomer(String name, String phone, String idNumber, LocalDate dob, String address) {
        Customer customer = Customer.builder()
                .name(name)
                .phone(phone)
                .idNumber(idNumber)
                .dob(dob)
                .address(address)
                .build();
        normalizeCustomer(customer);
        return customer;
    }

    private void normalizeCustomer(Customer customer) {
        customer.setName(normalizeRequired(customer.getName()));
        customer.setPhone(customer.getPhone() == null
                ? null
                : customer.getPhone().trim().replaceAll("[ .-]", ""));
        customer.setIdNumber(normalizeRequired(customer.getIdNumber()));
        customer.setAddress(normalizeRequired(customer.getAddress()));
    }

    private String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeUsername(String value) {
        String username = value == null ? "" : value.trim();
        if (username.length() < 6 || username.length() > 50) {
            throw new AppException(ErrorCode.INVALID_USERNAME);
        }
        return username;
    }

    private void validatePassword(String value) {
        if (value == null || value.isBlank() || value.length() < 8 || value.length() > 72) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
    }
}
