import { initShaders, vec4, flatten } from "../../helperfunctions.js";
let gl;
let canvas;
let program;
window.onload = function init() {
    canvas = document.getElementById("gl-canvas");
    gl = canvas.getContext("webgl2");
    if (!gl) {
        alert("WebGL isn't available");
    }
    program = initShaders(gl, "vertex-shader", "fragment-shader");
    gl.useProgram(program);
    // creating and buffering data
    let trianglePoints = []; // create empty array
    // large square
    trianglePoints.push(new vec4(0, -0.2, 0, 1));
    trianglePoints.push(new vec4(0.4, 0.2, 0, 1));
    trianglePoints.push(new vec4(0, 0.6, 0, 1));
    trianglePoints.push(new vec4(0, 0.6, 0, 1));
    trianglePoints.push(new vec4(0, -0.2, 0, 1));
    trianglePoints.push(new vec4(-.4, 0.2, 0, 1));
    // bottom small square
    trianglePoints.push(new vec4(0, -0.3, 0, 1));
    trianglePoints.push(new vec4(0.175, -0.475, 0, 1));
    trianglePoints.push(new vec4(0, -0.65, 0, 1));
    trianglePoints.push(new vec4(0, -0.3, 0, 1));
    trianglePoints.push(new vec4(0, -0.65, 0, 1));
    trianglePoints.push(new vec4(-0.175, -0.475, 0, 1));
    let offset = 0.225;
    // center right small square
    trianglePoints.push(new vec4(0 + offset, -0.3 + offset, 0, 1));
    trianglePoints.push(new vec4(0.175 + offset, -0.475 + offset, 0, 1));
    trianglePoints.push(new vec4(0 + offset, -0.65 + offset, 0, 1));
    trianglePoints.push(new vec4(0 + offset, -0.3 + offset, 0, 1));
    trianglePoints.push(new vec4(0 + offset, -0.65 + offset, 0, 1));
    trianglePoints.push(new vec4(-0.175 + offset, -0.475 + offset, 0, 1));
    offset = offset * 2;
    // top right small square
    trianglePoints.push(new vec4(0 + offset, -0.3 + offset, 0, 1));
    trianglePoints.push(new vec4(0.175 + offset, -0.475 + offset, 0, 1));
    trianglePoints.push(new vec4(0 + offset, -0.65 + offset, 0, 1));
    trianglePoints.push(new vec4(0 + offset, -0.3 + offset, 0, 1));
    trianglePoints.push(new vec4(0 + offset, -0.65 + offset, 0, 1));
    trianglePoints.push(new vec4(-0.175 + offset, -0.475 + offset, 0, 1));
    offset = 0.225;
    // center right small square
    trianglePoints.push(new vec4(0 - offset, -0.3 + offset, 0, 1));
    trianglePoints.push(new vec4(0.175 - offset, -0.475 + offset, 0, 1));
    trianglePoints.push(new vec4(0 - offset, -0.65 + offset, 0, 1));
    trianglePoints.push(new vec4(0 - offset, -0.3 + offset, 0, 1));
    trianglePoints.push(new vec4(0 - offset, -0.65 + offset, 0, 1));
    trianglePoints.push(new vec4(-0.175 - offset, -0.475 + offset, 0, 1));
    offset = offset * 2;
    // top right small square
    trianglePoints.push(new vec4(0 - offset, -0.3 + offset, 0, 1));
    trianglePoints.push(new vec4(0.175 - offset, -0.475 + offset, 0, 1));
    trianglePoints.push(new vec4(0 - offset, -0.65 + offset, 0, 1));
    trianglePoints.push(new vec4(0 - offset, -0.3 + offset, 0, 1));
    trianglePoints.push(new vec4(0 - offset, -0.65 + offset, 0, 1));
    trianglePoints.push(new vec4(-0.175 - offset, -0.475 + offset, 0, 1));
    //note that this is default viewing, so we have to stay between -1 and 1 for all coordinates
    let bufferID = gl.createBuffer(); // create handle/reference to GPU memory spot
    gl.bindBuffer(gl.ARRAY_BUFFER, bufferID);
    gl.bufferData(gl.ARRAY_BUFFER, flatten(trianglePoints), gl.STATIC_DRAW); // send from main memory to GPU memory
    // flatten converts data to 1D array
    let vPosition = gl.getAttribLocation(program, "vPosition");
    gl.vertexAttribPointer(vPosition, 4, gl.FLOAT, false, 0, 0);
    gl.enableVertexAttribArray(vPosition);
    // end creating and buffering data
    gl.viewport(0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight); // choose where we are going to draw to on the page
    gl.clearColor(0.0, 0.0, 0.0, 1.0); // what is the background color (where we haven't drawn anything)
    render(); // draw the frame
};
function render() {
    gl.clear(gl.COLOR_BUFFER_BIT); // remove everything that we drew last frame
    gl.drawArrays(gl.TRIANGLES, 0, 36);
}
//# sourceMappingURL=hellofunctions.js.map