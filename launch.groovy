import com.neuronrobotics.bowlerstudio.BowlerStudio
import com.neuronrobotics.bowlerstudio.creature.MobileBaseLoader
import com.neuronrobotics.bowlerstudio.physics.TransformFactory
import com.neuronrobotics.sdk.addons.gamepad.IGameControlEvent
import com.neuronrobotics.sdk.addons.gamepad.PersistantControllerMap
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.math.ITransformNRChangeListener
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
@Grab(group='org.hid4java', module='hid4java', version='0.7.0')

import com.neuronrobotics.sdk.common.DeviceManager

import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cube
import javafx.application.Platform
import javafx.scene.transform.Affine

import java.util.ArrayList;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

class HydraController{
	private int index=0;
	private byte [] buf=null;
	private byte [] lastUpdate=new byte[64];
	TransformNR pose = new TransformNR()
	Affine manipulator = new Affine()
	
	private double andalogx=0;
	private double andalogy=0;
	private double andalogtrig=0;
	int trigButton=0;
	int buttonHat=0;
	int buttonselect=0;
	int button1=0;
	int button2=0;
	int button3 =0;
	int button4=0;

	public HydraController(int index,byte [] buffer) {
		this.index=index;
		this.buf=buffer;
		if(buf==null)
			throw new NullPointerException()
	}
	public update() {
		boolean update=false;
		for(int i=0;i<lastUpdate.length && i<buf.length;i++) {
			if(lastUpdate[i]!=buf[i])
				update=true;
		}
		if(!update)
			return;
		//capture the update
		for(int i=0;i<lastUpdate.length;i++) {
			lastUpdate[i]=buf[i];
		}
		double posY = -getval16(8+(22*index))
		double posX = -getval16(10+(22*index))+500
		double posZ = -getval16(12+(22*index))

		double rotw=getval16(14+(22*index))/ 32768.0
		double rotx=getval16(16+(22*index))/ 32768.0
		double roty=getval16(18+(22*index))/ 32768.0
		double rotz=getval16(20+(22*index))/ 32768.0

		pose.setX(posX)
		pose.setY(posY)
		pose.setZ(posZ)
		def rotComp = new TransformNR(0,0,0, new RotationNR(180,-90,0))
				.times(new TransformNR(0,0,0, new RotationNR(rotw, rotx, roty, rotz)))
				.times(new TransformNR(0,0,0, new RotationNR(0,90,0)))

		pose.setRotation(rotComp.getRotation())
		Platform.runLater({TransformFactory.nrToAffine(pose, manipulator)})
		int buttonmask= buf[22+(22*index)]& 0x7f
		
		andalogy=getval16(23+(22*index))/ 32768.0
		andalogx=getval16(25+(22*index))/ 32768.0
		andalogtrig=buf[27+(22*index)]/255.0
		
		trigButton=(buttonmask & 0x01) ? 1 : 0;
		buttonHat=(buttonmask & 0x40) ? 1 : 0;
		buttonselect=(buttonmask & 0x20) ? 1 : 0;
		button1=(buttonmask & 0x02) ? 1 : 0;
		button2=(buttonmask & 0x10) ? 1 : 0;
		button3 =(buttonmask & 0x08) ? 1 : 0;
		button4=(buttonmask & 0x04) ? 1 : 0;
		
	}

	private double getval16(int index) {
		int top = buf[index+1]
		int bottom = buf[index]
		//		if(top<0)
		//			top+=256
		if(bottom<0)
			bottom+=256
		return (double)(((top ) << 8) |(bottom& 0xFF))
	}


	public disconnect() {

	}


}

class RazerHydra {
	private ArrayList<IGameControlEvent> listeners = new ArrayList<IGameControlEvent>();
	private ArrayList<ITransformNRChangeListener> listenersLeft=new ArrayList<ITransformNRChangeListener>();
	private ArrayList<ITransformNRChangeListener> listenersRight=new ArrayList<ITransformNRChangeListener>();

