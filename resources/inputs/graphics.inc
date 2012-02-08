Define Package { 'Graphics Objects' }

Define ObjectType {
                    Arrow
                    DisplayEntity
                    DisplayModel
                    Graph
                    PictureEntity
                    PropertyLabel
                    Region
                    TextLabel
                  }

Arrow            JavaClass { com.sandwell.JavaSimulation3D.Arrow            } Package { 'Graphics Objects' }
DisplayEntity    JavaClass { com.sandwell.JavaSimulation3D.DisplayEntity    } Package { 'Graphics Objects' }
DisplayModel     JavaClass { com.sandwell.JavaSimulation3D.DisplayModel	    } Package { 'Graphics Objects' } DragAndDrop { False }
Graph            JavaClass { com.sandwell.JavaSimulation3D.Graph            } Package { 'Graphics Objects' }
PictureEntity    JavaClass { com.sandwell.JavaSimulation3D.PictureEntity    } Package { 'Graphics Objects' } DragAndDrop { False }
PropertyLabel    JavaClass { com.sandwell.JavaSimulation3D.PropertyLabel    } Package { 'Graphics Objects' }
Region           JavaClass { com.sandwell.JavaSimulation3D.Region	    } Package { 'Graphics Objects' } DragAndDrop { False }
TextLabel        JavaClass { com.sandwell.JavaSimulation3D.TextLabel	    } Package { 'Graphics Objects' }

'Graphics Objects' HelpFile { 'Java Simulation Environment' } HelpSection { _41.htm }

ModelStage WindowPos  {  220 110 }
ModelStage WindowSize { 1060 600 }
