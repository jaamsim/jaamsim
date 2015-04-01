Define ObjectType {
                    Group
                    ScriptEntity
                    Output
                    BooleanIndicator
                    TimeSeries
                    TimeSeriesThreshold
                    ExpressionThreshold
					EntitlementSelector
					ExpressionEntity
                  }

Define	ImageModel {
			TimeSeriesIconModel
			SelectorIconModel
			}
			
TimeSeriesIconModel     ImageFile { '<res>/images/TimeSeries-256.png' }  Transparent { TRUE }
SelectorIconModel       ImageFile { '<res>/images/Selector-256.png'  }   Transparent { TRUE }

Group                   JavaClass { com.jaamsim.basicsim.Group                          } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
ScriptEntity            JavaClass { com.jaamsim.basicsim.ScriptEntity                   } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
Output                  JavaClass { com.jaamsim.basicsim.OutputMethod                   } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}
BooleanIndicator        JavaClass { com.jaamsim.BasicObjects.BooleanIndicator           } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	} DefaultDisplayModel { Circle              }
TimeSeries              JavaClass { com.jaamsim.Samples.TimeSeries                      } Palette { 'Basic Objects' } DefaultDisplayModel { TimeSeriesIconModel } IconFile { '<res>/images/TimeSeries-24.png' }
TimeSeriesThreshold	    JavaClass { com.jaamsim.Thresholds.TimeSeriesThreshold		    } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle           } IconFile { '<res>/images/Threshold-24.png'  }
ExpressionThreshold	    JavaClass { com.jaamsim.Thresholds.ExpressionThreshold		    } Palette { 'Basic Objects' } DefaultDisplayModel { Rectangle           } IconFile { '<res>/images/Threshold-24.png'  }
EntitlementSelector		JavaClass { com.jaamsim.BasicObjects.EntitlementSelector		} Palette { 'Basic Objects' } DefaultDisplayModel { SelectorIconModel   } IconFile { '<res>/images/Selector-24.png'   }
ExpressionEntity              JavaClass { com.jaamsim.basicsim.ExpressionEntity                 } Palette { 'Basic Objects' } DragAndDrop 			{ FALSE 	}

Group              Description { 'Collection of entities used as an convenience for specifying model inputs.' }
ScriptEntity       Description { 'Executes a series of commands in simulated time. Used primarily for recording videos.' }
Output             Description { '???' }
BooleanIndicator   Description { 'Circular entity that changes color to indicate TRUE and FALSE' }
TimeSeries         Description { 'Provides a floating point number that changes in simulated time following a series of input values.' }
TimeSeriesThreshold Description { 'Specifies a range of values from a TimeSeries for which an activity is permitted.' }
ExpressionThreshold Description { 'Specifies a logical condition for which an activity is permitted.' }
EntitlementSelector Description { 'Selects an index on the basis of entitlement from a given set of proportions.' }
