package org.abstractica.railroadapi.impl;

import org.abstractica.deviceserver.Device;
import org.abstractica.deviceserver.DeviceConnectionListener;
import org.abstractica.deviceserver.DevicePacketHandler;
import org.abstractica.deviceserver.Response;
import org.abstractica.railroadapi.Locomotive;

import java.util.ArrayList;
import java.util.Collection;

public class LocomotiveImpl implements Locomotive, DevicePacketHandler, DeviceConnectionListener
{
	//Commands for locomotives
	private final static int COMMAND_IDENTIFY = 1000;
	private final static int COMMAND_MOVE = 1001;
	private final static int COMMAND_DIRECTION = 1002;
	private final static int COMMAND_DISTANCE_TO_GOAL = 2000;
	private final static int COMMAND_STOPPED = 2001;

	private final Device device;

	private final String name;
	private volatile int distanceToGoal;
	private volatile boolean dtgOK;
	private volatile boolean isStopped;
	private final Object dtgLock;
	private final Object isStoppedLock;

	LocomotiveImpl(String name, Device device)
	{
		this.name = name;
		this.device = device;
		listeners = new ArrayList<>();
		distanceToGoal = 0;
		dtgOK = true;
		dtgLock = new Object();
		isStopped = true;
		isStoppedLock = new Object();
	}

	private Collection<DistanceToGoalListener> listeners;

	@Override
	public boolean identify(int numberOfBlinks, int seconds) throws InterruptedException
	{
		Response response = null;
		while(response == null)
		{
			response = device.sendPacket(COMMAND_IDENTIFY, numberOfBlinks, seconds, 0, 0, null, true, false);
		}
		return response.getResponse() == 0;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean move(int blocks) throws InterruptedException
	{
		if(blocks < 0 || blocks > 1000)
		{
			throw new IllegalArgumentException("Illegal move command!");
		}
		if(blocks == 0) return true;
		synchronized (isStoppedLock)
		{
			isStopped = false;
			isStoppedLock.notifyAll();
		}
		dtgOK = false;
		Response response = null;
		while(response == null)
		{
			response = device.sendPacket(COMMAND_MOVE, blocks, 0, 0, 0, null, true, false);
		}
		return response.getResponse() == 0;
	}

	@Override
	public boolean moveAndWait(int blocks) throws InterruptedException
	{
		boolean res = move(blocks);
		if(res)
		{
			waitWhileDistanceToGoalGreaterThan(0);
		}
		waitForLocomotiveToStop();
		return res;
	}

	@Override
	public boolean setDirection(Direction direction) throws InterruptedException
	{
		if(!isStopped)
		{
			return false;
		}
		Response response = null;
		while(response == null)
		{
			response = device.sendPacket(COMMAND_DIRECTION, direction.ordinal(), 0, 0, 0, null, true, false);
		}
		return response.getResponse() == 0;
	}

	@Override
	public boolean waitForLocomotiveToStopAndSetDirection(Direction direction) throws InterruptedException
	{
		synchronized (isStoppedLock)
		{
			while(!isStopped)
			{
				isStoppedLock.wait();
			}
		}
		Response response = null;
		while(response == null)
		{
			response = device.sendPacket(COMMAND_DIRECTION, direction.ordinal(), 0, 0, 0, null, true, false);
		}
		return response.getResponse() == 0;
	}

	@Override
	public int distanceToGoal() throws InterruptedException
	{
		synchronized(dtgLock)
		{
			while(!dtgOK)
			{
				dtgLock.wait();
			}
		}
		return distanceToGoal;
	}

	@Override
	public void waitWhileDistanceToGoalGreaterThan(int distance) throws InterruptedException
	{
		waitWhileDistanceToGoal(dtg -> dtg > distance);
	}

	@Override
	public void waitWhileDistanceToGoal(DistanceToGoalWaiter waiter) throws InterruptedException
	{
		if (waiter.keepWaiting(distanceToGoal))
		{
			DistanceToGoalListener listener = new DistanceToGoalListener(waiter);
			listeners.add(listener);
			listener.doWait();
		}
	}

	@Override
	public void waitForLocomotiveToStop() throws InterruptedException
	{
		synchronized (isStoppedLock)
		{
			while(!isStopped)
			{
				isStoppedLock.wait();
			}
		}
	}

	@Override
	public int onPacket(int command, int arg1, int arg2, int arg3, int arg4, byte[] load)
	{
		System.out.println("Cmd: " + command + " Arg1: " + arg1 + " Arg2: " + arg2 + " Arg3: " + arg3 + " Arg4: " + arg4);
		if(command == COMMAND_DISTANCE_TO_GOAL)
		{
			updateDistanceToGoal(arg2);
			return 0;
		}
		if(command == COMMAND_STOPPED)
		{
			synchronized (isStoppedLock)
			{
				isStopped = true;
				isStoppedLock.notifyAll();
			}
			return 0;
		}
		throw new IllegalStateException("Unknown command: " + command);
	}

	@Override
	public String toString()
	{
		return name;
	}

	private void updateDistanceToGoal(int distanceToGoal)
	{
		if (this.distanceToGoal != distanceToGoal)
		{
			this.distanceToGoal = distanceToGoal;
			onDistanceToGoalChange();
		}
		synchronized (dtgLock)
		{
			//System.out.println("dtgOK is true!");
			dtgOK = true;
			dtgLock.notifyAll();
		}
	}

	private void onDistanceToGoalChange()
	{
		ArrayList<DistanceToGoalListener> doomed = new ArrayList<>();
		for(DistanceToGoalListener listener : listeners)
		{
			if(!listener.onDistanceToGoalChange())
			{
				doomed.add(listener);
			}
		}
		listeners.removeAll(doomed);
	}

	@Override
	public void onCreated()
	{
		System.out.println(name + " created!");
	}

	@Override
	public void onConnected()
	{
		System.out.println(name + " connected!");
	}

	@Override
	public void onDisconnected()
	{
		System.out.println(name + " disconnected!");
	}

	@Override
	public void onLost()
	{
		System.out.println(name + " lost!");
	}

	@Override
	public void onDestroyed()
	{
		System.out.println(name + " destroyed!");
	}

	private class DistanceToGoalListener
	{
		private final DistanceToGoalWaiter waiter;
		private boolean keepWaiting;

		private DistanceToGoalListener(DistanceToGoalWaiter waiter)
		{
			this.waiter = waiter;
			this.keepWaiting = true;
		}

		public synchronized void doWait() throws InterruptedException
		{
			keepWaiting = waiter.keepWaiting(distanceToGoal);
			while(keepWaiting)
			{
				wait();
			}
		}

		public synchronized boolean onDistanceToGoalChange()
		{
			keepWaiting = waiter.keepWaiting(distanceToGoal);
			if(!keepWaiting)
			{
				notifyAll();
			}
			return keepWaiting;
		}
	}
}
