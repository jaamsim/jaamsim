Define ObjectType {
                    Fluid
					FluidFlow
					FluidFixedFlow
					FluidTank
					FluidPipe
					FluidCentrifugalPump
                    }

Fluid                   JavaClass { com.jaamsim.FluidObjects.Fluid                } Palette { 'Fluid Objects' } IconFile { '<res>/images/Fluid-24.png'     }
FluidFlow               JavaClass { com.jaamsim.FluidObjects.FluidFlow            } Palette { 'Fluid Objects' } IconFile { '<res>/images/Flow-24.png'      }
FluidFixedFlow          JavaClass { com.jaamsim.FluidObjects.FluidFixedFlow       } Palette { 'Fluid Objects' } IconFile { '<res>/images/Line-24.png'      }
FluidTank               JavaClass { com.jaamsim.FluidObjects.FluidTank            } Palette { 'Fluid Objects' } IconFile { '<res>/images/FluidTank-24.png' }
FluidPipe               JavaClass { com.jaamsim.FluidObjects.FluidPipe            } Palette { 'Fluid Objects' } IconFile { '<res>/images/Line-24.png'      }
FluidCentrifugalPump    JavaClass { com.jaamsim.FluidObjects.FluidCentrifugalPump } Palette { 'Fluid Objects' } IconFile { '<res>/images/Pump-24.png'      }

Define	IconModel  {
			FluidIconModel
			FluidFlowIconModel
			FluidCentrifugalPumpIconModel
		}

FluidIconModel                 ImageFile { '<res>/images/Fluid-256.png' }   Transparent { TRUE }
FluidFlowIconModel             ImageFile { '<res>/images/Flow-256.png' }   Transparent { TRUE }
FluidCentrifugalPumpIconModel  ImageFile { '<res>/images/Pump-256.png' }   Transparent { TRUE }

Define PolylineModel { FluidFixedFlowModel  FluidPipeModel }

Fluid                 DefaultDisplayModel 	{ FluidIconModel }
FluidFlow             DefaultDisplayModel 	{ FluidFlowIconModel }
FluidFixedFlow        DefaultDisplayModel 	{ FluidFixedFlowModel }
FluidTank             DefaultDisplayModel   { BarGauge2D }
FluidPipe             DefaultDisplayModel 	{ FluidPipeModel }
FluidCentrifugalPump  DefaultDisplayModel 	{ FluidCentrifugalPumpIconModel }

Fluid                Description { 'Defines a specific fluid and its properties.' }
FluidFlow            Description { 'Computed flow of a specified fluid between a source and a destination.' }
FluidFixedFlow       Description { 'Constant flow to/from a specified tank.' }
FluidTank            Description { 'Cylindrical vessel for storing fluid.' }
FluidPipe            Description { 'Cylindrical conduit for transporting fluid.' }
FluidCentrifugalPump Description { 'Type of pump with a rotating impeller.' }
