Define Palette { 'Basic Objects' }

Define ObjectType {
                    ModelEntity
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
                    }

ModelEntity             JavaClass { com.sandwell.JavaSimulation3D.ModelEntity           } Palette { 'Basic Objects' } DefaultDisplayModel 	{ Cube 					}	DragAndDrop 	{ FALSE 	}
EntityGenerator         JavaClass { com.jaamsim.BasicObjects.EntityGenerator            } Palette { 'Basic Objects' } DefaultDisplayModel 	{ Circle 				}
Server                  JavaClass { com.jaamsim.BasicObjects.Server                     } Palette { 'Basic Objects' } DefaultDisplayModel 	{ Rectangle 				}
EntitySink              JavaClass { com.jaamsim.BasicObjects.EntitySink                 } Palette { 'Basic Objects' } DefaultDisplayModel 	{ Circle 				}
EntityConveyor          JavaClass { com.jaamsim.BasicObjects.EntityConveyor             } Palette { 'Basic Objects' } DefaultDisplayModel	{ ScreenPointsModelDefault			}
Queue                   JavaClass { com.sandwell.JavaSimulation3D.Queue	                } Palette { 'Basic Objects' } DefaultDisplayModel 	{ Triangle 				}
QueueRecorder           JavaClass { com.sandwell.JavaSimulation3D.QueueRecorder         } Palette { 'Basic Objects' } DefaultDisplayModel	{ Cube 					}	DragAndDrop 	{ FALSE 	}
ProbabilityDistribution JavaClass { com.sandwell.JavaSimulation.ProbabilityDistribution } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
Group                   JavaClass { com.sandwell.JavaSimulation.Group                   } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
ScriptEntity            JavaClass { com.sandwell.JavaSimulation3D.ScriptEntity          } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
Output                  JavaClass { com.sandwell.JavaSimulation.OutputMethod            } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
BooleanIndicator        JavaClass { com.sandwell.JavaSimulation3D.BooleanIndicator      } Palette { 'Basic Objects' } DefaultDisplayModel { Circle                   }
