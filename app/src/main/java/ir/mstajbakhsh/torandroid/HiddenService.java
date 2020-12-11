package ir.mstajbakhsh.torandroid;

import java.util.HashMap;
import java.util.Map;

import ir.mstajbakhsh.torandroid.Connection.Address;


public class HiddenService {
    private Map<Integer, Address> hiddenServices = new HashMap<>();
    private String hiddenAddress = "notdefinedhidden.onion";
    private String hiddenServiceDir = "";
    private HSVersion hiddenServiceVersion = HSVersion.THREE;

    public String getHiddenServiceAddress() {
        return hiddenAddress;
    }

    public void setHiddenAddress(String hiddenAddress) {
        this.hiddenAddress = hiddenAddress;
    }

    public void addHiddenServiceMapping(int externalPort, Address destinationAddress) {
        hiddenServices.put(externalPort, destinationAddress);
    }

    public void removeAllHiddenServiceMappings() {
        hiddenServices.clear();
    }

    public HSVersion getHiddenServiceVersion() {
        return hiddenServiceVersion;
    }

    public void setHiddenServiceVersion(HSVersion version) {
        hiddenServiceVersion = version;
    }

    public String getHiddenServiceDir() {
        return hiddenServiceDir;
    }

    public void setHiddenServiceDir(String hiddenServiceDir) {
        this.hiddenServiceDir = hiddenServiceDir;
    }

    @Override
    public String toString() {

        String commands = "HiddenServiceDir " + getHiddenServiceDir() + "\r\n";

        if (getHiddenServiceVersion() == HSVersion.TWO) {
            commands += "HiddenServiceVersion 2\r\n";
        }

        for (Map.Entry<Integer, Address> pair : hiddenServices.entrySet()) {
            commands += "HiddenServicePort " + pair.getKey().toString() + " " + pair.getValue().getHost() + ":" + pair.getValue().getPort() + "\r\n";
        }

        return commands;
    }

    public Map<Integer, Address> getHiddenServiceMapping() {
        return hiddenServices;
    }

    public enum HSVersion {
        TWO(2), THREE(3);

        private final int value;

        HSVersion(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }
    }
}
