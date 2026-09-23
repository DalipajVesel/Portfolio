# WebGL Graphics

A 3D scene written in plain WebGL. It uses only gl-matrix for the matrix
maths and webgl-debug for error checking. The shaders, buffers, transforms,
texturing, camera and animation are written by hand. The scene is a dog made
of boxes, built in four steps, and every step is kept as its own page.

Computer Graphics semester project, spring 2025-26.

## Build and run

The pages open directly in Firefox. Chrome needs the folder served first,
for example with python -m http.server, so that it loads the textures.
