package com.skywind.delta_hedger.actors;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.skywind.ib.IbGateway;
import com.skywind.ib.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class Position {

    private static final Logger LOGGER = LoggerFactory.getLogger(Position.class);

    private final Contract contract;
    private ContractDetails contractDetails = null;
    private Instant expiry;
    private double pos;
    private double posPx;
    private double vol;
    private double ir;
    private Trade lastTrade;
    private boolean selected;
    private IbGateway.TickPrice lastTick;
    private Instant lastPxInstant;
    private String lastViewPx = "";
    private String lastPxTime = "";

    public Position(boolean selected, Contract contract, double pos, double posPx, double vol, double ir, Trade lastTrade) {
        this.selected = selected;
        this.contract = contract;
        this.pos = pos;
        this.posPx = posPx;
        this.vol = vol;
        this.ir = ir;
        this.lastTrade = lastTrade;
    }

    public Position(Position other) {
        this.contract = other.contract;
        this.pos = other.pos;
        this.posPx = other.posPx;
        this.vol = other.vol;
        this.ir = other.ir;
        this.contractDetails = other.contractDetails;
        this.expiry = other.expiry;
        this.lastTrade = other.lastTrade;
        this.selected = other.selected;
        this.lastTick = other.lastTick;
        this.lastViewPx = other.lastViewPx;
        this.lastPxTime = other.lastPxTime;
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

    public boolean isSelected() {
        return selected;
    }

    public void updatePosition(IbGateway.ExecDetails m) {
        double pos = Utils.getPosition(m);
        double posPx = m.getExecution().price();
        if (Math.signum(this.pos) * Math.signum(pos) > 0) {
            double grossValue = this.pos * this.posPx + pos * posPx;
            this.pos += pos;
            this.posPx = grossValue / this.pos;
        } else {
            if (Math.abs(this.pos) < Math.abs(pos)) {
                this.posPx = posPx;
            }
            this.pos += pos;
        }
    }

    public static final double ZERO_POS_TRESHOLD = 1e-6;

    public boolean isZero() {
        return Math.abs(this.pos) < ZERO_POS_TRESHOLD;
    }

    public boolean isContractDetailsDefined() {
        return contractDetails != null;
    }

    public void setContractDetails(ContractDetails contractDetails) {
        this.contractDetails = contractDetails;
        if (contractDetails != null) {
            parseExpityInstant();
        }
    }

    public ContractDetails getContractDetails() {
        return contractDetails;
    }

    private void parseExpityInstant() {
        expiry = null;
        try {
            String timezoneId = contractDetails.timeZoneId();
            String sLastTradeDate = contract.lastTradeDateOrContractMonth();

            String liquidHours = contractDetails.liquidHours();
            String[] datesSplit = liquidHours.split(";");
            for (String s : datesSplit) {
                String[] dateSplit = s.split(":");
                if (dateSplit.length == 2) {
                    if (dateSplit[0].equals(sLastTradeDate)) {
                        String[] timeSplit = dateSplit[1].split("-");
                        if (timeSplit.length == 2) {
                            String sTime = timeSplit[1];
                            String sDateTime = String.format("%s %s", sLastTradeDate, sTime);
                            DateTimeFormatter FMT = new DateTimeFormatterBuilder()
                                    .appendPattern("yyyyMMdd HHmm")
                                    .toFormatter()
                                    .withZone(ZoneId.of(timezoneId));
                            expiry = FMT.parse(sDateTime, Instant::from);
                            return;
                        }
                    }
                }
            }
            String sDateTime = contractDetails.contract().lastTradeDateOrContractMonth();
            DateTimeFormatter FMT = new DateTimeFormatterBuilder()
                    .appendPattern("yyyyMMdd HH:mm z")
                    .toFormatter()
                    .withZone(ZoneId.of(timezoneId));
            expiry = FMT.parse(sDateTime, Instant::from);
        } catch (Throwable t) {
            LOGGER.error("Can not get expiry instant", t);
        }
    }

    public Instant getExpiry() {
        return expiry;
    }

    public void setIr(double ir) {
        this.ir = ir;
    }

    public void setVol(double vol) {
        this.vol = vol;
    }

    public void setLastTrade(Trade lastTrade) {
        this.lastTrade = lastTrade;
    }

    public Trade getLastTrade() {
        return lastTrade;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    private static DateTimeFormatter LAST_PX_TIME_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMdd HH:mm:ss")
            .toFormatter()
            .withZone(ZoneId.systemDefault());

    public void setLastTick(IbGateway.TickPrice lastTick, String lastViewPx, Instant lastPxInstant) {
        this.lastTick = lastTick;
        this.lastViewPx = lastViewPx;
        this.lastPxInstant = lastPxInstant;
        this.lastPxTime = LAST_PX_TIME_FMT.format(lastPxInstant);
    }

    public IbGateway.TickPrice getLastTick() {
        return lastTick;
    }

    public String getLastViewPx() {
        return lastViewPx;
    }

    public String getLastPxTime() {
        return lastPxTime;
    }

    public Instant getLastPxInstant() {
        return lastPxInstant;
    }
}
