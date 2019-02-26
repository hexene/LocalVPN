# extendedVPN
A packet interceptor for Android built on top of VpnService. 
- Provides packages that goes through VPN. 
- VPN DNS server configurable.
- Filter desired app's packages.

## Usage
Your application will need a *ServiceConnection* that subscribes/unsubscribes to the packet listener:
```java
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
```
To configure VPN parameters, *VPNConfig* will have to be set before starting VPN Service.
```java
VPNConfig.setDnsServerAddress("8.8.8.8");  
List<String> packagesNames = Arrays.asList("com.google.android.youtube", "com.facebook.kataka");
VPNConfig.setFilteredPackageNames(packagesNames);
```
Once the configuration is applied, just start the service.
```java
Intent vpnIntent = new Intent(this, LocalVPNService.class);  
boolean result = this.bindService(vpnIntent, (serviceConnection == null)  
        ? serviceConnection = new VPNServiceConnection()  
        : serviceConnection, Context.BIND_AUTO_CREATE);
 ```
        
## License
```Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0
  
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

