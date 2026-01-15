package com.coding.interview.ekyc.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressVerificationRequestDto {
    private String customerId;
    private String address;
    private String proofType;
    private String proofDate;
    private String proofUrl;
}

