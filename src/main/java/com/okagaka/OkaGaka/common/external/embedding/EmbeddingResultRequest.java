package com.okagaka.OkaGaka.common.external.embedding;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EmbeddingResultRequest {
    private String Id;
    private String vehicleId;
    private String userId;
    private List<String> embeddingUrls;
}
