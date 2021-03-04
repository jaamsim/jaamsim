Define ObjectType {
                    SubModel
                    SubModelClone
                    SubModelStart
                    SubModelEnd
                    ServerAndQueue
                  }

Define IconModel  {
                    SubModelIcon
                    SubModelStartIcon
                    SubModelEndIcon
                    ServerAndQueueIcon
                  }

SubModelIcon        ImageFile { '<res>/images/SubModel-256.png'      }  Transparent { TRUE }
SubModelStartIcon   ImageFile { '<res>/images/SubModelStart-256.png' }  Transparent { TRUE }
SubModelEndIcon     ImageFile { '<res>/images/SubModelEnd-256.png'   }  Transparent { TRUE }
ServerAndQueueIcon  ImageFile { '<res>/images/ServerAndQueue-256.png'}  Transparent { TRUE }

SubModel            JavaClass { com.jaamsim.SubModels.SubModel      } Palette { 'SubModel Objects'   }  DefaultDisplayModel { SubModelIcon             } IconFile { '<res>/images/SubModel-24.png'        }
SubModelClone       JavaClass { com.jaamsim.SubModels.SubModelClone } Palette { 'SubModel Objects'   }  DefaultDisplayModel { SubModelIcon             } IconFile { '<res>/images/SubModel-24.png'        } DragAndDrop { FALSE }
SubModelStart       JavaClass { com.jaamsim.SubModels.SubModelStart } Palette { 'SubModel Objects'   }  DefaultDisplayModel { SubModelStartIcon        } IconFile { '<res>/images/SubModelStart-24.png'   } DefaultSize { 0.5 0.5 0.0 m }
SubModelEnd         JavaClass { com.jaamsim.SubModels.SubModelEnd   } Palette { 'SubModel Objects'   }  DefaultDisplayModel { SubModelEndIcon          } IconFile { '<res>/images/SubModelEnd-24.png'     } DefaultSize { 0.5 0.5 0.0 m }
ServerAndQueue      JavaClass { com.jaamsim.SubModels.ServerAndQueue} Palette { 'Pre-built SubModels'}  DefaultDisplayModel { ServerAndQueueIcon       } IconFile { '<res>/images/ServerAndQueue-24.png'  }

SubModel            Description { 'A collection of objects that can be used in multiple places within a model.' }
SubModelClone       Description { 'A clone of a sub-model.' }
SubModelStart       Description { 'The first component of a sub-model.' }
SubModelEnd         Description { 'The last component of a sub-model.' }
ServerAndQueue      Description { 'A sub-model that contains a Server and its Queue.' }
