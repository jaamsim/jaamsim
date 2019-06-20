Define ObjectType {
                    SubModelStart
                    SubModelEnd
                  }

Define IconModel  {
                    SubModelStartIcon
                    SubModelEndIcon
                  }

SubModelStartIcon   ImageFile { '<res>/images/SubModelStart-256.png' }  Transparent { TRUE }
SubModelEndIcon     ImageFile { '<res>/images/SubModelEnd-256.png'   }  Transparent { TRUE }

SubModelStart       JavaClass { com.jaamsim.SubModels.SubModelStart } Palette { 'SubModel Objects'   }  DefaultDisplayModel { SubModelStartIcon        } IconFile { '<res>/images/SubModelStart-24.png'   } DefaultSize { 0.5 0.5 0.0 m }
SubModelEnd         JavaClass { com.jaamsim.SubModels.SubModelEnd   } Palette { 'SubModel Objects'   }  DefaultDisplayModel { SubModelEndIcon          } IconFile { '<res>/images/SubModelEnd-24.png'     } DefaultSize { 0.5 0.5 0.0 m }

SubModelStart       DragAndDrop { FALSE }
SubModelEnd         DragAndDrop { FALSE }

SubModelStart       Description { 'The first component of a sub-model.' }
SubModelEnd         Description { 'The last component of a sub-model.' }
