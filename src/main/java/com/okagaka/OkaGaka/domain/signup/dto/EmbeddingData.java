package com.okagaka.OkaGaka.domain.signup.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class EmbeddingData {
    private Long userId;
    private List<EmbeddingItem> items;
}
