package com.ericsson.extendedvpn;

public interface VPNPacketListener {

    void onVPNPacketReceived(byte[] packet);
}
