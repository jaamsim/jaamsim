Define ObjectType {
	SimEntity
	SimShape
	EntityLauncher
	EntityGenerator
	EntitySink
	Server
	Queue
	EntityConveyor
	EntityDelay
	Seize
	Release
	EntityProcessor
	Assign
	Branch
	Duplicate
	Combine
	SetGraphics
	EntityGate
	EntitySignal
	SignalThreshold
	Assemble
	EntityContainer
	Pack
	Unpack
	AddTo
	RemoveFrom
	EntityLogger
	Statistics
}

Define	IconModel  {
	EntityLauncherIconModel
	SourceIconModel
	SinkIconModel
	ServerIconModel
	SeizeIconModel
	ReleaseIconModel
	EntityProcessorIconModel
	AssignModel
	BranchIconModel
	DuplicateIconModel
	CombineIconModel
	SetGraphicsIconModel
	EntityGateIconModel
	EntitySignalIconModel
	AssembleIconModel
	PackIconModel
	UnpackIconModel
	AddToIconModel
	RemoveFromIconModel
	EntityLoggerIconModel
	StatisticsIconModel
}

EntityLauncherIconModel ImageFile { '<res>/images/EntityLauncher-256.png' }  Transparent { TRUE }
SourceIconModel         ImageFile { '<res>/images/Source-256.png'       }  Transparent { TRUE }
SinkIconModel           ImageFile { '<res>/images/Sink-256.png'         }  Transparent { TRUE }
ServerIconModel         ImageFile { '<res>/images/Server-256.png'       }  Transparent { TRUE }
SeizeIconModel          ImageFile { '<res>/images/Seize-256.png'        }  Transparent { TRUE }
ReleaseIconModel        ImageFile { '<res>/images/Release-256.png'      }  Transparent { TRUE }
EntityProcessorIconModel  ImageFile { '<res>/images/EntityProcessor-256.png' }  Transparent { TRUE }
AssignModel             ImageFile { '<res>/images/Assign-256.png'       }  Transparent { TRUE }
BranchIconModel         ImageFile { '<res>/images/Branch-256.png'       }  Transparent { TRUE }
DuplicateIconModel      ImageFile { '<res>/images/Duplicate-256.png'    }  Transparent { TRUE }
CombineIconModel        ImageFile { '<res>/images/Combine-256.png'      }  Transparent { TRUE }
SetGraphicsIconModel    ImageFile { '<res>/images/SetGraphics-256.png'  }  Transparent { TRUE }
EntityGateIconModel     ImageFile { '<res>/images/Gate-256.png'         }  Transparent { TRUE }
EntitySignalIconModel   ImageFile { '<res>/images/Signal-256.png'       }  Transparent { TRUE }
AssembleIconModel       ImageFile { '<res>/images/Assemble-256.png'     }  Transparent { TRUE }
PackIconModel           ImageFile { '<res>/images/Pack-256.png'         }  Transparent { TRUE }
UnpackIconModel         ImageFile { '<res>/images/Unpack-256.png'       }  Transparent { TRUE }
AddToIconModel          ImageFile { '<res>/images/AddTo-256.png'        }  Transparent { TRUE }
RemoveFromIconModel     ImageFile { '<res>/images/RemoveFrom-256.png'   }  Transparent { TRUE }
EntityLoggerIconModel   ImageFile { '<res>/images/EntityLogger-256.png' }  Transparent { TRUE }
StatisticsIconModel     ImageFile { '<res>/images/Statistics-256.png'   }  Transparent { TRUE }

Define PolylineModel { EntityDelayModel  EntityConveyorModel }

