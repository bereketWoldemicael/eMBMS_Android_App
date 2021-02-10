package com.cumucore.cumucoreembms.tools;

import android.util.Log;


import com.cumucore.cumucoreembms.interfaces.IPacketListener;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;

/**
 * Receiver for one multicast address and port.
 *
 * @author Expway
 */
public class MulticastReceiver {
    private String info_ni;
    private String info_ma;
    private static String EWUSERNAME = "MulticastReceiver";

    private FileReceptionMeasure frm = new FileReceptionMeasure();
    private String frm_msg = "";

    private PacketAnalyzerAlc paa = new PacketAnalyzerAlc();
    private String paa_msg = "";

    private MulticastSocket msock;
    private DatagramPacket pack;
    private IPacketListener listener;

    private boolean frm_mode = false;

    public void setModeAnalyzer() {
        frm_mode = false;
    }

    public void setModeFileTimeMeasure() {
        frm_mode = true;
    }

    public String getInfo(String message) {
        return info_ma + " (total: " + count_packet + " packets, " + total_bytes + " bytes) " + getBitrateInfo()
                + frm_msg + "" + paa_msg//(frm_mode?frm_msg:paa_msg)
                + "\n"
                //+" "+message
                ;
    }

    public void clearStats() {
        count_packet = 0;
        total_bytes = 0L;
        start_time = System.currentTimeMillis();
        pbr_prev_bitrate = -1L;
        pbr_bytes = 0L;
        pbr_start_time = System.currentTimeMillis();
        paa_msg = "";
        frm_msg = "";
        paa.reset();
        frm.reset();
    }

    public FileReceptionMeasure getFileReceptionMeasure() {
        return frm;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || ((o instanceof MulticastReceiver) && equals((MulticastReceiver) o));
    }

    public boolean equals(MulticastReceiver o) {
        return o != null && info_ma.equals(o.info_ma);
    }

    @Override
    public int hashCode() {
        return info_ma.hashCode();
    }

    public MulticastReceiver(NetworkInterface ni, String address_and_port, int buf_len, IPacketListener listener) throws IOException {
        AddressAndPort aap = new AddressAndPort();
        if (!aap.parseString(address_and_port))
            throw new IOException("Invalid address and port: " + address_and_port);
        init(ni, aap.address, aap.port, buf_len, listener);
    }

    public MulticastReceiver(NetworkInterface ni, InetAddress address, int port, int buf_len, IPacketListener listener) throws IOException {
        init(ni, address, port, buf_len, listener);
    }

    private void init(NetworkInterface ni, InetAddress address, int port, int buf_len, IPacketListener listener) throws IOException {
        this.listener = listener;
        paa.addFileListener(frm);
        frm.configure(1, 1000);

        info_ni = ni.getDisplayName();
        info_ma = address.getHostAddress() + ":" + port;
        msock = new MulticastSocket(port);
        // todo to be deleted
        msock.setSoTimeout(10000);
        //Group Multicast RTP can be Receive with multiple IP
        InetSocketAddress groupAddr = new InetSocketAddress(address, port);

        //InetAddress groupAddr = address.getByName(address.getHostName().toString());

        msock.joinGroup(groupAddr, ni);
        //msock.joinGroup(groupAddr);

        byte[] buffer = new byte[buf_len];
        pack = new DatagramPacket(buffer, buffer.length);

        pack.setAddress(address);
        pack.setPort(port);
    }

    private volatile boolean run = false;

    public void stopReceiveLoop() {
        run = false;
        msock.close();
    }

    private int count_packet;
    private long total_bytes;
    private long start_time; // for whole bitrate

    // for periodic bitrate
    private long pbr_period = 100; // period in ms
    private long pbr_prev_bitrate; // to display
    private long pbr_start_time = -1L;
    private long pbr_bytes;

    public String getBitrateInfo() {
        return "(C:" + (pbr_prev_bitrate == -1L ? "****" : "" + (pbr_prev_bitrate >> 10)) +
                " T:" + (((total_bytes << 3) * 1000L / (System.currentTimeMillis() - start_time)) >> 10) + " kBits/sec)";
    }

    private void addPeriodicBitrate(int len) {
        long t = System.currentTimeMillis();
        while (t >= pbr_start_time + pbr_period) {
            pbr_start_time += pbr_period;
            if (pbr_period != 0) {
                pbr_prev_bitrate = (pbr_bytes << 3) * 1000L / pbr_period;
            } else {
                pbr_prev_bitrate = pbr_prev_bitrate;
            }
            pbr_bytes = 0L;
        }
        pbr_bytes += len;
    }

    public String getInfoMulticastAddress() {
        return info_ma;
    }

    public long getTotalBytes() {
        return total_bytes;
    }

    public int getCountPacket() {
        return count_packet;
    }

    public void receiveLoop() throws IOException {
        run = true;
        Log.i(EWUSERNAME, "Start receiving from " + info_ma);
        while (run) {
            try {

                msock.receive(pack);



                int len = pack.getLength();
                //if (len != 0)

                //addPeriodicBitrate(len);
                total_bytes += len;
                count_packet++;
                //Log.i(EWUSERNAME, "receiving... (" + count_packet + ")");
                //Log.i(EWUSERNAME, "received 1 packet ("+len+" bytes) from "+info_ma);
                //serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0, String.valueOf(countPacket)));
                byte[] data = pack.getData();

                // Analyse packet
                String msg = paa.analyze(data, 0, len);
                if (msg != null && msg.length() > 0) paa_msg = "\n        " + msg;
                frm_msg = frm.toString();

                listener.packetReceived(this, data, len);

            } catch (Exception e) {
                e.printStackTrace();
                Log.i(EWUSERNAME, "receiveLoop stopped");
            }
        }
        Log.i(EWUSERNAME, "Stop receiving from " + info_ma);
    }
}
