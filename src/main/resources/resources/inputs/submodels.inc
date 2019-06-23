Define ObjectType {
                    SubModelRegion
                    SubModelStart
                    SubModelEnd
                    ServerAndQueue
                  }

Define IconModel  {
                    SubModelStartIcon
                    SubModelEndIcon
                    ServerAndQueueIcon
                  }

SubModelStartIcon   ImageFile { '<res>/images/SubModelStart-256.png' }  Transparent { TRUE }
SubModelEndIcon     ImageFile { '<res>/images/SubModelEnd-256.png'   }  Transparent { TRUE }
ServerAndQueueIcon  ImageFile { '<res>/images/ServerAndQueue-256.png'}  Transparent { TRUE }

SubModelRegion      JavaClass { com.jaamsim.SubModels.SubModelRegion} Palette { 'SubModel Objects'   }  DefaultDisplayModel { RegionRectangle          } IconFile { '<res>/images/SubModelRegion-24.png'  } DefaultSize { 2.0 1.0 0.0 m }
SubModelStart       JavaClass { com.jaamsim.SubModels.SubModelStart } Palette { 'SubModel Objects'   }  DefaultDisplayModel { SubModelStartIcon        } IconFile { '<res>/images/SubModelStart-24.png'   } DefaultSize { 0.5 0.5 0.0 m }
SubModelEnd         JavaClass { com.jaamsim.SubModels.SubModelEnd   } Palette { 'SubModel Objects'   }  DefaultDisplayModel { SubModelEndIcon          } IconFile { '<res>/images/SubModelEnd-24.png'     } DefaultSize { 0.5 0.5 0.0 m }
ServerAndQueue      JavaClass { com.jaamsim.SubModels.ServerAndQueue} Palette { 'SubModel Objects'   }  DefaultDisplayModel { ServerAndQueueIcon       } IconFile { '<res>/images/ServerAndQueue-24.png'  }

SubModelRegion      DragAndDrop { FALSE }
SubModelStart       DragAndDrop { FALSE }
SubModelEnd         DragAndDrop { FALSE }

SubModelRegion      Description { 'A region containing the components of a sub-model.' }
SubModelStart       Description { 'The first component of a sub-model.' }
SubModelEnd         Description { 'The last component of a sub-model.' }
ServerAndQueue      Description { 'A sub-model that contains a Server and its Queue.' }
