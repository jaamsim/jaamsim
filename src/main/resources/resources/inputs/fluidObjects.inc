Define ObjectType {
                    Fluid
					FluidFlow
					FluidFixedFlow
					FluidTank
					FluidPipe
					FluidCentrifugalPump
                    }

Fluid                   JavaClass { com.jaamsim.FluidObjects.Fluid                } Palette { 'Fluid Objects' }
FluidFlow               JavaClass { com.jaamsim.FluidObjects.FluidFlow            } Palette { 'Fluid Objects' }
FluidFixedFlow          JavaClass { com.jaamsim.FluidObjects.FluidFixedFlow       } Palette { 'Fluid Objects' }
FluidTank               JavaClass { com.jaamsim.FluidObjects.FluidTank            } Palette { 'Fluid Objects' }
FluidPipe               JavaClass { com.jaamsim.FluidObjects.FluidPipe            } Palette { 'Fluid Objects' }
FluidCentrifugalPump    JavaClass { com.jaamsim.FluidObjects.FluidCentrifugalPump } Palette { 'Fluid Objects' }

Define	ImageModel {
			FluidIconModel
			FluidFlowIconModel
			FluidCentrifugalPumpIconModel
		}

FluidIconModel                 ImageFile { '<res>/images/Fluid-256.png' }   Transparent { TRUE }
FluidFlowIconModel             ImageFile { '<res>/images/Flow-256.png' }   Transparent { TRUE }
FluidCentrifugalPumpIconModel  ImageFile { '<res>/images/Pump-256.png' }   Transparent { TRUE }

Fluid                 DefaultDisplayModel 	{ FluidIconModel }
FluidFlow             DefaultDisplayModel 	{ FluidFlowIconModel }
FluidFixedFlow        DefaultDisplayModel 	{ ScreenPointsModelDefault }
FluidTank             DefaultDisplayModel   { BarGauge2D }
FluidPipe             DefaultDisplayModel 	{ ScreenPointsModelDefault }
FluidCentrifugalPump  DefaultDisplayModel 	{ FluidCentrifugalPumpIconModel }

Fluid                Description { 'Defines a specific fluid and its properties.' }
FluidFlow            Description { 'Computed flow of a specified fluid between a source and a destination.' }
FluidFixedFlow       Description { 'Constant flow to/from a specified tank.' }
FluidTank            Description { 'Cylindrical vessel for storing fluid.' }
FluidPipe            Description { 'Cylindrical conduit for transporting fluid.' }
FluidCentrifugalPump Description { 'Type of pump with a rotating impeller.' }
