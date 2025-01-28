package org.abstractica.railroadapi;

public interface Locomotive
{
    enum Direction {FORWARD, BACKWARD};
    String getName();
    boolean identify(int numberOfBlinks, int seconds) throws InterruptedException;
    boolean move(int blocks) throws InterruptedException;
    boolean moveAndWait(int blocks) throws InterruptedException;
    boolean setDirection(Direction direction) throws InterruptedException;
    boolean waitForLocomotiveToStopAndSetDirection(Direction direction) throws InterruptedException;
    int distanceToGoal() throws InterruptedException;
    void waitWhileDistanceToGoalGreaterThan(int distance) throws InterruptedException;
    void waitWhileDistanceToGoal(DistanceToGoalWaiter waiter) throws InterruptedException;
    void waitForLocomotiveToStop() throws InterruptedException;

    interface DistanceToGoalWaiter
    {
        public boolean keepWaiting(int distanceToGoal);
    }
}
