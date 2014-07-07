Define ObjectType {
					UniformDistribution
					TriangularDistribution
					NormalDistribution
					ExponentialDistribution
					ErlangDistribution
					GammaDistribution
					BetaDistribution
					WeibullDistribution
					LogNormalDistribution
					LogLogisticsDistribution
					DiscreteDistribution
					ContinuousDistribution
					EntitlementSelector
					RandomSelector
					BooleanSelector
                    }

UniformDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.UniformDistribution		} Palette { 'Probability Distributions' }
TriangularDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.TriangularDistribution		} Palette { 'Probability Distributions' }
NormalDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.NormalDistribution			} Palette { 'Probability Distributions' }
ExponentialDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.ExponentialDistribution	} Palette { 'Probability Distributions' }
ErlangDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.ErlangDistribution			} Palette { 'Probability Distributions' }
GammaDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.GammaDistribution			} Palette { 'Probability Distributions' }
BetaDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.BetaDistribution			} Palette { 'Probability Distributions' }
WeibullDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.WeibullDistribution		} Palette { 'Probability Distributions' }
LogNormalDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.LogNormalDistribution		} Palette { 'Probability Distributions' }
LogLogisticsDistribution	JavaClass { com.jaamsim.ProbabilityDistributions.LogLogisticDistribution	} Palette { 'Probability Distributions' }
DiscreteDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.DiscreteDistribution		} Palette { 'Probability Distributions' }
ContinuousDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.ContinuousDistribution		} Palette { 'Probability Distributions' }
EntitlementSelector			JavaClass { com.jaamsim.ProbabilityDistributions.EntitlementSelector		} Palette { 'Probability Distributions' }
RandomSelector				JavaClass { com.jaamsim.ProbabilityDistributions.RandomSelector				} Palette { 'Probability Distributions' }
BooleanSelector				JavaClass { com.jaamsim.ProbabilityDistributions.BooleanSelector			} Palette { 'Probability Distributions' }

Define	ImageModel {
					ProbabilityDistributionIconModel
					NormalDistributionIconModel
					ExponentialDistributionIconModel
					UniformDistributionIconModel
					TriangularDistributionIconModel
					ErlangDistributionIconModel
					DiscreteDistributionIconModel
					ContinuousDistributionIconModel
					SelectorIconModel
					}

ProbabilityDistributionIconModel	ImageFile { '<res>/images/ErlangDist-256.png'    }   Transparent { TRUE }
NormalDistributionIconModel			ImageFile { '<res>/images/Norm Dist-256.png'     }   Transparent { TRUE }
ExponentialDistributionIconModel	ImageFile { '<res>/images/Exp Dist-256.png'      }   Transparent { TRUE }
UniformDistributionIconModel		ImageFile { '<res>/images/UniformDist-256.png'   }   Transparent { TRUE }
TriangularDistributionIconModel		ImageFile { '<res>/images/TriangleDist-256.png'  }   Transparent { TRUE }
ErlangDistributionIconModel			ImageFile { '<res>/images/ErlangDist-256.png'    }   Transparent { TRUE }
DiscreteDistributionIconModel       ImageFile { '<res>/images/DiscreteDist-256.png'  }   Transparent { TRUE }
ContinuousDistributionIconModel     ImageFile { '<res>/images/ContinuousDist-256.png' }  Transparent { TRUE }
SelectorIconModel                   ImageFile { '<res>/images/Selector-256.png'      }   Transparent { TRUE }

UniformDistribution			DefaultDisplayModel { UniformDistributionIconModel     }
TriangularDistribution		DefaultDisplayModel { TriangularDistributionIconModel  }
NormalDistribution			DefaultDisplayModel { NormalDistributionIconModel      }
ExponentialDistribution		DefaultDisplayModel { ExponentialDistributionIconModel }
ErlangDistribution			DefaultDisplayModel { ErlangDistributionIconModel      }
GammaDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
BetaDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
WeibullDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
LogNormalDistribution		DefaultDisplayModel { ProbabilityDistributionIconModel }
LogLogisticsDistribution	DefaultDisplayModel { ProbabilityDistributionIconModel }
DiscreteDistribution		DefaultDisplayModel { DiscreteDistributionIconModel    }
ContinuousDistribution		DefaultDisplayModel { ContinuousDistributionIconModel  }
EntitlementSelector			DefaultDisplayModel { SelectorIconModel                }
RandomSelector				DefaultDisplayModel { SelectorIconModel                }
BooleanSelector				DefaultDisplayModel { SelectorIconModel                }

UniformDistribution      Description { 'Generates random numbers from a uniform probability density function.' }
TriangularDistribution   Description { 'Generates random numbers from a triangular probability density function.' }
NormalDistribution       Description { 'Generates random numbers from a normal probability density function.' }
ExponentialDistribution  Description { 'Generates random numbers from a negative exponential probability density function.' }
ErlangDistribution       Description { 'Generates random numbers from an Erlang probability density function.' }
GammaDistribution        Description { 'Generates random numbers from a gamma probability density function.' }
BetaDistribution        Description { 'Generates random numbers from a beta probability density function.' }
WeibullDistribution      Description { 'Generates random numbers from a Weibull probability density function.' }
LogNormalDistribution    Description { 'Generates random numbers from a log-normal probability density function.' }
LogLogisticsDistribution Description { 'Generates random numbers from a log-logistical probability density function.' }
DiscreteDistribution     Description { 'Generates random numbers from a user-specified set of values and probabilities.' }
ContinuousDistribution   Description { 'Generates random numbers from a user-specified probability density function.' }
EntitlementSelector      Description { 'Selects on the basis of entitlement from a given set of entities and proportions.' }
RandomSelector           Description { 'Randomly selects from a given set of entities and probabilities.' }
BooleanSelector          Description { 'Randomly selects true/false with a user-selectable probability of true.' }
