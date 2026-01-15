package com.coding.interview.ekyc.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVerificationRequestDto {
    private String customerId;
    private String documentType;
    private String documentNumber;
    private String expiryDate;
    private String documentImageUrl;
}

