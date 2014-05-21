Define ObjectType {
                    ArrowModel
                    GraphModel
                    ScreenPointsModel
                    TextModel
                    ImageModel
                    ColladaModel
                    DisplayModel
                    }

ArrowModel             JavaClass { com.jaamsim.DisplayModels.ArrowModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
GraphModel             JavaClass { com.jaamsim.DisplayModels.GraphModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
ScreenPointsModel      JavaClass { com.jaamsim.DisplayModels.ScreenPointsModel        } Palette { 'Display Models' }    DragAndDrop { FALSE }
TextModel              JavaClass { com.jaamsim.DisplayModels.TextModel                } Palette { 'Display Models' }    DragAndDrop { FALSE }
ImageModel             JavaClass { com.jaamsim.DisplayModels.ImageModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
ColladaModel           JavaClass { com.jaamsim.DisplayModels.ColladaModel             } Palette { 'Display Models' }    DragAndDrop { FALSE }
DisplayModel           JavaClass { com.sandwell.JavaSimulation3D.DisplayModelCompat   } Palette { 'Display Models' }    DragAndDrop { FALSE }

Define ArrowModel          { ArrowModelDefault          }
Define GraphModel          { GraphModelDefault          }
Define ScreenPointsModel   { ScreenPointsModelDefault   }
Define TextModel           { TextModelDefault           }

Define ObjectType {
                    View
                    Region
                    DisplayEntity
                    Text
                    OverlayImage
                    OverlayText
                    OverlayClock
                    BillboardText
                    Arrow
                    Graph
                    VideoRecorder
                    }

View                  JavaClass { com.jaamsim.ui.View                                } Palette { 'Graphics Objects' }                                           DragAndDrop { FALSE }
Region                JavaClass { com.sandwell.JavaSimulation3D.Region               } Palette { 'Graphics Objects' }                                           DragAndDrop { FALSE }
DisplayEntity         JavaClass { com.sandwell.JavaSimulation3D.DisplayEntity        } Palette { 'Graphics Objects' }
Text                  JavaClass { com.sandwell.JavaSimulation3D.Text                 } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
OverlayImage          JavaClass { com.sandwell.JavaSimulation3D.OverlayImage         } Palette { 'Graphics Objects' }
OverlayText           JavaClass { com.sandwell.JavaSimulation3D.OverlayText          } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
OverlayClock          JavaClass { com.sandwell.JavaSimulation3D.OverlayClock         } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
BillboardText         JavaClass { com.sandwell.JavaSimulation3D.BillboardText        } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
Arrow                 JavaClass { com.sandwell.JavaSimulation3D.Arrow                } Palette { 'Graphics Objects' } DefaultDisplayModel { ArrowModelDefault        }
Graph                 JavaClass { com.sandwell.JavaSimulation3D.Graph                } Palette { 'Graphics Objects' } DefaultDisplayModel { GraphModelDefault        }

VideoRecorder         JavaClass { com.jaamsim.ui.VideoRecorderEntity                 } Palette { 'Graphics Objects' }    DragAndDrop { FALSE }

View          Description { 'Display window showing view of the 3D model universe.' }
Region        Description { 'Local coordinate system.' }
DisplayEntity Description { 'Graphical object displaying either a 3D shape or a 2D picture.' }
Text          Description { 'Text that appears in the 3D model universe.' }
OverlayImage  Description { 'Picture that appears in a fixed position in the display window.' }
OverlayText   Description { 'Text that appears in a fixed position in the dispay window.' }
OverlayClock  Description { 'Time and date display that appears in a fixed position in the display window.' }
BillboardText Description { 'Text that follows a 3D position but is always upright on the screen.' }
Arrow         Description { 'Line that terminates in an arrow head.' }
Graph         Description { 'Chart that shows the values of model outputs as they change in time.' }
VideoRecorder Description { 'Object that makes a video recording of the model.' }
