import java.io.*;
import java.util.*;
import java.lang.Math.*;
	
// Catherine Siriett
// 1254467
public class Solve
{
	private static float budget = 0;
	private static float initalTemperature = 0;
	
	public static void main(String[] args){
		String filename = "";
		int limit = 0;
		try
		{
			if(args.length != 3){
				System.err.println("Incorrect number of parameters");
				System.err.println("Usage: java Solve <filename> <budget> <limit>");
				System.exit(-1); // quit		
			} else {
				filename = args[0];
				File f = new File(filename);
				if(!f.exists())
				{
					//file must exist
					System.err.println("The filename that was supplied doesn't not exist");
					System.err.println("Usage: java Solve <filename> <budget> <limit>");
					System.exit(-1); // quit
				} else {
					budget = Float.parseFloat(args[1]);
					limit = Integer.parseInt(args[2]);
					if (budget < 0 || limit < 0)
					{
						// can't have negative cost or limit
						System.err.println("Budget and Limit must be positive numbers");
						System.err.println("Usage: java Solve <filename> <budget> <limit>");
						System.exit(-1); // quit			
					}				
				} 
			}					
		}catch(NumberFormatException e){
			System.err.println("Limit must be an integer");
			System.err.println("Usage: java Solve <filename> <budget> <limit>");
			System.exit(-1); // quit			
		}catch(Exception e)
		{
			System.err.println("Error: " + e);
			System.err.println("Usage: java Solve <filename> <budget> <limit>");
		}
		try{

		ArrayList<Device> devices = new ArrayList<Device>();  		//Arraylist of devices in circuit
		ArrayList<Integer> stageAmount = new ArrayList<Integer>();	//ArrayList of current number of each given device in circuit
		ArrayList<Integer> neighbourSol;							//ArrayList of neighouring solution number of each device in circuit
		BufferedReader br = new BufferedReader(new FileReader(filename));
		float prevRel = 0;		//Reliability of best solution so far
		float nextRel = 0;		//Reliability of new solution
		float currCost = 0;		//Current cost of best solution
		String line = "";
		String[] lineSplit;
		
		while((line = br.readLine()) != null)
		{
			// add the reliability and cost into corresponding arraylists
			lineSplit = line.split(" ");
			Device dev = new Device(Float.parseFloat(lineSplit[0]), Float.parseFloat(lineSplit[1]));
			devices.add(dev);
			stageAmount.add(1);
		}
		
		currCost = calcCost(stageAmount, devices);
		//Check that when there is one device for each stage we don't exceed the budget
		//if we do exit program and inform user
		if (currCost > budget){
			System.err.println("Budget is too small");
			System.exit(-1); // quit
		}

		float temperature = budget;						//Initialising temperature
		initalTemperature = temperature;				//Saving backup value of initalTemperature
		float coolingRate = initalTemperature/limit;	//Cooling Rate of temperature
		
		//Randomly generate the first solution and calculate the cost and reliability of this solution
		stageAmount = GenerateSolution(stageAmount,devices,currCost);
		currCost = calcCost(stageAmount, devices);
		prevRel = calcReliabilty(stageAmount, devices);

		//Loop until we have reached our max number of iterations
		while(limit > 0){
			//get a neighouring solution and calculate its reliability
			neighbourSol = calcNeighbour(stageAmount, devices,temperature);
			nextRel = calcReliabilty(neighbourSol,devices);
			//calculate probability of accepting this new solution
			float acceptanceProb = acceptanceProb(prevRel,nextRel,temperature);	
			//if probability is greater than a random number between 1 and 0 then accept it
			if(acceptanceProb > Math.random()){
				//the new solution is now the best solution
				prevRel = nextRel;
				stageAmount = neighbourSol;
				currCost = calcCost(stageAmount, devices);
			}
			//decrement limit and cool the temperature
			limit--;
			temperature -= coolingRate;
		}
		//Printing out solution
		Print(stageAmount, devices);
		System.out.println("Total Cost: "+ String.format("%.2f", currCost) + " Overall Reliability: "+ prevRel);

		}catch(IOException e){
			System.err.println(e);
		}
	}
	
	//Method used to calculate a neighbour solution
	public static ArrayList<Integer> calcNeighbour(ArrayList<Integer> stageAmount, ArrayList<Device> devices, float temperature){
		ArrayList<Integer> newSolution = new ArrayList<Integer>(stageAmount);
		//Remove a random number of items from our current solution
		newSolution = RemoveItems(newSolution,devices,temperature);
		//Calculate the new solution cost
		float currCost = calcCost(newSolution, devices);
		//Refill our solution with more devices now that we have removed some to get a neighouring solution
		newSolution = GenerateSolution(newSolution,devices,currCost);
		return newSolution;
	}	
	
