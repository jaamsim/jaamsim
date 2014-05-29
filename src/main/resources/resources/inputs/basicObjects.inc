Define ObjectType {
                    EntityGenerator
                    Server
                    EntitySink
                    EntityConveyor
					EntityDelay
					Assign
                    Queue
					Seize
					Release
					Resource
					Branch
					EntityGate
					EntitySignal
                    QueueRecorder
                    Group
                    ScriptEntity
                    Output
                    BooleanIndicator
                    TimeSeries
                    TimeSeriesThreshold
                    ExpressionThreshold
                  }

Define	ImageModel {
			SourceIconModel
			SinkIconModel
			ServerIconModel
			AssignModel
			ResourceIconModel
			SeizeIconModel
			ReleaseIconModel
			BranchIconModel
			EntityGateIconModel
			EntitySignalIconModel
			TimeSeriesIconModel
			}
			
SourceIconModel         ImageFile { '<res>/images/Source-256.png' }      Transparent { TRUE }
SinkIconModel           ImageFile { '<res>/images/Sink-256.png'   }      Transparent { TRUE }
ServerIconModel         ImageFile { '<res>/images/Server-256.png' }      Transparent { TRUE }
AssignModel             ImageFile { '<res>/images/Assign-256.png' }      Transparent { TRUE }
ResourceIconModel       ImageFile { '<res>/images/Resource-256.png' }    Transparent { TRUE }
SeizeIconModel          ImageFile { '<res>/images/Seize-256.png'  }      Transparent { TRUE }
ReleaseIconModel        ImageFile { '<res>/images/Release-256.png' }     Transparent { TRUE }
BranchIconModel         ImageFile { '<res>/images/Branch-256.png' }      Transparent { TRUE }
EntityGateIconModel     ImageFile { '<res>/images/Gate-256.png' }        Transparent { TRUE }
EntitySignalIconModel   ImageFile { '<res>/images/Signal-256.png' }      Transparent { TRUE }
TimeSeriesIconModel     ImageFile { '<res>/images/TimeSeries-256.png' }  Transparent { TRUE }

EntityGenerator         JavaClass { com.jaamsim.BasicObjects.EntityGenerator            } Palette { 'Basic Objects' } DefaultDisplayModel 	{ SourceIconModel		}
Server                  JavaClass { com.jaamsim.BasicObjects.Server                     } Palette { 'Basic Objects' } DefaultDisplayModel 	{ ServerIconModel		}
EntitySink              JavaClass { com.jaamsim.BasicObjects.EntitySink                 } Palette { 'Basic Objects' } DefaultDisplayModel 	{ SinkIconModel 		}
EntityConveyor          JavaClass { com.jaamsim.BasicObjects.EntityConveyor             } Palette { 'Basic Objects' } DefaultDisplayModel	{ ScreenPointsModelDefault			}
EntityDelay             JavaClass { com.jaamsim.BasicObjects.EntityDelay                } Palette { 'Basic Objects' } DefaultDisplayModel	{ ScreenPointsModelDefault			}
Assign                  JavaClass { com.jaamsim.BasicObjects.Assign                     } Palette { 'Basic Objects' } DefaultDisplayModel	{ AssignModel			}
Queue                   JavaClass { com.sandwell.JavaSimulation3D.Queue	                } Palette { 'Basic Objects' } DefaultDisplayModel 	{ Triangle 				}
Seize                   JavaClass { com.jaamsim.BasicObjects.Seize                      } Palette { 'Basic Objects' } DefaultDisplayModel 	{ SeizeIconModel		}
Release                 JavaClass { com.jaamsim.BasicObjects.Release                    } Palette { 'Basic Objects' } DefaultDisplayModel 	{ ReleaseIconModel 		}
Resource                JavaClass { com.jaamsim.BasicObjects.Resource                   } Palette { 'Basic Objects' } DefaultDisplayModel 	{ ResourceIconModel		}
Branch                  JavaClass { com.jaamsim.BasicObjects.Branch                     } Palette { 'Basic Objects' } DefaultDisplayModel 	{ BranchIconModel       }
EntityGate              JavaClass { com.jaamsim.BasicObjects.EntityGate                 } Palette { 'Basic Objects' } DefaultDisplayModel 	{ EntityGateIconModel   }
EntitySignal            JavaClass { com.jaamsim.BasicObjects.EntitySignal               } Palette { 'Basic Objects' } DefaultDisplayModel 	{ EntitySignalIconModel }
QueueRecorder           JavaClass { com.sandwell.JavaSimulation3D.QueueRecorder         } Palette { 'Basic Objects' } DefaultDisplayModel	{ Cube 					}	DragAndDrop 	{ FALSE 	}
Group                   JavaClass { com.sandwell.JavaSimulation.Group                   } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
ScriptEntity            JavaClass { com.sandwell.JavaSimulation3D.ScriptEntity          } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
Output                  JavaClass { com.sandwell.JavaSimulation.OutputMethod            } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
BooleanIndicator        JavaClass { com.sandwell.JavaSimulation3D.BooleanIndicator      } Palette { 'Basic Objects' } DefaultDisplayModel { Circle                   }
TimeSeries              JavaClass { com.sandwell.JavaSimulation.TimeSeries              } Palette { 'Basic Objects' } DefaultDisplayModel 	{ TimeSeriesIconModel    }
TimeSeriesThreshold	    JavaClass { com.jaamsim.Thresholds.TimeSeriesThreshold		    } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle }
ExpressionThreshold	    JavaClass { com.jaamsim.Thresholds.ExpressionThreshold		    } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle }

EntityGenerator    Description { 'Creates copies of a specified entity at specified intervals.' }
Server             Description { 'Processes a received entity over a specified duration.' }
EntitySink         Description { 'Destroys any entity it receives.' }
EntityConveyor     Description { 'Transports a received entity along a specified path at a fixed speed.' }
EntityDelay        Description { 'Delays a received entity by a specified duration.' }
Assign             Description { 'Assigns new values to attributes.' }
Queue              Description { 'Stores received entities in first-in-first-out order until they are needed.' }
Seize              Description { 'Seizes one or more units of a Resource.' }
Release            Description { 'Releases one or more units of a Resource.' }
Resource           Description { 'Set of identical resource units that be seized and released by various process.' }
Branch             Description { 'Directs received entities to an assortment of destinations.' }
EntityGate         Description { 'Blocks received entities from progressing further until the Gate is opened by a Signal.' }
EntitySignal       Description { 'On receipt of an entity, a specified Gate is opened or closed.' }
QueueRecorder      Description { 'Logs an entry every time the specified queue receives or releases an entity.' }
Group              Description { 'Collection of entities used as an convenience for specifying model inputs.' }
ScriptEntity       Description { 'Executes a series of commands in simulated time. Used primarily for recording videos.' }
Output             Description { '???' }
BooleanIndicator   Description { 'Circular entity that changes color to indicate TRUE and FALSE' }
TimeSeries         Description { 'Provides a floating point number that changes in simulated time following a series of input values.' }
TimeSeriesThreshold Description { 'Specifies a range of values from a TimeSeries for which an activity is permitted.' }
ExpressionThreshold Description { 'Specifies a logical condition for which an activity is permitted.' }
