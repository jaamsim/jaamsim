Define ObjectType {
					UniformDistribution
					TriangularDistribution
					NormalDistribution
					ExponentialDistribution
					NonStatExponentialDist
					ErlangDistribution
					GammaDistribution
					BetaDistribution
					WeibullDistribution
					LogNormalDistribution
					LogLogisticDistribution
					PoissonDistribution
					DiscreteDistribution
					ContinuousDistribution
					BooleanSelector
                    }

UniformDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.UniformDistribution		} Palette { 'Probability Distributions' } IconFile { '<res>/images/UniformDist-24.png'    }
TriangularDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.TriangularDistribution		} Palette { 'Probability Distributions' } IconFile { '<res>/images/TriangleDist-24.png'   }
NormalDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.NormalDistribution			} Palette { 'Probability Distributions' } IconFile { '<res>/images/Norm Dist-24.png'      }
ExponentialDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.ExponentialDistribution	} Palette { 'Probability Distributions' } IconFile { '<res>/images/Exp Dist-24.png'       }
NonStatExponentialDist		JavaClass { com.jaamsim.ProbabilityDistributions.NonStatExponentialDist		} Palette { 'Probability Distributions' } IconFile { '<res>/images/NSExpoDist-24.png'     }
ErlangDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.ErlangDistribution			} Palette { 'Probability Distributions' } IconFile { '<res>/images/ErlangDist-24.png'     }
GammaDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.GammaDistribution			} Palette { 'Probability Distributions' } IconFile { '<res>/images/ErlangDist-24.png'     }
BetaDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.BetaDistribution			} Palette { 'Probability Distributions' } IconFile { '<res>/images/ErlangDist-24.png'     }
WeibullDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.WeibullDistribution		} Palette { 'Probability Distributions' } IconFile { '<res>/images/ErlangDist-24.png'     }
LogNormalDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.LogNormalDistribution		} Palette { 'Probability Distributions' } IconFile { '<res>/images/ErlangDist-24.png'     }
LogLogisticDistribution	    JavaClass { com.jaamsim.ProbabilityDistributions.LogLogisticDistribution	} Palette { 'Probability Distributions' } IconFile { '<res>/images/ErlangDist-24.png'     }
PoissonDistribution			JavaClass { com.jaamsim.ProbabilityDistributions.PoissonDistribution		} Palette { 'Probability Distributions' } IconFile { '<res>/images/DiscreteDist-24.png'   }
DiscreteDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.DiscreteDistribution		} Palette { 'Probability Distributions' } IconFile { '<res>/images/DiscreteDist-24.png'   }
ContinuousDistribution		JavaClass { com.jaamsim.ProbabilityDistributions.ContinuousDistribution		} Palette { 'Probability Distributions' } IconFile { '<res>/images/ContinuousDist-24.png' }
BooleanSelector				JavaClass { com.jaamsim.ProbabilityDistributions.BooleanSelector			} Palette { 'Probability Distributions' } IconFile { '<res>/images/Selector-24.png'       }

Define	IconModel  {
					ProbabilityDistributionIconModel
					NormalDistributionIconModel
					ExponentialDistributionIconModel
					NonStatExponentialDistIconModel
					UniformDistributionIconModel
					TriangularDistributionIconModel
					ErlangDistributionIconModel
					DiscreteDistributionIconModel
					ContinuousDistributionIconModel
					BooleanSelectorIconModel
					}

ProbabilityDistributionIconModel	ImageFile { '<res>/images/ErlangDist-256.png'    }   Transparent { TRUE }
NormalDistributionIconModel			ImageFile { '<res>/images/Norm Dist-256.png'     }   Transparent { TRUE }
ExponentialDistributionIconModel	ImageFile { '<res>/images/Exp Dist-256.png'      }   Transparent { TRUE }
NonStatExponentialDistIconModel		ImageFile { '<res>/images/NSExpoDist-256.png'    }   Transparent { TRUE }
UniformDistributionIconModel		ImageFile { '<res>/images/UniformDist-256.png'   }   Transparent { TRUE }
TriangularDistributionIconModel		ImageFile { '<res>/images/TriangleDist-256.png'  }   Transparent { TRUE }
ErlangDistributionIconModel			ImageFile { '<res>/images/ErlangDist-256.png'    }   Transparent { TRUE }
DiscreteDistributionIconModel       ImageFile { '<res>/images/DiscreteDist-256.png'  }   Transparent { TRUE }
ContinuousDistributionIconModel     ImageFile { '<res>/images/ContinuousDist-256.png' }  Transparent { TRUE }
BooleanSelectorIconModel            ImageFile { '<res>/images/Selector-256.png'      }   Transparent { TRUE }

UniformDistribution			DefaultDisplayModel { UniformDistributionIconModel     }
TriangularDistribution		DefaultDisplayModel { TriangularDistributionIconModel  }
NormalDistribution			DefaultDisplayModel { NormalDistributionIconModel      }
ExponentialDistribution		DefaultDisplayModel { ExponentialDistributionIconModel }
NonStatExponentialDist		DefaultDisplayModel { NonStatExponentialDistIconModel  }
ErlangDistribution			DefaultDisplayModel { ErlangDistributionIconModel      }
GammaDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
BetaDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
WeibullDistribution			DefaultDisplayModel { ProbabilityDistributionIconModel }
LogNormalDistribution		DefaultDisplayModel { ProbabilityDistributionIconModel }
LogLogisticDistribution	    DefaultDisplayModel { ProbabilityDistributionIconModel }
PoissonDistribution			DefaultDisplayModel { DiscreteDistributionIconModel    }
DiscreteDistribution		DefaultDisplayModel { DiscreteDistributionIconModel    }
ContinuousDistribution		DefaultDisplayModel { ContinuousDistributionIconModel  }
BooleanSelector				DefaultDisplayModel { BooleanSelectorIconModel         }

UniformDistribution      Description { 'Generates random numbers from a uniform probability density function.' }
TriangularDistribution   Description { 'Generates random numbers from a triangular probability density function.' }
NormalDistribution       Description { 'Generates random numbers from a normal probability density function.' }
ExponentialDistribution  Description { 'Generates random numbers from a negative exponential probability density function.' }
NonStatExponentialDist   Description { 'Generates random numbers from a negative exponential probability density function with an average rate that varies with time.' }
ErlangDistribution       Description { 'Generates random numbers from an Erlang probability density function.' }
GammaDistribution        Description { 'Generates random numbers from a gamma probability density function.' }
BetaDistribution        Description { 'Generates random numbers from a beta probability density function.' }
WeibullDistribution      Description { 'Generates random numbers from a Weibull probability density function.' }
LogNormalDistribution    Description { 'Generates random numbers from a log-normal probability density function.' }
LogLogisticDistribution  Description { 'Generates random numbers from a log-logistic probability density function.' }
PoissonDistribution      Description { 'Generates random numbers from a Poisson distribution.' }
DiscreteDistribution     Description { 'Generates random numbers from a user-specified set of values and probabilities.' }
ContinuousDistribution   Description { 'Generates random numbers from a user-specified probability density function.' }
BooleanSelector          Description { 'Randomly selects true/false with a user-selectable probability of true.' }
