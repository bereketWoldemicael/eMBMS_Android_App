package com.cumucore.cumucoreembms.tools;



import com.cumucore.cumucoreembms.interfaces.IFileListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;

public class PacketAnalyzerAlc
{
    static class StatBlock
    {
        int max_esi = -1, nb_esi=0;
        BitSet bs_esi = new BitSet();

        void addSymbol(int esi)
        {
            if (esi>max_esi) max_esi = esi;
            if (!bs_esi.get(esi))
            {
                bs_esi.set(esi);
                nb_esi++;
            }
        }

        public int getLossPercent()
        {
            // 0..max_esi -> total number of symbols = max_esi+1
            int total_esi = (max_esi+1);
            return 100*(total_esi - nb_esi)/total_esi;
        }
    }

    static class StatFile
    {
        String file_id;
        StatFile(String file_id)
        {
            this.file_id = file_id;
        }

        int max_sbn = -1, nb_blocks = 0, fec_error = 0;
        StatBlock[] blocks;

        public String getInfo()
        {
            return "max-loss="+getMaxLossPercent()+"%";//" LB="+getBlockLoss()+" FEC-errors="+fec_error;
        }

        public int getBlockLoss()
        {
            return max_sbn+1-nb_blocks;
        }

        public int getMaxLossPercent()
        {
            int max = 0;
            for(int i=0 ; i<=max_sbn ; i++)
            {
                StatBlock b = blocks[i];
                if (b!=null)
                {
                    int p = b.getLossPercent();
                    if (p>max) max=p;
                }
            }
            return max;
        }

        void addPacket(int sbn, int esi)
        {
            if (sbn>max_sbn) max_sbn = sbn;
            if (blocks==null) blocks = new StatBlock[sbn+16];
            else if (sbn>=blocks.length)
            {
                blocks = Arrays.copyOf(blocks, sbn+16);
            }
            if (blocks[sbn]==null)
            {
                blocks[sbn]=new StatBlock();
                nb_blocks++;
            }
            blocks[sbn].addSymbol(esi);
        }
    }

    private static void appendId(StringBuilder sb, short[] id)
    {
        for(int i=0 ; i<id.length ; i++)
        {
            if (i==0) sb.append(Integer.toHexString(id[i] & 0xFFFF));
            else
            {
                String t = "000"+ Integer.toHexString(id[i] & 0xFFFF);
                sb.append(t.substring(t.length()-4));
            }
        }
    }

    public static String getFileId(short[] tsi, short[] toi)
    {
        StringBuilder sb = new StringBuilder();
        appendId(sb, tsi);
        sb.append(':');
        appendId(sb, toi);
        return sb.toString();
    }

    public static String getToi(short[] toi)
    {
        StringBuilder sb = new StringBuilder();
        appendId(sb, toi);
        return sb.toString();
    }

    private ArrayList<IFileListener> all_file_listeners = new ArrayList<IFileListener>();

    public void addFileListener(IFileListener listener)
    {
        all_file_listeners.add(listener);
    }

    public void removeFileListener(IFileListener listener)
    {
        all_file_listeners.remove(listener);
    }

    protected void fireFileReceived(int toi)
    {
        for(IFileListener l : all_file_listeners)
            l.fileReceived(toi);
    }

    private HashMap<String, StatFile> hm_files = new HashMap<String, StatFile>();
    private LinkedList<StatFile> all_files = new LinkedList<StatFile>();

    private final Object lock = new Object();
    private String last_file_id = null;
    private String last_file_toi = null;
    private StatFile last_file_stat = null;

    private static final int LAST_COUNT = 10;
    private static final int MAX_STAT_MEMORY = 20;

    private int last_index=0, last_count=0;
    private String[] last_infos = new String[LAST_COUNT];
    private String[] last_id = new String[LAST_COUNT];

    private int findLastId(String id)
    {
        int j = (last_infos.length+last_index-last_count)%last_infos.length;
        for(int i=0;i<last_count;i++)
        {
            if (id.equals(last_id[j])) return j;
            j++;
            if (j>=last_infos.length) j=0;
        }
        return -1;
    }

    private void addLastInfo(String id, String info)
    {
        int k = findLastId(id);
        if (k<0)
        {
            if (last_count<last_infos.length) last_count++;
            last_id[last_index] = id;
            last_infos[last_index++] = info;
            if (last_index>=last_infos.length) last_index=0;
        }
        else
        {
            last_infos[k] = info;
        }
    }

