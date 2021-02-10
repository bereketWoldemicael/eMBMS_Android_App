package com.cumucore.cumucoreembms.tools;

public class FileMeasureItem
{
    String name;
    long period;
    boolean initialized = false;

    long prev_time = -1L;
    long delta_time;
    long drift, drift_min, drift_max;

    public String toString()
    {
        return name+" : DT="+delta_time+" ms, drift["+drift_min+" .. "+drift_max+"] = "+drift;
    }

    FileMeasureItem(String name, long period_ms)
    {
        reconfigure(name, period_ms);
    }

    public void reconfigure(String name, long period_ms)
    {
        this.name = name;
        setPeriod(period_ms);
    }

    public void setPeriod(long period_ms)
    {
        this.period = period_ms;
        reset();
    }

    public void reset()
    {
        drift_min = drift_max = drift = 0L;
        initialized = false;
    }

    public void newFileNow()
    {
        long time = System.currentTimeMillis();
        if (!initialized)
            initialized = true;
        else
        {
            delta_time = time - prev_time - period;
            drift += delta_time;
            if (drift < drift_min) drift_min = drift;
            if (drift > drift_max) drift_max = drift;
        }
        prev_time = time;
    }

    public String getName()
    {
        return name;
    }

    public long getDeltaTime()
    {
        return delta_time;
    }

    public long getDrift()
    {
        return drift;
    }

    public long getDriftMin()
    {
        return drift_min;
    }

    public long getDriftMax()
    {
        return drift_max;
    }
}
