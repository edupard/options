package com.skywind.delta_hedger.actors;

import com.skywind.ib.IbGateway;

public class Utils {
    static double getPosition(IbGateway.ExecDetails ed) {
        return ed.getExecution().side().equals("BOT") ? ed.getExecution().shares() : -ed.getExecution().shares();
    }
}
