package com.okagaka.OkaGaka.domain.user.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class EmbeddingImageUpdateRequest {

    @Getter
    public static class EmbeddingItem {
        private Long id;
        private String embeddingImageUrl;
    }

    private List<EmbeddingItem> embeddings;
}
