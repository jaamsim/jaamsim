" Input file for unit conversion

" Created February 9, 2011 by Stephen Wong

Define Palette { Units }

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
                     PowerUnit }

TimeUnit         JavaClass { com.sandwell.JavaSimulation.TimeUnit         } Palette { Units }
DistanceUnit     JavaClass { com.sandwell.JavaSimulation.DistanceUnit     } Palette { Units }
SpeedUnit        JavaClass { com.sandwell.JavaSimulation.SpeedUnit        } Palette { Units }
AccelerationUnit JavaClass { com.sandwell.JavaSimulation.AccelerationUnit } Palette { Units }
MassUnit         JavaClass { com.sandwell.JavaSimulation.MassUnit         } Palette { Units }
MassFlowUnit     JavaClass { com.sandwell.JavaSimulation.MassFlowUnit     } Palette { Units }
VolumeUnit       JavaClass { com.sandwell.JavaSimulation.VolumeUnit       } Palette { Units }
VolumeFlowUnit   JavaClass { com.sandwell.JavaSimulation.VolumeFlowUnit   } Palette { Units }
AngleUnit        JavaClass { com.sandwell.JavaSimulation.AngleUnit        } Palette { Units }
AngularSpeedUnit JavaClass { com.sandwell.JavaSimulation.AngularSpeedUnit } Palette { Units }
EnergyUnit         JavaClass { com.sandwell.JavaSimulation.EnergyUnit         } Palette { Units }
EnergyDensityUnit  JavaClass { com.sandwell.JavaSimulation.EnergyDensityUnit  } Palette { Units }
SpecificEnergyUnit JavaClass { com.sandwell.JavaSimulation.SpecificEnergyUnit } Palette { Units }
PowerUnit          JavaClass { com.sandwell.JavaSimulation.PowerUnit          } Palette { Units }

Units HelpFile { 'Java Simulation Environment' }

" ======================================================================================================================================================
" Time units
" ======================================================================================================================================================

Define TimeUnit       { Second Minute Hour Day Year }

Second ConversionFactorToSI { 1        } Alias { Seconds seconds second Secs secs Sec sec s }
Minute ConversionFactorToSI { 60       } Alias { Minutes minutes minute Mins mins Min min }
Hour   ConversionFactorToSI { 3600     } Alias { Hours hours hour Hrs hrs Hr hr h }
Day    ConversionFactorToSI { 86400    } Alias { Days  days  day                d }
Year   ConversionFactorToSI { 31536000 } Alias { Years years year Yrs yrs Yr yr y }

" ======================================================================================================================================================
" Distance units
" ======================================================================================================================================================

Define DistanceUnit   { Metres Kilometres nmi Miles Feet Inches }

Metres       ConversionFactorToSI { 1        } Alias { Meters m }
Kilometres   ConversionFactorToSI { 1000     } Alias { Kilometers km }
nmi          ConversionFactorToSI { 1852     }
Miles        ConversionFactorToSI { 1609.344 } Alias { mi }
Feet         ConversionFactorToSI { 0.3048   } Alias { ft }
Inches       ConversionFactorToSI { 0.0254   } Alias { in }

" ======================================================================================================================================================
" Speed units
" ======================================================================================================================================================

Define SpeedUnit      { m/s km/h Knots mph }

m/s          ConversionFactorToSI { 1             }
km/h         ConversionFactorToSI { 1000 3600     } Alias { kmph }
Knots        ConversionFactorToSI { 1852 3600     } Alias { knots kn }
mph          ConversionFactorToSI { 1609.344 3600 } Alias { mi/h } "Miles per hour

" ======================================================================================================================================================
" Acceleration units
" ======================================================================================================================================================

Define AccelerationUnit      { m/s2 ft/s2 }

m/s2         ConversionFactorToSI { 1      } Alias { m/s^2 }
ft/s2        ConversionFactorToSI { 0.3048 } Alias { ft/s^2 }

" ======================================================================================================================================================
" Mass units
" ======================================================================================================================================================

Define MassUnit       { Kilograms Tonnes kt Mt }

Kilograms ConversionFactorToSI { 1          } Alias { kg }
Tonnes    ConversionFactorToSI { 1000       } Alias { t tonne tonnes }
kt        ConversionFactorToSI { 1000000    }    
Mt        ConversionFactorToSI { 1000000000 }

" ======================================================================================================================================================
" Mass flow units
" ======================================================================================================================================================

Define MassFlowUnit   { kg/h kg/d kg/y t/h t/d t/y kt/h kt/d kt/y Mt/h Mt/d Mt/y }

kg/h ConversionFactorToSI { 1           3600     }
kg/d ConversionFactorToSI { 1           86400    }
kg/y ConversionFactorToSI { 1           31536000 }

t/h  ConversionFactorToSI { 1000        3600     } Alias { tph }
t/d  ConversionFactorToSI { 1000        86400    }
t/y  ConversionFactorToSI { 1000        31536000 }

kt/h ConversionFactorToSI { 1000000     3600     } Alias { ktph }
kt/d ConversionFactorToSI { 1000000     86400    }
kt/y ConversionFactorToSI { 1000000     31536000 } Alias { ktpa }

