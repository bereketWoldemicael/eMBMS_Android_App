package com.cumucore.cumucoreembms.tools;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class AddressAndPort
{
    public InetAddress address;
    public int port;

    public AddressAndPort()
    {}

    public AddressAndPort(String address_and_port)
    {
        if (!parseString(this, address_and_port))
            throw new IllegalArgumentException("Invalid address:port parameter");
    }

    public boolean parseString(String address_and_port)
    { return parseString(this, address_and_port); }

    public static boolean parseString(AddressAndPort res, String address_and_port)
    {
        int i = address_and_port.lastIndexOf(':');
        if (i<0) return false;
        String addr = null;
        if (address_and_port.startsWith("["))
        {
            if (address_and_port.indexOf(']')!=i-1) return false;
            addr = address_and_port.substring(1,i-1);
        }
        else addr = address_and_port.substring(0,i);
        int port = Integer.parseInt(address_and_port.substring(i+1));
        if (port<1 || port>65535) return false;
        try
        {
            res.address = InetAddress.getByName(addr);
        }
        catch (UnknownHostException e)
        {
            //e.printStackTrace();
            return false;
        }
        res.port = port;
        return true;
    }

    @Override
    public String toString()
    {
        String s = address.getHostAddress();
        if (s.indexOf(':')>=0 && (!s.startsWith("[") || !s.endsWith("]"))) s = "["+s+"]";
        return s + ":" + port;
    }
}
