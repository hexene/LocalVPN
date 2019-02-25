package com.ericsson.extendedvpn;

import android.os.Binder;

import java.util.ArrayList;
import java.util.List;

public class VPNServiceBinder extends Binder implements VPNPacketListenerSubscriber {

    private static List<VPNPacketListener> packetListeners;

    public static List<VPNPacketListener> getPacketListeners() {
        return packetListeners;
    }


    @Override
    public void subscribeVPNPacketListener(VPNPacketListener listener) {
        if (packetListeners == null) {
            packetListeners = new ArrayList<>();
        }
        packetListeners.add(listener);
    }

    @Override
    public void unsubscribeVPNPacketListener(VPNPacketListener listener) {
        if (packetListeners != null) {
            packetListeners.remove(listener);
        }
    }

    @Override
    public void removeAll() {
        if (packetListeners != null) {
            packetListeners.clear();
        }
    }

}