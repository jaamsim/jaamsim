Define ObjectType {
                    ColladaModel
                    ImageModel
                    ShapeModel
                    PolylineModel
                    TextModel
                    GraphModel
                  }

GraphModel      JavaClass { com.jaamsim.DisplayModels.GraphModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
PolylineModel   JavaClass { com.jaamsim.DisplayModels.PolylineModel            } Palette { 'Display Models' }    DragAndDrop { FALSE }
TextModel       JavaClass { com.jaamsim.DisplayModels.TextModel                } Palette { 'Display Models' }    DragAndDrop { FALSE }
ImageModel      JavaClass { com.jaamsim.DisplayModels.ImageModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }
ColladaModel    JavaClass { com.jaamsim.DisplayModels.ColladaModel             } Palette { 'Display Models' }    DragAndDrop { FALSE }
ShapeModel      JavaClass { com.jaamsim.DisplayModels.ShapeModel               } Palette { 'Display Models' }    DragAndDrop { FALSE }

Define ShapeModel {
                    Rectangle
                    Circle
                    Arrow2D
                    Triangle
                    BarGauge2D
                  }

Rectangle       Shape { RECTANGLE  }
Circle          Shape { CIRCLE     }
Arrow2D         Shape { ARROW2D    }
Triangle        Shape { TRIANGLE   }
BarGauge2D      Shape { BARGAUGE2D }

Define ColladaModel {
                      Cube
                      Sphere
                      CrossHairs
                      RegionModelDefault
                    }

Cube               ColladaFile { '<res>/shapes/cube.dae'       }
Sphere             ColladaFile { '<res>/shapes/Sphere.dae'     }
CrossHairs         ColladaFile { '<res>/shapes/CrossHairs.dae' }
RegionModelDefault ColladaFile { <res>/shapes/axis_text.dae }

Define GraphModel     { GraphModelDefault          }

Define TextModel      { TextModelDefault  EntityLabelModel  InputBoxModel  BillboardTextModel OverlayTextModel }
EntityLabelModel  TextHeight { 0.15 m }
BillboardTextModel  TextHeight { 10 m }
OverlayTextModel    TextHeight { 15 m }

Define PolylineModel  { PolylineModelDefault  ArrowModelDefault }
ArrowModelDefault ShowArrowHead { TRUE }

Define  ImageModel { VideoRecorderIconModel }
VideoRecorderIconModel ImageFile { '<res>/images/VideoRecorder-256.png' }
