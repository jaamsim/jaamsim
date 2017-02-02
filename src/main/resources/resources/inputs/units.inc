# Input file for unit conversion

Define ObjectType  { TimeUnit
                     DistanceUnit
                     SpeedUnit
                     AccelerationUnit
                     MassUnit
                     MassFlowUnit
                     VolumeUnit
                     VolumeFlowUnit
                     AngleUnit
                     AngularSpeedUnit
                     EnergyUnit
                     EnergyDensityUnit
                     SpecificEnergyUnit
                     PowerUnit
                     CostUnit
                     CostRateUnit
                     LinearDensityUnit
                     LinearDensityVolumeUnit
                     DensityUnit
                     PressureUnit
                     ViscosityUnit
                     AreaUnit
                     DimensionlessUnit
                     RateUnit
                     }

TimeUnit            JavaClass { com.jaamsim.units.TimeUnit            } Palette { Units } DragAndDrop { FALSE }
DistanceUnit        JavaClass { com.jaamsim.units.DistanceUnit        } Palette { Units } DragAndDrop { FALSE }
SpeedUnit           JavaClass { com.jaamsim.units.SpeedUnit           } Palette { Units } DragAndDrop { FALSE }
AccelerationUnit    JavaClass { com.jaamsim.units.AccelerationUnit    } Palette { Units } DragAndDrop { FALSE }
MassUnit            JavaClass { com.jaamsim.units.MassUnit            } Palette { Units } DragAndDrop { FALSE }
MassFlowUnit        JavaClass { com.jaamsim.units.MassFlowUnit        } Palette { Units } DragAndDrop { FALSE }
VolumeUnit          JavaClass { com.jaamsim.units.VolumeUnit          } Palette { Units } DragAndDrop { FALSE }
VolumeFlowUnit      JavaClass { com.jaamsim.units.VolumeFlowUnit      } Palette { Units } DragAndDrop { FALSE }
AngleUnit           JavaClass { com.jaamsim.units.AngleUnit           } Palette { Units } DragAndDrop { FALSE }
AngularSpeedUnit    JavaClass { com.jaamsim.units.AngularSpeedUnit    } Palette { Units } DragAndDrop { FALSE }
EnergyUnit          JavaClass { com.jaamsim.units.EnergyUnit          } Palette { Units } DragAndDrop { FALSE }
EnergyDensityUnit   JavaClass { com.jaamsim.units.EnergyDensityUnit   } Palette { Units } DragAndDrop { FALSE }
SpecificEnergyUnit  JavaClass { com.jaamsim.units.SpecificEnergyUnit  } Palette { Units } DragAndDrop { FALSE }
PowerUnit           JavaClass { com.jaamsim.units.PowerUnit           } Palette { Units } DragAndDrop { FALSE }
CostUnit            JavaClass { com.jaamsim.units.CostUnit            } Palette { Units } DragAndDrop { FALSE }
CostRateUnit        JavaClass { com.jaamsim.units.CostRateUnit        } Palette { Units } DragAndDrop { FALSE }
LinearDensityUnit   JavaClass { com.jaamsim.units.LinearDensityUnit   } Palette { Units } DragAndDrop { FALSE }
LinearDensityVolumeUnit   JavaClass { com.jaamsim.units.LinearDensityVolumeUnit   } Palette { Units } DragAndDrop { FALSE }
DensityUnit         JavaClass { com.jaamsim.units.DensityUnit         } Palette { Units } DragAndDrop { FALSE }
PressureUnit        JavaClass { com.jaamsim.units.PressureUnit        } Palette { Units } DragAndDrop { FALSE }
ViscosityUnit       JavaClass { com.jaamsim.units.ViscosityUnit       } Palette { Units } DragAndDrop { FALSE }
AreaUnit            JavaClass { com.jaamsim.units.AreaUnit            } Palette { Units } DragAndDrop { FALSE }
DimensionlessUnit   JavaClass { com.jaamsim.units.DimensionlessUnit   } Palette { Units } DragAndDrop { FALSE }
RateUnit            JavaClass { com.jaamsim.units.RateUnit            } Palette { Units } DragAndDrop { FALSE }

