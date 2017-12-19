package com.skywind.delta_hedger.actors;

import com.ib.client.Contract;

public class Trade {
    private final Contract contract;
    private final double pos;
    private final double posPx;
    private final String execId;
    private final String time;

    public Trade(Contract contract, double pos, double posPx, String execId, String time) {
        this.contract = contract;
        this.pos = pos;
        this.posPx = posPx;
        this.execId = execId;
        this.time = time;
    }

    public String getExecId() {
        return execId;
    }
}
