package com.jaamsim.ui;

import java.io.File;
import java.net.URL;

import javax.swing.JOptionPane;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.EntitySink;
import com.jaamsim.basicsim.Simulation;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.LogicalTimeFactoryFactory;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.RtiFactory;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.IllegalName;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

public class RTI extends NullFederateAmbassador
{
	private GUIFrame gui;
	public static RTI instance;
	
	private static RTIambassador _rtiAmbassador;
	private static EncoderFactory _encoderFactory;
	private static final int CRC_PORT = 8989;
	private static final String FEDERATION_NAME = "HLA_Lean";
	private static String RTI_HOST = "localhost";
    private static File xmlFile = new File("HLA_Lean.xml");
    
    private HLAfloat64TimeFactory _logicalTimeFactory;
//    private InteractionClassHandle _loadScenario;
	private HLAfloat64Time _logicalTime;
	private HLAfloat64Interval _lookahead;
	
	private AttributeHandle _name;
	private Object reservation = new Object();
	private boolean reservationSucceeded = false;
	private boolean reservationCompleted = false;
	
	public RTI(GUIFrame gui) throws Exception
	{
		this.gui = gui;
		instance = this;
		//
        RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
        _rtiAmbassador = rtiFactory.getRtiAmbassador();
        _encoderFactory = rtiFactory.getEncoderFactory();
		_rtiAmbassador.connect(this, CallbackModel.HLA_IMMEDIATE, RTI_HOST);
        _rtiAmbassador.joinFederationExecution("Scenario0", FEDERATION_NAME, new URL[]{xmlFile.toURL()});
        //
    	// Subscribe and publish interactions
        InteractionClassHandle _loadScenario = _rtiAmbassador.getInteractionClassHandle("ScenarioLoaded");
        ParameterHandle _workers = _rtiAmbassador.getParameterHandle(_loadScenario, "FederateName");
        //
//		_rtiAmbassador.subscribeInteractionClass(_loadScenario);
		_rtiAmbassador.publishInteractionClass(_loadScenario);
		//
        // Subscribe and publish objects
        ObjectClassHandle _scenarioOut = _rtiAmbassador.getObjectClassHandle("Scenario");
        _name = _rtiAmbassador.getAttributeHandle(_scenarioOut, "Name");

        AttributeHandleSet attributeSet = _rtiAmbassador.getAttributeHandleSetFactory().create();
        attributeSet.add(_name);
        
        _rtiAmbassador.subscribeObjectClassAttributes(_scenarioOut, attributeSet);
//        _rtiAmbassador.publishObjectClassAttributes(_scenarioOut, attributeSet);
        
        _logicalTimeFactory = (HLAfloat64TimeFactory) LogicalTimeFactoryFactory.getLogicalTimeFactory(HLAfloat64TimeFactory.NAME);
        _logicalTime = _logicalTimeFactory.makeInitial();
//        _logicalTime = _logicalTimeFactory.makeTime(new Double((getProperty("Federation.initialTime"))));
      
        _lookahead = _logicalTimeFactory.makeInterval(5);
        _rtiAmbassador.enableTimeRegulation(_lookahead);
		_rtiAmbassador.enableTimeConstrained();
		//
		String objName = "xx";
		//
		do
		{
			try
			{
				reservationCompleted = false;
				_rtiAmbassador.reserveObjectInstanceName(objName);
				//
				synchronized (reservation)
				{
					while(!reservationCompleted) reservation.wait();
				}  
			}
			catch (IllegalName e) 
			{
				System.out.println("Illegal name. Try again.");
			} 
			catch (RTIexception e) 
			{
				System.out.println("RTI exception when reserving name: " + e.getMessage());
				return;
			}
		}
		while(!reservationSucceeded);
		//
		System.out.println(111);
		/*
        ObjectInstanceHandle _scOUT = _rtiAmbassador.registerObjectInstance(_scenarioOut, objName); //register has to wait until the reserveObject Instance is done. the best way is to use the synchronized in Java or we use thread.sleep(1000) equivalent to 1 s but it is not professional
        while(true)
        {
        	String workersValue = "3";
        	if (workersValue.equals(".")) break;
        	System.out.print("Enter wip------Attribute: ");
        	//
			ParameterHandleValueMap parameters = _rtiAmbassador.getParameterHandleValueMapFactory().create(1);
			HLAunicodeString workersEncoded = _encoderFactory.createHLAunicodeString(workersValue);
	//        
			workersEncoded.setValue(workersValue);
			parameters.put(_workers, workersEncoded.toByteArray());
	//		Thread.sleep(500);
			_rtiAmbassador.sendInteraction(_loadScenario, parameters, null);
			
			AttributeHandleValueMap attributeValues = _rtiAmbassador.getAttributeHandleValueMapFactory().create(1);
	        HLAunicodeString wipDataEncoder = _encoderFactory.createHLAunicodeString(objName);
	        attributeValues.put(_name, wipDataEncoder.toByteArray());
	        
	        _rtiAmbassador.updateAttributeValues(_scOUT, attributeValues, null);
        }
        //
        _rtiAmbassador.resignFederationExecution(ResignAction.DELETE_OBJECTS_THEN_DIVEST);
        try 
        {
           _rtiAmbassador.destroyFederationExecution(FEDERATION_NAME);
        } 
        catch (FederatesCurrentlyJoined ignored) 
        {
        }
        _rtiAmbassador.disconnect();
        _rtiAmbassador = null;
        */
	}
	
	@Override
	public final void objectInstanceNameReservationSucceeded(String objectName)
	{
	   synchronized (reservation) {
	      reservationCompleted = true;
	      reservationSucceeded = true;
	      reservation.notifyAll();
	   }
	}

	@Override
	public final void objectInstanceNameReservationFailed(String objectName)
	{
	   synchronized (reservation) {
	      reservationCompleted = true;
	      reservationSucceeded = false;
	      reservation.notifyAll();
	   }
	}
	
	@Override
	public void reflectAttributeValues(ObjectInstanceHandle theObject,
	                                   AttributeHandleValueMap theAttributes,
	                                   byte[] userSuppliedTag,
	                                   OrderType sentOrdering,
	                                   TransportationTypeHandle theTransport,
	                                   SupplementalReflectInfo reflectInfo)
	{
	     try 
	     {
	        final HLAunicodeString usernameDecoder = _encoderFactory.createHLAunicodeString();
	        usernameDecoder.decode(theAttributes.get(_name));
	        String wipHLA = usernameDecoder.getValue();
	        System.out.println("Reflect Attribute Values in Scenario 0 Class ----- " + wipHLA);
	//	            Participant member = new Participant(memberName);
	//	            System.out.println("[" + member + " has joined]");
	//	            System.out.print("> ");
	//	            _knownObjects.put(theObject, member);
	     }
	     catch (DecoderException e) 
	     {
	        System.out.println("Failed to decode incoming attribute");
	     }
    }
	
	//

	public void onAddEntity(EntitySink entitySink, DisplayEntity ent)
	{
		try
		{
			System.out.println("Entity Proccessed...");
			System.out.println("Pause and wait HLA...");
			gui.pauseSimulation();
			//
			double simTime = Simulation.getInstance().getSimTime();
			_logicalTime = _logicalTimeFactory.makeTime(simTime);
			JOptionPane.showMessageDialog(null, "PAUSE: " + simTime);
			//
			_rtiAmbassador.nextMessageRequest(_logicalTime);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void timeAdvanceGrant(LogicalTime theTime) throws FederateInternalError 
	{
		JOptionPane.showMessageDialog(null, "START");
		gui.startSimulation();
	}
}