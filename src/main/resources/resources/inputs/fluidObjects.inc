Define Palette { 'Fluid Objects' }

Define ObjectType {
                    Fluid
					FluidFlow
					FluidTank
					FluidPipe
					FluidCentrifugalPump
                    }

Fluid                   JavaClass { com.jaamsim.FluidObjects.Fluid                } Palette { 'Fluid Objects' }
FluidFlow               JavaClass { com.jaamsim.FluidObjects.FluidFlow            } Palette { 'Fluid Objects' }
FluidTank               JavaClass { com.jaamsim.FluidObjects.FluidTank            } Palette { 'Fluid Objects' }
FluidPipe               JavaClass { com.jaamsim.FluidObjects.FluidPipe            } Palette { 'Fluid Objects' }
FluidCentrifugalPump    JavaClass { com.jaamsim.FluidObjects.FluidCentrifugalPump } Palette { 'Fluid Objects' }

Define	ImageModel {
			FluidIconModel
			FluidFlowIconModel
			FluidCentrifugalPumpIconModel
		}

FluidIconModel                 ImageFile { '/resources/images/ConstantValue-256.png' }   Transparent { TRUE }
FluidFlowIconModel             ImageFile { '/resources/images/ConstantValue-256.png' }   Transparent { TRUE }
FluidCentrifugalPumpIconModel  ImageFile { '/resources/images/ConstantValue-256.png' }   Transparent { TRUE }

Fluid                 DefaultDisplayModel 	{ FluidIconModel }
FluidFlow             DefaultDisplayModel 	{ FluidFlowIconModel }
FluidTank             DefaultDisplayModel   { BarGauge2D }
FluidPipe             DefaultDisplayModel 	{ ScreenPointsModelDefault }
FluidCentrifugalPump  DefaultDisplayModel 	{ FluidCentrifugalPumpIconModel }
