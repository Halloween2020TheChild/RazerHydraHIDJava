import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase
import com.neuronrobotics.bowlerstudio.creature.MobileBaseLoader
import com.neuronrobotics.sdk.addons.gamepad.BowlerJInputDevice
import com.neuronrobotics.sdk.addons.gamepad.IGameControlEvent
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.common.DeviceManager
import com.neuronrobotics.sdk.util.ThreadUtil

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
List<String> gameControllerNames = ConfigurationDatabase.getObject("katapult", "gameControllerNames", ["Dragon","X-Box","Game"])

//Check if the device already exists in the device Manager
BowlerJInputDevice g=DeviceManager.getSpecificDevice("gamepad",{
	def t = new BowlerJInputDevice(gameControllerNames); //
	t.connect(); // Connect to it.
	return t
})

def x =0;

def straif=0;
def rz=0;
def ljud =0;

IGameControlEvent listener = new IGameControlEvent() {
	@Override public void onEvent(String name,float value) {
		
		if(name.contentEquals("l-joy-left-right")){
			straif=-value;
		}
		else if(name.contentEquals("r-joy-up-down")){
			x=-value;
		}
		else if(name.contentEquals("l-joy-up-down")){
			ljud=value;
		}
		else if(name.contentEquals("r-joy-left-right")){
			rz=value;
		}
		else if(name.contentEquals("x-mode")){
			if(value>0) {
				
			}
		}
		else if(name.contentEquals("y-mode")){
			if(value>0) {
				
			}
		}
			System.out.println(name+" is value= "+value);
		
	}
}

g.clearListeners()

g.addListeners(listener);

try{
	while(!Thread.interrupted() ){
		ThreadUtil.wait(30)
	}
}catch(Throwable t){
	t.printStackTrace()
}
//remove listener and exit
g.removeListeners(listener);
