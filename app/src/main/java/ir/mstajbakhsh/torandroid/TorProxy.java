package ir.mstajbakhsh.torandroid;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import org.torproject.android.binary.TorResourceInstaller;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.LogOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeoutException;

public class TorProxy {

    private Context cntx;
    private TorResourceInstaller torResourceInstaller;
    private TorStatus ts;
    private File fileTorBin = null;
    private File fileTorRc = null;
    private ProcessExecutor ps;
    private boolean debuggable = false;
    private File HSDir = null;
    private File TorrcDir = null;

    public enum TorStatus {NOT_INIT, OK, CONNECTING, DIED, FAILED_INSTLLATION, INSTALLED, TORRC_NOT_FOUND}

    public static int SOCKS_PORT = 9050;
    private int externalHiddenServicePort = -1;
    private int internalHiddenServicePort = -1;
    private boolean useBrideges = false;
    private TorBuilder builder;
    private List<HiddenService> services = null;

    private TorProxy(Context context, TorBuilder tb) {
        this.builder = tb;
        this.cntx = context;

        if (builder != null) {
            HSDir = cntx.getDir("HiddenService", Context.MODE_PRIVATE);
            TorrcDir = cntx.getDir("torconfig", Context.MODE_PRIVATE);
            torResourceInstaller = new TorResourceInstaller(getContext(), getContext().getFilesDir());
            ts = TorStatus.NOT_INIT;

            //Clear previous configs
            clearEverything();

            this.SOCKS_PORT = builder.SOCKS_PORT;
            this.services = builder.getServices();
            this.useBrideges = builder.useBrideges;
            this.debuggable = builder.debuggable;
        }
    }

    /**
     * ُ This constructor will try to load existed torrc file
     */
    public TorProxy(Context context, File torrc) {
        this.cntx = context;
        HSDir = cntx.getDir("HiddenService", Context.MODE_PRIVATE);
        TorrcDir = cntx.getDir("torconfig", Context.MODE_PRIVATE);
        torResourceInstaller = new TorResourceInstaller(getContext(), getContext().getFilesDir());
        ts = TorStatus.NOT_INIT;
        builder = TorUtils.parseTorrc(torrc);


        if (builder != null) {
            this.SOCKS_PORT = builder.SOCKS_PORT;
            this.services = builder.getServices();
            this.useBrideges = builder.useBrideges;
            this.debuggable = builder.debuggable;
        }
    }


    private Context getContext() {
        return cntx;
    }

    public boolean init() {
        try {
            fileTorBin = torResourceInstaller.installResources();
            fileTorRc = torResourceInstaller.getTorrcFile();

            boolean success = fileTorBin != null && fileTorBin.canExecute();

            if (success) {
                ts = TorStatus.INSTALLED;
                return true;
            } else {
                ts = TorStatus.FAILED_INSTLLATION;
                return false;
            }
        } catch (Exception e) {
            ts = TorStatus.DIED;
            return false;
        }
    }

    public String getOnionAddress() {
        if (ts != TorStatus.OK) {
            if (debuggable) {
                Log.e("MSTTOR", "Tor is not OK. Run it first or check other errors");
            }
            return "Tor status is no OK.";
        }

        File hostnameFolder = cntx.getDir("HiddenService", Context.MODE_PRIVATE);
        File hostname = new File(hostnameFolder, "hostname");

        if (!hostname.exists()) {
            if (debuggable) {
                Log.e("MSTTOR", "HiddenService did not created. Check for errors.");
            }
            return "HiddenService did not created. Check for errors.";
        }

        try {
            FileInputStream fis = new FileInputStream(hostname);
            byte[] data = new byte[(int) hostname.length()];
            fis.read(data);
            fis.close();

            String hiddenName = new String(data, "UTF-8");

            return hiddenName;
        } catch (Exception ex) {
            if (debuggable) {
                Log.e("MSTTOR", "Error in reading file.\r\n" + ex.getMessage());
            }
            return "";
        }

    }

    /**
     * This method will remove any preconfigured hidden services! Use with caution, this can not be undo!
     *
     * @param cntx Context instance to get the local private storage directory of the application.
     * @return True if all the hidden services are deleted successfully, False otherwise.
     */
    public static boolean removeAllHiddenServices(Context cntx) {
        File HiddenServiceDir = cntx.getDir("HiddenService", Context.MODE_PRIVATE);
        try {
            new ProcessExecutor().command("rm", "-rf", HiddenServiceDir.getAbsolutePath()).execute();
            return true;
        } catch (Exception e) {
            Log.e("MSTTOR", "Error in removing hidden services!\r\n" + e.getMessage());
            return false;
        }
    }

    private File createTorrc(Vector<String> options) {
        try {

            File torrcDir = cntx.getDir("torconfig", Context.MODE_PRIVATE);
            File fileWithinMyDir = new File(torrcDir, "torrc");
            FileOutputStream out = new FileOutputStream(fileWithinMyDir);
            for (String line : options) {
                out.write((line + "\r\n").getBytes());
            }
            out.flush();
            out.close();

            return fileWithinMyDir;
        } catch (IOException ex) {
            if (debuggable) {
                Log.e("MSTTOR", ex.getMessage());
            }
            return null;
        }
    }


    public TorStatus getStatus() {
        return ts;
    }

