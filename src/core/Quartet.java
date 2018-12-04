package core;

import core.DTNHost;

/**
 * Class to hold user, device, service and valuation information and 
 * can be sorted based on valuation
 */
public class Quartet implements Comparable<Quartet> {
    /** User valuation of a device for a given application */
    public Double valuation;
    public DTNHost user;
    public DTNHost device;
    public Integer service;

    public Quartet(Double v, DTNHost u, DTNHost d, Integer s) {
        this.valuation = v;
        this.user = u;
        this.device = d;
        this.service = s;
    }

    @Override
    public int compareTo(Quartet aQuartet) {
        return aQuartet.valuation.compareTo(this.valuation);
    }

    public int hashCode() {
        return (this.user.getName() + this.device.getName()).hashCode();
    }

    public boolean equals(Quartet other) {
        if (this.hashCode() == other.hashCode()) 
            return true;

        return false;
    }

    @Override
    public String toString() {
        return "Val=" + this.valuation + " user=" + this.user + " device=" + this.device + " service=" + this.service + "\n";
    }
}

