Define Palette { 'Basic Objects' }

Define ObjectType {
                    ModelEntity
                    Queue
                    QueueRecorder
                    ProbabilityDistribution
                    Group
                    ScriptEntity
                    Output
                    }

ModelEntity             JavaClass { com.sandwell.JavaSimulation3D.ModelEntity           } Palette { 'Basic Objects' } DefaultDisplayModel { Cube }      DragAndDrop { FALSE }
Queue                   JavaClass { com.sandwell.JavaSimulation3D.Queue	                } Palette { 'Basic Objects' } DefaultDisplayModel { Triangle }                        DefaultObserver { DMObserver }
QueueRecorder           JavaClass { com.sandwell.JavaSimulation3D.QueueRecorder         } Palette { 'Basic Objects' } DefaultDisplayModel { Cube }      DragAndDrop { FALSE }
ProbabilityDistribution JavaClass { com.sandwell.JavaSimulation.ProbabilityDistribution } Palette { 'Basic Objects' }
Group                   JavaClass { com.sandwell.JavaSimulation.Group                   } Palette { 'Basic Objects' } DragAndDrop { FALSE }
ScriptEntity            JavaClass { com.sandwell.JavaSimulation3D.ScriptEntity          } Palette { 'Basic Objects' } DragAndDrop { FALSE }
Output                  JavaClass { com.sandwell.JavaSimulation.OutputMethod            } Palette { 'Basic Objects' } DragAndDrop { FALSE }
