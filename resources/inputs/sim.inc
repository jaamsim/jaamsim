Define Palette { 'Simulation Controls' }

Define ObjectType {
                    Group
                    MouseNode
                    ProbabilityDistribution
                    GraphicSimulation
                    ScriptEntity
                  }

Group                   JavaClass { com.sandwell.JavaSimulation.Group                   } Palette { 'Simulation Controls' }
MouseNode				JavaClass { com.sandwell.JavaSimulation3D.MouseNode             } Palette { 'Simulation Controls' } DragAndDrop { False }
ProbabilityDistribution JavaClass { com.sandwell.JavaSimulation.ProbabilityDistribution } Palette { 'Simulation Controls' }
GraphicSimulation       JavaClass { com.sandwell.JavaSimulation3D.GraphicSimulation     } Palette { 'Simulation Controls' }  DragAndDrop { False }
ScriptEntity            JavaClass { com.sandwell.JavaSimulation3D.ScriptEntity          } Palette { 'Simulation Controls' }

'Simulation Controls' HelpFile { 'Java Simulation Environment' } HelpSection { _41.htm }
