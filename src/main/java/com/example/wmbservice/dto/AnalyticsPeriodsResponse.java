package com.example.wmbservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * DTO for listing all available statement periods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsPeriodsResponse {
    private List<String> periods;
    private int count;
}

