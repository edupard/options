package com.skywind.delta_hedger.actors;

public class PriceUtils {

    public static String convertPrice(double px, double futPriceCoeff) {
        double pxWhole = Math.floor(px);
        double pxPart = px - pxWhole;
        double pxPartConverted = Math.round(pxPart * futPriceCoeff);
        return String.format("%.0f'%03.0f", pxWhole, pxPartConverted);
    }

    public static double convertPrice(String px, double futPriceCoeff) {
        String[] parts = px.split("'");
        double pxWhole = Double.parseDouble(parts[0]);
        double pxPart = Double.parseDouble(parts[1]);
        return pxWhole + pxPart / futPriceCoeff;
    }
}
