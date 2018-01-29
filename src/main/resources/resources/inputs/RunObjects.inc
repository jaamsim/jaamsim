Define ObjectType {
			SequentialSampler
			BatchStatistics
			BatchRecorder
				}

SequentialSampler JavaClass { com.jaamsim.RunControlObjects.SequentialSampler } Palette { 'Run Objects' } DefaultDisplayModel { StatisticsIconModel }
BatchStatistics   JavaClass { com.jaamsim.RunControlObjects.BatchStatistics   } Palette { 'Run Objects' } DefaultDisplayModel { StatisticsIconModel }
BatchRecorder     JavaClass { com.jaamsim.RunControlObjects.BatchRecorder     } Palette { 'Run Objects' } DefaultDisplayModel { StatisticsIconModel }

SequentialSampler Description { 'Calculates confidence intervals for samples collected over a number of runs' }
BatchStatistics   Description { 'Calculates confidence intervals for samples collected over a single run'     }
BatchRecorder     Description { 'Calculates confidence intervals for samples collected over a single run'     }
