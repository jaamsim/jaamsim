Define Package { 'Simulation Controls' }

Define ObjectType {
                    Group
                    MouseNode
                    ProbabilityDistribution
                    GraphicSimulation
                  }

Group                   JavaClass { com.sandwell.JavaSimulation.Group                   } Package { 'Simulation Controls' }
MouseNode				JavaClass { com.sandwell.JavaSimulation3D.MouseNode             } Package { 'Simulation Controls' } DragAndDrop { False }
ProbabilityDistribution JavaClass { com.sandwell.JavaSimulation.ProbabilityDistribution } Package { 'Simulation Controls' }
GraphicSimulation       JavaClass { com.sandwell.JavaSimulation3D.GraphicSimulation     } Package { 'Simulation Controls' }  DragAndDrop { False }

'Simulation Controls' HelpFile { 'Java Simulation Environment' } HelpSection { _41.htm }
