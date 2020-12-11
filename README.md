![Credits from: https://android.gadgethacks.com/how-to/tor-for-android-stay-anonymous-your-phone-0163360/](Extras/TorAndroid.jpg)

# Tor Android

YATLA (**Y**et **A**nother **T**or **L**ibrary for **A**ndroid) Version 2 

Tor Android is a library which included Tor binary inside it. Tor Binary is grabbed from [Guardian Project's maven](https://github.com/guardianproject/gpmaven/blob/master/org/torproject/tor-android-binary/). Some helper libraries added in order to make life easier. In the current version, 2 main features added including support for multiple hidden services, and ability to load former torrc file.



## Changelog

* Added Support for multiple Hidden Services
* Able to create a new torrc or load existed one

## How To Import

It is very simple. Just implement the following package in your application Gradle file:

```shell
implementation 'ir.mstajbakhsh:tor-android:2.0.0'

# Or Use JitPack

allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

dependencies {
	        implementation 'com.github.mirsamantajbakhsh:TorAndroid:2.0.0'
	}
```

## Code Example

### Creation Mode

In creation mode, every previous configuration will be overwritten. Any previous Hidden Services (private keys) will be lost. This action can not be undone. It is recommended to use *creation mode* for first time.

```java
HiddenService hs1 = new HiddenService();
hs1.addHiddenServiceMapping(8080, new Address("127.0.0.1", 8080));
hs1.addHiddenServiceMapping(8123, new Address("127.0.0.1", 8123));

HiddenService hs2 = new HiddenService();
hs2.addHiddenServiceMapping(80, new Address("192.168.1.24", 80));

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

			for(Map.Entry<Integer, Address> entry : hs.getHiddenServiceMapping().entrySet()) {
				int key = entry.getKey();
				Address value = entry.getValue();

				addresses += "\t" + key + " -> " + value.toString() + "\r\n";
			}
		}
	Log.d("MSTTOR", "Hidden Addresses: " + addresses);
}

	@Override
	public void onFailure(Exception ex) {

	}
});
} catch (IOException ex) {

}
```

Sample output of the above code is here:

```sh
2020-12-11 22:00:19.615 5390-5430/ir.mstajbakhsh.torandroid.test D/MSTTOR:
Hidden Addresses:
	l666a7nafbfw2csklnsrigmvyj4nqvitdtqy6kyrcuncwrktqfs3h6qd.onion:
    	8080 -> 127.0.0.1:8080
    	8123 -> 127.0.0.1:8123
    cvv6sb22aseo2sgkv6iixpnkriplerjgnyhpsubonbkpspxpxnh6xlid.onion:
    	80 -> 192.168.1.24:80
```

### Reload Mode

In this mode, TOR will be reloaded with previously configured settings. Use the following code to achieve:

```java
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
```

In this case, the previously configured torrc file (located in **torconfig** directory of private app storage), will be loaded.

# Donate

If you liked the project, buy me a cup of coffee:

BitCoin Wallet: ```1F5uiEmdCLJX5KktWHE1wkc63feKJYMmxS```

# Contact

You can reach me at my web site available at: https://mstajbakhsh.ir

