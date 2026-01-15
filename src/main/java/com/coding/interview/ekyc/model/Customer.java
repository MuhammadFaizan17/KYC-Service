package com.coding.interview.ekyc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private String customerId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String email;
    private String phone;
    private String address;
    private String nationality;
}

