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

package com.ericsson.vpnexample;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ericsson.extendedvpn.LocalVPNService;
import com.ericsson.extendedvpn.R;
import com.ericsson.extendedvpn.VPNConfig;
import com.ericsson.extendedvpn.VPNPacketListener;
import com.ericsson.extendedvpn.VPNServiceBinder;

import java.util.Arrays;
import java.util.List;


public class LocalVPNActivity extends AppCompatActivity implements VPNPacketListener {
    private static final int VPN_REQUEST_CODE = 0x0F;

    private boolean waitingForVPNStart;

    private static VPNServiceBinder serviceBinder;
    private static VPNServiceConnection serviceConnection;

    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (LocalVPNService.BROADCAST_VPN_STATE.equals(intent.getAction())) {
                if (intent.getBooleanExtra("running", false))
                    waitingForVPNStart = false;
            }
        }
    };


    private class VPNServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("ServiceConnection", "connected");
            serviceBinder = (VPNServiceBinder) service;
            serviceBinder.subscribeVPNPacketListener(LocalVPNActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("ServiceConnection", "disconnected");
            if (serviceBinder != null) {
                serviceBinder.unsubscribeVPNPacketListener(LocalVPNActivity.this);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_vpn);
        final Button vpnButton = (Button) findViewById(R.id.vpn);
        vpnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startVPN();
            }
        });
        waitingForVPNStart = false;
        LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
                new IntentFilter(LocalVPNService.BROADCAST_VPN_STATE));
    }

    private void startVPN() {
        Intent vpnIntent = VpnService.prepare(this);
        if (vpnIntent != null)
            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
        else
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            waitingForVPNStart = true;
//            startService(new Intent(this, LocalVPNService.class));
            VPNConfig.setDnsServerAddress("8.8.8.8");
            List<String> packagesNames = Arrays.asList("com.google.android.youtube", "com.facebook.kataka");
            VPNConfig.setFilteredPackageNames(packagesNames);
            Intent vpnIntent = new Intent(this, LocalVPNService.class);
            boolean result = this.bindService(vpnIntent, (serviceConnection == null)
                    ? serviceConnection = new VPNServiceConnection()
                    : serviceConnection, Context.BIND_AUTO_CREATE);
            enableButton(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        enableButton(!waitingForVPNStart && !LocalVPNService.isRunning());
    }

    private void enableButton(boolean enable) {
        final Button vpnButton = (Button) findViewById(R.id.vpn);
        if (enable) {
            vpnButton.setEnabled(true);
            vpnButton.setText(R.string.start_vpn);
        } else {
            vpnButton.setEnabled(false);
            vpnButton.setText(R.string.stop_vpn);
        }
    }

    @Override
    public void onVPNPacketReceived(byte[] packet) {
        Log.d("LocalVPN", "Packet received");
    }

    @Override
    protected void onDestroy() {
        if (LocalVPNService.isRunning()) {
            Intent stopIntent = new Intent(this, LocalVPNService.class);
            stopIntent.putExtra("cmd", "stop");
            this.startService(stopIntent);
        }
        super.onDestroy();
    }
}
