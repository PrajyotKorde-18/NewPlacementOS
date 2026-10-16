package com.placementos.tpms.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DrivePublishedEvent {
    private final Long driveId;
}
