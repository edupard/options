package com.skywind.log_trader.actors;

import com.ib.client.Contract;

public class Trade {
    private final String localSymbol;
    private final double pos;
    private final double posPx;
    private final String execId;
    private final String time;

    public Trade(String localSymbol, double pos, double posPx, String execId, String time) {
        this.localSymbol = localSymbol;
        this.pos = pos;
        this.posPx = posPx;
        this.execId = execId;
        this.time = time;
    }

    public String getExecId() {
        return execId;
    }

    public String getLocalSymbol() {
        return localSymbol;
    }

    public double getPos() {
        return pos;
    }

    public String getTime() {
        return time;
    }
}
