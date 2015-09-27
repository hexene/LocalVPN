/*
** Copyright 2015, Mohamed Naufal
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

package xyz.hexene.localvpn;

import android.util.Log;

import java.io.IOException;
import java.net.BindException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UDPOutput implements Runnable
{
    private static final String TAG = UDPOutput.class.getSimpleName();

    private LocalVPNService vpnService;
    private ConcurrentLinkedQueue<Packet> inputQueue;
    private Selector selector;

    private static final int MAX_CACHE_SIZE = 50;
    private LRUCache<String, DatagramChannel> channelCache =
            new LRUCache<>(MAX_CACHE_SIZE, new LRUCache.CleanupCallback<String, DatagramChannel>()
            {
                @Override
                public void cleanup(Map.Entry<String, DatagramChannel> eldest)
                {
                    closeChannel(eldest.getValue());
                }
            });

    public UDPOutput(ConcurrentLinkedQueue<Packet> inputQueue, Selector selector, LocalVPNService vpnService)
    {
        this.inputQueue = inputQueue;
        this.selector = selector;
        this.vpnService = vpnService;
    }

    @Override
    public void run()
    {
        Log.i(TAG, "Started");
        try
        {

            Thread currentThread = Thread.currentThread();
            while (true)
            {
                Packet currentPacket;
                // TODO: Block when not connected
                do
                {
                    currentPacket = inputQueue.poll();
                    if (currentPacket != null)
                        break;
                    Thread.sleep(10);
                } while (!currentThread.isInterrupted());

                if (currentThread.isInterrupted())
                    break;

                InetAddress destinationAddress = currentPacket.ip4Header.destinationAddress;
                int destinationPort = currentPacket.udpHeader.destinationPort;
                int sourcePort = currentPacket.udpHeader.sourcePort;

                String ipAndPort = destinationAddress.getHostAddress() + ":" + destinationPort + ":" + sourcePort;
                DatagramChannel outputChannel = channelCache.get(ipAndPort);
                if (outputChannel == null) {
                    outputChannel = DatagramChannel.open();
                    // Workaround for bug 64819 ( https://code.google.com/p/android/issues/detail?id=64819 )
                    InetSocketAddress sa = new InetSocketAddress(getIPAddress(), sourcePort);
                    try {
                        outputChannel.socket().setReuseAddress(true);
                        outputChannel.socket().bind(sa);
                    } catch (BindException e) {
                        Log.d(TAG, sa.toString() + " " + e.toString(), e);
                    }
                    try
                    {
                        outputChannel.connect(new InetSocketAddress(destinationAddress, destinationPort));
                    }
                    catch (IOException e)
                    {
                        Log.e(TAG, "Connection error: " + ipAndPort, e);
                        closeChannel(outputChannel);
                        ByteBufferPool.release(currentPacket.backingBuffer);
                        continue;
                    }
                    outputChannel.configureBlocking(false);
                    currentPacket.swapSourceAndDestination();

                    selector.wakeup();
                    outputChannel.register(selector, SelectionKey.OP_READ, currentPacket);

                    vpnService.protect(outputChannel.socket());

                    channelCache.put(ipAndPort, outputChannel);
                }

                try
                {
                    ByteBuffer payloadBuffer = currentPacket.backingBuffer;
                    while (payloadBuffer.hasRemaining())
                        outputChannel.write(payloadBuffer);
                }
                catch (IOException e)
                {
                    Log.e(TAG, "Network write error: " + ipAndPort, e);
                    channelCache.remove(ipAndPort);
                    closeChannel(outputChannel);
                }
                ByteBufferPool.release(currentPacket.backingBuffer);
            }
        }
        catch (InterruptedException e)
        {
            Log.i(TAG, "Stopping");
        }
        catch (IOException e)
        {
            Log.i(TAG, e.toString(), e);
        }
        finally
        {
            closeAll();
        }
    }

    private void closeAll()
    {
        Iterator<Map.Entry<String, DatagramChannel>> it = channelCache.entrySet().iterator();
        while (it.hasNext())
        {
            closeChannel(it.next().getValue());
            it.remove();
        }
    }

    private void closeChannel(DatagramChannel channel)
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

    //http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device
    private InetAddress getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                        return addr;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }
}
