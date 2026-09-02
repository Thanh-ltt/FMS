package com.FMS.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStatusRequest {
    @NotNull(message = "INVALID_EMPLOYEE_INPUT")
    Boolean active;
}
