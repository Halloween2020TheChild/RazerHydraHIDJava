
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase
import com.neuronrobotics.bowlerstudio.creature.MobileBaseLoader
import com.neuronrobotics.sdk.addons.gamepad.BowlerJInputDevice
import com.neuronrobotics.sdk.addons.gamepad.IGameControlEvent
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
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
ConfigurationDatabase.setObject("katapult", "gameControllerNames", ["Dragon","X-Box","Game", "Switch"])
List<String> gameControllerNames = ConfigurationDatabase.getObject("katapult", "gameControllerNames", ["Dragon","X-Box","Game", "Switch"])

//Check if the device already exists in the device Manager
BowlerJInputDevice g=DeviceManager.getSpecificDevice("gamepad",{
	def t = new BowlerJInputDevice(gameControllerNames); //
	t.connect(); // Connect to it.
	return t
})

float x =0;

float straif=0;
float rz=0;
float ljud =0;
float trigButton=0;
float trigAnalog=0;
float tilt=0;

IGameControlEvent listener = new IGameControlEvent() {
	@Override public void onEvent(String name,float value) {
		
		if(name.contentEquals("l-joy-left-right")){
			straif=value;
		}
		else if(name.contentEquals("r-joy-up-down")){
			x=-value;
		}
		else if(name.contentEquals("l-joy-up-down")){
			ljud=value;
		}
		else if(name.contentEquals("r-joy-left-right")){
			rz=value;
		}else if(name.contentEquals("analog-trig")){
			trigAnalog=value/2.0+0.5;
		}else if(name.contentEquals("z")){
			trigButton=value/2.0+0.5;
		}
		else if(name.contentEquals("x-mode")){
			if(value>0) {
				
			}
		}else if(name.contentEquals("r-trig-button")){
			if(value>0) {
				tilt=1;	
			}else
				tilt=0;
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
		TransformNR changed=new TransformNR()
		changed.setX(190)

		
		def headRnage=20
		def analogy = -straif*80
		def analogz = -ljud*65+90
		changed.setZ(100+analogz)
		changed.setY(analogy)
		def analogside = -x*headRnage
		def analogup = -rz*headRnage *1.5
		
		changed.setRotation(new RotationNR(0,179.96+analogup,-57.79+analogside))
		TransformNR tilted= new TransformNR(0,0,0, RotationNR.getRotationZ(tilt*-30))
		changed=changed.times(tilted)
		DHParameterKinematics arm = base.getAllDHChains().get(0)
		def trig=(trigAnalog*50)
		try {
			double[] jointSpaceVect = arm.inverseKinematics(arm.inverseOffset(changed));
			try {
				jointSpaceVect[6]=trig;
			}catch(Throwable t) {
				//BowlerStudio.printStackTrace(t)
			}
			for (int i = 0; i < 6; i++) {
				AbstractLink link = arm.factory.getLink(arm.getLinkConfiguration(i));
				double val = link.toLinkUnits(jointSpaceVect[i]);
				Double double1 = new Double(val);
				if(double1.isNaN() ||double1.isInfinite() ) {
					jointSpaceVect[i]=0;
				}
				if (val > link.getUpperLimit()) {
					jointSpaceVect[i]=link.toEngineeringUnits(link.getUpperLimit());
					println "Link "+i+" u-limit "+jointSpaceVect[i]
				}
				if (val < link.getLowerLimit()) {
					jointSpaceVect[i]=link.toEngineeringUnits(link.getLowerLimit());
					println "Link "+i+" l-limit "+jointSpaceVect[i]
				}
			}
			
			
			arm.setDesiredJointSpaceVector(jointSpaceVect, 0);
		}catch(Throwable t) {
			arm.setDesiredJointAxisValue(6, trig, 0)
		}
	}
}catch(Throwable t){
	t.printStackTrace()
}
//remove listener and exit
g.removeListeners(listener);
