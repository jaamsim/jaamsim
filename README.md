# What is this?

JaamSim is a Java based discrete-event simulation environment developed since 2002 and includes: 
- drag-and-drop user interface 
- interactive 3D graphics 
- input and output processing 
- model development tools and editors. 

Examples of our simulation models can be seen at: 
www.youtube.com/c/jaamsim. 

The key feature that makes JaamSim different from commercial off-the-shelf 
simulation software is that it allows a user to develop new palettes of 
high-level objects for a given application. These objects will automatically 
have 3D graphics, be available in the drag-and-drop interface, and have their 
inputs editable through the Input Editor. Users can focus on the logic for their 
objects without having to program a user interface and input/output processing. 

All the coding for new objects is done in standard Java using standard 
development tools such as Eclipse. There is no need for the specialised 
simulation languages, process flow diagrams, or scripting languages used by 
commercial off-the-shelf simulation software. Model logic can be coded directly 
in either a event- or process-oriented style using a few simple classes and 
methods provided by JaamSim.

The present release includes the following palettes:
- Graphic objects: static 3D objects, overlay text, clock, arrow, graph, etc.
- Probability distributions: uniform, triangular, normal, erlang, gamma, weibull, etc.
- Basic objects: generator, sink, server, queue, delay, resource, branch, time series, etc.
- Calculation objects: weighted sum, polynomial, integrator, differentiator, etc.
- Fluid objects: tank, pipe, pump, etc.

The JaamSim executable, user manuals, examples, and technical articles can be downloaded
from:
http://jaamsim.com

# Dependencies and Installation

A copy of all dependencies is shipped in the jar/ folder and are as follows:
- JOGL2 (from jogamp.org)

Run the ant build tool in the topmost directory to produce build products
in build/jars/

# License

JaamSim is Licensed under the Apache License, Version 2.0

# Contact

For licensing and general inquiries:
Dr. Harry King <d.harry.king@gmail.com>

If you have any problems building the source or technical inquiries:
Harvey Harrison <harvey.harrison@gmail.com>

# Citing JaamSim

[![DOI](https://zenodo.org/badge/doi/10.5281/zenodo.57118.svg)](http://dx.doi.org/10.5281/zenodo.57118)

Please Cite JaamSim as the following, replacing the version with the version used in your paper.

JaamSim Development Team (2016). JaamSim: Discrete-Event Simulation Software. Version 2016-14. URL http://jaamsim.com. doi:10.5281/zenodo.57118

BibText entry:

@Manual{,
    title = {JaamSim: Discrete-Event Simulation Software},
    author = {{JaamSim Development Team}},
    year = {2016},
    note = {Version 2016-14},
    url = {http://jaamsim.com},
    doi = {10.5281/zenodo.57118}
  }
