package com.jaamsim.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.jaamsim.Graphics.DisplayEntity;
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
import hla.rti1516e.exceptions.InTimeAdvancingState;
import hla.rti1516e.exceptions.InteractionClassNotDefined;
import hla.rti1516e.exceptions.InteractionClassNotPublished;
import hla.rti1516e.exceptions.InteractionParameterNotDefined;
import hla.rti1516e.exceptions.InvalidLogicalTime;
import hla.rti1516e.exceptions.LogicalTimeAlreadyPassed;
import hla.rti1516e.exceptions.NotConnected;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.exceptions.RTIinternalError;
import hla.rti1516e.exceptions.RequestForTimeConstrainedPending;
import hla.rti1516e.exceptions.RequestForTimeRegulationPending;
import hla.rti1516e.exceptions.RestoreInProgress;
import hla.rti1516e.exceptions.SaveInProgress;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

public class Federate extends NullFederateAmbassador {
  private GUIFrame gui;
  public static Federate instance;

  private RTIambassador _rtiAmbassador;
  private EncoderFactory _encoderFactory;
  private static final String FEDERATION_NAME = "HLA_Lean";
  private static String RTI_HOST = "localhost";
  File xmlFile = new File("C:/Users/academic1/Desktop/RTIEclipse/HLA_LM/HLA_Lean.xml");
  private AttributeHandle Name;
  private AttributeHandle SimTime;
  // private AttributeHandle RunDuration;
  private AttributeHandle MaterialBuffer;
  private AttributeHandle SKU;
  private AttributeHandle WIP;
  private AttributeHandle DefectRate;
  private AttributeHandle ProductionRate;
  private AttributeHandle LeadTime;
  private AttributeHandle SetupTime;
  private AttributeHandle ProcessingTime;
  private AttributeHandle TravelTime;
  private AttributeHandle PlannedDownTime;
  private AttributeHandle UnplannedDownTime;
  private AttributeHandle DefectiveProbability;
  private AttributeHandle MarketDemand;
  private AttributeHandle NumberOfWorkers;

  private volatile boolean reservationCompleted;
  private volatile boolean reservationSucceeded;
  private Object reservation = new Object();
  private ObjectInstanceHandle regObjInstName;
  String objectInstanceName;
  static BufferedWriter writer = null;
  private static String[] guiArgs;
  public int jaamsimPort = 0;
  private int scWIP = 0;
  private float scLeadTime = 0;
  private float scDefectRate = 0;
  private int scProductionRate = 0;

  private boolean trEnabled = false;
  private boolean tcEnabled = false;

  BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

  private InteractionClassHandle ScenarioLoad;
  private InteractionClassHandle ScenarioLoaded;
  private InteractionClassHandle ScenarioError;
  private InteractionClassHandle SimulationControl;
  private InteractionClassHandle SMEDInteraction;
  private InteractionClassHandle POKAYOKEInteraction;
  private InteractionClassHandle FiveSInteraction;
  private InteractionClassHandle UCELLInteraction;
  private InteractionClassHandle PULLInteraction;

  private ParameterHandle ScName;
  private ParameterHandle FederateNameLoaded;
  private ParameterHandle Action;
  private ParameterHandle RealTimeFactor;
  private ParameterHandle SetupTimeReduction;
  private ParameterHandle DefectsReduction;
  private ParameterHandle ProcessingAndDefectsReduction;
  private ParameterHandle TravelTimeReduction;
  private ParameterHandle PULLValues;

  private HLAfloat64TimeFactory _logicalTimeFactory;
  private HLAfloat64Time _logicalTime;
  private HLAfloat64Interval _lookahead;
  private String federateName;

  String setupTime;
  String processingTime;
  String travelTime;
  String plannedDownTime;
  String unplannedDownTime;
  String defectiveProbability;
  String marketDemand;
  String numberOfWorkers;