TimeUnit                Description { 'Unit of time. Default = seconds (s).' }
DistanceUnit            Description { 'Unit of length. Default = meters (m).' }
SpeedUnit               Description { 'Unit of velocity. Default = meters per second (m/s).' }
AccelerationUnit        Description { 'Unit of acceleration. Default = meters per second squared (m/s2).' }
MassUnit                Description { 'Unit of mass. Default = kilograms (kg).' }
MassFlowUnit            Description { 'Unit of mass flow per unit time. Default = kilograms per second (kg/s).' }
VolumeUnit              Description { 'Unit of volume. Default = cubic meters (m3).' }
VolumeFlowUnit          Description { 'Unit of volume flow per unit time. Default = cubic meters per second (m3/s).' }
AngleUnit               Description { 'Unit of angle. Default = radians (rad).' }
AngularSpeedUnit        Description { 'Unit of angular speed. Default = radians per second (rad/s).' }
EnergyUnit              Description { 'Unit of energy. Default = Joules (J).' }
EnergyDensityUnit       Description { 'Unit of energy per unit volume. Default = Joules per cubic meter (J/m3).' }
SpecificEnergyUnit      Description { 'Unit of energy per unit mass. Default = Joules per kilogram (J/kg).' }
PowerUnit               Description { 'Unit of power. Default = Watts (W).' }
CostUnit                Description { 'Unit of cost. Default = dollars ($).' }
CostRateUnit            Description { 'Unit of cost per unit time. Default = dollars per second ($/s).' }
LinearDensityUnit       Description { 'Unit of mass per unit distance. Default = kilograms per meter (kg/m).' }
LinearDensityVolumeUnit Description { 'Unit of volume per unit distance. Default = cubic meters per meter (m3/m).' }
DensityUnit             Description { 'Unit of mass per unit volume. Default = kilograms per cubic meter (kg/m3).' }
PressureUnit            Description { 'Unit of pressure. Default = Pascals (Pa).' }
ViscosityUnit           Description { 'Unit of dynamic viscosity. Default = Pascal-seconds (Pa-s).' }
AreaUnit                Description { 'Unit of area. Default = square meters (m2).' }
DimensionlessUnit       Description { 'Indicates that a value is a pure number. No unit is required or accepted.' }
RateUnit                Description { 'Unit of number per unit time. Default = per second (/s).' }

# ======================================================================================================================================================
# Time units
# ======================================================================================================================================================

Define TimeUnit { s min h d w y ms us ns }

s      ConversionFactorToSI { 1        }  Description { 'Seconds' }
min    ConversionFactorToSI { 60       }  Description { 'Minutes' }
h      ConversionFactorToSI { 3600     }  Description { 'Hours' }
d      ConversionFactorToSI { 86400    }  Description { 'Days' }
w      ConversionFactorToSI { 604800   }  Description { 'Weeks' }
y      ConversionFactorToSI { 31536000 }  Description { 'Years' }
ms     ConversionFactorToSI { 1.0e-3   }  Description { 'Milliseconds' }
us     ConversionFactorToSI { 1.0e-6   }  Description { 'Microseconds' }
ns     ConversionFactorToSI { 1.0e-9   }  Description { 'Nanoseconds' }

# ======================================================================================================================================================
# Distance units
# ======================================================================================================================================================

Define DistanceUnit { m km cm mm }

m      ConversionFactorToSI { 1        }  Description { 'Meters' }
km     ConversionFactorToSI { 1000     }  Description { 'Kilometers' }
cm     ConversionFactorToSI { 0.01     }  Description { 'Centimeters' }
mm     ConversionFactorToSI { 0.001    }  Description { 'Millimeters' }

# ======================================================================================================================================================
# Speed units
# ======================================================================================================================================================

Define SpeedUnit { m/s km/h }

