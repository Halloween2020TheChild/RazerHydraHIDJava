
import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase
import com.neuronrobotics.bowlerstudio.creature.MobileBaseLoader
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import com.neuronrobotics.sdk.addons.gamepad.BowlerJInputDevice
import com.neuronrobotics.sdk.addons.gamepad.IGameControlEvent
import com.neuronrobotics.sdk.addons.kinematics.AbstractLink
import com.neuronrobotics.sdk.addons.kinematics.DHParameterKinematics
import com.neuronrobotics.sdk.addons.kinematics.MobileBase
import com.neuronrobotics.sdk.addons.kinematics.math.RotationNR
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
import com.neuronrobotics.sdk.common.DeviceManager
import com.neuronrobotics.sdk.util.ThreadUtil
ScriptingEngine.pull("https://github.com/Halloween2020TheChild/GroguMechanicsCad.git")
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
		else if(name.contentEquals("l-trig-button")){
			if(value>0) {
				tilt=-1;	
			}else
				tilt=0;
		}
		else if(name.contentEquals("y-mode")){
			if(value>0) {
				
			}
		}
			//System.out.println(name+" is value= "+value);
		
	}
}

g.clearListeners()
Log.enableSystemPrint(true)
g.addListeners(listener);
long msAttempted = 30
long msActual=msAttempted

def fixVector(double[] jointSpaceVect,DHParameterKinematics arm ) {
	for (int i = 0; i < 6; i++) {
		AbstractLink link = arm.factory.getLink(arm.getLinkConfiguration(i));
		double val = jointSpaceVect[i];
		Double double1 = new Double(val);
		if(double1.isNaN() ||double1.isInfinite() ) {
			jointSpaceVect[i]=0;
		}
		if (val > link.getMaxEngineeringUnits()) {
			jointSpaceVect[i]=link.getMaxEngineeringUnits()-Double.MIN_VALUE;
			//println "Link "+i+" u-limit "+jointSpaceVect[i]
		}
		if (val < link.getMinEngineeringUnits()) {
			jointSpaceVect[i]=link.getMinEngineeringUnits()+Double.MIN_VALUE;
			//println "Link "+i+" l-limit "+jointSpaceVect[i]
		}
	}
}
try{
	while(!Thread.interrupted() ){

		TransformNR changed=new TransformNR()
		changed.setX(170+(x*30))

		
		def headRnage=15
		def analogy = -straif*70
		def analogz = -ljud*35
		changed.setZ(200+analogz)
		changed.setY(analogy)
		def analogup = -rz*headRnage *1.5
		
		changed.setRotation(new RotationNR(0,179.96+analogup,-50.79))
		TransformNR tilted= new TransformNR(0,0,0, RotationNR.getRotationZ(-90+tilt*-30))
		changed=changed.times(tilted)
		DHParameterKinematics arm = base.getAllDHChains().get(0)
		def trig=(trigAnalog*50)
		try {
			double[] jointSpaceVect = arm.inverseKinematics(arm.inverseOffset(changed));

			fixVector(jointSpaceVect,arm)
			
			double bestsecs = arm.getBestTime(jointSpaceVect);
			double normalsecs = ((double)msAttempted)/1000.0
			def vect;
			if(bestsecs>normalsecs) {
				double percentpossible = normalsecs/bestsecs

				TransformNR starttr=arm.getCurrentTaskSpaceTransform()
				TransformNR delta = starttr.inverse().times(changed);
				TransformNR scaled = delta.scale(percentpossible)
				TransformNR newTR= starttr.times(scaled)
				vect = arm.inverseKinematics(arm.inverseOffset(newTR));
				fixVector(vect,arm)
				TransformNR finaltr= arm.forwardOffset( arm.forwardKinematics(vect))
				if(!arm.checkTaskSpaceTransform(finaltr)) {
					println "\n\npercentage "+percentpossible
					println "Speed capped\t"+jointSpaceVect
					println "to\t\t\t"+vect
					println "changed"+changed
					println "starttr"+starttr
					println "delta"+delta
					println "scaled"+scaled
					println "newTR"+newTR
					println "ERROR, cant get to "+newTR
					continue;
				}
			}else
				vect = jointSpaceVect
			msActual=normalsecs*1000
			try {
				//vect[6]=trig;
			}catch(Throwable t) {
				//BowlerStudio.printStackTrace(t)
			}
			arm.setDesiredJointSpaceVector(vect, normalsecs);
		}catch(Throwable t) {
			t.printStackTrace(System.out)
			//arm.setDesiredJointAxisValue(6, trig, 0)
		}
		MobileBase head = arm.getDhLink(5).getSlaveMobileBase()
		//println head
		DHParameterKinematics mouth=head.getAllDHChains().get(0)
		//println mouth
		mouth.setDesiredJointAxisValue(0, trig, 0)
		Thread.sleep(msActual)
	}
}catch(Throwable t){
	t.printStackTrace(System.out)
}
//remove listener and exit
g.removeListeners(listener);
