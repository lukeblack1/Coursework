package Coursework;

import java.util.ArrayList;
import java.util.HashMap;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class BuyerAgent extends Agent {
	private ArrayList<AID> sellers = new ArrayList<>();
	private ArrayList<String>  booksToBuy = new ArrayList<>();
	private HashMap<String,ArrayList<Offer>> currentOffers = new HashMap<>();
	private AID tickerAgent;
	private int numQueriesSent;
	@Override
	protected void setup() {
		//add this agent to the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("buyer");
		sd.setName(getLocalName() + "-buyer-agent");
		dfd.addServices(sd);
		try{
			DFService.register(this, dfd);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}
		//add class every hour between 0900 - 1800 hrs
		booksToBuy.add("Module 1: 0900 - 1000 hrs");
		booksToBuy.add("Module 2: 1000 - 1100 hrs");
		booksToBuy.add("Module 3: 1100 - 1200 hrs");
		booksToBuy.add("Module 1: 1200 - 1300 hrs");
		booksToBuy.add("Module 2: 1300 - 1400 hrs");
		booksToBuy.add("Module 3: 1400 - 1500 hrs");
		booksToBuy.add("Module 1: 1500 - 1600 hrs");
		booksToBuy.add("Module 2: 1600 - 1700 hrs");
		booksToBuy.add("Module 3: 1700 - 1800 hrs");
		doWait(15000);

		addBehaviour(new TickerWaiter(this));
	}


	@Override
	protected void takeDown() {
		//Deregister from the yellow pages
		try{
			DFService.deregister(this);
		}
		catch(FIPAException e){
			e.printStackTrace();
		}
	}

	public class TickerWaiter extends CyclicBehaviour {

		//behaviour to wait for a new day
		public TickerWaiter(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			MessageTemplate mt = MessageTemplate.or(MessageTemplate.MatchContent("new day"),
					MessageTemplate.MatchContent("terminate"));
			ACLMessage msg = myAgent.receive(mt); 
			if(msg != null) {
				if(tickerAgent == null) {
					tickerAgent = msg.getSender();
				}
				if(msg.getContent().equals("new day")) {
					//spawn new sequential behaviour for day's activities
					SequentialBehaviour dailyActivity = new SequentialBehaviour();
					//sub-behaviours will execute in the order they are added
					dailyActivity.addSubBehaviour(new FindSellers(myAgent));
					dailyActivity.addSubBehaviour(new SendEnquiries(myAgent));
					dailyActivity.addSubBehaviour(new CollectOffers(myAgent));
					dailyActivity.addSubBehaviour(new AcceptOffer(myAgent));
					dailyActivity.addSubBehaviour(new EndDay(myAgent));
					
					myAgent.addBehaviour(dailyActivity);
				}
				else {
					//termination message to end simulation
					myAgent.doDelete();
				}
			}
			else{
				block();
			}
		}

	}

	public class FindSellers extends OneShotBehaviour {

		public FindSellers(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			DFAgentDescription sellerTemplate = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("seller");
			sellerTemplate.addServices(sd);
			try{
				sellers.clear();
				DFAgentDescription[] agentsType1  = DFService.search(myAgent,sellerTemplate); 
				for(int i=0; i<agentsType1.length; i++){
					sellers.add(agentsType1[i].getName()); // this is the AID
				}
			}
			catch(FIPAException e) {
				e.printStackTrace();
			}

		}

	}

	public class SendEnquiries extends OneShotBehaviour {

		public SendEnquiries(Agent a) {
			super(a);
		}

		@Override
		public void action() {
			//send out a call for proposals for each book
			numQueriesSent = 0;
			for(String bookTitle : booksToBuy) {
				ACLMessage enquiry = new ACLMessage(ACLMessage.CFP);
				enquiry.setContent(bookTitle);
				enquiry.setConversationId(bookTitle);
				for(AID seller : sellers) {
					enquiry.addReceiver(seller);
					numQueriesSent++;
				}
				myAgent.send(enquiry);

			}

		}
	}

	public class CollectOffers extends Behaviour {
		private int numRepliesReceived = 0;

		public CollectOffers(Agent a) {
			super(a);
			currentOffers.clear();
		}


		///
		//Instructions:
		//send a ACL message.acceptproposal
		//send a ACL message.rejectproposal
		//to the seller
		//book title = id
		//randomly accept/decline offer
		//Add another subbehaviour 

		//



		@Override
		public void action() {
			boolean received = false;
			for(String bookTitle : booksToBuy) {
				MessageTemplate mt = MessageTemplate.MatchConversationId(bookTitle);
				ACLMessage msg = myAgent.receive(mt);
				if(msg != null) {
					received = true;
					numRepliesReceived++;
					if(msg.getPerformative() == ACLMessage.PROPOSE) {
						//we have an offer
						//the first offer for a book today
						if(!currentOffers.containsKey(bookTitle)) {

							ArrayList<Offer> offers = new ArrayList<>();
							offers.add(new Offer(msg.getSender(),
									Integer.parseInt(msg.getContent())));
							currentOffers.put(bookTitle, offers);
						} 
						//subsequent offers
						else {
							ArrayList<Offer> offers = currentOffers.get(bookTitle);
							offers.add(new Offer(msg.getSender(),
									Integer.parseInt(msg.getContent())));
						}

					}

				}
			}
			if(!received) {
				block();
			}
		}



		@Override
		public boolean done() {
			return numRepliesReceived == numQueriesSent;
		}

		@Override
		public int onEnd() {
			//print the offers
			for(String book : booksToBuy) {
				if(currentOffers.containsKey(book)) {
					ArrayList<Offer> offers = currentOffers.get(book);
					for(Offer o : offers) {
						//Accepted offer
						System.out.println(book + "," + o.getSeller().getLocalName() + "," + o.getPrice());
					}
				}
				else {
					//No Offers recieved , no seller
					System.out.println("No offers for " + book);
				}
			}
			return 0;
		}

	}

	public class AcceptOffer extends OneShotBehaviour 
	{
		//
		public AcceptOffer(Agent a) {
			super(a);
		}
		@Override
		
		public void action() {
			boolean received = true;
			for(String bookTitle : booksToBuy) {
				if(currentOffers.containsKey(bookTitle))
				{
					ArrayList<Offer> offers=currentOffers.get(bookTitle);
					//Get offers out hash map, will give you the offers in the array list to store it.
					int index = 0;
					//GETAID
					for(Offer OneItem : offers)
					{
						if (index == 0)
						{
							ACLMessage sellerDone = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
							sellerDone.addReceiver(OneItem.getSeller());
							//conversationid
							sellerDone.setConversationId(bookTitle);
							myAgent.send(sellerDone);

						} else
						{
							ACLMessage sellerDone = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
							sellerDone.addReceiver(OneItem.getSeller());
							//conversationid
							sellerDone.setConversationId(bookTitle);
							myAgent.send(sellerDone);

						}
						index++;
					}
					//Accept //Decline 
					//accept the first 

				}


			}
		}
	}





		public class EndDay extends OneShotBehaviour {

			public EndDay(Agent a) {
				super(a);
			}

			@Override
			public void action() {
				ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
				msg.addReceiver(tickerAgent);
				msg.setContent("done");
				myAgent.send(msg);
				//send a message to each seller that we have finished
				ACLMessage sellerDone = new ACLMessage(ACLMessage.INFORM);
				sellerDone.setContent("done");
				for(AID seller : sellers) {
					sellerDone.addReceiver(seller);
				}
				myAgent.send(sellerDone);
			}
			
		

		}
		
	}