SimEntity               JavaClass { com.jaamsim.ProcessFlow.SimEntity             }  Palette { 'Process Flow' }  DefaultDisplayModel { Sphere                   } IconFile { '<res>/images/SimEntity-24.png'       } DefaultSize { 0.5 0.5 0.5 m }
SimShape                JavaClass { com.jaamsim.ProcessFlow.SimShape              }  Palette { 'Process Flow' }  DefaultDisplayModel { Circle                   } IconFile { '<res>/images/SimShape-24.png'        } DefaultSize { 0.5 0.5 0.5 m }
EntityLauncher          JavaClass { com.jaamsim.ProcessFlow.EntityLauncher        }  Palette { 'Process Flow' }  DefaultDisplayModel { EntityLauncherIconModel  } IconFile { '<res>/images/EntityLauncher-24.png'  }
EntityGenerator         JavaClass { com.jaamsim.ProcessFlow.EntityGenerator       }  Palette { 'Process Flow' }  DefaultDisplayModel { SourceIconModel          } IconFile { '<res>/images/Source-24.png'          }
EntitySink              JavaClass { com.jaamsim.ProcessFlow.EntitySink            }  Palette { 'Process Flow' }  DefaultDisplayModel { SinkIconModel            } IconFile { '<res>/images/Sink-24.png'            }
Server                  JavaClass { com.jaamsim.ProcessFlow.Server                }  Palette { 'Process Flow' }  DefaultDisplayModel { ServerIconModel          } IconFile { '<res>/images/Server-24.png'          }
Queue                   JavaClass { com.jaamsim.ProcessFlow.Queue                 }  Palette { 'Process Flow' }  DefaultDisplayModel { Triangle                 } IconFile { '<res>/images/Queue-24.png'           } DefaultSize { 0.5 0.5 0.0 m }
EntityConveyor          JavaClass { com.jaamsim.ProcessFlow.EntityConveyor        }  Palette { 'Process Flow' }  DefaultDisplayModel { EntityConveyorModel      } IconFile { '<res>/images/Line-24.png'            }
EntityDelay             JavaClass { com.jaamsim.ProcessFlow.EntityDelay           }  Palette { 'Process Flow' }  DefaultDisplayModel { EntityDelayModel         } IconFile { '<res>/images/Line-24.png'            }
Seize                   JavaClass { com.jaamsim.ProcessFlow.Seize                 }  Palette { 'Process Flow' }  DefaultDisplayModel { SeizeIconModel           } IconFile { '<res>/images/Seize-24.png'           }
Release                 JavaClass { com.jaamsim.ProcessFlow.Release               }  Palette { 'Process Flow' }  DefaultDisplayModel { ReleaseIconModel         } IconFile { '<res>/images/Release-24.png'         }
EntityProcessor         JavaClass { com.jaamsim.ProcessFlow.EntityProcessor       }  Palette { 'Process Flow' }  DefaultDisplayModel { EntityProcessorIconModel } IconFile { '<res>/images/EntityProcessor-24.png' }
Assign                  JavaClass { com.jaamsim.ProcessFlow.Assign                }  Palette { 'Process Flow' }  DefaultDisplayModel { AssignModel              } IconFile { '<res>/images/Assign-24.png'          }
Branch                  JavaClass { com.jaamsim.ProcessFlow.Branch                }  Palette { 'Process Flow' }  DefaultDisplayModel { BranchIconModel          } IconFile { '<res>/images/Branch-24.png'          }
Duplicate               JavaClass { com.jaamsim.ProcessFlow.Duplicate             }  Palette { 'Process Flow' }  DefaultDisplayModel { DuplicateIconModel       } IconFile { '<res>/images/Duplicate-24.png'       }
Combine                 JavaClass { com.jaamsim.ProcessFlow.Combine               }  Palette { 'Process Flow' }  DefaultDisplayModel { CombineIconModel         } IconFile { '<res>/images/Combine-24.png'         }
SetGraphics             JavaClass { com.jaamsim.ProcessFlow.SetGraphics           }  Palette { 'Process Flow' }  DefaultDisplayModel { SetGraphicsIconModel     } IconFile { '<res>/images/SetGraphics-24.png'     }
EntityGate              JavaClass { com.jaamsim.ProcessFlow.EntityGate            }  Palette { 'Process Flow' }  DefaultDisplayModel { EntityGateIconModel      } IconFile { '<res>/images/Gate-24.png'            }
EntitySignal            JavaClass { com.jaamsim.ProcessFlow.EntitySignal          }  Palette { 'Process Flow' }  DefaultDisplayModel { EntitySignalIconModel    } IconFile { '<res>/images/Signal-24.png'          }
SignalThreshold         JavaClass { com.jaamsim.Thresholds.SignalThreshold        }  Palette { 'Process Flow' }  DefaultDisplayModel { Rectangle                } IconFile { '<res>/images/Threshold-24.png'       } DefaultSize { 0.5 0.5 0.0 m }
Assemble                JavaClass { com.jaamsim.ProcessFlow.Assemble              }  Palette { 'Process Flow' }  DefaultDisplayModel { AssembleIconModel        } IconFile { '<res>/images/Assemble-24.png'        }
EntityContainer         JavaClass { com.jaamsim.ProcessFlow.EntityContainer       }  Palette { 'Process Flow' }  DefaultDisplayModel { Rectangle                } IconFile { '<res>/images/EntityContainer-24.png' }
Pack                    JavaClass { com.jaamsim.ProcessFlow.Pack                  }  Palette { 'Process Flow' }  DefaultDisplayModel { PackIconModel            } IconFile { '<res>/images/Pack-24.png'            }
Unpack                  JavaClass { com.jaamsim.ProcessFlow.Unpack                }  Palette { 'Process Flow' }  DefaultDisplayModel { UnpackIconModel          } IconFile { '<res>/images/Unpack-24.png'          }
AddTo                   JavaClass { com.jaamsim.ProcessFlow.AddTo                 }  Palette { 'Process Flow' }  DefaultDisplayModel { AddToIconModel           } IconFile { '<res>/images/AddTo-24.png'           }
RemoveFrom              JavaClass { com.jaamsim.ProcessFlow.RemoveFrom            }  Palette { 'Process Flow' }  DefaultDisplayModel { RemoveFromIconModel      } IconFile { '<res>/images/RemoveFrom-24.png'      }
EntityLogger            JavaClass { com.jaamsim.ProcessFlow.EntityLogger          }  Palette { 'Process Flow' }  DefaultDisplayModel { EntityLoggerIconModel    } IconFile { '<res>/images/EntityLogger-24.png'    }
Statistics              JavaClass { com.jaamsim.ProcessFlow.Statistics            }  Palette { 'Process Flow' }  DefaultDisplayModel { StatisticsIconModel      } IconFile { '<res>/images/Statistics-24.png'      }

