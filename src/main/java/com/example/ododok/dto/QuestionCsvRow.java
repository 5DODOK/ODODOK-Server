package com.example.ododok.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCsvRow {
    @CsvBindByName
    private String question;

    @CsvBindByName
    private String difficulty;

    @CsvBindByName
    private String year;

    @CsvBindByName(column = "company_name")
    private String companyName;

    @CsvBindByName(column = "category_name")
    private String categoryName;
}