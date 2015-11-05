Define ObjectType {
                    Group
                    ScriptEntity
                    Output
                    InputValue
                    BooleanIndicator
                    TimeSeries
                    TimeSeriesThreshold
                    ExpressionThreshold
                    ExpressionLogger
                    EntitlementSelector
                    ExpressionEntity
                    DowntimeEntity
                  }

Define TextModel  {
                    InputValueModel
                  }

Define ImageModel {
                    TimeSeriesIconModel
                    ExpLoggerIconModel
                    SelectorIconModel
                    ExpressionEntityIconModel
                    DowntimeIconModel
                  }

TimeSeriesIconModel     ImageFile { '<res>/images/TimeSeries-256.png'   }  Transparent { TRUE }
SelectorIconModel       ImageFile { '<res>/images/Selector-256.png'     }  Transparent { TRUE }
ExpLoggerIconModel      ImageFile { '<res>/images/EntityLogger-256.png' }  Transparent { TRUE }
ExpressionEntityIconModel  ImageFile { '<res>/images/ExpressionEntity-256.png' }  Transparent { TRUE }
DowntimeIconModel       ImageFile { '<res>/images/Downtime-256.png'     }  Transparent { TRUE }

Group                   JavaClass { com.jaamsim.basicsim.Group                          } Palette { 'Basic Objects' } DragAndDrop { FALSE }
ScriptEntity            JavaClass { com.jaamsim.basicsim.ScriptEntity                   } Palette { 'Basic Objects' } DragAndDrop { FALSE }
Output                  JavaClass { com.jaamsim.basicsim.OutputMethod                   } Palette { 'Basic Objects' } DragAndDrop { FALSE }
BooleanIndicator        JavaClass { com.jaamsim.BasicObjects.BooleanIndicator           } Palette { 'Basic Objects' } DefaultDisplayModel { Circle              } DragAndDrop { FALSE }
InputValue              JavaClass { com.jaamsim.BasicObjects.InputValue                 } Palette { 'Basic Objects' } DefaultDisplayModel { InputValueModel     } IconFile { '<res>/images/InputValue-24.png'         }
TimeSeries              JavaClass { com.jaamsim.Samples.TimeSeries                      } Palette { 'Basic Objects' } DefaultDisplayModel { TimeSeriesIconModel } IconFile { '<res>/images/TimeSeries-24.png'   }
TimeSeriesThreshold     JavaClass { com.jaamsim.Thresholds.TimeSeriesThreshold          } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle           } IconFile { '<res>/images/Threshold-24.png'    } DefaultSize { 0.5 0.5 0.0 m }
ExpressionThreshold     JavaClass { com.jaamsim.Thresholds.ExpressionThreshold          } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle           } IconFile { '<res>/images/Threshold-24.png'    } DefaultSize { 0.5 0.5 0.0 m }
ExpressionLogger        JavaClass { com.jaamsim.BasicObjects.ExpressionLogger           } Palette { 'Basic Objects' } DefaultDisplayModel { ExpLoggerIconModel  } IconFile { '<res>/images/EntityLogger-24.png' }
EntitlementSelector     JavaClass { com.jaamsim.BasicObjects.EntitlementSelector        } Palette { 'Basic Objects' } DefaultDisplayModel { SelectorIconModel   } IconFile { '<res>/images/Selector-24.png'     }
ExpressionEntity        JavaClass { com.jaamsim.BasicObjects.ExpressionEntity           } Palette { 'Basic Objects' } DefaultDisplayModel { ExpressionEntityIconModel   } IconFile { '<res>/images/ExpressionEntity-24.png' }
DowntimeEntity          JavaClass { com.jaamsim.BasicObjects.DowntimeEntity             } Palette { 'Basic Objects' } DefaultDisplayModel { DowntimeIconModel   } IconFile { '<res>/images/Downtime-24.png'     }

Group               Description { 'Collection of entities used as an convenience for specifying model inputs.' }
ScriptEntity        Description { 'Executes a series of commands in simulated time. Used primarily for recording videos.' }
Output              Description { '???' }
InputValue          Description { 'Provides a way to enter a numerical value directly into the simulation model screen.' }
BooleanIndicator    Description { 'Circular entity that changes color to indicate TRUE and FALSE' }
TimeSeries          Description { 'Provides a floating point number that changes in simulated time following a series of input values.' }
TimeSeriesThreshold Description { 'Specifies a range of values from a TimeSeries for which an activity is permitted.' }
ExpressionThreshold Description { 'Specifies a logical condition for which an activity is permitted.' }
ExpressionLogger    Description { 'Records the values for one or more expressions to a log file at regular intervals.' }
EntitlementSelector Description { 'Selects an index on the basis of entitlement from a given set of proportions.' }
ExpressionEntity    Description { 'Calculates the value for a specified expression.' }
DowntimeEntity      Description { 'Provides Breakdown and Maintenance controls.' }
