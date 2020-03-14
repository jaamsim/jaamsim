Define ObjectType {
                    GraphicSimulation
                    View
                    EntityTracer
                  }

GraphicSimulation       JavaClass { com.jaamsim.basicsim.Simulation     } Palette { 'Simulation Controls' } DragAndDrop { FALSE }
Define GraphicSimulation { Simulation }
Simulation DisplayedUnits { h deg }

View                    JavaClass { com.jaamsim.Graphics.View           } Palette { 'View'                } DragAndDrop { FALSE }
View Description { 'Display window showing view of the 3D model universe.' }

EntityTracer            JavaClass { com.jaamsim.basicsim.EntityTracer          } Palette { 'Simulation Controls' } DragAndDrop { FALSE }