m/s    ConversionFactorToSI { 1             }  Description { 'Meters per Second' }
km/h   ConversionFactorToSI { 1000 3600     }  Description { 'Kilometers per Hour' }

# ======================================================================================================================================================
# Acceleration units
# ======================================================================================================================================================

Define AccelerationUnit { m/s2 }

m/s2   ConversionFactorToSI { 1      }  Description { 'Meters per Second Squared' }

# ======================================================================================================================================================
# Mass units
# ======================================================================================================================================================

Define MassUnit { kg t kt Mt }

kg     ConversionFactorToSI { 1      }  Description { 'Kilograms' }
t      ConversionFactorToSI { 1000   }  Description { 'Metric Tonnes' }
kt     ConversionFactorToSI { 1.0e6  }  Description { 'Kilotonnes' }
Mt     ConversionFactorToSI { 1.0e9  }  Description { 'Millions of Tonnes' }

# ======================================================================================================================================================
# Mass flow units
# ======================================================================================================================================================

Define MassFlowUnit   { kg/s kg/h kg/d kg/y t/h t/d t/y kt/h kt/d kt/y Mt/h Mt/d Mt/y }

kg/s   ConversionFactorToSI { 1               }  Description { 'Kilograms per Second' }
kg/h   ConversionFactorToSI { 1      3600     }  Description { 'Kilograms per Hour' }
kg/d   ConversionFactorToSI { 1      86400    }  Description { 'Kilograms per Day' }
kg/y   ConversionFactorToSI { 1      31536000 }  Description { 'Kilograms per Year' }

t/h    ConversionFactorToSI { 1000   3600     }  Description { 'Tonnes per Hour' }
t/d    ConversionFactorToSI { 1000   86400    }  Description { 'Tonnes per Day' }
t/y    ConversionFactorToSI { 1000   31536000 }  Description { 'Tonnes per Year' }

kt/h   ConversionFactorToSI { 1.0e6  3600     }  Description { 'Kilotonnes per Hour' }
kt/d   ConversionFactorToSI { 1.0e6  86400    }  Description { 'Kilotonnes per Day' }
kt/y   ConversionFactorToSI { 1.0e6  31536000 }  Description { 'Kilotonnes per Year' }

Mt/h   ConversionFactorToSI { 1.0e9  3600     }  Description { 'Million Tonnes per Hour' }
Mt/d   ConversionFactorToSI { 1.0e9  86400    }  Description { 'Million Tonnes per Day' }
Mt/y   ConversionFactorToSI { 1.0e9  31536000 }  Description { 'Million Tonnes per Year' }

# ======================================================================================================================================================
# Volume units 
# ======================================================================================================================================================

Define VolumeUnit { m3 km3 bbl mbbl mmbbl }

m3     ConversionFactorToSI { 1                 }  Description { 'Cubic Meters' }
km3    ConversionFactorToSI { 1000              }  Description { 'Thousands of Cubic Meters' }
bbl    ConversionFactorToSI { 1.58987294928E-01 }  Description { 'Barrels of Oil' }
mbbl   ConversionFactorToSI { 1.58987294928E+02 }  Description { 'Thousands of Barrels of Oil' }
mmbbl  ConversionFactorToSI { 1.58987294928E+05 }  Description { 'Millions of Barrels of Oil' }

# ======================================================================================================================================================
# Volume flow units
# ======================================================================================================================================================

Define VolumeFlowUnit { m3/s m3/h m3/d m3/y km3/h km3/d km3/y bbl/h bbl/d bbl/y mbbl/h mbbl/d mbbl/y mmbbl/h mmbbl/d mmbbl/y }

m3/s    ConversionFactorToSI { 1                          }  Description { 'Cubic Meters per Second' }
m3/h    ConversionFactorToSI { 1                 3600     }  Description { 'Cubic Meters per Hour' }
m3/d    ConversionFactorToSI { 1                 86400    }  Description { 'Cubic Meters per Day' }
m3/y    ConversionFactorToSI { 1                 31536000 }  Description { 'Cubic Meters per Year' }

