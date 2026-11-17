package io.github.ahmedsh10.shiftwise.web.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * @param created  assignments added (in a dry run they have no id, because nothing was saved)
 * @param unfilled slots nobody could take without breaking a rule
 */
public record GenerateScheduleResponse(LocalDate weekStart, boolean dryRun, int createdCount, int unfilledCount,
                                       List<AssignmentResponse> created, List<UnfilledSlot> unfilled) {

    public record UnfilledSlot(LocalDate date, String areaCode, String shiftCode, int missing) {
    }
}
