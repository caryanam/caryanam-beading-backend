package com.bidding.dto.responce;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DealerBulkDeleteResponseDTO {

    private int totalRequested;
    private int deletedCount;
    private int skippedCount;
    private List<Long> deletedIds;
    private List<String> skippedReasons;
}
