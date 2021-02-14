package ir.mstajbakhsh.torandroid;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ir.mstajbakhsh.torandroid.Connection.Address;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        //assertEquals("ir.mstajbakhsh.torandroid", appContext.getPackageName());
        //startFromZero(appContext);
        loadPreviousConfig(appContext);
    }

    private void loadPreviousConfig(Context appContext) {
        TorProxy tp = new TorProxy(appContext, TorUtils.getTorrc(appContext));
        tp.init();
        try {
            tp.start(new IConnectionDone() {
                @Override
                public void onSuccess() {
                    Log.d("MSTTOR", "Everything is OK!");
                }

                @Override
                public void onFailure(Exception ex) {
                    Log.d("MSTTOR", "Error!");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startFromZero(Context appContext) {
        HiddenService hs1 = new HiddenService();
        hs1.addHiddenServiceMapping(8080, new Address("192.168.1.2", 8080));
        hs1.addHiddenServiceMapping(8123, new Address("192.168.1.3", 8123));

        HiddenService hs2 = new HiddenService();
        hs2.addHiddenServiceMapping(80, new Address("127.0.0.1", 80));

        List<HiddenService> hss = new ArrayList<>();
        hss.add(hs1);
        hss.add(hs2);

        TorProxy.TorBuilder tb = new TorProxy.TorBuilder()
                .setDebuggable(true)
                .setSOCKsPort(9050)
                .setServices(hss);

        final TorProxy tp = tb.build(appContext);

        tp.init();
        try {
            tp.start(new IConnectionDone() {
                @Override
                public void onSuccess() {
                    Log.d("MSTTOR", "Everything is OK!");
                    String addresses = "";

                    for (HiddenService hs : tp.getHiddenServices()) {
                        addresses += hs.getHiddenServiceAddress() + ":\r\n";

                        for (Map.Entry<Integer, Address> entry : hs.getHiddenServiceMapping().entrySet()) {
                            int key = entry.getKey();
                            Address value = entry.getValue();

                            addresses += "\t" + key + " -> " + value.toString() + "\r\n";
                        }
                    }
                    Log.d("MSTTOR", "Hidden Addresses: " + addresses);

                    Address a = new Address(tp.getHiddenServices().get(0).getHiddenServiceAddress(), 8080);
                    transparentSocketExample(a, tp.getHiddenServices().get(0).getHiddenServiceAddress());
                }

                @Override
                public void onFailure(Exception ex) {

                }
            });
        } catch (IOException ex) {

        }
    }

    private void transparentSocketExample(Address destination, String hiddenName) {
        try {
            Log.d("MSTTOR", "Starting HSConnect using transparent socket!");
            Socket s = new Socket(destination.getHost(), destination.getPort());
            s.getOutputStream().write(("GET / HTTP/1.0\r\nHOST: " + hiddenName + "\r\n\r\n").getBytes());
            s.getOutputStream().flush();
            BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String tmp = br.readLine();
            String html = "";

            while (tmp != null && !tmp.equalsIgnoreCase("")) {
                html += tmp + "\r\n";
                tmp = br.readLine();
            }

            Log.d("MSTTOR", "Response from facebook:\r\n" + html);

        } catch (Exception ex) {
            Log.e("MSTTOR", ex.getMessage());
        }
    }
}