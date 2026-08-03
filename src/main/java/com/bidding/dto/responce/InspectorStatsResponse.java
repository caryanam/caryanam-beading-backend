package com.bidding.dto.responce;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InspectorStatsResponse {
    private long todayInspections;
    private long pendingUploads;
    private long completedReports;
    private long vehiclesSubmitted;
}
