package com.shoppilot.shoppilot.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderApprovalRequest {

    @NotNull(message = "Customer approval is mandatory")
    private Boolean approved;

    private String customerNote;
}
