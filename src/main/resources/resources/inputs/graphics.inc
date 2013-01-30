
Define Palette { 'Display Models' }

Define ObjectType {
                    ArrowModel
                    GraphModel
                    OverlayImageModel
                    OverlayTextModel
                    ScreenPointsModel
                    TextModel
                    DisplayModel
                    }

ArrowModel             JavaClass { com.jaamsim.DisplayModels.ArrowModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
GraphModel             JavaClass { com.jaamsim.DisplayModels.GraphModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
OverlayImageModel      JavaClass { com.jaamsim.DisplayModels.OverlayImageModel        } Palette { 'Display Models' }    DragAndDrop { FALSE }
OverlayTextModel       JavaClass { com.jaamsim.DisplayModels.OverlayTextModel         } Palette { 'Display Models' }    DragAndDrop { FALSE }
ScreenPointsModel      JavaClass { com.jaamsim.DisplayModels.ScreenPointsModel        } Palette { 'Display Models' }    DragAndDrop { FALSE }
TextModel              JavaClass { com.jaamsim.DisplayModels.TextModel                } Palette { 'Display Models' }    DragAndDrop { FALSE }
DisplayModel               JavaClass { com.sandwell.JavaSimulation3D.DisplayModelCompat   } Palette { 'Display Models' }    DragAndDrop { FALSE }

Define Palette { 'Graphics Objects' }

Define ArrowModel         { ArrowModelDefault          }
Define GraphModel         { GraphModelDefault          }
Define OverlayImageModel  { OverlayImageModelDefault   }
Define OverlayTextModel   { OverlayTextModelDefault    }
Define TextModel          { TextModelDefault           }

Define ObjectType {
                    View
                    Region
                    DisplayEntity
                    Arrow
                    TextLabel
                    OverlayTextLabel
                    OverlayPropertyLabel
                    OverlayImage
                    PropertyLabel
                    Graph
                    }

View                  JavaClass { com.jaamsim.ui.View                                } Palette { 'Graphics Objects' }
Region                JavaClass { com.sandwell.JavaSimulation3D.Region               } Palette { 'Graphics Objects' }                                           DragAndDrop { FALSE }
DisplayEntity         JavaClass { com.sandwell.JavaSimulation3D.DisplayEntity        } Palette { 'Graphics Objects' }
TextLabel             JavaClass { com.sandwell.JavaSimulation3D.TextLabel            } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
OverlayTextLabel      JavaClass { com.sandwell.JavaSimulation3D.OverlayTextLabel     } Palette { 'Graphics Objects' } DefaultDisplayModel { OverlayTextModelDefault  }
OverlayPropertyLabel  JavaClass { com.sandwell.JavaSimulation3D.OverlayPropertyLabel } Palette { 'Graphics Objects' } DefaultDisplayModel { OverlayTextModelDefault  }
OverlayImage          JavaClass { com.sandwell.JavaSimulation3D.OverlayImage         } Palette { 'Graphics Objects' } DefaultDisplayModel { OverlayImageModelDefault }
PropertyLabel         JavaClass { com.sandwell.JavaSimulation3D.PropertyLabel        } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
Arrow                 JavaClass { com.sandwell.JavaSimulation3D.Arrow                } Palette { 'Graphics Objects' } DefaultDisplayModel { ArrowModelDefault        }
Graph                 JavaClass { com.sandwell.JavaSimulation3D.Graph                } Palette { 'Graphics Objects' } DefaultDisplayModel { GraphModelDefault        }
