Define			DisplayModel			{ Pixels }
Pixels			Shape					{ PIXELS }
Define			DisplayModel			{ Rectangle }
Rectangle		Shape					{ RECTANGLE }
Define				DisplayModel		{ Circle }
Circle				Shape				{ CIRCLE }
Define				DisplayModel		{ Arrow2D }
Arrow2D				Shape				{ ARROW2D }
Define				DisplayModel		{ Triangle }
Triangle			Shape				{ TRIANGLE }

Define				ColladaModel		{ CrossHairs }
CrossHairs			ColladaFile			{ '/resources/shapes/CrossHairs.dae' }

Define		ColladaModel	{ Cube }
Cube		ColladaFile		{ '/resources/shapes/cube.dae' }
DisplayEntity	DefaultDisplayModel { Cube }

Define		ColladaModel	{ Sphere }
Sphere		ColladaFile		{ '/resources/shapes/Sphere.dae' }