    public boolean start(final IConnectionDone connectionDone) throws IOException {
        File appCacheHome = cntx.getDir(SampleTorServiceConstants.DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);

        Vector<String> command = new Vector<>();
        command.add("DataDirectory " + appCacheHome.getCanonicalPath());
        command.add("SocksPort " + SOCKS_PORT);

        for (HiddenService hs : services) {
            if (hs.getHiddenServiceDir().equalsIgnoreCase("")) { //Create HSDir if not created!
                File newHSDir;

                int availableHSFolderNumber = 1;
                while (true) {
                    newHSDir = new File(HSDir.getAbsolutePath() + File.separator + "HS" + availableHSFolderNumber);
                    if (!newHSDir.exists()) {
                        newHSDir.mkdirs();
                        break;
                    }
                    availableHSFolderNumber++;
                }

                hs.setHiddenServiceDir(newHSDir.getAbsolutePath());
            }

            command.add(hs.toString());
        }

        if (useBrideges) {
            command.add("UseBridges 1"); //User may pass bridges as extra lines!
        }


        //Folder is too permissive
        try {
            //Make the main folder 700
            new ProcessExecutor("chmod", "700", HSDir.getCanonicalPath()).execute();

            //Make each HS folder 700
            for (HiddenService hsDir : services) {
                new ProcessExecutor("chmod", "700", hsDir.getHiddenServiceDir()).execute();
            }

        } catch (InterruptedException | TimeoutException e) {
            if (debuggable) {
                Log.e("MSTTOR", e.getMessage());
            }
        }


        if (!fileTorRc.exists()) {
            ts = TorStatus.TORRC_NOT_FOUND;
            return false;
        }

        //Start Tor
        ps = new ProcessExecutor();

        File torrc = createTorrc(command);
        try {
            ps.command(fileTorBin.getCanonicalPath(), "-f", torrc.getCanonicalPath())
                    .redirectOutput(new LogOutputStream() {
                        @Override
                        protected void processLine(String line) {
                            if (debuggable) {
                                Log.d("MSTTOR", line);
                            }
                            if (line.contains(SampleTorServiceConstants.TOR_CONTROL_PORT_MSG_BOOTSTRAP_DONE)) {
                                ts = TorStatus.OK;
                                //Fill Hidden Service Addresses
                                getOnionAddresses();
                                connectionDone.onSuccess();
                                return;
                            }
                        }
                    })
                    .execute();
        } catch (InterruptedException |
                TimeoutException e) {
            if (debuggable) {
                Log.e("MSTTOR", e.getMessage());
            }
            connectionDone.onFailure(e);
            return false;
        }
        return true;
    }

    private void clearEverything() {
        try {
            removeAllHiddenServices(cntx);
            new ProcessExecutor("rm", "-rf", TorrcDir.getCanonicalPath()).execute();

        } catch (InterruptedException | TimeoutException | IOException e) {
            if (debuggable) {
                Log.e("MSTTOR", e.getMessage());
            }
        }
    }

    public String[] getOnionAddresses() {
        List<String> hsAddresses = new ArrayList<>();

        if (ts != TorStatus.OK) {
            if (debuggable) {
                Log.e("MSTTOR", "Tor is not OK. Run it first or check other errors");
            }
            return new String[]{"Tor status is no OK."};
        }

        File hiddenServicesFolder = cntx.getDir("HiddenService", Context.MODE_PRIVATE);
        File hostname;

        for (File hsDir : TorUtils.getFolders(hiddenServicesFolder)) {

            HiddenService hs = TorUtils.getHSFromFolder(services, hsDir);

            hostname = new File(hsDir, "hostname");

            if (!hostname.exists()) {
                if (debuggable) {
                    Log.e("MSTTOR", "HiddenService [" + hsDir.getAbsolutePath() + "] did not created. Check for errors.");
                }
            } else {
                try {
                    FileInputStream fis = new FileInputStream(hostname);
                    byte[] data = new byte[(int) hostname.length()];
                    fis.read(data);
                    fis.close();

                    String hiddenName = new String(data, "UTF-8");

                    hiddenName = hiddenName.replaceAll("\n", "");

                    hsAddresses.add(hiddenName);
                    hs.setHiddenAddress(hiddenName);
                } catch (Exception ex) {
                    if (debuggable) {
                        Log.e("MSTTOR", "Error in reading file [" + hostname.getAbsolutePath() + "].\r\n" + ex.getMessage());
                    }
                }
            }
        }

        String[] result = new String[hsAddresses.size()];
        result = hsAddresses.toArray(result);
        return result;
    }

    public List<HiddenService> getHiddenServices() {
        return services;
    }

    public static class TorBuilder {
        private int SOCKS_PORT = -1;
        List<HiddenService> services = new ArrayList<>();
        private String extraLines = "";
        private boolean useBrideges = false;
        private boolean debuggable = false;

        public TorBuilder() {
        }

        public TorBuilder setSOCKsPort(int SOCKS_PORT) {
            this.SOCKS_PORT = SOCKS_PORT;
            return this;
        }

        public List<HiddenService> getServices() {
            return services;
        }

        public TorBuilder setUseBrideges(boolean useBrideges) {
            this.useBrideges = useBrideges;
            return this;
        }

        public TorBuilder setExtra(String extraLines) {
            this.extraLines = extraLines;
            return this;
        }

        public TorBuilder setDebuggable(Boolean debuggable) {
            this.debuggable = debuggable;
            return this;
        }

        public TorProxy build(Context cntx) {
            return new TorProxy(cntx, this);
        }
    }
}