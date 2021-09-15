package com.jaamsim.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTimeFactoryFactory;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactory;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfloat32LE;
import hla.rti1516e.encoding.HLAinteger32LE;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.FederateNotExecutionMember;
import hla.rti1516e.exceptions.IllegalName;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

public class FederateFactory extends NullFederateAmbassador
{
	private static FederateFactory instance;
	
	private RTIambassador _rtiAmbassador;
	private EncoderFactory _encoderFactory;
	private static final int CRC_PORT = 8989;
	private static final String FEDERATION_NAME = "HLA_Lean";
	private static String RTI_HOST = "localhost";
	File xmlFile = new File("HLA_Lean.xml");
	private InteractionClassHandle _scenarioLoad;
	private ParameterHandle _scenarioName;
	private AttributeHandle _name;
	private AttributeHandle Name;
	private AttributeHandle SimTime;
	private AttributeHandle RunDuration;
	private AttributeHandle MaterialBuffer;
	private AttributeHandle SKU;
	private volatile boolean reservationCompleted;
	private volatile boolean reservationSucceeded;
	private Object reservation = new Object();
	private ObjectInstanceHandle regObjInstName;
	String objectInstanceName = "Scenario";
	static BufferedWriter writer = null;
	public int jaamsimPort = 0;
	
	BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	
	private ParameterHandle ScName;
	private ParameterHandle FederateNameLoaded;
	private ParameterHandle FederateNameError;
	private InteractionClassHandle ScenarioLoad;
	private InteractionClassHandle ScenarioLoaded;
	private InteractionClassHandle ScenarioError;
	private HLAfloat64TimeFactory _logicalTimeFactory;
	private HLAfloat64Time _logicalTime;
	private HLAfloat64Interval _lookahead;
	private String federateName;

	public FederateFactory() throws Exception
	{
		String federateName = "JaamsimFactory";
		//
		RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
		_rtiAmbassador = rtiFactory.getRtiAmbassador();
		_encoderFactory = rtiFactory.getEncoderFactory();
		_rtiAmbassador.connect(this, CallbackModel.HLA_IMMEDIATE, RTI_HOST);

		///////////////////////////////////////////////////////////////////////////////////////////////
		//  Joining any existing Federation, Argument1 is the Federate, Argument2 is the Federation  //
		///////////////////////////////////////////////////////////////////////////////////////////////

		_rtiAmbassador.joinFederationExecution(federateName, FEDERATION_NAME, new URL[]{xmlFile.toURL()});


		///////////////////////////////////////////
		//  Interactions/Parameters Declaration  //
		///////////////////////////////////////////     

		ScenarioLoad = _rtiAmbassador.getInteractionClassHandle("ScenarioLoad");
		ScName = _rtiAmbassador.getParameterHandle(ScenarioLoad, "ScName");

		// Subscribe and publish interactions
		_rtiAmbassador.subscribeInteractionClass(ScenarioLoad);

	}

	void loadScenario() throws FederateNotExecutionMember, NotConnected, InteractionClassNotPublished, InteractionParameterNotDefined, InteractionClassNotDefined, SaveInProgress, RestoreInProgress, RTIinternalError
	{
//		gui = GUIFrame.create();
		
		
		
		ParameterHandleValueMap scenarioLoadedParameters = _rtiAmbassador.getParameterHandleValueMapFactory().create(1);
		HLAunicodeString scenarioLoadedEncoder = _encoderFactory.createHLAunicodeString();
		scenarioLoadedEncoder.setValue(federateName);
		scenarioLoadedParameters.put(FederateNameLoaded, scenarioLoadedEncoder.toByteArray());
		//
		boolean scLoaded = true;
		_rtiAmbassador.sendInteraction(scLoaded? ScenarioLoaded : ScenarioError, scenarioLoadedParameters, null);	
	}

	
	@Override
	public void receiveInteraction(InteractionClassHandle interactionClass,
			ParameterHandleValueMap theParameters,
			byte[] userSuppliedTag,
			OrderType sentOrdering,
			TransportationTypeHandle theTransport,
			SupplementalReceiveInfo receiveInfo)
					throws
					FederateInternalError
	{
		System.out.println("int received");
		try {
			final HLAunicodeString stringDecoder = _encoderFactory.createHLAunicodeString();

			if(theParameters.containsKey(ScName)){
				
				// Scenario Name + configuration file and arguments needed
				
				
				stringDecoder.decode(theParameters.get(ScName));
				System.out.println("The scenario to Load is: " + stringDecoder.getValue());
				String scenario = stringDecoder.getValue();
				if(scenario.equals(federateName)) 
					loadScenario();
			}



		} catch (FederateNotExecutionMember | NotConnected | InteractionClassNotPublished | InteractionParameterNotDefined
				| InteractionClassNotDefined | SaveInProgress | RestoreInProgress | RTIinternalError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DecoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws Exception 
	{
		instance = new FederateFactory();
	}
}

