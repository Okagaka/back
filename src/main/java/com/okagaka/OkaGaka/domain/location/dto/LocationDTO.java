package com.okagaka.OkaGaka.domain.location.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationDTO implements Serializable {
    private Long userId;
    private Long groupId;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;
}