km3/h   ConversionFactorToSI { 1000              3600     }  Description { 'Thousand Cubic Meters per Hour' }
km3/d   ConversionFactorToSI { 1000              86400    }  Description { 'Thousand Cubic Meters per Day' }
km3/y   ConversionFactorToSI { 1000              31536000 }  Description { 'Thousand Cubic Meters per Year' }

bbl/h   ConversionFactorToSI { 1.58987294928E-01 3600     }  Description { 'Barrels of Oil per Hour' }
bbl/d   ConversionFactorToSI { 1.58987294928E-01 86400    }  Description { 'Barrels of Oil per Day' }
bbl/y   ConversionFactorToSI { 1.58987294928E-01 31536000 }  Description { 'Barrels of Oil per Year' }

mbbl/h  ConversionFactorToSI { 1.58987294928E+02 3600     }  Description { 'Thousands of Barrels of Oil per Hour' }
mbbl/d  ConversionFactorToSI { 1.58987294928E+02 86400    }  Description { 'Thousands of Barrels of Oil per Day' }
mbbl/y  ConversionFactorToSI { 1.58987294928E+02 31536000 }  Description { 'Thousands of Barrels of Oil per Year' }

mmbbl/h ConversionFactorToSI { 1.58987294928E+05 3600     }  Description { 'Millions of Barrels of Oil per Hour' }
mmbbl/d ConversionFactorToSI { 1.58987294928E+05 86400    }  Description { 'Millions of Barrels of Oil per Day' }
mmbbl/y ConversionFactorToSI { 1.58987294928E+05 31536000 }  Description { 'Millions of Barrels of Oil per Year' }

# ======================================================================================================================================================
# Angle units
# ======================================================================================================================================================

Define AngleUnit { rad deg }

rad    ConversionFactorToSI { 1                    }  Description { 'Radians' }
deg    ConversionFactorToSI { 3.14159265358979 180 }  Description { 'Degrees' }

# ======================================================================================================================================================
# Anglular speed units
# ======================================================================================================================================================

Define AngularSpeedUnit { rad/s rad/h deg/s deg/h rpm }

rad/s  ConversionFactorToSI { 1                       }  Description { 'Radians per Second' }
rad/h  ConversionFactorToSI { 1                3600   }  Description { 'Radians per Hour' }
deg/s  ConversionFactorToSI { 3.14159265358979 180    }  Description { 'Degrees per Second' }
deg/h  ConversionFactorToSI { 3.14159265358979 648000 }  Description { 'Degrees per Hour' }
rpm    ConversionFactorToSI { 3.14159265358979 30     }  Description { 'Revolutions per Minute' }

# ======================================================================================================================================================
# Energy units
# ======================================================================================================================================================

Define EnergyUnit { J kWh }

J      ConversionFactorToSI { 1       }  Description { 'Joules' }
kWh    ConversionFactorToSI { 3600000 }  Description { 'Kilowatt-Hours' }

# ======================================================================================================================================================
# Energy density units
# ======================================================================================================================================================

Define EnergyDensityUnit { J/m3 kWh/m3 }

J/m3   ConversionFactorToSI { 1       }  Description { 'Joules per Cubic Meter' }
kWh/m3 ConversionFactorToSI { 3600000 }  Description { 'Kilowatt-Hours per Cubic Meter' }

# ======================================================================================================================================================
# Specific Energy units
# ======================================================================================================================================================

Define SpecificEnergyUnit { J/kg kWh/t }

J/kg   ConversionFactorToSI { 1            }  Description { 'Joules per Kilogram' }
kWh/t  ConversionFactorToSI { 3600000 1000 }  Description { 'Kilowatt-Hours per Tonne' }

# ======================================================================================================================================================
# Power units
# ======================================================================================================================================================

Define PowerUnit { W kW MW}

W      ConversionFactorToSI { 1     }  Description { 'Watts' }
kW     ConversionFactorToSI { 1000  }  Description { 'Kilowatts' }
MW     ConversionFactorToSI { 1.0e6 }  Description { 'Megawatts' }

