var Md=0,Ph=1,Ed=2;var qu=1,Sd=2,qn=3,Dn=0,en=1,Me=2,xi=0,yi=1,bs=2,Lh=3,Dh=4,Td=5,Bi=100,wd=101,bd=102,Ad=103,Rd=104,Cd=200,Fd=201,Id=202,Pd=203,ol=204,al=205,Ld=206,Dd=207,Ud=208,Nd=209,Bd=210,zd=211,Od=212,Hd=213,kd=214,ll=0,cl=1,hl=2,As=3,ul=4,fl=5,dl=6,pl=7,Uc=0,Gd=1,Vd=2,_i=0,Wd=1,Xd=2,qd=3,Yd=4,Zd=5,$d=6,Jd=7;var Yu=300,Rs=301,Cs=302,ml=303,gl=304,Qo=306,un=1e3,Oi=1001,xl=1002,hn=1003,Kd=1004;var Or=1005;var Pn=1006,Ta=1007;var Hi=1008;var Kn=1009,Zu=1010,$u=1011,fr=1012,Nc=1013,ki=1014,Ln=1015,Rr=1016,Bc=1017,zc=1018,Fs=1020,Ju=35902,Ku=1021,ju=1022,bn=1023,Qu=1024,tf=1025,Ss=1026,Is=1027,Oc=1028,Hc=1029,ef=1030,kc=1031;var Gc=1033,_o=33776,vo=33777,Mo=33778,Eo=33779,yl=35840,_l=35841,vl=35842,Ml=35843,El=36196,Sl=37492,Tl=37496,wl=37808,bl=37809,Al=37810,Rl=37811,Cl=37812,Fl=37813,Il=37814,Pl=37815,Ll=37816,Dl=37817,Ul=37818,Nl=37819,Bl=37820,zl=37821,So=36492,Ol=36494,Hl=36495,nf=36283,kl=36284,Gl=36285,Vl=36286;var To=2300,Wl=2301,wa=2302,Uh=2400,Nh=2401,Bh=2402;var jd=3200,Qd=3201;var sf=0,tp=1,mi="",ze="srgb",Us="srgb-linear",ta="linear",ye="srgb";var ns=7680;var zh=519,ep=512,np=513,ip=514,rf=515,sp=516,rp=517,op=518,ap=519,Xl=35044;var Oh="300 es",Zn=2e3,wo=2001,vi=class{addEventListener(t,e){this._listeners===void 0&&(this._listeners={});let n=this._listeners;n[t]===void 0&&(n[t]=[]),n[t].indexOf(e)===-1&&n[t].push(e)}hasEventListener(t,e){if(this._listeners===void 0)return!1;let n=this._listeners;return n[t]!==void 0&&n[t].indexOf(e)!==-1}removeEventListener(t,e){if(this._listeners===void 0)return;let s=this._listeners[t];if(s!==void 0){let r=s.indexOf(e);r!==-1&&s.splice(r,1)}}dispatchEvent(t){if(this._listeners===void 0)return;let n=this._listeners[t.type];if(n!==void 0){t.target=this;let s=n.slice(0);for(let r=0,o=s.length;r<o;r++)s[r].call(this,t);t.target=null}}},Ze=["00","01","02","03","04","05","06","07","08","09","0a","0b","0c","0d","0e","0f","10","11","12","13","14","15","16","17","18","19","1a","1b","1c","1d","1e","1f","20","21","22","23","24","25","26","27","28","29","2a","2b","2c","2d","2e","2f","30","31","32","33","34","35","36","37","38","39","3a","3b","3c","3d","3e","3f","40","41","42","43","44","45","46","47","48","49","4a","4b","4c","4d","4e","4f","50","51","52","53","54","55","56","57","58","59","5a","5b","5c","5d","5e","5f","60","61","62","63","64","65","66","67","68","69","6a","6b","6c","6d","6e","6f","70","71","72","73","74","75","76","77","78","79","7a","7b","7c","7d","7e","7f","80","81","82","83","84","85","86","87","88","89","8a","8b","8c","8d","8e","8f","90","91","92","93","94","95","96","97","98","99","9a","9b","9c","9d","9e","9f","a0","a1","a2","a3","a4","a5","a6","a7","a8","a9","aa","ab","ac","ad","ae","af","b0","b1","b2","b3","b4","b5","b6","b7","b8","b9","ba","bb","bc","bd","be","bf","c0","c1","c2","c3","c4","c5","c6","c7","c8","c9","ca","cb","cc","cd","ce","cf","d0","d1","d2","d3","d4","d5","d6","d7","d8","d9","da","db","dc","dd","de","df","e0","e1","e2","e3","e4","e5","e6","e7","e8","e9","ea","eb","ec","ed","ee","ef","f0","f1","f2","f3","f4","f5","f6","f7","f8","f9","fa","fb","fc","fd","fe","ff"];var ba=Math.PI/180,ql=180/Math.PI;function $n(){let i=Math.random()*4294967295|0,t=Math.random()*4294967295|0,e=Math.random()*4294967295|0,n=Math.random()*4294967295|0;return(Ze[i&255]+Ze[i>>8&255]+Ze[i>>16&255]+Ze[i>>24&255]+"-"+Ze[t&255]+Ze[t>>8&255]+"-"+Ze[t>>16&15|64]+Ze[t>>24&255]+"-"+Ze[e&63|128]+Ze[e>>8&255]+"-"+Ze[e>>16&255]+Ze[e>>24&255]+Ze[n&255]+Ze[n>>8&255]+Ze[n>>16&255]+Ze[n>>24&255]).toLowerCase()}function Xe(i,t,e){return Math.max(t,Math.min(e,i))}function lp(i,t){return(i%t+t)%t}function Aa(i,t,e){return(1-e)*i+e*t}function In(i,t){switch(t.constructor){case Float32Array:return i;case Uint32Array:return i/4294967295;case Uint16Array:return i/65535;case Uint8Array:return i/255;case Int32Array:return Math.max(i/2147483647,-1);case Int16Array:return Math.max(i/32767,-1);case Int8Array:return Math.max(i/127,-1);default:throw new Error("Invalid component type.")}}function _e(i,t){switch(t.constructor){case Float32Array:return i;case Uint32Array:return Math.round(i*4294967295);case Uint16Array:return Math.round(i*65535);case Uint8Array:return Math.round(i*255);case Int32Array:return Math.round(i*2147483647);case Int16Array:return Math.round(i*32767);case Int8Array:return Math.round(i*127);default:throw new Error("Invalid component type.")}}var lt=class i{constructor(t=0,e=0){i.prototype.isVector2=!0,this.x=t,this.y=e}get width(){return this.x}set width(t){this.x=t}get height(){return this.y}set height(t){this.y=t}set(t,e){return this.x=t,this.y=e,this}setScalar(t){return this.x=t,this.y=t,this}setX(t){return this.x=t,this}setY(t){return this.y=t,this}setComponent(t,e){switch(t){case 0:this.x=e;break;case 1:this.y=e;break;default:throw new Error("index is out of range: "+t)}return this}getComponent(t){switch(t){case 0:return this.x;case 1:return this.y;default:throw new Error("index is out of range: "+t)}}clone(){return new this.constructor(this.x,this.y)}copy(t){return this.x=t.x,this.y=t.y,this}add(t){return this.x+=t.x,this.y+=t.y,this}addScalar(t){return this.x+=t,this.y+=t,this}addVectors(t,e){return this.x=t.x+e.x,this.y=t.y+e.y,this}addScaledVector(t,e){return this.x+=t.x*e,this.y+=t.y*e,this}sub(t){return this.x-=t.x,this.y-=t.y,this}subScalar(t){return this.x-=t,this.y-=t,this}subVectors(t,e){return this.x=t.x-e.x,this.y=t.y-e.y,this}multiply(t){return this.x*=t.x,this.y*=t.y,this}multiplyScalar(t){return this.x*=t,this.y*=t,this}divide(t){return this.x/=t.x,this.y/=t.y,this}divideScalar(t){return this.multiplyScalar(1/t)}applyMatrix3(t){let e=this.x,n=this.y,s=t.elements;return this.x=s[0]*e+s[3]*n+s[6],this.y=s[1]*e+s[4]*n+s[7],this}min(t){return this.x=Math.min(this.x,t.x),this.y=Math.min(this.y,t.y),this}max(t){return this.x=Math.max(this.x,t.x),this.y=Math.max(this.y,t.y),this}clamp(t,e){return this.x=Math.max(t.x,Math.min(e.x,this.x)),this.y=Math.max(t.y,Math.min(e.y,this.y)),this}clampScalar(t,e){return this.x=Math.max(t,Math.min(e,this.x)),this.y=Math.max(t,Math.min(e,this.y)),this}clampLength(t,e){let n=this.length();return this.divideScalar(n||1).multiplyScalar(Math.max(t,Math.min(e,n)))}floor(){return this.x=Math.floor(this.x),this.y=Math.floor(this.y),this}ceil(){return this.x=Math.ceil(this.x),this.y=Math.ceil(this.y),this}round(){return this.x=Math.round(this.x),this.y=Math.round(this.y),this}roundToZero(){return this.x=Math.trunc(this.x),this.y=Math.trunc(this.y),this}negate(){return this.x=-this.x,this.y=-this.y,this}dot(t){return this.x*t.x+this.y*t.y}cross(t){return this.x*t.y-this.y*t.x}lengthSq(){return this.x*this.x+this.y*this.y}length(){return Math.sqrt(this.x*this.x+this.y*this.y)}manhattanLength(){return Math.abs(this.x)+Math.abs(this.y)}normalize(){return this.divideScalar(this.length()||1)}angle(){return Math.atan2(-this.y,-this.x)+Math.PI}angleTo(t){let e=Math.sqrt(this.lengthSq()*t.lengthSq());if(e===0)return Math.PI/2;let n=this.dot(t)/e;return Math.acos(Xe(n,-1,1))}distanceTo(t){return Math.sqrt(this.distanceToSquared(t))}distanceToSquared(t){let e=this.x-t.x,n=this.y-t.y;return e*e+n*n}manhattanDistanceTo(t){return Math.abs(this.x-t.x)+Math.abs(this.y-t.y)}setLength(t){return this.normalize().multiplyScalar(t)}lerp(t,e){return this.x+=(t.x-this.x)*e,this.y+=(t.y-this.y)*e,this}lerpVectors(t,e,n){return this.x=t.x+(e.x-t.x)*n,this.y=t.y+(e.y-t.y)*n,this}equals(t){return t.x===this.x&&t.y===this.y}fromArray(t,e=0){return this.x=t[e],this.y=t[e+1],this}toArray(t=[],e=0){return t[e]=this.x,t[e+1]=this.y,t}fromBufferAttribute(t,e){return this.x=t.getX(e),this.y=t.getY(e),this}rotateAround(t,e){let n=Math.cos(e),s=Math.sin(e),r=this.x-t.x,o=this.y-t.y;return this.x=r*n-o*s+t.x,this.y=r*s+o*n+t.y,this}random(){return this.x=Math.random(),this.y=Math.random(),this}*[Symbol.iterator](){yield this.x,yield this.y}},$t=class i{constructor(t,e,n,s,r,o,a,l,h){i.prototype.isMatrix3=!0,this.elements=[1,0,0,0,1,0,0,0,1],t!==void 0&&this.set(t,e,n,s,r,o,a,l,h)}set(t,e,n,s,r,o,a,l,h){let u=this.elements;return u[0]=t,u[1]=s,u[2]=a,u[3]=e,u[4]=r,u[5]=l,u[6]=n,u[7]=o,u[8]=h,this}identity(){return this.set(1,0,0,0,1,0,0,0,1),this}copy(t){let e=this.elements,n=t.elements;return e[0]=n[0],e[1]=n[1],e[2]=n[2],e[3]=n[3],e[4]=n[4],e[5]=n[5],e[6]=n[6],e[7]=n[7],e[8]=n[8],this}extractBasis(t,e,n){return t.setFromMatrix3Column(this,0),e.setFromMatrix3Column(this,1),n.setFromMatrix3Column(this,2),this}setFromMatrix4(t){let e=t.elements;return this.set(e[0],e[4],e[8],e[1],e[5],e[9],e[2],e[6],e[10]),this}multiply(t){return this.multiplyMatrices(this,t)}premultiply(t){return this.multiplyMatrices(t,this)}multiplyMatrices(t,e){let n=t.elements,s=e.elements,r=this.elements,o=n[0],a=n[3],l=n[6],h=n[1],u=n[4],m=n[7],x=n[2],f=n[5],v=n[8],d=s[0],y=s[3],c=s[6],_=s[1],p=s[4],g=s[7],T=s[2],S=s[5],b=s[8];return r[0]=o*d+a*_+l*T,r[3]=o*y+a*p+l*S,r[6]=o*c+a*g+l*b,r[1]=h*d+u*_+m*T,r[4]=h*y+u*p+m*S,r[7]=h*c+u*g+m*b,r[2]=x*d+f*_+v*T,r[5]=x*y+f*p+v*S,r[8]=x*c+f*g+v*b,this}multiplyScalar(t){let e=this.elements;return e[0]*=t,e[3]*=t,e[6]*=t,e[1]*=t,e[4]*=t,e[7]*=t,e[2]*=t,e[5]*=t,e[8]*=t,this}determinant(){let t=this.elements,e=t[0],n=t[1],s=t[2],r=t[3],o=t[4],a=t[5],l=t[6],h=t[7],u=t[8];return e*o*u-e*a*h-n*r*u+n*a*l+s*r*h-s*o*l}invert(){let t=this.elements,e=t[0],n=t[1],s=t[2],r=t[3],o=t[4],a=t[5],l=t[6],h=t[7],u=t[8],m=u*o-a*h,x=a*l-u*r,f=h*r-o*l,v=e*m+n*x+s*f;if(v===0)return this.set(0,0,0,0,0,0,0,0,0);let d=1/v;return t[0]=m*d,t[1]=(s*h-u*n)*d,t[2]=(a*n-s*o)*d,t[3]=x*d,t[4]=(u*e-s*l)*d,t[5]=(s*r-a*e)*d,t[6]=f*d,t[7]=(n*l-h*e)*d,t[8]=(o*e-n*r)*d,this}transpose(){let t,e=this.elements;return t=e[1],e[1]=e[3],e[3]=t,t=e[2],e[2]=e[6],e[6]=t,t=e[5],e[5]=e[7],e[7]=t,this}getNormalMatrix(t){return this.setFromMatrix4(t).invert().transpose()}transposeIntoArray(t){let e=this.elements;return t[0]=e[0],t[1]=e[3],t[2]=e[6],t[3]=e[1],t[4]=e[4],t[5]=e[7],t[6]=e[2],t[7]=e[5],t[8]=e[8],this}setUvTransform(t,e,n,s,r,o,a){let l=Math.cos(r),h=Math.sin(r);return this.set(n*l,n*h,-n*(l*o+h*a)+o+t,-s*h,s*l,-s*(-h*o+l*a)+a+e,0,0,1),this}scale(t,e){return this.premultiply(Ra.makeScale(t,e)),this}rotate(t){return this.premultiply(Ra.makeRotation(-t)),this}translate(t,e){return this.premultiply(Ra.makeTranslation(t,e)),this}makeTranslation(t,e){return t.isVector2?this.set(1,0,t.x,0,1,t.y,0,0,1):this.set(1,0,t,0,1,e,0,0,1),this}makeRotation(t){let e=Math.cos(t),n=Math.sin(t);return this.set(e,-n,0,n,e,0,0,0,1),this}makeScale(t,e){return this.set(t,0,0,0,e,0,0,0,1),this}equals(t){let e=this.elements,n=t.elements;for(let s=0;s<9;s++)if(e[s]!==n[s])return!1;return!0}fromArray(t,e=0){for(let n=0;n<9;n++)this.elements[n]=t[n+e];return this}toArray(t=[],e=0){let n=this.elements;return t[e]=n[0],t[e+1]=n[1],t[e+2]=n[2],t[e+3]=n[3],t[e+4]=n[4],t[e+5]=n[5],t[e+6]=n[6],t[e+7]=n[7],t[e+8]=n[8],t}clone(){return new this.constructor().fromArray(this.elements)}},Ra=new $t;function of(i){for(let t=i.length-1;t>=0;--t)if(i[t]>=65535)return!0;return!1}function bo(i){return document.createElementNS("http://www.w3.org/1999/xhtml",i)}function cp(){let i=bo("canvas");return i.style.display="block",i}var Hh={};function or(i){i in Hh||(Hh[i]=!0,console.warn(i))}function hp(i,t,e){return new Promise(function(n,s){function r(){switch(i.clientWaitSync(t,i.SYNC_FLUSH_COMMANDS_BIT,0)){case i.WAIT_FAILED:s();break;case i.TIMEOUT_EXPIRED:setTimeout(r,e);break;default:n()}}setTimeout(r,e)})}function up(i){let t=i.elements;t[2]=.5*t[2]+.5*t[3],t[6]=.5*t[6]+.5*t[7],t[10]=.5*t[10]+.5*t[11],t[14]=.5*t[14]+.5*t[15]}function fp(i){let t=i.elements;t[11]===-1?(t[10]=-t[10]-1,t[14]=-t[14]):(t[10]=-t[10],t[14]=-t[14]+1)}var ie={enabled:!0,workingColorSpace:Us,spaces:{},convert:function(i,t,e){return this.enabled===!1||t===e||!t||!e||(this.spaces[t].transfer===ye&&(i.r=Jn(i.r),i.g=Jn(i.g),i.b=Jn(i.b)),this.spaces[t].primaries!==this.spaces[e].primaries&&(i.applyMatrix3(this.spaces[t].toXYZ),i.applyMatrix3(this.spaces[e].fromXYZ)),this.spaces[e].transfer===ye&&(i.r=Ts(i.r),i.g=Ts(i.g),i.b=Ts(i.b))),i},fromWorkingColorSpace:function(i,t){return this.convert(i,this.workingColorSpace,t)},toWorkingColorSpace:function(i,t){return this.convert(i,t,this.workingColorSpace)},getPrimaries:function(i){return this.spaces[i].primaries},getTransfer:function(i){return i===mi?ta:this.spaces[i].transfer},getLuminanceCoefficients:function(i,t=this.workingColorSpace){return i.fromArray(this.spaces[t].luminanceCoefficients)},define:function(i){Object.assign(this.spaces,i)},_getMatrix:function(i,t,e){return i.copy(this.spaces[t].toXYZ).multiply(this.spaces[e].fromXYZ)},_getDrawingBufferColorSpace:function(i){return this.spaces[i].outputColorSpaceConfig.drawingBufferColorSpace},_getUnpackColorSpace:function(i=this.workingColorSpace){return this.spaces[i].workingColorSpaceConfig.unpackColorSpace}};function Jn(i){return i<.04045?i*.0773993808:Math.pow(i*.9478672986+.0521327014,2.4)}function Ts(i){return i<.0031308?i*12.92:1.055*Math.pow(i,.41666)-.055}var kh=[.64,.33,.3,.6,.15,.06],Gh=[.2126,.7152,.0722],Vh=[.3127,.329],Wh=new $t().set(.4123908,.3575843,.1804808,.212639,.7151687,.0721923,.0193308,.1191948,.9505322),Xh=new $t().set(3.2409699,-1.5373832,-.4986108,-.9692436,1.8759675,.0415551,.0556301,-.203977,1.0569715);ie.define({[Us]:{primaries:kh,whitePoint:Vh,transfer:ta,toXYZ:Wh,fromXYZ:Xh,luminanceCoefficients:Gh,workingColorSpaceConfig:{unpackColorSpace:ze},outputColorSpaceConfig:{drawingBufferColorSpace:ze}},[ze]:{primaries:kh,whitePoint:Vh,transfer:ye,toXYZ:Wh,fromXYZ:Xh,luminanceCoefficients:Gh,outputColorSpaceConfig:{drawingBufferColorSpace:ze}}});var is,Yl=class{static getDataURL(t){if(/^data:/i.test(t.src)||typeof HTMLCanvasElement>"u")return t.src;let e;if(t instanceof HTMLCanvasElement)e=t;else{is===void 0&&(is=bo("canvas")),is.width=t.width,is.height=t.height;let n=is.getContext("2d");t instanceof ImageData?n.putImageData(t,0,0):n.drawImage(t,0,0,t.width,t.height),e=is}return e.width>2048||e.height>2048?(console.warn("THREE.ImageUtils.getDataURL: Image converted to jpg for performance reasons",t),e.toDataURL("image/jpeg",.6)):e.toDataURL("image/png")}static sRGBToLinear(t){if(typeof HTMLImageElement<"u"&&t instanceof HTMLImageElement||typeof HTMLCanvasElement<"u"&&t instanceof HTMLCanvasElement||typeof ImageBitmap<"u"&&t instanceof ImageBitmap){let e=bo("canvas");e.width=t.width,e.height=t.height;let n=e.getContext("2d");n.drawImage(t,0,0,t.width,t.height);let s=n.getImageData(0,0,t.width,t.height),r=s.data;for(let o=0;o<r.length;o++)r[o]=Jn(r[o]/255)*255;return n.putImageData(s,0,0),e}else if(t.data){let e=t.data.slice(0);for(let n=0;n<e.length;n++)e instanceof Uint8Array||e instanceof Uint8ClampedArray?e[n]=Math.floor(Jn(e[n]/255)*255):e[n]=Jn(e[n]);return{data:e,width:t.width,height:t.height}}else return console.warn("THREE.ImageUtils.sRGBToLinear(): Unsupported image type. No color space conversion applied."),t}},dp=0,Ao=class{constructor(t=null){this.isSource=!0,Object.defineProperty(this,"id",{value:dp++}),this.uuid=$n(),this.data=t,this.dataReady=!0,this.version=0}set needsUpdate(t){t===!0&&this.version++}toJSON(t){let e=t===void 0||typeof t=="string";if(!e&&t.images[this.uuid]!==void 0)return t.images[this.uuid];let n={uuid:this.uuid,url:""},s=this.data;if(s!==null){let r;if(Array.isArray(s)){r=[];for(let o=0,a=s.length;o<a;o++)s[o].isDataTexture?r.push(Ca(s[o].image)):r.push(Ca(s[o]))}else r=Ca(s);n.url=r}return e||(t.images[this.uuid]=n),n}};function Ca(i){return typeof HTMLImageElement<"u"&&i instanceof HTMLImageElement||typeof HTMLCanvasElement<"u"&&i instanceof HTMLCanvasElement||typeof ImageBitmap<"u"&&i instanceof ImageBitmap?Yl.getDataURL(i):i.data?{data:Array.from(i.data),width:i.width,height:i.height,type:i.data.constructor.name}:(console.warn("THREE.Texture: Unable to serialize Texture."),{})}var pp=0,nn=class i extends vi{constructor(t=i.DEFAULT_IMAGE,e=i.DEFAULT_MAPPING,n=Oi,s=Oi,r=Pn,o=Hi,a=bn,l=Kn,h=i.DEFAULT_ANISOTROPY,u=mi){super(),this.isTexture=!0,Object.defineProperty(this,"id",{value:pp++}),this.uuid=$n(),this.name="",this.source=new Ao(t),this.mipmaps=[],this.mapping=e,this.channel=0,this.wrapS=n,this.wrapT=s,this.magFilter=r,this.minFilter=o,this.anisotropy=h,this.format=a,this.internalFormat=null,this.type=l,this.offset=new lt(0,0),this.repeat=new lt(1,1),this.center=new lt(0,0),this.rotation=0,this.matrixAutoUpdate=!0,this.matrix=new $t,this.generateMipmaps=!0,this.premultiplyAlpha=!1,this.flipY=!0,this.unpackAlignment=4,this.colorSpace=u,this.userData={},this.version=0,this.onUpdate=null,this.isRenderTargetTexture=!1,this.pmremVersion=0}get image(){return this.source.data}set image(t=null){this.source.data=t}updateMatrix(){this.matrix.setUvTransform(this.offset.x,this.offset.y,this.repeat.x,this.repeat.y,this.rotation,this.center.x,this.center.y)}clone(){return new this.constructor().copy(this)}copy(t){return this.name=t.name,this.source=t.source,this.mipmaps=t.mipmaps.slice(0),this.mapping=t.mapping,this.channel=t.channel,this.wrapS=t.wrapS,this.wrapT=t.wrapT,this.magFilter=t.magFilter,this.minFilter=t.minFilter,this.anisotropy=t.anisotropy,this.format=t.format,this.internalFormat=t.internalFormat,this.type=t.type,this.offset.copy(t.offset),this.repeat.copy(t.repeat),this.center.copy(t.center),this.rotation=t.rotation,this.matrixAutoUpdate=t.matrixAutoUpdate,this.matrix.copy(t.matrix),this.generateMipmaps=t.generateMipmaps,this.premultiplyAlpha=t.premultiplyAlpha,this.flipY=t.flipY,this.unpackAlignment=t.unpackAlignment,this.colorSpace=t.colorSpace,this.userData=JSON.parse(JSON.stringify(t.userData)),this.needsUpdate=!0,this}toJSON(t){let e=t===void 0||typeof t=="string";if(!e&&t.textures[this.uuid]!==void 0)return t.textures[this.uuid];let n={metadata:{version:4.6,type:"Texture",generator:"Texture.toJSON"},uuid:this.uuid,name:this.name,image:this.source.toJSON(t).uuid,mapping:this.mapping,channel:this.channel,repeat:[this.repeat.x,this.repeat.y],offset:[this.offset.x,this.offset.y],center:[this.center.x,this.center.y],rotation:this.rotation,wrap:[this.wrapS,this.wrapT],format:this.format,internalFormat:this.internalFormat,type:this.type,colorSpace:this.colorSpace,minFilter:this.minFilter,magFilter:this.magFilter,anisotropy:this.anisotropy,flipY:this.flipY,generateMipmaps:this.generateMipmaps,premultiplyAlpha:this.premultiplyAlpha,unpackAlignment:this.unpackAlignment};return Object.keys(this.userData).length>0&&(n.userData=this.userData),e||(t.textures[this.uuid]=n),n}dispose(){this.dispatchEvent({type:"dispose"})}transformUv(t){if(this.mapping!==Yu)return t;if(t.applyMatrix3(this.matrix),t.x<0||t.x>1)switch(this.wrapS){case un:t.x=t.x-Math.floor(t.x);break;case Oi:t.x=t.x<0?0:1;break;case xl:Math.abs(Math.floor(t.x)%2)===1?t.x=Math.ceil(t.x)-t.x:t.x=t.x-Math.floor(t.x);break}if(t.y<0||t.y>1)switch(this.wrapT){case un:t.y=t.y-Math.floor(t.y);break;case Oi:t.y=t.y<0?0:1;break;case xl:Math.abs(Math.floor(t.y)%2)===1?t.y=Math.ceil(t.y)-t.y:t.y=t.y-Math.floor(t.y);break}return this.flipY&&(t.y=1-t.y),t}set needsUpdate(t){t===!0&&(this.version++,this.source.needsUpdate=!0)}set needsPMREMUpdate(t){t===!0&&this.pmremVersion++}};nn.DEFAULT_IMAGE=null;nn.DEFAULT_MAPPING=Yu;nn.DEFAULT_ANISOTROPY=1;var Ue=class i{constructor(t=0,e=0,n=0,s=1){i.prototype.isVector4=!0,this.x=t,this.y=e,this.z=n,this.w=s}get width(){return this.z}set width(t){this.z=t}get height(){return this.w}set height(t){this.w=t}set(t,e,n,s){return this.x=t,this.y=e,this.z=n,this.w=s,this}setScalar(t){return this.x=t,this.y=t,this.z=t,this.w=t,this}setX(t){return this.x=t,this}setY(t){return this.y=t,this}setZ(t){return this.z=t,this}setW(t){return this.w=t,this}setComponent(t,e){switch(t){case 0:this.x=e;break;case 1:this.y=e;break;case 2:this.z=e;break;case 3:this.w=e;break;default:throw new Error("index is out of range: "+t)}return this}getComponent(t){switch(t){case 0:return this.x;case 1:return this.y;case 2:return this.z;case 3:return this.w;default:throw new Error("index is out of range: "+t)}}clone(){return new this.constructor(this.x,this.y,this.z,this.w)}copy(t){return this.x=t.x,this.y=t.y,this.z=t.z,this.w=t.w!==void 0?t.w:1,this}add(t){return this.x+=t.x,this.y+=t.y,this.z+=t.z,this.w+=t.w,this}addScalar(t){return this.x+=t,this.y+=t,this.z+=t,this.w+=t,this}addVectors(t,e){return this.x=t.x+e.x,this.y=t.y+e.y,this.z=t.z+e.z,this.w=t.w+e.w,this}addScaledVector(t,e){return this.x+=t.x*e,this.y+=t.y*e,this.z+=t.z*e,this.w+=t.w*e,this}sub(t){return this.x-=t.x,this.y-=t.y,this.z-=t.z,this.w-=t.w,this}subScalar(t){return this.x-=t,this.y-=t,this.z-=t,this.w-=t,this}subVectors(t,e){return this.x=t.x-e.x,this.y=t.y-e.y,this.z=t.z-e.z,this.w=t.w-e.w,this}multiply(t){return this.x*=t.x,this.y*=t.y,this.z*=t.z,this.w*=t.w,this}multiplyScalar(t){return this.x*=t,this.y*=t,this.z*=t,this.w*=t,this}applyMatrix4(t){let e=this.x,n=this.y,s=this.z,r=this.w,o=t.elements;return this.x=o[0]*e+o[4]*n+o[8]*s+o[12]*r,this.y=o[1]*e+o[5]*n+o[9]*s+o[13]*r,this.z=o[2]*e+o[6]*n+o[10]*s+o[14]*r,this.w=o[3]*e+o[7]*n+o[11]*s+o[15]*r,this}divide(t){return this.x/=t.x,this.y/=t.y,this.z/=t.z,this.w/=t.w,this}divideScalar(t){return this.multiplyScalar(1/t)}setAxisAngleFromQuaternion(t){this.w=2*Math.acos(t.w);let e=Math.sqrt(1-t.w*t.w);return e<1e-4?(this.x=1,this.y=0,this.z=0):(this.x=t.x/e,this.y=t.y/e,this.z=t.z/e),this}setAxisAngleFromRotationMatrix(t){let e,n,s,r,l=t.elements,h=l[0],u=l[4],m=l[8],x=l[1],f=l[5],v=l[9],d=l[2],y=l[6],c=l[10];if(Math.abs(u-x)<.01&&Math.abs(m-d)<.01&&Math.abs(v-y)<.01){if(Math.abs(u+x)<.1&&Math.abs(m+d)<.1&&Math.abs(v+y)<.1&&Math.abs(h+f+c-3)<.1)return this.set(1,0,0,0),this;e=Math.PI;let p=(h+1)/2,g=(f+1)/2,T=(c+1)/2,S=(u+x)/4,b=(m+d)/4,w=(v+y)/4;return p>g&&p>T?p<.01?(n=0,s=.707106781,r=.707106781):(n=Math.sqrt(p),s=S/n,r=b/n):g>T?g<.01?(n=.707106781,s=0,r=.707106781):(s=Math.sqrt(g),n=S/s,r=w/s):T<.01?(n=.707106781,s=.707106781,r=0):(r=Math.sqrt(T),n=b/r,s=w/r),this.set(n,s,r,e),this}let _=Math.sqrt((y-v)*(y-v)+(m-d)*(m-d)+(x-u)*(x-u));return Math.abs(_)<.001&&(_=1),this.x=(y-v)/_,this.y=(m-d)/_,this.z=(x-u)/_,this.w=Math.acos((h+f+c-1)/2),this}setFromMatrixPosition(t){let e=t.elements;return this.x=e[12],this.y=e[13],this.z=e[14],this.w=e[15],this}min(t){return this.x=Math.min(this.x,t.x),this.y=Math.min(this.y,t.y),this.z=Math.min(this.z,t.z),this.w=Math.min(this.w,t.w),this}max(t){return this.x=Math.max(this.x,t.x),this.y=Math.max(this.y,t.y),this.z=Math.max(this.z,t.z),this.w=Math.max(this.w,t.w),this}clamp(t,e){return this.x=Math.max(t.x,Math.min(e.x,this.x)),this.y=Math.max(t.y,Math.min(e.y,this.y)),this.z=Math.max(t.z,Math.min(e.z,this.z)),this.w=Math.max(t.w,Math.min(e.w,this.w)),this}clampScalar(t,e){return this.x=Math.max(t,Math.min(e,this.x)),this.y=Math.max(t,Math.min(e,this.y)),this.z=Math.max(t,Math.min(e,this.z)),this.w=Math.max(t,Math.min(e,this.w)),this}clampLength(t,e){let n=this.length();return this.divideScalar(n||1).multiplyScalar(Math.max(t,Math.min(e,n)))}floor(){return this.x=Math.floor(this.x),this.y=Math.floor(this.y),this.z=Math.floor(this.z),this.w=Math.floor(this.w),this}ceil(){return this.x=Math.ceil(this.x),this.y=Math.ceil(this.y),this.z=Math.ceil(this.z),this.w=Math.ceil(this.w),this}round(){return this.x=Math.round(this.x),this.y=Math.round(this.y),this.z=Math.round(this.z),this.w=Math.round(this.w),this}roundToZero(){return this.x=Math.trunc(this.x),this.y=Math.trunc(this.y),this.z=Math.trunc(this.z),this.w=Math.trunc(this.w),this}negate(){return this.x=-this.x,this.y=-this.y,this.z=-this.z,this.w=-this.w,this}dot(t){return this.x*t.x+this.y*t.y+this.z*t.z+this.w*t.w}lengthSq(){return this.x*this.x+this.y*this.y+this.z*this.z+this.w*this.w}length(){return Math.sqrt(this.x*this.x+this.y*this.y+this.z*this.z+this.w*this.w)}manhattanLength(){return Math.abs(this.x)+Math.abs(this.y)+Math.abs(this.z)+Math.abs(this.w)}normalize(){return this.divideScalar(this.length()||1)}setLength(t){return this.normalize().multiplyScalar(t)}lerp(t,e){return this.x+=(t.x-this.x)*e,this.y+=(t.y-this.y)*e,this.z+=(t.z-this.z)*e,this.w+=(t.w-this.w)*e,this}lerpVectors(t,e,n){return this.x=t.x+(e.x-t.x)*n,this.y=t.y+(e.y-t.y)*n,this.z=t.z+(e.z-t.z)*n,this.w=t.w+(e.w-t.w)*n,this}equals(t){return t.x===this.x&&t.y===this.y&&t.z===this.z&&t.w===this.w}fromArray(t,e=0){return this.x=t[e],this.y=t[e+1],this.z=t[e+2],this.w=t[e+3],this}toArray(t=[],e=0){return t[e]=this.x,t[e+1]=this.y,t[e+2]=this.z,t[e+3]=this.w,t}fromBufferAttribute(t,e){return this.x=t.getX(e),this.y=t.getY(e),this.z=t.getZ(e),this.w=t.getW(e),this}random(){return this.x=Math.random(),this.y=Math.random(),this.z=Math.random(),this.w=Math.random(),this}*[Symbol.iterator](){yield this.x,yield this.y,yield this.z,yield this.w}},Zl=class extends vi{constructor(t=1,e=1,n={}){super(),this.isRenderTarget=!0,this.width=t,this.height=e,this.depth=1,this.scissor=new Ue(0,0,t,e),this.scissorTest=!1,this.viewport=new Ue(0,0,t,e);let s={width:t,height:e,depth:1};n=Object.assign({generateMipmaps:!1,internalFormat:null,minFilter:Pn,depthBuffer:!0,stencilBuffer:!1,resolveDepthBuffer:!0,resolveStencilBuffer:!0,depthTexture:null,samples:0,count:1},n);let r=new nn(s,n.mapping,n.wrapS,n.wrapT,n.magFilter,n.minFilter,n.format,n.type,n.anisotropy,n.colorSpace);r.flipY=!1,r.generateMipmaps=n.generateMipmaps,r.internalFormat=n.internalFormat,this.textures=[];let o=n.count;for(let a=0;a<o;a++)this.textures[a]=r.clone(),this.textures[a].isRenderTargetTexture=!0;this.depthBuffer=n.depthBuffer,this.stencilBuffer=n.stencilBuffer,this.resolveDepthBuffer=n.resolveDepthBuffer,this.resolveStencilBuffer=n.resolveStencilBuffer,this.depthTexture=n.depthTexture,this.samples=n.samples}get texture(){return this.textures[0]}set texture(t){this.textures[0]=t}setSize(t,e,n=1){if(this.width!==t||this.height!==e||this.depth!==n){this.width=t,this.height=e,this.depth=n;for(let s=0,r=this.textures.length;s<r;s++)this.textures[s].image.width=t,this.textures[s].image.height=e,this.textures[s].image.depth=n;this.dispose()}this.viewport.set(0,0,t,e),this.scissor.set(0,0,t,e)}clone(){return new this.constructor().copy(this)}copy(t){this.width=t.width,this.height=t.height,this.depth=t.depth,this.scissor.copy(t.scissor),this.scissorTest=t.scissorTest,this.viewport.copy(t.viewport),this.textures.length=0;for(let n=0,s=t.textures.length;n<s;n++)this.textures[n]=t.textures[n].clone(),this.textures[n].isRenderTargetTexture=!0;let e=Object.assign({},t.texture.image);return this.texture.source=new Ao(e),this.depthBuffer=t.depthBuffer,this.stencilBuffer=t.stencilBuffer,this.resolveDepthBuffer=t.resolveDepthBuffer,this.resolveStencilBuffer=t.resolveStencilBuffer,t.depthTexture!==null&&(this.depthTexture=t.depthTexture.clone()),this.samples=t.samples,this}dispose(){this.dispatchEvent({type:"dispose"})}},jn=class extends Zl{constructor(t=1,e=1,n={}){super(t,e,n),this.isWebGLRenderTarget=!0}},Ro=class extends nn{constructor(t=null,e=1,n=1,s=1){super(null),this.isDataArrayTexture=!0,this.image={data:t,width:e,height:n,depth:s},this.magFilter=hn,this.minFilter=hn,this.wrapR=Oi,this.generateMipmaps=!1,this.flipY=!1,this.unpackAlignment=1,this.layerUpdates=new Set}addLayerUpdate(t){this.layerUpdates.add(t)}clearLayerUpdates(){this.layerUpdates.clear()}};var $l=class extends nn{constructor(t=null,e=1,n=1,s=1){super(null),this.isData3DTexture=!0,this.image={data:t,width:e,height:n,depth:s},this.magFilter=hn,this.minFilter=hn,this.wrapR=Oi,this.generateMipmaps=!1,this.flipY=!1,this.unpackAlignment=1}};var gn=class{constructor(t=0,e=0,n=0,s=1){this.isQuaternion=!0,this._x=t,this._y=e,this._z=n,this._w=s}static slerpFlat(t,e,n,s,r,o,a){let l=n[s+0],h=n[s+1],u=n[s+2],m=n[s+3],x=r[o+0],f=r[o+1],v=r[o+2],d=r[o+3];if(a===0){t[e+0]=l,t[e+1]=h,t[e+2]=u,t[e+3]=m;return}if(a===1){t[e+0]=x,t[e+1]=f,t[e+2]=v,t[e+3]=d;return}if(m!==d||l!==x||h!==f||u!==v){let y=1-a,c=l*x+h*f+u*v+m*d,_=c>=0?1:-1,p=1-c*c;if(p>Number.EPSILON){let T=Math.sqrt(p),S=Math.atan2(T,c*_);y=Math.sin(y*S)/T,a=Math.sin(a*S)/T}let g=a*_;if(l=l*y+x*g,h=h*y+f*g,u=u*y+v*g,m=m*y+d*g,y===1-a){let T=1/Math.sqrt(l*l+h*h+u*u+m*m);l*=T,h*=T,u*=T,m*=T}}t[e]=l,t[e+1]=h,t[e+2]=u,t[e+3]=m}static multiplyQuaternionsFlat(t,e,n,s,r,o){let a=n[s],l=n[s+1],h=n[s+2],u=n[s+3],m=r[o],x=r[o+1],f=r[o+2],v=r[o+3];return t[e]=a*v+u*m+l*f-h*x,t[e+1]=l*v+u*x+h*m-a*f,t[e+2]=h*v+u*f+a*x-l*m,t[e+3]=u*v-a*m-l*x-h*f,t}get x(){return this._x}set x(t){this._x=t,this._onChangeCallback()}get y(){return this._y}set y(t){this._y=t,this._onChangeCallback()}get z(){return this._z}set z(t){this._z=t,this._onChangeCallback()}get w(){return this._w}set w(t){this._w=t,this._onChangeCallback()}set(t,e,n,s){return this._x=t,this._y=e,this._z=n,this._w=s,this._onChangeCallback(),this}clone(){return new this.constructor(this._x,this._y,this._z,this._w)}copy(t){return this._x=t.x,this._y=t.y,this._z=t.z,this._w=t.w,this._onChangeCallback(),this}setFromEuler(t,e=!0){let n=t._x,s=t._y,r=t._z,o=t._order,a=Math.cos,l=Math.sin,h=a(n/2),u=a(s/2),m=a(r/2),x=l(n/2),f=l(s/2),v=l(r/2);switch(o){case"XYZ":this._x=x*u*m+h*f*v,this._y=h*f*m-x*u*v,this._z=h*u*v+x*f*m,this._w=h*u*m-x*f*v;break;case"YXZ":this._x=x*u*m+h*f*v,this._y=h*f*m-x*u*v,this._z=h*u*v-x*f*m,this._w=h*u*m+x*f*v;break;case"ZXY":this._x=x*u*m-h*f*v,this._y=h*f*m+x*u*v,this._z=h*u*v+x*f*m,this._w=h*u*m-x*f*v;break;case"ZYX":this._x=x*u*m-h*f*v,this._y=h*f*m+x*u*v,this._z=h*u*v-x*f*m,this._w=h*u*m+x*f*v;break;case"YZX":this._x=x*u*m+h*f*v,this._y=h*f*m+x*u*v,this._z=h*u*v-x*f*m,this._w=h*u*m-x*f*v;break;case"XZY":this._x=x*u*m-h*f*v,this._y=h*f*m-x*u*v,this._z=h*u*v+x*f*m,this._w=h*u*m+x*f*v;break;default:console.warn("THREE.Quaternion: .setFromEuler() encountered an unknown order: "+o)}return e===!0&&this._onChangeCallback(),this}setFromAxisAngle(t,e){let n=e/2,s=Math.sin(n);return this._x=t.x*s,this._y=t.y*s,this._z=t.z*s,this._w=Math.cos(n),this._onChangeCallback(),this}setFromRotationMatrix(t){let e=t.elements,n=e[0],s=e[4],r=e[8],o=e[1],a=e[5],l=e[9],h=e[2],u=e[6],m=e[10],x=n+a+m;if(x>0){let f=.5/Math.sqrt(x+1);this._w=.25/f,this._x=(u-l)*f,this._y=(r-h)*f,this._z=(o-s)*f}else if(n>a&&n>m){let f=2*Math.sqrt(1+n-a-m);this._w=(u-l)/f,this._x=.25*f,this._y=(s+o)/f,this._z=(r+h)/f}else if(a>m){let f=2*Math.sqrt(1+a-n-m);this._w=(r-h)/f,this._x=(s+o)/f,this._y=.25*f,this._z=(l+u)/f}else{let f=2*Math.sqrt(1+m-n-a);this._w=(o-s)/f,this._x=(r+h)/f,this._y=(l+u)/f,this._z=.25*f}return this._onChangeCallback(),this}setFromUnitVectors(t,e){let n=t.dot(e)+1;return n<Number.EPSILON?(n=0,Math.abs(t.x)>Math.abs(t.z)?(this._x=-t.y,this._y=t.x,this._z=0,this._w=n):(this._x=0,this._y=-t.z,this._z=t.y,this._w=n)):(this._x=t.y*e.z-t.z*e.y,this._y=t.z*e.x-t.x*e.z,this._z=t.x*e.y-t.y*e.x,this._w=n),this.normalize()}angleTo(t){return 2*Math.acos(Math.abs(Xe(this.dot(t),-1,1)))}rotateTowards(t,e){let n=this.angleTo(t);if(n===0)return this;let s=Math.min(1,e/n);return this.slerp(t,s),this}identity(){return this.set(0,0,0,1)}invert(){return this.conjugate()}conjugate(){return this._x*=-1,this._y*=-1,this._z*=-1,this._onChangeCallback(),this}dot(t){return this._x*t._x+this._y*t._y+this._z*t._z+this._w*t._w}lengthSq(){return this._x*this._x+this._y*this._y+this._z*this._z+this._w*this._w}length(){return Math.sqrt(this._x*this._x+this._y*this._y+this._z*this._z+this._w*this._w)}normalize(){let t=this.length();return t===0?(this._x=0,this._y=0,this._z=0,this._w=1):(t=1/t,this._x=this._x*t,this._y=this._y*t,this._z=this._z*t,this._w=this._w*t),this._onChangeCallback(),this}multiply(t){return this.multiplyQuaternions(this,t)}premultiply(t){return this.multiplyQuaternions(t,this)}multiplyQuaternions(t,e){let n=t._x,s=t._y,r=t._z,o=t._w,a=e._x,l=e._y,h=e._z,u=e._w;return this._x=n*u+o*a+s*h-r*l,this._y=s*u+o*l+r*a-n*h,this._z=r*u+o*h+n*l-s*a,this._w=o*u-n*a-s*l-r*h,this._onChangeCallback(),this}slerp(t,e){if(e===0)return this;if(e===1)return this.copy(t);let n=this._x,s=this._y,r=this._z,o=this._w,a=o*t._w+n*t._x+s*t._y+r*t._z;if(a<0?(this._w=-t._w,this._x=-t._x,this._y=-t._y,this._z=-t._z,a=-a):this.copy(t),a>=1)return this._w=o,this._x=n,this._y=s,this._z=r,this;let l=1-a*a;if(l<=Number.EPSILON){let f=1-e;return this._w=f*o+e*this._w,this._x=f*n+e*this._x,this._y=f*s+e*this._y,this._z=f*r+e*this._z,this.normalize(),this}let h=Math.sqrt(l),u=Math.atan2(h,a),m=Math.sin((1-e)*u)/h,x=Math.sin(e*u)/h;return this._w=o*m+this._w*x,this._x=n*m+this._x*x,this._y=s*m+this._y*x,this._z=r*m+this._z*x,this._onChangeCallback(),this}slerpQuaternions(t,e,n){return this.copy(t).slerp(e,n)}random(){let t=2*Math.PI*Math.random(),e=2*Math.PI*Math.random(),n=Math.random(),s=Math.sqrt(1-n),r=Math.sqrt(n);return this.set(s*Math.sin(t),s*Math.cos(t),r*Math.sin(e),r*Math.cos(e))}equals(t){return t._x===this._x&&t._y===this._y&&t._z===this._z&&t._w===this._w}fromArray(t,e=0){return this._x=t[e],this._y=t[e+1],this._z=t[e+2],this._w=t[e+3],this._onChangeCallback(),this}toArray(t=[],e=0){return t[e]=this._x,t[e+1]=this._y,t[e+2]=this._z,t[e+3]=this._w,t}fromBufferAttribute(t,e){return this._x=t.getX(e),this._y=t.getY(e),this._z=t.getZ(e),this._w=t.getW(e),this._onChangeCallback(),this}toJSON(){return this.toArray()}_onChange(t){return this._onChangeCallback=t,this}_onChangeCallback(){}*[Symbol.iterator](){yield this._x,yield this._y,yield this._z,yield this._w}},B=class i{constructor(t=0,e=0,n=0){i.prototype.isVector3=!0,this.x=t,this.y=e,this.z=n}set(t,e,n){return n===void 0&&(n=this.z),this.x=t,this.y=e,this.z=n,this}setScalar(t){return this.x=t,this.y=t,this.z=t,this}setX(t){return this.x=t,this}setY(t){return this.y=t,this}setZ(t){return this.z=t,this}setComponent(t,e){switch(t){case 0:this.x=e;break;case 1:this.y=e;break;case 2:this.z=e;break;default:throw new Error("index is out of range: "+t)}return this}getComponent(t){switch(t){case 0:return this.x;case 1:return this.y;case 2:return this.z;default:throw new Error("index is out of range: "+t)}}clone(){return new this.constructor(this.x,this.y,this.z)}copy(t){return this.x=t.x,this.y=t.y,this.z=t.z,this}add(t){return this.x+=t.x,this.y+=t.y,this.z+=t.z,this}addScalar(t){return this.x+=t,this.y+=t,this.z+=t,this}addVectors(t,e){return this.x=t.x+e.x,this.y=t.y+e.y,this.z=t.z+e.z,this}addScaledVector(t,e){return this.x+=t.x*e,this.y+=t.y*e,this.z+=t.z*e,this}sub(t){return this.x-=t.x,this.y-=t.y,this.z-=t.z,this}subScalar(t){return this.x-=t,this.y-=t,this.z-=t,this}subVectors(t,e){return this.x=t.x-e.x,this.y=t.y-e.y,this.z=t.z-e.z,this}multiply(t){return this.x*=t.x,this.y*=t.y,this.z*=t.z,this}multiplyScalar(t){return this.x*=t,this.y*=t,this.z*=t,this}multiplyVectors(t,e){return this.x=t.x*e.x,this.y=t.y*e.y,this.z=t.z*e.z,this}applyEuler(t){return this.applyQuaternion(qh.setFromEuler(t))}applyAxisAngle(t,e){return this.applyQuaternion(qh.setFromAxisAngle(t,e))}applyMatrix3(t){let e=this.x,n=this.y,s=this.z,r=t.elements;return this.x=r[0]*e+r[3]*n+r[6]*s,this.y=r[1]*e+r[4]*n+r[7]*s,this.z=r[2]*e+r[5]*n+r[8]*s,this}applyNormalMatrix(t){return this.applyMatrix3(t).normalize()}applyMatrix4(t){let e=this.x,n=this.y,s=this.z,r=t.elements,o=1/(r[3]*e+r[7]*n+r[11]*s+r[15]);return this.x=(r[0]*e+r[4]*n+r[8]*s+r[12])*o,this.y=(r[1]*e+r[5]*n+r[9]*s+r[13])*o,this.z=(r[2]*e+r[6]*n+r[10]*s+r[14])*o,this}applyQuaternion(t){let e=this.x,n=this.y,s=this.z,r=t.x,o=t.y,a=t.z,l=t.w,h=2*(o*s-a*n),u=2*(a*e-r*s),m=2*(r*n-o*e);return this.x=e+l*h+o*m-a*u,this.y=n+l*u+a*h-r*m,this.z=s+l*m+r*u-o*h,this}project(t){return this.applyMatrix4(t.matrixWorldInverse).applyMatrix4(t.projectionMatrix)}unproject(t){return this.applyMatrix4(t.projectionMatrixInverse).applyMatrix4(t.matrixWorld)}transformDirection(t){let e=this.x,n=this.y,s=this.z,r=t.elements;return this.x=r[0]*e+r[4]*n+r[8]*s,this.y=r[1]*e+r[5]*n+r[9]*s,this.z=r[2]*e+r[6]*n+r[10]*s,this.normalize()}divide(t){return this.x/=t.x,this.y/=t.y,this.z/=t.z,this}divideScalar(t){return this.multiplyScalar(1/t)}min(t){return this.x=Math.min(this.x,t.x),this.y=Math.min(this.y,t.y),this.z=Math.min(this.z,t.z),this}max(t){return this.x=Math.max(this.x,t.x),this.y=Math.max(this.y,t.y),this.z=Math.max(this.z,t.z),this}clamp(t,e){return this.x=Math.max(t.x,Math.min(e.x,this.x)),this.y=Math.max(t.y,Math.min(e.y,this.y)),this.z=Math.max(t.z,Math.min(e.z,this.z)),this}clampScalar(t,e){return this.x=Math.max(t,Math.min(e,this.x)),this.y=Math.max(t,Math.min(e,this.y)),this.z=Math.max(t,Math.min(e,this.z)),this}clampLength(t,e){let n=this.length();return this.divideScalar(n||1).multiplyScalar(Math.max(t,Math.min(e,n)))}floor(){return this.x=Math.floor(this.x),this.y=Math.floor(this.y),this.z=Math.floor(this.z),this}ceil(){return this.x=Math.ceil(this.x),this.y=Math.ceil(this.y),this.z=Math.ceil(this.z),this}round(){return this.x=Math.round(this.x),this.y=Math.round(this.y),this.z=Math.round(this.z),this}roundToZero(){return this.x=Math.trunc(this.x),this.y=Math.trunc(this.y),this.z=Math.trunc(this.z),this}negate(){return this.x=-this.x,this.y=-this.y,this.z=-this.z,this}dot(t){return this.x*t.x+this.y*t.y+this.z*t.z}lengthSq(){return this.x*this.x+this.y*this.y+this.z*this.z}length(){return Math.sqrt(this.x*this.x+this.y*this.y+this.z*this.z)}manhattanLength(){return Math.abs(this.x)+Math.abs(this.y)+Math.abs(this.z)}normalize(){return this.divideScalar(this.length()||1)}setLength(t){return this.normalize().multiplyScalar(t)}lerp(t,e){return this.x+=(t.x-this.x)*e,this.y+=(t.y-this.y)*e,this.z+=(t.z-this.z)*e,this}lerpVectors(t,e,n){return this.x=t.x+(e.x-t.x)*n,this.y=t.y+(e.y-t.y)*n,this.z=t.z+(e.z-t.z)*n,this}cross(t){return this.crossVectors(this,t)}crossVectors(t,e){let n=t.x,s=t.y,r=t.z,o=e.x,a=e.y,l=e.z;return this.x=s*l-r*a,this.y=r*o-n*l,this.z=n*a-s*o,this}projectOnVector(t){let e=t.lengthSq();if(e===0)return this.set(0,0,0);let n=t.dot(this)/e;return this.copy(t).multiplyScalar(n)}projectOnPlane(t){return Fa.copy(this).projectOnVector(t),this.sub(Fa)}reflect(t){return this.sub(Fa.copy(t).multiplyScalar(2*this.dot(t)))}angleTo(t){let e=Math.sqrt(this.lengthSq()*t.lengthSq());if(e===0)return Math.PI/2;let n=this.dot(t)/e;return Math.acos(Xe(n,-1,1))}distanceTo(t){return Math.sqrt(this.distanceToSquared(t))}distanceToSquared(t){let e=this.x-t.x,n=this.y-t.y,s=this.z-t.z;return e*e+n*n+s*s}manhattanDistanceTo(t){return Math.abs(this.x-t.x)+Math.abs(this.y-t.y)+Math.abs(this.z-t.z)}setFromSpherical(t){return this.setFromSphericalCoords(t.radius,t.phi,t.theta)}setFromSphericalCoords(t,e,n){let s=Math.sin(e)*t;return this.x=s*Math.sin(n),this.y=Math.cos(e)*t,this.z=s*Math.cos(n),this}setFromCylindrical(t){return this.setFromCylindricalCoords(t.radius,t.theta,t.y)}setFromCylindricalCoords(t,e,n){return this.x=t*Math.sin(e),this.y=n,this.z=t*Math.cos(e),this}setFromMatrixPosition(t){let e=t.elements;return this.x=e[12],this.y=e[13],this.z=e[14],this}setFromMatrixScale(t){let e=this.setFromMatrixColumn(t,0).length(),n=this.setFromMatrixColumn(t,1).length(),s=this.setFromMatrixColumn(t,2).length();return this.x=e,this.y=n,this.z=s,this}setFromMatrixColumn(t,e){return this.fromArray(t.elements,e*4)}setFromMatrix3Column(t,e){return this.fromArray(t.elements,e*3)}setFromEuler(t){return this.x=t._x,this.y=t._y,this.z=t._z,this}setFromColor(t){return this.x=t.r,this.y=t.g,this.z=t.b,this}equals(t){return t.x===this.x&&t.y===this.y&&t.z===this.z}fromArray(t,e=0){return this.x=t[e],this.y=t[e+1],this.z=t[e+2],this}toArray(t=[],e=0){return t[e]=this.x,t[e+1]=this.y,t[e+2]=this.z,t}fromBufferAttribute(t,e){return this.x=t.getX(e),this.y=t.getY(e),this.z=t.getZ(e),this}random(){return this.x=Math.random(),this.y=Math.random(),this.z=Math.random(),this}randomDirection(){let t=Math.random()*Math.PI*2,e=Math.random()*2-1,n=Math.sqrt(1-e*e);return this.x=n*Math.cos(t),this.y=e,this.z=n*Math.sin(t),this}*[Symbol.iterator](){yield this.x,yield this.y,yield this.z}},Fa=new B,qh=new gn,Qn=class{constructor(t=new B(1/0,1/0,1/0),e=new B(-1/0,-1/0,-1/0)){this.isBox3=!0,this.min=t,this.max=e}set(t,e){return this.min.copy(t),this.max.copy(e),this}setFromArray(t){this.makeEmpty();for(let e=0,n=t.length;e<n;e+=3)this.expandByPoint(Sn.fromArray(t,e));return this}setFromBufferAttribute(t){this.makeEmpty();for(let e=0,n=t.count;e<n;e++)this.expandByPoint(Sn.fromBufferAttribute(t,e));return this}setFromPoints(t){this.makeEmpty();for(let e=0,n=t.length;e<n;e++)this.expandByPoint(t[e]);return this}setFromCenterAndSize(t,e){let n=Sn.copy(e).multiplyScalar(.5);return this.min.copy(t).sub(n),this.max.copy(t).add(n),this}setFromObject(t,e=!1){return this.makeEmpty(),this.expandByObject(t,e)}clone(){return new this.constructor().copy(this)}copy(t){return this.min.copy(t.min),this.max.copy(t.max),this}makeEmpty(){return this.min.x=this.min.y=this.min.z=1/0,this.max.x=this.max.y=this.max.z=-1/0,this}isEmpty(){return this.max.x<this.min.x||this.max.y<this.min.y||this.max.z<this.min.z}getCenter(t){return this.isEmpty()?t.set(0,0,0):t.addVectors(this.min,this.max).multiplyScalar(.5)}getSize(t){return this.isEmpty()?t.set(0,0,0):t.subVectors(this.max,this.min)}expandByPoint(t){return this.min.min(t),this.max.max(t),this}expandByVector(t){return this.min.sub(t),this.max.add(t),this}expandByScalar(t){return this.min.addScalar(-t),this.max.addScalar(t),this}expandByObject(t,e=!1){t.updateWorldMatrix(!1,!1);let n=t.geometry;if(n!==void 0){let r=n.getAttribute("position");if(e===!0&&r!==void 0&&t.isInstancedMesh!==!0)for(let o=0,a=r.count;o<a;o++)t.isMesh===!0?t.getVertexPosition(o,Sn):Sn.fromBufferAttribute(r,o),Sn.applyMatrix4(t.matrixWorld),this.expandByPoint(Sn);else t.boundingBox!==void 0?(t.boundingBox===null&&t.computeBoundingBox(),Hr.copy(t.boundingBox)):(n.boundingBox===null&&n.computeBoundingBox(),Hr.copy(n.boundingBox)),Hr.applyMatrix4(t.matrixWorld),this.union(Hr)}let s=t.children;for(let r=0,o=s.length;r<o;r++)this.expandByObject(s[r],e);return this}containsPoint(t){return t.x>=this.min.x&&t.x<=this.max.x&&t.y>=this.min.y&&t.y<=this.max.y&&t.z>=this.min.z&&t.z<=this.max.z}containsBox(t){return this.min.x<=t.min.x&&t.max.x<=this.max.x&&this.min.y<=t.min.y&&t.max.y<=this.max.y&&this.min.z<=t.min.z&&t.max.z<=this.max.z}getParameter(t,e){return e.set((t.x-this.min.x)/(this.max.x-this.min.x),(t.y-this.min.y)/(this.max.y-this.min.y),(t.z-this.min.z)/(this.max.z-this.min.z))}intersectsBox(t){return t.max.x>=this.min.x&&t.min.x<=this.max.x&&t.max.y>=this.min.y&&t.min.y<=this.max.y&&t.max.z>=this.min.z&&t.min.z<=this.max.z}intersectsSphere(t){return this.clampPoint(t.center,Sn),Sn.distanceToSquared(t.center)<=t.radius*t.radius}intersectsPlane(t){let e,n;return t.normal.x>0?(e=t.normal.x*this.min.x,n=t.normal.x*this.max.x):(e=t.normal.x*this.max.x,n=t.normal.x*this.min.x),t.normal.y>0?(e+=t.normal.y*this.min.y,n+=t.normal.y*this.max.y):(e+=t.normal.y*this.max.y,n+=t.normal.y*this.min.y),t.normal.z>0?(e+=t.normal.z*this.min.z,n+=t.normal.z*this.max.z):(e+=t.normal.z*this.max.z,n+=t.normal.z*this.min.z),e<=-t.constant&&n>=-t.constant}intersectsTriangle(t){if(this.isEmpty())return!1;this.getCenter(Js),kr.subVectors(this.max,Js),ss.subVectors(t.a,Js),rs.subVectors(t.b,Js),os.subVectors(t.c,Js),ci.subVectors(rs,ss),hi.subVectors(os,rs),Fi.subVectors(ss,os);let e=[0,-ci.z,ci.y,0,-hi.z,hi.y,0,-Fi.z,Fi.y,ci.z,0,-ci.x,hi.z,0,-hi.x,Fi.z,0,-Fi.x,-ci.y,ci.x,0,-hi.y,hi.x,0,-Fi.y,Fi.x,0];return!Ia(e,ss,rs,os,kr)||(e=[1,0,0,0,1,0,0,0,1],!Ia(e,ss,rs,os,kr))?!1:(Gr.crossVectors(ci,hi),e=[Gr.x,Gr.y,Gr.z],Ia(e,ss,rs,os,kr))}clampPoint(t,e){return e.copy(t).clamp(this.min,this.max)}distanceToPoint(t){return this.clampPoint(t,Sn).distanceTo(t)}getBoundingSphere(t){return this.isEmpty()?t.makeEmpty():(this.getCenter(t.center),t.radius=this.getSize(Sn).length()*.5),t}intersect(t){return this.min.max(t.min),this.max.min(t.max),this.isEmpty()&&this.makeEmpty(),this}union(t){return this.min.min(t.min),this.max.max(t.max),this}applyMatrix4(t){return this.isEmpty()?this:(kn[0].set(this.min.x,this.min.y,this.min.z).applyMatrix4(t),kn[1].set(this.min.x,this.min.y,this.max.z).applyMatrix4(t),kn[2].set(this.min.x,this.max.y,this.min.z).applyMatrix4(t),kn[3].set(this.min.x,this.max.y,this.max.z).applyMatrix4(t),kn[4].set(this.max.x,this.min.y,this.min.z).applyMatrix4(t),kn[5].set(this.max.x,this.min.y,this.max.z).applyMatrix4(t),kn[6].set(this.max.x,this.max.y,this.min.z).applyMatrix4(t),kn[7].set(this.max.x,this.max.y,this.max.z).applyMatrix4(t),this.setFromPoints(kn),this)}translate(t){return this.min.add(t),this.max.add(t),this}equals(t){return t.min.equals(this.min)&&t.max.equals(this.max)}},kn=[new B,new B,new B,new B,new B,new B,new B,new B],Sn=new B,Hr=new Qn,ss=new B,rs=new B,os=new B,ci=new B,hi=new B,Fi=new B,Js=new B,kr=new B,Gr=new B,Ii=new B;function Ia(i,t,e,n,s){for(let r=0,o=i.length-3;r<=o;r+=3){Ii.fromArray(i,r);let a=s.x*Math.abs(Ii.x)+s.y*Math.abs(Ii.y)+s.z*Math.abs(Ii.z),l=t.dot(Ii),h=e.dot(Ii),u=n.dot(Ii);if(Math.max(-Math.max(l,h,u),Math.min(l,h,u))>a)return!1}return!0}var mp=new Qn,Ks=new B,Pa=new B,ti=class{constructor(t=new B,e=-1){this.isSphere=!0,this.center=t,this.radius=e}set(t,e){return this.center.copy(t),this.radius=e,this}setFromPoints(t,e){let n=this.center;e!==void 0?n.copy(e):mp.setFromPoints(t).getCenter(n);let s=0;for(let r=0,o=t.length;r<o;r++)s=Math.max(s,n.distanceToSquared(t[r]));return this.radius=Math.sqrt(s),this}copy(t){return this.center.copy(t.center),this.radius=t.radius,this}isEmpty(){return this.radius<0}makeEmpty(){return this.center.set(0,0,0),this.radius=-1,this}containsPoint(t){return t.distanceToSquared(this.center)<=this.radius*this.radius}distanceToPoint(t){return t.distanceTo(this.center)-this.radius}intersectsSphere(t){let e=this.radius+t.radius;return t.center.distanceToSquared(this.center)<=e*e}intersectsBox(t){return t.intersectsSphere(this)}intersectsPlane(t){return Math.abs(t.distanceToPoint(this.center))<=this.radius}clampPoint(t,e){let n=this.center.distanceToSquared(t);return e.copy(t),n>this.radius*this.radius&&(e.sub(this.center).normalize(),e.multiplyScalar(this.radius).add(this.center)),e}getBoundingBox(t){return this.isEmpty()?(t.makeEmpty(),t):(t.set(this.center,this.center),t.expandByScalar(this.radius),t)}applyMatrix4(t){return this.center.applyMatrix4(t),this.radius=this.radius*t.getMaxScaleOnAxis(),this}translate(t){return this.center.add(t),this}expandByPoint(t){if(this.isEmpty())return this.center.copy(t),this.radius=0,this;Ks.subVectors(t,this.center);let e=Ks.lengthSq();if(e>this.radius*this.radius){let n=Math.sqrt(e),s=(n-this.radius)*.5;this.center.addScaledVector(Ks,s/n),this.radius+=s}return this}union(t){return t.isEmpty()?this:this.isEmpty()?(this.copy(t),this):(this.center.equals(t.center)===!0?this.radius=Math.max(this.radius,t.radius):(Pa.subVectors(t.center,this.center).setLength(t.radius),this.expandByPoint(Ks.copy(t.center).add(Pa)),this.expandByPoint(Ks.copy(t.center).sub(Pa))),this)}equals(t){return t.center.equals(this.center)&&t.radius===this.radius}clone(){return new this.constructor().copy(this)}},Gn=new B,La=new B,Vr=new B,ui=new B,Da=new B,Wr=new B,Ua=new B,dr=class{constructor(t=new B,e=new B(0,0,-1)){this.origin=t,this.direction=e}set(t,e){return this.origin.copy(t),this.direction.copy(e),this}copy(t){return this.origin.copy(t.origin),this.direction.copy(t.direction),this}at(t,e){return e.copy(this.origin).addScaledVector(this.direction,t)}lookAt(t){return this.direction.copy(t).sub(this.origin).normalize(),this}recast(t){return this.origin.copy(this.at(t,Gn)),this}closestPointToPoint(t,e){e.subVectors(t,this.origin);let n=e.dot(this.direction);return n<0?e.copy(this.origin):e.copy(this.origin).addScaledVector(this.direction,n)}distanceToPoint(t){return Math.sqrt(this.distanceSqToPoint(t))}distanceSqToPoint(t){let e=Gn.subVectors(t,this.origin).dot(this.direction);return e<0?this.origin.distanceToSquared(t):(Gn.copy(this.origin).addScaledVector(this.direction,e),Gn.distanceToSquared(t))}distanceSqToSegment(t,e,n,s){La.copy(t).add(e).multiplyScalar(.5),Vr.copy(e).sub(t).normalize(),ui.copy(this.origin).sub(La);let r=t.distanceTo(e)*.5,o=-this.direction.dot(Vr),a=ui.dot(this.direction),l=-ui.dot(Vr),h=ui.lengthSq(),u=Math.abs(1-o*o),m,x,f,v;if(u>0)if(m=o*l-a,x=o*a-l,v=r*u,m>=0)if(x>=-v)if(x<=v){let d=1/u;m*=d,x*=d,f=m*(m+o*x+2*a)+x*(o*m+x+2*l)+h}else x=r,m=Math.max(0,-(o*x+a)),f=-m*m+x*(x+2*l)+h;else x=-r,m=Math.max(0,-(o*x+a)),f=-m*m+x*(x+2*l)+h;else x<=-v?(m=Math.max(0,-(-o*r+a)),x=m>0?-r:Math.min(Math.max(-r,-l),r),f=-m*m+x*(x+2*l)+h):x<=v?(m=0,x=Math.min(Math.max(-r,-l),r),f=x*(x+2*l)+h):(m=Math.max(0,-(o*r+a)),x=m>0?r:Math.min(Math.max(-r,-l),r),f=-m*m+x*(x+2*l)+h);else x=o>0?-r:r,m=Math.max(0,-(o*x+a)),f=-m*m+x*(x+2*l)+h;return n&&n.copy(this.origin).addScaledVector(this.direction,m),s&&s.copy(La).addScaledVector(Vr,x),f}intersectSphere(t,e){Gn.subVectors(t.center,this.origin);let n=Gn.dot(this.direction),s=Gn.dot(Gn)-n*n,r=t.radius*t.radius;if(s>r)return null;let o=Math.sqrt(r-s),a=n-o,l=n+o;return l<0?null:a<0?this.at(l,e):this.at(a,e)}intersectsSphere(t){return this.distanceSqToPoint(t.center)<=t.radius*t.radius}distanceToPlane(t){let e=t.normal.dot(this.direction);if(e===0)return t.distanceToPoint(this.origin)===0?0:null;let n=-(this.origin.dot(t.normal)+t.constant)/e;return n>=0?n:null}intersectPlane(t,e){let n=this.distanceToPlane(t);return n===null?null:this.at(n,e)}intersectsPlane(t){let e=t.distanceToPoint(this.origin);return e===0||t.normal.dot(this.direction)*e<0}intersectBox(t,e){let n,s,r,o,a,l,h=1/this.direction.x,u=1/this.direction.y,m=1/this.direction.z,x=this.origin;return h>=0?(n=(t.min.x-x.x)*h,s=(t.max.x-x.x)*h):(n=(t.max.x-x.x)*h,s=(t.min.x-x.x)*h),u>=0?(r=(t.min.y-x.y)*u,o=(t.max.y-x.y)*u):(r=(t.max.y-x.y)*u,o=(t.min.y-x.y)*u),n>o||r>s||((r>n||isNaN(n))&&(n=r),(o<s||isNaN(s))&&(s=o),m>=0?(a=(t.min.z-x.z)*m,l=(t.max.z-x.z)*m):(a=(t.max.z-x.z)*m,l=(t.min.z-x.z)*m),n>l||a>s)||((a>n||n!==n)&&(n=a),(l<s||s!==s)&&(s=l),s<0)?null:this.at(n>=0?n:s,e)}intersectsBox(t){return this.intersectBox(t,Gn)!==null}intersectTriangle(t,e,n,s,r){Da.subVectors(e,t),Wr.subVectors(n,t),Ua.crossVectors(Da,Wr);let o=this.direction.dot(Ua),a;if(o>0){if(s)return null;a=1}else if(o<0)a=-1,o=-o;else return null;ui.subVectors(this.origin,t);let l=a*this.direction.dot(Wr.crossVectors(ui,Wr));if(l<0)return null;let h=a*this.direction.dot(Da.cross(ui));if(h<0||l+h>o)return null;let u=-a*ui.dot(Ua);return u<0?null:this.at(u/o,r)}applyMatrix4(t){return this.origin.applyMatrix4(t),this.direction.transformDirection(t),this}equals(t){return t.origin.equals(this.origin)&&t.direction.equals(this.direction)}clone(){return new this.constructor().copy(this)}},ee=class i{constructor(t,e,n,s,r,o,a,l,h,u,m,x,f,v,d,y){i.prototype.isMatrix4=!0,this.elements=[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],t!==void 0&&this.set(t,e,n,s,r,o,a,l,h,u,m,x,f,v,d,y)}set(t,e,n,s,r,o,a,l,h,u,m,x,f,v,d,y){let c=this.elements;return c[0]=t,c[4]=e,c[8]=n,c[12]=s,c[1]=r,c[5]=o,c[9]=a,c[13]=l,c[2]=h,c[6]=u,c[10]=m,c[14]=x,c[3]=f,c[7]=v,c[11]=d,c[15]=y,this}identity(){return this.set(1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1),this}clone(){return new i().fromArray(this.elements)}copy(t){let e=this.elements,n=t.elements;return e[0]=n[0],e[1]=n[1],e[2]=n[2],e[3]=n[3],e[4]=n[4],e[5]=n[5],e[6]=n[6],e[7]=n[7],e[8]=n[8],e[9]=n[9],e[10]=n[10],e[11]=n[11],e[12]=n[12],e[13]=n[13],e[14]=n[14],e[15]=n[15],this}copyPosition(t){let e=this.elements,n=t.elements;return e[12]=n[12],e[13]=n[13],e[14]=n[14],this}setFromMatrix3(t){let e=t.elements;return this.set(e[0],e[3],e[6],0,e[1],e[4],e[7],0,e[2],e[5],e[8],0,0,0,0,1),this}extractBasis(t,e,n){return t.setFromMatrixColumn(this,0),e.setFromMatrixColumn(this,1),n.setFromMatrixColumn(this,2),this}makeBasis(t,e,n){return this.set(t.x,e.x,n.x,0,t.y,e.y,n.y,0,t.z,e.z,n.z,0,0,0,0,1),this}extractRotation(t){let e=this.elements,n=t.elements,s=1/as.setFromMatrixColumn(t,0).length(),r=1/as.setFromMatrixColumn(t,1).length(),o=1/as.setFromMatrixColumn(t,2).length();return e[0]=n[0]*s,e[1]=n[1]*s,e[2]=n[2]*s,e[3]=0,e[4]=n[4]*r,e[5]=n[5]*r,e[6]=n[6]*r,e[7]=0,e[8]=n[8]*o,e[9]=n[9]*o,e[10]=n[10]*o,e[11]=0,e[12]=0,e[13]=0,e[14]=0,e[15]=1,this}makeRotationFromEuler(t){let e=this.elements,n=t.x,s=t.y,r=t.z,o=Math.cos(n),a=Math.sin(n),l=Math.cos(s),h=Math.sin(s),u=Math.cos(r),m=Math.sin(r);if(t.order==="XYZ"){let x=o*u,f=o*m,v=a*u,d=a*m;e[0]=l*u,e[4]=-l*m,e[8]=h,e[1]=f+v*h,e[5]=x-d*h,e[9]=-a*l,e[2]=d-x*h,e[6]=v+f*h,e[10]=o*l}else if(t.order==="YXZ"){let x=l*u,f=l*m,v=h*u,d=h*m;e[0]=x+d*a,e[4]=v*a-f,e[8]=o*h,e[1]=o*m,e[5]=o*u,e[9]=-a,e[2]=f*a-v,e[6]=d+x*a,e[10]=o*l}else if(t.order==="ZXY"){let x=l*u,f=l*m,v=h*u,d=h*m;e[0]=x-d*a,e[4]=-o*m,e[8]=v+f*a,e[1]=f+v*a,e[5]=o*u,e[9]=d-x*a,e[2]=-o*h,e[6]=a,e[10]=o*l}else if(t.order==="ZYX"){let x=o*u,f=o*m,v=a*u,d=a*m;e[0]=l*u,e[4]=v*h-f,e[8]=x*h+d,e[1]=l*m,e[5]=d*h+x,e[9]=f*h-v,e[2]=-h,e[6]=a*l,e[10]=o*l}else if(t.order==="YZX"){let x=o*l,f=o*h,v=a*l,d=a*h;e[0]=l*u,e[4]=d-x*m,e[8]=v*m+f,e[1]=m,e[5]=o*u,e[9]=-a*u,e[2]=-h*u,e[6]=f*m+v,e[10]=x-d*m}else if(t.order==="XZY"){let x=o*l,f=o*h,v=a*l,d=a*h;e[0]=l*u,e[4]=-m,e[8]=h*u,e[1]=x*m+d,e[5]=o*u,e[9]=f*m-v,e[2]=v*m-f,e[6]=a*u,e[10]=d*m+x}return e[3]=0,e[7]=0,e[11]=0,e[12]=0,e[13]=0,e[14]=0,e[15]=1,this}makeRotationFromQuaternion(t){return this.compose(gp,t,xp)}lookAt(t,e,n){let s=this.elements;return ln.subVectors(t,e),ln.lengthSq()===0&&(ln.z=1),ln.normalize(),fi.crossVectors(n,ln),fi.lengthSq()===0&&(Math.abs(n.z)===1?ln.x+=1e-4:ln.z+=1e-4,ln.normalize(),fi.crossVectors(n,ln)),fi.normalize(),Xr.crossVectors(ln,fi),s[0]=fi.x,s[4]=Xr.x,s[8]=ln.x,s[1]=fi.y,s[5]=Xr.y,s[9]=ln.y,s[2]=fi.z,s[6]=Xr.z,s[10]=ln.z,this}multiply(t){return this.multiplyMatrices(this,t)}premultiply(t){return this.multiplyMatrices(t,this)}multiplyMatrices(t,e){let n=t.elements,s=e.elements,r=this.elements,o=n[0],a=n[4],l=n[8],h=n[12],u=n[1],m=n[5],x=n[9],f=n[13],v=n[2],d=n[6],y=n[10],c=n[14],_=n[3],p=n[7],g=n[11],T=n[15],S=s[0],b=s[4],w=s[8],M=s[12],E=s[1],F=s[5],U=s[9],R=s[13],N=s[2],V=s[6],z=s[10],O=s[14],D=s[3],X=s[7],$=s[11],tt=s[15];return r[0]=o*S+a*E+l*N+h*D,r[4]=o*b+a*F+l*V+h*X,r[8]=o*w+a*U+l*z+h*$,r[12]=o*M+a*R+l*O+h*tt,r[1]=u*S+m*E+x*N+f*D,r[5]=u*b+m*F+x*V+f*X,r[9]=u*w+m*U+x*z+f*$,r[13]=u*M+m*R+x*O+f*tt,r[2]=v*S+d*E+y*N+c*D,r[6]=v*b+d*F+y*V+c*X,r[10]=v*w+d*U+y*z+c*$,r[14]=v*M+d*R+y*O+c*tt,r[3]=_*S+p*E+g*N+T*D,r[7]=_*b+p*F+g*V+T*X,r[11]=_*w+p*U+g*z+T*$,r[15]=_*M+p*R+g*O+T*tt,this}multiplyScalar(t){let e=this.elements;return e[0]*=t,e[4]*=t,e[8]*=t,e[12]*=t,e[1]*=t,e[5]*=t,e[9]*=t,e[13]*=t,e[2]*=t,e[6]*=t,e[10]*=t,e[14]*=t,e[3]*=t,e[7]*=t,e[11]*=t,e[15]*=t,this}determinant(){let t=this.elements,e=t[0],n=t[4],s=t[8],r=t[12],o=t[1],a=t[5],l=t[9],h=t[13],u=t[2],m=t[6],x=t[10],f=t[14],v=t[3],d=t[7],y=t[11],c=t[15];return v*(+r*l*m-s*h*m-r*a*x+n*h*x+s*a*f-n*l*f)+d*(+e*l*f-e*h*x+r*o*x-s*o*f+s*h*u-r*l*u)+y*(+e*h*m-e*a*f-r*o*m+n*o*f+r*a*u-n*h*u)+c*(-s*a*u-e*l*m+e*a*x+s*o*m-n*o*x+n*l*u)}transpose(){let t=this.elements,e;return e=t[1],t[1]=t[4],t[4]=e,e=t[2],t[2]=t[8],t[8]=e,e=t[6],t[6]=t[9],t[9]=e,e=t[3],t[3]=t[12],t[12]=e,e=t[7],t[7]=t[13],t[13]=e,e=t[11],t[11]=t[14],t[14]=e,this}setPosition(t,e,n){let s=this.elements;return t.isVector3?(s[12]=t.x,s[13]=t.y,s[14]=t.z):(s[12]=t,s[13]=e,s[14]=n),this}invert(){let t=this.elements,e=t[0],n=t[1],s=t[2],r=t[3],o=t[4],a=t[5],l=t[6],h=t[7],u=t[8],m=t[9],x=t[10],f=t[11],v=t[12],d=t[13],y=t[14],c=t[15],_=m*y*h-d*x*h+d*l*f-a*y*f-m*l*c+a*x*c,p=v*x*h-u*y*h-v*l*f+o*y*f+u*l*c-o*x*c,g=u*d*h-v*m*h+v*a*f-o*d*f-u*a*c+o*m*c,T=v*m*l-u*d*l-v*a*x+o*d*x+u*a*y-o*m*y,S=e*_+n*p+s*g+r*T;if(S===0)return this.set(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);let b=1/S;return t[0]=_*b,t[1]=(d*x*r-m*y*r-d*s*f+n*y*f+m*s*c-n*x*c)*b,t[2]=(a*y*r-d*l*r+d*s*h-n*y*h-a*s*c+n*l*c)*b,t[3]=(m*l*r-a*x*r-m*s*h+n*x*h+a*s*f-n*l*f)*b,t[4]=p*b,t[5]=(u*y*r-v*x*r+v*s*f-e*y*f-u*s*c+e*x*c)*b,t[6]=(v*l*r-o*y*r-v*s*h+e*y*h+o*s*c-e*l*c)*b,t[7]=(o*x*r-u*l*r+u*s*h-e*x*h-o*s*f+e*l*f)*b,t[8]=g*b,t[9]=(v*m*r-u*d*r-v*n*f+e*d*f+u*n*c-e*m*c)*b,t[10]=(o*d*r-v*a*r+v*n*h-e*d*h-o*n*c+e*a*c)*b,t[11]=(u*a*r-o*m*r-u*n*h+e*m*h+o*n*f-e*a*f)*b,t[12]=T*b,t[13]=(u*d*s-v*m*s+v*n*x-e*d*x-u*n*y+e*m*y)*b,t[14]=(v*a*s-o*d*s-v*n*l+e*d*l+o*n*y-e*a*y)*b,t[15]=(o*m*s-u*a*s+u*n*l-e*m*l-o*n*x+e*a*x)*b,this}scale(t){let e=this.elements,n=t.x,s=t.y,r=t.z;return e[0]*=n,e[4]*=s,e[8]*=r,e[1]*=n,e[5]*=s,e[9]*=r,e[2]*=n,e[6]*=s,e[10]*=r,e[3]*=n,e[7]*=s,e[11]*=r,this}getMaxScaleOnAxis(){let t=this.elements,e=t[0]*t[0]+t[1]*t[1]+t[2]*t[2],n=t[4]*t[4]+t[5]*t[5]+t[6]*t[6],s=t[8]*t[8]+t[9]*t[9]+t[10]*t[10];return Math.sqrt(Math.max(e,n,s))}makeTranslation(t,e,n){return t.isVector3?this.set(1,0,0,t.x,0,1,0,t.y,0,0,1,t.z,0,0,0,1):this.set(1,0,0,t,0,1,0,e,0,0,1,n,0,0,0,1),this}makeRotationX(t){let e=Math.cos(t),n=Math.sin(t);return this.set(1,0,0,0,0,e,-n,0,0,n,e,0,0,0,0,1),this}makeRotationY(t){let e=Math.cos(t),n=Math.sin(t);return this.set(e,0,n,0,0,1,0,0,-n,0,e,0,0,0,0,1),this}makeRotationZ(t){let e=Math.cos(t),n=Math.sin(t);return this.set(e,-n,0,0,n,e,0,0,0,0,1,0,0,0,0,1),this}makeRotationAxis(t,e){let n=Math.cos(e),s=Math.sin(e),r=1-n,o=t.x,a=t.y,l=t.z,h=r*o,u=r*a;return this.set(h*o+n,h*a-s*l,h*l+s*a,0,h*a+s*l,u*a+n,u*l-s*o,0,h*l-s*a,u*l+s*o,r*l*l+n,0,0,0,0,1),this}makeScale(t,e,n){return this.set(t,0,0,0,0,e,0,0,0,0,n,0,0,0,0,1),this}makeShear(t,e,n,s,r,o){return this.set(1,n,r,0,t,1,o,0,e,s,1,0,0,0,0,1),this}compose(t,e,n){let s=this.elements,r=e._x,o=e._y,a=e._z,l=e._w,h=r+r,u=o+o,m=a+a,x=r*h,f=r*u,v=r*m,d=o*u,y=o*m,c=a*m,_=l*h,p=l*u,g=l*m,T=n.x,S=n.y,b=n.z;return s[0]=(1-(d+c))*T,s[1]=(f+g)*T,s[2]=(v-p)*T,s[3]=0,s[4]=(f-g)*S,s[5]=(1-(x+c))*S,s[6]=(y+_)*S,s[7]=0,s[8]=(v+p)*b,s[9]=(y-_)*b,s[10]=(1-(x+d))*b,s[11]=0,s[12]=t.x,s[13]=t.y,s[14]=t.z,s[15]=1,this}decompose(t,e,n){let s=this.elements,r=as.set(s[0],s[1],s[2]).length(),o=as.set(s[4],s[5],s[6]).length(),a=as.set(s[8],s[9],s[10]).length();this.determinant()<0&&(r=-r),t.x=s[12],t.y=s[13],t.z=s[14],Tn.copy(this);let h=1/r,u=1/o,m=1/a;return Tn.elements[0]*=h,Tn.elements[1]*=h,Tn.elements[2]*=h,Tn.elements[4]*=u,Tn.elements[5]*=u,Tn.elements[6]*=u,Tn.elements[8]*=m,Tn.elements[9]*=m,Tn.elements[10]*=m,e.setFromRotationMatrix(Tn),n.x=r,n.y=o,n.z=a,this}makePerspective(t,e,n,s,r,o,a=Zn){let l=this.elements,h=2*r/(e-t),u=2*r/(n-s),m=(e+t)/(e-t),x=(n+s)/(n-s),f,v;if(a===Zn)f=-(o+r)/(o-r),v=-2*o*r/(o-r);else if(a===wo)f=-o/(o-r),v=-o*r/(o-r);else throw new Error("THREE.Matrix4.makePerspective(): Invalid coordinate system: "+a);return l[0]=h,l[4]=0,l[8]=m,l[12]=0,l[1]=0,l[5]=u,l[9]=x,l[13]=0,l[2]=0,l[6]=0,l[10]=f,l[14]=v,l[3]=0,l[7]=0,l[11]=-1,l[15]=0,this}makeOrthographic(t,e,n,s,r,o,a=Zn){let l=this.elements,h=1/(e-t),u=1/(n-s),m=1/(o-r),x=(e+t)*h,f=(n+s)*u,v,d;if(a===Zn)v=(o+r)*m,d=-2*m;else if(a===wo)v=r*m,d=-1*m;else throw new Error("THREE.Matrix4.makeOrthographic(): Invalid coordinate system: "+a);return l[0]=2*h,l[4]=0,l[8]=0,l[12]=-x,l[1]=0,l[5]=2*u,l[9]=0,l[13]=-f,l[2]=0,l[6]=0,l[10]=d,l[14]=-v,l[3]=0,l[7]=0,l[11]=0,l[15]=1,this}equals(t){let e=this.elements,n=t.elements;for(let s=0;s<16;s++)if(e[s]!==n[s])return!1;return!0}fromArray(t,e=0){for(let n=0;n<16;n++)this.elements[n]=t[n+e];return this}toArray(t=[],e=0){let n=this.elements;return t[e]=n[0],t[e+1]=n[1],t[e+2]=n[2],t[e+3]=n[3],t[e+4]=n[4],t[e+5]=n[5],t[e+6]=n[6],t[e+7]=n[7],t[e+8]=n[8],t[e+9]=n[9],t[e+10]=n[10],t[e+11]=n[11],t[e+12]=n[12],t[e+13]=n[13],t[e+14]=n[14],t[e+15]=n[15],t}},as=new B,Tn=new ee,gp=new B(0,0,0),xp=new B(1,1,1),fi=new B,Xr=new B,ln=new B,Yh=new ee,Zh=new gn,sn=class i{constructor(t=0,e=0,n=0,s=i.DEFAULT_ORDER){this.isEuler=!0,this._x=t,this._y=e,this._z=n,this._order=s}get x(){return this._x}set x(t){this._x=t,this._onChangeCallback()}get y(){return this._y}set y(t){this._y=t,this._onChangeCallback()}get z(){return this._z}set z(t){this._z=t,this._onChangeCallback()}get order(){return this._order}set order(t){this._order=t,this._onChangeCallback()}set(t,e,n,s=this._order){return this._x=t,this._y=e,this._z=n,this._order=s,this._onChangeCallback(),this}clone(){return new this.constructor(this._x,this._y,this._z,this._order)}copy(t){return this._x=t._x,this._y=t._y,this._z=t._z,this._order=t._order,this._onChangeCallback(),this}setFromRotationMatrix(t,e=this._order,n=!0){let s=t.elements,r=s[0],o=s[4],a=s[8],l=s[1],h=s[5],u=s[9],m=s[2],x=s[6],f=s[10];switch(e){case"XYZ":this._y=Math.asin(Xe(a,-1,1)),Math.abs(a)<.9999999?(this._x=Math.atan2(-u,f),this._z=Math.atan2(-o,r)):(this._x=Math.atan2(x,h),this._z=0);break;case"YXZ":this._x=Math.asin(-Xe(u,-1,1)),Math.abs(u)<.9999999?(this._y=Math.atan2(a,f),this._z=Math.atan2(l,h)):(this._y=Math.atan2(-m,r),this._z=0);break;case"ZXY":this._x=Math.asin(Xe(x,-1,1)),Math.abs(x)<.9999999?(this._y=Math.atan2(-m,f),this._z=Math.atan2(-o,h)):(this._y=0,this._z=Math.atan2(l,r));break;case"ZYX":this._y=Math.asin(-Xe(m,-1,1)),Math.abs(m)<.9999999?(this._x=Math.atan2(x,f),this._z=Math.atan2(l,r)):(this._x=0,this._z=Math.atan2(-o,h));break;case"YZX":this._z=Math.asin(Xe(l,-1,1)),Math.abs(l)<.9999999?(this._x=Math.atan2(-u,h),this._y=Math.atan2(-m,r)):(this._x=0,this._y=Math.atan2(a,f));break;case"XZY":this._z=Math.asin(-Xe(o,-1,1)),Math.abs(o)<.9999999?(this._x=Math.atan2(x,h),this._y=Math.atan2(a,r)):(this._x=Math.atan2(-u,f),this._y=0);break;default:console.warn("THREE.Euler: .setFromRotationMatrix() encountered an unknown order: "+e)}return this._order=e,n===!0&&this._onChangeCallback(),this}setFromQuaternion(t,e,n){return Yh.makeRotationFromQuaternion(t),this.setFromRotationMatrix(Yh,e,n)}setFromVector3(t,e=this._order){return this.set(t.x,t.y,t.z,e)}reorder(t){return Zh.setFromEuler(this),this.setFromQuaternion(Zh,t)}equals(t){return t._x===this._x&&t._y===this._y&&t._z===this._z&&t._order===this._order}fromArray(t){return this._x=t[0],this._y=t[1],this._z=t[2],t[3]!==void 0&&(this._order=t[3]),this._onChangeCallback(),this}toArray(t=[],e=0){return t[e]=this._x,t[e+1]=this._y,t[e+2]=this._z,t[e+3]=this._order,t}_onChange(t){return this._onChangeCallback=t,this}_onChangeCallback(){}*[Symbol.iterator](){yield this._x,yield this._y,yield this._z,yield this._order}};sn.DEFAULT_ORDER="XYZ";var Co=class{constructor(){this.mask=1}set(t){this.mask=(1<<t|0)>>>0}enable(t){this.mask|=1<<t|0}enableAll(){this.mask=-1}toggle(t){this.mask^=1<<t|0}disable(t){this.mask&=~(1<<t|0)}disableAll(){this.mask=0}test(t){return(this.mask&t.mask)!==0}isEnabled(t){return(this.mask&(1<<t|0))!==0}},yp=0,$h=new B,ls=new gn,Vn=new ee,qr=new B,js=new B,_p=new B,vp=new gn,Jh=new B(1,0,0),Kh=new B(0,1,0),jh=new B(0,0,1),Qh={type:"added"},Mp={type:"removed"},cs={type:"childadded",child:null},Na={type:"childremoved",child:null},We=class i extends vi{constructor(){super(),this.isObject3D=!0,Object.defineProperty(this,"id",{value:yp++}),this.uuid=$n(),this.name="",this.type="Object3D",this.parent=null,this.children=[],this.up=i.DEFAULT_UP.clone();let t=new B,e=new sn,n=new gn,s=new B(1,1,1);function r(){n.setFromEuler(e,!1)}function o(){e.setFromQuaternion(n,void 0,!1)}e._onChange(r),n._onChange(o),Object.defineProperties(this,{position:{configurable:!0,enumerable:!0,value:t},rotation:{configurable:!0,enumerable:!0,value:e},quaternion:{configurable:!0,enumerable:!0,value:n},scale:{configurable:!0,enumerable:!0,value:s},modelViewMatrix:{value:new ee},normalMatrix:{value:new $t}}),this.matrix=new ee,this.matrixWorld=new ee,this.matrixAutoUpdate=i.DEFAULT_MATRIX_AUTO_UPDATE,this.matrixWorldAutoUpdate=i.DEFAULT_MATRIX_WORLD_AUTO_UPDATE,this.matrixWorldNeedsUpdate=!1,this.layers=new Co,this.visible=!0,this.castShadow=!1,this.receiveShadow=!1,this.frustumCulled=!0,this.renderOrder=0,this.animations=[],this.userData={}}onBeforeShadow(){}onAfterShadow(){}onBeforeRender(){}onAfterRender(){}applyMatrix4(t){this.matrixAutoUpdate&&this.updateMatrix(),this.matrix.premultiply(t),this.matrix.decompose(this.position,this.quaternion,this.scale)}applyQuaternion(t){return this.quaternion.premultiply(t),this}setRotationFromAxisAngle(t,e){this.quaternion.setFromAxisAngle(t,e)}setRotationFromEuler(t){this.quaternion.setFromEuler(t,!0)}setRotationFromMatrix(t){this.quaternion.setFromRotationMatrix(t)}setRotationFromQuaternion(t){this.quaternion.copy(t)}rotateOnAxis(t,e){return ls.setFromAxisAngle(t,e),this.quaternion.multiply(ls),this}rotateOnWorldAxis(t,e){return ls.setFromAxisAngle(t,e),this.quaternion.premultiply(ls),this}rotateX(t){return this.rotateOnAxis(Jh,t)}rotateY(t){return this.rotateOnAxis(Kh,t)}rotateZ(t){return this.rotateOnAxis(jh,t)}translateOnAxis(t,e){return $h.copy(t).applyQuaternion(this.quaternion),this.position.add($h.multiplyScalar(e)),this}translateX(t){return this.translateOnAxis(Jh,t)}translateY(t){return this.translateOnAxis(Kh,t)}translateZ(t){return this.translateOnAxis(jh,t)}localToWorld(t){return this.updateWorldMatrix(!0,!1),t.applyMatrix4(this.matrixWorld)}worldToLocal(t){return this.updateWorldMatrix(!0,!1),t.applyMatrix4(Vn.copy(this.matrixWorld).invert())}lookAt(t,e,n){t.isVector3?qr.copy(t):qr.set(t,e,n);let s=this.parent;this.updateWorldMatrix(!0,!1),js.setFromMatrixPosition(this.matrixWorld),this.isCamera||this.isLight?Vn.lookAt(js,qr,this.up):Vn.lookAt(qr,js,this.up),this.quaternion.setFromRotationMatrix(Vn),s&&(Vn.extractRotation(s.matrixWorld),ls.setFromRotationMatrix(Vn),this.quaternion.premultiply(ls.invert()))}add(t){if(arguments.length>1){for(let e=0;e<arguments.length;e++)this.add(arguments[e]);return this}return t===this?(console.error("THREE.Object3D.add: object can't be added as a child of itself.",t),this):(t&&t.isObject3D?(t.removeFromParent(),t.parent=this,this.children.push(t),t.dispatchEvent(Qh),cs.child=t,this.dispatchEvent(cs),cs.child=null):console.error("THREE.Object3D.add: object not an instance of THREE.Object3D.",t),this)}remove(t){if(arguments.length>1){for(let n=0;n<arguments.length;n++)this.remove(arguments[n]);return this}let e=this.children.indexOf(t);return e!==-1&&(t.parent=null,this.children.splice(e,1),t.dispatchEvent(Mp),Na.child=t,this.dispatchEvent(Na),Na.child=null),this}removeFromParent(){let t=this.parent;return t!==null&&t.remove(this),this}clear(){return this.remove(...this.children)}attach(t){return this.updateWorldMatrix(!0,!1),Vn.copy(this.matrixWorld).invert(),t.parent!==null&&(t.parent.updateWorldMatrix(!0,!1),Vn.multiply(t.parent.matrixWorld)),t.applyMatrix4(Vn),t.removeFromParent(),t.parent=this,this.children.push(t),t.updateWorldMatrix(!1,!0),t.dispatchEvent(Qh),cs.child=t,this.dispatchEvent(cs),cs.child=null,this}getObjectById(t){return this.getObjectByProperty("id",t)}getObjectByName(t){return this.getObjectByProperty("name",t)}getObjectByProperty(t,e){if(this[t]===e)return this;for(let n=0,s=this.children.length;n<s;n++){let o=this.children[n].getObjectByProperty(t,e);if(o!==void 0)return o}}getObjectsByProperty(t,e,n=[]){this[t]===e&&n.push(this);let s=this.children;for(let r=0,o=s.length;r<o;r++)s[r].getObjectsByProperty(t,e,n);return n}getWorldPosition(t){return this.updateWorldMatrix(!0,!1),t.setFromMatrixPosition(this.matrixWorld)}getWorldQuaternion(t){return this.updateWorldMatrix(!0,!1),this.matrixWorld.decompose(js,t,_p),t}getWorldScale(t){return this.updateWorldMatrix(!0,!1),this.matrixWorld.decompose(js,vp,t),t}getWorldDirection(t){this.updateWorldMatrix(!0,!1);let e=this.matrixWorld.elements;return t.set(e[8],e[9],e[10]).normalize()}raycast(){}traverse(t){t(this);let e=this.children;for(let n=0,s=e.length;n<s;n++)e[n].traverse(t)}traverseVisible(t){if(this.visible===!1)return;t(this);let e=this.children;for(let n=0,s=e.length;n<s;n++)e[n].traverseVisible(t)}traverseAncestors(t){let e=this.parent;e!==null&&(t(e),e.traverseAncestors(t))}updateMatrix(){this.matrix.compose(this.position,this.quaternion,this.scale),this.matrixWorldNeedsUpdate=!0}updateMatrixWorld(t){this.matrixAutoUpdate&&this.updateMatrix(),(this.matrixWorldNeedsUpdate||t)&&(this.matrixWorldAutoUpdate===!0&&(this.parent===null?this.matrixWorld.copy(this.matrix):this.matrixWorld.multiplyMatrices(this.parent.matrixWorld,this.matrix)),this.matrixWorldNeedsUpdate=!1,t=!0);let e=this.children;for(let n=0,s=e.length;n<s;n++)e[n].updateMatrixWorld(t)}updateWorldMatrix(t,e){let n=this.parent;if(t===!0&&n!==null&&n.updateWorldMatrix(!0,!1),this.matrixAutoUpdate&&this.updateMatrix(),this.matrixWorldAutoUpdate===!0&&(this.parent===null?this.matrixWorld.copy(this.matrix):this.matrixWorld.multiplyMatrices(this.parent.matrixWorld,this.matrix)),e===!0){let s=this.children;for(let r=0,o=s.length;r<o;r++)s[r].updateWorldMatrix(!1,!0)}}toJSON(t){let e=t===void 0||typeof t=="string",n={};e&&(t={geometries:{},materials:{},textures:{},images:{},shapes:{},skeletons:{},animations:{},nodes:{}},n.metadata={version:4.6,type:"Object",generator:"Object3D.toJSON"});let s={};s.uuid=this.uuid,s.type=this.type,this.name!==""&&(s.name=this.name),this.castShadow===!0&&(s.castShadow=!0),this.receiveShadow===!0&&(s.receiveShadow=!0),this.visible===!1&&(s.visible=!1),this.frustumCulled===!1&&(s.frustumCulled=!1),this.renderOrder!==0&&(s.renderOrder=this.renderOrder),Object.keys(this.userData).length>0&&(s.userData=this.userData),s.layers=this.layers.mask,s.matrix=this.matrix.toArray(),s.up=this.up.toArray(),this.matrixAutoUpdate===!1&&(s.matrixAutoUpdate=!1),this.isInstancedMesh&&(s.type="InstancedMesh",s.count=this.count,s.instanceMatrix=this.instanceMatrix.toJSON(),this.instanceColor!==null&&(s.instanceColor=this.instanceColor.toJSON())),this.isBatchedMesh&&(s.type="BatchedMesh",s.perObjectFrustumCulled=this.perObjectFrustumCulled,s.sortObjects=this.sortObjects,s.drawRanges=this._drawRanges,s.reservedRanges=this._reservedRanges,s.visibility=this._visibility,s.active=this._active,s.bounds=this._bounds.map(a=>({boxInitialized:a.boxInitialized,boxMin:a.box.min.toArray(),boxMax:a.box.max.toArray(),sphereInitialized:a.sphereInitialized,sphereRadius:a.sphere.radius,sphereCenter:a.sphere.center.toArray()})),s.maxInstanceCount=this._maxInstanceCount,s.maxVertexCount=this._maxVertexCount,s.maxIndexCount=this._maxIndexCount,s.geometryInitialized=this._geometryInitialized,s.geometryCount=this._geometryCount,s.matricesTexture=this._matricesTexture.toJSON(t),this._colorsTexture!==null&&(s.colorsTexture=this._colorsTexture.toJSON(t)),this.boundingSphere!==null&&(s.boundingSphere={center:s.boundingSphere.center.toArray(),radius:s.boundingSphere.radius}),this.boundingBox!==null&&(s.boundingBox={min:s.boundingBox.min.toArray(),max:s.boundingBox.max.toArray()}));function r(a,l){return a[l.uuid]===void 0&&(a[l.uuid]=l.toJSON(t)),l.uuid}if(this.isScene)this.background&&(this.background.isColor?s.background=this.background.toJSON():this.background.isTexture&&(s.background=this.background.toJSON(t).uuid)),this.environment&&this.environment.isTexture&&this.environment.isRenderTargetTexture!==!0&&(s.environment=this.environment.toJSON(t).uuid);else if(this.isMesh||this.isLine||this.isPoints){s.geometry=r(t.geometries,this.geometry);let a=this.geometry.parameters;if(a!==void 0&&a.shapes!==void 0){let l=a.shapes;if(Array.isArray(l))for(let h=0,u=l.length;h<u;h++){let m=l[h];r(t.shapes,m)}else r(t.shapes,l)}}if(this.isSkinnedMesh&&(s.bindMode=this.bindMode,s.bindMatrix=this.bindMatrix.toArray(),this.skeleton!==void 0&&(r(t.skeletons,this.skeleton),s.skeleton=this.skeleton.uuid)),this.material!==void 0)if(Array.isArray(this.material)){let a=[];for(let l=0,h=this.material.length;l<h;l++)a.push(r(t.materials,this.material[l]));s.material=a}else s.material=r(t.materials,this.material);if(this.children.length>0){s.children=[];for(let a=0;a<this.children.length;a++)s.children.push(this.children[a].toJSON(t).object)}if(this.animations.length>0){s.animations=[];for(let a=0;a<this.animations.length;a++){let l=this.animations[a];s.animations.push(r(t.animations,l))}}if(e){let a=o(t.geometries),l=o(t.materials),h=o(t.textures),u=o(t.images),m=o(t.shapes),x=o(t.skeletons),f=o(t.animations),v=o(t.nodes);a.length>0&&(n.geometries=a),l.length>0&&(n.materials=l),h.length>0&&(n.textures=h),u.length>0&&(n.images=u),m.length>0&&(n.shapes=m),x.length>0&&(n.skeletons=x),f.length>0&&(n.animations=f),v.length>0&&(n.nodes=v)}return n.object=s,n;function o(a){let l=[];for(let h in a){let u=a[h];delete u.metadata,l.push(u)}return l}}clone(t){return new this.constructor().copy(this,t)}copy(t,e=!0){if(this.name=t.name,this.up.copy(t.up),this.position.copy(t.position),this.rotation.order=t.rotation.order,this.quaternion.copy(t.quaternion),this.scale.copy(t.scale),this.matrix.copy(t.matrix),this.matrixWorld.copy(t.matrixWorld),this.matrixAutoUpdate=t.matrixAutoUpdate,this.matrixWorldAutoUpdate=t.matrixWorldAutoUpdate,this.matrixWorldNeedsUpdate=t.matrixWorldNeedsUpdate,this.layers.mask=t.layers.mask,this.visible=t.visible,this.castShadow=t.castShadow,this.receiveShadow=t.receiveShadow,this.frustumCulled=t.frustumCulled,this.renderOrder=t.renderOrder,this.animations=t.animations.slice(),this.userData=JSON.parse(JSON.stringify(t.userData)),e===!0)for(let n=0;n<t.children.length;n++){let s=t.children[n];this.add(s.clone())}return this}};We.DEFAULT_UP=new B(0,1,0);We.DEFAULT_MATRIX_AUTO_UPDATE=!0;We.DEFAULT_MATRIX_WORLD_AUTO_UPDATE=!0;var wn=new B,Wn=new B,Ba=new B,Xn=new B,hs=new B,us=new B,tu=new B,za=new B,Oa=new B,Ha=new B,ka=new Ue,Ga=new Ue,Va=new Ue,gi=class i{constructor(t=new B,e=new B,n=new B){this.a=t,this.b=e,this.c=n}static getNormal(t,e,n,s){s.subVectors(n,e),wn.subVectors(t,e),s.cross(wn);let r=s.lengthSq();return r>0?s.multiplyScalar(1/Math.sqrt(r)):s.set(0,0,0)}static getBarycoord(t,e,n,s,r){wn.subVectors(s,e),Wn.subVectors(n,e),Ba.subVectors(t,e);let o=wn.dot(wn),a=wn.dot(Wn),l=wn.dot(Ba),h=Wn.dot(Wn),u=Wn.dot(Ba),m=o*h-a*a;if(m===0)return r.set(0,0,0),null;let x=1/m,f=(h*l-a*u)*x,v=(o*u-a*l)*x;return r.set(1-f-v,v,f)}static containsPoint(t,e,n,s){return this.getBarycoord(t,e,n,s,Xn)===null?!1:Xn.x>=0&&Xn.y>=0&&Xn.x+Xn.y<=1}static getInterpolation(t,e,n,s,r,o,a,l){return this.getBarycoord(t,e,n,s,Xn)===null?(l.x=0,l.y=0,"z"in l&&(l.z=0),"w"in l&&(l.w=0),null):(l.setScalar(0),l.addScaledVector(r,Xn.x),l.addScaledVector(o,Xn.y),l.addScaledVector(a,Xn.z),l)}static getInterpolatedAttribute(t,e,n,s,r,o){return ka.setScalar(0),Ga.setScalar(0),Va.setScalar(0),ka.fromBufferAttribute(t,e),Ga.fromBufferAttribute(t,n),Va.fromBufferAttribute(t,s),o.setScalar(0),o.addScaledVector(ka,r.x),o.addScaledVector(Ga,r.y),o.addScaledVector(Va,r.z),o}static isFrontFacing(t,e,n,s){return wn.subVectors(n,e),Wn.subVectors(t,e),wn.cross(Wn).dot(s)<0}set(t,e,n){return this.a.copy(t),this.b.copy(e),this.c.copy(n),this}setFromPointsAndIndices(t,e,n,s){return this.a.copy(t[e]),this.b.copy(t[n]),this.c.copy(t[s]),this}setFromAttributeAndIndices(t,e,n,s){return this.a.fromBufferAttribute(t,e),this.b.fromBufferAttribute(t,n),this.c.fromBufferAttribute(t,s),this}clone(){return new this.constructor().copy(this)}copy(t){return this.a.copy(t.a),this.b.copy(t.b),this.c.copy(t.c),this}getArea(){return wn.subVectors(this.c,this.b),Wn.subVectors(this.a,this.b),wn.cross(Wn).length()*.5}getMidpoint(t){return t.addVectors(this.a,this.b).add(this.c).multiplyScalar(1/3)}getNormal(t){return i.getNormal(this.a,this.b,this.c,t)}getPlane(t){return t.setFromCoplanarPoints(this.a,this.b,this.c)}getBarycoord(t,e){return i.getBarycoord(t,this.a,this.b,this.c,e)}getInterpolation(t,e,n,s,r){return i.getInterpolation(t,this.a,this.b,this.c,e,n,s,r)}containsPoint(t){return i.containsPoint(t,this.a,this.b,this.c)}isFrontFacing(t){return i.isFrontFacing(this.a,this.b,this.c,t)}intersectsBox(t){return t.intersectsTriangle(this)}closestPointToPoint(t,e){let n=this.a,s=this.b,r=this.c,o,a;hs.subVectors(s,n),us.subVectors(r,n),za.subVectors(t,n);let l=hs.dot(za),h=us.dot(za);if(l<=0&&h<=0)return e.copy(n);Oa.subVectors(t,s);let u=hs.dot(Oa),m=us.dot(Oa);if(u>=0&&m<=u)return e.copy(s);let x=l*m-u*h;if(x<=0&&l>=0&&u<=0)return o=l/(l-u),e.copy(n).addScaledVector(hs,o);Ha.subVectors(t,r);let f=hs.dot(Ha),v=us.dot(Ha);if(v>=0&&f<=v)return e.copy(r);let d=f*h-l*v;if(d<=0&&h>=0&&v<=0)return a=h/(h-v),e.copy(n).addScaledVector(us,a);let y=u*v-f*m;if(y<=0&&m-u>=0&&f-v>=0)return tu.subVectors(r,s),a=(m-u)/(m-u+(f-v)),e.copy(s).addScaledVector(tu,a);let c=1/(y+d+x);return o=d*c,a=x*c,e.copy(n).addScaledVector(hs,o).addScaledVector(us,a)}equals(t){return t.a.equals(this.a)&&t.b.equals(this.b)&&t.c.equals(this.c)}},af={aliceblue:15792383,antiquewhite:16444375,aqua:65535,aquamarine:8388564,azure:15794175,beige:16119260,bisque:16770244,black:0,blanchedalmond:16772045,blue:255,blueviolet:9055202,brown:10824234,burlywood:14596231,cadetblue:6266528,chartreuse:8388352,chocolate:13789470,coral:16744272,cornflowerblue:6591981,cornsilk:16775388,crimson:14423100,cyan:65535,darkblue:139,darkcyan:35723,darkgoldenrod:12092939,darkgray:11119017,darkgreen:25600,darkgrey:11119017,darkkhaki:12433259,darkmagenta:9109643,darkolivegreen:5597999,darkorange:16747520,darkorchid:10040012,darkred:9109504,darksalmon:15308410,darkseagreen:9419919,darkslateblue:4734347,darkslategray:3100495,darkslategrey:3100495,darkturquoise:52945,darkviolet:9699539,deeppink:16716947,deepskyblue:49151,dimgray:6908265,dimgrey:6908265,dodgerblue:2003199,firebrick:11674146,floralwhite:16775920,forestgreen:2263842,fuchsia:16711935,gainsboro:14474460,ghostwhite:16316671,gold:16766720,goldenrod:14329120,gray:8421504,green:32768,greenyellow:11403055,grey:8421504,honeydew:15794160,hotpink:16738740,indianred:13458524,indigo:4915330,ivory:16777200,khaki:15787660,lavender:15132410,lavenderblush:16773365,lawngreen:8190976,lemonchiffon:16775885,lightblue:11393254,lightcoral:15761536,lightcyan:14745599,lightgoldenrodyellow:16448210,lightgray:13882323,lightgreen:9498256,lightgrey:13882323,lightpink:16758465,lightsalmon:16752762,lightseagreen:2142890,lightskyblue:8900346,lightslategray:7833753,lightslategrey:7833753,lightsteelblue:11584734,lightyellow:16777184,lime:65280,limegreen:3329330,linen:16445670,magenta:16711935,maroon:8388608,mediumaquamarine:6737322,mediumblue:205,mediumorchid:12211667,mediumpurple:9662683,mediumseagreen:3978097,mediumslateblue:8087790,mediumspringgreen:64154,mediumturquoise:4772300,mediumvioletred:13047173,midnightblue:1644912,mintcream:16121850,mistyrose:16770273,moccasin:16770229,navajowhite:16768685,navy:128,oldlace:16643558,olive:8421376,olivedrab:7048739,orange:16753920,orangered:16729344,orchid:14315734,palegoldenrod:15657130,palegreen:10025880,paleturquoise:11529966,palevioletred:14381203,papayawhip:16773077,peachpuff:16767673,peru:13468991,pink:16761035,plum:14524637,powderblue:11591910,purple:8388736,rebeccapurple:6697881,red:16711680,rosybrown:12357519,royalblue:4286945,saddlebrown:9127187,salmon:16416882,sandybrown:16032864,seagreen:3050327,seashell:16774638,sienna:10506797,silver:12632256,skyblue:8900331,slateblue:6970061,slategray:7372944,slategrey:7372944,snow:16775930,springgreen:65407,steelblue:4620980,tan:13808780,teal:32896,thistle:14204888,tomato:16737095,turquoise:4251856,violet:15631086,wheat:16113331,white:16777215,whitesmoke:16119285,yellow:16776960,yellowgreen:10145074},di={h:0,s:0,l:0},Yr={h:0,s:0,l:0};function Wa(i,t,e){return e<0&&(e+=1),e>1&&(e-=1),e<1/6?i+(t-i)*6*e:e<1/2?t:e<2/3?i+(t-i)*6*(2/3-e):i}var Jt=class{constructor(t,e,n){return this.isColor=!0,this.r=1,this.g=1,this.b=1,this.set(t,e,n)}set(t,e,n){if(e===void 0&&n===void 0){let s=t;s&&s.isColor?this.copy(s):typeof s=="number"?this.setHex(s):typeof s=="string"&&this.setStyle(s)}else this.setRGB(t,e,n);return this}setScalar(t){return this.r=t,this.g=t,this.b=t,this}setHex(t,e=ze){return t=Math.floor(t),this.r=(t>>16&255)/255,this.g=(t>>8&255)/255,this.b=(t&255)/255,ie.toWorkingColorSpace(this,e),this}setRGB(t,e,n,s=ie.workingColorSpace){return this.r=t,this.g=e,this.b=n,ie.toWorkingColorSpace(this,s),this}setHSL(t,e,n,s=ie.workingColorSpace){if(t=lp(t,1),e=Xe(e,0,1),n=Xe(n,0,1),e===0)this.r=this.g=this.b=n;else{let r=n<=.5?n*(1+e):n+e-n*e,o=2*n-r;this.r=Wa(o,r,t+1/3),this.g=Wa(o,r,t),this.b=Wa(o,r,t-1/3)}return ie.toWorkingColorSpace(this,s),this}setStyle(t,e=ze){function n(r){r!==void 0&&parseFloat(r)<1&&console.warn("THREE.Color: Alpha component of "+t+" will be ignored.")}let s;if(s=/^(\w+)\(([^\)]*)\)/.exec(t)){let r,o=s[1],a=s[2];switch(o){case"rgb":case"rgba":if(r=/^\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*(\d*\.?\d+)\s*)?$/.exec(a))return n(r[4]),this.setRGB(Math.min(255,parseInt(r[1],10))/255,Math.min(255,parseInt(r[2],10))/255,Math.min(255,parseInt(r[3],10))/255,e);if(r=/^\s*(\d+)\%\s*,\s*(\d+)\%\s*,\s*(\d+)\%\s*(?:,\s*(\d*\.?\d+)\s*)?$/.exec(a))return n(r[4]),this.setRGB(Math.min(100,parseInt(r[1],10))/100,Math.min(100,parseInt(r[2],10))/100,Math.min(100,parseInt(r[3],10))/100,e);break;case"hsl":case"hsla":if(r=/^\s*(\d*\.?\d+)\s*,\s*(\d*\.?\d+)\%\s*,\s*(\d*\.?\d+)\%\s*(?:,\s*(\d*\.?\d+)\s*)?$/.exec(a))return n(r[4]),this.setHSL(parseFloat(r[1])/360,parseFloat(r[2])/100,parseFloat(r[3])/100,e);break;default:console.warn("THREE.Color: Unknown color model "+t)}}else if(s=/^\#([A-Fa-f\d]+)$/.exec(t)){let r=s[1],o=r.length;if(o===3)return this.setRGB(parseInt(r.charAt(0),16)/15,parseInt(r.charAt(1),16)/15,parseInt(r.charAt(2),16)/15,e);if(o===6)return this.setHex(parseInt(r,16),e);console.warn("THREE.Color: Invalid hex color "+t)}else if(t&&t.length>0)return this.setColorName(t,e);return this}setColorName(t,e=ze){let n=af[t.toLowerCase()];return n!==void 0?this.setHex(n,e):console.warn("THREE.Color: Unknown color "+t),this}clone(){return new this.constructor(this.r,this.g,this.b)}copy(t){return this.r=t.r,this.g=t.g,this.b=t.b,this}copySRGBToLinear(t){return this.r=Jn(t.r),this.g=Jn(t.g),this.b=Jn(t.b),this}copyLinearToSRGB(t){return this.r=Ts(t.r),this.g=Ts(t.g),this.b=Ts(t.b),this}convertSRGBToLinear(){return this.copySRGBToLinear(this),this}convertLinearToSRGB(){return this.copyLinearToSRGB(this),this}getHex(t=ze){return ie.fromWorkingColorSpace($e.copy(this),t),Math.round(Xe($e.r*255,0,255))*65536+Math.round(Xe($e.g*255,0,255))*256+Math.round(Xe($e.b*255,0,255))}getHexString(t=ze){return("000000"+this.getHex(t).toString(16)).slice(-6)}getHSL(t,e=ie.workingColorSpace){ie.fromWorkingColorSpace($e.copy(this),e);let n=$e.r,s=$e.g,r=$e.b,o=Math.max(n,s,r),a=Math.min(n,s,r),l,h,u=(a+o)/2;if(a===o)l=0,h=0;else{let m=o-a;switch(h=u<=.5?m/(o+a):m/(2-o-a),o){case n:l=(s-r)/m+(s<r?6:0);break;case s:l=(r-n)/m+2;break;case r:l=(n-s)/m+4;break}l/=6}return t.h=l,t.s=h,t.l=u,t}getRGB(t,e=ie.workingColorSpace){return ie.fromWorkingColorSpace($e.copy(this),e),t.r=$e.r,t.g=$e.g,t.b=$e.b,t}getStyle(t=ze){ie.fromWorkingColorSpace($e.copy(this),t);let e=$e.r,n=$e.g,s=$e.b;return t!==ze?`color(${t} ${e.toFixed(3)} ${n.toFixed(3)} ${s.toFixed(3)})`:`rgb(${Math.round(e*255)},${Math.round(n*255)},${Math.round(s*255)})`}offsetHSL(t,e,n){return this.getHSL(di),this.setHSL(di.h+t,di.s+e,di.l+n)}add(t){return this.r+=t.r,this.g+=t.g,this.b+=t.b,this}addColors(t,e){return this.r=t.r+e.r,this.g=t.g+e.g,this.b=t.b+e.b,this}addScalar(t){return this.r+=t,this.g+=t,this.b+=t,this}sub(t){return this.r=Math.max(0,this.r-t.r),this.g=Math.max(0,this.g-t.g),this.b=Math.max(0,this.b-t.b),this}multiply(t){return this.r*=t.r,this.g*=t.g,this.b*=t.b,this}multiplyScalar(t){return this.r*=t,this.g*=t,this.b*=t,this}lerp(t,e){return this.r+=(t.r-this.r)*e,this.g+=(t.g-this.g)*e,this.b+=(t.b-this.b)*e,this}lerpColors(t,e,n){return this.r=t.r+(e.r-t.r)*n,this.g=t.g+(e.g-t.g)*n,this.b=t.b+(e.b-t.b)*n,this}lerpHSL(t,e){this.getHSL(di),t.getHSL(Yr);let n=Aa(di.h,Yr.h,e),s=Aa(di.s,Yr.s,e),r=Aa(di.l,Yr.l,e);return this.setHSL(n,s,r),this}setFromVector3(t){return this.r=t.x,this.g=t.y,this.b=t.z,this}applyMatrix3(t){let e=this.r,n=this.g,s=this.b,r=t.elements;return this.r=r[0]*e+r[3]*n+r[6]*s,this.g=r[1]*e+r[4]*n+r[7]*s,this.b=r[2]*e+r[5]*n+r[8]*s,this}equals(t){return t.r===this.r&&t.g===this.g&&t.b===this.b}fromArray(t,e=0){return this.r=t[e],this.g=t[e+1],this.b=t[e+2],this}toArray(t=[],e=0){return t[e]=this.r,t[e+1]=this.g,t[e+2]=this.b,t}fromBufferAttribute(t,e){return this.r=t.getX(e),this.g=t.getY(e),this.b=t.getZ(e),this}toJSON(){return this.getHex()}*[Symbol.iterator](){yield this.r,yield this.g,yield this.b}},$e=new Jt;Jt.NAMES=af;var Ep=0,Un=class extends vi{static get type(){return"Material"}get type(){return this.constructor.type}set type(t){}constructor(){super(),this.isMaterial=!0,Object.defineProperty(this,"id",{value:Ep++}),this.uuid=$n(),this.name="",this.blending=yi,this.side=Dn,this.vertexColors=!1,this.opacity=1,this.transparent=!1,this.alphaHash=!1,this.blendSrc=ol,this.blendDst=al,this.blendEquation=Bi,this.blendSrcAlpha=null,this.blendDstAlpha=null,this.blendEquationAlpha=null,this.blendColor=new Jt(0,0,0),this.blendAlpha=0,this.depthFunc=As,this.depthTest=!0,this.depthWrite=!0,this.stencilWriteMask=255,this.stencilFunc=zh,this.stencilRef=0,this.stencilFuncMask=255,this.stencilFail=ns,this.stencilZFail=ns,this.stencilZPass=ns,this.stencilWrite=!1,this.clippingPlanes=null,this.clipIntersection=!1,this.clipShadows=!1,this.shadowSide=null,this.colorWrite=!0,this.precision=null,this.polygonOffset=!1,this.polygonOffsetFactor=0,this.polygonOffsetUnits=0,this.dithering=!1,this.alphaToCoverage=!1,this.premultipliedAlpha=!1,this.forceSinglePass=!1,this.visible=!0,this.toneMapped=!0,this.userData={},this.version=0,this._alphaTest=0}get alphaTest(){return this._alphaTest}set alphaTest(t){this._alphaTest>0!=t>0&&this.version++,this._alphaTest=t}onBeforeRender(){}onBeforeCompile(){}customProgramCacheKey(){return this.onBeforeCompile.toString()}setValues(t){if(t!==void 0)for(let e in t){let n=t[e];if(n===void 0){console.warn(`THREE.Material: parameter '${e}' has value of undefined.`);continue}let s=this[e];if(s===void 0){console.warn(`THREE.Material: '${e}' is not a property of THREE.${this.type}.`);continue}s&&s.isColor?s.set(n):s&&s.isVector3&&n&&n.isVector3?s.copy(n):this[e]=n}}toJSON(t){let e=t===void 0||typeof t=="string";e&&(t={textures:{},images:{}});let n={metadata:{version:4.6,type:"Material",generator:"Material.toJSON"}};n.uuid=this.uuid,n.type=this.type,this.name!==""&&(n.name=this.name),this.color&&this.color.isColor&&(n.color=this.color.getHex()),this.roughness!==void 0&&(n.roughness=this.roughness),this.metalness!==void 0&&(n.metalness=this.metalness),this.sheen!==void 0&&(n.sheen=this.sheen),this.sheenColor&&this.sheenColor.isColor&&(n.sheenColor=this.sheenColor.getHex()),this.sheenRoughness!==void 0&&(n.sheenRoughness=this.sheenRoughness),this.emissive&&this.emissive.isColor&&(n.emissive=this.emissive.getHex()),this.emissiveIntensity!==void 0&&this.emissiveIntensity!==1&&(n.emissiveIntensity=this.emissiveIntensity),this.specular&&this.specular.isColor&&(n.specular=this.specular.getHex()),this.specularIntensity!==void 0&&(n.specularIntensity=this.specularIntensity),this.specularColor&&this.specularColor.isColor&&(n.specularColor=this.specularColor.getHex()),this.shininess!==void 0&&(n.shininess=this.shininess),this.clearcoat!==void 0&&(n.clearcoat=this.clearcoat),this.clearcoatRoughness!==void 0&&(n.clearcoatRoughness=this.clearcoatRoughness),this.clearcoatMap&&this.clearcoatMap.isTexture&&(n.clearcoatMap=this.clearcoatMap.toJSON(t).uuid),this.clearcoatRoughnessMap&&this.clearcoatRoughnessMap.isTexture&&(n.clearcoatRoughnessMap=this.clearcoatRoughnessMap.toJSON(t).uuid),this.clearcoatNormalMap&&this.clearcoatNormalMap.isTexture&&(n.clearcoatNormalMap=this.clearcoatNormalMap.toJSON(t).uuid,n.clearcoatNormalScale=this.clearcoatNormalScale.toArray()),this.dispersion!==void 0&&(n.dispersion=this.dispersion),this.iridescence!==void 0&&(n.iridescence=this.iridescence),this.iridescenceIOR!==void 0&&(n.iridescenceIOR=this.iridescenceIOR),this.iridescenceThicknessRange!==void 0&&(n.iridescenceThicknessRange=this.iridescenceThicknessRange),this.iridescenceMap&&this.iridescenceMap.isTexture&&(n.iridescenceMap=this.iridescenceMap.toJSON(t).uuid),this.iridescenceThicknessMap&&this.iridescenceThicknessMap.isTexture&&(n.iridescenceThicknessMap=this.iridescenceThicknessMap.toJSON(t).uuid),this.anisotropy!==void 0&&(n.anisotropy=this.anisotropy),this.anisotropyRotation!==void 0&&(n.anisotropyRotation=this.anisotropyRotation),this.anisotropyMap&&this.anisotropyMap.isTexture&&(n.anisotropyMap=this.anisotropyMap.toJSON(t).uuid),this.map&&this.map.isTexture&&(n.map=this.map.toJSON(t).uuid),this.matcap&&this.matcap.isTexture&&(n.matcap=this.matcap.toJSON(t).uuid),this.alphaMap&&this.alphaMap.isTexture&&(n.alphaMap=this.alphaMap.toJSON(t).uuid),this.lightMap&&this.lightMap.isTexture&&(n.lightMap=this.lightMap.toJSON(t).uuid,n.lightMapIntensity=this.lightMapIntensity),this.aoMap&&this.aoMap.isTexture&&(n.aoMap=this.aoMap.toJSON(t).uuid,n.aoMapIntensity=this.aoMapIntensity),this.bumpMap&&this.bumpMap.isTexture&&(n.bumpMap=this.bumpMap.toJSON(t).uuid,n.bumpScale=this.bumpScale),this.normalMap&&this.normalMap.isTexture&&(n.normalMap=this.normalMap.toJSON(t).uuid,n.normalMapType=this.normalMapType,n.normalScale=this.normalScale.toArray()),this.displacementMap&&this.displacementMap.isTexture&&(n.displacementMap=this.displacementMap.toJSON(t).uuid,n.displacementScale=this.displacementScale,n.displacementBias=this.displacementBias),this.roughnessMap&&this.roughnessMap.isTexture&&(n.roughnessMap=this.roughnessMap.toJSON(t).uuid),this.metalnessMap&&this.metalnessMap.isTexture&&(n.metalnessMap=this.metalnessMap.toJSON(t).uuid),this.emissiveMap&&this.emissiveMap.isTexture&&(n.emissiveMap=this.emissiveMap.toJSON(t).uuid),this.specularMap&&this.specularMap.isTexture&&(n.specularMap=this.specularMap.toJSON(t).uuid),this.specularIntensityMap&&this.specularIntensityMap.isTexture&&(n.specularIntensityMap=this.specularIntensityMap.toJSON(t).uuid),this.specularColorMap&&this.specularColorMap.isTexture&&(n.specularColorMap=this.specularColorMap.toJSON(t).uuid),this.envMap&&this.envMap.isTexture&&(n.envMap=this.envMap.toJSON(t).uuid,this.combine!==void 0&&(n.combine=this.combine)),this.envMapRotation!==void 0&&(n.envMapRotation=this.envMapRotation.toArray()),this.envMapIntensity!==void 0&&(n.envMapIntensity=this.envMapIntensity),this.reflectivity!==void 0&&(n.reflectivity=this.reflectivity),this.refractionRatio!==void 0&&(n.refractionRatio=this.refractionRatio),this.gradientMap&&this.gradientMap.isTexture&&(n.gradientMap=this.gradientMap.toJSON(t).uuid),this.transmission!==void 0&&(n.transmission=this.transmission),this.transmissionMap&&this.transmissionMap.isTexture&&(n.transmissionMap=this.transmissionMap.toJSON(t).uuid),this.thickness!==void 0&&(n.thickness=this.thickness),this.thicknessMap&&this.thicknessMap.isTexture&&(n.thicknessMap=this.thicknessMap.toJSON(t).uuid),this.attenuationDistance!==void 0&&this.attenuationDistance!==1/0&&(n.attenuationDistance=this.attenuationDistance),this.attenuationColor!==void 0&&(n.attenuationColor=this.attenuationColor.getHex()),this.size!==void 0&&(n.size=this.size),this.shadowSide!==null&&(n.shadowSide=this.shadowSide),this.sizeAttenuation!==void 0&&(n.sizeAttenuation=this.sizeAttenuation),this.blending!==yi&&(n.blending=this.blending),this.side!==Dn&&(n.side=this.side),this.vertexColors===!0&&(n.vertexColors=!0),this.opacity<1&&(n.opacity=this.opacity),this.transparent===!0&&(n.transparent=!0),this.blendSrc!==ol&&(n.blendSrc=this.blendSrc),this.blendDst!==al&&(n.blendDst=this.blendDst),this.blendEquation!==Bi&&(n.blendEquation=this.blendEquation),this.blendSrcAlpha!==null&&(n.blendSrcAlpha=this.blendSrcAlpha),this.blendDstAlpha!==null&&(n.blendDstAlpha=this.blendDstAlpha),this.blendEquationAlpha!==null&&(n.blendEquationAlpha=this.blendEquationAlpha),this.blendColor&&this.blendColor.isColor&&(n.blendColor=this.blendColor.getHex()),this.blendAlpha!==0&&(n.blendAlpha=this.blendAlpha),this.depthFunc!==As&&(n.depthFunc=this.depthFunc),this.depthTest===!1&&(n.depthTest=this.depthTest),this.depthWrite===!1&&(n.depthWrite=this.depthWrite),this.colorWrite===!1&&(n.colorWrite=this.colorWrite),this.stencilWriteMask!==255&&(n.stencilWriteMask=this.stencilWriteMask),this.stencilFunc!==zh&&(n.stencilFunc=this.stencilFunc),this.stencilRef!==0&&(n.stencilRef=this.stencilRef),this.stencilFuncMask!==255&&(n.stencilFuncMask=this.stencilFuncMask),this.stencilFail!==ns&&(n.stencilFail=this.stencilFail),this.stencilZFail!==ns&&(n.stencilZFail=this.stencilZFail),this.stencilZPass!==ns&&(n.stencilZPass=this.stencilZPass),this.stencilWrite===!0&&(n.stencilWrite=this.stencilWrite),this.rotation!==void 0&&this.rotation!==0&&(n.rotation=this.rotation),this.polygonOffset===!0&&(n.polygonOffset=!0),this.polygonOffsetFactor!==0&&(n.polygonOffsetFactor=this.polygonOffsetFactor),this.polygonOffsetUnits!==0&&(n.polygonOffsetUnits=this.polygonOffsetUnits),this.linewidth!==void 0&&this.linewidth!==1&&(n.linewidth=this.linewidth),this.dashSize!==void 0&&(n.dashSize=this.dashSize),this.gapSize!==void 0&&(n.gapSize=this.gapSize),this.scale!==void 0&&(n.scale=this.scale),this.dithering===!0&&(n.dithering=!0),this.alphaTest>0&&(n.alphaTest=this.alphaTest),this.alphaHash===!0&&(n.alphaHash=!0),this.alphaToCoverage===!0&&(n.alphaToCoverage=!0),this.premultipliedAlpha===!0&&(n.premultipliedAlpha=!0),this.forceSinglePass===!0&&(n.forceSinglePass=!0),this.wireframe===!0&&(n.wireframe=!0),this.wireframeLinewidth>1&&(n.wireframeLinewidth=this.wireframeLinewidth),this.wireframeLinecap!=="round"&&(n.wireframeLinecap=this.wireframeLinecap),this.wireframeLinejoin!=="round"&&(n.wireframeLinejoin=this.wireframeLinejoin),this.flatShading===!0&&(n.flatShading=!0),this.visible===!1&&(n.visible=!1),this.toneMapped===!1&&(n.toneMapped=!1),this.fog===!1&&(n.fog=!1),Object.keys(this.userData).length>0&&(n.userData=this.userData);function s(r){let o=[];for(let a in r){let l=r[a];delete l.metadata,o.push(l)}return o}if(e){let r=s(t.textures),o=s(t.images);r.length>0&&(n.textures=r),o.length>0&&(n.images=o)}return n}clone(){return new this.constructor().copy(this)}copy(t){this.name=t.name,this.blending=t.blending,this.side=t.side,this.vertexColors=t.vertexColors,this.opacity=t.opacity,this.transparent=t.transparent,this.blendSrc=t.blendSrc,this.blendDst=t.blendDst,this.blendEquation=t.blendEquation,this.blendSrcAlpha=t.blendSrcAlpha,this.blendDstAlpha=t.blendDstAlpha,this.blendEquationAlpha=t.blendEquationAlpha,this.blendColor.copy(t.blendColor),this.blendAlpha=t.blendAlpha,this.depthFunc=t.depthFunc,this.depthTest=t.depthTest,this.depthWrite=t.depthWrite,this.stencilWriteMask=t.stencilWriteMask,this.stencilFunc=t.stencilFunc,this.stencilRef=t.stencilRef,this.stencilFuncMask=t.stencilFuncMask,this.stencilFail=t.stencilFail,this.stencilZFail=t.stencilZFail,this.stencilZPass=t.stencilZPass,this.stencilWrite=t.stencilWrite;let e=t.clippingPlanes,n=null;if(e!==null){let s=e.length;n=new Array(s);for(let r=0;r!==s;++r)n[r]=e[r].clone()}return this.clippingPlanes=n,this.clipIntersection=t.clipIntersection,this.clipShadows=t.clipShadows,this.shadowSide=t.shadowSide,this.colorWrite=t.colorWrite,this.precision=t.precision,this.polygonOffset=t.polygonOffset,this.polygonOffsetFactor=t.polygonOffsetFactor,this.polygonOffsetUnits=t.polygonOffsetUnits,this.dithering=t.dithering,this.alphaTest=t.alphaTest,this.alphaHash=t.alphaHash,this.alphaToCoverage=t.alphaToCoverage,this.premultipliedAlpha=t.premultipliedAlpha,this.forceSinglePass=t.forceSinglePass,this.visible=t.visible,this.toneMapped=t.toneMapped,this.userData=JSON.parse(JSON.stringify(t.userData)),this}dispose(){this.dispatchEvent({type:"dispose"})}set needsUpdate(t){t===!0&&this.version++}onBuild(){console.warn("Material: onBuild() has been removed.")}},oe=class extends Un{static get type(){return"MeshBasicMaterial"}constructor(t){super(),this.isMeshBasicMaterial=!0,this.color=new Jt(16777215),this.map=null,this.lightMap=null,this.lightMapIntensity=1,this.aoMap=null,this.aoMapIntensity=1,this.specularMap=null,this.alphaMap=null,this.envMap=null,this.envMapRotation=new sn,this.combine=Uc,this.reflectivity=1,this.refractionRatio=.98,this.wireframe=!1,this.wireframeLinewidth=1,this.wireframeLinecap="round",this.wireframeLinejoin="round",this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.lightMap=t.lightMap,this.lightMapIntensity=t.lightMapIntensity,this.aoMap=t.aoMap,this.aoMapIntensity=t.aoMapIntensity,this.specularMap=t.specularMap,this.alphaMap=t.alphaMap,this.envMap=t.envMap,this.envMapRotation.copy(t.envMapRotation),this.combine=t.combine,this.reflectivity=t.reflectivity,this.refractionRatio=t.refractionRatio,this.wireframe=t.wireframe,this.wireframeLinewidth=t.wireframeLinewidth,this.wireframeLinecap=t.wireframeLinecap,this.wireframeLinejoin=t.wireframeLinejoin,this.fog=t.fog,this}};var Ne=new B,Zr=new lt,we=class{constructor(t,e,n=!1){if(Array.isArray(t))throw new TypeError("THREE.BufferAttribute: array should be a Typed Array.");this.isBufferAttribute=!0,this.name="",this.array=t,this.itemSize=e,this.count=t!==void 0?t.length/e:0,this.normalized=n,this.usage=Xl,this.updateRanges=[],this.gpuType=Ln,this.version=0}onUploadCallback(){}set needsUpdate(t){t===!0&&this.version++}setUsage(t){return this.usage=t,this}addUpdateRange(t,e){this.updateRanges.push({start:t,count:e})}clearUpdateRanges(){this.updateRanges.length=0}copy(t){return this.name=t.name,this.array=new t.array.constructor(t.array),this.itemSize=t.itemSize,this.count=t.count,this.normalized=t.normalized,this.usage=t.usage,this.gpuType=t.gpuType,this}copyAt(t,e,n){t*=this.itemSize,n*=e.itemSize;for(let s=0,r=this.itemSize;s<r;s++)this.array[t+s]=e.array[n+s];return this}copyArray(t){return this.array.set(t),this}applyMatrix3(t){if(this.itemSize===2)for(let e=0,n=this.count;e<n;e++)Zr.fromBufferAttribute(this,e),Zr.applyMatrix3(t),this.setXY(e,Zr.x,Zr.y);else if(this.itemSize===3)for(let e=0,n=this.count;e<n;e++)Ne.fromBufferAttribute(this,e),Ne.applyMatrix3(t),this.setXYZ(e,Ne.x,Ne.y,Ne.z);return this}applyMatrix4(t){for(let e=0,n=this.count;e<n;e++)Ne.fromBufferAttribute(this,e),Ne.applyMatrix4(t),this.setXYZ(e,Ne.x,Ne.y,Ne.z);return this}applyNormalMatrix(t){for(let e=0,n=this.count;e<n;e++)Ne.fromBufferAttribute(this,e),Ne.applyNormalMatrix(t),this.setXYZ(e,Ne.x,Ne.y,Ne.z);return this}transformDirection(t){for(let e=0,n=this.count;e<n;e++)Ne.fromBufferAttribute(this,e),Ne.transformDirection(t),this.setXYZ(e,Ne.x,Ne.y,Ne.z);return this}set(t,e=0){return this.array.set(t,e),this}getComponent(t,e){let n=this.array[t*this.itemSize+e];return this.normalized&&(n=In(n,this.array)),n}setComponent(t,e,n){return this.normalized&&(n=_e(n,this.array)),this.array[t*this.itemSize+e]=n,this}getX(t){let e=this.array[t*this.itemSize];return this.normalized&&(e=In(e,this.array)),e}setX(t,e){return this.normalized&&(e=_e(e,this.array)),this.array[t*this.itemSize]=e,this}getY(t){let e=this.array[t*this.itemSize+1];return this.normalized&&(e=In(e,this.array)),e}setY(t,e){return this.normalized&&(e=_e(e,this.array)),this.array[t*this.itemSize+1]=e,this}getZ(t){let e=this.array[t*this.itemSize+2];return this.normalized&&(e=In(e,this.array)),e}setZ(t,e){return this.normalized&&(e=_e(e,this.array)),this.array[t*this.itemSize+2]=e,this}getW(t){let e=this.array[t*this.itemSize+3];return this.normalized&&(e=In(e,this.array)),e}setW(t,e){return this.normalized&&(e=_e(e,this.array)),this.array[t*this.itemSize+3]=e,this}setXY(t,e,n){return t*=this.itemSize,this.normalized&&(e=_e(e,this.array),n=_e(n,this.array)),this.array[t+0]=e,this.array[t+1]=n,this}setXYZ(t,e,n,s){return t*=this.itemSize,this.normalized&&(e=_e(e,this.array),n=_e(n,this.array),s=_e(s,this.array)),this.array[t+0]=e,this.array[t+1]=n,this.array[t+2]=s,this}setXYZW(t,e,n,s,r){return t*=this.itemSize,this.normalized&&(e=_e(e,this.array),n=_e(n,this.array),s=_e(s,this.array),r=_e(r,this.array)),this.array[t+0]=e,this.array[t+1]=n,this.array[t+2]=s,this.array[t+3]=r,this}onUpload(t){return this.onUploadCallback=t,this}clone(){return new this.constructor(this.array,this.itemSize).copy(this)}toJSON(){let t={itemSize:this.itemSize,type:this.array.constructor.name,array:Array.from(this.array),normalized:this.normalized};return this.name!==""&&(t.name=this.name),this.usage!==Xl&&(t.usage=this.usage),t}};var Fo=class extends we{constructor(t,e,n){super(new Uint16Array(t),e,n)}};var Io=class extends we{constructor(t,e,n){super(new Uint32Array(t),e,n)}};var Wt=class extends we{constructor(t,e,n){super(new Float32Array(t),e,n)}},Sp=0,mn=new ee,Xa=new We,fs=new B,cn=new Qn,Qs=new Qn,Ve=new B,de=class i extends vi{constructor(){super(),this.isBufferGeometry=!0,Object.defineProperty(this,"id",{value:Sp++}),this.uuid=$n(),this.name="",this.type="BufferGeometry",this.index=null,this.indirect=null,this.attributes={},this.morphAttributes={},this.morphTargetsRelative=!1,this.groups=[],this.boundingBox=null,this.boundingSphere=null,this.drawRange={start:0,count:1/0},this.userData={}}getIndex(){return this.index}setIndex(t){return Array.isArray(t)?this.index=new(of(t)?Io:Fo)(t,1):this.index=t,this}setIndirect(t){return this.indirect=t,this}getIndirect(){return this.indirect}getAttribute(t){return this.attributes[t]}setAttribute(t,e){return this.attributes[t]=e,this}deleteAttribute(t){return delete this.attributes[t],this}hasAttribute(t){return this.attributes[t]!==void 0}addGroup(t,e,n=0){this.groups.push({start:t,count:e,materialIndex:n})}clearGroups(){this.groups=[]}setDrawRange(t,e){this.drawRange.start=t,this.drawRange.count=e}applyMatrix4(t){let e=this.attributes.position;e!==void 0&&(e.applyMatrix4(t),e.needsUpdate=!0);let n=this.attributes.normal;if(n!==void 0){let r=new $t().getNormalMatrix(t);n.applyNormalMatrix(r),n.needsUpdate=!0}let s=this.attributes.tangent;return s!==void 0&&(s.transformDirection(t),s.needsUpdate=!0),this.boundingBox!==null&&this.computeBoundingBox(),this.boundingSphere!==null&&this.computeBoundingSphere(),this}applyQuaternion(t){return mn.makeRotationFromQuaternion(t),this.applyMatrix4(mn),this}rotateX(t){return mn.makeRotationX(t),this.applyMatrix4(mn),this}rotateY(t){return mn.makeRotationY(t),this.applyMatrix4(mn),this}rotateZ(t){return mn.makeRotationZ(t),this.applyMatrix4(mn),this}translate(t,e,n){return mn.makeTranslation(t,e,n),this.applyMatrix4(mn),this}scale(t,e,n){return mn.makeScale(t,e,n),this.applyMatrix4(mn),this}lookAt(t){return Xa.lookAt(t),Xa.updateMatrix(),this.applyMatrix4(Xa.matrix),this}center(){return this.computeBoundingBox(),this.boundingBox.getCenter(fs).negate(),this.translate(fs.x,fs.y,fs.z),this}setFromPoints(t){let e=this.getAttribute("position");if(e===void 0){let n=[];for(let s=0,r=t.length;s<r;s++){let o=t[s];n.push(o.x,o.y,o.z||0)}this.setAttribute("position",new Wt(n,3))}else{for(let n=0,s=e.count;n<s;n++){let r=t[n];e.setXYZ(n,r.x,r.y,r.z||0)}t.length>e.count&&console.warn("THREE.BufferGeometry: Buffer size too small for points data. Use .dispose() and create a new geometry."),e.needsUpdate=!0}return this}computeBoundingBox(){this.boundingBox===null&&(this.boundingBox=new Qn);let t=this.attributes.position,e=this.morphAttributes.position;if(t&&t.isGLBufferAttribute){console.error("THREE.BufferGeometry.computeBoundingBox(): GLBufferAttribute requires a manual bounding box.",this),this.boundingBox.set(new B(-1/0,-1/0,-1/0),new B(1/0,1/0,1/0));return}if(t!==void 0){if(this.boundingBox.setFromBufferAttribute(t),e)for(let n=0,s=e.length;n<s;n++){let r=e[n];cn.setFromBufferAttribute(r),this.morphTargetsRelative?(Ve.addVectors(this.boundingBox.min,cn.min),this.boundingBox.expandByPoint(Ve),Ve.addVectors(this.boundingBox.max,cn.max),this.boundingBox.expandByPoint(Ve)):(this.boundingBox.expandByPoint(cn.min),this.boundingBox.expandByPoint(cn.max))}}else this.boundingBox.makeEmpty();(isNaN(this.boundingBox.min.x)||isNaN(this.boundingBox.min.y)||isNaN(this.boundingBox.min.z))&&console.error('THREE.BufferGeometry.computeBoundingBox(): Computed min/max have NaN values. The "position" attribute is likely to have NaN values.',this)}computeBoundingSphere(){this.boundingSphere===null&&(this.boundingSphere=new ti);let t=this.attributes.position,e=this.morphAttributes.position;if(t&&t.isGLBufferAttribute){console.error("THREE.BufferGeometry.computeBoundingSphere(): GLBufferAttribute requires a manual bounding sphere.",this),this.boundingSphere.set(new B,1/0);return}if(t){let n=this.boundingSphere.center;if(cn.setFromBufferAttribute(t),e)for(let r=0,o=e.length;r<o;r++){let a=e[r];Qs.setFromBufferAttribute(a),this.morphTargetsRelative?(Ve.addVectors(cn.min,Qs.min),cn.expandByPoint(Ve),Ve.addVectors(cn.max,Qs.max),cn.expandByPoint(Ve)):(cn.expandByPoint(Qs.min),cn.expandByPoint(Qs.max))}cn.getCenter(n);let s=0;for(let r=0,o=t.count;r<o;r++)Ve.fromBufferAttribute(t,r),s=Math.max(s,n.distanceToSquared(Ve));if(e)for(let r=0,o=e.length;r<o;r++){let a=e[r],l=this.morphTargetsRelative;for(let h=0,u=a.count;h<u;h++)Ve.fromBufferAttribute(a,h),l&&(fs.fromBufferAttribute(t,h),Ve.add(fs)),s=Math.max(s,n.distanceToSquared(Ve))}this.boundingSphere.radius=Math.sqrt(s),isNaN(this.boundingSphere.radius)&&console.error('THREE.BufferGeometry.computeBoundingSphere(): Computed radius is NaN. The "position" attribute is likely to have NaN values.',this)}}computeTangents(){let t=this.index,e=this.attributes;if(t===null||e.position===void 0||e.normal===void 0||e.uv===void 0){console.error("THREE.BufferGeometry: .computeTangents() failed. Missing required attributes (index, position, normal or uv)");return}let n=e.position,s=e.normal,r=e.uv;this.hasAttribute("tangent")===!1&&this.setAttribute("tangent",new we(new Float32Array(4*n.count),4));let o=this.getAttribute("tangent"),a=[],l=[];for(let w=0;w<n.count;w++)a[w]=new B,l[w]=new B;let h=new B,u=new B,m=new B,x=new lt,f=new lt,v=new lt,d=new B,y=new B;function c(w,M,E){h.fromBufferAttribute(n,w),u.fromBufferAttribute(n,M),m.fromBufferAttribute(n,E),x.fromBufferAttribute(r,w),f.fromBufferAttribute(r,M),v.fromBufferAttribute(r,E),u.sub(h),m.sub(h),f.sub(x),v.sub(x);let F=1/(f.x*v.y-v.x*f.y);isFinite(F)&&(d.copy(u).multiplyScalar(v.y).addScaledVector(m,-f.y).multiplyScalar(F),y.copy(m).multiplyScalar(f.x).addScaledVector(u,-v.x).multiplyScalar(F),a[w].add(d),a[M].add(d),a[E].add(d),l[w].add(y),l[M].add(y),l[E].add(y))}let _=this.groups;_.length===0&&(_=[{start:0,count:t.count}]);for(let w=0,M=_.length;w<M;++w){let E=_[w],F=E.start,U=E.count;for(let R=F,N=F+U;R<N;R+=3)c(t.getX(R+0),t.getX(R+1),t.getX(R+2))}let p=new B,g=new B,T=new B,S=new B;function b(w){T.fromBufferAttribute(s,w),S.copy(T);let M=a[w];p.copy(M),p.sub(T.multiplyScalar(T.dot(M))).normalize(),g.crossVectors(S,M);let F=g.dot(l[w])<0?-1:1;o.setXYZW(w,p.x,p.y,p.z,F)}for(let w=0,M=_.length;w<M;++w){let E=_[w],F=E.start,U=E.count;for(let R=F,N=F+U;R<N;R+=3)b(t.getX(R+0)),b(t.getX(R+1)),b(t.getX(R+2))}}computeVertexNormals(){let t=this.index,e=this.getAttribute("position");if(e!==void 0){let n=this.getAttribute("normal");if(n===void 0)n=new we(new Float32Array(e.count*3),3),this.setAttribute("normal",n);else for(let x=0,f=n.count;x<f;x++)n.setXYZ(x,0,0,0);let s=new B,r=new B,o=new B,a=new B,l=new B,h=new B,u=new B,m=new B;if(t)for(let x=0,f=t.count;x<f;x+=3){let v=t.getX(x+0),d=t.getX(x+1),y=t.getX(x+2);s.fromBufferAttribute(e,v),r.fromBufferAttribute(e,d),o.fromBufferAttribute(e,y),u.subVectors(o,r),m.subVectors(s,r),u.cross(m),a.fromBufferAttribute(n,v),l.fromBufferAttribute(n,d),h.fromBufferAttribute(n,y),a.add(u),l.add(u),h.add(u),n.setXYZ(v,a.x,a.y,a.z),n.setXYZ(d,l.x,l.y,l.z),n.setXYZ(y,h.x,h.y,h.z)}else for(let x=0,f=e.count;x<f;x+=3)s.fromBufferAttribute(e,x+0),r.fromBufferAttribute(e,x+1),o.fromBufferAttribute(e,x+2),u.subVectors(o,r),m.subVectors(s,r),u.cross(m),n.setXYZ(x+0,u.x,u.y,u.z),n.setXYZ(x+1,u.x,u.y,u.z),n.setXYZ(x+2,u.x,u.y,u.z);this.normalizeNormals(),n.needsUpdate=!0}}normalizeNormals(){let t=this.attributes.normal;for(let e=0,n=t.count;e<n;e++)Ve.fromBufferAttribute(t,e),Ve.normalize(),t.setXYZ(e,Ve.x,Ve.y,Ve.z)}toNonIndexed(){function t(a,l){let h=a.array,u=a.itemSize,m=a.normalized,x=new h.constructor(l.length*u),f=0,v=0;for(let d=0,y=l.length;d<y;d++){a.isInterleavedBufferAttribute?f=l[d]*a.data.stride+a.offset:f=l[d]*u;for(let c=0;c<u;c++)x[v++]=h[f++]}return new we(x,u,m)}if(this.index===null)return console.warn("THREE.BufferGeometry.toNonIndexed(): BufferGeometry is already non-indexed."),this;let e=new i,n=this.index.array,s=this.attributes;for(let a in s){let l=s[a],h=t(l,n);e.setAttribute(a,h)}let r=this.morphAttributes;for(let a in r){let l=[],h=r[a];for(let u=0,m=h.length;u<m;u++){let x=h[u],f=t(x,n);l.push(f)}e.morphAttributes[a]=l}e.morphTargetsRelative=this.morphTargetsRelative;let o=this.groups;for(let a=0,l=o.length;a<l;a++){let h=o[a];e.addGroup(h.start,h.count,h.materialIndex)}return e}toJSON(){let t={metadata:{version:4.6,type:"BufferGeometry",generator:"BufferGeometry.toJSON"}};if(t.uuid=this.uuid,t.type=this.type,this.name!==""&&(t.name=this.name),Object.keys(this.userData).length>0&&(t.userData=this.userData),this.parameters!==void 0){let l=this.parameters;for(let h in l)l[h]!==void 0&&(t[h]=l[h]);return t}t.data={attributes:{}};let e=this.index;e!==null&&(t.data.index={type:e.array.constructor.name,array:Array.prototype.slice.call(e.array)});let n=this.attributes;for(let l in n){let h=n[l];t.data.attributes[l]=h.toJSON(t.data)}let s={},r=!1;for(let l in this.morphAttributes){let h=this.morphAttributes[l],u=[];for(let m=0,x=h.length;m<x;m++){let f=h[m];u.push(f.toJSON(t.data))}u.length>0&&(s[l]=u,r=!0)}r&&(t.data.morphAttributes=s,t.data.morphTargetsRelative=this.morphTargetsRelative);let o=this.groups;o.length>0&&(t.data.groups=JSON.parse(JSON.stringify(o)));let a=this.boundingSphere;return a!==null&&(t.data.boundingSphere={center:a.center.toArray(),radius:a.radius}),t}clone(){return new this.constructor().copy(this)}copy(t){this.index=null,this.attributes={},this.morphAttributes={},this.groups=[],this.boundingBox=null,this.boundingSphere=null;let e={};this.name=t.name;let n=t.index;n!==null&&this.setIndex(n.clone(e));let s=t.attributes;for(let h in s){let u=s[h];this.setAttribute(h,u.clone(e))}let r=t.morphAttributes;for(let h in r){let u=[],m=r[h];for(let x=0,f=m.length;x<f;x++)u.push(m[x].clone(e));this.morphAttributes[h]=u}this.morphTargetsRelative=t.morphTargetsRelative;let o=t.groups;for(let h=0,u=o.length;h<u;h++){let m=o[h];this.addGroup(m.start,m.count,m.materialIndex)}let a=t.boundingBox;a!==null&&(this.boundingBox=a.clone());let l=t.boundingSphere;return l!==null&&(this.boundingSphere=l.clone()),this.drawRange.start=t.drawRange.start,this.drawRange.count=t.drawRange.count,this.userData=t.userData,this}dispose(){this.dispatchEvent({type:"dispose"})}},eu=new ee,Pi=new dr,$r=new ti,nu=new B,Jr=new B,Kr=new B,jr=new B,qa=new B,Qr=new B,iu=new B,to=new B,Z=class extends We{constructor(t=new de,e=new oe){super(),this.isMesh=!0,this.type="Mesh",this.geometry=t,this.material=e,this.updateMorphTargets()}copy(t,e){return super.copy(t,e),t.morphTargetInfluences!==void 0&&(this.morphTargetInfluences=t.morphTargetInfluences.slice()),t.morphTargetDictionary!==void 0&&(this.morphTargetDictionary=Object.assign({},t.morphTargetDictionary)),this.material=Array.isArray(t.material)?t.material.slice():t.material,this.geometry=t.geometry,this}updateMorphTargets(){let e=this.geometry.morphAttributes,n=Object.keys(e);if(n.length>0){let s=e[n[0]];if(s!==void 0){this.morphTargetInfluences=[],this.morphTargetDictionary={};for(let r=0,o=s.length;r<o;r++){let a=s[r].name||String(r);this.morphTargetInfluences.push(0),this.morphTargetDictionary[a]=r}}}}getVertexPosition(t,e){let n=this.geometry,s=n.attributes.position,r=n.morphAttributes.position,o=n.morphTargetsRelative;e.fromBufferAttribute(s,t);let a=this.morphTargetInfluences;if(r&&a){Qr.set(0,0,0);for(let l=0,h=r.length;l<h;l++){let u=a[l],m=r[l];u!==0&&(qa.fromBufferAttribute(m,t),o?Qr.addScaledVector(qa,u):Qr.addScaledVector(qa.sub(e),u))}e.add(Qr)}return e}raycast(t,e){let n=this.geometry,s=this.material,r=this.matrixWorld;s!==void 0&&(n.boundingSphere===null&&n.computeBoundingSphere(),$r.copy(n.boundingSphere),$r.applyMatrix4(r),Pi.copy(t.ray).recast(t.near),!($r.containsPoint(Pi.origin)===!1&&(Pi.intersectSphere($r,nu)===null||Pi.origin.distanceToSquared(nu)>(t.far-t.near)**2))&&(eu.copy(r).invert(),Pi.copy(t.ray).applyMatrix4(eu),!(n.boundingBox!==null&&Pi.intersectsBox(n.boundingBox)===!1)&&this._computeIntersections(t,e,Pi)))}_computeIntersections(t,e,n){let s,r=this.geometry,o=this.material,a=r.index,l=r.attributes.position,h=r.attributes.uv,u=r.attributes.uv1,m=r.attributes.normal,x=r.groups,f=r.drawRange;if(a!==null)if(Array.isArray(o))for(let v=0,d=x.length;v<d;v++){let y=x[v],c=o[y.materialIndex],_=Math.max(y.start,f.start),p=Math.min(a.count,Math.min(y.start+y.count,f.start+f.count));for(let g=_,T=p;g<T;g+=3){let S=a.getX(g),b=a.getX(g+1),w=a.getX(g+2);s=eo(this,c,t,n,h,u,m,S,b,w),s&&(s.faceIndex=Math.floor(g/3),s.face.materialIndex=y.materialIndex,e.push(s))}}else{let v=Math.max(0,f.start),d=Math.min(a.count,f.start+f.count);for(let y=v,c=d;y<c;y+=3){let _=a.getX(y),p=a.getX(y+1),g=a.getX(y+2);s=eo(this,o,t,n,h,u,m,_,p,g),s&&(s.faceIndex=Math.floor(y/3),e.push(s))}}else if(l!==void 0)if(Array.isArray(o))for(let v=0,d=x.length;v<d;v++){let y=x[v],c=o[y.materialIndex],_=Math.max(y.start,f.start),p=Math.min(l.count,Math.min(y.start+y.count,f.start+f.count));for(let g=_,T=p;g<T;g+=3){let S=g,b=g+1,w=g+2;s=eo(this,c,t,n,h,u,m,S,b,w),s&&(s.faceIndex=Math.floor(g/3),s.face.materialIndex=y.materialIndex,e.push(s))}}else{let v=Math.max(0,f.start),d=Math.min(l.count,f.start+f.count);for(let y=v,c=d;y<c;y+=3){let _=y,p=y+1,g=y+2;s=eo(this,o,t,n,h,u,m,_,p,g),s&&(s.faceIndex=Math.floor(y/3),e.push(s))}}}};function Tp(i,t,e,n,s,r,o,a){let l;if(t.side===en?l=n.intersectTriangle(o,r,s,!0,a):l=n.intersectTriangle(s,r,o,t.side===Dn,a),l===null)return null;to.copy(a),to.applyMatrix4(i.matrixWorld);let h=e.ray.origin.distanceTo(to);return h<e.near||h>e.far?null:{distance:h,point:to.clone(),object:i}}function eo(i,t,e,n,s,r,o,a,l,h){i.getVertexPosition(a,Jr),i.getVertexPosition(l,Kr),i.getVertexPosition(h,jr);let u=Tp(i,t,e,n,Jr,Kr,jr,iu);if(u){let m=new B;gi.getBarycoord(iu,Jr,Kr,jr,m),s&&(u.uv=gi.getInterpolatedAttribute(s,a,l,h,m,new lt)),r&&(u.uv1=gi.getInterpolatedAttribute(r,a,l,h,m,new lt)),o&&(u.normal=gi.getInterpolatedAttribute(o,a,l,h,m,new B),u.normal.dot(n.direction)>0&&u.normal.multiplyScalar(-1));let x={a,b:l,c:h,normal:new B,materialIndex:0};gi.getNormal(Jr,Kr,jr,x.normal),u.face=x,u.barycoord=m}return u}var kt=class i extends de{constructor(t=1,e=1,n=1,s=1,r=1,o=1){super(),this.type="BoxGeometry",this.parameters={width:t,height:e,depth:n,widthSegments:s,heightSegments:r,depthSegments:o};let a=this;s=Math.floor(s),r=Math.floor(r),o=Math.floor(o);let l=[],h=[],u=[],m=[],x=0,f=0;v("z","y","x",-1,-1,n,e,t,o,r,0),v("z","y","x",1,-1,n,e,-t,o,r,1),v("x","z","y",1,1,t,n,e,s,o,2),v("x","z","y",1,-1,t,n,-e,s,o,3),v("x","y","z",1,-1,t,e,n,s,r,4),v("x","y","z",-1,-1,t,e,-n,s,r,5),this.setIndex(l),this.setAttribute("position",new Wt(h,3)),this.setAttribute("normal",new Wt(u,3)),this.setAttribute("uv",new Wt(m,2));function v(d,y,c,_,p,g,T,S,b,w,M){let E=g/b,F=T/w,U=g/2,R=T/2,N=S/2,V=b+1,z=w+1,O=0,D=0,X=new B;for(let $=0;$<z;$++){let tt=$*F-R;for(let ut=0;ut<V;ut++){let Ct=ut*E-U;X[d]=Ct*_,X[y]=tt*p,X[c]=N,h.push(X.x,X.y,X.z),X[d]=0,X[y]=0,X[c]=S>0?1:-1,u.push(X.x,X.y,X.z),m.push(ut/b),m.push(1-$/w),O+=1}}for(let $=0;$<w;$++)for(let tt=0;tt<b;tt++){let ut=x+tt+V*$,Ct=x+tt+V*($+1),J=x+(tt+1)+V*($+1),st=x+(tt+1)+V*$;l.push(ut,Ct,st),l.push(Ct,J,st),D+=6}a.addGroup(f,D,M),f+=D,x+=O}}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.width,t.height,t.depth,t.widthSegments,t.heightSegments,t.depthSegments)}};function Ps(i){let t={};for(let e in i){t[e]={};for(let n in i[e]){let s=i[e][n];s&&(s.isColor||s.isMatrix3||s.isMatrix4||s.isVector2||s.isVector3||s.isVector4||s.isTexture||s.isQuaternion)?s.isRenderTargetTexture?(console.warn("UniformsUtils: Textures of render targets cannot be cloned via cloneUniforms() or mergeUniforms()."),t[e][n]=null):t[e][n]=s.clone():Array.isArray(s)?t[e][n]=s.slice():t[e][n]=s}}return t}function je(i){let t={};for(let e=0;e<i.length;e++){let n=Ps(i[e]);for(let s in n)t[s]=n[s]}return t}function wp(i){let t=[];for(let e=0;e<i.length;e++)t.push(i[e].clone());return t}function lf(i){let t=i.getRenderTarget();return t===null?i.outputColorSpace:t.isXRRenderTarget===!0?t.texture.colorSpace:ie.workingColorSpace}var bp={clone:Ps,merge:je},Ap=`void main() {
	gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );
}`,Rp=`void main() {
	gl_FragColor = vec4( 1.0, 0.0, 0.0, 1.0 );
}`,xn=class extends Un{static get type(){return"ShaderMaterial"}constructor(t){super(),this.isShaderMaterial=!0,this.defines={},this.uniforms={},this.uniformsGroups=[],this.vertexShader=Ap,this.fragmentShader=Rp,this.linewidth=1,this.wireframe=!1,this.wireframeLinewidth=1,this.fog=!1,this.lights=!1,this.clipping=!1,this.forceSinglePass=!0,this.extensions={clipCullDistance:!1,multiDraw:!1},this.defaultAttributeValues={color:[1,1,1],uv:[0,0],uv1:[0,0]},this.index0AttributeName=void 0,this.uniformsNeedUpdate=!1,this.glslVersion=null,t!==void 0&&this.setValues(t)}copy(t){return super.copy(t),this.fragmentShader=t.fragmentShader,this.vertexShader=t.vertexShader,this.uniforms=Ps(t.uniforms),this.uniformsGroups=wp(t.uniformsGroups),this.defines=Object.assign({},t.defines),this.wireframe=t.wireframe,this.wireframeLinewidth=t.wireframeLinewidth,this.fog=t.fog,this.lights=t.lights,this.clipping=t.clipping,this.extensions=Object.assign({},t.extensions),this.glslVersion=t.glslVersion,this}toJSON(t){let e=super.toJSON(t);e.glslVersion=this.glslVersion,e.uniforms={};for(let s in this.uniforms){let o=this.uniforms[s].value;o&&o.isTexture?e.uniforms[s]={type:"t",value:o.toJSON(t).uuid}:o&&o.isColor?e.uniforms[s]={type:"c",value:o.getHex()}:o&&o.isVector2?e.uniforms[s]={type:"v2",value:o.toArray()}:o&&o.isVector3?e.uniforms[s]={type:"v3",value:o.toArray()}:o&&o.isVector4?e.uniforms[s]={type:"v4",value:o.toArray()}:o&&o.isMatrix3?e.uniforms[s]={type:"m3",value:o.toArray()}:o&&o.isMatrix4?e.uniforms[s]={type:"m4",value:o.toArray()}:e.uniforms[s]={value:o}}Object.keys(this.defines).length>0&&(e.defines=this.defines),e.vertexShader=this.vertexShader,e.fragmentShader=this.fragmentShader,e.lights=this.lights,e.clipping=this.clipping;let n={};for(let s in this.extensions)this.extensions[s]===!0&&(n[s]=!0);return Object.keys(n).length>0&&(e.extensions=n),e}},Po=class extends We{constructor(){super(),this.isCamera=!0,this.type="Camera",this.matrixWorldInverse=new ee,this.projectionMatrix=new ee,this.projectionMatrixInverse=new ee,this.coordinateSystem=Zn}copy(t,e){return super.copy(t,e),this.matrixWorldInverse.copy(t.matrixWorldInverse),this.projectionMatrix.copy(t.projectionMatrix),this.projectionMatrixInverse.copy(t.projectionMatrixInverse),this.coordinateSystem=t.coordinateSystem,this}getWorldDirection(t){return super.getWorldDirection(t).negate()}updateMatrixWorld(t){super.updateMatrixWorld(t),this.matrixWorldInverse.copy(this.matrixWorld).invert()}updateWorldMatrix(t,e){super.updateWorldMatrix(t,e),this.matrixWorldInverse.copy(this.matrixWorld).invert()}clone(){return new this.constructor().copy(this)}},pi=new B,su=new lt,ru=new lt,Je=class extends Po{constructor(t=50,e=1,n=.1,s=2e3){super(),this.isPerspectiveCamera=!0,this.type="PerspectiveCamera",this.fov=t,this.zoom=1,this.near=n,this.far=s,this.focus=10,this.aspect=e,this.view=null,this.filmGauge=35,this.filmOffset=0,this.updateProjectionMatrix()}copy(t,e){return super.copy(t,e),this.fov=t.fov,this.zoom=t.zoom,this.near=t.near,this.far=t.far,this.focus=t.focus,this.aspect=t.aspect,this.view=t.view===null?null:Object.assign({},t.view),this.filmGauge=t.filmGauge,this.filmOffset=t.filmOffset,this}setFocalLength(t){let e=.5*this.getFilmHeight()/t;this.fov=ql*2*Math.atan(e),this.updateProjectionMatrix()}getFocalLength(){let t=Math.tan(ba*.5*this.fov);return .5*this.getFilmHeight()/t}getEffectiveFOV(){return ql*2*Math.atan(Math.tan(ba*.5*this.fov)/this.zoom)}getFilmWidth(){return this.filmGauge*Math.min(this.aspect,1)}getFilmHeight(){return this.filmGauge/Math.max(this.aspect,1)}getViewBounds(t,e,n){pi.set(-1,-1,.5).applyMatrix4(this.projectionMatrixInverse),e.set(pi.x,pi.y).multiplyScalar(-t/pi.z),pi.set(1,1,.5).applyMatrix4(this.projectionMatrixInverse),n.set(pi.x,pi.y).multiplyScalar(-t/pi.z)}getViewSize(t,e){return this.getViewBounds(t,su,ru),e.subVectors(ru,su)}setViewOffset(t,e,n,s,r,o){this.aspect=t/e,this.view===null&&(this.view={enabled:!0,fullWidth:1,fullHeight:1,offsetX:0,offsetY:0,width:1,height:1}),this.view.enabled=!0,this.view.fullWidth=t,this.view.fullHeight=e,this.view.offsetX=n,this.view.offsetY=s,this.view.width=r,this.view.height=o,this.updateProjectionMatrix()}clearViewOffset(){this.view!==null&&(this.view.enabled=!1),this.updateProjectionMatrix()}updateProjectionMatrix(){let t=this.near,e=t*Math.tan(ba*.5*this.fov)/this.zoom,n=2*e,s=this.aspect*n,r=-.5*s,o=this.view;if(this.view!==null&&this.view.enabled){let l=o.fullWidth,h=o.fullHeight;r+=o.offsetX*s/l,e-=o.offsetY*n/h,s*=o.width/l,n*=o.height/h}let a=this.filmOffset;a!==0&&(r+=t*a/this.getFilmWidth()),this.projectionMatrix.makePerspective(r,r+s,e,e-n,t,this.far,this.coordinateSystem),this.projectionMatrixInverse.copy(this.projectionMatrix).invert()}toJSON(t){let e=super.toJSON(t);return e.object.fov=this.fov,e.object.zoom=this.zoom,e.object.near=this.near,e.object.far=this.far,e.object.focus=this.focus,e.object.aspect=this.aspect,this.view!==null&&(e.object.view=Object.assign({},this.view)),e.object.filmGauge=this.filmGauge,e.object.filmOffset=this.filmOffset,e}},ds=-90,ps=1,Jl=class extends We{constructor(t,e,n){super(),this.type="CubeCamera",this.renderTarget=n,this.coordinateSystem=null,this.activeMipmapLevel=0;let s=new Je(ds,ps,t,e);s.layers=this.layers,this.add(s);let r=new Je(ds,ps,t,e);r.layers=this.layers,this.add(r);let o=new Je(ds,ps,t,e);o.layers=this.layers,this.add(o);let a=new Je(ds,ps,t,e);a.layers=this.layers,this.add(a);let l=new Je(ds,ps,t,e);l.layers=this.layers,this.add(l);let h=new Je(ds,ps,t,e);h.layers=this.layers,this.add(h)}updateCoordinateSystem(){let t=this.coordinateSystem,e=this.children.concat(),[n,s,r,o,a,l]=e;for(let h of e)this.remove(h);if(t===Zn)n.up.set(0,1,0),n.lookAt(1,0,0),s.up.set(0,1,0),s.lookAt(-1,0,0),r.up.set(0,0,-1),r.lookAt(0,1,0),o.up.set(0,0,1),o.lookAt(0,-1,0),a.up.set(0,1,0),a.lookAt(0,0,1),l.up.set(0,1,0),l.lookAt(0,0,-1);else if(t===wo)n.up.set(0,-1,0),n.lookAt(-1,0,0),s.up.set(0,-1,0),s.lookAt(1,0,0),r.up.set(0,0,1),r.lookAt(0,1,0),o.up.set(0,0,-1),o.lookAt(0,-1,0),a.up.set(0,-1,0),a.lookAt(0,0,1),l.up.set(0,-1,0),l.lookAt(0,0,-1);else throw new Error("THREE.CubeCamera.updateCoordinateSystem(): Invalid coordinate system: "+t);for(let h of e)this.add(h),h.updateMatrixWorld()}update(t,e){this.parent===null&&this.updateMatrixWorld();let{renderTarget:n,activeMipmapLevel:s}=this;this.coordinateSystem!==t.coordinateSystem&&(this.coordinateSystem=t.coordinateSystem,this.updateCoordinateSystem());let[r,o,a,l,h,u]=this.children,m=t.getRenderTarget(),x=t.getActiveCubeFace(),f=t.getActiveMipmapLevel(),v=t.xr.enabled;t.xr.enabled=!1;let d=n.texture.generateMipmaps;n.texture.generateMipmaps=!1,t.setRenderTarget(n,0,s),t.render(e,r),t.setRenderTarget(n,1,s),t.render(e,o),t.setRenderTarget(n,2,s),t.render(e,a),t.setRenderTarget(n,3,s),t.render(e,l),t.setRenderTarget(n,4,s),t.render(e,h),n.texture.generateMipmaps=d,t.setRenderTarget(n,5,s),t.render(e,u),t.setRenderTarget(m,x,f),t.xr.enabled=v,n.texture.needsPMREMUpdate=!0}},Lo=class extends nn{constructor(t,e,n,s,r,o,a,l,h,u){t=t!==void 0?t:[],e=e!==void 0?e:Rs,super(t,e,n,s,r,o,a,l,h,u),this.isCubeTexture=!0,this.flipY=!1}get images(){return this.image}set images(t){this.image=t}},Kl=class extends jn{constructor(t=1,e={}){super(t,t,e),this.isWebGLCubeRenderTarget=!0;let n={width:t,height:t,depth:1},s=[n,n,n,n,n,n];this.texture=new Lo(s,e.mapping,e.wrapS,e.wrapT,e.magFilter,e.minFilter,e.format,e.type,e.anisotropy,e.colorSpace),this.texture.isRenderTargetTexture=!0,this.texture.generateMipmaps=e.generateMipmaps!==void 0?e.generateMipmaps:!1,this.texture.minFilter=e.minFilter!==void 0?e.minFilter:Pn}fromEquirectangularTexture(t,e){this.texture.type=e.type,this.texture.colorSpace=e.colorSpace,this.texture.generateMipmaps=e.generateMipmaps,this.texture.minFilter=e.minFilter,this.texture.magFilter=e.magFilter;let n={uniforms:{tEquirect:{value:null}},vertexShader:`

				varying vec3 vWorldDirection;

				vec3 transformDirection( in vec3 dir, in mat4 matrix ) {

					return normalize( ( matrix * vec4( dir, 0.0 ) ).xyz );

				}

				void main() {

					vWorldDirection = transformDirection( position, modelMatrix );

					#include <begin_vertex>
					#include <project_vertex>

				}
			`,fragmentShader:`

				uniform sampler2D tEquirect;

				varying vec3 vWorldDirection;

				#include <common>

				void main() {

					vec3 direction = normalize( vWorldDirection );

					vec2 sampleUV = equirectUv( direction );

					gl_FragColor = texture2D( tEquirect, sampleUV );

				}
			`},s=new kt(5,5,5),r=new xn({name:"CubemapFromEquirect",uniforms:Ps(n.uniforms),vertexShader:n.vertexShader,fragmentShader:n.fragmentShader,side:en,blending:xi});r.uniforms.tEquirect.value=e;let o=new Z(s,r),a=e.minFilter;return e.minFilter===Hi&&(e.minFilter=Pn),new Jl(1,10,this).update(t,o),e.minFilter=a,o.geometry.dispose(),o.material.dispose(),this}clear(t,e,n,s){let r=t.getRenderTarget();for(let o=0;o<6;o++)t.setRenderTarget(this,o),t.clear(e,n,s);t.setRenderTarget(r)}},Ya=new B,Cp=new B,Fp=new $t,Yn=class{constructor(t=new B(1,0,0),e=0){this.isPlane=!0,this.normal=t,this.constant=e}set(t,e){return this.normal.copy(t),this.constant=e,this}setComponents(t,e,n,s){return this.normal.set(t,e,n),this.constant=s,this}setFromNormalAndCoplanarPoint(t,e){return this.normal.copy(t),this.constant=-e.dot(this.normal),this}setFromCoplanarPoints(t,e,n){let s=Ya.subVectors(n,e).cross(Cp.subVectors(t,e)).normalize();return this.setFromNormalAndCoplanarPoint(s,t),this}copy(t){return this.normal.copy(t.normal),this.constant=t.constant,this}normalize(){let t=1/this.normal.length();return this.normal.multiplyScalar(t),this.constant*=t,this}negate(){return this.constant*=-1,this.normal.negate(),this}distanceToPoint(t){return this.normal.dot(t)+this.constant}distanceToSphere(t){return this.distanceToPoint(t.center)-t.radius}projectPoint(t,e){return e.copy(t).addScaledVector(this.normal,-this.distanceToPoint(t))}intersectLine(t,e){let n=t.delta(Ya),s=this.normal.dot(n);if(s===0)return this.distanceToPoint(t.start)===0?e.copy(t.start):null;let r=-(t.start.dot(this.normal)+this.constant)/s;return r<0||r>1?null:e.copy(t.start).addScaledVector(n,r)}intersectsLine(t){let e=this.distanceToPoint(t.start),n=this.distanceToPoint(t.end);return e<0&&n>0||n<0&&e>0}intersectsBox(t){return t.intersectsPlane(this)}intersectsSphere(t){return t.intersectsPlane(this)}coplanarPoint(t){return t.copy(this.normal).multiplyScalar(-this.constant)}applyMatrix4(t,e){let n=e||Fp.getNormalMatrix(t),s=this.coplanarPoint(Ya).applyMatrix4(t),r=this.normal.applyMatrix3(n).normalize();return this.constant=-s.dot(r),this}translate(t){return this.constant-=t.dot(this.normal),this}equals(t){return t.normal.equals(this.normal)&&t.constant===this.constant}clone(){return new this.constructor().copy(this)}},Li=new ti,no=new B,pr=class{constructor(t=new Yn,e=new Yn,n=new Yn,s=new Yn,r=new Yn,o=new Yn){this.planes=[t,e,n,s,r,o]}set(t,e,n,s,r,o){let a=this.planes;return a[0].copy(t),a[1].copy(e),a[2].copy(n),a[3].copy(s),a[4].copy(r),a[5].copy(o),this}copy(t){let e=this.planes;for(let n=0;n<6;n++)e[n].copy(t.planes[n]);return this}setFromProjectionMatrix(t,e=Zn){let n=this.planes,s=t.elements,r=s[0],o=s[1],a=s[2],l=s[3],h=s[4],u=s[5],m=s[6],x=s[7],f=s[8],v=s[9],d=s[10],y=s[11],c=s[12],_=s[13],p=s[14],g=s[15];if(n[0].setComponents(l-r,x-h,y-f,g-c).normalize(),n[1].setComponents(l+r,x+h,y+f,g+c).normalize(),n[2].setComponents(l+o,x+u,y+v,g+_).normalize(),n[3].setComponents(l-o,x-u,y-v,g-_).normalize(),n[4].setComponents(l-a,x-m,y-d,g-p).normalize(),e===Zn)n[5].setComponents(l+a,x+m,y+d,g+p).normalize();else if(e===wo)n[5].setComponents(a,m,d,p).normalize();else throw new Error("THREE.Frustum.setFromProjectionMatrix(): Invalid coordinate system: "+e);return this}intersectsObject(t){if(t.boundingSphere!==void 0)t.boundingSphere===null&&t.computeBoundingSphere(),Li.copy(t.boundingSphere).applyMatrix4(t.matrixWorld);else{let e=t.geometry;e.boundingSphere===null&&e.computeBoundingSphere(),Li.copy(e.boundingSphere).applyMatrix4(t.matrixWorld)}return this.intersectsSphere(Li)}intersectsSprite(t){return Li.center.set(0,0,0),Li.radius=.7071067811865476,Li.applyMatrix4(t.matrixWorld),this.intersectsSphere(Li)}intersectsSphere(t){let e=this.planes,n=t.center,s=-t.radius;for(let r=0;r<6;r++)if(e[r].distanceToPoint(n)<s)return!1;return!0}intersectsBox(t){let e=this.planes;for(let n=0;n<6;n++){let s=e[n];if(no.x=s.normal.x>0?t.max.x:t.min.x,no.y=s.normal.y>0?t.max.y:t.min.y,no.z=s.normal.z>0?t.max.z:t.min.z,s.distanceToPoint(no)<0)return!1}return!0}containsPoint(t){let e=this.planes;for(let n=0;n<6;n++)if(e[n].distanceToPoint(t)<0)return!1;return!0}clone(){return new this.constructor().copy(this)}};function cf(){let i=null,t=!1,e=null,n=null;function s(r,o){e(r,o),n=i.requestAnimationFrame(s)}return{start:function(){t!==!0&&e!==null&&(n=i.requestAnimationFrame(s),t=!0)},stop:function(){i.cancelAnimationFrame(n),t=!1},setAnimationLoop:function(r){e=r},setContext:function(r){i=r}}}function Ip(i){let t=new WeakMap;function e(a,l){let h=a.array,u=a.usage,m=h.byteLength,x=i.createBuffer();i.bindBuffer(l,x),i.bufferData(l,h,u),a.onUploadCallback();let f;if(h instanceof Float32Array)f=i.FLOAT;else if(h instanceof Uint16Array)a.isFloat16BufferAttribute?f=i.HALF_FLOAT:f=i.UNSIGNED_SHORT;else if(h instanceof Int16Array)f=i.SHORT;else if(h instanceof Uint32Array)f=i.UNSIGNED_INT;else if(h instanceof Int32Array)f=i.INT;else if(h instanceof Int8Array)f=i.BYTE;else if(h instanceof Uint8Array)f=i.UNSIGNED_BYTE;else if(h instanceof Uint8ClampedArray)f=i.UNSIGNED_BYTE;else throw new Error("THREE.WebGLAttributes: Unsupported buffer data format: "+h);return{buffer:x,type:f,bytesPerElement:h.BYTES_PER_ELEMENT,version:a.version,size:m}}function n(a,l,h){let u=l.array,m=l.updateRanges;if(i.bindBuffer(h,a),m.length===0)i.bufferSubData(h,0,u);else{m.sort((f,v)=>f.start-v.start);let x=0;for(let f=1;f<m.length;f++){let v=m[x],d=m[f];d.start<=v.start+v.count+1?v.count=Math.max(v.count,d.start+d.count-v.start):(++x,m[x]=d)}m.length=x+1;for(let f=0,v=m.length;f<v;f++){let d=m[f];i.bufferSubData(h,d.start*u.BYTES_PER_ELEMENT,u,d.start,d.count)}l.clearUpdateRanges()}l.onUploadCallback()}function s(a){return a.isInterleavedBufferAttribute&&(a=a.data),t.get(a)}function r(a){a.isInterleavedBufferAttribute&&(a=a.data);let l=t.get(a);l&&(i.deleteBuffer(l.buffer),t.delete(a))}function o(a,l){if(a.isInterleavedBufferAttribute&&(a=a.data),a.isGLBufferAttribute){let u=t.get(a);(!u||u.version<a.version)&&t.set(a,{buffer:a.buffer,type:a.type,bytesPerElement:a.elementSize,version:a.version});return}let h=t.get(a);if(h===void 0)t.set(a,e(a,l));else if(h.version<a.version){if(h.size!==a.array.byteLength)throw new Error("THREE.WebGLAttributes: The size of the buffer attribute's array buffer does not match the original size. Resizing buffer attributes is not supported.");n(h.buffer,a,l),h.version=a.version}}return{get:s,remove:r,update:o}}var Qe=class i extends de{constructor(t=1,e=1,n=1,s=1){super(),this.type="PlaneGeometry",this.parameters={width:t,height:e,widthSegments:n,heightSegments:s};let r=t/2,o=e/2,a=Math.floor(n),l=Math.floor(s),h=a+1,u=l+1,m=t/a,x=e/l,f=[],v=[],d=[],y=[];for(let c=0;c<u;c++){let _=c*x-o;for(let p=0;p<h;p++){let g=p*m-r;v.push(g,-_,0),d.push(0,0,1),y.push(p/a),y.push(1-c/l)}}for(let c=0;c<l;c++)for(let _=0;_<a;_++){let p=_+h*c,g=_+h*(c+1),T=_+1+h*(c+1),S=_+1+h*c;f.push(p,g,S),f.push(g,T,S)}this.setIndex(f),this.setAttribute("position",new Wt(v,3)),this.setAttribute("normal",new Wt(d,3)),this.setAttribute("uv",new Wt(y,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.width,t.height,t.widthSegments,t.heightSegments)}},Pp=`#ifdef USE_ALPHAHASH
	if ( diffuseColor.a < getAlphaHashThreshold( vPosition ) ) discard;
#endif`,Lp=`#ifdef USE_ALPHAHASH
	const float ALPHA_HASH_SCALE = 0.05;
	float hash2D( vec2 value ) {
		return fract( 1.0e4 * sin( 17.0 * value.x + 0.1 * value.y ) * ( 0.1 + abs( sin( 13.0 * value.y + value.x ) ) ) );
	}
	float hash3D( vec3 value ) {
		return hash2D( vec2( hash2D( value.xy ), value.z ) );
	}
	float getAlphaHashThreshold( vec3 position ) {
		float maxDeriv = max(
			length( dFdx( position.xyz ) ),
			length( dFdy( position.xyz ) )
		);
		float pixScale = 1.0 / ( ALPHA_HASH_SCALE * maxDeriv );
		vec2 pixScales = vec2(
			exp2( floor( log2( pixScale ) ) ),
			exp2( ceil( log2( pixScale ) ) )
		);
		vec2 alpha = vec2(
			hash3D( floor( pixScales.x * position.xyz ) ),
			hash3D( floor( pixScales.y * position.xyz ) )
		);
		float lerpFactor = fract( log2( pixScale ) );
		float x = ( 1.0 - lerpFactor ) * alpha.x + lerpFactor * alpha.y;
		float a = min( lerpFactor, 1.0 - lerpFactor );
		vec3 cases = vec3(
			x * x / ( 2.0 * a * ( 1.0 - a ) ),
			( x - 0.5 * a ) / ( 1.0 - a ),
			1.0 - ( ( 1.0 - x ) * ( 1.0 - x ) / ( 2.0 * a * ( 1.0 - a ) ) )
		);
		float threshold = ( x < ( 1.0 - a ) )
			? ( ( x < a ) ? cases.x : cases.y )
			: cases.z;
		return clamp( threshold , 1.0e-6, 1.0 );
	}
#endif`,Dp=`#ifdef USE_ALPHAMAP
	diffuseColor.a *= texture2D( alphaMap, vAlphaMapUv ).g;
#endif`,Up=`#ifdef USE_ALPHAMAP
	uniform sampler2D alphaMap;
#endif`,Np=`#ifdef USE_ALPHATEST
	#ifdef ALPHA_TO_COVERAGE
	diffuseColor.a = smoothstep( alphaTest, alphaTest + fwidth( diffuseColor.a ), diffuseColor.a );
	if ( diffuseColor.a == 0.0 ) discard;
	#else
	if ( diffuseColor.a < alphaTest ) discard;
	#endif
#endif`,Bp=`#ifdef USE_ALPHATEST
	uniform float alphaTest;
#endif`,zp=`#ifdef USE_AOMAP
	float ambientOcclusion = ( texture2D( aoMap, vAoMapUv ).r - 1.0 ) * aoMapIntensity + 1.0;
	reflectedLight.indirectDiffuse *= ambientOcclusion;
	#if defined( USE_CLEARCOAT ) 
		clearcoatSpecularIndirect *= ambientOcclusion;
	#endif
	#if defined( USE_SHEEN ) 
		sheenSpecularIndirect *= ambientOcclusion;
	#endif
	#if defined( USE_ENVMAP ) && defined( STANDARD )
		float dotNV = saturate( dot( geometryNormal, geometryViewDir ) );
		reflectedLight.indirectSpecular *= computeSpecularOcclusion( dotNV, ambientOcclusion, material.roughness );
	#endif
#endif`,Op=`#ifdef USE_AOMAP
	uniform sampler2D aoMap;
	uniform float aoMapIntensity;
#endif`,Hp=`#ifdef USE_BATCHING
	#if ! defined( GL_ANGLE_multi_draw )
	#define gl_DrawID _gl_DrawID
	uniform int _gl_DrawID;
	#endif
	uniform highp sampler2D batchingTexture;
	uniform highp usampler2D batchingIdTexture;
	mat4 getBatchingMatrix( const in float i ) {
		int size = textureSize( batchingTexture, 0 ).x;
		int j = int( i ) * 4;
		int x = j % size;
		int y = j / size;
		vec4 v1 = texelFetch( batchingTexture, ivec2( x, y ), 0 );
		vec4 v2 = texelFetch( batchingTexture, ivec2( x + 1, y ), 0 );
		vec4 v3 = texelFetch( batchingTexture, ivec2( x + 2, y ), 0 );
		vec4 v4 = texelFetch( batchingTexture, ivec2( x + 3, y ), 0 );
		return mat4( v1, v2, v3, v4 );
	}
	float getIndirectIndex( const in int i ) {
		int size = textureSize( batchingIdTexture, 0 ).x;
		int x = i % size;
		int y = i / size;
		return float( texelFetch( batchingIdTexture, ivec2( x, y ), 0 ).r );
	}
#endif
#ifdef USE_BATCHING_COLOR
	uniform sampler2D batchingColorTexture;
	vec3 getBatchingColor( const in float i ) {
		int size = textureSize( batchingColorTexture, 0 ).x;
		int j = int( i );
		int x = j % size;
		int y = j / size;
		return texelFetch( batchingColorTexture, ivec2( x, y ), 0 ).rgb;
	}
#endif`,kp=`#ifdef USE_BATCHING
	mat4 batchingMatrix = getBatchingMatrix( getIndirectIndex( gl_DrawID ) );
#endif`,Gp=`vec3 transformed = vec3( position );
#ifdef USE_ALPHAHASH
	vPosition = vec3( position );
#endif`,Vp=`vec3 objectNormal = vec3( normal );
#ifdef USE_TANGENT
	vec3 objectTangent = vec3( tangent.xyz );
#endif`,Wp=`float G_BlinnPhong_Implicit( ) {
	return 0.25;
}
float D_BlinnPhong( const in float shininess, const in float dotNH ) {
	return RECIPROCAL_PI * ( shininess * 0.5 + 1.0 ) * pow( dotNH, shininess );
}
vec3 BRDF_BlinnPhong( const in vec3 lightDir, const in vec3 viewDir, const in vec3 normal, const in vec3 specularColor, const in float shininess ) {
	vec3 halfDir = normalize( lightDir + viewDir );
	float dotNH = saturate( dot( normal, halfDir ) );
	float dotVH = saturate( dot( viewDir, halfDir ) );
	vec3 F = F_Schlick( specularColor, 1.0, dotVH );
	float G = G_BlinnPhong_Implicit( );
	float D = D_BlinnPhong( shininess, dotNH );
	return F * ( G * D );
} // validated`,Xp=`#ifdef USE_IRIDESCENCE
	const mat3 XYZ_TO_REC709 = mat3(
		 3.2404542, -0.9692660,  0.0556434,
		-1.5371385,  1.8760108, -0.2040259,
		-0.4985314,  0.0415560,  1.0572252
	);
	vec3 Fresnel0ToIor( vec3 fresnel0 ) {
		vec3 sqrtF0 = sqrt( fresnel0 );
		return ( vec3( 1.0 ) + sqrtF0 ) / ( vec3( 1.0 ) - sqrtF0 );
	}
	vec3 IorToFresnel0( vec3 transmittedIor, float incidentIor ) {
		return pow2( ( transmittedIor - vec3( incidentIor ) ) / ( transmittedIor + vec3( incidentIor ) ) );
	}
	float IorToFresnel0( float transmittedIor, float incidentIor ) {
		return pow2( ( transmittedIor - incidentIor ) / ( transmittedIor + incidentIor ));
	}
	vec3 evalSensitivity( float OPD, vec3 shift ) {
		float phase = 2.0 * PI * OPD * 1.0e-9;
		vec3 val = vec3( 5.4856e-13, 4.4201e-13, 5.2481e-13 );
		vec3 pos = vec3( 1.6810e+06, 1.7953e+06, 2.2084e+06 );
		vec3 var = vec3( 4.3278e+09, 9.3046e+09, 6.6121e+09 );
		vec3 xyz = val * sqrt( 2.0 * PI * var ) * cos( pos * phase + shift ) * exp( - pow2( phase ) * var );
		xyz.x += 9.7470e-14 * sqrt( 2.0 * PI * 4.5282e+09 ) * cos( 2.2399e+06 * phase + shift[ 0 ] ) * exp( - 4.5282e+09 * pow2( phase ) );
		xyz /= 1.0685e-7;
		vec3 rgb = XYZ_TO_REC709 * xyz;
		return rgb;
	}
	vec3 evalIridescence( float outsideIOR, float eta2, float cosTheta1, float thinFilmThickness, vec3 baseF0 ) {
		vec3 I;
		float iridescenceIOR = mix( outsideIOR, eta2, smoothstep( 0.0, 0.03, thinFilmThickness ) );
		float sinTheta2Sq = pow2( outsideIOR / iridescenceIOR ) * ( 1.0 - pow2( cosTheta1 ) );
		float cosTheta2Sq = 1.0 - sinTheta2Sq;
		if ( cosTheta2Sq < 0.0 ) {
			return vec3( 1.0 );
		}
		float cosTheta2 = sqrt( cosTheta2Sq );
		float R0 = IorToFresnel0( iridescenceIOR, outsideIOR );
		float R12 = F_Schlick( R0, 1.0, cosTheta1 );
		float T121 = 1.0 - R12;
		float phi12 = 0.0;
		if ( iridescenceIOR < outsideIOR ) phi12 = PI;
		float phi21 = PI - phi12;
		vec3 baseIOR = Fresnel0ToIor( clamp( baseF0, 0.0, 0.9999 ) );		vec3 R1 = IorToFresnel0( baseIOR, iridescenceIOR );
		vec3 R23 = F_Schlick( R1, 1.0, cosTheta2 );
		vec3 phi23 = vec3( 0.0 );
		if ( baseIOR[ 0 ] < iridescenceIOR ) phi23[ 0 ] = PI;
		if ( baseIOR[ 1 ] < iridescenceIOR ) phi23[ 1 ] = PI;
		if ( baseIOR[ 2 ] < iridescenceIOR ) phi23[ 2 ] = PI;
		float OPD = 2.0 * iridescenceIOR * thinFilmThickness * cosTheta2;
		vec3 phi = vec3( phi21 ) + phi23;
		vec3 R123 = clamp( R12 * R23, 1e-5, 0.9999 );
		vec3 r123 = sqrt( R123 );
		vec3 Rs = pow2( T121 ) * R23 / ( vec3( 1.0 ) - R123 );
		vec3 C0 = R12 + Rs;
		I = C0;
		vec3 Cm = Rs - T121;
		for ( int m = 1; m <= 2; ++ m ) {
			Cm *= r123;
			vec3 Sm = 2.0 * evalSensitivity( float( m ) * OPD, float( m ) * phi );
			I += Cm * Sm;
		}
		return max( I, vec3( 0.0 ) );
	}
#endif`,qp=`#ifdef USE_BUMPMAP
	uniform sampler2D bumpMap;
	uniform float bumpScale;
	vec2 dHdxy_fwd() {
		vec2 dSTdx = dFdx( vBumpMapUv );
		vec2 dSTdy = dFdy( vBumpMapUv );
		float Hll = bumpScale * texture2D( bumpMap, vBumpMapUv ).x;
		float dBx = bumpScale * texture2D( bumpMap, vBumpMapUv + dSTdx ).x - Hll;
		float dBy = bumpScale * texture2D( bumpMap, vBumpMapUv + dSTdy ).x - Hll;
		return vec2( dBx, dBy );
	}
	vec3 perturbNormalArb( vec3 surf_pos, vec3 surf_norm, vec2 dHdxy, float faceDirection ) {
		vec3 vSigmaX = normalize( dFdx( surf_pos.xyz ) );
		vec3 vSigmaY = normalize( dFdy( surf_pos.xyz ) );
		vec3 vN = surf_norm;
		vec3 R1 = cross( vSigmaY, vN );
		vec3 R2 = cross( vN, vSigmaX );
		float fDet = dot( vSigmaX, R1 ) * faceDirection;
		vec3 vGrad = sign( fDet ) * ( dHdxy.x * R1 + dHdxy.y * R2 );
		return normalize( abs( fDet ) * surf_norm - vGrad );
	}
#endif`,Yp=`#if NUM_CLIPPING_PLANES > 0
	vec4 plane;
	#ifdef ALPHA_TO_COVERAGE
		float distanceToPlane, distanceGradient;
		float clipOpacity = 1.0;
		#pragma unroll_loop_start
		for ( int i = 0; i < UNION_CLIPPING_PLANES; i ++ ) {
			plane = clippingPlanes[ i ];
			distanceToPlane = - dot( vClipPosition, plane.xyz ) + plane.w;
			distanceGradient = fwidth( distanceToPlane ) / 2.0;
			clipOpacity *= smoothstep( - distanceGradient, distanceGradient, distanceToPlane );
			if ( clipOpacity == 0.0 ) discard;
		}
		#pragma unroll_loop_end
		#if UNION_CLIPPING_PLANES < NUM_CLIPPING_PLANES
			float unionClipOpacity = 1.0;
			#pragma unroll_loop_start
			for ( int i = UNION_CLIPPING_PLANES; i < NUM_CLIPPING_PLANES; i ++ ) {
				plane = clippingPlanes[ i ];
				distanceToPlane = - dot( vClipPosition, plane.xyz ) + plane.w;
				distanceGradient = fwidth( distanceToPlane ) / 2.0;
				unionClipOpacity *= 1.0 - smoothstep( - distanceGradient, distanceGradient, distanceToPlane );
			}
			#pragma unroll_loop_end
			clipOpacity *= 1.0 - unionClipOpacity;
		#endif
		diffuseColor.a *= clipOpacity;
		if ( diffuseColor.a == 0.0 ) discard;
	#else
		#pragma unroll_loop_start
		for ( int i = 0; i < UNION_CLIPPING_PLANES; i ++ ) {
			plane = clippingPlanes[ i ];
			if ( dot( vClipPosition, plane.xyz ) > plane.w ) discard;
		}
		#pragma unroll_loop_end
		#if UNION_CLIPPING_PLANES < NUM_CLIPPING_PLANES
			bool clipped = true;
			#pragma unroll_loop_start
			for ( int i = UNION_CLIPPING_PLANES; i < NUM_CLIPPING_PLANES; i ++ ) {
				plane = clippingPlanes[ i ];
				clipped = ( dot( vClipPosition, plane.xyz ) > plane.w ) && clipped;
			}
			#pragma unroll_loop_end
			if ( clipped ) discard;
		#endif
	#endif
#endif`,Zp=`#if NUM_CLIPPING_PLANES > 0
	varying vec3 vClipPosition;
	uniform vec4 clippingPlanes[ NUM_CLIPPING_PLANES ];
#endif`,$p=`#if NUM_CLIPPING_PLANES > 0
	varying vec3 vClipPosition;
#endif`,Jp=`#if NUM_CLIPPING_PLANES > 0
	vClipPosition = - mvPosition.xyz;
#endif`,Kp=`#if defined( USE_COLOR_ALPHA )
	diffuseColor *= vColor;
#elif defined( USE_COLOR )
	diffuseColor.rgb *= vColor;
#endif`,jp=`#if defined( USE_COLOR_ALPHA )
	varying vec4 vColor;
#elif defined( USE_COLOR )
	varying vec3 vColor;
#endif`,Qp=`#if defined( USE_COLOR_ALPHA )
	varying vec4 vColor;
#elif defined( USE_COLOR ) || defined( USE_INSTANCING_COLOR ) || defined( USE_BATCHING_COLOR )
	varying vec3 vColor;
#endif`,tm=`#if defined( USE_COLOR_ALPHA )
	vColor = vec4( 1.0 );
#elif defined( USE_COLOR ) || defined( USE_INSTANCING_COLOR ) || defined( USE_BATCHING_COLOR )
	vColor = vec3( 1.0 );
#endif
#ifdef USE_COLOR
	vColor *= color;
#endif
#ifdef USE_INSTANCING_COLOR
	vColor.xyz *= instanceColor.xyz;
#endif
#ifdef USE_BATCHING_COLOR
	vec3 batchingColor = getBatchingColor( getIndirectIndex( gl_DrawID ) );
	vColor.xyz *= batchingColor.xyz;
#endif`,em=`#define PI 3.141592653589793
#define PI2 6.283185307179586
#define PI_HALF 1.5707963267948966
#define RECIPROCAL_PI 0.3183098861837907
#define RECIPROCAL_PI2 0.15915494309189535
#define EPSILON 1e-6
#ifndef saturate
#define saturate( a ) clamp( a, 0.0, 1.0 )
#endif
#define whiteComplement( a ) ( 1.0 - saturate( a ) )
float pow2( const in float x ) { return x*x; }
vec3 pow2( const in vec3 x ) { return x*x; }
float pow3( const in float x ) { return x*x*x; }
float pow4( const in float x ) { float x2 = x*x; return x2*x2; }
float max3( const in vec3 v ) { return max( max( v.x, v.y ), v.z ); }
float average( const in vec3 v ) { return dot( v, vec3( 0.3333333 ) ); }
highp float rand( const in vec2 uv ) {
	const highp float a = 12.9898, b = 78.233, c = 43758.5453;
	highp float dt = dot( uv.xy, vec2( a,b ) ), sn = mod( dt, PI );
	return fract( sin( sn ) * c );
}
#ifdef HIGH_PRECISION
	float precisionSafeLength( vec3 v ) { return length( v ); }
#else
	float precisionSafeLength( vec3 v ) {
		float maxComponent = max3( abs( v ) );
		return length( v / maxComponent ) * maxComponent;
	}
#endif
struct IncidentLight {
	vec3 color;
	vec3 direction;
	bool visible;
};
struct ReflectedLight {
	vec3 directDiffuse;
	vec3 directSpecular;
	vec3 indirectDiffuse;
	vec3 indirectSpecular;
};
#ifdef USE_ALPHAHASH
	varying vec3 vPosition;
#endif
vec3 transformDirection( in vec3 dir, in mat4 matrix ) {
	return normalize( ( matrix * vec4( dir, 0.0 ) ).xyz );
}
vec3 inverseTransformDirection( in vec3 dir, in mat4 matrix ) {
	return normalize( ( vec4( dir, 0.0 ) * matrix ).xyz );
}
mat3 transposeMat3( const in mat3 m ) {
	mat3 tmp;
	tmp[ 0 ] = vec3( m[ 0 ].x, m[ 1 ].x, m[ 2 ].x );
	tmp[ 1 ] = vec3( m[ 0 ].y, m[ 1 ].y, m[ 2 ].y );
	tmp[ 2 ] = vec3( m[ 0 ].z, m[ 1 ].z, m[ 2 ].z );
	return tmp;
}
bool isPerspectiveMatrix( mat4 m ) {
	return m[ 2 ][ 3 ] == - 1.0;
}
vec2 equirectUv( in vec3 dir ) {
	float u = atan( dir.z, dir.x ) * RECIPROCAL_PI2 + 0.5;
	float v = asin( clamp( dir.y, - 1.0, 1.0 ) ) * RECIPROCAL_PI + 0.5;
	return vec2( u, v );
}
vec3 BRDF_Lambert( const in vec3 diffuseColor ) {
	return RECIPROCAL_PI * diffuseColor;
}
vec3 F_Schlick( const in vec3 f0, const in float f90, const in float dotVH ) {
	float fresnel = exp2( ( - 5.55473 * dotVH - 6.98316 ) * dotVH );
	return f0 * ( 1.0 - fresnel ) + ( f90 * fresnel );
}
float F_Schlick( const in float f0, const in float f90, const in float dotVH ) {
	float fresnel = exp2( ( - 5.55473 * dotVH - 6.98316 ) * dotVH );
	return f0 * ( 1.0 - fresnel ) + ( f90 * fresnel );
} // validated`,nm=`#ifdef ENVMAP_TYPE_CUBE_UV
	#define cubeUV_minMipLevel 4.0
	#define cubeUV_minTileSize 16.0
	float getFace( vec3 direction ) {
		vec3 absDirection = abs( direction );
		float face = - 1.0;
		if ( absDirection.x > absDirection.z ) {
			if ( absDirection.x > absDirection.y )
				face = direction.x > 0.0 ? 0.0 : 3.0;
			else
				face = direction.y > 0.0 ? 1.0 : 4.0;
		} else {
			if ( absDirection.z > absDirection.y )
				face = direction.z > 0.0 ? 2.0 : 5.0;
			else
				face = direction.y > 0.0 ? 1.0 : 4.0;
		}
		return face;
	}
	vec2 getUV( vec3 direction, float face ) {
		vec2 uv;
		if ( face == 0.0 ) {
			uv = vec2( direction.z, direction.y ) / abs( direction.x );
		} else if ( face == 1.0 ) {
			uv = vec2( - direction.x, - direction.z ) / abs( direction.y );
		} else if ( face == 2.0 ) {
			uv = vec2( - direction.x, direction.y ) / abs( direction.z );
		} else if ( face == 3.0 ) {
			uv = vec2( - direction.z, direction.y ) / abs( direction.x );
		} else if ( face == 4.0 ) {
			uv = vec2( - direction.x, direction.z ) / abs( direction.y );
		} else {
			uv = vec2( direction.x, direction.y ) / abs( direction.z );
		}
		return 0.5 * ( uv + 1.0 );
	}
	vec3 bilinearCubeUV( sampler2D envMap, vec3 direction, float mipInt ) {
		float face = getFace( direction );
		float filterInt = max( cubeUV_minMipLevel - mipInt, 0.0 );
		mipInt = max( mipInt, cubeUV_minMipLevel );
		float faceSize = exp2( mipInt );
		highp vec2 uv = getUV( direction, face ) * ( faceSize - 2.0 ) + 1.0;
		if ( face > 2.0 ) {
			uv.y += faceSize;
			face -= 3.0;
		}
		uv.x += face * faceSize;
		uv.x += filterInt * 3.0 * cubeUV_minTileSize;
		uv.y += 4.0 * ( exp2( CUBEUV_MAX_MIP ) - faceSize );
		uv.x *= CUBEUV_TEXEL_WIDTH;
		uv.y *= CUBEUV_TEXEL_HEIGHT;
		#ifdef texture2DGradEXT
			return texture2DGradEXT( envMap, uv, vec2( 0.0 ), vec2( 0.0 ) ).rgb;
		#else
			return texture2D( envMap, uv ).rgb;
		#endif
	}
	#define cubeUV_r0 1.0
	#define cubeUV_m0 - 2.0
	#define cubeUV_r1 0.8
	#define cubeUV_m1 - 1.0
	#define cubeUV_r4 0.4
	#define cubeUV_m4 2.0
	#define cubeUV_r5 0.305
	#define cubeUV_m5 3.0
	#define cubeUV_r6 0.21
	#define cubeUV_m6 4.0
	float roughnessToMip( float roughness ) {
		float mip = 0.0;
		if ( roughness >= cubeUV_r1 ) {
			mip = ( cubeUV_r0 - roughness ) * ( cubeUV_m1 - cubeUV_m0 ) / ( cubeUV_r0 - cubeUV_r1 ) + cubeUV_m0;
		} else if ( roughness >= cubeUV_r4 ) {
			mip = ( cubeUV_r1 - roughness ) * ( cubeUV_m4 - cubeUV_m1 ) / ( cubeUV_r1 - cubeUV_r4 ) + cubeUV_m1;
		} else if ( roughness >= cubeUV_r5 ) {
			mip = ( cubeUV_r4 - roughness ) * ( cubeUV_m5 - cubeUV_m4 ) / ( cubeUV_r4 - cubeUV_r5 ) + cubeUV_m4;
		} else if ( roughness >= cubeUV_r6 ) {
			mip = ( cubeUV_r5 - roughness ) * ( cubeUV_m6 - cubeUV_m5 ) / ( cubeUV_r5 - cubeUV_r6 ) + cubeUV_m5;
		} else {
			mip = - 2.0 * log2( 1.16 * roughness );		}
		return mip;
	}
	vec4 textureCubeUV( sampler2D envMap, vec3 sampleDir, float roughness ) {
		float mip = clamp( roughnessToMip( roughness ), cubeUV_m0, CUBEUV_MAX_MIP );
		float mipF = fract( mip );
		float mipInt = floor( mip );
		vec3 color0 = bilinearCubeUV( envMap, sampleDir, mipInt );
		if ( mipF == 0.0 ) {
			return vec4( color0, 1.0 );
		} else {
			vec3 color1 = bilinearCubeUV( envMap, sampleDir, mipInt + 1.0 );
			return vec4( mix( color0, color1, mipF ), 1.0 );
		}
	}
#endif`,im=`vec3 transformedNormal = objectNormal;
#ifdef USE_TANGENT
	vec3 transformedTangent = objectTangent;
#endif
#ifdef USE_BATCHING
	mat3 bm = mat3( batchingMatrix );
	transformedNormal /= vec3( dot( bm[ 0 ], bm[ 0 ] ), dot( bm[ 1 ], bm[ 1 ] ), dot( bm[ 2 ], bm[ 2 ] ) );
	transformedNormal = bm * transformedNormal;
	#ifdef USE_TANGENT
		transformedTangent = bm * transformedTangent;
	#endif
#endif
#ifdef USE_INSTANCING
	mat3 im = mat3( instanceMatrix );
	transformedNormal /= vec3( dot( im[ 0 ], im[ 0 ] ), dot( im[ 1 ], im[ 1 ] ), dot( im[ 2 ], im[ 2 ] ) );
	transformedNormal = im * transformedNormal;
	#ifdef USE_TANGENT
		transformedTangent = im * transformedTangent;
	#endif
#endif
transformedNormal = normalMatrix * transformedNormal;
#ifdef FLIP_SIDED
	transformedNormal = - transformedNormal;
#endif
#ifdef USE_TANGENT
	transformedTangent = ( modelViewMatrix * vec4( transformedTangent, 0.0 ) ).xyz;
	#ifdef FLIP_SIDED
		transformedTangent = - transformedTangent;
	#endif
#endif`,sm=`#ifdef USE_DISPLACEMENTMAP
	uniform sampler2D displacementMap;
	uniform float displacementScale;
	uniform float displacementBias;
#endif`,rm=`#ifdef USE_DISPLACEMENTMAP
	transformed += normalize( objectNormal ) * ( texture2D( displacementMap, vDisplacementMapUv ).x * displacementScale + displacementBias );
#endif`,om=`#ifdef USE_EMISSIVEMAP
	vec4 emissiveColor = texture2D( emissiveMap, vEmissiveMapUv );
	#ifdef DECODE_VIDEO_TEXTURE_EMISSIVE
		emissiveColor = sRGBTransferEOTF( emissiveColor );
	#endif
	totalEmissiveRadiance *= emissiveColor.rgb;
#endif`,am=`#ifdef USE_EMISSIVEMAP
	uniform sampler2D emissiveMap;
#endif`,lm="gl_FragColor = linearToOutputTexel( gl_FragColor );",cm=`vec4 LinearTransferOETF( in vec4 value ) {
	return value;
}
vec4 sRGBTransferEOTF( in vec4 value ) {
	return vec4( mix( pow( value.rgb * 0.9478672986 + vec3( 0.0521327014 ), vec3( 2.4 ) ), value.rgb * 0.0773993808, vec3( lessThanEqual( value.rgb, vec3( 0.04045 ) ) ) ), value.a );
}
vec4 sRGBTransferOETF( in vec4 value ) {
	return vec4( mix( pow( value.rgb, vec3( 0.41666 ) ) * 1.055 - vec3( 0.055 ), value.rgb * 12.92, vec3( lessThanEqual( value.rgb, vec3( 0.0031308 ) ) ) ), value.a );
}`,hm=`#ifdef USE_ENVMAP
	#ifdef ENV_WORLDPOS
		vec3 cameraToFrag;
		if ( isOrthographic ) {
			cameraToFrag = normalize( vec3( - viewMatrix[ 0 ][ 2 ], - viewMatrix[ 1 ][ 2 ], - viewMatrix[ 2 ][ 2 ] ) );
		} else {
			cameraToFrag = normalize( vWorldPosition - cameraPosition );
		}
		vec3 worldNormal = inverseTransformDirection( normal, viewMatrix );
		#ifdef ENVMAP_MODE_REFLECTION
			vec3 reflectVec = reflect( cameraToFrag, worldNormal );
		#else
			vec3 reflectVec = refract( cameraToFrag, worldNormal, refractionRatio );
		#endif
	#else
		vec3 reflectVec = vReflect;
	#endif
	#ifdef ENVMAP_TYPE_CUBE
		vec4 envColor = textureCube( envMap, envMapRotation * vec3( flipEnvMap * reflectVec.x, reflectVec.yz ) );
	#else
		vec4 envColor = vec4( 0.0 );
	#endif
	#ifdef ENVMAP_BLENDING_MULTIPLY
		outgoingLight = mix( outgoingLight, outgoingLight * envColor.xyz, specularStrength * reflectivity );
	#elif defined( ENVMAP_BLENDING_MIX )
		outgoingLight = mix( outgoingLight, envColor.xyz, specularStrength * reflectivity );
	#elif defined( ENVMAP_BLENDING_ADD )
		outgoingLight += envColor.xyz * specularStrength * reflectivity;
	#endif
#endif`,um=`#ifdef USE_ENVMAP
	uniform float envMapIntensity;
	uniform float flipEnvMap;
	uniform mat3 envMapRotation;
	#ifdef ENVMAP_TYPE_CUBE
		uniform samplerCube envMap;
	#else
		uniform sampler2D envMap;
	#endif
	
#endif`,fm=`#ifdef USE_ENVMAP
	uniform float reflectivity;
	#if defined( USE_BUMPMAP ) || defined( USE_NORMALMAP ) || defined( PHONG ) || defined( LAMBERT )
		#define ENV_WORLDPOS
	#endif
	#ifdef ENV_WORLDPOS
		varying vec3 vWorldPosition;
		uniform float refractionRatio;
	#else
		varying vec3 vReflect;
	#endif
#endif`,dm=`#ifdef USE_ENVMAP
	#if defined( USE_BUMPMAP ) || defined( USE_NORMALMAP ) || defined( PHONG ) || defined( LAMBERT )
		#define ENV_WORLDPOS
	#endif
	#ifdef ENV_WORLDPOS
		
		varying vec3 vWorldPosition;
	#else
		varying vec3 vReflect;
		uniform float refractionRatio;
	#endif
#endif`,pm=`#ifdef USE_ENVMAP
	#ifdef ENV_WORLDPOS
		vWorldPosition = worldPosition.xyz;
	#else
		vec3 cameraToVertex;
		if ( isOrthographic ) {
			cameraToVertex = normalize( vec3( - viewMatrix[ 0 ][ 2 ], - viewMatrix[ 1 ][ 2 ], - viewMatrix[ 2 ][ 2 ] ) );
		} else {
			cameraToVertex = normalize( worldPosition.xyz - cameraPosition );
		}
		vec3 worldNormal = inverseTransformDirection( transformedNormal, viewMatrix );
		#ifdef ENVMAP_MODE_REFLECTION
			vReflect = reflect( cameraToVertex, worldNormal );
		#else
			vReflect = refract( cameraToVertex, worldNormal, refractionRatio );
		#endif
	#endif
#endif`,mm=`#ifdef USE_FOG
	vFogDepth = - mvPosition.z;
#endif`,gm=`#ifdef USE_FOG
	varying float vFogDepth;
#endif`,xm=`#ifdef USE_FOG
	#ifdef FOG_EXP2
		float fogFactor = 1.0 - exp( - fogDensity * fogDensity * vFogDepth * vFogDepth );
	#else
		float fogFactor = smoothstep( fogNear, fogFar, vFogDepth );
	#endif
	gl_FragColor.rgb = mix( gl_FragColor.rgb, fogColor, fogFactor );
#endif`,ym=`#ifdef USE_FOG
	uniform vec3 fogColor;
	varying float vFogDepth;
	#ifdef FOG_EXP2
		uniform float fogDensity;
	#else
		uniform float fogNear;
		uniform float fogFar;
	#endif
#endif`,_m=`#ifdef USE_GRADIENTMAP
	uniform sampler2D gradientMap;
#endif
vec3 getGradientIrradiance( vec3 normal, vec3 lightDirection ) {
	float dotNL = dot( normal, lightDirection );
	vec2 coord = vec2( dotNL * 0.5 + 0.5, 0.0 );
	#ifdef USE_GRADIENTMAP
		return vec3( texture2D( gradientMap, coord ).r );
	#else
		vec2 fw = fwidth( coord ) * 0.5;
		return mix( vec3( 0.7 ), vec3( 1.0 ), smoothstep( 0.7 - fw.x, 0.7 + fw.x, coord.x ) );
	#endif
}`,vm=`#ifdef USE_LIGHTMAP
	uniform sampler2D lightMap;
	uniform float lightMapIntensity;
#endif`,Mm=`LambertMaterial material;
material.diffuseColor = diffuseColor.rgb;
material.specularStrength = specularStrength;`,Em=`varying vec3 vViewPosition;
struct LambertMaterial {
	vec3 diffuseColor;
	float specularStrength;
};
void RE_Direct_Lambert( const in IncidentLight directLight, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in LambertMaterial material, inout ReflectedLight reflectedLight ) {
	float dotNL = saturate( dot( geometryNormal, directLight.direction ) );
	vec3 irradiance = dotNL * directLight.color;
	reflectedLight.directDiffuse += irradiance * BRDF_Lambert( material.diffuseColor );
}
void RE_IndirectDiffuse_Lambert( const in vec3 irradiance, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in LambertMaterial material, inout ReflectedLight reflectedLight ) {
	reflectedLight.indirectDiffuse += irradiance * BRDF_Lambert( material.diffuseColor );
}
#define RE_Direct				RE_Direct_Lambert
#define RE_IndirectDiffuse		RE_IndirectDiffuse_Lambert`,Sm=`uniform bool receiveShadow;
uniform vec3 ambientLightColor;
#if defined( USE_LIGHT_PROBES )
	uniform vec3 lightProbe[ 9 ];
#endif
vec3 shGetIrradianceAt( in vec3 normal, in vec3 shCoefficients[ 9 ] ) {
	float x = normal.x, y = normal.y, z = normal.z;
	vec3 result = shCoefficients[ 0 ] * 0.886227;
	result += shCoefficients[ 1 ] * 2.0 * 0.511664 * y;
	result += shCoefficients[ 2 ] * 2.0 * 0.511664 * z;
	result += shCoefficients[ 3 ] * 2.0 * 0.511664 * x;
	result += shCoefficients[ 4 ] * 2.0 * 0.429043 * x * y;
	result += shCoefficients[ 5 ] * 2.0 * 0.429043 * y * z;
	result += shCoefficients[ 6 ] * ( 0.743125 * z * z - 0.247708 );
	result += shCoefficients[ 7 ] * 2.0 * 0.429043 * x * z;
	result += shCoefficients[ 8 ] * 0.429043 * ( x * x - y * y );
	return result;
}
vec3 getLightProbeIrradiance( const in vec3 lightProbe[ 9 ], const in vec3 normal ) {
	vec3 worldNormal = inverseTransformDirection( normal, viewMatrix );
	vec3 irradiance = shGetIrradianceAt( worldNormal, lightProbe );
	return irradiance;
}
vec3 getAmbientLightIrradiance( const in vec3 ambientLightColor ) {
	vec3 irradiance = ambientLightColor;
	return irradiance;
}
float getDistanceAttenuation( const in float lightDistance, const in float cutoffDistance, const in float decayExponent ) {
	float distanceFalloff = 1.0 / max( pow( lightDistance, decayExponent ), 0.01 );
	if ( cutoffDistance > 0.0 ) {
		distanceFalloff *= pow2( saturate( 1.0 - pow4( lightDistance / cutoffDistance ) ) );
	}
	return distanceFalloff;
}
float getSpotAttenuation( const in float coneCosine, const in float penumbraCosine, const in float angleCosine ) {
	return smoothstep( coneCosine, penumbraCosine, angleCosine );
}
#if NUM_DIR_LIGHTS > 0
	struct DirectionalLight {
		vec3 direction;
		vec3 color;
	};
	uniform DirectionalLight directionalLights[ NUM_DIR_LIGHTS ];
	void getDirectionalLightInfo( const in DirectionalLight directionalLight, out IncidentLight light ) {
		light.color = directionalLight.color;
		light.direction = directionalLight.direction;
		light.visible = true;
	}
#endif
#if NUM_POINT_LIGHTS > 0
	struct PointLight {
		vec3 position;
		vec3 color;
		float distance;
		float decay;
	};
	uniform PointLight pointLights[ NUM_POINT_LIGHTS ];
	void getPointLightInfo( const in PointLight pointLight, const in vec3 geometryPosition, out IncidentLight light ) {
		vec3 lVector = pointLight.position - geometryPosition;
		light.direction = normalize( lVector );
		float lightDistance = length( lVector );
		light.color = pointLight.color;
		light.color *= getDistanceAttenuation( lightDistance, pointLight.distance, pointLight.decay );
		light.visible = ( light.color != vec3( 0.0 ) );
	}
#endif
#if NUM_SPOT_LIGHTS > 0
	struct SpotLight {
		vec3 position;
		vec3 direction;
		vec3 color;
		float distance;
		float decay;
		float coneCos;
		float penumbraCos;
	};
	uniform SpotLight spotLights[ NUM_SPOT_LIGHTS ];
	void getSpotLightInfo( const in SpotLight spotLight, const in vec3 geometryPosition, out IncidentLight light ) {
		vec3 lVector = spotLight.position - geometryPosition;
		light.direction = normalize( lVector );
		float angleCos = dot( light.direction, spotLight.direction );
		float spotAttenuation = getSpotAttenuation( spotLight.coneCos, spotLight.penumbraCos, angleCos );
		if ( spotAttenuation > 0.0 ) {
			float lightDistance = length( lVector );
			light.color = spotLight.color * spotAttenuation;
			light.color *= getDistanceAttenuation( lightDistance, spotLight.distance, spotLight.decay );
			light.visible = ( light.color != vec3( 0.0 ) );
		} else {
			light.color = vec3( 0.0 );
			light.visible = false;
		}
	}
#endif
#if NUM_RECT_AREA_LIGHTS > 0
	struct RectAreaLight {
		vec3 color;
		vec3 position;
		vec3 halfWidth;
		vec3 halfHeight;
	};
	uniform sampler2D ltc_1;	uniform sampler2D ltc_2;
	uniform RectAreaLight rectAreaLights[ NUM_RECT_AREA_LIGHTS ];
#endif
#if NUM_HEMI_LIGHTS > 0
	struct HemisphereLight {
		vec3 direction;
		vec3 skyColor;
		vec3 groundColor;
	};
	uniform HemisphereLight hemisphereLights[ NUM_HEMI_LIGHTS ];
	vec3 getHemisphereLightIrradiance( const in HemisphereLight hemiLight, const in vec3 normal ) {
		float dotNL = dot( normal, hemiLight.direction );
		float hemiDiffuseWeight = 0.5 * dotNL + 0.5;
		vec3 irradiance = mix( hemiLight.groundColor, hemiLight.skyColor, hemiDiffuseWeight );
		return irradiance;
	}
#endif`,Tm=`#ifdef USE_ENVMAP
	vec3 getIBLIrradiance( const in vec3 normal ) {
		#ifdef ENVMAP_TYPE_CUBE_UV
			vec3 worldNormal = inverseTransformDirection( normal, viewMatrix );
			vec4 envMapColor = textureCubeUV( envMap, envMapRotation * worldNormal, 1.0 );
			return PI * envMapColor.rgb * envMapIntensity;
		#else
			return vec3( 0.0 );
		#endif
	}
	vec3 getIBLRadiance( const in vec3 viewDir, const in vec3 normal, const in float roughness ) {
		#ifdef ENVMAP_TYPE_CUBE_UV
			vec3 reflectVec = reflect( - viewDir, normal );
			reflectVec = normalize( mix( reflectVec, normal, roughness * roughness) );
			reflectVec = inverseTransformDirection( reflectVec, viewMatrix );
			vec4 envMapColor = textureCubeUV( envMap, envMapRotation * reflectVec, roughness );
			return envMapColor.rgb * envMapIntensity;
		#else
			return vec3( 0.0 );
		#endif
	}
	#ifdef USE_ANISOTROPY
		vec3 getIBLAnisotropyRadiance( const in vec3 viewDir, const in vec3 normal, const in float roughness, const in vec3 bitangent, const in float anisotropy ) {
			#ifdef ENVMAP_TYPE_CUBE_UV
				vec3 bentNormal = cross( bitangent, viewDir );
				bentNormal = normalize( cross( bentNormal, bitangent ) );
				bentNormal = normalize( mix( bentNormal, normal, pow2( pow2( 1.0 - anisotropy * ( 1.0 - roughness ) ) ) ) );
				return getIBLRadiance( viewDir, bentNormal, roughness );
			#else
				return vec3( 0.0 );
			#endif
		}
	#endif
#endif`,wm=`ToonMaterial material;
material.diffuseColor = diffuseColor.rgb;`,bm=`varying vec3 vViewPosition;
struct ToonMaterial {
	vec3 diffuseColor;
};
void RE_Direct_Toon( const in IncidentLight directLight, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in ToonMaterial material, inout ReflectedLight reflectedLight ) {
	vec3 irradiance = getGradientIrradiance( geometryNormal, directLight.direction ) * directLight.color;
	reflectedLight.directDiffuse += irradiance * BRDF_Lambert( material.diffuseColor );
}
void RE_IndirectDiffuse_Toon( const in vec3 irradiance, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in ToonMaterial material, inout ReflectedLight reflectedLight ) {
	reflectedLight.indirectDiffuse += irradiance * BRDF_Lambert( material.diffuseColor );
}
#define RE_Direct				RE_Direct_Toon
#define RE_IndirectDiffuse		RE_IndirectDiffuse_Toon`,Am=`BlinnPhongMaterial material;
material.diffuseColor = diffuseColor.rgb;
material.specularColor = specular;
material.specularShininess = shininess;
material.specularStrength = specularStrength;`,Rm=`varying vec3 vViewPosition;
struct BlinnPhongMaterial {
	vec3 diffuseColor;
	vec3 specularColor;
	float specularShininess;
	float specularStrength;
};
void RE_Direct_BlinnPhong( const in IncidentLight directLight, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in BlinnPhongMaterial material, inout ReflectedLight reflectedLight ) {
	float dotNL = saturate( dot( geometryNormal, directLight.direction ) );
	vec3 irradiance = dotNL * directLight.color;
	reflectedLight.directDiffuse += irradiance * BRDF_Lambert( material.diffuseColor );
	reflectedLight.directSpecular += irradiance * BRDF_BlinnPhong( directLight.direction, geometryViewDir, geometryNormal, material.specularColor, material.specularShininess ) * material.specularStrength;
}
void RE_IndirectDiffuse_BlinnPhong( const in vec3 irradiance, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in BlinnPhongMaterial material, inout ReflectedLight reflectedLight ) {
	reflectedLight.indirectDiffuse += irradiance * BRDF_Lambert( material.diffuseColor );
}
#define RE_Direct				RE_Direct_BlinnPhong
#define RE_IndirectDiffuse		RE_IndirectDiffuse_BlinnPhong`,Cm=`PhysicalMaterial material;
material.diffuseColor = diffuseColor.rgb * ( 1.0 - metalnessFactor );
vec3 dxy = max( abs( dFdx( nonPerturbedNormal ) ), abs( dFdy( nonPerturbedNormal ) ) );
float geometryRoughness = max( max( dxy.x, dxy.y ), dxy.z );
material.roughness = max( roughnessFactor, 0.0525 );material.roughness += geometryRoughness;
material.roughness = min( material.roughness, 1.0 );
#ifdef IOR
	material.ior = ior;
	#ifdef USE_SPECULAR
		float specularIntensityFactor = specularIntensity;
		vec3 specularColorFactor = specularColor;
		#ifdef USE_SPECULAR_COLORMAP
			specularColorFactor *= texture2D( specularColorMap, vSpecularColorMapUv ).rgb;
		#endif
		#ifdef USE_SPECULAR_INTENSITYMAP
			specularIntensityFactor *= texture2D( specularIntensityMap, vSpecularIntensityMapUv ).a;
		#endif
		material.specularF90 = mix( specularIntensityFactor, 1.0, metalnessFactor );
	#else
		float specularIntensityFactor = 1.0;
		vec3 specularColorFactor = vec3( 1.0 );
		material.specularF90 = 1.0;
	#endif
	material.specularColor = mix( min( pow2( ( material.ior - 1.0 ) / ( material.ior + 1.0 ) ) * specularColorFactor, vec3( 1.0 ) ) * specularIntensityFactor, diffuseColor.rgb, metalnessFactor );
#else
	material.specularColor = mix( vec3( 0.04 ), diffuseColor.rgb, metalnessFactor );
	material.specularF90 = 1.0;
#endif
#ifdef USE_CLEARCOAT
	material.clearcoat = clearcoat;
	material.clearcoatRoughness = clearcoatRoughness;
	material.clearcoatF0 = vec3( 0.04 );
	material.clearcoatF90 = 1.0;
	#ifdef USE_CLEARCOATMAP
		material.clearcoat *= texture2D( clearcoatMap, vClearcoatMapUv ).x;
	#endif
	#ifdef USE_CLEARCOAT_ROUGHNESSMAP
		material.clearcoatRoughness *= texture2D( clearcoatRoughnessMap, vClearcoatRoughnessMapUv ).y;
	#endif
	material.clearcoat = saturate( material.clearcoat );	material.clearcoatRoughness = max( material.clearcoatRoughness, 0.0525 );
	material.clearcoatRoughness += geometryRoughness;
	material.clearcoatRoughness = min( material.clearcoatRoughness, 1.0 );
#endif
#ifdef USE_DISPERSION
	material.dispersion = dispersion;
#endif
#ifdef USE_IRIDESCENCE
	material.iridescence = iridescence;
	material.iridescenceIOR = iridescenceIOR;
	#ifdef USE_IRIDESCENCEMAP
		material.iridescence *= texture2D( iridescenceMap, vIridescenceMapUv ).r;
	#endif
	#ifdef USE_IRIDESCENCE_THICKNESSMAP
		material.iridescenceThickness = (iridescenceThicknessMaximum - iridescenceThicknessMinimum) * texture2D( iridescenceThicknessMap, vIridescenceThicknessMapUv ).g + iridescenceThicknessMinimum;
	#else
		material.iridescenceThickness = iridescenceThicknessMaximum;
	#endif
#endif
#ifdef USE_SHEEN
	material.sheenColor = sheenColor;
	#ifdef USE_SHEEN_COLORMAP
		material.sheenColor *= texture2D( sheenColorMap, vSheenColorMapUv ).rgb;
	#endif
	material.sheenRoughness = clamp( sheenRoughness, 0.07, 1.0 );
	#ifdef USE_SHEEN_ROUGHNESSMAP
		material.sheenRoughness *= texture2D( sheenRoughnessMap, vSheenRoughnessMapUv ).a;
	#endif
#endif
#ifdef USE_ANISOTROPY
	#ifdef USE_ANISOTROPYMAP
		mat2 anisotropyMat = mat2( anisotropyVector.x, anisotropyVector.y, - anisotropyVector.y, anisotropyVector.x );
		vec3 anisotropyPolar = texture2D( anisotropyMap, vAnisotropyMapUv ).rgb;
		vec2 anisotropyV = anisotropyMat * normalize( 2.0 * anisotropyPolar.rg - vec2( 1.0 ) ) * anisotropyPolar.b;
	#else
		vec2 anisotropyV = anisotropyVector;
	#endif
	material.anisotropy = length( anisotropyV );
	if( material.anisotropy == 0.0 ) {
		anisotropyV = vec2( 1.0, 0.0 );
	} else {
		anisotropyV /= material.anisotropy;
		material.anisotropy = saturate( material.anisotropy );
	}
	material.alphaT = mix( pow2( material.roughness ), 1.0, pow2( material.anisotropy ) );
	material.anisotropyT = tbn[ 0 ] * anisotropyV.x + tbn[ 1 ] * anisotropyV.y;
	material.anisotropyB = tbn[ 1 ] * anisotropyV.x - tbn[ 0 ] * anisotropyV.y;
#endif`,Fm=`struct PhysicalMaterial {
	vec3 diffuseColor;
	float roughness;
	vec3 specularColor;
	float specularF90;
	float dispersion;
	#ifdef USE_CLEARCOAT
		float clearcoat;
		float clearcoatRoughness;
		vec3 clearcoatF0;
		float clearcoatF90;
	#endif
	#ifdef USE_IRIDESCENCE
		float iridescence;
		float iridescenceIOR;
		float iridescenceThickness;
		vec3 iridescenceFresnel;
		vec3 iridescenceF0;
	#endif
	#ifdef USE_SHEEN
		vec3 sheenColor;
		float sheenRoughness;
	#endif
	#ifdef IOR
		float ior;
	#endif
	#ifdef USE_TRANSMISSION
		float transmission;
		float transmissionAlpha;
		float thickness;
		float attenuationDistance;
		vec3 attenuationColor;
	#endif
	#ifdef USE_ANISOTROPY
		float anisotropy;
		float alphaT;
		vec3 anisotropyT;
		vec3 anisotropyB;
	#endif
};
vec3 clearcoatSpecularDirect = vec3( 0.0 );
vec3 clearcoatSpecularIndirect = vec3( 0.0 );
vec3 sheenSpecularDirect = vec3( 0.0 );
vec3 sheenSpecularIndirect = vec3(0.0 );
vec3 Schlick_to_F0( const in vec3 f, const in float f90, const in float dotVH ) {
    float x = clamp( 1.0 - dotVH, 0.0, 1.0 );
    float x2 = x * x;
    float x5 = clamp( x * x2 * x2, 0.0, 0.9999 );
    return ( f - vec3( f90 ) * x5 ) / ( 1.0 - x5 );
}
float V_GGX_SmithCorrelated( const in float alpha, const in float dotNL, const in float dotNV ) {
	float a2 = pow2( alpha );
	float gv = dotNL * sqrt( a2 + ( 1.0 - a2 ) * pow2( dotNV ) );
	float gl = dotNV * sqrt( a2 + ( 1.0 - a2 ) * pow2( dotNL ) );
	return 0.5 / max( gv + gl, EPSILON );
}
float D_GGX( const in float alpha, const in float dotNH ) {
	float a2 = pow2( alpha );
	float denom = pow2( dotNH ) * ( a2 - 1.0 ) + 1.0;
	return RECIPROCAL_PI * a2 / pow2( denom );
}
#ifdef USE_ANISOTROPY
	float V_GGX_SmithCorrelated_Anisotropic( const in float alphaT, const in float alphaB, const in float dotTV, const in float dotBV, const in float dotTL, const in float dotBL, const in float dotNV, const in float dotNL ) {
		float gv = dotNL * length( vec3( alphaT * dotTV, alphaB * dotBV, dotNV ) );
		float gl = dotNV * length( vec3( alphaT * dotTL, alphaB * dotBL, dotNL ) );
		float v = 0.5 / ( gv + gl );
		return saturate(v);
	}
	float D_GGX_Anisotropic( const in float alphaT, const in float alphaB, const in float dotNH, const in float dotTH, const in float dotBH ) {
		float a2 = alphaT * alphaB;
		highp vec3 v = vec3( alphaB * dotTH, alphaT * dotBH, a2 * dotNH );
		highp float v2 = dot( v, v );
		float w2 = a2 / v2;
		return RECIPROCAL_PI * a2 * pow2 ( w2 );
	}
#endif
#ifdef USE_CLEARCOAT
	vec3 BRDF_GGX_Clearcoat( const in vec3 lightDir, const in vec3 viewDir, const in vec3 normal, const in PhysicalMaterial material) {
		vec3 f0 = material.clearcoatF0;
		float f90 = material.clearcoatF90;
		float roughness = material.clearcoatRoughness;
		float alpha = pow2( roughness );
		vec3 halfDir = normalize( lightDir + viewDir );
		float dotNL = saturate( dot( normal, lightDir ) );
		float dotNV = saturate( dot( normal, viewDir ) );
		float dotNH = saturate( dot( normal, halfDir ) );
		float dotVH = saturate( dot( viewDir, halfDir ) );
		vec3 F = F_Schlick( f0, f90, dotVH );
		float V = V_GGX_SmithCorrelated( alpha, dotNL, dotNV );
		float D = D_GGX( alpha, dotNH );
		return F * ( V * D );
	}
#endif
vec3 BRDF_GGX( const in vec3 lightDir, const in vec3 viewDir, const in vec3 normal, const in PhysicalMaterial material ) {
	vec3 f0 = material.specularColor;
	float f90 = material.specularF90;
	float roughness = material.roughness;
	float alpha = pow2( roughness );
	vec3 halfDir = normalize( lightDir + viewDir );
	float dotNL = saturate( dot( normal, lightDir ) );
	float dotNV = saturate( dot( normal, viewDir ) );
	float dotNH = saturate( dot( normal, halfDir ) );
	float dotVH = saturate( dot( viewDir, halfDir ) );
	vec3 F = F_Schlick( f0, f90, dotVH );
	#ifdef USE_IRIDESCENCE
		F = mix( F, material.iridescenceFresnel, material.iridescence );
	#endif
	#ifdef USE_ANISOTROPY
		float dotTL = dot( material.anisotropyT, lightDir );
		float dotTV = dot( material.anisotropyT, viewDir );
		float dotTH = dot( material.anisotropyT, halfDir );
		float dotBL = dot( material.anisotropyB, lightDir );
		float dotBV = dot( material.anisotropyB, viewDir );
		float dotBH = dot( material.anisotropyB, halfDir );
		float V = V_GGX_SmithCorrelated_Anisotropic( material.alphaT, alpha, dotTV, dotBV, dotTL, dotBL, dotNV, dotNL );
		float D = D_GGX_Anisotropic( material.alphaT, alpha, dotNH, dotTH, dotBH );
	#else
		float V = V_GGX_SmithCorrelated( alpha, dotNL, dotNV );
		float D = D_GGX( alpha, dotNH );
	#endif
	return F * ( V * D );
}
vec2 LTC_Uv( const in vec3 N, const in vec3 V, const in float roughness ) {
	const float LUT_SIZE = 64.0;
	const float LUT_SCALE = ( LUT_SIZE - 1.0 ) / LUT_SIZE;
	const float LUT_BIAS = 0.5 / LUT_SIZE;
	float dotNV = saturate( dot( N, V ) );
	vec2 uv = vec2( roughness, sqrt( 1.0 - dotNV ) );
	uv = uv * LUT_SCALE + LUT_BIAS;
	return uv;
}
float LTC_ClippedSphereFormFactor( const in vec3 f ) {
	float l = length( f );
	return max( ( l * l + f.z ) / ( l + 1.0 ), 0.0 );
}
vec3 LTC_EdgeVectorFormFactor( const in vec3 v1, const in vec3 v2 ) {
	float x = dot( v1, v2 );
	float y = abs( x );
	float a = 0.8543985 + ( 0.4965155 + 0.0145206 * y ) * y;
	float b = 3.4175940 + ( 4.1616724 + y ) * y;
	float v = a / b;
	float theta_sintheta = ( x > 0.0 ) ? v : 0.5 * inversesqrt( max( 1.0 - x * x, 1e-7 ) ) - v;
	return cross( v1, v2 ) * theta_sintheta;
}
vec3 LTC_Evaluate( const in vec3 N, const in vec3 V, const in vec3 P, const in mat3 mInv, const in vec3 rectCoords[ 4 ] ) {
	vec3 v1 = rectCoords[ 1 ] - rectCoords[ 0 ];
	vec3 v2 = rectCoords[ 3 ] - rectCoords[ 0 ];
	vec3 lightNormal = cross( v1, v2 );
	if( dot( lightNormal, P - rectCoords[ 0 ] ) < 0.0 ) return vec3( 0.0 );
	vec3 T1, T2;
	T1 = normalize( V - N * dot( V, N ) );
	T2 = - cross( N, T1 );
	mat3 mat = mInv * transposeMat3( mat3( T1, T2, N ) );
	vec3 coords[ 4 ];
	coords[ 0 ] = mat * ( rectCoords[ 0 ] - P );
	coords[ 1 ] = mat * ( rectCoords[ 1 ] - P );
	coords[ 2 ] = mat * ( rectCoords[ 2 ] - P );
	coords[ 3 ] = mat * ( rectCoords[ 3 ] - P );
	coords[ 0 ] = normalize( coords[ 0 ] );
	coords[ 1 ] = normalize( coords[ 1 ] );
	coords[ 2 ] = normalize( coords[ 2 ] );
	coords[ 3 ] = normalize( coords[ 3 ] );
	vec3 vectorFormFactor = vec3( 0.0 );
	vectorFormFactor += LTC_EdgeVectorFormFactor( coords[ 0 ], coords[ 1 ] );
	vectorFormFactor += LTC_EdgeVectorFormFactor( coords[ 1 ], coords[ 2 ] );
	vectorFormFactor += LTC_EdgeVectorFormFactor( coords[ 2 ], coords[ 3 ] );
	vectorFormFactor += LTC_EdgeVectorFormFactor( coords[ 3 ], coords[ 0 ] );
	float result = LTC_ClippedSphereFormFactor( vectorFormFactor );
	return vec3( result );
}
#if defined( USE_SHEEN )
float D_Charlie( float roughness, float dotNH ) {
	float alpha = pow2( roughness );
	float invAlpha = 1.0 / alpha;
	float cos2h = dotNH * dotNH;
	float sin2h = max( 1.0 - cos2h, 0.0078125 );
	return ( 2.0 + invAlpha ) * pow( sin2h, invAlpha * 0.5 ) / ( 2.0 * PI );
}
float V_Neubelt( float dotNV, float dotNL ) {
	return saturate( 1.0 / ( 4.0 * ( dotNL + dotNV - dotNL * dotNV ) ) );
}
vec3 BRDF_Sheen( const in vec3 lightDir, const in vec3 viewDir, const in vec3 normal, vec3 sheenColor, const in float sheenRoughness ) {
	vec3 halfDir = normalize( lightDir + viewDir );
	float dotNL = saturate( dot( normal, lightDir ) );
	float dotNV = saturate( dot( normal, viewDir ) );
	float dotNH = saturate( dot( normal, halfDir ) );
	float D = D_Charlie( sheenRoughness, dotNH );
	float V = V_Neubelt( dotNV, dotNL );
	return sheenColor * ( D * V );
}
#endif
float IBLSheenBRDF( const in vec3 normal, const in vec3 viewDir, const in float roughness ) {
	float dotNV = saturate( dot( normal, viewDir ) );
	float r2 = roughness * roughness;
	float a = roughness < 0.25 ? -339.2 * r2 + 161.4 * roughness - 25.9 : -8.48 * r2 + 14.3 * roughness - 9.95;
	float b = roughness < 0.25 ? 44.0 * r2 - 23.7 * roughness + 3.26 : 1.97 * r2 - 3.27 * roughness + 0.72;
	float DG = exp( a * dotNV + b ) + ( roughness < 0.25 ? 0.0 : 0.1 * ( roughness - 0.25 ) );
	return saturate( DG * RECIPROCAL_PI );
}
vec2 DFGApprox( const in vec3 normal, const in vec3 viewDir, const in float roughness ) {
	float dotNV = saturate( dot( normal, viewDir ) );
	const vec4 c0 = vec4( - 1, - 0.0275, - 0.572, 0.022 );
	const vec4 c1 = vec4( 1, 0.0425, 1.04, - 0.04 );
	vec4 r = roughness * c0 + c1;
	float a004 = min( r.x * r.x, exp2( - 9.28 * dotNV ) ) * r.x + r.y;
	vec2 fab = vec2( - 1.04, 1.04 ) * a004 + r.zw;
	return fab;
}
vec3 EnvironmentBRDF( const in vec3 normal, const in vec3 viewDir, const in vec3 specularColor, const in float specularF90, const in float roughness ) {
	vec2 fab = DFGApprox( normal, viewDir, roughness );
	return specularColor * fab.x + specularF90 * fab.y;
}
#ifdef USE_IRIDESCENCE
void computeMultiscatteringIridescence( const in vec3 normal, const in vec3 viewDir, const in vec3 specularColor, const in float specularF90, const in float iridescence, const in vec3 iridescenceF0, const in float roughness, inout vec3 singleScatter, inout vec3 multiScatter ) {
#else
void computeMultiscattering( const in vec3 normal, const in vec3 viewDir, const in vec3 specularColor, const in float specularF90, const in float roughness, inout vec3 singleScatter, inout vec3 multiScatter ) {
#endif
	vec2 fab = DFGApprox( normal, viewDir, roughness );
	#ifdef USE_IRIDESCENCE
		vec3 Fr = mix( specularColor, iridescenceF0, iridescence );
	#else
		vec3 Fr = specularColor;
	#endif
	vec3 FssEss = Fr * fab.x + specularF90 * fab.y;
	float Ess = fab.x + fab.y;
	float Ems = 1.0 - Ess;
	vec3 Favg = Fr + ( 1.0 - Fr ) * 0.047619;	vec3 Fms = FssEss * Favg / ( 1.0 - Ems * Favg );
	singleScatter += FssEss;
	multiScatter += Fms * Ems;
}
#if NUM_RECT_AREA_LIGHTS > 0
	void RE_Direct_RectArea_Physical( const in RectAreaLight rectAreaLight, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in PhysicalMaterial material, inout ReflectedLight reflectedLight ) {
		vec3 normal = geometryNormal;
		vec3 viewDir = geometryViewDir;
		vec3 position = geometryPosition;
		vec3 lightPos = rectAreaLight.position;
		vec3 halfWidth = rectAreaLight.halfWidth;
		vec3 halfHeight = rectAreaLight.halfHeight;
		vec3 lightColor = rectAreaLight.color;
		float roughness = material.roughness;
		vec3 rectCoords[ 4 ];
		rectCoords[ 0 ] = lightPos + halfWidth - halfHeight;		rectCoords[ 1 ] = lightPos - halfWidth - halfHeight;
		rectCoords[ 2 ] = lightPos - halfWidth + halfHeight;
		rectCoords[ 3 ] = lightPos + halfWidth + halfHeight;
		vec2 uv = LTC_Uv( normal, viewDir, roughness );
		vec4 t1 = texture2D( ltc_1, uv );
		vec4 t2 = texture2D( ltc_2, uv );
		mat3 mInv = mat3(
			vec3( t1.x, 0, t1.y ),
			vec3(    0, 1,    0 ),
			vec3( t1.z, 0, t1.w )
		);
		vec3 fresnel = ( material.specularColor * t2.x + ( vec3( 1.0 ) - material.specularColor ) * t2.y );
		reflectedLight.directSpecular += lightColor * fresnel * LTC_Evaluate( normal, viewDir, position, mInv, rectCoords );
		reflectedLight.directDiffuse += lightColor * material.diffuseColor * LTC_Evaluate( normal, viewDir, position, mat3( 1.0 ), rectCoords );
	}
#endif
void RE_Direct_Physical( const in IncidentLight directLight, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in PhysicalMaterial material, inout ReflectedLight reflectedLight ) {
	float dotNL = saturate( dot( geometryNormal, directLight.direction ) );
	vec3 irradiance = dotNL * directLight.color;
	#ifdef USE_CLEARCOAT
		float dotNLcc = saturate( dot( geometryClearcoatNormal, directLight.direction ) );
		vec3 ccIrradiance = dotNLcc * directLight.color;
		clearcoatSpecularDirect += ccIrradiance * BRDF_GGX_Clearcoat( directLight.direction, geometryViewDir, geometryClearcoatNormal, material );
	#endif
	#ifdef USE_SHEEN
		sheenSpecularDirect += irradiance * BRDF_Sheen( directLight.direction, geometryViewDir, geometryNormal, material.sheenColor, material.sheenRoughness );
	#endif
	reflectedLight.directSpecular += irradiance * BRDF_GGX( directLight.direction, geometryViewDir, geometryNormal, material );
	reflectedLight.directDiffuse += irradiance * BRDF_Lambert( material.diffuseColor );
}
void RE_IndirectDiffuse_Physical( const in vec3 irradiance, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in PhysicalMaterial material, inout ReflectedLight reflectedLight ) {
	reflectedLight.indirectDiffuse += irradiance * BRDF_Lambert( material.diffuseColor );
}
void RE_IndirectSpecular_Physical( const in vec3 radiance, const in vec3 irradiance, const in vec3 clearcoatRadiance, const in vec3 geometryPosition, const in vec3 geometryNormal, const in vec3 geometryViewDir, const in vec3 geometryClearcoatNormal, const in PhysicalMaterial material, inout ReflectedLight reflectedLight) {
	#ifdef USE_CLEARCOAT
		clearcoatSpecularIndirect += clearcoatRadiance * EnvironmentBRDF( geometryClearcoatNormal, geometryViewDir, material.clearcoatF0, material.clearcoatF90, material.clearcoatRoughness );
	#endif
	#ifdef USE_SHEEN
		sheenSpecularIndirect += irradiance * material.sheenColor * IBLSheenBRDF( geometryNormal, geometryViewDir, material.sheenRoughness );
	#endif
	vec3 singleScattering = vec3( 0.0 );
	vec3 multiScattering = vec3( 0.0 );
	vec3 cosineWeightedIrradiance = irradiance * RECIPROCAL_PI;
	#ifdef USE_IRIDESCENCE
		computeMultiscatteringIridescence( geometryNormal, geometryViewDir, material.specularColor, material.specularF90, material.iridescence, material.iridescenceFresnel, material.roughness, singleScattering, multiScattering );
	#else
		computeMultiscattering( geometryNormal, geometryViewDir, material.specularColor, material.specularF90, material.roughness, singleScattering, multiScattering );
	#endif
	vec3 totalScattering = singleScattering + multiScattering;
	vec3 diffuse = material.diffuseColor * ( 1.0 - max( max( totalScattering.r, totalScattering.g ), totalScattering.b ) );
	reflectedLight.indirectSpecular += radiance * singleScattering;
	reflectedLight.indirectSpecular += multiScattering * cosineWeightedIrradiance;
	reflectedLight.indirectDiffuse += diffuse * cosineWeightedIrradiance;
}
#define RE_Direct				RE_Direct_Physical
#define RE_Direct_RectArea		RE_Direct_RectArea_Physical
#define RE_IndirectDiffuse		RE_IndirectDiffuse_Physical
#define RE_IndirectSpecular		RE_IndirectSpecular_Physical
float computeSpecularOcclusion( const in float dotNV, const in float ambientOcclusion, const in float roughness ) {
	return saturate( pow( dotNV + ambientOcclusion, exp2( - 16.0 * roughness - 1.0 ) ) - 1.0 + ambientOcclusion );
}`,Im=`
vec3 geometryPosition = - vViewPosition;
vec3 geometryNormal = normal;
vec3 geometryViewDir = ( isOrthographic ) ? vec3( 0, 0, 1 ) : normalize( vViewPosition );
vec3 geometryClearcoatNormal = vec3( 0.0 );
#ifdef USE_CLEARCOAT
	geometryClearcoatNormal = clearcoatNormal;
#endif
#ifdef USE_IRIDESCENCE
	float dotNVi = saturate( dot( normal, geometryViewDir ) );
	if ( material.iridescenceThickness == 0.0 ) {
		material.iridescence = 0.0;
	} else {
		material.iridescence = saturate( material.iridescence );
	}
	if ( material.iridescence > 0.0 ) {
		material.iridescenceFresnel = evalIridescence( 1.0, material.iridescenceIOR, dotNVi, material.iridescenceThickness, material.specularColor );
		material.iridescenceF0 = Schlick_to_F0( material.iridescenceFresnel, 1.0, dotNVi );
	}
#endif
IncidentLight directLight;
#if ( NUM_POINT_LIGHTS > 0 ) && defined( RE_Direct )
	PointLight pointLight;
	#if defined( USE_SHADOWMAP ) && NUM_POINT_LIGHT_SHADOWS > 0
	PointLightShadow pointLightShadow;
	#endif
	#pragma unroll_loop_start
	for ( int i = 0; i < NUM_POINT_LIGHTS; i ++ ) {
		pointLight = pointLights[ i ];
		getPointLightInfo( pointLight, geometryPosition, directLight );
		#if defined( USE_SHADOWMAP ) && ( UNROLLED_LOOP_INDEX < NUM_POINT_LIGHT_SHADOWS )
		pointLightShadow = pointLightShadows[ i ];
		directLight.color *= ( directLight.visible && receiveShadow ) ? getPointShadow( pointShadowMap[ i ], pointLightShadow.shadowMapSize, pointLightShadow.shadowIntensity, pointLightShadow.shadowBias, pointLightShadow.shadowRadius, vPointShadowCoord[ i ], pointLightShadow.shadowCameraNear, pointLightShadow.shadowCameraFar ) : 1.0;
		#endif
		RE_Direct( directLight, geometryPosition, geometryNormal, geometryViewDir, geometryClearcoatNormal, material, reflectedLight );
	}
	#pragma unroll_loop_end
#endif
#if ( NUM_SPOT_LIGHTS > 0 ) && defined( RE_Direct )
	SpotLight spotLight;
	vec4 spotColor;
	vec3 spotLightCoord;
	bool inSpotLightMap;
	#if defined( USE_SHADOWMAP ) && NUM_SPOT_LIGHT_SHADOWS > 0
	SpotLightShadow spotLightShadow;
	#endif
	#pragma unroll_loop_start
	for ( int i = 0; i < NUM_SPOT_LIGHTS; i ++ ) {
		spotLight = spotLights[ i ];
		getSpotLightInfo( spotLight, geometryPosition, directLight );
		#if ( UNROLLED_LOOP_INDEX < NUM_SPOT_LIGHT_SHADOWS_WITH_MAPS )
		#define SPOT_LIGHT_MAP_INDEX UNROLLED_LOOP_INDEX
		#elif ( UNROLLED_LOOP_INDEX < NUM_SPOT_LIGHT_SHADOWS )
		#define SPOT_LIGHT_MAP_INDEX NUM_SPOT_LIGHT_MAPS
		#else
		#define SPOT_LIGHT_MAP_INDEX ( UNROLLED_LOOP_INDEX - NUM_SPOT_LIGHT_SHADOWS + NUM_SPOT_LIGHT_SHADOWS_WITH_MAPS )
		#endif
		#if ( SPOT_LIGHT_MAP_INDEX < NUM_SPOT_LIGHT_MAPS )
			spotLightCoord = vSpotLightCoord[ i ].xyz / vSpotLightCoord[ i ].w;
			inSpotLightMap = all( lessThan( abs( spotLightCoord * 2. - 1. ), vec3( 1.0 ) ) );
			spotColor = texture2D( spotLightMap[ SPOT_LIGHT_MAP_INDEX ], spotLightCoord.xy );
			directLight.color = inSpotLightMap ? directLight.color * spotColor.rgb : directLight.color;
		#endif
		#undef SPOT_LIGHT_MAP_INDEX
		#if defined( USE_SHADOWMAP ) && ( UNROLLED_LOOP_INDEX < NUM_SPOT_LIGHT_SHADOWS )
		spotLightShadow = spotLightShadows[ i ];
		directLight.color *= ( directLight.visible && receiveShadow ) ? getShadow( spotShadowMap[ i ], spotLightShadow.shadowMapSize, spotLightShadow.shadowIntensity, spotLightShadow.shadowBias, spotLightShadow.shadowRadius, vSpotLightCoord[ i ] ) : 1.0;
		#endif
		RE_Direct( directLight, geometryPosition, geometryNormal, geometryViewDir, geometryClearcoatNormal, material, reflectedLight );
	}
	#pragma unroll_loop_end
#endif
#if ( NUM_DIR_LIGHTS > 0 ) && defined( RE_Direct )
	DirectionalLight directionalLight;
	#if defined( USE_SHADOWMAP ) && NUM_DIR_LIGHT_SHADOWS > 0
	DirectionalLightShadow directionalLightShadow;
	#endif
	#pragma unroll_loop_start
	for ( int i = 0; i < NUM_DIR_LIGHTS; i ++ ) {
		directionalLight = directionalLights[ i ];
		getDirectionalLightInfo( directionalLight, directLight );
		#if defined( USE_SHADOWMAP ) && ( UNROLLED_LOOP_INDEX < NUM_DIR_LIGHT_SHADOWS )
		directionalLightShadow = directionalLightShadows[ i ];
		directLight.color *= ( directLight.visible && receiveShadow ) ? getShadow( directionalShadowMap[ i ], directionalLightShadow.shadowMapSize, directionalLightShadow.shadowIntensity, directionalLightShadow.shadowBias, directionalLightShadow.shadowRadius, vDirectionalShadowCoord[ i ] ) : 1.0;
		#endif
		RE_Direct( directLight, geometryPosition, geometryNormal, geometryViewDir, geometryClearcoatNormal, material, reflectedLight );
	}
	#pragma unroll_loop_end
#endif
#if ( NUM_RECT_AREA_LIGHTS > 0 ) && defined( RE_Direct_RectArea )
	RectAreaLight rectAreaLight;
	#pragma unroll_loop_start
	for ( int i = 0; i < NUM_RECT_AREA_LIGHTS; i ++ ) {
		rectAreaLight = rectAreaLights[ i ];
		RE_Direct_RectArea( rectAreaLight, geometryPosition, geometryNormal, geometryViewDir, geometryClearcoatNormal, material, reflectedLight );
	}
	#pragma unroll_loop_end
#endif
#if defined( RE_IndirectDiffuse )
	vec3 iblIrradiance = vec3( 0.0 );
	vec3 irradiance = getAmbientLightIrradiance( ambientLightColor );
	#if defined( USE_LIGHT_PROBES )
		irradiance += getLightProbeIrradiance( lightProbe, geometryNormal );
	#endif
	#if ( NUM_HEMI_LIGHTS > 0 )
		#pragma unroll_loop_start
		for ( int i = 0; i < NUM_HEMI_LIGHTS; i ++ ) {
			irradiance += getHemisphereLightIrradiance( hemisphereLights[ i ], geometryNormal );
		}
		#pragma unroll_loop_end
	#endif
#endif
#if defined( RE_IndirectSpecular )
	vec3 radiance = vec3( 0.0 );
	vec3 clearcoatRadiance = vec3( 0.0 );
#endif`,Pm=`#if defined( RE_IndirectDiffuse )
	#ifdef USE_LIGHTMAP
		vec4 lightMapTexel = texture2D( lightMap, vLightMapUv );
		vec3 lightMapIrradiance = lightMapTexel.rgb * lightMapIntensity;
		irradiance += lightMapIrradiance;
	#endif
	#if defined( USE_ENVMAP ) && defined( STANDARD ) && defined( ENVMAP_TYPE_CUBE_UV )
		iblIrradiance += getIBLIrradiance( geometryNormal );
	#endif
#endif
#if defined( USE_ENVMAP ) && defined( RE_IndirectSpecular )
	#ifdef USE_ANISOTROPY
		radiance += getIBLAnisotropyRadiance( geometryViewDir, geometryNormal, material.roughness, material.anisotropyB, material.anisotropy );
	#else
		radiance += getIBLRadiance( geometryViewDir, geometryNormal, material.roughness );
	#endif
	#ifdef USE_CLEARCOAT
		clearcoatRadiance += getIBLRadiance( geometryViewDir, geometryClearcoatNormal, material.clearcoatRoughness );
	#endif
#endif`,Lm=`#if defined( RE_IndirectDiffuse )
	RE_IndirectDiffuse( irradiance, geometryPosition, geometryNormal, geometryViewDir, geometryClearcoatNormal, material, reflectedLight );
#endif
#if defined( RE_IndirectSpecular )
	RE_IndirectSpecular( radiance, iblIrradiance, clearcoatRadiance, geometryPosition, geometryNormal, geometryViewDir, geometryClearcoatNormal, material, reflectedLight );
#endif`,Dm=`#if defined( USE_LOGDEPTHBUF )
	gl_FragDepth = vIsPerspective == 0.0 ? gl_FragCoord.z : log2( vFragDepth ) * logDepthBufFC * 0.5;
#endif`,Um=`#if defined( USE_LOGDEPTHBUF )
	uniform float logDepthBufFC;
	varying float vFragDepth;
	varying float vIsPerspective;
#endif`,Nm=`#ifdef USE_LOGDEPTHBUF
	varying float vFragDepth;
	varying float vIsPerspective;
#endif`,Bm=`#ifdef USE_LOGDEPTHBUF
	vFragDepth = 1.0 + gl_Position.w;
	vIsPerspective = float( isPerspectiveMatrix( projectionMatrix ) );
#endif`,zm=`#ifdef USE_MAP
	vec4 sampledDiffuseColor = texture2D( map, vMapUv );
	#ifdef DECODE_VIDEO_TEXTURE
		sampledDiffuseColor = sRGBTransferEOTF( sampledDiffuseColor );
	#endif
	diffuseColor *= sampledDiffuseColor;
#endif`,Om=`#ifdef USE_MAP
	uniform sampler2D map;
#endif`,Hm=`#if defined( USE_MAP ) || defined( USE_ALPHAMAP )
	#if defined( USE_POINTS_UV )
		vec2 uv = vUv;
	#else
		vec2 uv = ( uvTransform * vec3( gl_PointCoord.x, 1.0 - gl_PointCoord.y, 1 ) ).xy;
	#endif
#endif
#ifdef USE_MAP
	diffuseColor *= texture2D( map, uv );
#endif
#ifdef USE_ALPHAMAP
	diffuseColor.a *= texture2D( alphaMap, uv ).g;
#endif`,km=`#if defined( USE_POINTS_UV )
	varying vec2 vUv;
#else
	#if defined( USE_MAP ) || defined( USE_ALPHAMAP )
		uniform mat3 uvTransform;
	#endif
#endif
#ifdef USE_MAP
	uniform sampler2D map;
#endif
#ifdef USE_ALPHAMAP
	uniform sampler2D alphaMap;
#endif`,Gm=`float metalnessFactor = metalness;
#ifdef USE_METALNESSMAP
	vec4 texelMetalness = texture2D( metalnessMap, vMetalnessMapUv );
	metalnessFactor *= texelMetalness.b;
#endif`,Vm=`#ifdef USE_METALNESSMAP
	uniform sampler2D metalnessMap;
#endif`,Wm=`#ifdef USE_INSTANCING_MORPH
	float morphTargetInfluences[ MORPHTARGETS_COUNT ];
	float morphTargetBaseInfluence = texelFetch( morphTexture, ivec2( 0, gl_InstanceID ), 0 ).r;
	for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
		morphTargetInfluences[i] =  texelFetch( morphTexture, ivec2( i + 1, gl_InstanceID ), 0 ).r;
	}
#endif`,Xm=`#if defined( USE_MORPHCOLORS )
	vColor *= morphTargetBaseInfluence;
	for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
		#if defined( USE_COLOR_ALPHA )
			if ( morphTargetInfluences[ i ] != 0.0 ) vColor += getMorph( gl_VertexID, i, 2 ) * morphTargetInfluences[ i ];
		#elif defined( USE_COLOR )
			if ( morphTargetInfluences[ i ] != 0.0 ) vColor += getMorph( gl_VertexID, i, 2 ).rgb * morphTargetInfluences[ i ];
		#endif
	}
#endif`,qm=`#ifdef USE_MORPHNORMALS
	objectNormal *= morphTargetBaseInfluence;
	for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
		if ( morphTargetInfluences[ i ] != 0.0 ) objectNormal += getMorph( gl_VertexID, i, 1 ).xyz * morphTargetInfluences[ i ];
	}
#endif`,Ym=`#ifdef USE_MORPHTARGETS
	#ifndef USE_INSTANCING_MORPH
		uniform float morphTargetBaseInfluence;
		uniform float morphTargetInfluences[ MORPHTARGETS_COUNT ];
	#endif
	uniform sampler2DArray morphTargetsTexture;
	uniform ivec2 morphTargetsTextureSize;
	vec4 getMorph( const in int vertexIndex, const in int morphTargetIndex, const in int offset ) {
		int texelIndex = vertexIndex * MORPHTARGETS_TEXTURE_STRIDE + offset;
		int y = texelIndex / morphTargetsTextureSize.x;
		int x = texelIndex - y * morphTargetsTextureSize.x;
		ivec3 morphUV = ivec3( x, y, morphTargetIndex );
		return texelFetch( morphTargetsTexture, morphUV, 0 );
	}
#endif`,Zm=`#ifdef USE_MORPHTARGETS
	transformed *= morphTargetBaseInfluence;
	for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
		if ( morphTargetInfluences[ i ] != 0.0 ) transformed += getMorph( gl_VertexID, i, 0 ).xyz * morphTargetInfluences[ i ];
	}
#endif`,$m=`float faceDirection = gl_FrontFacing ? 1.0 : - 1.0;
#ifdef FLAT_SHADED
	vec3 fdx = dFdx( vViewPosition );
	vec3 fdy = dFdy( vViewPosition );
	vec3 normal = normalize( cross( fdx, fdy ) );
#else
	vec3 normal = normalize( vNormal );
	#ifdef DOUBLE_SIDED
		normal *= faceDirection;
	#endif
#endif
#if defined( USE_NORMALMAP_TANGENTSPACE ) || defined( USE_CLEARCOAT_NORMALMAP ) || defined( USE_ANISOTROPY )
	#ifdef USE_TANGENT
		mat3 tbn = mat3( normalize( vTangent ), normalize( vBitangent ), normal );
	#else
		mat3 tbn = getTangentFrame( - vViewPosition, normal,
		#if defined( USE_NORMALMAP )
			vNormalMapUv
		#elif defined( USE_CLEARCOAT_NORMALMAP )
			vClearcoatNormalMapUv
		#else
			vUv
		#endif
		);
	#endif
	#if defined( DOUBLE_SIDED ) && ! defined( FLAT_SHADED )
		tbn[0] *= faceDirection;
		tbn[1] *= faceDirection;
	#endif
#endif
#ifdef USE_CLEARCOAT_NORMALMAP
	#ifdef USE_TANGENT
		mat3 tbn2 = mat3( normalize( vTangent ), normalize( vBitangent ), normal );
	#else
		mat3 tbn2 = getTangentFrame( - vViewPosition, normal, vClearcoatNormalMapUv );
	#endif
	#if defined( DOUBLE_SIDED ) && ! defined( FLAT_SHADED )
		tbn2[0] *= faceDirection;
		tbn2[1] *= faceDirection;
	#endif
#endif
vec3 nonPerturbedNormal = normal;`,Jm=`#ifdef USE_NORMALMAP_OBJECTSPACE
	normal = texture2D( normalMap, vNormalMapUv ).xyz * 2.0 - 1.0;
	#ifdef FLIP_SIDED
		normal = - normal;
	#endif
	#ifdef DOUBLE_SIDED
		normal = normal * faceDirection;
	#endif
	normal = normalize( normalMatrix * normal );
#elif defined( USE_NORMALMAP_TANGENTSPACE )
	vec3 mapN = texture2D( normalMap, vNormalMapUv ).xyz * 2.0 - 1.0;
	mapN.xy *= normalScale;
	normal = normalize( tbn * mapN );
#elif defined( USE_BUMPMAP )
	normal = perturbNormalArb( - vViewPosition, normal, dHdxy_fwd(), faceDirection );
#endif`,Km=`#ifndef FLAT_SHADED
	varying vec3 vNormal;
	#ifdef USE_TANGENT
		varying vec3 vTangent;
		varying vec3 vBitangent;
	#endif
#endif`,jm=`#ifndef FLAT_SHADED
	varying vec3 vNormal;
	#ifdef USE_TANGENT
		varying vec3 vTangent;
		varying vec3 vBitangent;
	#endif
#endif`,Qm=`#ifndef FLAT_SHADED
	vNormal = normalize( transformedNormal );
	#ifdef USE_TANGENT
		vTangent = normalize( transformedTangent );
		vBitangent = normalize( cross( vNormal, vTangent ) * tangent.w );
	#endif
#endif`,t0=`#ifdef USE_NORMALMAP
	uniform sampler2D normalMap;
	uniform vec2 normalScale;
#endif
#ifdef USE_NORMALMAP_OBJECTSPACE
	uniform mat3 normalMatrix;
#endif
#if ! defined ( USE_TANGENT ) && ( defined ( USE_NORMALMAP_TANGENTSPACE ) || defined ( USE_CLEARCOAT_NORMALMAP ) || defined( USE_ANISOTROPY ) )
	mat3 getTangentFrame( vec3 eye_pos, vec3 surf_norm, vec2 uv ) {
		vec3 q0 = dFdx( eye_pos.xyz );
		vec3 q1 = dFdy( eye_pos.xyz );
		vec2 st0 = dFdx( uv.st );
		vec2 st1 = dFdy( uv.st );
		vec3 N = surf_norm;
		vec3 q1perp = cross( q1, N );
		vec3 q0perp = cross( N, q0 );
		vec3 T = q1perp * st0.x + q0perp * st1.x;
		vec3 B = q1perp * st0.y + q0perp * st1.y;
		float det = max( dot( T, T ), dot( B, B ) );
		float scale = ( det == 0.0 ) ? 0.0 : inversesqrt( det );
		return mat3( T * scale, B * scale, N );
	}
#endif`,e0=`#ifdef USE_CLEARCOAT
	vec3 clearcoatNormal = nonPerturbedNormal;
#endif`,n0=`#ifdef USE_CLEARCOAT_NORMALMAP
	vec3 clearcoatMapN = texture2D( clearcoatNormalMap, vClearcoatNormalMapUv ).xyz * 2.0 - 1.0;
	clearcoatMapN.xy *= clearcoatNormalScale;
	clearcoatNormal = normalize( tbn2 * clearcoatMapN );
#endif`,i0=`#ifdef USE_CLEARCOATMAP
	uniform sampler2D clearcoatMap;
#endif
#ifdef USE_CLEARCOAT_NORMALMAP
	uniform sampler2D clearcoatNormalMap;
	uniform vec2 clearcoatNormalScale;
#endif
#ifdef USE_CLEARCOAT_ROUGHNESSMAP
	uniform sampler2D clearcoatRoughnessMap;
#endif`,s0=`#ifdef USE_IRIDESCENCEMAP
	uniform sampler2D iridescenceMap;
#endif
#ifdef USE_IRIDESCENCE_THICKNESSMAP
	uniform sampler2D iridescenceThicknessMap;
#endif`,r0=`#ifdef OPAQUE
diffuseColor.a = 1.0;
#endif
#ifdef USE_TRANSMISSION
diffuseColor.a *= material.transmissionAlpha;
#endif
gl_FragColor = vec4( outgoingLight, diffuseColor.a );`,o0=`vec3 packNormalToRGB( const in vec3 normal ) {
	return normalize( normal ) * 0.5 + 0.5;
}
vec3 unpackRGBToNormal( const in vec3 rgb ) {
	return 2.0 * rgb.xyz - 1.0;
}
const float PackUpscale = 256. / 255.;const float UnpackDownscale = 255. / 256.;const float ShiftRight8 = 1. / 256.;
const float Inv255 = 1. / 255.;
const vec4 PackFactors = vec4( 1.0, 256.0, 256.0 * 256.0, 256.0 * 256.0 * 256.0 );
const vec2 UnpackFactors2 = vec2( UnpackDownscale, 1.0 / PackFactors.g );
const vec3 UnpackFactors3 = vec3( UnpackDownscale / PackFactors.rg, 1.0 / PackFactors.b );
const vec4 UnpackFactors4 = vec4( UnpackDownscale / PackFactors.rgb, 1.0 / PackFactors.a );
vec4 packDepthToRGBA( const in float v ) {
	if( v <= 0.0 )
		return vec4( 0., 0., 0., 0. );
	if( v >= 1.0 )
		return vec4( 1., 1., 1., 1. );
	float vuf;
	float af = modf( v * PackFactors.a, vuf );
	float bf = modf( vuf * ShiftRight8, vuf );
	float gf = modf( vuf * ShiftRight8, vuf );
	return vec4( vuf * Inv255, gf * PackUpscale, bf * PackUpscale, af );
}
vec3 packDepthToRGB( const in float v ) {
	if( v <= 0.0 )
		return vec3( 0., 0., 0. );
	if( v >= 1.0 )
		return vec3( 1., 1., 1. );
	float vuf;
	float bf = modf( v * PackFactors.b, vuf );
	float gf = modf( vuf * ShiftRight8, vuf );
	return vec3( vuf * Inv255, gf * PackUpscale, bf );
}
vec2 packDepthToRG( const in float v ) {
	if( v <= 0.0 )
		return vec2( 0., 0. );
	if( v >= 1.0 )
		return vec2( 1., 1. );
	float vuf;
	float gf = modf( v * 256., vuf );
	return vec2( vuf * Inv255, gf );
}
float unpackRGBAToDepth( const in vec4 v ) {
	return dot( v, UnpackFactors4 );
}
float unpackRGBToDepth( const in vec3 v ) {
	return dot( v, UnpackFactors3 );
}
float unpackRGToDepth( const in vec2 v ) {
	return v.r * UnpackFactors2.r + v.g * UnpackFactors2.g;
}
vec4 pack2HalfToRGBA( const in vec2 v ) {
	vec4 r = vec4( v.x, fract( v.x * 255.0 ), v.y, fract( v.y * 255.0 ) );
	return vec4( r.x - r.y / 255.0, r.y, r.z - r.w / 255.0, r.w );
}
vec2 unpackRGBATo2Half( const in vec4 v ) {
	return vec2( v.x + ( v.y / 255.0 ), v.z + ( v.w / 255.0 ) );
}
float viewZToOrthographicDepth( const in float viewZ, const in float near, const in float far ) {
	return ( viewZ + near ) / ( near - far );
}
float orthographicDepthToViewZ( const in float depth, const in float near, const in float far ) {
	return depth * ( near - far ) - near;
}
float viewZToPerspectiveDepth( const in float viewZ, const in float near, const in float far ) {
	return ( ( near + viewZ ) * far ) / ( ( far - near ) * viewZ );
}
float perspectiveDepthToViewZ( const in float depth, const in float near, const in float far ) {
	return ( near * far ) / ( ( far - near ) * depth - far );
}`,a0=`#ifdef PREMULTIPLIED_ALPHA
	gl_FragColor.rgb *= gl_FragColor.a;
#endif`,l0=`vec4 mvPosition = vec4( transformed, 1.0 );
#ifdef USE_BATCHING
	mvPosition = batchingMatrix * mvPosition;
#endif
#ifdef USE_INSTANCING
	mvPosition = instanceMatrix * mvPosition;
#endif
mvPosition = modelViewMatrix * mvPosition;
gl_Position = projectionMatrix * mvPosition;`,c0=`#ifdef DITHERING
	gl_FragColor.rgb = dithering( gl_FragColor.rgb );
#endif`,h0=`#ifdef DITHERING
	vec3 dithering( vec3 color ) {
		float grid_position = rand( gl_FragCoord.xy );
		vec3 dither_shift_RGB = vec3( 0.25 / 255.0, -0.25 / 255.0, 0.25 / 255.0 );
		dither_shift_RGB = mix( 2.0 * dither_shift_RGB, -2.0 * dither_shift_RGB, grid_position );
		return color + dither_shift_RGB;
	}
#endif`,u0=`float roughnessFactor = roughness;
#ifdef USE_ROUGHNESSMAP
	vec4 texelRoughness = texture2D( roughnessMap, vRoughnessMapUv );
	roughnessFactor *= texelRoughness.g;
#endif`,f0=`#ifdef USE_ROUGHNESSMAP
	uniform sampler2D roughnessMap;
#endif`,d0=`#if NUM_SPOT_LIGHT_COORDS > 0
	varying vec4 vSpotLightCoord[ NUM_SPOT_LIGHT_COORDS ];
#endif
#if NUM_SPOT_LIGHT_MAPS > 0
	uniform sampler2D spotLightMap[ NUM_SPOT_LIGHT_MAPS ];
#endif
#ifdef USE_SHADOWMAP
	#if NUM_DIR_LIGHT_SHADOWS > 0
		uniform sampler2D directionalShadowMap[ NUM_DIR_LIGHT_SHADOWS ];
		varying vec4 vDirectionalShadowCoord[ NUM_DIR_LIGHT_SHADOWS ];
		struct DirectionalLightShadow {
			float shadowIntensity;
			float shadowBias;
			float shadowNormalBias;
			float shadowRadius;
			vec2 shadowMapSize;
		};
		uniform DirectionalLightShadow directionalLightShadows[ NUM_DIR_LIGHT_SHADOWS ];
	#endif
	#if NUM_SPOT_LIGHT_SHADOWS > 0
		uniform sampler2D spotShadowMap[ NUM_SPOT_LIGHT_SHADOWS ];
		struct SpotLightShadow {
			float shadowIntensity;
			float shadowBias;
			float shadowNormalBias;
			float shadowRadius;
			vec2 shadowMapSize;
		};
		uniform SpotLightShadow spotLightShadows[ NUM_SPOT_LIGHT_SHADOWS ];
	#endif
	#if NUM_POINT_LIGHT_SHADOWS > 0
		uniform sampler2D pointShadowMap[ NUM_POINT_LIGHT_SHADOWS ];
		varying vec4 vPointShadowCoord[ NUM_POINT_LIGHT_SHADOWS ];
		struct PointLightShadow {
			float shadowIntensity;
			float shadowBias;
			float shadowNormalBias;
			float shadowRadius;
			vec2 shadowMapSize;
			float shadowCameraNear;
			float shadowCameraFar;
		};
		uniform PointLightShadow pointLightShadows[ NUM_POINT_LIGHT_SHADOWS ];
	#endif
	float texture2DCompare( sampler2D depths, vec2 uv, float compare ) {
		return step( compare, unpackRGBAToDepth( texture2D( depths, uv ) ) );
	}
	vec2 texture2DDistribution( sampler2D shadow, vec2 uv ) {
		return unpackRGBATo2Half( texture2D( shadow, uv ) );
	}
	float VSMShadow (sampler2D shadow, vec2 uv, float compare ){
		float occlusion = 1.0;
		vec2 distribution = texture2DDistribution( shadow, uv );
		float hard_shadow = step( compare , distribution.x );
		if (hard_shadow != 1.0 ) {
			float distance = compare - distribution.x ;
			float variance = max( 0.00000, distribution.y * distribution.y );
			float softness_probability = variance / (variance + distance * distance );			softness_probability = clamp( ( softness_probability - 0.3 ) / ( 0.95 - 0.3 ), 0.0, 1.0 );			occlusion = clamp( max( hard_shadow, softness_probability ), 0.0, 1.0 );
		}
		return occlusion;
	}
	float getShadow( sampler2D shadowMap, vec2 shadowMapSize, float shadowIntensity, float shadowBias, float shadowRadius, vec4 shadowCoord ) {
		float shadow = 1.0;
		shadowCoord.xyz /= shadowCoord.w;
		shadowCoord.z += shadowBias;
		bool inFrustum = shadowCoord.x >= 0.0 && shadowCoord.x <= 1.0 && shadowCoord.y >= 0.0 && shadowCoord.y <= 1.0;
		bool frustumTest = inFrustum && shadowCoord.z <= 1.0;
		if ( frustumTest ) {
		#if defined( SHADOWMAP_TYPE_PCF )
			vec2 texelSize = vec2( 1.0 ) / shadowMapSize;
			float dx0 = - texelSize.x * shadowRadius;
			float dy0 = - texelSize.y * shadowRadius;
			float dx1 = + texelSize.x * shadowRadius;
			float dy1 = + texelSize.y * shadowRadius;
			float dx2 = dx0 / 2.0;
			float dy2 = dy0 / 2.0;
			float dx3 = dx1 / 2.0;
			float dy3 = dy1 / 2.0;
			shadow = (
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx0, dy0 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( 0.0, dy0 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx1, dy0 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx2, dy2 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( 0.0, dy2 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx3, dy2 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx0, 0.0 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx2, 0.0 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy, shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx3, 0.0 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx1, 0.0 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx2, dy3 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( 0.0, dy3 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx3, dy3 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx0, dy1 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( 0.0, dy1 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, shadowCoord.xy + vec2( dx1, dy1 ), shadowCoord.z )
			) * ( 1.0 / 17.0 );
		#elif defined( SHADOWMAP_TYPE_PCF_SOFT )
			vec2 texelSize = vec2( 1.0 ) / shadowMapSize;
			float dx = texelSize.x;
			float dy = texelSize.y;
			vec2 uv = shadowCoord.xy;
			vec2 f = fract( uv * shadowMapSize + 0.5 );
			uv -= f * texelSize;
			shadow = (
				texture2DCompare( shadowMap, uv, shadowCoord.z ) +
				texture2DCompare( shadowMap, uv + vec2( dx, 0.0 ), shadowCoord.z ) +
				texture2DCompare( shadowMap, uv + vec2( 0.0, dy ), shadowCoord.z ) +
				texture2DCompare( shadowMap, uv + texelSize, shadowCoord.z ) +
				mix( texture2DCompare( shadowMap, uv + vec2( -dx, 0.0 ), shadowCoord.z ),
					 texture2DCompare( shadowMap, uv + vec2( 2.0 * dx, 0.0 ), shadowCoord.z ),
					 f.x ) +
				mix( texture2DCompare( shadowMap, uv + vec2( -dx, dy ), shadowCoord.z ),
					 texture2DCompare( shadowMap, uv + vec2( 2.0 * dx, dy ), shadowCoord.z ),
					 f.x ) +
				mix( texture2DCompare( shadowMap, uv + vec2( 0.0, -dy ), shadowCoord.z ),
					 texture2DCompare( shadowMap, uv + vec2( 0.0, 2.0 * dy ), shadowCoord.z ),
					 f.y ) +
				mix( texture2DCompare( shadowMap, uv + vec2( dx, -dy ), shadowCoord.z ),
					 texture2DCompare( shadowMap, uv + vec2( dx, 2.0 * dy ), shadowCoord.z ),
					 f.y ) +
				mix( mix( texture2DCompare( shadowMap, uv + vec2( -dx, -dy ), shadowCoord.z ),
						  texture2DCompare( shadowMap, uv + vec2( 2.0 * dx, -dy ), shadowCoord.z ),
						  f.x ),
					 mix( texture2DCompare( shadowMap, uv + vec2( -dx, 2.0 * dy ), shadowCoord.z ),
						  texture2DCompare( shadowMap, uv + vec2( 2.0 * dx, 2.0 * dy ), shadowCoord.z ),
						  f.x ),
					 f.y )
			) * ( 1.0 / 9.0 );
		#elif defined( SHADOWMAP_TYPE_VSM )
			shadow = VSMShadow( shadowMap, shadowCoord.xy, shadowCoord.z );
		#else
			shadow = texture2DCompare( shadowMap, shadowCoord.xy, shadowCoord.z );
		#endif
		}
		return mix( 1.0, shadow, shadowIntensity );
	}
	vec2 cubeToUV( vec3 v, float texelSizeY ) {
		vec3 absV = abs( v );
		float scaleToCube = 1.0 / max( absV.x, max( absV.y, absV.z ) );
		absV *= scaleToCube;
		v *= scaleToCube * ( 1.0 - 2.0 * texelSizeY );
		vec2 planar = v.xy;
		float almostATexel = 1.5 * texelSizeY;
		float almostOne = 1.0 - almostATexel;
		if ( absV.z >= almostOne ) {
			if ( v.z > 0.0 )
				planar.x = 4.0 - v.x;
		} else if ( absV.x >= almostOne ) {
			float signX = sign( v.x );
			planar.x = v.z * signX + 2.0 * signX;
		} else if ( absV.y >= almostOne ) {
			float signY = sign( v.y );
			planar.x = v.x + 2.0 * signY + 2.0;
			planar.y = v.z * signY - 2.0;
		}
		return vec2( 0.125, 0.25 ) * planar + vec2( 0.375, 0.75 );
	}
	float getPointShadow( sampler2D shadowMap, vec2 shadowMapSize, float shadowIntensity, float shadowBias, float shadowRadius, vec4 shadowCoord, float shadowCameraNear, float shadowCameraFar ) {
		float shadow = 1.0;
		vec3 lightToPosition = shadowCoord.xyz;
		
		float lightToPositionLength = length( lightToPosition );
		if ( lightToPositionLength - shadowCameraFar <= 0.0 && lightToPositionLength - shadowCameraNear >= 0.0 ) {
			float dp = ( lightToPositionLength - shadowCameraNear ) / ( shadowCameraFar - shadowCameraNear );			dp += shadowBias;
			vec3 bd3D = normalize( lightToPosition );
			vec2 texelSize = vec2( 1.0 ) / ( shadowMapSize * vec2( 4.0, 2.0 ) );
			#if defined( SHADOWMAP_TYPE_PCF ) || defined( SHADOWMAP_TYPE_PCF_SOFT ) || defined( SHADOWMAP_TYPE_VSM )
				vec2 offset = vec2( - 1, 1 ) * shadowRadius * texelSize.y;
				shadow = (
					texture2DCompare( shadowMap, cubeToUV( bd3D + offset.xyy, texelSize.y ), dp ) +
					texture2DCompare( shadowMap, cubeToUV( bd3D + offset.yyy, texelSize.y ), dp ) +
					texture2DCompare( shadowMap, cubeToUV( bd3D + offset.xyx, texelSize.y ), dp ) +
					texture2DCompare( shadowMap, cubeToUV( bd3D + offset.yyx, texelSize.y ), dp ) +
					texture2DCompare( shadowMap, cubeToUV( bd3D, texelSize.y ), dp ) +
					texture2DCompare( shadowMap, cubeToUV( bd3D + offset.xxy, texelSize.y ), dp ) +
					texture2DCompare( shadowMap, cubeToUV( bd3D + offset.yxy, texelSize.y ), dp ) +
					texture2DCompare( shadowMap, cubeToUV( bd3D + offset.xxx, texelSize.y ), dp ) +
					texture2DCompare( shadowMap, cubeToUV( bd3D + offset.yxx, texelSize.y ), dp )
				) * ( 1.0 / 9.0 );
			#else
				shadow = texture2DCompare( shadowMap, cubeToUV( bd3D, texelSize.y ), dp );
			#endif
		}
		return mix( 1.0, shadow, shadowIntensity );
	}
#endif`,p0=`#if NUM_SPOT_LIGHT_COORDS > 0
	uniform mat4 spotLightMatrix[ NUM_SPOT_LIGHT_COORDS ];
	varying vec4 vSpotLightCoord[ NUM_SPOT_LIGHT_COORDS ];
#endif
#ifdef USE_SHADOWMAP
	#if NUM_DIR_LIGHT_SHADOWS > 0
		uniform mat4 directionalShadowMatrix[ NUM_DIR_LIGHT_SHADOWS ];
		varying vec4 vDirectionalShadowCoord[ NUM_DIR_LIGHT_SHADOWS ];
		struct DirectionalLightShadow {
			float shadowIntensity;
			float shadowBias;
			float shadowNormalBias;
			float shadowRadius;
			vec2 shadowMapSize;
		};
		uniform DirectionalLightShadow directionalLightShadows[ NUM_DIR_LIGHT_SHADOWS ];
	#endif
	#if NUM_SPOT_LIGHT_SHADOWS > 0
		struct SpotLightShadow {
			float shadowIntensity;
			float shadowBias;
			float shadowNormalBias;
			float shadowRadius;
			vec2 shadowMapSize;
		};
		uniform SpotLightShadow spotLightShadows[ NUM_SPOT_LIGHT_SHADOWS ];
	#endif
	#if NUM_POINT_LIGHT_SHADOWS > 0
		uniform mat4 pointShadowMatrix[ NUM_POINT_LIGHT_SHADOWS ];
		varying vec4 vPointShadowCoord[ NUM_POINT_LIGHT_SHADOWS ];
		struct PointLightShadow {
			float shadowIntensity;
			float shadowBias;
			float shadowNormalBias;
			float shadowRadius;
			vec2 shadowMapSize;
			float shadowCameraNear;
			float shadowCameraFar;
		};
		uniform PointLightShadow pointLightShadows[ NUM_POINT_LIGHT_SHADOWS ];
	#endif
#endif`,m0=`#if ( defined( USE_SHADOWMAP ) && ( NUM_DIR_LIGHT_SHADOWS > 0 || NUM_POINT_LIGHT_SHADOWS > 0 ) ) || ( NUM_SPOT_LIGHT_COORDS > 0 )
	vec3 shadowWorldNormal = inverseTransformDirection( transformedNormal, viewMatrix );
	vec4 shadowWorldPosition;
#endif
#if defined( USE_SHADOWMAP )
	#if NUM_DIR_LIGHT_SHADOWS > 0
		#pragma unroll_loop_start
		for ( int i = 0; i < NUM_DIR_LIGHT_SHADOWS; i ++ ) {
			shadowWorldPosition = worldPosition + vec4( shadowWorldNormal * directionalLightShadows[ i ].shadowNormalBias, 0 );
			vDirectionalShadowCoord[ i ] = directionalShadowMatrix[ i ] * shadowWorldPosition;
		}
		#pragma unroll_loop_end
	#endif
	#if NUM_POINT_LIGHT_SHADOWS > 0
		#pragma unroll_loop_start
		for ( int i = 0; i < NUM_POINT_LIGHT_SHADOWS; i ++ ) {
			shadowWorldPosition = worldPosition + vec4( shadowWorldNormal * pointLightShadows[ i ].shadowNormalBias, 0 );
			vPointShadowCoord[ i ] = pointShadowMatrix[ i ] * shadowWorldPosition;
		}
		#pragma unroll_loop_end
	#endif
#endif
#if NUM_SPOT_LIGHT_COORDS > 0
	#pragma unroll_loop_start
	for ( int i = 0; i < NUM_SPOT_LIGHT_COORDS; i ++ ) {
		shadowWorldPosition = worldPosition;
		#if ( defined( USE_SHADOWMAP ) && UNROLLED_LOOP_INDEX < NUM_SPOT_LIGHT_SHADOWS )
			shadowWorldPosition.xyz += shadowWorldNormal * spotLightShadows[ i ].shadowNormalBias;
		#endif
		vSpotLightCoord[ i ] = spotLightMatrix[ i ] * shadowWorldPosition;
	}
	#pragma unroll_loop_end
#endif`,g0=`float getShadowMask() {
	float shadow = 1.0;
	#ifdef USE_SHADOWMAP
	#if NUM_DIR_LIGHT_SHADOWS > 0
	DirectionalLightShadow directionalLight;
	#pragma unroll_loop_start
	for ( int i = 0; i < NUM_DIR_LIGHT_SHADOWS; i ++ ) {
		directionalLight = directionalLightShadows[ i ];
		shadow *= receiveShadow ? getShadow( directionalShadowMap[ i ], directionalLight.shadowMapSize, directionalLight.shadowIntensity, directionalLight.shadowBias, directionalLight.shadowRadius, vDirectionalShadowCoord[ i ] ) : 1.0;
	}
	#pragma unroll_loop_end
	#endif
	#if NUM_SPOT_LIGHT_SHADOWS > 0
	SpotLightShadow spotLight;
	#pragma unroll_loop_start
	for ( int i = 0; i < NUM_SPOT_LIGHT_SHADOWS; i ++ ) {
		spotLight = spotLightShadows[ i ];
		shadow *= receiveShadow ? getShadow( spotShadowMap[ i ], spotLight.shadowMapSize, spotLight.shadowIntensity, spotLight.shadowBias, spotLight.shadowRadius, vSpotLightCoord[ i ] ) : 1.0;
	}
	#pragma unroll_loop_end
	#endif
	#if NUM_POINT_LIGHT_SHADOWS > 0
	PointLightShadow pointLight;
	#pragma unroll_loop_start
	for ( int i = 0; i < NUM_POINT_LIGHT_SHADOWS; i ++ ) {
		pointLight = pointLightShadows[ i ];
		shadow *= receiveShadow ? getPointShadow( pointShadowMap[ i ], pointLight.shadowMapSize, pointLight.shadowIntensity, pointLight.shadowBias, pointLight.shadowRadius, vPointShadowCoord[ i ], pointLight.shadowCameraNear, pointLight.shadowCameraFar ) : 1.0;
	}
	#pragma unroll_loop_end
	#endif
	#endif
	return shadow;
}`,x0=`#ifdef USE_SKINNING
	mat4 boneMatX = getBoneMatrix( skinIndex.x );
	mat4 boneMatY = getBoneMatrix( skinIndex.y );
	mat4 boneMatZ = getBoneMatrix( skinIndex.z );
	mat4 boneMatW = getBoneMatrix( skinIndex.w );
#endif`,y0=`#ifdef USE_SKINNING
	uniform mat4 bindMatrix;
	uniform mat4 bindMatrixInverse;
	uniform highp sampler2D boneTexture;
	mat4 getBoneMatrix( const in float i ) {
		int size = textureSize( boneTexture, 0 ).x;
		int j = int( i ) * 4;
		int x = j % size;
		int y = j / size;
		vec4 v1 = texelFetch( boneTexture, ivec2( x, y ), 0 );
		vec4 v2 = texelFetch( boneTexture, ivec2( x + 1, y ), 0 );
		vec4 v3 = texelFetch( boneTexture, ivec2( x + 2, y ), 0 );
		vec4 v4 = texelFetch( boneTexture, ivec2( x + 3, y ), 0 );
		return mat4( v1, v2, v3, v4 );
	}
#endif`,_0=`#ifdef USE_SKINNING
	vec4 skinVertex = bindMatrix * vec4( transformed, 1.0 );
	vec4 skinned = vec4( 0.0 );
	skinned += boneMatX * skinVertex * skinWeight.x;
	skinned += boneMatY * skinVertex * skinWeight.y;
	skinned += boneMatZ * skinVertex * skinWeight.z;
	skinned += boneMatW * skinVertex * skinWeight.w;
	transformed = ( bindMatrixInverse * skinned ).xyz;
#endif`,v0=`#ifdef USE_SKINNING
	mat4 skinMatrix = mat4( 0.0 );
	skinMatrix += skinWeight.x * boneMatX;
	skinMatrix += skinWeight.y * boneMatY;
	skinMatrix += skinWeight.z * boneMatZ;
	skinMatrix += skinWeight.w * boneMatW;
	skinMatrix = bindMatrixInverse * skinMatrix * bindMatrix;
	objectNormal = vec4( skinMatrix * vec4( objectNormal, 0.0 ) ).xyz;
	#ifdef USE_TANGENT
		objectTangent = vec4( skinMatrix * vec4( objectTangent, 0.0 ) ).xyz;
	#endif
#endif`,M0=`float specularStrength;
#ifdef USE_SPECULARMAP
	vec4 texelSpecular = texture2D( specularMap, vSpecularMapUv );
	specularStrength = texelSpecular.r;
#else
	specularStrength = 1.0;
#endif`,E0=`#ifdef USE_SPECULARMAP
	uniform sampler2D specularMap;
#endif`,S0=`#if defined( TONE_MAPPING )
	gl_FragColor.rgb = toneMapping( gl_FragColor.rgb );
#endif`,T0=`#ifndef saturate
#define saturate( a ) clamp( a, 0.0, 1.0 )
#endif
uniform float toneMappingExposure;
vec3 LinearToneMapping( vec3 color ) {
	return saturate( toneMappingExposure * color );
}
vec3 ReinhardToneMapping( vec3 color ) {
	color *= toneMappingExposure;
	return saturate( color / ( vec3( 1.0 ) + color ) );
}
vec3 CineonToneMapping( vec3 color ) {
	color *= toneMappingExposure;
	color = max( vec3( 0.0 ), color - 0.004 );
	return pow( ( color * ( 6.2 * color + 0.5 ) ) / ( color * ( 6.2 * color + 1.7 ) + 0.06 ), vec3( 2.2 ) );
}
vec3 RRTAndODTFit( vec3 v ) {
	vec3 a = v * ( v + 0.0245786 ) - 0.000090537;
	vec3 b = v * ( 0.983729 * v + 0.4329510 ) + 0.238081;
	return a / b;
}
vec3 ACESFilmicToneMapping( vec3 color ) {
	const mat3 ACESInputMat = mat3(
		vec3( 0.59719, 0.07600, 0.02840 ),		vec3( 0.35458, 0.90834, 0.13383 ),
		vec3( 0.04823, 0.01566, 0.83777 )
	);
	const mat3 ACESOutputMat = mat3(
		vec3(  1.60475, -0.10208, -0.00327 ),		vec3( -0.53108,  1.10813, -0.07276 ),
		vec3( -0.07367, -0.00605,  1.07602 )
	);
	color *= toneMappingExposure / 0.6;
	color = ACESInputMat * color;
	color = RRTAndODTFit( color );
	color = ACESOutputMat * color;
	return saturate( color );
}
const mat3 LINEAR_REC2020_TO_LINEAR_SRGB = mat3(
	vec3( 1.6605, - 0.1246, - 0.0182 ),
	vec3( - 0.5876, 1.1329, - 0.1006 ),
	vec3( - 0.0728, - 0.0083, 1.1187 )
);
const mat3 LINEAR_SRGB_TO_LINEAR_REC2020 = mat3(
	vec3( 0.6274, 0.0691, 0.0164 ),
	vec3( 0.3293, 0.9195, 0.0880 ),
	vec3( 0.0433, 0.0113, 0.8956 )
);
vec3 agxDefaultContrastApprox( vec3 x ) {
	vec3 x2 = x * x;
	vec3 x4 = x2 * x2;
	return + 15.5 * x4 * x2
		- 40.14 * x4 * x
		+ 31.96 * x4
		- 6.868 * x2 * x
		+ 0.4298 * x2
		+ 0.1191 * x
		- 0.00232;
}
vec3 AgXToneMapping( vec3 color ) {
	const mat3 AgXInsetMatrix = mat3(
		vec3( 0.856627153315983, 0.137318972929847, 0.11189821299995 ),
		vec3( 0.0951212405381588, 0.761241990602591, 0.0767994186031903 ),
		vec3( 0.0482516061458583, 0.101439036467562, 0.811302368396859 )
	);
	const mat3 AgXOutsetMatrix = mat3(
		vec3( 1.1271005818144368, - 0.1413297634984383, - 0.14132976349843826 ),
		vec3( - 0.11060664309660323, 1.157823702216272, - 0.11060664309660294 ),
		vec3( - 0.016493938717834573, - 0.016493938717834257, 1.2519364065950405 )
	);
	const float AgxMinEv = - 12.47393;	const float AgxMaxEv = 4.026069;
	color *= toneMappingExposure;
	color = LINEAR_SRGB_TO_LINEAR_REC2020 * color;
	color = AgXInsetMatrix * color;
	color = max( color, 1e-10 );	color = log2( color );
	color = ( color - AgxMinEv ) / ( AgxMaxEv - AgxMinEv );
	color = clamp( color, 0.0, 1.0 );
	color = agxDefaultContrastApprox( color );
	color = AgXOutsetMatrix * color;
	color = pow( max( vec3( 0.0 ), color ), vec3( 2.2 ) );
	color = LINEAR_REC2020_TO_LINEAR_SRGB * color;
	color = clamp( color, 0.0, 1.0 );
	return color;
}
vec3 NeutralToneMapping( vec3 color ) {
	const float StartCompression = 0.8 - 0.04;
	const float Desaturation = 0.15;
	color *= toneMappingExposure;
	float x = min( color.r, min( color.g, color.b ) );
	float offset = x < 0.08 ? x - 6.25 * x * x : 0.04;
	color -= offset;
	float peak = max( color.r, max( color.g, color.b ) );
	if ( peak < StartCompression ) return color;
	float d = 1. - StartCompression;
	float newPeak = 1. - d * d / ( peak + d - StartCompression );
	color *= newPeak / peak;
	float g = 1. - 1. / ( Desaturation * ( peak - newPeak ) + 1. );
	return mix( color, vec3( newPeak ), g );
}
vec3 CustomToneMapping( vec3 color ) { return color; }`,w0=`#ifdef USE_TRANSMISSION
	material.transmission = transmission;
	material.transmissionAlpha = 1.0;
	material.thickness = thickness;
	material.attenuationDistance = attenuationDistance;
	material.attenuationColor = attenuationColor;
	#ifdef USE_TRANSMISSIONMAP
		material.transmission *= texture2D( transmissionMap, vTransmissionMapUv ).r;
	#endif
	#ifdef USE_THICKNESSMAP
		material.thickness *= texture2D( thicknessMap, vThicknessMapUv ).g;
	#endif
	vec3 pos = vWorldPosition;
	vec3 v = normalize( cameraPosition - pos );
	vec3 n = inverseTransformDirection( normal, viewMatrix );
	vec4 transmitted = getIBLVolumeRefraction(
		n, v, material.roughness, material.diffuseColor, material.specularColor, material.specularF90,
		pos, modelMatrix, viewMatrix, projectionMatrix, material.dispersion, material.ior, material.thickness,
		material.attenuationColor, material.attenuationDistance );
	material.transmissionAlpha = mix( material.transmissionAlpha, transmitted.a, material.transmission );
	totalDiffuse = mix( totalDiffuse, transmitted.rgb, material.transmission );
#endif`,b0=`#ifdef USE_TRANSMISSION
	uniform float transmission;
	uniform float thickness;
	uniform float attenuationDistance;
	uniform vec3 attenuationColor;
	#ifdef USE_TRANSMISSIONMAP
		uniform sampler2D transmissionMap;
	#endif
	#ifdef USE_THICKNESSMAP
		uniform sampler2D thicknessMap;
	#endif
	uniform vec2 transmissionSamplerSize;
	uniform sampler2D transmissionSamplerMap;
	uniform mat4 modelMatrix;
	uniform mat4 projectionMatrix;
	varying vec3 vWorldPosition;
	float w0( float a ) {
		return ( 1.0 / 6.0 ) * ( a * ( a * ( - a + 3.0 ) - 3.0 ) + 1.0 );
	}
	float w1( float a ) {
		return ( 1.0 / 6.0 ) * ( a *  a * ( 3.0 * a - 6.0 ) + 4.0 );
	}
	float w2( float a ){
		return ( 1.0 / 6.0 ) * ( a * ( a * ( - 3.0 * a + 3.0 ) + 3.0 ) + 1.0 );
	}
	float w3( float a ) {
		return ( 1.0 / 6.0 ) * ( a * a * a );
	}
	float g0( float a ) {
		return w0( a ) + w1( a );
	}
	float g1( float a ) {
		return w2( a ) + w3( a );
	}
	float h0( float a ) {
		return - 1.0 + w1( a ) / ( w0( a ) + w1( a ) );
	}
	float h1( float a ) {
		return 1.0 + w3( a ) / ( w2( a ) + w3( a ) );
	}
	vec4 bicubic( sampler2D tex, vec2 uv, vec4 texelSize, float lod ) {
		uv = uv * texelSize.zw + 0.5;
		vec2 iuv = floor( uv );
		vec2 fuv = fract( uv );
		float g0x = g0( fuv.x );
		float g1x = g1( fuv.x );
		float h0x = h0( fuv.x );
		float h1x = h1( fuv.x );
		float h0y = h0( fuv.y );
		float h1y = h1( fuv.y );
		vec2 p0 = ( vec2( iuv.x + h0x, iuv.y + h0y ) - 0.5 ) * texelSize.xy;
		vec2 p1 = ( vec2( iuv.x + h1x, iuv.y + h0y ) - 0.5 ) * texelSize.xy;
		vec2 p2 = ( vec2( iuv.x + h0x, iuv.y + h1y ) - 0.5 ) * texelSize.xy;
		vec2 p3 = ( vec2( iuv.x + h1x, iuv.y + h1y ) - 0.5 ) * texelSize.xy;
		return g0( fuv.y ) * ( g0x * textureLod( tex, p0, lod ) + g1x * textureLod( tex, p1, lod ) ) +
			g1( fuv.y ) * ( g0x * textureLod( tex, p2, lod ) + g1x * textureLod( tex, p3, lod ) );
	}
	vec4 textureBicubic( sampler2D sampler, vec2 uv, float lod ) {
		vec2 fLodSize = vec2( textureSize( sampler, int( lod ) ) );
		vec2 cLodSize = vec2( textureSize( sampler, int( lod + 1.0 ) ) );
		vec2 fLodSizeInv = 1.0 / fLodSize;
		vec2 cLodSizeInv = 1.0 / cLodSize;
		vec4 fSample = bicubic( sampler, uv, vec4( fLodSizeInv, fLodSize ), floor( lod ) );
		vec4 cSample = bicubic( sampler, uv, vec4( cLodSizeInv, cLodSize ), ceil( lod ) );
		return mix( fSample, cSample, fract( lod ) );
	}
	vec3 getVolumeTransmissionRay( const in vec3 n, const in vec3 v, const in float thickness, const in float ior, const in mat4 modelMatrix ) {
		vec3 refractionVector = refract( - v, normalize( n ), 1.0 / ior );
		vec3 modelScale;
		modelScale.x = length( vec3( modelMatrix[ 0 ].xyz ) );
		modelScale.y = length( vec3( modelMatrix[ 1 ].xyz ) );
		modelScale.z = length( vec3( modelMatrix[ 2 ].xyz ) );
		return normalize( refractionVector ) * thickness * modelScale;
	}
	float applyIorToRoughness( const in float roughness, const in float ior ) {
		return roughness * clamp( ior * 2.0 - 2.0, 0.0, 1.0 );
	}
	vec4 getTransmissionSample( const in vec2 fragCoord, const in float roughness, const in float ior ) {
		float lod = log2( transmissionSamplerSize.x ) * applyIorToRoughness( roughness, ior );
		return textureBicubic( transmissionSamplerMap, fragCoord.xy, lod );
	}
	vec3 volumeAttenuation( const in float transmissionDistance, const in vec3 attenuationColor, const in float attenuationDistance ) {
		if ( isinf( attenuationDistance ) ) {
			return vec3( 1.0 );
		} else {
			vec3 attenuationCoefficient = -log( attenuationColor ) / attenuationDistance;
			vec3 transmittance = exp( - attenuationCoefficient * transmissionDistance );			return transmittance;
		}
	}
	vec4 getIBLVolumeRefraction( const in vec3 n, const in vec3 v, const in float roughness, const in vec3 diffuseColor,
		const in vec3 specularColor, const in float specularF90, const in vec3 position, const in mat4 modelMatrix,
		const in mat4 viewMatrix, const in mat4 projMatrix, const in float dispersion, const in float ior, const in float thickness,
		const in vec3 attenuationColor, const in float attenuationDistance ) {
		vec4 transmittedLight;
		vec3 transmittance;
		#ifdef USE_DISPERSION
			float halfSpread = ( ior - 1.0 ) * 0.025 * dispersion;
			vec3 iors = vec3( ior - halfSpread, ior, ior + halfSpread );
			for ( int i = 0; i < 3; i ++ ) {
				vec3 transmissionRay = getVolumeTransmissionRay( n, v, thickness, iors[ i ], modelMatrix );
				vec3 refractedRayExit = position + transmissionRay;
		
				vec4 ndcPos = projMatrix * viewMatrix * vec4( refractedRayExit, 1.0 );
				vec2 refractionCoords = ndcPos.xy / ndcPos.w;
				refractionCoords += 1.0;
				refractionCoords /= 2.0;
		
				vec4 transmissionSample = getTransmissionSample( refractionCoords, roughness, iors[ i ] );
				transmittedLight[ i ] = transmissionSample[ i ];
				transmittedLight.a += transmissionSample.a;
				transmittance[ i ] = diffuseColor[ i ] * volumeAttenuation( length( transmissionRay ), attenuationColor, attenuationDistance )[ i ];
			}
			transmittedLight.a /= 3.0;
		
		#else
		
			vec3 transmissionRay = getVolumeTransmissionRay( n, v, thickness, ior, modelMatrix );
			vec3 refractedRayExit = position + transmissionRay;
			vec4 ndcPos = projMatrix * viewMatrix * vec4( refractedRayExit, 1.0 );
			vec2 refractionCoords = ndcPos.xy / ndcPos.w;
			refractionCoords += 1.0;
			refractionCoords /= 2.0;
			transmittedLight = getTransmissionSample( refractionCoords, roughness, ior );
			transmittance = diffuseColor * volumeAttenuation( length( transmissionRay ), attenuationColor, attenuationDistance );
		
		#endif
		vec3 attenuatedColor = transmittance * transmittedLight.rgb;
		vec3 F = EnvironmentBRDF( n, v, specularColor, specularF90, roughness );
		float transmittanceFactor = ( transmittance.r + transmittance.g + transmittance.b ) / 3.0;
		return vec4( ( 1.0 - F ) * attenuatedColor, 1.0 - ( 1.0 - transmittedLight.a ) * transmittanceFactor );
	}
#endif`,A0=`#if defined( USE_UV ) || defined( USE_ANISOTROPY )
	varying vec2 vUv;
#endif
#ifdef USE_MAP
	varying vec2 vMapUv;
#endif
#ifdef USE_ALPHAMAP
	varying vec2 vAlphaMapUv;
#endif
#ifdef USE_LIGHTMAP
	varying vec2 vLightMapUv;
#endif
#ifdef USE_AOMAP
	varying vec2 vAoMapUv;
#endif
#ifdef USE_BUMPMAP
	varying vec2 vBumpMapUv;
#endif
#ifdef USE_NORMALMAP
	varying vec2 vNormalMapUv;
#endif
#ifdef USE_EMISSIVEMAP
	varying vec2 vEmissiveMapUv;
#endif
#ifdef USE_METALNESSMAP
	varying vec2 vMetalnessMapUv;
#endif
#ifdef USE_ROUGHNESSMAP
	varying vec2 vRoughnessMapUv;
#endif
#ifdef USE_ANISOTROPYMAP
	varying vec2 vAnisotropyMapUv;
#endif
#ifdef USE_CLEARCOATMAP
	varying vec2 vClearcoatMapUv;
#endif
#ifdef USE_CLEARCOAT_NORMALMAP
	varying vec2 vClearcoatNormalMapUv;
#endif
#ifdef USE_CLEARCOAT_ROUGHNESSMAP
	varying vec2 vClearcoatRoughnessMapUv;
#endif
#ifdef USE_IRIDESCENCEMAP
	varying vec2 vIridescenceMapUv;
#endif
#ifdef USE_IRIDESCENCE_THICKNESSMAP
	varying vec2 vIridescenceThicknessMapUv;
#endif
#ifdef USE_SHEEN_COLORMAP
	varying vec2 vSheenColorMapUv;
#endif
#ifdef USE_SHEEN_ROUGHNESSMAP
	varying vec2 vSheenRoughnessMapUv;
#endif
#ifdef USE_SPECULARMAP
	varying vec2 vSpecularMapUv;
#endif
#ifdef USE_SPECULAR_COLORMAP
	varying vec2 vSpecularColorMapUv;
#endif
#ifdef USE_SPECULAR_INTENSITYMAP
	varying vec2 vSpecularIntensityMapUv;
#endif
#ifdef USE_TRANSMISSIONMAP
	uniform mat3 transmissionMapTransform;
	varying vec2 vTransmissionMapUv;
#endif
#ifdef USE_THICKNESSMAP
	uniform mat3 thicknessMapTransform;
	varying vec2 vThicknessMapUv;
#endif`,R0=`#if defined( USE_UV ) || defined( USE_ANISOTROPY )
	varying vec2 vUv;
#endif
#ifdef USE_MAP
	uniform mat3 mapTransform;
	varying vec2 vMapUv;
#endif
#ifdef USE_ALPHAMAP
	uniform mat3 alphaMapTransform;
	varying vec2 vAlphaMapUv;
#endif
#ifdef USE_LIGHTMAP
	uniform mat3 lightMapTransform;
	varying vec2 vLightMapUv;
#endif
#ifdef USE_AOMAP
	uniform mat3 aoMapTransform;
	varying vec2 vAoMapUv;
#endif
#ifdef USE_BUMPMAP
	uniform mat3 bumpMapTransform;
	varying vec2 vBumpMapUv;
#endif
#ifdef USE_NORMALMAP
	uniform mat3 normalMapTransform;
	varying vec2 vNormalMapUv;
#endif
#ifdef USE_DISPLACEMENTMAP
	uniform mat3 displacementMapTransform;
	varying vec2 vDisplacementMapUv;
#endif
#ifdef USE_EMISSIVEMAP
	uniform mat3 emissiveMapTransform;
	varying vec2 vEmissiveMapUv;
#endif
#ifdef USE_METALNESSMAP
	uniform mat3 metalnessMapTransform;
	varying vec2 vMetalnessMapUv;
#endif
#ifdef USE_ROUGHNESSMAP
	uniform mat3 roughnessMapTransform;
	varying vec2 vRoughnessMapUv;
#endif
#ifdef USE_ANISOTROPYMAP
	uniform mat3 anisotropyMapTransform;
	varying vec2 vAnisotropyMapUv;
#endif
#ifdef USE_CLEARCOATMAP
	uniform mat3 clearcoatMapTransform;
	varying vec2 vClearcoatMapUv;
#endif
#ifdef USE_CLEARCOAT_NORMALMAP
	uniform mat3 clearcoatNormalMapTransform;
	varying vec2 vClearcoatNormalMapUv;
#endif
#ifdef USE_CLEARCOAT_ROUGHNESSMAP
	uniform mat3 clearcoatRoughnessMapTransform;
	varying vec2 vClearcoatRoughnessMapUv;
#endif
#ifdef USE_SHEEN_COLORMAP
	uniform mat3 sheenColorMapTransform;
	varying vec2 vSheenColorMapUv;
#endif
#ifdef USE_SHEEN_ROUGHNESSMAP
	uniform mat3 sheenRoughnessMapTransform;
	varying vec2 vSheenRoughnessMapUv;
#endif
#ifdef USE_IRIDESCENCEMAP
	uniform mat3 iridescenceMapTransform;
	varying vec2 vIridescenceMapUv;
#endif
#ifdef USE_IRIDESCENCE_THICKNESSMAP
	uniform mat3 iridescenceThicknessMapTransform;
	varying vec2 vIridescenceThicknessMapUv;
#endif
#ifdef USE_SPECULARMAP
	uniform mat3 specularMapTransform;
	varying vec2 vSpecularMapUv;
#endif
#ifdef USE_SPECULAR_COLORMAP
	uniform mat3 specularColorMapTransform;
	varying vec2 vSpecularColorMapUv;
#endif
#ifdef USE_SPECULAR_INTENSITYMAP
	uniform mat3 specularIntensityMapTransform;
	varying vec2 vSpecularIntensityMapUv;
#endif
#ifdef USE_TRANSMISSIONMAP
	uniform mat3 transmissionMapTransform;
	varying vec2 vTransmissionMapUv;
#endif
#ifdef USE_THICKNESSMAP
	uniform mat3 thicknessMapTransform;
	varying vec2 vThicknessMapUv;
#endif`,C0=`#if defined( USE_UV ) || defined( USE_ANISOTROPY )
	vUv = vec3( uv, 1 ).xy;
#endif
#ifdef USE_MAP
	vMapUv = ( mapTransform * vec3( MAP_UV, 1 ) ).xy;
#endif
#ifdef USE_ALPHAMAP
	vAlphaMapUv = ( alphaMapTransform * vec3( ALPHAMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_LIGHTMAP
	vLightMapUv = ( lightMapTransform * vec3( LIGHTMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_AOMAP
	vAoMapUv = ( aoMapTransform * vec3( AOMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_BUMPMAP
	vBumpMapUv = ( bumpMapTransform * vec3( BUMPMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_NORMALMAP
	vNormalMapUv = ( normalMapTransform * vec3( NORMALMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_DISPLACEMENTMAP
	vDisplacementMapUv = ( displacementMapTransform * vec3( DISPLACEMENTMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_EMISSIVEMAP
	vEmissiveMapUv = ( emissiveMapTransform * vec3( EMISSIVEMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_METALNESSMAP
	vMetalnessMapUv = ( metalnessMapTransform * vec3( METALNESSMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_ROUGHNESSMAP
	vRoughnessMapUv = ( roughnessMapTransform * vec3( ROUGHNESSMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_ANISOTROPYMAP
	vAnisotropyMapUv = ( anisotropyMapTransform * vec3( ANISOTROPYMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_CLEARCOATMAP
	vClearcoatMapUv = ( clearcoatMapTransform * vec3( CLEARCOATMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_CLEARCOAT_NORMALMAP
	vClearcoatNormalMapUv = ( clearcoatNormalMapTransform * vec3( CLEARCOAT_NORMALMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_CLEARCOAT_ROUGHNESSMAP
	vClearcoatRoughnessMapUv = ( clearcoatRoughnessMapTransform * vec3( CLEARCOAT_ROUGHNESSMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_IRIDESCENCEMAP
	vIridescenceMapUv = ( iridescenceMapTransform * vec3( IRIDESCENCEMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_IRIDESCENCE_THICKNESSMAP
	vIridescenceThicknessMapUv = ( iridescenceThicknessMapTransform * vec3( IRIDESCENCE_THICKNESSMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_SHEEN_COLORMAP
	vSheenColorMapUv = ( sheenColorMapTransform * vec3( SHEEN_COLORMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_SHEEN_ROUGHNESSMAP
	vSheenRoughnessMapUv = ( sheenRoughnessMapTransform * vec3( SHEEN_ROUGHNESSMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_SPECULARMAP
	vSpecularMapUv = ( specularMapTransform * vec3( SPECULARMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_SPECULAR_COLORMAP
	vSpecularColorMapUv = ( specularColorMapTransform * vec3( SPECULAR_COLORMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_SPECULAR_INTENSITYMAP
	vSpecularIntensityMapUv = ( specularIntensityMapTransform * vec3( SPECULAR_INTENSITYMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_TRANSMISSIONMAP
	vTransmissionMapUv = ( transmissionMapTransform * vec3( TRANSMISSIONMAP_UV, 1 ) ).xy;
#endif
#ifdef USE_THICKNESSMAP
	vThicknessMapUv = ( thicknessMapTransform * vec3( THICKNESSMAP_UV, 1 ) ).xy;
#endif`,F0=`#if defined( USE_ENVMAP ) || defined( DISTANCE ) || defined ( USE_SHADOWMAP ) || defined ( USE_TRANSMISSION ) || NUM_SPOT_LIGHT_COORDS > 0
	vec4 worldPosition = vec4( transformed, 1.0 );
	#ifdef USE_BATCHING
		worldPosition = batchingMatrix * worldPosition;
	#endif
	#ifdef USE_INSTANCING
		worldPosition = instanceMatrix * worldPosition;
	#endif
	worldPosition = modelMatrix * worldPosition;
#endif`,I0=`varying vec2 vUv;
uniform mat3 uvTransform;
void main() {
	vUv = ( uvTransform * vec3( uv, 1 ) ).xy;
	gl_Position = vec4( position.xy, 1.0, 1.0 );
}`,P0=`uniform sampler2D t2D;
uniform float backgroundIntensity;
varying vec2 vUv;
void main() {
	vec4 texColor = texture2D( t2D, vUv );
	#ifdef DECODE_VIDEO_TEXTURE
		texColor = vec4( mix( pow( texColor.rgb * 0.9478672986 + vec3( 0.0521327014 ), vec3( 2.4 ) ), texColor.rgb * 0.0773993808, vec3( lessThanEqual( texColor.rgb, vec3( 0.04045 ) ) ) ), texColor.w );
	#endif
	texColor.rgb *= backgroundIntensity;
	gl_FragColor = texColor;
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
}`,L0=`varying vec3 vWorldDirection;
#include <common>
void main() {
	vWorldDirection = transformDirection( position, modelMatrix );
	#include <begin_vertex>
	#include <project_vertex>
	gl_Position.z = gl_Position.w;
}`,D0=`#ifdef ENVMAP_TYPE_CUBE
	uniform samplerCube envMap;
#elif defined( ENVMAP_TYPE_CUBE_UV )
	uniform sampler2D envMap;
#endif
uniform float flipEnvMap;
uniform float backgroundBlurriness;
uniform float backgroundIntensity;
uniform mat3 backgroundRotation;
varying vec3 vWorldDirection;
#include <cube_uv_reflection_fragment>
void main() {
	#ifdef ENVMAP_TYPE_CUBE
		vec4 texColor = textureCube( envMap, backgroundRotation * vec3( flipEnvMap * vWorldDirection.x, vWorldDirection.yz ) );
	#elif defined( ENVMAP_TYPE_CUBE_UV )
		vec4 texColor = textureCubeUV( envMap, backgroundRotation * vWorldDirection, backgroundBlurriness );
	#else
		vec4 texColor = vec4( 0.0, 0.0, 0.0, 1.0 );
	#endif
	texColor.rgb *= backgroundIntensity;
	gl_FragColor = texColor;
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
}`,U0=`varying vec3 vWorldDirection;
#include <common>
void main() {
	vWorldDirection = transformDirection( position, modelMatrix );
	#include <begin_vertex>
	#include <project_vertex>
	gl_Position.z = gl_Position.w;
}`,N0=`uniform samplerCube tCube;
uniform float tFlip;
uniform float opacity;
varying vec3 vWorldDirection;
void main() {
	vec4 texColor = textureCube( tCube, vec3( tFlip * vWorldDirection.x, vWorldDirection.yz ) );
	gl_FragColor = texColor;
	gl_FragColor.a *= opacity;
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
}`,B0=`#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <displacementmap_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
varying vec2 vHighPrecisionZW;
void main() {
	#include <uv_vertex>
	#include <batching_vertex>
	#include <skinbase_vertex>
	#include <morphinstance_vertex>
	#ifdef USE_DISPLACEMENTMAP
		#include <beginnormal_vertex>
		#include <morphnormal_vertex>
		#include <skinnormal_vertex>
	#endif
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <displacementmap_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	vHighPrecisionZW = gl_Position.zw;
}`,z0=`#if DEPTH_PACKING == 3200
	uniform float opacity;
#endif
#include <common>
#include <packing>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
varying vec2 vHighPrecisionZW;
void main() {
	vec4 diffuseColor = vec4( 1.0 );
	#include <clipping_planes_fragment>
	#if DEPTH_PACKING == 3200
		diffuseColor.a = opacity;
	#endif
	#include <map_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	#include <logdepthbuf_fragment>
	float fragCoordZ = 0.5 * vHighPrecisionZW[0] / vHighPrecisionZW[1] + 0.5;
	#if DEPTH_PACKING == 3200
		gl_FragColor = vec4( vec3( 1.0 - fragCoordZ ), opacity );
	#elif DEPTH_PACKING == 3201
		gl_FragColor = packDepthToRGBA( fragCoordZ );
	#elif DEPTH_PACKING == 3202
		gl_FragColor = vec4( packDepthToRGB( fragCoordZ ), 1.0 );
	#elif DEPTH_PACKING == 3203
		gl_FragColor = vec4( packDepthToRG( fragCoordZ ), 0.0, 1.0 );
	#endif
}`,O0=`#define DISTANCE
varying vec3 vWorldPosition;
#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <displacementmap_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	#include <batching_vertex>
	#include <skinbase_vertex>
	#include <morphinstance_vertex>
	#ifdef USE_DISPLACEMENTMAP
		#include <beginnormal_vertex>
		#include <morphnormal_vertex>
		#include <skinnormal_vertex>
	#endif
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <displacementmap_vertex>
	#include <project_vertex>
	#include <worldpos_vertex>
	#include <clipping_planes_vertex>
	vWorldPosition = worldPosition.xyz;
}`,H0=`#define DISTANCE
uniform vec3 referencePosition;
uniform float nearDistance;
uniform float farDistance;
varying vec3 vWorldPosition;
#include <common>
#include <packing>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <clipping_planes_pars_fragment>
void main () {
	vec4 diffuseColor = vec4( 1.0 );
	#include <clipping_planes_fragment>
	#include <map_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	float dist = length( vWorldPosition - referencePosition );
	dist = ( dist - nearDistance ) / ( farDistance - nearDistance );
	dist = saturate( dist );
	gl_FragColor = packDepthToRGBA( dist );
}`,k0=`varying vec3 vWorldDirection;
#include <common>
void main() {
	vWorldDirection = transformDirection( position, modelMatrix );
	#include <begin_vertex>
	#include <project_vertex>
}`,G0=`uniform sampler2D tEquirect;
varying vec3 vWorldDirection;
#include <common>
void main() {
	vec3 direction = normalize( vWorldDirection );
	vec2 sampleUV = equirectUv( direction );
	gl_FragColor = texture2D( tEquirect, sampleUV );
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
}`,V0=`uniform float scale;
attribute float lineDistance;
varying float vLineDistance;
#include <common>
#include <uv_pars_vertex>
#include <color_pars_vertex>
#include <fog_pars_vertex>
#include <morphtarget_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	vLineDistance = scale * lineDistance;
	#include <uv_vertex>
	#include <color_vertex>
	#include <morphinstance_vertex>
	#include <morphcolor_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	#include <fog_vertex>
}`,W0=`uniform vec3 diffuse;
uniform float opacity;
uniform float dashSize;
uniform float totalSize;
varying float vLineDistance;
#include <common>
#include <color_pars_fragment>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <fog_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	if ( mod( vLineDistance, totalSize ) > dashSize ) {
		discard;
	}
	vec3 outgoingLight = vec3( 0.0 );
	#include <logdepthbuf_fragment>
	#include <map_fragment>
	#include <color_fragment>
	outgoingLight = diffuseColor.rgb;
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
	#include <premultiplied_alpha_fragment>
}`,X0=`#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <envmap_pars_vertex>
#include <color_pars_vertex>
#include <fog_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	#include <color_vertex>
	#include <morphinstance_vertex>
	#include <morphcolor_vertex>
	#include <batching_vertex>
	#if defined ( USE_ENVMAP ) || defined ( USE_SKINNING )
		#include <beginnormal_vertex>
		#include <morphnormal_vertex>
		#include <skinbase_vertex>
		#include <skinnormal_vertex>
		#include <defaultnormal_vertex>
	#endif
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	#include <worldpos_vertex>
	#include <envmap_vertex>
	#include <fog_vertex>
}`,q0=`uniform vec3 diffuse;
uniform float opacity;
#ifndef FLAT_SHADED
	varying vec3 vNormal;
#endif
#include <common>
#include <dithering_pars_fragment>
#include <color_pars_fragment>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <aomap_pars_fragment>
#include <lightmap_pars_fragment>
#include <envmap_common_pars_fragment>
#include <envmap_pars_fragment>
#include <fog_pars_fragment>
#include <specularmap_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	#include <logdepthbuf_fragment>
	#include <map_fragment>
	#include <color_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	#include <specularmap_fragment>
	ReflectedLight reflectedLight = ReflectedLight( vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ) );
	#ifdef USE_LIGHTMAP
		vec4 lightMapTexel = texture2D( lightMap, vLightMapUv );
		reflectedLight.indirectDiffuse += lightMapTexel.rgb * lightMapIntensity * RECIPROCAL_PI;
	#else
		reflectedLight.indirectDiffuse += vec3( 1.0 );
	#endif
	#include <aomap_fragment>
	reflectedLight.indirectDiffuse *= diffuseColor.rgb;
	vec3 outgoingLight = reflectedLight.indirectDiffuse;
	#include <envmap_fragment>
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
	#include <premultiplied_alpha_fragment>
	#include <dithering_fragment>
}`,Y0=`#define LAMBERT
varying vec3 vViewPosition;
#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <displacementmap_pars_vertex>
#include <envmap_pars_vertex>
#include <color_pars_vertex>
#include <fog_pars_vertex>
#include <normal_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <shadowmap_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	#include <color_vertex>
	#include <morphinstance_vertex>
	#include <morphcolor_vertex>
	#include <batching_vertex>
	#include <beginnormal_vertex>
	#include <morphnormal_vertex>
	#include <skinbase_vertex>
	#include <skinnormal_vertex>
	#include <defaultnormal_vertex>
	#include <normal_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <displacementmap_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	vViewPosition = - mvPosition.xyz;
	#include <worldpos_vertex>
	#include <envmap_vertex>
	#include <shadowmap_vertex>
	#include <fog_vertex>
}`,Z0=`#define LAMBERT
uniform vec3 diffuse;
uniform vec3 emissive;
uniform float opacity;
#include <common>
#include <packing>
#include <dithering_pars_fragment>
#include <color_pars_fragment>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <aomap_pars_fragment>
#include <lightmap_pars_fragment>
#include <emissivemap_pars_fragment>
#include <envmap_common_pars_fragment>
#include <envmap_pars_fragment>
#include <fog_pars_fragment>
#include <bsdfs>
#include <lights_pars_begin>
#include <normal_pars_fragment>
#include <lights_lambert_pars_fragment>
#include <shadowmap_pars_fragment>
#include <bumpmap_pars_fragment>
#include <normalmap_pars_fragment>
#include <specularmap_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	ReflectedLight reflectedLight = ReflectedLight( vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ) );
	vec3 totalEmissiveRadiance = emissive;
	#include <logdepthbuf_fragment>
	#include <map_fragment>
	#include <color_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	#include <specularmap_fragment>
	#include <normal_fragment_begin>
	#include <normal_fragment_maps>
	#include <emissivemap_fragment>
	#include <lights_lambert_fragment>
	#include <lights_fragment_begin>
	#include <lights_fragment_maps>
	#include <lights_fragment_end>
	#include <aomap_fragment>
	vec3 outgoingLight = reflectedLight.directDiffuse + reflectedLight.indirectDiffuse + totalEmissiveRadiance;
	#include <envmap_fragment>
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
	#include <premultiplied_alpha_fragment>
	#include <dithering_fragment>
}`,$0=`#define MATCAP
varying vec3 vViewPosition;
#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <color_pars_vertex>
#include <displacementmap_pars_vertex>
#include <fog_pars_vertex>
#include <normal_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	#include <color_vertex>
	#include <morphinstance_vertex>
	#include <morphcolor_vertex>
	#include <batching_vertex>
	#include <beginnormal_vertex>
	#include <morphnormal_vertex>
	#include <skinbase_vertex>
	#include <skinnormal_vertex>
	#include <defaultnormal_vertex>
	#include <normal_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <displacementmap_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	#include <fog_vertex>
	vViewPosition = - mvPosition.xyz;
}`,J0=`#define MATCAP
uniform vec3 diffuse;
uniform float opacity;
uniform sampler2D matcap;
varying vec3 vViewPosition;
#include <common>
#include <dithering_pars_fragment>
#include <color_pars_fragment>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <fog_pars_fragment>
#include <normal_pars_fragment>
#include <bumpmap_pars_fragment>
#include <normalmap_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	#include <logdepthbuf_fragment>
	#include <map_fragment>
	#include <color_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	#include <normal_fragment_begin>
	#include <normal_fragment_maps>
	vec3 viewDir = normalize( vViewPosition );
	vec3 x = normalize( vec3( viewDir.z, 0.0, - viewDir.x ) );
	vec3 y = cross( viewDir, x );
	vec2 uv = vec2( dot( x, normal ), dot( y, normal ) ) * 0.495 + 0.5;
	#ifdef USE_MATCAP
		vec4 matcapColor = texture2D( matcap, uv );
	#else
		vec4 matcapColor = vec4( vec3( mix( 0.2, 0.8, uv.y ) ), 1.0 );
	#endif
	vec3 outgoingLight = diffuseColor.rgb * matcapColor.rgb;
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
	#include <premultiplied_alpha_fragment>
	#include <dithering_fragment>
}`,K0=`#define NORMAL
#if defined( FLAT_SHADED ) || defined( USE_BUMPMAP ) || defined( USE_NORMALMAP_TANGENTSPACE )
	varying vec3 vViewPosition;
#endif
#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <displacementmap_pars_vertex>
#include <normal_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	#include <batching_vertex>
	#include <beginnormal_vertex>
	#include <morphinstance_vertex>
	#include <morphnormal_vertex>
	#include <skinbase_vertex>
	#include <skinnormal_vertex>
	#include <defaultnormal_vertex>
	#include <normal_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <displacementmap_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
#if defined( FLAT_SHADED ) || defined( USE_BUMPMAP ) || defined( USE_NORMALMAP_TANGENTSPACE )
	vViewPosition = - mvPosition.xyz;
#endif
}`,j0=`#define NORMAL
uniform float opacity;
#if defined( FLAT_SHADED ) || defined( USE_BUMPMAP ) || defined( USE_NORMALMAP_TANGENTSPACE )
	varying vec3 vViewPosition;
#endif
#include <packing>
#include <uv_pars_fragment>
#include <normal_pars_fragment>
#include <bumpmap_pars_fragment>
#include <normalmap_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( 0.0, 0.0, 0.0, opacity );
	#include <clipping_planes_fragment>
	#include <logdepthbuf_fragment>
	#include <normal_fragment_begin>
	#include <normal_fragment_maps>
	gl_FragColor = vec4( packNormalToRGB( normal ), diffuseColor.a );
	#ifdef OPAQUE
		gl_FragColor.a = 1.0;
	#endif
}`,Q0=`#define PHONG
varying vec3 vViewPosition;
#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <displacementmap_pars_vertex>
#include <envmap_pars_vertex>
#include <color_pars_vertex>
#include <fog_pars_vertex>
#include <normal_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <shadowmap_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	#include <color_vertex>
	#include <morphcolor_vertex>
	#include <batching_vertex>
	#include <beginnormal_vertex>
	#include <morphinstance_vertex>
	#include <morphnormal_vertex>
	#include <skinbase_vertex>
	#include <skinnormal_vertex>
	#include <defaultnormal_vertex>
	#include <normal_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <displacementmap_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	vViewPosition = - mvPosition.xyz;
	#include <worldpos_vertex>
	#include <envmap_vertex>
	#include <shadowmap_vertex>
	#include <fog_vertex>
}`,tg=`#define PHONG
uniform vec3 diffuse;
uniform vec3 emissive;
uniform vec3 specular;
uniform float shininess;
uniform float opacity;
#include <common>
#include <packing>
#include <dithering_pars_fragment>
#include <color_pars_fragment>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <aomap_pars_fragment>
#include <lightmap_pars_fragment>
#include <emissivemap_pars_fragment>
#include <envmap_common_pars_fragment>
#include <envmap_pars_fragment>
#include <fog_pars_fragment>
#include <bsdfs>
#include <lights_pars_begin>
#include <normal_pars_fragment>
#include <lights_phong_pars_fragment>
#include <shadowmap_pars_fragment>
#include <bumpmap_pars_fragment>
#include <normalmap_pars_fragment>
#include <specularmap_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	ReflectedLight reflectedLight = ReflectedLight( vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ) );
	vec3 totalEmissiveRadiance = emissive;
	#include <logdepthbuf_fragment>
	#include <map_fragment>
	#include <color_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	#include <specularmap_fragment>
	#include <normal_fragment_begin>
	#include <normal_fragment_maps>
	#include <emissivemap_fragment>
	#include <lights_phong_fragment>
	#include <lights_fragment_begin>
	#include <lights_fragment_maps>
	#include <lights_fragment_end>
	#include <aomap_fragment>
	vec3 outgoingLight = reflectedLight.directDiffuse + reflectedLight.indirectDiffuse + reflectedLight.directSpecular + reflectedLight.indirectSpecular + totalEmissiveRadiance;
	#include <envmap_fragment>
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
	#include <premultiplied_alpha_fragment>
	#include <dithering_fragment>
}`,eg=`#define STANDARD
varying vec3 vViewPosition;
#ifdef USE_TRANSMISSION
	varying vec3 vWorldPosition;
#endif
#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <displacementmap_pars_vertex>
#include <color_pars_vertex>
#include <fog_pars_vertex>
#include <normal_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <shadowmap_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	#include <color_vertex>
	#include <morphinstance_vertex>
	#include <morphcolor_vertex>
	#include <batching_vertex>
	#include <beginnormal_vertex>
	#include <morphnormal_vertex>
	#include <skinbase_vertex>
	#include <skinnormal_vertex>
	#include <defaultnormal_vertex>
	#include <normal_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <displacementmap_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	vViewPosition = - mvPosition.xyz;
	#include <worldpos_vertex>
	#include <shadowmap_vertex>
	#include <fog_vertex>
#ifdef USE_TRANSMISSION
	vWorldPosition = worldPosition.xyz;
#endif
}`,ng=`#define STANDARD
#ifdef PHYSICAL
	#define IOR
	#define USE_SPECULAR
#endif
uniform vec3 diffuse;
uniform vec3 emissive;
uniform float roughness;
uniform float metalness;
uniform float opacity;
#ifdef IOR
	uniform float ior;
#endif
#ifdef USE_SPECULAR
	uniform float specularIntensity;
	uniform vec3 specularColor;
	#ifdef USE_SPECULAR_COLORMAP
		uniform sampler2D specularColorMap;
	#endif
	#ifdef USE_SPECULAR_INTENSITYMAP
		uniform sampler2D specularIntensityMap;
	#endif
#endif
#ifdef USE_CLEARCOAT
	uniform float clearcoat;
	uniform float clearcoatRoughness;
#endif
#ifdef USE_DISPERSION
	uniform float dispersion;
#endif
#ifdef USE_IRIDESCENCE
	uniform float iridescence;
	uniform float iridescenceIOR;
	uniform float iridescenceThicknessMinimum;
	uniform float iridescenceThicknessMaximum;
#endif
#ifdef USE_SHEEN
	uniform vec3 sheenColor;
	uniform float sheenRoughness;
	#ifdef USE_SHEEN_COLORMAP
		uniform sampler2D sheenColorMap;
	#endif
	#ifdef USE_SHEEN_ROUGHNESSMAP
		uniform sampler2D sheenRoughnessMap;
	#endif
#endif
#ifdef USE_ANISOTROPY
	uniform vec2 anisotropyVector;
	#ifdef USE_ANISOTROPYMAP
		uniform sampler2D anisotropyMap;
	#endif
#endif
varying vec3 vViewPosition;
#include <common>
#include <packing>
#include <dithering_pars_fragment>
#include <color_pars_fragment>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <aomap_pars_fragment>
#include <lightmap_pars_fragment>
#include <emissivemap_pars_fragment>
#include <iridescence_fragment>
#include <cube_uv_reflection_fragment>
#include <envmap_common_pars_fragment>
#include <envmap_physical_pars_fragment>
#include <fog_pars_fragment>
#include <lights_pars_begin>
#include <normal_pars_fragment>
#include <lights_physical_pars_fragment>
#include <transmission_pars_fragment>
#include <shadowmap_pars_fragment>
#include <bumpmap_pars_fragment>
#include <normalmap_pars_fragment>
#include <clearcoat_pars_fragment>
#include <iridescence_pars_fragment>
#include <roughnessmap_pars_fragment>
#include <metalnessmap_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	ReflectedLight reflectedLight = ReflectedLight( vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ) );
	vec3 totalEmissiveRadiance = emissive;
	#include <logdepthbuf_fragment>
	#include <map_fragment>
	#include <color_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	#include <roughnessmap_fragment>
	#include <metalnessmap_fragment>
	#include <normal_fragment_begin>
	#include <normal_fragment_maps>
	#include <clearcoat_normal_fragment_begin>
	#include <clearcoat_normal_fragment_maps>
	#include <emissivemap_fragment>
	#include <lights_physical_fragment>
	#include <lights_fragment_begin>
	#include <lights_fragment_maps>
	#include <lights_fragment_end>
	#include <aomap_fragment>
	vec3 totalDiffuse = reflectedLight.directDiffuse + reflectedLight.indirectDiffuse;
	vec3 totalSpecular = reflectedLight.directSpecular + reflectedLight.indirectSpecular;
	#include <transmission_fragment>
	vec3 outgoingLight = totalDiffuse + totalSpecular + totalEmissiveRadiance;
	#ifdef USE_SHEEN
		float sheenEnergyComp = 1.0 - 0.157 * max3( material.sheenColor );
		outgoingLight = outgoingLight * sheenEnergyComp + sheenSpecularDirect + sheenSpecularIndirect;
	#endif
	#ifdef USE_CLEARCOAT
		float dotNVcc = saturate( dot( geometryClearcoatNormal, geometryViewDir ) );
		vec3 Fcc = F_Schlick( material.clearcoatF0, material.clearcoatF90, dotNVcc );
		outgoingLight = outgoingLight * ( 1.0 - material.clearcoat * Fcc ) + ( clearcoatSpecularDirect + clearcoatSpecularIndirect ) * material.clearcoat;
	#endif
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
	#include <premultiplied_alpha_fragment>
	#include <dithering_fragment>
}`,ig=`#define TOON
varying vec3 vViewPosition;
#include <common>
#include <batching_pars_vertex>
#include <uv_pars_vertex>
#include <displacementmap_pars_vertex>
#include <color_pars_vertex>
#include <fog_pars_vertex>
#include <normal_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <shadowmap_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	#include <color_vertex>
	#include <morphinstance_vertex>
	#include <morphcolor_vertex>
	#include <batching_vertex>
	#include <beginnormal_vertex>
	#include <morphnormal_vertex>
	#include <skinbase_vertex>
	#include <skinnormal_vertex>
	#include <defaultnormal_vertex>
	#include <normal_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <displacementmap_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	vViewPosition = - mvPosition.xyz;
	#include <worldpos_vertex>
	#include <shadowmap_vertex>
	#include <fog_vertex>
}`,sg=`#define TOON
uniform vec3 diffuse;
uniform vec3 emissive;
uniform float opacity;
#include <common>
#include <packing>
#include <dithering_pars_fragment>
#include <color_pars_fragment>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <aomap_pars_fragment>
#include <lightmap_pars_fragment>
#include <emissivemap_pars_fragment>
#include <gradientmap_pars_fragment>
#include <fog_pars_fragment>
#include <bsdfs>
#include <lights_pars_begin>
#include <normal_pars_fragment>
#include <lights_toon_pars_fragment>
#include <shadowmap_pars_fragment>
#include <bumpmap_pars_fragment>
#include <normalmap_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	ReflectedLight reflectedLight = ReflectedLight( vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ), vec3( 0.0 ) );
	vec3 totalEmissiveRadiance = emissive;
	#include <logdepthbuf_fragment>
	#include <map_fragment>
	#include <color_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	#include <normal_fragment_begin>
	#include <normal_fragment_maps>
	#include <emissivemap_fragment>
	#include <lights_toon_fragment>
	#include <lights_fragment_begin>
	#include <lights_fragment_maps>
	#include <lights_fragment_end>
	#include <aomap_fragment>
	vec3 outgoingLight = reflectedLight.directDiffuse + reflectedLight.indirectDiffuse + totalEmissiveRadiance;
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
	#include <premultiplied_alpha_fragment>
	#include <dithering_fragment>
}`,rg=`uniform float size;
uniform float scale;
#include <common>
#include <color_pars_vertex>
#include <fog_pars_vertex>
#include <morphtarget_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
#ifdef USE_POINTS_UV
	varying vec2 vUv;
	uniform mat3 uvTransform;
#endif
void main() {
	#ifdef USE_POINTS_UV
		vUv = ( uvTransform * vec3( uv, 1 ) ).xy;
	#endif
	#include <color_vertex>
	#include <morphinstance_vertex>
	#include <morphcolor_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <project_vertex>
	gl_PointSize = size;
	#ifdef USE_SIZEATTENUATION
		bool isPerspective = isPerspectiveMatrix( projectionMatrix );
		if ( isPerspective ) gl_PointSize *= ( scale / - mvPosition.z );
	#endif
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	#include <worldpos_vertex>
	#include <fog_vertex>
}`,og=`uniform vec3 diffuse;
uniform float opacity;
#include <common>
#include <color_pars_fragment>
#include <map_particle_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <fog_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	vec3 outgoingLight = vec3( 0.0 );
	#include <logdepthbuf_fragment>
	#include <map_particle_fragment>
	#include <color_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	outgoingLight = diffuseColor.rgb;
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
	#include <premultiplied_alpha_fragment>
}`,ag=`#include <common>
#include <batching_pars_vertex>
#include <fog_pars_vertex>
#include <morphtarget_pars_vertex>
#include <skinning_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <shadowmap_pars_vertex>
void main() {
	#include <batching_vertex>
	#include <beginnormal_vertex>
	#include <morphinstance_vertex>
	#include <morphnormal_vertex>
	#include <skinbase_vertex>
	#include <skinnormal_vertex>
	#include <defaultnormal_vertex>
	#include <begin_vertex>
	#include <morphtarget_vertex>
	#include <skinning_vertex>
	#include <project_vertex>
	#include <logdepthbuf_vertex>
	#include <worldpos_vertex>
	#include <shadowmap_vertex>
	#include <fog_vertex>
}`,lg=`uniform vec3 color;
uniform float opacity;
#include <common>
#include <packing>
#include <fog_pars_fragment>
#include <bsdfs>
#include <lights_pars_begin>
#include <logdepthbuf_pars_fragment>
#include <shadowmap_pars_fragment>
#include <shadowmask_pars_fragment>
void main() {
	#include <logdepthbuf_fragment>
	gl_FragColor = vec4( color, opacity * ( 1.0 - getShadowMask() ) );
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
}`,cg=`uniform float rotation;
uniform vec2 center;
#include <common>
#include <uv_pars_vertex>
#include <fog_pars_vertex>
#include <logdepthbuf_pars_vertex>
#include <clipping_planes_pars_vertex>
void main() {
	#include <uv_vertex>
	vec4 mvPosition = modelViewMatrix[ 3 ];
	vec2 scale = vec2( length( modelMatrix[ 0 ].xyz ), length( modelMatrix[ 1 ].xyz ) );
	#ifndef USE_SIZEATTENUATION
		bool isPerspective = isPerspectiveMatrix( projectionMatrix );
		if ( isPerspective ) scale *= - mvPosition.z;
	#endif
	vec2 alignedPosition = ( position.xy - ( center - vec2( 0.5 ) ) ) * scale;
	vec2 rotatedPosition;
	rotatedPosition.x = cos( rotation ) * alignedPosition.x - sin( rotation ) * alignedPosition.y;
	rotatedPosition.y = sin( rotation ) * alignedPosition.x + cos( rotation ) * alignedPosition.y;
	mvPosition.xy += rotatedPosition;
	gl_Position = projectionMatrix * mvPosition;
	#include <logdepthbuf_vertex>
	#include <clipping_planes_vertex>
	#include <fog_vertex>
}`,hg=`uniform vec3 diffuse;
uniform float opacity;
#include <common>
#include <uv_pars_fragment>
#include <map_pars_fragment>
#include <alphamap_pars_fragment>
#include <alphatest_pars_fragment>
#include <alphahash_pars_fragment>
#include <fog_pars_fragment>
#include <logdepthbuf_pars_fragment>
#include <clipping_planes_pars_fragment>
void main() {
	vec4 diffuseColor = vec4( diffuse, opacity );
	#include <clipping_planes_fragment>
	vec3 outgoingLight = vec3( 0.0 );
	#include <logdepthbuf_fragment>
	#include <map_fragment>
	#include <alphamap_fragment>
	#include <alphatest_fragment>
	#include <alphahash_fragment>
	outgoingLight = diffuseColor.rgb;
	#include <opaque_fragment>
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
	#include <fog_fragment>
}`,jt={alphahash_fragment:Pp,alphahash_pars_fragment:Lp,alphamap_fragment:Dp,alphamap_pars_fragment:Up,alphatest_fragment:Np,alphatest_pars_fragment:Bp,aomap_fragment:zp,aomap_pars_fragment:Op,batching_pars_vertex:Hp,batching_vertex:kp,begin_vertex:Gp,beginnormal_vertex:Vp,bsdfs:Wp,iridescence_fragment:Xp,bumpmap_pars_fragment:qp,clipping_planes_fragment:Yp,clipping_planes_pars_fragment:Zp,clipping_planes_pars_vertex:$p,clipping_planes_vertex:Jp,color_fragment:Kp,color_pars_fragment:jp,color_pars_vertex:Qp,color_vertex:tm,common:em,cube_uv_reflection_fragment:nm,defaultnormal_vertex:im,displacementmap_pars_vertex:sm,displacementmap_vertex:rm,emissivemap_fragment:om,emissivemap_pars_fragment:am,colorspace_fragment:lm,colorspace_pars_fragment:cm,envmap_fragment:hm,envmap_common_pars_fragment:um,envmap_pars_fragment:fm,envmap_pars_vertex:dm,envmap_physical_pars_fragment:Tm,envmap_vertex:pm,fog_vertex:mm,fog_pars_vertex:gm,fog_fragment:xm,fog_pars_fragment:ym,gradientmap_pars_fragment:_m,lightmap_pars_fragment:vm,lights_lambert_fragment:Mm,lights_lambert_pars_fragment:Em,lights_pars_begin:Sm,lights_toon_fragment:wm,lights_toon_pars_fragment:bm,lights_phong_fragment:Am,lights_phong_pars_fragment:Rm,lights_physical_fragment:Cm,lights_physical_pars_fragment:Fm,lights_fragment_begin:Im,lights_fragment_maps:Pm,lights_fragment_end:Lm,logdepthbuf_fragment:Dm,logdepthbuf_pars_fragment:Um,logdepthbuf_pars_vertex:Nm,logdepthbuf_vertex:Bm,map_fragment:zm,map_pars_fragment:Om,map_particle_fragment:Hm,map_particle_pars_fragment:km,metalnessmap_fragment:Gm,metalnessmap_pars_fragment:Vm,morphinstance_vertex:Wm,morphcolor_vertex:Xm,morphnormal_vertex:qm,morphtarget_pars_vertex:Ym,morphtarget_vertex:Zm,normal_fragment_begin:$m,normal_fragment_maps:Jm,normal_pars_fragment:Km,normal_pars_vertex:jm,normal_vertex:Qm,normalmap_pars_fragment:t0,clearcoat_normal_fragment_begin:e0,clearcoat_normal_fragment_maps:n0,clearcoat_pars_fragment:i0,iridescence_pars_fragment:s0,opaque_fragment:r0,packing:o0,premultiplied_alpha_fragment:a0,project_vertex:l0,dithering_fragment:c0,dithering_pars_fragment:h0,roughnessmap_fragment:u0,roughnessmap_pars_fragment:f0,shadowmap_pars_fragment:d0,shadowmap_pars_vertex:p0,shadowmap_vertex:m0,shadowmask_pars_fragment:g0,skinbase_vertex:x0,skinning_pars_vertex:y0,skinning_vertex:_0,skinnormal_vertex:v0,specularmap_fragment:M0,specularmap_pars_fragment:E0,tonemapping_fragment:S0,tonemapping_pars_fragment:T0,transmission_fragment:w0,transmission_pars_fragment:b0,uv_pars_fragment:A0,uv_pars_vertex:R0,uv_vertex:C0,worldpos_vertex:F0,background_vert:I0,background_frag:P0,backgroundCube_vert:L0,backgroundCube_frag:D0,cube_vert:U0,cube_frag:N0,depth_vert:B0,depth_frag:z0,distanceRGBA_vert:O0,distanceRGBA_frag:H0,equirect_vert:k0,equirect_frag:G0,linedashed_vert:V0,linedashed_frag:W0,meshbasic_vert:X0,meshbasic_frag:q0,meshlambert_vert:Y0,meshlambert_frag:Z0,meshmatcap_vert:$0,meshmatcap_frag:J0,meshnormal_vert:K0,meshnormal_frag:j0,meshphong_vert:Q0,meshphong_frag:tg,meshphysical_vert:eg,meshphysical_frag:ng,meshtoon_vert:ig,meshtoon_frag:sg,points_vert:rg,points_frag:og,shadow_vert:ag,shadow_frag:lg,sprite_vert:cg,sprite_frag:hg},pt={common:{diffuse:{value:new Jt(16777215)},opacity:{value:1},map:{value:null},mapTransform:{value:new $t},alphaMap:{value:null},alphaMapTransform:{value:new $t},alphaTest:{value:0}},specularmap:{specularMap:{value:null},specularMapTransform:{value:new $t}},envmap:{envMap:{value:null},envMapRotation:{value:new $t},flipEnvMap:{value:-1},reflectivity:{value:1},ior:{value:1.5},refractionRatio:{value:.98}},aomap:{aoMap:{value:null},aoMapIntensity:{value:1},aoMapTransform:{value:new $t}},lightmap:{lightMap:{value:null},lightMapIntensity:{value:1},lightMapTransform:{value:new $t}},bumpmap:{bumpMap:{value:null},bumpMapTransform:{value:new $t},bumpScale:{value:1}},normalmap:{normalMap:{value:null},normalMapTransform:{value:new $t},normalScale:{value:new lt(1,1)}},displacementmap:{displacementMap:{value:null},displacementMapTransform:{value:new $t},displacementScale:{value:1},displacementBias:{value:0}},emissivemap:{emissiveMap:{value:null},emissiveMapTransform:{value:new $t}},metalnessmap:{metalnessMap:{value:null},metalnessMapTransform:{value:new $t}},roughnessmap:{roughnessMap:{value:null},roughnessMapTransform:{value:new $t}},gradientmap:{gradientMap:{value:null}},fog:{fogDensity:{value:25e-5},fogNear:{value:1},fogFar:{value:2e3},fogColor:{value:new Jt(16777215)}},lights:{ambientLightColor:{value:[]},lightProbe:{value:[]},directionalLights:{value:[],properties:{direction:{},color:{}}},directionalLightShadows:{value:[],properties:{shadowIntensity:1,shadowBias:{},shadowNormalBias:{},shadowRadius:{},shadowMapSize:{}}},directionalShadowMap:{value:[]},directionalShadowMatrix:{value:[]},spotLights:{value:[],properties:{color:{},position:{},direction:{},distance:{},coneCos:{},penumbraCos:{},decay:{}}},spotLightShadows:{value:[],properties:{shadowIntensity:1,shadowBias:{},shadowNormalBias:{},shadowRadius:{},shadowMapSize:{}}},spotLightMap:{value:[]},spotShadowMap:{value:[]},spotLightMatrix:{value:[]},pointLights:{value:[],properties:{color:{},position:{},decay:{},distance:{}}},pointLightShadows:{value:[],properties:{shadowIntensity:1,shadowBias:{},shadowNormalBias:{},shadowRadius:{},shadowMapSize:{},shadowCameraNear:{},shadowCameraFar:{}}},pointShadowMap:{value:[]},pointShadowMatrix:{value:[]},hemisphereLights:{value:[],properties:{direction:{},skyColor:{},groundColor:{}}},rectAreaLights:{value:[],properties:{color:{},position:{},width:{},height:{}}},ltc_1:{value:null},ltc_2:{value:null}},points:{diffuse:{value:new Jt(16777215)},opacity:{value:1},size:{value:1},scale:{value:1},map:{value:null},alphaMap:{value:null},alphaMapTransform:{value:new $t},alphaTest:{value:0},uvTransform:{value:new $t}},sprite:{diffuse:{value:new Jt(16777215)},opacity:{value:1},center:{value:new lt(.5,.5)},rotation:{value:0},map:{value:null},mapTransform:{value:new $t},alphaMap:{value:null},alphaMapTransform:{value:new $t},alphaTest:{value:0}}},Fn={basic:{uniforms:je([pt.common,pt.specularmap,pt.envmap,pt.aomap,pt.lightmap,pt.fog]),vertexShader:jt.meshbasic_vert,fragmentShader:jt.meshbasic_frag},lambert:{uniforms:je([pt.common,pt.specularmap,pt.envmap,pt.aomap,pt.lightmap,pt.emissivemap,pt.bumpmap,pt.normalmap,pt.displacementmap,pt.fog,pt.lights,{emissive:{value:new Jt(0)}}]),vertexShader:jt.meshlambert_vert,fragmentShader:jt.meshlambert_frag},phong:{uniforms:je([pt.common,pt.specularmap,pt.envmap,pt.aomap,pt.lightmap,pt.emissivemap,pt.bumpmap,pt.normalmap,pt.displacementmap,pt.fog,pt.lights,{emissive:{value:new Jt(0)},specular:{value:new Jt(1118481)},shininess:{value:30}}]),vertexShader:jt.meshphong_vert,fragmentShader:jt.meshphong_frag},standard:{uniforms:je([pt.common,pt.envmap,pt.aomap,pt.lightmap,pt.emissivemap,pt.bumpmap,pt.normalmap,pt.displacementmap,pt.roughnessmap,pt.metalnessmap,pt.fog,pt.lights,{emissive:{value:new Jt(0)},roughness:{value:1},metalness:{value:0},envMapIntensity:{value:1}}]),vertexShader:jt.meshphysical_vert,fragmentShader:jt.meshphysical_frag},toon:{uniforms:je([pt.common,pt.aomap,pt.lightmap,pt.emissivemap,pt.bumpmap,pt.normalmap,pt.displacementmap,pt.gradientmap,pt.fog,pt.lights,{emissive:{value:new Jt(0)}}]),vertexShader:jt.meshtoon_vert,fragmentShader:jt.meshtoon_frag},matcap:{uniforms:je([pt.common,pt.bumpmap,pt.normalmap,pt.displacementmap,pt.fog,{matcap:{value:null}}]),vertexShader:jt.meshmatcap_vert,fragmentShader:jt.meshmatcap_frag},points:{uniforms:je([pt.points,pt.fog]),vertexShader:jt.points_vert,fragmentShader:jt.points_frag},dashed:{uniforms:je([pt.common,pt.fog,{scale:{value:1},dashSize:{value:1},totalSize:{value:2}}]),vertexShader:jt.linedashed_vert,fragmentShader:jt.linedashed_frag},depth:{uniforms:je([pt.common,pt.displacementmap]),vertexShader:jt.depth_vert,fragmentShader:jt.depth_frag},normal:{uniforms:je([pt.common,pt.bumpmap,pt.normalmap,pt.displacementmap,{opacity:{value:1}}]),vertexShader:jt.meshnormal_vert,fragmentShader:jt.meshnormal_frag},sprite:{uniforms:je([pt.sprite,pt.fog]),vertexShader:jt.sprite_vert,fragmentShader:jt.sprite_frag},background:{uniforms:{uvTransform:{value:new $t},t2D:{value:null},backgroundIntensity:{value:1}},vertexShader:jt.background_vert,fragmentShader:jt.background_frag},backgroundCube:{uniforms:{envMap:{value:null},flipEnvMap:{value:-1},backgroundBlurriness:{value:0},backgroundIntensity:{value:1},backgroundRotation:{value:new $t}},vertexShader:jt.backgroundCube_vert,fragmentShader:jt.backgroundCube_frag},cube:{uniforms:{tCube:{value:null},tFlip:{value:-1},opacity:{value:1}},vertexShader:jt.cube_vert,fragmentShader:jt.cube_frag},equirect:{uniforms:{tEquirect:{value:null}},vertexShader:jt.equirect_vert,fragmentShader:jt.equirect_frag},distanceRGBA:{uniforms:je([pt.common,pt.displacementmap,{referencePosition:{value:new B},nearDistance:{value:1},farDistance:{value:1e3}}]),vertexShader:jt.distanceRGBA_vert,fragmentShader:jt.distanceRGBA_frag},shadow:{uniforms:je([pt.lights,pt.fog,{color:{value:new Jt(0)},opacity:{value:1}}]),vertexShader:jt.shadow_vert,fragmentShader:jt.shadow_frag}};Fn.physical={uniforms:je([Fn.standard.uniforms,{clearcoat:{value:0},clearcoatMap:{value:null},clearcoatMapTransform:{value:new $t},clearcoatNormalMap:{value:null},clearcoatNormalMapTransform:{value:new $t},clearcoatNormalScale:{value:new lt(1,1)},clearcoatRoughness:{value:0},clearcoatRoughnessMap:{value:null},clearcoatRoughnessMapTransform:{value:new $t},dispersion:{value:0},iridescence:{value:0},iridescenceMap:{value:null},iridescenceMapTransform:{value:new $t},iridescenceIOR:{value:1.3},iridescenceThicknessMinimum:{value:100},iridescenceThicknessMaximum:{value:400},iridescenceThicknessMap:{value:null},iridescenceThicknessMapTransform:{value:new $t},sheen:{value:0},sheenColor:{value:new Jt(0)},sheenColorMap:{value:null},sheenColorMapTransform:{value:new $t},sheenRoughness:{value:1},sheenRoughnessMap:{value:null},sheenRoughnessMapTransform:{value:new $t},transmission:{value:0},transmissionMap:{value:null},transmissionMapTransform:{value:new $t},transmissionSamplerSize:{value:new lt},transmissionSamplerMap:{value:null},thickness:{value:0},thicknessMap:{value:null},thicknessMapTransform:{value:new $t},attenuationDistance:{value:0},attenuationColor:{value:new Jt(0)},specularColor:{value:new Jt(1,1,1)},specularColorMap:{value:null},specularColorMapTransform:{value:new $t},specularIntensity:{value:1},specularIntensityMap:{value:null},specularIntensityMapTransform:{value:new $t},anisotropyVector:{value:new lt},anisotropyMap:{value:null},anisotropyMapTransform:{value:new $t}}]),vertexShader:jt.meshphysical_vert,fragmentShader:jt.meshphysical_frag};var io={r:0,b:0,g:0},Di=new sn,ug=new ee;function fg(i,t,e,n,s,r,o){let a=new Jt(0),l=r===!0?0:1,h,u,m=null,x=0,f=null;function v(_){let p=_.isScene===!0?_.background:null;return p&&p.isTexture&&(p=(_.backgroundBlurriness>0?e:t).get(p)),p}function d(_){let p=!1,g=v(_);g===null?c(a,l):g&&g.isColor&&(c(g,1),p=!0);let T=i.xr.getEnvironmentBlendMode();T==="additive"?n.buffers.color.setClear(0,0,0,1,o):T==="alpha-blend"&&n.buffers.color.setClear(0,0,0,0,o),(i.autoClear||p)&&(n.buffers.depth.setTest(!0),n.buffers.depth.setMask(!0),n.buffers.color.setMask(!0),i.clear(i.autoClearColor,i.autoClearDepth,i.autoClearStencil))}function y(_,p){let g=v(p);g&&(g.isCubeTexture||g.mapping===Qo)?(u===void 0&&(u=new Z(new kt(1,1,1),new xn({name:"BackgroundCubeMaterial",uniforms:Ps(Fn.backgroundCube.uniforms),vertexShader:Fn.backgroundCube.vertexShader,fragmentShader:Fn.backgroundCube.fragmentShader,side:en,depthTest:!1,depthWrite:!1,fog:!1})),u.geometry.deleteAttribute("normal"),u.geometry.deleteAttribute("uv"),u.onBeforeRender=function(T,S,b){this.matrixWorld.copyPosition(b.matrixWorld)},Object.defineProperty(u.material,"envMap",{get:function(){return this.uniforms.envMap.value}}),s.update(u)),Di.copy(p.backgroundRotation),Di.x*=-1,Di.y*=-1,Di.z*=-1,g.isCubeTexture&&g.isRenderTargetTexture===!1&&(Di.y*=-1,Di.z*=-1),u.material.uniforms.envMap.value=g,u.material.uniforms.flipEnvMap.value=g.isCubeTexture&&g.isRenderTargetTexture===!1?-1:1,u.material.uniforms.backgroundBlurriness.value=p.backgroundBlurriness,u.material.uniforms.backgroundIntensity.value=p.backgroundIntensity,u.material.uniforms.backgroundRotation.value.setFromMatrix4(ug.makeRotationFromEuler(Di)),u.material.toneMapped=ie.getTransfer(g.colorSpace)!==ye,(m!==g||x!==g.version||f!==i.toneMapping)&&(u.material.needsUpdate=!0,m=g,x=g.version,f=i.toneMapping),u.layers.enableAll(),_.unshift(u,u.geometry,u.material,0,0,null)):g&&g.isTexture&&(h===void 0&&(h=new Z(new Qe(2,2),new xn({name:"BackgroundMaterial",uniforms:Ps(Fn.background.uniforms),vertexShader:Fn.background.vertexShader,fragmentShader:Fn.background.fragmentShader,side:Dn,depthTest:!1,depthWrite:!1,fog:!1})),h.geometry.deleteAttribute("normal"),Object.defineProperty(h.material,"map",{get:function(){return this.uniforms.t2D.value}}),s.update(h)),h.material.uniforms.t2D.value=g,h.material.uniforms.backgroundIntensity.value=p.backgroundIntensity,h.material.toneMapped=ie.getTransfer(g.colorSpace)!==ye,g.matrixAutoUpdate===!0&&g.updateMatrix(),h.material.uniforms.uvTransform.value.copy(g.matrix),(m!==g||x!==g.version||f!==i.toneMapping)&&(h.material.needsUpdate=!0,m=g,x=g.version,f=i.toneMapping),h.layers.enableAll(),_.unshift(h,h.geometry,h.material,0,0,null))}function c(_,p){_.getRGB(io,lf(i)),n.buffers.color.setClear(io.r,io.g,io.b,p,o)}return{getClearColor:function(){return a},setClearColor:function(_,p=1){a.set(_),l=p,c(a,l)},getClearAlpha:function(){return l},setClearAlpha:function(_){l=_,c(a,l)},render:d,addToRenderList:y}}function dg(i,t){let e=i.getParameter(i.MAX_VERTEX_ATTRIBS),n={},s=x(null),r=s,o=!1;function a(E,F,U,R,N){let V=!1,z=m(R,U,F);r!==z&&(r=z,h(r.object)),V=f(E,R,U,N),V&&v(E,R,U,N),N!==null&&t.update(N,i.ELEMENT_ARRAY_BUFFER),(V||o)&&(o=!1,g(E,F,U,R),N!==null&&i.bindBuffer(i.ELEMENT_ARRAY_BUFFER,t.get(N).buffer))}function l(){return i.createVertexArray()}function h(E){return i.bindVertexArray(E)}function u(E){return i.deleteVertexArray(E)}function m(E,F,U){let R=U.wireframe===!0,N=n[E.id];N===void 0&&(N={},n[E.id]=N);let V=N[F.id];V===void 0&&(V={},N[F.id]=V);let z=V[R];return z===void 0&&(z=x(l()),V[R]=z),z}function x(E){let F=[],U=[],R=[];for(let N=0;N<e;N++)F[N]=0,U[N]=0,R[N]=0;return{geometry:null,program:null,wireframe:!1,newAttributes:F,enabledAttributes:U,attributeDivisors:R,object:E,attributes:{},index:null}}function f(E,F,U,R){let N=r.attributes,V=F.attributes,z=0,O=U.getAttributes();for(let D in O)if(O[D].location>=0){let $=N[D],tt=V[D];if(tt===void 0&&(D==="instanceMatrix"&&E.instanceMatrix&&(tt=E.instanceMatrix),D==="instanceColor"&&E.instanceColor&&(tt=E.instanceColor)),$===void 0||$.attribute!==tt||tt&&$.data!==tt.data)return!0;z++}return r.attributesNum!==z||r.index!==R}function v(E,F,U,R){let N={},V=F.attributes,z=0,O=U.getAttributes();for(let D in O)if(O[D].location>=0){let $=V[D];$===void 0&&(D==="instanceMatrix"&&E.instanceMatrix&&($=E.instanceMatrix),D==="instanceColor"&&E.instanceColor&&($=E.instanceColor));let tt={};tt.attribute=$,$&&$.data&&(tt.data=$.data),N[D]=tt,z++}r.attributes=N,r.attributesNum=z,r.index=R}function d(){let E=r.newAttributes;for(let F=0,U=E.length;F<U;F++)E[F]=0}function y(E){c(E,0)}function c(E,F){let U=r.newAttributes,R=r.enabledAttributes,N=r.attributeDivisors;U[E]=1,R[E]===0&&(i.enableVertexAttribArray(E),R[E]=1),N[E]!==F&&(i.vertexAttribDivisor(E,F),N[E]=F)}function _(){let E=r.newAttributes,F=r.enabledAttributes;for(let U=0,R=F.length;U<R;U++)F[U]!==E[U]&&(i.disableVertexAttribArray(U),F[U]=0)}function p(E,F,U,R,N,V,z){z===!0?i.vertexAttribIPointer(E,F,U,N,V):i.vertexAttribPointer(E,F,U,R,N,V)}function g(E,F,U,R){d();let N=R.attributes,V=U.getAttributes(),z=F.defaultAttributeValues;for(let O in V){let D=V[O];if(D.location>=0){let X=N[O];if(X===void 0&&(O==="instanceMatrix"&&E.instanceMatrix&&(X=E.instanceMatrix),O==="instanceColor"&&E.instanceColor&&(X=E.instanceColor)),X!==void 0){let $=X.normalized,tt=X.itemSize,ut=t.get(X);if(ut===void 0)continue;let Ct=ut.buffer,J=ut.type,st=ut.bytesPerElement,_t=J===i.INT||J===i.UNSIGNED_INT||X.gpuType===Nc;if(X.isInterleavedBufferAttribute){let rt=X.data,Nt=rt.stride,Xt=X.offset;if(rt.isInstancedInterleavedBuffer){for(let Ht=0;Ht<D.locationSize;Ht++)c(D.location+Ht,rt.meshPerAttribute);E.isInstancedMesh!==!0&&R._maxInstanceCount===void 0&&(R._maxInstanceCount=rt.meshPerAttribute*rt.count)}else for(let Ht=0;Ht<D.locationSize;Ht++)y(D.location+Ht);i.bindBuffer(i.ARRAY_BUFFER,Ct);for(let Ht=0;Ht<D.locationSize;Ht++)p(D.location+Ht,tt/D.locationSize,J,$,Nt*st,(Xt+tt/D.locationSize*Ht)*st,_t)}else{if(X.isInstancedBufferAttribute){for(let rt=0;rt<D.locationSize;rt++)c(D.location+rt,X.meshPerAttribute);E.isInstancedMesh!==!0&&R._maxInstanceCount===void 0&&(R._maxInstanceCount=X.meshPerAttribute*X.count)}else for(let rt=0;rt<D.locationSize;rt++)y(D.location+rt);i.bindBuffer(i.ARRAY_BUFFER,Ct);for(let rt=0;rt<D.locationSize;rt++)p(D.location+rt,tt/D.locationSize,J,$,tt*st,tt/D.locationSize*rt*st,_t)}}else if(z!==void 0){let $=z[O];if($!==void 0)switch($.length){case 2:i.vertexAttrib2fv(D.location,$);break;case 3:i.vertexAttrib3fv(D.location,$);break;case 4:i.vertexAttrib4fv(D.location,$);break;default:i.vertexAttrib1fv(D.location,$)}}}}_()}function T(){w();for(let E in n){let F=n[E];for(let U in F){let R=F[U];for(let N in R)u(R[N].object),delete R[N];delete F[U]}delete n[E]}}function S(E){if(n[E.id]===void 0)return;let F=n[E.id];for(let U in F){let R=F[U];for(let N in R)u(R[N].object),delete R[N];delete F[U]}delete n[E.id]}function b(E){for(let F in n){let U=n[F];if(U[E.id]===void 0)continue;let R=U[E.id];for(let N in R)u(R[N].object),delete R[N];delete U[E.id]}}function w(){M(),o=!0,r!==s&&(r=s,h(r.object))}function M(){s.geometry=null,s.program=null,s.wireframe=!1}return{setup:a,reset:w,resetDefaultState:M,dispose:T,releaseStatesOfGeometry:S,releaseStatesOfProgram:b,initAttributes:d,enableAttribute:y,disableUnusedAttributes:_}}function pg(i,t,e){let n;function s(h){n=h}function r(h,u){i.drawArrays(n,h,u),e.update(u,n,1)}function o(h,u,m){m!==0&&(i.drawArraysInstanced(n,h,u,m),e.update(u,n,m))}function a(h,u,m){if(m===0)return;t.get("WEBGL_multi_draw").multiDrawArraysWEBGL(n,h,0,u,0,m);let f=0;for(let v=0;v<m;v++)f+=u[v];e.update(f,n,1)}function l(h,u,m,x){if(m===0)return;let f=t.get("WEBGL_multi_draw");if(f===null)for(let v=0;v<h.length;v++)o(h[v],u[v],x[v]);else{f.multiDrawArraysInstancedWEBGL(n,h,0,u,0,x,0,m);let v=0;for(let d=0;d<m;d++)v+=u[d]*x[d];e.update(v,n,1)}}this.setMode=s,this.render=r,this.renderInstances=o,this.renderMultiDraw=a,this.renderMultiDrawInstances=l}function mg(i,t,e,n){let s;function r(){if(s!==void 0)return s;if(t.has("EXT_texture_filter_anisotropic")===!0){let b=t.get("EXT_texture_filter_anisotropic");s=i.getParameter(b.MAX_TEXTURE_MAX_ANISOTROPY_EXT)}else s=0;return s}function o(b){return!(b!==bn&&n.convert(b)!==i.getParameter(i.IMPLEMENTATION_COLOR_READ_FORMAT))}function a(b){let w=b===Rr&&(t.has("EXT_color_buffer_half_float")||t.has("EXT_color_buffer_float"));return!(b!==Kn&&n.convert(b)!==i.getParameter(i.IMPLEMENTATION_COLOR_READ_TYPE)&&b!==Ln&&!w)}function l(b){if(b==="highp"){if(i.getShaderPrecisionFormat(i.VERTEX_SHADER,i.HIGH_FLOAT).precision>0&&i.getShaderPrecisionFormat(i.FRAGMENT_SHADER,i.HIGH_FLOAT).precision>0)return"highp";b="mediump"}return b==="mediump"&&i.getShaderPrecisionFormat(i.VERTEX_SHADER,i.MEDIUM_FLOAT).precision>0&&i.getShaderPrecisionFormat(i.FRAGMENT_SHADER,i.MEDIUM_FLOAT).precision>0?"mediump":"lowp"}let h=e.precision!==void 0?e.precision:"highp",u=l(h);u!==h&&(console.warn("THREE.WebGLRenderer:",h,"not supported, using",u,"instead."),h=u);let m=e.logarithmicDepthBuffer===!0,x=e.reverseDepthBuffer===!0&&t.has("EXT_clip_control"),f=i.getParameter(i.MAX_TEXTURE_IMAGE_UNITS),v=i.getParameter(i.MAX_VERTEX_TEXTURE_IMAGE_UNITS),d=i.getParameter(i.MAX_TEXTURE_SIZE),y=i.getParameter(i.MAX_CUBE_MAP_TEXTURE_SIZE),c=i.getParameter(i.MAX_VERTEX_ATTRIBS),_=i.getParameter(i.MAX_VERTEX_UNIFORM_VECTORS),p=i.getParameter(i.MAX_VARYING_VECTORS),g=i.getParameter(i.MAX_FRAGMENT_UNIFORM_VECTORS),T=v>0,S=i.getParameter(i.MAX_SAMPLES);return{isWebGL2:!0,getMaxAnisotropy:r,getMaxPrecision:l,textureFormatReadable:o,textureTypeReadable:a,precision:h,logarithmicDepthBuffer:m,reverseDepthBuffer:x,maxTextures:f,maxVertexTextures:v,maxTextureSize:d,maxCubemapSize:y,maxAttributes:c,maxVertexUniforms:_,maxVaryings:p,maxFragmentUniforms:g,vertexTextures:T,maxSamples:S}}function gg(i){let t=this,e=null,n=0,s=!1,r=!1,o=new Yn,a=new $t,l={value:null,needsUpdate:!1};this.uniform=l,this.numPlanes=0,this.numIntersection=0,this.init=function(m,x){let f=m.length!==0||x||n!==0||s;return s=x,n=m.length,f},this.beginShadows=function(){r=!0,u(null)},this.endShadows=function(){r=!1},this.setGlobalState=function(m,x){e=u(m,x,0)},this.setState=function(m,x,f){let v=m.clippingPlanes,d=m.clipIntersection,y=m.clipShadows,c=i.get(m);if(!s||v===null||v.length===0||r&&!y)r?u(null):h();else{let _=r?0:n,p=_*4,g=c.clippingState||null;l.value=g,g=u(v,x,p,f);for(let T=0;T!==p;++T)g[T]=e[T];c.clippingState=g,this.numIntersection=d?this.numPlanes:0,this.numPlanes+=_}};function h(){l.value!==e&&(l.value=e,l.needsUpdate=n>0),t.numPlanes=n,t.numIntersection=0}function u(m,x,f,v){let d=m!==null?m.length:0,y=null;if(d!==0){if(y=l.value,v!==!0||y===null){let c=f+d*4,_=x.matrixWorldInverse;a.getNormalMatrix(_),(y===null||y.length<c)&&(y=new Float32Array(c));for(let p=0,g=f;p!==d;++p,g+=4)o.copy(m[p]).applyMatrix4(_,a),o.normal.toArray(y,g),y[g+3]=o.constant}l.value=y,l.needsUpdate=!0}return t.numPlanes=d,t.numIntersection=0,y}}function xg(i){let t=new WeakMap;function e(o,a){return a===ml?o.mapping=Rs:a===gl&&(o.mapping=Cs),o}function n(o){if(o&&o.isTexture){let a=o.mapping;if(a===ml||a===gl)if(t.has(o)){let l=t.get(o).texture;return e(l,o.mapping)}else{let l=o.image;if(l&&l.height>0){let h=new Kl(l.height);return h.fromEquirectangularTexture(i,o),t.set(o,h),o.addEventListener("dispose",s),e(h.texture,o.mapping)}else return null}}return o}function s(o){let a=o.target;a.removeEventListener("dispose",s);let l=t.get(a);l!==void 0&&(t.delete(a),l.dispose())}function r(){t=new WeakMap}return{get:n,dispose:r}}var Do=class extends Po{constructor(t=-1,e=1,n=1,s=-1,r=.1,o=2e3){super(),this.isOrthographicCamera=!0,this.type="OrthographicCamera",this.zoom=1,this.view=null,this.left=t,this.right=e,this.top=n,this.bottom=s,this.near=r,this.far=o,this.updateProjectionMatrix()}copy(t,e){return super.copy(t,e),this.left=t.left,this.right=t.right,this.top=t.top,this.bottom=t.bottom,this.near=t.near,this.far=t.far,this.zoom=t.zoom,this.view=t.view===null?null:Object.assign({},t.view),this}setViewOffset(t,e,n,s,r,o){this.view===null&&(this.view={enabled:!0,fullWidth:1,fullHeight:1,offsetX:0,offsetY:0,width:1,height:1}),this.view.enabled=!0,this.view.fullWidth=t,this.view.fullHeight=e,this.view.offsetX=n,this.view.offsetY=s,this.view.width=r,this.view.height=o,this.updateProjectionMatrix()}clearViewOffset(){this.view!==null&&(this.view.enabled=!1),this.updateProjectionMatrix()}updateProjectionMatrix(){let t=(this.right-this.left)/(2*this.zoom),e=(this.top-this.bottom)/(2*this.zoom),n=(this.right+this.left)/2,s=(this.top+this.bottom)/2,r=n-t,o=n+t,a=s+e,l=s-e;if(this.view!==null&&this.view.enabled){let h=(this.right-this.left)/this.view.fullWidth/this.zoom,u=(this.top-this.bottom)/this.view.fullHeight/this.zoom;r+=h*this.view.offsetX,o=r+h*this.view.width,a-=u*this.view.offsetY,l=a-u*this.view.height}this.projectionMatrix.makeOrthographic(r,o,a,l,this.near,this.far,this.coordinateSystem),this.projectionMatrixInverse.copy(this.projectionMatrix).invert()}toJSON(t){let e=super.toJSON(t);return e.object.zoom=this.zoom,e.object.left=this.left,e.object.right=this.right,e.object.top=this.top,e.object.bottom=this.bottom,e.object.near=this.near,e.object.far=this.far,this.view!==null&&(e.object.view=Object.assign({},this.view)),e}},Ms=4,ou=[.125,.215,.35,.446,.526,.582],zi=20,Za=new Do,au=new Jt,$a=null,Ja=0,Ka=0,ja=!1,Ni=(1+Math.sqrt(5))/2,ms=1/Ni,lu=[new B(-Ni,ms,0),new B(Ni,ms,0),new B(-ms,0,Ni),new B(ms,0,Ni),new B(0,Ni,-ms),new B(0,Ni,ms),new B(-1,1,-1),new B(1,1,-1),new B(-1,1,1),new B(1,1,1)],Uo=class{constructor(t){this._renderer=t,this._pingPongRenderTarget=null,this._lodMax=0,this._cubeSize=0,this._lodPlanes=[],this._sizeLods=[],this._sigmas=[],this._blurMaterial=null,this._cubemapMaterial=null,this._equirectMaterial=null,this._compileMaterial(this._blurMaterial)}fromScene(t,e=0,n=.1,s=100){$a=this._renderer.getRenderTarget(),Ja=this._renderer.getActiveCubeFace(),Ka=this._renderer.getActiveMipmapLevel(),ja=this._renderer.xr.enabled,this._renderer.xr.enabled=!1,this._setSize(256);let r=this._allocateTargets();return r.depthBuffer=!0,this._sceneToCubeUV(t,n,s,r),e>0&&this._blur(r,0,0,e),this._applyPMREM(r),this._cleanup(r),r}fromEquirectangular(t,e=null){return this._fromTexture(t,e)}fromCubemap(t,e=null){return this._fromTexture(t,e)}compileCubemapShader(){this._cubemapMaterial===null&&(this._cubemapMaterial=uu(),this._compileMaterial(this._cubemapMaterial))}compileEquirectangularShader(){this._equirectMaterial===null&&(this._equirectMaterial=hu(),this._compileMaterial(this._equirectMaterial))}dispose(){this._dispose(),this._cubemapMaterial!==null&&this._cubemapMaterial.dispose(),this._equirectMaterial!==null&&this._equirectMaterial.dispose()}_setSize(t){this._lodMax=Math.floor(Math.log2(t)),this._cubeSize=Math.pow(2,this._lodMax)}_dispose(){this._blurMaterial!==null&&this._blurMaterial.dispose(),this._pingPongRenderTarget!==null&&this._pingPongRenderTarget.dispose();for(let t=0;t<this._lodPlanes.length;t++)this._lodPlanes[t].dispose()}_cleanup(t){this._renderer.setRenderTarget($a,Ja,Ka),this._renderer.xr.enabled=ja,t.scissorTest=!1,so(t,0,0,t.width,t.height)}_fromTexture(t,e){t.mapping===Rs||t.mapping===Cs?this._setSize(t.image.length===0?16:t.image[0].width||t.image[0].image.width):this._setSize(t.image.width/4),$a=this._renderer.getRenderTarget(),Ja=this._renderer.getActiveCubeFace(),Ka=this._renderer.getActiveMipmapLevel(),ja=this._renderer.xr.enabled,this._renderer.xr.enabled=!1;let n=e||this._allocateTargets();return this._textureToCubeUV(t,n),this._applyPMREM(n),this._cleanup(n),n}_allocateTargets(){let t=3*Math.max(this._cubeSize,112),e=4*this._cubeSize,n={magFilter:Pn,minFilter:Pn,generateMipmaps:!1,type:Rr,format:bn,colorSpace:Us,depthBuffer:!1},s=cu(t,e,n);if(this._pingPongRenderTarget===null||this._pingPongRenderTarget.width!==t||this._pingPongRenderTarget.height!==e){this._pingPongRenderTarget!==null&&this._dispose(),this._pingPongRenderTarget=cu(t,e,n);let{_lodMax:r}=this;({sizeLods:this._sizeLods,lodPlanes:this._lodPlanes,sigmas:this._sigmas}=yg(r)),this._blurMaterial=_g(r,t,e)}return s}_compileMaterial(t){let e=new Z(this._lodPlanes[0],t);this._renderer.compile(e,Za)}_sceneToCubeUV(t,e,n,s){let a=new Je(90,1,e,n),l=[1,-1,1,1,1,1],h=[1,1,1,-1,-1,-1],u=this._renderer,m=u.autoClear,x=u.toneMapping;u.getClearColor(au),u.toneMapping=_i,u.autoClear=!1;let f=new oe({name:"PMREM.Background",side:en,depthWrite:!1,depthTest:!1}),v=new Z(new kt,f),d=!1,y=t.background;y?y.isColor&&(f.color.copy(y),t.background=null,d=!0):(f.color.copy(au),d=!0);for(let c=0;c<6;c++){let _=c%3;_===0?(a.up.set(0,l[c],0),a.lookAt(h[c],0,0)):_===1?(a.up.set(0,0,l[c]),a.lookAt(0,h[c],0)):(a.up.set(0,l[c],0),a.lookAt(0,0,h[c]));let p=this._cubeSize;so(s,_*p,c>2?p:0,p,p),u.setRenderTarget(s),d&&u.render(v,a),u.render(t,a)}v.geometry.dispose(),v.material.dispose(),u.toneMapping=x,u.autoClear=m,t.background=y}_textureToCubeUV(t,e){let n=this._renderer,s=t.mapping===Rs||t.mapping===Cs;s?(this._cubemapMaterial===null&&(this._cubemapMaterial=uu()),this._cubemapMaterial.uniforms.flipEnvMap.value=t.isRenderTargetTexture===!1?-1:1):this._equirectMaterial===null&&(this._equirectMaterial=hu());let r=s?this._cubemapMaterial:this._equirectMaterial,o=new Z(this._lodPlanes[0],r),a=r.uniforms;a.envMap.value=t;let l=this._cubeSize;so(e,0,0,3*l,2*l),n.setRenderTarget(e),n.render(o,Za)}_applyPMREM(t){let e=this._renderer,n=e.autoClear;e.autoClear=!1;let s=this._lodPlanes.length;for(let r=1;r<s;r++){let o=Math.sqrt(this._sigmas[r]*this._sigmas[r]-this._sigmas[r-1]*this._sigmas[r-1]),a=lu[(s-r-1)%lu.length];this._blur(t,r-1,r,o,a)}e.autoClear=n}_blur(t,e,n,s,r){let o=this._pingPongRenderTarget;this._halfBlur(t,o,e,n,s,"latitudinal",r),this._halfBlur(o,t,n,n,s,"longitudinal",r)}_halfBlur(t,e,n,s,r,o,a){let l=this._renderer,h=this._blurMaterial;o!=="latitudinal"&&o!=="longitudinal"&&console.error("blur direction must be either latitudinal or longitudinal!");let u=3,m=new Z(this._lodPlanes[s],h),x=h.uniforms,f=this._sizeLods[n]-1,v=isFinite(r)?Math.PI/(2*f):2*Math.PI/(2*zi-1),d=r/v,y=isFinite(r)?1+Math.floor(u*d):zi;y>zi&&console.warn(`sigmaRadians, ${r}, is too large and will clip, as it requested ${y} samples when the maximum is set to ${zi}`);let c=[],_=0;for(let b=0;b<zi;++b){let w=b/d,M=Math.exp(-w*w/2);c.push(M),b===0?_+=M:b<y&&(_+=2*M)}for(let b=0;b<c.length;b++)c[b]=c[b]/_;x.envMap.value=t.texture,x.samples.value=y,x.weights.value=c,x.latitudinal.value=o==="latitudinal",a&&(x.poleAxis.value=a);let{_lodMax:p}=this;x.dTheta.value=v,x.mipInt.value=p-n;let g=this._sizeLods[s],T=3*g*(s>p-Ms?s-p+Ms:0),S=4*(this._cubeSize-g);so(e,T,S,3*g,2*g),l.setRenderTarget(e),l.render(m,Za)}};function yg(i){let t=[],e=[],n=[],s=i,r=i-Ms+1+ou.length;for(let o=0;o<r;o++){let a=Math.pow(2,s);e.push(a);let l=1/a;o>i-Ms?l=ou[o-i+Ms-1]:o===0&&(l=0),n.push(l);let h=1/(a-2),u=-h,m=1+h,x=[u,u,m,u,m,m,u,u,m,m,u,m],f=6,v=6,d=3,y=2,c=1,_=new Float32Array(d*v*f),p=new Float32Array(y*v*f),g=new Float32Array(c*v*f);for(let S=0;S<f;S++){let b=S%3*2/3-1,w=S>2?0:-1,M=[b,w,0,b+2/3,w,0,b+2/3,w+1,0,b,w,0,b+2/3,w+1,0,b,w+1,0];_.set(M,d*v*S),p.set(x,y*v*S);let E=[S,S,S,S,S,S];g.set(E,c*v*S)}let T=new de;T.setAttribute("position",new we(_,d)),T.setAttribute("uv",new we(p,y)),T.setAttribute("faceIndex",new we(g,c)),t.push(T),s>Ms&&s--}return{lodPlanes:t,sizeLods:e,sigmas:n}}function cu(i,t,e){let n=new jn(i,t,e);return n.texture.mapping=Qo,n.texture.name="PMREM.cubeUv",n.scissorTest=!0,n}function so(i,t,e,n,s){i.viewport.set(t,e,n,s),i.scissor.set(t,e,n,s)}function _g(i,t,e){let n=new Float32Array(zi),s=new B(0,1,0);return new xn({name:"SphericalGaussianBlur",defines:{n:zi,CUBEUV_TEXEL_WIDTH:1/t,CUBEUV_TEXEL_HEIGHT:1/e,CUBEUV_MAX_MIP:`${i}.0`},uniforms:{envMap:{value:null},samples:{value:1},weights:{value:n},latitudinal:{value:!1},dTheta:{value:0},mipInt:{value:0},poleAxis:{value:s}},vertexShader:Vc(),fragmentShader:`

			precision mediump float;
			precision mediump int;

			varying vec3 vOutputDirection;

			uniform sampler2D envMap;
			uniform int samples;
			uniform float weights[ n ];
			uniform bool latitudinal;
			uniform float dTheta;
			uniform float mipInt;
			uniform vec3 poleAxis;

			#define ENVMAP_TYPE_CUBE_UV
			#include <cube_uv_reflection_fragment>

			vec3 getSample( float theta, vec3 axis ) {

				float cosTheta = cos( theta );
				// Rodrigues' axis-angle rotation
				vec3 sampleDirection = vOutputDirection * cosTheta
					+ cross( axis, vOutputDirection ) * sin( theta )
					+ axis * dot( axis, vOutputDirection ) * ( 1.0 - cosTheta );

				return bilinearCubeUV( envMap, sampleDirection, mipInt );

			}

			void main() {

				vec3 axis = latitudinal ? poleAxis : cross( poleAxis, vOutputDirection );

				if ( all( equal( axis, vec3( 0.0 ) ) ) ) {

					axis = vec3( vOutputDirection.z, 0.0, - vOutputDirection.x );

				}

				axis = normalize( axis );

				gl_FragColor = vec4( 0.0, 0.0, 0.0, 1.0 );
				gl_FragColor.rgb += weights[ 0 ] * getSample( 0.0, axis );

				for ( int i = 1; i < n; i++ ) {

					if ( i >= samples ) {

						break;

					}

					float theta = dTheta * float( i );
					gl_FragColor.rgb += weights[ i ] * getSample( -1.0 * theta, axis );
					gl_FragColor.rgb += weights[ i ] * getSample( theta, axis );

				}

			}
		`,blending:xi,depthTest:!1,depthWrite:!1})}function hu(){return new xn({name:"EquirectangularToCubeUV",uniforms:{envMap:{value:null}},vertexShader:Vc(),fragmentShader:`

			precision mediump float;
			precision mediump int;

			varying vec3 vOutputDirection;

			uniform sampler2D envMap;

			#include <common>

			void main() {

				vec3 outputDirection = normalize( vOutputDirection );
				vec2 uv = equirectUv( outputDirection );

				gl_FragColor = vec4( texture2D ( envMap, uv ).rgb, 1.0 );

			}
		`,blending:xi,depthTest:!1,depthWrite:!1})}function uu(){return new xn({name:"CubemapToCubeUV",uniforms:{envMap:{value:null},flipEnvMap:{value:-1}},vertexShader:Vc(),fragmentShader:`

			precision mediump float;
			precision mediump int;

			uniform float flipEnvMap;

			varying vec3 vOutputDirection;

			uniform samplerCube envMap;

			void main() {

				gl_FragColor = textureCube( envMap, vec3( flipEnvMap * vOutputDirection.x, vOutputDirection.yz ) );

			}
		`,blending:xi,depthTest:!1,depthWrite:!1})}function Vc(){return`

		precision mediump float;
		precision mediump int;

		attribute float faceIndex;

		varying vec3 vOutputDirection;

		// RH coordinate system; PMREM face-indexing convention
		vec3 getDirection( vec2 uv, float face ) {

			uv = 2.0 * uv - 1.0;

			vec3 direction = vec3( uv, 1.0 );

			if ( face == 0.0 ) {

				direction = direction.zyx; // ( 1, v, u ) pos x

			} else if ( face == 1.0 ) {

				direction = direction.xzy;
				direction.xz *= -1.0; // ( -u, 1, -v ) pos y

			} else if ( face == 2.0 ) {

				direction.x *= -1.0; // ( -u, v, 1 ) pos z

			} else if ( face == 3.0 ) {

				direction = direction.zyx;
				direction.xz *= -1.0; // ( -1, v, -u ) neg x

			} else if ( face == 4.0 ) {

				direction = direction.xzy;
				direction.xy *= -1.0; // ( -u, -1, v ) neg y

			} else if ( face == 5.0 ) {

				direction.z *= -1.0; // ( u, v, -1 ) neg z

			}

			return direction;

		}

		void main() {

			vOutputDirection = getDirection( uv, faceIndex );
			gl_Position = vec4( position, 1.0 );

		}
	`}function vg(i){let t=new WeakMap,e=null;function n(a){if(a&&a.isTexture){let l=a.mapping,h=l===ml||l===gl,u=l===Rs||l===Cs;if(h||u){let m=t.get(a),x=m!==void 0?m.texture.pmremVersion:0;if(a.isRenderTargetTexture&&a.pmremVersion!==x)return e===null&&(e=new Uo(i)),m=h?e.fromEquirectangular(a,m):e.fromCubemap(a,m),m.texture.pmremVersion=a.pmremVersion,t.set(a,m),m.texture;if(m!==void 0)return m.texture;{let f=a.image;return h&&f&&f.height>0||u&&f&&s(f)?(e===null&&(e=new Uo(i)),m=h?e.fromEquirectangular(a):e.fromCubemap(a),m.texture.pmremVersion=a.pmremVersion,t.set(a,m),a.addEventListener("dispose",r),m.texture):null}}}return a}function s(a){let l=0,h=6;for(let u=0;u<h;u++)a[u]!==void 0&&l++;return l===h}function r(a){let l=a.target;l.removeEventListener("dispose",r);let h=t.get(l);h!==void 0&&(t.delete(l),h.dispose())}function o(){t=new WeakMap,e!==null&&(e.dispose(),e=null)}return{get:n,dispose:o}}function Mg(i){let t={};function e(n){if(t[n]!==void 0)return t[n];let s;switch(n){case"WEBGL_depth_texture":s=i.getExtension("WEBGL_depth_texture")||i.getExtension("MOZ_WEBGL_depth_texture")||i.getExtension("WEBKIT_WEBGL_depth_texture");break;case"EXT_texture_filter_anisotropic":s=i.getExtension("EXT_texture_filter_anisotropic")||i.getExtension("MOZ_EXT_texture_filter_anisotropic")||i.getExtension("WEBKIT_EXT_texture_filter_anisotropic");break;case"WEBGL_compressed_texture_s3tc":s=i.getExtension("WEBGL_compressed_texture_s3tc")||i.getExtension("MOZ_WEBGL_compressed_texture_s3tc")||i.getExtension("WEBKIT_WEBGL_compressed_texture_s3tc");break;case"WEBGL_compressed_texture_pvrtc":s=i.getExtension("WEBGL_compressed_texture_pvrtc")||i.getExtension("WEBKIT_WEBGL_compressed_texture_pvrtc");break;default:s=i.getExtension(n)}return t[n]=s,s}return{has:function(n){return e(n)!==null},init:function(){e("EXT_color_buffer_float"),e("WEBGL_clip_cull_distance"),e("OES_texture_float_linear"),e("EXT_color_buffer_half_float"),e("WEBGL_multisampled_render_to_texture"),e("WEBGL_render_shared_exponent")},get:function(n){let s=e(n);return s===null&&or("THREE.WebGLRenderer: "+n+" extension not supported."),s}}}function Eg(i,t,e,n){let s={},r=new WeakMap;function o(m){let x=m.target;x.index!==null&&t.remove(x.index);for(let v in x.attributes)t.remove(x.attributes[v]);for(let v in x.morphAttributes){let d=x.morphAttributes[v];for(let y=0,c=d.length;y<c;y++)t.remove(d[y])}x.removeEventListener("dispose",o),delete s[x.id];let f=r.get(x);f&&(t.remove(f),r.delete(x)),n.releaseStatesOfGeometry(x),x.isInstancedBufferGeometry===!0&&delete x._maxInstanceCount,e.memory.geometries--}function a(m,x){return s[x.id]===!0||(x.addEventListener("dispose",o),s[x.id]=!0,e.memory.geometries++),x}function l(m){let x=m.attributes;for(let v in x)t.update(x[v],i.ARRAY_BUFFER);let f=m.morphAttributes;for(let v in f){let d=f[v];for(let y=0,c=d.length;y<c;y++)t.update(d[y],i.ARRAY_BUFFER)}}function h(m){let x=[],f=m.index,v=m.attributes.position,d=0;if(f!==null){let _=f.array;d=f.version;for(let p=0,g=_.length;p<g;p+=3){let T=_[p+0],S=_[p+1],b=_[p+2];x.push(T,S,S,b,b,T)}}else if(v!==void 0){let _=v.array;d=v.version;for(let p=0,g=_.length/3-1;p<g;p+=3){let T=p+0,S=p+1,b=p+2;x.push(T,S,S,b,b,T)}}else return;let y=new(of(x)?Io:Fo)(x,1);y.version=d;let c=r.get(m);c&&t.remove(c),r.set(m,y)}function u(m){let x=r.get(m);if(x){let f=m.index;f!==null&&x.version<f.version&&h(m)}else h(m);return r.get(m)}return{get:a,update:l,getWireframeAttribute:u}}function Sg(i,t,e){let n;function s(x){n=x}let r,o;function a(x){r=x.type,o=x.bytesPerElement}function l(x,f){i.drawElements(n,f,r,x*o),e.update(f,n,1)}function h(x,f,v){v!==0&&(i.drawElementsInstanced(n,f,r,x*o,v),e.update(f,n,v))}function u(x,f,v){if(v===0)return;t.get("WEBGL_multi_draw").multiDrawElementsWEBGL(n,f,0,r,x,0,v);let y=0;for(let c=0;c<v;c++)y+=f[c];e.update(y,n,1)}function m(x,f,v,d){if(v===0)return;let y=t.get("WEBGL_multi_draw");if(y===null)for(let c=0;c<x.length;c++)h(x[c]/o,f[c],d[c]);else{y.multiDrawElementsInstancedWEBGL(n,f,0,r,x,0,d,0,v);let c=0;for(let _=0;_<v;_++)c+=f[_]*d[_];e.update(c,n,1)}}this.setMode=s,this.setIndex=a,this.render=l,this.renderInstances=h,this.renderMultiDraw=u,this.renderMultiDrawInstances=m}function Tg(i){let t={geometries:0,textures:0},e={frame:0,calls:0,triangles:0,points:0,lines:0};function n(r,o,a){switch(e.calls++,o){case i.TRIANGLES:e.triangles+=a*(r/3);break;case i.LINES:e.lines+=a*(r/2);break;case i.LINE_STRIP:e.lines+=a*(r-1);break;case i.LINE_LOOP:e.lines+=a*r;break;case i.POINTS:e.points+=a*r;break;default:console.error("THREE.WebGLInfo: Unknown draw mode:",o);break}}function s(){e.calls=0,e.triangles=0,e.points=0,e.lines=0}return{memory:t,render:e,programs:null,autoReset:!0,reset:s,update:n}}function wg(i,t,e){let n=new WeakMap,s=new Ue;function r(o,a,l){let h=o.morphTargetInfluences,u=a.morphAttributes.position||a.morphAttributes.normal||a.morphAttributes.color,m=u!==void 0?u.length:0,x=n.get(a);if(x===void 0||x.count!==m){let M=function(){b.dispose(),n.delete(a),a.removeEventListener("dispose",M)};x!==void 0&&x.texture.dispose();let f=a.morphAttributes.position!==void 0,v=a.morphAttributes.normal!==void 0,d=a.morphAttributes.color!==void 0,y=a.morphAttributes.position||[],c=a.morphAttributes.normal||[],_=a.morphAttributes.color||[],p=0;f===!0&&(p=1),v===!0&&(p=2),d===!0&&(p=3);let g=a.attributes.position.count*p,T=1;g>t.maxTextureSize&&(T=Math.ceil(g/t.maxTextureSize),g=t.maxTextureSize);let S=new Float32Array(g*T*4*m),b=new Ro(S,g,T,m);b.type=Ln,b.needsUpdate=!0;let w=p*4;for(let E=0;E<m;E++){let F=y[E],U=c[E],R=_[E],N=g*T*4*E;for(let V=0;V<F.count;V++){let z=V*w;f===!0&&(s.fromBufferAttribute(F,V),S[N+z+0]=s.x,S[N+z+1]=s.y,S[N+z+2]=s.z,S[N+z+3]=0),v===!0&&(s.fromBufferAttribute(U,V),S[N+z+4]=s.x,S[N+z+5]=s.y,S[N+z+6]=s.z,S[N+z+7]=0),d===!0&&(s.fromBufferAttribute(R,V),S[N+z+8]=s.x,S[N+z+9]=s.y,S[N+z+10]=s.z,S[N+z+11]=R.itemSize===4?s.w:1)}}x={count:m,texture:b,size:new lt(g,T)},n.set(a,x),a.addEventListener("dispose",M)}if(o.isInstancedMesh===!0&&o.morphTexture!==null)l.getUniforms().setValue(i,"morphTexture",o.morphTexture,e);else{let f=0;for(let d=0;d<h.length;d++)f+=h[d];let v=a.morphTargetsRelative?1:1-f;l.getUniforms().setValue(i,"morphTargetBaseInfluence",v),l.getUniforms().setValue(i,"morphTargetInfluences",h)}l.getUniforms().setValue(i,"morphTargetsTexture",x.texture,e),l.getUniforms().setValue(i,"morphTargetsTextureSize",x.size)}return{update:r}}function bg(i,t,e,n){let s=new WeakMap;function r(l){let h=n.render.frame,u=l.geometry,m=t.get(l,u);if(s.get(m)!==h&&(t.update(m),s.set(m,h)),l.isInstancedMesh&&(l.hasEventListener("dispose",a)===!1&&l.addEventListener("dispose",a),s.get(l)!==h&&(e.update(l.instanceMatrix,i.ARRAY_BUFFER),l.instanceColor!==null&&e.update(l.instanceColor,i.ARRAY_BUFFER),s.set(l,h))),l.isSkinnedMesh){let x=l.skeleton;s.get(x)!==h&&(x.update(),s.set(x,h))}return m}function o(){s=new WeakMap}function a(l){let h=l.target;h.removeEventListener("dispose",a),e.remove(h.instanceMatrix),h.instanceColor!==null&&e.remove(h.instanceColor)}return{update:r,dispose:o}}var No=class extends nn{constructor(t,e,n,s,r,o,a,l,h,u=Ss){if(u!==Ss&&u!==Is)throw new Error("DepthTexture format must be either THREE.DepthFormat or THREE.DepthStencilFormat");n===void 0&&u===Ss&&(n=ki),n===void 0&&u===Is&&(n=Fs),super(null,s,r,o,a,l,u,n,h),this.isDepthTexture=!0,this.image={width:t,height:e},this.magFilter=a!==void 0?a:hn,this.minFilter=l!==void 0?l:hn,this.flipY=!1,this.generateMipmaps=!1,this.compareFunction=null}copy(t){return super.copy(t),this.compareFunction=t.compareFunction,this}toJSON(t){let e=super.toJSON(t);return this.compareFunction!==null&&(e.compareFunction=this.compareFunction),e}},hf=new nn,fu=new No(1,1),uf=new Ro,ff=new $l,df=new Lo,du=[],pu=[],mu=new Float32Array(16),gu=new Float32Array(9),xu=new Float32Array(4);function Ns(i,t,e){let n=i[0];if(n<=0||n>0)return i;let s=t*e,r=du[s];if(r===void 0&&(r=new Float32Array(s),du[s]=r),t!==0){n.toArray(r,0);for(let o=1,a=0;o!==t;++o)a+=e,i[o].toArray(r,a)}return r}function Oe(i,t){if(i.length!==t.length)return!1;for(let e=0,n=i.length;e<n;e++)if(i[e]!==t[e])return!1;return!0}function He(i,t){for(let e=0,n=t.length;e<n;e++)i[e]=t[e]}function ea(i,t){let e=pu[t];e===void 0&&(e=new Int32Array(t),pu[t]=e);for(let n=0;n!==t;++n)e[n]=i.allocateTextureUnit();return e}function Ag(i,t){let e=this.cache;e[0]!==t&&(i.uniform1f(this.addr,t),e[0]=t)}function Rg(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y)&&(i.uniform2f(this.addr,t.x,t.y),e[0]=t.x,e[1]=t.y);else{if(Oe(e,t))return;i.uniform2fv(this.addr,t),He(e,t)}}function Cg(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z)&&(i.uniform3f(this.addr,t.x,t.y,t.z),e[0]=t.x,e[1]=t.y,e[2]=t.z);else if(t.r!==void 0)(e[0]!==t.r||e[1]!==t.g||e[2]!==t.b)&&(i.uniform3f(this.addr,t.r,t.g,t.b),e[0]=t.r,e[1]=t.g,e[2]=t.b);else{if(Oe(e,t))return;i.uniform3fv(this.addr,t),He(e,t)}}function Fg(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z||e[3]!==t.w)&&(i.uniform4f(this.addr,t.x,t.y,t.z,t.w),e[0]=t.x,e[1]=t.y,e[2]=t.z,e[3]=t.w);else{if(Oe(e,t))return;i.uniform4fv(this.addr,t),He(e,t)}}function Ig(i,t){let e=this.cache,n=t.elements;if(n===void 0){if(Oe(e,t))return;i.uniformMatrix2fv(this.addr,!1,t),He(e,t)}else{if(Oe(e,n))return;xu.set(n),i.uniformMatrix2fv(this.addr,!1,xu),He(e,n)}}function Pg(i,t){let e=this.cache,n=t.elements;if(n===void 0){if(Oe(e,t))return;i.uniformMatrix3fv(this.addr,!1,t),He(e,t)}else{if(Oe(e,n))return;gu.set(n),i.uniformMatrix3fv(this.addr,!1,gu),He(e,n)}}function Lg(i,t){let e=this.cache,n=t.elements;if(n===void 0){if(Oe(e,t))return;i.uniformMatrix4fv(this.addr,!1,t),He(e,t)}else{if(Oe(e,n))return;mu.set(n),i.uniformMatrix4fv(this.addr,!1,mu),He(e,n)}}function Dg(i,t){let e=this.cache;e[0]!==t&&(i.uniform1i(this.addr,t),e[0]=t)}function Ug(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y)&&(i.uniform2i(this.addr,t.x,t.y),e[0]=t.x,e[1]=t.y);else{if(Oe(e,t))return;i.uniform2iv(this.addr,t),He(e,t)}}function Ng(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z)&&(i.uniform3i(this.addr,t.x,t.y,t.z),e[0]=t.x,e[1]=t.y,e[2]=t.z);else{if(Oe(e,t))return;i.uniform3iv(this.addr,t),He(e,t)}}function Bg(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z||e[3]!==t.w)&&(i.uniform4i(this.addr,t.x,t.y,t.z,t.w),e[0]=t.x,e[1]=t.y,e[2]=t.z,e[3]=t.w);else{if(Oe(e,t))return;i.uniform4iv(this.addr,t),He(e,t)}}function zg(i,t){let e=this.cache;e[0]!==t&&(i.uniform1ui(this.addr,t),e[0]=t)}function Og(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y)&&(i.uniform2ui(this.addr,t.x,t.y),e[0]=t.x,e[1]=t.y);else{if(Oe(e,t))return;i.uniform2uiv(this.addr,t),He(e,t)}}function Hg(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z)&&(i.uniform3ui(this.addr,t.x,t.y,t.z),e[0]=t.x,e[1]=t.y,e[2]=t.z);else{if(Oe(e,t))return;i.uniform3uiv(this.addr,t),He(e,t)}}function kg(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z||e[3]!==t.w)&&(i.uniform4ui(this.addr,t.x,t.y,t.z,t.w),e[0]=t.x,e[1]=t.y,e[2]=t.z,e[3]=t.w);else{if(Oe(e,t))return;i.uniform4uiv(this.addr,t),He(e,t)}}function Gg(i,t,e){let n=this.cache,s=e.allocateTextureUnit();n[0]!==s&&(i.uniform1i(this.addr,s),n[0]=s);let r;this.type===i.SAMPLER_2D_SHADOW?(fu.compareFunction=rf,r=fu):r=hf,e.setTexture2D(t||r,s)}function Vg(i,t,e){let n=this.cache,s=e.allocateTextureUnit();n[0]!==s&&(i.uniform1i(this.addr,s),n[0]=s),e.setTexture3D(t||ff,s)}function Wg(i,t,e){let n=this.cache,s=e.allocateTextureUnit();n[0]!==s&&(i.uniform1i(this.addr,s),n[0]=s),e.setTextureCube(t||df,s)}function Xg(i,t,e){let n=this.cache,s=e.allocateTextureUnit();n[0]!==s&&(i.uniform1i(this.addr,s),n[0]=s),e.setTexture2DArray(t||uf,s)}function qg(i){switch(i){case 5126:return Ag;case 35664:return Rg;case 35665:return Cg;case 35666:return Fg;case 35674:return Ig;case 35675:return Pg;case 35676:return Lg;case 5124:case 35670:return Dg;case 35667:case 35671:return Ug;case 35668:case 35672:return Ng;case 35669:case 35673:return Bg;case 5125:return zg;case 36294:return Og;case 36295:return Hg;case 36296:return kg;case 35678:case 36198:case 36298:case 36306:case 35682:return Gg;case 35679:case 36299:case 36307:return Vg;case 35680:case 36300:case 36308:case 36293:return Wg;case 36289:case 36303:case 36311:case 36292:return Xg}}function Yg(i,t){i.uniform1fv(this.addr,t)}function Zg(i,t){let e=Ns(t,this.size,2);i.uniform2fv(this.addr,e)}function $g(i,t){let e=Ns(t,this.size,3);i.uniform3fv(this.addr,e)}function Jg(i,t){let e=Ns(t,this.size,4);i.uniform4fv(this.addr,e)}function Kg(i,t){let e=Ns(t,this.size,4);i.uniformMatrix2fv(this.addr,!1,e)}function jg(i,t){let e=Ns(t,this.size,9);i.uniformMatrix3fv(this.addr,!1,e)}function Qg(i,t){let e=Ns(t,this.size,16);i.uniformMatrix4fv(this.addr,!1,e)}function tx(i,t){i.uniform1iv(this.addr,t)}function ex(i,t){i.uniform2iv(this.addr,t)}function nx(i,t){i.uniform3iv(this.addr,t)}function ix(i,t){i.uniform4iv(this.addr,t)}function sx(i,t){i.uniform1uiv(this.addr,t)}function rx(i,t){i.uniform2uiv(this.addr,t)}function ox(i,t){i.uniform3uiv(this.addr,t)}function ax(i,t){i.uniform4uiv(this.addr,t)}function lx(i,t,e){let n=this.cache,s=t.length,r=ea(e,s);Oe(n,r)||(i.uniform1iv(this.addr,r),He(n,r));for(let o=0;o!==s;++o)e.setTexture2D(t[o]||hf,r[o])}function cx(i,t,e){let n=this.cache,s=t.length,r=ea(e,s);Oe(n,r)||(i.uniform1iv(this.addr,r),He(n,r));for(let o=0;o!==s;++o)e.setTexture3D(t[o]||ff,r[o])}function hx(i,t,e){let n=this.cache,s=t.length,r=ea(e,s);Oe(n,r)||(i.uniform1iv(this.addr,r),He(n,r));for(let o=0;o!==s;++o)e.setTextureCube(t[o]||df,r[o])}function ux(i,t,e){let n=this.cache,s=t.length,r=ea(e,s);Oe(n,r)||(i.uniform1iv(this.addr,r),He(n,r));for(let o=0;o!==s;++o)e.setTexture2DArray(t[o]||uf,r[o])}function fx(i){switch(i){case 5126:return Yg;case 35664:return Zg;case 35665:return $g;case 35666:return Jg;case 35674:return Kg;case 35675:return jg;case 35676:return Qg;case 5124:case 35670:return tx;case 35667:case 35671:return ex;case 35668:case 35672:return nx;case 35669:case 35673:return ix;case 5125:return sx;case 36294:return rx;case 36295:return ox;case 36296:return ax;case 35678:case 36198:case 36298:case 36306:case 35682:return lx;case 35679:case 36299:case 36307:return cx;case 35680:case 36300:case 36308:case 36293:return hx;case 36289:case 36303:case 36311:case 36292:return ux}}var jl=class{constructor(t,e,n){this.id=t,this.addr=n,this.cache=[],this.type=e.type,this.setValue=qg(e.type)}},Ql=class{constructor(t,e,n){this.id=t,this.addr=n,this.cache=[],this.type=e.type,this.size=e.size,this.setValue=fx(e.type)}},tc=class{constructor(t){this.id=t,this.seq=[],this.map={}}setValue(t,e,n){let s=this.seq;for(let r=0,o=s.length;r!==o;++r){let a=s[r];a.setValue(t,e[a.id],n)}}},Qa=/(\w+)(\])?(\[|\.)?/g;function yu(i,t){i.seq.push(t),i.map[t.id]=t}function dx(i,t,e){let n=i.name,s=n.length;for(Qa.lastIndex=0;;){let r=Qa.exec(n),o=Qa.lastIndex,a=r[1],l=r[2]==="]",h=r[3];if(l&&(a=a|0),h===void 0||h==="["&&o+2===s){yu(e,h===void 0?new jl(a,i,t):new Ql(a,i,t));break}else{let m=e.map[a];m===void 0&&(m=new tc(a),yu(e,m)),e=m}}}var ws=class{constructor(t,e){this.seq=[],this.map={};let n=t.getProgramParameter(e,t.ACTIVE_UNIFORMS);for(let s=0;s<n;++s){let r=t.getActiveUniform(e,s),o=t.getUniformLocation(e,r.name);dx(r,o,this)}}setValue(t,e,n,s){let r=this.map[e];r!==void 0&&r.setValue(t,n,s)}setOptional(t,e,n){let s=e[n];s!==void 0&&this.setValue(t,n,s)}static upload(t,e,n,s){for(let r=0,o=e.length;r!==o;++r){let a=e[r],l=n[a.id];l.needsUpdate!==!1&&a.setValue(t,l.value,s)}}static seqWithValue(t,e){let n=[];for(let s=0,r=t.length;s!==r;++s){let o=t[s];o.id in e&&n.push(o)}return n}};function _u(i,t,e){let n=i.createShader(t);return i.shaderSource(n,e),i.compileShader(n),n}var px=37297,mx=0;function gx(i,t){let e=i.split(`
`),n=[],s=Math.max(t-6,0),r=Math.min(t+6,e.length);for(let o=s;o<r;o++){let a=o+1;n.push(`${a===t?">":" "} ${a}: ${e[o]}`)}return n.join(`
`)}var vu=new $t;function xx(i){ie._getMatrix(vu,ie.workingColorSpace,i);let t=`mat3( ${vu.elements.map(e=>e.toFixed(4))} )`;switch(ie.getTransfer(i)){case ta:return[t,"LinearTransferOETF"];case ye:return[t,"sRGBTransferOETF"];default:return console.warn("THREE.WebGLProgram: Unsupported color space: ",i),[t,"LinearTransferOETF"]}}function Mu(i,t,e){let n=i.getShaderParameter(t,i.COMPILE_STATUS),s=i.getShaderInfoLog(t).trim();if(n&&s==="")return"";let r=/ERROR: 0:(\d+)/.exec(s);if(r){let o=parseInt(r[1]);return e.toUpperCase()+`

`+s+`

`+gx(i.getShaderSource(t),o)}else return s}function yx(i,t){let e=xx(t);return[`vec4 ${i}( vec4 value ) {`,`	return ${e[1]}( vec4( value.rgb * ${e[0]}, value.a ) );`,"}"].join(`
`)}function _x(i,t){let e;switch(t){case Wd:e="Linear";break;case Xd:e="Reinhard";break;case qd:e="Cineon";break;case Yd:e="ACESFilmic";break;case $d:e="AgX";break;case Jd:e="Neutral";break;case Zd:e="Custom";break;default:console.warn("THREE.WebGLProgram: Unsupported toneMapping:",t),e="Linear"}return"vec3 "+i+"( vec3 color ) { return "+e+"ToneMapping( color ); }"}var ro=new B;function vx(){ie.getLuminanceCoefficients(ro);let i=ro.x.toFixed(4),t=ro.y.toFixed(4),e=ro.z.toFixed(4);return["float luminance( const in vec3 rgb ) {",`	const vec3 weights = vec3( ${i}, ${t}, ${e} );`,"	return dot( weights, rgb );","}"].join(`
`)}function Mx(i){return[i.extensionClipCullDistance?"#extension GL_ANGLE_clip_cull_distance : require":"",i.extensionMultiDraw?"#extension GL_ANGLE_multi_draw : require":""].filter(ar).join(`
`)}function Ex(i){let t=[];for(let e in i){let n=i[e];n!==!1&&t.push("#define "+e+" "+n)}return t.join(`
`)}function Sx(i,t){let e={},n=i.getProgramParameter(t,i.ACTIVE_ATTRIBUTES);for(let s=0;s<n;s++){let r=i.getActiveAttrib(t,s),o=r.name,a=1;r.type===i.FLOAT_MAT2&&(a=2),r.type===i.FLOAT_MAT3&&(a=3),r.type===i.FLOAT_MAT4&&(a=4),e[o]={type:r.type,location:i.getAttribLocation(t,o),locationSize:a}}return e}function ar(i){return i!==""}function Eu(i,t){let e=t.numSpotLightShadows+t.numSpotLightMaps-t.numSpotLightShadowsWithMaps;return i.replace(/NUM_DIR_LIGHTS/g,t.numDirLights).replace(/NUM_SPOT_LIGHTS/g,t.numSpotLights).replace(/NUM_SPOT_LIGHT_MAPS/g,t.numSpotLightMaps).replace(/NUM_SPOT_LIGHT_COORDS/g,e).replace(/NUM_RECT_AREA_LIGHTS/g,t.numRectAreaLights).replace(/NUM_POINT_LIGHTS/g,t.numPointLights).replace(/NUM_HEMI_LIGHTS/g,t.numHemiLights).replace(/NUM_DIR_LIGHT_SHADOWS/g,t.numDirLightShadows).replace(/NUM_SPOT_LIGHT_SHADOWS_WITH_MAPS/g,t.numSpotLightShadowsWithMaps).replace(/NUM_SPOT_LIGHT_SHADOWS/g,t.numSpotLightShadows).replace(/NUM_POINT_LIGHT_SHADOWS/g,t.numPointLightShadows)}function Su(i,t){return i.replace(/NUM_CLIPPING_PLANES/g,t.numClippingPlanes).replace(/UNION_CLIPPING_PLANES/g,t.numClippingPlanes-t.numClipIntersection)}var Tx=/^[ \t]*#include +<([\w\d./]+)>/gm;function ec(i){return i.replace(Tx,bx)}var wx=new Map;function bx(i,t){let e=jt[t];if(e===void 0){let n=wx.get(t);if(n!==void 0)e=jt[n],console.warn('THREE.WebGLRenderer: Shader chunk "%s" has been deprecated. Use "%s" instead.',t,n);else throw new Error("Can not resolve #include <"+t+">")}return ec(e)}var Ax=/#pragma unroll_loop_start\s+for\s*\(\s*int\s+i\s*=\s*(\d+)\s*;\s*i\s*<\s*(\d+)\s*;\s*i\s*\+\+\s*\)\s*{([\s\S]+?)}\s+#pragma unroll_loop_end/g;function Tu(i){return i.replace(Ax,Rx)}function Rx(i,t,e,n){let s="";for(let r=parseInt(t);r<parseInt(e);r++)s+=n.replace(/\[\s*i\s*\]/g,"[ "+r+" ]").replace(/UNROLLED_LOOP_INDEX/g,r);return s}function wu(i){let t=`precision ${i.precision} float;
	precision ${i.precision} int;
	precision ${i.precision} sampler2D;
	precision ${i.precision} samplerCube;
	precision ${i.precision} sampler3D;
	precision ${i.precision} sampler2DArray;
	precision ${i.precision} sampler2DShadow;
	precision ${i.precision} samplerCubeShadow;
	precision ${i.precision} sampler2DArrayShadow;
	precision ${i.precision} isampler2D;
	precision ${i.precision} isampler3D;
	precision ${i.precision} isamplerCube;
	precision ${i.precision} isampler2DArray;
	precision ${i.precision} usampler2D;
	precision ${i.precision} usampler3D;
	precision ${i.precision} usamplerCube;
	precision ${i.precision} usampler2DArray;
	`;return i.precision==="highp"?t+=`
#define HIGH_PRECISION`:i.precision==="mediump"?t+=`
#define MEDIUM_PRECISION`:i.precision==="lowp"&&(t+=`
#define LOW_PRECISION`),t}function Cx(i){let t="SHADOWMAP_TYPE_BASIC";return i.shadowMapType===qu?t="SHADOWMAP_TYPE_PCF":i.shadowMapType===Sd?t="SHADOWMAP_TYPE_PCF_SOFT":i.shadowMapType===qn&&(t="SHADOWMAP_TYPE_VSM"),t}function Fx(i){let t="ENVMAP_TYPE_CUBE";if(i.envMap)switch(i.envMapMode){case Rs:case Cs:t="ENVMAP_TYPE_CUBE";break;case Qo:t="ENVMAP_TYPE_CUBE_UV";break}return t}function Ix(i){let t="ENVMAP_MODE_REFLECTION";if(i.envMap)switch(i.envMapMode){case Cs:t="ENVMAP_MODE_REFRACTION";break}return t}function Px(i){let t="ENVMAP_BLENDING_NONE";if(i.envMap)switch(i.combine){case Uc:t="ENVMAP_BLENDING_MULTIPLY";break;case Gd:t="ENVMAP_BLENDING_MIX";break;case Vd:t="ENVMAP_BLENDING_ADD";break}return t}function Lx(i){let t=i.envMapCubeUVHeight;if(t===null)return null;let e=Math.log2(t)-2,n=1/t;return{texelWidth:1/(3*Math.max(Math.pow(2,e),112)),texelHeight:n,maxMip:e}}function Dx(i,t,e,n){let s=i.getContext(),r=e.defines,o=e.vertexShader,a=e.fragmentShader,l=Cx(e),h=Fx(e),u=Ix(e),m=Px(e),x=Lx(e),f=Mx(e),v=Ex(r),d=s.createProgram(),y,c,_=e.glslVersion?"#version "+e.glslVersion+`
`:"";e.isRawShaderMaterial?(y=["#define SHADER_TYPE "+e.shaderType,"#define SHADER_NAME "+e.shaderName,v].filter(ar).join(`
`),y.length>0&&(y+=`
`),c=["#define SHADER_TYPE "+e.shaderType,"#define SHADER_NAME "+e.shaderName,v].filter(ar).join(`
`),c.length>0&&(c+=`
`)):(y=[wu(e),"#define SHADER_TYPE "+e.shaderType,"#define SHADER_NAME "+e.shaderName,v,e.extensionClipCullDistance?"#define USE_CLIP_DISTANCE":"",e.batching?"#define USE_BATCHING":"",e.batchingColor?"#define USE_BATCHING_COLOR":"",e.instancing?"#define USE_INSTANCING":"",e.instancingColor?"#define USE_INSTANCING_COLOR":"",e.instancingMorph?"#define USE_INSTANCING_MORPH":"",e.useFog&&e.fog?"#define USE_FOG":"",e.useFog&&e.fogExp2?"#define FOG_EXP2":"",e.map?"#define USE_MAP":"",e.envMap?"#define USE_ENVMAP":"",e.envMap?"#define "+u:"",e.lightMap?"#define USE_LIGHTMAP":"",e.aoMap?"#define USE_AOMAP":"",e.bumpMap?"#define USE_BUMPMAP":"",e.normalMap?"#define USE_NORMALMAP":"",e.normalMapObjectSpace?"#define USE_NORMALMAP_OBJECTSPACE":"",e.normalMapTangentSpace?"#define USE_NORMALMAP_TANGENTSPACE":"",e.displacementMap?"#define USE_DISPLACEMENTMAP":"",e.emissiveMap?"#define USE_EMISSIVEMAP":"",e.anisotropy?"#define USE_ANISOTROPY":"",e.anisotropyMap?"#define USE_ANISOTROPYMAP":"",e.clearcoatMap?"#define USE_CLEARCOATMAP":"",e.clearcoatRoughnessMap?"#define USE_CLEARCOAT_ROUGHNESSMAP":"",e.clearcoatNormalMap?"#define USE_CLEARCOAT_NORMALMAP":"",e.iridescenceMap?"#define USE_IRIDESCENCEMAP":"",e.iridescenceThicknessMap?"#define USE_IRIDESCENCE_THICKNESSMAP":"",e.specularMap?"#define USE_SPECULARMAP":"",e.specularColorMap?"#define USE_SPECULAR_COLORMAP":"",e.specularIntensityMap?"#define USE_SPECULAR_INTENSITYMAP":"",e.roughnessMap?"#define USE_ROUGHNESSMAP":"",e.metalnessMap?"#define USE_METALNESSMAP":"",e.alphaMap?"#define USE_ALPHAMAP":"",e.alphaHash?"#define USE_ALPHAHASH":"",e.transmission?"#define USE_TRANSMISSION":"",e.transmissionMap?"#define USE_TRANSMISSIONMAP":"",e.thicknessMap?"#define USE_THICKNESSMAP":"",e.sheenColorMap?"#define USE_SHEEN_COLORMAP":"",e.sheenRoughnessMap?"#define USE_SHEEN_ROUGHNESSMAP":"",e.mapUv?"#define MAP_UV "+e.mapUv:"",e.alphaMapUv?"#define ALPHAMAP_UV "+e.alphaMapUv:"",e.lightMapUv?"#define LIGHTMAP_UV "+e.lightMapUv:"",e.aoMapUv?"#define AOMAP_UV "+e.aoMapUv:"",e.emissiveMapUv?"#define EMISSIVEMAP_UV "+e.emissiveMapUv:"",e.bumpMapUv?"#define BUMPMAP_UV "+e.bumpMapUv:"",e.normalMapUv?"#define NORMALMAP_UV "+e.normalMapUv:"",e.displacementMapUv?"#define DISPLACEMENTMAP_UV "+e.displacementMapUv:"",e.metalnessMapUv?"#define METALNESSMAP_UV "+e.metalnessMapUv:"",e.roughnessMapUv?"#define ROUGHNESSMAP_UV "+e.roughnessMapUv:"",e.anisotropyMapUv?"#define ANISOTROPYMAP_UV "+e.anisotropyMapUv:"",e.clearcoatMapUv?"#define CLEARCOATMAP_UV "+e.clearcoatMapUv:"",e.clearcoatNormalMapUv?"#define CLEARCOAT_NORMALMAP_UV "+e.clearcoatNormalMapUv:"",e.clearcoatRoughnessMapUv?"#define CLEARCOAT_ROUGHNESSMAP_UV "+e.clearcoatRoughnessMapUv:"",e.iridescenceMapUv?"#define IRIDESCENCEMAP_UV "+e.iridescenceMapUv:"",e.iridescenceThicknessMapUv?"#define IRIDESCENCE_THICKNESSMAP_UV "+e.iridescenceThicknessMapUv:"",e.sheenColorMapUv?"#define SHEEN_COLORMAP_UV "+e.sheenColorMapUv:"",e.sheenRoughnessMapUv?"#define SHEEN_ROUGHNESSMAP_UV "+e.sheenRoughnessMapUv:"",e.specularMapUv?"#define SPECULARMAP_UV "+e.specularMapUv:"",e.specularColorMapUv?"#define SPECULAR_COLORMAP_UV "+e.specularColorMapUv:"",e.specularIntensityMapUv?"#define SPECULAR_INTENSITYMAP_UV "+e.specularIntensityMapUv:"",e.transmissionMapUv?"#define TRANSMISSIONMAP_UV "+e.transmissionMapUv:"",e.thicknessMapUv?"#define THICKNESSMAP_UV "+e.thicknessMapUv:"",e.vertexTangents&&e.flatShading===!1?"#define USE_TANGENT":"",e.vertexColors?"#define USE_COLOR":"",e.vertexAlphas?"#define USE_COLOR_ALPHA":"",e.vertexUv1s?"#define USE_UV1":"",e.vertexUv2s?"#define USE_UV2":"",e.vertexUv3s?"#define USE_UV3":"",e.pointsUvs?"#define USE_POINTS_UV":"",e.flatShading?"#define FLAT_SHADED":"",e.skinning?"#define USE_SKINNING":"",e.morphTargets?"#define USE_MORPHTARGETS":"",e.morphNormals&&e.flatShading===!1?"#define USE_MORPHNORMALS":"",e.morphColors?"#define USE_MORPHCOLORS":"",e.morphTargetsCount>0?"#define MORPHTARGETS_TEXTURE_STRIDE "+e.morphTextureStride:"",e.morphTargetsCount>0?"#define MORPHTARGETS_COUNT "+e.morphTargetsCount:"",e.doubleSided?"#define DOUBLE_SIDED":"",e.flipSided?"#define FLIP_SIDED":"",e.shadowMapEnabled?"#define USE_SHADOWMAP":"",e.shadowMapEnabled?"#define "+l:"",e.sizeAttenuation?"#define USE_SIZEATTENUATION":"",e.numLightProbes>0?"#define USE_LIGHT_PROBES":"",e.logarithmicDepthBuffer?"#define USE_LOGDEPTHBUF":"",e.reverseDepthBuffer?"#define USE_REVERSEDEPTHBUF":"","uniform mat4 modelMatrix;","uniform mat4 modelViewMatrix;","uniform mat4 projectionMatrix;","uniform mat4 viewMatrix;","uniform mat3 normalMatrix;","uniform vec3 cameraPosition;","uniform bool isOrthographic;","#ifdef USE_INSTANCING","	attribute mat4 instanceMatrix;","#endif","#ifdef USE_INSTANCING_COLOR","	attribute vec3 instanceColor;","#endif","#ifdef USE_INSTANCING_MORPH","	uniform sampler2D morphTexture;","#endif","attribute vec3 position;","attribute vec3 normal;","attribute vec2 uv;","#ifdef USE_UV1","	attribute vec2 uv1;","#endif","#ifdef USE_UV2","	attribute vec2 uv2;","#endif","#ifdef USE_UV3","	attribute vec2 uv3;","#endif","#ifdef USE_TANGENT","	attribute vec4 tangent;","#endif","#if defined( USE_COLOR_ALPHA )","	attribute vec4 color;","#elif defined( USE_COLOR )","	attribute vec3 color;","#endif","#ifdef USE_SKINNING","	attribute vec4 skinIndex;","	attribute vec4 skinWeight;","#endif",`
`].filter(ar).join(`
`),c=[wu(e),"#define SHADER_TYPE "+e.shaderType,"#define SHADER_NAME "+e.shaderName,v,e.useFog&&e.fog?"#define USE_FOG":"",e.useFog&&e.fogExp2?"#define FOG_EXP2":"",e.alphaToCoverage?"#define ALPHA_TO_COVERAGE":"",e.map?"#define USE_MAP":"",e.matcap?"#define USE_MATCAP":"",e.envMap?"#define USE_ENVMAP":"",e.envMap?"#define "+h:"",e.envMap?"#define "+u:"",e.envMap?"#define "+m:"",x?"#define CUBEUV_TEXEL_WIDTH "+x.texelWidth:"",x?"#define CUBEUV_TEXEL_HEIGHT "+x.texelHeight:"",x?"#define CUBEUV_MAX_MIP "+x.maxMip+".0":"",e.lightMap?"#define USE_LIGHTMAP":"",e.aoMap?"#define USE_AOMAP":"",e.bumpMap?"#define USE_BUMPMAP":"",e.normalMap?"#define USE_NORMALMAP":"",e.normalMapObjectSpace?"#define USE_NORMALMAP_OBJECTSPACE":"",e.normalMapTangentSpace?"#define USE_NORMALMAP_TANGENTSPACE":"",e.emissiveMap?"#define USE_EMISSIVEMAP":"",e.anisotropy?"#define USE_ANISOTROPY":"",e.anisotropyMap?"#define USE_ANISOTROPYMAP":"",e.clearcoat?"#define USE_CLEARCOAT":"",e.clearcoatMap?"#define USE_CLEARCOATMAP":"",e.clearcoatRoughnessMap?"#define USE_CLEARCOAT_ROUGHNESSMAP":"",e.clearcoatNormalMap?"#define USE_CLEARCOAT_NORMALMAP":"",e.dispersion?"#define USE_DISPERSION":"",e.iridescence?"#define USE_IRIDESCENCE":"",e.iridescenceMap?"#define USE_IRIDESCENCEMAP":"",e.iridescenceThicknessMap?"#define USE_IRIDESCENCE_THICKNESSMAP":"",e.specularMap?"#define USE_SPECULARMAP":"",e.specularColorMap?"#define USE_SPECULAR_COLORMAP":"",e.specularIntensityMap?"#define USE_SPECULAR_INTENSITYMAP":"",e.roughnessMap?"#define USE_ROUGHNESSMAP":"",e.metalnessMap?"#define USE_METALNESSMAP":"",e.alphaMap?"#define USE_ALPHAMAP":"",e.alphaTest?"#define USE_ALPHATEST":"",e.alphaHash?"#define USE_ALPHAHASH":"",e.sheen?"#define USE_SHEEN":"",e.sheenColorMap?"#define USE_SHEEN_COLORMAP":"",e.sheenRoughnessMap?"#define USE_SHEEN_ROUGHNESSMAP":"",e.transmission?"#define USE_TRANSMISSION":"",e.transmissionMap?"#define USE_TRANSMISSIONMAP":"",e.thicknessMap?"#define USE_THICKNESSMAP":"",e.vertexTangents&&e.flatShading===!1?"#define USE_TANGENT":"",e.vertexColors||e.instancingColor||e.batchingColor?"#define USE_COLOR":"",e.vertexAlphas?"#define USE_COLOR_ALPHA":"",e.vertexUv1s?"#define USE_UV1":"",e.vertexUv2s?"#define USE_UV2":"",e.vertexUv3s?"#define USE_UV3":"",e.pointsUvs?"#define USE_POINTS_UV":"",e.gradientMap?"#define USE_GRADIENTMAP":"",e.flatShading?"#define FLAT_SHADED":"",e.doubleSided?"#define DOUBLE_SIDED":"",e.flipSided?"#define FLIP_SIDED":"",e.shadowMapEnabled?"#define USE_SHADOWMAP":"",e.shadowMapEnabled?"#define "+l:"",e.premultipliedAlpha?"#define PREMULTIPLIED_ALPHA":"",e.numLightProbes>0?"#define USE_LIGHT_PROBES":"",e.decodeVideoTexture?"#define DECODE_VIDEO_TEXTURE":"",e.decodeVideoTextureEmissive?"#define DECODE_VIDEO_TEXTURE_EMISSIVE":"",e.logarithmicDepthBuffer?"#define USE_LOGDEPTHBUF":"",e.reverseDepthBuffer?"#define USE_REVERSEDEPTHBUF":"","uniform mat4 viewMatrix;","uniform vec3 cameraPosition;","uniform bool isOrthographic;",e.toneMapping!==_i?"#define TONE_MAPPING":"",e.toneMapping!==_i?jt.tonemapping_pars_fragment:"",e.toneMapping!==_i?_x("toneMapping",e.toneMapping):"",e.dithering?"#define DITHERING":"",e.opaque?"#define OPAQUE":"",jt.colorspace_pars_fragment,yx("linearToOutputTexel",e.outputColorSpace),vx(),e.useDepthPacking?"#define DEPTH_PACKING "+e.depthPacking:"",`
`].filter(ar).join(`
`)),o=ec(o),o=Eu(o,e),o=Su(o,e),a=ec(a),a=Eu(a,e),a=Su(a,e),o=Tu(o),a=Tu(a),e.isRawShaderMaterial!==!0&&(_=`#version 300 es
`,y=[f,"#define attribute in","#define varying out","#define texture2D texture"].join(`
`)+`
`+y,c=["#define varying in",e.glslVersion===Oh?"":"layout(location = 0) out highp vec4 pc_fragColor;",e.glslVersion===Oh?"":"#define gl_FragColor pc_fragColor","#define gl_FragDepthEXT gl_FragDepth","#define texture2D texture","#define textureCube texture","#define texture2DProj textureProj","#define texture2DLodEXT textureLod","#define texture2DProjLodEXT textureProjLod","#define textureCubeLodEXT textureLod","#define texture2DGradEXT textureGrad","#define texture2DProjGradEXT textureProjGrad","#define textureCubeGradEXT textureGrad"].join(`
`)+`
`+c);let p=_+y+o,g=_+c+a,T=_u(s,s.VERTEX_SHADER,p),S=_u(s,s.FRAGMENT_SHADER,g);s.attachShader(d,T),s.attachShader(d,S),e.index0AttributeName!==void 0?s.bindAttribLocation(d,0,e.index0AttributeName):e.morphTargets===!0&&s.bindAttribLocation(d,0,"position"),s.linkProgram(d);function b(F){if(i.debug.checkShaderErrors){let U=s.getProgramInfoLog(d).trim(),R=s.getShaderInfoLog(T).trim(),N=s.getShaderInfoLog(S).trim(),V=!0,z=!0;if(s.getProgramParameter(d,s.LINK_STATUS)===!1)if(V=!1,typeof i.debug.onShaderError=="function")i.debug.onShaderError(s,d,T,S);else{let O=Mu(s,T,"vertex"),D=Mu(s,S,"fragment");console.error("THREE.WebGLProgram: Shader Error "+s.getError()+" - VALIDATE_STATUS "+s.getProgramParameter(d,s.VALIDATE_STATUS)+`

Material Name: `+F.name+`
Material Type: `+F.type+`

Program Info Log: `+U+`
`+O+`
`+D)}else U!==""?console.warn("THREE.WebGLProgram: Program Info Log:",U):(R===""||N==="")&&(z=!1);z&&(F.diagnostics={runnable:V,programLog:U,vertexShader:{log:R,prefix:y},fragmentShader:{log:N,prefix:c}})}s.deleteShader(T),s.deleteShader(S),w=new ws(s,d),M=Sx(s,d)}let w;this.getUniforms=function(){return w===void 0&&b(this),w};let M;this.getAttributes=function(){return M===void 0&&b(this),M};let E=e.rendererExtensionParallelShaderCompile===!1;return this.isReady=function(){return E===!1&&(E=s.getProgramParameter(d,px)),E},this.destroy=function(){n.releaseStatesOfProgram(this),s.deleteProgram(d),this.program=void 0},this.type=e.shaderType,this.name=e.shaderName,this.id=mx++,this.cacheKey=t,this.usedTimes=1,this.program=d,this.vertexShader=T,this.fragmentShader=S,this}var Ux=0,nc=class{constructor(){this.shaderCache=new Map,this.materialCache=new Map}update(t){let e=t.vertexShader,n=t.fragmentShader,s=this._getShaderStage(e),r=this._getShaderStage(n),o=this._getShaderCacheForMaterial(t);return o.has(s)===!1&&(o.add(s),s.usedTimes++),o.has(r)===!1&&(o.add(r),r.usedTimes++),this}remove(t){let e=this.materialCache.get(t);for(let n of e)n.usedTimes--,n.usedTimes===0&&this.shaderCache.delete(n.code);return this.materialCache.delete(t),this}getVertexShaderID(t){return this._getShaderStage(t.vertexShader).id}getFragmentShaderID(t){return this._getShaderStage(t.fragmentShader).id}dispose(){this.shaderCache.clear(),this.materialCache.clear()}_getShaderCacheForMaterial(t){let e=this.materialCache,n=e.get(t);return n===void 0&&(n=new Set,e.set(t,n)),n}_getShaderStage(t){let e=this.shaderCache,n=e.get(t);return n===void 0&&(n=new ic(t),e.set(t,n)),n}},ic=class{constructor(t){this.id=Ux++,this.code=t,this.usedTimes=0}};function Nx(i,t,e,n,s,r,o){let a=new Co,l=new nc,h=new Set,u=[],m=s.logarithmicDepthBuffer,x=s.vertexTextures,f=s.precision,v={MeshDepthMaterial:"depth",MeshDistanceMaterial:"distanceRGBA",MeshNormalMaterial:"normal",MeshBasicMaterial:"basic",MeshLambertMaterial:"lambert",MeshPhongMaterial:"phong",MeshToonMaterial:"toon",MeshStandardMaterial:"physical",MeshPhysicalMaterial:"physical",MeshMatcapMaterial:"matcap",LineBasicMaterial:"basic",LineDashedMaterial:"dashed",PointsMaterial:"points",ShadowMaterial:"shadow",SpriteMaterial:"sprite"};function d(M){return h.add(M),M===0?"uv":`uv${M}`}function y(M,E,F,U,R){let N=U.fog,V=R.geometry,z=M.isMeshStandardMaterial?U.environment:null,O=(M.isMeshStandardMaterial?e:t).get(M.envMap||z),D=O&&O.mapping===Qo?O.image.height:null,X=v[M.type];M.precision!==null&&(f=s.getMaxPrecision(M.precision),f!==M.precision&&console.warn("THREE.WebGLProgram.getParameters:",M.precision,"not supported, using",f,"instead."));let $=V.morphAttributes.position||V.morphAttributes.normal||V.morphAttributes.color,tt=$!==void 0?$.length:0,ut=0;V.morphAttributes.position!==void 0&&(ut=1),V.morphAttributes.normal!==void 0&&(ut=2),V.morphAttributes.color!==void 0&&(ut=3);let Ct,J,st,_t;if(X){let xe=Fn[X];Ct=xe.vertexShader,J=xe.fragmentShader}else Ct=M.vertexShader,J=M.fragmentShader,l.update(M),st=l.getVertexShaderID(M),_t=l.getFragmentShaderID(M);let rt=i.getRenderTarget(),Nt=i.state.buffers.depth.getReversed(),Xt=R.isInstancedMesh===!0,Ht=R.isBatchedMesh===!0,ne=!!M.map,nt=!!M.matcap,ct=!!O,L=!!M.aoMap,Dt=!!M.lightMap,ot=!!M.bumpMap,bt=!!M.normalMap,ft=!!M.displacementMap,zt=!!M.emissiveMap,Tt=!!M.metalnessMap,P=!!M.roughnessMap,A=M.anisotropy>0,W=M.clearcoat>0,j=M.dispersion>0,it=M.iridescence>0,Q=M.sheen>0,Ft=M.transmission>0,gt=A&&!!M.anisotropyMap,wt=W&&!!M.clearcoatMap,Qt=W&&!!M.clearcoatNormalMap,at=W&&!!M.clearcoatRoughnessMap,At=it&&!!M.iridescenceMap,Ot=it&&!!M.iridescenceThicknessMap,Vt=Q&&!!M.sheenColorMap,Rt=Q&&!!M.sheenRoughnessMap,te=!!M.specularMap,Kt=!!M.specularColorMap,Se=!!M.specularIntensityMap,H=Ft&&!!M.transmissionMap,yt=Ft&&!!M.thicknessMap,K=!!M.gradientMap,et=!!M.alphaMap,Et=M.alphaTest>0,vt=!!M.alphaHash,Yt=!!M.extensions,De=_i;M.toneMapped&&(rt===null||rt.isXRRenderTarget===!0)&&(De=i.toneMapping);let Ye={shaderID:X,shaderType:M.type,shaderName:M.name,vertexShader:Ct,fragmentShader:J,defines:M.defines,customVertexShaderID:st,customFragmentShaderID:_t,isRawShaderMaterial:M.isRawShaderMaterial===!0,glslVersion:M.glslVersion,precision:f,batching:Ht,batchingColor:Ht&&R._colorsTexture!==null,instancing:Xt,instancingColor:Xt&&R.instanceColor!==null,instancingMorph:Xt&&R.morphTexture!==null,supportsVertexTextures:x,outputColorSpace:rt===null?i.outputColorSpace:rt.isXRRenderTarget===!0?rt.texture.colorSpace:Us,alphaToCoverage:!!M.alphaToCoverage,map:ne,matcap:nt,envMap:ct,envMapMode:ct&&O.mapping,envMapCubeUVHeight:D,aoMap:L,lightMap:Dt,bumpMap:ot,normalMap:bt,displacementMap:x&&ft,emissiveMap:zt,normalMapObjectSpace:bt&&M.normalMapType===tp,normalMapTangentSpace:bt&&M.normalMapType===sf,metalnessMap:Tt,roughnessMap:P,anisotropy:A,anisotropyMap:gt,clearcoat:W,clearcoatMap:wt,clearcoatNormalMap:Qt,clearcoatRoughnessMap:at,dispersion:j,iridescence:it,iridescenceMap:At,iridescenceThicknessMap:Ot,sheen:Q,sheenColorMap:Vt,sheenRoughnessMap:Rt,specularMap:te,specularColorMap:Kt,specularIntensityMap:Se,transmission:Ft,transmissionMap:H,thicknessMap:yt,gradientMap:K,opaque:M.transparent===!1&&M.blending===yi&&M.alphaToCoverage===!1,alphaMap:et,alphaTest:Et,alphaHash:vt,combine:M.combine,mapUv:ne&&d(M.map.channel),aoMapUv:L&&d(M.aoMap.channel),lightMapUv:Dt&&d(M.lightMap.channel),bumpMapUv:ot&&d(M.bumpMap.channel),normalMapUv:bt&&d(M.normalMap.channel),displacementMapUv:ft&&d(M.displacementMap.channel),emissiveMapUv:zt&&d(M.emissiveMap.channel),metalnessMapUv:Tt&&d(M.metalnessMap.channel),roughnessMapUv:P&&d(M.roughnessMap.channel),anisotropyMapUv:gt&&d(M.anisotropyMap.channel),clearcoatMapUv:wt&&d(M.clearcoatMap.channel),clearcoatNormalMapUv:Qt&&d(M.clearcoatNormalMap.channel),clearcoatRoughnessMapUv:at&&d(M.clearcoatRoughnessMap.channel),iridescenceMapUv:At&&d(M.iridescenceMap.channel),iridescenceThicknessMapUv:Ot&&d(M.iridescenceThicknessMap.channel),sheenColorMapUv:Vt&&d(M.sheenColorMap.channel),sheenRoughnessMapUv:Rt&&d(M.sheenRoughnessMap.channel),specularMapUv:te&&d(M.specularMap.channel),specularColorMapUv:Kt&&d(M.specularColorMap.channel),specularIntensityMapUv:Se&&d(M.specularIntensityMap.channel),transmissionMapUv:H&&d(M.transmissionMap.channel),thicknessMapUv:yt&&d(M.thicknessMap.channel),alphaMapUv:et&&d(M.alphaMap.channel),vertexTangents:!!V.attributes.tangent&&(bt||A),vertexColors:M.vertexColors,vertexAlphas:M.vertexColors===!0&&!!V.attributes.color&&V.attributes.color.itemSize===4,pointsUvs:R.isPoints===!0&&!!V.attributes.uv&&(ne||et),fog:!!N,useFog:M.fog===!0,fogExp2:!!N&&N.isFogExp2,flatShading:M.flatShading===!0,sizeAttenuation:M.sizeAttenuation===!0,logarithmicDepthBuffer:m,reverseDepthBuffer:Nt,skinning:R.isSkinnedMesh===!0,morphTargets:V.morphAttributes.position!==void 0,morphNormals:V.morphAttributes.normal!==void 0,morphColors:V.morphAttributes.color!==void 0,morphTargetsCount:tt,morphTextureStride:ut,numDirLights:E.directional.length,numPointLights:E.point.length,numSpotLights:E.spot.length,numSpotLightMaps:E.spotLightMap.length,numRectAreaLights:E.rectArea.length,numHemiLights:E.hemi.length,numDirLightShadows:E.directionalShadowMap.length,numPointLightShadows:E.pointShadowMap.length,numSpotLightShadows:E.spotShadowMap.length,numSpotLightShadowsWithMaps:E.numSpotLightShadowsWithMaps,numLightProbes:E.numLightProbes,numClippingPlanes:o.numPlanes,numClipIntersection:o.numIntersection,dithering:M.dithering,shadowMapEnabled:i.shadowMap.enabled&&F.length>0,shadowMapType:i.shadowMap.type,toneMapping:De,decodeVideoTexture:ne&&M.map.isVideoTexture===!0&&ie.getTransfer(M.map.colorSpace)===ye,decodeVideoTextureEmissive:zt&&M.emissiveMap.isVideoTexture===!0&&ie.getTransfer(M.emissiveMap.colorSpace)===ye,premultipliedAlpha:M.premultipliedAlpha,doubleSided:M.side===Me,flipSided:M.side===en,useDepthPacking:M.depthPacking>=0,depthPacking:M.depthPacking||0,index0AttributeName:M.index0AttributeName,extensionClipCullDistance:Yt&&M.extensions.clipCullDistance===!0&&n.has("WEBGL_clip_cull_distance"),extensionMultiDraw:(Yt&&M.extensions.multiDraw===!0||Ht)&&n.has("WEBGL_multi_draw"),rendererExtensionParallelShaderCompile:n.has("KHR_parallel_shader_compile"),customProgramCacheKey:M.customProgramCacheKey()};return Ye.vertexUv1s=h.has(1),Ye.vertexUv2s=h.has(2),Ye.vertexUv3s=h.has(3),h.clear(),Ye}function c(M){let E=[];if(M.shaderID?E.push(M.shaderID):(E.push(M.customVertexShaderID),E.push(M.customFragmentShaderID)),M.defines!==void 0)for(let F in M.defines)E.push(F),E.push(M.defines[F]);return M.isRawShaderMaterial===!1&&(_(E,M),p(E,M),E.push(i.outputColorSpace)),E.push(M.customProgramCacheKey),E.join()}function _(M,E){M.push(E.precision),M.push(E.outputColorSpace),M.push(E.envMapMode),M.push(E.envMapCubeUVHeight),M.push(E.mapUv),M.push(E.alphaMapUv),M.push(E.lightMapUv),M.push(E.aoMapUv),M.push(E.bumpMapUv),M.push(E.normalMapUv),M.push(E.displacementMapUv),M.push(E.emissiveMapUv),M.push(E.metalnessMapUv),M.push(E.roughnessMapUv),M.push(E.anisotropyMapUv),M.push(E.clearcoatMapUv),M.push(E.clearcoatNormalMapUv),M.push(E.clearcoatRoughnessMapUv),M.push(E.iridescenceMapUv),M.push(E.iridescenceThicknessMapUv),M.push(E.sheenColorMapUv),M.push(E.sheenRoughnessMapUv),M.push(E.specularMapUv),M.push(E.specularColorMapUv),M.push(E.specularIntensityMapUv),M.push(E.transmissionMapUv),M.push(E.thicknessMapUv),M.push(E.combine),M.push(E.fogExp2),M.push(E.sizeAttenuation),M.push(E.morphTargetsCount),M.push(E.morphAttributeCount),M.push(E.numDirLights),M.push(E.numPointLights),M.push(E.numSpotLights),M.push(E.numSpotLightMaps),M.push(E.numHemiLights),M.push(E.numRectAreaLights),M.push(E.numDirLightShadows),M.push(E.numPointLightShadows),M.push(E.numSpotLightShadows),M.push(E.numSpotLightShadowsWithMaps),M.push(E.numLightProbes),M.push(E.shadowMapType),M.push(E.toneMapping),M.push(E.numClippingPlanes),M.push(E.numClipIntersection),M.push(E.depthPacking)}function p(M,E){a.disableAll(),E.supportsVertexTextures&&a.enable(0),E.instancing&&a.enable(1),E.instancingColor&&a.enable(2),E.instancingMorph&&a.enable(3),E.matcap&&a.enable(4),E.envMap&&a.enable(5),E.normalMapObjectSpace&&a.enable(6),E.normalMapTangentSpace&&a.enable(7),E.clearcoat&&a.enable(8),E.iridescence&&a.enable(9),E.alphaTest&&a.enable(10),E.vertexColors&&a.enable(11),E.vertexAlphas&&a.enable(12),E.vertexUv1s&&a.enable(13),E.vertexUv2s&&a.enable(14),E.vertexUv3s&&a.enable(15),E.vertexTangents&&a.enable(16),E.anisotropy&&a.enable(17),E.alphaHash&&a.enable(18),E.batching&&a.enable(19),E.dispersion&&a.enable(20),E.batchingColor&&a.enable(21),M.push(a.mask),a.disableAll(),E.fog&&a.enable(0),E.useFog&&a.enable(1),E.flatShading&&a.enable(2),E.logarithmicDepthBuffer&&a.enable(3),E.reverseDepthBuffer&&a.enable(4),E.skinning&&a.enable(5),E.morphTargets&&a.enable(6),E.morphNormals&&a.enable(7),E.morphColors&&a.enable(8),E.premultipliedAlpha&&a.enable(9),E.shadowMapEnabled&&a.enable(10),E.doubleSided&&a.enable(11),E.flipSided&&a.enable(12),E.useDepthPacking&&a.enable(13),E.dithering&&a.enable(14),E.transmission&&a.enable(15),E.sheen&&a.enable(16),E.opaque&&a.enable(17),E.pointsUvs&&a.enable(18),E.decodeVideoTexture&&a.enable(19),E.decodeVideoTextureEmissive&&a.enable(20),E.alphaToCoverage&&a.enable(21),M.push(a.mask)}function g(M){let E=v[M.type],F;if(E){let U=Fn[E];F=bp.clone(U.uniforms)}else F=M.uniforms;return F}function T(M,E){let F;for(let U=0,R=u.length;U<R;U++){let N=u[U];if(N.cacheKey===E){F=N,++F.usedTimes;break}}return F===void 0&&(F=new Dx(i,E,M,r),u.push(F)),F}function S(M){if(--M.usedTimes===0){let E=u.indexOf(M);u[E]=u[u.length-1],u.pop(),M.destroy()}}function b(M){l.remove(M)}function w(){l.dispose()}return{getParameters:y,getProgramCacheKey:c,getUniforms:g,acquireProgram:T,releaseProgram:S,releaseShaderCache:b,programs:u,dispose:w}}function Bx(){let i=new WeakMap;function t(o){return i.has(o)}function e(o){let a=i.get(o);return a===void 0&&(a={},i.set(o,a)),a}function n(o){i.delete(o)}function s(o,a,l){i.get(o)[a]=l}function r(){i=new WeakMap}return{has:t,get:e,remove:n,update:s,dispose:r}}function zx(i,t){return i.groupOrder!==t.groupOrder?i.groupOrder-t.groupOrder:i.renderOrder!==t.renderOrder?i.renderOrder-t.renderOrder:i.material.id!==t.material.id?i.material.id-t.material.id:i.z!==t.z?i.z-t.z:i.id-t.id}function bu(i,t){return i.groupOrder!==t.groupOrder?i.groupOrder-t.groupOrder:i.renderOrder!==t.renderOrder?i.renderOrder-t.renderOrder:i.z!==t.z?t.z-i.z:i.id-t.id}function Au(){let i=[],t=0,e=[],n=[],s=[];function r(){t=0,e.length=0,n.length=0,s.length=0}function o(m,x,f,v,d,y){let c=i[t];return c===void 0?(c={id:m.id,object:m,geometry:x,material:f,groupOrder:v,renderOrder:m.renderOrder,z:d,group:y},i[t]=c):(c.id=m.id,c.object=m,c.geometry=x,c.material=f,c.groupOrder=v,c.renderOrder=m.renderOrder,c.z=d,c.group=y),t++,c}function a(m,x,f,v,d,y){let c=o(m,x,f,v,d,y);f.transmission>0?n.push(c):f.transparent===!0?s.push(c):e.push(c)}function l(m,x,f,v,d,y){let c=o(m,x,f,v,d,y);f.transmission>0?n.unshift(c):f.transparent===!0?s.unshift(c):e.unshift(c)}function h(m,x){e.length>1&&e.sort(m||zx),n.length>1&&n.sort(x||bu),s.length>1&&s.sort(x||bu)}function u(){for(let m=t,x=i.length;m<x;m++){let f=i[m];if(f.id===null)break;f.id=null,f.object=null,f.geometry=null,f.material=null,f.group=null}}return{opaque:e,transmissive:n,transparent:s,init:r,push:a,unshift:l,finish:u,sort:h}}function Ox(){let i=new WeakMap;function t(n,s){let r=i.get(n),o;return r===void 0?(o=new Au,i.set(n,[o])):s>=r.length?(o=new Au,r.push(o)):o=r[s],o}function e(){i=new WeakMap}return{get:t,dispose:e}}function Hx(){let i={};return{get:function(t){if(i[t.id]!==void 0)return i[t.id];let e;switch(t.type){case"DirectionalLight":e={direction:new B,color:new Jt};break;case"SpotLight":e={position:new B,direction:new B,color:new Jt,distance:0,coneCos:0,penumbraCos:0,decay:0};break;case"PointLight":e={position:new B,color:new Jt,distance:0,decay:0};break;case"HemisphereLight":e={direction:new B,skyColor:new Jt,groundColor:new Jt};break;case"RectAreaLight":e={color:new Jt,position:new B,halfWidth:new B,halfHeight:new B};break}return i[t.id]=e,e}}}function kx(){let i={};return{get:function(t){if(i[t.id]!==void 0)return i[t.id];let e;switch(t.type){case"DirectionalLight":e={shadowIntensity:1,shadowBias:0,shadowNormalBias:0,shadowRadius:1,shadowMapSize:new lt};break;case"SpotLight":e={shadowIntensity:1,shadowBias:0,shadowNormalBias:0,shadowRadius:1,shadowMapSize:new lt};break;case"PointLight":e={shadowIntensity:1,shadowBias:0,shadowNormalBias:0,shadowRadius:1,shadowMapSize:new lt,shadowCameraNear:1,shadowCameraFar:1e3};break}return i[t.id]=e,e}}}var Gx=0;function Vx(i,t){return(t.castShadow?2:0)-(i.castShadow?2:0)+(t.map?1:0)-(i.map?1:0)}function Wx(i){let t=new Hx,e=kx(),n={version:0,hash:{directionalLength:-1,pointLength:-1,spotLength:-1,rectAreaLength:-1,hemiLength:-1,numDirectionalShadows:-1,numPointShadows:-1,numSpotShadows:-1,numSpotMaps:-1,numLightProbes:-1},ambient:[0,0,0],probe:[],directional:[],directionalShadow:[],directionalShadowMap:[],directionalShadowMatrix:[],spot:[],spotLightMap:[],spotShadow:[],spotShadowMap:[],spotLightMatrix:[],rectArea:[],rectAreaLTC1:null,rectAreaLTC2:null,point:[],pointShadow:[],pointShadowMap:[],pointShadowMatrix:[],hemi:[],numSpotLightShadowsWithMaps:0,numLightProbes:0};for(let h=0;h<9;h++)n.probe.push(new B);let s=new B,r=new ee,o=new ee;function a(h){let u=0,m=0,x=0;for(let M=0;M<9;M++)n.probe[M].set(0,0,0);let f=0,v=0,d=0,y=0,c=0,_=0,p=0,g=0,T=0,S=0,b=0;h.sort(Vx);for(let M=0,E=h.length;M<E;M++){let F=h[M],U=F.color,R=F.intensity,N=F.distance,V=F.shadow&&F.shadow.map?F.shadow.map.texture:null;if(F.isAmbientLight)u+=U.r*R,m+=U.g*R,x+=U.b*R;else if(F.isLightProbe){for(let z=0;z<9;z++)n.probe[z].addScaledVector(F.sh.coefficients[z],R);b++}else if(F.isDirectionalLight){let z=t.get(F);if(z.color.copy(F.color).multiplyScalar(F.intensity),F.castShadow){let O=F.shadow,D=e.get(F);D.shadowIntensity=O.intensity,D.shadowBias=O.bias,D.shadowNormalBias=O.normalBias,D.shadowRadius=O.radius,D.shadowMapSize=O.mapSize,n.directionalShadow[f]=D,n.directionalShadowMap[f]=V,n.directionalShadowMatrix[f]=F.shadow.matrix,_++}n.directional[f]=z,f++}else if(F.isSpotLight){let z=t.get(F);z.position.setFromMatrixPosition(F.matrixWorld),z.color.copy(U).multiplyScalar(R),z.distance=N,z.coneCos=Math.cos(F.angle),z.penumbraCos=Math.cos(F.angle*(1-F.penumbra)),z.decay=F.decay,n.spot[d]=z;let O=F.shadow;if(F.map&&(n.spotLightMap[T]=F.map,T++,O.updateMatrices(F),F.castShadow&&S++),n.spotLightMatrix[d]=O.matrix,F.castShadow){let D=e.get(F);D.shadowIntensity=O.intensity,D.shadowBias=O.bias,D.shadowNormalBias=O.normalBias,D.shadowRadius=O.radius,D.shadowMapSize=O.mapSize,n.spotShadow[d]=D,n.spotShadowMap[d]=V,g++}d++}else if(F.isRectAreaLight){let z=t.get(F);z.color.copy(U).multiplyScalar(R),z.halfWidth.set(F.width*.5,0,0),z.halfHeight.set(0,F.height*.5,0),n.rectArea[y]=z,y++}else if(F.isPointLight){let z=t.get(F);if(z.color.copy(F.color).multiplyScalar(F.intensity),z.distance=F.distance,z.decay=F.decay,F.castShadow){let O=F.shadow,D=e.get(F);D.shadowIntensity=O.intensity,D.shadowBias=O.bias,D.shadowNormalBias=O.normalBias,D.shadowRadius=O.radius,D.shadowMapSize=O.mapSize,D.shadowCameraNear=O.camera.near,D.shadowCameraFar=O.camera.far,n.pointShadow[v]=D,n.pointShadowMap[v]=V,n.pointShadowMatrix[v]=F.shadow.matrix,p++}n.point[v]=z,v++}else if(F.isHemisphereLight){let z=t.get(F);z.skyColor.copy(F.color).multiplyScalar(R),z.groundColor.copy(F.groundColor).multiplyScalar(R),n.hemi[c]=z,c++}}y>0&&(i.has("OES_texture_float_linear")===!0?(n.rectAreaLTC1=pt.LTC_FLOAT_1,n.rectAreaLTC2=pt.LTC_FLOAT_2):(n.rectAreaLTC1=pt.LTC_HALF_1,n.rectAreaLTC2=pt.LTC_HALF_2)),n.ambient[0]=u,n.ambient[1]=m,n.ambient[2]=x;let w=n.hash;(w.directionalLength!==f||w.pointLength!==v||w.spotLength!==d||w.rectAreaLength!==y||w.hemiLength!==c||w.numDirectionalShadows!==_||w.numPointShadows!==p||w.numSpotShadows!==g||w.numSpotMaps!==T||w.numLightProbes!==b)&&(n.directional.length=f,n.spot.length=d,n.rectArea.length=y,n.point.length=v,n.hemi.length=c,n.directionalShadow.length=_,n.directionalShadowMap.length=_,n.pointShadow.length=p,n.pointShadowMap.length=p,n.spotShadow.length=g,n.spotShadowMap.length=g,n.directionalShadowMatrix.length=_,n.pointShadowMatrix.length=p,n.spotLightMatrix.length=g+T-S,n.spotLightMap.length=T,n.numSpotLightShadowsWithMaps=S,n.numLightProbes=b,w.directionalLength=f,w.pointLength=v,w.spotLength=d,w.rectAreaLength=y,w.hemiLength=c,w.numDirectionalShadows=_,w.numPointShadows=p,w.numSpotShadows=g,w.numSpotMaps=T,w.numLightProbes=b,n.version=Gx++)}function l(h,u){let m=0,x=0,f=0,v=0,d=0,y=u.matrixWorldInverse;for(let c=0,_=h.length;c<_;c++){let p=h[c];if(p.isDirectionalLight){let g=n.directional[m];g.direction.setFromMatrixPosition(p.matrixWorld),s.setFromMatrixPosition(p.target.matrixWorld),g.direction.sub(s),g.direction.transformDirection(y),m++}else if(p.isSpotLight){let g=n.spot[f];g.position.setFromMatrixPosition(p.matrixWorld),g.position.applyMatrix4(y),g.direction.setFromMatrixPosition(p.matrixWorld),s.setFromMatrixPosition(p.target.matrixWorld),g.direction.sub(s),g.direction.transformDirection(y),f++}else if(p.isRectAreaLight){let g=n.rectArea[v];g.position.setFromMatrixPosition(p.matrixWorld),g.position.applyMatrix4(y),o.identity(),r.copy(p.matrixWorld),r.premultiply(y),o.extractRotation(r),g.halfWidth.set(p.width*.5,0,0),g.halfHeight.set(0,p.height*.5,0),g.halfWidth.applyMatrix4(o),g.halfHeight.applyMatrix4(o),v++}else if(p.isPointLight){let g=n.point[x];g.position.setFromMatrixPosition(p.matrixWorld),g.position.applyMatrix4(y),x++}else if(p.isHemisphereLight){let g=n.hemi[d];g.direction.setFromMatrixPosition(p.matrixWorld),g.direction.transformDirection(y),d++}}}return{setup:a,setupView:l,state:n}}function Ru(i){let t=new Wx(i),e=[],n=[];function s(u){h.camera=u,e.length=0,n.length=0}function r(u){e.push(u)}function o(u){n.push(u)}function a(){t.setup(e)}function l(u){t.setupView(e,u)}let h={lightsArray:e,shadowsArray:n,camera:null,lights:t,transmissionRenderTarget:{}};return{init:s,state:h,setupLights:a,setupLightsView:l,pushLight:r,pushShadow:o}}function Xx(i){let t=new WeakMap;function e(s,r=0){let o=t.get(s),a;return o===void 0?(a=new Ru(i),t.set(s,[a])):r>=o.length?(a=new Ru(i),o.push(a)):a=o[r],a}function n(){t=new WeakMap}return{get:e,dispose:n}}var sc=class extends Un{static get type(){return"MeshDepthMaterial"}constructor(t){super(),this.isMeshDepthMaterial=!0,this.depthPacking=jd,this.map=null,this.alphaMap=null,this.displacementMap=null,this.displacementScale=1,this.displacementBias=0,this.wireframe=!1,this.wireframeLinewidth=1,this.setValues(t)}copy(t){return super.copy(t),this.depthPacking=t.depthPacking,this.map=t.map,this.alphaMap=t.alphaMap,this.displacementMap=t.displacementMap,this.displacementScale=t.displacementScale,this.displacementBias=t.displacementBias,this.wireframe=t.wireframe,this.wireframeLinewidth=t.wireframeLinewidth,this}},rc=class extends Un{static get type(){return"MeshDistanceMaterial"}constructor(t){super(),this.isMeshDistanceMaterial=!0,this.map=null,this.alphaMap=null,this.displacementMap=null,this.displacementScale=1,this.displacementBias=0,this.setValues(t)}copy(t){return super.copy(t),this.map=t.map,this.alphaMap=t.alphaMap,this.displacementMap=t.displacementMap,this.displacementScale=t.displacementScale,this.displacementBias=t.displacementBias,this}},qx=`void main() {
	gl_Position = vec4( position, 1.0 );
}`,Yx=`uniform sampler2D shadow_pass;
uniform vec2 resolution;
uniform float radius;
#include <packing>
void main() {
	const float samples = float( VSM_SAMPLES );
	float mean = 0.0;
	float squared_mean = 0.0;
	float uvStride = samples <= 1.0 ? 0.0 : 2.0 / ( samples - 1.0 );
	float uvStart = samples <= 1.0 ? 0.0 : - 1.0;
	for ( float i = 0.0; i < samples; i ++ ) {
		float uvOffset = uvStart + i * uvStride;
		#ifdef HORIZONTAL_PASS
			vec2 distribution = unpackRGBATo2Half( texture2D( shadow_pass, ( gl_FragCoord.xy + vec2( uvOffset, 0.0 ) * radius ) / resolution ) );
			mean += distribution.x;
			squared_mean += distribution.y * distribution.y + distribution.x * distribution.x;
		#else
			float depth = unpackRGBAToDepth( texture2D( shadow_pass, ( gl_FragCoord.xy + vec2( 0.0, uvOffset ) * radius ) / resolution ) );
			mean += depth;
			squared_mean += depth * depth;
		#endif
	}
	mean = mean / samples;
	squared_mean = squared_mean / samples;
	float std_dev = sqrt( squared_mean - mean * mean );
	gl_FragColor = pack2HalfToRGBA( vec2( mean, std_dev ) );
}`;function Zx(i,t,e){let n=new pr,s=new lt,r=new lt,o=new Ue,a=new sc({depthPacking:Qd}),l=new rc,h={},u=e.maxTextureSize,m={[Dn]:en,[en]:Dn,[Me]:Me},x=new xn({defines:{VSM_SAMPLES:8},uniforms:{shadow_pass:{value:null},resolution:{value:new lt},radius:{value:4}},vertexShader:qx,fragmentShader:Yx}),f=x.clone();f.defines.HORIZONTAL_PASS=1;let v=new de;v.setAttribute("position",new we(new Float32Array([-1,-1,.5,3,-1,.5,-1,3,.5]),3));let d=new Z(v,x),y=this;this.enabled=!1,this.autoUpdate=!0,this.needsUpdate=!1,this.type=qu;let c=this.type;this.render=function(S,b,w){if(y.enabled===!1||y.autoUpdate===!1&&y.needsUpdate===!1||S.length===0)return;let M=i.getRenderTarget(),E=i.getActiveCubeFace(),F=i.getActiveMipmapLevel(),U=i.state;U.setBlending(xi),U.buffers.color.setClear(1,1,1,1),U.buffers.depth.setTest(!0),U.setScissorTest(!1);let R=c!==qn&&this.type===qn,N=c===qn&&this.type!==qn;for(let V=0,z=S.length;V<z;V++){let O=S[V],D=O.shadow;if(D===void 0){console.warn("THREE.WebGLShadowMap:",O,"has no shadow.");continue}if(D.autoUpdate===!1&&D.needsUpdate===!1)continue;s.copy(D.mapSize);let X=D.getFrameExtents();if(s.multiply(X),r.copy(D.mapSize),(s.x>u||s.y>u)&&(s.x>u&&(r.x=Math.floor(u/X.x),s.x=r.x*X.x,D.mapSize.x=r.x),s.y>u&&(r.y=Math.floor(u/X.y),s.y=r.y*X.y,D.mapSize.y=r.y)),D.map===null||R===!0||N===!0){let tt=this.type!==qn?{minFilter:hn,magFilter:hn}:{};D.map!==null&&D.map.dispose(),D.map=new jn(s.x,s.y,tt),D.map.texture.name=O.name+".shadowMap",D.camera.updateProjectionMatrix()}i.setRenderTarget(D.map),i.clear();let $=D.getViewportCount();for(let tt=0;tt<$;tt++){let ut=D.getViewport(tt);o.set(r.x*ut.x,r.y*ut.y,r.x*ut.z,r.y*ut.w),U.viewport(o),D.updateMatrices(O,tt),n=D.getFrustum(),g(b,w,D.camera,O,this.type)}D.isPointLightShadow!==!0&&this.type===qn&&_(D,w),D.needsUpdate=!1}c=this.type,y.needsUpdate=!1,i.setRenderTarget(M,E,F)};function _(S,b){let w=t.update(d);x.defines.VSM_SAMPLES!==S.blurSamples&&(x.defines.VSM_SAMPLES=S.blurSamples,f.defines.VSM_SAMPLES=S.blurSamples,x.needsUpdate=!0,f.needsUpdate=!0),S.mapPass===null&&(S.mapPass=new jn(s.x,s.y)),x.uniforms.shadow_pass.value=S.map.texture,x.uniforms.resolution.value=S.mapSize,x.uniforms.radius.value=S.radius,i.setRenderTarget(S.mapPass),i.clear(),i.renderBufferDirect(b,null,w,x,d,null),f.uniforms.shadow_pass.value=S.mapPass.texture,f.uniforms.resolution.value=S.mapSize,f.uniforms.radius.value=S.radius,i.setRenderTarget(S.map),i.clear(),i.renderBufferDirect(b,null,w,f,d,null)}function p(S,b,w,M){let E=null,F=w.isPointLight===!0?S.customDistanceMaterial:S.customDepthMaterial;if(F!==void 0)E=F;else if(E=w.isPointLight===!0?l:a,i.localClippingEnabled&&b.clipShadows===!0&&Array.isArray(b.clippingPlanes)&&b.clippingPlanes.length!==0||b.displacementMap&&b.displacementScale!==0||b.alphaMap&&b.alphaTest>0||b.map&&b.alphaTest>0){let U=E.uuid,R=b.uuid,N=h[U];N===void 0&&(N={},h[U]=N);let V=N[R];V===void 0&&(V=E.clone(),N[R]=V,b.addEventListener("dispose",T)),E=V}if(E.visible=b.visible,E.wireframe=b.wireframe,M===qn?E.side=b.shadowSide!==null?b.shadowSide:b.side:E.side=b.shadowSide!==null?b.shadowSide:m[b.side],E.alphaMap=b.alphaMap,E.alphaTest=b.alphaTest,E.map=b.map,E.clipShadows=b.clipShadows,E.clippingPlanes=b.clippingPlanes,E.clipIntersection=b.clipIntersection,E.displacementMap=b.displacementMap,E.displacementScale=b.displacementScale,E.displacementBias=b.displacementBias,E.wireframeLinewidth=b.wireframeLinewidth,E.linewidth=b.linewidth,w.isPointLight===!0&&E.isMeshDistanceMaterial===!0){let U=i.properties.get(E);U.light=w}return E}function g(S,b,w,M,E){if(S.visible===!1)return;if(S.layers.test(b.layers)&&(S.isMesh||S.isLine||S.isPoints)&&(S.castShadow||S.receiveShadow&&E===qn)&&(!S.frustumCulled||n.intersectsObject(S))){S.modelViewMatrix.multiplyMatrices(w.matrixWorldInverse,S.matrixWorld);let R=t.update(S),N=S.material;if(Array.isArray(N)){let V=R.groups;for(let z=0,O=V.length;z<O;z++){let D=V[z],X=N[D.materialIndex];if(X&&X.visible){let $=p(S,X,M,E);S.onBeforeShadow(i,S,b,w,R,$,D),i.renderBufferDirect(w,null,R,$,S,D),S.onAfterShadow(i,S,b,w,R,$,D)}}}else if(N.visible){let V=p(S,N,M,E);S.onBeforeShadow(i,S,b,w,R,V,null),i.renderBufferDirect(w,null,R,V,S,null),S.onAfterShadow(i,S,b,w,R,V,null)}}let U=S.children;for(let R=0,N=U.length;R<N;R++)g(U[R],b,w,M,E)}function T(S){S.target.removeEventListener("dispose",T);for(let w in h){let M=h[w],E=S.target.uuid;E in M&&(M[E].dispose(),delete M[E])}}}var $x={[ll]:cl,[hl]:dl,[ul]:pl,[As]:fl,[cl]:ll,[dl]:hl,[pl]:ul,[fl]:As};function Jx(i,t){function e(){let H=!1,yt=new Ue,K=null,et=new Ue(0,0,0,0);return{setMask:function(Et){K!==Et&&!H&&(i.colorMask(Et,Et,Et,Et),K=Et)},setLocked:function(Et){H=Et},setClear:function(Et,vt,Yt,De,Ye){Ye===!0&&(Et*=De,vt*=De,Yt*=De),yt.set(Et,vt,Yt,De),et.equals(yt)===!1&&(i.clearColor(Et,vt,Yt,De),et.copy(yt))},reset:function(){H=!1,K=null,et.set(-1,0,0,0)}}}function n(){let H=!1,yt=!1,K=null,et=null,Et=null;return{setReversed:function(vt){if(yt!==vt){let Yt=t.get("EXT_clip_control");yt?Yt.clipControlEXT(Yt.LOWER_LEFT_EXT,Yt.ZERO_TO_ONE_EXT):Yt.clipControlEXT(Yt.LOWER_LEFT_EXT,Yt.NEGATIVE_ONE_TO_ONE_EXT);let De=Et;Et=null,this.setClear(De)}yt=vt},getReversed:function(){return yt},setTest:function(vt){vt?rt(i.DEPTH_TEST):Nt(i.DEPTH_TEST)},setMask:function(vt){K!==vt&&!H&&(i.depthMask(vt),K=vt)},setFunc:function(vt){if(yt&&(vt=$x[vt]),et!==vt){switch(vt){case ll:i.depthFunc(i.NEVER);break;case cl:i.depthFunc(i.ALWAYS);break;case hl:i.depthFunc(i.LESS);break;case As:i.depthFunc(i.LEQUAL);break;case ul:i.depthFunc(i.EQUAL);break;case fl:i.depthFunc(i.GEQUAL);break;case dl:i.depthFunc(i.GREATER);break;case pl:i.depthFunc(i.NOTEQUAL);break;default:i.depthFunc(i.LEQUAL)}et=vt}},setLocked:function(vt){H=vt},setClear:function(vt){Et!==vt&&(yt&&(vt=1-vt),i.clearDepth(vt),Et=vt)},reset:function(){H=!1,K=null,et=null,Et=null,yt=!1}}}function s(){let H=!1,yt=null,K=null,et=null,Et=null,vt=null,Yt=null,De=null,Ye=null;return{setTest:function(xe){H||(xe?rt(i.STENCIL_TEST):Nt(i.STENCIL_TEST))},setMask:function(xe){yt!==xe&&!H&&(i.stencilMask(xe),yt=xe)},setFunc:function(xe,Mn,On){(K!==xe||et!==Mn||Et!==On)&&(i.stencilFunc(xe,Mn,On),K=xe,et=Mn,Et=On)},setOp:function(xe,Mn,On){(vt!==xe||Yt!==Mn||De!==On)&&(i.stencilOp(xe,Mn,On),vt=xe,Yt=Mn,De=On)},setLocked:function(xe){H=xe},setClear:function(xe){Ye!==xe&&(i.clearStencil(xe),Ye=xe)},reset:function(){H=!1,yt=null,K=null,et=null,Et=null,vt=null,Yt=null,De=null,Ye=null}}}let r=new e,o=new n,a=new s,l=new WeakMap,h=new WeakMap,u={},m={},x=new WeakMap,f=[],v=null,d=!1,y=null,c=null,_=null,p=null,g=null,T=null,S=null,b=new Jt(0,0,0),w=0,M=!1,E=null,F=null,U=null,R=null,N=null,V=i.getParameter(i.MAX_COMBINED_TEXTURE_IMAGE_UNITS),z=!1,O=0,D=i.getParameter(i.VERSION);D.indexOf("WebGL")!==-1?(O=parseFloat(/^WebGL (\d)/.exec(D)[1]),z=O>=1):D.indexOf("OpenGL ES")!==-1&&(O=parseFloat(/^OpenGL ES (\d)/.exec(D)[1]),z=O>=2);let X=null,$={},tt=i.getParameter(i.SCISSOR_BOX),ut=i.getParameter(i.VIEWPORT),Ct=new Ue().fromArray(tt),J=new Ue().fromArray(ut);function st(H,yt,K,et){let Et=new Uint8Array(4),vt=i.createTexture();i.bindTexture(H,vt),i.texParameteri(H,i.TEXTURE_MIN_FILTER,i.NEAREST),i.texParameteri(H,i.TEXTURE_MAG_FILTER,i.NEAREST);for(let Yt=0;Yt<K;Yt++)H===i.TEXTURE_3D||H===i.TEXTURE_2D_ARRAY?i.texImage3D(yt,0,i.RGBA,1,1,et,0,i.RGBA,i.UNSIGNED_BYTE,Et):i.texImage2D(yt+Yt,0,i.RGBA,1,1,0,i.RGBA,i.UNSIGNED_BYTE,Et);return vt}let _t={};_t[i.TEXTURE_2D]=st(i.TEXTURE_2D,i.TEXTURE_2D,1),_t[i.TEXTURE_CUBE_MAP]=st(i.TEXTURE_CUBE_MAP,i.TEXTURE_CUBE_MAP_POSITIVE_X,6),_t[i.TEXTURE_2D_ARRAY]=st(i.TEXTURE_2D_ARRAY,i.TEXTURE_2D_ARRAY,1,1),_t[i.TEXTURE_3D]=st(i.TEXTURE_3D,i.TEXTURE_3D,1,1),r.setClear(0,0,0,1),o.setClear(1),a.setClear(0),rt(i.DEPTH_TEST),o.setFunc(As),ot(!1),bt(Ph),rt(i.CULL_FACE),L(xi);function rt(H){u[H]!==!0&&(i.enable(H),u[H]=!0)}function Nt(H){u[H]!==!1&&(i.disable(H),u[H]=!1)}function Xt(H,yt){return m[H]!==yt?(i.bindFramebuffer(H,yt),m[H]=yt,H===i.DRAW_FRAMEBUFFER&&(m[i.FRAMEBUFFER]=yt),H===i.FRAMEBUFFER&&(m[i.DRAW_FRAMEBUFFER]=yt),!0):!1}function Ht(H,yt){let K=f,et=!1;if(H){K=x.get(yt),K===void 0&&(K=[],x.set(yt,K));let Et=H.textures;if(K.length!==Et.length||K[0]!==i.COLOR_ATTACHMENT0){for(let vt=0,Yt=Et.length;vt<Yt;vt++)K[vt]=i.COLOR_ATTACHMENT0+vt;K.length=Et.length,et=!0}}else K[0]!==i.BACK&&(K[0]=i.BACK,et=!0);et&&i.drawBuffers(K)}function ne(H){return v!==H?(i.useProgram(H),v=H,!0):!1}let nt={[Bi]:i.FUNC_ADD,[wd]:i.FUNC_SUBTRACT,[bd]:i.FUNC_REVERSE_SUBTRACT};nt[Ad]=i.MIN,nt[Rd]=i.MAX;let ct={[Cd]:i.ZERO,[Fd]:i.ONE,[Id]:i.SRC_COLOR,[ol]:i.SRC_ALPHA,[Bd]:i.SRC_ALPHA_SATURATE,[Ud]:i.DST_COLOR,[Ld]:i.DST_ALPHA,[Pd]:i.ONE_MINUS_SRC_COLOR,[al]:i.ONE_MINUS_SRC_ALPHA,[Nd]:i.ONE_MINUS_DST_COLOR,[Dd]:i.ONE_MINUS_DST_ALPHA,[zd]:i.CONSTANT_COLOR,[Od]:i.ONE_MINUS_CONSTANT_COLOR,[Hd]:i.CONSTANT_ALPHA,[kd]:i.ONE_MINUS_CONSTANT_ALPHA};function L(H,yt,K,et,Et,vt,Yt,De,Ye,xe){if(H===xi){d===!0&&(Nt(i.BLEND),d=!1);return}if(d===!1&&(rt(i.BLEND),d=!0),H!==Td){if(H!==y||xe!==M){if((c!==Bi||g!==Bi)&&(i.blendEquation(i.FUNC_ADD),c=Bi,g=Bi),xe)switch(H){case yi:i.blendFuncSeparate(i.ONE,i.ONE_MINUS_SRC_ALPHA,i.ONE,i.ONE_MINUS_SRC_ALPHA);break;case bs:i.blendFunc(i.ONE,i.ONE);break;case Lh:i.blendFuncSeparate(i.ZERO,i.ONE_MINUS_SRC_COLOR,i.ZERO,i.ONE);break;case Dh:i.blendFuncSeparate(i.ZERO,i.SRC_COLOR,i.ZERO,i.SRC_ALPHA);break;default:console.error("THREE.WebGLState: Invalid blending: ",H);break}else switch(H){case yi:i.blendFuncSeparate(i.SRC_ALPHA,i.ONE_MINUS_SRC_ALPHA,i.ONE,i.ONE_MINUS_SRC_ALPHA);break;case bs:i.blendFunc(i.SRC_ALPHA,i.ONE);break;case Lh:i.blendFuncSeparate(i.ZERO,i.ONE_MINUS_SRC_COLOR,i.ZERO,i.ONE);break;case Dh:i.blendFunc(i.ZERO,i.SRC_COLOR);break;default:console.error("THREE.WebGLState: Invalid blending: ",H);break}_=null,p=null,T=null,S=null,b.set(0,0,0),w=0,y=H,M=xe}return}Et=Et||yt,vt=vt||K,Yt=Yt||et,(yt!==c||Et!==g)&&(i.blendEquationSeparate(nt[yt],nt[Et]),c=yt,g=Et),(K!==_||et!==p||vt!==T||Yt!==S)&&(i.blendFuncSeparate(ct[K],ct[et],ct[vt],ct[Yt]),_=K,p=et,T=vt,S=Yt),(De.equals(b)===!1||Ye!==w)&&(i.blendColor(De.r,De.g,De.b,Ye),b.copy(De),w=Ye),y=H,M=!1}function Dt(H,yt){H.side===Me?Nt(i.CULL_FACE):rt(i.CULL_FACE);let K=H.side===en;yt&&(K=!K),ot(K),H.blending===yi&&H.transparent===!1?L(xi):L(H.blending,H.blendEquation,H.blendSrc,H.blendDst,H.blendEquationAlpha,H.blendSrcAlpha,H.blendDstAlpha,H.blendColor,H.blendAlpha,H.premultipliedAlpha),o.setFunc(H.depthFunc),o.setTest(H.depthTest),o.setMask(H.depthWrite),r.setMask(H.colorWrite);let et=H.stencilWrite;a.setTest(et),et&&(a.setMask(H.stencilWriteMask),a.setFunc(H.stencilFunc,H.stencilRef,H.stencilFuncMask),a.setOp(H.stencilFail,H.stencilZFail,H.stencilZPass)),zt(H.polygonOffset,H.polygonOffsetFactor,H.polygonOffsetUnits),H.alphaToCoverage===!0?rt(i.SAMPLE_ALPHA_TO_COVERAGE):Nt(i.SAMPLE_ALPHA_TO_COVERAGE)}function ot(H){E!==H&&(H?i.frontFace(i.CW):i.frontFace(i.CCW),E=H)}function bt(H){H!==Md?(rt(i.CULL_FACE),H!==F&&(H===Ph?i.cullFace(i.BACK):H===Ed?i.cullFace(i.FRONT):i.cullFace(i.FRONT_AND_BACK))):Nt(i.CULL_FACE),F=H}function ft(H){H!==U&&(z&&i.lineWidth(H),U=H)}function zt(H,yt,K){H?(rt(i.POLYGON_OFFSET_FILL),(R!==yt||N!==K)&&(i.polygonOffset(yt,K),R=yt,N=K)):Nt(i.POLYGON_OFFSET_FILL)}function Tt(H){H?rt(i.SCISSOR_TEST):Nt(i.SCISSOR_TEST)}function P(H){H===void 0&&(H=i.TEXTURE0+V-1),X!==H&&(i.activeTexture(H),X=H)}function A(H,yt,K){K===void 0&&(X===null?K=i.TEXTURE0+V-1:K=X);let et=$[K];et===void 0&&(et={type:void 0,texture:void 0},$[K]=et),(et.type!==H||et.texture!==yt)&&(X!==K&&(i.activeTexture(K),X=K),i.bindTexture(H,yt||_t[H]),et.type=H,et.texture=yt)}function W(){let H=$[X];H!==void 0&&H.type!==void 0&&(i.bindTexture(H.type,null),H.type=void 0,H.texture=void 0)}function j(){try{i.compressedTexImage2D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function it(){try{i.compressedTexImage3D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function Q(){try{i.texSubImage2D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function Ft(){try{i.texSubImage3D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function gt(){try{i.compressedTexSubImage2D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function wt(){try{i.compressedTexSubImage3D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function Qt(){try{i.texStorage2D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function at(){try{i.texStorage3D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function At(){try{i.texImage2D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function Ot(){try{i.texImage3D.apply(i,arguments)}catch(H){console.error("THREE.WebGLState:",H)}}function Vt(H){Ct.equals(H)===!1&&(i.scissor(H.x,H.y,H.z,H.w),Ct.copy(H))}function Rt(H){J.equals(H)===!1&&(i.viewport(H.x,H.y,H.z,H.w),J.copy(H))}function te(H,yt){let K=h.get(yt);K===void 0&&(K=new WeakMap,h.set(yt,K));let et=K.get(H);et===void 0&&(et=i.getUniformBlockIndex(yt,H.name),K.set(H,et))}function Kt(H,yt){let et=h.get(yt).get(H);l.get(yt)!==et&&(i.uniformBlockBinding(yt,et,H.__bindingPointIndex),l.set(yt,et))}function Se(){i.disable(i.BLEND),i.disable(i.CULL_FACE),i.disable(i.DEPTH_TEST),i.disable(i.POLYGON_OFFSET_FILL),i.disable(i.SCISSOR_TEST),i.disable(i.STENCIL_TEST),i.disable(i.SAMPLE_ALPHA_TO_COVERAGE),i.blendEquation(i.FUNC_ADD),i.blendFunc(i.ONE,i.ZERO),i.blendFuncSeparate(i.ONE,i.ZERO,i.ONE,i.ZERO),i.blendColor(0,0,0,0),i.colorMask(!0,!0,!0,!0),i.clearColor(0,0,0,0),i.depthMask(!0),i.depthFunc(i.LESS),o.setReversed(!1),i.clearDepth(1),i.stencilMask(4294967295),i.stencilFunc(i.ALWAYS,0,4294967295),i.stencilOp(i.KEEP,i.KEEP,i.KEEP),i.clearStencil(0),i.cullFace(i.BACK),i.frontFace(i.CCW),i.polygonOffset(0,0),i.activeTexture(i.TEXTURE0),i.bindFramebuffer(i.FRAMEBUFFER,null),i.bindFramebuffer(i.DRAW_FRAMEBUFFER,null),i.bindFramebuffer(i.READ_FRAMEBUFFER,null),i.useProgram(null),i.lineWidth(1),i.scissor(0,0,i.canvas.width,i.canvas.height),i.viewport(0,0,i.canvas.width,i.canvas.height),u={},X=null,$={},m={},x=new WeakMap,f=[],v=null,d=!1,y=null,c=null,_=null,p=null,g=null,T=null,S=null,b=new Jt(0,0,0),w=0,M=!1,E=null,F=null,U=null,R=null,N=null,Ct.set(0,0,i.canvas.width,i.canvas.height),J.set(0,0,i.canvas.width,i.canvas.height),r.reset(),o.reset(),a.reset()}return{buffers:{color:r,depth:o,stencil:a},enable:rt,disable:Nt,bindFramebuffer:Xt,drawBuffers:Ht,useProgram:ne,setBlending:L,setMaterial:Dt,setFlipSided:ot,setCullFace:bt,setLineWidth:ft,setPolygonOffset:zt,setScissorTest:Tt,activeTexture:P,bindTexture:A,unbindTexture:W,compressedTexImage2D:j,compressedTexImage3D:it,texImage2D:At,texImage3D:Ot,updateUBOMapping:te,uniformBlockBinding:Kt,texStorage2D:Qt,texStorage3D:at,texSubImage2D:Q,texSubImage3D:Ft,compressedTexSubImage2D:gt,compressedTexSubImage3D:wt,scissor:Vt,viewport:Rt,reset:Se}}function Cu(i,t,e,n){let s=Kx(n);switch(e){case Ku:return i*t;case Qu:return i*t;case tf:return i*t*2;case Oc:return i*t/s.components*s.byteLength;case Hc:return i*t/s.components*s.byteLength;case ef:return i*t*2/s.components*s.byteLength;case kc:return i*t*2/s.components*s.byteLength;case ju:return i*t*3/s.components*s.byteLength;case bn:return i*t*4/s.components*s.byteLength;case Gc:return i*t*4/s.components*s.byteLength;case _o:case vo:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*8;case Mo:case Eo:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*16;case _l:case Ml:return Math.max(i,16)*Math.max(t,8)/4;case yl:case vl:return Math.max(i,8)*Math.max(t,8)/2;case El:case Sl:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*8;case Tl:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*16;case wl:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*16;case bl:return Math.floor((i+4)/5)*Math.floor((t+3)/4)*16;case Al:return Math.floor((i+4)/5)*Math.floor((t+4)/5)*16;case Rl:return Math.floor((i+5)/6)*Math.floor((t+4)/5)*16;case Cl:return Math.floor((i+5)/6)*Math.floor((t+5)/6)*16;case Fl:return Math.floor((i+7)/8)*Math.floor((t+4)/5)*16;case Il:return Math.floor((i+7)/8)*Math.floor((t+5)/6)*16;case Pl:return Math.floor((i+7)/8)*Math.floor((t+7)/8)*16;case Ll:return Math.floor((i+9)/10)*Math.floor((t+4)/5)*16;case Dl:return Math.floor((i+9)/10)*Math.floor((t+5)/6)*16;case Ul:return Math.floor((i+9)/10)*Math.floor((t+7)/8)*16;case Nl:return Math.floor((i+9)/10)*Math.floor((t+9)/10)*16;case Bl:return Math.floor((i+11)/12)*Math.floor((t+9)/10)*16;case zl:return Math.floor((i+11)/12)*Math.floor((t+11)/12)*16;case So:case Ol:case Hl:return Math.ceil(i/4)*Math.ceil(t/4)*16;case nf:case kl:return Math.ceil(i/4)*Math.ceil(t/4)*8;case Gl:case Vl:return Math.ceil(i/4)*Math.ceil(t/4)*16}throw new Error(`Unable to determine texture byte length for ${e} format.`)}function Kx(i){switch(i){case Kn:case Zu:return{byteLength:1,components:1};case fr:case $u:case Rr:return{byteLength:2,components:1};case Bc:case zc:return{byteLength:2,components:4};case ki:case Nc:case Ln:return{byteLength:4,components:1};case Ju:return{byteLength:4,components:3}}throw new Error(`Unknown texture type ${i}.`)}function jx(i,t,e,n,s,r,o){let a=t.has("WEBGL_multisampled_render_to_texture")?t.get("WEBGL_multisampled_render_to_texture"):null,l=typeof navigator>"u"?!1:/OculusBrowser/g.test(navigator.userAgent),h=new lt,u=new WeakMap,m,x=new WeakMap,f=!1;try{f=typeof OffscreenCanvas<"u"&&new OffscreenCanvas(1,1).getContext("2d")!==null}catch{}function v(P,A){return f?new OffscreenCanvas(P,A):bo("canvas")}function d(P,A,W){let j=1,it=Tt(P);if((it.width>W||it.height>W)&&(j=W/Math.max(it.width,it.height)),j<1)if(typeof HTMLImageElement<"u"&&P instanceof HTMLImageElement||typeof HTMLCanvasElement<"u"&&P instanceof HTMLCanvasElement||typeof ImageBitmap<"u"&&P instanceof ImageBitmap||typeof VideoFrame<"u"&&P instanceof VideoFrame){let Q=Math.floor(j*it.width),Ft=Math.floor(j*it.height);m===void 0&&(m=v(Q,Ft));let gt=A?v(Q,Ft):m;return gt.width=Q,gt.height=Ft,gt.getContext("2d").drawImage(P,0,0,Q,Ft),console.warn("THREE.WebGLRenderer: Texture has been resized from ("+it.width+"x"+it.height+") to ("+Q+"x"+Ft+")."),gt}else return"data"in P&&console.warn("THREE.WebGLRenderer: Image in DataTexture is too big ("+it.width+"x"+it.height+")."),P;return P}function y(P){return P.generateMipmaps}function c(P){i.generateMipmap(P)}function _(P){return P.isWebGLCubeRenderTarget?i.TEXTURE_CUBE_MAP:P.isWebGL3DRenderTarget?i.TEXTURE_3D:P.isWebGLArrayRenderTarget||P.isCompressedArrayTexture?i.TEXTURE_2D_ARRAY:i.TEXTURE_2D}function p(P,A,W,j,it=!1){if(P!==null){if(i[P]!==void 0)return i[P];console.warn("THREE.WebGLRenderer: Attempt to use non-existing WebGL internal format '"+P+"'")}let Q=A;if(A===i.RED&&(W===i.FLOAT&&(Q=i.R32F),W===i.HALF_FLOAT&&(Q=i.R16F),W===i.UNSIGNED_BYTE&&(Q=i.R8)),A===i.RED_INTEGER&&(W===i.UNSIGNED_BYTE&&(Q=i.R8UI),W===i.UNSIGNED_SHORT&&(Q=i.R16UI),W===i.UNSIGNED_INT&&(Q=i.R32UI),W===i.BYTE&&(Q=i.R8I),W===i.SHORT&&(Q=i.R16I),W===i.INT&&(Q=i.R32I)),A===i.RG&&(W===i.FLOAT&&(Q=i.RG32F),W===i.HALF_FLOAT&&(Q=i.RG16F),W===i.UNSIGNED_BYTE&&(Q=i.RG8)),A===i.RG_INTEGER&&(W===i.UNSIGNED_BYTE&&(Q=i.RG8UI),W===i.UNSIGNED_SHORT&&(Q=i.RG16UI),W===i.UNSIGNED_INT&&(Q=i.RG32UI),W===i.BYTE&&(Q=i.RG8I),W===i.SHORT&&(Q=i.RG16I),W===i.INT&&(Q=i.RG32I)),A===i.RGB_INTEGER&&(W===i.UNSIGNED_BYTE&&(Q=i.RGB8UI),W===i.UNSIGNED_SHORT&&(Q=i.RGB16UI),W===i.UNSIGNED_INT&&(Q=i.RGB32UI),W===i.BYTE&&(Q=i.RGB8I),W===i.SHORT&&(Q=i.RGB16I),W===i.INT&&(Q=i.RGB32I)),A===i.RGBA_INTEGER&&(W===i.UNSIGNED_BYTE&&(Q=i.RGBA8UI),W===i.UNSIGNED_SHORT&&(Q=i.RGBA16UI),W===i.UNSIGNED_INT&&(Q=i.RGBA32UI),W===i.BYTE&&(Q=i.RGBA8I),W===i.SHORT&&(Q=i.RGBA16I),W===i.INT&&(Q=i.RGBA32I)),A===i.RGB&&W===i.UNSIGNED_INT_5_9_9_9_REV&&(Q=i.RGB9_E5),A===i.RGBA){let Ft=it?ta:ie.getTransfer(j);W===i.FLOAT&&(Q=i.RGBA32F),W===i.HALF_FLOAT&&(Q=i.RGBA16F),W===i.UNSIGNED_BYTE&&(Q=Ft===ye?i.SRGB8_ALPHA8:i.RGBA8),W===i.UNSIGNED_SHORT_4_4_4_4&&(Q=i.RGBA4),W===i.UNSIGNED_SHORT_5_5_5_1&&(Q=i.RGB5_A1)}return(Q===i.R16F||Q===i.R32F||Q===i.RG16F||Q===i.RG32F||Q===i.RGBA16F||Q===i.RGBA32F)&&t.get("EXT_color_buffer_float"),Q}function g(P,A){let W;return P?A===null||A===ki||A===Fs?W=i.DEPTH24_STENCIL8:A===Ln?W=i.DEPTH32F_STENCIL8:A===fr&&(W=i.DEPTH24_STENCIL8,console.warn("DepthTexture: 16 bit depth attachment is not supported with stencil. Using 24-bit attachment.")):A===null||A===ki||A===Fs?W=i.DEPTH_COMPONENT24:A===Ln?W=i.DEPTH_COMPONENT32F:A===fr&&(W=i.DEPTH_COMPONENT16),W}function T(P,A){return y(P)===!0||P.isFramebufferTexture&&P.minFilter!==hn&&P.minFilter!==Pn?Math.log2(Math.max(A.width,A.height))+1:P.mipmaps!==void 0&&P.mipmaps.length>0?P.mipmaps.length:P.isCompressedTexture&&Array.isArray(P.image)?A.mipmaps.length:1}function S(P){let A=P.target;A.removeEventListener("dispose",S),w(A),A.isVideoTexture&&u.delete(A)}function b(P){let A=P.target;A.removeEventListener("dispose",b),E(A)}function w(P){let A=n.get(P);if(A.__webglInit===void 0)return;let W=P.source,j=x.get(W);if(j){let it=j[A.__cacheKey];it.usedTimes--,it.usedTimes===0&&M(P),Object.keys(j).length===0&&x.delete(W)}n.remove(P)}function M(P){let A=n.get(P);i.deleteTexture(A.__webglTexture);let W=P.source,j=x.get(W);delete j[A.__cacheKey],o.memory.textures--}function E(P){let A=n.get(P);if(P.depthTexture&&(P.depthTexture.dispose(),n.remove(P.depthTexture)),P.isWebGLCubeRenderTarget)for(let j=0;j<6;j++){if(Array.isArray(A.__webglFramebuffer[j]))for(let it=0;it<A.__webglFramebuffer[j].length;it++)i.deleteFramebuffer(A.__webglFramebuffer[j][it]);else i.deleteFramebuffer(A.__webglFramebuffer[j]);A.__webglDepthbuffer&&i.deleteRenderbuffer(A.__webglDepthbuffer[j])}else{if(Array.isArray(A.__webglFramebuffer))for(let j=0;j<A.__webglFramebuffer.length;j++)i.deleteFramebuffer(A.__webglFramebuffer[j]);else i.deleteFramebuffer(A.__webglFramebuffer);if(A.__webglDepthbuffer&&i.deleteRenderbuffer(A.__webglDepthbuffer),A.__webglMultisampledFramebuffer&&i.deleteFramebuffer(A.__webglMultisampledFramebuffer),A.__webglColorRenderbuffer)for(let j=0;j<A.__webglColorRenderbuffer.length;j++)A.__webglColorRenderbuffer[j]&&i.deleteRenderbuffer(A.__webglColorRenderbuffer[j]);A.__webglDepthRenderbuffer&&i.deleteRenderbuffer(A.__webglDepthRenderbuffer)}let W=P.textures;for(let j=0,it=W.length;j<it;j++){let Q=n.get(W[j]);Q.__webglTexture&&(i.deleteTexture(Q.__webglTexture),o.memory.textures--),n.remove(W[j])}n.remove(P)}let F=0;function U(){F=0}function R(){let P=F;return P>=s.maxTextures&&console.warn("THREE.WebGLTextures: Trying to use "+P+" texture units while this GPU supports only "+s.maxTextures),F+=1,P}function N(P){let A=[];return A.push(P.wrapS),A.push(P.wrapT),A.push(P.wrapR||0),A.push(P.magFilter),A.push(P.minFilter),A.push(P.anisotropy),A.push(P.internalFormat),A.push(P.format),A.push(P.type),A.push(P.generateMipmaps),A.push(P.premultiplyAlpha),A.push(P.flipY),A.push(P.unpackAlignment),A.push(P.colorSpace),A.join()}function V(P,A){let W=n.get(P);if(P.isVideoTexture&&ft(P),P.isRenderTargetTexture===!1&&P.version>0&&W.__version!==P.version){let j=P.image;if(j===null)console.warn("THREE.WebGLRenderer: Texture marked for update but no image data found.");else if(j.complete===!1)console.warn("THREE.WebGLRenderer: Texture marked for update but image is incomplete");else{J(W,P,A);return}}e.bindTexture(i.TEXTURE_2D,W.__webglTexture,i.TEXTURE0+A)}function z(P,A){let W=n.get(P);if(P.version>0&&W.__version!==P.version){J(W,P,A);return}e.bindTexture(i.TEXTURE_2D_ARRAY,W.__webglTexture,i.TEXTURE0+A)}function O(P,A){let W=n.get(P);if(P.version>0&&W.__version!==P.version){J(W,P,A);return}e.bindTexture(i.TEXTURE_3D,W.__webglTexture,i.TEXTURE0+A)}function D(P,A){let W=n.get(P);if(P.version>0&&W.__version!==P.version){st(W,P,A);return}e.bindTexture(i.TEXTURE_CUBE_MAP,W.__webglTexture,i.TEXTURE0+A)}let X={[un]:i.REPEAT,[Oi]:i.CLAMP_TO_EDGE,[xl]:i.MIRRORED_REPEAT},$={[hn]:i.NEAREST,[Kd]:i.NEAREST_MIPMAP_NEAREST,[Or]:i.NEAREST_MIPMAP_LINEAR,[Pn]:i.LINEAR,[Ta]:i.LINEAR_MIPMAP_NEAREST,[Hi]:i.LINEAR_MIPMAP_LINEAR},tt={[ep]:i.NEVER,[ap]:i.ALWAYS,[np]:i.LESS,[rf]:i.LEQUAL,[ip]:i.EQUAL,[op]:i.GEQUAL,[sp]:i.GREATER,[rp]:i.NOTEQUAL};function ut(P,A){if(A.type===Ln&&t.has("OES_texture_float_linear")===!1&&(A.magFilter===Pn||A.magFilter===Ta||A.magFilter===Or||A.magFilter===Hi||A.minFilter===Pn||A.minFilter===Ta||A.minFilter===Or||A.minFilter===Hi)&&console.warn("THREE.WebGLRenderer: Unable to use linear filtering with floating point textures. OES_texture_float_linear not supported on this device."),i.texParameteri(P,i.TEXTURE_WRAP_S,X[A.wrapS]),i.texParameteri(P,i.TEXTURE_WRAP_T,X[A.wrapT]),(P===i.TEXTURE_3D||P===i.TEXTURE_2D_ARRAY)&&i.texParameteri(P,i.TEXTURE_WRAP_R,X[A.wrapR]),i.texParameteri(P,i.TEXTURE_MAG_FILTER,$[A.magFilter]),i.texParameteri(P,i.TEXTURE_MIN_FILTER,$[A.minFilter]),A.compareFunction&&(i.texParameteri(P,i.TEXTURE_COMPARE_MODE,i.COMPARE_REF_TO_TEXTURE),i.texParameteri(P,i.TEXTURE_COMPARE_FUNC,tt[A.compareFunction])),t.has("EXT_texture_filter_anisotropic")===!0){if(A.magFilter===hn||A.minFilter!==Or&&A.minFilter!==Hi||A.type===Ln&&t.has("OES_texture_float_linear")===!1)return;if(A.anisotropy>1||n.get(A).__currentAnisotropy){let W=t.get("EXT_texture_filter_anisotropic");i.texParameterf(P,W.TEXTURE_MAX_ANISOTROPY_EXT,Math.min(A.anisotropy,s.getMaxAnisotropy())),n.get(A).__currentAnisotropy=A.anisotropy}}}function Ct(P,A){let W=!1;P.__webglInit===void 0&&(P.__webglInit=!0,A.addEventListener("dispose",S));let j=A.source,it=x.get(j);it===void 0&&(it={},x.set(j,it));let Q=N(A);if(Q!==P.__cacheKey){it[Q]===void 0&&(it[Q]={texture:i.createTexture(),usedTimes:0},o.memory.textures++,W=!0),it[Q].usedTimes++;let Ft=it[P.__cacheKey];Ft!==void 0&&(it[P.__cacheKey].usedTimes--,Ft.usedTimes===0&&M(A)),P.__cacheKey=Q,P.__webglTexture=it[Q].texture}return W}function J(P,A,W){let j=i.TEXTURE_2D;(A.isDataArrayTexture||A.isCompressedArrayTexture)&&(j=i.TEXTURE_2D_ARRAY),A.isData3DTexture&&(j=i.TEXTURE_3D);let it=Ct(P,A),Q=A.source;e.bindTexture(j,P.__webglTexture,i.TEXTURE0+W);let Ft=n.get(Q);if(Q.version!==Ft.__version||it===!0){e.activeTexture(i.TEXTURE0+W);let gt=ie.getPrimaries(ie.workingColorSpace),wt=A.colorSpace===mi?null:ie.getPrimaries(A.colorSpace),Qt=A.colorSpace===mi||gt===wt?i.NONE:i.BROWSER_DEFAULT_WEBGL;i.pixelStorei(i.UNPACK_FLIP_Y_WEBGL,A.flipY),i.pixelStorei(i.UNPACK_PREMULTIPLY_ALPHA_WEBGL,A.premultiplyAlpha),i.pixelStorei(i.UNPACK_ALIGNMENT,A.unpackAlignment),i.pixelStorei(i.UNPACK_COLORSPACE_CONVERSION_WEBGL,Qt);let at=d(A.image,!1,s.maxTextureSize);at=zt(A,at);let At=r.convert(A.format,A.colorSpace),Ot=r.convert(A.type),Vt=p(A.internalFormat,At,Ot,A.colorSpace,A.isVideoTexture);ut(j,A);let Rt,te=A.mipmaps,Kt=A.isVideoTexture!==!0,Se=Ft.__version===void 0||it===!0,H=Q.dataReady,yt=T(A,at);if(A.isDepthTexture)Vt=g(A.format===Is,A.type),Se&&(Kt?e.texStorage2D(i.TEXTURE_2D,1,Vt,at.width,at.height):e.texImage2D(i.TEXTURE_2D,0,Vt,at.width,at.height,0,At,Ot,null));else if(A.isDataTexture)if(te.length>0){Kt&&Se&&e.texStorage2D(i.TEXTURE_2D,yt,Vt,te[0].width,te[0].height);for(let K=0,et=te.length;K<et;K++)Rt=te[K],Kt?H&&e.texSubImage2D(i.TEXTURE_2D,K,0,0,Rt.width,Rt.height,At,Ot,Rt.data):e.texImage2D(i.TEXTURE_2D,K,Vt,Rt.width,Rt.height,0,At,Ot,Rt.data);A.generateMipmaps=!1}else Kt?(Se&&e.texStorage2D(i.TEXTURE_2D,yt,Vt,at.width,at.height),H&&e.texSubImage2D(i.TEXTURE_2D,0,0,0,at.width,at.height,At,Ot,at.data)):e.texImage2D(i.TEXTURE_2D,0,Vt,at.width,at.height,0,At,Ot,at.data);else if(A.isCompressedTexture)if(A.isCompressedArrayTexture){Kt&&Se&&e.texStorage3D(i.TEXTURE_2D_ARRAY,yt,Vt,te[0].width,te[0].height,at.depth);for(let K=0,et=te.length;K<et;K++)if(Rt=te[K],A.format!==bn)if(At!==null)if(Kt){if(H)if(A.layerUpdates.size>0){let Et=Cu(Rt.width,Rt.height,A.format,A.type);for(let vt of A.layerUpdates){let Yt=Rt.data.subarray(vt*Et/Rt.data.BYTES_PER_ELEMENT,(vt+1)*Et/Rt.data.BYTES_PER_ELEMENT);e.compressedTexSubImage3D(i.TEXTURE_2D_ARRAY,K,0,0,vt,Rt.width,Rt.height,1,At,Yt)}A.clearLayerUpdates()}else e.compressedTexSubImage3D(i.TEXTURE_2D_ARRAY,K,0,0,0,Rt.width,Rt.height,at.depth,At,Rt.data)}else e.compressedTexImage3D(i.TEXTURE_2D_ARRAY,K,Vt,Rt.width,Rt.height,at.depth,0,Rt.data,0,0);else console.warn("THREE.WebGLRenderer: Attempt to load unsupported compressed texture format in .uploadTexture()");else Kt?H&&e.texSubImage3D(i.TEXTURE_2D_ARRAY,K,0,0,0,Rt.width,Rt.height,at.depth,At,Ot,Rt.data):e.texImage3D(i.TEXTURE_2D_ARRAY,K,Vt,Rt.width,Rt.height,at.depth,0,At,Ot,Rt.data)}else{Kt&&Se&&e.texStorage2D(i.TEXTURE_2D,yt,Vt,te[0].width,te[0].height);for(let K=0,et=te.length;K<et;K++)Rt=te[K],A.format!==bn?At!==null?Kt?H&&e.compressedTexSubImage2D(i.TEXTURE_2D,K,0,0,Rt.width,Rt.height,At,Rt.data):e.compressedTexImage2D(i.TEXTURE_2D,K,Vt,Rt.width,Rt.height,0,Rt.data):console.warn("THREE.WebGLRenderer: Attempt to load unsupported compressed texture format in .uploadTexture()"):Kt?H&&e.texSubImage2D(i.TEXTURE_2D,K,0,0,Rt.width,Rt.height,At,Ot,Rt.data):e.texImage2D(i.TEXTURE_2D,K,Vt,Rt.width,Rt.height,0,At,Ot,Rt.data)}else if(A.isDataArrayTexture)if(Kt){if(Se&&e.texStorage3D(i.TEXTURE_2D_ARRAY,yt,Vt,at.width,at.height,at.depth),H)if(A.layerUpdates.size>0){let K=Cu(at.width,at.height,A.format,A.type);for(let et of A.layerUpdates){let Et=at.data.subarray(et*K/at.data.BYTES_PER_ELEMENT,(et+1)*K/at.data.BYTES_PER_ELEMENT);e.texSubImage3D(i.TEXTURE_2D_ARRAY,0,0,0,et,at.width,at.height,1,At,Ot,Et)}A.clearLayerUpdates()}else e.texSubImage3D(i.TEXTURE_2D_ARRAY,0,0,0,0,at.width,at.height,at.depth,At,Ot,at.data)}else e.texImage3D(i.TEXTURE_2D_ARRAY,0,Vt,at.width,at.height,at.depth,0,At,Ot,at.data);else if(A.isData3DTexture)Kt?(Se&&e.texStorage3D(i.TEXTURE_3D,yt,Vt,at.width,at.height,at.depth),H&&e.texSubImage3D(i.TEXTURE_3D,0,0,0,0,at.width,at.height,at.depth,At,Ot,at.data)):e.texImage3D(i.TEXTURE_3D,0,Vt,at.width,at.height,at.depth,0,At,Ot,at.data);else if(A.isFramebufferTexture){if(Se)if(Kt)e.texStorage2D(i.TEXTURE_2D,yt,Vt,at.width,at.height);else{let K=at.width,et=at.height;for(let Et=0;Et<yt;Et++)e.texImage2D(i.TEXTURE_2D,Et,Vt,K,et,0,At,Ot,null),K>>=1,et>>=1}}else if(te.length>0){if(Kt&&Se){let K=Tt(te[0]);e.texStorage2D(i.TEXTURE_2D,yt,Vt,K.width,K.height)}for(let K=0,et=te.length;K<et;K++)Rt=te[K],Kt?H&&e.texSubImage2D(i.TEXTURE_2D,K,0,0,At,Ot,Rt):e.texImage2D(i.TEXTURE_2D,K,Vt,At,Ot,Rt);A.generateMipmaps=!1}else if(Kt){if(Se){let K=Tt(at);e.texStorage2D(i.TEXTURE_2D,yt,Vt,K.width,K.height)}H&&e.texSubImage2D(i.TEXTURE_2D,0,0,0,At,Ot,at)}else e.texImage2D(i.TEXTURE_2D,0,Vt,At,Ot,at);y(A)&&c(j),Ft.__version=Q.version,A.onUpdate&&A.onUpdate(A)}P.__version=A.version}function st(P,A,W){if(A.image.length!==6)return;let j=Ct(P,A),it=A.source;e.bindTexture(i.TEXTURE_CUBE_MAP,P.__webglTexture,i.TEXTURE0+W);let Q=n.get(it);if(it.version!==Q.__version||j===!0){e.activeTexture(i.TEXTURE0+W);let Ft=ie.getPrimaries(ie.workingColorSpace),gt=A.colorSpace===mi?null:ie.getPrimaries(A.colorSpace),wt=A.colorSpace===mi||Ft===gt?i.NONE:i.BROWSER_DEFAULT_WEBGL;i.pixelStorei(i.UNPACK_FLIP_Y_WEBGL,A.flipY),i.pixelStorei(i.UNPACK_PREMULTIPLY_ALPHA_WEBGL,A.premultiplyAlpha),i.pixelStorei(i.UNPACK_ALIGNMENT,A.unpackAlignment),i.pixelStorei(i.UNPACK_COLORSPACE_CONVERSION_WEBGL,wt);let Qt=A.isCompressedTexture||A.image[0].isCompressedTexture,at=A.image[0]&&A.image[0].isDataTexture,At=[];for(let et=0;et<6;et++)!Qt&&!at?At[et]=d(A.image[et],!0,s.maxCubemapSize):At[et]=at?A.image[et].image:A.image[et],At[et]=zt(A,At[et]);let Ot=At[0],Vt=r.convert(A.format,A.colorSpace),Rt=r.convert(A.type),te=p(A.internalFormat,Vt,Rt,A.colorSpace),Kt=A.isVideoTexture!==!0,Se=Q.__version===void 0||j===!0,H=it.dataReady,yt=T(A,Ot);ut(i.TEXTURE_CUBE_MAP,A);let K;if(Qt){Kt&&Se&&e.texStorage2D(i.TEXTURE_CUBE_MAP,yt,te,Ot.width,Ot.height);for(let et=0;et<6;et++){K=At[et].mipmaps;for(let Et=0;Et<K.length;Et++){let vt=K[Et];A.format!==bn?Vt!==null?Kt?H&&e.compressedTexSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,Et,0,0,vt.width,vt.height,Vt,vt.data):e.compressedTexImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,Et,te,vt.width,vt.height,0,vt.data):console.warn("THREE.WebGLRenderer: Attempt to load unsupported compressed texture format in .setTextureCube()"):Kt?H&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,Et,0,0,vt.width,vt.height,Vt,Rt,vt.data):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,Et,te,vt.width,vt.height,0,Vt,Rt,vt.data)}}}else{if(K=A.mipmaps,Kt&&Se){K.length>0&&yt++;let et=Tt(At[0]);e.texStorage2D(i.TEXTURE_CUBE_MAP,yt,te,et.width,et.height)}for(let et=0;et<6;et++)if(at){Kt?H&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,0,0,0,At[et].width,At[et].height,Vt,Rt,At[et].data):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,0,te,At[et].width,At[et].height,0,Vt,Rt,At[et].data);for(let Et=0;Et<K.length;Et++){let Yt=K[Et].image[et].image;Kt?H&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,Et+1,0,0,Yt.width,Yt.height,Vt,Rt,Yt.data):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,Et+1,te,Yt.width,Yt.height,0,Vt,Rt,Yt.data)}}else{Kt?H&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,0,0,0,Vt,Rt,At[et]):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,0,te,Vt,Rt,At[et]);for(let Et=0;Et<K.length;Et++){let vt=K[Et];Kt?H&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,Et+1,0,0,Vt,Rt,vt.image[et]):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+et,Et+1,te,Vt,Rt,vt.image[et])}}}y(A)&&c(i.TEXTURE_CUBE_MAP),Q.__version=it.version,A.onUpdate&&A.onUpdate(A)}P.__version=A.version}function _t(P,A,W,j,it,Q){let Ft=r.convert(W.format,W.colorSpace),gt=r.convert(W.type),wt=p(W.internalFormat,Ft,gt,W.colorSpace),Qt=n.get(A),at=n.get(W);if(at.__renderTarget=A,!Qt.__hasExternalTextures){let At=Math.max(1,A.width>>Q),Ot=Math.max(1,A.height>>Q);it===i.TEXTURE_3D||it===i.TEXTURE_2D_ARRAY?e.texImage3D(it,Q,wt,At,Ot,A.depth,0,Ft,gt,null):e.texImage2D(it,Q,wt,At,Ot,0,Ft,gt,null)}e.bindFramebuffer(i.FRAMEBUFFER,P),bt(A)?a.framebufferTexture2DMultisampleEXT(i.FRAMEBUFFER,j,it,at.__webglTexture,0,ot(A)):(it===i.TEXTURE_2D||it>=i.TEXTURE_CUBE_MAP_POSITIVE_X&&it<=i.TEXTURE_CUBE_MAP_NEGATIVE_Z)&&i.framebufferTexture2D(i.FRAMEBUFFER,j,it,at.__webglTexture,Q),e.bindFramebuffer(i.FRAMEBUFFER,null)}function rt(P,A,W){if(i.bindRenderbuffer(i.RENDERBUFFER,P),A.depthBuffer){let j=A.depthTexture,it=j&&j.isDepthTexture?j.type:null,Q=g(A.stencilBuffer,it),Ft=A.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT,gt=ot(A);bt(A)?a.renderbufferStorageMultisampleEXT(i.RENDERBUFFER,gt,Q,A.width,A.height):W?i.renderbufferStorageMultisample(i.RENDERBUFFER,gt,Q,A.width,A.height):i.renderbufferStorage(i.RENDERBUFFER,Q,A.width,A.height),i.framebufferRenderbuffer(i.FRAMEBUFFER,Ft,i.RENDERBUFFER,P)}else{let j=A.textures;for(let it=0;it<j.length;it++){let Q=j[it],Ft=r.convert(Q.format,Q.colorSpace),gt=r.convert(Q.type),wt=p(Q.internalFormat,Ft,gt,Q.colorSpace),Qt=ot(A);W&&bt(A)===!1?i.renderbufferStorageMultisample(i.RENDERBUFFER,Qt,wt,A.width,A.height):bt(A)?a.renderbufferStorageMultisampleEXT(i.RENDERBUFFER,Qt,wt,A.width,A.height):i.renderbufferStorage(i.RENDERBUFFER,wt,A.width,A.height)}}i.bindRenderbuffer(i.RENDERBUFFER,null)}function Nt(P,A){if(A&&A.isWebGLCubeRenderTarget)throw new Error("Depth Texture with cube render targets is not supported");if(e.bindFramebuffer(i.FRAMEBUFFER,P),!(A.depthTexture&&A.depthTexture.isDepthTexture))throw new Error("renderTarget.depthTexture must be an instance of THREE.DepthTexture");let j=n.get(A.depthTexture);j.__renderTarget=A,(!j.__webglTexture||A.depthTexture.image.width!==A.width||A.depthTexture.image.height!==A.height)&&(A.depthTexture.image.width=A.width,A.depthTexture.image.height=A.height,A.depthTexture.needsUpdate=!0),V(A.depthTexture,0);let it=j.__webglTexture,Q=ot(A);if(A.depthTexture.format===Ss)bt(A)?a.framebufferTexture2DMultisampleEXT(i.FRAMEBUFFER,i.DEPTH_ATTACHMENT,i.TEXTURE_2D,it,0,Q):i.framebufferTexture2D(i.FRAMEBUFFER,i.DEPTH_ATTACHMENT,i.TEXTURE_2D,it,0);else if(A.depthTexture.format===Is)bt(A)?a.framebufferTexture2DMultisampleEXT(i.FRAMEBUFFER,i.DEPTH_STENCIL_ATTACHMENT,i.TEXTURE_2D,it,0,Q):i.framebufferTexture2D(i.FRAMEBUFFER,i.DEPTH_STENCIL_ATTACHMENT,i.TEXTURE_2D,it,0);else throw new Error("Unknown depthTexture format")}function Xt(P){let A=n.get(P),W=P.isWebGLCubeRenderTarget===!0;if(A.__boundDepthTexture!==P.depthTexture){let j=P.depthTexture;if(A.__depthDisposeCallback&&A.__depthDisposeCallback(),j){let it=()=>{delete A.__boundDepthTexture,delete A.__depthDisposeCallback,j.removeEventListener("dispose",it)};j.addEventListener("dispose",it),A.__depthDisposeCallback=it}A.__boundDepthTexture=j}if(P.depthTexture&&!A.__autoAllocateDepthBuffer){if(W)throw new Error("target.depthTexture not supported in Cube render targets");Nt(A.__webglFramebuffer,P)}else if(W){A.__webglDepthbuffer=[];for(let j=0;j<6;j++)if(e.bindFramebuffer(i.FRAMEBUFFER,A.__webglFramebuffer[j]),A.__webglDepthbuffer[j]===void 0)A.__webglDepthbuffer[j]=i.createRenderbuffer(),rt(A.__webglDepthbuffer[j],P,!1);else{let it=P.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT,Q=A.__webglDepthbuffer[j];i.bindRenderbuffer(i.RENDERBUFFER,Q),i.framebufferRenderbuffer(i.FRAMEBUFFER,it,i.RENDERBUFFER,Q)}}else if(e.bindFramebuffer(i.FRAMEBUFFER,A.__webglFramebuffer),A.__webglDepthbuffer===void 0)A.__webglDepthbuffer=i.createRenderbuffer(),rt(A.__webglDepthbuffer,P,!1);else{let j=P.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT,it=A.__webglDepthbuffer;i.bindRenderbuffer(i.RENDERBUFFER,it),i.framebufferRenderbuffer(i.FRAMEBUFFER,j,i.RENDERBUFFER,it)}e.bindFramebuffer(i.FRAMEBUFFER,null)}function Ht(P,A,W){let j=n.get(P);A!==void 0&&_t(j.__webglFramebuffer,P,P.texture,i.COLOR_ATTACHMENT0,i.TEXTURE_2D,0),W!==void 0&&Xt(P)}function ne(P){let A=P.texture,W=n.get(P),j=n.get(A);P.addEventListener("dispose",b);let it=P.textures,Q=P.isWebGLCubeRenderTarget===!0,Ft=it.length>1;if(Ft||(j.__webglTexture===void 0&&(j.__webglTexture=i.createTexture()),j.__version=A.version,o.memory.textures++),Q){W.__webglFramebuffer=[];for(let gt=0;gt<6;gt++)if(A.mipmaps&&A.mipmaps.length>0){W.__webglFramebuffer[gt]=[];for(let wt=0;wt<A.mipmaps.length;wt++)W.__webglFramebuffer[gt][wt]=i.createFramebuffer()}else W.__webglFramebuffer[gt]=i.createFramebuffer()}else{if(A.mipmaps&&A.mipmaps.length>0){W.__webglFramebuffer=[];for(let gt=0;gt<A.mipmaps.length;gt++)W.__webglFramebuffer[gt]=i.createFramebuffer()}else W.__webglFramebuffer=i.createFramebuffer();if(Ft)for(let gt=0,wt=it.length;gt<wt;gt++){let Qt=n.get(it[gt]);Qt.__webglTexture===void 0&&(Qt.__webglTexture=i.createTexture(),o.memory.textures++)}if(P.samples>0&&bt(P)===!1){W.__webglMultisampledFramebuffer=i.createFramebuffer(),W.__webglColorRenderbuffer=[],e.bindFramebuffer(i.FRAMEBUFFER,W.__webglMultisampledFramebuffer);for(let gt=0;gt<it.length;gt++){let wt=it[gt];W.__webglColorRenderbuffer[gt]=i.createRenderbuffer(),i.bindRenderbuffer(i.RENDERBUFFER,W.__webglColorRenderbuffer[gt]);let Qt=r.convert(wt.format,wt.colorSpace),at=r.convert(wt.type),At=p(wt.internalFormat,Qt,at,wt.colorSpace,P.isXRRenderTarget===!0),Ot=ot(P);i.renderbufferStorageMultisample(i.RENDERBUFFER,Ot,At,P.width,P.height),i.framebufferRenderbuffer(i.FRAMEBUFFER,i.COLOR_ATTACHMENT0+gt,i.RENDERBUFFER,W.__webglColorRenderbuffer[gt])}i.bindRenderbuffer(i.RENDERBUFFER,null),P.depthBuffer&&(W.__webglDepthRenderbuffer=i.createRenderbuffer(),rt(W.__webglDepthRenderbuffer,P,!0)),e.bindFramebuffer(i.FRAMEBUFFER,null)}}if(Q){e.bindTexture(i.TEXTURE_CUBE_MAP,j.__webglTexture),ut(i.TEXTURE_CUBE_MAP,A);for(let gt=0;gt<6;gt++)if(A.mipmaps&&A.mipmaps.length>0)for(let wt=0;wt<A.mipmaps.length;wt++)_t(W.__webglFramebuffer[gt][wt],P,A,i.COLOR_ATTACHMENT0,i.TEXTURE_CUBE_MAP_POSITIVE_X+gt,wt);else _t(W.__webglFramebuffer[gt],P,A,i.COLOR_ATTACHMENT0,i.TEXTURE_CUBE_MAP_POSITIVE_X+gt,0);y(A)&&c(i.TEXTURE_CUBE_MAP),e.unbindTexture()}else if(Ft){for(let gt=0,wt=it.length;gt<wt;gt++){let Qt=it[gt],at=n.get(Qt);e.bindTexture(i.TEXTURE_2D,at.__webglTexture),ut(i.TEXTURE_2D,Qt),_t(W.__webglFramebuffer,P,Qt,i.COLOR_ATTACHMENT0+gt,i.TEXTURE_2D,0),y(Qt)&&c(i.TEXTURE_2D)}e.unbindTexture()}else{let gt=i.TEXTURE_2D;if((P.isWebGL3DRenderTarget||P.isWebGLArrayRenderTarget)&&(gt=P.isWebGL3DRenderTarget?i.TEXTURE_3D:i.TEXTURE_2D_ARRAY),e.bindTexture(gt,j.__webglTexture),ut(gt,A),A.mipmaps&&A.mipmaps.length>0)for(let wt=0;wt<A.mipmaps.length;wt++)_t(W.__webglFramebuffer[wt],P,A,i.COLOR_ATTACHMENT0,gt,wt);else _t(W.__webglFramebuffer,P,A,i.COLOR_ATTACHMENT0,gt,0);y(A)&&c(gt),e.unbindTexture()}P.depthBuffer&&Xt(P)}function nt(P){let A=P.textures;for(let W=0,j=A.length;W<j;W++){let it=A[W];if(y(it)){let Q=_(P),Ft=n.get(it).__webglTexture;e.bindTexture(Q,Ft),c(Q),e.unbindTexture()}}}let ct=[],L=[];function Dt(P){if(P.samples>0){if(bt(P)===!1){let A=P.textures,W=P.width,j=P.height,it=i.COLOR_BUFFER_BIT,Q=P.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT,Ft=n.get(P),gt=A.length>1;if(gt)for(let wt=0;wt<A.length;wt++)e.bindFramebuffer(i.FRAMEBUFFER,Ft.__webglMultisampledFramebuffer),i.framebufferRenderbuffer(i.FRAMEBUFFER,i.COLOR_ATTACHMENT0+wt,i.RENDERBUFFER,null),e.bindFramebuffer(i.FRAMEBUFFER,Ft.__webglFramebuffer),i.framebufferTexture2D(i.DRAW_FRAMEBUFFER,i.COLOR_ATTACHMENT0+wt,i.TEXTURE_2D,null,0);e.bindFramebuffer(i.READ_FRAMEBUFFER,Ft.__webglMultisampledFramebuffer),e.bindFramebuffer(i.DRAW_FRAMEBUFFER,Ft.__webglFramebuffer);for(let wt=0;wt<A.length;wt++){if(P.resolveDepthBuffer&&(P.depthBuffer&&(it|=i.DEPTH_BUFFER_BIT),P.stencilBuffer&&P.resolveStencilBuffer&&(it|=i.STENCIL_BUFFER_BIT)),gt){i.framebufferRenderbuffer(i.READ_FRAMEBUFFER,i.COLOR_ATTACHMENT0,i.RENDERBUFFER,Ft.__webglColorRenderbuffer[wt]);let Qt=n.get(A[wt]).__webglTexture;i.framebufferTexture2D(i.DRAW_FRAMEBUFFER,i.COLOR_ATTACHMENT0,i.TEXTURE_2D,Qt,0)}i.blitFramebuffer(0,0,W,j,0,0,W,j,it,i.NEAREST),l===!0&&(ct.length=0,L.length=0,ct.push(i.COLOR_ATTACHMENT0+wt),P.depthBuffer&&P.resolveDepthBuffer===!1&&(ct.push(Q),L.push(Q),i.invalidateFramebuffer(i.DRAW_FRAMEBUFFER,L)),i.invalidateFramebuffer(i.READ_FRAMEBUFFER,ct))}if(e.bindFramebuffer(i.READ_FRAMEBUFFER,null),e.bindFramebuffer(i.DRAW_FRAMEBUFFER,null),gt)for(let wt=0;wt<A.length;wt++){e.bindFramebuffer(i.FRAMEBUFFER,Ft.__webglMultisampledFramebuffer),i.framebufferRenderbuffer(i.FRAMEBUFFER,i.COLOR_ATTACHMENT0+wt,i.RENDERBUFFER,Ft.__webglColorRenderbuffer[wt]);let Qt=n.get(A[wt]).__webglTexture;e.bindFramebuffer(i.FRAMEBUFFER,Ft.__webglFramebuffer),i.framebufferTexture2D(i.DRAW_FRAMEBUFFER,i.COLOR_ATTACHMENT0+wt,i.TEXTURE_2D,Qt,0)}e.bindFramebuffer(i.DRAW_FRAMEBUFFER,Ft.__webglMultisampledFramebuffer)}else if(P.depthBuffer&&P.resolveDepthBuffer===!1&&l){let A=P.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT;i.invalidateFramebuffer(i.DRAW_FRAMEBUFFER,[A])}}}function ot(P){return Math.min(s.maxSamples,P.samples)}function bt(P){let A=n.get(P);return P.samples>0&&t.has("WEBGL_multisampled_render_to_texture")===!0&&A.__useRenderToTexture!==!1}function ft(P){let A=o.render.frame;u.get(P)!==A&&(u.set(P,A),P.update())}function zt(P,A){let W=P.colorSpace,j=P.format,it=P.type;return P.isCompressedTexture===!0||P.isVideoTexture===!0||W!==Us&&W!==mi&&(ie.getTransfer(W)===ye?(j!==bn||it!==Kn)&&console.warn("THREE.WebGLTextures: sRGB encoded textures have to use RGBAFormat and UnsignedByteType."):console.error("THREE.WebGLTextures: Unsupported texture color space:",W)),A}function Tt(P){return typeof HTMLImageElement<"u"&&P instanceof HTMLImageElement?(h.width=P.naturalWidth||P.width,h.height=P.naturalHeight||P.height):typeof VideoFrame<"u"&&P instanceof VideoFrame?(h.width=P.displayWidth,h.height=P.displayHeight):(h.width=P.width,h.height=P.height),h}this.allocateTextureUnit=R,this.resetTextureUnits=U,this.setTexture2D=V,this.setTexture2DArray=z,this.setTexture3D=O,this.setTextureCube=D,this.rebindTextures=Ht,this.setupRenderTarget=ne,this.updateRenderTargetMipmap=nt,this.updateMultisampleRenderTarget=Dt,this.setupDepthRenderbuffer=Xt,this.setupFrameBufferTexture=_t,this.useMultisampledRTT=bt}function Qx(i,t){function e(n,s=mi){let r,o=ie.getTransfer(s);if(n===Kn)return i.UNSIGNED_BYTE;if(n===Bc)return i.UNSIGNED_SHORT_4_4_4_4;if(n===zc)return i.UNSIGNED_SHORT_5_5_5_1;if(n===Ju)return i.UNSIGNED_INT_5_9_9_9_REV;if(n===Zu)return i.BYTE;if(n===$u)return i.SHORT;if(n===fr)return i.UNSIGNED_SHORT;if(n===Nc)return i.INT;if(n===ki)return i.UNSIGNED_INT;if(n===Ln)return i.FLOAT;if(n===Rr)return i.HALF_FLOAT;if(n===Ku)return i.ALPHA;if(n===ju)return i.RGB;if(n===bn)return i.RGBA;if(n===Qu)return i.LUMINANCE;if(n===tf)return i.LUMINANCE_ALPHA;if(n===Ss)return i.DEPTH_COMPONENT;if(n===Is)return i.DEPTH_STENCIL;if(n===Oc)return i.RED;if(n===Hc)return i.RED_INTEGER;if(n===ef)return i.RG;if(n===kc)return i.RG_INTEGER;if(n===Gc)return i.RGBA_INTEGER;if(n===_o||n===vo||n===Mo||n===Eo)if(o===ye)if(r=t.get("WEBGL_compressed_texture_s3tc_srgb"),r!==null){if(n===_o)return r.COMPRESSED_SRGB_S3TC_DXT1_EXT;if(n===vo)return r.COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT;if(n===Mo)return r.COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT;if(n===Eo)return r.COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT}else return null;else if(r=t.get("WEBGL_compressed_texture_s3tc"),r!==null){if(n===_o)return r.COMPRESSED_RGB_S3TC_DXT1_EXT;if(n===vo)return r.COMPRESSED_RGBA_S3TC_DXT1_EXT;if(n===Mo)return r.COMPRESSED_RGBA_S3TC_DXT3_EXT;if(n===Eo)return r.COMPRESSED_RGBA_S3TC_DXT5_EXT}else return null;if(n===yl||n===_l||n===vl||n===Ml)if(r=t.get("WEBGL_compressed_texture_pvrtc"),r!==null){if(n===yl)return r.COMPRESSED_RGB_PVRTC_4BPPV1_IMG;if(n===_l)return r.COMPRESSED_RGB_PVRTC_2BPPV1_IMG;if(n===vl)return r.COMPRESSED_RGBA_PVRTC_4BPPV1_IMG;if(n===Ml)return r.COMPRESSED_RGBA_PVRTC_2BPPV1_IMG}else return null;if(n===El||n===Sl||n===Tl)if(r=t.get("WEBGL_compressed_texture_etc"),r!==null){if(n===El||n===Sl)return o===ye?r.COMPRESSED_SRGB8_ETC2:r.COMPRESSED_RGB8_ETC2;if(n===Tl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ETC2_EAC:r.COMPRESSED_RGBA8_ETC2_EAC}else return null;if(n===wl||n===bl||n===Al||n===Rl||n===Cl||n===Fl||n===Il||n===Pl||n===Ll||n===Dl||n===Ul||n===Nl||n===Bl||n===zl)if(r=t.get("WEBGL_compressed_texture_astc"),r!==null){if(n===wl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_4x4_KHR:r.COMPRESSED_RGBA_ASTC_4x4_KHR;if(n===bl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_5x4_KHR:r.COMPRESSED_RGBA_ASTC_5x4_KHR;if(n===Al)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_5x5_KHR:r.COMPRESSED_RGBA_ASTC_5x5_KHR;if(n===Rl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_6x5_KHR:r.COMPRESSED_RGBA_ASTC_6x5_KHR;if(n===Cl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_6x6_KHR:r.COMPRESSED_RGBA_ASTC_6x6_KHR;if(n===Fl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_8x5_KHR:r.COMPRESSED_RGBA_ASTC_8x5_KHR;if(n===Il)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_8x6_KHR:r.COMPRESSED_RGBA_ASTC_8x6_KHR;if(n===Pl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_8x8_KHR:r.COMPRESSED_RGBA_ASTC_8x8_KHR;if(n===Ll)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_10x5_KHR:r.COMPRESSED_RGBA_ASTC_10x5_KHR;if(n===Dl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_10x6_KHR:r.COMPRESSED_RGBA_ASTC_10x6_KHR;if(n===Ul)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_10x8_KHR:r.COMPRESSED_RGBA_ASTC_10x8_KHR;if(n===Nl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_10x10_KHR:r.COMPRESSED_RGBA_ASTC_10x10_KHR;if(n===Bl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_12x10_KHR:r.COMPRESSED_RGBA_ASTC_12x10_KHR;if(n===zl)return o===ye?r.COMPRESSED_SRGB8_ALPHA8_ASTC_12x12_KHR:r.COMPRESSED_RGBA_ASTC_12x12_KHR}else return null;if(n===So||n===Ol||n===Hl)if(r=t.get("EXT_texture_compression_bptc"),r!==null){if(n===So)return o===ye?r.COMPRESSED_SRGB_ALPHA_BPTC_UNORM_EXT:r.COMPRESSED_RGBA_BPTC_UNORM_EXT;if(n===Ol)return r.COMPRESSED_RGB_BPTC_SIGNED_FLOAT_EXT;if(n===Hl)return r.COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT_EXT}else return null;if(n===nf||n===kl||n===Gl||n===Vl)if(r=t.get("EXT_texture_compression_rgtc"),r!==null){if(n===So)return r.COMPRESSED_RED_RGTC1_EXT;if(n===kl)return r.COMPRESSED_SIGNED_RED_RGTC1_EXT;if(n===Gl)return r.COMPRESSED_RED_GREEN_RGTC2_EXT;if(n===Vl)return r.COMPRESSED_SIGNED_RED_GREEN_RGTC2_EXT}else return null;return n===Fs?i.UNSIGNED_INT_24_8:i[n]!==void 0?i[n]:null}return{convert:e}}var oc=class extends Je{constructor(t=[]){super(),this.isArrayCamera=!0,this.cameras=t}},fe=class extends We{constructor(){super(),this.isGroup=!0,this.type="Group"}},ty={type:"move"},lr=class{constructor(){this._targetRay=null,this._grip=null,this._hand=null}getHandSpace(){return this._hand===null&&(this._hand=new fe,this._hand.matrixAutoUpdate=!1,this._hand.visible=!1,this._hand.joints={},this._hand.inputState={pinching:!1}),this._hand}getTargetRaySpace(){return this._targetRay===null&&(this._targetRay=new fe,this._targetRay.matrixAutoUpdate=!1,this._targetRay.visible=!1,this._targetRay.hasLinearVelocity=!1,this._targetRay.linearVelocity=new B,this._targetRay.hasAngularVelocity=!1,this._targetRay.angularVelocity=new B),this._targetRay}getGripSpace(){return this._grip===null&&(this._grip=new fe,this._grip.matrixAutoUpdate=!1,this._grip.visible=!1,this._grip.hasLinearVelocity=!1,this._grip.linearVelocity=new B,this._grip.hasAngularVelocity=!1,this._grip.angularVelocity=new B),this._grip}dispatchEvent(t){return this._targetRay!==null&&this._targetRay.dispatchEvent(t),this._grip!==null&&this._grip.dispatchEvent(t),this._hand!==null&&this._hand.dispatchEvent(t),this}connect(t){if(t&&t.hand){let e=this._hand;if(e)for(let n of t.hand.values())this._getHandJoint(e,n)}return this.dispatchEvent({type:"connected",data:t}),this}disconnect(t){return this.dispatchEvent({type:"disconnected",data:t}),this._targetRay!==null&&(this._targetRay.visible=!1),this._grip!==null&&(this._grip.visible=!1),this._hand!==null&&(this._hand.visible=!1),this}update(t,e,n){let s=null,r=null,o=null,a=this._targetRay,l=this._grip,h=this._hand;if(t&&e.session.visibilityState!=="visible-blurred"){if(h&&t.hand){o=!0;for(let d of t.hand.values()){let y=e.getJointPose(d,n),c=this._getHandJoint(h,d);y!==null&&(c.matrix.fromArray(y.transform.matrix),c.matrix.decompose(c.position,c.rotation,c.scale),c.matrixWorldNeedsUpdate=!0,c.jointRadius=y.radius),c.visible=y!==null}let u=h.joints["index-finger-tip"],m=h.joints["thumb-tip"],x=u.position.distanceTo(m.position),f=.02,v=.005;h.inputState.pinching&&x>f+v?(h.inputState.pinching=!1,this.dispatchEvent({type:"pinchend",handedness:t.handedness,target:this})):!h.inputState.pinching&&x<=f-v&&(h.inputState.pinching=!0,this.dispatchEvent({type:"pinchstart",handedness:t.handedness,target:this}))}else l!==null&&t.gripSpace&&(r=e.getPose(t.gripSpace,n),r!==null&&(l.matrix.fromArray(r.transform.matrix),l.matrix.decompose(l.position,l.rotation,l.scale),l.matrixWorldNeedsUpdate=!0,r.linearVelocity?(l.hasLinearVelocity=!0,l.linearVelocity.copy(r.linearVelocity)):l.hasLinearVelocity=!1,r.angularVelocity?(l.hasAngularVelocity=!0,l.angularVelocity.copy(r.angularVelocity)):l.hasAngularVelocity=!1));a!==null&&(s=e.getPose(t.targetRaySpace,n),s===null&&r!==null&&(s=r),s!==null&&(a.matrix.fromArray(s.transform.matrix),a.matrix.decompose(a.position,a.rotation,a.scale),a.matrixWorldNeedsUpdate=!0,s.linearVelocity?(a.hasLinearVelocity=!0,a.linearVelocity.copy(s.linearVelocity)):a.hasLinearVelocity=!1,s.angularVelocity?(a.hasAngularVelocity=!0,a.angularVelocity.copy(s.angularVelocity)):a.hasAngularVelocity=!1,this.dispatchEvent(ty)))}return a!==null&&(a.visible=s!==null),l!==null&&(l.visible=r!==null),h!==null&&(h.visible=o!==null),this}_getHandJoint(t,e){if(t.joints[e.jointName]===void 0){let n=new fe;n.matrixAutoUpdate=!1,n.visible=!1,t.joints[e.jointName]=n,t.add(n)}return t.joints[e.jointName]}},ey=`
void main() {

	gl_Position = vec4( position, 1.0 );

}`,ny=`
uniform sampler2DArray depthColor;
uniform float depthWidth;
uniform float depthHeight;

void main() {

	vec2 coord = vec2( gl_FragCoord.x / depthWidth, gl_FragCoord.y / depthHeight );

	if ( coord.x >= 1.0 ) {

		gl_FragDepth = texture( depthColor, vec3( coord.x - 1.0, coord.y, 1 ) ).r;

	} else {

		gl_FragDepth = texture( depthColor, vec3( coord.x, coord.y, 0 ) ).r;

	}

}`,ac=class{constructor(){this.texture=null,this.mesh=null,this.depthNear=0,this.depthFar=0}init(t,e,n){if(this.texture===null){let s=new nn,r=t.properties.get(s);r.__webglTexture=e.texture,(e.depthNear!=n.depthNear||e.depthFar!=n.depthFar)&&(this.depthNear=e.depthNear,this.depthFar=e.depthFar),this.texture=s}}getMesh(t){if(this.texture!==null&&this.mesh===null){let e=t.cameras[0].viewport,n=new xn({vertexShader:ey,fragmentShader:ny,uniforms:{depthColor:{value:this.texture},depthWidth:{value:e.z},depthHeight:{value:e.w}}});this.mesh=new Z(new Qe(20,20),n)}return this.mesh}reset(){this.texture=null,this.mesh=null}getDepthTexture(){return this.texture}},lc=class extends vi{constructor(t,e){super();let n=this,s=null,r=1,o=null,a="local-floor",l=1,h=null,u=null,m=null,x=null,f=null,v=null,d=new ac,y=e.getContextAttributes(),c=null,_=null,p=[],g=[],T=new lt,S=null,b=new Je;b.viewport=new Ue;let w=new Je;w.viewport=new Ue;let M=[b,w],E=new oc,F=null,U=null;this.cameraAutoUpdate=!0,this.enabled=!1,this.isPresenting=!1,this.getController=function(J){let st=p[J];return st===void 0&&(st=new lr,p[J]=st),st.getTargetRaySpace()},this.getControllerGrip=function(J){let st=p[J];return st===void 0&&(st=new lr,p[J]=st),st.getGripSpace()},this.getHand=function(J){let st=p[J];return st===void 0&&(st=new lr,p[J]=st),st.getHandSpace()};function R(J){let st=g.indexOf(J.inputSource);if(st===-1)return;let _t=p[st];_t!==void 0&&(_t.update(J.inputSource,J.frame,h||o),_t.dispatchEvent({type:J.type,data:J.inputSource}))}function N(){s.removeEventListener("select",R),s.removeEventListener("selectstart",R),s.removeEventListener("selectend",R),s.removeEventListener("squeeze",R),s.removeEventListener("squeezestart",R),s.removeEventListener("squeezeend",R),s.removeEventListener("end",N),s.removeEventListener("inputsourceschange",V);for(let J=0;J<p.length;J++){let st=g[J];st!==null&&(g[J]=null,p[J].disconnect(st))}F=null,U=null,d.reset(),t.setRenderTarget(c),f=null,x=null,m=null,s=null,_=null,Ct.stop(),n.isPresenting=!1,t.setPixelRatio(S),t.setSize(T.width,T.height,!1),n.dispatchEvent({type:"sessionend"})}this.setFramebufferScaleFactor=function(J){r=J,n.isPresenting===!0&&console.warn("THREE.WebXRManager: Cannot change framebuffer scale while presenting.")},this.setReferenceSpaceType=function(J){a=J,n.isPresenting===!0&&console.warn("THREE.WebXRManager: Cannot change reference space type while presenting.")},this.getReferenceSpace=function(){return h||o},this.setReferenceSpace=function(J){h=J},this.getBaseLayer=function(){return x!==null?x:f},this.getBinding=function(){return m},this.getFrame=function(){return v},this.getSession=function(){return s},this.setSession=async function(J){if(s=J,s!==null){if(c=t.getRenderTarget(),s.addEventListener("select",R),s.addEventListener("selectstart",R),s.addEventListener("selectend",R),s.addEventListener("squeeze",R),s.addEventListener("squeezestart",R),s.addEventListener("squeezeend",R),s.addEventListener("end",N),s.addEventListener("inputsourceschange",V),y.xrCompatible!==!0&&await e.makeXRCompatible(),S=t.getPixelRatio(),t.getSize(T),s.renderState.layers===void 0){let st={antialias:y.antialias,alpha:!0,depth:y.depth,stencil:y.stencil,framebufferScaleFactor:r};f=new XRWebGLLayer(s,e,st),s.updateRenderState({baseLayer:f}),t.setPixelRatio(1),t.setSize(f.framebufferWidth,f.framebufferHeight,!1),_=new jn(f.framebufferWidth,f.framebufferHeight,{format:bn,type:Kn,colorSpace:t.outputColorSpace,stencilBuffer:y.stencil})}else{let st=null,_t=null,rt=null;y.depth&&(rt=y.stencil?e.DEPTH24_STENCIL8:e.DEPTH_COMPONENT24,st=y.stencil?Is:Ss,_t=y.stencil?Fs:ki);let Nt={colorFormat:e.RGBA8,depthFormat:rt,scaleFactor:r};m=new XRWebGLBinding(s,e),x=m.createProjectionLayer(Nt),s.updateRenderState({layers:[x]}),t.setPixelRatio(1),t.setSize(x.textureWidth,x.textureHeight,!1),_=new jn(x.textureWidth,x.textureHeight,{format:bn,type:Kn,depthTexture:new No(x.textureWidth,x.textureHeight,_t,void 0,void 0,void 0,void 0,void 0,void 0,st),stencilBuffer:y.stencil,colorSpace:t.outputColorSpace,samples:y.antialias?4:0,resolveDepthBuffer:x.ignoreDepthValues===!1})}_.isXRRenderTarget=!0,this.setFoveation(l),h=null,o=await s.requestReferenceSpace(a),Ct.setContext(s),Ct.start(),n.isPresenting=!0,n.dispatchEvent({type:"sessionstart"})}},this.getEnvironmentBlendMode=function(){if(s!==null)return s.environmentBlendMode},this.getDepthTexture=function(){return d.getDepthTexture()};function V(J){for(let st=0;st<J.removed.length;st++){let _t=J.removed[st],rt=g.indexOf(_t);rt>=0&&(g[rt]=null,p[rt].disconnect(_t))}for(let st=0;st<J.added.length;st++){let _t=J.added[st],rt=g.indexOf(_t);if(rt===-1){for(let Xt=0;Xt<p.length;Xt++)if(Xt>=g.length){g.push(_t),rt=Xt;break}else if(g[Xt]===null){g[Xt]=_t,rt=Xt;break}if(rt===-1)break}let Nt=p[rt];Nt&&Nt.connect(_t)}}let z=new B,O=new B;function D(J,st,_t){z.setFromMatrixPosition(st.matrixWorld),O.setFromMatrixPosition(_t.matrixWorld);let rt=z.distanceTo(O),Nt=st.projectionMatrix.elements,Xt=_t.projectionMatrix.elements,Ht=Nt[14]/(Nt[10]-1),ne=Nt[14]/(Nt[10]+1),nt=(Nt[9]+1)/Nt[5],ct=(Nt[9]-1)/Nt[5],L=(Nt[8]-1)/Nt[0],Dt=(Xt[8]+1)/Xt[0],ot=Ht*L,bt=Ht*Dt,ft=rt/(-L+Dt),zt=ft*-L;if(st.matrixWorld.decompose(J.position,J.quaternion,J.scale),J.translateX(zt),J.translateZ(ft),J.matrixWorld.compose(J.position,J.quaternion,J.scale),J.matrixWorldInverse.copy(J.matrixWorld).invert(),Nt[10]===-1)J.projectionMatrix.copy(st.projectionMatrix),J.projectionMatrixInverse.copy(st.projectionMatrixInverse);else{let Tt=Ht+ft,P=ne+ft,A=ot-zt,W=bt+(rt-zt),j=nt*ne/P*Tt,it=ct*ne/P*Tt;J.projectionMatrix.makePerspective(A,W,j,it,Tt,P),J.projectionMatrixInverse.copy(J.projectionMatrix).invert()}}function X(J,st){st===null?J.matrixWorld.copy(J.matrix):J.matrixWorld.multiplyMatrices(st.matrixWorld,J.matrix),J.matrixWorldInverse.copy(J.matrixWorld).invert()}this.updateCamera=function(J){if(s===null)return;let st=J.near,_t=J.far;d.texture!==null&&(d.depthNear>0&&(st=d.depthNear),d.depthFar>0&&(_t=d.depthFar)),E.near=w.near=b.near=st,E.far=w.far=b.far=_t,(F!==E.near||U!==E.far)&&(s.updateRenderState({depthNear:E.near,depthFar:E.far}),F=E.near,U=E.far),b.layers.mask=J.layers.mask|2,w.layers.mask=J.layers.mask|4,E.layers.mask=b.layers.mask|w.layers.mask;let rt=J.parent,Nt=E.cameras;X(E,rt);for(let Xt=0;Xt<Nt.length;Xt++)X(Nt[Xt],rt);Nt.length===2?D(E,b,w):E.projectionMatrix.copy(b.projectionMatrix),$(J,E,rt)};function $(J,st,_t){_t===null?J.matrix.copy(st.matrixWorld):(J.matrix.copy(_t.matrixWorld),J.matrix.invert(),J.matrix.multiply(st.matrixWorld)),J.matrix.decompose(J.position,J.quaternion,J.scale),J.updateMatrixWorld(!0),J.projectionMatrix.copy(st.projectionMatrix),J.projectionMatrixInverse.copy(st.projectionMatrixInverse),J.isPerspectiveCamera&&(J.fov=ql*2*Math.atan(1/J.projectionMatrix.elements[5]),J.zoom=1)}this.getCamera=function(){return E},this.getFoveation=function(){if(!(x===null&&f===null))return l},this.setFoveation=function(J){l=J,x!==null&&(x.fixedFoveation=J),f!==null&&f.fixedFoveation!==void 0&&(f.fixedFoveation=J)},this.hasDepthSensing=function(){return d.texture!==null},this.getDepthSensingMesh=function(){return d.getMesh(E)};let tt=null;function ut(J,st){if(u=st.getViewerPose(h||o),v=st,u!==null){let _t=u.views;f!==null&&(t.setRenderTargetFramebuffer(_,f.framebuffer),t.setRenderTarget(_));let rt=!1;_t.length!==E.cameras.length&&(E.cameras.length=0,rt=!0);for(let Xt=0;Xt<_t.length;Xt++){let Ht=_t[Xt],ne=null;if(f!==null)ne=f.getViewport(Ht);else{let ct=m.getViewSubImage(x,Ht);ne=ct.viewport,Xt===0&&(t.setRenderTargetTextures(_,ct.colorTexture,x.ignoreDepthValues?void 0:ct.depthStencilTexture),t.setRenderTarget(_))}let nt=M[Xt];nt===void 0&&(nt=new Je,nt.layers.enable(Xt),nt.viewport=new Ue,M[Xt]=nt),nt.matrix.fromArray(Ht.transform.matrix),nt.matrix.decompose(nt.position,nt.quaternion,nt.scale),nt.projectionMatrix.fromArray(Ht.projectionMatrix),nt.projectionMatrixInverse.copy(nt.projectionMatrix).invert(),nt.viewport.set(ne.x,ne.y,ne.width,ne.height),Xt===0&&(E.matrix.copy(nt.matrix),E.matrix.decompose(E.position,E.quaternion,E.scale)),rt===!0&&E.cameras.push(nt)}let Nt=s.enabledFeatures;if(Nt&&Nt.includes("depth-sensing")){let Xt=m.getDepthInformation(_t[0]);Xt&&Xt.isValid&&Xt.texture&&d.init(t,Xt,s.renderState)}}for(let _t=0;_t<p.length;_t++){let rt=g[_t],Nt=p[_t];rt!==null&&Nt!==void 0&&Nt.update(rt,st,h||o)}tt&&tt(J,st),st.detectedPlanes&&n.dispatchEvent({type:"planesdetected",data:st}),v=null}let Ct=new cf;Ct.setAnimationLoop(ut),this.setAnimationLoop=function(J){tt=J},this.dispose=function(){}}},Ui=new sn,iy=new ee;function sy(i,t){function e(y,c){y.matrixAutoUpdate===!0&&y.updateMatrix(),c.value.copy(y.matrix)}function n(y,c){c.color.getRGB(y.fogColor.value,lf(i)),c.isFog?(y.fogNear.value=c.near,y.fogFar.value=c.far):c.isFogExp2&&(y.fogDensity.value=c.density)}function s(y,c,_,p,g){c.isMeshBasicMaterial||c.isMeshLambertMaterial?r(y,c):c.isMeshToonMaterial?(r(y,c),m(y,c)):c.isMeshPhongMaterial?(r(y,c),u(y,c)):c.isMeshStandardMaterial?(r(y,c),x(y,c),c.isMeshPhysicalMaterial&&f(y,c,g)):c.isMeshMatcapMaterial?(r(y,c),v(y,c)):c.isMeshDepthMaterial?r(y,c):c.isMeshDistanceMaterial?(r(y,c),d(y,c)):c.isMeshNormalMaterial?r(y,c):c.isLineBasicMaterial?(o(y,c),c.isLineDashedMaterial&&a(y,c)):c.isPointsMaterial?l(y,c,_,p):c.isSpriteMaterial?h(y,c):c.isShadowMaterial?(y.color.value.copy(c.color),y.opacity.value=c.opacity):c.isShaderMaterial&&(c.uniformsNeedUpdate=!1)}function r(y,c){y.opacity.value=c.opacity,c.color&&y.diffuse.value.copy(c.color),c.emissive&&y.emissive.value.copy(c.emissive).multiplyScalar(c.emissiveIntensity),c.map&&(y.map.value=c.map,e(c.map,y.mapTransform)),c.alphaMap&&(y.alphaMap.value=c.alphaMap,e(c.alphaMap,y.alphaMapTransform)),c.bumpMap&&(y.bumpMap.value=c.bumpMap,e(c.bumpMap,y.bumpMapTransform),y.bumpScale.value=c.bumpScale,c.side===en&&(y.bumpScale.value*=-1)),c.normalMap&&(y.normalMap.value=c.normalMap,e(c.normalMap,y.normalMapTransform),y.normalScale.value.copy(c.normalScale),c.side===en&&y.normalScale.value.negate()),c.displacementMap&&(y.displacementMap.value=c.displacementMap,e(c.displacementMap,y.displacementMapTransform),y.displacementScale.value=c.displacementScale,y.displacementBias.value=c.displacementBias),c.emissiveMap&&(y.emissiveMap.value=c.emissiveMap,e(c.emissiveMap,y.emissiveMapTransform)),c.specularMap&&(y.specularMap.value=c.specularMap,e(c.specularMap,y.specularMapTransform)),c.alphaTest>0&&(y.alphaTest.value=c.alphaTest);let _=t.get(c),p=_.envMap,g=_.envMapRotation;p&&(y.envMap.value=p,Ui.copy(g),Ui.x*=-1,Ui.y*=-1,Ui.z*=-1,p.isCubeTexture&&p.isRenderTargetTexture===!1&&(Ui.y*=-1,Ui.z*=-1),y.envMapRotation.value.setFromMatrix4(iy.makeRotationFromEuler(Ui)),y.flipEnvMap.value=p.isCubeTexture&&p.isRenderTargetTexture===!1?-1:1,y.reflectivity.value=c.reflectivity,y.ior.value=c.ior,y.refractionRatio.value=c.refractionRatio),c.lightMap&&(y.lightMap.value=c.lightMap,y.lightMapIntensity.value=c.lightMapIntensity,e(c.lightMap,y.lightMapTransform)),c.aoMap&&(y.aoMap.value=c.aoMap,y.aoMapIntensity.value=c.aoMapIntensity,e(c.aoMap,y.aoMapTransform))}function o(y,c){y.diffuse.value.copy(c.color),y.opacity.value=c.opacity,c.map&&(y.map.value=c.map,e(c.map,y.mapTransform))}function a(y,c){y.dashSize.value=c.dashSize,y.totalSize.value=c.dashSize+c.gapSize,y.scale.value=c.scale}function l(y,c,_,p){y.diffuse.value.copy(c.color),y.opacity.value=c.opacity,y.size.value=c.size*_,y.scale.value=p*.5,c.map&&(y.map.value=c.map,e(c.map,y.uvTransform)),c.alphaMap&&(y.alphaMap.value=c.alphaMap,e(c.alphaMap,y.alphaMapTransform)),c.alphaTest>0&&(y.alphaTest.value=c.alphaTest)}function h(y,c){y.diffuse.value.copy(c.color),y.opacity.value=c.opacity,y.rotation.value=c.rotation,c.map&&(y.map.value=c.map,e(c.map,y.mapTransform)),c.alphaMap&&(y.alphaMap.value=c.alphaMap,e(c.alphaMap,y.alphaMapTransform)),c.alphaTest>0&&(y.alphaTest.value=c.alphaTest)}function u(y,c){y.specular.value.copy(c.specular),y.shininess.value=Math.max(c.shininess,1e-4)}function m(y,c){c.gradientMap&&(y.gradientMap.value=c.gradientMap)}function x(y,c){y.metalness.value=c.metalness,c.metalnessMap&&(y.metalnessMap.value=c.metalnessMap,e(c.metalnessMap,y.metalnessMapTransform)),y.roughness.value=c.roughness,c.roughnessMap&&(y.roughnessMap.value=c.roughnessMap,e(c.roughnessMap,y.roughnessMapTransform)),c.envMap&&(y.envMapIntensity.value=c.envMapIntensity)}function f(y,c,_){y.ior.value=c.ior,c.sheen>0&&(y.sheenColor.value.copy(c.sheenColor).multiplyScalar(c.sheen),y.sheenRoughness.value=c.sheenRoughness,c.sheenColorMap&&(y.sheenColorMap.value=c.sheenColorMap,e(c.sheenColorMap,y.sheenColorMapTransform)),c.sheenRoughnessMap&&(y.sheenRoughnessMap.value=c.sheenRoughnessMap,e(c.sheenRoughnessMap,y.sheenRoughnessMapTransform))),c.clearcoat>0&&(y.clearcoat.value=c.clearcoat,y.clearcoatRoughness.value=c.clearcoatRoughness,c.clearcoatMap&&(y.clearcoatMap.value=c.clearcoatMap,e(c.clearcoatMap,y.clearcoatMapTransform)),c.clearcoatRoughnessMap&&(y.clearcoatRoughnessMap.value=c.clearcoatRoughnessMap,e(c.clearcoatRoughnessMap,y.clearcoatRoughnessMapTransform)),c.clearcoatNormalMap&&(y.clearcoatNormalMap.value=c.clearcoatNormalMap,e(c.clearcoatNormalMap,y.clearcoatNormalMapTransform),y.clearcoatNormalScale.value.copy(c.clearcoatNormalScale),c.side===en&&y.clearcoatNormalScale.value.negate())),c.dispersion>0&&(y.dispersion.value=c.dispersion),c.iridescence>0&&(y.iridescence.value=c.iridescence,y.iridescenceIOR.value=c.iridescenceIOR,y.iridescenceThicknessMinimum.value=c.iridescenceThicknessRange[0],y.iridescenceThicknessMaximum.value=c.iridescenceThicknessRange[1],c.iridescenceMap&&(y.iridescenceMap.value=c.iridescenceMap,e(c.iridescenceMap,y.iridescenceMapTransform)),c.iridescenceThicknessMap&&(y.iridescenceThicknessMap.value=c.iridescenceThicknessMap,e(c.iridescenceThicknessMap,y.iridescenceThicknessMapTransform))),c.transmission>0&&(y.transmission.value=c.transmission,y.transmissionSamplerMap.value=_.texture,y.transmissionSamplerSize.value.set(_.width,_.height),c.transmissionMap&&(y.transmissionMap.value=c.transmissionMap,e(c.transmissionMap,y.transmissionMapTransform)),y.thickness.value=c.thickness,c.thicknessMap&&(y.thicknessMap.value=c.thicknessMap,e(c.thicknessMap,y.thicknessMapTransform)),y.attenuationDistance.value=c.attenuationDistance,y.attenuationColor.value.copy(c.attenuationColor)),c.anisotropy>0&&(y.anisotropyVector.value.set(c.anisotropy*Math.cos(c.anisotropyRotation),c.anisotropy*Math.sin(c.anisotropyRotation)),c.anisotropyMap&&(y.anisotropyMap.value=c.anisotropyMap,e(c.anisotropyMap,y.anisotropyMapTransform))),y.specularIntensity.value=c.specularIntensity,y.specularColor.value.copy(c.specularColor),c.specularColorMap&&(y.specularColorMap.value=c.specularColorMap,e(c.specularColorMap,y.specularColorMapTransform)),c.specularIntensityMap&&(y.specularIntensityMap.value=c.specularIntensityMap,e(c.specularIntensityMap,y.specularIntensityMapTransform))}function v(y,c){c.matcap&&(y.matcap.value=c.matcap)}function d(y,c){let _=t.get(c).light;y.referencePosition.value.setFromMatrixPosition(_.matrixWorld),y.nearDistance.value=_.shadow.camera.near,y.farDistance.value=_.shadow.camera.far}return{refreshFogUniforms:n,refreshMaterialUniforms:s}}function ry(i,t,e,n){let s={},r={},o=[],a=i.getParameter(i.MAX_UNIFORM_BUFFER_BINDINGS);function l(_,p){let g=p.program;n.uniformBlockBinding(_,g)}function h(_,p){let g=s[_.id];g===void 0&&(v(_),g=u(_),s[_.id]=g,_.addEventListener("dispose",y));let T=p.program;n.updateUBOMapping(_,T);let S=t.render.frame;r[_.id]!==S&&(x(_),r[_.id]=S)}function u(_){let p=m();_.__bindingPointIndex=p;let g=i.createBuffer(),T=_.__size,S=_.usage;return i.bindBuffer(i.UNIFORM_BUFFER,g),i.bufferData(i.UNIFORM_BUFFER,T,S),i.bindBuffer(i.UNIFORM_BUFFER,null),i.bindBufferBase(i.UNIFORM_BUFFER,p,g),g}function m(){for(let _=0;_<a;_++)if(o.indexOf(_)===-1)return o.push(_),_;return console.error("THREE.WebGLRenderer: Maximum number of simultaneously usable uniforms groups reached."),0}function x(_){let p=s[_.id],g=_.uniforms,T=_.__cache;i.bindBuffer(i.UNIFORM_BUFFER,p);for(let S=0,b=g.length;S<b;S++){let w=Array.isArray(g[S])?g[S]:[g[S]];for(let M=0,E=w.length;M<E;M++){let F=w[M];if(f(F,S,M,T)===!0){let U=F.__offset,R=Array.isArray(F.value)?F.value:[F.value],N=0;for(let V=0;V<R.length;V++){let z=R[V],O=d(z);typeof z=="number"||typeof z=="boolean"?(F.__data[0]=z,i.bufferSubData(i.UNIFORM_BUFFER,U+N,F.__data)):z.isMatrix3?(F.__data[0]=z.elements[0],F.__data[1]=z.elements[1],F.__data[2]=z.elements[2],F.__data[3]=0,F.__data[4]=z.elements[3],F.__data[5]=z.elements[4],F.__data[6]=z.elements[5],F.__data[7]=0,F.__data[8]=z.elements[6],F.__data[9]=z.elements[7],F.__data[10]=z.elements[8],F.__data[11]=0):(z.toArray(F.__data,N),N+=O.storage/Float32Array.BYTES_PER_ELEMENT)}i.bufferSubData(i.UNIFORM_BUFFER,U,F.__data)}}}i.bindBuffer(i.UNIFORM_BUFFER,null)}function f(_,p,g,T){let S=_.value,b=p+"_"+g;if(T[b]===void 0)return typeof S=="number"||typeof S=="boolean"?T[b]=S:T[b]=S.clone(),!0;{let w=T[b];if(typeof S=="number"||typeof S=="boolean"){if(w!==S)return T[b]=S,!0}else if(w.equals(S)===!1)return w.copy(S),!0}return!1}function v(_){let p=_.uniforms,g=0,T=16;for(let b=0,w=p.length;b<w;b++){let M=Array.isArray(p[b])?p[b]:[p[b]];for(let E=0,F=M.length;E<F;E++){let U=M[E],R=Array.isArray(U.value)?U.value:[U.value];for(let N=0,V=R.length;N<V;N++){let z=R[N],O=d(z),D=g%T,X=D%O.boundary,$=D+X;g+=X,$!==0&&T-$<O.storage&&(g+=T-$),U.__data=new Float32Array(O.storage/Float32Array.BYTES_PER_ELEMENT),U.__offset=g,g+=O.storage}}}let S=g%T;return S>0&&(g+=T-S),_.__size=g,_.__cache={},this}function d(_){let p={boundary:0,storage:0};return typeof _=="number"||typeof _=="boolean"?(p.boundary=4,p.storage=4):_.isVector2?(p.boundary=8,p.storage=8):_.isVector3||_.isColor?(p.boundary=16,p.storage=12):_.isVector4?(p.boundary=16,p.storage=16):_.isMatrix3?(p.boundary=48,p.storage=48):_.isMatrix4?(p.boundary=64,p.storage=64):_.isTexture?console.warn("THREE.WebGLRenderer: Texture samplers can not be part of an uniforms group."):console.warn("THREE.WebGLRenderer: Unsupported uniform value type.",_),p}function y(_){let p=_.target;p.removeEventListener("dispose",y);let g=o.indexOf(p.__bindingPointIndex);o.splice(g,1),i.deleteBuffer(s[p.id]),delete s[p.id],delete r[p.id]}function c(){for(let _ in s)i.deleteBuffer(s[_]);o=[],s={},r={}}return{bind:l,update:h,dispose:c}}var mr=class{constructor(t={}){let{canvas:e=cp(),context:n=null,depth:s=!0,stencil:r=!1,alpha:o=!1,antialias:a=!1,premultipliedAlpha:l=!0,preserveDrawingBuffer:h=!1,powerPreference:u="default",failIfMajorPerformanceCaveat:m=!1,reverseDepthBuffer:x=!1}=t;this.isWebGLRenderer=!0;let f;if(n!==null){if(typeof WebGLRenderingContext<"u"&&n instanceof WebGLRenderingContext)throw new Error("THREE.WebGLRenderer: WebGL 1 is not supported since r163.");f=n.getContextAttributes().alpha}else f=o;let v=new Uint32Array(4),d=new Int32Array(4),y=null,c=null,_=[],p=[];this.domElement=e,this.debug={checkShaderErrors:!0,onShaderError:null},this.autoClear=!0,this.autoClearColor=!0,this.autoClearDepth=!0,this.autoClearStencil=!0,this.sortObjects=!0,this.clippingPlanes=[],this.localClippingEnabled=!1,this._outputColorSpace=ze,this.toneMapping=_i,this.toneMappingExposure=1;let g=this,T=!1,S=0,b=0,w=null,M=-1,E=null,F=new Ue,U=new Ue,R=null,N=new Jt(0),V=0,z=e.width,O=e.height,D=1,X=null,$=null,tt=new Ue(0,0,z,O),ut=new Ue(0,0,z,O),Ct=!1,J=new pr,st=!1,_t=!1,rt=new ee,Nt=new ee,Xt=new B,Ht=new Ue,ne={background:null,fog:null,environment:null,overrideMaterial:null,isScene:!0},nt=!1;function ct(){return w===null?D:1}let L=n;function Dt(C,k){return e.getContext(C,k)}try{let C={alpha:!0,depth:s,stencil:r,antialias:a,premultipliedAlpha:l,preserveDrawingBuffer:h,powerPreference:u,failIfMajorPerformanceCaveat:m};if("setAttribute"in e&&e.setAttribute("data-engine","three.js r170"),e.addEventListener("webglcontextlost",et,!1),e.addEventListener("webglcontextrestored",Et,!1),e.addEventListener("webglcontextcreationerror",vt,!1),L===null){let k="webgl2";if(L=Dt(k,C),L===null)throw Dt(k)?new Error("Error creating WebGL context with your selected attributes."):new Error("Error creating WebGL context.")}}catch(C){throw console.error("THREE.WebGLRenderer: "+C.message),C}let ot,bt,ft,zt,Tt,P,A,W,j,it,Q,Ft,gt,wt,Qt,at,At,Ot,Vt,Rt,te,Kt,Se,H;function yt(){ot=new Mg(L),ot.init(),Kt=new Qx(L,ot),bt=new mg(L,ot,t,Kt),ft=new Jx(L,ot),bt.reverseDepthBuffer&&x&&ft.buffers.depth.setReversed(!0),zt=new Tg(L),Tt=new Bx,P=new jx(L,ot,ft,Tt,bt,Kt,zt),A=new xg(g),W=new vg(g),j=new Ip(L),Se=new dg(L,j),it=new Eg(L,j,zt,Se),Q=new bg(L,it,j,zt),Vt=new wg(L,bt,P),at=new gg(Tt),Ft=new Nx(g,A,W,ot,bt,Se,at),gt=new sy(g,Tt),wt=new Ox,Qt=new Xx(ot),Ot=new fg(g,A,W,ft,Q,f,l),At=new Zx(g,Q,bt),H=new ry(L,zt,bt,ft),Rt=new pg(L,ot,zt),te=new Sg(L,ot,zt),zt.programs=Ft.programs,g.capabilities=bt,g.extensions=ot,g.properties=Tt,g.renderLists=wt,g.shadowMap=At,g.state=ft,g.info=zt}yt();let K=new lc(g,L);this.xr=K,this.getContext=function(){return L},this.getContextAttributes=function(){return L.getContextAttributes()},this.forceContextLoss=function(){let C=ot.get("WEBGL_lose_context");C&&C.loseContext()},this.forceContextRestore=function(){let C=ot.get("WEBGL_lose_context");C&&C.restoreContext()},this.getPixelRatio=function(){return D},this.setPixelRatio=function(C){C!==void 0&&(D=C,this.setSize(z,O,!1))},this.getSize=function(C){return C.set(z,O)},this.setSize=function(C,k,q=!0){if(K.isPresenting){console.warn("THREE.WebGLRenderer: Can't change size while VR device is presenting.");return}z=C,O=k,e.width=Math.floor(C*D),e.height=Math.floor(k*D),q===!0&&(e.style.width=C+"px",e.style.height=k+"px"),this.setViewport(0,0,C,k)},this.getDrawingBufferSize=function(C){return C.set(z*D,O*D).floor()},this.setDrawingBufferSize=function(C,k,q){z=C,O=k,D=q,e.width=Math.floor(C*q),e.height=Math.floor(k*q),this.setViewport(0,0,C,k)},this.getCurrentViewport=function(C){return C.copy(F)},this.getViewport=function(C){return C.copy(tt)},this.setViewport=function(C,k,q,Y){C.isVector4?tt.set(C.x,C.y,C.z,C.w):tt.set(C,k,q,Y),ft.viewport(F.copy(tt).multiplyScalar(D).round())},this.getScissor=function(C){return C.copy(ut)},this.setScissor=function(C,k,q,Y){C.isVector4?ut.set(C.x,C.y,C.z,C.w):ut.set(C,k,q,Y),ft.scissor(U.copy(ut).multiplyScalar(D).round())},this.getScissorTest=function(){return Ct},this.setScissorTest=function(C){ft.setScissorTest(Ct=C)},this.setOpaqueSort=function(C){X=C},this.setTransparentSort=function(C){$=C},this.getClearColor=function(C){return C.copy(Ot.getClearColor())},this.setClearColor=function(){Ot.setClearColor.apply(Ot,arguments)},this.getClearAlpha=function(){return Ot.getClearAlpha()},this.setClearAlpha=function(){Ot.setClearAlpha.apply(Ot,arguments)},this.clear=function(C=!0,k=!0,q=!0){let Y=0;if(C){let G=!1;if(w!==null){let ht=w.texture.format;G=ht===Gc||ht===kc||ht===Hc}if(G){let ht=w.texture.type,Mt=ht===Kn||ht===ki||ht===fr||ht===Fs||ht===Bc||ht===zc,It=Ot.getClearColor(),Pt=Ot.getClearAlpha(),qt=It.r,Zt=It.g,Lt=It.b;Mt?(v[0]=qt,v[1]=Zt,v[2]=Lt,v[3]=Pt,L.clearBufferuiv(L.COLOR,0,v)):(d[0]=qt,d[1]=Zt,d[2]=Lt,d[3]=Pt,L.clearBufferiv(L.COLOR,0,d))}else Y|=L.COLOR_BUFFER_BIT}k&&(Y|=L.DEPTH_BUFFER_BIT),q&&(Y|=L.STENCIL_BUFFER_BIT,this.state.buffers.stencil.setMask(4294967295)),L.clear(Y)},this.clearColor=function(){this.clear(!0,!1,!1)},this.clearDepth=function(){this.clear(!1,!0,!1)},this.clearStencil=function(){this.clear(!1,!1,!0)},this.dispose=function(){e.removeEventListener("webglcontextlost",et,!1),e.removeEventListener("webglcontextrestored",Et,!1),e.removeEventListener("webglcontextcreationerror",vt,!1),wt.dispose(),Qt.dispose(),Tt.dispose(),A.dispose(),W.dispose(),Q.dispose(),Se.dispose(),H.dispose(),Ft.dispose(),K.dispose(),K.removeEventListener("sessionstart",Th),K.removeEventListener("sessionend",wh),Ci.stop()};function et(C){C.preventDefault(),console.log("THREE.WebGLRenderer: Context Lost."),T=!0}function Et(){console.log("THREE.WebGLRenderer: Context Restored."),T=!1;let C=zt.autoReset,k=At.enabled,q=At.autoUpdate,Y=At.needsUpdate,G=At.type;yt(),zt.autoReset=C,At.enabled=k,At.autoUpdate=q,At.needsUpdate=Y,At.type=G}function vt(C){console.error("THREE.WebGLRenderer: A WebGL context could not be created. Reason: ",C.statusMessage)}function Yt(C){let k=C.target;k.removeEventListener("dispose",Yt),De(k)}function De(C){Ye(C),Tt.remove(C)}function Ye(C){let k=Tt.get(C).programs;k!==void 0&&(k.forEach(function(q){Ft.releaseProgram(q)}),C.isShaderMaterial&&Ft.releaseShaderCache(C))}this.renderBufferDirect=function(C,k,q,Y,G,ht){k===null&&(k=ne);let Mt=G.isMesh&&G.matrixWorld.determinant()<0,It=yd(C,k,q,Y,G);ft.setMaterial(Y,Mt);let Pt=q.index,qt=1;if(Y.wireframe===!0){if(Pt=it.getWireframeAttribute(q),Pt===void 0)return;qt=2}let Zt=q.drawRange,Lt=q.attributes.position,re=Zt.start*qt,Te=(Zt.start+Zt.count)*qt;ht!==null&&(re=Math.max(re,ht.start*qt),Te=Math.min(Te,(ht.start+ht.count)*qt)),Pt!==null?(re=Math.max(re,0),Te=Math.min(Te,Pt.count)):Lt!=null&&(re=Math.max(re,0),Te=Math.min(Te,Lt.count));let Ae=Te-re;if(Ae<0||Ae===1/0)return;Se.setup(G,Y,It,q,Pt);let tn,he=Rt;if(Pt!==null&&(tn=j.get(Pt),he=te,he.setIndex(tn)),G.isMesh)Y.wireframe===!0?(ft.setLineWidth(Y.wireframeLinewidth*ct()),he.setMode(L.LINES)):he.setMode(L.TRIANGLES);else if(G.isLine){let Ut=Y.linewidth;Ut===void 0&&(Ut=1),ft.setLineWidth(Ut*ct()),G.isLineSegments?he.setMode(L.LINES):G.isLineLoop?he.setMode(L.LINE_LOOP):he.setMode(L.LINE_STRIP)}else G.isPoints?he.setMode(L.POINTS):G.isSprite&&he.setMode(L.TRIANGLES);if(G.isBatchedMesh)if(G._multiDrawInstances!==null)he.renderMultiDrawInstances(G._multiDrawStarts,G._multiDrawCounts,G._multiDrawCount,G._multiDrawInstances);else if(ot.get("WEBGL_multi_draw"))he.renderMultiDraw(G._multiDrawStarts,G._multiDrawCounts,G._multiDrawCount);else{let Ut=G._multiDrawStarts,Hn=G._multiDrawCounts,ue=G._multiDrawCount,En=Pt?j.get(Pt).bytesPerElement:1,es=Tt.get(Y).currentProgram.getUniforms();for(let an=0;an<ue;an++)es.setValue(L,"_gl_DrawID",an),he.render(Ut[an]/En,Hn[an])}else if(G.isInstancedMesh)he.renderInstances(re,Ae,G.count);else if(q.isInstancedBufferGeometry){let Ut=q._maxInstanceCount!==void 0?q._maxInstanceCount:1/0,Hn=Math.min(q.instanceCount,Ut);he.renderInstances(re,Ae,Hn)}else he.render(re,Ae)};function xe(C,k,q){C.transparent===!0&&C.side===Me&&C.forceSinglePass===!1?(C.side=en,C.needsUpdate=!0,zr(C,k,q),C.side=Dn,C.needsUpdate=!0,zr(C,k,q),C.side=Me):zr(C,k,q)}this.compile=function(C,k,q=null){q===null&&(q=C),c=Qt.get(q),c.init(k),p.push(c),q.traverseVisible(function(G){G.isLight&&G.layers.test(k.layers)&&(c.pushLight(G),G.castShadow&&c.pushShadow(G))}),C!==q&&C.traverseVisible(function(G){G.isLight&&G.layers.test(k.layers)&&(c.pushLight(G),G.castShadow&&c.pushShadow(G))}),c.setupLights();let Y=new Set;return C.traverse(function(G){if(!(G.isMesh||G.isPoints||G.isLine||G.isSprite))return;let ht=G.material;if(ht)if(Array.isArray(ht))for(let Mt=0;Mt<ht.length;Mt++){let It=ht[Mt];xe(It,q,G),Y.add(It)}else xe(ht,q,G),Y.add(ht)}),p.pop(),c=null,Y},this.compileAsync=function(C,k,q=null){let Y=this.compile(C,k,q);return new Promise(G=>{function ht(){if(Y.forEach(function(Mt){Tt.get(Mt).currentProgram.isReady()&&Y.delete(Mt)}),Y.size===0){G(C);return}setTimeout(ht,10)}ot.get("KHR_parallel_shader_compile")!==null?ht():setTimeout(ht,10)})};let Mn=null;function On(C){Mn&&Mn(C)}function Th(){Ci.stop()}function wh(){Ci.start()}let Ci=new cf;Ci.setAnimationLoop(On),typeof self<"u"&&Ci.setContext(self),this.setAnimationLoop=function(C){Mn=C,K.setAnimationLoop(C),C===null?Ci.stop():Ci.start()},K.addEventListener("sessionstart",Th),K.addEventListener("sessionend",wh),this.render=function(C,k){if(k!==void 0&&k.isCamera!==!0){console.error("THREE.WebGLRenderer.render: camera is not an instance of THREE.Camera.");return}if(T===!0)return;if(C.matrixWorldAutoUpdate===!0&&C.updateMatrixWorld(),k.parent===null&&k.matrixWorldAutoUpdate===!0&&k.updateMatrixWorld(),K.enabled===!0&&K.isPresenting===!0&&(K.cameraAutoUpdate===!0&&K.updateCamera(k),k=K.getCamera()),C.isScene===!0&&C.onBeforeRender(g,C,k,w),c=Qt.get(C,p.length),c.init(k),p.push(c),Nt.multiplyMatrices(k.projectionMatrix,k.matrixWorldInverse),J.setFromProjectionMatrix(Nt),_t=this.localClippingEnabled,st=at.init(this.clippingPlanes,_t),y=wt.get(C,_.length),y.init(),_.push(y),K.enabled===!0&&K.isPresenting===!0){let ht=g.xr.getDepthSensingMesh();ht!==null&&Sa(ht,k,-1/0,g.sortObjects)}Sa(C,k,0,g.sortObjects),y.finish(),g.sortObjects===!0&&y.sort(X,$),nt=K.enabled===!1||K.isPresenting===!1||K.hasDepthSensing()===!1,nt&&Ot.addToRenderList(y,C),this.info.render.frame++,st===!0&&at.beginShadows();let q=c.state.shadowsArray;At.render(q,C,k),st===!0&&at.endShadows(),this.info.autoReset===!0&&this.info.reset();let Y=y.opaque,G=y.transmissive;if(c.setupLights(),k.isArrayCamera){let ht=k.cameras;if(G.length>0)for(let Mt=0,It=ht.length;Mt<It;Mt++){let Pt=ht[Mt];Ah(Y,G,C,Pt)}nt&&Ot.render(C);for(let Mt=0,It=ht.length;Mt<It;Mt++){let Pt=ht[Mt];bh(y,C,Pt,Pt.viewport)}}else G.length>0&&Ah(Y,G,C,k),nt&&Ot.render(C),bh(y,C,k);w!==null&&(P.updateMultisampleRenderTarget(w),P.updateRenderTargetMipmap(w)),C.isScene===!0&&C.onAfterRender(g,C,k),Se.resetDefaultState(),M=-1,E=null,p.pop(),p.length>0?(c=p[p.length-1],st===!0&&at.setGlobalState(g.clippingPlanes,c.state.camera)):c=null,_.pop(),_.length>0?y=_[_.length-1]:y=null};function Sa(C,k,q,Y){if(C.visible===!1)return;if(C.layers.test(k.layers)){if(C.isGroup)q=C.renderOrder;else if(C.isLOD)C.autoUpdate===!0&&C.update(k);else if(C.isLight)c.pushLight(C),C.castShadow&&c.pushShadow(C);else if(C.isSprite){if(!C.frustumCulled||J.intersectsSprite(C)){Y&&Ht.setFromMatrixPosition(C.matrixWorld).applyMatrix4(Nt);let Mt=Q.update(C),It=C.material;It.visible&&y.push(C,Mt,It,q,Ht.z,null)}}else if((C.isMesh||C.isLine||C.isPoints)&&(!C.frustumCulled||J.intersectsObject(C))){let Mt=Q.update(C),It=C.material;if(Y&&(C.boundingSphere!==void 0?(C.boundingSphere===null&&C.computeBoundingSphere(),Ht.copy(C.boundingSphere.center)):(Mt.boundingSphere===null&&Mt.computeBoundingSphere(),Ht.copy(Mt.boundingSphere.center)),Ht.applyMatrix4(C.matrixWorld).applyMatrix4(Nt)),Array.isArray(It)){let Pt=Mt.groups;for(let qt=0,Zt=Pt.length;qt<Zt;qt++){let Lt=Pt[qt],re=It[Lt.materialIndex];re&&re.visible&&y.push(C,Mt,re,q,Ht.z,Lt)}}else It.visible&&y.push(C,Mt,It,q,Ht.z,null)}}let ht=C.children;for(let Mt=0,It=ht.length;Mt<It;Mt++)Sa(ht[Mt],k,q,Y)}function bh(C,k,q,Y){let G=C.opaque,ht=C.transmissive,Mt=C.transparent;c.setupLightsView(q),st===!0&&at.setGlobalState(g.clippingPlanes,q),Y&&ft.viewport(F.copy(Y)),G.length>0&&Br(G,k,q),ht.length>0&&Br(ht,k,q),Mt.length>0&&Br(Mt,k,q),ft.buffers.depth.setTest(!0),ft.buffers.depth.setMask(!0),ft.buffers.color.setMask(!0),ft.setPolygonOffset(!1)}function Ah(C,k,q,Y){if((q.isScene===!0?q.overrideMaterial:null)!==null)return;c.state.transmissionRenderTarget[Y.id]===void 0&&(c.state.transmissionRenderTarget[Y.id]=new jn(1,1,{generateMipmaps:!0,type:ot.has("EXT_color_buffer_half_float")||ot.has("EXT_color_buffer_float")?Rr:Kn,minFilter:Hi,samples:4,stencilBuffer:r,resolveDepthBuffer:!1,resolveStencilBuffer:!1,colorSpace:ie.workingColorSpace}));let ht=c.state.transmissionRenderTarget[Y.id],Mt=Y.viewport||F;ht.setSize(Mt.z,Mt.w);let It=g.getRenderTarget();g.setRenderTarget(ht),g.getClearColor(N),V=g.getClearAlpha(),V<1&&g.setClearColor(16777215,.5),g.clear(),nt&&Ot.render(q);let Pt=g.toneMapping;g.toneMapping=_i;let qt=Y.viewport;if(Y.viewport!==void 0&&(Y.viewport=void 0),c.setupLightsView(Y),st===!0&&at.setGlobalState(g.clippingPlanes,Y),Br(C,q,Y),P.updateMultisampleRenderTarget(ht),P.updateRenderTargetMipmap(ht),ot.has("WEBGL_multisampled_render_to_texture")===!1){let Zt=!1;for(let Lt=0,re=k.length;Lt<re;Lt++){let Te=k[Lt],Ae=Te.object,tn=Te.geometry,he=Te.material,Ut=Te.group;if(he.side===Me&&Ae.layers.test(Y.layers)){let Hn=he.side;he.side=en,he.needsUpdate=!0,Rh(Ae,q,Y,tn,he,Ut),he.side=Hn,he.needsUpdate=!0,Zt=!0}}Zt===!0&&(P.updateMultisampleRenderTarget(ht),P.updateRenderTargetMipmap(ht))}g.setRenderTarget(It),g.setClearColor(N,V),qt!==void 0&&(Y.viewport=qt),g.toneMapping=Pt}function Br(C,k,q){let Y=k.isScene===!0?k.overrideMaterial:null;for(let G=0,ht=C.length;G<ht;G++){let Mt=C[G],It=Mt.object,Pt=Mt.geometry,qt=Y===null?Mt.material:Y,Zt=Mt.group;It.layers.test(q.layers)&&Rh(It,k,q,Pt,qt,Zt)}}function Rh(C,k,q,Y,G,ht){C.onBeforeRender(g,k,q,Y,G,ht),C.modelViewMatrix.multiplyMatrices(q.matrixWorldInverse,C.matrixWorld),C.normalMatrix.getNormalMatrix(C.modelViewMatrix),G.onBeforeRender(g,k,q,Y,C,ht),G.transparent===!0&&G.side===Me&&G.forceSinglePass===!1?(G.side=en,G.needsUpdate=!0,g.renderBufferDirect(q,k,Y,G,C,ht),G.side=Dn,G.needsUpdate=!0,g.renderBufferDirect(q,k,Y,G,C,ht),G.side=Me):g.renderBufferDirect(q,k,Y,G,C,ht),C.onAfterRender(g,k,q,Y,G,ht)}function zr(C,k,q){k.isScene!==!0&&(k=ne);let Y=Tt.get(C),G=c.state.lights,ht=c.state.shadowsArray,Mt=G.state.version,It=Ft.getParameters(C,G.state,ht,k,q),Pt=Ft.getProgramCacheKey(It),qt=Y.programs;Y.environment=C.isMeshStandardMaterial?k.environment:null,Y.fog=k.fog,Y.envMap=(C.isMeshStandardMaterial?W:A).get(C.envMap||Y.environment),Y.envMapRotation=Y.environment!==null&&C.envMap===null?k.environmentRotation:C.envMapRotation,qt===void 0&&(C.addEventListener("dispose",Yt),qt=new Map,Y.programs=qt);let Zt=qt.get(Pt);if(Zt!==void 0){if(Y.currentProgram===Zt&&Y.lightsStateVersion===Mt)return Fh(C,It),Zt}else It.uniforms=Ft.getUniforms(C),C.onBeforeCompile(It,g),Zt=Ft.acquireProgram(It,Pt),qt.set(Pt,Zt),Y.uniforms=It.uniforms;let Lt=Y.uniforms;return(!C.isShaderMaterial&&!C.isRawShaderMaterial||C.clipping===!0)&&(Lt.clippingPlanes=at.uniform),Fh(C,It),Y.needsLights=vd(C),Y.lightsStateVersion=Mt,Y.needsLights&&(Lt.ambientLightColor.value=G.state.ambient,Lt.lightProbe.value=G.state.probe,Lt.directionalLights.value=G.state.directional,Lt.directionalLightShadows.value=G.state.directionalShadow,Lt.spotLights.value=G.state.spot,Lt.spotLightShadows.value=G.state.spotShadow,Lt.rectAreaLights.value=G.state.rectArea,Lt.ltc_1.value=G.state.rectAreaLTC1,Lt.ltc_2.value=G.state.rectAreaLTC2,Lt.pointLights.value=G.state.point,Lt.pointLightShadows.value=G.state.pointShadow,Lt.hemisphereLights.value=G.state.hemi,Lt.directionalShadowMap.value=G.state.directionalShadowMap,Lt.directionalShadowMatrix.value=G.state.directionalShadowMatrix,Lt.spotShadowMap.value=G.state.spotShadowMap,Lt.spotLightMatrix.value=G.state.spotLightMatrix,Lt.spotLightMap.value=G.state.spotLightMap,Lt.pointShadowMap.value=G.state.pointShadowMap,Lt.pointShadowMatrix.value=G.state.pointShadowMatrix),Y.currentProgram=Zt,Y.uniformsList=null,Zt}function Ch(C){if(C.uniformsList===null){let k=C.currentProgram.getUniforms();C.uniformsList=ws.seqWithValue(k.seq,C.uniforms)}return C.uniformsList}function Fh(C,k){let q=Tt.get(C);q.outputColorSpace=k.outputColorSpace,q.batching=k.batching,q.batchingColor=k.batchingColor,q.instancing=k.instancing,q.instancingColor=k.instancingColor,q.instancingMorph=k.instancingMorph,q.skinning=k.skinning,q.morphTargets=k.morphTargets,q.morphNormals=k.morphNormals,q.morphColors=k.morphColors,q.morphTargetsCount=k.morphTargetsCount,q.numClippingPlanes=k.numClippingPlanes,q.numIntersection=k.numClipIntersection,q.vertexAlphas=k.vertexAlphas,q.vertexTangents=k.vertexTangents,q.toneMapping=k.toneMapping}function yd(C,k,q,Y,G){k.isScene!==!0&&(k=ne),P.resetTextureUnits();let ht=k.fog,Mt=Y.isMeshStandardMaterial?k.environment:null,It=w===null?g.outputColorSpace:w.isXRRenderTarget===!0?w.texture.colorSpace:Us,Pt=(Y.isMeshStandardMaterial?W:A).get(Y.envMap||Mt),qt=Y.vertexColors===!0&&!!q.attributes.color&&q.attributes.color.itemSize===4,Zt=!!q.attributes.tangent&&(!!Y.normalMap||Y.anisotropy>0),Lt=!!q.morphAttributes.position,re=!!q.morphAttributes.normal,Te=!!q.morphAttributes.color,Ae=_i;Y.toneMapped&&(w===null||w.isXRRenderTarget===!0)&&(Ae=g.toneMapping);let tn=q.morphAttributes.position||q.morphAttributes.normal||q.morphAttributes.color,he=tn!==void 0?tn.length:0,Ut=Tt.get(Y),Hn=c.state.lights;if(st===!0&&(_t===!0||C!==E)){let pn=C===E&&Y.id===M;at.setState(Y,C,pn)}let ue=!1;Y.version===Ut.__version?(Ut.needsLights&&Ut.lightsStateVersion!==Hn.state.version||Ut.outputColorSpace!==It||G.isBatchedMesh&&Ut.batching===!1||!G.isBatchedMesh&&Ut.batching===!0||G.isBatchedMesh&&Ut.batchingColor===!0&&G.colorTexture===null||G.isBatchedMesh&&Ut.batchingColor===!1&&G.colorTexture!==null||G.isInstancedMesh&&Ut.instancing===!1||!G.isInstancedMesh&&Ut.instancing===!0||G.isSkinnedMesh&&Ut.skinning===!1||!G.isSkinnedMesh&&Ut.skinning===!0||G.isInstancedMesh&&Ut.instancingColor===!0&&G.instanceColor===null||G.isInstancedMesh&&Ut.instancingColor===!1&&G.instanceColor!==null||G.isInstancedMesh&&Ut.instancingMorph===!0&&G.morphTexture===null||G.isInstancedMesh&&Ut.instancingMorph===!1&&G.morphTexture!==null||Ut.envMap!==Pt||Y.fog===!0&&Ut.fog!==ht||Ut.numClippingPlanes!==void 0&&(Ut.numClippingPlanes!==at.numPlanes||Ut.numIntersection!==at.numIntersection)||Ut.vertexAlphas!==qt||Ut.vertexTangents!==Zt||Ut.morphTargets!==Lt||Ut.morphNormals!==re||Ut.morphColors!==Te||Ut.toneMapping!==Ae||Ut.morphTargetsCount!==he)&&(ue=!0):(ue=!0,Ut.__version=Y.version);let En=Ut.currentProgram;ue===!0&&(En=zr(Y,k,G));let es=!1,an=!1,Zs=!1,Re=En.getUniforms(),Cn=Ut.uniforms;if(ft.useProgram(En.program)&&(es=!0,an=!0,Zs=!0),Y.id!==M&&(M=Y.id,an=!0),es||E!==C){ft.buffers.depth.getReversed()?(rt.copy(C.projectionMatrix),up(rt),fp(rt),Re.setValue(L,"projectionMatrix",rt)):Re.setValue(L,"projectionMatrix",C.projectionMatrix),Re.setValue(L,"viewMatrix",C.matrixWorldInverse);let ai=Re.map.cameraPosition;ai!==void 0&&ai.setValue(L,Xt.setFromMatrixPosition(C.matrixWorld)),bt.logarithmicDepthBuffer&&Re.setValue(L,"logDepthBufFC",2/(Math.log(C.far+1)/Math.LN2)),(Y.isMeshPhongMaterial||Y.isMeshToonMaterial||Y.isMeshLambertMaterial||Y.isMeshBasicMaterial||Y.isMeshStandardMaterial||Y.isShaderMaterial)&&Re.setValue(L,"isOrthographic",C.isOrthographicCamera===!0),E!==C&&(E=C,an=!0,Zs=!0)}if(G.isSkinnedMesh){Re.setOptional(L,G,"bindMatrix"),Re.setOptional(L,G,"bindMatrixInverse");let pn=G.skeleton;pn&&(pn.boneTexture===null&&pn.computeBoneTexture(),Re.setValue(L,"boneTexture",pn.boneTexture,P))}G.isBatchedMesh&&(Re.setOptional(L,G,"batchingTexture"),Re.setValue(L,"batchingTexture",G._matricesTexture,P),Re.setOptional(L,G,"batchingIdTexture"),Re.setValue(L,"batchingIdTexture",G._indirectTexture,P),Re.setOptional(L,G,"batchingColorTexture"),G._colorsTexture!==null&&Re.setValue(L,"batchingColorTexture",G._colorsTexture,P));let $s=q.morphAttributes;if(($s.position!==void 0||$s.normal!==void 0||$s.color!==void 0)&&Vt.update(G,q,En),(an||Ut.receiveShadow!==G.receiveShadow)&&(Ut.receiveShadow=G.receiveShadow,Re.setValue(L,"receiveShadow",G.receiveShadow)),Y.isMeshGouraudMaterial&&Y.envMap!==null&&(Cn.envMap.value=Pt,Cn.flipEnvMap.value=Pt.isCubeTexture&&Pt.isRenderTargetTexture===!1?-1:1),Y.isMeshStandardMaterial&&Y.envMap===null&&k.environment!==null&&(Cn.envMapIntensity.value=k.environmentIntensity),an&&(Re.setValue(L,"toneMappingExposure",g.toneMappingExposure),Ut.needsLights&&_d(Cn,Zs),ht&&Y.fog===!0&&gt.refreshFogUniforms(Cn,ht),gt.refreshMaterialUniforms(Cn,Y,D,O,c.state.transmissionRenderTarget[C.id]),ws.upload(L,Ch(Ut),Cn,P)),Y.isShaderMaterial&&Y.uniformsNeedUpdate===!0&&(ws.upload(L,Ch(Ut),Cn,P),Y.uniformsNeedUpdate=!1),Y.isSpriteMaterial&&Re.setValue(L,"center",G.center),Re.setValue(L,"modelViewMatrix",G.modelViewMatrix),Re.setValue(L,"normalMatrix",G.normalMatrix),Re.setValue(L,"modelMatrix",G.matrixWorld),Y.isShaderMaterial||Y.isRawShaderMaterial){let pn=Y.uniformsGroups;for(let ai=0,li=pn.length;ai<li;ai++){let Ih=pn[ai];H.update(Ih,En),H.bind(Ih,En)}}return En}function _d(C,k){C.ambientLightColor.needsUpdate=k,C.lightProbe.needsUpdate=k,C.directionalLights.needsUpdate=k,C.directionalLightShadows.needsUpdate=k,C.pointLights.needsUpdate=k,C.pointLightShadows.needsUpdate=k,C.spotLights.needsUpdate=k,C.spotLightShadows.needsUpdate=k,C.rectAreaLights.needsUpdate=k,C.hemisphereLights.needsUpdate=k}function vd(C){return C.isMeshLambertMaterial||C.isMeshToonMaterial||C.isMeshPhongMaterial||C.isMeshStandardMaterial||C.isShadowMaterial||C.isShaderMaterial&&C.lights===!0}this.getActiveCubeFace=function(){return S},this.getActiveMipmapLevel=function(){return b},this.getRenderTarget=function(){return w},this.setRenderTargetTextures=function(C,k,q){Tt.get(C.texture).__webglTexture=k,Tt.get(C.depthTexture).__webglTexture=q;let Y=Tt.get(C);Y.__hasExternalTextures=!0,Y.__autoAllocateDepthBuffer=q===void 0,Y.__autoAllocateDepthBuffer||ot.has("WEBGL_multisampled_render_to_texture")===!0&&(console.warn("THREE.WebGLRenderer: Render-to-texture extension was disabled because an external texture was provided"),Y.__useRenderToTexture=!1)},this.setRenderTargetFramebuffer=function(C,k){let q=Tt.get(C);q.__webglFramebuffer=k,q.__useDefaultFramebuffer=k===void 0},this.setRenderTarget=function(C,k=0,q=0){w=C,S=k,b=q;let Y=!0,G=null,ht=!1,Mt=!1;if(C){let Pt=Tt.get(C);if(Pt.__useDefaultFramebuffer!==void 0)ft.bindFramebuffer(L.FRAMEBUFFER,null),Y=!1;else if(Pt.__webglFramebuffer===void 0)P.setupRenderTarget(C);else if(Pt.__hasExternalTextures)P.rebindTextures(C,Tt.get(C.texture).__webglTexture,Tt.get(C.depthTexture).__webglTexture);else if(C.depthBuffer){let Lt=C.depthTexture;if(Pt.__boundDepthTexture!==Lt){if(Lt!==null&&Tt.has(Lt)&&(C.width!==Lt.image.width||C.height!==Lt.image.height))throw new Error("WebGLRenderTarget: Attached DepthTexture is initialized to the incorrect size.");P.setupDepthRenderbuffer(C)}}let qt=C.texture;(qt.isData3DTexture||qt.isDataArrayTexture||qt.isCompressedArrayTexture)&&(Mt=!0);let Zt=Tt.get(C).__webglFramebuffer;C.isWebGLCubeRenderTarget?(Array.isArray(Zt[k])?G=Zt[k][q]:G=Zt[k],ht=!0):C.samples>0&&P.useMultisampledRTT(C)===!1?G=Tt.get(C).__webglMultisampledFramebuffer:Array.isArray(Zt)?G=Zt[q]:G=Zt,F.copy(C.viewport),U.copy(C.scissor),R=C.scissorTest}else F.copy(tt).multiplyScalar(D).floor(),U.copy(ut).multiplyScalar(D).floor(),R=Ct;if(ft.bindFramebuffer(L.FRAMEBUFFER,G)&&Y&&ft.drawBuffers(C,G),ft.viewport(F),ft.scissor(U),ft.setScissorTest(R),ht){let Pt=Tt.get(C.texture);L.framebufferTexture2D(L.FRAMEBUFFER,L.COLOR_ATTACHMENT0,L.TEXTURE_CUBE_MAP_POSITIVE_X+k,Pt.__webglTexture,q)}else if(Mt){let Pt=Tt.get(C.texture),qt=k||0;L.framebufferTextureLayer(L.FRAMEBUFFER,L.COLOR_ATTACHMENT0,Pt.__webglTexture,q||0,qt)}M=-1},this.readRenderTargetPixels=function(C,k,q,Y,G,ht,Mt){if(!(C&&C.isWebGLRenderTarget)){console.error("THREE.WebGLRenderer.readRenderTargetPixels: renderTarget is not THREE.WebGLRenderTarget.");return}let It=Tt.get(C).__webglFramebuffer;if(C.isWebGLCubeRenderTarget&&Mt!==void 0&&(It=It[Mt]),It){ft.bindFramebuffer(L.FRAMEBUFFER,It);try{let Pt=C.texture,qt=Pt.format,Zt=Pt.type;if(!bt.textureFormatReadable(qt)){console.error("THREE.WebGLRenderer.readRenderTargetPixels: renderTarget is not in RGBA or implementation defined format.");return}if(!bt.textureTypeReadable(Zt)){console.error("THREE.WebGLRenderer.readRenderTargetPixels: renderTarget is not in UnsignedByteType or implementation defined type.");return}k>=0&&k<=C.width-Y&&q>=0&&q<=C.height-G&&L.readPixels(k,q,Y,G,Kt.convert(qt),Kt.convert(Zt),ht)}finally{let Pt=w!==null?Tt.get(w).__webglFramebuffer:null;ft.bindFramebuffer(L.FRAMEBUFFER,Pt)}}},this.readRenderTargetPixelsAsync=async function(C,k,q,Y,G,ht,Mt){if(!(C&&C.isWebGLRenderTarget))throw new Error("THREE.WebGLRenderer.readRenderTargetPixels: renderTarget is not THREE.WebGLRenderTarget.");let It=Tt.get(C).__webglFramebuffer;if(C.isWebGLCubeRenderTarget&&Mt!==void 0&&(It=It[Mt]),It){let Pt=C.texture,qt=Pt.format,Zt=Pt.type;if(!bt.textureFormatReadable(qt))throw new Error("THREE.WebGLRenderer.readRenderTargetPixelsAsync: renderTarget is not in RGBA or implementation defined format.");if(!bt.textureTypeReadable(Zt))throw new Error("THREE.WebGLRenderer.readRenderTargetPixelsAsync: renderTarget is not in UnsignedByteType or implementation defined type.");if(k>=0&&k<=C.width-Y&&q>=0&&q<=C.height-G){ft.bindFramebuffer(L.FRAMEBUFFER,It);let Lt=L.createBuffer();L.bindBuffer(L.PIXEL_PACK_BUFFER,Lt),L.bufferData(L.PIXEL_PACK_BUFFER,ht.byteLength,L.STREAM_READ),L.readPixels(k,q,Y,G,Kt.convert(qt),Kt.convert(Zt),0);let re=w!==null?Tt.get(w).__webglFramebuffer:null;ft.bindFramebuffer(L.FRAMEBUFFER,re);let Te=L.fenceSync(L.SYNC_GPU_COMMANDS_COMPLETE,0);return L.flush(),await hp(L,Te,4),L.bindBuffer(L.PIXEL_PACK_BUFFER,Lt),L.getBufferSubData(L.PIXEL_PACK_BUFFER,0,ht),L.deleteBuffer(Lt),L.deleteSync(Te),ht}else throw new Error("THREE.WebGLRenderer.readRenderTargetPixelsAsync: requested read bounds are out of range.")}},this.copyFramebufferToTexture=function(C,k=null,q=0){C.isTexture!==!0&&(or("WebGLRenderer: copyFramebufferToTexture function signature has changed."),k=arguments[0]||null,C=arguments[1]);let Y=Math.pow(2,-q),G=Math.floor(C.image.width*Y),ht=Math.floor(C.image.height*Y),Mt=k!==null?k.x:0,It=k!==null?k.y:0;P.setTexture2D(C,0),L.copyTexSubImage2D(L.TEXTURE_2D,q,0,0,Mt,It,G,ht),ft.unbindTexture()},this.copyTextureToTexture=function(C,k,q=null,Y=null,G=0){C.isTexture!==!0&&(or("WebGLRenderer: copyTextureToTexture function signature has changed."),Y=arguments[0]||null,C=arguments[1],k=arguments[2],G=arguments[3]||0,q=null);let ht,Mt,It,Pt,qt,Zt,Lt,re,Te,Ae=C.isCompressedTexture?C.mipmaps[G]:C.image;q!==null?(ht=q.max.x-q.min.x,Mt=q.max.y-q.min.y,It=q.isBox3?q.max.z-q.min.z:1,Pt=q.min.x,qt=q.min.y,Zt=q.isBox3?q.min.z:0):(ht=Ae.width,Mt=Ae.height,It=Ae.depth||1,Pt=0,qt=0,Zt=0),Y!==null?(Lt=Y.x,re=Y.y,Te=Y.z):(Lt=0,re=0,Te=0);let tn=Kt.convert(k.format),he=Kt.convert(k.type),Ut;k.isData3DTexture?(P.setTexture3D(k,0),Ut=L.TEXTURE_3D):k.isDataArrayTexture||k.isCompressedArrayTexture?(P.setTexture2DArray(k,0),Ut=L.TEXTURE_2D_ARRAY):(P.setTexture2D(k,0),Ut=L.TEXTURE_2D),L.pixelStorei(L.UNPACK_FLIP_Y_WEBGL,k.flipY),L.pixelStorei(L.UNPACK_PREMULTIPLY_ALPHA_WEBGL,k.premultiplyAlpha),L.pixelStorei(L.UNPACK_ALIGNMENT,k.unpackAlignment);let Hn=L.getParameter(L.UNPACK_ROW_LENGTH),ue=L.getParameter(L.UNPACK_IMAGE_HEIGHT),En=L.getParameter(L.UNPACK_SKIP_PIXELS),es=L.getParameter(L.UNPACK_SKIP_ROWS),an=L.getParameter(L.UNPACK_SKIP_IMAGES);L.pixelStorei(L.UNPACK_ROW_LENGTH,Ae.width),L.pixelStorei(L.UNPACK_IMAGE_HEIGHT,Ae.height),L.pixelStorei(L.UNPACK_SKIP_PIXELS,Pt),L.pixelStorei(L.UNPACK_SKIP_ROWS,qt),L.pixelStorei(L.UNPACK_SKIP_IMAGES,Zt);let Zs=C.isDataArrayTexture||C.isData3DTexture,Re=k.isDataArrayTexture||k.isData3DTexture;if(C.isRenderTargetTexture||C.isDepthTexture){let Cn=Tt.get(C),$s=Tt.get(k),pn=Tt.get(Cn.__renderTarget),ai=Tt.get($s.__renderTarget);ft.bindFramebuffer(L.READ_FRAMEBUFFER,pn.__webglFramebuffer),ft.bindFramebuffer(L.DRAW_FRAMEBUFFER,ai.__webglFramebuffer);for(let li=0;li<It;li++)Zs&&L.framebufferTextureLayer(L.READ_FRAMEBUFFER,L.COLOR_ATTACHMENT0,Tt.get(C).__webglTexture,G,Zt+li),C.isDepthTexture?(Re&&L.framebufferTextureLayer(L.DRAW_FRAMEBUFFER,L.COLOR_ATTACHMENT0,Tt.get(k).__webglTexture,G,Te+li),L.blitFramebuffer(Pt,qt,ht,Mt,Lt,re,ht,Mt,L.DEPTH_BUFFER_BIT,L.NEAREST)):Re?L.copyTexSubImage3D(Ut,G,Lt,re,Te+li,Pt,qt,ht,Mt):L.copyTexSubImage2D(Ut,G,Lt,re,Te+li,Pt,qt,ht,Mt);ft.bindFramebuffer(L.READ_FRAMEBUFFER,null),ft.bindFramebuffer(L.DRAW_FRAMEBUFFER,null)}else Re?C.isDataTexture||C.isData3DTexture?L.texSubImage3D(Ut,G,Lt,re,Te,ht,Mt,It,tn,he,Ae.data):k.isCompressedArrayTexture?L.compressedTexSubImage3D(Ut,G,Lt,re,Te,ht,Mt,It,tn,Ae.data):L.texSubImage3D(Ut,G,Lt,re,Te,ht,Mt,It,tn,he,Ae):C.isDataTexture?L.texSubImage2D(L.TEXTURE_2D,G,Lt,re,ht,Mt,tn,he,Ae.data):C.isCompressedTexture?L.compressedTexSubImage2D(L.TEXTURE_2D,G,Lt,re,Ae.width,Ae.height,tn,Ae.data):L.texSubImage2D(L.TEXTURE_2D,G,Lt,re,ht,Mt,tn,he,Ae);L.pixelStorei(L.UNPACK_ROW_LENGTH,Hn),L.pixelStorei(L.UNPACK_IMAGE_HEIGHT,ue),L.pixelStorei(L.UNPACK_SKIP_PIXELS,En),L.pixelStorei(L.UNPACK_SKIP_ROWS,es),L.pixelStorei(L.UNPACK_SKIP_IMAGES,an),G===0&&k.generateMipmaps&&L.generateMipmap(Ut),ft.unbindTexture()},this.copyTextureToTexture3D=function(C,k,q=null,Y=null,G=0){return C.isTexture!==!0&&(or("WebGLRenderer: copyTextureToTexture3D function signature has changed."),q=arguments[0]||null,Y=arguments[1]||null,C=arguments[2],k=arguments[3],G=arguments[4]||0),or('WebGLRenderer: copyTextureToTexture3D function has been deprecated. Use "copyTextureToTexture" instead.'),this.copyTextureToTexture(C,k,q,Y,G)},this.initRenderTarget=function(C){Tt.get(C).__webglFramebuffer===void 0&&P.setupRenderTarget(C)},this.initTexture=function(C){C.isCubeTexture?P.setTextureCube(C,0):C.isData3DTexture?P.setTexture3D(C,0):C.isDataArrayTexture||C.isCompressedArrayTexture?P.setTexture2DArray(C,0):P.setTexture2D(C,0),ft.unbindTexture()},this.resetState=function(){S=0,b=0,w=null,ft.reset(),Se.reset()},typeof __THREE_DEVTOOLS__<"u"&&__THREE_DEVTOOLS__.dispatchEvent(new CustomEvent("observe",{detail:this}))}get coordinateSystem(){return Zn}get outputColorSpace(){return this._outputColorSpace}set outputColorSpace(t){this._outputColorSpace=t;let e=this.getContext();e.drawingBufferColorspace=ie._getDrawingBufferColorSpace(t),e.unpackColorSpace=ie._getUnpackColorSpace()}};var Bo=class i{constructor(t,e=1,n=1e3){this.isFog=!0,this.name="",this.color=new Jt(t),this.near=e,this.far=n}clone(){return new i(this.color,this.near,this.far)}toJSON(){return{type:"Fog",name:this.name,color:this.color.getHex(),near:this.near,far:this.far}}},gr=class extends We{constructor(){super(),this.isScene=!0,this.type="Scene",this.background=null,this.environment=null,this.fog=null,this.backgroundBlurriness=0,this.backgroundIntensity=1,this.backgroundRotation=new sn,this.environmentIntensity=1,this.environmentRotation=new sn,this.overrideMaterial=null,typeof __THREE_DEVTOOLS__<"u"&&__THREE_DEVTOOLS__.dispatchEvent(new CustomEvent("observe",{detail:this}))}copy(t,e){return super.copy(t,e),t.background!==null&&(this.background=t.background.clone()),t.environment!==null&&(this.environment=t.environment.clone()),t.fog!==null&&(this.fog=t.fog.clone()),this.backgroundBlurriness=t.backgroundBlurriness,this.backgroundIntensity=t.backgroundIntensity,this.backgroundRotation.copy(t.backgroundRotation),this.environmentIntensity=t.environmentIntensity,this.environmentRotation.copy(t.environmentRotation),t.overrideMaterial!==null&&(this.overrideMaterial=t.overrideMaterial.clone()),this.matrixAutoUpdate=t.matrixAutoUpdate,this}toJSON(t){let e=super.toJSON(t);return this.fog!==null&&(e.object.fog=this.fog.toJSON()),this.backgroundBlurriness>0&&(e.object.backgroundBlurriness=this.backgroundBlurriness),this.backgroundIntensity!==1&&(e.object.backgroundIntensity=this.backgroundIntensity),e.object.backgroundRotation=this.backgroundRotation.toArray(),this.environmentIntensity!==1&&(e.object.environmentIntensity=this.environmentIntensity),e.object.environmentRotation=this.environmentRotation.toArray(),e}},zo=class{constructor(t,e){this.isInterleavedBuffer=!0,this.array=t,this.stride=e,this.count=t!==void 0?t.length/e:0,this.usage=Xl,this.updateRanges=[],this.version=0,this.uuid=$n()}onUploadCallback(){}set needsUpdate(t){t===!0&&this.version++}setUsage(t){return this.usage=t,this}addUpdateRange(t,e){this.updateRanges.push({start:t,count:e})}clearUpdateRanges(){this.updateRanges.length=0}copy(t){return this.array=new t.array.constructor(t.array),this.count=t.count,this.stride=t.stride,this.usage=t.usage,this}copyAt(t,e,n){t*=this.stride,n*=e.stride;for(let s=0,r=this.stride;s<r;s++)this.array[t+s]=e.array[n+s];return this}set(t,e=0){return this.array.set(t,e),this}clone(t){t.arrayBuffers===void 0&&(t.arrayBuffers={}),this.array.buffer._uuid===void 0&&(this.array.buffer._uuid=$n()),t.arrayBuffers[this.array.buffer._uuid]===void 0&&(t.arrayBuffers[this.array.buffer._uuid]=this.array.slice(0).buffer);let e=new this.array.constructor(t.arrayBuffers[this.array.buffer._uuid]),n=new this.constructor(e,this.stride);return n.setUsage(this.usage),n}onUpload(t){return this.onUploadCallback=t,this}toJSON(t){return t.arrayBuffers===void 0&&(t.arrayBuffers={}),this.array.buffer._uuid===void 0&&(this.array.buffer._uuid=$n()),t.arrayBuffers[this.array.buffer._uuid]===void 0&&(t.arrayBuffers[this.array.buffer._uuid]=Array.from(new Uint32Array(this.array.buffer))),{uuid:this.uuid,buffer:this.array.buffer._uuid,type:this.array.constructor.name,stride:this.stride}}},Ke=new B,xr=class i{constructor(t,e,n,s=!1){this.isInterleavedBufferAttribute=!0,this.name="",this.data=t,this.itemSize=e,this.offset=n,this.normalized=s}get count(){return this.data.count}get array(){return this.data.array}set needsUpdate(t){this.data.needsUpdate=t}applyMatrix4(t){for(let e=0,n=this.data.count;e<n;e++)Ke.fromBufferAttribute(this,e),Ke.applyMatrix4(t),this.setXYZ(e,Ke.x,Ke.y,Ke.z);return this}applyNormalMatrix(t){for(let e=0,n=this.count;e<n;e++)Ke.fromBufferAttribute(this,e),Ke.applyNormalMatrix(t),this.setXYZ(e,Ke.x,Ke.y,Ke.z);return this}transformDirection(t){for(let e=0,n=this.count;e<n;e++)Ke.fromBufferAttribute(this,e),Ke.transformDirection(t),this.setXYZ(e,Ke.x,Ke.y,Ke.z);return this}getComponent(t,e){let n=this.array[t*this.data.stride+this.offset+e];return this.normalized&&(n=In(n,this.array)),n}setComponent(t,e,n){return this.normalized&&(n=_e(n,this.array)),this.data.array[t*this.data.stride+this.offset+e]=n,this}setX(t,e){return this.normalized&&(e=_e(e,this.array)),this.data.array[t*this.data.stride+this.offset]=e,this}setY(t,e){return this.normalized&&(e=_e(e,this.array)),this.data.array[t*this.data.stride+this.offset+1]=e,this}setZ(t,e){return this.normalized&&(e=_e(e,this.array)),this.data.array[t*this.data.stride+this.offset+2]=e,this}setW(t,e){return this.normalized&&(e=_e(e,this.array)),this.data.array[t*this.data.stride+this.offset+3]=e,this}getX(t){let e=this.data.array[t*this.data.stride+this.offset];return this.normalized&&(e=In(e,this.array)),e}getY(t){let e=this.data.array[t*this.data.stride+this.offset+1];return this.normalized&&(e=In(e,this.array)),e}getZ(t){let e=this.data.array[t*this.data.stride+this.offset+2];return this.normalized&&(e=In(e,this.array)),e}getW(t){let e=this.data.array[t*this.data.stride+this.offset+3];return this.normalized&&(e=In(e,this.array)),e}setXY(t,e,n){return t=t*this.data.stride+this.offset,this.normalized&&(e=_e(e,this.array),n=_e(n,this.array)),this.data.array[t+0]=e,this.data.array[t+1]=n,this}setXYZ(t,e,n,s){return t=t*this.data.stride+this.offset,this.normalized&&(e=_e(e,this.array),n=_e(n,this.array),s=_e(s,this.array)),this.data.array[t+0]=e,this.data.array[t+1]=n,this.data.array[t+2]=s,this}setXYZW(t,e,n,s,r){return t=t*this.data.stride+this.offset,this.normalized&&(e=_e(e,this.array),n=_e(n,this.array),s=_e(s,this.array),r=_e(r,this.array)),this.data.array[t+0]=e,this.data.array[t+1]=n,this.data.array[t+2]=s,this.data.array[t+3]=r,this}clone(t){if(t===void 0){console.log("THREE.InterleavedBufferAttribute.clone(): Cloning an interleaved buffer attribute will de-interleave buffer data.");let e=[];for(let n=0;n<this.count;n++){let s=n*this.data.stride+this.offset;for(let r=0;r<this.itemSize;r++)e.push(this.data.array[s+r])}return new we(new this.array.constructor(e),this.itemSize,this.normalized)}else return t.interleavedBuffers===void 0&&(t.interleavedBuffers={}),t.interleavedBuffers[this.data.uuid]===void 0&&(t.interleavedBuffers[this.data.uuid]=this.data.clone(t)),new i(t.interleavedBuffers[this.data.uuid],this.itemSize,this.offset,this.normalized)}toJSON(t){if(t===void 0){console.log("THREE.InterleavedBufferAttribute.toJSON(): Serializing an interleaved buffer attribute will de-interleave buffer data.");let e=[];for(let n=0;n<this.count;n++){let s=n*this.data.stride+this.offset;for(let r=0;r<this.itemSize;r++)e.push(this.data.array[s+r])}return{itemSize:this.itemSize,type:this.array.constructor.name,array:e,normalized:this.normalized}}else return t.interleavedBuffers===void 0&&(t.interleavedBuffers={}),t.interleavedBuffers[this.data.uuid]===void 0&&(t.interleavedBuffers[this.data.uuid]=this.data.toJSON(t)),{isInterleavedBufferAttribute:!0,itemSize:this.itemSize,data:this.data.uuid,offset:this.offset,normalized:this.normalized}}},ei=class extends Un{static get type(){return"SpriteMaterial"}constructor(t){super(),this.isSpriteMaterial=!0,this.color=new Jt(16777215),this.map=null,this.alphaMap=null,this.rotation=0,this.sizeAttenuation=!0,this.transparent=!0,this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.alphaMap=t.alphaMap,this.rotation=t.rotation,this.sizeAttenuation=t.sizeAttenuation,this.fog=t.fog,this}},gs,tr=new B,xs=new B,ys=new B,_s=new lt,er=new lt,pf=new ee,oo=new B,nr=new B,ao=new B,Fu=new lt,tl=new lt,Iu=new lt,Mi=class extends We{constructor(t=new ei){if(super(),this.isSprite=!0,this.type="Sprite",gs===void 0){gs=new de;let e=new Float32Array([-.5,-.5,0,0,0,.5,-.5,0,1,0,.5,.5,0,1,1,-.5,.5,0,0,1]),n=new zo(e,5);gs.setIndex([0,1,2,0,2,3]),gs.setAttribute("position",new xr(n,3,0,!1)),gs.setAttribute("uv",new xr(n,2,3,!1))}this.geometry=gs,this.material=t,this.center=new lt(.5,.5)}raycast(t,e){t.camera===null&&console.error('THREE.Sprite: "Raycaster.camera" needs to be set in order to raycast against sprites.'),xs.setFromMatrixScale(this.matrixWorld),pf.copy(t.camera.matrixWorld),this.modelViewMatrix.multiplyMatrices(t.camera.matrixWorldInverse,this.matrixWorld),ys.setFromMatrixPosition(this.modelViewMatrix),t.camera.isPerspectiveCamera&&this.material.sizeAttenuation===!1&&xs.multiplyScalar(-ys.z);let n=this.material.rotation,s,r;n!==0&&(r=Math.cos(n),s=Math.sin(n));let o=this.center;lo(oo.set(-.5,-.5,0),ys,o,xs,s,r),lo(nr.set(.5,-.5,0),ys,o,xs,s,r),lo(ao.set(.5,.5,0),ys,o,xs,s,r),Fu.set(0,0),tl.set(1,0),Iu.set(1,1);let a=t.ray.intersectTriangle(oo,nr,ao,!1,tr);if(a===null&&(lo(nr.set(-.5,.5,0),ys,o,xs,s,r),tl.set(0,1),a=t.ray.intersectTriangle(oo,ao,nr,!1,tr),a===null))return;let l=t.ray.origin.distanceTo(tr);l<t.near||l>t.far||e.push({distance:l,point:tr.clone(),uv:gi.getInterpolation(tr,oo,nr,ao,Fu,tl,Iu,new lt),face:null,object:this})}copy(t,e){return super.copy(t,e),t.center!==void 0&&this.center.copy(t.center),this.material=t.material,this}};function lo(i,t,e,n,s,r){_s.subVectors(i,e).addScalar(.5).multiply(n),s!==void 0?(er.x=r*_s.x-s*_s.y,er.y=s*_s.x+r*_s.y):er.copy(_s),i.copy(t),i.x+=er.x,i.y+=er.y,i.applyMatrix4(pf)}var cc=class extends nn{constructor(t=null,e=1,n=1,s,r,o,a,l,h=hn,u=hn,m,x){super(null,o,a,l,h,u,s,r,m,x),this.isDataTexture=!0,this.image={data:t,width:e,height:n},this.generateMipmaps=!1,this.flipY=!1,this.unpackAlignment=1}};var yr=class extends we{constructor(t,e,n,s=1){super(t,e,n),this.isInstancedBufferAttribute=!0,this.meshPerAttribute=s}copy(t){return super.copy(t),this.meshPerAttribute=t.meshPerAttribute,this}toJSON(){let t=super.toJSON();return t.meshPerAttribute=this.meshPerAttribute,t.isInstancedBufferAttribute=!0,t}},vs=new ee,Pu=new ee,co=[],Lu=new Qn,oy=new ee,ir=new Z,sr=new ti,be=class extends Z{constructor(t,e,n){super(t,e),this.isInstancedMesh=!0,this.instanceMatrix=new yr(new Float32Array(n*16),16),this.instanceColor=null,this.morphTexture=null,this.count=n,this.boundingBox=null,this.boundingSphere=null;for(let s=0;s<n;s++)this.setMatrixAt(s,oy)}computeBoundingBox(){let t=this.geometry,e=this.count;this.boundingBox===null&&(this.boundingBox=new Qn),t.boundingBox===null&&t.computeBoundingBox(),this.boundingBox.makeEmpty();for(let n=0;n<e;n++)this.getMatrixAt(n,vs),Lu.copy(t.boundingBox).applyMatrix4(vs),this.boundingBox.union(Lu)}computeBoundingSphere(){let t=this.geometry,e=this.count;this.boundingSphere===null&&(this.boundingSphere=new ti),t.boundingSphere===null&&t.computeBoundingSphere(),this.boundingSphere.makeEmpty();for(let n=0;n<e;n++)this.getMatrixAt(n,vs),sr.copy(t.boundingSphere).applyMatrix4(vs),this.boundingSphere.union(sr)}copy(t,e){return super.copy(t,e),this.instanceMatrix.copy(t.instanceMatrix),t.morphTexture!==null&&(this.morphTexture=t.morphTexture.clone()),t.instanceColor!==null&&(this.instanceColor=t.instanceColor.clone()),this.count=t.count,t.boundingBox!==null&&(this.boundingBox=t.boundingBox.clone()),t.boundingSphere!==null&&(this.boundingSphere=t.boundingSphere.clone()),this}getColorAt(t,e){e.fromArray(this.instanceColor.array,t*3)}getMatrixAt(t,e){e.fromArray(this.instanceMatrix.array,t*16)}getMorphAt(t,e){let n=e.morphTargetInfluences,s=this.morphTexture.source.data.data,r=n.length+1,o=t*r+1;for(let a=0;a<n.length;a++)n[a]=s[o+a]}raycast(t,e){let n=this.matrixWorld,s=this.count;if(ir.geometry=this.geometry,ir.material=this.material,ir.material!==void 0&&(this.boundingSphere===null&&this.computeBoundingSphere(),sr.copy(this.boundingSphere),sr.applyMatrix4(n),t.ray.intersectsSphere(sr)!==!1))for(let r=0;r<s;r++){this.getMatrixAt(r,vs),Pu.multiplyMatrices(n,vs),ir.matrixWorld=Pu,ir.raycast(t,co);for(let o=0,a=co.length;o<a;o++){let l=co[o];l.instanceId=r,l.object=this,e.push(l)}co.length=0}}setColorAt(t,e){this.instanceColor===null&&(this.instanceColor=new yr(new Float32Array(this.instanceMatrix.count*3).fill(1),3)),e.toArray(this.instanceColor.array,t*3)}setMatrixAt(t,e){e.toArray(this.instanceMatrix.array,t*16)}setMorphAt(t,e){let n=e.morphTargetInfluences,s=n.length+1;this.morphTexture===null&&(this.morphTexture=new cc(new Float32Array(s*this.count),s,this.count,Oc,Ln));let r=this.morphTexture.source.data.data,o=0;for(let h=0;h<n.length;h++)o+=n[h];let a=this.geometry.morphTargetsRelative?1:1-o,l=s*t;r[l]=a,r.set(n,l+1)}updateMorphTargets(){}dispose(){return this.dispatchEvent({type:"dispose"}),this.morphTexture!==null&&(this.morphTexture.dispose(),this.morphTexture=null),this}};var _r=class extends Un{static get type(){return"LineBasicMaterial"}constructor(t){super(),this.isLineBasicMaterial=!0,this.color=new Jt(16777215),this.map=null,this.linewidth=1,this.linecap="round",this.linejoin="round",this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.linewidth=t.linewidth,this.linecap=t.linecap,this.linejoin=t.linejoin,this.fog=t.fog,this}},Oo=new B,Ho=new B,Du=new ee,rr=new dr,ho=new ti,el=new B,Uu=new B,hc=class extends We{constructor(t=new de,e=new _r){super(),this.isLine=!0,this.type="Line",this.geometry=t,this.material=e,this.updateMorphTargets()}copy(t,e){return super.copy(t,e),this.material=Array.isArray(t.material)?t.material.slice():t.material,this.geometry=t.geometry,this}computeLineDistances(){let t=this.geometry;if(t.index===null){let e=t.attributes.position,n=[0];for(let s=1,r=e.count;s<r;s++)Oo.fromBufferAttribute(e,s-1),Ho.fromBufferAttribute(e,s),n[s]=n[s-1],n[s]+=Oo.distanceTo(Ho);t.setAttribute("lineDistance",new Wt(n,1))}else console.warn("THREE.Line.computeLineDistances(): Computation only possible with non-indexed BufferGeometry.");return this}raycast(t,e){let n=this.geometry,s=this.matrixWorld,r=t.params.Line.threshold,o=n.drawRange;if(n.boundingSphere===null&&n.computeBoundingSphere(),ho.copy(n.boundingSphere),ho.applyMatrix4(s),ho.radius+=r,t.ray.intersectsSphere(ho)===!1)return;Du.copy(s).invert(),rr.copy(t.ray).applyMatrix4(Du);let a=r/((this.scale.x+this.scale.y+this.scale.z)/3),l=a*a,h=this.isLineSegments?2:1,u=n.index,x=n.attributes.position;if(u!==null){let f=Math.max(0,o.start),v=Math.min(u.count,o.start+o.count);for(let d=f,y=v-1;d<y;d+=h){let c=u.getX(d),_=u.getX(d+1),p=uo(this,t,rr,l,c,_);p&&e.push(p)}if(this.isLineLoop){let d=u.getX(v-1),y=u.getX(f),c=uo(this,t,rr,l,d,y);c&&e.push(c)}}else{let f=Math.max(0,o.start),v=Math.min(x.count,o.start+o.count);for(let d=f,y=v-1;d<y;d+=h){let c=uo(this,t,rr,l,d,d+1);c&&e.push(c)}if(this.isLineLoop){let d=uo(this,t,rr,l,v-1,f);d&&e.push(d)}}}updateMorphTargets(){let e=this.geometry.morphAttributes,n=Object.keys(e);if(n.length>0){let s=e[n[0]];if(s!==void 0){this.morphTargetInfluences=[],this.morphTargetDictionary={};for(let r=0,o=s.length;r<o;r++){let a=s[r].name||String(r);this.morphTargetInfluences.push(0),this.morphTargetDictionary[a]=r}}}}};function uo(i,t,e,n,s,r){let o=i.geometry.attributes.position;if(Oo.fromBufferAttribute(o,s),Ho.fromBufferAttribute(o,r),e.distanceSqToSegment(Oo,Ho,el,Uu)>n)return;el.applyMatrix4(i.matrixWorld);let l=t.ray.origin.distanceTo(el);if(!(l<t.near||l>t.far))return{distance:l,point:Uu.clone().applyMatrix4(i.matrixWorld),index:s,face:null,faceIndex:null,barycoord:null,object:i}}var Nu=new B,Bu=new B,ko=class extends hc{constructor(t,e){super(t,e),this.isLineSegments=!0,this.type="LineSegments"}computeLineDistances(){let t=this.geometry;if(t.index===null){let e=t.attributes.position,n=[];for(let s=0,r=e.count;s<r;s+=2)Nu.fromBufferAttribute(e,s),Bu.fromBufferAttribute(e,s+1),n[s]=s===0?0:n[s-1],n[s+1]=n[s]+Nu.distanceTo(Bu);t.setAttribute("lineDistance",new Wt(n,1))}else console.warn("THREE.LineSegments.computeLineDistances(): Computation only possible with non-indexed BufferGeometry.");return this}};var uc=class extends Un{static get type(){return"PointsMaterial"}constructor(t){super(),this.isPointsMaterial=!0,this.color=new Jt(16777215),this.map=null,this.alphaMap=null,this.size=1,this.sizeAttenuation=!0,this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.alphaMap=t.alphaMap,this.size=t.size,this.sizeAttenuation=t.sizeAttenuation,this.fog=t.fog,this}},zu=new ee,fc=new dr,fo=new ti,po=new B,Go=class extends We{constructor(t=new de,e=new uc){super(),this.isPoints=!0,this.type="Points",this.geometry=t,this.material=e,this.updateMorphTargets()}copy(t,e){return super.copy(t,e),this.material=Array.isArray(t.material)?t.material.slice():t.material,this.geometry=t.geometry,this}raycast(t,e){let n=this.geometry,s=this.matrixWorld,r=t.params.Points.threshold,o=n.drawRange;if(n.boundingSphere===null&&n.computeBoundingSphere(),fo.copy(n.boundingSphere),fo.applyMatrix4(s),fo.radius+=r,t.ray.intersectsSphere(fo)===!1)return;zu.copy(s).invert(),fc.copy(t.ray).applyMatrix4(zu);let a=r/((this.scale.x+this.scale.y+this.scale.z)/3),l=a*a,h=n.index,m=n.attributes.position;if(h!==null){let x=Math.max(0,o.start),f=Math.min(h.count,o.start+o.count);for(let v=x,d=f;v<d;v++){let y=h.getX(v);po.fromBufferAttribute(m,y),Ou(po,y,l,s,t,e,this)}}else{let x=Math.max(0,o.start),f=Math.min(m.count,o.start+o.count);for(let v=x,d=f;v<d;v++)po.fromBufferAttribute(m,v),Ou(po,v,l,s,t,e,this)}}updateMorphTargets(){let e=this.geometry.morphAttributes,n=Object.keys(e);if(n.length>0){let s=e[n[0]];if(s!==void 0){this.morphTargetInfluences=[],this.morphTargetDictionary={};for(let r=0,o=s.length;r<o;r++){let a=s[r].name||String(r);this.morphTargetInfluences.push(0),this.morphTargetDictionary[a]=r}}}}};function Ou(i,t,e,n,s,r,o){let a=fc.distanceSqToPoint(i);if(a<e){let l=new B;fc.closestPointToPoint(i,l),l.applyMatrix4(n);let h=s.ray.origin.distanceTo(l);if(h<s.near||h>s.far)return;r.push({distance:h,distanceToRay:Math.sqrt(a),point:l,index:t,face:null,faceIndex:null,barycoord:null,object:o})}}var Gi=class extends nn{constructor(t,e,n,s,r,o,a,l,h){super(t,e,n,s,r,o,a,l,h),this.isCanvasTexture=!0,this.needsUpdate=!0}},yn=class{constructor(){this.type="Curve",this.arcLengthDivisions=200}getPoint(){return console.warn("THREE.Curve: .getPoint() not implemented."),null}getPointAt(t,e){let n=this.getUtoTmapping(t);return this.getPoint(n,e)}getPoints(t=5){let e=[];for(let n=0;n<=t;n++)e.push(this.getPoint(n/t));return e}getSpacedPoints(t=5){let e=[];for(let n=0;n<=t;n++)e.push(this.getPointAt(n/t));return e}getLength(){let t=this.getLengths();return t[t.length-1]}getLengths(t=this.arcLengthDivisions){if(this.cacheArcLengths&&this.cacheArcLengths.length===t+1&&!this.needsUpdate)return this.cacheArcLengths;this.needsUpdate=!1;let e=[],n,s=this.getPoint(0),r=0;e.push(0);for(let o=1;o<=t;o++)n=this.getPoint(o/t),r+=n.distanceTo(s),e.push(r),s=n;return this.cacheArcLengths=e,e}updateArcLengths(){this.needsUpdate=!0,this.getLengths()}getUtoTmapping(t,e){let n=this.getLengths(),s=0,r=n.length,o;e?o=e:o=t*n[r-1];let a=0,l=r-1,h;for(;a<=l;)if(s=Math.floor(a+(l-a)/2),h=n[s]-o,h<0)a=s+1;else if(h>0)l=s-1;else{l=s;break}if(s=l,n[s]===o)return s/(r-1);let u=n[s],x=n[s+1]-u,f=(o-u)/x;return(s+f)/(r-1)}getTangent(t,e){let s=t-1e-4,r=t+1e-4;s<0&&(s=0),r>1&&(r=1);let o=this.getPoint(s),a=this.getPoint(r),l=e||(o.isVector2?new lt:new B);return l.copy(a).sub(o).normalize(),l}getTangentAt(t,e){let n=this.getUtoTmapping(t);return this.getTangent(n,e)}computeFrenetFrames(t,e){let n=new B,s=[],r=[],o=[],a=new B,l=new ee;for(let f=0;f<=t;f++){let v=f/t;s[f]=this.getTangentAt(v,new B)}r[0]=new B,o[0]=new B;let h=Number.MAX_VALUE,u=Math.abs(s[0].x),m=Math.abs(s[0].y),x=Math.abs(s[0].z);u<=h&&(h=u,n.set(1,0,0)),m<=h&&(h=m,n.set(0,1,0)),x<=h&&n.set(0,0,1),a.crossVectors(s[0],n).normalize(),r[0].crossVectors(s[0],a),o[0].crossVectors(s[0],r[0]);for(let f=1;f<=t;f++){if(r[f]=r[f-1].clone(),o[f]=o[f-1].clone(),a.crossVectors(s[f-1],s[f]),a.length()>Number.EPSILON){a.normalize();let v=Math.acos(Xe(s[f-1].dot(s[f]),-1,1));r[f].applyMatrix4(l.makeRotationAxis(a,v))}o[f].crossVectors(s[f],r[f])}if(e===!0){let f=Math.acos(Xe(r[0].dot(r[t]),-1,1));f/=t,s[0].dot(a.crossVectors(r[0],r[t]))>0&&(f=-f);for(let v=1;v<=t;v++)r[v].applyMatrix4(l.makeRotationAxis(s[v],f*v)),o[v].crossVectors(s[v],r[v])}return{tangents:s,normals:r,binormals:o}}clone(){return new this.constructor().copy(this)}copy(t){return this.arcLengthDivisions=t.arcLengthDivisions,this}toJSON(){let t={metadata:{version:4.6,type:"Curve",generator:"Curve.toJSON"}};return t.arcLengthDivisions=this.arcLengthDivisions,t.type=this.type,t}fromJSON(t){return this.arcLengthDivisions=t.arcLengthDivisions,this}},vr=class extends yn{constructor(t=0,e=0,n=1,s=1,r=0,o=Math.PI*2,a=!1,l=0){super(),this.isEllipseCurve=!0,this.type="EllipseCurve",this.aX=t,this.aY=e,this.xRadius=n,this.yRadius=s,this.aStartAngle=r,this.aEndAngle=o,this.aClockwise=a,this.aRotation=l}getPoint(t,e=new lt){let n=e,s=Math.PI*2,r=this.aEndAngle-this.aStartAngle,o=Math.abs(r)<Number.EPSILON;for(;r<0;)r+=s;for(;r>s;)r-=s;r<Number.EPSILON&&(o?r=0:r=s),this.aClockwise===!0&&!o&&(r===s?r=-s:r=r-s);let a=this.aStartAngle+t*r,l=this.aX+this.xRadius*Math.cos(a),h=this.aY+this.yRadius*Math.sin(a);if(this.aRotation!==0){let u=Math.cos(this.aRotation),m=Math.sin(this.aRotation),x=l-this.aX,f=h-this.aY;l=x*u-f*m+this.aX,h=x*m+f*u+this.aY}return n.set(l,h)}copy(t){return super.copy(t),this.aX=t.aX,this.aY=t.aY,this.xRadius=t.xRadius,this.yRadius=t.yRadius,this.aStartAngle=t.aStartAngle,this.aEndAngle=t.aEndAngle,this.aClockwise=t.aClockwise,this.aRotation=t.aRotation,this}toJSON(){let t=super.toJSON();return t.aX=this.aX,t.aY=this.aY,t.xRadius=this.xRadius,t.yRadius=this.yRadius,t.aStartAngle=this.aStartAngle,t.aEndAngle=this.aEndAngle,t.aClockwise=this.aClockwise,t.aRotation=this.aRotation,t}fromJSON(t){return super.fromJSON(t),this.aX=t.aX,this.aY=t.aY,this.xRadius=t.xRadius,this.yRadius=t.yRadius,this.aStartAngle=t.aStartAngle,this.aEndAngle=t.aEndAngle,this.aClockwise=t.aClockwise,this.aRotation=t.aRotation,this}},dc=class extends vr{constructor(t,e,n,s,r,o){super(t,e,n,n,s,r,o),this.isArcCurve=!0,this.type="ArcCurve"}};function Wc(){let i=0,t=0,e=0,n=0;function s(r,o,a,l){i=r,t=a,e=-3*r+3*o-2*a-l,n=2*r-2*o+a+l}return{initCatmullRom:function(r,o,a,l,h){s(o,a,h*(a-r),h*(l-o))},initNonuniformCatmullRom:function(r,o,a,l,h,u,m){let x=(o-r)/h-(a-r)/(h+u)+(a-o)/u,f=(a-o)/u-(l-o)/(u+m)+(l-a)/m;x*=u,f*=u,s(o,a,x,f)},calc:function(r){let o=r*r,a=o*r;return i+t*r+e*o+n*a}}}var mo=new B,nl=new Wc,il=new Wc,sl=new Wc,pc=class extends yn{constructor(t=[],e=!1,n="centripetal",s=.5){super(),this.isCatmullRomCurve3=!0,this.type="CatmullRomCurve3",this.points=t,this.closed=e,this.curveType=n,this.tension=s}getPoint(t,e=new B){let n=e,s=this.points,r=s.length,o=(r-(this.closed?0:1))*t,a=Math.floor(o),l=o-a;this.closed?a+=a>0?0:(Math.floor(Math.abs(a)/r)+1)*r:l===0&&a===r-1&&(a=r-2,l=1);let h,u;this.closed||a>0?h=s[(a-1)%r]:(mo.subVectors(s[0],s[1]).add(s[0]),h=mo);let m=s[a%r],x=s[(a+1)%r];if(this.closed||a+2<r?u=s[(a+2)%r]:(mo.subVectors(s[r-1],s[r-2]).add(s[r-1]),u=mo),this.curveType==="centripetal"||this.curveType==="chordal"){let f=this.curveType==="chordal"?.5:.25,v=Math.pow(h.distanceToSquared(m),f),d=Math.pow(m.distanceToSquared(x),f),y=Math.pow(x.distanceToSquared(u),f);d<1e-4&&(d=1),v<1e-4&&(v=d),y<1e-4&&(y=d),nl.initNonuniformCatmullRom(h.x,m.x,x.x,u.x,v,d,y),il.initNonuniformCatmullRom(h.y,m.y,x.y,u.y,v,d,y),sl.initNonuniformCatmullRom(h.z,m.z,x.z,u.z,v,d,y)}else this.curveType==="catmullrom"&&(nl.initCatmullRom(h.x,m.x,x.x,u.x,this.tension),il.initCatmullRom(h.y,m.y,x.y,u.y,this.tension),sl.initCatmullRom(h.z,m.z,x.z,u.z,this.tension));return n.set(nl.calc(l),il.calc(l),sl.calc(l)),n}copy(t){super.copy(t),this.points=[];for(let e=0,n=t.points.length;e<n;e++){let s=t.points[e];this.points.push(s.clone())}return this.closed=t.closed,this.curveType=t.curveType,this.tension=t.tension,this}toJSON(){let t=super.toJSON();t.points=[];for(let e=0,n=this.points.length;e<n;e++){let s=this.points[e];t.points.push(s.toArray())}return t.closed=this.closed,t.curveType=this.curveType,t.tension=this.tension,t}fromJSON(t){super.fromJSON(t),this.points=[];for(let e=0,n=t.points.length;e<n;e++){let s=t.points[e];this.points.push(new B().fromArray(s))}return this.closed=t.closed,this.curveType=t.curveType,this.tension=t.tension,this}};function Hu(i,t,e,n,s){let r=(n-t)*.5,o=(s-e)*.5,a=i*i,l=i*a;return(2*e-2*n+r+o)*l+(-3*e+3*n-2*r-o)*a+r*i+e}function ay(i,t){let e=1-i;return e*e*t}function ly(i,t){return 2*(1-i)*i*t}function cy(i,t){return i*i*t}function cr(i,t,e,n){return ay(i,t)+ly(i,e)+cy(i,n)}function hy(i,t){let e=1-i;return e*e*e*t}function uy(i,t){let e=1-i;return 3*e*e*i*t}function fy(i,t){return 3*(1-i)*i*i*t}function dy(i,t){return i*i*i*t}function hr(i,t,e,n,s){return hy(i,t)+uy(i,e)+fy(i,n)+dy(i,s)}var Vo=class extends yn{constructor(t=new lt,e=new lt,n=new lt,s=new lt){super(),this.isCubicBezierCurve=!0,this.type="CubicBezierCurve",this.v0=t,this.v1=e,this.v2=n,this.v3=s}getPoint(t,e=new lt){let n=e,s=this.v0,r=this.v1,o=this.v2,a=this.v3;return n.set(hr(t,s.x,r.x,o.x,a.x),hr(t,s.y,r.y,o.y,a.y)),n}copy(t){return super.copy(t),this.v0.copy(t.v0),this.v1.copy(t.v1),this.v2.copy(t.v2),this.v3.copy(t.v3),this}toJSON(){let t=super.toJSON();return t.v0=this.v0.toArray(),t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t.v3=this.v3.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v0.fromArray(t.v0),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this.v3.fromArray(t.v3),this}},mc=class extends yn{constructor(t=new B,e=new B,n=new B,s=new B){super(),this.isCubicBezierCurve3=!0,this.type="CubicBezierCurve3",this.v0=t,this.v1=e,this.v2=n,this.v3=s}getPoint(t,e=new B){let n=e,s=this.v0,r=this.v1,o=this.v2,a=this.v3;return n.set(hr(t,s.x,r.x,o.x,a.x),hr(t,s.y,r.y,o.y,a.y),hr(t,s.z,r.z,o.z,a.z)),n}copy(t){return super.copy(t),this.v0.copy(t.v0),this.v1.copy(t.v1),this.v2.copy(t.v2),this.v3.copy(t.v3),this}toJSON(){let t=super.toJSON();return t.v0=this.v0.toArray(),t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t.v3=this.v3.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v0.fromArray(t.v0),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this.v3.fromArray(t.v3),this}},Wo=class extends yn{constructor(t=new lt,e=new lt){super(),this.isLineCurve=!0,this.type="LineCurve",this.v1=t,this.v2=e}getPoint(t,e=new lt){let n=e;return t===1?n.copy(this.v2):(n.copy(this.v2).sub(this.v1),n.multiplyScalar(t).add(this.v1)),n}getPointAt(t,e){return this.getPoint(t,e)}getTangent(t,e=new lt){return e.subVectors(this.v2,this.v1).normalize()}getTangentAt(t,e){return this.getTangent(t,e)}copy(t){return super.copy(t),this.v1.copy(t.v1),this.v2.copy(t.v2),this}toJSON(){let t=super.toJSON();return t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this}},gc=class extends yn{constructor(t=new B,e=new B){super(),this.isLineCurve3=!0,this.type="LineCurve3",this.v1=t,this.v2=e}getPoint(t,e=new B){let n=e;return t===1?n.copy(this.v2):(n.copy(this.v2).sub(this.v1),n.multiplyScalar(t).add(this.v1)),n}getPointAt(t,e){return this.getPoint(t,e)}getTangent(t,e=new B){return e.subVectors(this.v2,this.v1).normalize()}getTangentAt(t,e){return this.getTangent(t,e)}copy(t){return super.copy(t),this.v1.copy(t.v1),this.v2.copy(t.v2),this}toJSON(){let t=super.toJSON();return t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this}},Xo=class extends yn{constructor(t=new lt,e=new lt,n=new lt){super(),this.isQuadraticBezierCurve=!0,this.type="QuadraticBezierCurve",this.v0=t,this.v1=e,this.v2=n}getPoint(t,e=new lt){let n=e,s=this.v0,r=this.v1,o=this.v2;return n.set(cr(t,s.x,r.x,o.x),cr(t,s.y,r.y,o.y)),n}copy(t){return super.copy(t),this.v0.copy(t.v0),this.v1.copy(t.v1),this.v2.copy(t.v2),this}toJSON(){let t=super.toJSON();return t.v0=this.v0.toArray(),t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v0.fromArray(t.v0),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this}},xc=class extends yn{constructor(t=new B,e=new B,n=new B){super(),this.isQuadraticBezierCurve3=!0,this.type="QuadraticBezierCurve3",this.v0=t,this.v1=e,this.v2=n}getPoint(t,e=new B){let n=e,s=this.v0,r=this.v1,o=this.v2;return n.set(cr(t,s.x,r.x,o.x),cr(t,s.y,r.y,o.y),cr(t,s.z,r.z,o.z)),n}copy(t){return super.copy(t),this.v0.copy(t.v0),this.v1.copy(t.v1),this.v2.copy(t.v2),this}toJSON(){let t=super.toJSON();return t.v0=this.v0.toArray(),t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v0.fromArray(t.v0),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this}},qo=class extends yn{constructor(t=[]){super(),this.isSplineCurve=!0,this.type="SplineCurve",this.points=t}getPoint(t,e=new lt){let n=e,s=this.points,r=(s.length-1)*t,o=Math.floor(r),a=r-o,l=s[o===0?o:o-1],h=s[o],u=s[o>s.length-2?s.length-1:o+1],m=s[o>s.length-3?s.length-1:o+2];return n.set(Hu(a,l.x,h.x,u.x,m.x),Hu(a,l.y,h.y,u.y,m.y)),n}copy(t){super.copy(t),this.points=[];for(let e=0,n=t.points.length;e<n;e++){let s=t.points[e];this.points.push(s.clone())}return this}toJSON(){let t=super.toJSON();t.points=[];for(let e=0,n=this.points.length;e<n;e++){let s=this.points[e];t.points.push(s.toArray())}return t}fromJSON(t){super.fromJSON(t),this.points=[];for(let e=0,n=t.points.length;e<n;e++){let s=t.points[e];this.points.push(new lt().fromArray(s))}return this}},yc=Object.freeze({__proto__:null,ArcCurve:dc,CatmullRomCurve3:pc,CubicBezierCurve:Vo,CubicBezierCurve3:mc,EllipseCurve:vr,LineCurve:Wo,LineCurve3:gc,QuadraticBezierCurve:Xo,QuadraticBezierCurve3:xc,SplineCurve:qo}),_c=class extends yn{constructor(){super(),this.type="CurvePath",this.curves=[],this.autoClose=!1}add(t){this.curves.push(t)}closePath(){let t=this.curves[0].getPoint(0),e=this.curves[this.curves.length-1].getPoint(1);if(!t.equals(e)){let n=t.isVector2===!0?"LineCurve":"LineCurve3";this.curves.push(new yc[n](e,t))}return this}getPoint(t,e){let n=t*this.getLength(),s=this.getCurveLengths(),r=0;for(;r<s.length;){if(s[r]>=n){let o=s[r]-n,a=this.curves[r],l=a.getLength(),h=l===0?0:1-o/l;return a.getPointAt(h,e)}r++}return null}getLength(){let t=this.getCurveLengths();return t[t.length-1]}updateArcLengths(){this.needsUpdate=!0,this.cacheLengths=null,this.getCurveLengths()}getCurveLengths(){if(this.cacheLengths&&this.cacheLengths.length===this.curves.length)return this.cacheLengths;let t=[],e=0;for(let n=0,s=this.curves.length;n<s;n++)e+=this.curves[n].getLength(),t.push(e);return this.cacheLengths=t,t}getSpacedPoints(t=40){let e=[];for(let n=0;n<=t;n++)e.push(this.getPoint(n/t));return this.autoClose&&e.push(e[0]),e}getPoints(t=12){let e=[],n;for(let s=0,r=this.curves;s<r.length;s++){let o=r[s],a=o.isEllipseCurve?t*2:o.isLineCurve||o.isLineCurve3?1:o.isSplineCurve?t*o.points.length:t,l=o.getPoints(a);for(let h=0;h<l.length;h++){let u=l[h];n&&n.equals(u)||(e.push(u),n=u)}}return this.autoClose&&e.length>1&&!e[e.length-1].equals(e[0])&&e.push(e[0]),e}copy(t){super.copy(t),this.curves=[];for(let e=0,n=t.curves.length;e<n;e++){let s=t.curves[e];this.curves.push(s.clone())}return this.autoClose=t.autoClose,this}toJSON(){let t=super.toJSON();t.autoClose=this.autoClose,t.curves=[];for(let e=0,n=this.curves.length;e<n;e++){let s=this.curves[e];t.curves.push(s.toJSON())}return t}fromJSON(t){super.fromJSON(t),this.autoClose=t.autoClose,this.curves=[];for(let e=0,n=t.curves.length;e<n;e++){let s=t.curves[e];this.curves.push(new yc[s.type]().fromJSON(s))}return this}},Mr=class extends _c{constructor(t){super(),this.type="Path",this.currentPoint=new lt,t&&this.setFromPoints(t)}setFromPoints(t){this.moveTo(t[0].x,t[0].y);for(let e=1,n=t.length;e<n;e++)this.lineTo(t[e].x,t[e].y);return this}moveTo(t,e){return this.currentPoint.set(t,e),this}lineTo(t,e){let n=new Wo(this.currentPoint.clone(),new lt(t,e));return this.curves.push(n),this.currentPoint.set(t,e),this}quadraticCurveTo(t,e,n,s){let r=new Xo(this.currentPoint.clone(),new lt(t,e),new lt(n,s));return this.curves.push(r),this.currentPoint.set(n,s),this}bezierCurveTo(t,e,n,s,r,o){let a=new Vo(this.currentPoint.clone(),new lt(t,e),new lt(n,s),new lt(r,o));return this.curves.push(a),this.currentPoint.set(r,o),this}splineThru(t){let e=[this.currentPoint.clone()].concat(t),n=new qo(e);return this.curves.push(n),this.currentPoint.copy(t[t.length-1]),this}arc(t,e,n,s,r,o){let a=this.currentPoint.x,l=this.currentPoint.y;return this.absarc(t+a,e+l,n,s,r,o),this}absarc(t,e,n,s,r,o){return this.absellipse(t,e,n,n,s,r,o),this}ellipse(t,e,n,s,r,o,a,l){let h=this.currentPoint.x,u=this.currentPoint.y;return this.absellipse(t+h,e+u,n,s,r,o,a,l),this}absellipse(t,e,n,s,r,o,a,l){let h=new vr(t,e,n,s,r,o,a,l);if(this.curves.length>0){let m=h.getPoint(0);m.equals(this.currentPoint)||this.lineTo(m.x,m.y)}this.curves.push(h);let u=h.getPoint(1);return this.currentPoint.copy(u),this}copy(t){return super.copy(t),this.currentPoint.copy(t.currentPoint),this}toJSON(){let t=super.toJSON();return t.currentPoint=this.currentPoint.toArray(),t}fromJSON(t){return super.fromJSON(t),this.currentPoint.fromArray(t.currentPoint),this}},vc=class i extends de{constructor(t=[new lt(0,-.5),new lt(.5,0),new lt(0,.5)],e=12,n=0,s=Math.PI*2){super(),this.type="LatheGeometry",this.parameters={points:t,segments:e,phiStart:n,phiLength:s},e=Math.floor(e),s=Xe(s,0,Math.PI*2);let r=[],o=[],a=[],l=[],h=[],u=1/e,m=new B,x=new lt,f=new B,v=new B,d=new B,y=0,c=0;for(let _=0;_<=t.length-1;_++)switch(_){case 0:y=t[_+1].x-t[_].x,c=t[_+1].y-t[_].y,f.x=c*1,f.y=-y,f.z=c*0,d.copy(f),f.normalize(),l.push(f.x,f.y,f.z);break;case t.length-1:l.push(d.x,d.y,d.z);break;default:y=t[_+1].x-t[_].x,c=t[_+1].y-t[_].y,f.x=c*1,f.y=-y,f.z=c*0,v.copy(f),f.x+=d.x,f.y+=d.y,f.z+=d.z,f.normalize(),l.push(f.x,f.y,f.z),d.copy(v)}for(let _=0;_<=e;_++){let p=n+_*u*s,g=Math.sin(p),T=Math.cos(p);for(let S=0;S<=t.length-1;S++){m.x=t[S].x*g,m.y=t[S].y,m.z=t[S].x*T,o.push(m.x,m.y,m.z),x.x=_/e,x.y=S/(t.length-1),a.push(x.x,x.y);let b=l[3*S+0]*g,w=l[3*S+1],M=l[3*S+0]*T;h.push(b,w,M)}}for(let _=0;_<e;_++)for(let p=0;p<t.length-1;p++){let g=p+_*t.length,T=g,S=g+t.length,b=g+t.length+1,w=g+1;r.push(T,S,w),r.push(b,w,S)}this.setIndex(r),this.setAttribute("position",new Wt(o,3)),this.setAttribute("uv",new Wt(a,2)),this.setAttribute("normal",new Wt(h,3))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.points,t.segments,t.phiStart,t.phiLength)}},Vi=class i extends vc{constructor(t=1,e=1,n=4,s=8){let r=new Mr;r.absarc(0,-e/2,t,Math.PI*1.5,0),r.absarc(0,e/2,t,0,Math.PI*.5),super(r.getPoints(n),s),this.type="CapsuleGeometry",this.parameters={radius:t,length:e,capSegments:n,radialSegments:s}}static fromJSON(t){return new i(t.radius,t.length,t.capSegments,t.radialSegments)}},Ls=class i extends de{constructor(t=1,e=32,n=0,s=Math.PI*2){super(),this.type="CircleGeometry",this.parameters={radius:t,segments:e,thetaStart:n,thetaLength:s},e=Math.max(3,e);let r=[],o=[],a=[],l=[],h=new B,u=new lt;o.push(0,0,0),a.push(0,0,1),l.push(.5,.5);for(let m=0,x=3;m<=e;m++,x+=3){let f=n+m/e*s;h.x=t*Math.cos(f),h.y=t*Math.sin(f),o.push(h.x,h.y,h.z),a.push(0,0,1),u.x=(o[x]/t+1)/2,u.y=(o[x+1]/t+1)/2,l.push(u.x,u.y)}for(let m=1;m<=e;m++)r.push(m,m+1,0);this.setIndex(r),this.setAttribute("position",new Wt(o,3)),this.setAttribute("normal",new Wt(a,3)),this.setAttribute("uv",new Wt(l,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.radius,t.segments,t.thetaStart,t.thetaLength)}},Gt=class i extends de{constructor(t=1,e=1,n=1,s=32,r=1,o=!1,a=0,l=Math.PI*2){super(),this.type="CylinderGeometry",this.parameters={radiusTop:t,radiusBottom:e,height:n,radialSegments:s,heightSegments:r,openEnded:o,thetaStart:a,thetaLength:l};let h=this;s=Math.floor(s),r=Math.floor(r);let u=[],m=[],x=[],f=[],v=0,d=[],y=n/2,c=0;_(),o===!1&&(t>0&&p(!0),e>0&&p(!1)),this.setIndex(u),this.setAttribute("position",new Wt(m,3)),this.setAttribute("normal",new Wt(x,3)),this.setAttribute("uv",new Wt(f,2));function _(){let g=new B,T=new B,S=0,b=(e-t)/n;for(let w=0;w<=r;w++){let M=[],E=w/r,F=E*(e-t)+t;for(let U=0;U<=s;U++){let R=U/s,N=R*l+a,V=Math.sin(N),z=Math.cos(N);T.x=F*V,T.y=-E*n+y,T.z=F*z,m.push(T.x,T.y,T.z),g.set(V,b,z).normalize(),x.push(g.x,g.y,g.z),f.push(R,1-E),M.push(v++)}d.push(M)}for(let w=0;w<s;w++)for(let M=0;M<r;M++){let E=d[M][w],F=d[M+1][w],U=d[M+1][w+1],R=d[M][w+1];(t>0||M!==0)&&(u.push(E,F,R),S+=3),(e>0||M!==r-1)&&(u.push(F,U,R),S+=3)}h.addGroup(c,S,0),c+=S}function p(g){let T=v,S=new lt,b=new B,w=0,M=g===!0?t:e,E=g===!0?1:-1;for(let U=1;U<=s;U++)m.push(0,y*E,0),x.push(0,E,0),f.push(.5,.5),v++;let F=v;for(let U=0;U<=s;U++){let N=U/s*l+a,V=Math.cos(N),z=Math.sin(N);b.x=M*z,b.y=y*E,b.z=M*V,m.push(b.x,b.y,b.z),x.push(0,E,0),S.x=V*.5+.5,S.y=z*.5*E+.5,f.push(S.x,S.y),v++}for(let U=0;U<s;U++){let R=T+U,N=F+U;g===!0?u.push(N,N+1,R):u.push(N+1,N,R),w+=3}h.addGroup(c,w,g===!0?1:2),c+=w}}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.radiusTop,t.radiusBottom,t.height,t.radialSegments,t.heightSegments,t.openEnded,t.thetaStart,t.thetaLength)}},rn=class i extends Gt{constructor(t=1,e=1,n=32,s=1,r=!1,o=0,a=Math.PI*2){super(0,t,e,n,s,r,o,a),this.type="ConeGeometry",this.parameters={radius:t,height:e,radialSegments:n,heightSegments:s,openEnded:r,thetaStart:o,thetaLength:a}}static fromJSON(t){return new i(t.radius,t.height,t.radialSegments,t.heightSegments,t.openEnded,t.thetaStart,t.thetaLength)}},Yo=class i extends de{constructor(t=[],e=[],n=1,s=0){super(),this.type="PolyhedronGeometry",this.parameters={vertices:t,indices:e,radius:n,detail:s};let r=[],o=[];a(s),h(n),u(),this.setAttribute("position",new Wt(r,3)),this.setAttribute("normal",new Wt(r.slice(),3)),this.setAttribute("uv",new Wt(o,2)),s===0?this.computeVertexNormals():this.normalizeNormals();function a(_){let p=new B,g=new B,T=new B;for(let S=0;S<e.length;S+=3)f(e[S+0],p),f(e[S+1],g),f(e[S+2],T),l(p,g,T,_)}function l(_,p,g,T){let S=T+1,b=[];for(let w=0;w<=S;w++){b[w]=[];let M=_.clone().lerp(g,w/S),E=p.clone().lerp(g,w/S),F=S-w;for(let U=0;U<=F;U++)U===0&&w===S?b[w][U]=M:b[w][U]=M.clone().lerp(E,U/F)}for(let w=0;w<S;w++)for(let M=0;M<2*(S-w)-1;M++){let E=Math.floor(M/2);M%2===0?(x(b[w][E+1]),x(b[w+1][E]),x(b[w][E])):(x(b[w][E+1]),x(b[w+1][E+1]),x(b[w+1][E]))}}function h(_){let p=new B;for(let g=0;g<r.length;g+=3)p.x=r[g+0],p.y=r[g+1],p.z=r[g+2],p.normalize().multiplyScalar(_),r[g+0]=p.x,r[g+1]=p.y,r[g+2]=p.z}function u(){let _=new B;for(let p=0;p<r.length;p+=3){_.x=r[p+0],_.y=r[p+1],_.z=r[p+2];let g=y(_)/2/Math.PI+.5,T=c(_)/Math.PI+.5;o.push(g,1-T)}v(),m()}function m(){for(let _=0;_<o.length;_+=6){let p=o[_+0],g=o[_+2],T=o[_+4],S=Math.max(p,g,T),b=Math.min(p,g,T);S>.9&&b<.1&&(p<.2&&(o[_+0]+=1),g<.2&&(o[_+2]+=1),T<.2&&(o[_+4]+=1))}}function x(_){r.push(_.x,_.y,_.z)}function f(_,p){let g=_*3;p.x=t[g+0],p.y=t[g+1],p.z=t[g+2]}function v(){let _=new B,p=new B,g=new B,T=new B,S=new lt,b=new lt,w=new lt;for(let M=0,E=0;M<r.length;M+=9,E+=6){_.set(r[M+0],r[M+1],r[M+2]),p.set(r[M+3],r[M+4],r[M+5]),g.set(r[M+6],r[M+7],r[M+8]),S.set(o[E+0],o[E+1]),b.set(o[E+2],o[E+3]),w.set(o[E+4],o[E+5]),T.copy(_).add(p).add(g).divideScalar(3);let F=y(T);d(S,E+0,_,F),d(b,E+2,p,F),d(w,E+4,g,F)}}function d(_,p,g,T){T<0&&_.x===1&&(o[p]=_.x-1),g.x===0&&g.z===0&&(o[p]=T/2/Math.PI+.5)}function y(_){return Math.atan2(_.z,-_.x)}function c(_){return Math.atan2(-_.y,Math.sqrt(_.x*_.x+_.z*_.z))}}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.vertices,t.indices,t.radius,t.details)}};var Er=class extends Mr{constructor(t){super(t),this.uuid=$n(),this.type="Shape",this.holes=[]}getPointsHoles(t){let e=[];for(let n=0,s=this.holes.length;n<s;n++)e[n]=this.holes[n].getPoints(t);return e}extractPoints(t){return{shape:this.getPoints(t),holes:this.getPointsHoles(t)}}copy(t){super.copy(t),this.holes=[];for(let e=0,n=t.holes.length;e<n;e++){let s=t.holes[e];this.holes.push(s.clone())}return this}toJSON(){let t=super.toJSON();t.uuid=this.uuid,t.holes=[];for(let e=0,n=this.holes.length;e<n;e++){let s=this.holes[e];t.holes.push(s.toJSON())}return t}fromJSON(t){super.fromJSON(t),this.uuid=t.uuid,this.holes=[];for(let e=0,n=t.holes.length;e<n;e++){let s=t.holes[e];this.holes.push(new Mr().fromJSON(s))}return this}},py={triangulate:function(i,t,e=2){let n=t&&t.length,s=n?t[0]*e:i.length,r=mf(i,0,s,e,!0),o=[];if(!r||r.next===r.prev)return o;let a,l,h,u,m,x,f;if(n&&(r=_y(i,t,r,e)),i.length>80*e){a=h=i[0],l=u=i[1];for(let v=e;v<s;v+=e)m=i[v],x=i[v+1],m<a&&(a=m),x<l&&(l=x),m>h&&(h=m),x>u&&(u=x);f=Math.max(h-a,u-l),f=f!==0?32767/f:0}return Sr(r,o,e,a,l,f,0),o}};function mf(i,t,e,n,s){let r,o;if(s===Fy(i,t,e,n)>0)for(r=t;r<e;r+=n)o=ku(r,i[r],i[r+1],o);else for(r=e-n;r>=t;r-=n)o=ku(r,i[r],i[r+1],o);return o&&na(o,o.next)&&(wr(o),o=o.next),o}function Wi(i,t){if(!i)return i;t||(t=i);let e=i,n;do if(n=!1,!e.steiner&&(na(e,e.next)||Le(e.prev,e,e.next)===0)){if(wr(e),e=t=e.prev,e===e.next)break;n=!0}else e=e.next;while(n||e!==t);return t}function Sr(i,t,e,n,s,r,o){if(!i)return;!o&&r&&Ty(i,n,s,r);let a=i,l,h;for(;i.prev!==i.next;){if(l=i.prev,h=i.next,r?gy(i,n,s,r):my(i)){t.push(l.i/e|0),t.push(i.i/e|0),t.push(h.i/e|0),wr(i),i=h.next,a=h.next;continue}if(i=h,i===a){o?o===1?(i=xy(Wi(i),t,e),Sr(i,t,e,n,s,r,2)):o===2&&yy(i,t,e,n,s,r):Sr(Wi(i),t,e,n,s,r,1);break}}}function my(i){let t=i.prev,e=i,n=i.next;if(Le(t,e,n)>=0)return!1;let s=t.x,r=e.x,o=n.x,a=t.y,l=e.y,h=n.y,u=s<r?s<o?s:o:r<o?r:o,m=a<l?a<h?a:h:l<h?l:h,x=s>r?s>o?s:o:r>o?r:o,f=a>l?a>h?a:h:l>h?l:h,v=n.next;for(;v!==t;){if(v.x>=u&&v.x<=x&&v.y>=m&&v.y<=f&&Es(s,a,r,l,o,h,v.x,v.y)&&Le(v.prev,v,v.next)>=0)return!1;v=v.next}return!0}function gy(i,t,e,n){let s=i.prev,r=i,o=i.next;if(Le(s,r,o)>=0)return!1;let a=s.x,l=r.x,h=o.x,u=s.y,m=r.y,x=o.y,f=a<l?a<h?a:h:l<h?l:h,v=u<m?u<x?u:x:m<x?m:x,d=a>l?a>h?a:h:l>h?l:h,y=u>m?u>x?u:x:m>x?m:x,c=Mc(f,v,t,e,n),_=Mc(d,y,t,e,n),p=i.prevZ,g=i.nextZ;for(;p&&p.z>=c&&g&&g.z<=_;){if(p.x>=f&&p.x<=d&&p.y>=v&&p.y<=y&&p!==s&&p!==o&&Es(a,u,l,m,h,x,p.x,p.y)&&Le(p.prev,p,p.next)>=0||(p=p.prevZ,g.x>=f&&g.x<=d&&g.y>=v&&g.y<=y&&g!==s&&g!==o&&Es(a,u,l,m,h,x,g.x,g.y)&&Le(g.prev,g,g.next)>=0))return!1;g=g.nextZ}for(;p&&p.z>=c;){if(p.x>=f&&p.x<=d&&p.y>=v&&p.y<=y&&p!==s&&p!==o&&Es(a,u,l,m,h,x,p.x,p.y)&&Le(p.prev,p,p.next)>=0)return!1;p=p.prevZ}for(;g&&g.z<=_;){if(g.x>=f&&g.x<=d&&g.y>=v&&g.y<=y&&g!==s&&g!==o&&Es(a,u,l,m,h,x,g.x,g.y)&&Le(g.prev,g,g.next)>=0)return!1;g=g.nextZ}return!0}function xy(i,t,e){let n=i;do{let s=n.prev,r=n.next.next;!na(s,r)&&gf(s,n,n.next,r)&&Tr(s,r)&&Tr(r,s)&&(t.push(s.i/e|0),t.push(n.i/e|0),t.push(r.i/e|0),wr(n),wr(n.next),n=i=r),n=n.next}while(n!==i);return Wi(n)}function yy(i,t,e,n,s,r){let o=i;do{let a=o.next.next;for(;a!==o.prev;){if(o.i!==a.i&&Ay(o,a)){let l=xf(o,a);o=Wi(o,o.next),l=Wi(l,l.next),Sr(o,t,e,n,s,r,0),Sr(l,t,e,n,s,r,0);return}a=a.next}o=o.next}while(o!==i)}function _y(i,t,e,n){let s=[],r,o,a,l,h;for(r=0,o=t.length;r<o;r++)a=t[r]*n,l=r<o-1?t[r+1]*n:i.length,h=mf(i,a,l,n,!1),h===h.next&&(h.steiner=!0),s.push(by(h));for(s.sort(vy),r=0;r<s.length;r++)e=My(s[r],e);return e}function vy(i,t){return i.x-t.x}function My(i,t){let e=Ey(i,t);if(!e)return t;let n=xf(e,i);return Wi(n,n.next),Wi(e,e.next)}function Ey(i,t){let e=t,n=-1/0,s,r=i.x,o=i.y;do{if(o<=e.y&&o>=e.next.y&&e.next.y!==e.y){let x=e.x+(o-e.y)*(e.next.x-e.x)/(e.next.y-e.y);if(x<=r&&x>n&&(n=x,s=e.x<e.next.x?e:e.next,x===r))return s}e=e.next}while(e!==t);if(!s)return null;let a=s,l=s.x,h=s.y,u=1/0,m;e=s;do r>=e.x&&e.x>=l&&r!==e.x&&Es(o<h?r:n,o,l,h,o<h?n:r,o,e.x,e.y)&&(m=Math.abs(o-e.y)/(r-e.x),Tr(e,i)&&(m<u||m===u&&(e.x>s.x||e.x===s.x&&Sy(s,e)))&&(s=e,u=m)),e=e.next;while(e!==a);return s}function Sy(i,t){return Le(i.prev,i,t.prev)<0&&Le(t.next,i,i.next)<0}function Ty(i,t,e,n){let s=i;do s.z===0&&(s.z=Mc(s.x,s.y,t,e,n)),s.prevZ=s.prev,s.nextZ=s.next,s=s.next;while(s!==i);s.prevZ.nextZ=null,s.prevZ=null,wy(s)}function wy(i){let t,e,n,s,r,o,a,l,h=1;do{for(e=i,i=null,r=null,o=0;e;){for(o++,n=e,a=0,t=0;t<h&&(a++,n=n.nextZ,!!n);t++);for(l=h;a>0||l>0&&n;)a!==0&&(l===0||!n||e.z<=n.z)?(s=e,e=e.nextZ,a--):(s=n,n=n.nextZ,l--),r?r.nextZ=s:i=s,s.prevZ=r,r=s;e=n}r.nextZ=null,h*=2}while(o>1);return i}function Mc(i,t,e,n,s){return i=(i-e)*s|0,t=(t-n)*s|0,i=(i|i<<8)&16711935,i=(i|i<<4)&252645135,i=(i|i<<2)&858993459,i=(i|i<<1)&1431655765,t=(t|t<<8)&16711935,t=(t|t<<4)&252645135,t=(t|t<<2)&858993459,t=(t|t<<1)&1431655765,i|t<<1}function by(i){let t=i,e=i;do(t.x<e.x||t.x===e.x&&t.y<e.y)&&(e=t),t=t.next;while(t!==i);return e}function Es(i,t,e,n,s,r,o,a){return(s-o)*(t-a)>=(i-o)*(r-a)&&(i-o)*(n-a)>=(e-o)*(t-a)&&(e-o)*(r-a)>=(s-o)*(n-a)}function Ay(i,t){return i.next.i!==t.i&&i.prev.i!==t.i&&!Ry(i,t)&&(Tr(i,t)&&Tr(t,i)&&Cy(i,t)&&(Le(i.prev,i,t.prev)||Le(i,t.prev,t))||na(i,t)&&Le(i.prev,i,i.next)>0&&Le(t.prev,t,t.next)>0)}function Le(i,t,e){return(t.y-i.y)*(e.x-t.x)-(t.x-i.x)*(e.y-t.y)}function na(i,t){return i.x===t.x&&i.y===t.y}function gf(i,t,e,n){let s=xo(Le(i,t,e)),r=xo(Le(i,t,n)),o=xo(Le(e,n,i)),a=xo(Le(e,n,t));return!!(s!==r&&o!==a||s===0&&go(i,e,t)||r===0&&go(i,n,t)||o===0&&go(e,i,n)||a===0&&go(e,t,n))}function go(i,t,e){return t.x<=Math.max(i.x,e.x)&&t.x>=Math.min(i.x,e.x)&&t.y<=Math.max(i.y,e.y)&&t.y>=Math.min(i.y,e.y)}function xo(i){return i>0?1:i<0?-1:0}function Ry(i,t){let e=i;do{if(e.i!==i.i&&e.next.i!==i.i&&e.i!==t.i&&e.next.i!==t.i&&gf(e,e.next,i,t))return!0;e=e.next}while(e!==i);return!1}function Tr(i,t){return Le(i.prev,i,i.next)<0?Le(i,t,i.next)>=0&&Le(i,i.prev,t)>=0:Le(i,t,i.prev)<0||Le(i,i.next,t)<0}function Cy(i,t){let e=i,n=!1,s=(i.x+t.x)/2,r=(i.y+t.y)/2;do e.y>r!=e.next.y>r&&e.next.y!==e.y&&s<(e.next.x-e.x)*(r-e.y)/(e.next.y-e.y)+e.x&&(n=!n),e=e.next;while(e!==i);return n}function xf(i,t){let e=new Ec(i.i,i.x,i.y),n=new Ec(t.i,t.x,t.y),s=i.next,r=t.prev;return i.next=t,t.prev=i,e.next=s,s.prev=e,n.next=e,e.prev=n,r.next=n,n.prev=r,n}function ku(i,t,e,n){let s=new Ec(i,t,e);return n?(s.next=n.next,s.prev=n,n.next.prev=s,n.next=s):(s.prev=s,s.next=s),s}function wr(i){i.next.prev=i.prev,i.prev.next=i.next,i.prevZ&&(i.prevZ.nextZ=i.nextZ),i.nextZ&&(i.nextZ.prevZ=i.prevZ)}function Ec(i,t,e){this.i=i,this.x=t,this.y=e,this.prev=null,this.next=null,this.z=0,this.prevZ=null,this.nextZ=null,this.steiner=!1}function Fy(i,t,e,n){let s=0;for(let r=t,o=e-n;r<e;r+=n)s+=(i[o]-i[r])*(i[r+1]+i[o+1]),o=r;return s}var ur=class i{static area(t){let e=t.length,n=0;for(let s=e-1,r=0;r<e;s=r++)n+=t[s].x*t[r].y-t[r].x*t[s].y;return n*.5}static isClockWise(t){return i.area(t)<0}static triangulateShape(t,e){let n=[],s=[],r=[];Gu(t),Vu(n,t);let o=t.length;e.forEach(Gu);for(let l=0;l<e.length;l++)s.push(o),o+=e[l].length,Vu(n,e[l]);let a=py.triangulate(n,s);for(let l=0;l<a.length;l+=3)r.push(a.slice(l,l+3));return r}};function Gu(i){let t=i.length;t>2&&i[t-1].equals(i[0])&&i.pop()}function Vu(i,t){for(let e=0;e<t.length;e++)i.push(t[e].x),i.push(t[e].y)}var Zo=class i extends de{constructor(t=new Er([new lt(.5,.5),new lt(-.5,.5),new lt(-.5,-.5),new lt(.5,-.5)]),e={}){super(),this.type="ExtrudeGeometry",this.parameters={shapes:t,options:e},t=Array.isArray(t)?t:[t];let n=this,s=[],r=[];for(let a=0,l=t.length;a<l;a++){let h=t[a];o(h)}this.setAttribute("position",new Wt(s,3)),this.setAttribute("uv",new Wt(r,2)),this.computeVertexNormals();function o(a){let l=[],h=e.curveSegments!==void 0?e.curveSegments:12,u=e.steps!==void 0?e.steps:1,m=e.depth!==void 0?e.depth:1,x=e.bevelEnabled!==void 0?e.bevelEnabled:!0,f=e.bevelThickness!==void 0?e.bevelThickness:.2,v=e.bevelSize!==void 0?e.bevelSize:f-.1,d=e.bevelOffset!==void 0?e.bevelOffset:0,y=e.bevelSegments!==void 0?e.bevelSegments:3,c=e.extrudePath,_=e.UVGenerator!==void 0?e.UVGenerator:Iy,p,g=!1,T,S,b,w;c&&(p=c.getSpacedPoints(u),g=!0,x=!1,T=c.computeFrenetFrames(u,!1),S=new B,b=new B,w=new B),x||(y=0,f=0,v=0,d=0);let M=a.extractPoints(h),E=M.shape,F=M.holes;if(!ur.isClockWise(E)){E=E.reverse();for(let nt=0,ct=F.length;nt<ct;nt++){let L=F[nt];ur.isClockWise(L)&&(F[nt]=L.reverse())}}let R=ur.triangulateShape(E,F),N=E;for(let nt=0,ct=F.length;nt<ct;nt++){let L=F[nt];E=E.concat(L)}function V(nt,ct,L){return ct||console.error("THREE.ExtrudeGeometry: vec does not exist"),nt.clone().addScaledVector(ct,L)}let z=E.length,O=R.length;function D(nt,ct,L){let Dt,ot,bt,ft=nt.x-ct.x,zt=nt.y-ct.y,Tt=L.x-nt.x,P=L.y-nt.y,A=ft*ft+zt*zt,W=ft*P-zt*Tt;if(Math.abs(W)>Number.EPSILON){let j=Math.sqrt(A),it=Math.sqrt(Tt*Tt+P*P),Q=ct.x-zt/j,Ft=ct.y+ft/j,gt=L.x-P/it,wt=L.y+Tt/it,Qt=((gt-Q)*P-(wt-Ft)*Tt)/(ft*P-zt*Tt);Dt=Q+ft*Qt-nt.x,ot=Ft+zt*Qt-nt.y;let at=Dt*Dt+ot*ot;if(at<=2)return new lt(Dt,ot);bt=Math.sqrt(at/2)}else{let j=!1;ft>Number.EPSILON?Tt>Number.EPSILON&&(j=!0):ft<-Number.EPSILON?Tt<-Number.EPSILON&&(j=!0):Math.sign(zt)===Math.sign(P)&&(j=!0),j?(Dt=-zt,ot=ft,bt=Math.sqrt(A)):(Dt=ft,ot=zt,bt=Math.sqrt(A/2))}return new lt(Dt/bt,ot/bt)}let X=[];for(let nt=0,ct=N.length,L=ct-1,Dt=nt+1;nt<ct;nt++,L++,Dt++)L===ct&&(L=0),Dt===ct&&(Dt=0),X[nt]=D(N[nt],N[L],N[Dt]);let $=[],tt,ut=X.concat();for(let nt=0,ct=F.length;nt<ct;nt++){let L=F[nt];tt=[];for(let Dt=0,ot=L.length,bt=ot-1,ft=Dt+1;Dt<ot;Dt++,bt++,ft++)bt===ot&&(bt=0),ft===ot&&(ft=0),tt[Dt]=D(L[Dt],L[bt],L[ft]);$.push(tt),ut=ut.concat(tt)}for(let nt=0;nt<y;nt++){let ct=nt/y,L=f*Math.cos(ct*Math.PI/2),Dt=v*Math.sin(ct*Math.PI/2)+d;for(let ot=0,bt=N.length;ot<bt;ot++){let ft=V(N[ot],X[ot],Dt);rt(ft.x,ft.y,-L)}for(let ot=0,bt=F.length;ot<bt;ot++){let ft=F[ot];tt=$[ot];for(let zt=0,Tt=ft.length;zt<Tt;zt++){let P=V(ft[zt],tt[zt],Dt);rt(P.x,P.y,-L)}}}let Ct=v+d;for(let nt=0;nt<z;nt++){let ct=x?V(E[nt],ut[nt],Ct):E[nt];g?(b.copy(T.normals[0]).multiplyScalar(ct.x),S.copy(T.binormals[0]).multiplyScalar(ct.y),w.copy(p[0]).add(b).add(S),rt(w.x,w.y,w.z)):rt(ct.x,ct.y,0)}for(let nt=1;nt<=u;nt++)for(let ct=0;ct<z;ct++){let L=x?V(E[ct],ut[ct],Ct):E[ct];g?(b.copy(T.normals[nt]).multiplyScalar(L.x),S.copy(T.binormals[nt]).multiplyScalar(L.y),w.copy(p[nt]).add(b).add(S),rt(w.x,w.y,w.z)):rt(L.x,L.y,m/u*nt)}for(let nt=y-1;nt>=0;nt--){let ct=nt/y,L=f*Math.cos(ct*Math.PI/2),Dt=v*Math.sin(ct*Math.PI/2)+d;for(let ot=0,bt=N.length;ot<bt;ot++){let ft=V(N[ot],X[ot],Dt);rt(ft.x,ft.y,m+L)}for(let ot=0,bt=F.length;ot<bt;ot++){let ft=F[ot];tt=$[ot];for(let zt=0,Tt=ft.length;zt<Tt;zt++){let P=V(ft[zt],tt[zt],Dt);g?rt(P.x,P.y+p[u-1].y,p[u-1].x+L):rt(P.x,P.y,m+L)}}}J(),st();function J(){let nt=s.length/3;if(x){let ct=0,L=z*ct;for(let Dt=0;Dt<O;Dt++){let ot=R[Dt];Nt(ot[2]+L,ot[1]+L,ot[0]+L)}ct=u+y*2,L=z*ct;for(let Dt=0;Dt<O;Dt++){let ot=R[Dt];Nt(ot[0]+L,ot[1]+L,ot[2]+L)}}else{for(let ct=0;ct<O;ct++){let L=R[ct];Nt(L[2],L[1],L[0])}for(let ct=0;ct<O;ct++){let L=R[ct];Nt(L[0]+z*u,L[1]+z*u,L[2]+z*u)}}n.addGroup(nt,s.length/3-nt,0)}function st(){let nt=s.length/3,ct=0;_t(N,ct),ct+=N.length;for(let L=0,Dt=F.length;L<Dt;L++){let ot=F[L];_t(ot,ct),ct+=ot.length}n.addGroup(nt,s.length/3-nt,1)}function _t(nt,ct){let L=nt.length;for(;--L>=0;){let Dt=L,ot=L-1;ot<0&&(ot=nt.length-1);for(let bt=0,ft=u+y*2;bt<ft;bt++){let zt=z*bt,Tt=z*(bt+1),P=ct+Dt+zt,A=ct+ot+zt,W=ct+ot+Tt,j=ct+Dt+Tt;Xt(P,A,W,j)}}}function rt(nt,ct,L){l.push(nt),l.push(ct),l.push(L)}function Nt(nt,ct,L){Ht(nt),Ht(ct),Ht(L);let Dt=s.length/3,ot=_.generateTopUV(n,s,Dt-3,Dt-2,Dt-1);ne(ot[0]),ne(ot[1]),ne(ot[2])}function Xt(nt,ct,L,Dt){Ht(nt),Ht(ct),Ht(Dt),Ht(ct),Ht(L),Ht(Dt);let ot=s.length/3,bt=_.generateSideWallUV(n,s,ot-6,ot-3,ot-2,ot-1);ne(bt[0]),ne(bt[1]),ne(bt[3]),ne(bt[1]),ne(bt[2]),ne(bt[3])}function Ht(nt){s.push(l[nt*3+0]),s.push(l[nt*3+1]),s.push(l[nt*3+2])}function ne(nt){r.push(nt.x),r.push(nt.y)}}}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}toJSON(){let t=super.toJSON(),e=this.parameters.shapes,n=this.parameters.options;return Py(e,n,t)}static fromJSON(t,e){let n=[];for(let r=0,o=t.shapes.length;r<o;r++){let a=e[t.shapes[r]];n.push(a)}let s=t.options.extrudePath;return s!==void 0&&(t.options.extrudePath=new yc[s.type]().fromJSON(s)),new i(n,t.options)}},Iy={generateTopUV:function(i,t,e,n,s){let r=t[e*3],o=t[e*3+1],a=t[n*3],l=t[n*3+1],h=t[s*3],u=t[s*3+1];return[new lt(r,o),new lt(a,l),new lt(h,u)]},generateSideWallUV:function(i,t,e,n,s,r){let o=t[e*3],a=t[e*3+1],l=t[e*3+2],h=t[n*3],u=t[n*3+1],m=t[n*3+2],x=t[s*3],f=t[s*3+1],v=t[s*3+2],d=t[r*3],y=t[r*3+1],c=t[r*3+2];return Math.abs(a-u)<Math.abs(o-h)?[new lt(o,1-l),new lt(h,1-m),new lt(x,1-v),new lt(d,1-c)]:[new lt(a,1-l),new lt(u,1-m),new lt(f,1-v),new lt(y,1-c)]}};function Py(i,t,e){if(e.shapes=[],Array.isArray(i))for(let n=0,s=i.length;n<s;n++){let r=i[n];e.shapes.push(r.uuid)}else e.shapes.push(i.uuid);return e.options=Object.assign({},t),t.extrudePath!==void 0&&(e.options.extrudePath=t.extrudePath.toJSON()),e}var $o=class i extends Yo{constructor(t=1,e=0){let n=(1+Math.sqrt(5))/2,s=[-1,n,0,1,n,0,-1,-n,0,1,-n,0,0,-1,n,0,1,n,0,-1,-n,0,1,-n,n,0,-1,n,0,1,-n,0,-1,-n,0,1],r=[0,11,5,0,5,1,0,1,7,0,7,10,0,10,11,1,5,9,5,11,4,11,10,2,10,7,6,7,1,8,3,9,4,3,4,2,3,2,6,3,6,8,3,8,9,4,9,5,2,4,11,6,2,10,8,6,7,9,8,1];super(s,r,t,e),this.type="IcosahedronGeometry",this.parameters={radius:t,detail:e}}static fromJSON(t){return new i(t.radius,t.detail)}},Jo=class i extends Yo{constructor(t=1,e=0){let n=[1,0,0,-1,0,0,0,1,0,0,-1,0,0,0,1,0,0,-1],s=[0,2,4,0,4,3,0,3,5,0,5,2,1,2,5,1,5,3,1,3,4,1,4,2];super(n,s,t,e),this.type="OctahedronGeometry",this.parameters={radius:t,detail:e}}static fromJSON(t){return new i(t.radius,t.detail)}},ni=class i extends de{constructor(t=.5,e=1,n=32,s=1,r=0,o=Math.PI*2){super(),this.type="RingGeometry",this.parameters={innerRadius:t,outerRadius:e,thetaSegments:n,phiSegments:s,thetaStart:r,thetaLength:o},n=Math.max(3,n),s=Math.max(1,s);let a=[],l=[],h=[],u=[],m=t,x=(e-t)/s,f=new B,v=new lt;for(let d=0;d<=s;d++){for(let y=0;y<=n;y++){let c=r+y/n*o;f.x=m*Math.cos(c),f.y=m*Math.sin(c),l.push(f.x,f.y,f.z),h.push(0,0,1),v.x=(f.x/e+1)/2,v.y=(f.y/e+1)/2,u.push(v.x,v.y)}m+=x}for(let d=0;d<s;d++){let y=d*(n+1);for(let c=0;c<n;c++){let _=c+y,p=_,g=_+n+1,T=_+n+2,S=_+1;a.push(p,g,S),a.push(g,T,S)}}this.setIndex(a),this.setAttribute("position",new Wt(l,3)),this.setAttribute("normal",new Wt(h,3)),this.setAttribute("uv",new Wt(u,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.innerRadius,t.outerRadius,t.thetaSegments,t.phiSegments,t.thetaStart,t.thetaLength)}};var ae=class i extends de{constructor(t=1,e=32,n=16,s=0,r=Math.PI*2,o=0,a=Math.PI){super(),this.type="SphereGeometry",this.parameters={radius:t,widthSegments:e,heightSegments:n,phiStart:s,phiLength:r,thetaStart:o,thetaLength:a},e=Math.max(3,Math.floor(e)),n=Math.max(2,Math.floor(n));let l=Math.min(o+a,Math.PI),h=0,u=[],m=new B,x=new B,f=[],v=[],d=[],y=[];for(let c=0;c<=n;c++){let _=[],p=c/n,g=0;c===0&&o===0?g=.5/e:c===n&&l===Math.PI&&(g=-.5/e);for(let T=0;T<=e;T++){let S=T/e;m.x=-t*Math.cos(s+S*r)*Math.sin(o+p*a),m.y=t*Math.cos(o+p*a),m.z=t*Math.sin(s+S*r)*Math.sin(o+p*a),v.push(m.x,m.y,m.z),x.copy(m).normalize(),d.push(x.x,x.y,x.z),y.push(S+g,1-p),_.push(h++)}u.push(_)}for(let c=0;c<n;c++)for(let _=0;_<e;_++){let p=u[c][_+1],g=u[c][_],T=u[c+1][_],S=u[c+1][_+1];(c!==0||o>0)&&f.push(p,g,S),(c!==n-1||l<Math.PI)&&f.push(g,T,S)}this.setIndex(f),this.setAttribute("position",new Wt(v,3)),this.setAttribute("normal",new Wt(d,3)),this.setAttribute("uv",new Wt(y,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.radius,t.widthSegments,t.heightSegments,t.phiStart,t.phiLength,t.thetaStart,t.thetaLength)}};var Be=class i extends de{constructor(t=1,e=.4,n=12,s=48,r=Math.PI*2){super(),this.type="TorusGeometry",this.parameters={radius:t,tube:e,radialSegments:n,tubularSegments:s,arc:r},n=Math.floor(n),s=Math.floor(s);let o=[],a=[],l=[],h=[],u=new B,m=new B,x=new B;for(let f=0;f<=n;f++)for(let v=0;v<=s;v++){let d=v/s*r,y=f/n*Math.PI*2;m.x=(t+e*Math.cos(y))*Math.cos(d),m.y=(t+e*Math.cos(y))*Math.sin(d),m.z=e*Math.sin(y),a.push(m.x,m.y,m.z),u.x=t*Math.cos(d),u.y=t*Math.sin(d),x.subVectors(m,u).normalize(),l.push(x.x,x.y,x.z),h.push(v/s),h.push(f/n)}for(let f=1;f<=n;f++)for(let v=1;v<=s;v++){let d=(s+1)*f+v-1,y=(s+1)*(f-1)+v-1,c=(s+1)*(f-1)+v,_=(s+1)*f+v;o.push(d,y,_),o.push(y,c,_)}this.setIndex(o),this.setAttribute("position",new Wt(a,3)),this.setAttribute("normal",new Wt(l,3)),this.setAttribute("uv",new Wt(h,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.radius,t.tube,t.radialSegments,t.tubularSegments,t.arc)}};var Bt=class extends Un{static get type(){return"MeshLambertMaterial"}constructor(t){super(),this.isMeshLambertMaterial=!0,this.color=new Jt(16777215),this.map=null,this.lightMap=null,this.lightMapIntensity=1,this.aoMap=null,this.aoMapIntensity=1,this.emissive=new Jt(0),this.emissiveIntensity=1,this.emissiveMap=null,this.bumpMap=null,this.bumpScale=1,this.normalMap=null,this.normalMapType=sf,this.normalScale=new lt(1,1),this.displacementMap=null,this.displacementScale=1,this.displacementBias=0,this.specularMap=null,this.alphaMap=null,this.envMap=null,this.envMapRotation=new sn,this.combine=Uc,this.reflectivity=1,this.refractionRatio=.98,this.wireframe=!1,this.wireframeLinewidth=1,this.wireframeLinecap="round",this.wireframeLinejoin="round",this.flatShading=!1,this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.lightMap=t.lightMap,this.lightMapIntensity=t.lightMapIntensity,this.aoMap=t.aoMap,this.aoMapIntensity=t.aoMapIntensity,this.emissive.copy(t.emissive),this.emissiveMap=t.emissiveMap,this.emissiveIntensity=t.emissiveIntensity,this.bumpMap=t.bumpMap,this.bumpScale=t.bumpScale,this.normalMap=t.normalMap,this.normalMapType=t.normalMapType,this.normalScale.copy(t.normalScale),this.displacementMap=t.displacementMap,this.displacementScale=t.displacementScale,this.displacementBias=t.displacementBias,this.specularMap=t.specularMap,this.alphaMap=t.alphaMap,this.envMap=t.envMap,this.envMapRotation.copy(t.envMapRotation),this.combine=t.combine,this.reflectivity=t.reflectivity,this.refractionRatio=t.refractionRatio,this.wireframe=t.wireframe,this.wireframeLinewidth=t.wireframeLinewidth,this.wireframeLinecap=t.wireframeLinecap,this.wireframeLinejoin=t.wireframeLinejoin,this.flatShading=t.flatShading,this.fog=t.fog,this}};function yo(i,t,e){return!i||!e&&i.constructor===t?i:typeof t.BYTES_PER_ELEMENT=="number"?new t(i):Array.prototype.slice.call(i)}function Ly(i){return ArrayBuffer.isView(i)&&!(i instanceof DataView)}var Ds=class{constructor(t,e,n,s){this.parameterPositions=t,this._cachedIndex=0,this.resultBuffer=s!==void 0?s:new e.constructor(n),this.sampleValues=e,this.valueSize=n,this.settings=null,this.DefaultSettings_={}}evaluate(t){let e=this.parameterPositions,n=this._cachedIndex,s=e[n],r=e[n-1];n:{t:{let o;e:{i:if(!(t<s)){for(let a=n+2;;){if(s===void 0){if(t<r)break i;return n=e.length,this._cachedIndex=n,this.copySampleValue_(n-1)}if(n===a)break;if(r=s,s=e[++n],t<s)break t}o=e.length;break e}if(!(t>=r)){let a=e[1];t<a&&(n=2,r=a);for(let l=n-2;;){if(r===void 0)return this._cachedIndex=0,this.copySampleValue_(0);if(n===l)break;if(s=r,r=e[--n-1],t>=r)break t}o=n,n=0;break e}break n}for(;n<o;){let a=n+o>>>1;t<e[a]?o=a:n=a+1}if(s=e[n],r=e[n-1],r===void 0)return this._cachedIndex=0,this.copySampleValue_(0);if(s===void 0)return n=e.length,this._cachedIndex=n,this.copySampleValue_(n-1)}this._cachedIndex=n,this.intervalChanged_(n,r,s)}return this.interpolate_(n,r,t,s)}getSettings_(){return this.settings||this.DefaultSettings_}copySampleValue_(t){let e=this.resultBuffer,n=this.sampleValues,s=this.valueSize,r=t*s;for(let o=0;o!==s;++o)e[o]=n[r+o];return e}interpolate_(){throw new Error("call to abstract method")}intervalChanged_(){}},Sc=class extends Ds{constructor(t,e,n,s){super(t,e,n,s),this._weightPrev=-0,this._offsetPrev=-0,this._weightNext=-0,this._offsetNext=-0,this.DefaultSettings_={endingStart:Uh,endingEnd:Uh}}intervalChanged_(t,e,n){let s=this.parameterPositions,r=t-2,o=t+1,a=s[r],l=s[o];if(a===void 0)switch(this.getSettings_().endingStart){case Nh:r=t,a=2*e-n;break;case Bh:r=s.length-2,a=e+s[r]-s[r+1];break;default:r=t,a=n}if(l===void 0)switch(this.getSettings_().endingEnd){case Nh:o=t,l=2*n-e;break;case Bh:o=1,l=n+s[1]-s[0];break;default:o=t-1,l=e}let h=(n-e)*.5,u=this.valueSize;this._weightPrev=h/(e-a),this._weightNext=h/(l-n),this._offsetPrev=r*u,this._offsetNext=o*u}interpolate_(t,e,n,s){let r=this.resultBuffer,o=this.sampleValues,a=this.valueSize,l=t*a,h=l-a,u=this._offsetPrev,m=this._offsetNext,x=this._weightPrev,f=this._weightNext,v=(n-e)/(s-e),d=v*v,y=d*v,c=-x*y+2*x*d-x*v,_=(1+x)*y+(-1.5-2*x)*d+(-.5+x)*v+1,p=(-1-f)*y+(1.5+f)*d+.5*v,g=f*y-f*d;for(let T=0;T!==a;++T)r[T]=c*o[u+T]+_*o[h+T]+p*o[l+T]+g*o[m+T];return r}},Tc=class extends Ds{constructor(t,e,n,s){super(t,e,n,s)}interpolate_(t,e,n,s){let r=this.resultBuffer,o=this.sampleValues,a=this.valueSize,l=t*a,h=l-a,u=(n-e)/(s-e),m=1-u;for(let x=0;x!==a;++x)r[x]=o[h+x]*m+o[l+x]*u;return r}},wc=class extends Ds{constructor(t,e,n,s){super(t,e,n,s)}interpolate_(t){return this.copySampleValue_(t-1)}},An=class{constructor(t,e,n,s){if(t===void 0)throw new Error("THREE.KeyframeTrack: track name is undefined");if(e===void 0||e.length===0)throw new Error("THREE.KeyframeTrack: no keyframes in track named "+t);this.name=t,this.times=yo(e,this.TimeBufferType),this.values=yo(n,this.ValueBufferType),this.setInterpolation(s||this.DefaultInterpolation)}static toJSON(t){let e=t.constructor,n;if(e.toJSON!==this.toJSON)n=e.toJSON(t);else{n={name:t.name,times:yo(t.times,Array),values:yo(t.values,Array)};let s=t.getInterpolation();s!==t.DefaultInterpolation&&(n.interpolation=s)}return n.type=t.ValueTypeName,n}InterpolantFactoryMethodDiscrete(t){return new wc(this.times,this.values,this.getValueSize(),t)}InterpolantFactoryMethodLinear(t){return new Tc(this.times,this.values,this.getValueSize(),t)}InterpolantFactoryMethodSmooth(t){return new Sc(this.times,this.values,this.getValueSize(),t)}setInterpolation(t){let e;switch(t){case To:e=this.InterpolantFactoryMethodDiscrete;break;case Wl:e=this.InterpolantFactoryMethodLinear;break;case wa:e=this.InterpolantFactoryMethodSmooth;break}if(e===void 0){let n="unsupported interpolation for "+this.ValueTypeName+" keyframe track named "+this.name;if(this.createInterpolant===void 0)if(t!==this.DefaultInterpolation)this.setInterpolation(this.DefaultInterpolation);else throw new Error(n);return console.warn("THREE.KeyframeTrack:",n),this}return this.createInterpolant=e,this}getInterpolation(){switch(this.createInterpolant){case this.InterpolantFactoryMethodDiscrete:return To;case this.InterpolantFactoryMethodLinear:return Wl;case this.InterpolantFactoryMethodSmooth:return wa}}getValueSize(){return this.values.length/this.times.length}shift(t){if(t!==0){let e=this.times;for(let n=0,s=e.length;n!==s;++n)e[n]+=t}return this}scale(t){if(t!==1){let e=this.times;for(let n=0,s=e.length;n!==s;++n)e[n]*=t}return this}trim(t,e){let n=this.times,s=n.length,r=0,o=s-1;for(;r!==s&&n[r]<t;)++r;for(;o!==-1&&n[o]>e;)--o;if(++o,r!==0||o!==s){r>=o&&(o=Math.max(o,1),r=o-1);let a=this.getValueSize();this.times=n.slice(r,o),this.values=this.values.slice(r*a,o*a)}return this}validate(){let t=!0,e=this.getValueSize();e-Math.floor(e)!==0&&(console.error("THREE.KeyframeTrack: Invalid value size in track.",this),t=!1);let n=this.times,s=this.values,r=n.length;r===0&&(console.error("THREE.KeyframeTrack: Track is empty.",this),t=!1);let o=null;for(let a=0;a!==r;a++){let l=n[a];if(typeof l=="number"&&isNaN(l)){console.error("THREE.KeyframeTrack: Time is not a valid number.",this,a,l),t=!1;break}if(o!==null&&o>l){console.error("THREE.KeyframeTrack: Out of order keys.",this,a,l,o),t=!1;break}o=l}if(s!==void 0&&Ly(s))for(let a=0,l=s.length;a!==l;++a){let h=s[a];if(isNaN(h)){console.error("THREE.KeyframeTrack: Value is not a valid number.",this,a,h),t=!1;break}}return t}optimize(){let t=this.times.slice(),e=this.values.slice(),n=this.getValueSize(),s=this.getInterpolation()===wa,r=t.length-1,o=1;for(let a=1;a<r;++a){let l=!1,h=t[a],u=t[a+1];if(h!==u&&(a!==1||h!==t[0]))if(s)l=!0;else{let m=a*n,x=m-n,f=m+n;for(let v=0;v!==n;++v){let d=e[m+v];if(d!==e[x+v]||d!==e[f+v]){l=!0;break}}}if(l){if(a!==o){t[o]=t[a];let m=a*n,x=o*n;for(let f=0;f!==n;++f)e[x+f]=e[m+f]}++o}}if(r>0){t[o]=t[r];for(let a=r*n,l=o*n,h=0;h!==n;++h)e[l+h]=e[a+h];++o}return o!==t.length?(this.times=t.slice(0,o),this.values=e.slice(0,o*n)):(this.times=t,this.values=e),this}clone(){let t=this.times.slice(),e=this.values.slice(),n=this.constructor,s=new n(this.name,t,e);return s.createInterpolant=this.createInterpolant,s}};An.prototype.TimeBufferType=Float32Array;An.prototype.ValueBufferType=Float32Array;An.prototype.DefaultInterpolation=Wl;var Xi=class extends An{constructor(t,e,n){super(t,e,n)}};Xi.prototype.ValueTypeName="bool";Xi.prototype.ValueBufferType=Array;Xi.prototype.DefaultInterpolation=To;Xi.prototype.InterpolantFactoryMethodLinear=void 0;Xi.prototype.InterpolantFactoryMethodSmooth=void 0;var bc=class extends An{};bc.prototype.ValueTypeName="color";var Ac=class extends An{};Ac.prototype.ValueTypeName="number";var Rc=class extends Ds{constructor(t,e,n,s){super(t,e,n,s)}interpolate_(t,e,n,s){let r=this.resultBuffer,o=this.sampleValues,a=this.valueSize,l=(n-e)/(s-e),h=t*a;for(let u=h+a;h!==u;h+=4)gn.slerpFlat(r,0,o,h-a,o,h,l);return r}},Ko=class extends An{InterpolantFactoryMethodLinear(t){return new Rc(this.times,this.values,this.getValueSize(),t)}};Ko.prototype.ValueTypeName="quaternion";Ko.prototype.InterpolantFactoryMethodSmooth=void 0;var qi=class extends An{constructor(t,e,n){super(t,e,n)}};qi.prototype.ValueTypeName="string";qi.prototype.ValueBufferType=Array;qi.prototype.DefaultInterpolation=To;qi.prototype.InterpolantFactoryMethodLinear=void 0;qi.prototype.InterpolantFactoryMethodSmooth=void 0;var Cc=class extends An{};Cc.prototype.ValueTypeName="vector";var Fc=class{constructor(t,e,n){let s=this,r=!1,o=0,a=0,l,h=[];this.onStart=void 0,this.onLoad=t,this.onProgress=e,this.onError=n,this.itemStart=function(u){a++,r===!1&&s.onStart!==void 0&&s.onStart(u,o,a),r=!0},this.itemEnd=function(u){o++,s.onProgress!==void 0&&s.onProgress(u,o,a),o===a&&(r=!1,s.onLoad!==void 0&&s.onLoad())},this.itemError=function(u){s.onError!==void 0&&s.onError(u)},this.resolveURL=function(u){return l?l(u):u},this.setURLModifier=function(u){return l=u,this},this.addHandler=function(u,m){return h.push(u,m),this},this.removeHandler=function(u){let m=h.indexOf(u);return m!==-1&&h.splice(m,2),this},this.getHandler=function(u){for(let m=0,x=h.length;m<x;m+=2){let f=h[m],v=h[m+1];if(f.global&&(f.lastIndex=0),f.test(u))return v}return null}}},Dy=new Fc,Ic=class{constructor(t){this.manager=t!==void 0?t:Dy,this.crossOrigin="anonymous",this.withCredentials=!1,this.path="",this.resourcePath="",this.requestHeader={}}load(){}loadAsync(t,e){let n=this;return new Promise(function(s,r){n.load(t,s,e,r)})}parse(){}setCrossOrigin(t){return this.crossOrigin=t,this}setWithCredentials(t){return this.withCredentials=t,this}setPath(t){return this.path=t,this}setResourcePath(t){return this.resourcePath=t,this}setRequestHeader(t){return this.requestHeader=t,this}};Ic.DEFAULT_MATERIAL_NAME="__DEFAULT";var jo=class extends We{constructor(t,e=1){super(),this.isLight=!0,this.type="Light",this.color=new Jt(t),this.intensity=e}dispose(){}copy(t,e){return super.copy(t,e),this.color.copy(t.color),this.intensity=t.intensity,this}toJSON(t){let e=super.toJSON(t);return e.object.color=this.color.getHex(),e.object.intensity=this.intensity,this.groundColor!==void 0&&(e.object.groundColor=this.groundColor.getHex()),this.distance!==void 0&&(e.object.distance=this.distance),this.angle!==void 0&&(e.object.angle=this.angle),this.decay!==void 0&&(e.object.decay=this.decay),this.penumbra!==void 0&&(e.object.penumbra=this.penumbra),this.shadow!==void 0&&(e.object.shadow=this.shadow.toJSON()),this.target!==void 0&&(e.object.target=this.target.uuid),e}},br=class extends jo{constructor(t,e,n){super(t,n),this.isHemisphereLight=!0,this.type="HemisphereLight",this.position.copy(We.DEFAULT_UP),this.updateMatrix(),this.groundColor=new Jt(e)}copy(t,e){return super.copy(t,e),this.groundColor.copy(t.groundColor),this}},rl=new ee,Wu=new B,Xu=new B,Pc=class{constructor(t){this.camera=t,this.intensity=1,this.bias=0,this.normalBias=0,this.radius=1,this.blurSamples=8,this.mapSize=new lt(512,512),this.map=null,this.mapPass=null,this.matrix=new ee,this.autoUpdate=!0,this.needsUpdate=!1,this._frustum=new pr,this._frameExtents=new lt(1,1),this._viewportCount=1,this._viewports=[new Ue(0,0,1,1)]}getViewportCount(){return this._viewportCount}getFrustum(){return this._frustum}updateMatrices(t){let e=this.camera,n=this.matrix;Wu.setFromMatrixPosition(t.matrixWorld),e.position.copy(Wu),Xu.setFromMatrixPosition(t.target.matrixWorld),e.lookAt(Xu),e.updateMatrixWorld(),rl.multiplyMatrices(e.projectionMatrix,e.matrixWorldInverse),this._frustum.setFromProjectionMatrix(rl),n.set(.5,0,0,.5,0,.5,0,.5,0,0,.5,.5,0,0,0,1),n.multiply(rl)}getViewport(t){return this._viewports[t]}getFrameExtents(){return this._frameExtents}dispose(){this.map&&this.map.dispose(),this.mapPass&&this.mapPass.dispose()}copy(t){return this.camera=t.camera.clone(),this.intensity=t.intensity,this.bias=t.bias,this.radius=t.radius,this.mapSize.copy(t.mapSize),this}clone(){return new this.constructor().copy(this)}toJSON(){let t={};return this.intensity!==1&&(t.intensity=this.intensity),this.bias!==0&&(t.bias=this.bias),this.normalBias!==0&&(t.normalBias=this.normalBias),this.radius!==1&&(t.radius=this.radius),(this.mapSize.x!==512||this.mapSize.y!==512)&&(t.mapSize=this.mapSize.toArray()),t.camera=this.camera.toJSON(!1).object,delete t.camera.matrix,t}};var Lc=class extends Pc{constructor(){super(new Do(-5,5,5,-5,.5,500)),this.isDirectionalLightShadow=!0}},Ar=class extends jo{constructor(t,e){super(t,e),this.isDirectionalLight=!0,this.type="DirectionalLight",this.position.copy(We.DEFAULT_UP),this.updateMatrix(),this.target=new We,this.shadow=new Lc}dispose(){this.shadow.dispose()}copy(t){return super.copy(t),this.target=t.target.clone(),this.shadow=t.shadow.clone(),this}};var Xc="\\[\\]\\.:\\/",Uy=new RegExp("["+Xc+"]","g"),qc="[^"+Xc+"]",Ny="[^"+Xc.replace("\\.","")+"]",By=/((?:WC+[\/:])*)/.source.replace("WC",qc),zy=/(WCOD+)?/.source.replace("WCOD",Ny),Oy=/(?:\.(WC+)(?:\[(.+)\])?)?/.source.replace("WC",qc),Hy=/\.(WC+)(?:\[(.+)\])?/.source.replace("WC",qc),ky=new RegExp("^"+By+zy+Oy+Hy+"$"),Gy=["material","materials","bones","map"],Dc=class{constructor(t,e,n){let s=n||Fe.parseTrackName(e);this._targetGroup=t,this._bindings=t.subscribe_(e,s)}getValue(t,e){this.bind();let n=this._targetGroup.nCachedObjects_,s=this._bindings[n];s!==void 0&&s.getValue(t,e)}setValue(t,e){let n=this._bindings;for(let s=this._targetGroup.nCachedObjects_,r=n.length;s!==r;++s)n[s].setValue(t,e)}bind(){let t=this._bindings;for(let e=this._targetGroup.nCachedObjects_,n=t.length;e!==n;++e)t[e].bind()}unbind(){let t=this._bindings;for(let e=this._targetGroup.nCachedObjects_,n=t.length;e!==n;++e)t[e].unbind()}},Fe=class i{constructor(t,e,n){this.path=e,this.parsedPath=n||i.parseTrackName(e),this.node=i.findNode(t,this.parsedPath.nodeName),this.rootNode=t,this.getValue=this._getValue_unbound,this.setValue=this._setValue_unbound}static create(t,e,n){return t&&t.isAnimationObjectGroup?new i.Composite(t,e,n):new i(t,e,n)}static sanitizeNodeName(t){return t.replace(/\s/g,"_").replace(Uy,"")}static parseTrackName(t){let e=ky.exec(t);if(e===null)throw new Error("PropertyBinding: Cannot parse trackName: "+t);let n={nodeName:e[2],objectName:e[3],objectIndex:e[4],propertyName:e[5],propertyIndex:e[6]},s=n.nodeName&&n.nodeName.lastIndexOf(".");if(s!==void 0&&s!==-1){let r=n.nodeName.substring(s+1);Gy.indexOf(r)!==-1&&(n.nodeName=n.nodeName.substring(0,s),n.objectName=r)}if(n.propertyName===null||n.propertyName.length===0)throw new Error("PropertyBinding: can not parse propertyName from trackName: "+t);return n}static findNode(t,e){if(e===void 0||e===""||e==="."||e===-1||e===t.name||e===t.uuid)return t;if(t.skeleton){let n=t.skeleton.getBoneByName(e);if(n!==void 0)return n}if(t.children){let n=function(r){for(let o=0;o<r.length;o++){let a=r[o];if(a.name===e||a.uuid===e)return a;let l=n(a.children);if(l)return l}return null},s=n(t.children);if(s)return s}return null}_getValue_unavailable(){}_setValue_unavailable(){}_getValue_direct(t,e){t[e]=this.targetObject[this.propertyName]}_getValue_array(t,e){let n=this.resolvedProperty;for(let s=0,r=n.length;s!==r;++s)t[e++]=n[s]}_getValue_arrayElement(t,e){t[e]=this.resolvedProperty[this.propertyIndex]}_getValue_toArray(t,e){this.resolvedProperty.toArray(t,e)}_setValue_direct(t,e){this.targetObject[this.propertyName]=t[e]}_setValue_direct_setNeedsUpdate(t,e){this.targetObject[this.propertyName]=t[e],this.targetObject.needsUpdate=!0}_setValue_direct_setMatrixWorldNeedsUpdate(t,e){this.targetObject[this.propertyName]=t[e],this.targetObject.matrixWorldNeedsUpdate=!0}_setValue_array(t,e){let n=this.resolvedProperty;for(let s=0,r=n.length;s!==r;++s)n[s]=t[e++]}_setValue_array_setNeedsUpdate(t,e){let n=this.resolvedProperty;for(let s=0,r=n.length;s!==r;++s)n[s]=t[e++];this.targetObject.needsUpdate=!0}_setValue_array_setMatrixWorldNeedsUpdate(t,e){let n=this.resolvedProperty;for(let s=0,r=n.length;s!==r;++s)n[s]=t[e++];this.targetObject.matrixWorldNeedsUpdate=!0}_setValue_arrayElement(t,e){this.resolvedProperty[this.propertyIndex]=t[e]}_setValue_arrayElement_setNeedsUpdate(t,e){this.resolvedProperty[this.propertyIndex]=t[e],this.targetObject.needsUpdate=!0}_setValue_arrayElement_setMatrixWorldNeedsUpdate(t,e){this.resolvedProperty[this.propertyIndex]=t[e],this.targetObject.matrixWorldNeedsUpdate=!0}_setValue_fromArray(t,e){this.resolvedProperty.fromArray(t,e)}_setValue_fromArray_setNeedsUpdate(t,e){this.resolvedProperty.fromArray(t,e),this.targetObject.needsUpdate=!0}_setValue_fromArray_setMatrixWorldNeedsUpdate(t,e){this.resolvedProperty.fromArray(t,e),this.targetObject.matrixWorldNeedsUpdate=!0}_getValue_unbound(t,e){this.bind(),this.getValue(t,e)}_setValue_unbound(t,e){this.bind(),this.setValue(t,e)}bind(){let t=this.node,e=this.parsedPath,n=e.objectName,s=e.propertyName,r=e.propertyIndex;if(t||(t=i.findNode(this.rootNode,e.nodeName),this.node=t),this.getValue=this._getValue_unavailable,this.setValue=this._setValue_unavailable,!t){console.warn("THREE.PropertyBinding: No target node found for track: "+this.path+".");return}if(n){let h=e.objectIndex;switch(n){case"materials":if(!t.material){console.error("THREE.PropertyBinding: Can not bind to material as node does not have a material.",this);return}if(!t.material.materials){console.error("THREE.PropertyBinding: Can not bind to material.materials as node.material does not have a materials array.",this);return}t=t.material.materials;break;case"bones":if(!t.skeleton){console.error("THREE.PropertyBinding: Can not bind to bones as node does not have a skeleton.",this);return}t=t.skeleton.bones;for(let u=0;u<t.length;u++)if(t[u].name===h){h=u;break}break;case"map":if("map"in t){t=t.map;break}if(!t.material){console.error("THREE.PropertyBinding: Can not bind to material as node does not have a material.",this);return}if(!t.material.map){console.error("THREE.PropertyBinding: Can not bind to material.map as node.material does not have a map.",this);return}t=t.material.map;break;default:if(t[n]===void 0){console.error("THREE.PropertyBinding: Can not bind to objectName of node undefined.",this);return}t=t[n]}if(h!==void 0){if(t[h]===void 0){console.error("THREE.PropertyBinding: Trying to bind to objectIndex of objectName, but is undefined.",this,t);return}t=t[h]}}let o=t[s];if(o===void 0){let h=e.nodeName;console.error("THREE.PropertyBinding: Trying to update property for track: "+h+"."+s+" but it wasn't found.",t);return}let a=this.Versioning.None;this.targetObject=t,t.needsUpdate!==void 0?a=this.Versioning.NeedsUpdate:t.matrixWorldNeedsUpdate!==void 0&&(a=this.Versioning.MatrixWorldNeedsUpdate);let l=this.BindingType.Direct;if(r!==void 0){if(s==="morphTargetInfluences"){if(!t.geometry){console.error("THREE.PropertyBinding: Can not bind to morphTargetInfluences because node does not have a geometry.",this);return}if(!t.geometry.morphAttributes){console.error("THREE.PropertyBinding: Can not bind to morphTargetInfluences because node does not have a geometry.morphAttributes.",this);return}t.morphTargetDictionary[r]!==void 0&&(r=t.morphTargetDictionary[r])}l=this.BindingType.ArrayElement,this.resolvedProperty=o,this.propertyIndex=r}else o.fromArray!==void 0&&o.toArray!==void 0?(l=this.BindingType.HasFromToArray,this.resolvedProperty=o):Array.isArray(o)?(l=this.BindingType.EntireArray,this.resolvedProperty=o):this.propertyName=s;this.getValue=this.GetterByBindingType[l],this.setValue=this.SetterByBindingTypeAndVersioning[l][a]}unbind(){this.node=null,this.getValue=this._getValue_unbound,this.setValue=this._setValue_unbound}};Fe.Composite=Dc;Fe.prototype.BindingType={Direct:0,EntireArray:1,ArrayElement:2,HasFromToArray:3};Fe.prototype.Versioning={None:0,NeedsUpdate:1,MatrixWorldNeedsUpdate:2};Fe.prototype.GetterByBindingType=[Fe.prototype._getValue_direct,Fe.prototype._getValue_array,Fe.prototype._getValue_arrayElement,Fe.prototype._getValue_toArray];Fe.prototype.SetterByBindingTypeAndVersioning=[[Fe.prototype._setValue_direct,Fe.prototype._setValue_direct_setNeedsUpdate,Fe.prototype._setValue_direct_setMatrixWorldNeedsUpdate],[Fe.prototype._setValue_array,Fe.prototype._setValue_array_setNeedsUpdate,Fe.prototype._setValue_array_setMatrixWorldNeedsUpdate],[Fe.prototype._setValue_arrayElement,Fe.prototype._setValue_arrayElement_setNeedsUpdate,Fe.prototype._setValue_arrayElement_setMatrixWorldNeedsUpdate],[Fe.prototype._setValue_fromArray,Fe.prototype._setValue_fromArray_setNeedsUpdate,Fe.prototype._setValue_fromArray_setMatrixWorldNeedsUpdate]];var G_=new Float32Array(1);typeof __THREE_DEVTOOLS__<"u"&&__THREE_DEVTOOLS__.dispatchEvent(new CustomEvent("register",{detail:{revision:"170"}}));typeof window<"u"&&(window.__THREE__?console.warn("WARNING: Multiple instances of Three.js being imported."):window.__THREE__="170");var ii=[{name:"\uBAB8\uD480\uAE30",len:117.7,y0:0,y1:8,par:25e3,min:9e3},{name:"\uBC00\uCE58\uAE30",len:100,y0:8,y1:14,par:24e3,min:8500},{name:"\uD754\uB4E4 \uB2E4\uB9AC",len:100,y0:14,y1:20,par:22e3,min:8e3},{name:"\uBB34\uB108\uC9C0\uB294 \uAE38",len:103.4,y0:20,y1:24,par:22e3,min:7500},{name:"\uC815\uC0C1",len:88,y0:24,y1:34.5,par:26e3,min:9e3}],Vy=[1e3,600,300],Zc=480;function ia(i,t,e,n,s,r,o){let a=i*1e3;for(let m=0;m<i;m++)a+=Math.max(0,Math.floor((ii[m].par*1.6-t[m])/100));let l=Math.min(i,ii.length-1),h=ii[l],u=n?ii[ii.length-1].y1:h.y0+Math.min(1,Math.max(0,e/h.len))*(h.y1-h.y0);return a+=Math.floor(u*10),n&&(a+=(Vy[s-1]||0)+(r?0:500)),a+Math.min(Math.max(o,0),20)*50}var Bs=i=>[(i>>16&255)/255,(i>>8&255)/255,(i&255)/255],Wy=`attribute float size; attribute vec4 rgba; attribute float shape; uniform float uScale;
varying vec4 vC; varying float vS;
void main(){ vC = rgba; vS = shape; vec4 mv = modelViewMatrix * vec4(position, 1.0);
  gl_PointSize = size * uScale / max(0.1, -mv.z); gl_Position = projectionMatrix * mv; }`,Xy=`varying vec4 vC; varying float vS;
void main(){ vec2 d = gl_PointCoord - 0.5; float r = length(d); float a;
  if (vS < 0.5) { if (r > 0.5) discard; a = smoothstep(0.5, 0.0, r); a *= a * 1.6; }
  else if (vS < 1.5) { if (max(abs(d.x), abs(d.y)) > 0.36) discard; a = 1.0; }
  else if (vS < 2.5) { float ang = atan(d.y, d.x); float k = 0.22 + 0.2 * pow(abs(cos(ang * 2.5)), 6.0); if (r > k + 0.06) discard; a = r < k ? 1.0 : 0.5; }
  else { if (r > 0.5) discard; a = r > 0.38 ? 1.0 : 0.75; }
  gl_FragColor = vec4(vC.rgb, vC.a * min(1.0, a)); }`,ra=class{constructor(t,e,n){this.n=e,this.next=0,this.live=0,this.pos=new Float32Array(e*3),this.col=new Float32Array(e*4),this.size=new Float32Array(e),this.shape=new Float32Array(e),this.vel=new Float32Array(e*3),this.life=new Float32Array(e),this.max=new Float32Array(e),this.s0=new Float32Array(e),this.s1=new Float32Array(e),this.grav=new Float32Array(e),this.drag=new Float32Array(e),this.a0=new Float32Array(e),this.base=new Float32Array(e*3);for(let r=0;r<e;r++)this.pos[r*3+1]=-9999;let s=new de;s.setAttribute("position",new we(this.pos,3)),s.setAttribute("rgba",new we(this.col,4)),s.setAttribute("size",new we(this.size,1)),s.setAttribute("shape",new we(this.shape,1)),this.geo=s,this.mat=new xn({uniforms:{uScale:{value:400}},vertexShader:Wy,fragmentShader:Xy,transparent:!0,depthWrite:!1,blending:n?bs:yi}),this.pts=new Go(s,this.mat),this.pts.frustumCulled=!1,this.pts.renderOrder=n?4:3,t.add(this.pts)}add(t,e,n,s,r,o,a,l,h,u,m,x,f,v){let d=this.next;this.next=(d+1)%this.n,this.pos[d*3]=t,this.pos[d*3+1]=e,this.pos[d*3+2]=n,this.vel[d*3]=s,this.vel[d*3+1]=r,this.vel[d*3+2]=o,this.life[d]=a,this.max[d]=a,this.s0[d]=l,this.s1[d]=h,this.a0[d]=m,this.grav[d]=x,this.drag[d]=f,this.shape[d]=v,this.base[d*3]=u[0],this.base[d*3+1]=u[1],this.base[d*3+2]=u[2]}tick(t){let e=this.pos,n=this.vel,s=this.col,r=0;for(let a=0;a<this.n;a++){if(this.life[a]<=0)continue;let l=this.life[a]-=t;if(l<=0){e[a*3+1]=-9999,this.size[a]=0;continue}r++;let h=1-l/this.max[a],u=Math.max(0,1-this.drag[a]*t);n[a*3]*=u,n[a*3+2]*=u,n[a*3+1]=n[a*3+1]*u-this.grav[a]*t,e[a*3]+=n[a*3]*t,e[a*3+1]+=n[a*3+1]*t,e[a*3+2]+=n[a*3+2]*t,this.size[a]=this.s0[a]+(this.s1[a]-this.s0[a])*h,s[a*4]=this.base[a*3],s[a*4+1]=this.base[a*3+1],s[a*4+2]=this.base[a*3+2],s[a*4+3]=this.a0[a]*(h<.7?1:1-(h-.7)/.3)}this.live=r;let o=this.geo.attributes;o.position.needsUpdate=!0,o.rgba.needsUpdate=!0,o.size.needsUpdate=!0,o.shape.needsUpdate=!0}},$c=new Map;function qy(i,t){let e=i+t;if($c.has(e))return $c.get(e);let n=document.createElement("canvas");n.width=256,n.height=128;let s=n.getContext("2d"),r=i.length>4?58:76;s.font=`900 ${r}px "Black Han Sans", "Arial Black", system-ui, sans-serif`,s.textAlign="center",s.textBaseline="middle",s.lineJoin="round",s.lineWidth=16,s.strokeStyle="#0B1A33",s.strokeText(i,128,66),s.lineWidth=7,s.strokeStyle="#FFFFFF",s.strokeText(i,128,66);let o=s.createLinearGradient(0,30,0,100);o.addColorStop(0,"#FFFFFF"),o.addColorStop(.35,t),o.addColorStop(1,t),s.fillStyle=o,s.fillText(i,128,66);let a=new Gi(n);return a.colorSpace=ze,$c.set(e,a),a}var Jc=new Map;function Yy(i){if(Jc.has(i))return Jc.get(i);let t=document.createElement("canvas");t.width=96,t.height=96;let e=t.getContext("2d");e.fillStyle="rgba(255,255,255,.94)",e.beginPath(),e.arc(48,44,38,0,Math.PI*2),e.fill(),e.beginPath(),e.moveTo(40,78),e.lineTo(48,94),e.lineTo(58,76),e.fill(),e.lineWidth=4,e.strokeStyle="rgba(11,26,51,.35)",e.beginPath(),e.arc(48,44,38,0,Math.PI*2),e.stroke(),e.font='48px "Apple Color Emoji","Segoe UI Emoji","Noto Color Emoji",sans-serif',e.textAlign="center",e.textBaseline="middle",e.fillText(i,48,47);let n=new Gi(t);return n.colorSpace=ze,Jc.set(i,n),n}var sa=[16732043,16765503,2414847,8191922,16752412,10976255,16777215];function yf(i){let t=new ra(i,1800,!0),e=new ra(i,1600,!1),n=(p,g)=>p+Math.random()*(g-p),s=p=>p[Math.random()*p.length|0],r=new ni(.82,1,48),o=[];for(let p=0;p<28;p++){let g=new Z(r,new oe({color:16777215,transparent:!0,opacity:0,depthWrite:!1,blending:bs,side:Me}));g.visible=!1,g.renderOrder=4,i.add(g),o.push({m:g,t:0,life:1,r0:.5,r1:3})}let a=0,l=[];for(let p=0;p<16;p++){let g=new Mi(new ei({transparent:!0,depthTest:!1,depthWrite:!1}));g.visible=!1,g.renderOrder=9,i.add(g),l.push({sp:g,t:0,life:1,follow:null,y0:0,big:1,rot:0})}let h=0,u=new de,m=new Float32Array(384);u.setAttribute("position",new we(m,3));let x=new ko(u,new _r({color:12580095,transparent:!0,opacity:.95,blending:bs,depthWrite:!1}));x.frustumCulled=!1,x.renderOrder=5,i.add(x);let f=0,v=0,d=[],y=0,c=new B,_={on:!0,burst(p,g,T,S={}){if(!_.on)return;let b=S.n||20,w=S.speed||6,M=S.up!=null?S.up:3,E=(S.colors||sa).map(Bs),F=S.soft?e:t,U=S.life||.7,R=S.size||.5,N=S.size1!=null?S.size1:R*.2,V=S.grav!=null?S.grav:9,z=S.drag!=null?S.drag:1.5,O=S.shape||0,D=S.flat||0;for(let X=0;X<b;X++){let $=Math.random()*Math.PI*2,tt=D?Math.PI/2+n(-D,D):Math.acos(n(-1,1)),ut=w*n(.45,1),Ct=Math.sin(tt)*Math.cos($)*ut+(S.vx||0),J=Math.sin(tt)*Math.sin($)*ut+(S.vz||0),st=Math.abs(Math.cos(tt))*ut*(D?.3:1)*(S.down?-1:1)+M*n(.5,1);F.add(p+n(-1,1)*(S.jitter||0),g+n(-1,1)*(S.jitter||0),T+n(-1,1)*(S.jitter||0),Ct,st,J,U*n(.6,1.15),R*n(.7,1.3),N,s(E),S.alpha||1,V,z,O)}},one(p,g,T,S,b,w,M={}){_.on&&(M.soft?e:t).add(p,g,T,S,b,w,M.life||.5,M.size||.4,M.size1!=null?M.size1:.05,Bs(M.color!=null?M.color:16777215),M.alpha||1,M.grav||0,M.drag||0,M.shape||0)},ring(p,g,T,S={}){if(!_.on)return;let b=o[a];a=(a+1)%o.length,b.t=0,b.life=S.life||.45,b.r0=S.r0||.4,b.r1=S.r1||3,b.a=S.alpha||.9,b.m.material.color.setHex(S.color!=null?S.color:16777215),b.m.position.set(p,g,T),S.normal?b.m.lookAt(p+S.normal[0],g+S.normal[1],T+S.normal[2]):b.m.rotation.set(-Math.PI/2,0,0),b.m.visible=!0},word(p,g,T,S,b="#FFD23F",w={}){if(!_.on)return;let M=l[h];h=(h+1)%l.length,M.sp.material.map=qy(S,b),M.sp.material.needsUpdate=!0,M.sp.material.rotation=M.rot=n(-.25,.25),M.t=0,M.life=w.life||.9,M.big=w.big||1,M.follow=w.follow||null,M.x=p,M.y0=g,M.z=T,M.ratio=2,M.sp.visible=!0},emote(p,g){if(!_.on)return;let T=l[h];h=(h+1)%l.length,T.sp.material.map=Yy(g),T.sp.material.needsUpdate=!0,T.sp.material.rotation=T.rot=0,T.t=0,T.life=1.4,T.big=.55,T.follow=p,T.ratio=1,T.sp.visible=!0},bolt(p,g,T,S,b,w){if(!_.on)return;let M=7,E=p,F=g,U=T;for(let R=1;R<=M&&v<64;R++){let N=R/M,V=R===M?0:.7,z=p+(S-p)*N+n(-V,V),O=g+(b-g)*N+n(-V,V),D=T+(w-T)*N+n(-V,V);m.set([E,F,U,z,O,D],v*6),v++,E=z,F=O,U=D}f=.28,u.setDrawRange(0,v*2),u.attributes.position.needsUpdate=!0,_.burst(S,b,w,{n:14,speed:5,colors:[12580095,16777215,2414847],life:.4,size:.45})},firework(p,g,T,S={}){_.on&&d.push({x:p,y:g,z:T,vy:S.vy||n(16,22),vx:n(-1.5,1.5),vz:n(-1.5,1.5),t:S.delay||0,fuse:S.fuse||n(.8,1.15),cols:S.colors||[s(sa),s(sa)],big:S.big||1})},explode(p,g,T,S,b=1){_.burst(p,g,T,{n:Math.round(70*b),speed:13*b,up:0,colors:S,life:1.4,size:1.1*b,size1:.15,grav:5,drag:1.6}),_.burst(p,g,T,{n:Math.round(26*b),speed:6*b,up:0,colors:[16777215,16774064],life:.6,size:.8*b,grav:2}),_.burst(p,g,T,{n:Math.round(14*b),speed:9*b,up:0,colors:S,life:1.9,size:.6,grav:7,drag:1,shape:2}),_.ring(p,g,T,{r0:.5,r1:9*b,life:.55,color:S[0],normal:[0,0,1]})},cannon(p,g,T,S,b,w=50){if(_.on){for(let M=0;M<w;M++){let E=n(8,16);e.add(p,g,T,S*E+n(-2.5,2.5),n(9,17),b*E+n(-2.5,2.5),n(1.6,2.6),n(.35,.6),.3,Bs(s(sa)),1,7,1.2,1)}_.burst(p,g,T,{n:16,speed:5,colors:[16777215,16765503],life:.4,size:.9})}},splash(p,g,T,S=1){if(_.on){for(let b=0;b<46*S;b++){let w=Math.random()*Math.PI*2,M=n(0,1.2);e.add(p+Math.cos(w)*M,g,T+Math.sin(w)*M,Math.cos(w)*n(1,4),n(8,17)*S,Math.sin(w)*n(1,4),n(.9,1.4),n(.45,.8),.2,Bs(s([16777215,12577279,8378623])),.95,22,.2,3)}_.ring(p,g+.1,T,{r0:.6,r1:6*S,life:.8,color:12577279}),_.ring(p,g+.1,T,{r0:.3,r1:3.5*S,life:.6,color:16777215})}},dust(p,g,T,S={}){if(!_.on)return;let b=S.n||8,w=(S.colors||[16777215,15398143,14149365]).map(Bs);for(let M=0;M<b;M++){let E=Math.random()*Math.PI*2,F=n(.5,1)*(S.speed||2.6);e.add(p,g+.1,T,Math.cos(E)*F,n(.3,1.4),Math.sin(E)*F,n(.35,.6)*(S.life||1),(S.size||.55)*n(.7,1.2),(S.size||.55)*2.2,s(w),S.alpha||.75,-.5,3,0)}},shake(p){y=Math.max(y,p)},shakeOffset(p){y=Math.max(0,y-p*2.6);let g=y*y;return c.set(n(-1,1)*g,n(-1,1)*g,n(-1,1)*g),c},tick(p,g){for(let T=d.length-1;T>=0;T--){let S=d[T];if(S.t>0){S.t-=p;continue}S.fuse-=p,S.vy-=9*p,S.x+=S.vx*p,S.y+=S.vy*p,S.z+=S.vz*p,t.add(S.x,S.y,S.z,n(-.6,.6),n(-2,-.5),n(-.6,.6),.45,.55,.05,Bs(16771496),1,2,1,0),S.fuse<=0&&(_.explode(S.x,S.y,S.z,S.cols.map(b=>b),S.big),d.splice(T,1))}t.tick(p),e.tick(p);for(let T of o){if(!T.m.visible)continue;T.t+=p;let S=T.t/T.life;if(S>=1){T.m.visible=!1;continue}let b=1-Math.pow(1-S,3),w=T.r0+(T.r1-T.r0)*b;T.m.scale.set(w,w,w),T.m.material.opacity=T.a*(1-S)}for(let T of l){if(!T.sp.visible)continue;T.t+=p;let S=T.t/T.life;if(S>=1){T.sp.visible=!1,T.follow=null;continue}let b=S<.18?.4+.9*(S/.18):S<.3?1.3-.3*((S-.18)/.12):1,w=T.big*b*1.5;T.sp.scale.set(w*T.ratio,w,1),T.follow?T.sp.position.set(T.follow.pos.x,T.follow.pos.y+2.75+S*.4,T.follow.pos.z):T.sp.position.set(T.x,T.y0+S*1.6,T.z),T.sp.material.opacity=S>.75?1-(S-.75)/.25:1}if(f>0&&(f-=p,x.material.opacity=Math.max(0,f/.28)*(Math.random()<.5?1:.5),f<=0&&(v=0,u.setDrawRange(0,0))),g){let T=g.isPerspectiveCamera?innerHeight*Math.min(window.devicePixelRatio||1,1.75)/(2*Math.tan(g.fov*Math.PI/360)):400;t.mat.uniforms.uScale.value=T,e.mat.uniforms.uScale.value=T}},get count(){return t.live+e.live}};return _}function Ei(i,t=!1){let e=i[0].index!==null,n=new Set(Object.keys(i[0].attributes)),s=new Set(Object.keys(i[0].morphAttributes)),r={},o={},a=i[0].morphTargetsRelative,l=new de,h=0;for(let u=0;u<i.length;++u){let m=i[u],x=0;if(e!==(m.index!==null))return console.error("THREE.BufferGeometryUtils: .mergeGeometries() failed with geometry at index "+u+". All geometries must have compatible attributes; make sure index attribute exists among all geometries, or in none of them."),null;for(let f in m.attributes){if(!n.has(f))return console.error("THREE.BufferGeometryUtils: .mergeGeometries() failed with geometry at index "+u+'. All geometries must have compatible attributes; make sure "'+f+'" attribute exists among all geometries, or in none of them.'),null;r[f]===void 0&&(r[f]=[]),r[f].push(m.attributes[f]),x++}if(x!==n.size)return console.error("THREE.BufferGeometryUtils: .mergeGeometries() failed with geometry at index "+u+". Make sure all geometries have the same number of attributes."),null;if(a!==m.morphTargetsRelative)return console.error("THREE.BufferGeometryUtils: .mergeGeometries() failed with geometry at index "+u+". .morphTargetsRelative must be consistent throughout all geometries."),null;for(let f in m.morphAttributes){if(!s.has(f))return console.error("THREE.BufferGeometryUtils: .mergeGeometries() failed with geometry at index "+u+".  .morphAttributes must be consistent throughout all geometries."),null;o[f]===void 0&&(o[f]=[]),o[f].push(m.morphAttributes[f])}if(t){let f;if(e)f=m.index.count;else if(m.attributes.position!==void 0)f=m.attributes.position.count;else return console.error("THREE.BufferGeometryUtils: .mergeGeometries() failed with geometry at index "+u+". The geometry must have either an index or a position attribute"),null;l.addGroup(h,f,u),h+=f}}if(e){let u=0,m=[];for(let x=0;x<i.length;++x){let f=i[x].index;for(let v=0;v<f.count;++v)m.push(f.getX(v)+u);u+=i[x].attributes.position.count}l.setIndex(m)}for(let u in r){let m=_f(r[u]);if(!m)return console.error("THREE.BufferGeometryUtils: .mergeGeometries() failed while trying to merge the "+u+" attribute."),null;l.setAttribute(u,m)}for(let u in o){let m=o[u][0].length;if(m===0)break;l.morphAttributes=l.morphAttributes||{},l.morphAttributes[u]=[];for(let x=0;x<m;++x){let f=[];for(let d=0;d<o[u].length;++d)f.push(o[u][d][x]);let v=_f(f);if(!v)return console.error("THREE.BufferGeometryUtils: .mergeGeometries() failed while trying to merge the "+u+" morphAttribute."),null;l.morphAttributes[u].push(v)}}return l}function _f(i){let t,e,n,s=-1,r=0;for(let h=0;h<i.length;++h){let u=i[h];if(t===void 0&&(t=u.array.constructor),t!==u.array.constructor)return console.error("THREE.BufferGeometryUtils: .mergeAttributes() failed. BufferAttribute.array must be of consistent array types across matching attributes."),null;if(e===void 0&&(e=u.itemSize),e!==u.itemSize)return console.error("THREE.BufferGeometryUtils: .mergeAttributes() failed. BufferAttribute.itemSize must be consistent across matching attributes."),null;if(n===void 0&&(n=u.normalized),n!==u.normalized)return console.error("THREE.BufferGeometryUtils: .mergeAttributes() failed. BufferAttribute.normalized must be consistent across matching attributes."),null;if(s===-1&&(s=u.gpuType),s!==u.gpuType)return console.error("THREE.BufferGeometryUtils: .mergeAttributes() failed. BufferAttribute.gpuType must be consistent across matching attributes."),null;r+=u.count*e}let o=new t(r),a=new we(o,e,n),l=0;for(let h=0;h<i.length;++h){let u=i[h];if(u.isInterleavedBufferAttribute){let m=l/e;for(let x=0,f=u.count;x<f;x++)for(let v=0;v<e;v++){let d=u.getComponent(x,v);a.setComponent(x+m,v,d)}}else o.set(u.array,l);l+=u.count*e}return s!==void 0&&(a.gpuType=s),a}var la={value:0},vf=new ee,oa=new gn,aa=new sn,Zy=new B,$y=new B,Jy=new Jt,fn=i=>new Jt().setHex(i);function Ie(i,t,e,n,s,r=0,o=0,a=0,l=1,h=l,u=l){aa.set(r,o,a),oa.setFromEuler(aa),vf.compose(Zy.set(e,n,s),oa,$y.set(l,h,u)),i.setMatrixAt(t,vf)}function zs(i,t,e){return i.onBeforeCompile=n=>{n.uniforms.uTime=la,n.vertexShader=`uniform float uTime;
`+n.vertexShader.replace("#include <begin_vertex>",`#include <begin_vertex>
#ifdef USE_INSTANCING
`+t+`
#endif`)},i.customProgramCacheKey=()=>e,i}function Nn(i){let t=i.map(([e,n,s])=>{let r=e.index?e.toNonIndexed():e.clone();r.applyMatrix4(n),r.deleteAttribute("uv");let o=fn(s),a=r.attributes.position.count,l=new Float32Array(a*3);for(let h=0;h<a;h++)l[h*3]=o.r,l[h*3+1]=o.g,l[h*3+2]=o.b;return r.setAttribute("color",new we(l,3)),r});return Ei(t)}var pe=(i,t,e,n=0,s=0,r=0,o=1,a=o,l=o)=>(aa.set(n,s,r),oa.setFromEuler(aa),new ee().compose(new B(i,t,e),oa.clone(),new B(o,a,l))),Bn=(i={})=>new Bt(Object.assign({vertexColors:!0},i));function Mf(i,t,e){let{P:n,trackY:s,RT:r,HW:o,PILLAR:a,TAU:l,mulberry:h,canvasTex:u,textTex:m,THEMES:x}=e,f=h(77),v=[16732043,16765503,2414847,8191922,16752412,10976255,16777215,1140479],d={anim:[]},y=i.solids.filter(c=>c.kind==="arc"&&!c.pillar);{let c=[],_=[],p=[];for(let R of y){let N=Math.max(2,Math.ceil(R.len/1.5)),V=x[R.theme],z=V.accent;for(let O=0;O<N;O++){let D=O/N,X=(O+1)/N,$=R.a0+(R.a1-R.a0)*D,tt=R.a0+(R.a1-R.a0)*X,ut=R.y0+(R.y1-R.y0)*D+.02,Ct=R.y0+(R.y1-R.y0)*X+.02;for(let[J,st]of[[R.r1-.32,R.r1],[R.r0,R.r0+.22]])c.push([J,st,$,tt,ut,Ct,O%2?z:16777215])}for(let O=1.1;O<R.len;O+=2.2){let D=n(R.s0+O,R.r1-r+.02,0);p.push([D.x,R.y0+(R.y1-R.y0)*O/R.len+.16,D.z])}if(R.r1-R.r0>8&&!R.door&&!R.ice)for(let O=5;O<R.len-2;O+=13)_.push([R,O])}let g=[],T=[];for(let[R,N,V,z,O,D,X]of c){let $=fn(X),tt=(ut,Ct,J)=>g.push(ut*Math.cos(Ct),J,ut*Math.sin(Ct));tt(R,V,O),tt(N,V,O),tt(N,z,D),tt(R,V,O),tt(N,z,D),tt(R,z,D);for(let ut=0;ut<6;ut++)T.push($.r,$.g,$.b)}let S=new de;S.setAttribute("position",new Wt(g,3)),S.setAttribute("color",new Wt(T,3));let b=new Z(S,new oe({vertexColors:!0,side:Me,polygonOffset:!0,polygonOffsetFactor:-2}));t.add(b);let w=new be(new ae(.13,8,6),new oe,p.length);p.forEach(([R,N,V],z)=>{Ie(w,z,R,N,V),w.setColorAt(z,fn(v[z%4]))}),w.instanceMatrix.needsUpdate=!0,t.add(w);let M=0,E=0;d.anim.push((R,N)=>{if(E-=N,!(E>0)){E=.14,M++;for(let V=0;V<p.length;V++){let z=(V+M)%4;w.setColorAt(V,Jy.setHex(z===0?16777215:v[(V+(M>>2))%4]))}w.instanceColor.needsUpdate=!0}});let F=u(128,128,(R,N,V)=>{R.clearRect(0,0,N,V),R.fillStyle="rgba(17,102,255,.32)";for(let z of[0,44])R.beginPath(),R.moveTo(10,40+z),R.lineTo(64,4+z),R.lineTo(118,40+z),R.lineTo(118,62+z),R.lineTo(64,26+z),R.lineTo(10,62+z),R.fill()}),U=_.map(([R,N])=>{let V=R.s0+N,z=n(V,0,0),O=R.y0+(R.y1-R.y0)*N/R.len+.03,D=new Qe(3.2,3.2);D.rotateX(-Math.PI/2),D.rotateY(-z.a+Math.PI);let X=Math.atan2(R.y1-R.y0,R.len);return D.applyMatrix4(new ee().makeRotationAxis(new B(Math.cos(z.a),0,Math.sin(z.a)),-X)),D.translate(z.x,O,z.z),D});U.length&&t.add(new Z(Ei(U),new oe({map:F,transparent:!0,depthWrite:!1,polygonOffset:!0,polygonOffsetFactor:-3})))}{let c=[];for(let E=4;E<i.endS-3;E+=7)c.push(E);let _=new be(new Gt(.08,.08,6.5,6),new Bt({color:16777215}),c.length),p=new Qe(1.8,1.1);p.translate(.9,0,0);let g=new be(p,new Bt({side:Me}),c.length),T=[];c.forEach((E,F)=>{let U=n(E,o+.9,0),R=s(E)-1.6;Ie(_,F,U.x,R+3.25,U.z),g.setColorAt(F,fn(v[F%6])),T.push([U.x,R+6.4,U.z,E])}),_.instanceMatrix.needsUpdate=!0,t.add(_,g);let S=i.winds[0];d.anim.push((E,F,U)=>{let R=S&&S.active(U.ht),N=S&&S.warn(U.ht);c.forEach((V,z)=>{let O=n(V,o+.9,0),D=S&&V>=S.s0&&V<=S.s1,X=D&&(R||N)?60:300;Ie(g,z,O.x,s(V)-1.6+5.8,O.z,0,-O.a+Math.PI/2+Math.sin(E/X+z)*(D&&R?.9:.35),0)}),g.instanceMatrix.needsUpdate=!0});let b=[],w=[];for(let E=0;E+1<T.length;E++){let[F,U,R]=T[E],[N,V,z]=T[E+1];if(!(Math.hypot(N-F,V-U,z-R)>9))for(let O=0;O<6;O++){let D=(O+.15)/6,X=(O+.85)/6,$=rt=>-Math.sin(Math.PI*rt)*.9,tt=rt=>[F+(N-F)*rt,U+(V-U)*rt+$(rt),R+(z-R)*rt],ut=tt(D),Ct=tt(X),J=(D+X)/2,st=tt(J);st[1]-=.75;let _t=fn(v[(E+O)%7]);b.push(...ut,...Ct,...st);for(let rt=0;rt<3;rt++)w.push(_t.r,_t.g,_t.b)}}let M=new de;M.setAttribute("position",new Wt(b,3)),M.setAttribute("color",new Wt(w,3)),M.computeVertexNormals(),t.add(new Z(M,new oe({vertexColors:!0,side:Me})))}{let c=[];for(let b=0;b<140;b++){let w=f()*l,M=r+o+6+f()*42;c.push([Math.cos(w)*M,-6+f()*(i.topY+26),Math.sin(w)*M,v[b%7],1])}for(let b of i.gates.concat([{s:i.finishS,y:i.finishY}]))for(let w of[-1,1])for(let M=0;M<5;M++){let E=n(b.s,w*(o+.4)+(f()-.5)*1.4,0);c.push([E.x+(f()-.5),b.y+8+f()*1.8,E.z+(f()-.5),v[(M+2)%7],.75])}let _=new ae(1,12,10);_.scale(1,1.2,1);let p=zs(new Bt,"float bph = instanceMatrix[3][0] * 0.31 + instanceMatrix[3][2] * 0.17; transformed.y += sin(uTime * 0.7 + bph) * 1.2 / max(0.5, instanceMatrix[1][1]); transformed.x += sin(uTime * 0.5 + bph * 2.0) * 0.25;","balloon"),g=new be(_,p,c.length),T=new Gt(.02,.02,2.6,3);T.translate(0,-2.4,0);let S=new be(T,zs(new oe({color:16777215}),"float bph = instanceMatrix[3][0] * 0.31 + instanceMatrix[3][2] * 0.17; transformed.y += sin(uTime * 0.7 + bph) * 1.2 / max(0.5, instanceMatrix[1][1]); transformed.x += sin(uTime * 0.5 + bph * 2.0) * 0.25;","balloonStr"),c.length);c.forEach(([b,w,M,E,F],U)=>{Ie(g,U,b,w,M,0,0,0,F),Ie(S,U,b,w,M,0,0,0,F),g.setColorAt(U,fn(E))}),t.add(g,S)}{let c=[];for(let p=0;p<40;p++){let g=f()*l,T=120+f()*160,S=6+f()*80,b=3+(f()*3|0),w=g+Math.PI/2;for(let M=0;M<b;M++)c.push([Math.cos(g)*T+Math.cos(w)*M*4.2,S+f()*2,Math.sin(g)*T+Math.sin(w)*M*4.2,3+f()*4])}let _=new be(new ae(1,10,8),new Bt({color:16777215,emissive:5991306}),c.length);c.forEach(([p,g,T,S],b)=>Ie(_,b,p,g,T,0,0,0,S,S*.8,S)),t.add(_),d.anim.push(p=>{_.rotation.y=p/4e5})}{let c=[],p=[];for(let z=0;z<28;z++){let O=(z+.5)/28*l;for(let D=0;D<4;D++){let X=150+D*6;c.push([new kt(34,4,6),pe(Math.cos(O)*X,-22+D*4,Math.sin(O)*X,0,-O+Math.PI/2,0),v[(z+D)%6]])}z%2===0&&p.push(O)}t.add(new Z(Nn(c),Bn()));let g=[];for(let z=0;z<28;z++)for(let O=0;O<4;O++)for(let D=0;D<12;D++){let X=(z+(D+.5)/12)/28*l,$=150+O*6+(f()-.5)*2;f()<.12||g.push([Math.cos(X)*$,-20+O*4,Math.sin(X)*$,X])}let T="vec3 ip = vec3(instanceMatrix[3][0], instanceMatrix[3][1], instanceMatrix[3][2]); float ang = atan(ip.z, ip.x); float wv = pow(max(0.0, sin(ang * 3.0 - uTime * 2.4)), 8.0); float hop = max(0.0, sin(uTime * (5.0 + fract(ip.x * 0.37) * 4.0) + ip.z * 0.7)) * 0.25; transformed.y += (hop + wv * 1.1) / max(0.5, instanceMatrix[1][1]);",S=new kt(.7,1.1,.5);S.translate(0,.55,0);let b=new $o(.34,0);b.translate(0,1.42,0);let w=new kt(1.25,.16,.16);w.translate(0,1.55,0);let M=new be(S,zs(new Bt,T,"crowd"),g.length),E=new be(b,zs(new Bt,T,"crowdH"),g.length),F=Math.round(g.length*.35),U=new be(w,zs(new Bt,T+" transformed.x *= 1.0 + 0.15 * sin(uTime * 9.0 + ip.x);","crowdA"),F),R=[16769220,16107418,14262374,11037500];g.forEach(([z,O,D,X],$)=>{if(Ie(M,$,z,O,D,0,-X+Math.PI/2,0,2.2),Ie(E,$,z,O,D,0,0,0,2.2),M.setColorAt($,fn(v[$*7%8])),E.setColorAt($,fn(R[$%4])),$<F){let tt=g[$*3%g.length];Ie(U,$,tt[0],tt[1],tt[2],0,-tt[3]+Math.PI/2,0,2.2),U.setColorAt($,fn(R[$*3%4]))}}),t.add(M,E,U);let N=m("StepUP  \u{1F3C3}  \uD608\uC555 \uC8FC\uC758  \u{1FA78}  StepUP","#1166FF","#FFFFFF",1024,128),V=p.map(z=>{let O=new Qe(30,3.6);return O.rotateY(-z-Math.PI/2),O.translate(Math.cos(z)*171,-2.5,Math.sin(z)*171),O});t.add(new Z(Ei(V),new oe({map:N,side:Me})))}{let p=Math.cos(.6)*118,g=Math.sin(.6)*118,T=8,S=new fe;S.position.set(p,T,g),S.rotation.y=-.6+Math.PI/2,t.add(S);let b=new fe;S.add(b);let w=[];for(let F of[-1.6,1.6])w.push([new Be(26,.45,6,48),pe(0,0,F),16732043]);for(let F=0;F<12;F++){let U=F/12*l;for(let R of[-1.6,1.6])w.push([new Gt(.18,.18,26,5),pe(Math.cos(U)*26/2,Math.sin(U)*26/2,R,0,0,U-Math.PI/2),16777215])}w.push([new Gt(1.4,1.4,4,16),pe(0,0,0,Math.PI/2,0,0),16765503]),b.add(new Z(Nn(w),Bn()));let M=[];for(let F of[-3,3])for(let U of[-1,1])M.push([new Gt(.6,.8,T+24,6),pe(U*9,-(T+22)/2+1,F,0,0,U*.33),1140479]);S.add(new Z(Nn(M),Bn()));let E=new be(new kt(3,2.6,2.6),new Bt,12);for(let F=0;F<12;F++)E.setColorAt(F,fn(v[F%6]));S.add(E),d.anim.push(F=>{let U=F/9e3;b.rotation.z=U;for(let R=0;R<12;R++){let N=R/12*l+U;Ie(E,R,Math.cos(N)*26,Math.sin(N)*26-2,0)}E.instanceMatrix.needsUpdate=!0})}{let c=u(256,64,(E,F,U)=>{for(let R=0;R<8;R++)E.fillStyle=R%2?"#FFFFFF":"#DDDDDD",E.fillRect(R*32,0,32,U)}),_=new ae(4,16,12);_.scale(1,1.25,1);let p=new be(_,new Bt({map:c}),9),g=new be(new kt(1.8,1.4,1.8),new Bt({color:9133620}),9),T=[];for(let E=0;E<9;E++)T.push({a:f()*l,r:85+f()*110,y:10+f()*60,sp:(f()<.5?-1:1)*(.004+f()*.006)}),p.setColorAt(E,fn(v[E%6]));t.add(p,g),d.anim.push(E=>{T.forEach((F,U)=>{let R=F.a+E/1e3*F.sp,N=F.y+Math.sin(E/2600+U)*2,V=Math.cos(R)*F.r,z=Math.sin(R)*F.r;Ie(p,U,V,N,z,0,R,0),Ie(g,U,V,N-6.2,z,0,R,0)}),p.instanceMatrix.needsUpdate=!0,g.instanceMatrix.needsUpdate=!0});let S=new fe,b=[];b.push([new kt(16,4.4,6),pe(0,0,0),16777215]),b.push([new kt(16.4,1.4,6.4),pe(0,-2.6,0),1140479]),b.push([new kt(6,3,5.4),pe(-4.5,3.5,0),16777215]),b.push([new Gt(2.9,2.9,6,16,1,!1,0,Math.PI),pe(8,-.6,0,Math.PI/2,0,0),16777215]),b.push([new kt(10,.6,6.15),pe(1,.4,0),16732043]);for(let E=0;E<4;E++)b.push([new Be(.35,.1,5,10),pe(-1+E*2.4,1.6,3.05),727603]);S.add(new Z(Nn(b),Bn()));let w=new Z(new Qe(14,2.2),new oe({map:m("StepUP \xB7 \uAC78\uC73C\uBA74 \uC624\uB978\uB2E4","#0B1A33","#FFD23F",1024,160),side:Me}));w.position.set(0,-6,0),S.add(w);let M=new Z(new Gt(.05,.05,3,4),new oe({color:727603}));M.position.set(0,-4.4,0),S.add(M),t.add(S),d.anim.push(E=>{let F=E/1e3*.025,U=96;S.position.set(Math.cos(F)*U,i.topY+22+Math.sin(E/3e3)*2,Math.sin(F)*U),S.rotation.y=-F})}{let c=[],_=[],p=(D,X,$,tt,ut)=>{for(let Ct=0;Ct<5;Ct++)c.push([D+Ct*.35,X+Ct*tt/5,$,.5-Ct*.05]);for(let Ct=0;Ct<6;Ct++)_.push([D+1.75,X+tt,$,ut+Ct/6*l])};for(let D=0;D<18;D++){let X=f()*l,$=52+f()*24;p(Math.cos(X)*$,-21,Math.sin(X)*$,7+f()*4,f()*l)}let g=[];for(let D=0;D<6;D++){let X=D/6*l+.3,$=72+f()*20,tt=4+D*6+f()*4;g.push([Math.cos(X)*$,tt,Math.sin(X)*$]),p(Math.cos(X)*$-1,tt+.6,Math.sin(X)*$,5+f()*2,f()*l)}let T=new be(new Gt(1,1.15,1.6,6),new Bt({color:9133620}),c.length);c.forEach(([D,X,$,tt],ut)=>Ie(T,ut,D,X,$,0,0,.1,tt,1.4,tt));let S=new rn(.9,4.2,4);S.rotateZ(-Math.PI/2),S.translate(2.1,0,0),S.scale(1,.25,1);let b=new be(S,zs(new Bt({color:3129179}),"transformed.y += sin(uTime * 1.6 + instanceMatrix[3][0]) * 0.12 * transformed.x;","palm"),_.length);_.forEach(([D,X,$,tt],ut)=>Ie(b,ut,D,X,$,0,tt,-.35)),t.add(T,b);let w=new be(new rn(6,9,7),new Bt({color:10254932}),g.length),M=new be(new Gt(6.2,6.2,1.2,14),new Bt({color:8182638}),g.length);g.forEach(([D,X,$],tt)=>{Ie(w,tt,D,X-4.5,$,Math.PI,0,0),Ie(M,tt,D,X,$)}),t.add(w,M),d.anim.push(D=>{g.forEach(([X,$,tt],ut)=>{let Ct=Math.sin(D/2200+ut)*.8;Ie(w,ut,X,$-4.5+Ct,tt,Math.PI,0,0),Ie(M,ut,X,$+Ct,tt)}),w.instanceMatrix.needsUpdate=!0,M.instanceMatrix.needsUpdate=!0});let E=new be(new rn(2.4,1.2,8),new Bt,14),F=new be(new Gt(.08,.08,3,4),new Bt({color:16777215}),14);for(let D=0;D<14;D++){let X=f()*l,$=63+f()*6;Ie(E,D,Math.cos(X)*$,-18.2,Math.sin(X)*$),Ie(F,D,Math.cos(X)*$,-19.7,Math.sin(X)*$),E.setColorAt(D,fn(v[D%6]))}t.add(E,F);let U=new fe,R=[];R.push([new kt(12,3.6,5),pe(0,0,0),1140479]),R.push([new kt(12.4,1.2,5.3),pe(0,-2.3,0),16777215]),R.push([new kt(5,3.4,4.6),pe(-3.4,3,0),1140479]),R.push([new kt(8,.6,5.12),pe(.6,.2,0),16765503]);for(let D=0;D<3;D++)R.push([new kt(.3,.2,5.15),pe(.5+D*1.6,1.85,0),16777215]);U.add(new Z(Nn(R),Bn())),U.position.set(Math.cos(-.25)*58,-15,Math.sin(-.25)*58),U.rotation.y=.25+Math.PI/2,t.add(U),d.anim.push(D=>{U.position.y=-15+Math.sin(D/900)*.6,U.rotation.z=Math.sin(D/1300)*.06});let N=16,V=new be(new ae(1.4,10,8),new Bt({color:16765503}),N*2),z=new be(new rn(.35,.9,6),new Bt({color:16752412}),N),O=[];for(let D=0;D<N;D++){let X=f()*l,$=90+f()*50;O.push([Math.cos(X)*$,Math.sin(X)*$,f()*l])}t.add(V,z),d.anim.push(D=>{O.forEach(([X,$,tt],ut)=>{let Ct=-23.4+Math.sin(D/700+ut)*.25,J=tt+Math.sin(D/2500+ut)*.4;Ie(V,ut*2,X,Ct,$,0,J,0,1,.8,1.2);let st=X+Math.sin(J)*-1.2,_t=$+Math.cos(J)*-1.2;Ie(V,ut*2+1,st,Ct+1.5,_t,0,J,0,.62),Ie(z,ut,st+Math.sin(J)*-.95,Ct+1.4,_t+Math.cos(J)*-.95,-Math.PI/2,J,0)}),V.instanceMatrix.needsUpdate=!0,z.instanceMatrix.needsUpdate=!0})}{let c=new ni(76,240,96,10);c.rotateX(-Math.PI/2);let _=new Bt({color:3841023,emissive:670362,transparent:!0,opacity:.85});_.onBeforeCompile=M=>{M.uniforms.uTime=la,M.vertexShader=`uniform float uTime;
`+M.vertexShader.replace("#include <begin_vertex>",`#include <begin_vertex>
float rr = length(transformed.xz); transformed.y += sin(rr * 0.35 - uTime * 1.6) * 0.45 + sin(transformed.x * 0.08 + uTime * 0.9) * 0.35;`)},_.customProgramCacheKey=()=>"waves";let p=new Z(c,_);p.position.y=-23.6,t.add(p);let g=u(256,16,(M,E,F)=>{M.fillStyle="rgba(255,255,255,0)",M.fillRect(0,0,E,F);for(let U=0;U<18;U++)M.fillStyle="rgba(255,255,255,.85)",M.beginPath(),M.ellipse((U+.5)*E/18,F/2,6+U%3*3,5,0,0,l),M.fill()});g.wrapS=un,g.repeat.set(24,1);let T=new ni(73.5,78,96,1);T.rotateX(-Math.PI/2);let S=T.attributes.uv,b=T.attributes.position;for(let M=0;M<S.count;M++){let E=b.getX(M),F=b.getZ(M);S.setXY(M,Math.atan2(F,E)/l+.5,Math.hypot(E,F)>75.7?1:0)}let w=new Z(T,new oe({map:g,transparent:!0,depthWrite:!1}));w.position.y=-23.35,t.add(w),d.anim.push(M=>{g.offset.x=M/9e3})}{let c=new Er;for(let S=0;S<10;S++){let b=S%2?.45:1,w=Math.PI/2+S*Math.PI/5;S?c.lineTo(Math.cos(w)*b,Math.sin(w)*b):c.moveTo(Math.cos(w)*b,Math.sin(w)*b)}let _=new Zo(c,{depth:.3,bevelEnabled:!1});_.center();let p=90,g=new be(_,new Bt({color:16761856,emissive:6965760}),p),T=[];for(let S=0;S<p;S++)T.push([f()*l,r+o+4+f()*16,-2+f()*(i.topY+20),.6+f()*.8,f()*l]);t.add(g),d.anim.push(S=>{let b=S/1e3;T.forEach(([w,M,E,F,U],R)=>{let N=w+b*.03;Ie(g,R,Math.cos(N)*M,E+Math.sin(b+U)*.6,Math.sin(N)*M,0,b*1.6+U,0,F)}),g.instanceMatrix.needsUpdate=!0})}{let c=[];for(let M=3;M<i.topY+12;M+=18)c.push(M);let _=c.map(M=>{let E=new Be(a+.1,.22,6,96);return E.rotateX(Math.PI/2),E.translate(0,M,0),E}),p=new Z(Ei(_),new oe({color:2414847}));t.add(p),d.anim.push(M=>{p.material.color.setHSL(M/6e3%1,.85,.6)});let g=(M,E)=>{let F=s(M)+9.5,U=M/r,R=new fe;return R.position.set(Math.cos(U)*(a+.6),F,Math.sin(U)*(a+.6)),R.rotation.y=-U+Math.PI/2,E(R),t.add(R),R},T=M=>(i.secs[M].s+(M+1<i.secs.length?i.secs[M+1].s:i.finishS))/2;g(T(0)-18,M=>{let E=[];E.push([new kt(5,1.6,2.2),pe(0,0,.6),16732043]),E.push([new kt(5.2,.5,2.3),pe(0,-1,.6),16777215]),E.push([new kt(2.2,1.5,2),pe(-1.4,1.3,.6),16732043]),M.add(new Z(Nn(E),Bn()))});let S=g(T(1),M=>{let E=[];E.push([new ae(1.8,14,10),pe(0,0,1.4,0,0,0,1,1,1.15),14753096]),E.push([new ae(.8,10,8),pe(1.4,.6,1.3),14753096]),E.push([new Gt(1.1,1.2,1.6,12),pe(0,-2.2,1.2),16777215]),M.add(new Z(Nn(E),Bn()))});d.anim.push(M=>{S.children[0].position.z=Math.max(0,Math.sin(M/400))*1.2});let b=g(T(2),M=>{let E=[];E.push([new Gt(2.4,2.4,.6,24),pe(0,0,.5,Math.PI/2,0,0),16777215]),E.push([new Be(2.4,.3,6,24),pe(0,0,.85),16752412]),E.push([new Gt(.4,.4,.8,10),pe(0,2.9,.5),16752412]),M.add(new Z(Nn(E),Bn()));let F=new Z(new kt(.18,2,.1),new oe({color:727603}));F.geometry.translate(0,.9,0),F.position.z=.9,M.add(F),M.userData.hand=F});d.anim.push(M=>{b.userData.hand.rotation.z=-M/1e3*l/6});let w=g(T(3),M=>{let E=[];for(let F=0;F<6;F++)E.push([new Jo(.9+F%3*.4,0),pe((F-2.5)*1.1,F%2*.9,1,0,F,0,.8,1.8,.8),[2414847,10976255,8191922][F%3]]);M.add(new Z(Nn(E),Bn({emissive:1122884})))});d.anim.push(M=>{w.children[0].rotation.y=Math.sin(M/1500)*.3}),g(T(4)-10,M=>{let E=[];E.push([new Gt(1.8,.6,2.6,16),pe(0,1,1.2),16761856]),E.push([new Gt(.3,.3,1.2,8),pe(0,-.9,1.2),16761856]),E.push([new kt(2.2,.7,1.6),pe(0,-1.8,1.2),1140479]);for(let F of[-1,1])E.push([new Be(.7,.18,6,12,Math.PI),pe(F*1.8,1.4,1.2,0,0,F*Math.PI/2),16761856]);M.add(new Z(Nn(E),Bn({emissive:3811840})))})}{let c=u(128,128,(S,b,w)=>{let M=S.createRadialGradient(64,64,0,64,64,64);M.addColorStop(0,"rgba(255,255,255,1)"),M.addColorStop(.25,"rgba(255,246,200,.95)"),M.addColorStop(.5,"rgba(255,220,120,.35)"),M.addColorStop(1,"rgba(255,220,120,0)"),S.fillStyle=M,S.fillRect(0,0,b,w)}),_=new Mi(new ei({map:c,transparent:!0,depthWrite:!1,fog:!1}));_.scale.set(110,110,1),_.position.set(-260,190,-300),t.add(_);let p=[],g=[];for(let S=0;S<40;S++){let b=S/40*l,w=(S+1)/40*l,M=380,E=25+Math.abs(Math.sin(S*1.7))*45+S%3*8;p.push(Math.cos(b)*M,-24,Math.sin(b)*M,Math.cos(w)*M,-24,Math.sin(w)*M,Math.cos((b+w)/2)*M,E,Math.sin((b+w)/2)*M);let F=fn(S%2?8366296:9418978);for(let U=0;U<3;U++)g.push(F.r,F.g,F.b)}let T=new de;T.setAttribute("position",new Wt(p,3)),T.setAttribute("color",new Wt(g,3)),t.add(new Z(T,new oe({vertexColors:!0,side:Me,fog:!1})))}return d.tick=(c,_,p)=>{for(let g of d.anim)g(c,_,p)},d}var Kc=window.STEPUP_RACE||{},xh=/[?&]dev\b/.test(location.search),zf=-30,Of=11.2,Ws=8.6,Ky=60,Hf=24,ce=.42,Ri=1.5,Ef=.4,jy=.1,si=.13,Ir=1/120,Qy=.8,eh=1.8,nh=4.5,le=40,Pe=7,Si=le-Pe,t_=7,e_=-20,ve=Math.PI*2,ca=[{top:"#EAF4FF",stripe:"#D2E5FF",side:1784427,accent:1140479},{top:"#FFF1DE",stripe:"#FFD9A8",side:12730636,accent:16747008},{top:"#FFE6F1",stripe:"#FFC2DD",side:10295117,accent:16732043},{top:"#E3FBF4",stripe:"#B5F0DF",side:1013358,accent:1357990},{top:"#FFF7D1",stripe:"#FFE58A",side:5972406,accent:16761856}],Sf=16732043,kf=16752412,Tf=1140479,wf=["\uD608\uC555 \uC8FC\uC758 \u{1FA78}","\uC544\uB798\uCE35 \uC190\uB2D8 \uC785\uC7A5~","\uB2E4\uC2DC \uC62C\uB77C\uAC00\uBA74 \uB418\uC8E0 \u{1F607}","\uADF8 \uC790\uB9AC\u2026 \uC544\uAE4C \uC9C0\uB098\uAC04 \uACF3 \uAC19\uC740\uB370\uC694?","\uAD1C\uCC2E\uC544\uC694, \uD0D1\uC740 \uC548 \uB3C4\uB9DD\uAC00\uC694","\uC2EC\uD638\uD761 \uD55C \uBC88 \u{1F9D8}"];function qs(i){return function(){i|=0,i=i+1831565813|0;let t=Math.imul(i^i>>>15,1|i);return t=t+Math.imul(t^t>>>7,61|t)^t,((t^t>>>14)>>>0)/4294967296}}var dn=(i,t)=>(i%t+t)%t;function Ee(i,t,e){let n=i/le,s=le+t;return{x:s*Math.cos(n),y:e,z:s*Math.sin(n),a:n}}function Gf(i,t,e,n,s,r){let o=s-e,a=r-n,l=o*o+a*a,h=l?Math.max(0,Math.min(1,((i-e)*o+(t-n)*a)/l)):0,u=e+o*h,m=n+a*h;return[Math.hypot(i-u,t-m),u,m]}var Pr=class{constructor(t){Object.assign(this,{kind:"box",cx:0,cz:0,top:0,h:1.4,yaw:0,hx:2,hz:2,r:0,move:null,spin:0,belt:0,fall:!1,theme:0,color:null,ice:!1,mystery:!1,door:null,baseYaw:0},t),this.static=!this.move&&!this.spin&&!this.fall&&this.kind!=="tilt"&&!this.door,this.enabled=!0,this.state="idle",this.timer=0,this.drop=0,this.tilt=0,this.flash=0,this.warn=!1,this.baseX=this.cx,this.baseZ=this.cz,this.vx=0,this.vz=0,this.p={cx:this.cx,cz:this.cz,top:this.top,yaw:this.yaw,tilt:0},this.bounds()}bounds(){if(this.kind==="arc"){let t=1/0,e=-1/0,n=1/0,s=-1/0;for(let r=0;r<=8;r++){let o=this.a0+(this.a1-this.a0)*r/8;for(let a of[this.r0,this.r1]){let l=a*Math.cos(o),h=a*Math.sin(o);t=Math.min(t,l),e=Math.max(e,l),n=Math.min(n,h),s=Math.max(s,h)}}this.bb=[t,e,n,s,Math.min(this.y0,this.y1)-this.h,Math.max(this.y0,this.y1)]}else{let t=(this.kind==="disc"?this.r:Math.hypot(this.hx,this.hz))+(this.move?this.move.amp:0);this.bb=[this.cx-t,this.cx+t,this.cz-t,this.cz+t,this.top-this.h-30,this.top+3]}}near(t,e,n){let s=this.bb;return t>s[0]-1.5&&t<s[1]+1.5&&n>s[2]-1.5&&n<s[3]+1.5&&e>s[4]-2&&e<s[5]+3}save(){let t=this.p;t.cx=this.cx,t.cz=this.cz,t.top=this.top,t.yaw=this.yaw,t.tilt=this.tilt}update(t,e){if(this.move){let n=this.move,s=this.offset(t),r=this.offset(t+.01);if(this.cx=this.baseX+n.dir[0]*s,this.cz=this.baseZ+n.dir[1]*s,this.vx=n.dir[0]*(r-s)/.01,this.vz=n.dir[1]*(r-s)/.01,n.profile==="punch"){let o=dn(t/n.period+(n.phase||0),1);this.flash=o>.22&&o<.4?1:0}}if(this.spin&&(this.yaw=this.baseYaw+this.spin*t),this.fall&&(this.state==="shake"?(this.timer-=e,this.timer<=0&&(this.state="drop",this.timer=3.2,this.enabled=!1,this.onDrop&&this.onDrop(this))):this.state==="drop"&&(this.timer-=e,this.drop+=e,this.timer<=0&&(this.state="idle",this.enabled=!0,this.drop=0))),this.door){let n=dn(t/this.door.period+(this.door.phase||0),1);this.enabled=n>=this.door.open,this.warn=n>.72,this.doorPh=n}}offset(t){let e=this.move;if(e.profile==="punch"){let n=dn(t/e.period+(e.phase||0),1);return n<.4?0:n<.47?e.amp*(n-.4)/.07:n<.7?e.amp:e.amp*(1-(n-.7)/.3)}return e.amp*Math.sin(ve*t/e.period+(e.phase||0))}trigger(){this.fall&&this.state==="idle"&&(this.state="shake",this.timer=this.fake?.04:.32)}toLocal(t,e,n){let s=n||this,r=t-s.cx,o=e-s.cz,a=Math.cos(-s.yaw),l=Math.sin(-s.yaw);return[r*a-o*l,r*l+o*a]}toWorld(t,e,n){let s=n||this,r=Math.cos(s.yaw),o=Math.sin(s.yaw);return[s.cx+t*r-e*o,s.cz+t*o+e*r]}arcD(t,e){return dn(Math.atan2(e,t)-this.a0,ve)}inside(t,e,n){if(this.kind==="disc"){let o=t-this.cx,a=e-this.cz;return o*o+a*a<=(this.r+n)**2}if(this.kind==="arc"){let o=Math.hypot(t,e);if(o<this.r0-n||o>this.r1+n)return!1;let a=this.arcD(t,e),l=this.a1-this.a0,h=n/o;return a<=l+h||a>=ve-h}let[s,r]=this.toLocal(t,e);return Math.abs(s)<=this.hx+n&&Math.abs(r)<=this.hz+n}topAt(t,e){if(this.kind==="arc"){let n=this.a1-this.a0,s=this.arcD(t,e);return s>n&&(s=s>n+(ve-n)/2?0:n),this.y0+(this.y1-this.y0)*s/n}if(this.kind==="tilt"){let[n]=this.toLocal(t,e);return this.top+Math.tan(this.tilt)*Math.max(-this.hx,Math.min(this.hx,n))}return this.top}bottom(){return(this.kind==="arc"?Math.min(this.y0,this.y1):this.top-(this.kind==="tilt"?Math.abs(Math.tan(this.tilt))*this.hx:0))-this.h}pushOut(t){if(this.kind==="disc"){let f=t.x-this.cx,v=t.z-this.cz,d=Math.hypot(f,v),y=this.r+ce;if(d>=y)return!1;let c=d>1e-5?y/d:1;return t.x=this.cx+(d>1e-5?f*c:y),t.z=this.cz+(d>1e-5?v*c:0),!0}if(this.kind==="arc"){let f=Math.hypot(t.x,t.z),v=this.arcD(t.x,t.z),d=this.a1-this.a0,y=M=>{let E=M/f;t.x*=E,t.z*=E},c=M=>{let E=Math.atan2(t.z,t.x)+M;t.x=f*Math.cos(E),t.z=f*Math.sin(E)};if(v<=d){if(f>=this.r0&&f<=this.r1){let M=[[f-this.r0,()=>y(this.r0-ce)],[this.r1-f,()=>y(this.r1+ce)],[v*f,()=>c(-(v+ce/f))],[(d-v)*f,()=>c(d-v+ce/f)]];return M.sort((E,F)=>E[0]-F[0]),M[0][1](),!0}return f<this.r0&&this.r0-f<ce?(y(this.r0-ce),!0):f>this.r1&&f-this.r1<ce?(y(this.r1+ce),!0):!1}let _=v>d+(ve-d)/2?this.a0:this.a1,p=Math.cos(_),g=Math.sin(_),[T,S,b]=Gf(t.x,t.z,this.r0*p,this.r0*g,this.r1*p,this.r1*g);if(T>=ce)return!1;let w=ce/Math.max(T,1e-4);return t.x=S+(t.x-S)*w,t.z=b+(t.z-b)*w,!0}let[e,n]=this.toLocal(t.x,t.z),s=Math.max(-this.hx,Math.min(this.hx,e)),r=Math.max(-this.hz,Math.min(this.hz,n)),o=e-s,a=n-r,l=Math.hypot(o,a),h,u;if(l>=ce)return!1;if(l>1e-5)h=s+o/l*ce,u=r+a/l*ce;else{let f=this.hx-Math.abs(e),v=this.hz-Math.abs(n);f<v?(h=Math.sign(e||1)*(this.hx+ce),u=n):(h=e,u=Math.sign(n||1)*(this.hz+ce))}let[m,x]=this.toWorld(h,u);return t.x=m,t.z=x,!0}carry(t){if(!(this.static||this.door)){if(this.kind==="disc"||this.spin){let e=t.x-this.p.cx,n=t.z-this.p.cz,s=this.yaw-this.p.yaw,r=Math.cos(s),o=Math.sin(s);t.x=this.cx+e*r-n*o,t.z=this.cz+e*o+n*r}else t.x+=this.cx-this.p.cx,t.z+=this.cz-this.p.cz;if(this.kind==="tilt"){let[e]=this.toLocal(t.x,t.z);t.y+=(Math.tan(this.tilt)-Math.tan(this.p.tilt))*e}}}},Qi=class{constructor(t){Object.assign(this,t),this.kind="sweep",this.bars=t.bars.map(e=>Object.assign({a:0},e))}update(t){for(let e of this.bars)e.a=e.spin*t+(e.phase||0)}hit(t){let e=t.pos;for(let n of this.bars){let s=this.base+n.y0,r=this.base+n.y1;if(e.y>r||e.y+Ri<s)continue;let o=Math.cos(n.a)*n.len,a=Math.sin(n.a)*n.len,[l,h,u]=Gf(e.x,e.z,this.x-o,this.z-a,this.x+o,this.z+a);if(l>ce+.28)continue;let m=Math.sign((e.x-this.x)*-Math.sin(n.a)+(e.z-this.z)*Math.cos(n.a))||1,x=-Math.sin(n.a)*m,f=Math.cos(n.a)*m,v=(ce+.3)/Math.max(l,.001);return e.x=h+(e.x-h)*v,e.z=u+(e.z-u)*v,{vx:x*8.5,vz:f*8.5,vy:6,stun:.42}}return null}},Ys=class{constructor(t){Object.assign(this,{len:7,r:1.15,amp:.95,period:2.1,phase:0},t),this.kind="hammer",this.update(0)}update(t){let e=ve/this.period,n=this.amp*Math.sin(e*t+this.phase),s=this.len*Math.sin(n);this.f=n,this.v=this.amp*e*Math.cos(e*t+this.phase)*this.len*Math.cos(n),this.hx=this.x+this.n[0]*s,this.hz=this.z+this.n[1]*s,this.hy=this.py-this.len*Math.cos(n),this.lat=s}hit(t){let e=t.pos,n=Math.max(e.y+ce,Math.min(e.y+Ri-ce,this.hy));if(Math.hypot(e.x-this.hx,n-this.hy,e.z-this.hz)>this.r+ce)return null;let s=Math.sign(this.v)||1;return{vx:this.n[0]*s*14,vz:this.n[1]*s*14,vy:7.5,stun:.55}}},ih=class{constructor(t){Object.assign(this,t),this.list=[],this.next=.3,this.rnd=qs(t.seed||7)}update(t,e,n,s){if(this.next-=e,this.next<=0&&!(s>this.sTop-7)){this.next=this.every*(.6+this.rnd()*.7);let r=1.1+this.rnd()*.6;this.list.push({s:this.sTop,u:(this.rnd()*2-1)*(Pe-r-.2),r,v:this.speed*(.85+this.rnd()*.35),spin:0,mesh:null})}for(let r of this.list){r.s-=r.v*e,r.spin+=r.v*e/r.r;let o=Ee(r.s,r.u,ke(r.s)+r.r);r.x=o.x,r.y=o.y,r.z=o.z,r.a=o.a,r.s<this.sEnd&&(r.dead=!0)}for(let r of this.list)r.dead&&r.mesh&&(n.group.remove(r.mesh),r.mesh=null);this.list=this.list.filter(r=>!r.dead)}hit(t){let e=t.pos;for(let n of this.list){let s=Math.max(e.y+ce,Math.min(e.y+Ri-ce,n.y));if(Math.hypot(e.x-n.x,s-n.y,e.z-n.z)<n.r+ce){let r=-Math.sin(n.a),o=Math.cos(n.a);return{vx:-r*n.v*1.2,vz:-o*n.v*1.2,vy:7,stun:.5}}}return null}},sh=class{constructor(t){Object.assign(this,{period:4.2,dur:1.3,force:5},t)}phase(t){return dn(t+(this.offset||0),this.period)}active(t){return this.phase(t)<this.dur}warn(t){return this.phase(t)>this.period-.9}},ha=[],rh=class{constructor(t){Object.assign(this,{r:.95,h:1.5},t),this.kind="bumper",this.pulse=0}touch(t){let e=t.pos;if(e.y>this.y+this.h||e.y+Ri<this.y)return null;let n=e.x-this.x,s=e.z-this.z,r=Math.hypot(n,s),o=this.r+ce;if(r>=o)return null;let a=r>1e-4?n/r:1,l=r>1e-4?s/r:0;return e.x=this.x+a*o,e.z=this.z+l*o,[a,l]}},oh=class{constructor(t){Object.assign(this,{period:2.6,phase:0,speed:10,r:.62},t),this.kind="cannon",this.shots=[],this.lastK=null,this.u0=this.side*(Pe+.6),this.span=2*Pe+2}update(t){let e=t+this.phase,n=Math.floor(e/this.period);this.warn=dn(e,this.period)>this.period-.75,this.lastK!==null&&n>this.lastK&&(this.shots.push({t0:n*this.period-this.phase}),this.fired=!0),this.lastK=n,this.shots=this.shots.filter(s=>t>=s.t0&&(t-s.t0)*this.speed<this.span);for(let s of this.shots){s.u=this.u0-this.side*(t-s.t0)*this.speed;let r=Ee(this.s,s.u,this.y+this.r+.05);s.x=r.x,s.y=r.y,s.z=r.z}}hit(t){let e=t.pos;for(let n of this.shots){let s=Math.max(e.y+ce,Math.min(e.y+Ri-ce,n.y));if(Math.hypot(e.x-n.x,s-n.y,e.z-n.z)<this.r+ce){let r=this.s/le,o=-this.side;return{vx:Math.cos(r)*o*10,vz:Math.sin(r)*o*10,vy:5,stun:.45}}}return null}};function ke(i){let t=0;for(let[e,n,s,r]of ha){if(i<e)break;t=i<=n?s+(r-s)*(i-e)/(n-e):r}return t}function n_(i){let t={solids:[],hazards:[],balls:null,winds:[],secs:[],gates:[],signs:[],group:new fe,finishS:0,flags:[],tilts:[],bumpers:[],zones:[],cannons:[],bananas:[]};ha=[];let e=0,n=0,s=0,r=qs(i||4242),o=()=>r()*ve,a=(c,_=0,p={})=>{let g=le+(p.u0!=null?p.u0:-Pe),T=le+(p.u1!=null?p.u1:Pe),S=new Pr({kind:"arc",r0:g,r1:T,a0:e/le,a1:(e+c)/le,y0:n,y1:n+_,h:p.h||1.6,theme:s,belt:p.belt||0,ice:!!p.ice,door:p.door||null,s0:e,len:c});return t.solids.push(S),ha.push([e,e+c,n,n+_]),e+=c,n+=_,S},l=c=>{ha.push([e,e+c,n,n]),e+=c},h=(c,_,p)=>{let g=Ee(e+c,_,0),T=new Pr(Object.assign({cx:g.x,cz:g.z,yaw:g.a,baseYaw:g.a,theme:s,s0:e+c,u:_},p));return t.solids.push(T),T},u=c=>{let _=(e+c)/le;return[Math.cos(_),Math.sin(_)]},m=(c,_,p)=>{let g=Ee(e+c,0,0);_.forEach(T=>{T.phase=o(),T.spin*=.9+r()*.25}),t.hazards.push(new Qi(Object.assign({x:g.x,z:g.z,base:n,bars:_,s:e+c},p||{})))},x=c=>{t.secs.push({s:e,y:n,name:c,theme:s}),t.gates.push({s:e,y:n,k:t.secs.length})},f=(c,_)=>{let p=Ee(e+c,_,0);t.bumpers.push(new rh({x:p.x,z:p.z,y:ke(e+c),s:e+c,u:_}))},v=(c,_,p,g,T)=>t.zones.push({type:c,s:e+_,u:p,hs:g,hu:T,y:ke(e+_)}),d=(c,_,p)=>{let g=new oh({s:e+c,side:_,y:ke(e+c),period:p,phase:r()*p});t.hazards.push(g),t.cannons.push(g)};s=0,x("\uBAB8\uD480\uAE30"),a(14),a(16,3),f(-11,-1.6),f(-6.5,1.4),f(-2.5,-.4),a(34),v("slime",-17,-3,1.9,1.7),v("slime",-13.5,3.2,1.7,1.5),m(-25,[{y0:.2,y1:.75,spin:1.6,len:7.3}]),m(-9,[{y0:.2,y1:.75,spin:-1.75,len:7.3},{y0:1.05,y1:1.55,spin:1.2,len:7.3,phase:2.1}]),a(8,1),l(3.2),a(8,.8,{u0:-5,u1:5}),l(3.5),a(8,.8,{u0:-4,u1:4}),l(3),a(6),a(14,2.4),v("boost",-9,1.2,1.6,1.4),s=1,x("\uBC00\uCE58\uAE30"),a(8),v("slime",-4.2,-3.5,1.7,1.6),a(40),[-32,-22,-12].forEach(c=>h(c,0,{hx:2.6,hz:.7,top:n+2.3,h:2.3,move:{dir:u(c),amp:4.6,period:2+r()*.5,phase:o()},color:Tf})),a(10,3,{belt:-3.2}),a(32),[[-27,1],[-21,-1],[-15,1],[-9,-1]].forEach(([c,_],p)=>{let g=u(c),T=[-g[0]*_,-g[1]*_];h(c,_*(Pe+1.2),{hx:3.2,hz:1.3,top:n+2.2,h:2.2,move:{dir:T,amp:5.4,period:2.1+r()*.6,phase:r(),profile:"punch"},color:kf,punch:!0})}),h(-2.5,(r()<.5?-1:1)*4.4,{hx:1.5,hz:1.5,top:n+.15,h:.4,mystery:!0}),a(10,3),d(-5,1,2.4+r()*.6),s=2,x("\uD754\uB4E4 \uB2E4\uB9AC"),a(8),a(38,2,{u0:-1.8,u1:1.8}),[-30,-19,-8].forEach(c=>{let _=Ee(e+c,0,0);t.hazards.push(new Ys({x:_.x,z:_.z,py:ke(e+c)+8.3,n:u(c),phase:o(),period:2+r()*.4,a:_.a,s:e+c}))}),a(6,0,{u0:-5,u1:5}),[4.5,11,17.5,24].forEach((c,_)=>h(c,0,{hx:2.3,hz:2.3,top:n,h:1,move:{dir:u(c),amp:4.4,period:3.1,phase:_%2?Math.PI:0},color:Tf})),l(28),a(8),v("boost",-6.4,-2,1.5,1.4),d(-3.6,1,2.2+r()*.6),a(12,4),f(-9.5,-1.8),f(-6,1.5),f(-2.6,-.6),s=3,x("\uBB34\uB108\uC9C0\uB294 \uAE38"),a(6),h(7.4,0,{kind:"disc",r:6.2,top:n+.3,h:1.2,spin:.9}),m(7.4,[{y0:.2,y1:.75,spin:1.8,len:5.8}],{base:n+.3}),h(21,0,{kind:"disc",r:6.2,top:n+.3,h:1.2,spin:-1.05}),l(28.4),a(6);for(let c=0;c<8;c++)for(let _=0;_<5;_++)h(1.95+c*2.9,-5.8+_*2.9,{hx:1.3,hz:1.3,top:n,h:.5,fall:!0,fake:r()<.15,tileColor:(c+_)%2});l(24.6),a(6),t.tilts.push(h(7.2,0,{kind:"tilt",hx:7,hz:6,top:n,h:.8,ice:!0})),l(14.4),a(8),d(-4,-1,2.5+r()*.6),a(10,4,{belt:-3}),s=4,x("\uC815\uC0C1"),a(6),v("boost",-3,0,1.6,1.6);let y=e;a(50,9),v("pad",-39,-3.6,1.2,1.2),v("pad",-22,-.4,1.2,1.2),v("slime",-45,3,1.8,1.6),t.balls=new ih({sTop:e-.5,sEnd:y-2,speed:11,every:.62,seed:r()*1e6|0}),t.winds.push(new sh({s0:y,s1:e,period:3.6+r()*1.4,dur:1.3,force:5,offset:r()*4})),a(14,0,{ice:!0}),m(-7,[{y0:.2,y1:.75,spin:2,len:7.3},{y0:1.05,y1:1.55,spin:-1.4,len:7.3,phase:1.2}]),a(3,.5),t.signs.push({s:e,y:n,text:"\uAC70\uC758 \uB2E4 \uC654\uC5B4\uC694 \u{1F607}"}),t.door=a(5,.5,{door:{period:2.6+r()*.8,open:.36,phase:r()}}),a(2,.5),t.finishS=e+8,t.finishY=n,a(18,0,{u0:-8,u1:8}),t.endS=e,t.topY=n,t.items=[];for(let[c,_]of[[16,28],[44,50],[105,115],[147,150],[167,174],[200,206],[219,224],[264,268],[298,304],[318,322],[353,357],[383,387],[404,420],[440,470],[479,489]]){if(r()>.6)continue;let p=c+r()*(_-c),g=(r()*2-1)*3.5;t.items.push({s:p,u:g,y:ke(p),on:!0,t:0})}if(t.spawnS=4,xh){let c=ve*le,_=1/0;for(let g=0;g+c<t.endS;g+=2)_=Math.min(_,ke(g+c)-ke(g));let p=t.secs.map((g,T)=>+((T+1<t.secs.length?t.secs[T+1].s:t.finishS)-g.s).toFixed(1));console.log("[tower] \uCE35 \uAC04\uACA9 \uCD5C\uC18C",_.toFixed(1),"m \xB7 \uAD6C\uAC04 \uAE38\uC774",p.join(" "),"\xB7 \uB192\uC774",t.secs.map(g=>g.y).join(" "),"\u2192",t.finishY),p.forEach((g,T)=>{Math.abs(g-ii[T].len)>.05&&console.warn("[tower] \uAD6C\uAC04",T+1,"\uAE38\uC774\uAC00 \uADDC\uCE59\uACFC \uB2E4\uB974\uB2E4",g,ii[T].len)})}return t}var bf={},Af=new ee,Rf=new gn,Cf=new sn,i_=new B,s_=new B;function xt(i,t){let e=i+JSON.stringify(t||{});return bf[e]||(bf[e]=new Bt(Object.assign({color:i},t||{})))}function on(i,t,e){let n=document.createElement("canvas");n.width=i,n.height=t,e(n.getContext("2d"),i,t);let s=new Gi(n);return s.colorSpace=ze,s.anisotropy=4,s}var r_=ca.map(i=>{let t=on(128,128,(e,n,s)=>{e.fillStyle=i.top,e.fillRect(0,0,n,s),e.fillStyle=i.stripe;for(let r=0;r<4;r++)e.fillRect(0,r*32,n,16);e.fillStyle="rgba(255,255,255,.55)",e.fillRect(0,0,6,s),e.fillRect(n-6,0,6,s)});return t.wrapS=t.wrapT=un,t}),o_=(()=>{let i=on(128,128,(t,e,n)=>{t.fillStyle="#CFF4FF",t.fillRect(0,0,e,n),t.strokeStyle="rgba(255,255,255,.9)",t.lineWidth=3;for(let s=0;s<6;s++)t.beginPath(),t.moveTo(Math.random()*e,Math.random()*n),t.lineTo(Math.random()*e,Math.random()*n),t.stroke()});return i.wrapS=i.wrapT=un,i})(),a_=["#6FA8FF","#5CE1C6"].map(i=>on(64,64,(t,e,n)=>{t.fillStyle=i,t.fillRect(0,0,e,n),t.strokeStyle="rgba(11,26,51,.32)",t.lineWidth=1.6,t.beginPath(),t.moveTo(10,8),t.lineTo(26,30),t.lineTo(20,46),t.moveTo(26,30),t.lineTo(48,36),t.lineTo(56,54),t.stroke()})),l_=(()=>{let i=on(128,128,(t,e,n)=>{for(let s=-2;s<8;s++)t.fillStyle=s%2?"#111":"#FFD23F",t.beginPath(),t.moveTo(s*32,0),t.lineTo(s*32+32,0),t.lineTo(s*32+32-64,n),t.lineTo(s*32-64,n),t.fill()});return i.wrapS=i.wrapT=un,i})(),Vf=(()=>{let i=on(128,128,(t,e,n)=>{t.fillStyle="#2A3346",t.fillRect(0,0,e,n),t.fillStyle="#3A4560";for(let s=0;s<8;s++)t.fillRect(0,s*16,e,7);t.fillStyle="#FFD23F";for(let s of[32,96])t.beginPath(),t.moveTo(s-22,40),t.lineTo(s,70),t.lineTo(s+22,40),t.lineTo(s+22,56),t.lineTo(s,86),t.lineTo(s-22,56),t.fill();t.fillStyle="rgba(255,255,255,.6)",t.fillRect(0,0,5,n),t.fillRect(e-5,0,5,n)});return i.wrapS=i.wrapT=un,i.repeat.set(4,1),i})(),Wf=(()=>{let i=on(64,128,(t,e,n)=>{t.fillStyle="#FF9F1C",t.fillRect(0,0,e,n),t.fillStyle="#FFE066";for(let s of[8,72])t.beginPath(),t.moveTo(4,s+40),t.lineTo(32,s),t.lineTo(60,s+40),t.lineTo(60,s+56),t.lineTo(32,s+16),t.lineTo(4,s+56),t.fill()});return i.wrapS=i.wrapT=un,i})();function Ti(i,t,e,n=512,s=128){return on(n,s,r=>{r.fillStyle=t,r.fillRect(0,0,n,s),r.fillStyle="rgba(255,255,255,.14)";for(let o=0;o<n/32;o+=2)r.fillRect(o*32,0,32,10),r.fillRect(o*32+32,s-10,32,10);r.fillStyle=e,r.font=`900 ${Math.round(s*.48)}px system-ui, sans-serif`,r.textAlign="center",r.textBaseline="middle",r.fillText(i,n/2,s/2+4)})}function c_(i,t,e){let n=new kt(i,t,e),s=Array.from(n.index.array);return n.setIndex([...s.slice(0,12),...s.slice(18),...s.slice(12,18)]),n.clearGroups(),n.addGroup(0,30,0),n.addGroup(30,6,1),n}function Ff(i){let t=Math.max(4,Math.ceil((i.a1-i.a0)*le/2.5)),e=[],n=[],s=[],r=[],o=i.a1-i.a0,a=o*le,l=(c,_,p,g,T)=>(e.push(c,_,p),n.push(g,T),e.length/3-1),h=(c,_,p,g)=>s.push(c,_,p,c,p,g),u=c=>i.y0+(i.y1-i.y0)*c/t,m=c=>i.a0+o*c/t,x=s.length,f=[];for(let c=0;c<=t;c++){let _=m(c),p=u(c),g=a*c/t/6;f.push([l(i.r0*Math.cos(_),p,i.r0*Math.sin(_),0,g),l(i.r1*Math.cos(_),p,i.r1*Math.sin(_),(i.r1-i.r0)/14,g)])}for(let c=0;c<t;c++)h(f[c][0],f[c+1][0],f[c+1][1],f[c][1]);r.push([x,s.length-x,0]),x=s.length;let v=(c,_)=>{let p=[],g=[];for(let T=0;T<=t;T++){let S=m(T),b=u(T);p.push(l(c*Math.cos(S),b,c*Math.sin(S),T/t,1)),g.push(l(c*Math.cos(S),b-i.h,c*Math.sin(S),T/t,0))}for(let T=0;T<t;T++)_?h(p[T],g[T],g[T+1],p[T+1]):h(p[T],p[T+1],g[T+1],g[T])};v(i.r1,!1),v(i.r0,!0);let d=[];for(let c=0;c<=t;c++){let _=m(c),p=u(c)-i.h;d.push([l(i.r0*Math.cos(_),p,i.r0*Math.sin(_),0,0),l(i.r1*Math.cos(_),p,i.r1*Math.sin(_),1,0)])}for(let c=0;c<t;c++)h(d[c][0],d[c][1],d[c+1][1],d[c+1][0]);for(let c of[0,t]){let _=m(c),p=u(c),g=Math.cos(_),T=Math.sin(_),S=[l(i.r0*g,p,i.r0*T,0,1),l(i.r1*g,p,i.r1*T,1,1),l(i.r1*g,p-i.h,i.r1*T,1,0),l(i.r0*g,p-i.h,i.r0*T,0,0)];c?h(S[0],S[1],S[2],S[3]):h(S[0],S[3],S[2],S[1])}r.push([x,s.length-x,1]);let y=new de;y.setAttribute("position",new Wt(e,3)),y.setAttribute("uv",new Wt(n,2)),y.setIndex(s);for(let[c,_,p]of r)y.addGroup(c,_,p);return y.computeVertexNormals(),y}function h_(i){let t=i.index.array,e=n=>{let s=new de;for(let r of["position","normal","uv"])s.setAttribute(r,i.attributes[r]);return s.setIndex(Array.from(t.slice(n.start,n.start+n.count))),s};return[e(i.groups[0]),e(i.groups[1])]}function u_(i){let t=i.group,e=new Map,n=(d,y,c)=>{e.has(d)||e.set(d,{m:y(),list:[]}),e.get(d).list.push(c)};for(let d of i.solids){let y=ca[d.theme],c;if(d.kind==="arc"){if(d.door){let g=new Bt({map:l_,emissive:0});d.flashMat=g,c=new Z(Ff(d),[g,xt(y.side)]),t.add(c),d.mesh=c;continue}let[_,p]=h_(Ff(d));d.ice?n("ice",()=>new Bt({map:o_}),_):d.belt?n("belt",()=>new Bt({map:Vf}),_):n("top"+d.theme,()=>new Bt({map:r_[d.theme]}),_),n("side"+d.theme,()=>xt(y.side),p);continue}if(d.kind==="disc"){c=new Z(new Gt(d.r,d.r,d.h,48),[xt(y.side),xt(12120319),xt(y.side)]);let _=new Z(new ni(d.r*.2,d.r*.95,48,1,0,Math.PI),xt(8378623));_.rotation.x=-Math.PI/2,_.position.y=d.h/2+.01,c.add(_)}else{let _,p;if(d.fall&&(d.onDrop=g=>{if(!Xs(g.cx,g.top,g.cz))return;let T=g.fake?[16732043,7317759,2772096]:[g.tileColor?6087110:7317759,2772096,16777215];mt.burst(g.cx,g.top,g.cz,{n:g.fake?26:14,speed:4,up:3,colors:T,life:1.2,size:.55,size1:.4,grav:18,drag:.5,soft:!0,shape:1,jitter:1}),g.fake&&mt.word(g.cx,g.top+1.8,g.cz,"\uC640\uB974\uB974","#FF4F8B",{big:.8})}),d.fall?(_=d.fake?new Bt({map:a_[d.tileColor]}):xt(d.tileColor?6087110:7317759),p=xt(2772096)):d.kind==="tilt"?(_=new Bt({map:Ti("\u21C4 \uC5BC\uC74C \uC2DC\uC18C \u21C4","#BFF0FF","#0B4A6B",256,256)}),p=xt(8378623)):d.mystery?(_=new Bt({map:Ti("?","#A77BFF","#FFFFFF",128,128),emissive:2756704}),p=xt(5972406)):d.punch?(_=new Bt({color:d.color,emissive:0}),p=_,d.flashMat=_):(_=xt(d.color!=null?d.color:y.accent),p=xt(d.move?739268:y.side)),c=new Z(c_(d.hx*2,d.h,d.hz*2),[p,_]),d.punch){let g=new Z(new ae(1.25,16,12),d.flashMat);g.scale.set(.7,1,1.35),g.position.x=0,c.add(g)}}t.add(c),d.mesh=c}for(let{m:d,list:y}of e.values())t.add(new Z(Ei(y),d));for(let d of i.hazards)if(d instanceof Qi){let y=new fe,c=new Z(new Gt(.5,.7,2,16),xt(1784427));c.position.y=1,y.add(c);let _=new Z(new ae(.7,16,10),xt(16765503));_.position.y=2.1,y.add(_),d.meshes=d.bars.map((p,g)=>{let T=new Z(new kt(p.len*2,p.y1-p.y0,.5),xt(g?kf:Sf));T.position.y=(p.y0+p.y1)/2;let S=new fe;return S.add(T),y.add(S),S}),y.position.set(d.x,d.base,d.z),t.add(y)}else if(d instanceof Ys){let y=new fe;y.position.set(d.x,d.py,d.z),y.rotation.y=-d.a,[-7.8,7.8].forEach(T=>{let S=new Z(new kt(.6,12,.6),xt(10295117));S.position.set(T,-4.2,0),y.add(S)});let c=new Z(new kt(16.2,.6,.6),xt(10295117));c.position.y=1.5,y.add(c);let _=new fe;y.add(_);let p=new Z(new Gt(.13,.13,d.len,8),xt(13227242));p.position.y=-d.len/2,_.add(p);let g=new Z(new Gt(d.r,d.r,d.r*2.4,22),[xt(Sf),xt(16777215),xt(16777215)]);g.rotation.z=Math.PI/2,g.position.y=-d.len,_.add(g),d.piv=_,t.add(y)}let s=(d,y,c,_)=>{let p=Ee(d,0,y),g=new fe;g.position.set(p.x,y,p.z),g.rotation.y=-p.a,[-Pe-.4,Pe+.4].forEach(w=>{let M=new Z(new Gt(.45,.45,7,12),xt(16777215));M.position.set(w,3.5,0),g.add(M);let E=new Z(new ae(.8,14,10),xt(16765503));E.position.set(w,7.2,0),g.add(E)});let T=Ti(c,_,"#FFFFFF"),S=new kt(15.6,1.7,.5);S.clearGroups(),S.addGroup(0,24,0),S.addGroup(24,12,1);let b=new Z(S,[xt(16777215),new oe({map:T})]);return b.position.y=6.3,g.add(b),t.add(g),g};i.gates.forEach(d=>{d.k>1&&s(d.s,d.y,`STAGE ${d.k} \xB7 ${i.secs[d.k-1].name}`,"#"+ca[d.k-1].accent.toString(16).padStart(6,"0"))}),i.signs.forEach(d=>s(d.s,d.y,d.text,"#FF4F8B")),s(i.finishS,i.finishY,"StepUP \uC815\uC0C1","#1166FF");{let d=Ee(i.finishS,0,i.finishY),y=new fe;y.position.set(d.x,i.finishY,d.z),y.rotation.y=-d.a;let c=new Z(new Qe(16,1.4),new oe({map:on(256,32,_=>{for(let p=0;p<16;p++)for(let g=0;g<2;g++)_.fillStyle=(p+g)%2?"#111":"#fff",_.fillRect(p*16,g*16,16,16)})}));c.rotation.x=-Math.PI/2,c.position.y=.03,y.add(c),t.add(y)}{let d=new fe,y=Ee(i.finishS+6,0,i.topY),c=new Z(new Gt(1.6,1.9,1.3,6,1,!0),new Bt({color:16761856,side:Me,emissive:3811840}));d.add(c);for(let _=0;_<6;_++){let p=new Z(new rn(.42,1.1,6),xt(16761856)),g=_/6*ve;p.position.set(Math.cos(g)*1.7,1.1,Math.sin(g)*1.7),d.add(p);let T=new Z(new ae(.22,8,6),xt(_%2?16732043:2414847));T.position.set(Math.cos(g)*1.85,.1,Math.sin(g)*1.85),d.add(T)}d.position.set(y.x,i.topY+4,y.z),i.crown=d,t.add(d)}let r=i.topY+14;{let d=on(512,256,(p,g,T)=>{let S=["#1166FF","#FFFFFF","#24D8FF","#FFFFFF"];for(let b=0;b<20;b++)p.fillStyle=S[b%4],p.beginPath(),p.moveTo(b*32-64,0),p.lineTo(b*32-32,0),p.lineTo(b*32-96,T),p.lineTo(b*32-128,T),p.fill()});d.wrapS=d.wrapT=un,d.repeat.set(10,4);let y=new Z(new Gt(Si+.02,Si+.02,r+40,64,1,!0),new Bt({map:d,side:Me}));y.position.y=(r-40)/2,t.add(y);let c=new Z(new Gt(Si+1,Si-.05,2,64),xt(16765503));c.position.y=r+1,t.add(c);let _=Ti("StepUP  \xB7  StepUP  \xB7  StepUP  \xB7  StepUP","#0B1A33","#FFFFFF",2048,128);_.wrapS=un,_.repeat.set(2,1);for(let p=12;p<r;p+=18){let g=new Z(new Gt(Si+.05,Si+.05,3,64,1,!0),new oe({map:_}));g.position.y=p,t.add(g)}}i.solids.push(new Pr({kind:"disc",cx:0,cz:0,r:Si,top:r,h:r+60,pillar:!0}));let o=new Z(new Ls(700,64),new Bt({color:3116031,emissive:666234}));o.rotation.x=-Math.PI/2,o.position.y=-24,t.add(o);let a=new Z(new Gt(70,82,6,64),xt(16769955));a.position.y=-24,t.add(a);let l=new Z(new Gt(62,66,6.5,64),xt(8182638));l.position.y=-23.6,t.add(l),i.decor=Mf(i,t,{P:Ee,trackY:ke,RT:le,HW:Pe,PILLAR:Si,TAU:ve,mulberry:qs,canvasTex:on,textTex:Ti,THEMES:ca});let h=on(128,64,(d,y,c)=>{for(let _=0;_<8;_++)d.fillStyle=_%2?"#FFFFFF":"#A77BFF",d.fillRect(_*16,0,16,c)});i.bumpers.forEach(d=>{let y=new fe;y.position.set(d.x,d.y,d.z);let c=new Z(new Gt(d.r,d.r*1.08,1.1,24),[new Bt({map:h,emissive:0}),xt(16732043),xt(16732043)]);c.position.y=.55,y.add(c);let _=new Z(new ae(d.r*.98,20,10,0,ve,0,Math.PI/2),new Bt({color:16732043,emissive:0}));_.position.y=1.1,y.add(_);let p=new Z(new Be(d.r*1.1,.09,6,28),new oe({color:16765503}));p.rotation.x=Math.PI/2,p.position.y=.12,y.add(p),d.mesh=y,d.mats=[c.material[0],_.material],t.add(y)}),i.zones.forEach(d=>{let y=Ee(d.s,d.u,d.y),c=new fe;c.position.set(y.x,d.y+.03,y.z),c.rotation.y=-y.a;let _=Math.atan2(ke(d.s+1)-ke(d.s-1),2);if(d.type==="slime"){let p=new Z(new ae(1,20,10,0,ve,0,Math.PI/2),new Bt({color:6021994,emissive:940572}));p.scale.set(d.hu,.16,d.hs),c.add(p);for(let g=0;g<4;g++){let T=new Z(new ae(.18+g*.05,10,8),xt(10223498));T.position.set((g-1.5)*d.hu*.4,.14,Math.sin(g*2.1)*d.hs*.4),c.add(T)}d.blob=p}else if(d.type==="boost"){let p=new Z(new Qe(d.hu*2,d.hs*2),new oe({map:Wf,polygonOffset:!0,polygonOffsetFactor:-4}));p.rotation.set(-Math.PI/2,0,Math.PI),c.add(p)}else if(d.type==="pad"){let p=new Z(new Gt(d.hu,d.hu*1.1,.24,24),[xt(739268),new Bt({map:Ti("\u21E7","#7CFFB2","#0B4A2B",128,128),emissive:0}),xt(739268)]);p.position.y=.1,p.rotation.y=-Math.PI/2,c.add(p);let g=new Z(new Be(d.hu*.92,.07,6,24),new oe({color:16777215}));g.rotation.x=Math.PI/2,g.position.y=.24,c.add(g),d.top=p}c.rotateOnAxis(new B(1,0,0),-_),d.mesh=c,t.add(c)}),i.cannons.forEach(d=>{let y=Ee(d.s,d.u0+d.side*.9,d.y),c=new fe;if(c.position.set(y.x,d.y,y.z),c.rotation.y=-y.a+(d.side>0?Math.PI/2:-Math.PI/2),d.side>0){let S=new Z(new Gt(.35,.5,9,8),xt(1784427));S.position.y=-3.6,c.add(S)}let _=new Z(new kt(1.6,.9,1.6),xt(727603));_.position.y=.45,c.add(_);let p=new Z(new Gt(.62,.78,2.4,18),new Bt({color:2765638,emissive:0}));p.rotation.x=Math.PI/2,p.position.set(0,.75,-.6),c.add(p);let g=new Z(new Be(.66,.12,6,18),xt(16765503));g.position.set(0,.75,-1.8),c.add(g);let T=new Z(new Qe(.9,.9),new oe({map:Ti("\u{1F4A5}","#FF4F8B","#FFFFFF",128,128),side:Me}));T.position.set(0,1.55,0),c.add(T),d.mesh=c,d.barrelMat=p.material,t.add(c),d.balls=[0,1,2].map(()=>{let S=new Z(new ae(d.r,16,12),xt(2763306)),b=new Z(new Be(d.r*.98,.08,6,18),xt(16732043));return S.add(b),S.visible=!1,t.add(S),S})});{let d=new Be(.26,.09,6,12,Math.PI*1.1);d.rotateX(Math.PI/2),i.bananaMesh=new be(d,new Bt({color:16765503,emissive:3811840}),16),i.bananaMesh.count=0,i.bananaMesh.frustumCulled=!1,t.add(i.bananaMesh)}let u=Ti("?","#1166FF","#FFFFFF",128,128),m=new Bt({map:u,emissive:666234});i.items.forEach(d=>{let y=Ee(d.s,d.u,d.y),c=new fe,_=new Z(new kt(.9,.9,.9),m),p=new Z(new kt(.95,.18,.95),xt(16765503));_.add(p);let g=new Z(new kt(.18,.95,.95),xt(16765503));_.add(g),c.add(_),c.position.set(y.x,d.y+1.1,y.z),d.mesh=c,t.add(c)});let x=new fe,f=new Z(new Gt(.07,.07,3.4,6),xt(16777215));f.position.y=1.7,x.add(f);let v=new Z(new Qe(1.4,.9),new oe({color:16765503,side:Me,transparent:!0,opacity:.85}));v.position.set(.7,3,0),x.add(v),x.visible=!1,i.bestFlag=x,t.add(x)}function f_(i,t,e){for(let n of i.solids){let s=n.mesh;if(!s)continue;if(n.kind==="arc"){n.door&&(s.visible=n.enabled,n.flashMat.emissive.setHex(n.warn&&Math.floor(t/90)%2?6693376:0));continue}let r=n.fall&&n.state==="shake"?Math.sin(t/16)*.07:0;s.position.set(n.cx+r,n.top-n.h/2-(n.fall?n.drop*n.drop*9:0),n.cz),s.rotation.set(0,-n.yaw,n.kind==="tilt"?n.tilt:0),s.visible=!(n.fall&&n.drop>1.6),n.flashMat&&n.flashMat.emissive.setHex(n.flash&&Math.floor(t/90)%2?16720384:0)}for(let n of i.hazards)n instanceof Qi?n.bars.forEach((s,r)=>{n.meshes[r].rotation.y=-s.a}):n instanceof Ys&&(n.piv.rotation.z=n.f);if(i.balls)for(let n of i.balls.list){if(!n.mesh){n.mesh=new Z(new ae(n.r,20,14),xt(16739133));let s=new Z(new Be(n.r*.98,.14,6,24),xt(16777215));s.rotation.y=Math.PI/2,n.mesh.add(s),i.group.add(n.mesh)}n.mesh.position.set(n.x,n.y,n.z),n.mesh.rotation.set(0,-n.a,0),n.mesh.children[0].rotation.x=n.spin}i.crown&&(i.crown.rotation.y+=.02,i.crown.position.y=i.topY+4+Math.sin(t/500)*.4),i.decor&&i.decor.tick(t,e||.016,I);for(let n of i.bumpers){n.pulse=Math.max(0,n.pulse-(e||.016)*4);let s=1+n.pulse*.28;n.mesh.scale.set(s,1-n.pulse*.15,s);let r=n.pulse>.05?6957706:0;n.mats.forEach(o=>o.emissive.setHex(r))}for(let n of i.zones)if(n.pulse=Math.max(0,(n.pulse||0)-(e||.016)*3),n.type==="slime"&&n.blob&&(n.blob.scale.y=.16+Math.sin(t/300+n.s)*.03),n.type==="pad"&&n.top&&(n.top.scale.y=1+n.pulse*2.5,n.top.material[1].emissive.setHex(n.pulse>.05?2787914:0)),n.type==="slime"&&Math.random()<(e||.016)*2.5&&Xs(n.mesh.position.x,n.y,n.mesh.position.z,26)){let s=Ee(n.s+(Math.random()-.5)*n.hs,n.u+(Math.random()-.5)*n.hu,n.y);mt.one(s.x,n.y+.2,s.z,0,1.2,0,{color:10223498,size:.4,size1:.7,life:.6,soft:!0,shape:3})}for(let n of i.cannons){if(n.barrelMat.emissive.setHex(n.warn&&Math.floor(t/80)%2?11145472:0),n.balls.forEach((s,r)=>{let o=n.shots[r];s.visible=!!o,o&&(s.position.set(o.x,o.y,o.z),s.rotation.z+=.3)}),n.fired){n.fired=!1;let s=Ee(n.s,n.u0,n.y+n.r+.1);if(Xs(s.x,s.y,s.z,40)){let r=n.s/le,o=-n.side;mt.burst(s.x,s.y,s.z,{n:18,speed:6,colors:[16765503,16752412,16777215],life:.4,size:.7,vx:Math.cos(r)*o*6,vz:Math.sin(r)*o*6}),mt.dust(s.x,s.y-.4,s.z,{n:8,speed:3,size:.8,colors:[12303291,8947848]}),mt.word(s.x,s.y+1.6,s.z,"\uD391!","#FF9F1C",{big:.8}),I.me&&Math.hypot(I.me.pos.x-s.x,I.me.pos.z-s.z)<18&&Ge("cannon")}}for(let s of n.shots)!s.done&&Math.abs(s.u)>Pe&&Math.sign(s.u)===-n.side&&(s.done=!0,Xs(s.x,s.y,s.z)&&mt.burst(s.x,s.y,s.z,{n:12,speed:5,colors:[16732043,16777215],life:.35,size:.5}))}if(i.bananaMesh){let n=i.bananaMesh;n.count=i.bananas.length,i.bananas.forEach((s,r)=>{Cf.set(0,s.rot,0),Rf.setFromEuler(Cf),Af.compose(i_.set(s.x,s.y+.1,s.z),Rf,s_.set(1,1,1)),n.setMatrixAt(r,Af)}),n.instanceMatrix.needsUpdate=!0}i.items.forEach((n,s)=>{n.mesh&&(n.mesh.visible=n.on,n.mesh.rotation.y=t/600+s,n.mesh.position.y=n.y+1.1+Math.sin(t/300+s)*.15)}),d_(t)}function d_(i){Vf.offset.y=i/1e3*3.1/6%1,Wf.offset.y=-(i/1e3*1.6)%1}var qe={skin:{name:"\uD53C\uBD80",list:["#FFE0C4","#F5C79A","#D9A066","#A86B3C","#6E4224"]},hair:{name:"\uBA38\uB9AC",list:["\uC9E7\uC740 \uBA38\uB9AC","\uC090\uC8FD \uBA38\uB9AC","\uD3EC\uB2C8\uD14C\uC77C","\uB625\uBA38\uB9AC","\uBAA8\uD788\uCE78","\uAE34 \uBA38\uB9AC","\uACF1\uC2AC","\uBBFC\uBA38\uB9AC"]},hairC:{name:"\uBA38\uB9AC\uC0C9",list:["#2A1B12","#6B3E1F","#E0B04A","#1B1B1B","#D9483B","#7F5AF0","#F3F3F3","#2BB3A3","#FF7EB6"]},hat:{name:"\uBAA8\uC790",list:["\uC5C6\uC74C","\uC57C\uAD6C\uBAA8\uC790","\uBE44\uB2C8","\uBC99\uAC70\uC9C0","\uBA38\uB9AC\uB760","\uB9AC\uBCF8","\uBFD4","\uB354\uB4EC\uC774","\uC655\uAD00","\uD5E4\uB4DC\uD3F0"]},top:{name:"\uC637 \uC0C9",list:["#1166FF","#FF4F8B","#FF9F1C","#FFD23F","#3DDC84","#14B8A6","#A77BFF","#E11D48","#0B1A33","#FFFFFF","#7FD8FF","#8B5E34"]},pattern:{name:"\uBB34\uB2AC",list:["\uBBFC\uBB34\uB2AC","\uC904\uBB34\uB2AC","\uBC88\uD638\uD45C","\uBC88\uAC1C","\uBCC4"]},shorts:{name:"\uBC14\uC9C0",list:["#0B1A33","#1B3A6B","#2A2A2A","#FFFFFF","#E11D48","#3DDC84","#FFD23F"]},shoe:{name:"\uC2E0\uBC1C",list:["#FFFFFF","#1166FF","#FF4F8B","#FFD23F","#111111","#3DDC84","#FF9F1C"]},acc:{name:"\uAFB8\uBBF8\uAE30",list:["\uC5C6\uC74C","\uC548\uACBD","\uC120\uAE00\uB77C\uC2A4","\uBC30\uB0AD","\uBAA9\uB3C4\uB9AC","\uBA54\uB2EC","\uB0A0\uAC1C","\uB9DD\uD1A0"]},size:{name:"\uCCB4\uD615",list:["\uBCF4\uD1B5","\uC791\uACE0 \uD1B5\uD1B5","\uD06C\uACE0 \uB0A0\uC52C"]}},yh=Object.keys(qe),Xf={skin:0,hair:0,hairC:0,hat:4,top:0,pattern:2,shorts:0,shoe:0,acc:0,size:0};function qf(i,t){let e={};for(let n of yh)e[n]=i()*qe[n].list.length|0;if(t!=null){let n="#"+t.toString(16).padStart(6,"0").toUpperCase(),s=qe.top.list.findIndex(r=>r.toUpperCase()===n);e.topHex=n,s>=0&&(e.top=s)}return i()<.35&&(e.acc=0),i()<.25&&(e.hat=0),e}function p_(i,t){return t==="top"&&i.topHex?i.topHex:qe[t].list[i[t]%qe[t].list.length]}function m_(i,t,e){return on(128,64,(n,s,r)=>{if(n.fillStyle=t,n.fillRect(0,0,s,r),i===1){n.fillStyle="rgba(255,255,255,.85)";for(let o=0;o<4;o++)n.fillRect(0,6+o*16,s,6)}if(i===2&&(n.fillStyle="#FFFFFF",n.fillRect(s/2-22,12,44,34),n.fillStyle="#0B1A33",n.font="900 24px system-ui, sans-serif",n.textAlign="center",n.textBaseline="middle",n.fillText(String(e),s/2,30)),i===3&&(n.fillStyle="#FFD23F",n.beginPath(),n.moveTo(s/2+6,6),n.lineTo(s/2-12,34),n.lineTo(s/2,34),n.lineTo(s/2-6,58),n.lineTo(s/2+14,26),n.lineTo(s/2+2,26),n.fill()),i===4){n.fillStyle="rgba(255,255,255,.9)";for(let[o,a]of[[20,16],[64,34],[104,14],[40,50],[92,52]]){n.beginPath();for(let l=0;l<10;l++){let h=l%2?3:7,u=-Math.PI/2+l*Math.PI/5;n.lineTo(o+Math.cos(u)*h,a+Math.sin(u)*h)}n.fill()}}})}function ah(i,t,e){let n=Object.assign({},Xf,i||{}),s=R=>p_(n,R),r=xt(s("skin")),o=xt(s("hairC")),a=s("top"),l=new fe,h=new fe;l.add(h);let u=n.size%3,m=u===1?1.12:u===2?.92:1,x=u===1?.9:u===2?1.1:1;h.scale.set(m,x,m),h.userData.base=[m,x,m];let f=new Z(new Vi(.34,.36,6,16),xt(a));f.position.y=.82,h.add(f);let v=n.pattern%qe.pattern.list.length;if(v){let R=new Z(new Gt(.346,.346,.42,20,1,!0,Math.PI*.75,Math.PI*.5),new Bt({map:m_(v,a,e||7),side:Me}));R.position.y=.84,h.add(R)}let d=new Z(new Gt(.35,.33,.2,16),xt(s("shorts")));d.position.y=.56,h.add(d);let y=new Z(new ae(.33,18,14),r);y.position.y=1.36,h.add(y),[-.11,.11].forEach(R=>{let N=new Z(new ae(.045,8,6),xt(727603));N.position.set(R,1.38,-.3),h.add(N)});let c=new Z(new Be(.05,.012,4,10,Math.PI),xt(8007199));c.position.set(0,1.27,-.315),c.rotation.z=Math.PI,h.add(c),[-.17,.17].forEach(R=>{let N=new Z(new ae(.04,8,6),new oe({color:16748451,transparent:!0,opacity:.6}));N.position.set(R,1.3,-.28),h.add(N)});let _=n.hair%qe.hair.list.length,p=(R,N,V,z)=>(R.position.set(N,V,z),h.add(R),R);if(_!==7&&p(new Z(new ae(.345,18,10,0,ve,0,Math.PI*(_===5?.55:.42)),o),0,1.38,0),_===1)for(let R=0;R<6;R++){let N=R/6*ve;p(new Z(new rn(.07,.2,6),o),Math.cos(N)*.17,1.66,Math.sin(N)*.17+.03).rotation.set(Math.sin(N)*.5,0,-Math.cos(N)*.5)}if(_===2){p(new Z(new ae(.1,10,8),o),0,1.5,.33);let R=p(new Z(new Vi(.07,.3,4,8),o),0,1.28,.4);R.rotation.x=.4}if(_===3&&p(new Z(new ae(.15,12,10),o),0,1.74,.06),_===4&&p(new Z(new kt(.08,.2,.56),o),0,1.7,.02),_===5&&p(new Z(new kt(.62,.5,.18),o),0,1.18,.22),_===6)for(let R=0;R<9;R++){let N=R/9*ve;p(new Z(new ae(.12,8,6),o),Math.cos(N)*.26,1.52+R%2*.06,Math.sin(N)*.26+.04)}let g=n.hat%qe.hat.list.length,T=xt(a==="#FFFFFF"?"#1166FF":"#FFFFFF"),S=xt(a);if(g===1&&(p(new Z(new ae(.355,16,8,0,ve,0,Math.PI*.45),S),0,1.41,0),p(new Z(new kt(.4,.04,.3),S),0,1.52,-.36)),g===2&&(p(new Z(new ae(.36,16,10,0,ve,0,Math.PI*.5),S),0,1.42,0),p(new Z(new ae(.1,10,8),T),0,1.8,0),p(new Z(new Gt(.37,.37,.1,18),T),0,1.44,0)),g===3&&(p(new Z(new Gt(.28,.33,.22,18),S),0,1.66,0),p(new Z(new Gt(.5,.5,.03,22),S),0,1.56,0)),g===4){let R=p(new Z(new Be(.335,.055,6,22),t?xt(16765503):S),0,1.45,0);R.rotation.x=Math.PI/2}if(g===5&&[-1,1].forEach(R=>{let N=p(new Z(new rn(.11,.22,8),xt(16732043)),R*.12,1.7,.05);N.rotation.z=R*Math.PI/2}),g===6&&[-1,1].forEach(R=>{let N=p(new Z(new rn(.07,.26,8),xt(16774102)),R*.18,1.72,0);N.rotation.z=-R*.4}),g===7&&[-1,1].forEach(R=>{let N=p(new Z(new Gt(.015,.015,.3,5),xt(727603)),R*.1,1.8,0);N.rotation.z=-R*.3,p(new Z(new ae(.06,8,6),xt(16765503)),R*.15,1.96,0)}),g===8){p(new Z(new Gt(.2,.22,.12,6,1,!0),new Bt({color:16761856,side:Me})),0,1.74,0);for(let R=0;R<6;R++){let N=R/6*ve;p(new Z(new rn(.04,.1,5),xt(16761856)),Math.cos(N)*.2,1.84,Math.sin(N)*.2)}}if(g===9){let R=p(new Z(new Be(.36,.03,6,20,Math.PI),xt(2236962)),0,1.42,0);R.rotation.y=Math.PI/2,[-1,1].forEach(N=>{let V=p(new Z(new Gt(.11,.11,.08,14),S),N*.35,1.36,0);V.rotation.z=Math.PI/2})}let b=n.acc%qe.acc.list.length;if(b===1&&([-.11,.11].forEach(R=>{let N=p(new Z(new Be(.07,.014,6,14),xt(1118481)),R,1.38,-.32)}),p(new Z(new kt(.08,.015,.015),xt(1118481)),0,1.39,-.33)),b===2&&p(new Z(new kt(.4,.09,.05),xt(1118481)),0,1.385,-.31),b===3&&(p(new Z(new kt(.4,.42,.18),xt(qe.top.list[(n.top+3)%12])),0,.9,.34),p(new Z(new kt(.3,.14,.06),xt(16777215)),0,.82,.44)),b===4){let R=p(new Z(new Be(.28,.07,8,18),xt(14753096)),0,1.08,0);R.rotation.x=Math.PI/2,p(new Z(new kt(.1,.32,.05),xt(14753096)),.12,.92,.3)}if(b===5&&(p(new Z(new Gt(.08,.08,.02,16),xt(16761856)),0,.92,-.35).rotation.x=Math.PI/2,p(new Z(new kt(.05,.22,.02),xt(1140479)),0,1.04,-.33)),b===6&&[-1,1].forEach(R=>{let N=p(new Z(new Ls(.28,12),new Bt({color:16777215,side:Me})),R*.24,1,.32);N.rotation.y=R*.9,N.scale.set(1,.6,1)}),b===7){let R=p(new Z(new Qe(.62,.7),new Bt({color:qe.top.list[(n.top+7)%12],side:Me})),0,.78,.36);R.rotation.x=.18}let w=s("shoe"),M=w==="#FFFFFF"?a:"#FFFFFF",E=[];[-.17,.17].forEach(R=>{let N=new fe;N.position.set(R,.5,0);let V=new Z(new Gt(.08,.08,.34,6),r);V.position.y=-.2,N.add(V);let z=new Z(new Gt(.085,.085,.08,8),xt(16777215));z.position.y=-.3,N.add(z);let O=new Z(new kt(.26,.17,.5),xt(w));O.position.set(0,-.4,-.08),N.add(O);let D=new Z(new kt(.27,.06,.52),xt(M));D.position.set(0,-.49,-.08),N.add(D);let X=new Z(new kt(.275,.04,.24),xt(M));X.position.set(0,-.39,-.02),N.add(X),h.add(N),E.push(N)});let F=[];[-.43,.43].forEach(R=>{let N=new fe;N.position.set(R,.98,0);let V=new Z(new Vi(.08,.1,4,8),xt(a));V.position.y=-.04,N.add(V);let z=new Z(new Vi(.065,.14,4,8),r);z.position.y=-.2,N.add(z),h.add(N),F.push(N)});let U=new Z(new ae(1,18,14),new oe({color:8378623,transparent:!0,opacity:.28,depthWrite:!1}));if(U.position.y=.85,U.visible=!1,l.add(U),l.userData.bubble=U,t){let R=new Z(new rn(.22,.4,4),new oe({color:16765503}));R.rotation.x=Math.PI,R.position.y=2.3,l.add(R),l.userData.arrow=R}return Object.assign(l.userData,{body:h,legs:E,arms:F,phase:0}),jc(h),E.forEach(jc),F.forEach(jc),l}var g_=new Bt({vertexColors:!0});function jc(i){let t=[];for(let e of[...i.children]){if(!e.isMesh||Array.isArray(e.material)||!e.material.isMeshLambertMaterial||e.material.map||e.material.transparent||e.material.side!==Dn||e.children.length)continue;e.updateMatrix();let n=e.geometry.index?e.geometry.toNonIndexed():e.geometry.clone();n.applyMatrix4(e.matrix);for(let a of Object.keys(n.attributes))a!=="position"&&a!=="normal"&&n.deleteAttribute(a);let s=e.material.color,r=n.attributes.position.count,o=new Float32Array(r*3);for(let a=0;a<r;a++)o[a*3]=s.r,o[a*3+1]=s.g,o[a*3+2]=s.b;n.setAttribute("color",new we(o,3)),t.push(n),i.remove(e)}t.length&&i.add(new Z(Ei(t),g_))}function x_(i,t){let e=on(256,64,(s,r,o)=>{s.fillStyle="rgba(11,26,51,.72)",s.beginPath(),s.roundRect?s.roundRect(4,6,r-8,o-12,26):s.rect(4,6,r-8,o-12),s.fill(),s.fillStyle=t,s.font="800 30px system-ui, sans-serif",s.textAlign="center",s.textBaseline="middle",s.fillText(i,r/2,o/2+1)}),n=new Mi(new ei({map:e,depthTest:!1,transparent:!0}));return n.scale.set(1.3,.33,1),n.position.y=2.1,n.renderOrder=5,n}function Yf(i,t,e,n,s){let r=i.userData;r.phase+=n*(4+t*1.4);let o=e?.25:Math.min(1,t/6),a=Math.sin(r.phase)*.9*o;r.legs[0].rotation.x=a,r.legs[1].rotation.x=-a,r.arms[0].rotation.x=-a,r.arms[1].rotation.x=a,r.body.position.y=e?0:Math.abs(Math.sin(r.phase))*.08*o,r.body.rotation.x=s==="dive"?-1.25:s==="shove"?-.45:0,r.body.position.z=s==="dive"?-.3:0,s==="shove"&&(r.arms[0].rotation.x=-1.5,r.arms[1].rotation.x=-1.5),r.arrow&&(r.arrow.position.y=2.2+Math.sin(performance.now()/200)*.12)}var Zf=[{key:"ace",name:"\uC5D0\uC774\uC2A4",speed:1,wait:.7,sloppy:.04,skill:.95,color:1140479},{key:"rush",name:"\uB3CC\uC9C4\uC655",speed:1,wait:0,sloppy:.2,skill:.55,rush:1,color:16732043},{key:"bully",name:"\uBC29\uD574\uAFBC",speed:.95,wait:.5,sloppy:.12,skill:.72,bully:1,color:14753096},{key:"block",name:"\uAE38\uB9C9\uC774",speed:.86,wait:.6,sloppy:.1,skill:.7,block:1,color:16752412},{key:"copy",name:"\uB530\uB77C\uC7C1\uC774",speed:.93,wait:.5,sloppy:.12,skill:.76,copy:1,color:10976255},{key:"shy",name:"\uAC81\uC7C1\uC774",speed:.9,wait:1,sloppy:.05,skill:.8,shy:1,color:1357990},{key:"goof",name:"\uC5C9\uB6B1\uC774",speed:.92,wait:.4,sloppy:.15,skill:.5,goof:1,color:16761856},{key:"gamble",name:"\uB3C4\uBC15\uC0AC",speed:.97,wait:.2,sloppy:.18,skill:.5,gamble:1,color:4054148}],If=["ace","rush","bully","copy","goof","gamble","shy","ace","block","bully","copy","goof","rush","gamble","ace"],ga=class{constructor(t){Object.assign(this,{pos:new B,vel:new B,ground:null,onGround:!1,coyote:0,jumpBuf:0,stun:0,safe:0,yaw:0,theta:0,lastA:0,lives:2,out:!1,deadT:0,shoveCd:0,shoveT:0,diveUsed:!1,diving:!1,sec:0,maxS:0,finished:!1,lastShover:null,lastShoveAt:-9,ix:0,iz:0,airS:0,speedK:1},t)}place(t,e){let n=ke(t),s=Ee(t,e,n);this.pos.set(s.x,n+.02,s.z),this.vel.set(0,0,0),this.theta=t/le,this.lastA=Math.atan2(s.z,s.x),this.ground=null,this.onGround=!1,this.stun=0,this.safe=1,this.diving=!1,this.airS=t,this.yaw=Math.atan2(Math.sin(s.a),-Math.cos(s.a))}get s(){return this.theta*le}get u(){return Math.hypot(this.pos.x,this.pos.z)-le}},Pf=[{k:"boost",name:"\uBD80\uC2A4\uD130 \uC2E0\uBC1C",icon:"\u26A1",sec:3.2},{k:"spring",name:"\uC2A4\uD504\uB9C1 \uC2E0\uBC1C",icon:"\u{1F998}",sec:0},{k:"shield",name:"\uBC29\uD328",icon:"\u{1F6E1}\uFE0F",sec:6},{k:"zap",name:"\uBC88\uAC1C \uC6B4\uB3D9\uD654",icon:"\u{1F329}\uFE0F",sec:0}],lh=[{k:"lowg",name:"\uC800\uC911\uB825",icon:"\u{1FA90}"},{k:"fast",name:"\uBE68\uB9AC\uAC10\uAE30",icon:"\u23E9"},{k:"ice",name:"\uAF41\uAF41",icon:"\u{1F9CA}"}],I={ev:null,evT:0,evNext:40,ht:0,mode:"title",world:null,t:0,acc:0,playT:0,bodies:[],me:null,secT0:0,secMs:[],place:0,finishers:0,ko:0,revived:!1,run:null,daily:!1,paused:!1,shovesOnMe:0};function $f(){I.world&&(_n.remove(I.world.group),I.world.group.traverse(t=>{t.geometry&&t.geometry.dispose()}));let i=n_(I.courseSeed);u_(i),_n.add(i.group),I.world=i,I.t=0,I.ht=0;for(let t of i.solids)t.update(0,0),t.save();for(let t of i.hazards)t.update(0);return i}function y_(i){let t=I.world,e=0;for(let n=0;n<t.secs.length;n++)i>=t.secs[n].s&&(e=n);return e}var Lf={shove:["\uD37D!","#FF4F8B"],hammer:["\uCF85!","#FF9F1C"],punch:["\uD321!","#FFD23F"],sweep:["\uD719!","#24D8FF"],ball:["\uCFF5!","#FF6B3D"],bumper:["\uBFC5!","#A77BFF"],cannon:["\uD391!","#FF4F8B"],dive:["\uD37D!","#7CFFB2"],slip:["\uBBF8\uB04C!","#FFD23F"]};function wi(i,t,e){if(!t||i.safe>0||i.shieldT>0){t&&i.shieldT>0&&Ce(i)&&(mt.burst(i.pos.x,i.pos.y+.9,i.pos.z,{n:18,speed:6,colors:[8378623,16777215],life:.4,size:.5}),mt.ring(i.pos.x,i.pos.y+.9,i.pos.z,{r1:2.2,color:8378623,normal:[t.vx,0,t.vz]}));return}if(i.onGround&&(i.airS=i.s),xh&&(i.lastHit=e+"@"+Math.round(i.s)+(i.lastShover?":"+(i.lastShover.persona?i.lastShover.persona.key:"me"):"")),i.vel.x=t.vx,i.vel.z=t.vz,i.vel.y=Math.max(i.vel.y,t.vy),i.stun=t.stun,i.safe=.35,i.onGround=!1,i.ground=null,i.diving=!1,i===I.me&&(Ge("hit"),mt.shake(e==="hammer"||e==="cannon"?.9:.6)),Ce(i)){let n=Lf[e]||Lf.shove,s=i.pos.x,r=i.pos.y+1,o=i.pos.z;mt.burst(s,r,o,{n:i===I.me?30:16,speed:8,colors:[16777215,16765503,parseInt(n[1].slice(1),16)],life:.45,size:.55}),mt.burst(s,r,o,{n:6,speed:5,colors:[16765503,16777215],life:.9,size:.45,shape:2,grav:4}),mt.ring(s,r,o,{r1:2.6,color:parseInt(n[1].slice(1),16),normal:[t.vx,.001,t.vz]}),(i===I.me||Ce(i,16)&&Math.random()<.7)&&mt.word(s,r+1.2,o,n[0],n[1],{big:i===I.me?1.1:.8}),!i.isMe&&Math.random()<.55&&mt.emote(i,["\u{1F635}","\u{1F915}","\u{1F624}","\u{1F640}"][Math.random()*4|0])}}function Ce(i,t=30){if(I.mode==="sim"||I.mode==="simdone"||!i)return!1;let e=I.me;return e?Math.abs(i.pos.y-e.pos.y)<14&&Math.hypot(i.pos.x-e.pos.x,i.pos.z-e.pos.z)<t:!0}function Xs(i,t,e,n=34){if(I.mode==="sim"||I.mode==="simdone")return!1;let s=I.me;return s?Math.abs(t-s.pos.y)<16&&Math.hypot(i-s.pos.x,e-s.pos.z)<n:!0}function Jf(i,t){let e=I.world,n=i.pos,s=i.vel;i.ground&&i.ground.enabled&&i.ground.carry(n);let r=i.ix,o=i.iz,a=Math.hypot(r,o);if(a>1&&(r/=a,o/=a),i.padT=Math.max(0,(i.padT||0)-t),i.slipT=Math.max(0,(i.slipT||0)-t),i.inSlime=!1,i.onGround){let w=i.s,M=i.u;for(let E of e.zones)if(!(Math.abs(w-E.s)>E.hs||Math.abs(M-E.u)>E.hu||Math.abs(n.y-E.y)>.9))if(E.type==="slime"){let F=(w-E.s)/E.hs,U=(M-E.u)/E.hu;F*F+U*U<=1&&(i.inSlime=!0)}else E.type==="boost"?v_(i,E):E.type==="pad"&&i.padT<=0&&__(i,E);if(i.ground&&i.ground.belt){let E=Math.atan2(n.z,n.x),F=i.ground.belt*t;n.x+=-Math.sin(E)*F,n.z+=Math.cos(E)*F}}let l=Ws*i.speedK*(i.boostT>0?1.45:1)*(i.inSlime?.45:1),h=i.onGround&&i.ground&&(i.ground.ice||I.ev==="ice"),u=i.diving?.08:i.stun>0?.12:h?.13:1,m=(i.onGround?Ky:Hf)*u,x=r*l,f=o*l,v=x-s.x,d=f-s.z,y=Math.hypot(v,d),c=m*t;if(y>c?(s.x+=v/y*c,s.z+=d/y*c):(s.x=x,s.z=f),i.onGround&&i.ground&&i.ground.kind==="tilt"){let w=i.ground,M=-Math.sin(w.tilt)*11*t;n.x+=Math.cos(w.yaw)*M,n.z+=Math.sin(w.yaw)*M}for(let w of e.winds)if(w.active(I.ht)&&i.s>=w.s0&&i.s<=w.s1){let M=Math.atan2(n.z,n.x);n.x+=Math.cos(M)*w.force*t,n.z+=Math.sin(M)*w.force*t}if(i.coyote=i.onGround?jy:Math.max(0,i.coyote-t),i.jumpBuf=Math.max(0,i.jumpBuf-t),i.jumpBuf>0&&i.coyote>0&&i.stun<=0){let w=i.springN>0;s.y=Of*(w?1.25:1)*(i.inSlime?.78:1),w&&i.springN--,i.coyote=0,i.jumpBuf=0,i.onGround=!1,i.ground=null,i.diveUsed=!1,i===I.me&&Ge("jump"),Ce(i,18)&&(mt.dust(n.x,n.y,n.z,{n:i===I.me?7:4,speed:2}),w&&(mt.ring(n.x,n.y+.05,n.z,{r1:2,color:8191922}),mt.burst(n.x,n.y+.2,n.z,{n:12,speed:4,colors:[8191922,16777215],life:.4,size:.4})))}else if(i.jumpBuf>0&&!i.onGround&&!i.diveUsed&&i.stun<=0&&i.coyote<=0){let w=[-Math.sin(i.yaw),-Math.cos(i.yaw)];s.x=w[0]*(l+nh),s.z=w[1]*(l+nh),s.y=Math.max(s.y,3),i.diveUsed=!0,i.diving=!0,i.jumpBuf=0,i===I.me&&Ge("dive"),Ce(i,18)&&(mt.burst(n.x,n.y+.6,n.z,{n:10,speed:3,colors:[16777215,8378623],life:.35,size:.45,vx:-s.x*.3,vz:-s.z*.3}),mt.word(n.x,n.y+2,n.z,"\uC29D!","#7FD8FF",{big:.7}))}s.y=Math.max(-45,s.y+zf*(I.ev==="lowg"?.5:1)*t),i.boostT=Math.max(0,(i.boostT||0)-t),i.shieldT=Math.max(0,(i.shieldT||0)-t);let _=n.y;n.x+=s.x*t,n.y+=s.y*t,n.z+=s.z*t,i.stun=Math.max(0,i.stun-t),i.safe=Math.max(0,i.safe-t),i.shoveCd=Math.max(0,i.shoveCd-t),i.shoveT=Math.max(0,i.shoveT-t);let p=null,g=-1/0;if(s.y<=.01)for(let w of e.solids){if(!w.enabled||!w.near(n.x,n.y,n.z))continue;let M=w.topAt(n.x,n.z);if(!w.inside(n.x,n.z,M>_+.01?ce:ce*.5))continue;let E=w.kind==="arc"||w.kind==="tilt"||w===i.ground?Ef+.3:Ef;n.y<=M+.02&&_>=M-E&&M>g&&(p=w,g=M)}let T=i.onGround;i.onGround=!1,p?(i.landV=-s.y,n.y=g,s.y=0,i.onGround=!0,i.ground=p,p.trigger(),i.diving&&(i.diving=!1,i.stun=.3),p.mystery?T_(i):T||S_(i)):(i.ground=null,T&&(i.airS=i.s));for(let w of e.solids){if(!w.enabled||w===p||!w.near(n.x,n.y,n.z))continue;let M=w.topAt(n.x,n.z);if(n.y>=M-.02||n.y+Ri<=w.bottom())continue;let E=w.pushOut(n);if(E&&w.pillar&&Kf(i),E&&w.move)if(w.move.profile==="punch"&&Math.hypot(w.vx,w.vz)>3&&(w.vx*w.cx+w.vz*w.cz)*Math.sign(w.u)<0){let F=Math.hypot(w.vx,w.vz),U=Math.min(1,10/F);wi(i,{vx:w.vx*U,vz:w.vz*U,vy:6,stun:.45},"punch")}else s.x+=(w.vx-s.x)*.35,s.z+=(w.vz-s.z)*.35}for(let w of e.bumpers){if(Math.abs(w.y-n.y)>3)continue;let M=w.touch(i);if(M&&(i.safe<=0||w.pulse<.2)){w.pulse=1;let E={vx:M[0]*9,vz:M[1]*9,vy:4,stun:.2};i.safe>0?(s.x=E.vx,s.z=E.vz):wi(i,E,"bumper"),i===I.me&&Ge("boing")}}if(i.onGround&&e.bananas.length)for(let w=0;w<e.bananas.length;w++){let M=e.bananas[w];if(!(Math.abs(M.y-n.y)>.7||Math.hypot(M.x-n.x,M.z-n.z)>.6||M.owner===i&&I.t-M.t0<1.5)){e.bananas.splice(w,1),M_(i);break}}if(i.safe<=0){for(let w of e.hazards){let M=w.hit(i);if(M){wi(i,M,w.kind);break}}e.balls&&i.safe<=0&&wi(i,e.balls.hit(i),"ball")}let S=Math.atan2(n.z,n.x),b=S-i.lastA;b>Math.PI&&(b-=ve),b<-Math.PI&&(b+=ve),i.theta+=b,i.lastA=S}function __(i,t){let e=i.s/le,n=[-Math.sin(e),Math.cos(e)],s=i.vel,r=s.x*n[0]+s.z*n[1],o=Math.max(8.5,r),a=s.x-n[0]*r,l=s.z-n[1]*r;if(s.set(n[0]*o+a*.4,15.5,n[1]*o+l*.4),i.onGround=!1,i.ground=null,i.airS=i.s,i.padT=.6,i.coyote=0,i.diveUsed=!1,i.jumpBuf=0,t.pulse=1,i===I.me&&(Ge("boing"),mt.shake(.2)),Ce(i)){let h=i.pos;mt.ring(h.x,h.y+.1,h.z,{r1:2.8,color:8191922}),mt.burst(h.x,h.y+.3,h.z,{n:22,speed:6,up:4,colors:[8191922,16777215,2414847],life:.5,size:.5}),mt.word(h.x,h.y+2.4,h.z,"\uC29D~!","#7CFFB2",{big:.9})}}function v_(i,t){let e=!(i.boostT>.25);if(i.boostT=Math.max(i.boostT||0,1.4),t.pulse=1,e&&(i===I.me&&Ge("boost"),Ce(i,22))){let n=i.pos;mt.burst(n.x,n.y+.4,n.z,{n:16,speed:5,colors:[16765503,2414847,16777215],life:.4,size:.5}),mt.word(n.x,n.y+2.3,n.z,"\uBD80\uC6C5!","#FFD23F",{big:.8})}}function M_(i){let t=i.pos;Ce(i)&&mt.burst(t.x,t.y+.2,t.z,{n:10,speed:4,colors:[16769126,16777215],life:.4,size:.45}),!(i.shieldT>0)&&(i.onGround&&(i.airS=i.s),i.vel.x*=.45,i.vel.z*=.45,i.vel.y=3.2,i.stun=.6,i.safe=.3,i.slipT=.6,i.onGround=!1,i.ground=null,i===I.me&&(Ge("slip"),mt.shake(.3)),Ce(i)&&(mt.word(t.x,t.y+2.3,t.z,"\uBBF8\uB04C!","#FFD23F",{big:i===I.me?1.1:.8}),i.isMe||mt.emote(i,"\u{1F34C}")))}function E_(i){let t=I.world,e=i.s/le,n=i.pos;t.bananas.length>=14&&t.bananas.shift(),t.bananas.push({x:n.x+Math.sin(e)*.9,y:n.y,z:n.z-Math.cos(e)*.9,t0:I.t,owner:i,rot:Math.random()*ve}),Ce(i)&&mt.emote(i,"\u{1F61C}")}function S_(i){for(;i.theta>ve&&ke(i.s)-i.pos.y>4;)i.theta-=ve;let t=i.airS-i.s;if(Ce(i,20)&&(i.landV||0)>6){let e=i.pos,n=Math.min(1.6,i.landV/12);mt.dust(e.x,e.y,e.z,{n:Math.round(6+8*n),speed:2+3*n,size:.5+.3*n}),i.landV>13&&(mt.ring(e.x,e.y+.06,e.z,{r1:2.2*n+.8,color:16777215,life:.35}),i===I.me&&mt.shake(.25))}!i.isMe&&t>25&&Ce(i)&&mt.emote(i,["\u{1F631}","\u{1F62D}","\u{1F92F}"][Math.random()*3|0]),i===I.me&&(Ge("land"),t>25&&(vn(`\u2212${Math.round(t)}m \xB7 ${wf[Math.random()*wf.length|0]}`,2.4),Ge("fall"))),i.airS=i.s}function T_(i){let t=i.s/le,e=[-Math.sin(t),Math.cos(t)],n=Math.random()<.55;if(i.vel.set(e[0]*(n?11:-10),n?16:14,e[1]*(n?11:-10)),i.onGround=!1,i.ground=null,i.airS=i.s,i.stun=.25,i===I.me&&(Ge("boing"),vn(n?"\uB7ED\uD0A4! \u{1F340}":"\uC544\u2026 \uB4A4\uB85C \u{1F635}")),Ce(i)){let s=i.pos;mt.burst(s.x,s.y+.3,s.z,{n:28,speed:8,colors:[10976255,16777215,16765503],life:.6,size:.6}),mt.ring(s.x,s.y+.1,s.z,{r1:3.2,color:10976255}),mt.word(s.x,s.y+2.2,s.z,n?"\uB7ED\uD0A4!":"\uC73C\uC545!",n?"#7CFFB2":"#FF4F8B")}}function w_(){let i=I.bodies.filter(t=>!t.out&&t.deadT<=0&&!t.finished);for(let t=0;t<i.length;t++)for(let e=t+1;e<i.length;e++){let n=i[t],s=i[e],r=s.pos.x-n.pos.x,o=s.pos.z-n.pos.z;if(Math.abs(r)>1||Math.abs(o)>1||Math.abs(n.pos.y-s.pos.y)>Ri)continue;let a=Math.hypot(r,o),l=ce*2;if(a>=l||a<1e-4)continue;let h=y=>y.ground&&y.ground.kind==="arc"&&y.ground.r1-y.ground.r0<5;if(!n.isMe&&!s.isMe&&(h(n)||h(s)))continue;let u=n.isMe?0:s.isMe?1:.5,m=1-u,x=l-a,f=r/a,v=o/a;n.pos.x-=f*x*u,n.pos.z-=v*x*u,s.pos.x+=f*x*m,s.pos.z+=v*x*m,Df(n),Df(s);let d=(s.vel.x-n.vel.x)*f+(s.vel.z-n.vel.z)*v;d<0&&(n.vel.x+=f*d*u,n.vel.z+=v*d*u,s.vel.x-=f*d*m,s.vel.z-=v*d*m),n.diving&&s.safe<=0&&(wi(s,{vx:f*8,vz:v*8,vy:4,stun:.35},"dive"),s.lastShover=n,s.lastShoveAt=I.t),s.diving&&n.safe<=0&&(wi(n,{vx:-f*8,vz:-v*8,vy:4,stun:.35},"dive"),n.lastShover=s,n.lastShoveAt=I.t)}}function Df(i){let t=i.pos;for(let e of I.world.solids){if(!e.enabled||e===i.ground||!e.near(t.x,t.y,t.z))continue;let n=e.topAt(t.x,t.z);t.y>=n-.02||t.y+Ri<=e.bottom()||e.pushOut(t)&&e.pillar&&Kf(i)}}function Kf(i){let t=i.pos,e=i.vel,n=Math.hypot(t.x,t.z);if(n<.001)return;let s=t.x/n,r=t.z/n,o=e.x*s+e.z*r;o<0&&(e.x-=s*o,e.z-=r*o)}function b_(i,t){t.on=!1,t.t=9;let e=Pf[Math.random()*Pf.length|0];if(e.k==="boost")i.boostT=e.sec;else if(e.k==="spring")i.springN=2;else if(e.k==="shield")i.shieldT=e.sec;else if(e.k==="zap")for(let n of I.bodies)n===i||n.out||n.deadT>0||n.shieldT>0||Math.hypot(n.pos.x-i.pos.x,n.pos.z-i.pos.z)<9&&Math.abs(n.pos.y-i.pos.y)<3&&(n.stun=Math.max(n.stun,1.3),n.vel.set(0,4,0),n.onGround=!1,n.ground=null);if(i===I.me&&(Ge("item"),vn(`${e.icon} ${e.name}${e.k==="spring"?" \xB7 \uC810\uD504 2\uBC88 \uB192\uC774":e.k==="zap"?" \xB7 \uC8FC\uBCC0 AI \uAE30\uC808!":` \xB7 ${e.sec}\uCD08`}`,1.8)),Ce(i)){let n=i.pos;mt.burst(n.x,n.y+1,n.z,{n:26,speed:7,colors:[2414847,16765503,16777215],life:.6,size:.55}),mt.burst(n.x,n.y+1,n.z,{n:8,speed:4,colors:[16765503],life:1,size:.5,shape:2,grav:3}),mt.ring(n.x,n.y+.9,n.z,{r1:3,color:2414847}),mt.word(n.x,n.y+2.4,n.z,e.icon+" "+e.name.split(" ")[0],"#24D8FF",{big:.75})}if(e.k==="zap"&&Ce(i))for(let n of I.bodies)n!==i&&!n.out&&n.deadT<=0&&n.stun>1&&Math.hypot(n.pos.x-i.pos.x,n.pos.z-i.pos.z)<9&&mt.bolt(i.pos.x,i.pos.y+1.4,i.pos.z,n.pos.x,n.pos.y+1,n.pos.z)}function Lr(i){if(i.shoveCd>0||i.stun>0||i.out||i.deadT>0)return!1;i.shoveCd=Qy,i.shoveT=.25;let t=[-Math.sin(i.yaw),-Math.cos(i.yaw)],e=!1;for(let n of I.bodies){if(n===i||n.out||n.deadT>0||n.finished||n.shieldT>0)continue;let s=n.pos.x-i.pos.x,r=n.pos.z-i.pos.z,o=Math.hypot(s,r);if(o>eh||Math.abs(n.pos.y-i.pos.y)>1.2||(s*t[0]+r*t[1])/Math.max(o,.001)<.35)continue;if(n===I.me&&i!==I.me){if(I.shovesOnMe>=2+I.me.sec)continue;I.shovesOnMe++}let a=9.5/Math.max(o,.5);wi(n,{vx:s*a,vz:r*a,vy:4.5,stun:.4},"shove"),n.lastShover=i,n.lastShoveAt=I.t,e=!0}if(i===I.me&&Ge(e?"shove":"whiff"),Ce(i,20)){let n=i.pos;mt.burst(n.x+t[0]*.9,n.y+.9,n.z+t[1]*.9,{n:e?4:6,speed:2.5,colors:[16777215],life:.3,size:.5,soft:!0,vx:t[0]*5,vz:t[1]*5,grav:0}),e&&!i.isMe&&Math.random()<.5&&mt.emote(i,["\u{1F60E}","\u{1F608}","\u{1F92D}"][Math.random()*3|0])}return e}function Ki(i,t,e){return(dn(Math.atan2(t,i)-dn(e/le,ve)+Math.PI,ve)-Math.PI)*le}function _h(i,t){let e={cx:i.cx,cz:i.cz,yaw:i.yaw,en:i.enabled};if(i.move){let n=i.offset(t);e.cx=i.baseX+i.move.dir[0]*n,e.cz=i.baseZ+i.move.dir[1]*n}return i.spin&&(e.yaw=i.baseYaw+i.spin*t),i.door&&(e.en=dn(t/i.door.period+(i.door.phase||0),1)>=i.door.open),i.fall&&i.state!=="idle"&&(e.en=!1),e}function A_(i,t,e,n,s){if(i.kind==="arc")return i.inside(e,n,s);if(i.kind==="disc"){let a=e-t.cx,l=n-t.cz;return a*a+l*l<=Math.max(0,i.r+s)**2}let[r,o]=i.toLocal(e,n,t);return Math.abs(r)<=i.hx+s&&Math.abs(o)<=i.hz+s}function R_(i,t,e,n){if(i.kind==="arc")return i.topAt(e,n);if(i.kind==="tilt"){let[s]=i.toLocal(e,n,t);return i.top+Math.tan(i.tilt)*Math.max(-i.hx,Math.min(i.hx,s))}return i.top}function ch(i,t,e,n,s,r,o){let a=I.world,l=i.pos,h=-zf*(I.ev==="lowg"?.5:1),u=I.ev==="fast"?1.4:1,m=1/30,x=[];for(let T of a.solids)T.enabled!==void 0&&!T.pillar&&(T.kind!=="arc"||Math.abs(T.y0-l.y)<6||Math.abs(T.y1-l.y)<6)&&(T.near(l.x+t*2.5,l.y,l.z+e*2.5)||T.near(l.x+t*5.5,l.y,l.z+e*5.5))&&x.push(T);let f=l.y,v=l.x,d=l.z,y=o==="dive"?t*n:i.vel.x,c=o==="dive"?e*n:i.vel.z,_=t*n,p=e*n,g=Hf*m;for(let T=m;T<1.8;T+=m){let S=_-y,b=p-c,w=Math.hypot(S,b);w>g?(y+=S/w*g,c+=b/w*g):(y=_,c=p),v+=y*m,d+=c*m;let M=l.y+s*T-.5*h*T*T;if(s-h*T<0)for(let E of x){let F=_h(E,I.ht+T*u);if(!F.en||o&&E.fake||!A_(E,F,v,d,r))continue;let U=R_(E,F,v,d);if(U<=f+.05&&U>=M-.05)return{ok:U>l.y-2.2,top:U,tau:T,sd:E,x:v,z:d}}if(f=M,M<l.y-3)return null}return null}function C_(i,t,e,n,s){let r=Ee(t,e,0);for(let o of i){if(!o.enabled||s&&o.fake||o.fall&&o.state!=="idle"||!o.inside(r.x,r.z,-.2))continue;let a=o.topAt(r.x,r.z);if(a<=n+.7&&a>=n-.9)return!0}return!1}function F_(i){let t=I.world,e=i.pos,n=i.s,s=i.u,r=i.persona,o=i.rnd,a=r.skill,l=i.patient||a>.85,h=t.solids.filter(d=>!d.pillar&&d.near(e.x,e.y,e.z)||d.kind==="arc"&&n+6>=d.s0&&n-1<=d.s0+d.len&&Math.abs(Math.min(d.y0,d.y1)-e.y)<3),u=Ws*i.speedK,m=i.planU!=null?i.planU:s,x=1/0,f=1/0,v=t.winds.some(d=>n>=d.s0-4&&n<=d.s1&&(d.active(I.ht)||d.warn(I.ht)));for(let d=-Pe+.8;d<=Pe-1.4;d+=.6){let y=.22*Math.abs(d-s)+.06*Math.abs(d-(i.lane||0));for(let c of[1.4,2.8,4.4])C_(h,n+c,d,e.y,l)||(y+=4.2/(c*.6));d>Pe-2&&(y+=1.2),v&&d>-2&&(y+=1+(d+2)*.9);for(let c of t.bumpers){let _=c.s-n;_>-.5&&_<5&&Math.abs(c.u-d)<1.7&&Math.abs(c.y-e.y)<2&&(y+=3)}for(let c of t.zones){let _=c.s-n;_<-c.hs||_>6||Math.abs(c.y-e.y)>2||!(Math.abs(c.u-d)<c.hu+.3)||(c.type==="slime"?y+=1+2*a:c.type==="boost"?y-=1.6:c.type==="pad"&&(y-=a>.8||r.gamble||r.rush?1.4:.3))}for(let c of t.bananas){let _=Ki(c.x,c.z,n);_>-.3&&_<5&&Math.abs(Math.hypot(c.x,c.z)-le-d)<.9&&Math.abs(c.y-e.y)<1&&(y+=2+2*a)}for(let c of t.items)c.on&&c.s-n>.8&&c.s-n<8&&Math.abs(c.y-e.y)<1.5&&Math.abs(c.u-d)<.9&&(y-=1.3);if(t.balls)for(let c of t.balls.list){let _=c.s-n;_>-1.5&&_<10&&Math.abs(c.u-d)<c.r+.9&&(y+=5)}for(let c of t.cannons){let _=c.s-n;if(_<-.6||_>5||Math.abs(c.y-e.y)>2)continue;let p=Math.max(0,_)/u;for(let g of c.shots){let T=g.u-c.side*c.speed*p;Math.abs(T-d)<1.4&&(y+=3)}c.warn&&_<2.5&&Math.abs(c.u0-d)<4&&(y+=1)}for(let c of h)if(c.punch){let _=Ki(c.cx,c.cz,n);_>-1&&_<5.5&&Math.sign(c.u)===Math.sign(d)&&Math.abs(d)>.8&&(y+=3)}for(let c of I.bodies)if(c!==i&&!c.out&&c.deadT<=0){let _=c.s-n;_>.3&&_<2.6&&Math.abs(c.u-d)<.9&&Math.abs(c.pos.y-e.y)<1&&(y+=.7)}y+=(o()-.5)*r.sloppy*(1.4-a)*2,Math.abs(d-m)<.31&&(f=y),y<x&&(x=y,i._bu=d)}return f-x<.35?m:i._bu}function jf(i,t){let e=I.world,n=i.pos,s=i.s,r=i.u,o=i.persona,a=i.rnd,l=o.skill,h=s/le,u=[-Math.sin(h),Math.cos(h)],m=[Math.cos(h),Math.sin(h)];if(i.stun>0){i.ix=0,i.iz=0;return}if(i.goofT=Math.max(0,(i.goofT||0)-t),o.goof&&!i.goofT&&a()<t*.14&&(i.goofT=.6+a()*.8,i.goofKind=["stop","back","hop","spin","dance"][a()*5|0],Ce(i,20)&&a()<.6&&mt.emote(i,["\u{1F92A}","\u{1F483}","\u{1F643}","\u{1F3B5}"][a()*4|0])),i.goofT>0&&i.onGround&&!Os(i)){if(i.goofKind==="stop"){i.ix=i.iz=0;return}if(i.goofKind==="back"){i.ix=-u[0]*.6,i.iz=-u[1]*.6,i.yaw=Math.atan2(-i.ix,-i.iz);return}if(i.goofKind==="spin"||i.goofKind==="dance"){let p=I.t*9;i.ix=Math.cos(p)*.4+u[0]*.5,i.iz=Math.sin(p)*.4+u[1]*.5,i.goofKind==="dance"&&i.onGround&&a()<t*4&&(i.jumpBuf=si);return}i.goofKind==="hop"&&i.onGround&&(i.jumpBuf=si)}(o.goof||o.bully)&&i.onGround&&i.ground&&i.ground.kind==="arc"&&i.ground.r1-i.ground.r0>=10&&!i.ground.belt&&a()<t/(o.goof?8:16)&&E_(i),i.planT=(i.planT||0)-t,i.planT<=0&&(i.planT=.07+(1-l)*.18+a()*.05,i.planU=F_(i));let x=i.planU!=null?i.planU:i.lane||0,f=1,v=null;for(let p of e.solids){if(!p.enabled||p.pillar||!p.near(n.x,n.y,n.z)&&p.kind!=="arc")continue;if(p.kind==="arc"){s+3.4>=p.s0&&s+3.4<=p.s0+p.len&&Math.abs(p.y0-n.y)<3&&p.r1-p.r0<13&&(v={u:(p.r0+p.r1)/2-le,half:Math.max(.2,(p.r1-p.r0)/2-.7)});continue}if(p.fall||p.mystery&&!o.gamble||p===i.ground)continue;let g=p.topAt(p.cx,p.cz);if(g>n.y+.7||g<n.y-2.5)continue;let T=_h(p,I.ht+.55),S=Ki(T.cx,T.cz,s);S<.5||S>8.5||(!v||v.ds==null||S<v.ds)&&(v={u:Math.hypot(T.cx,T.cz)-le,half:p.kind==="disc"?p.r*.4:p.hx*.5,ds:S})}v&&(x=v.u+Math.max(-v.half,Math.min(v.half,x-v.u)),v.ds==null&&Math.abs(r-v.u)>v.half+.4&&(f=Math.max(.25,1-(Math.abs(r-v.u)-v.half)/2.5))),x=P_(i,s,r,x,n);for(let p of e.solids){if(!p.move||p.kind!=="box"||!p.near(n.x,n.y,n.z)||p.top<=n.y+.7)continue;let g=Ki(p.cx,p.cz,s);if(g<-1||g>5.5)continue;let T=Math.hypot(p.cx,p.cz)-le;if(p.punch){p.flash&&g<3.2&&Math.sign(p.u)===Math.sign(r)&&i.patient&&(f=0);continue}x=-5;let S=T-p.hx+(p.vx*m[0]+p.vz*m[1])*.3;g<2.2&&g>0&&S<r+.7&&(i.patient||a()<.5)&&(f=0)}for(let p of e.hazards)if(p instanceof Ys){let g=Ki(p.x,p.z,s),T=Math.abs(p.lat-r),b=Math.sign(p.v)===Math.sign(p.lat-r)&&T>1.7;g>2.6&&g<6?b||(f=Math.min(f,.45)):g>=1.9&&g<=2.6&&!b&&(f=i.patient||l>=.7?0:Math.min(f,.25))}for(let p of e.cannons){let g=p.s-s;if(g>.4&&g<2.4&&Math.abs(p.y-n.y)<1.5&&l>.6){for(let T of p.shots){let S=(T.u-r)*-p.side/p.speed;S>-.1&&S<.55&&(f=0)}p.warn&&Math.abs(p.u0-r)<5&&(f=Math.min(f,.5))}}if(e.door&&(e.door.enabled===!1||e.door.warn)){let p=e.door.s0-s;p>0&&p<3.5&&i.patient&&l<.85&&(f=0)}let d=I.me;if(d&&!d.out&&d.deadT<=0&&!d.finished){let p=Math.hypot(d.pos.x-n.x,d.pos.z-n.z),g=v&&v.half<2.2||Os(i)||Os(d);if(o.bully&&p<5&&Math.abs(d.pos.y-n.y)<1.2&&!g){x=d.u+1.1;let T=d.s-s,S=Ee(d.s+2.5,d.u,0),b=Zi(S.x,S.z,d.pos.y+.5);if(T>-1.5&&T<4&&(f=T>.6?1:.7,p<eh-.1&&Math.abs(T)<1.2&&r>d.u+.3&&d.u<Pe-2.6&&b>d.pos.y-.6&&a()<t*3)){i.yaw=Math.atan2(-(d.pos.x-n.x),-(d.pos.z-n.z)),Lr(i);return}}if(o.copy&&p<6&&d.s>s&&!g){let T=i.side||(i.side=r>=d.u?1:-1);x=d.u+T*1.6,d.s-s<2.2&&(f=Math.min(f,.6)),!d.onGround&&i.onGround&&d.vel.y>5&&!i.ground.fall&&(i.jumpBuf=si)}else!o.bully&&!g&&p<2.2&&d.s>s&&Math.abs(d.u-r)<1&&(x=d.u+(r>=d.u?1.4:-1.4));p<3&&d.s<s&&!i.passedMe&&Ce(i,10)&&(i.passedMe=!0,a()<.5&&mt.emote(i,["\u{1F61C}","\u{1F44B}","\u{1F3C3}","\u{1F60E}"][a()*4|0])),d.s>s+4&&(i.passedMe=!1)}if((o.bully||o.rush)&&i.shoveCd<=0&&i.onGround&&!Os(i)&&a()<t*(o.bully?1.2:.7))for(let p of I.bodies){if(p===i||p.isMe||p.out||p.deadT>0||p.finished||Os(p))continue;let g=p.pos.x-n.x,T=p.pos.z-n.z;if(Math.hypot(g,T)<eh-.2&&Math.abs(p.pos.y-n.y)<1&&g*u[0]+T*u[1]>-.3){i.yaw=Math.atan2(-g,-T),Lr(i);return}}o.block&&v&&v.half<2&&(x=v.u+Math.sin(I.t*1.7+i.seed)*1),o.shy&&(f===0?i.shyT=.5:i.shyT>0&&(i.shyT-=t,i.shyT>.38&&(f=0)));for(let p of e.winds)s>=p.s0-3&&s<=p.s1&&(p.active(I.ht)||p.warn(I.ht))&&(x=Math.min(x,-3),r>.5&&(f=Math.min(f,.55)));if(i.maxS>(i.progS||0)+1?(i.progS=i.maxS,i.progT=I.t):I.t-(i.progT||0)>8&&i.onGround&&(i.frustT=1.2,i.progT=I.t,Ce(i,14)&&mt.emote(i,"\u{1F624}")),(i.frustT=Math.max(0,(i.frustT||0)-t))>0&&(f=1),x=Math.max(-Pe+.8,Math.min(Pe-1.4,x)),!i.onGround){I_(i,x,u,m);return}let y=Math.max(-1,Math.min(1,(x-r)*.9)),c=u[0]*f+m[0]*y,_=u[1]*f+m[1]*y;if(i.edgeWait=i.edgeWait||0,f>0){let p=Math.hypot(c,_)||1,g=c/p,T=_/p,S=Ws*i.speedK*(i.boostT>0?1.45:1)*(i.inSlime?.45:1)*Math.min(1,p),b=.42+Math.min(1,S/Ws)*.3,w={x:n.x+g*b,z:n.z+T*b},M=Zi(w.x,w.z,n.y+.5),E=Ee(s+.9,r,0),F=Zi(E.x,E.z,n.y+1.2),U={x:n.x+g*(b+.55),z:n.z+T*(b+.55)},R=Zi(U.x,U.z,n.y+.5),N=R>n.y-.6&&R<n.y+.5,V=(M===-1/0||M<n.y-.6)&&!N,z=F>n.y+.35;if(V){let O=i.edgeL;if(!(i.edgeEvalT>I.t)){let D=l>.35&&a()<.5+l*.6,X=Of*(i.springN>0?1.25:1)*(i.inSlime?.78:1);O=i.edgeL=D?ch(i,g,T,Math.max(S,Ws*i.speedK*.9),X,-.3*l,i.patient):{ok:!0},i.edgeEvalT=I.t+.05}if(O&&O.ok&&O.sd&&O.sd.kind==="disc"&&l>=.6){let D=!1;for(let X of e.hazards)if(X instanceof Qi&&Math.hypot(X.x-O.sd.cx,X.z-O.sd.cz)<.6)for(let $ of X.bars){let tt=I.ht+O.tau*(I.ev==="fast"?1.4:1),ut=$.spin*tt+($.phase||0),Ct=Math.atan2(O.z-X.z,O.x-X.x),J=$.spin-O.sd.spin;(J>0?dn(Ct-ut,Math.PI):dn(ut-Ct,Math.PI))/Math.max(.05,Math.abs(J))<.5&&(D=!0)}D&&(O=null)}if(O&&O.ok)z=!0,i.edgeWait=0,i.aim=O.sd||null,i.aimT=I.ht+(O.tau||.7)*(I.ev==="fast"?1.4:1);else{i.edgeWait+=t;let D=i.ground&&i.ground.fall?0:1.2+l*4+(i.patient?1.5:0);i.edgeWait>D?(z=!0,i.edgeWait=0,Ce(i,16)&&mt.emote(i,"\u{1F62C}")):(c=m[0]*y*.6,_=m[1]*y*.6,i.edgeWait>1.6&&!i.waitEmo&&Ce(i,14)&&(i.waitEmo=!0,mt.emote(i,["\u{1F630}","\u{1F914}","\u{1F624}"][a()*3|0])))}}else i.edgeWait=Math.max(0,i.edgeWait-t*2),i.waitEmo=!1;for(let O of e.hazards)if(O instanceof Qi&&Math.abs(O.base-n.y)<.8){let D=n.x-O.x,X=n.z-O.z,$=Math.hypot(D,X),tt=Math.atan2(X,D);for(let ut of O.bars){if($>ut.len+.6||$<.9)continue;let Ct=i.ground&&i.ground.spin&&Math.hypot(i.ground.cx-O.x,i.ground.cz-O.z)<.5?i.ground.spin:0,J=(ut.spin-Ct)*(I.ev==="fast"?1.4:1);if(Math.abs(J)<.05)continue;let st=(J>0?dn(tt-ut.a,Math.PI):dn(ut.a-tt,Math.PI))/Math.abs(J),_t=(a()-.5)*o.sloppy*(1.3-l)*.6,rt=ut.y0>.9;st>(rt?.25:.15)+_t&&st<(rt?.38:.28)+_t&&(z=!0)}}if(e.balls)for(let O of e.balls.list){let D=O.s-s;Math.abs(O.u-r)<O.r+.6&&D>0&&D/(O.v+S)<.32+(1-l)*.1&&a()<l&&(z=!0)}for(let O of e.cannons){let D=O.s-s;if(Math.abs(D)<1.4&&Math.abs(O.y-n.y)<1.5)for(let X of O.shots){let $=(X.u-r)*-O.side/O.speed;$>.05&&$<.3&&a()<.3+l&&(z=!0)}}z&&i.stun<=0&&(i.jumpBuf=si),o.gamble&&a()<t*.03&&(i.jumpBuf=si),o.rush&&!i.diveUsed&&a()<t*.06&&!V&&!Os(i)&&(i.jumpBuf=si)}i.ix=c,i.iz=_,i.yaw=Math.atan2(-c,-_)}function Os(i){return i.ground&&i.ground.kind==="arc"&&i.ground.r1-i.ground.r0<5}function I_(i,t,e,n){let s=i.pos,r=i.persona.skill,o=i.vel,a=null,l=null;if(i.aim&&i.aim.enabled!==!1){let u=_h(i.aim,Math.max(I.ht,i.aimT||I.ht));i.aim.kind!=="arc"&&(a=u.cx,l=u.cz)}let h=o.x*e[0]+o.z*e[1];if(a!=null&&r>.4){let u=a-s.x,m=l-s.z,x=u*n[0]+m*n[1];i.ix=e[0]*1+n[0]*Math.max(-1,Math.min(1,x*.6)),i.iz=e[1]*1+n[1]*Math.max(-1,Math.min(1,x*.6))}else{let u=Math.max(-1,Math.min(1,(t-i.u)*.7));i.ix=e[0]+n[0]*u,i.iz=e[1]+n[1]*u}if(i.yaw=Math.atan2(-i.ix,-i.iz),!i.diveUsed&&o.y<1.5&&i.stun<=0&&r>.3&&i.rnd()<.3+r){let u=Math.hypot(i.ix,i.iz)||1,m=i.ix/u,x=i.iz/u,f=Ws*i.speedK,v=ch(i,o.x/(Math.hypot(o.x,o.z)||1),o.z/(Math.hypot(o.x,o.z)||1),Math.hypot(o.x,o.z),o.y,-.1,!1);if(!v||!v.ok){let d=ch(i,m,x,f+nh,Math.max(o.y,3),-.6,"dive");d&&d.ok&&d.sd.kind==="arc"&&d.sd.r1-d.sd.r0>=8&&Math.hypot(d.x,d.z)<d.sd.r1-1.5&&(i.jumpBuf=si,i.aim=d.sd)}}}function P_(i,t,e,n,s){let r=i.patient||i.persona.skill>.85,o=u=>{let m=Ee(t+1.8,u,0);for(let x of I.world.solids){if(!x.enabled||x.pillar||!x.near(m.x,s.y,m.z)||!x.inside(m.x,m.z,0))continue;let f=x.topAt(m.x,m.z);if(Math.abs(f-s.y)<1.2&&!(r&&x.fake)&&x.state!=="shake")return!0}return!1};if(!I.world.solids.some(u=>u.fall&&u.near(s.x,s.y,s.z)&&Math.abs(Ki(u.cx,u.cz,t))<4)||o(n))return n;let l=n,h=1/0;for(let u of[-2.9,2.9,-5.8,5.8,-1.45,1.45]){let m=Math.max(-Pe+.8,Math.min(Pe-.8,e+u));o(m)&&Math.abs(u)<h&&(h=Math.abs(u),l=m)}return l}function Zi(i,t,e){let n=-1/0;for(let s of I.world.solids)if(s.enabled&&!s.pillar&&s.near(i,e,t)&&s.inside(i,t,.1)){let r=s.topAt(i,t);r<=e+.05&&r>n&&(n=r)}return n}var ua=[];function Qf(i){I.courseSeed=i!=null?i*977+13:Math.random()*1e9|0,$f(),I.ev=null,I.evT=0,I.evNext=28+Math.random()*14,Object.assign(I,{bodies:[],secMs:[],place:0,finishers:0,ko:0,revived:!1,playT:0,secT0:0,shovesOnMe:0});for(let r of ua)_n.remove(r);ua.length=0;let t=qs(i??Date.now()&16777215^24301),e=[];for(let r=0;r<4;r++)for(let o=0;o<4;o++)e.push([2+r*2.4,-4.8+o*3.2]);let n=5;I.me=new ga({name:"\uB098",isMe:!0}),I.me.place(e[n][0],e[n][1]),I.me.mesh=ah(ld(),!0,1),I.bodies.push(I.me);let s=0;e.forEach((r,o)=>{if(o===n)return;let a=Zf.find(h=>h.key===If[s%If.length]);s++;let l=new ga({lives:3,persona:a,name:a.name,rnd:qs(t()*1e9|0),seed:t()*6,lane:(t()*2-1)*2.5,patient:a.skill>=.9||t()<.6+a.wait*.4,speedK:Math.min(.97,a.speed*(.93+t()*.09))});l.place(r[0],r[1]),l.mesh=ah(qf(l.rnd,a.color),!1,2+(t()*98|0)),l.tag=x_(a.name,"#"+a.color.toString(16).padStart(6,"0")),l.mesh.add(l.tag),I.bodies.push(l)});for(let r of I.bodies)_n.add(r.mesh),ua.push(r.mesh),r.maxS=r.s}function L_(i){if(i.out||i.deadT>0||i.finished||i.onGround)return;let t=ke(Math.min(i.s,I.world.endS));if(i.pos.y>e_&&(i.pos.y>t-t_||Zi(i.pos.x,i.pos.z,i.pos.y)>-1/0))return;let e=i!==I.me&&i.lastShover===I.me&&I.t-i.lastShoveAt<3;if(e&&(I.ko++,vn("\uBC00\uC5B4\uC11C \uB5A8\uC5B4\uB728\uB9BC +50")),i.lives--,i===I.me&&Ge("splash"),Ce(i,60)&&(mt.splash(i.pos.x,-23.6,i.pos.z,i===I.me?1.3:.8),e&&mt.word(i.pos.x,Math.max(i.pos.y,I.me.pos.y-2)+2,i.pos.z,"KO! +50","#FFD23F",{big:1.2,life:1.3}),i===I.me&&mt.shake(.5)),i.lives<=0){i.out=!0,i.mesh.visible=!1,i===I.me&&hh(!1,"dead");return}i.deadT=1.2,i.mesh.visible=!1,i===I.me&&(I.revived=!0,vn("\uD48D\uB369! \uBD80\uD65C \u2014 \uB9C8\uC9C0\uB9C9 \uBAA9\uC228\uC774\uC5D0\uC694",2))}function D_(i,t){if(i.deadT-=t,i.deadT<=0){let e=I.world.secs[i.sec];if(i.place(e.s+2.5,i===I.me?0:(i.lane||0)*.5),i.mesh.visible=!0,Ce(i)){let n=i.pos;mt.burst(n.x,n.y+.5,n.z,{n:30,speed:3,up:6,colors:[8191922,16777215,16765503],life:.9,size:.5,grav:-2}),mt.ring(n.x,n.y+.05,n.z,{r1:2.4,color:8191922})}}}function td(i){let t=I.world;if(I.t+=i,I.ev)I.evT-=i,I.evT<=0&&(I.ev=null,I.mode==="play"&&vn("\uC774\uBCA4\uD2B8 \uB05D!",1));else if((I.evNext-=i)<=0){let s=lh[Math.random()*lh.length|0];I.ev=s.k,I.evT=8,I.evNext=30+Math.random()*20,I.mode==="play"&&(Dr(`<small>\uB3CC\uBC1C \uC774\uBCA4\uD2B8</small><b>${s.icon} ${s.name}</b><span>8\uCD08</span>`,"event",1.8),Ge("event"))}let e=i*(I.ev==="fast"?1.4:1);I.ht+=e;for(let s of t.solids)s.save();for(let s of t.tilts){let r=0;for(let a of I.bodies)if(a.ground===s&&!a.out){let[l]=s.toLocal(a.pos.x,a.pos.z);r+=l}let o=Math.max(-.3,Math.min(.3,-r*.035));s.tilt+=(o-s.tilt)*Math.min(1,i*2.2)}for(let s of t.solids)s.update(I.ht,e);for(let s of t.hazards)s.update(I.ht);let n=0;for(let s of I.bodies)s.out||(n=Math.max(n,s.s));t.balls&&t.balls.update(I.ht,e,t,I.me&&!I.me.out?I.me.s:n),t.bananas.length&&I.t-t.bananas[0].t0>35&&t.bananas.shift();for(let s of t.items){if(!s.on){s.t-=i,s.t<=0&&(s.on=!0);continue}let r=s.q||(s.q=Ee(s.s,s.u,s.y));for(let o of I.bodies)if(!o.out&&o.deadT<=0&&!o.finished&&Math.abs(o.pos.x-r.x)<1.1&&Math.abs(o.pos.z-r.z)<1.1&&Math.abs(o.pos.y-s.y)<1.8){b_(o,s);break}}for(let s of I.bodies)if(!(s.out||s.finished)){if(s.deadT>0){D_(s,i);continue}s.isMe||jf(s,i),Jf(s,i)}w_();for(let s of I.bodies)if(!(s.out||s.finished||s.deadT>0)){if(s.onGround){s.maxS=Math.max(s.maxS,s.s);let r=y_(s.s);r>s.sec&&(s.sec=r,s===I.me&&Uf(r))}if(s.s>=t.finishS&&s.pos.y>t.finishY-1.5&&(s.finished=!0,I.finishers++,s.rank=I.finishers,!s.isMe&&Ce(s,40)&&mt.emote(s,["\u{1F389}","\u{1F3C6}","\u{1F60E}","\u{1F973}"][Math.random()*4|0]),s===I.me)){I.place=I.finishers,Uf(t.secs.length),hh(!0);return}if(L_(s),I.mode!=="play"&&I.mode!=="sim")return}I.playT>Zc&&I.mode==="play"&&hh(!1,"time")}function Uf(i){let t=Math.round((I.playT-I.secT0)*1e3);I.secMs.push(t),I.secT0=I.playT,I.mode!=="sim"&&(Ge("win"),i<I.world.secs.length&&(Dr(`<small>STAGE ${i+1}</small><b>${I.world.secs[i].name}</b>`,"good",1.6),Ma(I.world.secs[i].s,I.world.secs[i].y,1)),I.daily&&I.run&&ts("tower_ping",{p_run:I.run,p_section:I.secMs.length,p_ms:t}).catch(()=>null))}var dt=i=>document.getElementById(i),bi=new mr({canvas:dt("stage"),antialias:(window.devicePixelRatio||1)<2,powerPreference:"high-performance"});bi.setPixelRatio(Math.min(window.devicePixelRatio||1,1.75));bi.outputColorSpace=ze;var _n=new gr;_n.background=on(4,256,(i,t,e)=>{let n=i.createLinearGradient(0,0,0,e);n.addColorStop(0,"#3B8EFF"),n.addColorStop(.55,"#9ED4FF"),n.addColorStop(1,"#E8F6FF"),i.fillStyle=n,i.fillRect(0,0,t,e)});_n.fog=new Bo(12575743,120,320);_n.add(new br(15398143,4872827,1.7));var ed=new Ar(16777215,1.6);ed.position.set(30,60,20);_n.add(ed);var zn=new Je(60,1,.1,900),$i=new Z(new Ls(.5,20),new oe({color:0,transparent:!0,opacity:.28,depthWrite:!1}));$i.rotation.x=-Math.PI/2;_n.add($i);var mt=yf(_n);function Ma(i,t,e,n){let s=i/le,r=[-Math.sin(s),Math.cos(s)],o=[Math.cos(s),Math.sin(s)];for(let l of[-1,1]){let h=Ee(i,l*(Pe+.4),t);mt.cannon(h.x,t+7.3,h.z,-o[0]*l*.6+r[0]*.5,-o[1]*l*.6+r[1]*.5,Math.round(45*e))}let a=Ee(i,0,t);mt.ring(a.x,t+6.3,a.z,{r1:10*e,color:16765503,life:.6,normal:[r[0],0,r[1]]});for(let l=0;l<Math.round(3*e);l++){let h=Ee(i+6+Math.random()*10,(Math.random()*2-1)*12+10,0);mt.firework(h.x,t+2,h.z,{delay:l*.22,big:.8+.3*e})}I.me&&!n&&mt.word(I.me.pos.x,I.me.pos.y+2.8,I.me.pos.z,e>1.5?"\uC815\uC0C1!":"\uD1B5\uACFC!","#FFD23F",{big:1.4,life:1.2})}function U_(i){mt.tick(i,zn)}var St={title:dt("title"),hud:dt("hud"),stage:dt("hudStage"),score:dt("hudScore"),time:dt("hudTime"),pos:dt("hudPos"),lives:dt("hudLives"),banner:dt("banner"),over:dt("over"),board:dt("board"),toast:dt("toast"),controls:dt("controls"),join:dt("join"),sns:dt("sns"),wind:dt("hudWind"),fx:dt("hudFx"),look:dt("look")};function nd(i){let t=Math.max(0,i)/1e3,e=Math.floor(t/60),n=t-e*60;return(e?e+":"+(n<10?"0":""):"")+n.toFixed(1)}var se=(i,t)=>{i.hidden=!t},fa=0,da=0;function vn(i,t){I.mode!=="sim"&&(St.toast.textContent=i,St.toast.hidden=!1,fa=t||1.3)}function Dr(i,t,e){St.banner.innerHTML=i,St.banner.className="banner "+(t||""),St.banner.hidden=!1,da=e||0}function vh(i){return String(i??"").replace(/[&<>"']/g,t=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"})[t])}function id(){let i=I.world,t=I.secMs.length;return t<i.secs.length?Math.max(0,Math.round(I.me.maxS-i.secs[t].s)):0}function sd(){let i=I.me,t=I.world;St.stage.textContent=`STAGE ${Math.min(i.sec+1,t.secs.length)} \xB7 ${t.secs[Math.min(i.sec,t.secs.length-1)].name}`,St.score.textContent=ia(I.secMs.length,I.secMs,id(),!1,0,I.revived,I.ko).toLocaleString("ko-KR")+"\uC810",St.time.textContent=nd((Zc-I.playT)*1e3);let e=0;for(let r of I.bodies)r!==i&&!r.out&&(r.finished||r.s>i.s)&&e++;St.pos.textContent=`${e+1}\uC704 \xB7 \uB0A8\uC740 ${I.bodies.filter(r=>!r.out).length}\uBA85`,St.lives.textContent=i.lives>=2?"\u2764\uFE0F\u2764\uFE0F":"\u2764\uFE0F\u{1F90D}";let n=[];if(i.boostT>0&&n.push("\u26A1 "+i.boostT.toFixed(1)),i.springN>0&&n.push("\u{1F998} \xD7"+i.springN),i.shieldT>0&&n.push("\u{1F6E1}\uFE0F "+i.shieldT.toFixed(1)),I.ev){let r=lh.find(o=>o.k===I.ev);n.push(`${r.icon} ${r.name} ${I.evT.toFixed(0)}`)}St.fx.textContent=n.join(" \xB7 "),St.fx.hidden=!n.length;let s=t.winds.find(r=>i.s>=r.s0-6&&i.s<=r.s1);if(St.wind.hidden=!(s&&(s.active(I.ht)||s.warn(I.ht))),t.bestFlag.visible=i.maxS-i.s>15,t.bestFlag.visible){let r=Ee(i.maxS,-Pe+1,ke(i.maxS));t.bestFlag.position.set(r.x,r.y,r.z),t.bestFlag.rotation.y=-r.a}}var ge={email:"",name:"",status:null};try{ge.email=localStorage.getItem("tower_email")||"",ge.name=localStorage.getItem("tower_name")||""}catch{}async function ts(i,t){if(!Kc.url)throw new Error("no server");let e=await fetch(Kc.url+"/rest/v1/rpc/"+i,{method:"POST",headers:{"Content-Type":"application/json",apikey:Kc.key},body:JSON.stringify(t),cache:"no-store"});if(!e.ok)throw new Error("http "+e.status);return e.json()}var N_=/^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 _.\-]{2,12}$/,Ji={email:"\uC774\uBA54\uC77C \uC8FC\uC18C\uB97C \uB2E4\uC2DC \uD655\uC778\uD574 \uC8FC\uC138\uC694.",not_registered:"\uC0AC\uC804 \uB4F1\uB85D\uD55C \uC774\uBA54\uC77C\uC774 \uC544\uB2C8\uC5D0\uC694.",name:"\uB2C9\uB124\uC784\uC740 2~12\uC790, \uD55C\uAE00 \xB7 \uC601\uBB38 \xB7 \uC22B\uC790\uB85C \uC801\uC5B4 \uC8FC\uC138\uC694.",busy:"\uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.",no_runs:"\uC624\uB298 \uAE30\uD68C\uB97C \uB2E4 \uC37C\uC5B4\uC694. SNS\uC5D0 \uC62C\uB9AC\uBA74 \uD55C \uBC88 \uB354 \uD560 \uC218 \uC788\uC5B4\uC694.",url:"X \xB7 \uC778\uC2A4\uD0C0 \xB7 \uC2A4\uB808\uB4DC \uAC8C\uC2DC\uBB3C \uB9C1\uD06C\uAC00 \uC544\uB2C8\uC5D0\uC694.",account:"\uACC4\uC815 \uC544\uC774\uB514\uB97C \uC601\uBB38 \xB7 \uC22B\uC790 \xB7 \uBC11\uC904 \xB7 \uC810\uC73C\uB85C \uC801\uC5B4 \uC8FC\uC138\uC694.",duplicate:"\uC774\uBBF8 \uB0B8 \uAC8C\uC2DC\uBB3C\uC774\uC5D0\uC694. \uC0C8 \uAC8C\uC2DC\uBB3C\uC744 \uC62C\uB824 \uC8FC\uC138\uC694.",day_limit:"\uC624\uB298\uC740 SNS\uB85C \uBC1B\uC744 \uC218 \uC788\uB294 \uAE30\uD68C(3\uBC88)\uB97C \uB2E4 \uC37C\uC5B4\uC694. 00\uC2DC\uC5D0 \uB2E4\uC2DC \uCC44\uC6CC\uC838\uC694.",consent:"\uB3D9\uC758\uC5D0 \uCCB4\uD06C\uD574 \uC8FC\uC138\uC694.",closed:"\uC9C0\uAE08\uC740 \uAC8C\uC784\uC744 \uC26C\uACE0 \uC788\uC5B4\uC694."};function Ur(){let i=ge.status,t=dt("whoLine"),e=dt("dailyBtn");if(!i){t.innerHTML=ge.email?"":'<span class="muted">\uC0AC\uC804 \uB4F1\uB85D\uD55C \uC774\uBA54\uC77C\uB85C \uCC38\uAC00\uD574\uC694</span>',e.textContent="\uC624\uB298\uC758 \uB3C4\uC804";return}t.innerHTML=`<b>${vh(i.name)}</b> \xB7 \uC624\uB298 \uCD5C\uACE0 ${i.best_today!=null?(+i.best_today).toLocaleString("ko-KR")+"\uC810":"\u2014"} <button class="link" id="switchAcc" type="button">\uBC14\uAFB8\uAE30</button>`,dt("switchAcc").onclick=()=>xa(!0),e.textContent=i.runs_left>0?`\uC624\uB298\uC758 \uB3C4\uC804 (${i.runs_left}\uBC88 \uB0A8\uC74C)`:"SNS \uC62C\uB9AC\uACE0 \uD55C \uBC88 \uB354 \uD558\uAE30"}async function Mh(){if(!ge.email){ge.status=null,Ur();return}try{let i=await ts("tower_status",{p_email:ge.email});i&&i.ok?ge.status=i:i&&(i.code==="not_joined"||i.code==="not_registered")&&(ge.status=null)}catch{}Ur()}function xa(){se(St.title,!1),se(St.join,!0),dt("joinEmail").value=ge.email,dt("joinName").value=ge.name,dt("joinErr").textContent=""}dt("joinForm").onsubmit=async i=>{i.preventDefault();let t=dt("joinEmail").value.trim().toLowerCase(),e=dt("joinName").value.replace(/\s+/g," ").trim(),n=dt("joinErr"),s=dt("joinBtn");if(n.textContent="",!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(t)){n.textContent=Ji.email;return}if(!N_.test(e)){n.textContent=Ji.name;return}if(!dt("joinConsent").checked){n.textContent=Ji.consent;return}s.disabled=!0,s.textContent="\uD655\uC778 \uC911\u2026";try{let r=await ts("tower_join",{p_email:t,p_name:e,p_trap:dt("joinTrap").value});if(r&&r.ok){ge.email=t,ge.name=e,ge.status=r;try{localStorage.setItem("tower_email",t),localStorage.setItem("tower_name",e)}catch{}se(St.join,!1),se(St.title,!0),Ur()}else n.textContent=Ji[r&&r.code]||"\uD655\uC778\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.",r&&r.code==="not_registered"&&n.insertAdjacentHTML("beforeend",' <a href="/?utm_source=game&amp;utm_medium=web#join">\uC0AC\uC804 \uB4F1\uB85D\uD558\uAE30</a>')}catch{n.textContent="\uC5F0\uACB0\uC774 \uB04A\uACBC\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."}s.disabled=!1,s.textContent="\uCC38\uAC00\uD558\uAE30"};dt("joinBack").onclick=()=>{se(St.join,!1),se(St.title,!0)};async function rd(){if(!ge.email)return xa();if(ge.status&&ge.status.runs_left<=0)return uh();let i=dt("dailyBtn");i.disabled=!0;try{let t=await ts("tower_start",{p_email:ge.email});if(t&&t.ok)I.daily=!0,I.run=t.run,ge.status&&(ge.status.runs_left=t.runs_left),ad();else{if(t&&t.code==="not_registered")return i.disabled=!1,xa();t&&t.code==="no_runs"?(ge.status&&(ge.status.runs_left=0,ge.status.sns_left=t.sns_left),Ur(),uh()):vn(Ji[t&&t.code]||"\uC2DC\uC791\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.",2)}}catch{vn("\uC5F0\uACB0\uC774 \uB04A\uACA8 \uC624\uB298\uC758 \uB3C4\uC804\uC744 \uC2DC\uC791\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694.",2)}i.disabled=!1}function od(){I.daily=!1,I.run=null,ad()}function ad(){md(),Qf(),I.mode="intro",I.intro=5.2,[St.title,St.over,St.board,St.sns,St.join].forEach(i=>se(i,!1)),se(St.hud,!0),se(St.controls,!0),Dr(`<small>${I.daily?"\uC624\uB298\uC758 \uB3C4\uC804":"\uC5F0\uC2B5 \xB7 \uC810\uC218 \uC5C6\uC74C"}</small><b>StepUP \uD0C0\uC6CC</b><span>\uC815\uC0C1\uAE4C\uC9C0 \uB2E4\uC12F \uAD6C\uAC04 \xB7 \uBB3C\uC5D0 \uBE60\uC9C0\uBA74 \uBD80\uD65C 1\uBC88</span>`,"",0),sd()}async function hh(i,t){if(I.mode==="sim"){I.simEnd={finished:i,why:t},I.mode="simdone";return}I.mode="over",se(St.controls,!1),St.banner.hidden=!0,St.wind.hidden=!0,St.fx.hidden=!0;let e=I.me,n=I.secMs.length,s=i?0:id(),r=ia(n,I.secMs,s,i,I.place,I.revived,I.ko);Ge(i?"win":"out"),i&&(Ma(I.world.finishS,I.world.finishY,2.2),I.showT=6);let o=i?I.place===1?"\u{1F3C6} \uC815\uC0C1 1\uB4F1!":"\uC815\uC0C1 \uB3C4\uCC29!":t==="time"?"\uC2DC\uAC04 \uB05D!":"\uD0C8\uB77D\u2026 \u{1FA78}";if(St.over.innerHTML=`<h2>${o}</h2><p class="big">${r.toLocaleString("ko-KR")}\uC810</p>
    <p class="sub">${i?`${I.place}\uC704\uB85C \uB3C4\uCC29`:`STAGE ${Math.min(n+1,5)}\uAE4C\uC9C0`} \xB7 ${nd(I.playT*1e3)}${I.ko?` \xB7 \uBC00\uC5B4 \uB5A8\uC5B4\uB728\uB9B0 AI ${I.ko}\uBA85`:""}${I.revived?" \xB7 \uBD80\uD65C \uC0AC\uC6A9":""}</p>
    <p class="sub" id="overNote">${I.daily?"\uC810\uC218\uB97C \uAE30\uB85D\uD558\uB294 \uC911\u2026":"\uC5F0\uC2B5 \uD310\uC740 \uC810\uC218\uAC00 \uB0A8\uC9C0 \uC54A\uC544\uC694."}</p>
    <button class="btn" id="againDaily">${I.daily?"\uB2E4\uC2DC \uB3C4\uC804":"\uC624\uB298\uC758 \uB3C4\uC804"}</button>
    <div class="row2"><button class="btn ghost" id="againPractice">\uC5F0\uC2B5 \uD55C \uD310</button><button class="btn ghost" id="toBoard">\uC21C\uC704 \uBCF4\uAE30</button></div>
    <button class="btn ghost" id="toSns">SNS \uC62C\uB9AC\uACE0 \uD55C \uBC88 \uB354</button><button class="link" id="toTitle" type="button">\uCC98\uC74C\uC73C\uB85C</button>`,se(St.over,!0),dt("againDaily").onclick=()=>{se(St.over,!1),rd()},dt("againPractice").onclick=od,dt("toBoard").onclick=()=>hd(),dt("toTitle").onclick=B_,dt("toSns").onclick=uh,!I.daily||!I.run)return;let a=I.run;I.run=null;try{let l=await ts("tower_end",{p_run:a,p_progress:s,p_revived:I.revived,p_ko:I.ko,p_place:i?I.place:0});dt("overNote").textContent=l&&l.ok?`\uAE30\uB85D\uD588\uC5B4\uC694 \xB7 \uC624\uB298 ${l.rank_today}\uC704 \xB7 \uC5ED\uB300 ${l.rank_all}\uC704${l.score!==r?` (\uC11C\uBC84 \uC810\uC218 ${(+l.score).toLocaleString("ko-KR")}\uC810)`:""}`:"\uC810\uC218\uB97C \uAE30\uB85D\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694."}catch{dt("overNote").textContent="\uC5F0\uACB0\uC774 \uB04A\uACA8 \uB9C8\uC9C0\uB9C9\uC73C\uB85C \uD1B5\uACFC\uD55C \uAD6C\uAC04\uAE4C\uC9C0\uB9CC \uAE30\uB85D\uB410\uC5B4\uC694."}Mh()}function B_(){I.mode="title",[St.over,St.board,St.sns,St.join,St.hud,St.controls].forEach(i=>se(i,!1)),se(St.title,!0);for(let i of ua)i.visible=!1;I.me=null,$i.visible=!1,Mh()}var Ea=`\uB098\uB294 StepUP \uD0C0\uC6CC \uC815\uC0C1\uAE4C\uC9C0 \uAC08 \uC218 \uC788\uC744\uAE4C? \u{1F3C3} \uD608\uC555 \uC8FC\uC758 \u{1FA78}
https://stepupcrew.com/
#StepUP\uCC4C\uB9B0\uC9C0 #\uC2A4\uD15D\uC5C5 #StepUp`;function uh(){[St.title,St.over].forEach(t=>se(t,!1)),se(St.sns,!0),dt("snsErr").textContent="",dt("snsUrl").value="",dt("snsText").textContent=Ea;let i=ge.status;dt("snsLeft").textContent=i?`\uC624\uB298 SNS\uB85C \uBC1B\uC744 \uC218 \uC788\uB294 \uAE30\uD68C ${i.sns_left}\uBC88 \uB0A8\uC74C \xB7 00\uC2DC\uC5D0 \uB2E4\uC2DC 3\uBC88`:"\uBA3C\uC800 \uC0AC\uC804 \uB4F1\uB85D\uD55C \uC774\uBA54\uC77C\uB85C \uCC38\uAC00\uD574 \uC8FC\uC138\uC694."}dt("snsCopy").onclick=()=>{(navigator.clipboard?navigator.clipboard.writeText(Ea):Promise.reject(new Error("x"))).then(()=>vn("\uAE00\uC744 \uBCF5\uC0AC\uD588\uC5B4\uC694. SNS\uC5D0 \uBD99\uC5EC \uB123\uC5B4 \uC62C\uB824 \uC8FC\uC138\uC694.",2),()=>vn("\uBCF5\uC0AC\uAC00 \uC548 \uB3FC\uC694. \uAE00\uC744 \uAE38\uAC8C \uB20C\uB7EC \uBCF5\uC0AC\uD574 \uC8FC\uC138\uC694.",2))};dt("snsX").href="https://x.com/intent/post?text="+encodeURIComponent(Ea);dt("snsThreads").href="https://www.threads.net/intent/post?text="+encodeURIComponent(Ea);dt("snsForm").onsubmit=async i=>{i.preventDefault();let t=dt("snsErr"),e=dt("snsSend");if(t.textContent="",!ge.email)return se(St.sns,!1),xa();if(!dt("snsConsent").checked){t.textContent=Ji.consent;return}e.disabled=!0,e.textContent="\uD655\uC778 \uC911\u2026";try{let n=await ts("tower_sns_submit",{p_email:ge.email,p_url:dt("snsUrl").value.trim(),p_account:dt("snsAcct").value.trim(),p_consent:!0,p_trap:""});n&&n.ok?(ge.status&&(ge.status.runs_left=n.runs_left,ge.status.sns_left=n.sns_left),Ur(),se(St.sns,!1),se(St.title,!0),I.mode="title",vn("\uAE30\uD68C +1! \uBC14\uB85C \uB3C4\uC804\uD574 \uBCF4\uC138\uC694.",2)):t.textContent=Ji[n&&n.code]||"\uD655\uC778\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."}catch{t.textContent="\uC5F0\uACB0\uC774 \uB04A\uACBC\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."}e.disabled=!1,e.textContent="\uB9C1\uD06C \uB0B4\uACE0 \uD55C \uBC88 \uB354"};dt("snsUrl").addEventListener("input",()=>{dt("acctRow").hidden=!/instagram\.com|(x|twitter)\.com\/i\//i.test(dt("snsUrl").value)});dt("snsBack").onclick=()=>{se(St.sns,!1),I.mode==="over"?se(St.over,!0):se(St.title,!0)};function ld(){let i=null;try{i=JSON.parse(localStorage.getItem("tower_look")||"null")}catch{i=null}let t=Object.assign({},Xf);if(i&&typeof i=="object")for(let e of yh)Number.isInteger(i[e])&&i[e]>=0&&i[e]<qe[e].list.length&&(t[e]=i[e]);return t}var Ai=null,ks=null,Gs=null,pa=null,ri=null,ya=!1;function Eh(){if(!ks){ks=new mr({canvas:dt("lookCanvas"),antialias:!0,alpha:!0}),ks.setPixelRatio(Math.min(window.devicePixelRatio||1,2)),ks.setSize(220,220,!1),ks.outputColorSpace=ze,Gs=new gr,Gs.add(new br(15398143,4872827,1.9));let i=new Ar(16777215,1.4);i.position.set(2,4,-3),Gs.add(i),pa=new Je(32,1,.1,50),pa.position.set(0,1.3,-4.4),pa.lookAt(0,.95,0)}ri&&(Gs.remove(ri),ri.traverse(i=>{i.geometry&&i.geometry.dispose()})),ri=ah(Ai,!1,1),ri.userData.bubble.visible=!1,Gs.add(ri)}function Sh(){dt("lookRows").innerHTML=yh.map(i=>{let t=qe[i].list[Ai[i]],e=/^#/.test(t);return`<div class="lk"><span>${qe[i].name}</span><button type="button" data-k="${i}" data-d="-1" aria-label="${qe[i].name} \uC774\uC804">\u2039</button><b>${e?`<i style="background:${t}"></i>`:vh(t)}</b><button type="button" data-k="${i}" data-d="1" aria-label="${qe[i].name} \uB2E4\uC74C">\u203A</button></div>`}).join(""),[].forEach.call(dt("lookRows").querySelectorAll("button"),i=>{i.onclick=()=>{let t=i.dataset.k,e=qe[t].list.length;Ai[t]=(Ai[t]+ +i.dataset.d+e)%e,Sh(),Eh()}})}function z_(){Ai=ld(),se(St.title,!1),se(St.look,!0),ya=!0;try{Eh()}catch{}Sh(),(function i(){ya&&(requestAnimationFrame(i),ri&&(ri.rotation.y+=.02,Yf(ri,2,!1,.016,""),ks.render(Gs,pa)))})()}dt("lookBtn").onclick=z_;dt("lookRandom").onclick=()=>{Ai=qf(Math.random),delete Ai.topHex,Sh(),Eh()};dt("lookSave").onclick=()=>{try{localStorage.setItem("tower_look",JSON.stringify(Ai))}catch{}ya=!1,se(St.look,!1),se(St.title,!0),vn("\uB0B4 \uB7EC\uB108\uB97C \uC800\uC7A5\uD588\uC5B4\uC694",1.4)};dt("lookBack").onclick=()=>{ya=!1,se(St.look,!1),se(St.title,!0)};var Vs="today",cd=null;async function hd(){[St.over,St.title,St.controls].forEach(i=>se(i,!1)),se(St.board,!0),dt("boardList").innerHTML='<li class="muted">\uBD88\uB7EC\uC624\uB294 \uC911\u2026</li>',dt("boardMe").textContent="";try{cd=await ts("tower_board",{p_email:ge.email||""}),ud()}catch{dt("boardList").innerHTML='<li class="muted">\uC21C\uC704\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC5B4\uC694.</li>'}}function ud(){let i=cd||{},t=(Vs==="today"?i.today:i.all)||[],e=Vs==="today"?i.me_today:i.me_all;[].forEach.call(document.querySelectorAll("[data-board]"),n=>n.setAttribute("aria-selected",String(n.dataset.board===Vs))),dt("boardList").innerHTML=t.length?t.map(n=>`<li class="${n.me?"me":""}"><b>${n.rank}</b><span>${vh(n.name)}<small>${n.cleared>=5?"\uC815\uC0C1":"STAGE "+(n.cleared+1)}</small></span><em>${(+n.score).toLocaleString("ko-KR")}</em></li>`).join(""):`<li class="muted">${Vs==="today"?"\uC624\uB298\uC740 \uC544\uC9C1 \uAE30\uB85D\uC774 \uC5C6\uC5B4\uC694. \uCCAB \uBC88\uC9F8 \uC8FC\uC778\uACF5\uC774 \uB418\uC5B4 \uBCF4\uC138\uC694.":"\uC544\uC9C1 \uAE30\uB85D\uC774 \uC5C6\uC5B4\uC694."}</li>`,dt("boardMe").textContent=e?`\uB0B4 ${Vs==="today"?"\uC624\uB298":"\uC5ED\uB300"} \uCD5C\uACE0 ${(+e.score).toLocaleString("ko-KR")}\uC810 \xB7 ${e.rank}\uC704`:i.players?`\uCC38\uAC00 ${i.players}\uBA85`:""}[].forEach.call(document.querySelectorAll("[data-board]"),i=>{i.onclick=()=>{Vs=i.dataset.board,ud()}});dt("boardClose").onclick=()=>{se(St.board,!1),I.mode==="over"?se(St.over,!0):se(St.title,!0)};dt("dailyBtn").onclick=rd;dt("practiceBtn").onclick=od;dt("boardBtn").onclick=()=>hd();var oi={keys:new Set,stickX:0,stickZ:0},fh={ArrowUp:[0,-1],KeyW:[0,-1],ArrowDown:[0,1],KeyS:[0,1],ArrowLeft:[-1,0],KeyA:[-1,0],ArrowRight:[1,0],KeyD:[1,0]};addEventListener("keydown",i=>{i.target&&/INPUT|TEXTAREA/.test(i.target.tagName)||(fh[i.code]&&(oi.keys.add(i.code),i.preventDefault()),i.code==="Space"&&(!i.repeat&&I.me&&(I.me.jumpBuf=si),i.preventDefault()),(i.code==="ShiftLeft"||i.code==="ShiftRight"||i.code==="KeyJ")&&I.mode==="play"&&I.me&&Lr(I.me))});addEventListener("keyup",i=>oi.keys.delete(i.code));var Yi=0;function Nf(){let i=I.me;if(!i)return;let t=0,e=0;for(let r of oi.keys)t+=fh[r][0],e+=fh[r][1];t=Math.max(-1,Math.min(1,t+oi.stickX)),e=Math.max(-1,Math.min(1,e+oi.stickZ));let n=[-Math.sin(Yi),-Math.cos(Yi)],s=[-n[1],n[0]];i.ix=n[0]*-e+s[0]*t,i.iz=n[1]*-e+s[1]*t}var ma=dt("stick"),fd=dt("knob"),_a=dt("jumpBtn"),va=dt("shoveBtn"),Nr=null,dh=0,ph=0,Hs=56;St.controls.addEventListener("pointerdown",i=>{i.target===_a||i.target===va||Nr!==null||i.clientX>innerWidth*.6||(Nr=i.pointerId,dh=i.clientX,ph=i.clientY,ma.style.left=dh+"px",ma.style.top=ph+"px",ma.classList.add("on"),St.controls.setPointerCapture(i.pointerId),i.preventDefault())});St.controls.addEventListener("pointermove",i=>{if(i.pointerId!==Nr)return;let t=i.clientX-dh,e=i.clientY-ph,n=Math.hypot(t,e);n>Hs&&(t=t/n*Hs,e=e/n*Hs),fd.style.transform=`translate(${t}px, ${e}px)`;let s=Math.min(1,n/Hs),r=s<.12?0:(s-.12)/.88/Math.max(s,.001);oi.stickX=t/Hs*r,oi.stickZ=e/Hs*r});function dd(i){i.pointerId===Nr&&(Nr=null,oi.stickX=oi.stickZ=0,fd.style.transform="",ma.classList.remove("on"))}St.controls.addEventListener("pointerup",dd);St.controls.addEventListener("pointercancel",dd);_a.addEventListener("pointerdown",i=>{I.me&&(I.me.jumpBuf=si),_a.classList.add("on"),i.preventDefault(),i.stopPropagation()});va.addEventListener("pointerdown",i=>{I.mode==="play"&&I.me&&Lr(I.me),va.classList.add("on"),i.preventDefault(),i.stopPropagation()});[_a,va].forEach(i=>["pointerup","pointercancel","pointerleave"].forEach(t=>i.addEventListener(t,()=>i.classList.remove("on"))));document.addEventListener("visibilitychange",()=>{I.paused=document.hidden,gh=performance.now()});var Rn=null,ji=!1;try{ji=localStorage.getItem("race_mute")==="1"}catch{}var mh=dt("muteBtn");function pd(){mh.textContent=ji?"\u{1F507}":"\u{1F50A}",mh.setAttribute("aria-label",ji?"\uC18C\uB9AC \uCF1C\uAE30":"\uC18C\uB9AC \uB044\uAE30")}pd();mh.onclick=()=>{ji=!ji,pd();try{localStorage.setItem("race_mute",ji?"1":"0")}catch{}md()};function md(){if(!Rn)try{Rn=new(window.AudioContext||window.webkitAudioContext)}catch{Rn=null}Rn&&Rn.state==="suspended"&&Rn.resume()}function me(i,t,e,n="sine",s=.12,r=0){if(!Rn||ji||I.mode==="sim")return;let o=Rn.currentTime+r,a=Rn.createOscillator(),l=Rn.createGain();a.type=n,a.frequency.setValueAtTime(i,o),a.frequency.exponentialRampToValueAtTime(Math.max(30,t),o+e),l.gain.setValueAtTime(s,o),l.gain.exponentialRampToValueAtTime(.001,o+e),a.connect(l).connect(Rn.destination),a.start(o),a.stop(o+e+.02)}function Ge(i){i==="jump"?me(420,760,.12,"square",.05):i==="land"?me(160,90,.06,"sine",.06):i==="hit"?me(220,60,.22,"sawtooth",.09):i==="dive"?me(600,300,.18,"triangle",.08):i==="shove"?(me(180,90,.12,"square",.1),me(500,700,.08,"triangle",.06,.05)):i==="whiff"?me(300,220,.08,"sine",.05):i==="boing"?me(300,1100,.25,"triangle",.1):i==="fall"?(me(700,90,.7,"triangle",.08),me(220,110,.5,"sawtooth",.04,.3)):i==="splash"?(me(900,60,.6,"sawtooth",.06),me(200,40,.5,"square",.05,.1)):i==="beep"?me(520,520,.14,"square",.07):i==="boost"?(me(260,980,.28,"sawtooth",.05),me(520,1400,.2,"triangle",.04,.06)):i==="slip"?(me(1200,300,.35,"triangle",.08),me(300,120,.25,"square",.05,.3)):i==="cannon"?(me(140,40,.35,"sawtooth",.1),me(80,30,.4,"square",.07)):i==="item"?(me(660,990,.1,"square",.06),me(990,1320,.12,"triangle",.06,.08)):i==="event"?(me(300,600,.2,"sawtooth",.06),me(600,300,.2,"sawtooth",.06,.2)):i==="go"?(me(880,880,.35,"square",.08),me(1320,1320,.3,"triangle",.05,.05)):i==="win"?(me(523,523,.12,"triangle",.1),me(659,659,.12,"triangle",.1,.12),me(784,784,.25,"triangle",.1,.24)):i==="out"&&(me(392,330,.25,"triangle",.1),me(330,220,.4,"triangle",.1,.25))}function gd(){let i=innerWidth,t=innerHeight;bi.setSize(i,t,!1),zn.aspect=i/t,zn.fov=i/t<.8?72:60,zn.updateProjectionMatrix()}addEventListener("resize",gd);gd();var Cr=new B(0,30,95),Fr=new B(0,14,0);function O_(i){let t=zn.aspect<.8,e=I.me;if(!e||e.out){let n=I.t*.08,s=new B(Math.cos(n)*95,32,Math.sin(n)*95);Cr.lerp(s,1-Math.exp(-i*1.2)),Fr.lerp(new B(0,16,0),1-Math.exp(-i*1.2))}else{let n=Math.min(e.s,I.world.endS),s=n/le,r=[-Math.sin(s),Math.cos(s)],o=[Math.cos(s),Math.sin(s)];if(I.mode==="intro"&&I.intro>3.2){let c=Math.min(1,Math.max(0,(5.2-I.intro)/2)),_=c*c*(3-2*c),p=s-.9*(1-_),g=le+70*(1-_)+10;Cr.set(Math.cos(p)*g,I.world.topY+30*(1-_)+6,Math.sin(p)*g),Fr.set(e.pos.x*_,14*(1-_)+e.pos.y*_,e.pos.z*_),Yi=Math.atan2(-r[0],-r[1]),zn.position.copy(Cr),zn.lookAt(Fr);return}let l=Math.atan2(-r[0],-r[1])-Yi;l=Math.atan2(Math.sin(l),Math.cos(l)),Yi+=l*Math.min(1,i*5);let h=t?10.5:9,u=t?6.6:5.4,m=[-Math.sin(Yi),-Math.cos(Yi)],x=e.pos,f=e.deadT>0?ke(n):Math.max(x.y,ke(n)-8),v=1.5+Math.max(0,le-3.5-Math.hypot(x.x,x.z))*.8,d=new B(x.x-m[0]*h+o[0]*v,f+u,x.z-m[1]*h+o[1]*v),y=1-Math.exp(-i*7);Cr.lerp(d,y),Fr.lerp(new B(x.x+m[0]*6,f+1,x.z+m[1]*6),y)}zn.position.copy(Cr).add(mt.shakeOffset(i)),zn.lookAt(Fr)}function Qc(i){for(let n of I.bodies){if(n.out)continue;let s=n.mesh,r=Math.hypot(n.vel.x,n.vel.z);s.position.copy(n.pos),r>.5&&!n.diving&&n.stun<=0&&n.shoveT<=0&&(n.yaw=Math.atan2(-n.vel.x,-n.vel.z));let o=n.yaw-s.rotation.y;o=Math.atan2(Math.sin(o),Math.cos(o)),s.rotation.y+=o*Math.min(1,i*14),s.rotation.z=n.stun>0?Math.sin(n.stun*30)*.3:0,n.slipT>0&&(s.rotation.y+=i*22),n.deadT<=0&&(s.visible=!(n.safe>.4&&Math.floor(n.safe*12)%2)),Yf(s,r,!n.onGround,i,n.diving?"dive":n.shoveT>0?"shove":""),s.userData.bubble.visible=n.shieldT>0;{let a=s.userData.body.userData.base||[1,1,1],l=n.boostT>0?1.08+Math.sin(performance.now()/60)*.04:1;s.userData.body.scale.set(a[0]*l,a[1]*l,a[2]*l)}if(n.tag){let a=I.me?Math.hypot(n.pos.x-I.me.pos.x,n.pos.z-I.me.pos.z):99;n.tag.visible=a>2.5&&a<11&&Math.abs(n.pos.y-I.me.pos.y)<4}I.mode==="play"&&n.deadT<=0&&Ce(n,16)&&H_(n,r,i)}let t=I.me;if(!t)return;let e=Zi(t.pos.x,t.pos.z,t.pos.y);if($i.visible=e>-1/0&&t.deadT<=0&&!t.out,$i.visible){$i.position.set(t.pos.x,e+.03,t.pos.z);let n=Math.max(.35,1-(t.pos.y-e)*.12);$i.scale.set(n,n,n)}}function H_(i,t,e){let n=i.pos;if(i.onGround&&t>3&&(i.stepT=(i.stepT||0)-e,i.stepT<=0)){i.stepT=.24+Math.random()*.06;let s=i.ground&&i.ground.ice;mt.dust(n.x,n.y,n.z,{n:i===I.me?2:1,speed:.8,size:.32,life:.8,colors:s?[14678271,16777215]:void 0}),s&&Math.random()<.5&&mt.one(n.x,n.y+.1,n.z,(Math.random()-.5)*2,1.5,(Math.random()-.5)*2,{color:12580095,size:.3,life:.4,grav:6,shape:2})}if(i.boostT>0)for(let s=0;s<2;s++)mt.one(n.x+(Math.random()-.5)*.5,n.y+.2+Math.random()*1.2,n.z+(Math.random()-.5)*.5,-i.vel.x*.15,.4,-i.vel.z*.15,{color:Math.random()<.5?16765503:2414847,size:.55,life:.35});if(i.shieldT>0&&Math.random()<e*14){let s=Math.random()*ve;mt.one(n.x+Math.cos(s),n.y+.4+Math.random()*1.2,n.z+Math.sin(s),0,.8,0,{color:12580095,size:.35,life:.5,shape:2})}i.diving&&mt.one(n.x,n.y+.6,n.z,0,0,0,{color:16777215,size:.6,life:.25,soft:!0,alpha:.6})}var th=0;function k_(i){let t=I.world,e=I.me;for(let s of t.solids)if(s.punch){if(s.flash&&!s.wasFlash&&Xs(s.cx,s.top,s.cz)){let r=s.mesh?s.mesh.position:{x:s.cx,y:s.top,z:s.cz};mt.burst(r.x,r.y+.4,r.z,{n:10,speed:5,colors:[16765503,16752412,16777215],life:.35,size:.5})}s.wasFlash=s.flash}for(let s of t.winds){let r=s.active(I.ht),o=s.warn(I.ht);if((r||o)&&e&&e.s>s.s0-20&&e.s<s.s1+5)for(let a=0;a<(r?5:1);a++){let l=s.s0+Math.random()*(s.s1-s.s0),h=Ee(l,-Pe+Math.random()*2*Pe,ke(l)+.3+Math.random()*2.4),u=h.a;mt.one(h.x,h.y,h.z,Math.cos(u)*(r?14:5),0,Math.sin(u)*(r?14:5),{color:16777215,size:r?.5:.3,size1:.1,life:.55,soft:!0,alpha:r?.75:.4})}}if(t.balls)for(let s of t.balls.list)Math.random()<i*8&&Xs(s.x,s.y,s.z,26)&&mt.dust(s.x,s.y-s.r,s.z,{n:2,speed:1.5,size:.6});if(t.crown&&Math.random()<i*6){let s=t.crown.position,r=Math.random()*ve;mt.one(s.x+Math.cos(r)*2.4,s.y+Math.random()*1.5,s.z+Math.sin(r)*2.4,0,1,0,{color:16769658,size:.6,life:.7,shape:2})}th-=i;let n=I.mode==="title"?.9:I.showT>0?.25:4.5;if(I.showT>0&&(I.showT-=i),th<=0){th=n*(.6+Math.random()*.8);let s=Math.random()*ve,r=I.showT>0?20+Math.random()*40:70+Math.random()*60,o=I.showT>0&&e?e.pos.y-4:t.topY*Math.random();mt.firework(Math.cos(s)*r,o,Math.sin(s)*r,{vy:20+Math.random()*10,big:I.showT>0?1.3:1.6})}}function Bf(i){let t=I.world;I.t+=i;for(let e of t.solids)e.save(),e.update(I.t,i);for(let e of t.hazards)e.update(I.t);t.balls&&t.balls.update(I.t,i,t,0)}var gh=performance.now();function xd(i){requestAnimationFrame(xd);let t=Math.min(.05,(i-gh)/1e3);if(gh=i,!(I.paused||I.mode==="sim"||I.mode==="simdone")){if(fa>0&&(fa-=t,fa<=0&&(St.toast.hidden=!0)),da>0&&(da-=t,da<=0&&(St.banner.hidden=!0)),I.mode==="intro"){let e=Math.ceil(I.intro-.2);I.intro-=t;let n=Math.ceil(I.intro-.2);I.intro<3.2&&n>0&&e!==n&&(Dr(`<b class="num">${n}</b><span>${["","\uACE7 \uCD9C\uBC1C","\uC870\uAE08\uB9CC","\uC900\uBE44"][n]||""}</span>`,"count",0),Ge("beep")),Bf(t),Nf(),Qc(t),I.intro<=.2&&(I.mode="play",Dr('<b class="num go">GO!</b>',"count",.8),Ge("go"),I.t=0,Ma(.5,0,1.2,!0),mt.shake(.35))}else if(I.mode==="play"){for(Nf(),I.acc+=t;I.acc>=Ir&&I.mode==="play";)I.acc-=Ir,I.playT+=Ir,td(Ir);Qc(t),I.mode==="play"&&sd()}else Bf(t),I.me&&Qc(t);la.value=i/1e3,O_(t),f_(I.world,i,t),I.mode!=="sim"&&k_(t),U_(t),bi.render(_n,zn)}}$f();I.mode="title";Mh();requestAnimationFrame(xd);xh&&(window.__tower={S:I,P:Ee,RT:le,HW:Pe,trackY:ke,simStep:td,setupRace:Qf,stepBody:Jf,groundTop:Zi,DT:Ir,SECTIONS:ii,towerScore:ia,shove:Lr,aiThink:jf,PERSONAS:Zf,mulberry:qs,relS:Ki,fx:mt,hitBody:wi,celebrateGate:Ma,scene:_n,info:()=>({calls:bi.info.render.calls,tris:bi.info.render.triangles,geos:bi.info.memory.geometries,tex:bi.info.memory.textures,fx:mt.count})});export{Xf as DEFAULT_LOOK,qe as LOOK};
/*! Bundled license information:

three/build/three.module.js:
  (**
   * @license
   * Copyright 2010-2024 Three.js Authors
   * SPDX-License-Identifier: MIT
   *)
*/
