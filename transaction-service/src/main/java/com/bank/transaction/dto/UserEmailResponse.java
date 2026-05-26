package com.bank.transaction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserEmailResponse {
    private Long id;
    private String email;
}
