Define ObjectType {
					WeightedSum
					Polynomial
					Integrator
					Differentiator
					PIDController
					Lag
					MovingAverage
					SineWave
					SquareWave
					UnitDelay
                    }

WeightedSum             JavaClass { com.jaamsim.CalculationObjects.WeightedSum         } Palette { 'Calculation Objects' } IconFile { '<res>/images/WeightedSum-24.png'    }
Polynomial              JavaClass { com.jaamsim.CalculationObjects.Polynomial          } Palette { 'Calculation Objects' } IconFile { '<res>/images/Polynomial-24.png'     }
Integrator              JavaClass { com.jaamsim.CalculationObjects.Integrator          } Palette { 'Calculation Objects' } IconFile { '<res>/images/Integrator-24.png'     }
Differentiator          JavaClass { com.jaamsim.CalculationObjects.Differentiator      } Palette { 'Calculation Objects' } IconFile { '<res>/images/Differentiator-24.png' }
PIDController           JavaClass { com.jaamsim.CalculationObjects.PIDController       } Palette { 'Calculation Objects' } IconFile { '<res>/images/PID-24.png'            }
Lag                     JavaClass { com.jaamsim.CalculationObjects.Lag                 } Palette { 'Calculation Objects' } IconFile { '<res>/images/LAG-24.png'            }
MovingAverage           JavaClass { com.jaamsim.CalculationObjects.MovingAverage       } Palette { 'Calculation Objects' } IconFile { '<res>/images/AVG-24.png'            }
SineWave                JavaClass { com.jaamsim.CalculationObjects.SineWave            } Palette { 'Calculation Objects' } IconFile { '<res>/images/SineWave-24.png'       }
SquareWave              JavaClass { com.jaamsim.CalculationObjects.SquareWave          } Palette { 'Calculation Objects' } IconFile { '<res>/images/SquareWave-24.png'     }
UnitDelay               JavaClass { com.jaamsim.CalculationObjects.UnitDelay           } Palette { 'Calculation Objects' } IconFile { '<res>/images/UnitDelay-24.png'      }

Define	IconModel  {
			WeightedSumIconModel
			PolynomialIconModel
			IntegratorIconModel
			DifferentiatorIconModel
			PIDControllerIconModel
			LagIconModel
			MovingAverageIconModel
			SineWaveIconModel
			SquareWaveIconModel
			UnitDelayIconModel
		}

WeightedSumIconModel     ImageFile { '<res>/images/WeightedSum-256.png' }     Transparent { TRUE }
PolynomialIconModel      ImageFile { '<res>/images/Polynomial-256.png' }      Transparent { TRUE }
IntegratorIconModel      ImageFile { '<res>/images/Integrator-256.png' }      Transparent { TRUE }
DifferentiatorIconModel  ImageFile { '<res>/images/Differentiator-256.png' }  Transparent { TRUE }
PIDControllerIconModel   ImageFile { '<res>/images/PID-256.png' }             Transparent { TRUE }
LagIconModel             ImageFile { '<res>/images/LAG-256.png' }             Transparent { TRUE }
MovingAverageIconModel   ImageFile { '<res>/images/AVG-256.png' }             Transparent { TRUE }
SineWaveIconModel        ImageFile { '<res>/images/SineWave-256.png' }        Transparent { TRUE }
SquareWaveIconModel      ImageFile { '<res>/images/SquareWave-256.png' }      Transparent { TRUE }
UnitDelayIconModel       ImageFile { '<res>/images/UnitDelay-256.png' }       Transparent { TRUE }

WeightedSum             DefaultDisplayModel 	{ WeightedSumIconModel }
Polynomial              DefaultDisplayModel 	{ PolynomialIconModel }
Integrator              DefaultDisplayModel 	{ IntegratorIconModel }
Differentiator          DefaultDisplayModel 	{ DifferentiatorIconModel }
PIDController           DefaultDisplayModel 	{ PIDControllerIconModel }
Lag                     DefaultDisplayModel 	{ LagIconModel }
MovingAverage           DefaultDisplayModel 	{ MovingAverageIconModel }
SineWave                DefaultDisplayModel 	{ SineWaveIconModel }
SquareWave              DefaultDisplayModel 	{ SquareWaveIconModel }
UnitDelay               DefaultDisplayModel 	{ UnitDelayIconModel }

WeightedSum      Description { 'Calculates the weighted sum of the input values.' }
Polynomial       Description { 'Evaluates a polynomial function of the input value.' }
Integrator       Description { 'Integrates the input value over time.' }
Differentiator   Description { 'Differentiates the input value over time.' }
PIDController    Description { 'Proportional-Integral-Differential controller.' }
Lag              Description { 'Calculates the LAG operation for the input value.' }
MovingAverage    Description { 'Calculates a moving average of the input value over a specified range of time.' }
SineWave         Description { 'Generates a sinusoidal wave.' }
SquareWave       Description { 'Generates a square wave.' }
UnitDelay        Description { 'Delays the input value by one Controller time step.' }
