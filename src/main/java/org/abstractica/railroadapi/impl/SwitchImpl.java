package org.abstractica.railroadapi.impl;

import org.abstractica.deviceserver.Device;
import org.abstractica.deviceserver.DeviceConnectionListener;
import org.abstractica.deviceserver.DevicePacketHandler;
import org.abstractica.deviceserver.Response;
import org.abstractica.railroadapi.Switch;

public class SwitchImpl implements Switch, DevicePacketHandler, DeviceConnectionListener
{

	private static final int STATE_LEFT = 0;
	private static final int STATE_RIGHT = 1;
	private static final int STATE_SWITCHING_LEFT = 2;
	private static final int STATE_SWITCHING_RIGHT = 3;

	//Commands for switches
	private final static int COMMAND_IDENTIFY = 1000;
	private final int COMMAND_SWITCH_TO_LEFT = 1500;
	private final int COMMAND_SWITCH_TO_RIGHT = 1501;
	private final int COMMAND_SWITCH_GET_STATE = 1502;
	private final int COMMAND_SWITCH_ON_STATE_CHANGE = 2500;


	private final Device device;

	private final String name;
	private final Side type;
	private final Object stateLock;
	private Side waitingForSide;
	private volatile int state = -1;

	public SwitchImpl(String name, Side type, Device device)
	{
		this.name = name;
		this.type = type;
		this.device = device;
		stateLock = new Object();
		waitingForSide = null;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public Side getType()
	{
		return type;
	}

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
	public boolean switchTo(Side side) throws InterruptedException
	{
		int iCmd = side == Side.LEFT ? COMMAND_SWITCH_TO_LEFT : COMMAND_SWITCH_TO_RIGHT;
		Response response = null;
		while(response == null)
		{
			response = device.sendPacket(iCmd, 0, 0, 0, 0, null, true, false);
		}
		return response.getResponse() == 0;
	}

	@Override
	public boolean switchAndWait(Side side) throws InterruptedException
	{
		waitingForSide = side;
		if (!switchTo(side))
		{
			return false;
		}
		waitFor(side);
		return true;
	}

	@Override
	public void waitFor(Side side) throws InterruptedException
	{
		waitingForSide = side;
		synchronized(stateLock)
		{
			if (side == Side.LEFT)
			{
				while (state != STATE_LEFT)
				{
					stateLock.wait();
				}
			} else
			{
				while (state != STATE_RIGHT)
				{
					stateLock.wait();
				}
			}
		}
	}

	@Override
	public Side waitForSwitch() throws InterruptedException
	{
		synchronized(stateLock)
		{
			while (state != STATE_LEFT && state != STATE_RIGHT)
			{
				stateLock.wait();
			}
			Side side = state == STATE_LEFT ? Side.LEFT : Side.RIGHT;
			return side;
		}
	}

	@Override
	public int onPacket(int command, int arg1, int arg2, int arg3, int arg4, byte[] load)
	{
		if(command == COMMAND_SWITCH_ON_STATE_CHANGE)
		{
			updateState(arg1);
			return 0;
		}
		throw new IllegalStateException("Unknown command: " + command);
	}

	private void updateState(int state)
	{
		synchronized(stateLock)
		{
			this.state = state;
			stateLock.notifyAll();
		}
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
		if(waitingForSide != null)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						while(!switchTo(waitingForSide))
						{
							Thread.sleep(1000);
						}
					} catch (InterruptedException e)
					{

					}
					//System.out.println("Tread done!");
				}
			}).start();
		}
	}

	@Override
	public void onDisconnected()
	{
		System.out.println(name + " disonnected!");
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
}
