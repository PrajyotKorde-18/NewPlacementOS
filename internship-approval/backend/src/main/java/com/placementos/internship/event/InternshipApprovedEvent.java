package com.placementos.internship.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InternshipApprovedEvent {
    private final Long requestId;
    private final Long nocRecordId;
}
