package com.cumucore.cumucoreembms.tools;


import com.cumucore.cumucoreembms.interfaces.IFileListener;

public class FileReceptionMeasure
        implements IFileListener
{
    FileMeasureItem[] items;
    long period;
    int prev_toi = -1;

    @Override
    public String toString()
    {
        if (items==null) return "NOT CONFIGURED";
        StringBuilder sb = new StringBuilder("(period =  "+period+"ms)");
        for(FileMeasureItem item : items)
        {
            sb.append("\n  ");
            sb.append(item);
        }
        return sb.toString();
    }

    public void setPeriod(long p_ms)
    {
        if (p_ms != period)
        {
            period = p_ms;
            if (items != null)
            {
                //p_ms *= items.length;
                for (FileMeasureItem item : items)
                    item.setPeriod(p_ms);
            }
        }
    }

    public void fileReceived(int toi)
    {
        // ignore FDTs and other file now
        if (toi!= 0 && toi != prev_toi)
        {
            prev_toi = toi;
            items[toi % items.length].newFileNow();
        }
    }

    public void reset()
    {
        if (items != null)
            for (FileMeasureItem item : items)
                item.reset();
    }

    public void setFileCount(int live_file_count)
    {
        configure(live_file_count, period);
    }

    public void configure(int live_file_count, long period_ms)
    {
        if (items==null || live_file_count != items.length)
        {
            this.period = period_ms;
            items = new FileMeasureItem[live_file_count];
            //period_ms *= items.length;
            for(int i=0 ; i<items.length ; i++)
                items[i] = new FileMeasureItem(""+(char)('A'+i), period_ms);
        }
        else setPeriod(period_ms);
    }
}
