package com.placementos.internship.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InternshipRejectedEvent {
    private final Long requestId;
    private final String rejectionReason;
}
