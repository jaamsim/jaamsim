Define ObjectType {
                    Region
                    DisplayEntity
                    Shape
                    Polyline
                    Arrow
                    Text
                    Graph
                    EntityLabel
                    BarGauge
                    BooleanIndicator
                    OverlayImage
                    OverlayText
                    OverlayClock
                    BillboardText
                    MimicEntity
                    VideoRecorder
                    }

Define  IconModel  { MimicEntityIconModel }
MimicEntityIconModel   ImageFile { '<res>/images/MimicEntity-256.png' }  Transparent { TRUE }

Region                JavaClass { com.jaamsim.Graphics.Region                        } Palette { 'Graphics Objects' } DefaultDisplayModel { RegionModelDefault       } IconFile { '<res>/images/Region-24.png' } DefaultSize { 1.125000 1.1568242 1.1266404 m } DefaultAlignment { -0.4393409 -0.4410096 -0.4394292 }
DisplayEntity         JavaClass { com.jaamsim.Graphics.DisplayEntity                 } Palette { 'Graphics Objects' } DefaultDisplayModel { Cube                     } IconFile { '<res>/images/DisplayEntity-24.png' }
Shape                 JavaClass { com.jaamsim.Graphics.Shape                         } Palette { 'Graphics Objects' } DefaultDisplayModel { Rectangle                } IconFile { '<res>/images/Shape-24.png' }
Polyline              JavaClass { com.jaamsim.Graphics.Polyline                      } Palette { 'Graphics Objects' } DefaultDisplayModel { PolylineModelDefault     } IconFile { '<res>/images/Polyline-24.png' }
Arrow                 JavaClass { com.jaamsim.Graphics.Arrow                         } Palette { 'Graphics Objects' } DefaultDisplayModel { ArrowModelDefault        } IconFile { '<res>/images/Arrow-24.png' }
Text                  JavaClass { com.jaamsim.Graphics.Text                          } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } IconFile { '<res>/images/Text-24.png' }
Graph                 JavaClass { com.jaamsim.Graphics.Graph                         } Palette { 'Graphics Objects' } DefaultDisplayModel { GraphModelDefault        } IconFile { '<res>/images/Graph-24.png' } DefaultSize { 5.0 3.0 0.0 m }
EntityLabel           JavaClass { com.jaamsim.Graphics.EntityLabel                   } Palette { 'Graphics Objects' } DefaultDisplayModel { EntityLabelModel         } DragAndDrop { FALSE }
BarGauge              JavaClass { com.jaamsim.Graphics.BarGauge                      } Palette { 'Graphics Objects' } DefaultDisplayModel { BarGauge2D               } IconFile { '<res>/images/BarGauge-24.png' } DefaultSize { 0.5 1.0 0.0 m }
BooleanIndicator      JavaClass { com.jaamsim.BasicObjects.BooleanIndicator          } Palette { 'Graphics Objects' } DefaultDisplayModel { Circle                   } IconFile { '<res>/images/BooleanIndicator-24.png' } DefaultSize { 0.25 0.25 0.0 m }
OverlayImage          JavaClass { com.jaamsim.Graphics.OverlayImage                  } Palette { 'Graphics Objects' }
OverlayText           JavaClass { com.jaamsim.Graphics.OverlayText                   } Palette { 'Graphics Objects' } DefaultDisplayModel { OverlayTextModel         } IconFile { '<res>/images/Text-24.png' }
OverlayClock          JavaClass { com.jaamsim.Graphics.OverlayClock                  } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
BillboardText         JavaClass { com.jaamsim.Graphics.BillboardText                 } Palette { 'Graphics Objects' } DefaultDisplayModel { BillboardTextModel       } IconFile { '<res>/images/Text-24.png' }
MimicEntity           JavaClass { com.jaamsim.Graphics.MimicEntity                   } Palette { 'Graphics Objects' } DefaultDisplayModel { MimicEntityIconModel     } IconFile { '<res>/images/MimicEntity-24.png' }
VideoRecorder         JavaClass { com.jaamsim.ui.VideoRecorderEntity                 } Palette { 'Graphics Objects' } DefaultDisplayModel { VideoRecorderIconModel   } IconFile { '<res>/images/VideoRecorder-24.png' }

Region        Description { 'Local coordinate system.' }
DisplayEntity Description { 'Graphical object displaying either a 3D shape or a 2D picture.' }
Shape         Description { 'Two dimensional graphical object whose shape and colour can be selected.' }
Polyline      Description { 'Straight or curved line segments.' }
Arrow         Description { 'Line that terminates in an arrow head.' }
Text          Description { 'Text that appears in the 3D model universe.' }
Graph         Description { 'Chart that shows the values of model outputs as they change in time.' }
BarGauge      Description { 'Gauge that displays the value of an expression.' }
BooleanIndicator  Description { 'Circular entity that changes color to indicate TRUE and FALSE' }
OverlayImage  Description { 'Picture that appears in a fixed position in the display window.' }
OverlayText   Description { 'Text that appears in a fixed position in the dispay window.' }
OverlayClock  Description { 'Time and date display that appears in a fixed position in the display window.' }
BillboardText Description { 'Text that follows a 3D position but is always upright on the screen.' }
MimicEntity   Description { 'Graphical object that copies the appearance of another DisplayEntity.' }
VideoRecorder Description { 'Object that makes a video recording of the model.' }
