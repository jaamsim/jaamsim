Define ObjectType {
                    Group
                    ScriptEntity
                    Output
                    Controller
                    InputValue
                    InputBox
                    ToggleButton
                    TimeSeries
                    TimeSeriesThreshold
                    ExpressionThreshold
                    ExpressionLogger
                    EntitlementSelector
                    ExpressionEntity
                    ExpressionStatistics
                    DowntimeEntity
                    ValueSequence
                    EventSchedule
                    FileToVector
                    FileToMatrix
                    FileToHashMap
                    ExternalProgram
                    ExternalProgramServer
                    EntitySystem
                  }

Define TextModel  {
                    InputValueModel
                  }

Define IconModel  {
                    ControllerIconModel
                    TimeSeriesIconModel
                    ExpLoggerIconModel
                    SelectorIconModel
                    ExpressionEntityIconModel
                    ExpressionStatisticsIconModel
                    DowntimeIconModel
                    ValueSequenceIconModel
                    EventScheduleIconModel
                    FileToVectorIconModel
                    FileToMatrixIconModel
                    FileToHashMapIconModel
                    ExternalProgramIconModel
                    EntitySystemIconModel
                  }

ControllerIconModel     ImageFile { '<res>/images/Controller-256.png'   }  Transparent { TRUE }
TimeSeriesIconModel     ImageFile { '<res>/images/TimeSeries-256.png'   }  Transparent { TRUE }
SelectorIconModel       ImageFile { '<res>/images/Selector-256.png'     }  Transparent { TRUE }
ExpLoggerIconModel      ImageFile { '<res>/images/ExpressionLogger-256.png' }  Transparent { TRUE }
ExpressionEntityIconModel  ImageFile { '<res>/images/ExpressionEntity-256.png' }  Transparent { TRUE }
ExpressionStatisticsIconModel  ImageFile { '<res>/images/ExpressionStatistics-256.png' }  Transparent { TRUE }
DowntimeIconModel       ImageFile { '<res>/images/Downtime-256.png'     }  Transparent { TRUE }
ValueSequenceIconModel  ImageFile { '<res>/images/ValueSequence-256.png'}  Transparent { TRUE }
EventScheduleIconModel  ImageFile { '<res>/images/EventSchedule-256.png'}  Transparent { TRUE }
FileToVectorIconModel   ImageFile { '<res>/images/FileToVector-256.png' }  Transparent { TRUE }
FileToMatrixIconModel   ImageFile { '<res>/images/FileToMatrix-256.png' }  Transparent { TRUE }
FileToHashMapIconModel  ImageFile { '<res>/images/FileToHashMap-256.png' }  Transparent { TRUE }
ExternalProgramIconModel  ImageFile { '<res>/images/ExternalProgram-256.png' }  Transparent { TRUE }
EntitySystemIconModel   ImageFile { '<res>/images/EntitySystem-256.png' }  Transparent { TRUE }

