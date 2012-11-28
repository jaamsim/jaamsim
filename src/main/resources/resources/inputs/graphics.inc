Define Palette { 'Graphics Objects' }

Define ObjectType {
                    View
                    Region
                    DisplayEntity
                    DisplayModel
                    Arrow
                    TextLabel
                    OverlayTextLabel
                    OverlayImage
                    PropertyLabel
                    Graph
                    }

View             JavaClass { com.jaamsim.ui.View                            } Palette { 'Graphics Objects' }
Region           JavaClass { com.sandwell.JavaSimulation3D.Region           } Palette { 'Graphics Objects' }                                          DragAndDrop { FALSE }
DisplayEntity    JavaClass { com.sandwell.JavaSimulation3D.DisplayEntity    } Palette { 'Graphics Objects' } DefaultObserver { DMObserver           }
DisplayModel     JavaClass { com.sandwell.JavaSimulation3D.DisplayModel     } Palette { 'Graphics Objects' }                                          DragAndDrop { FALSE }
TextLabel        JavaClass { com.sandwell.JavaSimulation3D.TextLabel        } Palette { 'Graphics Objects' } DefaultObserver { LabelObserver        }
OverlayTextLabel JavaClass { com.sandwell.JavaSimulation3D.OverlayTextLabel } Palette { 'Graphics Objects' } DefaultObserver { OverlayLabelObserver }
OverlayImage     JavaClass { com.sandwell.JavaSimulation3D.OverlayImage     } Palette { 'Graphics Objects' } DefaultObserver { OverlayImageObserver }
PropertyLabel    JavaClass { com.sandwell.JavaSimulation3D.PropertyLabel    } Palette { 'Graphics Objects' } DefaultObserver { LabelObserver        }
Arrow            JavaClass { com.sandwell.JavaSimulation3D.Arrow            } Palette { 'Graphics Objects' } DefaultObserver { ArrowObserver        }
Graph            JavaClass { com.sandwell.JavaSimulation3D.Graph            } Palette { 'Graphics Objects' } DefaultObserver { GraphObserver        }
