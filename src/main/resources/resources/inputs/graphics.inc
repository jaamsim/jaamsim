
Define Palette { 'Display Models' }

Define ObjectType {
                    ArrowModel
                    GraphModel
                    ScreenPointsModel
                    TextModel
                    ImageModel
                    ColladaModel
                    DisplayModel
                    ColladaFactory
                    }

ArrowModel             JavaClass { com.jaamsim.DisplayModels.ArrowModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
GraphModel             JavaClass { com.jaamsim.DisplayModels.GraphModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
ScreenPointsModel      JavaClass { com.jaamsim.DisplayModels.ScreenPointsModel        } Palette { 'Display Models' }    DragAndDrop { FALSE }
TextModel              JavaClass { com.jaamsim.DisplayModels.TextModel                } Palette { 'Display Models' }    DragAndDrop { FALSE }
ImageModel             JavaClass { com.jaamsim.DisplayModels.ImageModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
ColladaModel           JavaClass { com.jaamsim.DisplayModels.ColladaModel             } Palette { 'Display Models' }    DragAndDrop { FALSE }
DisplayModel           JavaClass { com.sandwell.JavaSimulation3D.DisplayModelCompat   } Palette { 'Display Models' }    DragAndDrop { FALSE }
ColladaFactory         JavaClass { com.jaamsim.collada.ColladaEntityFactory           }                                 DragAndDrop { FALSE }

Define Palette { 'Graphics Objects' }

Define ArrowModel          { ArrowModelDefault          }
Define GraphModel          { GraphModelDefault          }
Define ScreenPointsModel   { ScreenPointsModelDefault   }
Define TextModel           { TextModelDefault           }

Define ObjectType {
                    View
                    Region
                    DisplayEntity
					Text
                    Arrow
                    TextLabel
                    PropertyLabel
                    OutputPropertyLabel
                    OverlayTextLabel
                    OverlayPropertyLabel
                    OverlayOPropLabel
                    OverlayImage
					OverlayText
                    Graph
                    VideoRecorder
                    }

View                  JavaClass { com.jaamsim.ui.View                                } Palette { 'Graphics Objects' }                                           DragAndDrop { FALSE }
Region                JavaClass { com.sandwell.JavaSimulation3D.Region               } Palette { 'Graphics Objects' }                                           DragAndDrop { FALSE }
DisplayEntity         JavaClass { com.sandwell.JavaSimulation3D.DisplayEntity        } Palette { 'Graphics Objects' }
Text                  JavaClass { com.sandwell.JavaSimulation3D.Text                 } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
TextLabel             JavaClass { com.sandwell.JavaSimulation3D.TextLabel            } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } DragAndDrop { FALSE }
PropertyLabel         JavaClass { com.sandwell.JavaSimulation3D.PropertyLabel        } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } DragAndDrop { FALSE }
OutputPropertyLabel   JavaClass { com.sandwell.JavaSimulation3D.OutputPropertyLabel  } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } DragAndDrop { FALSE }
OverlayTextLabel      JavaClass { com.sandwell.JavaSimulation3D.OverlayTextLabel     } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
OverlayPropertyLabel  JavaClass { com.sandwell.JavaSimulation3D.OverlayPropertyLabel } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
OverlayOPropLabel     JavaClass { com.sandwell.JavaSimulation3D.OverlayOPropLabel    } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
OverlayImage          JavaClass { com.sandwell.JavaSimulation3D.OverlayImage         } Palette { 'Graphics Objects' }
OverlayText           JavaClass { com.sandwell.JavaSimulation3D.OverlayText          } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
Arrow                 JavaClass { com.sandwell.JavaSimulation3D.Arrow                } Palette { 'Graphics Objects' } DefaultDisplayModel { ArrowModelDefault        }
Graph                 JavaClass { com.sandwell.JavaSimulation3D.Graph                } Palette { 'Graphics Objects' } DefaultDisplayModel { GraphModelDefault        }

VideoRecorder         JavaClass { com.jaamsim.ui.VideoRecorderEntity                 } Palette { 'Graphics Objects' }    DragAndDrop { FALSE }
