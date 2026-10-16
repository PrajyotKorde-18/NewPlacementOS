package com.placementos.tpms.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentShortlistedEvent {
    private final Long applicationId;
    private final Long driveId;
    private final Long studentId;
}
