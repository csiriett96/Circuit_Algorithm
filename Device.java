import java.io.*;
import java.util.*;

// Catherine Siriett
// 1254467
public class Device{
    private float reliability;
    private float cost;
    
    public Device(float r, float c)
    {
		reliability = r;
		cost = c;
    }
		// return reliability of device
		public float getReliability(){
			return reliability;		
		}
		
		// returns cost of device
		public float getCost(){
			return cost;		
		}

}
