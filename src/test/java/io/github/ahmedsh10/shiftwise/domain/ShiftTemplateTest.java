package io.github.ahmedsh10.shiftwise.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftTemplateTest {

    private static ShiftTemplate shift(String code, String... times) {
        List<ShiftSegment> segments = new java.util.ArrayList<>();
        for (int i = 0; i < times.length; i += 2) {
            segments.add(new ShiftSegment(LocalTime.parse(times[i]), LocalTime.parse(times[i + 1])));
        }
        return new ShiftTemplate(code, code, segments);
    }

    @Test
    void splitShiftCountsOnlyWorkedTime() {
        ShiftTemplate split = shift("SPLIT", "13:00", "15:30", "17:30", "21:30");

        assertThat(split.workedTime().toMinutes()).isEqualTo(390); // 6.5 h, not 8.5 h
        assertThat(split.firstStart()).isEqualTo(LocalTime.of(13, 0));
        assertThat(split.lastEnd()).isEqualTo(LocalTime.of(21, 30));
    }

    @Test
    void shiftInTheGapOfASplitShiftDoesNotOverlap() {
        ShiftTemplate split = shift("SPLIT", "13:00", "15:30", "17:30", "21:30");
        ShiftTemplate gap = shift("GAP", "15:30", "17:30");

        assertThat(split.overlaps(gap)).isFalse();
    }

    @Test
    void eveningShiftOverlapsSecondHalfOfSplitShift() {
        ShiftTemplate split = shift("SPLIT", "13:00", "15:30", "17:30", "21:30");
        ShiftTemplate evening = shift("EVENING", "17:30", "21:30");

        assertThat(split.overlaps(evening)).isTrue();
        assertThat(evening.overlaps(split)).isTrue();
    }

    @Test
    void backToBackShiftsDoNotOverlap() {
        ShiftTemplate morning = shift("MORNING", "08:00", "12:00");
        ShiftTemplate noon = shift("NOON", "12:00", "13:00");

        assertThat(morning.overlaps(noon)).isFalse();
    }
}
