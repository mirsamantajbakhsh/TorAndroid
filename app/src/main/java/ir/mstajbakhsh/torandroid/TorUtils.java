package ir.mstajbakhsh.torandroid;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ir.mstajbakhsh.torandroid.Connection.Address;

public class TorUtils {
    private static HiddenService hs;

    private static String[] cases = {"SocksPort", "HiddenServiceDir", "HiddenServicePort", "HiddenServiceVersion", "UseBridges 1"};


    public static TorProxy.TorBuilder parseTorrc(File torrc) {
        TorProxy.TorBuilder builder = new TorProxy.TorBuilder();
        String extraLines = "";

        List<HiddenService> foundList = new ArrayList<>();

        String[] lines = readAll(torrc).split("\r\n");
        for (String line : lines) {

            int i = findMatch(line, cases);

            switch (i) {
                case 0: //SocksPort
                    builder.setSOCKsPort(Integer.parseInt(line.split(" ")[1].trim()));
                    break;
                case 1: //HiddenServiceDir
                    hs = new HiddenService();
                    hs.setHiddenServiceDir(line.substring(line.indexOf(" ")).trim());

                    //Check for hidden service onion address
                    String hostname = readAll(new File(hs.getHiddenServiceDir() + File.separator + "hostname"));
                    hostname = hostname.replaceAll("\n", "");
                    hs.setHiddenAddress(hostname);

                    foundList.add(hs);
                    break;
                case 2: //HiddenServicePort
                    String[] parts = line.split(" ");

                    int externalPort = 0;
                    String ip = "";
                    int innerPort = 0;

                    externalPort = Integer.parseInt(parts[1]);

                    if (parts[2].contains(":")) {
                        ip = parts[2].split(":")[0];
                        innerPort = Integer.parseInt(parts[2].split(":")[1]);
                    } else {
                        ip = parts[2];
                        innerPort = externalPort;
                    }

                    hs.addHiddenServiceMapping(externalPort, new Address(ip, innerPort));
                    break;
                case 3: //HiddenServiceVersion
                    //tor binary is 0.4. Default hs version is 3!
                    if (line.split(" ")[1].equalsIgnoreCase("2")) {
                        hs.setHiddenServiceVersion(HiddenService.HSVersion.TWO);
                    } else {
                        hs.setHiddenServiceVersion(HiddenService.HSVersion.THREE);
                    }
                case 4: //UseBridges 1
                    builder.setUseBrideges(true);
                    break;
                case -1:
                default:
                    extraLines += line + "\r\n";
                    break;
            }
        }

        builder.services = foundList;
        builder.setExtra(extraLines);
        return builder;
    }

    private static int findMatch(String line, String[] cases) {
        for (int i = 0; i < cases.length; i++) {
            if (line.contains(cases[i])) {
                return i;
            }
        }
        return -1;
    }

    private static String readAll(File inputFile) {
        try {
            FileInputStream fis = new FileInputStream(inputFile);
            byte[] data = new byte[(int) inputFile.length()];
            fis.read(data);
            fis.close();

            String totalFile = new String(data, "UTF-8");
            return totalFile;
        } catch (IOException ex) {
            return "";
        }
    }

    /**
     * Returns list of all folders inside a folder
     *
     * @param hsDirectory The main hidden service directory
     * @return The list of hidden service directories
     */
    public static File[] getFolders(File hsDirectory) {
        List<File> folders = new ArrayList<>();

        if (hsDirectory.exists() && hsDirectory.isDirectory()) {
            for (File f : hsDirectory.listFiles()) {
                if (f.isDirectory()) {
                    folders.add(f);
                }
            }
        }

        File[] finalFiles = new File[folders.size()];
        finalFiles = folders.toArray(finalFiles);
        return finalFiles;
    }

    public static HiddenService getHSFromFolder(List<HiddenService> services, File hsFolder) {
        for (HiddenService hs : services) {
            if (hs.getHiddenServiceDir().equalsIgnoreCase(hsFolder.getAbsolutePath())) {
                return hs;
            }
        }

        return null;
    }

    public static File getTorrc(Context cntx) {
        File torrcDir = cntx.getDir("torconfig", Context.MODE_PRIVATE);
        torrcDir.mkdirs();
        File fileWithinMyDir = new File(torrcDir, "torrc");

        return fileWithinMyDir;
    }
}