    private String getLastInfos()
    {
        StringBuilder sb = new StringBuilder();
        int j = (last_infos.length+last_index-last_count)%last_infos.length;
        for(int i=0;i<last_count;i++)
        {
            if (sb.length()>0)
                sb.append(' ');
            //sb.append("\n        ");
            sb.append(last_infos[j]);
            j++;
            if (j>=last_infos.length) j=0;
        }
        return sb.toString();
    }

    private StatFile getFileStat(short[] tsi, short[] toi)
    {
        return getFileStat(getFileId(tsi, toi));
    }

    private StatFile getFileStat(String file_id)
    {
        StatFile f;
        synchronized(hm_files)
        {
            f = hm_files.get(file_id);
            if (f==null)
            {
                if (hm_files.size()>=MAX_STAT_MEMORY) // map full
                {
                    // remove least recent element
                    StatFile fr = all_files.remove();
                    hm_files.remove(fr.file_id);
                }
                hm_files.put(file_id, f = new StatFile(file_id));
            }
            else all_files.remove(f);
            all_files.add(f); // set as most recent
        }
        return f;
    }

    static class BufferIterator
    {
        byte[] b;
        int ofs;
        int len;

        BufferIterator(byte[] b, int ofs, int len)
        {
            this.b = b;
            this.ofs = ofs;
            this.len = len;
        }

        public void skip(int l) throws IOException
        {
            if (l>len) throw new IOException("Invalid format");
            len -= l;
            ofs += l;
        }

        public void readBytes(byte[] b, int o, int l) throws IOException
        {
            if (l>len) throw new IOException("Invalid format");
            System.arraycopy(this.b,ofs, b,o, l);
            len -= l;
            ofs += l;
        }

        // Big endian
        public int readInt(int l) throws IOException
        {
            if (l>len) throw new IOException("Invalid format");
            int n = 0;
            while (l-->0)
            {
                n = (n<<8)|(b[ofs++]&0xFF);
                len--;
            }
            return n;
        }

        // Big endian
        public long readLong(int l) throws IOException
        {
            if (l>len) throw new IOException("Invalid format");
            long n = 0L;
            while (l-->0)
            {
                n = (n<<8)|(b[ofs++]&0xFF);
                len--;
            }
            return n;
        }
    }

    int lct_parsing_error = 0;
    int fec_parsing_error = 0;

    public void reset() // reset all stats
    {
        last_index=last_count=0;
        lct_parsing_error = 0;
        fec_parsing_error = 0;
        last_file_id = null;
        last_file_toi = null;
        last_file_stat = null;
        hm_files.clear();
    }

    public String analyze(byte[] b, int ofs, int len)
    {
        try
        {
            return analyzeData(b, ofs, len);
        }
        catch (IOException e)
        {
            lct_parsing_error++;
            return "RTP Analyser";
        }
    }

