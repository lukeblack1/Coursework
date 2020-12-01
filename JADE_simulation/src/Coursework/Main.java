package Coursework;
import jade.core.*;
import jade.core.Runtime;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;


public class Main {

	public static void main(String[] args) {
		Profile myProfile = new ProfileImpl();
		Runtime myRuntime = Runtime.instance();
		try{
			ContainerController myContainer = myRuntime.createMainContainer(myProfile);	
			AgentController rma = myContainer.createNewAgent("rma", "jade.tools.rma.rma", null);
			rma.start();
			
			AgentController simulationAgent = myContainer.createNewAgent("StudentBuyer", BuyerAgent.class.getCanonicalName(), null);
			simulationAgent.start();
			
			int numSellers = 30;
			AgentController seller;
			for(int i=0; i<numSellers; i++) {
				seller = myContainer.createNewAgent("StudentSeller" + i, SellerAgent.class.getCanonicalName(), null);
				seller.start();
			}
			
			AgentController tickerAgent = myContainer.createNewAgent("ticker", BuyerSellerTicker.class.getCanonicalName(),
					null);
			tickerAgent.start();
			
			AgentController TimetablerAgent = myContainer.createNewAgent("TimetablerAgent", TimetablerAgent.class.getCanonicalName(),
					null);
			TimetablerAgent.start();
			
			 
			
		}
		catch(Exception e){
			System.out.println("Exception starting agent: " + e.toString());
		}


	}

}
