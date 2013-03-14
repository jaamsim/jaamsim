Define Palette { 'Probability Distributions' }

Define ObjectType {
					UniformDistribution
					TriangularDistribution
					NormalDistribution
					ExponentialDistribution
					ErlangDistribution
					GammaDistribution
					WeibullDistribution
					LogNormalDistribution
					LogLogisticsDistribution
					DiscreteDistribution
					ContinuousDistribution
					EntitlementSelector
					RandomSelector
                    }

UniformDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.UniformDistribution		} Palette { 'Probability Distributions' }
TriangularDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.TriangularDistribution		} Palette { 'Probability Distributions' }
NormalDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.NormalDistribution			} Palette { 'Probability Distributions' }
ExponentialDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.ExponentialDistribution	} Palette { 'Probability Distributions' }
ErlangDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.ErlangDistribution			} Palette { 'Probability Distributions' }
GammaDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.GammaDistribution			} Palette { 'Probability Distributions' }
WeibullDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.WeibullDistribution		} Palette { 'Probability Distributions' }
LogNormalDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.LogNormalDistribution		} Palette { 'Probability Distributions' }
LogLogisticsDistribution	JavaClass { com.jaamsim.ProbabilityDistributions.LogLogisticDistribution	} Palette { 'Probability Distributions' }
DiscreteDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.DiscreteDistribution		} Palette { 'Probability Distributions' }
ContinuousDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.ContinuousDistribution		} Palette { 'Probability Distributions' }
EntitlementSelector			JavaClass { com.jaamsim.ProbabilityDistributions.EntitlementSelector		} Palette { 'Probability Distributions' }
RandomSelector				JavaClass { com.jaamsim.ProbabilityDistributions.RandomSelector				} Palette { 'Probability Distributions' }

Define	ImageModel {
					ProbabilityDistributionIconModel
					}

ProbabilityDistributionIconModel	ImageFile { '/resources/images/ConstantValue-256.png' }   Transparent { TRUE }

UniformDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
TriangularDistribution		DefaultDisplayModel { ProbabilityDistributionIconModel }
NormalDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
ExponentialDistribution		DefaultDisplayModel { ProbabilityDistributionIconModel }
ErlangDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
GammaDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
WeibullDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
LogNormalDistribution		DefaultDisplayModel { ProbabilityDistributionIconModel }
LogLogisticsDistribution	DefaultDisplayModel { ProbabilityDistributionIconModel }
DiscreteDistribution		DefaultDisplayModel { ProbabilityDistributionIconModel }
ContinuousDistribution		DefaultDisplayModel { ProbabilityDistributionIconModel }
EntitlementSelector			DefaultDisplayModel { ProbabilityDistributionIconModel }
RandomSelector				DefaultDisplayModel { ProbabilityDistributionIconModel }
