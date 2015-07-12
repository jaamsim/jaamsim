Define			ShapeModel				{ Pixels }
Pixels			Shape					{ PIXELS }
Define			ShapeModel				{ Rectangle }
Rectangle		Shape					{ RECTANGLE }
Define				ShapeModel			{ Circle }
Circle				Shape				{ CIRCLE }
Define				ShapeModel			{ Arrow2D }
Arrow2D				Shape				{ ARROW2D }
Define				ShapeModel			{ Triangle }
Triangle			Shape				{ TRIANGLE }

Define				ShapeModel			{ BarGauge2D }
BarGauge2D			Shape				{ BARGAUGE2D }

Define				ColladaModel		{ CrossHairs }
CrossHairs			ColladaFile			{ '<res>/shapes/CrossHairs.dae' }

Define		ColladaModel	{ Cube }
Cube		ColladaFile		{ '<res>/shapes/cube.dae' }
DisplayEntity	DefaultDisplayModel { Cube }

Define		ColladaModel	{ Sphere }
Sphere		ColladaFile		{ '<res>/shapes/Sphere.dae' }