	//Method used to remove random number of items from our current solution
	public static ArrayList<Integer> RemoveItems(ArrayList<Integer> stageAmount, ArrayList<Device> devices, float temperature){
		ArrayList<Integer> modifedStages = new ArrayList<Integer>(stageAmount);
		ArrayList<Integer> selection = new ArrayList<Integer>(); // list of the devices that you can afford to remove
		float removedCost = 0f;
		// add every device index to the available devices to choose from
		for(int i = 0; i < stageAmount.size(); i++){
			selection.add(i);
		}
		//While the temperature is greater than the amount we have removed loop
		while(temperature > removedCost){
			//Randomly select an index in from the devices we can remove
			int index = getRandom(selection.size()-1,0);
			//Select corresponding index from selection array
			int val = selection.get(index);
			//if this stage only has 1 device then leave skip it
			if(modifedStages.get(val) > 1){
				//else get the device at this index and find its cost
				Device dev = devices.get(val);
				float cost = removedCost + dev.getCost();
				//if its cost plus the current removed cost is still less than temperature remove it
				if(cost < temperature){
					//removing one of these devices from its stage and increasing removedcost
					modifedStages.set(val, modifedStages.get(val)-1);
					removedCost = removedCost + dev.getCost();
				//else we can't remove anymore of this device
				}else{
					//remove device from list of devices we can remove
					selection.remove(index);	
					//if we can't remove any more devices then break
					if(selection.isEmpty()) break;
				}
			}else{
				//if stage only has on this device then remove from list of devices that can be removed
				selection.remove(index);
				//if we can't remove any more devices then break
				if(selection.isEmpty())break;
			}				
		}
		return modifedStages;
	}
	
	
	//Method used to generate solutions given the current cost and devices
	public static ArrayList<Integer> GenerateSolution(ArrayList<Integer> stageSize, ArrayList<Device> devices, float currCost){
		ArrayList<Integer> stageQuantity = new ArrayList<Integer>(stageSize);
		ArrayList<Integer> selection = new ArrayList<Integer>(); // list of the devices that you can afford to add
		float cost = 0;
		// add every device index to the available devices to chosen from
		for(int i = 0; i < stageSize.size(); i++){
			selection.add(i);
		}
		//While our current cost is less than our budget loop
		while(currCost <= budget){
			//Generates a random index that is in our available devices to add
			int index = getRandom(selection.size()-1,0);
			//Selects the corresponding index from available devices
			int val = selection.get(index);
			//Gets the device at this index and it's corresponding cost
			Device dev = devices.get(val);
			cost = dev.getCost() + currCost;
				//if the current cost plus the device cost is still less than budget then add this device and cost to current solution
				if (cost <= budget) {
					stageQuantity.set(val, stageQuantity.get(val) + 1);
					currCost = currCost + dev.getCost();
					//else we can't afford to add more of this device
				} else {
					// remove this device from selection
					selection.remove(index);					
						// break if you can't afford any devices
						if(selection.isEmpty())	break;
				}
		}
		return stageQuantity;
	}
	
	//Method to calculate the reliability of a stage
	public static float calcStageRel(float rel, int quantity){
		double calc = 1 - (Math.pow(1 - rel, quantity));  
		return (float)calc;
	}
	
	//Method to calculate the reliability of the overall circuit combination
	public static float calcReliabilty(ArrayList<Integer> stageSize, ArrayList<Device> devices){
		float totalRel = 0;
		float devRel = 0;
		int devQuantity = 0;
		for( int i = 0; i < devices.size() ; i++){
			devRel = devices.get(i).getReliability();
			devQuantity = stageSize.get(i);
			float stageRel = calcStageRel(devRel, devQuantity);
			if(totalRel == 0){
				totalRel = stageRel;
			}else{
				totalRel *= stageRel;
			}
		}
		return totalRel;
	}
	
	//Method to calculate the overall cost of the circuit
	public static float calcCost(ArrayList<Integer> stageAmount, ArrayList<Device> devices){
		float totalCost = 0;
		float cost = 0;
		for(int i = 0; i < devices.size(); i++){
			cost = devices.get(i).getCost();
			cost *= stageAmount.get(i);
			totalCost += cost;
		}
		return totalCost;
	}
	
	//Method to calculate the acceptance probability of accepting the new solution
	//if the new solution has a better reliability then return 1
	//else calculate its probability and return this
	public static float acceptanceProb(float prevRel, float nextRel, float temperature){
		if(nextRel > prevRel){
			return 1.0f;
		}
		float percentage = (nextRel - prevRel)/ prevRel;	
		return (percentage+1) * (temperature/initalTemperature);
	}
	
	//Method to generate a random number between a max and min number
	public static int getRandom(int max, int min){
		Random rand = new Random();
		int randNum = rand.nextInt(((max -min)+1) + min);
		return randNum;
	}
	
	//Method to print out the final solution
	public static void Print(ArrayList<Integer> solution, ArrayList<Device> devices){
		
		float cost = 0;
		float reliability = 0;
		float stageRel = 0;
		float stageCost = 0;
		Device dev;	
		System.out.println("Rel     " + "Cost  " + "Qty      " + "FRel      " + "FCost");
		for(int i = 0; i < solution.size(); i++){
			dev = devices.get(i);
			cost = dev.getCost();
			reliability = dev.getReliability();
			stageRel = calcStageRel(reliability, solution.get(i));
			stageCost = cost * solution.get(i);
			System.out.println(String.format("%.2f", reliability) + "  " + String.format("%6.6s" ,cost) + "  " + String.format("%3.3s", solution.get(i)) + 
			"  " + String.format("%12.12s", stageRel) + "  " +  String.format("%.2f", stageCost));
		}
		System.out.println();
	}
}
