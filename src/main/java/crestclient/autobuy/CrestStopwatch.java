package crestclient.autobuy;

/** Minimal monotonic stopwatch, milliseconds. */
public final class CrestStopwatch {
    private long start = System.currentTimeMillis();

    public void reset() {
        this.start = System.currentTimeMillis();
    }

    public long elapsed() {
        return System.currentTimeMillis() - this.start;
    }

    public boolean finished(long ms) {
        return elapsed() >= ms;
    }

    /** Returns true once every {@code ms}, resetting the timer. */
    public boolean every(long ms) {
        if (elapsed() >= ms) {
            reset();
            return true;
        }
        return false;
    }
}
