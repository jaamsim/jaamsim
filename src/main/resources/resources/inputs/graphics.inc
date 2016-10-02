Define ObjectType {
                    View
                    Region
                    DisplayEntity
                    Text
                    EntityLabel
                    InputBox
                    BarGauge
                    OverlayImage
                    OverlayText
                    OverlayClock
                    BillboardText
                    Arrow
                    Graph
                    VideoRecorder
                    }

View                  JavaClass { com.jaamsim.ui.View                                } Palette { 'Graphics Objects' }                                           DragAndDrop { FALSE }
Region                JavaClass { com.jaamsim.Graphics.Region                        } Palette { 'Graphics Objects' } DefaultDisplayModel { RegionModelDefault       } IconFile { '<res>/images/Region-24.png' } DefaultSize { 1.125000 1.1568242 1.1266404 m } DefaultAlignment { -0.4393409 -0.4410096 -0.4394292 }
DisplayEntity         JavaClass { com.jaamsim.Graphics.DisplayEntity                 } Palette { 'Graphics Objects' } DefaultDisplayModel { Cube                     } IconFile { '<res>/images/DisplayEntity-24.png' }
Text                  JavaClass { com.jaamsim.Graphics.Text                          } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } IconFile { '<res>/images/Text-24.png' }
EntityLabel           JavaClass { com.jaamsim.Graphics.EntityLabel                   } Palette { 'Graphics Objects' } DefaultDisplayModel { EntityLabelModel         } DragAndDrop { FALSE }
InputBox              JavaClass { com.jaamsim.Graphics.InputBox                      } Palette { 'Graphics Objects' } DefaultDisplayModel { InputBoxModel            } IconFile { '<res>/images/InputValue-24.png' }
BarGauge              JavaClass { com.jaamsim.Graphics.BarGauge                      } Palette { 'Graphics Objects' } DefaultDisplayModel { BarGauge2D               } IconFile { '<res>/images/BarGauge-24.png' } DefaultSize { 0.5 1.0 0.0 m }
OverlayImage          JavaClass { com.jaamsim.Graphics.OverlayImage                  } Palette { 'Graphics Objects' }
OverlayText           JavaClass { com.jaamsim.Graphics.OverlayText                   } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } IconFile { '<res>/images/Text-24.png' }
OverlayClock          JavaClass { com.jaamsim.Graphics.OverlayClock                  } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         }
BillboardText         JavaClass { com.jaamsim.Graphics.BillboardText                 } Palette { 'Graphics Objects' } DefaultDisplayModel { TextModelDefault         } IconFile { '<res>/images/Text-24.png' }
Arrow                 JavaClass { com.jaamsim.Graphics.Arrow                         } Palette { 'Graphics Objects' } DefaultDisplayModel { ArrowModelDefault        } IconFile { '<res>/images/Arrow-24.png' }
Graph                 JavaClass { com.jaamsim.Graphics.Graph                         } Palette { 'Graphics Objects' } DefaultDisplayModel { GraphModelDefault        } IconFile { '<res>/images/Graph-24.png' } DefaultSize { 5.0 3.0 0.0 m }
VideoRecorder         JavaClass { com.jaamsim.ui.VideoRecorderEntity                 } Palette { 'Graphics Objects' } DefaultDisplayModel { VideoRecorderIconModel   } IconFile { '<res>/images/VideoRecorder-24.png' }

View          Description { 'Display window showing view of the 3D model universe.' }
Region        Description { 'Local coordinate system.' }
DisplayEntity Description { 'Graphical object displaying either a 3D shape or a 2D picture.' }
Text          Description { 'Text that appears in the 3D model universe.' }
InputBox      Description { 'Text that provides an input value for the model.' }
BarGauge      Description { 'Gauge that displays the value of an expression.' }
OverlayImage  Description { 'Picture that appears in a fixed position in the display window.' }
OverlayText   Description { 'Text that appears in a fixed position in the dispay window.' }
OverlayClock  Description { 'Time and date display that appears in a fixed position in the display window.' }
BillboardText Description { 'Text that follows a 3D position but is always upright on the screen.' }
Arrow         Description { 'Line that terminates in an arrow head.' }
Graph         Description { 'Chart that shows the values of model outputs as they change in time.' }
VideoRecorder Description { 'Object that makes a video recording of the model.' }
