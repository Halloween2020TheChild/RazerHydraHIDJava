@Grab(group='org.hid4java', module='hid4java', version='0.5.0')

import com.neuronrobotics.sdk.common.DeviceManager
import java.util.ArrayList;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

class HydraController{
	private int index=0;
	private byte [] buffer=null;
	public HydraController(int index,byte [] buffer) {
		this.index=index;
		this.buffer=buffer;
	}
	
}

class RazerHydra {
	private HidDevice hidDevice = null;
	private HidServices hidServices = null;
	private byte[] message=new byte[64];
	HydraController left= new HydraController(0,message)
	HydraController right= new HydraController(1,message)
	boolean polling =false;
	Thread controllerPoller=null;
	public String getName() {
		return "RazerHydraProvider"
	}
	public boolean connect() {
		if(polling)
			return;
		println "Hydra connect"
		hidServices=HidManager.getHidServices();
		for (HidDevice h : hidServices.getAttachedHidDevices()) {
			if (h.isVidPidSerial(0x1532, 0x0300, null)) {
				System.out.println("Found! " + h.getInterfaceNumber() + " " + h);
				if ( !h.isOpen() ) {
					hidDevice = h;
					break;
				}

			}
		}
		boolean open= hidDevice.open();
		if(open) {
			polling=true;
			controllerPoller=new Thread({
				while(polling) {
					Thread.sleep(20)
					
				}
				println "Hydra disconnect"
				if(hidDevice!=null)
					hidDevice.close();
				hidServices.shutdown();
			})
			controllerPoller.start()
		}else {
			println "FAILED to open device!"
			hidServices.shutdown();
			hidServices=null;
		}
		return open;
	}
	public void disconnect() {

		polling=false;
	}
}

DeviceManager.getSpecificDevice( "RazerHydraProvider",{
	def hydra =new RazerHydra();
	hydra.connect();
	return hydra
})

return null;