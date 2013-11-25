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

Define				DisplayModel		{ BarGauge2D }
BarGauge2D			Shape				{ BARGAUGE2D }

Define				ColladaModel		{ CrossHairs }
CrossHairs			ColladaFile			{ '<res>/shapes/CrossHairs.dae' }

Define		ColladaModel	{ Cube }
Cube		ColladaFile		{ '<res>/shapes/cube.dae' }
DisplayEntity	DefaultDisplayModel { Cube }

Define		ColladaModel	{ Sphere }
Sphere		ColladaFile		{ '<res>/shapes/Sphere.dae' }