Group                   JavaClass { com.jaamsim.basicsim.Group                          } Palette { 'Basic Objects' } DragAndDrop { FALSE }
ScriptEntity            JavaClass { com.jaamsim.basicsim.ScriptEntity                   } Palette { 'Basic Objects' } DragAndDrop { FALSE }
Output                  JavaClass { com.jaamsim.basicsim.OutputMethod                   } Palette { 'Basic Objects' } DragAndDrop { FALSE }
Controller              JavaClass { com.jaamsim.CalculationObjects.Controller           } Palette { 'Basic Objects' } DefaultDisplayModel { ControllerIconModel } IconFile { '<res>/images/Controller-24.png'   }
InputValue              JavaClass { com.jaamsim.BasicObjects.InputValue                 } Palette { 'Basic Objects' } DefaultDisplayModel { InputValueModel     } IconFile { '<res>/images/InputValue-24.png'         }
InputBox                JavaClass { com.jaamsim.Graphics.InputBox                       } Palette { 'Basic Objects' } DefaultDisplayModel { InputBoxModel       } IconFile { '<res>/images/InputValue-24.png'   } DragAndDrop { FALSE }
ToggleButton            JavaClass { com.jaamsim.BasicObjects.ToggleButton               } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle           } IconFile { '<res>/images/Button-24.png'       } DefaultSize { 1.0 0.5 0.0 m }
TimeSeries              JavaClass { com.jaamsim.Samples.TimeSeries                      } Palette { 'Basic Objects' } DefaultDisplayModel { TimeSeriesIconModel } IconFile { '<res>/images/TimeSeries-24.png'   }
TimeSeriesThreshold     JavaClass { com.jaamsim.Thresholds.TimeSeriesThreshold          } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle           } IconFile { '<res>/images/Threshold-24.png'    } DefaultSize { 0.5 0.5 0.0 m }
ExpressionThreshold     JavaClass { com.jaamsim.Thresholds.ExpressionThreshold          } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle           } IconFile { '<res>/images/Threshold-24.png'    } DefaultSize { 0.5 0.5 0.0 m }
ExpressionLogger        JavaClass { com.jaamsim.BasicObjects.ExpressionLogger           } Palette { 'Basic Objects' } DefaultDisplayModel { ExpLoggerIconModel  } IconFile { '<res>/images/ExpressionLogger-24.png' }
EntitlementSelector     JavaClass { com.jaamsim.BasicObjects.EntitlementSelector        } Palette { 'Basic Objects' } DefaultDisplayModel { SelectorIconModel   } IconFile { '<res>/images/Selector-24.png'     }
ExpressionEntity        JavaClass { com.jaamsim.BasicObjects.ExpressionEntity           } Palette { 'Basic Objects' } DefaultDisplayModel { ExpressionEntityIconModel   } IconFile { '<res>/images/ExpressionEntity-24.png' }
ExpressionStatistics    JavaClass { com.jaamsim.BasicObjects.ExpressionStatistics       } Palette { 'Basic Objects' } DefaultDisplayModel { ExpressionStatisticsIconModel } IconFile { '<res>/images/ExpressionStatistics-24.png' }
DowntimeEntity          JavaClass { com.jaamsim.BasicObjects.DowntimeEntity             } Palette { 'Basic Objects' } DefaultDisplayModel { DowntimeIconModel   } IconFile { '<res>/images/Downtime-24.png'     }
ValueSequence           JavaClass { com.jaamsim.BasicObjects.ValueSequence              } Palette { 'Basic Objects' } DefaultDisplayModel { ValueSequenceIconModel } IconFile { '<res>/images/ValueSequence-24.png' }
EventSchedule           JavaClass { com.jaamsim.BasicObjects.EventSchedule              } Palette { 'Basic Objects' } DefaultDisplayModel { EventScheduleIconModel } IconFile { '<res>/images/EventSchedule-24.png' }
FileToVector            JavaClass { com.jaamsim.BasicObjects.FileToVector               } Palette { 'Basic Objects' } DefaultDisplayModel { FileToVectorIconModel  } IconFile { '<res>/images/FileToVector-24.png'  }
FileToMatrix            JavaClass { com.jaamsim.BasicObjects.FileToMatrix               } Palette { 'Basic Objects' } DefaultDisplayModel { FileToMatrixIconModel  } IconFile { '<res>/images/FileToMatrix-24.png'  }
FileToHashMap           JavaClass { com.jaamsim.BasicObjects.FileToHashMap              } Palette { 'Basic Objects' } DefaultDisplayModel { FileToHashMapIconModel } IconFile { '<res>/images/FileToHashMap-24.png'  }
ExternalProgram         JavaClass { com.jaamsim.BasicObjects.ExternalProgram            } Palette { 'Basic Objects' } DefaultDisplayModel { ExternalProgramIconModel } IconFile { '<res>/images/ExternalProgram-24.png'  }
ExternalProgramServer   JavaClass { com.jaamsim.BasicObjects.ExternalProgramServer      } Palette { 'Basic Objects' } DefaultDisplayModel { ExternalProgramIconModel } IconFile { '<res>/images/ExternalProgram-24.png'  }
EntitySystem            JavaClass { com.jaamsim.BasicObjects.EntitySystem               } Palette { 'Basic Objects' } DefaultDisplayModel { EntitySystemIconModel  } IconFile { '<res>/images/EntitySystem-24.png'  }

Group                 Description { 'Collection of entities used as an convenience for specifying model inputs.' }
ScriptEntity          Description { 'Executes a series of commands in simulated time. Used primarily for recording videos.' }
Output                Description { '???' }
Controller            Description { 'Signals modelled objects to perform an update.' }
InputValue            Description { 'Provides a way to enter a numerical value directly into the simulation model screen.' }
InputBox              Description { 'Text that provides an input value for the model.' }
ToggleButton          Description { 'Button that can be clicked by the user.' }
TimeSeries            Description { 'Provides a floating point number that changes in simulated time following a series of input values.' }
TimeSeriesThreshold   Description { 'Specifies a range of values from a TimeSeries for which an activity is permitted.' }
ExpressionThreshold   Description { 'Specifies a logical condition for which an activity is permitted.' }
ExpressionLogger      Description { 'Records the values for one or more expressions to a log file at regular intervals.' }
EntitlementSelector   Description { 'Selects an index on the basis of entitlement from a given set of proportions.' }
ExpressionEntity      Description { 'Calculates the value for a specified expression.' }
ExpressionStatistics  Description { 'Records the statistics for the numerical value of a specified expression.' }
DowntimeEntity        Description { 'Provides Breakdown and Maintenance controls.' }
ValueSequence         Description { 'Generates a repeating sequence of numerical values.' }
EventSchedule         Description { 'Generates a sequence inter-arrival times from a list of event times.' }
FileToVector          Description { 'Populates a one-dimensional array with data from a specified file.' }
FileToMatrix          Description { 'Populates a two-dimensional array with data from a specified file.' }
FileToHashMap         Description { 'Populates a hashmap with data from a specified file.' }
ExternalProgram       Description { 'Executes an external program when an entity is received.' }
ExternalProgramServer Description { 'Executes a long running external program that can respond to multiple requests.' }
EntitySystem          Description { 'Represents a group of entities whose state is determined by the states of those entities.' }