SimEntity          Description { 'Basic 3D entity for use in a process flow type model.' }
SimShape           Description { 'Basic 2D entity for use in a process flow type model.' }
EntityLauncher     Description { 'Creates a clone of a prototype entity when clicked by the user.' }
EntityGenerator    Description { 'Creates clones of a prototype entity at specified intervals.' }
EntitySink         Description { 'Destroys any entity it receives.' }
Server             Description { 'Processes a received entity over a specified duration.' }
Queue              Description { 'Stores received entities until they are needed.' }
EntityConveyor     Description { 'Transports a received entity along a specified path at a fixed speed.' }
EntityDelay        Description { 'Delays a received entity by a specified duration.' }
Seize              Description { 'Seizes one or more units of a Resource.' }
Release            Description { 'Releases one or more units of a Resource.' }
EntityProcessor    Description { 'Seizes one or more Resources, delays the received entity by a specified duration, and releases the resources.' }
Assign             Description { 'Assigns new values to attributes.' }
Branch             Description { 'Directs a received entity to a selected destination.' }
Duplicate          Description { 'Sends copies of the received entity to a set of destinations.' }
Combine            Description { 'Matches entities from multiple queues. Either all the entities are passed on to the next component or just the one from the first queue. Entities not passed on are destroyed.' }
SetGraphics        Description { 'Changes the appearance of the received entity.' }
EntityGate         Description { 'Blocks received entities from progressing further until the EntityGate is opened by one or more Thresholds.' }
EntitySignal       Description { 'Opens or closes a specified SignalThreshold when an entity is received.' }
SignalThreshold    Description { 'Threshold that is opened and closed directly by an EntitySignal object.' }
Assemble           Description { 'Combines sub-components to create an assembled part.' }
EntityContainer    Description { 'An entity that can hold one or more other entities.' }
Pack               Description { 'Inserts entities in a new EntityContainer.' }
Unpack             Description { 'Removes all the entities from an EntityContainer which is subsequently destroyed.' }
AddTo              Description { 'Add entities to an existing EntityContainer.' }
RemoveFrom         Description { 'Removes some or all of the entities from an EntityContainer.' }
EntityLogger       Description { 'Records the outputs and state data for a generated entity in an output log.' }
Statistics         Description { 'Collects statistics from the received entities.' }

