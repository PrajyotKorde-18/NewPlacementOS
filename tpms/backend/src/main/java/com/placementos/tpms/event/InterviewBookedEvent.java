package com.placementos.tpms.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InterviewBookedEvent {
    private final Long applicationId;
    private final Long driveId;
    private final Long studentId;
    private final String slotDetails;
}
