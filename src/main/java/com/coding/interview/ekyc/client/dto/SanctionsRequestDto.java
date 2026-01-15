package com.coding.interview.ekyc.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SanctionsRequestDto {
    private String customerId;
    private String fullName;
    private LocalDate dateOfBirth;
    private String nationality;
}

