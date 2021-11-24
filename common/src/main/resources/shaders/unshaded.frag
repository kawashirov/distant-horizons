#version 150 core

in vec4 vertexColor;
//in vec2 textureCoord;

out vec4 fragColor;

//uniform sampler2D texImage;


/** 
 * Fragment Shader
 * 
 * author: James Seibel
 * version: 11-8-2021
 */
void main()
{
	// TODO: add a white texture to support Optifine shaders
    //vec4 textureColor = texture(texImage, textureCoord);
    //fragColor = vertexColor * textureColor;
    
	
	// very simple fragment shader, just return the vertix's color
	fragColor = vertexColor;
}
