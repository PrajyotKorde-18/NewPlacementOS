package com.placementos.internship.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InternshipSubmittedEvent {
    private final Long requestId;
}