Mt/h ConversionFactorToSI { 1000000000  3600     }
Mt/d ConversionFactorToSI { 1000000000  86400    }
Mt/y ConversionFactorToSI { 1000000000  31536000 } Alias { Mtpa }

" ======================================================================================================================================================
" Volume units 
" ======================================================================================================================================================

Define VolumeUnit     { m3 km3 bbl mbbl mmbbl }

m3    ConversionFactorToSI { 1                 } Alias { M3  M^3  m^3  }        " cubic meters
km3   ConversionFactorToSI { 1000              } Alias { KM3 KM^3 km^3 }        " thousands of cubic meters
bbl   ConversionFactorToSI { 1.58987294928E-01 } Alias { BBL }                  " barrels of oil
mbbl  ConversionFactorToSI { 1.58987294928E+02 } Alias { MBBL Mbbl mBBL kbbl }  " thousands of barrels of oil
mmbbl ConversionFactorToSI { 1.58987294928E+05 } Alias { MMBBL MMbbl mmBBL }    " millions of barrels of oil

" ======================================================================================================================================================
" Volume flow units
" ======================================================================================================================================================

Define VolumeFlowUnit { m3/h m3/d m3/y km3/h km3/d km3/y bbl/h bbl/d bbl/y mbbl/h mbbl/d mbbl/y mmbbl/h mmbbl/d mmbbl/y }

m3/h    ConversionFactorToSI { 1                 3600     } Alias {    M3/h   M^3/h   m^3/h }
m3/d    ConversionFactorToSI { 1                 86400    } Alias {    M3/d   M^3/d   m^3/d }
m3/y    ConversionFactorToSI { 1                 31536000 } Alias {    M3/y   M^3/y   m^3/y }

km3/h   ConversionFactorToSI { 1000              3600     } Alias {   KM3/h  KM^3/h  km^3/h }
km3/d   ConversionFactorToSI { 1000              86400    } Alias {   KM3/d  KM^3/d  km^3/d }
km3/y   ConversionFactorToSI { 1000              31536000 } Alias {   KM3/y  KM^3/y  km^3/y }

bbl/h   ConversionFactorToSI { 1.58987294928E-01 3600     } Alias {   BBL/h }
bbl/d   ConversionFactorToSI { 1.58987294928E-01 86400    } Alias {   BBL/d }
bbl/y   ConversionFactorToSI { 1.58987294928E-01 31536000 } Alias {   BBL/y }

mbbl/h  ConversionFactorToSI { 1.58987294928E+02 3600     } Alias {  MBBL/h  Mbbl/h  mBBL/h kbbl/h }
mbbl/d  ConversionFactorToSI { 1.58987294928E+02 86400    } Alias {  MBBL/d  Mbbl/d  mBBL/d kbbl/d }
mbbl/y  ConversionFactorToSI { 1.58987294928E+02 31536000 } Alias {  MBBL/y  Mbbl/y  mBBL/y kbbl/y }

mmbbl/h ConversionFactorToSI { 1.58987294928E+05 3600     } Alias { MMBBL/h MMbbl/h mmBBL/h }
mmbbl/d ConversionFactorToSI { 1.58987294928E+05 86400    } Alias { MMBBL/d MMbbl/d mmBBL/d }
mmbbl/y ConversionFactorToSI { 1.58987294928E+05 31536000 } Alias { MMBBL/y MMbbl/y mmBBL/y }

" ======================================================================================================================================================
" Angle units
" ======================================================================================================================================================

Define AngleUnit      { Radians Degrees }

Degrees ConversionFactorToSI { 3.14159265358979 180 } Alias { deg }
Radians ConversionFactorToSI { 1                    } Alias { rad }

" ======================================================================================================================================================
" Anglular speed units
" ======================================================================================================================================================

Define AngularSpeedUnit { rad/s rad/h deg/s deg/h }

rad/s ConversionFactorToSI { 1                       }
rad/h ConversionFactorToSI { 1                3600   }
deg/s ConversionFactorToSI { 3.14159265358979 180    }
deg/h ConversionFactorToSI { 3.14159265358979 648000 }

" ======================================================================================================================================================
" Energy units
" ======================================================================================================================================================

Define EnergyUnit      { J kWh }

kWh    ConversionFactorToSI { 3600000 }
J      ConversionFactorToSI { 1       }

" ======================================================================================================================================================
" Energy density units
" ======================================================================================================================================================

Define EnergyDensityUnit      { J/m3 kWh/m3 }

kWh/m3 ConversionFactorToSI { 3600000 }
J/m3   ConversionFactorToSI { 1       }

" ======================================================================================================================================================
" Specific Energy units
" ======================================================================================================================================================

Define SpecificEnergyUnit      { J/kg kWh/t }

kWh/t  ConversionFactorToSI { 3600000 1000 }
J/kg   ConversionFactorToSI { 1            }

" ======================================================================================================================================================
" Power units
" ======================================================================================================================================================

Define PowerUnit      { Watt kW }

kW     ConversionFactorToSI { 1000 }
Watt   ConversionFactorToSI { 1    } Alias { W }
