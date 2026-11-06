package io.github.ahmedsh10.shiftwise.web.dto;

import java.time.LocalDate;

/** How well one area is staffed for one shift on one day. */
public record CoverageSlot(LocalDate date, String areaCode, String areaName, String shiftCode,
                           int required, int assigned, int missing, Status status) {

    public enum Status {
        /** Fully staffed (or over). */
        GREEN,
        /** One person short. */
        YELLOW,
        /** Two or more short. */
        RED
    }

    public static Status statusFor(int missing) {
        if (missing <= 0) {
            return Status.GREEN;
        }
        return missing == 1 ? Status.YELLOW : Status.RED;
    }
}
