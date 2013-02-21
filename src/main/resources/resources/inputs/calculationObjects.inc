Define Palette { 'Calculation Objects' }

Define ObjectType {
                    Controller
					ConstantValue
					WeightedSum
					Integrator
					Differentiator
					SineWave
					SquareWave
                    }

Controller              JavaClass { com.jaamsim.CalculationObjects.Controller          } Palette { 'Calculation Objects' }
ConstantValue           JavaClass { com.jaamsim.CalculationObjects.ConstantValue       } Palette { 'Calculation Objects' }
WeightedSum             JavaClass { com.jaamsim.CalculationObjects.WeightedSum         } Palette { 'Calculation Objects' }
Integrator              JavaClass { com.jaamsim.CalculationObjects.Integrator          } Palette { 'Calculation Objects' }
Differentiator          JavaClass { com.jaamsim.CalculationObjects.Differentiator      } Palette { 'Calculation Objects' }
SineWave                JavaClass { com.jaamsim.CalculationObjects.SineWave            } Palette { 'Calculation Objects' }
SquareWave              JavaClass { com.jaamsim.CalculationObjects.SquareWave          } Palette { 'Calculation Objects' }

Define	ImageModel {
			ControllerIconModel
			ConstantValueIconModel
			WeightedSumIconModel
			IntegratorIconModel
			DifferentiatorIconModel
			SineWaveIconModel
			SquareWaveIconModel
		}

ControllerIconModel      ImageFile { '/resources/images/Controller-256.png' }      Transparent { TRUE }
ConstantValueIconModel   ImageFile { '/resources/images/ConstantValue-256.png' }   Transparent { TRUE }
WeightedSumIconModel     ImageFile { '/resources/images/WeightedSum-256.png' }     Transparent { TRUE }
IntegratorIconModel      ImageFile { '/resources/images/Integrator-256.png' }      Transparent { TRUE }
DifferentiatorIconModel  ImageFile { '/resources/images/Differentiator-256.png' }  Transparent { TRUE }
SineWaveIconModel        ImageFile { '/resources/images/SineWave-256.png' }        Transparent { TRUE }
SquareWaveIconModel      ImageFile { '/resources/images/SquareWave-256.png' }      Transparent { TRUE }

Controller              DefaultDisplayModel 	{ ControllerIconModel }
ConstantValue           DefaultDisplayModel 	{ ConstantValueIconModel    }
WeightedSum             DefaultDisplayModel 	{ WeightedSumIconModel    }
Integrator              DefaultDisplayModel 	{ IntegratorIconModel }
Differentiator          DefaultDisplayModel 	{ DifferentiatorIconModel }
SineWave                DefaultDisplayModel 	{ SineWaveIconModel    }
SquareWave              DefaultDisplayModel 	{ SquareWaveIconModel    }