  public Federate(String federateName) throws Exception {
    instance = this;
    this.federateName = federateName;
    this.objectInstanceName = "Senario_" + federateName;
    //
    ////////////////////////////////////////
    // Get RTI Ambassador Host and port //
    ////////////////////////////////////////

    RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
    _rtiAmbassador = rtiFactory.getRtiAmbassador();
    _encoderFactory = rtiFactory.getEncoderFactory();
    _rtiAmbassador.connect(this, CallbackModel.HLA_IMMEDIATE, RTI_HOST);

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Joining any existing Federation, Argument1 is the Federate, Argument2
    /////////////////////////////////////////////////////////////////////////////////////////////// //
    ///////////////////////////////////////////////////////////////////////////////////////////////

    _rtiAmbassador.joinFederationExecution(federateName, FEDERATION_NAME, new URL[] { xmlFile.toURL() });

    //////////////////////////////////////
    // Objects/Attributes Declaration //
    //////////////////////////////////////

    ObjectClassHandle Scenario = _rtiAmbassador.getObjectClassHandle("Scenario");
    AttributeHandleSet attributeSet = _rtiAmbassador.getAttributeHandleSetFactory().create();

    Name = _rtiAmbassador.getAttributeHandle(Scenario, "Name");
    SimTime = _rtiAmbassador.getAttributeHandle(Scenario, "SimTime");
    MaterialBuffer = _rtiAmbassador.getAttributeHandle(Scenario, "MaterialBuffer");
    SKU = _rtiAmbassador.getAttributeHandle(Scenario, "SKU");
    WIP = _rtiAmbassador.getAttributeHandle(Scenario, "WIP");
    ProductionRate = _rtiAmbassador.getAttributeHandle(Scenario, "ProductionRate");
    DefectRate = _rtiAmbassador.getAttributeHandle(Scenario, "DefectRate");
    LeadTime = _rtiAmbassador.getAttributeHandle(Scenario, "LeadTime");
    SetupTime = _rtiAmbassador.getAttributeHandle(Scenario, "SetupTime");
    ProcessingTime = _rtiAmbassador.getAttributeHandle(Scenario, "ProcessingTime");
    TravelTime = _rtiAmbassador.getAttributeHandle(Scenario, "TravelTime");
    PlannedDownTime = _rtiAmbassador.getAttributeHandle(Scenario, "PlannedDownTime");
    UnplannedDownTime = _rtiAmbassador.getAttributeHandle(Scenario, "UnplannedDownTime");
    DefectiveProbability = _rtiAmbassador.getAttributeHandle(Scenario, "DefectiveProbability");
    MarketDemand = _rtiAmbassador.getAttributeHandle(Scenario, "MarketDemand");
    NumberOfWorkers = _rtiAmbassador.getAttributeHandle(Scenario, "NumberOfWorkers");

    attributeSet.add(Name);
    attributeSet.add(SimTime);
    attributeSet.add(MaterialBuffer);
    attributeSet.add(SKU);
    attributeSet.add(WIP);
    attributeSet.add(ProductionRate);
    attributeSet.add(DefectRate);
    attributeSet.add(LeadTime);
    attributeSet.add(SetupTime);
    attributeSet.add(ProcessingTime);
    attributeSet.add(TravelTime);
    attributeSet.add(PlannedDownTime);
    attributeSet.add(UnplannedDownTime);
    attributeSet.add(DefectiveProbability);
    attributeSet.add(MarketDemand);
    attributeSet.add(NumberOfWorkers);

    // Subscribe and publish objects
    _rtiAmbassador.subscribeObjectClassAttributes(Scenario, attributeSet);
    _rtiAmbassador.publishObjectClassAttributes(Scenario, attributeSet);

    ///////////////////////////////////////////
    // Interactions/Parameters Declaration //
    ///////////////////////////////////////////

    ScenarioLoad = _rtiAmbassador.getInteractionClassHandle("ScenarioLoad");
    ScName = _rtiAmbassador.getParameterHandle(ScenarioLoad, "ScName");

    ScenarioLoaded = _rtiAmbassador.getInteractionClassHandle("ScenarioLoaded");
    FederateNameLoaded = _rtiAmbassador.getParameterHandle(ScenarioLoaded, "FederateName");

    ScenarioError = _rtiAmbassador.getInteractionClassHandle("ScenarioError");
    _rtiAmbassador.getParameterHandle(ScenarioError, "FederateName");

    SimulationControl = _rtiAmbassador.getInteractionClassHandle("SimulationControl");
    Action = _rtiAmbassador.getParameterHandle(SimulationControl, "Action");
    RealTimeFactor = _rtiAmbassador.getParameterHandle(SimulationControl, "RealTimeFactor");

    SMEDInteraction = _rtiAmbassador.getInteractionClassHandle("SMEDInteraction");
    SetupTimeReduction = _rtiAmbassador.getParameterHandle(SMEDInteraction, "SetupTimeReduction");

    POKAYOKEInteraction = _rtiAmbassador.getInteractionClassHandle("POKAYOKEInteraction");
    DefectsReduction = _rtiAmbassador.getParameterHandle(POKAYOKEInteraction, "DefectsReduction");

    FiveSInteraction = _rtiAmbassador.getInteractionClassHandle("FiveSInteraction");
    ProcessingAndDefectsReduction = _rtiAmbassador.getParameterHandle(FiveSInteraction,
        "ProcessingAndDefectsReduction");

    UCELLInteraction = _rtiAmbassador.getInteractionClassHandle("UCELLInteraction");
    TravelTimeReduction = _rtiAmbassador.getParameterHandle(UCELLInteraction, "TravelTimeReduction");

    PULLInteraction = _rtiAmbassador.getInteractionClassHandle("PULLInteraction");
    PULLValues = _rtiAmbassador.getParameterHandle(PULLInteraction, "PULLValues");

    // Subscribe and publish interactions
    _rtiAmbassador.subscribeInteractionClass(ScenarioLoad);
    _rtiAmbassador.subscribeInteractionClass(SimulationControl);
    _rtiAmbassador.subscribeInteractionClass(SMEDInteraction);
    _rtiAmbassador.subscribeInteractionClass(POKAYOKEInteraction);
    _rtiAmbassador.subscribeInteractionClass(FiveSInteraction);
    _rtiAmbassador.subscribeInteractionClass(UCELLInteraction);
    _rtiAmbassador.subscribeInteractionClass(PULLInteraction);

    _rtiAmbassador.publishInteractionClass(ScenarioLoaded);
    _rtiAmbassador.publishInteractionClass(ScenarioError);

    ///////////////////////////////////////////
    // Object reservation and registration //
    ///////////////////////////////////////////

    do {

      try {
        reservationCompleted = false;
        _rtiAmbassador.reserveObjectInstanceName(objectInstanceName);
        // Thread.sleep(3000);
        synchronized (reservation) {
          while (!reservationCompleted) {
            reservation.wait();
          }
        }
      } catch (IllegalName e) {
        System.out.println("Illegal name. Try again.");
      } catch (RTIexception e) {
        System.out.println("RTI exception when reserving name: " + e.getMessage());
        return;
      }
    } while (!reservationSucceeded);

    regObjInstName = _rtiAmbassador.registerObjectInstance(Scenario, objectInstanceName);

    ///////////////////////
    // Attributes Data //
    ///////////////////////

    System.out.print("My Scenario Name is " + federateName + "\r");
  }

