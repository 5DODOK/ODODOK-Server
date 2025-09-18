# Question CSV Upload API

This document describes the CSV upload functionality for bulk question management.

## Endpoints

### 1. Sample CSV Download
```
GET /question/csv/sample
```
- Downloads a sample CSV file with the correct format
- No authentication required
- Returns CSV file with sample data

### 2. CSV Upload
```
POST /question/csv
```
- Uploads CSV file for bulk question creation/update
- Requires admin authentication
- Content-Type: multipart/form-data

#### Parameters
- `file` (required): CSV file with UTF-8 encoding
- `dry_run` (optional): Boolean, default false. If true, validates only without saving

#### Request Headers
```
Authorization: Bearer <jwt_token>
Content-Type: multipart/form-data
```

## CSV Format

### Supported Header Combinations
1. `question,difficulty,year,company_name,category_name`
2. `question,difficulty,year,company_id,category_id`
3. `question,difficulty,year`

### Column Definitions
| Column | Required | Description |
|--------|----------|-------------|
| question | Yes | Question text, max 200 characters |
| difficulty | No | EASY/MEDIUM/HARD or 1/2/3, default MEDIUM |
| year | No | Integer year (e.g., 2024) |
| company_name | No* | Company name for FK lookup |
| company_id | No* | Direct company ID |
| category_name | No* | Category name for FK lookup |
| category_id | No* | Direct category ID |

*Mutual exclusion: Use either name OR id, not both

### Example CSV
```csv
question,difficulty,year,company_name,category_name
"자바에서 HashMap과 TreeMap의 차이점은 무엇인가요?",MEDIUM,2024,"네이버","자료구조"
"React에서 useState Hook을 사용하는 이유는?",EASY,2024,"카카오","프론트엔드"
"데이터베이스 인덱스의 장단점을 설명하세요",HARD,2023,"삼성전자","데이터베이스"
```

## Response Format

### Success Response (200 OK)
```json
{
  "summary": {
    "total_rows": 120,
    "created": 90,
    "updated": 25,
    "skipped": 5,
    "dry_run": false,
    "upsert_key": "question"
  },
  "errors": [
    {
      "row": 3,
      "code": "HEADER_MISMATCH",
      "field": null,
      "message": "CSV 헤더가 사양과 일치하지 않습니다."
    }
  ]
}
```

### Error Responses
| HTTP Status | Condition | Message |
|------------|-----------|---------|
| 400 | Invalid CSV format | "유효하지 않은 CSV 형식입니다." |
| 401 | Missing/invalid token | "인증이 필요합니다." |
| 403 | Non-admin user | "이 작업을 수행할 권한이 없습니다." |
| 413 | File too large | "업로드 가능한 최대 크기를 초과했습니다." |
| 415 | Invalid content type | "CSV만 허용됩니다." |
| 422 | Validation errors | "일부 행에 유효하지 않은 값이 있습니다." |

## Configuration

The following properties can be configured in `application.properties`:

```properties
# CSV Upload Configuration
csv.upload.max-file-size=5242880    # 5MB
csv.upload.max-rows=1000
csv.upload.upsert-key=question      # Options: "question" or "composite"

# File Upload Configuration
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

## Business Rules

1. **Authentication**: Only users with ADMIN role can upload CSV
2. **Upsert Logic**: Based on `upsert-key` configuration
   - "question": Match by question text only
   - "composite": Match by (question, year, company_id, category_id)
3. **Validation**: Row-by-row validation with partial success allowed
4. **Encoding**: UTF-8 required, BOM supported
5. **File Limits**: Max 5MB file size, 1000 rows
6. **FK Resolution**: Name fields resolve to IDs via database lookup
7. **Dry Run**: Validates without saving, useful for preview

## Error Codes

- `HEADER_MISMATCH`: CSV headers don't match expected format
- `INVALID_CSV_FORMAT`: Malformed CSV structure
- `EMPTY_FILE`: No content in uploaded file
- `FILE_SIZE_EXCEEDED`: File larger than limit
- `TOO_MANY_ROWS`: More rows than allowed
- `INVALID_CONTENT_TYPE`: Not CSV content type
- `USER_NOT_FOUND`: User ID not found
- `REQUIRED_FIELD_MISSING`: Missing required column value
- `FIELD_TOO_LONG`: Column value exceeds length limit
- `INVALID_DIFFICULTY_LABEL`: Invalid difficulty value
- `INVALID_YEAR_FORMAT`: Year not a valid integer
- `MUTUAL_EXCLUSION_VIOLATION`: Both name and ID provided
- `FK_NOT_FOUND`: Referenced company/category not found
- `INVALID_ID_FORMAT`: ID field not numeric