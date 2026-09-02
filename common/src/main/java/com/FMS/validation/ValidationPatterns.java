package com.FMS.validation;

public final class ValidationPatterns {
    public static final String PHONE = "^(?:\\+84|0)(?:[ .-]?[0-9]){9,10}$";
    public static final String ID_NUMBER = "^(?:[0-9]{9}|[0-9]{10}|[0-9]{12}|[0-9]{10}-[0-9]{3})$";
    public static final String DRIVER_LICENSE = "^[A-Za-z0-9./-]{5,20}$";
    public static final String VEHICLE_LICENSE_PLATE = "^[0-9]{2}[A-Za-z][0-9]?-?[0-9]{3}(?:\\.?[0-9]{2}|[0-9])$";
    public static final String EMPLOYEE_CODE = "^[A-Za-z0-9._-]{2,30}$";

    private ValidationPatterns() {
    }
}
