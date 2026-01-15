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
public class SanctionsResponseDto {
    private String status;
    private Integer matchCount;
    private List<String> matches;
}

