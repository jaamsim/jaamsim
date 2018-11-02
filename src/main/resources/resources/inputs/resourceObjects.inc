Define ObjectType {
                    Resource
                    ResourcePool
                    ResourceUnit
                  }

Define IconModel  {
                    ResourceIconModel
                    ResourcePoolIconModel
                    ResourceUnitIconModel
                  }

Define ShapeModel {
                    ResourceUnitModelDefault
                  }
ResourceUnitModelDefault  Shape { PENTAGRAM }

ResourceIconModel        ImageFile { '<res>/images/Resource-256.png'       }  Transparent { TRUE }
ResourcePoolIconModel    ImageFile { '<res>/images/ResourcePool-256.png'   }  Transparent { TRUE }
ResourceUnitIconModel    ImageFile { '<res>/images/ResourceUnit-256.png'   }  Transparent { TRUE }

Resource            JavaClass { com.jaamsim.resourceObjects.Resource       } Palette { 'Resource Objects' } DefaultDisplayModel { ResourceIconModel     } IconFile { '<res>/images/Resource-24.png'     }
ResourcePool        JavaClass { com.jaamsim.resourceObjects.ResourcePool   } Palette { 'Resource Objects' } DefaultDisplayModel { ResourcePoolIconModel } IconFile { '<res>/images/ResourcePool-24.png' }
ResourceUnit        JavaClass { com.jaamsim.resourceObjects.ResourceUnit   } Palette { 'Resource Objects' } DefaultDisplayModel { ResourceUnitModelDefault } IconFile { '<res>/images/ResourceUnit-24.png' } DefaultSize { 0.4 0.4 0.0 m }

Resource            Description { 'Set of identical resource units that can be seized and released by various processes.' }
ResourcePool        Description { 'Set of unique resource units that can be seized and released by various processes.'    }
ResourceUnit        Description { 'A single resource that can be seized and released individually from a ResourcePool.'   }
