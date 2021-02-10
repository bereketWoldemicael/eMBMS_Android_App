package com.cumucore.cumucoreembms.interfaces;

import com.cumucore.cumucoreembms.tools.MulticastReceiver;

public interface IPacketListener {
    public void packetReceived(MulticastReceiver source, byte[] data, int length);
}
