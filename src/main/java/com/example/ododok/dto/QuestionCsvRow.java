package com.example.ododok.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCsvRow {
    private String question;
    private String difficulty;
    private String year;
    private String companyName;
    private String companyId;
    private String categoryName;
    private String categoryId;
}