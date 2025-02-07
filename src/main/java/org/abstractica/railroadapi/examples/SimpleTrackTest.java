package org.abstractica.railroadapi.examples;

import org.abstractica.railroadapi.Locomotive;
import org.abstractica.railroadapi.RailroadAPI;
import org.abstractica.railroadapi.Switch;
import org.abstractica.railroadapi.impl.RailroadAPIImpl;

import java.net.SocketException;
import java.net.UnknownHostException;

public class SimpleTrackTest
{
    public static void main(String[] args) throws SocketException, UnknownHostException, InterruptedException
    {
        RailroadAPI api = new RailroadAPIImpl();
        Locomotive loc1 = api.createLocomotive("Locomotive 1",2703589);
        Switch sw1 = api.createSwitch("Switch 1", Switch.Side.LEFT, 14948781);
        api.start();
        api.waitForAllDevicesToConnect();
        System.out.println("Identifying locomotive with 2 blinks");
        loc1.identify(2, 20);
        System.out.println("Identifying switch with 3 blinks");
        sw1.identify(3, 20);
        System.out.println("Waiting for 5 seconds...");
        Thread.sleep(5000);
        Switch.Side currentSide = Switch.Side.LEFT;
        while(true)
        {
            System.out.println("Switch to LEFT...");
            sw1.switchAndWait(Switch.Side.LEFT);
            System.out.println("Move forward 18 blocks...");
            loc1.setDirection(Locomotive.Direction.FORWARD);
            loc1.moveAndWait(18);
            Thread.sleep(1000);
            System.out.println("Move backward 12 blocks...");
            loc1.setDirection(Locomotive.Direction.BACKWARD);
            loc1.moveAndWait(12);
            System.out.println("Switch to RIGHT...");
            sw1.switchAndWait(Switch.Side.RIGHT);
            System.out.println("Move forward 12 blocks...");
            loc1.setDirection(Locomotive.Direction.FORWARD);
            loc1.moveAndWait(12);
            Thread.sleep(1000);
            System.out.println("Move backward 18 blocks...");
            loc1.setDirection(Locomotive.Direction.BACKWARD);
            loc1.moveAndWait(18);
            Thread.sleep(1000);
        }
    }
}