  @Override
  public final void objectInstanceNameReservationSucceeded(String objectName) {
    synchronized (reservation) {
      reservationCompleted = true;
      reservationSucceeded = true;
      reservation.notifyAll();
    }
  }

  @Override
  public final void objectInstanceNameReservationFailed(String objectName) {
    synchronized (reservation) {
      reservationCompleted = true;
      reservationSucceeded = false;
      reservation.notifyAll();
    }
  }

  @Override
  public void removeObjectInstance(ObjectInstanceHandle theObject, byte[] userSuppliedTag, OrderType sentOrdering,
      SupplementalRemoveInfo removeInfo) {
  }

  @Override
  public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass,
      String objectName) throws FederateInternalError {

    System.out.println("Object Instance Discovered is: " + theObject + " " + theObjectClass + " " + objectName);
  }

  @Override
  public void turnUpdatesOnForObjectInstance(ObjectInstanceHandle theObject, AttributeHandleSet theAttributes)
      throws FederateInternalError {

    System.out.println("Updates turned ON for Object Instance Handle: " + theObject
        + " and for the Attribute Handle Set: " + theAttributes);
  }

  @Override
  public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes,
      byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport,
      SupplementalReflectInfo reflectInfo)

  {
    try {

      final HLAunicodeString stringDecoder = _encoderFactory.createHLAunicodeString();

      if (theAttributes.containsKey(SetupTime)) {
        stringDecoder.decode(theAttributes.get(SetupTime));
        setupTime = stringDecoder.getValue();

        File SetupTimeFile = new File(
            "C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/" + federateName + "/SetupTime.txt");
        FileWriter fw = new FileWriter(SetupTimeFile, false);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(setupTime);
        bw.close();

      }

      if (theAttributes.containsKey(ProcessingTime)) {
        stringDecoder.decode(theAttributes.get(ProcessingTime));
        processingTime = stringDecoder.getValue();

        File ProcessingTimeFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
            + federateName + "/ProcessingTime.txt");
        FileWriter fw = new FileWriter(ProcessingTimeFile, false);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(processingTime);
        bw.close();

      }

      if (theAttributes.containsKey(TravelTime)) {
        stringDecoder.decode(theAttributes.get(TravelTime));
        travelTime = stringDecoder.getValue();

        File TravelTimeFile = new File(
            "C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/" + federateName + "/TravelTime.txt");
        FileWriter fw = new FileWriter(TravelTimeFile, false);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(travelTime);
        bw.close();

      }

      if (theAttributes.containsKey(PlannedDownTime)) {
        stringDecoder.decode(theAttributes.get(PlannedDownTime));
        plannedDownTime = stringDecoder.getValue();

        File PlannedDownTimeFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
            + federateName + "/PlannedDownTime.txt");
        FileWriter fw = new FileWriter(PlannedDownTimeFile, false);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(plannedDownTime);
        bw.close();

      }

      if (theAttributes.containsKey(UnplannedDownTime)) {
        stringDecoder.decode(theAttributes.get(UnplannedDownTime));
        unplannedDownTime = stringDecoder.getValue();

        File UnplannedDownTimeFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
            + federateName + "/UnplannedDownTime.txt");
        FileWriter fw = new FileWriter(UnplannedDownTimeFile, false);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(unplannedDownTime);
        bw.close();

      }

      if (theAttributes.containsKey(DefectiveProbability)) {
        stringDecoder.decode(theAttributes.get(DefectiveProbability));
        defectiveProbability = stringDecoder.getValue();

        File DefectiveProbabilityFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
            + federateName + "/DefectiveProbability.txt");
        FileWriter fw = new FileWriter(DefectiveProbabilityFile, false);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(defectiveProbability);
        bw.close();

      }

      if (theAttributes.containsKey(NumberOfWorkers)) {
        stringDecoder.decode(theAttributes.get(NumberOfWorkers));
        numberOfWorkers = stringDecoder.getValue();

        if (federateName.equals("CrossTraining")) {
          String arr[] = numberOfWorkers.split("\t");
          int nbOfWorkers = Integer.parseInt(arr[0]) + Integer.parseInt(arr[1]) + Integer.parseInt(arr[2])
              + Integer.parseInt(arr[3]);

          File NumberOfWorkersFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
              + federateName + "/NumberOfWorkers.txt");
          FileWriter fw = new FileWriter(NumberOfWorkersFile, false);
          BufferedWriter bw = new BufferedWriter(fw);

          bw.write(Integer.toString(nbOfWorkers));
          bw.close();
        } else {
          File NumberOfWorkersFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
              + federateName + "/NumberOfWorkers.txt");
          FileWriter fw = new FileWriter(NumberOfWorkersFile, false);
          BufferedWriter bw = new BufferedWriter(fw);

          bw.write(numberOfWorkers);
          bw.close();
        }
      }

      if (theAttributes.containsKey(MarketDemand)) {
        stringDecoder.decode(theAttributes.get(MarketDemand));
        marketDemand = stringDecoder.getValue();

        File MarketDemandFile = new File(
            "C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/" + federateName + "/MarketDemand.txt");
        FileWriter fw = new FileWriter(MarketDemandFile, false);
        BufferedWriter bw = new BufferedWriter(fw);

        bw.write(marketDemand);
        bw.close();

      }

    } catch (DecoderException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public final void provideAttributeValueUpdate(ObjectInstanceHandle theObject, AttributeHandleSet theAttributes,
      byte[] userSuppliedTag) {

  }

  @Override
  public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters,
      byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport,
      SupplementalReceiveInfo receiveInfo) throws FederateInternalError {

    try {

      final HLAunicodeString stringDecoder = _encoderFactory.createHLAunicodeString();

      if (theParameters.containsKey(ScName)) {
        stringDecoder.decode(theParameters.get(ScName));
        System.out.println("The scenario to Load is: " + stringDecoder.getValue());
        String scenario = stringDecoder.getValue();
        if (scenario.equals(federateName))
          loadScenario();
      }

      else if (theParameters.containsKey(Action)) {
        stringDecoder.decode(theParameters.get(Action));
        String action = stringDecoder.getValue();
        if (action.equals("START")) {
          enableTimeManagement();
          startScenario();
        }
        if (action.equals("PAUSE"))
          pauseScenario();
        disableTimeManagement();
        if (action.equals("STOP"))
          stopScenario();
      }

      else if (theParameters.containsKey(RealTimeFactor)) {

        stringDecoder.decode(theParameters.get(RealTimeFactor));
        gui.setRealTimeFactor(Double.valueOf(stringDecoder.getValue()));
      }

      else if (theParameters.containsKey(SetupTimeReduction)) {

        if (federateName.equals("SMED")) {
          stringDecoder.decode(theParameters.get(SetupTimeReduction));

          File SetupTimeReductionFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
              + federateName + "/SetupTimeReduction.txt");
          FileWriter fw = new FileWriter(SetupTimeReductionFile, false);
          BufferedWriter bw = new BufferedWriter(fw);

          bw.write(stringDecoder.getValue());
          bw.close();
        }
      }

      else if (theParameters.containsKey(ProcessingAndDefectsReduction)) {
        System.out.println("Marhabaaaaaaaaaaaa");
        if (federateName.equals("5S")) // make sure about FiveS
        {
          stringDecoder.decode(theParameters.get(ProcessingAndDefectsReduction));

          File ProcessingAndDefectsReductionFile = new File(
              "C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/" + federateName
                  + "/ProcessingAndDefectsReduction.txt");
          FileWriter fw = new FileWriter(ProcessingAndDefectsReductionFile, false);
          BufferedWriter bw = new BufferedWriter(fw);

          bw.write(stringDecoder.getValue());
          bw.close();
        }
      }

      else if (theParameters.containsKey(DefectsReduction)) {

        if (federateName.equals("POKAYOKE")) {
          stringDecoder.decode(theParameters.get(DefectsReduction));

          File DefectsReductionFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
              + federateName + "/DefectsReduction.txt");
          FileWriter fw = new FileWriter(DefectsReductionFile, false);
          BufferedWriter bw = new BufferedWriter(fw);

          bw.write(stringDecoder.getValue());
          bw.close();
        }
      }

      else if (theParameters.containsKey(TravelTimeReduction)) {

        if (federateName.equals("UCELL")) // make sure about FiveS
        {
          stringDecoder.decode(theParameters.get(TravelTimeReduction));

          File TravelTimeReductionFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
              + federateName + "/TravelTimeReduction.txt");
          FileWriter fw = new FileWriter(TravelTimeReductionFile, false);
          BufferedWriter bw = new BufferedWriter(fw);

          bw.write(stringDecoder.getValue());
          bw.close();
        }
      }

      else if (theParameters.containsKey(PULLValues)) {

        if (federateName.equals("PULL")) // make sure about FiveS
        {
          stringDecoder.decode(theParameters.get(PULLValues));

          File PULLValuesFile = new File("C:/Users/academic1/Desktop/Lean_Simulation_Aero-May19/"
              + federateName + "/PULLValues.txt");
          FileWriter fw = new FileWriter(PULLValuesFile, false);
          BufferedWriter bw = new BufferedWriter(fw);

          bw.write(stringDecoder.getValue());
          bw.close();
        }
      }

      else
        System.out.println("nothinnnggg");

    } catch (FederateNotExecutionMember | NotConnected | InteractionClassNotPublished
        | InteractionParameterNotDefined | InteractionClassNotDefined | SaveInProgress | RestoreInProgress
        | RTIinternalError e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  void enableTimeManagement() throws Exception {
    ///////////////////////
    // Time Management //
    ///////////////////////

    _logicalTimeFactory = (HLAfloat64TimeFactory) LogicalTimeFactoryFactory
        .getLogicalTimeFactory(HLAfloat64TimeFactory.NAME);
    _logicalTime = _logicalTimeFactory.makeInitial();

    _lookahead = _logicalTimeFactory.makeInterval(5);
    _rtiAmbassador.enableTimeRegulation(_lookahead);
    _rtiAmbassador.enableTimeConstrained();
  }

  void disableTimeManagement() throws Exception {
    ///////////////////////
    // Time Management //
    ///////////////////////

    _rtiAmbassador.disableTimeRegulation();
    trEnabled = false;
    _rtiAmbassador.disableTimeConstrained();
    tcEnabled = false;
  }

  @Override
  public void timeRegulationEnabled(LogicalTime time) throws FederateInternalError {
    // TODO Auto-generated method stub

    trEnabled = true;
    System.out.println("time regulation enabled");
  }

  @Override
  public void timeConstrainedEnabled(LogicalTime time) throws FederateInternalError {
    // TODO Auto-generated method stub

    tcEnabled = true;
    System.out.println("time constrained enabled");

  }

  void updateAttributes(DisplayEntity entity) throws Exception {
    AttributeHandleValueMap attributeValues = _rtiAmbassador.getAttributeHandleValueMapFactory().create(1);
    HLAunicodeString scNameEncoder = _encoderFactory.createHLAunicodeString(federateName);
    //

    float scSimTime = (float) Simulation.getInstance().getSimTime() / 3600;
    HLAfloat32LE scSimTimeEncoder = _encoderFactory.createHLAfloat32LE(scSimTime);
    HLAinteger32LE scWIPEncoder = _encoderFactory.createHLAinteger32LE(scWIP);
    HLAinteger32LE scProductionRateEncoder = _encoderFactory.createHLAinteger32LE(scProductionRate);
    HLAfloat32LE scDefectRateEncoder = _encoderFactory.createHLAfloat32LE(scDefectRate);
    HLAfloat32LE scLeadTimeEncoder = _encoderFactory.createHLAfloat32LE(scLeadTime);

    attributeValues.put(Name, scNameEncoder.toByteArray());
    attributeValues.put(SimTime, scSimTimeEncoder.toByteArray());
    attributeValues.put(WIP, scWIPEncoder.toByteArray());
    attributeValues.put(ProductionRate, scProductionRateEncoder.toByteArray());
    attributeValues.put(DefectRate, scDefectRateEncoder.toByteArray());
    attributeValues.put(LeadTime, scLeadTimeEncoder.toByteArray());

    //
    _rtiAmbassador.updateAttributeValues(regObjInstName, attributeValues, null);
  }

  void disconnect() throws Exception {
    _rtiAmbassador.resignFederationExecution(ResignAction.DELETE_OBJECTS_THEN_DIVEST);
    _rtiAmbassador.destroyFederationExecution(FEDERATION_NAME);
    _rtiAmbassador.disconnect();
    _rtiAmbassador = null;
  }

  void loadScenario() throws Exception {

    System.out.println("starting guiFrame");
    gui = GUIFrame.create(guiArgs);

    ParameterHandleValueMap scenarioLoadedParameters = _rtiAmbassador.getParameterHandleValueMapFactory().create(1);
    HLAunicodeString scenarioLoadedEncoder = _encoderFactory.createHLAunicodeString();
    scenarioLoadedEncoder.setValue(federateName);
    scenarioLoadedParameters.put(FederateNameLoaded, scenarioLoadedEncoder.toByteArray());
    //
    boolean scLoaded = true;
    _rtiAmbassador.sendInteraction(scLoaded ? ScenarioLoaded : ScenarioError, scenarioLoadedParameters, null);
  }

  void startScenario() {

    gui.startSimulation();

  }

  void pauseScenario() {
    gui.pauseSimulation();
  }

  void stopScenario() {
    gui.stopSimulation();
    gui.close();
  }

  private boolean pause = false;

  public synchronized void onAddEntity(DisplayEntity entity) {
    try {
      double simTime = Simulation.getInstance().getSimTime();

      updateAttributes(entity);

      while (!trEnabled || !tcEnabled) {
        Thread.sleep(100);
      }
      //
      try {

        double dtWIP = entity.getOutputHandle("dtWIP").getValue(simTime, Double.class);
        scWIP = (int) dtWIP;
        System.out.println("WIP isssssssssss " + scWIP);

        double productionRate = entity.getOutputHandle("productionRate").getValue(simTime, Double.class);
        scProductionRate = (int) productionRate;
        System.out.println("Prod Rate is " + scProductionRate);

        double defectRate = entity.getOutputHandle("defectRate").getValue(simTime, Double.class);
        scDefectRate = (float) defectRate;
        System.out.println("Defect Rate is" + scDefectRate);

        double dtLeadTime = entity.getOutputHandle("dtLeadTime").getValue(simTime, Double.class);
        scLeadTime = (float) dtLeadTime;

        double month = entity.getOutputHandle("month").getValue(simTime, Double.class);
        //
      } catch (Exception e) {
        e.printStackTrace();
      }
      //
      pause = true;
      _logicalTime = _logicalTimeFactory.makeTime(simTime / 3600);

      try {
        _rtiAmbassador.nextMessageRequest(_logicalTime);
        while (pause)
          wait();

      } catch (LogicalTimeAlreadyPassed | InvalidLogicalTime | InTimeAdvancingState
          | RequestForTimeRegulationPending | RequestForTimeConstrainedPending | SaveInProgress
          | RestoreInProgress | FederateNotExecutionMember | NotConnected | RTIinternalError e) {
        e.printStackTrace();

      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public synchronized void timeAdvanceGrant(LogicalTime theTime) throws FederateInternalError {
    System.out.println("Before gui.startSimulation: " + gui.getSimState());
    pause = false;
    notifyAll();
    System.out.println("After gui.startSimulation: " + gui.getSimState());
  }

  public static void main(String[] args) throws Exception {
    String federateName = args[0].substring(4);
    instance = new Federate(federateName);
    guiArgs = args;
  }
}
