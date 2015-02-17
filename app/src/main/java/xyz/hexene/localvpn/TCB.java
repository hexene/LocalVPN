package xyz.hexene.localvpn;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

/**
 * Transmission Control Block
 */
public class TCB
{
    public String ipAndPort;

    public long mySequenceNum, theirSequenceNum;
    public long myAcknowledgementNum, theirAcknowledgementNum;
    public TCBStatus status;

    // TCP has more states, but we need only these
    public enum TCBStatus
    {
        SYN_RECEIVED,
        ESTABLISHED,
        CLOSE_WAIT,
        LAST_ACK,
    }

    public Packet referencePacket;

    public SocketChannel channel;
    public boolean waitingForNetworkData;
    public SelectionKey selectionKey;

    private static final int MAX_CACHE_SIZE = 50; // XXX: Is this ideal?
    private static LRUCache<String, TCB> tcbCache =
            new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<String, TCB>()
            {
                @Override
                public void cleanup(Map.Entry<String, TCB> eldest)
                {
                    eldest.getValue().closeChannel();
                }
            });

    public static TCB getTCB(String ipAndPort)
    {
        synchronized (tcbCache)
        {
            return tcbCache.get(ipAndPort);
        }
    }

    public static void putTCB(String ipAndPort, TCB tcb)
    {
        synchronized (tcbCache)
        {
            tcbCache.put(ipAndPort, tcb);
        }
    }

    public TCB(String ipAndPort, long mySequenceNum, long theirSequenceNum, long myAcknowledgementNum, long theirAcknowledgementNum,
               SocketChannel channel, Packet referencePacket)
    {
        this.ipAndPort = ipAndPort;

        this.mySequenceNum = mySequenceNum;
        this.theirSequenceNum = theirSequenceNum;
        this.myAcknowledgementNum = myAcknowledgementNum;
        this.theirAcknowledgementNum = theirAcknowledgementNum;

        this.channel = channel;
        this.referencePacket = referencePacket;
        this.status = TCBStatus.SYN_RECEIVED;
    }

    public static void closeTCB(TCB tcb)
    {
        tcb.closeChannel();
        synchronized (tcbCache)
        {
            tcbCache.remove(tcb.ipAndPort);
        }
    }

    public static void closeAll()
    {
        synchronized (tcbCache)
        {
            Iterator<Map.Entry<String, TCB>> it = tcbCache.entrySet().iterator();
            while (it.hasNext())
            {
                it.next().getValue().closeChannel();
                it.remove();
            }
        }
    }

    private void closeChannel()
    {
        try
        {
            channel.close();
        }
        catch (IOException e)
        {
            // Ignore
        }
    }
}
