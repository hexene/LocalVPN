package com.ericsson.extendedvpn;

public interface VPNPacketListenerSubscriber {
    void subscribeVPNPacketListener(VPNPacketListener listener);
    void unsubscribeVPNPacketListener(VPNPacketListener listener);
    void removeAll();
}
