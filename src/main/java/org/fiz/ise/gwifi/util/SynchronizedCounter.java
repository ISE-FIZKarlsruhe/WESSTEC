package org.fiz.ise.gwifi.util;
public class SynchronizedCounter {
    private long c = 0;

    public synchronized void increment() {
        c++;
    }
    public synchronized void incrementbyValue(int value) {
        c+=value;
    }
    public synchronized void decrement() {
        c--;
    }

    public synchronized long value() {
        return c;
    }
    public synchronized void setValue(long value) {
        c=value;
    }
}