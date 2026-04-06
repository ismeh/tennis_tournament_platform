package com.tennis.domain.model;

import com.tfm.tennis_platform.domain.models.enums.Surface;
import com.tennis.domain.model.enums.TournamentCategory;
import com.tfm.tennis_platform.domain.models.enums.TournamentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {
    private UUID id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String venue;
    private String country;
    private Surface surface;
    private TournamentCategory category;
    private TournamentStatus status;

    public void cancel() {
        if (this.status == TournamentStatus.OPEN || this.status == TournamentStatus.DRAFT) {
            this.status = TournamentStatus.CANCELLED;
        }
    }
}
