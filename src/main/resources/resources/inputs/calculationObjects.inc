Define ObjectType {
                    Controller
					WeightedSum
					Polynomial
					Integrator
					Differentiator
					PIDController
					Lag
					MovingAverage
					SineWave
					SquareWave
                    }

Controller              JavaClass { com.jaamsim.CalculationObjects.Controller          } Palette { 'Calculation Objects' }
WeightedSum             JavaClass { com.jaamsim.CalculationObjects.WeightedSum         } Palette { 'Calculation Objects' }
Polynomial              JavaClass { com.jaamsim.CalculationObjects.Polynomial          } Palette { 'Calculation Objects' }
Integrator              JavaClass { com.jaamsim.CalculationObjects.Integrator          } Palette { 'Calculation Objects' }
Differentiator          JavaClass { com.jaamsim.CalculationObjects.Differentiator      } Palette { 'Calculation Objects' }
PIDController           JavaClass { com.jaamsim.CalculationObjects.PIDController       } Palette { 'Calculation Objects' }
Lag                     JavaClass { com.jaamsim.CalculationObjects.Lag                 } Palette { 'Calculation Objects' }
MovingAverage           JavaClass { com.jaamsim.CalculationObjects.MovingAverage       } Palette { 'Calculation Objects' }
SineWave                JavaClass { com.jaamsim.CalculationObjects.SineWave            } Palette { 'Calculation Objects' }
SquareWave              JavaClass { com.jaamsim.CalculationObjects.SquareWave          } Palette { 'Calculation Objects' }

Define	ImageModel {
			ControllerIconModel
			WeightedSumIconModel
			PolynomialIconModel
			IntegratorIconModel
			DifferentiatorIconModel
			PIDControllerIconModel
			LagIconModel
			MovingAverageIconModel
			SineWaveIconModel
			SquareWaveIconModel
		}

ControllerIconModel      ImageFile { '<res>/images/Controller-256.png' }      Transparent { TRUE }
WeightedSumIconModel     ImageFile { '<res>/images/WeightedSum-256.png' }     Transparent { TRUE }
PolynomialIconModel      ImageFile { '<res>/images/Polynomial-256.png' }      Transparent { TRUE }
IntegratorIconModel      ImageFile { '<res>/images/Integrator-256.png' }      Transparent { TRUE }
DifferentiatorIconModel  ImageFile { '<res>/images/Differentiator-256.png' }  Transparent { TRUE }
PIDControllerIconModel   ImageFile { '<res>/images/PID-256.png' }             Transparent { TRUE }
LagIconModel             ImageFile { '<res>/images/LAG-256.png' }             Transparent { TRUE }
MovingAverageIconModel   ImageFile { '<res>/images/AVG-256.png' }             Transparent { TRUE }
SineWaveIconModel        ImageFile { '<res>/images/SineWave-256.png' }        Transparent { TRUE }
SquareWaveIconModel      ImageFile { '<res>/images/SquareWave-256.png' }      Transparent { TRUE }

Controller              DefaultDisplayModel 	{ ControllerIconModel }
WeightedSum             DefaultDisplayModel 	{ WeightedSumIconModel }
Polynomial              DefaultDisplayModel 	{ PolynomialIconModel }
Integrator              DefaultDisplayModel 	{ IntegratorIconModel }
Differentiator          DefaultDisplayModel 	{ DifferentiatorIconModel }
PIDController           DefaultDisplayModel 	{ PIDControllerIconModel }
Lag                     DefaultDisplayModel 	{ LagIconModel }
MovingAverage           DefaultDisplayModel 	{ MovingAverageIconModel }
SineWave                DefaultDisplayModel 	{ SineWaveIconModel }
SquareWave              DefaultDisplayModel 	{ SquareWaveIconModel }

Controller       Description { 'Signals the updating of each component in the specified sequence.' }
WeightedSum      Description { 'Calculates the weighted sum of the input values.' }
Polynomial       Description { 'Evaluates a polynomial function of the input value.' }
Integrator       Description { 'Integrates the input value over time.' }
Differentiator   Description { 'Differentiates the input value over time.' }
PIDController    Description { 'Proportional-Integral-Differential controller.' }
Lag              Description { 'Calculates the LAG operation for the input value.' }
MovingAverage    Description { 'Calculates a moving average of the input value over a specified range of time.' }
SineWave         Description { 'Generates a sinusoidal wave.' }
SquareWave       Description { 'Generates a square wave.' }