    public String analyzeData(byte[] b, int ofs, int len) throws IOException
    {
        String result = null;
        BufferIterator buf = new BufferIterator(b, ofs, len);
        int flags = buf.readInt(2);
        int version = (byte) (flags>>>12);
        if (version != 1)
            throw new IOException("Unsupported LCT version "+version);
//			if ((flags&0x000C)!=0)
//				throw new IOException("Invalid LCT packet");
        byte psi = (byte) ((flags>>>8)&3);
        boolean end_of_session = ((flags&0x0002)!=0);
        boolean end_of_object = ((flags&0x0001)!=0);
        int hlen = buf.readInt(1)<<2;
        byte codepoint = (byte) buf.readInt(1);
        int count = 1+((flags>>>10)&3);
        int[] cci = new int[count];
        for(int i=0;i<count;i++)
            cci[i] = buf.readInt(4);
        count = ((flags>>>6)&0x02)|((flags>>>4)&0x01);
        short[] tsi = new short[count];
        for(int i=0;i<count;i++)
            tsi[i] = (short)buf.readInt(2);
        count = ((flags>>>4)&0x07);
        short[] toi = new short[count];
        for(int i=0;i<count;i++)
            toi[i] = (short)buf.readInt(2);

        if (hlen<buf.ofs) throw new IOException("Invalid LCT packet header length");
        // Parse header extensions
        int m = 8, g = 1; // Reed-Solomon
        int n1m3 = -1; // LDPC
        int prng_seed = -1; // LDPC
        int max_source_block_len = -1; // B
        int encoding_symbol_len = -1;  // E
        int max_nb_enc_symbols = -1;   // max_n
        int fec_instance_id = 0;

        while (buf.ofs<hlen)
        {
            int a_ext = buf.readInt(4);
            byte ext_type = (byte)(a_ext>>>24); // HET
            if (ext_type<0) // 80..FF
            {
                switch(ext_type)
                {
                    case -0x40: // EXT_FDT
                        //int flute_ver = (a_ext>>>20) & 0xF;
                        //int fdt_instance_id = a_ext & 0xFFFFF;
                        break;
                }
            }
            else // 00..7F
            {
                int l = ((a_ext>>>16)&0xFF)<<2;
                if (buf.ofs+l-4>hlen)
                    throw new IOException("Invalid LCT packet header extension length");
                byte[] content = new byte[l-2];
                content[0] = (byte)(a_ext>>>8);
                content[1] = (byte)(a_ext);
                buf.readBytes(content, 2, l-4);
                BufferIterator bufext = new BufferIterator(content, 0, content.length);

                switch(ext_type)
                {
                    case 0x40: // EXT_FTI
                        bufext.skip(6);//long transfer_length = bufext.readLong(6);
                        switch(codepoint)
                        {
                            case 0:   // Compact No-Code
                            case -128: // Small Block, Large Block and Expandable
                            case -126: // Compact
                                fec_instance_id = bufext.readInt(2);
                                encoding_symbol_len = bufext.readInt(2);
                                max_source_block_len = bufext.readInt(4);
                                break;
                            case 2: // Reed-Solomon
                                m = bufext.readInt(1);
                                g = bufext.readInt(1);
                                encoding_symbol_len = bufext.readInt(2);
                                max_source_block_len = bufext.readInt(2);
                                max_nb_enc_symbols = bufext.readInt(2);
                                break;
                            case 3: // LDPC staircase
                            case 4: // LDPC triangle
                                encoding_symbol_len = bufext.readInt(2);
                            {
                                int a = bufext.readInt(1);
                                n1m3 = a>>>5;
                                g = a & 0x1F;
                            }
                            {
                                long a = bufext.readLong(5);
                                max_source_block_len = (int)(a>>>20);
                                max_nb_enc_symbols = (int)(a & 0xFFFFF);
                            }
                            prng_seed = bufext.readInt(4);
                            break;
                            case 5: // Reed-Solomon m=8 g=1
                                m = 8;
                                g = 1;
                                encoding_symbol_len = bufext.readInt(2);
                                max_source_block_len = bufext.readInt(1);
                                max_nb_enc_symbols = bufext.readInt(1);
                                break;
                        }
                        break;
                }
            }
        }
        if (hlen!=buf.ofs) throw new IOException("Invalid LCT packet header length");

        //Log.i("Expway", "Analyze HLEN="+hlen+" file-ID="+getFileId(tsi, toi)+"...");

        String file_id = getFileId(tsi, toi);
        String file_toi = getToi(toi);
        StatFile sf = getFileStat(file_id);

        int sbn, esi;
        // Parse FEC payload ID
        switch(codepoint)
        {
            case 2: // Reed-Solomon
            case 5: // Reed-Solomon m=8
            {
                int fec_p_id = buf.readInt(4);
                sbn = fec_p_id >>> m;
                esi = fec_p_id & ((1<<m)-1);
            }
            break;
            case 3: // LDPC staircase
            case 4: // LDPC triangle
            {
                int fec_p_id = buf.readInt(4);
                sbn = fec_p_id >>> 20;
                esi = fec_p_id & 0xFFFFF;
            }
            break;
            default:
                fec_parsing_error++;
                //return "ERROR FEC "+codepoint;
                sf.fec_error++;
                return null;
        }

        sf.addPacket(sbn, esi);
        synchronized (lock)
        {
            if (last_file_id==null)
            {
                last_file_id = file_id;
                last_file_toi = file_toi;
                last_file_stat = sf;
            }
            else if (!last_file_id.equals(file_id))
            {
                //result = "file "+last_file_id+" "+last_file_stat.getInfo();
                result = "TOI:0x"+last_file_toi+" "+last_file_stat.getInfo();
                addLastInfo(last_file_toi, "("+result+")");
                result = getLastInfos();

                last_file_id = file_id;
                last_file_toi = file_toi;
                last_file_stat = sf;
            }
            fireFileReceived(toi[0]);
        }
        return result;
    }
}