# ======================================================================================================================================================
# Cost units
# ======================================================================================================================================================

Define CostUnit { $ }

$      ConversionFactorToSI { 1     }  Description { 'Dollars' }

# ======================================================================================================================================================
# Cost rate units
# ======================================================================================================================================================

Define CostRateUnit { $/s $/h $/d }

$/s    ConversionFactorToSI { 1       }  Description { 'Dollars per Second' }
$/h    ConversionFactorToSI { 1 3600  }  Description { 'Dollars per Hour' }
$/d    ConversionFactorToSI { 1 86400 }  Description { 'Dollars per Day' }

# ======================================================================================================================================================
# Linear Density units (mass)
# ======================================================================================================================================================

Define LinearDensityUnit { kg/m t/m kt/m  }

kg/m   ConversionFactorToSI { 1       }  Description { 'Kilograms per Meter' }
t/m    ConversionFactorToSI { 1000    }  Description { 'Tonnes per Meter' }
kt/m   ConversionFactorToSI { 1.0e6   }  Description { 'Kilotonnes per Meter' }

# ======================================================================================================================================================
# Linear Density units (volume)
# ======================================================================================================================================================

Define LinearDensityVolumeUnit { m3/m }

m3/m   ConversionFactorToSI { 1       }  Description { 'Cubic Meters per Meter' }

# ======================================================================================================================================================
# Density units
# ======================================================================================================================================================

Define DensityUnit { kg/m3 t/m3 kt/m3 }

kg/m3  ConversionFactorToSI { 1       }  Description { 'Kilograms per Cubic Meter' }
t/m3   ConversionFactorToSI { 1000    }  Description { 'Tonnes per Cubic Meter' }
kt/m3  ConversionFactorToSI { 1.0e6   }  Description { 'Kilotonnes per Cubic Meter' }

# ======================================================================================================================================================
# Pressure units
# ======================================================================================================================================================

Define PressureUnit { Pa kPa }

Pa     ConversionFactorToSI { 1         }  Description { 'Pascals' }
kPa    ConversionFactorToSI { 1000      }  Description { 'Kilopascals' }

# ======================================================================================================================================================
# Viscosity units
# ======================================================================================================================================================

Define ViscosityUnit { Pa-s P cP }

Pa-s   ConversionFactorToSI { 1        }  Description { 'Pascals-Seconds' }
P      ConversionFactorToSI { 0.1      }  Description { 'Poise' }
cP     ConversionFactorToSI { 0.001    }  Description { 'Centipoise' }

# ======================================================================================================================================================
# Area units
# ======================================================================================================================================================

Define AreaUnit { m2 cm2 mm2 }

m2     ConversionFactorToSI { 1         }  Description { 'Square Meters' }
cm2    ConversionFactorToSI { 1.0e-4    }  Description { 'Square Centimeters' }
mm2    ConversionFactorToSI { 1.0e-6    }  Description { 'Square Millimeters' }

# ======================================================================================================================================================
# Rate units
# ======================================================================================================================================================

Define RateUnit  { /s  /min  /h  /d  /w  /y  /ms  /us  /ns }

/s     ConversionFactorToSI { 1          }  Description { 'Per Second' }
/min   ConversionFactorToSI { 1 60       }  Description { 'Per Minute' }
/h     ConversionFactorToSI { 1 3600     }  Description { 'Per Hour' }
/d     ConversionFactorToSI { 1 86400    }  Description { 'Per Day' }
/w     ConversionFactorToSI { 1 604800   }  Description { 'Per Week' }
/y     ConversionFactorToSI { 1 31536000 }  Description { 'Per Year' }
/ms    ConversionFactorToSI { 1.0e3      }  Description { 'Per Millisecond' }
/us    ConversionFactorToSI { 1.0e6      }  Description { 'Per Microsecond' }
/ns    ConversionFactorToSI { 1.0e9      }  Description { 'Per Nanosecond' }


