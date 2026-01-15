package com.coding.interview.ekyc.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressVerificationResponseDto {
    private String status;
    private Integer confidence;
    private List<String> reasons;
}

