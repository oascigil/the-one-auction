package core;

public class VM {

    private int service;
    private double completionTime;
    private DTNHost host;


    public VM() {
        completionTime = SimClock.getTime();
    }

    public void instantiateService(int service) {
        double serviceTime = 0.0;
        try {
            serviceTime = DTNHost.execTimes.get(service);
        } catch (IndexOutOfBoundsException ex) {
            throw new SimError("No such service: " +service+ " at " + this);
        }
        this.service = service;
        this.completionTime = SimClock.getTime() + serviceTime;
    }

    public boolean is_idle(){
        double time = SimClock.getTime();
        return this.completionTime <= time;
    }

    public double getCompletionTime()
    {
        return this.completionTime;
    }
}
