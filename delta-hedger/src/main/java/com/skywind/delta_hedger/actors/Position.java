package com.skywind.delta_hedger.actors;

import com.ib.client.Contract;
import com.ib.client.Util;
import com.skywind.ib.IbGateway;

public class Position {

    private final Contract contract;
    private double pos;
    private double posPx;
    private double vol;
    private double ir;

    public Position(Contract contract, double pos, double posPx, double vol, double ir) {
        this.contract = contract;
        this.pos = pos;
        this.posPx = posPx;
        this.vol = vol;
        this.ir = ir;
    }

    public Position(Position other) {
        this.contract = other.contract;
        this.pos = other.pos;
        this.posPx = other.posPx;
        this.vol = other.vol;
        this.ir = other.ir;
    }

    public void setPos(double pos) {
        this.pos = pos;
    }

    public void setPosPx(double posPx) {
        this.posPx = posPx;
    }

    public Contract getContract() {
        return contract;
    }

    public double getPos() {
        return pos;
    }

    public double getPosPx() {
        return posPx;
    }

    public Double getIr() {
        return ir;
    }

    public Double getVol() {
        return vol;
    }

    public void updatePosition(IbGateway.ExecDetails m) {
        double pos = Utils.getPosition(m);
        double posPx = m.getExecution().price();
        if (Math.signum(this.pos) * Math.signum(pos) > 0) {
            double grossValue = this.pos * this.posPx + pos * posPx;
            this.pos += pos;
            this.posPx = grossValue / this.pos;
        }
        else {
            if (Math.abs(this.pos) < Math.abs(pos)) {
                this.posPx = posPx;
            }
            this.pos += pos;
        }
    }

    private static final double ZERO_POS_TRESHOLD = 1e-6;

    public boolean isZero() {
        return Math.abs(this.pos) < ZERO_POS_TRESHOLD;
    }
}
