package com.example.ododok.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
public class SearchRequest {

    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "크기는 100 이하여야 합니다.")
    private int size = 20;

    private String sort = "rel"; // rel, new, old

    @Min(value = 1, message = "카테고리 ID는 1 이상이어야 합니다.")
    private Long categoryId;

    private Integer year;

    @Min(value = 1, message = "회사 ID는 1 이상이어야 합니다.")
    private Long companyId;

    @Size(max = 100, message = "회사명은 최대 100자까지 허용됩니다.")
    private String companyName;
}