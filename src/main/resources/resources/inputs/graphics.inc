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
DisplayModel           JavaClass { com.jaamsim.Graphics.DisplayModelCompat            } Palette { 'Display Models' }    DragAndDrop { FALSE }

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
Region                JavaClass { com.jaamsim.Graphics.Region                        } Palette { 'Graphics Objects' }                                           DragAndDrop { FALSE }
DisplayEntity         JavaClass { com.jaamsim.Graphics.DisplayEntity                 } Palette { 'Graphics Objects' }
Text                  JavaClass { com.jaamsim.Graphics.Text                          } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } IconFile { '<res>/images/Text-24.png' }
OverlayImage          JavaClass { com.jaamsim.Graphics.OverlayImage                  } Palette { 'Graphics Objects' }
OverlayText           JavaClass { com.jaamsim.Graphics.OverlayText                   } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } IconFile { '<res>/images/Text-24.png' }
OverlayClock          JavaClass { com.jaamsim.Graphics.OverlayClock                  } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
BillboardText         JavaClass { com.jaamsim.Graphics.BillboardText                 } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } IconFile { '<res>/images/Text-24.png' }
Arrow                 JavaClass { com.jaamsim.Graphics.Arrow                         } Palette { 'Graphics Objects' } DefaultDisplayModel { ArrowModelDefault        } IconFile { '<res>/images/Arrow-24.png' }
Graph                 JavaClass { com.jaamsim.Graphics.Graph                         } Palette { 'Graphics Objects' } DefaultDisplayModel { GraphModelDefault        } IconFile { '<res>/images/Graph-24.png' } DefaultSize { 5.0 3.0 0.0 m }

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
