package com.example.ododok.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SearchRequest {

    @Size(max = 200, message = "검색어는 최대 200자까지 허용됩니다.")
    private String q = "";

    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "크기는 100 이하여야 합니다.")
    private int size = 20;

    private String sort = "rel"; // rel, new, old, pop

    private List<String> types = List.of("question", "user");

    private String difficulty; // EASY, MEDIUM, HARD

    @Min(value = 1, message = "카테고리 ID는 1 이상이어야 합니다.")
    private Long categoryId;

    private boolean highlight = true;
}