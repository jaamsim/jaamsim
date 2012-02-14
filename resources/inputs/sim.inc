Define Package { 'Simulation Controls' }

Define ObjectType {
                    Group
                    MouseNode
                    ProbabilityDistribution
                    GraphicSimulation
                    ScriptEntity
                  }

Group                   JavaClass { com.sandwell.JavaSimulation.Group                   } Package { 'Simulation Controls' }
MouseNode				JavaClass { com.sandwell.JavaSimulation3D.MouseNode             } Package { 'Simulation Controls' } DragAndDrop { False }
ProbabilityDistribution JavaClass { com.sandwell.JavaSimulation.ProbabilityDistribution } Package { 'Simulation Controls' }
GraphicSimulation       JavaClass { com.sandwell.JavaSimulation3D.GraphicSimulation     } Package { 'Simulation Controls' }  DragAndDrop { False }
ScriptEntity            JavaClass { com.sandwell.JavaSimulation3D.ScriptEntity          } Package { 'Simulation Controls' }

'Simulation Controls' HelpFile { 'Java Simulation Environment' } HelpSection { _41.htm }
