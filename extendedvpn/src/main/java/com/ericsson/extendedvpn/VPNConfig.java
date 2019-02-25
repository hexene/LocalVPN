package com.ericsson.extendedvpn;

import java.util.ArrayList;
import java.util.List;

public class VPNConfig {

    private static String dnsServerAddress;
    private static List<String> filteredPackageNames;
    private static String vpnName;

    public static String getDnsServerAddress() {
        return dnsServerAddress;
    }

    public static void setDnsServerAddress(String newDnsServer) {
        dnsServerAddress = newDnsServer;
    }

    public static List<String> getFilteredPackageNames() {
        return filteredPackageNames;
    }

    public static void setFilteredPackageNames(List<String> packageNames) {
        filteredPackageNames = packageNames;
    }

    public boolean addFilteredPackageName(String filteredPackageName) {
        if (filteredPackageNames == null) {
            filteredPackageNames = new ArrayList<>();
        }
        filteredPackageNames.add(filteredPackageName);
        return true;
    }

    public static String getVpnName() {
        return vpnName;
    }

    public static void setVpnName(String name) {
        vpnName = name;
    }
}
