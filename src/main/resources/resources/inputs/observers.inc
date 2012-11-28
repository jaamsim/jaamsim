"Define Palette { 'Observer Types' }

Define ObjectType {
                    CubeObserverType
                    BoxObserverType
                    TextObserverType
                    OverlayTextObserverType
                    OverlayImageObserverType
                    ImageObserverType
                    DMObserverType
                    ScreenPointsObserverType
                    GraphObserverType
                    ArrowObserverType
                  }

CubeObserverType JavaClass          { com.jaamsim.observers.CubeObserverProto          } 
BoxObserverType JavaClass           { com.jaamsim.observers.BoxObserverProto           } 
TextObserverType JavaClass          { com.jaamsim.observers.TextObserverProto          } 
OverlayTextObserverType JavaClass   { com.jaamsim.observers.OverlayTextObserverProto   }
OverlayImageObserverType JavaClass  { com.jaamsim.observers.OverlayImageObserverProto  }
ImageObserverType JavaClass         { com.jaamsim.observers.ImageObserverProto         } 
DMObserverType JavaClass            { com.jaamsim.observers.DisplayModelObserverProto  } 
ScreenPointsObserverType JavaClass  { com.jaamsim.observers.ScreenPointsObserverProto  } 
GraphObserverType JavaClass         { com.jaamsim.observers.GraphObserverProto         } 
ArrowObserverType JavaClass         { com.jaamsim.observers.ArrowObserverProto         } 

Define TextObserverType          { LabelObserver         }
Define OverlayTextObserverType   { OverlayLabelObserver  }
Define OverlayImageObserverType  { OverlayImageObserver  }
Define ScreenPointsObserverType  { ScreenPointsObserver  }
Define DMObserverType            { DMObserver            }
Define GraphObserverType         { GraphObserver         }
Define ArrowObserverType         { ArrowObserver         }
