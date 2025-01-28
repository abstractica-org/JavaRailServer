package org.abstractica.railroadapi;


public interface Switch
{
    enum Side {LEFT, RIGHT}
    String getName();
    Side getType();
    boolean identify(int numberOfBlinks, int seconds) throws InterruptedException;
    boolean switchTo(Side side) throws InterruptedException;
    boolean switchAndWait(Side side) throws InterruptedException;
    void waitFor(Side side) throws InterruptedException;
    Side waitForSwitch() throws InterruptedException;
}
