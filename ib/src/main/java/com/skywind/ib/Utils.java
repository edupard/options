package com.skywind.ib;

import com.ib.client.Contract;
import com.ib.client.Types;
import com.skywind.ib.IbGateway;

public class Utils {
    public static double getPosition(IbGateway.ExecDetails ed) {
        return ed.getExecution().side().equals("BOT") ? ed.getExecution().shares() : -ed.getExecution().shares();
    }

    public static String getSecType(Contract c) {
        String secType = "U";
        if (c.secType() == Types.SecType.FOP) {
            if (c.right() == Types.Right.Call) {
                secType = "P";
            }
            else if (c.right()== Types.Right.Put) {
                secType = "C";
            }
        }
        else if (c.secType()== Types.SecType.FUT)
        {
            secType = "F";
        }
        return secType;
    }
}