	private HashMap<String, Double> recentValue = new HashMap<String, Double>();
	private HidDevice hidDevice = null;
	private HidServices hidServices = null;
	private byte[] message=new byte[64];
	HydraController left= new HydraController(0,message)
	HydraController right= new HydraController(1,message)
	boolean polling =false;
	Thread controllerPoller=null;
	public String getName() {
		return "RazerHydra"
	}
	public void addChangeListenerLeft(ITransformNRChangeListener l) {
		if(!getListenersLeft().contains(l))
			getListenersLeft().add(l);
	}
	public void removeChangeListenerLeft(ITransformNRChangeListener l) {
		if(getListenersLeft().contains(l))
			getListenersLeft().remove(l);
	}
	public void clearChangeListenerLeft() {
		getListenersLeft().clear();
		listenersLeft=null;
	}
	public ArrayList<ITransformNRChangeListener> getListenersLeft() {
		if(listenersLeft==null)
			listenersLeft=new ArrayList<ITransformNRChangeListener>();
		return listenersLeft;
	}
	public void addChangeListenerRight(ITransformNRChangeListener l) {
		if(!getListenersRight().contains(l))
			getListenersRight().add(l);
	}
	public void removeChangeListenerRight(ITransformNRChangeListener l) {
		if(getListenersRight().contains(l))
			getListenersRight().remove(l);
	}
	public void clearChangeListenerRight() {
		getListenersRight().clear();
		listenersRight=null;
	}
	public ArrayList<ITransformNRChangeListener> getListenersRight() {
		if(listenersRight==null)
			listenersRight=new ArrayList<ITransformNRChangeListener>();
		return listenersRight;
	}
	public boolean connect() {
		if(polling)
			return true;
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
		if(hidDevice==null) {
			println "Device Missing!"
			return false
		}
		boolean open= hidDevice.open();
		if(open) {
			// Configure the device to stream positions
			byte[] buf=new byte[90];
			buf[5] = 1;
			buf[7] = 4;
			buf[8] = 3;
			buf[88] = 6;
			int res = hidDevice.sendFeatureReport(buf, (byte)0)
			//println "Result of IOCTL "+res
			// Ready to poll
			polling=true;
			controllerPoller=new Thread({
				println "Hydra connect"
				try {
					while(polling) {
						Thread.sleep(16)
						hidDevice.read(message, 20);
						left.update()
						right.update()
						for(int i=0;i<listenersLeft.size();i++) {
							try {listenersLeft.get(i).event(left.pose)} catch (Throwable ex) {
								listenersLeft.remove(i)
								BowlerStudio.printStackTrace(ex)
								break;
							}
						}
						for(int i=0;i<listenersRight.size();i++) {
							try {listenersRight.get(i).event(right.pose)} catch (Throwable ex) {
								listenersRight.remove(i)
								BowlerStudio.printStackTrace(ex)								
								break;
							}
						}
						sendValue(left.andalogtrig, "l-analog-trig")
						sendValue(left.andalogx, "l-joy-up-down")
						sendValue(left.andalogy, "l-joy-left-right")
						sendValue(left.trigButton,"l-trig-button")
						sendValue(left.buttonHat,"l-start")
						sendValue(left.buttonselect,"l-select")
						sendValue(left.button1,"l-x-mode")
						sendValue(left.button2,"l-y-mode")
						sendValue(left.button3,"l-a-mode")
						sendValue(left.button4,"l-b-mode")
						
						sendValue(right.andalogtrig, "r-analog-trig")
						sendValue(right.andalogx, "r-joy-up-down")
						sendValue(right.andalogy, "r-joy-left-right")
						sendValue(right.trigButton,"r-trig-button")
						sendValue(right.buttonHat,"r-start")
						sendValue(right.buttonselect,"r-select")
						sendValue(right.button1,"r-x-mode")
						sendValue(right.button2,"r-y-mode")
						sendValue(right.button3,"r-a-mode")
						sendValue(right.button4,"r-b-mode")
					}
				}catch(Throwable t) {
					BowlerStudio.printStackTrace(t)
				}
				println "Hydra disconnect"
//				// stop  the device to stream positions
//				buf=new byte[90];
//				buf[5] = 1;
//				buf[7] = 4;
//				buf[88] = 5;
//				hidDevice.sendFeatureReport(buf, (byte)0)
				if(hidDevice!=null)
					hidDevice.close();
				hidServices.shutdown();
				hidServices=null;
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
		left.disconnect()
		right.disconnect()
		listeners.clear();
		listenersLeft.clear();
		listenersRight.clear();
	}
	/**
	 * Removes the listeners.
	 *
	 * @param l the l
	 */
	public void removeListeners(IGameControlEvent l) {
		if (listeners.contains(l))
			this.listeners.remove(l);
	}

	/**
	 * Removes all the listeners.
	 *
	 */
	public void clearListeners() {
		this.listeners.clear();
	}

	/**
	 * Adds the listeners.
	 *
	 * @param l the l
	 */
	public void addListeners(IGameControlEvent l) {
		if (!listeners.contains(l))
			this.listeners.add(l);
	}

	private void sendValue(double value, String n) {
		n = PersistantControllerMap.getMappedAxisName(getName(), n);
		if (Math.abs(value) < 0.0001 && value != 0)
			value = 0;
		if(recentValue.get(n)==null)
			recentValue.put(n, (double) value);
		//Check to see that the new value is different from the last sent value
		if(Math.abs(Math.abs(value)-Math.abs(recentValue.get(n)))>0.001) {
			recentValue.put(n, (double) value);
			for (int i = 0; i < listeners.size(); i++) {
				IGameControlEvent l = listeners.get(i);
				try {
					l.onEvent(n, (float)value);
				} catch (Throwable ex) {
					BowlerStudio.printStackTrace(ex)
				}
			}
		}
	}
}
def hydra=DeviceManager.getSpecificDevice( "RazerHydra",{
	def hydra =new RazerHydra();
	hydra.connect();
	return hydra
})

CSG left= new Cube(30,5,20).toCSG().toXMin().toZMin().toYMin()
CSG right = new Cube(30,5,20).toCSG().toXMin().toZMin().toYMin()
left.setManipulator(hydra.left.manipulator)
right.setManipulator(hydra.right.manipulator)
hydra.addListeners(new IGameControlEvent(){
	public void onEvent(String n,float value) {
			println n+" "+value
	}
})
MobileBase base=DeviceManager.getSpecificDevice( "Standard6dof",{
			//If the device does not exist, prompt for the connection
			
			MobileBase m = MobileBaseLoader.fromGit(
				"https://github.com/Halloween2020TheChild/GroguMechanicsCad.git",
				"hephaestus.xml"
				)
			if(m==null)
				throw new RuntimeException("Arm failed to assemble itself")
			println "Connecting new device robot arm "+m
			return m
		})
println base
hydra.clearChangeListenerLeft()
hydra.addChangeListenerLeft(new ITransformNRChangeListener() {
	public  void event(TransformNR changed) {
		DHParameterKinematics arm = base.getAllDHChains().get(0)
		try {
			double[] jointSpaceVect = arm.inverseKinematics(arm.inverseOffset(changed));
			
			for (int i = 0; i < jointSpaceVect.length; i++) {
				AbstractLink link = arm.factory.getLink(arm.getLinkConfiguration(i));
				double val = link.toLinkUnits(jointSpaceVect[i]);
				Double double1 = new Double(val);
				if(double1.isNaN() ||double1.isInfinite() ) {
					jointSpaceVect[i]=0;
				}
				if (val > link.getUpperLimit()) {
					jointSpaceVect[i]=link.toEngineeringUnits(link.getUpperLimit());
				}
				if (val < link.getLowerLimit()) {
					jointSpaceVect[i]=link.toEngineeringUnits(link.getLowerLimit());
				}
			}
			arm.setDesiredJointSpaceVector(jointSpaceVect, 0);
		}catch(Throwable t) {
			//t.printStackTrace()
		}
		
	}
})
return [left, right];


