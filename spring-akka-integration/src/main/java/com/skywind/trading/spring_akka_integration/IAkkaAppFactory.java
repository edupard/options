package com.skywind.trading.spring_akka_integration;

public interface IAkkaAppFactory {

    public void createActors();

    public void stopActors();

}
