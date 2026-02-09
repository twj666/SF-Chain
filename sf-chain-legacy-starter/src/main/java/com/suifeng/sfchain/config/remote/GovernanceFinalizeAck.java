package com.suifeng.sfchain.config.remote;

import lombok.Data;

/**
 * 治理终态回调ACK
 */
@Data
public class GovernanceFinalizeAck {
    private boolean acknowledged = true;
    private String ackId;
    private Long ackVersion;
    private Long serverTimeEpochMs;
}
