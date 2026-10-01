package com.bidding.dto.responce;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealerImportResponseDTO {

    private int totalRows;
    private int importedCount;
    private int skippedCount;
    private List<String> issues;
}
