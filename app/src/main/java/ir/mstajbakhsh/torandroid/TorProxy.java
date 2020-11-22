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

    public enum TorStatus {NOT_INIT, OK, CONNECTING, DIED, FAILED_INSTLLATION, INSTALLED, TORRC_NOT_FOUND}

    public static int SOCKS_PORT = 9050;
    private int externalHiddenServicePort = -1;
    private int internalHiddenServicePort = -1;
    private boolean useBrideges = false;

    public TorProxy(TorBuilder builder) {
        this.cntx = builder.cntx;
        torResourceInstaller = new TorResourceInstaller(getContext(), getContext().getFilesDir());
        ts = TorStatus.NOT_INIT;

        if (builder != null) {
            this.SOCKS_PORT = builder.SOCKS_PORT;
            this.externalHiddenServicePort = builder.externalHiddenServicePort;
            this.internalHiddenServicePort = builder.internalHiddenServicePort;
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

    public boolean start(final IConnectionDone connectionDone) throws IOException {
        File appCacheHome = cntx.getDir(SampleTorServiceConstants.DIRECTORY_TOR_DATA, Application.MODE_PRIVATE);
        File HiddenServiceDir = null;

        Vector<String> command = new Vector<>();
        command.add("DataDirectory " + appCacheHome.getCanonicalPath());
        command.add("SocksPort " + SOCKS_PORT);

        if (this.internalHiddenServicePort != -1 && this.externalHiddenServicePort != -1) {
            HiddenServiceDir = cntx.getDir("HiddenService", Context.MODE_PRIVATE);
            command.add("HiddenServiceDir " + HiddenServiceDir.getCanonicalPath());
            command.add("HiddenServicePort " + externalHiddenServicePort + " 127.0.0.1:" + internalHiddenServicePort);

            //Folder is too permissive
            try {
                new ProcessExecutor("chmod" , "700", HiddenServiceDir.getCanonicalPath()).execute();
            } catch (InterruptedException | TimeoutException e) {
                if (debuggable) {
                    Log.e("MSTTOR", e.getMessage());
                }
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
                                connectionDone.onSuccess();
                                return;
                            }
                        }
                    })
                    .execute();
        } catch (InterruptedException | TimeoutException e) {
            if (debuggable) {
                Log.e("MSTTOR", e.getMessage());
            }
            connectionDone.onFailure(e);
            return false;
        }
        return true;
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

    public static class TorBuilder {
        // optional parameters
        private int SOCKS_PORT = -1;
        private int externalHiddenServicePort = -1;
        private int internalHiddenServicePort = -1;
        private String extraLines = "";
        private boolean useBrideges = false;
        private Context cntx;
        private boolean debuggable = false;

        public TorBuilder(Context cntx) {
            this.cntx = cntx;
        }

        public TorBuilder setSOCKsPort(int SOCKS_PORT) {
            this.SOCKS_PORT = SOCKS_PORT;
            return this;
        }

        public TorBuilder setExternalHiddenServicePort(int externalHiddenServicePort) {
            this.externalHiddenServicePort = externalHiddenServicePort;
            return this;
        }

        public TorBuilder setInternalHiddenServicePort(int internalHiddenServicePort) {
            this.internalHiddenServicePort = internalHiddenServicePort;
            return this;
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

        public TorProxy build() {
            return new TorProxy(this);
        }

    }
}
