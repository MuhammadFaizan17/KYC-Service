package com.coding.interview.ekyc.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricRequestDto {
    private String customerId;
    private String selfieUrl;
    private String idPhotoUrl;
}

