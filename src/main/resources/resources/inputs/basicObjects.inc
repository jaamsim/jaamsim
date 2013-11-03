Define Palette { 'Basic Objects' }

Define ObjectType {
                    EntityGenerator
                    Server
                    EntitySink
                    EntityConveyor
                    Queue
                    QueueRecorder
                    ProbabilityDistribution
                    Group
                    ScriptEntity
                    Output
                    BooleanIndicator
                    TimeSeries
                  }

Define	ImageModel {
			SourceIconModel
			SinkIconModel
			ServerIconModel
			}
			
SourceIconModel         ImageFile { '/resources/images/Source-256.png' }      Transparent { TRUE }
SinkIconModel           ImageFile { '/resources/images/Sink-256.png'   }      Transparent { TRUE }
ServerIconModel         ImageFile { '/resources/images/Server-256.png' }      Transparent { TRUE }

EntityGenerator         JavaClass { com.jaamsim.BasicObjects.EntityGenerator            } Palette { 'Basic Objects' } DefaultDisplayModel 	{ SourceIconModel		}
Server                  JavaClass { com.jaamsim.BasicObjects.Server                     } Palette { 'Basic Objects' } DefaultDisplayModel 	{ ServerIconModel		}
EntitySink              JavaClass { com.jaamsim.BasicObjects.EntitySink                 } Palette { 'Basic Objects' } DefaultDisplayModel 	{ SinkIconModel 		}
EntityConveyor          JavaClass { com.jaamsim.BasicObjects.EntityConveyor             } Palette { 'Basic Objects' } DefaultDisplayModel	{ ScreenPointsModelDefault			}
Queue                   JavaClass { com.sandwell.JavaSimulation3D.Queue	                } Palette { 'Basic Objects' } DefaultDisplayModel 	{ Triangle 				}
QueueRecorder           JavaClass { com.sandwell.JavaSimulation3D.QueueRecorder         } Palette { 'Basic Objects' } DefaultDisplayModel	{ Cube 					}	DragAndDrop 	{ FALSE 	}
ProbabilityDistribution JavaClass { com.sandwell.JavaSimulation.ProbabilityDistribution } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
Group                   JavaClass { com.sandwell.JavaSimulation.Group                   } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
ScriptEntity            JavaClass { com.sandwell.JavaSimulation3D.ScriptEntity          } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
Output                  JavaClass { com.sandwell.JavaSimulation.OutputMethod            } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
BooleanIndicator        JavaClass { com.sandwell.JavaSimulation3D.BooleanIndicator      } Palette { 'Basic Objects' } DefaultDisplayModel { Circle                   }
TimeSeries              JavaClass { com.sandwell.JavaSimulation.TimeSeries              } Palette { 'Basic Objects' }

EntityGenerator    Description { 'Creates copies of a specified entity at specified intervals.' }
Server             Description { 'Processes a received entity over a specified duration.' }
EntitySink         Description { 'Destroys any entity it receives.' }
EntityConveyor     Description { 'Transports a received entity along a specified path at a fixed speed.' }
Queue              Description { 'Stores received entities in first-in-first-out order until they are needed.' }
QueueRecorder      Description { 'Logs an entry every time the specified queue receives or releases an entity.' }
ProbabilityDistribution  Description { 'Obsolete probability distribution retained for backwards compatibity with TLS.' }
Group              Description { 'Collection of entities used as an convenience for specifying model inputs.' }
ScriptEntity       Description { 'Executes a series of commands in simulated time. Used primarily for recording videos.' }
Output             Description { '???' }
BooleanIndicator   Description { 'Circular entity that changes color to indicate TRUE and FALSE' }
TimeSeries         Description { 'Provides a floating point number that changes in simulated time following a series of input values.' }
