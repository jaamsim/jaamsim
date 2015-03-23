Define ObjectType {
	SimEntity
	EntityGenerator
	EntitySink
	Server
	Queue
	EntityConveyor
	EntityDelay
	Resource
	Seize
	Release
	Assign
	Branch
	Duplicate
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

Define	ImageModel {
	SourceIconModel
	SinkIconModel
	ServerIconModel
	ResourceIconModel
	SeizeIconModel
	ReleaseIconModel
	AssignModel
	BranchIconModel
	DuplicateIconModel
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

SourceIconModel         ImageFile { '<res>/images/Source-256.png'       }  Transparent { TRUE }
SinkIconModel           ImageFile { '<res>/images/Sink-256.png'         }  Transparent { TRUE }
ServerIconModel         ImageFile { '<res>/images/Server-256.png'       }  Transparent { TRUE }
ResourceIconModel       ImageFile { '<res>/images/Resource-256.png'     }  Transparent { TRUE }
SeizeIconModel          ImageFile { '<res>/images/Seize-256.png'        }  Transparent { TRUE }
ReleaseIconModel        ImageFile { '<res>/images/Release-256.png'      }  Transparent { TRUE }
AssignModel             ImageFile { '<res>/images/Assign-256.png'       }  Transparent { TRUE }
BranchIconModel         ImageFile { '<res>/images/Branch-256.png'       }  Transparent { TRUE }
DuplicateIconModel      ImageFile { '<res>/images/Duplicate-256.png'    }  Transparent { TRUE }
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

SimEntity               JavaClass { com.jaamsim.BasicObjects.SimEntity            }  Palette { 'Process Flow' }  DefaultDisplayModel { Sphere                   } IconFile { '<res>/images/SimEntity-24.png'       }
EntityGenerator         JavaClass { com.jaamsim.BasicObjects.EntityGenerator      }  Palette { 'Process Flow' }  DefaultDisplayModel { SourceIconModel          } IconFile { '<res>/images/Source-24.png'          }
EntitySink              JavaClass { com.jaamsim.BasicObjects.EntitySink           }  Palette { 'Process Flow' }  DefaultDisplayModel { SinkIconModel            } IconFile { '<res>/images/Sink-24.png'            }
Server                  JavaClass { com.jaamsim.BasicObjects.Server               }  Palette { 'Process Flow' }  DefaultDisplayModel { ServerIconModel          } IconFile { '<res>/images/Server-24.png'          }
Queue                   JavaClass { com.jaamsim.BasicObjects.Queue                }  Palette { 'Process Flow' }  DefaultDisplayModel { Triangle                 } IconFile { '<res>/images/Queue-24.png'           }
EntityConveyor          JavaClass { com.jaamsim.BasicObjects.EntityConveyor       }  Palette { 'Process Flow' }  DefaultDisplayModel { ScreenPointsModelDefault } IconFile { '<res>/images/Line-24.png'            }
EntityDelay             JavaClass { com.jaamsim.BasicObjects.EntityDelay          }  Palette { 'Process Flow' }  DefaultDisplayModel { ScreenPointsModelDefault } IconFile { '<res>/images/Line-24.png'            }
Resource                JavaClass { com.jaamsim.BasicObjects.Resource             }  Palette { 'Process Flow' }  DefaultDisplayModel { ResourceIconModel        } IconFile { '<res>/images/Resource-24.png'        }
Seize                   JavaClass { com.jaamsim.BasicObjects.Seize                }  Palette { 'Process Flow' }  DefaultDisplayModel { SeizeIconModel           } IconFile { '<res>/images/Seize-24.png'           }
Release                 JavaClass { com.jaamsim.BasicObjects.Release              }  Palette { 'Process Flow' }  DefaultDisplayModel { ReleaseIconModel         } IconFile { '<res>/images/Release-24.png'         }
Assign                  JavaClass { com.jaamsim.BasicObjects.Assign               }  Palette { 'Process Flow' }  DefaultDisplayModel { AssignModel              } IconFile { '<res>/images/Assign-24.png'          }
Branch                  JavaClass { com.jaamsim.BasicObjects.Branch               }  Palette { 'Process Flow' }  DefaultDisplayModel { BranchIconModel          } IconFile { '<res>/images/Branch-24.png'          }
Duplicate               JavaClass { com.jaamsim.BasicObjects.Duplicate            }  Palette { 'Process Flow' }  DefaultDisplayModel { DuplicateIconModel       } IconFile { '<res>/images/Duplicate-24.png'       }
SetGraphics             JavaClass { com.jaamsim.BasicObjects.SetGraphics          }  Palette { 'Process Flow' }  DefaultDisplayModel { SetGraphicsIconModel     } IconFile { '<res>/images/SetGraphics-24.png'     }
EntityGate              JavaClass { com.jaamsim.BasicObjects.EntityGate           }  Palette { 'Process Flow' }  DefaultDisplayModel { EntityGateIconModel      } IconFile { '<res>/images/Gate-24.png'            }
EntitySignal            JavaClass { com.jaamsim.BasicObjects.EntitySignal         }  Palette { 'Process Flow' }  DefaultDisplayModel { EntitySignalIconModel    } IconFile { '<res>/images/Signal-24.png'          }
SignalThreshold         JavaClass { com.jaamsim.Thresholds.SignalThreshold        }  Palette { 'Process Flow' }  DefaultDisplayModel { Rectangle                } IconFile { '<res>/images/Threshold-24.png'       }
Assemble                JavaClass { com.jaamsim.BasicObjects.Assemble             }  Palette { 'Process Flow' }  DefaultDisplayModel { AssembleIconModel        } IconFile { '<res>/images/Assemble-24.png'        }
EntityContainer         JavaClass { com.jaamsim.BasicObjects.EntityContainer      }  Palette { 'Process Flow' }  DefaultDisplayModel { Rectangle                } IconFile { '<res>/images/EntityContainer-24.png' }
Pack                    JavaClass { com.jaamsim.BasicObjects.Pack                 }  Palette { 'Process Flow' }  DefaultDisplayModel { PackIconModel            } IconFile { '<res>/images/Pack-24.png'            }
Unpack                  JavaClass { com.jaamsim.BasicObjects.Unpack               }  Palette { 'Process Flow' }  DefaultDisplayModel { UnpackIconModel          } IconFile { '<res>/images/Unpack-24.png'          }
AddTo                   JavaClass { com.jaamsim.BasicObjects.AddTo                }  Palette { 'Process Flow' }  DefaultDisplayModel { AddToIconModel           } IconFile { '<res>/images/AddTo-24.png'           }
RemoveFrom              JavaClass { com.jaamsim.BasicObjects.RemoveFrom           }  Palette { 'Process Flow' }  DefaultDisplayModel { RemoveFromIconModel      } IconFile { '<res>/images/RemoveFrom-24.png'      }
EntityLogger            JavaClass { com.jaamsim.BasicObjects.EntityLogger         }  Palette { 'Process Flow' }  DefaultDisplayModel { EntityLoggerIconModel    } IconFile { '<res>/images/EntityLogger-24.png'    }
Statistics              JavaClass { com.jaamsim.BasicObjects.Statistics           }  Palette { 'Process Flow' }  DefaultDisplayModel { StatisticsIconModel      } IconFile { '<res>/images/Statistics-24.png'      }

SimEntity          Description { 'A basic entity for use in a process flow type model.' }
EntityGenerator    Description { 'Creates copies of a specified entity at specified intervals.' }
EntitySink         Description { 'Destroys any entity it receives.' }
Server             Description { 'Processes a received entity over a specified duration.' }
Queue              Description { 'Stores received entities in first-in-first-out order until they are needed.' }
EntityConveyor     Description { 'Transports a received entity along a specified path at a fixed speed.' }
EntityDelay        Description { 'Delays a received entity by a specified duration.' }
Resource           Description { 'Set of identical resource units that be seized and released by various process.' }
Seize              Description { 'Seizes one or more units of a Resource.' }
Release            Description { 'Releases one or more units of a Resource.' }
Assign             Description { 'Assigns new values to attributes.' }
Branch             Description { 'Directs received entities to an assortment of destinations.' }
Duplicate          Description { 'Sends copies of the received entity to various destinations.' }
SetGraphics        Description { 'Changes the appearance of the received entity.' }
EntityGate         Description { 'Blocks received entities from progressing further until the Gate is opened by a Signal.' }
EntitySignal       Description { 'On receipt of an entity, a specified Gate is opened or closed.' }
SignalThreshold    Description { 'Threshold that is opened and closed directly by an EntitySignal object.' }
Assemble           Description { 'Combines sub-components to create an assembled part.' }
EntityContainer    Description { 'Can be used to hold multiple entities.' }
Pack               Description { 'Insert entities to a new EntityContainer.' }
Unpack             Description { 'Removes all the entities from an EntityContainer.' }
AddTo              Description { 'Add entities to an existing EntityContainer.' }
RemoveFrom         Description { 'Removes some or all of the entities in an EntityContainer.' }
EntityLogger       Description { 'Records the output and state data for a generated entity in an output log.' }
Statistics         Description { 'Collects statistics from the received entities.' }

