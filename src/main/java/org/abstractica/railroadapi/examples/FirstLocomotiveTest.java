package org.abstractica.railroadapi.examples;

import org.abstractica.railroadapi.Locomotive;
import org.abstractica.railroadapi.RailroadAPI;
import org.abstractica.railroadapi.impl.RailroadAPIImpl;

import java.net.SocketException;
import java.net.UnknownHostException;

public class FirstLocomotiveTest
{
	public static void main(String[] args) throws SocketException, UnknownHostException, InterruptedException
	{
		RailroadAPI api = new RailroadAPIImpl();
		Locomotive loc1 = api.createLocomotive("Locomotive 1",2703589);
		api.start();
		api.waitForAllDevicesToConnect();
		while(true)
		{
			System.out.println("Identifying with 2 blinks");
			loc1.identify(2, 10);
			System.out.println("Waiting for 5 seconds...");
			Thread.sleep(5000);
			System.out.println("Changing direction to Forward!");
			loc1.waitForLocomotiveToStopAndSetDirection(Locomotive.Direction.FORWARD);
			System.out.println("Waiting for 5 seconds...");
			Thread.sleep(5000);
			System.out.println("Moving 5 blocks...");
			loc1.moveAndWait(5);
			System.out.println("Changing direction to Backward!");
			loc1.waitForLocomotiveToStopAndSetDirection(Locomotive.Direction.BACKWARD);
			System.out.println("Waiting for 5 seconds...");
			Thread.sleep(5000);
			System.out.println("Moving 5 blocks...");
			loc1.moveAndWait(5);
		}
	}
}
