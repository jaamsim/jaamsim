Define Palette { 'Fluid Objects' }

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

FluidIconModel                 ImageFile { '/resources/images/Fluid-256.png' }   Transparent { TRUE }
FluidFlowIconModel             ImageFile { '/resources/images/Flow-256.png' }   Transparent { TRUE }
FluidCentrifugalPumpIconModel  ImageFile { '/resources/images/Pump-256.png' }   Transparent { TRUE }

Fluid                 DefaultDisplayModel 	{ FluidIconModel }
FluidFlow             DefaultDisplayModel 	{ FluidFlowIconModel }
FluidFixedFlow        DefaultDisplayModel 	{ ScreenPointsModelDefault }
FluidTank             DefaultDisplayModel   { BarGauge2D }
FluidPipe             DefaultDisplayModel 	{ ScreenPointsModelDefault }
FluidCentrifugalPump  DefaultDisplayModel 	{ FluidCentrifugalPumpIconModel }
