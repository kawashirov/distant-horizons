#version 150 core

in vec3 vPosition;
in vec4 color;

out vec4 vertexColor;
//out vec2 textureCoord;

uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrix;


/** 
 * Vertex Shader
 * 
 * author: James Seibel
 * version: 11-8-2021
 */
void main()
{
	// TODO: add a simple white texture to support Optifine shaders
	//textureCoord = textureCoord;
	
	vertexColor = color;
    
	// the vPosition needs to be converted to a vec4 so it can be multiplied
	// by the 4x4 matrices
    gl_Position = projectionMatrix * modelViewMatrix * vec4(vPosition, 1);
}
