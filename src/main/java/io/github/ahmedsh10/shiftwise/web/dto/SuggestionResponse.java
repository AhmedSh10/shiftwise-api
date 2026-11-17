package io.github.ahmedsh10.shiftwise.web.dto;

import java.time.LocalDate;
import java.util.List;

/** Who can fill one slot, best first. Blocked people are listed after, with the rule that blocks them. */
public record SuggestionResponse(LocalDate date, String areaCode, String shiftCode, int required, int assigned,
                                 int missing, List<CandidateResponse> candidates) {
}
