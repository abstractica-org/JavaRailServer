package org.abstractica.railroadapi;

public interface LocomotiveListener
{
    void onLocomotiveStopped();
    void onBlockExit(int blockValue, int distanceToGoal, int blockTime, int curPower);
}
