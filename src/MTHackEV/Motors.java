import lejos.hardware.lcd.LCD;
import lejos.hardware.port.MotorPort;
import lejos.hardware.port.TachoMotorPort;
import lejos.utility.Delay;

// yks tapa, forward menee eteepäi ja turn vaikuttaa forwardii vaa
// kattoo arvon mukaa ja nopeuttaa vauhtia jos arvo pysyy tasasesti alhasena

public class Motors extends Thread{
	
	double white = 0.6, black = 0.06;	
	
	private DataExchange DEObj;
	TachoMotorPort leftMotor = MotorPort.C.open(TachoMotorPort.class);
	TachoMotorPort rightMotor = MotorPort.B.open(TachoMotorPort.class);
	
	TachoMotorPort middleMotor = MotorPort.A.open(TachoMotorPort.class);
		
	public Motors(DataExchange DE){
		DEObj = DE;
	}
	
	public void SetBlack(){
		black = DEObj.getColor();
	}
	
	public void SetWhite(){
		white = DEObj.getColor();
		}
	
	public void Turn(int degrees){
		
		if(degrees>0){
			Forward(50,0);
		}
		else{
			Forward(0,50);
		}
		
		Delay.msDelay(degrees);
	}
	
	public String GetColor(){
		
		float[] RGB = new float[3]; //Red = 0, Green = 1, Blue = 2
		
		//get each color
		RGB[0] = DEObj.GetRed();
		RGB[1] = DEObj.GetGreen();
		RGB[2] = DEObj.GetBlue();
		
		if(RGB[0]*0.8 > RGB[1] && RGB[0]*0.8 > RGB[2]){ //if red color is the biggest
			return "red";
		}
		else if(RGB[1]*0.8 > RGB[0] && RGB[1]*0.8 > RGB[2]){ //if green color is the biggest
			return "green";
		}
		else if(RGB[2] > RGB[0] && RGB[2] > RGB[1]){ //if blue is biggest
			return "blue";
		}
		
		return "none";
		
	}

	public void MotorInit(){
		
		rightMotor.controlMotor(0, 0);
		rightMotor.resetTachoCount();
		
		leftMotor.controlMotor(0, 0); 
		leftMotor.resetTachoCount();
		
		middleMotor.controlMotor(0, 0);
		middleMotor.resetTachoCount();
				
	}
	
	private void Forward(int left, int right){
		rightMotor.controlMotor(right, 1);
		leftMotor.controlMotor(left, 1);
	}
	
	
	public void run(){
		
		// must 0.06 , valk = 0.6
		
		double correction=0,value = 0,kp = 1.1; //kp affects the turning when following the line
		double midpoint = (white - black ) / 2 + black;
		DEObj.SetMiddleColor(midpoint);
		int right=0,left=0,forward=38, turn=0, stage=1, rightTurns = 0, straight = 0;
		int time=0;
		
		DEObj.ResetTime(); //to make sure that there is no time counted
		
		while(true){
			
			value = DEObj.getColor(); //get the color value
			
			switch(stage){
			
				//will follow the line and continue trying to find it unless little while has passed on white.
				case 1:
					//this moves code to stage 2 if only white is detected for a while
					if(DEObj.GetTime() > 5000 && DEObj.GetFollow()){ 
						stage=2;
						break;
					}
					
					//calculations for the turn is calculated here
					correction = kp * ( midpoint - value);
					turn = (int)(correction*100);
					if(turn < -5){
						turn = -5;
					}
					left = forward - turn;
					right = forward + turn;
					break;
					
					// will search for non-white line
				case 2:
					//if no longer on only white, will go to stage 3
					if(value < (DEObj.GetMiddle() * 1.2)){
						stage = 3;
						break;
					}
					
					//starts turning to left
					value = midpoint*0.7;
					
					//calculations for the turn is calculated here
					correction = kp * ( midpoint - value);
					turn = (int)(correction*100);
					turn = 10;
					left = forward + (turn-10);
					right = forward + turn;
					
					break;
					
					// stops and turns left in order go to the right direction
				case 3:
					
					left = 0;
					right = 45;
					time++;
					
					if(value < DEObj.GetMiddle() * 1.1 && time > 50){
						stage = 4;
					}
					
					break;
				
				//will try to find the colors.
				case 4:
					String color = GetColor();
					
					if(color=="red" || color=="blue"){
						
						if(rightTurns<1){
							left = 50;
							right = 20;
							Forward(left,right);
							Delay.msDelay(1000);
							rightTurns++;
							time=0;
						}
						else if(straight >= 2){ 
							stage = 5;
							break;
						}
						else{
							left = 42;
							right = 50;
							Forward(left,right);
							Delay.msDelay(1000);
							straight++;
						}
					}
					
					//calculations for the turn is calculated here
					correction = (kp-0.1) * ( midpoint - value);
					turn = (int)(correction*100);
					if(DEObj.GetFollow()){
						left = (forward - turn);
						right = (forward + turn);
					}
					else{
						left = (forward + turn);
						right = (forward - turn);
					}
					
					break;
					
				case 5:
					left=40;
					right=35;
					time++;

					if(value < DEObj.GetMiddle() * 1.1 && time > 1000){

						forward = 50;
						midpoint = midpoint + 0.11;
						stage = 6;
					}
					
					break;
					
				case 6:
					
					correction = kp * ( midpoint - value);
					turn = (int)(correction*100);
					left = forward - turn;
					right = forward + turn;
					break;
			}
			
			//makes the robot move
			Forward(left,right);

			
			//if button is pressed, this will stop the loop.
			if(DEObj.getStop()){
				break;
			}
			
		}
		
	}

}
