var Iu=0,ul=1,Pu=2;var xh=1,Lu=2,bn=3,Jn=0,ke=1,ze=2,Yn=0,Wi=1,dl=2,fl=3,pl=4,Du=5,ci=100,Uu=101,Fu=102,Nu=103,Ou=104,Bu=200,zu=201,ku=202,Hu=203,ia=204,sa=205,Vu=206,Gu=207,Wu=208,Xu=209,qu=210,Yu=211,Zu=212,$u=213,Ju=214,ra=0,oa=1,aa=2,Zi=3,ca=4,la=5,ha=6,ua=7,Rc=0,Ku=1,ju=2,Zn=0,Qu=1,td=2,ed=3,nd=4,id=5,sd=6,rd=7;var yh=300,$i=301,Ji=302,da=303,fa=304,eo=306,Cn=1e3,hi=1001,pa=1002,an=1003,od=1004;var qs=1005;var pn=1006,So=1007;var ui=1008;var In=1009,vh=1010,Mh=1011,Ts=1012,Cc=1013,di=1014,Tn=1015,Fs=1016,Ic=1017,Pc=1018,Ki=1020,Sh=35902,Eh=1021,bh=1022,on=1023,wh=1024,Th=1025,Xi=1026,ji=1027,Ah=1028,Lc=1029,Rh=1030,Dc=1031;var Uc=1033,vr=33776,Mr=33777,Sr=33778,Er=33779,ma=35840,ga=35841,_a=35842,xa=35843,ya=36196,va=37492,Ma=37496,Sa=37808,Ea=37809,ba=37810,wa=37811,Ta=37812,Aa=37813,Ra=37814,Ca=37815,Ia=37816,Pa=37817,La=37818,Da=37819,Ua=37820,Fa=37821,br=36492,Na=36494,Oa=36495,Ch=36283,Ba=36284,za=36285,ka=36286;var wr=2300,Ha=2301,Eo=2302,ml=2400,gl=2401,_l=2402;var ad=3200,cd=3201;var Ih=0,ld=1,Xn="",De="srgb",ns="srgb-linear",no="linear",Qt="srgb";var wi=7680;var xl=519,hd=512,ud=513,dd=514,Ph=515,fd=516,pd=517,md=518,gd=519,Va=35044;var yl="300 es",An=2e3,Tr=2001,Kn=class{addEventListener(t,e){this._listeners===void 0&&(this._listeners={});let n=this._listeners;n[t]===void 0&&(n[t]=[]),n[t].indexOf(e)===-1&&n[t].push(e)}hasEventListener(t,e){if(this._listeners===void 0)return!1;let n=this._listeners;return n[t]!==void 0&&n[t].indexOf(e)!==-1}removeEventListener(t,e){if(this._listeners===void 0)return;let s=this._listeners[t];if(s!==void 0){let r=s.indexOf(e);r!==-1&&s.splice(r,1)}}dispatchEvent(t){if(this._listeners===void 0)return;let n=this._listeners[t.type];if(n!==void 0){t.target=this;let s=n.slice(0);for(let r=0,o=s.length;r<o;r++)s[r].call(this,t);t.target=null}}},Re=["00","01","02","03","04","05","06","07","08","09","0a","0b","0c","0d","0e","0f","10","11","12","13","14","15","16","17","18","19","1a","1b","1c","1d","1e","1f","20","21","22","23","24","25","26","27","28","29","2a","2b","2c","2d","2e","2f","30","31","32","33","34","35","36","37","38","39","3a","3b","3c","3d","3e","3f","40","41","42","43","44","45","46","47","48","49","4a","4b","4c","4d","4e","4f","50","51","52","53","54","55","56","57","58","59","5a","5b","5c","5d","5e","5f","60","61","62","63","64","65","66","67","68","69","6a","6b","6c","6d","6e","6f","70","71","72","73","74","75","76","77","78","79","7a","7b","7c","7d","7e","7f","80","81","82","83","84","85","86","87","88","89","8a","8b","8c","8d","8e","8f","90","91","92","93","94","95","96","97","98","99","9a","9b","9c","9d","9e","9f","a0","a1","a2","a3","a4","a5","a6","a7","a8","a9","aa","ab","ac","ad","ae","af","b0","b1","b2","b3","b4","b5","b6","b7","b8","b9","ba","bb","bc","bd","be","bf","c0","c1","c2","c3","c4","c5","c6","c7","c8","c9","ca","cb","cc","cd","ce","cf","d0","d1","d2","d3","d4","d5","d6","d7","d8","d9","da","db","dc","dd","de","df","e0","e1","e2","e3","e4","e5","e6","e7","e8","e9","ea","eb","ec","ed","ee","ef","f0","f1","f2","f3","f4","f5","f6","f7","f8","f9","fa","fb","fc","fd","fe","ff"];var bo=Math.PI/180,Ga=180/Math.PI;function $n(){let i=Math.random()*4294967295|0,t=Math.random()*4294967295|0,e=Math.random()*4294967295|0,n=Math.random()*4294967295|0;return(Re[i&255]+Re[i>>8&255]+Re[i>>16&255]+Re[i>>24&255]+"-"+Re[t&255]+Re[t>>8&255]+"-"+Re[t>>16&15|64]+Re[t>>24&255]+"-"+Re[e&63|128]+Re[e>>8&255]+"-"+Re[e>>16&255]+Re[e>>24&255]+Re[n&255]+Re[n>>8&255]+Re[n>>16&255]+Re[n>>24&255]).toLowerCase()}function we(i,t,e){return Math.max(t,Math.min(e,i))}function _d(i,t){return(i%t+t)%t}function wo(i,t,e){return(1-e)*i+e*t}function fn(i,t){switch(t.constructor){case Float32Array:return i;case Uint32Array:return i/4294967295;case Uint16Array:return i/65535;case Uint8Array:return i/255;case Int32Array:return Math.max(i/2147483647,-1);case Int16Array:return Math.max(i/32767,-1);case Int8Array:return Math.max(i/127,-1);default:throw new Error("Invalid component type.")}}function te(i,t){switch(t.constructor){case Float32Array:return i;case Uint32Array:return Math.round(i*4294967295);case Uint16Array:return Math.round(i*65535);case Uint8Array:return Math.round(i*255);case Int32Array:return Math.round(i*2147483647);case Int16Array:return Math.round(i*32767);case Int8Array:return Math.round(i*127);default:throw new Error("Invalid component type.")}}var ut=class i{constructor(t=0,e=0){i.prototype.isVector2=!0,this.x=t,this.y=e}get width(){return this.x}set width(t){this.x=t}get height(){return this.y}set height(t){this.y=t}set(t,e){return this.x=t,this.y=e,this}setScalar(t){return this.x=t,this.y=t,this}setX(t){return this.x=t,this}setY(t){return this.y=t,this}setComponent(t,e){switch(t){case 0:this.x=e;break;case 1:this.y=e;break;default:throw new Error("index is out of range: "+t)}return this}getComponent(t){switch(t){case 0:return this.x;case 1:return this.y;default:throw new Error("index is out of range: "+t)}}clone(){return new this.constructor(this.x,this.y)}copy(t){return this.x=t.x,this.y=t.y,this}add(t){return this.x+=t.x,this.y+=t.y,this}addScalar(t){return this.x+=t,this.y+=t,this}addVectors(t,e){return this.x=t.x+e.x,this.y=t.y+e.y,this}addScaledVector(t,e){return this.x+=t.x*e,this.y+=t.y*e,this}sub(t){return this.x-=t.x,this.y-=t.y,this}subScalar(t){return this.x-=t,this.y-=t,this}subVectors(t,e){return this.x=t.x-e.x,this.y=t.y-e.y,this}multiply(t){return this.x*=t.x,this.y*=t.y,this}multiplyScalar(t){return this.x*=t,this.y*=t,this}divide(t){return this.x/=t.x,this.y/=t.y,this}divideScalar(t){return this.multiplyScalar(1/t)}applyMatrix3(t){let e=this.x,n=this.y,s=t.elements;return this.x=s[0]*e+s[3]*n+s[6],this.y=s[1]*e+s[4]*n+s[7],this}min(t){return this.x=Math.min(this.x,t.x),this.y=Math.min(this.y,t.y),this}max(t){return this.x=Math.max(this.x,t.x),this.y=Math.max(this.y,t.y),this}clamp(t,e){return this.x=Math.max(t.x,Math.min(e.x,this.x)),this.y=Math.max(t.y,Math.min(e.y,this.y)),this}clampScalar(t,e){return this.x=Math.max(t,Math.min(e,this.x)),this.y=Math.max(t,Math.min(e,this.y)),this}clampLength(t,e){let n=this.length();return this.divideScalar(n||1).multiplyScalar(Math.max(t,Math.min(e,n)))}floor(){return this.x=Math.floor(this.x),this.y=Math.floor(this.y),this}ceil(){return this.x=Math.ceil(this.x),this.y=Math.ceil(this.y),this}round(){return this.x=Math.round(this.x),this.y=Math.round(this.y),this}roundToZero(){return this.x=Math.trunc(this.x),this.y=Math.trunc(this.y),this}negate(){return this.x=-this.x,this.y=-this.y,this}dot(t){return this.x*t.x+this.y*t.y}cross(t){return this.x*t.y-this.y*t.x}lengthSq(){return this.x*this.x+this.y*this.y}length(){return Math.sqrt(this.x*this.x+this.y*this.y)}manhattanLength(){return Math.abs(this.x)+Math.abs(this.y)}normalize(){return this.divideScalar(this.length()||1)}angle(){return Math.atan2(-this.y,-this.x)+Math.PI}angleTo(t){let e=Math.sqrt(this.lengthSq()*t.lengthSq());if(e===0)return Math.PI/2;let n=this.dot(t)/e;return Math.acos(we(n,-1,1))}distanceTo(t){return Math.sqrt(this.distanceToSquared(t))}distanceToSquared(t){let e=this.x-t.x,n=this.y-t.y;return e*e+n*n}manhattanDistanceTo(t){return Math.abs(this.x-t.x)+Math.abs(this.y-t.y)}setLength(t){return this.normalize().multiplyScalar(t)}lerp(t,e){return this.x+=(t.x-this.x)*e,this.y+=(t.y-this.y)*e,this}lerpVectors(t,e,n){return this.x=t.x+(e.x-t.x)*n,this.y=t.y+(e.y-t.y)*n,this}equals(t){return t.x===this.x&&t.y===this.y}fromArray(t,e=0){return this.x=t[e],this.y=t[e+1],this}toArray(t=[],e=0){return t[e]=this.x,t[e+1]=this.y,t}fromBufferAttribute(t,e){return this.x=t.getX(e),this.y=t.getY(e),this}rotateAround(t,e){let n=Math.cos(e),s=Math.sin(e),r=this.x-t.x,o=this.y-t.y;return this.x=r*n-o*s+t.x,this.y=r*s+o*n+t.y,this}random(){return this.x=Math.random(),this.y=Math.random(),this}*[Symbol.iterator](){yield this.x,yield this.y}},Ft=class i{constructor(t,e,n,s,r,o,a,c,u){i.prototype.isMatrix3=!0,this.elements=[1,0,0,0,1,0,0,0,1],t!==void 0&&this.set(t,e,n,s,r,o,a,c,u)}set(t,e,n,s,r,o,a,c,u){let f=this.elements;return f[0]=t,f[1]=s,f[2]=a,f[3]=e,f[4]=r,f[5]=c,f[6]=n,f[7]=o,f[8]=u,this}identity(){return this.set(1,0,0,0,1,0,0,0,1),this}copy(t){let e=this.elements,n=t.elements;return e[0]=n[0],e[1]=n[1],e[2]=n[2],e[3]=n[3],e[4]=n[4],e[5]=n[5],e[6]=n[6],e[7]=n[7],e[8]=n[8],this}extractBasis(t,e,n){return t.setFromMatrix3Column(this,0),e.setFromMatrix3Column(this,1),n.setFromMatrix3Column(this,2),this}setFromMatrix4(t){let e=t.elements;return this.set(e[0],e[4],e[8],e[1],e[5],e[9],e[2],e[6],e[10]),this}multiply(t){return this.multiplyMatrices(this,t)}premultiply(t){return this.multiplyMatrices(t,this)}multiplyMatrices(t,e){let n=t.elements,s=e.elements,r=this.elements,o=n[0],a=n[3],c=n[6],u=n[1],f=n[4],p=n[7],h=n[2],d=n[5],g=n[8],_=s[0],m=s[3],l=s[6],S=s[1],b=s[4],y=s[7],I=s[2],T=s[5],R=s[8];return r[0]=o*_+a*S+c*I,r[3]=o*m+a*b+c*T,r[6]=o*l+a*y+c*R,r[1]=u*_+f*S+p*I,r[4]=u*m+f*b+p*T,r[7]=u*l+f*y+p*R,r[2]=h*_+d*S+g*I,r[5]=h*m+d*b+g*T,r[8]=h*l+d*y+g*R,this}multiplyScalar(t){let e=this.elements;return e[0]*=t,e[3]*=t,e[6]*=t,e[1]*=t,e[4]*=t,e[7]*=t,e[2]*=t,e[5]*=t,e[8]*=t,this}determinant(){let t=this.elements,e=t[0],n=t[1],s=t[2],r=t[3],o=t[4],a=t[5],c=t[6],u=t[7],f=t[8];return e*o*f-e*a*u-n*r*f+n*a*c+s*r*u-s*o*c}invert(){let t=this.elements,e=t[0],n=t[1],s=t[2],r=t[3],o=t[4],a=t[5],c=t[6],u=t[7],f=t[8],p=f*o-a*u,h=a*c-f*r,d=u*r-o*c,g=e*p+n*h+s*d;if(g===0)return this.set(0,0,0,0,0,0,0,0,0);let _=1/g;return t[0]=p*_,t[1]=(s*u-f*n)*_,t[2]=(a*n-s*o)*_,t[3]=h*_,t[4]=(f*e-s*c)*_,t[5]=(s*r-a*e)*_,t[6]=d*_,t[7]=(n*c-u*e)*_,t[8]=(o*e-n*r)*_,this}transpose(){let t,e=this.elements;return t=e[1],e[1]=e[3],e[3]=t,t=e[2],e[2]=e[6],e[6]=t,t=e[5],e[5]=e[7],e[7]=t,this}getNormalMatrix(t){return this.setFromMatrix4(t).invert().transpose()}transposeIntoArray(t){let e=this.elements;return t[0]=e[0],t[1]=e[3],t[2]=e[6],t[3]=e[1],t[4]=e[4],t[5]=e[7],t[6]=e[2],t[7]=e[5],t[8]=e[8],this}setUvTransform(t,e,n,s,r,o,a){let c=Math.cos(r),u=Math.sin(r);return this.set(n*c,n*u,-n*(c*o+u*a)+o+t,-s*u,s*c,-s*(-u*o+c*a)+a+e,0,0,1),this}scale(t,e){return this.premultiply(To.makeScale(t,e)),this}rotate(t){return this.premultiply(To.makeRotation(-t)),this}translate(t,e){return this.premultiply(To.makeTranslation(t,e)),this}makeTranslation(t,e){return t.isVector2?this.set(1,0,t.x,0,1,t.y,0,0,1):this.set(1,0,t,0,1,e,0,0,1),this}makeRotation(t){let e=Math.cos(t),n=Math.sin(t);return this.set(e,-n,0,n,e,0,0,0,1),this}makeScale(t,e){return this.set(t,0,0,0,e,0,0,0,1),this}equals(t){let e=this.elements,n=t.elements;for(let s=0;s<9;s++)if(e[s]!==n[s])return!1;return!0}fromArray(t,e=0){for(let n=0;n<9;n++)this.elements[n]=t[n+e];return this}toArray(t=[],e=0){let n=this.elements;return t[e]=n[0],t[e+1]=n[1],t[e+2]=n[2],t[e+3]=n[3],t[e+4]=n[4],t[e+5]=n[5],t[e+6]=n[6],t[e+7]=n[7],t[e+8]=n[8],t}clone(){return new this.constructor().fromArray(this.elements)}},To=new Ft;function Lh(i){for(let t=i.length-1;t>=0;--t)if(i[t]>=65535)return!0;return!1}function Ar(i){return document.createElementNS("http://www.w3.org/1999/xhtml",i)}function xd(){let i=Ar("canvas");return i.style.display="block",i}var vl={};function Ms(i){i in vl||(vl[i]=!0,console.warn(i))}function yd(i,t,e){return new Promise(function(n,s){function r(){switch(i.clientWaitSync(t,i.SYNC_FLUSH_COMMANDS_BIT,0)){case i.WAIT_FAILED:s();break;case i.TIMEOUT_EXPIRED:setTimeout(r,e);break;default:n()}}setTimeout(r,e)})}function vd(i){let t=i.elements;t[2]=.5*t[2]+.5*t[3],t[6]=.5*t[6]+.5*t[7],t[10]=.5*t[10]+.5*t[11],t[14]=.5*t[14]+.5*t[15]}function Md(i){let t=i.elements;t[11]===-1?(t[10]=-t[10]-1,t[14]=-t[14]):(t[10]=-t[10],t[14]=-t[14]+1)}var qt={enabled:!0,workingColorSpace:ns,spaces:{},convert:function(i,t,e){return this.enabled===!1||t===e||!t||!e||(this.spaces[t].transfer===Qt&&(i.r=Rn(i.r),i.g=Rn(i.g),i.b=Rn(i.b)),this.spaces[t].primaries!==this.spaces[e].primaries&&(i.applyMatrix3(this.spaces[t].toXYZ),i.applyMatrix3(this.spaces[e].fromXYZ)),this.spaces[e].transfer===Qt&&(i.r=qi(i.r),i.g=qi(i.g),i.b=qi(i.b))),i},fromWorkingColorSpace:function(i,t){return this.convert(i,this.workingColorSpace,t)},toWorkingColorSpace:function(i,t){return this.convert(i,t,this.workingColorSpace)},getPrimaries:function(i){return this.spaces[i].primaries},getTransfer:function(i){return i===Xn?no:this.spaces[i].transfer},getLuminanceCoefficients:function(i,t=this.workingColorSpace){return i.fromArray(this.spaces[t].luminanceCoefficients)},define:function(i){Object.assign(this.spaces,i)},_getMatrix:function(i,t,e){return i.copy(this.spaces[t].toXYZ).multiply(this.spaces[e].fromXYZ)},_getDrawingBufferColorSpace:function(i){return this.spaces[i].outputColorSpaceConfig.drawingBufferColorSpace},_getUnpackColorSpace:function(i=this.workingColorSpace){return this.spaces[i].workingColorSpaceConfig.unpackColorSpace}};function Rn(i){return i<.04045?i*.0773993808:Math.pow(i*.9478672986+.0521327014,2.4)}function qi(i){return i<.0031308?i*12.92:1.055*Math.pow(i,.41666)-.055}var Ml=[.64,.33,.3,.6,.15,.06],Sl=[.2126,.7152,.0722],El=[.3127,.329],bl=new Ft().set(.4123908,.3575843,.1804808,.212639,.7151687,.0721923,.0193308,.1191948,.9505322),wl=new Ft().set(3.2409699,-1.5373832,-.4986108,-.9692436,1.8759675,.0415551,.0556301,-.203977,1.0569715);qt.define({[ns]:{primaries:Ml,whitePoint:El,transfer:no,toXYZ:bl,fromXYZ:wl,luminanceCoefficients:Sl,workingColorSpaceConfig:{unpackColorSpace:De},outputColorSpaceConfig:{drawingBufferColorSpace:De}},[De]:{primaries:Ml,whitePoint:El,transfer:Qt,toXYZ:bl,fromXYZ:wl,luminanceCoefficients:Sl,outputColorSpaceConfig:{drawingBufferColorSpace:De}}});var Ti,Wa=class{static getDataURL(t){if(/^data:/i.test(t.src)||typeof HTMLCanvasElement>"u")return t.src;let e;if(t instanceof HTMLCanvasElement)e=t;else{Ti===void 0&&(Ti=Ar("canvas")),Ti.width=t.width,Ti.height=t.height;let n=Ti.getContext("2d");t instanceof ImageData?n.putImageData(t,0,0):n.drawImage(t,0,0,t.width,t.height),e=Ti}return e.width>2048||e.height>2048?(console.warn("THREE.ImageUtils.getDataURL: Image converted to jpg for performance reasons",t),e.toDataURL("image/jpeg",.6)):e.toDataURL("image/png")}static sRGBToLinear(t){if(typeof HTMLImageElement<"u"&&t instanceof HTMLImageElement||typeof HTMLCanvasElement<"u"&&t instanceof HTMLCanvasElement||typeof ImageBitmap<"u"&&t instanceof ImageBitmap){let e=Ar("canvas");e.width=t.width,e.height=t.height;let n=e.getContext("2d");n.drawImage(t,0,0,t.width,t.height);let s=n.getImageData(0,0,t.width,t.height),r=s.data;for(let o=0;o<r.length;o++)r[o]=Rn(r[o]/255)*255;return n.putImageData(s,0,0),e}else if(t.data){let e=t.data.slice(0);for(let n=0;n<e.length;n++)e instanceof Uint8Array||e instanceof Uint8ClampedArray?e[n]=Math.floor(Rn(e[n]/255)*255):e[n]=Rn(e[n]);return{data:e,width:t.width,height:t.height}}else return console.warn("THREE.ImageUtils.sRGBToLinear(): Unsupported image type. No color space conversion applied."),t}},Sd=0,Rr=class{constructor(t=null){this.isSource=!0,Object.defineProperty(this,"id",{value:Sd++}),this.uuid=$n(),this.data=t,this.dataReady=!0,this.version=0}set needsUpdate(t){t===!0&&this.version++}toJSON(t){let e=t===void 0||typeof t=="string";if(!e&&t.images[this.uuid]!==void 0)return t.images[this.uuid];let n={uuid:this.uuid,url:""},s=this.data;if(s!==null){let r;if(Array.isArray(s)){r=[];for(let o=0,a=s.length;o<a;o++)s[o].isDataTexture?r.push(Ao(s[o].image)):r.push(Ao(s[o]))}else r=Ao(s);n.url=r}return e||(t.images[this.uuid]=n),n}};function Ao(i){return typeof HTMLImageElement<"u"&&i instanceof HTMLImageElement||typeof HTMLCanvasElement<"u"&&i instanceof HTMLCanvasElement||typeof ImageBitmap<"u"&&i instanceof ImageBitmap?Wa.getDataURL(i):i.data?{data:Array.from(i.data),width:i.width,height:i.height,type:i.data.constructor.name}:(console.warn("THREE.Texture: Unable to serialize Texture."),{})}var Ed=0,Xe=class i extends Kn{constructor(t=i.DEFAULT_IMAGE,e=i.DEFAULT_MAPPING,n=hi,s=hi,r=pn,o=ui,a=on,c=In,u=i.DEFAULT_ANISOTROPY,f=Xn){super(),this.isTexture=!0,Object.defineProperty(this,"id",{value:Ed++}),this.uuid=$n(),this.name="",this.source=new Rr(t),this.mipmaps=[],this.mapping=e,this.channel=0,this.wrapS=n,this.wrapT=s,this.magFilter=r,this.minFilter=o,this.anisotropy=u,this.format=a,this.internalFormat=null,this.type=c,this.offset=new ut(0,0),this.repeat=new ut(1,1),this.center=new ut(0,0),this.rotation=0,this.matrixAutoUpdate=!0,this.matrix=new Ft,this.generateMipmaps=!0,this.premultiplyAlpha=!1,this.flipY=!0,this.unpackAlignment=4,this.colorSpace=f,this.userData={},this.version=0,this.onUpdate=null,this.isRenderTargetTexture=!1,this.pmremVersion=0}get image(){return this.source.data}set image(t=null){this.source.data=t}updateMatrix(){this.matrix.setUvTransform(this.offset.x,this.offset.y,this.repeat.x,this.repeat.y,this.rotation,this.center.x,this.center.y)}clone(){return new this.constructor().copy(this)}copy(t){return this.name=t.name,this.source=t.source,this.mipmaps=t.mipmaps.slice(0),this.mapping=t.mapping,this.channel=t.channel,this.wrapS=t.wrapS,this.wrapT=t.wrapT,this.magFilter=t.magFilter,this.minFilter=t.minFilter,this.anisotropy=t.anisotropy,this.format=t.format,this.internalFormat=t.internalFormat,this.type=t.type,this.offset.copy(t.offset),this.repeat.copy(t.repeat),this.center.copy(t.center),this.rotation=t.rotation,this.matrixAutoUpdate=t.matrixAutoUpdate,this.matrix.copy(t.matrix),this.generateMipmaps=t.generateMipmaps,this.premultiplyAlpha=t.premultiplyAlpha,this.flipY=t.flipY,this.unpackAlignment=t.unpackAlignment,this.colorSpace=t.colorSpace,this.userData=JSON.parse(JSON.stringify(t.userData)),this.needsUpdate=!0,this}toJSON(t){let e=t===void 0||typeof t=="string";if(!e&&t.textures[this.uuid]!==void 0)return t.textures[this.uuid];let n={metadata:{version:4.6,type:"Texture",generator:"Texture.toJSON"},uuid:this.uuid,name:this.name,image:this.source.toJSON(t).uuid,mapping:this.mapping,channel:this.channel,repeat:[this.repeat.x,this.repeat.y],offset:[this.offset.x,this.offset.y],center:[this.center.x,this.center.y],rotation:this.rotation,wrap:[this.wrapS,this.wrapT],format:this.format,internalFormat:this.internalFormat,type:this.type,colorSpace:this.colorSpace,minFilter:this.minFilter,magFilter:this.magFilter,anisotropy:this.anisotropy,flipY:this.flipY,generateMipmaps:this.generateMipmaps,premultiplyAlpha:this.premultiplyAlpha,unpackAlignment:this.unpackAlignment};return Object.keys(this.userData).length>0&&(n.userData=this.userData),e||(t.textures[this.uuid]=n),n}dispose(){this.dispatchEvent({type:"dispose"})}transformUv(t){if(this.mapping!==yh)return t;if(t.applyMatrix3(this.matrix),t.x<0||t.x>1)switch(this.wrapS){case Cn:t.x=t.x-Math.floor(t.x);break;case hi:t.x=t.x<0?0:1;break;case pa:Math.abs(Math.floor(t.x)%2)===1?t.x=Math.ceil(t.x)-t.x:t.x=t.x-Math.floor(t.x);break}if(t.y<0||t.y>1)switch(this.wrapT){case Cn:t.y=t.y-Math.floor(t.y);break;case hi:t.y=t.y<0?0:1;break;case pa:Math.abs(Math.floor(t.y)%2)===1?t.y=Math.ceil(t.y)-t.y:t.y=t.y-Math.floor(t.y);break}return this.flipY&&(t.y=1-t.y),t}set needsUpdate(t){t===!0&&(this.version++,this.source.needsUpdate=!0)}set needsPMREMUpdate(t){t===!0&&this.pmremVersion++}};Xe.DEFAULT_IMAGE=null;Xe.DEFAULT_MAPPING=yh;Xe.DEFAULT_ANISOTROPY=1;var pe=class i{constructor(t=0,e=0,n=0,s=1){i.prototype.isVector4=!0,this.x=t,this.y=e,this.z=n,this.w=s}get width(){return this.z}set width(t){this.z=t}get height(){return this.w}set height(t){this.w=t}set(t,e,n,s){return this.x=t,this.y=e,this.z=n,this.w=s,this}setScalar(t){return this.x=t,this.y=t,this.z=t,this.w=t,this}setX(t){return this.x=t,this}setY(t){return this.y=t,this}setZ(t){return this.z=t,this}setW(t){return this.w=t,this}setComponent(t,e){switch(t){case 0:this.x=e;break;case 1:this.y=e;break;case 2:this.z=e;break;case 3:this.w=e;break;default:throw new Error("index is out of range: "+t)}return this}getComponent(t){switch(t){case 0:return this.x;case 1:return this.y;case 2:return this.z;case 3:return this.w;default:throw new Error("index is out of range: "+t)}}clone(){return new this.constructor(this.x,this.y,this.z,this.w)}copy(t){return this.x=t.x,this.y=t.y,this.z=t.z,this.w=t.w!==void 0?t.w:1,this}add(t){return this.x+=t.x,this.y+=t.y,this.z+=t.z,this.w+=t.w,this}addScalar(t){return this.x+=t,this.y+=t,this.z+=t,this.w+=t,this}addVectors(t,e){return this.x=t.x+e.x,this.y=t.y+e.y,this.z=t.z+e.z,this.w=t.w+e.w,this}addScaledVector(t,e){return this.x+=t.x*e,this.y+=t.y*e,this.z+=t.z*e,this.w+=t.w*e,this}sub(t){return this.x-=t.x,this.y-=t.y,this.z-=t.z,this.w-=t.w,this}subScalar(t){return this.x-=t,this.y-=t,this.z-=t,this.w-=t,this}subVectors(t,e){return this.x=t.x-e.x,this.y=t.y-e.y,this.z=t.z-e.z,this.w=t.w-e.w,this}multiply(t){return this.x*=t.x,this.y*=t.y,this.z*=t.z,this.w*=t.w,this}multiplyScalar(t){return this.x*=t,this.y*=t,this.z*=t,this.w*=t,this}applyMatrix4(t){let e=this.x,n=this.y,s=this.z,r=this.w,o=t.elements;return this.x=o[0]*e+o[4]*n+o[8]*s+o[12]*r,this.y=o[1]*e+o[5]*n+o[9]*s+o[13]*r,this.z=o[2]*e+o[6]*n+o[10]*s+o[14]*r,this.w=o[3]*e+o[7]*n+o[11]*s+o[15]*r,this}divide(t){return this.x/=t.x,this.y/=t.y,this.z/=t.z,this.w/=t.w,this}divideScalar(t){return this.multiplyScalar(1/t)}setAxisAngleFromQuaternion(t){this.w=2*Math.acos(t.w);let e=Math.sqrt(1-t.w*t.w);return e<1e-4?(this.x=1,this.y=0,this.z=0):(this.x=t.x/e,this.y=t.y/e,this.z=t.z/e),this}setAxisAngleFromRotationMatrix(t){let e,n,s,r,c=t.elements,u=c[0],f=c[4],p=c[8],h=c[1],d=c[5],g=c[9],_=c[2],m=c[6],l=c[10];if(Math.abs(f-h)<.01&&Math.abs(p-_)<.01&&Math.abs(g-m)<.01){if(Math.abs(f+h)<.1&&Math.abs(p+_)<.1&&Math.abs(g+m)<.1&&Math.abs(u+d+l-3)<.1)return this.set(1,0,0,0),this;e=Math.PI;let b=(u+1)/2,y=(d+1)/2,I=(l+1)/2,T=(f+h)/4,R=(p+_)/4,A=(g+m)/4;return b>y&&b>I?b<.01?(n=0,s=.707106781,r=.707106781):(n=Math.sqrt(b),s=T/n,r=R/n):y>I?y<.01?(n=.707106781,s=0,r=.707106781):(s=Math.sqrt(y),n=T/s,r=A/s):I<.01?(n=.707106781,s=.707106781,r=0):(r=Math.sqrt(I),n=R/r,s=A/r),this.set(n,s,r,e),this}let S=Math.sqrt((m-g)*(m-g)+(p-_)*(p-_)+(h-f)*(h-f));return Math.abs(S)<.001&&(S=1),this.x=(m-g)/S,this.y=(p-_)/S,this.z=(h-f)/S,this.w=Math.acos((u+d+l-1)/2),this}setFromMatrixPosition(t){let e=t.elements;return this.x=e[12],this.y=e[13],this.z=e[14],this.w=e[15],this}min(t){return this.x=Math.min(this.x,t.x),this.y=Math.min(this.y,t.y),this.z=Math.min(this.z,t.z),this.w=Math.min(this.w,t.w),this}max(t){return this.x=Math.max(this.x,t.x),this.y=Math.max(this.y,t.y),this.z=Math.max(this.z,t.z),this.w=Math.max(this.w,t.w),this}clamp(t,e){return this.x=Math.max(t.x,Math.min(e.x,this.x)),this.y=Math.max(t.y,Math.min(e.y,this.y)),this.z=Math.max(t.z,Math.min(e.z,this.z)),this.w=Math.max(t.w,Math.min(e.w,this.w)),this}clampScalar(t,e){return this.x=Math.max(t,Math.min(e,this.x)),this.y=Math.max(t,Math.min(e,this.y)),this.z=Math.max(t,Math.min(e,this.z)),this.w=Math.max(t,Math.min(e,this.w)),this}clampLength(t,e){let n=this.length();return this.divideScalar(n||1).multiplyScalar(Math.max(t,Math.min(e,n)))}floor(){return this.x=Math.floor(this.x),this.y=Math.floor(this.y),this.z=Math.floor(this.z),this.w=Math.floor(this.w),this}ceil(){return this.x=Math.ceil(this.x),this.y=Math.ceil(this.y),this.z=Math.ceil(this.z),this.w=Math.ceil(this.w),this}round(){return this.x=Math.round(this.x),this.y=Math.round(this.y),this.z=Math.round(this.z),this.w=Math.round(this.w),this}roundToZero(){return this.x=Math.trunc(this.x),this.y=Math.trunc(this.y),this.z=Math.trunc(this.z),this.w=Math.trunc(this.w),this}negate(){return this.x=-this.x,this.y=-this.y,this.z=-this.z,this.w=-this.w,this}dot(t){return this.x*t.x+this.y*t.y+this.z*t.z+this.w*t.w}lengthSq(){return this.x*this.x+this.y*this.y+this.z*this.z+this.w*this.w}length(){return Math.sqrt(this.x*this.x+this.y*this.y+this.z*this.z+this.w*this.w)}manhattanLength(){return Math.abs(this.x)+Math.abs(this.y)+Math.abs(this.z)+Math.abs(this.w)}normalize(){return this.divideScalar(this.length()||1)}setLength(t){return this.normalize().multiplyScalar(t)}lerp(t,e){return this.x+=(t.x-this.x)*e,this.y+=(t.y-this.y)*e,this.z+=(t.z-this.z)*e,this.w+=(t.w-this.w)*e,this}lerpVectors(t,e,n){return this.x=t.x+(e.x-t.x)*n,this.y=t.y+(e.y-t.y)*n,this.z=t.z+(e.z-t.z)*n,this.w=t.w+(e.w-t.w)*n,this}equals(t){return t.x===this.x&&t.y===this.y&&t.z===this.z&&t.w===this.w}fromArray(t,e=0){return this.x=t[e],this.y=t[e+1],this.z=t[e+2],this.w=t[e+3],this}toArray(t=[],e=0){return t[e]=this.x,t[e+1]=this.y,t[e+2]=this.z,t[e+3]=this.w,t}fromBufferAttribute(t,e){return this.x=t.getX(e),this.y=t.getY(e),this.z=t.getZ(e),this.w=t.getW(e),this}random(){return this.x=Math.random(),this.y=Math.random(),this.z=Math.random(),this.w=Math.random(),this}*[Symbol.iterator](){yield this.x,yield this.y,yield this.z,yield this.w}},Xa=class extends Kn{constructor(t=1,e=1,n={}){super(),this.isRenderTarget=!0,this.width=t,this.height=e,this.depth=1,this.scissor=new pe(0,0,t,e),this.scissorTest=!1,this.viewport=new pe(0,0,t,e);let s={width:t,height:e,depth:1};n=Object.assign({generateMipmaps:!1,internalFormat:null,minFilter:pn,depthBuffer:!0,stencilBuffer:!1,resolveDepthBuffer:!0,resolveStencilBuffer:!0,depthTexture:null,samples:0,count:1},n);let r=new Xe(s,n.mapping,n.wrapS,n.wrapT,n.magFilter,n.minFilter,n.format,n.type,n.anisotropy,n.colorSpace);r.flipY=!1,r.generateMipmaps=n.generateMipmaps,r.internalFormat=n.internalFormat,this.textures=[];let o=n.count;for(let a=0;a<o;a++)this.textures[a]=r.clone(),this.textures[a].isRenderTargetTexture=!0;this.depthBuffer=n.depthBuffer,this.stencilBuffer=n.stencilBuffer,this.resolveDepthBuffer=n.resolveDepthBuffer,this.resolveStencilBuffer=n.resolveStencilBuffer,this.depthTexture=n.depthTexture,this.samples=n.samples}get texture(){return this.textures[0]}set texture(t){this.textures[0]=t}setSize(t,e,n=1){if(this.width!==t||this.height!==e||this.depth!==n){this.width=t,this.height=e,this.depth=n;for(let s=0,r=this.textures.length;s<r;s++)this.textures[s].image.width=t,this.textures[s].image.height=e,this.textures[s].image.depth=n;this.dispose()}this.viewport.set(0,0,t,e),this.scissor.set(0,0,t,e)}clone(){return new this.constructor().copy(this)}copy(t){this.width=t.width,this.height=t.height,this.depth=t.depth,this.scissor.copy(t.scissor),this.scissorTest=t.scissorTest,this.viewport.copy(t.viewport),this.textures.length=0;for(let n=0,s=t.textures.length;n<s;n++)this.textures[n]=t.textures[n].clone(),this.textures[n].isRenderTargetTexture=!0;let e=Object.assign({},t.texture.image);return this.texture.source=new Rr(e),this.depthBuffer=t.depthBuffer,this.stencilBuffer=t.stencilBuffer,this.resolveDepthBuffer=t.resolveDepthBuffer,this.resolveStencilBuffer=t.resolveStencilBuffer,t.depthTexture!==null&&(this.depthTexture=t.depthTexture.clone()),this.samples=t.samples,this}dispose(){this.dispatchEvent({type:"dispose"})}},Pn=class extends Xa{constructor(t=1,e=1,n={}){super(t,e,n),this.isWebGLRenderTarget=!0}},Cr=class extends Xe{constructor(t=null,e=1,n=1,s=1){super(null),this.isDataArrayTexture=!0,this.image={data:t,width:e,height:n,depth:s},this.magFilter=an,this.minFilter=an,this.wrapR=hi,this.generateMipmaps=!1,this.flipY=!1,this.unpackAlignment=1,this.layerUpdates=new Set}addLayerUpdate(t){this.layerUpdates.add(t)}clearLayerUpdates(){this.layerUpdates.clear()}};var qa=class extends Xe{constructor(t=null,e=1,n=1,s=1){super(null),this.isData3DTexture=!0,this.image={data:t,width:e,height:n,depth:s},this.magFilter=an,this.minFilter=an,this.wrapR=hi,this.generateMipmaps=!1,this.flipY=!1,this.unpackAlignment=1}};var jn=class{constructor(t=0,e=0,n=0,s=1){this.isQuaternion=!0,this._x=t,this._y=e,this._z=n,this._w=s}static slerpFlat(t,e,n,s,r,o,a){let c=n[s+0],u=n[s+1],f=n[s+2],p=n[s+3],h=r[o+0],d=r[o+1],g=r[o+2],_=r[o+3];if(a===0){t[e+0]=c,t[e+1]=u,t[e+2]=f,t[e+3]=p;return}if(a===1){t[e+0]=h,t[e+1]=d,t[e+2]=g,t[e+3]=_;return}if(p!==_||c!==h||u!==d||f!==g){let m=1-a,l=c*h+u*d+f*g+p*_,S=l>=0?1:-1,b=1-l*l;if(b>Number.EPSILON){let I=Math.sqrt(b),T=Math.atan2(I,l*S);m=Math.sin(m*T)/I,a=Math.sin(a*T)/I}let y=a*S;if(c=c*m+h*y,u=u*m+d*y,f=f*m+g*y,p=p*m+_*y,m===1-a){let I=1/Math.sqrt(c*c+u*u+f*f+p*p);c*=I,u*=I,f*=I,p*=I}}t[e]=c,t[e+1]=u,t[e+2]=f,t[e+3]=p}static multiplyQuaternionsFlat(t,e,n,s,r,o){let a=n[s],c=n[s+1],u=n[s+2],f=n[s+3],p=r[o],h=r[o+1],d=r[o+2],g=r[o+3];return t[e]=a*g+f*p+c*d-u*h,t[e+1]=c*g+f*h+u*p-a*d,t[e+2]=u*g+f*d+a*h-c*p,t[e+3]=f*g-a*p-c*h-u*d,t}get x(){return this._x}set x(t){this._x=t,this._onChangeCallback()}get y(){return this._y}set y(t){this._y=t,this._onChangeCallback()}get z(){return this._z}set z(t){this._z=t,this._onChangeCallback()}get w(){return this._w}set w(t){this._w=t,this._onChangeCallback()}set(t,e,n,s){return this._x=t,this._y=e,this._z=n,this._w=s,this._onChangeCallback(),this}clone(){return new this.constructor(this._x,this._y,this._z,this._w)}copy(t){return this._x=t.x,this._y=t.y,this._z=t.z,this._w=t.w,this._onChangeCallback(),this}setFromEuler(t,e=!0){let n=t._x,s=t._y,r=t._z,o=t._order,a=Math.cos,c=Math.sin,u=a(n/2),f=a(s/2),p=a(r/2),h=c(n/2),d=c(s/2),g=c(r/2);switch(o){case"XYZ":this._x=h*f*p+u*d*g,this._y=u*d*p-h*f*g,this._z=u*f*g+h*d*p,this._w=u*f*p-h*d*g;break;case"YXZ":this._x=h*f*p+u*d*g,this._y=u*d*p-h*f*g,this._z=u*f*g-h*d*p,this._w=u*f*p+h*d*g;break;case"ZXY":this._x=h*f*p-u*d*g,this._y=u*d*p+h*f*g,this._z=u*f*g+h*d*p,this._w=u*f*p-h*d*g;break;case"ZYX":this._x=h*f*p-u*d*g,this._y=u*d*p+h*f*g,this._z=u*f*g-h*d*p,this._w=u*f*p+h*d*g;break;case"YZX":this._x=h*f*p+u*d*g,this._y=u*d*p+h*f*g,this._z=u*f*g-h*d*p,this._w=u*f*p-h*d*g;break;case"XZY":this._x=h*f*p-u*d*g,this._y=u*d*p-h*f*g,this._z=u*f*g+h*d*p,this._w=u*f*p+h*d*g;break;default:console.warn("THREE.Quaternion: .setFromEuler() encountered an unknown order: "+o)}return e===!0&&this._onChangeCallback(),this}setFromAxisAngle(t,e){let n=e/2,s=Math.sin(n);return this._x=t.x*s,this._y=t.y*s,this._z=t.z*s,this._w=Math.cos(n),this._onChangeCallback(),this}setFromRotationMatrix(t){let e=t.elements,n=e[0],s=e[4],r=e[8],o=e[1],a=e[5],c=e[9],u=e[2],f=e[6],p=e[10],h=n+a+p;if(h>0){let d=.5/Math.sqrt(h+1);this._w=.25/d,this._x=(f-c)*d,this._y=(r-u)*d,this._z=(o-s)*d}else if(n>a&&n>p){let d=2*Math.sqrt(1+n-a-p);this._w=(f-c)/d,this._x=.25*d,this._y=(s+o)/d,this._z=(r+u)/d}else if(a>p){let d=2*Math.sqrt(1+a-n-p);this._w=(r-u)/d,this._x=(s+o)/d,this._y=.25*d,this._z=(c+f)/d}else{let d=2*Math.sqrt(1+p-n-a);this._w=(o-s)/d,this._x=(r+u)/d,this._y=(c+f)/d,this._z=.25*d}return this._onChangeCallback(),this}setFromUnitVectors(t,e){let n=t.dot(e)+1;return n<Number.EPSILON?(n=0,Math.abs(t.x)>Math.abs(t.z)?(this._x=-t.y,this._y=t.x,this._z=0,this._w=n):(this._x=0,this._y=-t.z,this._z=t.y,this._w=n)):(this._x=t.y*e.z-t.z*e.y,this._y=t.z*e.x-t.x*e.z,this._z=t.x*e.y-t.y*e.x,this._w=n),this.normalize()}angleTo(t){return 2*Math.acos(Math.abs(we(this.dot(t),-1,1)))}rotateTowards(t,e){let n=this.angleTo(t);if(n===0)return this;let s=Math.min(1,e/n);return this.slerp(t,s),this}identity(){return this.set(0,0,0,1)}invert(){return this.conjugate()}conjugate(){return this._x*=-1,this._y*=-1,this._z*=-1,this._onChangeCallback(),this}dot(t){return this._x*t._x+this._y*t._y+this._z*t._z+this._w*t._w}lengthSq(){return this._x*this._x+this._y*this._y+this._z*this._z+this._w*this._w}length(){return Math.sqrt(this._x*this._x+this._y*this._y+this._z*this._z+this._w*this._w)}normalize(){let t=this.length();return t===0?(this._x=0,this._y=0,this._z=0,this._w=1):(t=1/t,this._x=this._x*t,this._y=this._y*t,this._z=this._z*t,this._w=this._w*t),this._onChangeCallback(),this}multiply(t){return this.multiplyQuaternions(this,t)}premultiply(t){return this.multiplyQuaternions(t,this)}multiplyQuaternions(t,e){let n=t._x,s=t._y,r=t._z,o=t._w,a=e._x,c=e._y,u=e._z,f=e._w;return this._x=n*f+o*a+s*u-r*c,this._y=s*f+o*c+r*a-n*u,this._z=r*f+o*u+n*c-s*a,this._w=o*f-n*a-s*c-r*u,this._onChangeCallback(),this}slerp(t,e){if(e===0)return this;if(e===1)return this.copy(t);let n=this._x,s=this._y,r=this._z,o=this._w,a=o*t._w+n*t._x+s*t._y+r*t._z;if(a<0?(this._w=-t._w,this._x=-t._x,this._y=-t._y,this._z=-t._z,a=-a):this.copy(t),a>=1)return this._w=o,this._x=n,this._y=s,this._z=r,this;let c=1-a*a;if(c<=Number.EPSILON){let d=1-e;return this._w=d*o+e*this._w,this._x=d*n+e*this._x,this._y=d*s+e*this._y,this._z=d*r+e*this._z,this.normalize(),this}let u=Math.sqrt(c),f=Math.atan2(u,a),p=Math.sin((1-e)*f)/u,h=Math.sin(e*f)/u;return this._w=o*p+this._w*h,this._x=n*p+this._x*h,this._y=s*p+this._y*h,this._z=r*p+this._z*h,this._onChangeCallback(),this}slerpQuaternions(t,e,n){return this.copy(t).slerp(e,n)}random(){let t=2*Math.PI*Math.random(),e=2*Math.PI*Math.random(),n=Math.random(),s=Math.sqrt(1-n),r=Math.sqrt(n);return this.set(s*Math.sin(t),s*Math.cos(t),r*Math.sin(e),r*Math.cos(e))}equals(t){return t._x===this._x&&t._y===this._y&&t._z===this._z&&t._w===this._w}fromArray(t,e=0){return this._x=t[e],this._y=t[e+1],this._z=t[e+2],this._w=t[e+3],this._onChangeCallback(),this}toArray(t=[],e=0){return t[e]=this._x,t[e+1]=this._y,t[e+2]=this._z,t[e+3]=this._w,t}fromBufferAttribute(t,e){return this._x=t.getX(e),this._y=t.getY(e),this._z=t.getZ(e),this._w=t.getW(e),this._onChangeCallback(),this}toJSON(){return this.toArray()}_onChange(t){return this._onChangeCallback=t,this}_onChangeCallback(){}*[Symbol.iterator](){yield this._x,yield this._y,yield this._z,yield this._w}},P=class i{constructor(t=0,e=0,n=0){i.prototype.isVector3=!0,this.x=t,this.y=e,this.z=n}set(t,e,n){return n===void 0&&(n=this.z),this.x=t,this.y=e,this.z=n,this}setScalar(t){return this.x=t,this.y=t,this.z=t,this}setX(t){return this.x=t,this}setY(t){return this.y=t,this}setZ(t){return this.z=t,this}setComponent(t,e){switch(t){case 0:this.x=e;break;case 1:this.y=e;break;case 2:this.z=e;break;default:throw new Error("index is out of range: "+t)}return this}getComponent(t){switch(t){case 0:return this.x;case 1:return this.y;case 2:return this.z;default:throw new Error("index is out of range: "+t)}}clone(){return new this.constructor(this.x,this.y,this.z)}copy(t){return this.x=t.x,this.y=t.y,this.z=t.z,this}add(t){return this.x+=t.x,this.y+=t.y,this.z+=t.z,this}addScalar(t){return this.x+=t,this.y+=t,this.z+=t,this}addVectors(t,e){return this.x=t.x+e.x,this.y=t.y+e.y,this.z=t.z+e.z,this}addScaledVector(t,e){return this.x+=t.x*e,this.y+=t.y*e,this.z+=t.z*e,this}sub(t){return this.x-=t.x,this.y-=t.y,this.z-=t.z,this}subScalar(t){return this.x-=t,this.y-=t,this.z-=t,this}subVectors(t,e){return this.x=t.x-e.x,this.y=t.y-e.y,this.z=t.z-e.z,this}multiply(t){return this.x*=t.x,this.y*=t.y,this.z*=t.z,this}multiplyScalar(t){return this.x*=t,this.y*=t,this.z*=t,this}multiplyVectors(t,e){return this.x=t.x*e.x,this.y=t.y*e.y,this.z=t.z*e.z,this}applyEuler(t){return this.applyQuaternion(Tl.setFromEuler(t))}applyAxisAngle(t,e){return this.applyQuaternion(Tl.setFromAxisAngle(t,e))}applyMatrix3(t){let e=this.x,n=this.y,s=this.z,r=t.elements;return this.x=r[0]*e+r[3]*n+r[6]*s,this.y=r[1]*e+r[4]*n+r[7]*s,this.z=r[2]*e+r[5]*n+r[8]*s,this}applyNormalMatrix(t){return this.applyMatrix3(t).normalize()}applyMatrix4(t){let e=this.x,n=this.y,s=this.z,r=t.elements,o=1/(r[3]*e+r[7]*n+r[11]*s+r[15]);return this.x=(r[0]*e+r[4]*n+r[8]*s+r[12])*o,this.y=(r[1]*e+r[5]*n+r[9]*s+r[13])*o,this.z=(r[2]*e+r[6]*n+r[10]*s+r[14])*o,this}applyQuaternion(t){let e=this.x,n=this.y,s=this.z,r=t.x,o=t.y,a=t.z,c=t.w,u=2*(o*s-a*n),f=2*(a*e-r*s),p=2*(r*n-o*e);return this.x=e+c*u+o*p-a*f,this.y=n+c*f+a*u-r*p,this.z=s+c*p+r*f-o*u,this}project(t){return this.applyMatrix4(t.matrixWorldInverse).applyMatrix4(t.projectionMatrix)}unproject(t){return this.applyMatrix4(t.projectionMatrixInverse).applyMatrix4(t.matrixWorld)}transformDirection(t){let e=this.x,n=this.y,s=this.z,r=t.elements;return this.x=r[0]*e+r[4]*n+r[8]*s,this.y=r[1]*e+r[5]*n+r[9]*s,this.z=r[2]*e+r[6]*n+r[10]*s,this.normalize()}divide(t){return this.x/=t.x,this.y/=t.y,this.z/=t.z,this}divideScalar(t){return this.multiplyScalar(1/t)}min(t){return this.x=Math.min(this.x,t.x),this.y=Math.min(this.y,t.y),this.z=Math.min(this.z,t.z),this}max(t){return this.x=Math.max(this.x,t.x),this.y=Math.max(this.y,t.y),this.z=Math.max(this.z,t.z),this}clamp(t,e){return this.x=Math.max(t.x,Math.min(e.x,this.x)),this.y=Math.max(t.y,Math.min(e.y,this.y)),this.z=Math.max(t.z,Math.min(e.z,this.z)),this}clampScalar(t,e){return this.x=Math.max(t,Math.min(e,this.x)),this.y=Math.max(t,Math.min(e,this.y)),this.z=Math.max(t,Math.min(e,this.z)),this}clampLength(t,e){let n=this.length();return this.divideScalar(n||1).multiplyScalar(Math.max(t,Math.min(e,n)))}floor(){return this.x=Math.floor(this.x),this.y=Math.floor(this.y),this.z=Math.floor(this.z),this}ceil(){return this.x=Math.ceil(this.x),this.y=Math.ceil(this.y),this.z=Math.ceil(this.z),this}round(){return this.x=Math.round(this.x),this.y=Math.round(this.y),this.z=Math.round(this.z),this}roundToZero(){return this.x=Math.trunc(this.x),this.y=Math.trunc(this.y),this.z=Math.trunc(this.z),this}negate(){return this.x=-this.x,this.y=-this.y,this.z=-this.z,this}dot(t){return this.x*t.x+this.y*t.y+this.z*t.z}lengthSq(){return this.x*this.x+this.y*this.y+this.z*this.z}length(){return Math.sqrt(this.x*this.x+this.y*this.y+this.z*this.z)}manhattanLength(){return Math.abs(this.x)+Math.abs(this.y)+Math.abs(this.z)}normalize(){return this.divideScalar(this.length()||1)}setLength(t){return this.normalize().multiplyScalar(t)}lerp(t,e){return this.x+=(t.x-this.x)*e,this.y+=(t.y-this.y)*e,this.z+=(t.z-this.z)*e,this}lerpVectors(t,e,n){return this.x=t.x+(e.x-t.x)*n,this.y=t.y+(e.y-t.y)*n,this.z=t.z+(e.z-t.z)*n,this}cross(t){return this.crossVectors(this,t)}crossVectors(t,e){let n=t.x,s=t.y,r=t.z,o=e.x,a=e.y,c=e.z;return this.x=s*c-r*a,this.y=r*o-n*c,this.z=n*a-s*o,this}projectOnVector(t){let e=t.lengthSq();if(e===0)return this.set(0,0,0);let n=t.dot(this)/e;return this.copy(t).multiplyScalar(n)}projectOnPlane(t){return Ro.copy(this).projectOnVector(t),this.sub(Ro)}reflect(t){return this.sub(Ro.copy(t).multiplyScalar(2*this.dot(t)))}angleTo(t){let e=Math.sqrt(this.lengthSq()*t.lengthSq());if(e===0)return Math.PI/2;let n=this.dot(t)/e;return Math.acos(we(n,-1,1))}distanceTo(t){return Math.sqrt(this.distanceToSquared(t))}distanceToSquared(t){let e=this.x-t.x,n=this.y-t.y,s=this.z-t.z;return e*e+n*n+s*s}manhattanDistanceTo(t){return Math.abs(this.x-t.x)+Math.abs(this.y-t.y)+Math.abs(this.z-t.z)}setFromSpherical(t){return this.setFromSphericalCoords(t.radius,t.phi,t.theta)}setFromSphericalCoords(t,e,n){let s=Math.sin(e)*t;return this.x=s*Math.sin(n),this.y=Math.cos(e)*t,this.z=s*Math.cos(n),this}setFromCylindrical(t){return this.setFromCylindricalCoords(t.radius,t.theta,t.y)}setFromCylindricalCoords(t,e,n){return this.x=t*Math.sin(e),this.y=n,this.z=t*Math.cos(e),this}setFromMatrixPosition(t){let e=t.elements;return this.x=e[12],this.y=e[13],this.z=e[14],this}setFromMatrixScale(t){let e=this.setFromMatrixColumn(t,0).length(),n=this.setFromMatrixColumn(t,1).length(),s=this.setFromMatrixColumn(t,2).length();return this.x=e,this.y=n,this.z=s,this}setFromMatrixColumn(t,e){return this.fromArray(t.elements,e*4)}setFromMatrix3Column(t,e){return this.fromArray(t.elements,e*3)}setFromEuler(t){return this.x=t._x,this.y=t._y,this.z=t._z,this}setFromColor(t){return this.x=t.r,this.y=t.g,this.z=t.b,this}equals(t){return t.x===this.x&&t.y===this.y&&t.z===this.z}fromArray(t,e=0){return this.x=t[e],this.y=t[e+1],this.z=t[e+2],this}toArray(t=[],e=0){return t[e]=this.x,t[e+1]=this.y,t[e+2]=this.z,t}fromBufferAttribute(t,e){return this.x=t.getX(e),this.y=t.getY(e),this.z=t.getZ(e),this}random(){return this.x=Math.random(),this.y=Math.random(),this.z=Math.random(),this}randomDirection(){let t=Math.random()*Math.PI*2,e=Math.random()*2-1,n=Math.sqrt(1-e*e);return this.x=n*Math.cos(t),this.y=e,this.z=n*Math.sin(t),this}*[Symbol.iterator](){yield this.x,yield this.y,yield this.z}},Ro=new P,Tl=new jn,fi=class{constructor(t=new P(1/0,1/0,1/0),e=new P(-1/0,-1/0,-1/0)){this.isBox3=!0,this.min=t,this.max=e}set(t,e){return this.min.copy(t),this.max.copy(e),this}setFromArray(t){this.makeEmpty();for(let e=0,n=t.length;e<n;e+=3)this.expandByPoint(nn.fromArray(t,e));return this}setFromBufferAttribute(t){this.makeEmpty();for(let e=0,n=t.count;e<n;e++)this.expandByPoint(nn.fromBufferAttribute(t,e));return this}setFromPoints(t){this.makeEmpty();for(let e=0,n=t.length;e<n;e++)this.expandByPoint(t[e]);return this}setFromCenterAndSize(t,e){let n=nn.copy(e).multiplyScalar(.5);return this.min.copy(t).sub(n),this.max.copy(t).add(n),this}setFromObject(t,e=!1){return this.makeEmpty(),this.expandByObject(t,e)}clone(){return new this.constructor().copy(this)}copy(t){return this.min.copy(t.min),this.max.copy(t.max),this}makeEmpty(){return this.min.x=this.min.y=this.min.z=1/0,this.max.x=this.max.y=this.max.z=-1/0,this}isEmpty(){return this.max.x<this.min.x||this.max.y<this.min.y||this.max.z<this.min.z}getCenter(t){return this.isEmpty()?t.set(0,0,0):t.addVectors(this.min,this.max).multiplyScalar(.5)}getSize(t){return this.isEmpty()?t.set(0,0,0):t.subVectors(this.max,this.min)}expandByPoint(t){return this.min.min(t),this.max.max(t),this}expandByVector(t){return this.min.sub(t),this.max.add(t),this}expandByScalar(t){return this.min.addScalar(-t),this.max.addScalar(t),this}expandByObject(t,e=!1){t.updateWorldMatrix(!1,!1);let n=t.geometry;if(n!==void 0){let r=n.getAttribute("position");if(e===!0&&r!==void 0&&t.isInstancedMesh!==!0)for(let o=0,a=r.count;o<a;o++)t.isMesh===!0?t.getVertexPosition(o,nn):nn.fromBufferAttribute(r,o),nn.applyMatrix4(t.matrixWorld),this.expandByPoint(nn);else t.boundingBox!==void 0?(t.boundingBox===null&&t.computeBoundingBox(),Ys.copy(t.boundingBox)):(n.boundingBox===null&&n.computeBoundingBox(),Ys.copy(n.boundingBox)),Ys.applyMatrix4(t.matrixWorld),this.union(Ys)}let s=t.children;for(let r=0,o=s.length;r<o;r++)this.expandByObject(s[r],e);return this}containsPoint(t){return t.x>=this.min.x&&t.x<=this.max.x&&t.y>=this.min.y&&t.y<=this.max.y&&t.z>=this.min.z&&t.z<=this.max.z}containsBox(t){return this.min.x<=t.min.x&&t.max.x<=this.max.x&&this.min.y<=t.min.y&&t.max.y<=this.max.y&&this.min.z<=t.min.z&&t.max.z<=this.max.z}getParameter(t,e){return e.set((t.x-this.min.x)/(this.max.x-this.min.x),(t.y-this.min.y)/(this.max.y-this.min.y),(t.z-this.min.z)/(this.max.z-this.min.z))}intersectsBox(t){return t.max.x>=this.min.x&&t.min.x<=this.max.x&&t.max.y>=this.min.y&&t.min.y<=this.max.y&&t.max.z>=this.min.z&&t.min.z<=this.max.z}intersectsSphere(t){return this.clampPoint(t.center,nn),nn.distanceToSquared(t.center)<=t.radius*t.radius}intersectsPlane(t){let e,n;return t.normal.x>0?(e=t.normal.x*this.min.x,n=t.normal.x*this.max.x):(e=t.normal.x*this.max.x,n=t.normal.x*this.min.x),t.normal.y>0?(e+=t.normal.y*this.min.y,n+=t.normal.y*this.max.y):(e+=t.normal.y*this.max.y,n+=t.normal.y*this.min.y),t.normal.z>0?(e+=t.normal.z*this.min.z,n+=t.normal.z*this.max.z):(e+=t.normal.z*this.max.z,n+=t.normal.z*this.min.z),e<=-t.constant&&n>=-t.constant}intersectsTriangle(t){if(this.isEmpty())return!1;this.getCenter(ps),Zs.subVectors(this.max,ps),Ai.subVectors(t.a,ps),Ri.subVectors(t.b,ps),Ci.subVectors(t.c,ps),zn.subVectors(Ri,Ai),kn.subVectors(Ci,Ri),ei.subVectors(Ai,Ci);let e=[0,-zn.z,zn.y,0,-kn.z,kn.y,0,-ei.z,ei.y,zn.z,0,-zn.x,kn.z,0,-kn.x,ei.z,0,-ei.x,-zn.y,zn.x,0,-kn.y,kn.x,0,-ei.y,ei.x,0];return!Co(e,Ai,Ri,Ci,Zs)||(e=[1,0,0,0,1,0,0,0,1],!Co(e,Ai,Ri,Ci,Zs))?!1:($s.crossVectors(zn,kn),e=[$s.x,$s.y,$s.z],Co(e,Ai,Ri,Ci,Zs))}clampPoint(t,e){return e.copy(t).clamp(this.min,this.max)}distanceToPoint(t){return this.clampPoint(t,nn).distanceTo(t)}getBoundingSphere(t){return this.isEmpty()?t.makeEmpty():(this.getCenter(t.center),t.radius=this.getSize(nn).length()*.5),t}intersect(t){return this.min.max(t.min),this.max.min(t.max),this.isEmpty()&&this.makeEmpty(),this}union(t){return this.min.min(t.min),this.max.max(t.max),this}applyMatrix4(t){return this.isEmpty()?this:(yn[0].set(this.min.x,this.min.y,this.min.z).applyMatrix4(t),yn[1].set(this.min.x,this.min.y,this.max.z).applyMatrix4(t),yn[2].set(this.min.x,this.max.y,this.min.z).applyMatrix4(t),yn[3].set(this.min.x,this.max.y,this.max.z).applyMatrix4(t),yn[4].set(this.max.x,this.min.y,this.min.z).applyMatrix4(t),yn[5].set(this.max.x,this.min.y,this.max.z).applyMatrix4(t),yn[6].set(this.max.x,this.max.y,this.min.z).applyMatrix4(t),yn[7].set(this.max.x,this.max.y,this.max.z).applyMatrix4(t),this.setFromPoints(yn),this)}translate(t){return this.min.add(t),this.max.add(t),this}equals(t){return t.min.equals(this.min)&&t.max.equals(this.max)}},yn=[new P,new P,new P,new P,new P,new P,new P,new P],nn=new P,Ys=new fi,Ai=new P,Ri=new P,Ci=new P,zn=new P,kn=new P,ei=new P,ps=new P,Zs=new P,$s=new P,ni=new P;function Co(i,t,e,n,s){for(let r=0,o=i.length-3;r<=o;r+=3){ni.fromArray(i,r);let a=s.x*Math.abs(ni.x)+s.y*Math.abs(ni.y)+s.z*Math.abs(ni.z),c=t.dot(ni),u=e.dot(ni),f=n.dot(ni);if(Math.max(-Math.max(c,u,f),Math.min(c,u,f))>a)return!1}return!0}var bd=new fi,ms=new P,Io=new P,Qi=class{constructor(t=new P,e=-1){this.isSphere=!0,this.center=t,this.radius=e}set(t,e){return this.center.copy(t),this.radius=e,this}setFromPoints(t,e){let n=this.center;e!==void 0?n.copy(e):bd.setFromPoints(t).getCenter(n);let s=0;for(let r=0,o=t.length;r<o;r++)s=Math.max(s,n.distanceToSquared(t[r]));return this.radius=Math.sqrt(s),this}copy(t){return this.center.copy(t.center),this.radius=t.radius,this}isEmpty(){return this.radius<0}makeEmpty(){return this.center.set(0,0,0),this.radius=-1,this}containsPoint(t){return t.distanceToSquared(this.center)<=this.radius*this.radius}distanceToPoint(t){return t.distanceTo(this.center)-this.radius}intersectsSphere(t){let e=this.radius+t.radius;return t.center.distanceToSquared(this.center)<=e*e}intersectsBox(t){return t.intersectsSphere(this)}intersectsPlane(t){return Math.abs(t.distanceToPoint(this.center))<=this.radius}clampPoint(t,e){let n=this.center.distanceToSquared(t);return e.copy(t),n>this.radius*this.radius&&(e.sub(this.center).normalize(),e.multiplyScalar(this.radius).add(this.center)),e}getBoundingBox(t){return this.isEmpty()?(t.makeEmpty(),t):(t.set(this.center,this.center),t.expandByScalar(this.radius),t)}applyMatrix4(t){return this.center.applyMatrix4(t),this.radius=this.radius*t.getMaxScaleOnAxis(),this}translate(t){return this.center.add(t),this}expandByPoint(t){if(this.isEmpty())return this.center.copy(t),this.radius=0,this;ms.subVectors(t,this.center);let e=ms.lengthSq();if(e>this.radius*this.radius){let n=Math.sqrt(e),s=(n-this.radius)*.5;this.center.addScaledVector(ms,s/n),this.radius+=s}return this}union(t){return t.isEmpty()?this:this.isEmpty()?(this.copy(t),this):(this.center.equals(t.center)===!0?this.radius=Math.max(this.radius,t.radius):(Io.subVectors(t.center,this.center).setLength(t.radius),this.expandByPoint(ms.copy(t.center).add(Io)),this.expandByPoint(ms.copy(t.center).sub(Io))),this)}equals(t){return t.center.equals(this.center)&&t.radius===this.radius}clone(){return new this.constructor().copy(this)}},vn=new P,Po=new P,Js=new P,Hn=new P,Lo=new P,Ks=new P,Do=new P,Ir=class{constructor(t=new P,e=new P(0,0,-1)){this.origin=t,this.direction=e}set(t,e){return this.origin.copy(t),this.direction.copy(e),this}copy(t){return this.origin.copy(t.origin),this.direction.copy(t.direction),this}at(t,e){return e.copy(this.origin).addScaledVector(this.direction,t)}lookAt(t){return this.direction.copy(t).sub(this.origin).normalize(),this}recast(t){return this.origin.copy(this.at(t,vn)),this}closestPointToPoint(t,e){e.subVectors(t,this.origin);let n=e.dot(this.direction);return n<0?e.copy(this.origin):e.copy(this.origin).addScaledVector(this.direction,n)}distanceToPoint(t){return Math.sqrt(this.distanceSqToPoint(t))}distanceSqToPoint(t){let e=vn.subVectors(t,this.origin).dot(this.direction);return e<0?this.origin.distanceToSquared(t):(vn.copy(this.origin).addScaledVector(this.direction,e),vn.distanceToSquared(t))}distanceSqToSegment(t,e,n,s){Po.copy(t).add(e).multiplyScalar(.5),Js.copy(e).sub(t).normalize(),Hn.copy(this.origin).sub(Po);let r=t.distanceTo(e)*.5,o=-this.direction.dot(Js),a=Hn.dot(this.direction),c=-Hn.dot(Js),u=Hn.lengthSq(),f=Math.abs(1-o*o),p,h,d,g;if(f>0)if(p=o*c-a,h=o*a-c,g=r*f,p>=0)if(h>=-g)if(h<=g){let _=1/f;p*=_,h*=_,d=p*(p+o*h+2*a)+h*(o*p+h+2*c)+u}else h=r,p=Math.max(0,-(o*h+a)),d=-p*p+h*(h+2*c)+u;else h=-r,p=Math.max(0,-(o*h+a)),d=-p*p+h*(h+2*c)+u;else h<=-g?(p=Math.max(0,-(-o*r+a)),h=p>0?-r:Math.min(Math.max(-r,-c),r),d=-p*p+h*(h+2*c)+u):h<=g?(p=0,h=Math.min(Math.max(-r,-c),r),d=h*(h+2*c)+u):(p=Math.max(0,-(o*r+a)),h=p>0?r:Math.min(Math.max(-r,-c),r),d=-p*p+h*(h+2*c)+u);else h=o>0?-r:r,p=Math.max(0,-(o*h+a)),d=-p*p+h*(h+2*c)+u;return n&&n.copy(this.origin).addScaledVector(this.direction,p),s&&s.copy(Po).addScaledVector(Js,h),d}intersectSphere(t,e){vn.subVectors(t.center,this.origin);let n=vn.dot(this.direction),s=vn.dot(vn)-n*n,r=t.radius*t.radius;if(s>r)return null;let o=Math.sqrt(r-s),a=n-o,c=n+o;return c<0?null:a<0?this.at(c,e):this.at(a,e)}intersectsSphere(t){return this.distanceSqToPoint(t.center)<=t.radius*t.radius}distanceToPlane(t){let e=t.normal.dot(this.direction);if(e===0)return t.distanceToPoint(this.origin)===0?0:null;let n=-(this.origin.dot(t.normal)+t.constant)/e;return n>=0?n:null}intersectPlane(t,e){let n=this.distanceToPlane(t);return n===null?null:this.at(n,e)}intersectsPlane(t){let e=t.distanceToPoint(this.origin);return e===0||t.normal.dot(this.direction)*e<0}intersectBox(t,e){let n,s,r,o,a,c,u=1/this.direction.x,f=1/this.direction.y,p=1/this.direction.z,h=this.origin;return u>=0?(n=(t.min.x-h.x)*u,s=(t.max.x-h.x)*u):(n=(t.max.x-h.x)*u,s=(t.min.x-h.x)*u),f>=0?(r=(t.min.y-h.y)*f,o=(t.max.y-h.y)*f):(r=(t.max.y-h.y)*f,o=(t.min.y-h.y)*f),n>o||r>s||((r>n||isNaN(n))&&(n=r),(o<s||isNaN(s))&&(s=o),p>=0?(a=(t.min.z-h.z)*p,c=(t.max.z-h.z)*p):(a=(t.max.z-h.z)*p,c=(t.min.z-h.z)*p),n>c||a>s)||((a>n||n!==n)&&(n=a),(c<s||s!==s)&&(s=c),s<0)?null:this.at(n>=0?n:s,e)}intersectsBox(t){return this.intersectBox(t,vn)!==null}intersectTriangle(t,e,n,s,r){Lo.subVectors(e,t),Ks.subVectors(n,t),Do.crossVectors(Lo,Ks);let o=this.direction.dot(Do),a;if(o>0){if(s)return null;a=1}else if(o<0)a=-1,o=-o;else return null;Hn.subVectors(this.origin,t);let c=a*this.direction.dot(Ks.crossVectors(Hn,Ks));if(c<0)return null;let u=a*this.direction.dot(Lo.cross(Hn));if(u<0||c+u>o)return null;let f=-a*Hn.dot(Do);return f<0?null:this.at(f/o,r)}applyMatrix4(t){return this.origin.applyMatrix4(t),this.direction.transformDirection(t),this}equals(t){return t.origin.equals(this.origin)&&t.direction.equals(this.direction)}clone(){return new this.constructor().copy(this)}},he=class i{constructor(t,e,n,s,r,o,a,c,u,f,p,h,d,g,_,m){i.prototype.isMatrix4=!0,this.elements=[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1],t!==void 0&&this.set(t,e,n,s,r,o,a,c,u,f,p,h,d,g,_,m)}set(t,e,n,s,r,o,a,c,u,f,p,h,d,g,_,m){let l=this.elements;return l[0]=t,l[4]=e,l[8]=n,l[12]=s,l[1]=r,l[5]=o,l[9]=a,l[13]=c,l[2]=u,l[6]=f,l[10]=p,l[14]=h,l[3]=d,l[7]=g,l[11]=_,l[15]=m,this}identity(){return this.set(1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1),this}clone(){return new i().fromArray(this.elements)}copy(t){let e=this.elements,n=t.elements;return e[0]=n[0],e[1]=n[1],e[2]=n[2],e[3]=n[3],e[4]=n[4],e[5]=n[5],e[6]=n[6],e[7]=n[7],e[8]=n[8],e[9]=n[9],e[10]=n[10],e[11]=n[11],e[12]=n[12],e[13]=n[13],e[14]=n[14],e[15]=n[15],this}copyPosition(t){let e=this.elements,n=t.elements;return e[12]=n[12],e[13]=n[13],e[14]=n[14],this}setFromMatrix3(t){let e=t.elements;return this.set(e[0],e[3],e[6],0,e[1],e[4],e[7],0,e[2],e[5],e[8],0,0,0,0,1),this}extractBasis(t,e,n){return t.setFromMatrixColumn(this,0),e.setFromMatrixColumn(this,1),n.setFromMatrixColumn(this,2),this}makeBasis(t,e,n){return this.set(t.x,e.x,n.x,0,t.y,e.y,n.y,0,t.z,e.z,n.z,0,0,0,0,1),this}extractRotation(t){let e=this.elements,n=t.elements,s=1/Ii.setFromMatrixColumn(t,0).length(),r=1/Ii.setFromMatrixColumn(t,1).length(),o=1/Ii.setFromMatrixColumn(t,2).length();return e[0]=n[0]*s,e[1]=n[1]*s,e[2]=n[2]*s,e[3]=0,e[4]=n[4]*r,e[5]=n[5]*r,e[6]=n[6]*r,e[7]=0,e[8]=n[8]*o,e[9]=n[9]*o,e[10]=n[10]*o,e[11]=0,e[12]=0,e[13]=0,e[14]=0,e[15]=1,this}makeRotationFromEuler(t){let e=this.elements,n=t.x,s=t.y,r=t.z,o=Math.cos(n),a=Math.sin(n),c=Math.cos(s),u=Math.sin(s),f=Math.cos(r),p=Math.sin(r);if(t.order==="XYZ"){let h=o*f,d=o*p,g=a*f,_=a*p;e[0]=c*f,e[4]=-c*p,e[8]=u,e[1]=d+g*u,e[5]=h-_*u,e[9]=-a*c,e[2]=_-h*u,e[6]=g+d*u,e[10]=o*c}else if(t.order==="YXZ"){let h=c*f,d=c*p,g=u*f,_=u*p;e[0]=h+_*a,e[4]=g*a-d,e[8]=o*u,e[1]=o*p,e[5]=o*f,e[9]=-a,e[2]=d*a-g,e[6]=_+h*a,e[10]=o*c}else if(t.order==="ZXY"){let h=c*f,d=c*p,g=u*f,_=u*p;e[0]=h-_*a,e[4]=-o*p,e[8]=g+d*a,e[1]=d+g*a,e[5]=o*f,e[9]=_-h*a,e[2]=-o*u,e[6]=a,e[10]=o*c}else if(t.order==="ZYX"){let h=o*f,d=o*p,g=a*f,_=a*p;e[0]=c*f,e[4]=g*u-d,e[8]=h*u+_,e[1]=c*p,e[5]=_*u+h,e[9]=d*u-g,e[2]=-u,e[6]=a*c,e[10]=o*c}else if(t.order==="YZX"){let h=o*c,d=o*u,g=a*c,_=a*u;e[0]=c*f,e[4]=_-h*p,e[8]=g*p+d,e[1]=p,e[5]=o*f,e[9]=-a*f,e[2]=-u*f,e[6]=d*p+g,e[10]=h-_*p}else if(t.order==="XZY"){let h=o*c,d=o*u,g=a*c,_=a*u;e[0]=c*f,e[4]=-p,e[8]=u*f,e[1]=h*p+_,e[5]=o*f,e[9]=d*p-g,e[2]=g*p-d,e[6]=a*f,e[10]=_*p+h}return e[3]=0,e[7]=0,e[11]=0,e[12]=0,e[13]=0,e[14]=0,e[15]=1,this}makeRotationFromQuaternion(t){return this.compose(wd,t,Td)}lookAt(t,e,n){let s=this.elements;return Ge.subVectors(t,e),Ge.lengthSq()===0&&(Ge.z=1),Ge.normalize(),Vn.crossVectors(n,Ge),Vn.lengthSq()===0&&(Math.abs(n.z)===1?Ge.x+=1e-4:Ge.z+=1e-4,Ge.normalize(),Vn.crossVectors(n,Ge)),Vn.normalize(),js.crossVectors(Ge,Vn),s[0]=Vn.x,s[4]=js.x,s[8]=Ge.x,s[1]=Vn.y,s[5]=js.y,s[9]=Ge.y,s[2]=Vn.z,s[6]=js.z,s[10]=Ge.z,this}multiply(t){return this.multiplyMatrices(this,t)}premultiply(t){return this.multiplyMatrices(t,this)}multiplyMatrices(t,e){let n=t.elements,s=e.elements,r=this.elements,o=n[0],a=n[4],c=n[8],u=n[12],f=n[1],p=n[5],h=n[9],d=n[13],g=n[2],_=n[6],m=n[10],l=n[14],S=n[3],b=n[7],y=n[11],I=n[15],T=s[0],R=s[4],A=s[8],E=s[12],v=s[1],C=s[5],H=s[9],k=s[13],W=s[2],J=s[6],X=s[10],tt=s[14],G=s[3],st=s[7],dt=s[11],bt=s[15];return r[0]=o*T+a*v+c*W+u*G,r[4]=o*R+a*C+c*J+u*st,r[8]=o*A+a*H+c*X+u*dt,r[12]=o*E+a*k+c*tt+u*bt,r[1]=f*T+p*v+h*W+d*G,r[5]=f*R+p*C+h*J+d*st,r[9]=f*A+p*H+h*X+d*dt,r[13]=f*E+p*k+h*tt+d*bt,r[2]=g*T+_*v+m*W+l*G,r[6]=g*R+_*C+m*J+l*st,r[10]=g*A+_*H+m*X+l*dt,r[14]=g*E+_*k+m*tt+l*bt,r[3]=S*T+b*v+y*W+I*G,r[7]=S*R+b*C+y*J+I*st,r[11]=S*A+b*H+y*X+I*dt,r[15]=S*E+b*k+y*tt+I*bt,this}multiplyScalar(t){let e=this.elements;return e[0]*=t,e[4]*=t,e[8]*=t,e[12]*=t,e[1]*=t,e[5]*=t,e[9]*=t,e[13]*=t,e[2]*=t,e[6]*=t,e[10]*=t,e[14]*=t,e[3]*=t,e[7]*=t,e[11]*=t,e[15]*=t,this}determinant(){let t=this.elements,e=t[0],n=t[4],s=t[8],r=t[12],o=t[1],a=t[5],c=t[9],u=t[13],f=t[2],p=t[6],h=t[10],d=t[14],g=t[3],_=t[7],m=t[11],l=t[15];return g*(+r*c*p-s*u*p-r*a*h+n*u*h+s*a*d-n*c*d)+_*(+e*c*d-e*u*h+r*o*h-s*o*d+s*u*f-r*c*f)+m*(+e*u*p-e*a*d-r*o*p+n*o*d+r*a*f-n*u*f)+l*(-s*a*f-e*c*p+e*a*h+s*o*p-n*o*h+n*c*f)}transpose(){let t=this.elements,e;return e=t[1],t[1]=t[4],t[4]=e,e=t[2],t[2]=t[8],t[8]=e,e=t[6],t[6]=t[9],t[9]=e,e=t[3],t[3]=t[12],t[12]=e,e=t[7],t[7]=t[13],t[13]=e,e=t[11],t[11]=t[14],t[14]=e,this}setPosition(t,e,n){let s=this.elements;return t.isVector3?(s[12]=t.x,s[13]=t.y,s[14]=t.z):(s[12]=t,s[13]=e,s[14]=n),this}invert(){let t=this.elements,e=t[0],n=t[1],s=t[2],r=t[3],o=t[4],a=t[5],c=t[6],u=t[7],f=t[8],p=t[9],h=t[10],d=t[11],g=t[12],_=t[13],m=t[14],l=t[15],S=p*m*u-_*h*u+_*c*d-a*m*d-p*c*l+a*h*l,b=g*h*u-f*m*u-g*c*d+o*m*d+f*c*l-o*h*l,y=f*_*u-g*p*u+g*a*d-o*_*d-f*a*l+o*p*l,I=g*p*c-f*_*c-g*a*h+o*_*h+f*a*m-o*p*m,T=e*S+n*b+s*y+r*I;if(T===0)return this.set(0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0);let R=1/T;return t[0]=S*R,t[1]=(_*h*r-p*m*r-_*s*d+n*m*d+p*s*l-n*h*l)*R,t[2]=(a*m*r-_*c*r+_*s*u-n*m*u-a*s*l+n*c*l)*R,t[3]=(p*c*r-a*h*r-p*s*u+n*h*u+a*s*d-n*c*d)*R,t[4]=b*R,t[5]=(f*m*r-g*h*r+g*s*d-e*m*d-f*s*l+e*h*l)*R,t[6]=(g*c*r-o*m*r-g*s*u+e*m*u+o*s*l-e*c*l)*R,t[7]=(o*h*r-f*c*r+f*s*u-e*h*u-o*s*d+e*c*d)*R,t[8]=y*R,t[9]=(g*p*r-f*_*r-g*n*d+e*_*d+f*n*l-e*p*l)*R,t[10]=(o*_*r-g*a*r+g*n*u-e*_*u-o*n*l+e*a*l)*R,t[11]=(f*a*r-o*p*r-f*n*u+e*p*u+o*n*d-e*a*d)*R,t[12]=I*R,t[13]=(f*_*s-g*p*s+g*n*h-e*_*h-f*n*m+e*p*m)*R,t[14]=(g*a*s-o*_*s-g*n*c+e*_*c+o*n*m-e*a*m)*R,t[15]=(o*p*s-f*a*s+f*n*c-e*p*c-o*n*h+e*a*h)*R,this}scale(t){let e=this.elements,n=t.x,s=t.y,r=t.z;return e[0]*=n,e[4]*=s,e[8]*=r,e[1]*=n,e[5]*=s,e[9]*=r,e[2]*=n,e[6]*=s,e[10]*=r,e[3]*=n,e[7]*=s,e[11]*=r,this}getMaxScaleOnAxis(){let t=this.elements,e=t[0]*t[0]+t[1]*t[1]+t[2]*t[2],n=t[4]*t[4]+t[5]*t[5]+t[6]*t[6],s=t[8]*t[8]+t[9]*t[9]+t[10]*t[10];return Math.sqrt(Math.max(e,n,s))}makeTranslation(t,e,n){return t.isVector3?this.set(1,0,0,t.x,0,1,0,t.y,0,0,1,t.z,0,0,0,1):this.set(1,0,0,t,0,1,0,e,0,0,1,n,0,0,0,1),this}makeRotationX(t){let e=Math.cos(t),n=Math.sin(t);return this.set(1,0,0,0,0,e,-n,0,0,n,e,0,0,0,0,1),this}makeRotationY(t){let e=Math.cos(t),n=Math.sin(t);return this.set(e,0,n,0,0,1,0,0,-n,0,e,0,0,0,0,1),this}makeRotationZ(t){let e=Math.cos(t),n=Math.sin(t);return this.set(e,-n,0,0,n,e,0,0,0,0,1,0,0,0,0,1),this}makeRotationAxis(t,e){let n=Math.cos(e),s=Math.sin(e),r=1-n,o=t.x,a=t.y,c=t.z,u=r*o,f=r*a;return this.set(u*o+n,u*a-s*c,u*c+s*a,0,u*a+s*c,f*a+n,f*c-s*o,0,u*c-s*a,f*c+s*o,r*c*c+n,0,0,0,0,1),this}makeScale(t,e,n){return this.set(t,0,0,0,0,e,0,0,0,0,n,0,0,0,0,1),this}makeShear(t,e,n,s,r,o){return this.set(1,n,r,0,t,1,o,0,e,s,1,0,0,0,0,1),this}compose(t,e,n){let s=this.elements,r=e._x,o=e._y,a=e._z,c=e._w,u=r+r,f=o+o,p=a+a,h=r*u,d=r*f,g=r*p,_=o*f,m=o*p,l=a*p,S=c*u,b=c*f,y=c*p,I=n.x,T=n.y,R=n.z;return s[0]=(1-(_+l))*I,s[1]=(d+y)*I,s[2]=(g-b)*I,s[3]=0,s[4]=(d-y)*T,s[5]=(1-(h+l))*T,s[6]=(m+S)*T,s[7]=0,s[8]=(g+b)*R,s[9]=(m-S)*R,s[10]=(1-(h+_))*R,s[11]=0,s[12]=t.x,s[13]=t.y,s[14]=t.z,s[15]=1,this}decompose(t,e,n){let s=this.elements,r=Ii.set(s[0],s[1],s[2]).length(),o=Ii.set(s[4],s[5],s[6]).length(),a=Ii.set(s[8],s[9],s[10]).length();this.determinant()<0&&(r=-r),t.x=s[12],t.y=s[13],t.z=s[14],sn.copy(this);let u=1/r,f=1/o,p=1/a;return sn.elements[0]*=u,sn.elements[1]*=u,sn.elements[2]*=u,sn.elements[4]*=f,sn.elements[5]*=f,sn.elements[6]*=f,sn.elements[8]*=p,sn.elements[9]*=p,sn.elements[10]*=p,e.setFromRotationMatrix(sn),n.x=r,n.y=o,n.z=a,this}makePerspective(t,e,n,s,r,o,a=An){let c=this.elements,u=2*r/(e-t),f=2*r/(n-s),p=(e+t)/(e-t),h=(n+s)/(n-s),d,g;if(a===An)d=-(o+r)/(o-r),g=-2*o*r/(o-r);else if(a===Tr)d=-o/(o-r),g=-o*r/(o-r);else throw new Error("THREE.Matrix4.makePerspective(): Invalid coordinate system: "+a);return c[0]=u,c[4]=0,c[8]=p,c[12]=0,c[1]=0,c[5]=f,c[9]=h,c[13]=0,c[2]=0,c[6]=0,c[10]=d,c[14]=g,c[3]=0,c[7]=0,c[11]=-1,c[15]=0,this}makeOrthographic(t,e,n,s,r,o,a=An){let c=this.elements,u=1/(e-t),f=1/(n-s),p=1/(o-r),h=(e+t)*u,d=(n+s)*f,g,_;if(a===An)g=(o+r)*p,_=-2*p;else if(a===Tr)g=r*p,_=-1*p;else throw new Error("THREE.Matrix4.makeOrthographic(): Invalid coordinate system: "+a);return c[0]=2*u,c[4]=0,c[8]=0,c[12]=-h,c[1]=0,c[5]=2*f,c[9]=0,c[13]=-d,c[2]=0,c[6]=0,c[10]=_,c[14]=-g,c[3]=0,c[7]=0,c[11]=0,c[15]=1,this}equals(t){let e=this.elements,n=t.elements;for(let s=0;s<16;s++)if(e[s]!==n[s])return!1;return!0}fromArray(t,e=0){for(let n=0;n<16;n++)this.elements[n]=t[n+e];return this}toArray(t=[],e=0){let n=this.elements;return t[e]=n[0],t[e+1]=n[1],t[e+2]=n[2],t[e+3]=n[3],t[e+4]=n[4],t[e+5]=n[5],t[e+6]=n[6],t[e+7]=n[7],t[e+8]=n[8],t[e+9]=n[9],t[e+10]=n[10],t[e+11]=n[11],t[e+12]=n[12],t[e+13]=n[13],t[e+14]=n[14],t[e+15]=n[15],t}},Ii=new P,sn=new he,wd=new P(0,0,0),Td=new P(1,1,1),Vn=new P,js=new P,Ge=new P,Al=new he,Rl=new jn,mn=class i{constructor(t=0,e=0,n=0,s=i.DEFAULT_ORDER){this.isEuler=!0,this._x=t,this._y=e,this._z=n,this._order=s}get x(){return this._x}set x(t){this._x=t,this._onChangeCallback()}get y(){return this._y}set y(t){this._y=t,this._onChangeCallback()}get z(){return this._z}set z(t){this._z=t,this._onChangeCallback()}get order(){return this._order}set order(t){this._order=t,this._onChangeCallback()}set(t,e,n,s=this._order){return this._x=t,this._y=e,this._z=n,this._order=s,this._onChangeCallback(),this}clone(){return new this.constructor(this._x,this._y,this._z,this._order)}copy(t){return this._x=t._x,this._y=t._y,this._z=t._z,this._order=t._order,this._onChangeCallback(),this}setFromRotationMatrix(t,e=this._order,n=!0){let s=t.elements,r=s[0],o=s[4],a=s[8],c=s[1],u=s[5],f=s[9],p=s[2],h=s[6],d=s[10];switch(e){case"XYZ":this._y=Math.asin(we(a,-1,1)),Math.abs(a)<.9999999?(this._x=Math.atan2(-f,d),this._z=Math.atan2(-o,r)):(this._x=Math.atan2(h,u),this._z=0);break;case"YXZ":this._x=Math.asin(-we(f,-1,1)),Math.abs(f)<.9999999?(this._y=Math.atan2(a,d),this._z=Math.atan2(c,u)):(this._y=Math.atan2(-p,r),this._z=0);break;case"ZXY":this._x=Math.asin(we(h,-1,1)),Math.abs(h)<.9999999?(this._y=Math.atan2(-p,d),this._z=Math.atan2(-o,u)):(this._y=0,this._z=Math.atan2(c,r));break;case"ZYX":this._y=Math.asin(-we(p,-1,1)),Math.abs(p)<.9999999?(this._x=Math.atan2(h,d),this._z=Math.atan2(c,r)):(this._x=0,this._z=Math.atan2(-o,u));break;case"YZX":this._z=Math.asin(we(c,-1,1)),Math.abs(c)<.9999999?(this._x=Math.atan2(-f,u),this._y=Math.atan2(-p,r)):(this._x=0,this._y=Math.atan2(a,d));break;case"XZY":this._z=Math.asin(-we(o,-1,1)),Math.abs(o)<.9999999?(this._x=Math.atan2(h,u),this._y=Math.atan2(a,r)):(this._x=Math.atan2(-f,d),this._y=0);break;default:console.warn("THREE.Euler: .setFromRotationMatrix() encountered an unknown order: "+e)}return this._order=e,n===!0&&this._onChangeCallback(),this}setFromQuaternion(t,e,n){return Al.makeRotationFromQuaternion(t),this.setFromRotationMatrix(Al,e,n)}setFromVector3(t,e=this._order){return this.set(t.x,t.y,t.z,e)}reorder(t){return Rl.setFromEuler(this),this.setFromQuaternion(Rl,t)}equals(t){return t._x===this._x&&t._y===this._y&&t._z===this._z&&t._order===this._order}fromArray(t){return this._x=t[0],this._y=t[1],this._z=t[2],t[3]!==void 0&&(this._order=t[3]),this._onChangeCallback(),this}toArray(t=[],e=0){return t[e]=this._x,t[e+1]=this._y,t[e+2]=this._z,t[e+3]=this._order,t}_onChange(t){return this._onChangeCallback=t,this}_onChangeCallback(){}*[Symbol.iterator](){yield this._x,yield this._y,yield this._z,yield this._order}};mn.DEFAULT_ORDER="XYZ";var Pr=class{constructor(){this.mask=1}set(t){this.mask=(1<<t|0)>>>0}enable(t){this.mask|=1<<t|0}enableAll(){this.mask=-1}toggle(t){this.mask^=1<<t|0}disable(t){this.mask&=~(1<<t|0)}disableAll(){this.mask=0}test(t){return(this.mask&t.mask)!==0}isEnabled(t){return(this.mask&(1<<t|0))!==0}},Ad=0,Cl=new P,Pi=new jn,Mn=new he,Qs=new P,gs=new P,Rd=new P,Cd=new jn,Il=new P(1,0,0),Pl=new P(0,1,0),Ll=new P(0,0,1),Dl={type:"added"},Id={type:"removed"},Li={type:"childadded",child:null},Uo={type:"childremoved",child:null},Te=class i extends Kn{constructor(){super(),this.isObject3D=!0,Object.defineProperty(this,"id",{value:Ad++}),this.uuid=$n(),this.name="",this.type="Object3D",this.parent=null,this.children=[],this.up=i.DEFAULT_UP.clone();let t=new P,e=new mn,n=new jn,s=new P(1,1,1);function r(){n.setFromEuler(e,!1)}function o(){e.setFromQuaternion(n,void 0,!1)}e._onChange(r),n._onChange(o),Object.defineProperties(this,{position:{configurable:!0,enumerable:!0,value:t},rotation:{configurable:!0,enumerable:!0,value:e},quaternion:{configurable:!0,enumerable:!0,value:n},scale:{configurable:!0,enumerable:!0,value:s},modelViewMatrix:{value:new he},normalMatrix:{value:new Ft}}),this.matrix=new he,this.matrixWorld=new he,this.matrixAutoUpdate=i.DEFAULT_MATRIX_AUTO_UPDATE,this.matrixWorldAutoUpdate=i.DEFAULT_MATRIX_WORLD_AUTO_UPDATE,this.matrixWorldNeedsUpdate=!1,this.layers=new Pr,this.visible=!0,this.castShadow=!1,this.receiveShadow=!1,this.frustumCulled=!0,this.renderOrder=0,this.animations=[],this.userData={}}onBeforeShadow(){}onAfterShadow(){}onBeforeRender(){}onAfterRender(){}applyMatrix4(t){this.matrixAutoUpdate&&this.updateMatrix(),this.matrix.premultiply(t),this.matrix.decompose(this.position,this.quaternion,this.scale)}applyQuaternion(t){return this.quaternion.premultiply(t),this}setRotationFromAxisAngle(t,e){this.quaternion.setFromAxisAngle(t,e)}setRotationFromEuler(t){this.quaternion.setFromEuler(t,!0)}setRotationFromMatrix(t){this.quaternion.setFromRotationMatrix(t)}setRotationFromQuaternion(t){this.quaternion.copy(t)}rotateOnAxis(t,e){return Pi.setFromAxisAngle(t,e),this.quaternion.multiply(Pi),this}rotateOnWorldAxis(t,e){return Pi.setFromAxisAngle(t,e),this.quaternion.premultiply(Pi),this}rotateX(t){return this.rotateOnAxis(Il,t)}rotateY(t){return this.rotateOnAxis(Pl,t)}rotateZ(t){return this.rotateOnAxis(Ll,t)}translateOnAxis(t,e){return Cl.copy(t).applyQuaternion(this.quaternion),this.position.add(Cl.multiplyScalar(e)),this}translateX(t){return this.translateOnAxis(Il,t)}translateY(t){return this.translateOnAxis(Pl,t)}translateZ(t){return this.translateOnAxis(Ll,t)}localToWorld(t){return this.updateWorldMatrix(!0,!1),t.applyMatrix4(this.matrixWorld)}worldToLocal(t){return this.updateWorldMatrix(!0,!1),t.applyMatrix4(Mn.copy(this.matrixWorld).invert())}lookAt(t,e,n){t.isVector3?Qs.copy(t):Qs.set(t,e,n);let s=this.parent;this.updateWorldMatrix(!0,!1),gs.setFromMatrixPosition(this.matrixWorld),this.isCamera||this.isLight?Mn.lookAt(gs,Qs,this.up):Mn.lookAt(Qs,gs,this.up),this.quaternion.setFromRotationMatrix(Mn),s&&(Mn.extractRotation(s.matrixWorld),Pi.setFromRotationMatrix(Mn),this.quaternion.premultiply(Pi.invert()))}add(t){if(arguments.length>1){for(let e=0;e<arguments.length;e++)this.add(arguments[e]);return this}return t===this?(console.error("THREE.Object3D.add: object can't be added as a child of itself.",t),this):(t&&t.isObject3D?(t.removeFromParent(),t.parent=this,this.children.push(t),t.dispatchEvent(Dl),Li.child=t,this.dispatchEvent(Li),Li.child=null):console.error("THREE.Object3D.add: object not an instance of THREE.Object3D.",t),this)}remove(t){if(arguments.length>1){for(let n=0;n<arguments.length;n++)this.remove(arguments[n]);return this}let e=this.children.indexOf(t);return e!==-1&&(t.parent=null,this.children.splice(e,1),t.dispatchEvent(Id),Uo.child=t,this.dispatchEvent(Uo),Uo.child=null),this}removeFromParent(){let t=this.parent;return t!==null&&t.remove(this),this}clear(){return this.remove(...this.children)}attach(t){return this.updateWorldMatrix(!0,!1),Mn.copy(this.matrixWorld).invert(),t.parent!==null&&(t.parent.updateWorldMatrix(!0,!1),Mn.multiply(t.parent.matrixWorld)),t.applyMatrix4(Mn),t.removeFromParent(),t.parent=this,this.children.push(t),t.updateWorldMatrix(!1,!0),t.dispatchEvent(Dl),Li.child=t,this.dispatchEvent(Li),Li.child=null,this}getObjectById(t){return this.getObjectByProperty("id",t)}getObjectByName(t){return this.getObjectByProperty("name",t)}getObjectByProperty(t,e){if(this[t]===e)return this;for(let n=0,s=this.children.length;n<s;n++){let o=this.children[n].getObjectByProperty(t,e);if(o!==void 0)return o}}getObjectsByProperty(t,e,n=[]){this[t]===e&&n.push(this);let s=this.children;for(let r=0,o=s.length;r<o;r++)s[r].getObjectsByProperty(t,e,n);return n}getWorldPosition(t){return this.updateWorldMatrix(!0,!1),t.setFromMatrixPosition(this.matrixWorld)}getWorldQuaternion(t){return this.updateWorldMatrix(!0,!1),this.matrixWorld.decompose(gs,t,Rd),t}getWorldScale(t){return this.updateWorldMatrix(!0,!1),this.matrixWorld.decompose(gs,Cd,t),t}getWorldDirection(t){this.updateWorldMatrix(!0,!1);let e=this.matrixWorld.elements;return t.set(e[8],e[9],e[10]).normalize()}raycast(){}traverse(t){t(this);let e=this.children;for(let n=0,s=e.length;n<s;n++)e[n].traverse(t)}traverseVisible(t){if(this.visible===!1)return;t(this);let e=this.children;for(let n=0,s=e.length;n<s;n++)e[n].traverseVisible(t)}traverseAncestors(t){let e=this.parent;e!==null&&(t(e),e.traverseAncestors(t))}updateMatrix(){this.matrix.compose(this.position,this.quaternion,this.scale),this.matrixWorldNeedsUpdate=!0}updateMatrixWorld(t){this.matrixAutoUpdate&&this.updateMatrix(),(this.matrixWorldNeedsUpdate||t)&&(this.matrixWorldAutoUpdate===!0&&(this.parent===null?this.matrixWorld.copy(this.matrix):this.matrixWorld.multiplyMatrices(this.parent.matrixWorld,this.matrix)),this.matrixWorldNeedsUpdate=!1,t=!0);let e=this.children;for(let n=0,s=e.length;n<s;n++)e[n].updateMatrixWorld(t)}updateWorldMatrix(t,e){let n=this.parent;if(t===!0&&n!==null&&n.updateWorldMatrix(!0,!1),this.matrixAutoUpdate&&this.updateMatrix(),this.matrixWorldAutoUpdate===!0&&(this.parent===null?this.matrixWorld.copy(this.matrix):this.matrixWorld.multiplyMatrices(this.parent.matrixWorld,this.matrix)),e===!0){let s=this.children;for(let r=0,o=s.length;r<o;r++)s[r].updateWorldMatrix(!1,!0)}}toJSON(t){let e=t===void 0||typeof t=="string",n={};e&&(t={geometries:{},materials:{},textures:{},images:{},shapes:{},skeletons:{},animations:{},nodes:{}},n.metadata={version:4.6,type:"Object",generator:"Object3D.toJSON"});let s={};s.uuid=this.uuid,s.type=this.type,this.name!==""&&(s.name=this.name),this.castShadow===!0&&(s.castShadow=!0),this.receiveShadow===!0&&(s.receiveShadow=!0),this.visible===!1&&(s.visible=!1),this.frustumCulled===!1&&(s.frustumCulled=!1),this.renderOrder!==0&&(s.renderOrder=this.renderOrder),Object.keys(this.userData).length>0&&(s.userData=this.userData),s.layers=this.layers.mask,s.matrix=this.matrix.toArray(),s.up=this.up.toArray(),this.matrixAutoUpdate===!1&&(s.matrixAutoUpdate=!1),this.isInstancedMesh&&(s.type="InstancedMesh",s.count=this.count,s.instanceMatrix=this.instanceMatrix.toJSON(),this.instanceColor!==null&&(s.instanceColor=this.instanceColor.toJSON())),this.isBatchedMesh&&(s.type="BatchedMesh",s.perObjectFrustumCulled=this.perObjectFrustumCulled,s.sortObjects=this.sortObjects,s.drawRanges=this._drawRanges,s.reservedRanges=this._reservedRanges,s.visibility=this._visibility,s.active=this._active,s.bounds=this._bounds.map(a=>({boxInitialized:a.boxInitialized,boxMin:a.box.min.toArray(),boxMax:a.box.max.toArray(),sphereInitialized:a.sphereInitialized,sphereRadius:a.sphere.radius,sphereCenter:a.sphere.center.toArray()})),s.maxInstanceCount=this._maxInstanceCount,s.maxVertexCount=this._maxVertexCount,s.maxIndexCount=this._maxIndexCount,s.geometryInitialized=this._geometryInitialized,s.geometryCount=this._geometryCount,s.matricesTexture=this._matricesTexture.toJSON(t),this._colorsTexture!==null&&(s.colorsTexture=this._colorsTexture.toJSON(t)),this.boundingSphere!==null&&(s.boundingSphere={center:s.boundingSphere.center.toArray(),radius:s.boundingSphere.radius}),this.boundingBox!==null&&(s.boundingBox={min:s.boundingBox.min.toArray(),max:s.boundingBox.max.toArray()}));function r(a,c){return a[c.uuid]===void 0&&(a[c.uuid]=c.toJSON(t)),c.uuid}if(this.isScene)this.background&&(this.background.isColor?s.background=this.background.toJSON():this.background.isTexture&&(s.background=this.background.toJSON(t).uuid)),this.environment&&this.environment.isTexture&&this.environment.isRenderTargetTexture!==!0&&(s.environment=this.environment.toJSON(t).uuid);else if(this.isMesh||this.isLine||this.isPoints){s.geometry=r(t.geometries,this.geometry);let a=this.geometry.parameters;if(a!==void 0&&a.shapes!==void 0){let c=a.shapes;if(Array.isArray(c))for(let u=0,f=c.length;u<f;u++){let p=c[u];r(t.shapes,p)}else r(t.shapes,c)}}if(this.isSkinnedMesh&&(s.bindMode=this.bindMode,s.bindMatrix=this.bindMatrix.toArray(),this.skeleton!==void 0&&(r(t.skeletons,this.skeleton),s.skeleton=this.skeleton.uuid)),this.material!==void 0)if(Array.isArray(this.material)){let a=[];for(let c=0,u=this.material.length;c<u;c++)a.push(r(t.materials,this.material[c]));s.material=a}else s.material=r(t.materials,this.material);if(this.children.length>0){s.children=[];for(let a=0;a<this.children.length;a++)s.children.push(this.children[a].toJSON(t).object)}if(this.animations.length>0){s.animations=[];for(let a=0;a<this.animations.length;a++){let c=this.animations[a];s.animations.push(r(t.animations,c))}}if(e){let a=o(t.geometries),c=o(t.materials),u=o(t.textures),f=o(t.images),p=o(t.shapes),h=o(t.skeletons),d=o(t.animations),g=o(t.nodes);a.length>0&&(n.geometries=a),c.length>0&&(n.materials=c),u.length>0&&(n.textures=u),f.length>0&&(n.images=f),p.length>0&&(n.shapes=p),h.length>0&&(n.skeletons=h),d.length>0&&(n.animations=d),g.length>0&&(n.nodes=g)}return n.object=s,n;function o(a){let c=[];for(let u in a){let f=a[u];delete f.metadata,c.push(f)}return c}}clone(t){return new this.constructor().copy(this,t)}copy(t,e=!0){if(this.name=t.name,this.up.copy(t.up),this.position.copy(t.position),this.rotation.order=t.rotation.order,this.quaternion.copy(t.quaternion),this.scale.copy(t.scale),this.matrix.copy(t.matrix),this.matrixWorld.copy(t.matrixWorld),this.matrixAutoUpdate=t.matrixAutoUpdate,this.matrixWorldAutoUpdate=t.matrixWorldAutoUpdate,this.matrixWorldNeedsUpdate=t.matrixWorldNeedsUpdate,this.layers.mask=t.layers.mask,this.visible=t.visible,this.castShadow=t.castShadow,this.receiveShadow=t.receiveShadow,this.frustumCulled=t.frustumCulled,this.renderOrder=t.renderOrder,this.animations=t.animations.slice(),this.userData=JSON.parse(JSON.stringify(t.userData)),e===!0)for(let n=0;n<t.children.length;n++){let s=t.children[n];this.add(s.clone())}return this}};Te.DEFAULT_UP=new P(0,1,0);Te.DEFAULT_MATRIX_AUTO_UPDATE=!0;Te.DEFAULT_MATRIX_WORLD_AUTO_UPDATE=!0;var rn=new P,Sn=new P,Fo=new P,En=new P,Di=new P,Ui=new P,Ul=new P,No=new P,Oo=new P,Bo=new P,zo=new pe,ko=new pe,Ho=new pe,qn=class i{constructor(t=new P,e=new P,n=new P){this.a=t,this.b=e,this.c=n}static getNormal(t,e,n,s){s.subVectors(n,e),rn.subVectors(t,e),s.cross(rn);let r=s.lengthSq();return r>0?s.multiplyScalar(1/Math.sqrt(r)):s.set(0,0,0)}static getBarycoord(t,e,n,s,r){rn.subVectors(s,e),Sn.subVectors(n,e),Fo.subVectors(t,e);let o=rn.dot(rn),a=rn.dot(Sn),c=rn.dot(Fo),u=Sn.dot(Sn),f=Sn.dot(Fo),p=o*u-a*a;if(p===0)return r.set(0,0,0),null;let h=1/p,d=(u*c-a*f)*h,g=(o*f-a*c)*h;return r.set(1-d-g,g,d)}static containsPoint(t,e,n,s){return this.getBarycoord(t,e,n,s,En)===null?!1:En.x>=0&&En.y>=0&&En.x+En.y<=1}static getInterpolation(t,e,n,s,r,o,a,c){return this.getBarycoord(t,e,n,s,En)===null?(c.x=0,c.y=0,"z"in c&&(c.z=0),"w"in c&&(c.w=0),null):(c.setScalar(0),c.addScaledVector(r,En.x),c.addScaledVector(o,En.y),c.addScaledVector(a,En.z),c)}static getInterpolatedAttribute(t,e,n,s,r,o){return zo.setScalar(0),ko.setScalar(0),Ho.setScalar(0),zo.fromBufferAttribute(t,e),ko.fromBufferAttribute(t,n),Ho.fromBufferAttribute(t,s),o.setScalar(0),o.addScaledVector(zo,r.x),o.addScaledVector(ko,r.y),o.addScaledVector(Ho,r.z),o}static isFrontFacing(t,e,n,s){return rn.subVectors(n,e),Sn.subVectors(t,e),rn.cross(Sn).dot(s)<0}set(t,e,n){return this.a.copy(t),this.b.copy(e),this.c.copy(n),this}setFromPointsAndIndices(t,e,n,s){return this.a.copy(t[e]),this.b.copy(t[n]),this.c.copy(t[s]),this}setFromAttributeAndIndices(t,e,n,s){return this.a.fromBufferAttribute(t,e),this.b.fromBufferAttribute(t,n),this.c.fromBufferAttribute(t,s),this}clone(){return new this.constructor().copy(this)}copy(t){return this.a.copy(t.a),this.b.copy(t.b),this.c.copy(t.c),this}getArea(){return rn.subVectors(this.c,this.b),Sn.subVectors(this.a,this.b),rn.cross(Sn).length()*.5}getMidpoint(t){return t.addVectors(this.a,this.b).add(this.c).multiplyScalar(1/3)}getNormal(t){return i.getNormal(this.a,this.b,this.c,t)}getPlane(t){return t.setFromCoplanarPoints(this.a,this.b,this.c)}getBarycoord(t,e){return i.getBarycoord(t,this.a,this.b,this.c,e)}getInterpolation(t,e,n,s,r){return i.getInterpolation(t,this.a,this.b,this.c,e,n,s,r)}containsPoint(t){return i.containsPoint(t,this.a,this.b,this.c)}isFrontFacing(t){return i.isFrontFacing(this.a,this.b,this.c,t)}intersectsBox(t){return t.intersectsTriangle(this)}closestPointToPoint(t,e){let n=this.a,s=this.b,r=this.c,o,a;Di.subVectors(s,n),Ui.subVectors(r,n),No.subVectors(t,n);let c=Di.dot(No),u=Ui.dot(No);if(c<=0&&u<=0)return e.copy(n);Oo.subVectors(t,s);let f=Di.dot(Oo),p=Ui.dot(Oo);if(f>=0&&p<=f)return e.copy(s);let h=c*p-f*u;if(h<=0&&c>=0&&f<=0)return o=c/(c-f),e.copy(n).addScaledVector(Di,o);Bo.subVectors(t,r);let d=Di.dot(Bo),g=Ui.dot(Bo);if(g>=0&&d<=g)return e.copy(r);let _=d*u-c*g;if(_<=0&&u>=0&&g<=0)return a=u/(u-g),e.copy(n).addScaledVector(Ui,a);let m=f*g-d*p;if(m<=0&&p-f>=0&&d-g>=0)return Ul.subVectors(r,s),a=(p-f)/(p-f+(d-g)),e.copy(s).addScaledVector(Ul,a);let l=1/(m+_+h);return o=_*l,a=h*l,e.copy(n).addScaledVector(Di,o).addScaledVector(Ui,a)}equals(t){return t.a.equals(this.a)&&t.b.equals(this.b)&&t.c.equals(this.c)}},Dh={aliceblue:15792383,antiquewhite:16444375,aqua:65535,aquamarine:8388564,azure:15794175,beige:16119260,bisque:16770244,black:0,blanchedalmond:16772045,blue:255,blueviolet:9055202,brown:10824234,burlywood:14596231,cadetblue:6266528,chartreuse:8388352,chocolate:13789470,coral:16744272,cornflowerblue:6591981,cornsilk:16775388,crimson:14423100,cyan:65535,darkblue:139,darkcyan:35723,darkgoldenrod:12092939,darkgray:11119017,darkgreen:25600,darkgrey:11119017,darkkhaki:12433259,darkmagenta:9109643,darkolivegreen:5597999,darkorange:16747520,darkorchid:10040012,darkred:9109504,darksalmon:15308410,darkseagreen:9419919,darkslateblue:4734347,darkslategray:3100495,darkslategrey:3100495,darkturquoise:52945,darkviolet:9699539,deeppink:16716947,deepskyblue:49151,dimgray:6908265,dimgrey:6908265,dodgerblue:2003199,firebrick:11674146,floralwhite:16775920,forestgreen:2263842,fuchsia:16711935,gainsboro:14474460,ghostwhite:16316671,gold:16766720,goldenrod:14329120,gray:8421504,green:32768,greenyellow:11403055,grey:8421504,honeydew:15794160,hotpink:16738740,indianred:13458524,indigo:4915330,ivory:16777200,khaki:15787660,lavender:15132410,lavenderblush:16773365,lawngreen:8190976,lemonchiffon:16775885,lightblue:11393254,lightcoral:15761536,lightcyan:14745599,lightgoldenrodyellow:16448210,lightgray:13882323,lightgreen:9498256,lightgrey:13882323,lightpink:16758465,lightsalmon:16752762,lightseagreen:2142890,lightskyblue:8900346,lightslategray:7833753,lightslategrey:7833753,lightsteelblue:11584734,lightyellow:16777184,lime:65280,limegreen:3329330,linen:16445670,magenta:16711935,maroon:8388608,mediumaquamarine:6737322,mediumblue:205,mediumorchid:12211667,mediumpurple:9662683,mediumseagreen:3978097,mediumslateblue:8087790,mediumspringgreen:64154,mediumturquoise:4772300,mediumvioletred:13047173,midnightblue:1644912,mintcream:16121850,mistyrose:16770273,moccasin:16770229,navajowhite:16768685,navy:128,oldlace:16643558,olive:8421376,olivedrab:7048739,orange:16753920,orangered:16729344,orchid:14315734,palegoldenrod:15657130,palegreen:10025880,paleturquoise:11529966,palevioletred:14381203,papayawhip:16773077,peachpuff:16767673,peru:13468991,pink:16761035,plum:14524637,powderblue:11591910,purple:8388736,rebeccapurple:6697881,red:16711680,rosybrown:12357519,royalblue:4286945,saddlebrown:9127187,salmon:16416882,sandybrown:16032864,seagreen:3050327,seashell:16774638,sienna:10506797,silver:12632256,skyblue:8900331,slateblue:6970061,slategray:7372944,slategrey:7372944,snow:16775930,springgreen:65407,steelblue:4620980,tan:13808780,teal:32896,thistle:14204888,tomato:16737095,turquoise:4251856,violet:15631086,wheat:16113331,white:16777215,whitesmoke:16119285,yellow:16776960,yellowgreen:10145074},Gn={h:0,s:0,l:0},tr={h:0,s:0,l:0};function Vo(i,t,e){return e<0&&(e+=1),e>1&&(e-=1),e<1/6?i+(t-i)*6*e:e<1/2?t:e<2/3?i+(t-i)*6*(2/3-e):i}var Bt=class{constructor(t,e,n){return this.isColor=!0,this.r=1,this.g=1,this.b=1,this.set(t,e,n)}set(t,e,n){if(e===void 0&&n===void 0){let s=t;s&&s.isColor?this.copy(s):typeof s=="number"?this.setHex(s):typeof s=="string"&&this.setStyle(s)}else this.setRGB(t,e,n);return this}setScalar(t){return this.r=t,this.g=t,this.b=t,this}setHex(t,e=De){return t=Math.floor(t),this.r=(t>>16&255)/255,this.g=(t>>8&255)/255,this.b=(t&255)/255,qt.toWorkingColorSpace(this,e),this}setRGB(t,e,n,s=qt.workingColorSpace){return this.r=t,this.g=e,this.b=n,qt.toWorkingColorSpace(this,s),this}setHSL(t,e,n,s=qt.workingColorSpace){if(t=_d(t,1),e=we(e,0,1),n=we(n,0,1),e===0)this.r=this.g=this.b=n;else{let r=n<=.5?n*(1+e):n+e-n*e,o=2*n-r;this.r=Vo(o,r,t+1/3),this.g=Vo(o,r,t),this.b=Vo(o,r,t-1/3)}return qt.toWorkingColorSpace(this,s),this}setStyle(t,e=De){function n(r){r!==void 0&&parseFloat(r)<1&&console.warn("THREE.Color: Alpha component of "+t+" will be ignored.")}let s;if(s=/^(\w+)\(([^\)]*)\)/.exec(t)){let r,o=s[1],a=s[2];switch(o){case"rgb":case"rgba":if(r=/^\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*(\d*\.?\d+)\s*)?$/.exec(a))return n(r[4]),this.setRGB(Math.min(255,parseInt(r[1],10))/255,Math.min(255,parseInt(r[2],10))/255,Math.min(255,parseInt(r[3],10))/255,e);if(r=/^\s*(\d+)\%\s*,\s*(\d+)\%\s*,\s*(\d+)\%\s*(?:,\s*(\d*\.?\d+)\s*)?$/.exec(a))return n(r[4]),this.setRGB(Math.min(100,parseInt(r[1],10))/100,Math.min(100,parseInt(r[2],10))/100,Math.min(100,parseInt(r[3],10))/100,e);break;case"hsl":case"hsla":if(r=/^\s*(\d*\.?\d+)\s*,\s*(\d*\.?\d+)\%\s*,\s*(\d*\.?\d+)\%\s*(?:,\s*(\d*\.?\d+)\s*)?$/.exec(a))return n(r[4]),this.setHSL(parseFloat(r[1])/360,parseFloat(r[2])/100,parseFloat(r[3])/100,e);break;default:console.warn("THREE.Color: Unknown color model "+t)}}else if(s=/^\#([A-Fa-f\d]+)$/.exec(t)){let r=s[1],o=r.length;if(o===3)return this.setRGB(parseInt(r.charAt(0),16)/15,parseInt(r.charAt(1),16)/15,parseInt(r.charAt(2),16)/15,e);if(o===6)return this.setHex(parseInt(r,16),e);console.warn("THREE.Color: Invalid hex color "+t)}else if(t&&t.length>0)return this.setColorName(t,e);return this}setColorName(t,e=De){let n=Dh[t.toLowerCase()];return n!==void 0?this.setHex(n,e):console.warn("THREE.Color: Unknown color "+t),this}clone(){return new this.constructor(this.r,this.g,this.b)}copy(t){return this.r=t.r,this.g=t.g,this.b=t.b,this}copySRGBToLinear(t){return this.r=Rn(t.r),this.g=Rn(t.g),this.b=Rn(t.b),this}copyLinearToSRGB(t){return this.r=qi(t.r),this.g=qi(t.g),this.b=qi(t.b),this}convertSRGBToLinear(){return this.copySRGBToLinear(this),this}convertLinearToSRGB(){return this.copyLinearToSRGB(this),this}getHex(t=De){return qt.fromWorkingColorSpace(Ce.copy(this),t),Math.round(we(Ce.r*255,0,255))*65536+Math.round(we(Ce.g*255,0,255))*256+Math.round(we(Ce.b*255,0,255))}getHexString(t=De){return("000000"+this.getHex(t).toString(16)).slice(-6)}getHSL(t,e=qt.workingColorSpace){qt.fromWorkingColorSpace(Ce.copy(this),e);let n=Ce.r,s=Ce.g,r=Ce.b,o=Math.max(n,s,r),a=Math.min(n,s,r),c,u,f=(a+o)/2;if(a===o)c=0,u=0;else{let p=o-a;switch(u=f<=.5?p/(o+a):p/(2-o-a),o){case n:c=(s-r)/p+(s<r?6:0);break;case s:c=(r-n)/p+2;break;case r:c=(n-s)/p+4;break}c/=6}return t.h=c,t.s=u,t.l=f,t}getRGB(t,e=qt.workingColorSpace){return qt.fromWorkingColorSpace(Ce.copy(this),e),t.r=Ce.r,t.g=Ce.g,t.b=Ce.b,t}getStyle(t=De){qt.fromWorkingColorSpace(Ce.copy(this),t);let e=Ce.r,n=Ce.g,s=Ce.b;return t!==De?`color(${t} ${e.toFixed(3)} ${n.toFixed(3)} ${s.toFixed(3)})`:`rgb(${Math.round(e*255)},${Math.round(n*255)},${Math.round(s*255)})`}offsetHSL(t,e,n){return this.getHSL(Gn),this.setHSL(Gn.h+t,Gn.s+e,Gn.l+n)}add(t){return this.r+=t.r,this.g+=t.g,this.b+=t.b,this}addColors(t,e){return this.r=t.r+e.r,this.g=t.g+e.g,this.b=t.b+e.b,this}addScalar(t){return this.r+=t,this.g+=t,this.b+=t,this}sub(t){return this.r=Math.max(0,this.r-t.r),this.g=Math.max(0,this.g-t.g),this.b=Math.max(0,this.b-t.b),this}multiply(t){return this.r*=t.r,this.g*=t.g,this.b*=t.b,this}multiplyScalar(t){return this.r*=t,this.g*=t,this.b*=t,this}lerp(t,e){return this.r+=(t.r-this.r)*e,this.g+=(t.g-this.g)*e,this.b+=(t.b-this.b)*e,this}lerpColors(t,e,n){return this.r=t.r+(e.r-t.r)*n,this.g=t.g+(e.g-t.g)*n,this.b=t.b+(e.b-t.b)*n,this}lerpHSL(t,e){this.getHSL(Gn),t.getHSL(tr);let n=wo(Gn.h,tr.h,e),s=wo(Gn.s,tr.s,e),r=wo(Gn.l,tr.l,e);return this.setHSL(n,s,r),this}setFromVector3(t){return this.r=t.x,this.g=t.y,this.b=t.z,this}applyMatrix3(t){let e=this.r,n=this.g,s=this.b,r=t.elements;return this.r=r[0]*e+r[3]*n+r[6]*s,this.g=r[1]*e+r[4]*n+r[7]*s,this.b=r[2]*e+r[5]*n+r[8]*s,this}equals(t){return t.r===this.r&&t.g===this.g&&t.b===this.b}fromArray(t,e=0){return this.r=t[e],this.g=t[e+1],this.b=t[e+2],this}toArray(t=[],e=0){return t[e]=this.r,t[e+1]=this.g,t[e+2]=this.b,t}fromBufferAttribute(t,e){return this.r=t.getX(e),this.g=t.getY(e),this.b=t.getZ(e),this}toJSON(){return this.getHex()}*[Symbol.iterator](){yield this.r,yield this.g,yield this.b}},Ce=new Bt;Bt.NAMES=Dh;var Pd=0,Ln=class extends Kn{static get type(){return"Material"}get type(){return this.constructor.type}set type(t){}constructor(){super(),this.isMaterial=!0,Object.defineProperty(this,"id",{value:Pd++}),this.uuid=$n(),this.name="",this.blending=Wi,this.side=Jn,this.vertexColors=!1,this.opacity=1,this.transparent=!1,this.alphaHash=!1,this.blendSrc=ia,this.blendDst=sa,this.blendEquation=ci,this.blendSrcAlpha=null,this.blendDstAlpha=null,this.blendEquationAlpha=null,this.blendColor=new Bt(0,0,0),this.blendAlpha=0,this.depthFunc=Zi,this.depthTest=!0,this.depthWrite=!0,this.stencilWriteMask=255,this.stencilFunc=xl,this.stencilRef=0,this.stencilFuncMask=255,this.stencilFail=wi,this.stencilZFail=wi,this.stencilZPass=wi,this.stencilWrite=!1,this.clippingPlanes=null,this.clipIntersection=!1,this.clipShadows=!1,this.shadowSide=null,this.colorWrite=!0,this.precision=null,this.polygonOffset=!1,this.polygonOffsetFactor=0,this.polygonOffsetUnits=0,this.dithering=!1,this.alphaToCoverage=!1,this.premultipliedAlpha=!1,this.forceSinglePass=!1,this.visible=!0,this.toneMapped=!0,this.userData={},this.version=0,this._alphaTest=0}get alphaTest(){return this._alphaTest}set alphaTest(t){this._alphaTest>0!=t>0&&this.version++,this._alphaTest=t}onBeforeRender(){}onBeforeCompile(){}customProgramCacheKey(){return this.onBeforeCompile.toString()}setValues(t){if(t!==void 0)for(let e in t){let n=t[e];if(n===void 0){console.warn(`THREE.Material: parameter '${e}' has value of undefined.`);continue}let s=this[e];if(s===void 0){console.warn(`THREE.Material: '${e}' is not a property of THREE.${this.type}.`);continue}s&&s.isColor?s.set(n):s&&s.isVector3&&n&&n.isVector3?s.copy(n):this[e]=n}}toJSON(t){let e=t===void 0||typeof t=="string";e&&(t={textures:{},images:{}});let n={metadata:{version:4.6,type:"Material",generator:"Material.toJSON"}};n.uuid=this.uuid,n.type=this.type,this.name!==""&&(n.name=this.name),this.color&&this.color.isColor&&(n.color=this.color.getHex()),this.roughness!==void 0&&(n.roughness=this.roughness),this.metalness!==void 0&&(n.metalness=this.metalness),this.sheen!==void 0&&(n.sheen=this.sheen),this.sheenColor&&this.sheenColor.isColor&&(n.sheenColor=this.sheenColor.getHex()),this.sheenRoughness!==void 0&&(n.sheenRoughness=this.sheenRoughness),this.emissive&&this.emissive.isColor&&(n.emissive=this.emissive.getHex()),this.emissiveIntensity!==void 0&&this.emissiveIntensity!==1&&(n.emissiveIntensity=this.emissiveIntensity),this.specular&&this.specular.isColor&&(n.specular=this.specular.getHex()),this.specularIntensity!==void 0&&(n.specularIntensity=this.specularIntensity),this.specularColor&&this.specularColor.isColor&&(n.specularColor=this.specularColor.getHex()),this.shininess!==void 0&&(n.shininess=this.shininess),this.clearcoat!==void 0&&(n.clearcoat=this.clearcoat),this.clearcoatRoughness!==void 0&&(n.clearcoatRoughness=this.clearcoatRoughness),this.clearcoatMap&&this.clearcoatMap.isTexture&&(n.clearcoatMap=this.clearcoatMap.toJSON(t).uuid),this.clearcoatRoughnessMap&&this.clearcoatRoughnessMap.isTexture&&(n.clearcoatRoughnessMap=this.clearcoatRoughnessMap.toJSON(t).uuid),this.clearcoatNormalMap&&this.clearcoatNormalMap.isTexture&&(n.clearcoatNormalMap=this.clearcoatNormalMap.toJSON(t).uuid,n.clearcoatNormalScale=this.clearcoatNormalScale.toArray()),this.dispersion!==void 0&&(n.dispersion=this.dispersion),this.iridescence!==void 0&&(n.iridescence=this.iridescence),this.iridescenceIOR!==void 0&&(n.iridescenceIOR=this.iridescenceIOR),this.iridescenceThicknessRange!==void 0&&(n.iridescenceThicknessRange=this.iridescenceThicknessRange),this.iridescenceMap&&this.iridescenceMap.isTexture&&(n.iridescenceMap=this.iridescenceMap.toJSON(t).uuid),this.iridescenceThicknessMap&&this.iridescenceThicknessMap.isTexture&&(n.iridescenceThicknessMap=this.iridescenceThicknessMap.toJSON(t).uuid),this.anisotropy!==void 0&&(n.anisotropy=this.anisotropy),this.anisotropyRotation!==void 0&&(n.anisotropyRotation=this.anisotropyRotation),this.anisotropyMap&&this.anisotropyMap.isTexture&&(n.anisotropyMap=this.anisotropyMap.toJSON(t).uuid),this.map&&this.map.isTexture&&(n.map=this.map.toJSON(t).uuid),this.matcap&&this.matcap.isTexture&&(n.matcap=this.matcap.toJSON(t).uuid),this.alphaMap&&this.alphaMap.isTexture&&(n.alphaMap=this.alphaMap.toJSON(t).uuid),this.lightMap&&this.lightMap.isTexture&&(n.lightMap=this.lightMap.toJSON(t).uuid,n.lightMapIntensity=this.lightMapIntensity),this.aoMap&&this.aoMap.isTexture&&(n.aoMap=this.aoMap.toJSON(t).uuid,n.aoMapIntensity=this.aoMapIntensity),this.bumpMap&&this.bumpMap.isTexture&&(n.bumpMap=this.bumpMap.toJSON(t).uuid,n.bumpScale=this.bumpScale),this.normalMap&&this.normalMap.isTexture&&(n.normalMap=this.normalMap.toJSON(t).uuid,n.normalMapType=this.normalMapType,n.normalScale=this.normalScale.toArray()),this.displacementMap&&this.displacementMap.isTexture&&(n.displacementMap=this.displacementMap.toJSON(t).uuid,n.displacementScale=this.displacementScale,n.displacementBias=this.displacementBias),this.roughnessMap&&this.roughnessMap.isTexture&&(n.roughnessMap=this.roughnessMap.toJSON(t).uuid),this.metalnessMap&&this.metalnessMap.isTexture&&(n.metalnessMap=this.metalnessMap.toJSON(t).uuid),this.emissiveMap&&this.emissiveMap.isTexture&&(n.emissiveMap=this.emissiveMap.toJSON(t).uuid),this.specularMap&&this.specularMap.isTexture&&(n.specularMap=this.specularMap.toJSON(t).uuid),this.specularIntensityMap&&this.specularIntensityMap.isTexture&&(n.specularIntensityMap=this.specularIntensityMap.toJSON(t).uuid),this.specularColorMap&&this.specularColorMap.isTexture&&(n.specularColorMap=this.specularColorMap.toJSON(t).uuid),this.envMap&&this.envMap.isTexture&&(n.envMap=this.envMap.toJSON(t).uuid,this.combine!==void 0&&(n.combine=this.combine)),this.envMapRotation!==void 0&&(n.envMapRotation=this.envMapRotation.toArray()),this.envMapIntensity!==void 0&&(n.envMapIntensity=this.envMapIntensity),this.reflectivity!==void 0&&(n.reflectivity=this.reflectivity),this.refractionRatio!==void 0&&(n.refractionRatio=this.refractionRatio),this.gradientMap&&this.gradientMap.isTexture&&(n.gradientMap=this.gradientMap.toJSON(t).uuid),this.transmission!==void 0&&(n.transmission=this.transmission),this.transmissionMap&&this.transmissionMap.isTexture&&(n.transmissionMap=this.transmissionMap.toJSON(t).uuid),this.thickness!==void 0&&(n.thickness=this.thickness),this.thicknessMap&&this.thicknessMap.isTexture&&(n.thicknessMap=this.thicknessMap.toJSON(t).uuid),this.attenuationDistance!==void 0&&this.attenuationDistance!==1/0&&(n.attenuationDistance=this.attenuationDistance),this.attenuationColor!==void 0&&(n.attenuationColor=this.attenuationColor.getHex()),this.size!==void 0&&(n.size=this.size),this.shadowSide!==null&&(n.shadowSide=this.shadowSide),this.sizeAttenuation!==void 0&&(n.sizeAttenuation=this.sizeAttenuation),this.blending!==Wi&&(n.blending=this.blending),this.side!==Jn&&(n.side=this.side),this.vertexColors===!0&&(n.vertexColors=!0),this.opacity<1&&(n.opacity=this.opacity),this.transparent===!0&&(n.transparent=!0),this.blendSrc!==ia&&(n.blendSrc=this.blendSrc),this.blendDst!==sa&&(n.blendDst=this.blendDst),this.blendEquation!==ci&&(n.blendEquation=this.blendEquation),this.blendSrcAlpha!==null&&(n.blendSrcAlpha=this.blendSrcAlpha),this.blendDstAlpha!==null&&(n.blendDstAlpha=this.blendDstAlpha),this.blendEquationAlpha!==null&&(n.blendEquationAlpha=this.blendEquationAlpha),this.blendColor&&this.blendColor.isColor&&(n.blendColor=this.blendColor.getHex()),this.blendAlpha!==0&&(n.blendAlpha=this.blendAlpha),this.depthFunc!==Zi&&(n.depthFunc=this.depthFunc),this.depthTest===!1&&(n.depthTest=this.depthTest),this.depthWrite===!1&&(n.depthWrite=this.depthWrite),this.colorWrite===!1&&(n.colorWrite=this.colorWrite),this.stencilWriteMask!==255&&(n.stencilWriteMask=this.stencilWriteMask),this.stencilFunc!==xl&&(n.stencilFunc=this.stencilFunc),this.stencilRef!==0&&(n.stencilRef=this.stencilRef),this.stencilFuncMask!==255&&(n.stencilFuncMask=this.stencilFuncMask),this.stencilFail!==wi&&(n.stencilFail=this.stencilFail),this.stencilZFail!==wi&&(n.stencilZFail=this.stencilZFail),this.stencilZPass!==wi&&(n.stencilZPass=this.stencilZPass),this.stencilWrite===!0&&(n.stencilWrite=this.stencilWrite),this.rotation!==void 0&&this.rotation!==0&&(n.rotation=this.rotation),this.polygonOffset===!0&&(n.polygonOffset=!0),this.polygonOffsetFactor!==0&&(n.polygonOffsetFactor=this.polygonOffsetFactor),this.polygonOffsetUnits!==0&&(n.polygonOffsetUnits=this.polygonOffsetUnits),this.linewidth!==void 0&&this.linewidth!==1&&(n.linewidth=this.linewidth),this.dashSize!==void 0&&(n.dashSize=this.dashSize),this.gapSize!==void 0&&(n.gapSize=this.gapSize),this.scale!==void 0&&(n.scale=this.scale),this.dithering===!0&&(n.dithering=!0),this.alphaTest>0&&(n.alphaTest=this.alphaTest),this.alphaHash===!0&&(n.alphaHash=!0),this.alphaToCoverage===!0&&(n.alphaToCoverage=!0),this.premultipliedAlpha===!0&&(n.premultipliedAlpha=!0),this.forceSinglePass===!0&&(n.forceSinglePass=!0),this.wireframe===!0&&(n.wireframe=!0),this.wireframeLinewidth>1&&(n.wireframeLinewidth=this.wireframeLinewidth),this.wireframeLinecap!=="round"&&(n.wireframeLinecap=this.wireframeLinecap),this.wireframeLinejoin!=="round"&&(n.wireframeLinejoin=this.wireframeLinejoin),this.flatShading===!0&&(n.flatShading=!0),this.visible===!1&&(n.visible=!1),this.toneMapped===!1&&(n.toneMapped=!1),this.fog===!1&&(n.fog=!1),Object.keys(this.userData).length>0&&(n.userData=this.userData);function s(r){let o=[];for(let a in r){let c=r[a];delete c.metadata,o.push(c)}return o}if(e){let r=s(t.textures),o=s(t.images);r.length>0&&(n.textures=r),o.length>0&&(n.images=o)}return n}clone(){return new this.constructor().copy(this)}copy(t){this.name=t.name,this.blending=t.blending,this.side=t.side,this.vertexColors=t.vertexColors,this.opacity=t.opacity,this.transparent=t.transparent,this.blendSrc=t.blendSrc,this.blendDst=t.blendDst,this.blendEquation=t.blendEquation,this.blendSrcAlpha=t.blendSrcAlpha,this.blendDstAlpha=t.blendDstAlpha,this.blendEquationAlpha=t.blendEquationAlpha,this.blendColor.copy(t.blendColor),this.blendAlpha=t.blendAlpha,this.depthFunc=t.depthFunc,this.depthTest=t.depthTest,this.depthWrite=t.depthWrite,this.stencilWriteMask=t.stencilWriteMask,this.stencilFunc=t.stencilFunc,this.stencilRef=t.stencilRef,this.stencilFuncMask=t.stencilFuncMask,this.stencilFail=t.stencilFail,this.stencilZFail=t.stencilZFail,this.stencilZPass=t.stencilZPass,this.stencilWrite=t.stencilWrite;let e=t.clippingPlanes,n=null;if(e!==null){let s=e.length;n=new Array(s);for(let r=0;r!==s;++r)n[r]=e[r].clone()}return this.clippingPlanes=n,this.clipIntersection=t.clipIntersection,this.clipShadows=t.clipShadows,this.shadowSide=t.shadowSide,this.colorWrite=t.colorWrite,this.precision=t.precision,this.polygonOffset=t.polygonOffset,this.polygonOffsetFactor=t.polygonOffsetFactor,this.polygonOffsetUnits=t.polygonOffsetUnits,this.dithering=t.dithering,this.alphaTest=t.alphaTest,this.alphaHash=t.alphaHash,this.alphaToCoverage=t.alphaToCoverage,this.premultipliedAlpha=t.premultipliedAlpha,this.forceSinglePass=t.forceSinglePass,this.visible=t.visible,this.toneMapped=t.toneMapped,this.userData=JSON.parse(JSON.stringify(t.userData)),this}dispose(){this.dispatchEvent({type:"dispose"})}set needsUpdate(t){t===!0&&this.version++}onBuild(){console.warn("Material: onBuild() has been removed.")}},Je=class extends Ln{static get type(){return"MeshBasicMaterial"}constructor(t){super(),this.isMeshBasicMaterial=!0,this.color=new Bt(16777215),this.map=null,this.lightMap=null,this.lightMapIntensity=1,this.aoMap=null,this.aoMapIntensity=1,this.specularMap=null,this.alphaMap=null,this.envMap=null,this.envMapRotation=new mn,this.combine=Rc,this.reflectivity=1,this.refractionRatio=.98,this.wireframe=!1,this.wireframeLinewidth=1,this.wireframeLinecap="round",this.wireframeLinejoin="round",this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.lightMap=t.lightMap,this.lightMapIntensity=t.lightMapIntensity,this.aoMap=t.aoMap,this.aoMapIntensity=t.aoMapIntensity,this.specularMap=t.specularMap,this.alphaMap=t.alphaMap,this.envMap=t.envMap,this.envMapRotation.copy(t.envMapRotation),this.combine=t.combine,this.reflectivity=t.reflectivity,this.refractionRatio=t.refractionRatio,this.wireframe=t.wireframe,this.wireframeLinewidth=t.wireframeLinewidth,this.wireframeLinecap=t.wireframeLinecap,this.wireframeLinejoin=t.wireframeLinejoin,this.fog=t.fog,this}};var _e=new P,er=new ut,Ie=class{constructor(t,e,n=!1){if(Array.isArray(t))throw new TypeError("THREE.BufferAttribute: array should be a Typed Array.");this.isBufferAttribute=!0,this.name="",this.array=t,this.itemSize=e,this.count=t!==void 0?t.length/e:0,this.normalized=n,this.usage=Va,this.updateRanges=[],this.gpuType=Tn,this.version=0}onUploadCallback(){}set needsUpdate(t){t===!0&&this.version++}setUsage(t){return this.usage=t,this}addUpdateRange(t,e){this.updateRanges.push({start:t,count:e})}clearUpdateRanges(){this.updateRanges.length=0}copy(t){return this.name=t.name,this.array=new t.array.constructor(t.array),this.itemSize=t.itemSize,this.count=t.count,this.normalized=t.normalized,this.usage=t.usage,this.gpuType=t.gpuType,this}copyAt(t,e,n){t*=this.itemSize,n*=e.itemSize;for(let s=0,r=this.itemSize;s<r;s++)this.array[t+s]=e.array[n+s];return this}copyArray(t){return this.array.set(t),this}applyMatrix3(t){if(this.itemSize===2)for(let e=0,n=this.count;e<n;e++)er.fromBufferAttribute(this,e),er.applyMatrix3(t),this.setXY(e,er.x,er.y);else if(this.itemSize===3)for(let e=0,n=this.count;e<n;e++)_e.fromBufferAttribute(this,e),_e.applyMatrix3(t),this.setXYZ(e,_e.x,_e.y,_e.z);return this}applyMatrix4(t){for(let e=0,n=this.count;e<n;e++)_e.fromBufferAttribute(this,e),_e.applyMatrix4(t),this.setXYZ(e,_e.x,_e.y,_e.z);return this}applyNormalMatrix(t){for(let e=0,n=this.count;e<n;e++)_e.fromBufferAttribute(this,e),_e.applyNormalMatrix(t),this.setXYZ(e,_e.x,_e.y,_e.z);return this}transformDirection(t){for(let e=0,n=this.count;e<n;e++)_e.fromBufferAttribute(this,e),_e.transformDirection(t),this.setXYZ(e,_e.x,_e.y,_e.z);return this}set(t,e=0){return this.array.set(t,e),this}getComponent(t,e){let n=this.array[t*this.itemSize+e];return this.normalized&&(n=fn(n,this.array)),n}setComponent(t,e,n){return this.normalized&&(n=te(n,this.array)),this.array[t*this.itemSize+e]=n,this}getX(t){let e=this.array[t*this.itemSize];return this.normalized&&(e=fn(e,this.array)),e}setX(t,e){return this.normalized&&(e=te(e,this.array)),this.array[t*this.itemSize]=e,this}getY(t){let e=this.array[t*this.itemSize+1];return this.normalized&&(e=fn(e,this.array)),e}setY(t,e){return this.normalized&&(e=te(e,this.array)),this.array[t*this.itemSize+1]=e,this}getZ(t){let e=this.array[t*this.itemSize+2];return this.normalized&&(e=fn(e,this.array)),e}setZ(t,e){return this.normalized&&(e=te(e,this.array)),this.array[t*this.itemSize+2]=e,this}getW(t){let e=this.array[t*this.itemSize+3];return this.normalized&&(e=fn(e,this.array)),e}setW(t,e){return this.normalized&&(e=te(e,this.array)),this.array[t*this.itemSize+3]=e,this}setXY(t,e,n){return t*=this.itemSize,this.normalized&&(e=te(e,this.array),n=te(n,this.array)),this.array[t+0]=e,this.array[t+1]=n,this}setXYZ(t,e,n,s){return t*=this.itemSize,this.normalized&&(e=te(e,this.array),n=te(n,this.array),s=te(s,this.array)),this.array[t+0]=e,this.array[t+1]=n,this.array[t+2]=s,this}setXYZW(t,e,n,s,r){return t*=this.itemSize,this.normalized&&(e=te(e,this.array),n=te(n,this.array),s=te(s,this.array),r=te(r,this.array)),this.array[t+0]=e,this.array[t+1]=n,this.array[t+2]=s,this.array[t+3]=r,this}onUpload(t){return this.onUploadCallback=t,this}clone(){return new this.constructor(this.array,this.itemSize).copy(this)}toJSON(){let t={itemSize:this.itemSize,type:this.array.constructor.name,array:Array.from(this.array),normalized:this.normalized};return this.name!==""&&(t.name=this.name),this.usage!==Va&&(t.usage=this.usage),t}};var Lr=class extends Ie{constructor(t,e,n){super(new Uint16Array(t),e,n)}};var Dr=class extends Ie{constructor(t,e,n){super(new Uint32Array(t),e,n)}};var Jt=class extends Ie{constructor(t,e,n){super(new Float32Array(t),e,n)}},Ld=0,$e=new he,Go=new Te,Fi=new P,We=new fi,_s=new fi,Se=new P,Ee=class i extends Kn{constructor(){super(),this.isBufferGeometry=!0,Object.defineProperty(this,"id",{value:Ld++}),this.uuid=$n(),this.name="",this.type="BufferGeometry",this.index=null,this.indirect=null,this.attributes={},this.morphAttributes={},this.morphTargetsRelative=!1,this.groups=[],this.boundingBox=null,this.boundingSphere=null,this.drawRange={start:0,count:1/0},this.userData={}}getIndex(){return this.index}setIndex(t){return Array.isArray(t)?this.index=new(Lh(t)?Dr:Lr)(t,1):this.index=t,this}setIndirect(t){return this.indirect=t,this}getIndirect(){return this.indirect}getAttribute(t){return this.attributes[t]}setAttribute(t,e){return this.attributes[t]=e,this}deleteAttribute(t){return delete this.attributes[t],this}hasAttribute(t){return this.attributes[t]!==void 0}addGroup(t,e,n=0){this.groups.push({start:t,count:e,materialIndex:n})}clearGroups(){this.groups=[]}setDrawRange(t,e){this.drawRange.start=t,this.drawRange.count=e}applyMatrix4(t){let e=this.attributes.position;e!==void 0&&(e.applyMatrix4(t),e.needsUpdate=!0);let n=this.attributes.normal;if(n!==void 0){let r=new Ft().getNormalMatrix(t);n.applyNormalMatrix(r),n.needsUpdate=!0}let s=this.attributes.tangent;return s!==void 0&&(s.transformDirection(t),s.needsUpdate=!0),this.boundingBox!==null&&this.computeBoundingBox(),this.boundingSphere!==null&&this.computeBoundingSphere(),this}applyQuaternion(t){return $e.makeRotationFromQuaternion(t),this.applyMatrix4($e),this}rotateX(t){return $e.makeRotationX(t),this.applyMatrix4($e),this}rotateY(t){return $e.makeRotationY(t),this.applyMatrix4($e),this}rotateZ(t){return $e.makeRotationZ(t),this.applyMatrix4($e),this}translate(t,e,n){return $e.makeTranslation(t,e,n),this.applyMatrix4($e),this}scale(t,e,n){return $e.makeScale(t,e,n),this.applyMatrix4($e),this}lookAt(t){return Go.lookAt(t),Go.updateMatrix(),this.applyMatrix4(Go.matrix),this}center(){return this.computeBoundingBox(),this.boundingBox.getCenter(Fi).negate(),this.translate(Fi.x,Fi.y,Fi.z),this}setFromPoints(t){let e=this.getAttribute("position");if(e===void 0){let n=[];for(let s=0,r=t.length;s<r;s++){let o=t[s];n.push(o.x,o.y,o.z||0)}this.setAttribute("position",new Jt(n,3))}else{for(let n=0,s=e.count;n<s;n++){let r=t[n];e.setXYZ(n,r.x,r.y,r.z||0)}t.length>e.count&&console.warn("THREE.BufferGeometry: Buffer size too small for points data. Use .dispose() and create a new geometry."),e.needsUpdate=!0}return this}computeBoundingBox(){this.boundingBox===null&&(this.boundingBox=new fi);let t=this.attributes.position,e=this.morphAttributes.position;if(t&&t.isGLBufferAttribute){console.error("THREE.BufferGeometry.computeBoundingBox(): GLBufferAttribute requires a manual bounding box.",this),this.boundingBox.set(new P(-1/0,-1/0,-1/0),new P(1/0,1/0,1/0));return}if(t!==void 0){if(this.boundingBox.setFromBufferAttribute(t),e)for(let n=0,s=e.length;n<s;n++){let r=e[n];We.setFromBufferAttribute(r),this.morphTargetsRelative?(Se.addVectors(this.boundingBox.min,We.min),this.boundingBox.expandByPoint(Se),Se.addVectors(this.boundingBox.max,We.max),this.boundingBox.expandByPoint(Se)):(this.boundingBox.expandByPoint(We.min),this.boundingBox.expandByPoint(We.max))}}else this.boundingBox.makeEmpty();(isNaN(this.boundingBox.min.x)||isNaN(this.boundingBox.min.y)||isNaN(this.boundingBox.min.z))&&console.error('THREE.BufferGeometry.computeBoundingBox(): Computed min/max have NaN values. The "position" attribute is likely to have NaN values.',this)}computeBoundingSphere(){this.boundingSphere===null&&(this.boundingSphere=new Qi);let t=this.attributes.position,e=this.morphAttributes.position;if(t&&t.isGLBufferAttribute){console.error("THREE.BufferGeometry.computeBoundingSphere(): GLBufferAttribute requires a manual bounding sphere.",this),this.boundingSphere.set(new P,1/0);return}if(t){let n=this.boundingSphere.center;if(We.setFromBufferAttribute(t),e)for(let r=0,o=e.length;r<o;r++){let a=e[r];_s.setFromBufferAttribute(a),this.morphTargetsRelative?(Se.addVectors(We.min,_s.min),We.expandByPoint(Se),Se.addVectors(We.max,_s.max),We.expandByPoint(Se)):(We.expandByPoint(_s.min),We.expandByPoint(_s.max))}We.getCenter(n);let s=0;for(let r=0,o=t.count;r<o;r++)Se.fromBufferAttribute(t,r),s=Math.max(s,n.distanceToSquared(Se));if(e)for(let r=0,o=e.length;r<o;r++){let a=e[r],c=this.morphTargetsRelative;for(let u=0,f=a.count;u<f;u++)Se.fromBufferAttribute(a,u),c&&(Fi.fromBufferAttribute(t,u),Se.add(Fi)),s=Math.max(s,n.distanceToSquared(Se))}this.boundingSphere.radius=Math.sqrt(s),isNaN(this.boundingSphere.radius)&&console.error('THREE.BufferGeometry.computeBoundingSphere(): Computed radius is NaN. The "position" attribute is likely to have NaN values.',this)}}computeTangents(){let t=this.index,e=this.attributes;if(t===null||e.position===void 0||e.normal===void 0||e.uv===void 0){console.error("THREE.BufferGeometry: .computeTangents() failed. Missing required attributes (index, position, normal or uv)");return}let n=e.position,s=e.normal,r=e.uv;this.hasAttribute("tangent")===!1&&this.setAttribute("tangent",new Ie(new Float32Array(4*n.count),4));let o=this.getAttribute("tangent"),a=[],c=[];for(let A=0;A<n.count;A++)a[A]=new P,c[A]=new P;let u=new P,f=new P,p=new P,h=new ut,d=new ut,g=new ut,_=new P,m=new P;function l(A,E,v){u.fromBufferAttribute(n,A),f.fromBufferAttribute(n,E),p.fromBufferAttribute(n,v),h.fromBufferAttribute(r,A),d.fromBufferAttribute(r,E),g.fromBufferAttribute(r,v),f.sub(u),p.sub(u),d.sub(h),g.sub(h);let C=1/(d.x*g.y-g.x*d.y);isFinite(C)&&(_.copy(f).multiplyScalar(g.y).addScaledVector(p,-d.y).multiplyScalar(C),m.copy(p).multiplyScalar(d.x).addScaledVector(f,-g.x).multiplyScalar(C),a[A].add(_),a[E].add(_),a[v].add(_),c[A].add(m),c[E].add(m),c[v].add(m))}let S=this.groups;S.length===0&&(S=[{start:0,count:t.count}]);for(let A=0,E=S.length;A<E;++A){let v=S[A],C=v.start,H=v.count;for(let k=C,W=C+H;k<W;k+=3)l(t.getX(k+0),t.getX(k+1),t.getX(k+2))}let b=new P,y=new P,I=new P,T=new P;function R(A){I.fromBufferAttribute(s,A),T.copy(I);let E=a[A];b.copy(E),b.sub(I.multiplyScalar(I.dot(E))).normalize(),y.crossVectors(T,E);let C=y.dot(c[A])<0?-1:1;o.setXYZW(A,b.x,b.y,b.z,C)}for(let A=0,E=S.length;A<E;++A){let v=S[A],C=v.start,H=v.count;for(let k=C,W=C+H;k<W;k+=3)R(t.getX(k+0)),R(t.getX(k+1)),R(t.getX(k+2))}}computeVertexNormals(){let t=this.index,e=this.getAttribute("position");if(e!==void 0){let n=this.getAttribute("normal");if(n===void 0)n=new Ie(new Float32Array(e.count*3),3),this.setAttribute("normal",n);else for(let h=0,d=n.count;h<d;h++)n.setXYZ(h,0,0,0);let s=new P,r=new P,o=new P,a=new P,c=new P,u=new P,f=new P,p=new P;if(t)for(let h=0,d=t.count;h<d;h+=3){let g=t.getX(h+0),_=t.getX(h+1),m=t.getX(h+2);s.fromBufferAttribute(e,g),r.fromBufferAttribute(e,_),o.fromBufferAttribute(e,m),f.subVectors(o,r),p.subVectors(s,r),f.cross(p),a.fromBufferAttribute(n,g),c.fromBufferAttribute(n,_),u.fromBufferAttribute(n,m),a.add(f),c.add(f),u.add(f),n.setXYZ(g,a.x,a.y,a.z),n.setXYZ(_,c.x,c.y,c.z),n.setXYZ(m,u.x,u.y,u.z)}else for(let h=0,d=e.count;h<d;h+=3)s.fromBufferAttribute(e,h+0),r.fromBufferAttribute(e,h+1),o.fromBufferAttribute(e,h+2),f.subVectors(o,r),p.subVectors(s,r),f.cross(p),n.setXYZ(h+0,f.x,f.y,f.z),n.setXYZ(h+1,f.x,f.y,f.z),n.setXYZ(h+2,f.x,f.y,f.z);this.normalizeNormals(),n.needsUpdate=!0}}normalizeNormals(){let t=this.attributes.normal;for(let e=0,n=t.count;e<n;e++)Se.fromBufferAttribute(t,e),Se.normalize(),t.setXYZ(e,Se.x,Se.y,Se.z)}toNonIndexed(){function t(a,c){let u=a.array,f=a.itemSize,p=a.normalized,h=new u.constructor(c.length*f),d=0,g=0;for(let _=0,m=c.length;_<m;_++){a.isInterleavedBufferAttribute?d=c[_]*a.data.stride+a.offset:d=c[_]*f;for(let l=0;l<f;l++)h[g++]=u[d++]}return new Ie(h,f,p)}if(this.index===null)return console.warn("THREE.BufferGeometry.toNonIndexed(): BufferGeometry is already non-indexed."),this;let e=new i,n=this.index.array,s=this.attributes;for(let a in s){let c=s[a],u=t(c,n);e.setAttribute(a,u)}let r=this.morphAttributes;for(let a in r){let c=[],u=r[a];for(let f=0,p=u.length;f<p;f++){let h=u[f],d=t(h,n);c.push(d)}e.morphAttributes[a]=c}e.morphTargetsRelative=this.morphTargetsRelative;let o=this.groups;for(let a=0,c=o.length;a<c;a++){let u=o[a];e.addGroup(u.start,u.count,u.materialIndex)}return e}toJSON(){let t={metadata:{version:4.6,type:"BufferGeometry",generator:"BufferGeometry.toJSON"}};if(t.uuid=this.uuid,t.type=this.type,this.name!==""&&(t.name=this.name),Object.keys(this.userData).length>0&&(t.userData=this.userData),this.parameters!==void 0){let c=this.parameters;for(let u in c)c[u]!==void 0&&(t[u]=c[u]);return t}t.data={attributes:{}};let e=this.index;e!==null&&(t.data.index={type:e.array.constructor.name,array:Array.prototype.slice.call(e.array)});let n=this.attributes;for(let c in n){let u=n[c];t.data.attributes[c]=u.toJSON(t.data)}let s={},r=!1;for(let c in this.morphAttributes){let u=this.morphAttributes[c],f=[];for(let p=0,h=u.length;p<h;p++){let d=u[p];f.push(d.toJSON(t.data))}f.length>0&&(s[c]=f,r=!0)}r&&(t.data.morphAttributes=s,t.data.morphTargetsRelative=this.morphTargetsRelative);let o=this.groups;o.length>0&&(t.data.groups=JSON.parse(JSON.stringify(o)));let a=this.boundingSphere;return a!==null&&(t.data.boundingSphere={center:a.center.toArray(),radius:a.radius}),t}clone(){return new this.constructor().copy(this)}copy(t){this.index=null,this.attributes={},this.morphAttributes={},this.groups=[],this.boundingBox=null,this.boundingSphere=null;let e={};this.name=t.name;let n=t.index;n!==null&&this.setIndex(n.clone(e));let s=t.attributes;for(let u in s){let f=s[u];this.setAttribute(u,f.clone(e))}let r=t.morphAttributes;for(let u in r){let f=[],p=r[u];for(let h=0,d=p.length;h<d;h++)f.push(p[h].clone(e));this.morphAttributes[u]=f}this.morphTargetsRelative=t.morphTargetsRelative;let o=t.groups;for(let u=0,f=o.length;u<f;u++){let p=o[u];this.addGroup(p.start,p.count,p.materialIndex)}let a=t.boundingBox;a!==null&&(this.boundingBox=a.clone());let c=t.boundingSphere;return c!==null&&(this.boundingSphere=c.clone()),this.drawRange.start=t.drawRange.start,this.drawRange.count=t.drawRange.count,this.userData=t.userData,this}dispose(){this.dispatchEvent({type:"dispose"})}},Fl=new he,ii=new Ir,nr=new Qi,Nl=new P,ir=new P,sr=new P,rr=new P,Wo=new P,or=new P,Ol=new P,ar=new P,_t=class extends Te{constructor(t=new Ee,e=new Je){super(),this.isMesh=!0,this.type="Mesh",this.geometry=t,this.material=e,this.updateMorphTargets()}copy(t,e){return super.copy(t,e),t.morphTargetInfluences!==void 0&&(this.morphTargetInfluences=t.morphTargetInfluences.slice()),t.morphTargetDictionary!==void 0&&(this.morphTargetDictionary=Object.assign({},t.morphTargetDictionary)),this.material=Array.isArray(t.material)?t.material.slice():t.material,this.geometry=t.geometry,this}updateMorphTargets(){let e=this.geometry.morphAttributes,n=Object.keys(e);if(n.length>0){let s=e[n[0]];if(s!==void 0){this.morphTargetInfluences=[],this.morphTargetDictionary={};for(let r=0,o=s.length;r<o;r++){let a=s[r].name||String(r);this.morphTargetInfluences.push(0),this.morphTargetDictionary[a]=r}}}}getVertexPosition(t,e){let n=this.geometry,s=n.attributes.position,r=n.morphAttributes.position,o=n.morphTargetsRelative;e.fromBufferAttribute(s,t);let a=this.morphTargetInfluences;if(r&&a){or.set(0,0,0);for(let c=0,u=r.length;c<u;c++){let f=a[c],p=r[c];f!==0&&(Wo.fromBufferAttribute(p,t),o?or.addScaledVector(Wo,f):or.addScaledVector(Wo.sub(e),f))}e.add(or)}return e}raycast(t,e){let n=this.geometry,s=this.material,r=this.matrixWorld;s!==void 0&&(n.boundingSphere===null&&n.computeBoundingSphere(),nr.copy(n.boundingSphere),nr.applyMatrix4(r),ii.copy(t.ray).recast(t.near),!(nr.containsPoint(ii.origin)===!1&&(ii.intersectSphere(nr,Nl)===null||ii.origin.distanceToSquared(Nl)>(t.far-t.near)**2))&&(Fl.copy(r).invert(),ii.copy(t.ray).applyMatrix4(Fl),!(n.boundingBox!==null&&ii.intersectsBox(n.boundingBox)===!1)&&this._computeIntersections(t,e,ii)))}_computeIntersections(t,e,n){let s,r=this.geometry,o=this.material,a=r.index,c=r.attributes.position,u=r.attributes.uv,f=r.attributes.uv1,p=r.attributes.normal,h=r.groups,d=r.drawRange;if(a!==null)if(Array.isArray(o))for(let g=0,_=h.length;g<_;g++){let m=h[g],l=o[m.materialIndex],S=Math.max(m.start,d.start),b=Math.min(a.count,Math.min(m.start+m.count,d.start+d.count));for(let y=S,I=b;y<I;y+=3){let T=a.getX(y),R=a.getX(y+1),A=a.getX(y+2);s=cr(this,l,t,n,u,f,p,T,R,A),s&&(s.faceIndex=Math.floor(y/3),s.face.materialIndex=m.materialIndex,e.push(s))}}else{let g=Math.max(0,d.start),_=Math.min(a.count,d.start+d.count);for(let m=g,l=_;m<l;m+=3){let S=a.getX(m),b=a.getX(m+1),y=a.getX(m+2);s=cr(this,o,t,n,u,f,p,S,b,y),s&&(s.faceIndex=Math.floor(m/3),e.push(s))}}else if(c!==void 0)if(Array.isArray(o))for(let g=0,_=h.length;g<_;g++){let m=h[g],l=o[m.materialIndex],S=Math.max(m.start,d.start),b=Math.min(c.count,Math.min(m.start+m.count,d.start+d.count));for(let y=S,I=b;y<I;y+=3){let T=y,R=y+1,A=y+2;s=cr(this,l,t,n,u,f,p,T,R,A),s&&(s.faceIndex=Math.floor(y/3),s.face.materialIndex=m.materialIndex,e.push(s))}}else{let g=Math.max(0,d.start),_=Math.min(c.count,d.start+d.count);for(let m=g,l=_;m<l;m+=3){let S=m,b=m+1,y=m+2;s=cr(this,o,t,n,u,f,p,S,b,y),s&&(s.faceIndex=Math.floor(m/3),e.push(s))}}}};function Dd(i,t,e,n,s,r,o,a){let c;if(t.side===ke?c=n.intersectTriangle(o,r,s,!0,a):c=n.intersectTriangle(s,r,o,t.side===Jn,a),c===null)return null;ar.copy(a),ar.applyMatrix4(i.matrixWorld);let u=e.ray.origin.distanceTo(ar);return u<e.near||u>e.far?null:{distance:u,point:ar.clone(),object:i}}function cr(i,t,e,n,s,r,o,a,c,u){i.getVertexPosition(a,ir),i.getVertexPosition(c,sr),i.getVertexPosition(u,rr);let f=Dd(i,t,e,n,ir,sr,rr,Ol);if(f){let p=new P;qn.getBarycoord(Ol,ir,sr,rr,p),s&&(f.uv=qn.getInterpolatedAttribute(s,a,c,u,p,new ut)),r&&(f.uv1=qn.getInterpolatedAttribute(r,a,c,u,p,new ut)),o&&(f.normal=qn.getInterpolatedAttribute(o,a,c,u,p,new P),f.normal.dot(n.direction)>0&&f.normal.multiplyScalar(-1));let h={a,b:c,c:u,normal:new P,materialIndex:0};qn.getNormal(ir,sr,rr,h.normal),f.face=h,f.barycoord=p}return f}var He=class i extends Ee{constructor(t=1,e=1,n=1,s=1,r=1,o=1){super(),this.type="BoxGeometry",this.parameters={width:t,height:e,depth:n,widthSegments:s,heightSegments:r,depthSegments:o};let a=this;s=Math.floor(s),r=Math.floor(r),o=Math.floor(o);let c=[],u=[],f=[],p=[],h=0,d=0;g("z","y","x",-1,-1,n,e,t,o,r,0),g("z","y","x",1,-1,n,e,-t,o,r,1),g("x","z","y",1,1,t,n,e,s,o,2),g("x","z","y",1,-1,t,n,-e,s,o,3),g("x","y","z",1,-1,t,e,n,s,r,4),g("x","y","z",-1,-1,t,e,-n,s,r,5),this.setIndex(c),this.setAttribute("position",new Jt(u,3)),this.setAttribute("normal",new Jt(f,3)),this.setAttribute("uv",new Jt(p,2));function g(_,m,l,S,b,y,I,T,R,A,E){let v=y/R,C=I/A,H=y/2,k=I/2,W=T/2,J=R+1,X=A+1,tt=0,G=0,st=new P;for(let dt=0;dt<X;dt++){let bt=dt*C-k;for(let zt=0;zt<J;zt++){let ee=zt*v-H;st[_]=ee*S,st[m]=bt*b,st[l]=W,u.push(st.x,st.y,st.z),st[_]=0,st[m]=0,st[l]=T>0?1:-1,f.push(st.x,st.y,st.z),p.push(zt/R),p.push(1-dt/A),tt+=1}}for(let dt=0;dt<A;dt++)for(let bt=0;bt<R;bt++){let zt=h+bt+J*dt,ee=h+bt+J*(dt+1),Y=h+(bt+1)+J*(dt+1),et=h+(bt+1)+J*dt;c.push(zt,ee,et),c.push(ee,Y,et),G+=6}a.addGroup(d,G,E),d+=G,h+=tt}}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.width,t.height,t.depth,t.widthSegments,t.heightSegments,t.depthSegments)}};function ts(i){let t={};for(let e in i){t[e]={};for(let n in i[e]){let s=i[e][n];s&&(s.isColor||s.isMatrix3||s.isMatrix4||s.isVector2||s.isVector3||s.isVector4||s.isTexture||s.isQuaternion)?s.isRenderTargetTexture?(console.warn("UniformsUtils: Textures of render targets cannot be cloned via cloneUniforms() or mergeUniforms()."),t[e][n]=null):t[e][n]=s.clone():Array.isArray(s)?t[e][n]=s.slice():t[e][n]=s}}return t}function Le(i){let t={};for(let e=0;e<i.length;e++){let n=ts(i[e]);for(let s in n)t[s]=n[s]}return t}function Ud(i){let t=[];for(let e=0;e<i.length;e++)t.push(i[e].clone());return t}function Uh(i){let t=i.getRenderTarget();return t===null?i.outputColorSpace:t.isXRRenderTarget===!0?t.texture.colorSpace:qt.workingColorSpace}var Fd={clone:ts,merge:Le},Nd=`void main() {
	gl_Position = projectionMatrix * modelViewMatrix * vec4( position, 1.0 );
}`,Od=`void main() {
	gl_FragColor = vec4( 1.0, 0.0, 0.0, 1.0 );
}`,gn=class extends Ln{static get type(){return"ShaderMaterial"}constructor(t){super(),this.isShaderMaterial=!0,this.defines={},this.uniforms={},this.uniformsGroups=[],this.vertexShader=Nd,this.fragmentShader=Od,this.linewidth=1,this.wireframe=!1,this.wireframeLinewidth=1,this.fog=!1,this.lights=!1,this.clipping=!1,this.forceSinglePass=!0,this.extensions={clipCullDistance:!1,multiDraw:!1},this.defaultAttributeValues={color:[1,1,1],uv:[0,0],uv1:[0,0]},this.index0AttributeName=void 0,this.uniformsNeedUpdate=!1,this.glslVersion=null,t!==void 0&&this.setValues(t)}copy(t){return super.copy(t),this.fragmentShader=t.fragmentShader,this.vertexShader=t.vertexShader,this.uniforms=ts(t.uniforms),this.uniformsGroups=Ud(t.uniformsGroups),this.defines=Object.assign({},t.defines),this.wireframe=t.wireframe,this.wireframeLinewidth=t.wireframeLinewidth,this.fog=t.fog,this.lights=t.lights,this.clipping=t.clipping,this.extensions=Object.assign({},t.extensions),this.glslVersion=t.glslVersion,this}toJSON(t){let e=super.toJSON(t);e.glslVersion=this.glslVersion,e.uniforms={};for(let s in this.uniforms){let o=this.uniforms[s].value;o&&o.isTexture?e.uniforms[s]={type:"t",value:o.toJSON(t).uuid}:o&&o.isColor?e.uniforms[s]={type:"c",value:o.getHex()}:o&&o.isVector2?e.uniforms[s]={type:"v2",value:o.toArray()}:o&&o.isVector3?e.uniforms[s]={type:"v3",value:o.toArray()}:o&&o.isVector4?e.uniforms[s]={type:"v4",value:o.toArray()}:o&&o.isMatrix3?e.uniforms[s]={type:"m3",value:o.toArray()}:o&&o.isMatrix4?e.uniforms[s]={type:"m4",value:o.toArray()}:e.uniforms[s]={value:o}}Object.keys(this.defines).length>0&&(e.defines=this.defines),e.vertexShader=this.vertexShader,e.fragmentShader=this.fragmentShader,e.lights=this.lights,e.clipping=this.clipping;let n={};for(let s in this.extensions)this.extensions[s]===!0&&(n[s]=!0);return Object.keys(n).length>0&&(e.extensions=n),e}},Ur=class extends Te{constructor(){super(),this.isCamera=!0,this.type="Camera",this.matrixWorldInverse=new he,this.projectionMatrix=new he,this.projectionMatrixInverse=new he,this.coordinateSystem=An}copy(t,e){return super.copy(t,e),this.matrixWorldInverse.copy(t.matrixWorldInverse),this.projectionMatrix.copy(t.projectionMatrix),this.projectionMatrixInverse.copy(t.projectionMatrixInverse),this.coordinateSystem=t.coordinateSystem,this}getWorldDirection(t){return super.getWorldDirection(t).negate()}updateMatrixWorld(t){super.updateMatrixWorld(t),this.matrixWorldInverse.copy(this.matrixWorld).invert()}updateWorldMatrix(t,e){super.updateWorldMatrix(t,e),this.matrixWorldInverse.copy(this.matrixWorld).invert()}clone(){return new this.constructor().copy(this)}},Wn=new P,Bl=new ut,zl=new ut,Ue=class extends Ur{constructor(t=50,e=1,n=.1,s=2e3){super(),this.isPerspectiveCamera=!0,this.type="PerspectiveCamera",this.fov=t,this.zoom=1,this.near=n,this.far=s,this.focus=10,this.aspect=e,this.view=null,this.filmGauge=35,this.filmOffset=0,this.updateProjectionMatrix()}copy(t,e){return super.copy(t,e),this.fov=t.fov,this.zoom=t.zoom,this.near=t.near,this.far=t.far,this.focus=t.focus,this.aspect=t.aspect,this.view=t.view===null?null:Object.assign({},t.view),this.filmGauge=t.filmGauge,this.filmOffset=t.filmOffset,this}setFocalLength(t){let e=.5*this.getFilmHeight()/t;this.fov=Ga*2*Math.atan(e),this.updateProjectionMatrix()}getFocalLength(){let t=Math.tan(bo*.5*this.fov);return .5*this.getFilmHeight()/t}getEffectiveFOV(){return Ga*2*Math.atan(Math.tan(bo*.5*this.fov)/this.zoom)}getFilmWidth(){return this.filmGauge*Math.min(this.aspect,1)}getFilmHeight(){return this.filmGauge/Math.max(this.aspect,1)}getViewBounds(t,e,n){Wn.set(-1,-1,.5).applyMatrix4(this.projectionMatrixInverse),e.set(Wn.x,Wn.y).multiplyScalar(-t/Wn.z),Wn.set(1,1,.5).applyMatrix4(this.projectionMatrixInverse),n.set(Wn.x,Wn.y).multiplyScalar(-t/Wn.z)}getViewSize(t,e){return this.getViewBounds(t,Bl,zl),e.subVectors(zl,Bl)}setViewOffset(t,e,n,s,r,o){this.aspect=t/e,this.view===null&&(this.view={enabled:!0,fullWidth:1,fullHeight:1,offsetX:0,offsetY:0,width:1,height:1}),this.view.enabled=!0,this.view.fullWidth=t,this.view.fullHeight=e,this.view.offsetX=n,this.view.offsetY=s,this.view.width=r,this.view.height=o,this.updateProjectionMatrix()}clearViewOffset(){this.view!==null&&(this.view.enabled=!1),this.updateProjectionMatrix()}updateProjectionMatrix(){let t=this.near,e=t*Math.tan(bo*.5*this.fov)/this.zoom,n=2*e,s=this.aspect*n,r=-.5*s,o=this.view;if(this.view!==null&&this.view.enabled){let c=o.fullWidth,u=o.fullHeight;r+=o.offsetX*s/c,e-=o.offsetY*n/u,s*=o.width/c,n*=o.height/u}let a=this.filmOffset;a!==0&&(r+=t*a/this.getFilmWidth()),this.projectionMatrix.makePerspective(r,r+s,e,e-n,t,this.far,this.coordinateSystem),this.projectionMatrixInverse.copy(this.projectionMatrix).invert()}toJSON(t){let e=super.toJSON(t);return e.object.fov=this.fov,e.object.zoom=this.zoom,e.object.near=this.near,e.object.far=this.far,e.object.focus=this.focus,e.object.aspect=this.aspect,this.view!==null&&(e.object.view=Object.assign({},this.view)),e.object.filmGauge=this.filmGauge,e.object.filmOffset=this.filmOffset,e}},Ni=-90,Oi=1,Ya=class extends Te{constructor(t,e,n){super(),this.type="CubeCamera",this.renderTarget=n,this.coordinateSystem=null,this.activeMipmapLevel=0;let s=new Ue(Ni,Oi,t,e);s.layers=this.layers,this.add(s);let r=new Ue(Ni,Oi,t,e);r.layers=this.layers,this.add(r);let o=new Ue(Ni,Oi,t,e);o.layers=this.layers,this.add(o);let a=new Ue(Ni,Oi,t,e);a.layers=this.layers,this.add(a);let c=new Ue(Ni,Oi,t,e);c.layers=this.layers,this.add(c);let u=new Ue(Ni,Oi,t,e);u.layers=this.layers,this.add(u)}updateCoordinateSystem(){let t=this.coordinateSystem,e=this.children.concat(),[n,s,r,o,a,c]=e;for(let u of e)this.remove(u);if(t===An)n.up.set(0,1,0),n.lookAt(1,0,0),s.up.set(0,1,0),s.lookAt(-1,0,0),r.up.set(0,0,-1),r.lookAt(0,1,0),o.up.set(0,0,1),o.lookAt(0,-1,0),a.up.set(0,1,0),a.lookAt(0,0,1),c.up.set(0,1,0),c.lookAt(0,0,-1);else if(t===Tr)n.up.set(0,-1,0),n.lookAt(-1,0,0),s.up.set(0,-1,0),s.lookAt(1,0,0),r.up.set(0,0,1),r.lookAt(0,1,0),o.up.set(0,0,-1),o.lookAt(0,-1,0),a.up.set(0,-1,0),a.lookAt(0,0,1),c.up.set(0,-1,0),c.lookAt(0,0,-1);else throw new Error("THREE.CubeCamera.updateCoordinateSystem(): Invalid coordinate system: "+t);for(let u of e)this.add(u),u.updateMatrixWorld()}update(t,e){this.parent===null&&this.updateMatrixWorld();let{renderTarget:n,activeMipmapLevel:s}=this;this.coordinateSystem!==t.coordinateSystem&&(this.coordinateSystem=t.coordinateSystem,this.updateCoordinateSystem());let[r,o,a,c,u,f]=this.children,p=t.getRenderTarget(),h=t.getActiveCubeFace(),d=t.getActiveMipmapLevel(),g=t.xr.enabled;t.xr.enabled=!1;let _=n.texture.generateMipmaps;n.texture.generateMipmaps=!1,t.setRenderTarget(n,0,s),t.render(e,r),t.setRenderTarget(n,1,s),t.render(e,o),t.setRenderTarget(n,2,s),t.render(e,a),t.setRenderTarget(n,3,s),t.render(e,c),t.setRenderTarget(n,4,s),t.render(e,u),n.texture.generateMipmaps=_,t.setRenderTarget(n,5,s),t.render(e,f),t.setRenderTarget(p,h,d),t.xr.enabled=g,n.texture.needsPMREMUpdate=!0}},Fr=class extends Xe{constructor(t,e,n,s,r,o,a,c,u,f){t=t!==void 0?t:[],e=e!==void 0?e:$i,super(t,e,n,s,r,o,a,c,u,f),this.isCubeTexture=!0,this.flipY=!1}get images(){return this.image}set images(t){this.image=t}},Za=class extends Pn{constructor(t=1,e={}){super(t,t,e),this.isWebGLCubeRenderTarget=!0;let n={width:t,height:t,depth:1},s=[n,n,n,n,n,n];this.texture=new Fr(s,e.mapping,e.wrapS,e.wrapT,e.magFilter,e.minFilter,e.format,e.type,e.anisotropy,e.colorSpace),this.texture.isRenderTargetTexture=!0,this.texture.generateMipmaps=e.generateMipmaps!==void 0?e.generateMipmaps:!1,this.texture.minFilter=e.minFilter!==void 0?e.minFilter:pn}fromEquirectangularTexture(t,e){this.texture.type=e.type,this.texture.colorSpace=e.colorSpace,this.texture.generateMipmaps=e.generateMipmaps,this.texture.minFilter=e.minFilter,this.texture.magFilter=e.magFilter;let n={uniforms:{tEquirect:{value:null}},vertexShader:`

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
			`},s=new He(5,5,5),r=new gn({name:"CubemapFromEquirect",uniforms:ts(n.uniforms),vertexShader:n.vertexShader,fragmentShader:n.fragmentShader,side:ke,blending:Yn});r.uniforms.tEquirect.value=e;let o=new _t(s,r),a=e.minFilter;return e.minFilter===ui&&(e.minFilter=pn),new Ya(1,10,this).update(t,o),e.minFilter=a,o.geometry.dispose(),o.material.dispose(),this}clear(t,e,n,s){let r=t.getRenderTarget();for(let o=0;o<6;o++)t.setRenderTarget(this,o),t.clear(e,n,s);t.setRenderTarget(r)}},Xo=new P,Bd=new P,zd=new Ft,wn=class{constructor(t=new P(1,0,0),e=0){this.isPlane=!0,this.normal=t,this.constant=e}set(t,e){return this.normal.copy(t),this.constant=e,this}setComponents(t,e,n,s){return this.normal.set(t,e,n),this.constant=s,this}setFromNormalAndCoplanarPoint(t,e){return this.normal.copy(t),this.constant=-e.dot(this.normal),this}setFromCoplanarPoints(t,e,n){let s=Xo.subVectors(n,e).cross(Bd.subVectors(t,e)).normalize();return this.setFromNormalAndCoplanarPoint(s,t),this}copy(t){return this.normal.copy(t.normal),this.constant=t.constant,this}normalize(){let t=1/this.normal.length();return this.normal.multiplyScalar(t),this.constant*=t,this}negate(){return this.constant*=-1,this.normal.negate(),this}distanceToPoint(t){return this.normal.dot(t)+this.constant}distanceToSphere(t){return this.distanceToPoint(t.center)-t.radius}projectPoint(t,e){return e.copy(t).addScaledVector(this.normal,-this.distanceToPoint(t))}intersectLine(t,e){let n=t.delta(Xo),s=this.normal.dot(n);if(s===0)return this.distanceToPoint(t.start)===0?e.copy(t.start):null;let r=-(t.start.dot(this.normal)+this.constant)/s;return r<0||r>1?null:e.copy(t.start).addScaledVector(n,r)}intersectsLine(t){let e=this.distanceToPoint(t.start),n=this.distanceToPoint(t.end);return e<0&&n>0||n<0&&e>0}intersectsBox(t){return t.intersectsPlane(this)}intersectsSphere(t){return t.intersectsPlane(this)}coplanarPoint(t){return t.copy(this.normal).multiplyScalar(-this.constant)}applyMatrix4(t,e){let n=e||zd.getNormalMatrix(t),s=this.coplanarPoint(Xo).applyMatrix4(t),r=this.normal.applyMatrix3(n).normalize();return this.constant=-s.dot(r),this}translate(t){return this.constant-=t.dot(this.normal),this}equals(t){return t.normal.equals(this.normal)&&t.constant===this.constant}clone(){return new this.constructor().copy(this)}},si=new Qi,lr=new P,As=class{constructor(t=new wn,e=new wn,n=new wn,s=new wn,r=new wn,o=new wn){this.planes=[t,e,n,s,r,o]}set(t,e,n,s,r,o){let a=this.planes;return a[0].copy(t),a[1].copy(e),a[2].copy(n),a[3].copy(s),a[4].copy(r),a[5].copy(o),this}copy(t){let e=this.planes;for(let n=0;n<6;n++)e[n].copy(t.planes[n]);return this}setFromProjectionMatrix(t,e=An){let n=this.planes,s=t.elements,r=s[0],o=s[1],a=s[2],c=s[3],u=s[4],f=s[5],p=s[6],h=s[7],d=s[8],g=s[9],_=s[10],m=s[11],l=s[12],S=s[13],b=s[14],y=s[15];if(n[0].setComponents(c-r,h-u,m-d,y-l).normalize(),n[1].setComponents(c+r,h+u,m+d,y+l).normalize(),n[2].setComponents(c+o,h+f,m+g,y+S).normalize(),n[3].setComponents(c-o,h-f,m-g,y-S).normalize(),n[4].setComponents(c-a,h-p,m-_,y-b).normalize(),e===An)n[5].setComponents(c+a,h+p,m+_,y+b).normalize();else if(e===Tr)n[5].setComponents(a,p,_,b).normalize();else throw new Error("THREE.Frustum.setFromProjectionMatrix(): Invalid coordinate system: "+e);return this}intersectsObject(t){if(t.boundingSphere!==void 0)t.boundingSphere===null&&t.computeBoundingSphere(),si.copy(t.boundingSphere).applyMatrix4(t.matrixWorld);else{let e=t.geometry;e.boundingSphere===null&&e.computeBoundingSphere(),si.copy(e.boundingSphere).applyMatrix4(t.matrixWorld)}return this.intersectsSphere(si)}intersectsSprite(t){return si.center.set(0,0,0),si.radius=.7071067811865476,si.applyMatrix4(t.matrixWorld),this.intersectsSphere(si)}intersectsSphere(t){let e=this.planes,n=t.center,s=-t.radius;for(let r=0;r<6;r++)if(e[r].distanceToPoint(n)<s)return!1;return!0}intersectsBox(t){let e=this.planes;for(let n=0;n<6;n++){let s=e[n];if(lr.x=s.normal.x>0?t.max.x:t.min.x,lr.y=s.normal.y>0?t.max.y:t.min.y,lr.z=s.normal.z>0?t.max.z:t.min.z,s.distanceToPoint(lr)<0)return!1}return!0}containsPoint(t){let e=this.planes;for(let n=0;n<6;n++)if(e[n].distanceToPoint(t)<0)return!1;return!0}clone(){return new this.constructor().copy(this)}};function Fh(){let i=null,t=!1,e=null,n=null;function s(r,o){e(r,o),n=i.requestAnimationFrame(s)}return{start:function(){t!==!0&&e!==null&&(n=i.requestAnimationFrame(s),t=!0)},stop:function(){i.cancelAnimationFrame(n),t=!1},setAnimationLoop:function(r){e=r},setContext:function(r){i=r}}}function kd(i){let t=new WeakMap;function e(a,c){let u=a.array,f=a.usage,p=u.byteLength,h=i.createBuffer();i.bindBuffer(c,h),i.bufferData(c,u,f),a.onUploadCallback();let d;if(u instanceof Float32Array)d=i.FLOAT;else if(u instanceof Uint16Array)a.isFloat16BufferAttribute?d=i.HALF_FLOAT:d=i.UNSIGNED_SHORT;else if(u instanceof Int16Array)d=i.SHORT;else if(u instanceof Uint32Array)d=i.UNSIGNED_INT;else if(u instanceof Int32Array)d=i.INT;else if(u instanceof Int8Array)d=i.BYTE;else if(u instanceof Uint8Array)d=i.UNSIGNED_BYTE;else if(u instanceof Uint8ClampedArray)d=i.UNSIGNED_BYTE;else throw new Error("THREE.WebGLAttributes: Unsupported buffer data format: "+u);return{buffer:h,type:d,bytesPerElement:u.BYTES_PER_ELEMENT,version:a.version,size:p}}function n(a,c,u){let f=c.array,p=c.updateRanges;if(i.bindBuffer(u,a),p.length===0)i.bufferSubData(u,0,f);else{p.sort((d,g)=>d.start-g.start);let h=0;for(let d=1;d<p.length;d++){let g=p[h],_=p[d];_.start<=g.start+g.count+1?g.count=Math.max(g.count,_.start+_.count-g.start):(++h,p[h]=_)}p.length=h+1;for(let d=0,g=p.length;d<g;d++){let _=p[d];i.bufferSubData(u,_.start*f.BYTES_PER_ELEMENT,f,_.start,_.count)}c.clearUpdateRanges()}c.onUploadCallback()}function s(a){return a.isInterleavedBufferAttribute&&(a=a.data),t.get(a)}function r(a){a.isInterleavedBufferAttribute&&(a=a.data);let c=t.get(a);c&&(i.deleteBuffer(c.buffer),t.delete(a))}function o(a,c){if(a.isInterleavedBufferAttribute&&(a=a.data),a.isGLBufferAttribute){let f=t.get(a);(!f||f.version<a.version)&&t.set(a,{buffer:a.buffer,type:a.type,bytesPerElement:a.elementSize,version:a.version});return}let u=t.get(a);if(u===void 0)t.set(a,e(a,c));else if(u.version<a.version){if(u.size!==a.array.byteLength)throw new Error("THREE.WebGLAttributes: The size of the buffer attribute's array buffer does not match the original size. Resizing buffer attributes is not supported.");n(u.buffer,a,c),u.version=a.version}}return{get:s,remove:r,update:o}}var Qn=class i extends Ee{constructor(t=1,e=1,n=1,s=1){super(),this.type="PlaneGeometry",this.parameters={width:t,height:e,widthSegments:n,heightSegments:s};let r=t/2,o=e/2,a=Math.floor(n),c=Math.floor(s),u=a+1,f=c+1,p=t/a,h=e/c,d=[],g=[],_=[],m=[];for(let l=0;l<f;l++){let S=l*h-o;for(let b=0;b<u;b++){let y=b*p-r;g.push(y,-S,0),_.push(0,0,1),m.push(b/a),m.push(1-l/c)}}for(let l=0;l<c;l++)for(let S=0;S<a;S++){let b=S+u*l,y=S+u*(l+1),I=S+1+u*(l+1),T=S+1+u*l;d.push(b,y,T),d.push(y,I,T)}this.setIndex(d),this.setAttribute("position",new Jt(g,3)),this.setAttribute("normal",new Jt(_,3)),this.setAttribute("uv",new Jt(m,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.width,t.height,t.widthSegments,t.heightSegments)}},Hd=`#ifdef USE_ALPHAHASH
	if ( diffuseColor.a < getAlphaHashThreshold( vPosition ) ) discard;
#endif`,Vd=`#ifdef USE_ALPHAHASH
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
#endif`,Gd=`#ifdef USE_ALPHAMAP
	diffuseColor.a *= texture2D( alphaMap, vAlphaMapUv ).g;
#endif`,Wd=`#ifdef USE_ALPHAMAP
	uniform sampler2D alphaMap;
#endif`,Xd=`#ifdef USE_ALPHATEST
	#ifdef ALPHA_TO_COVERAGE
	diffuseColor.a = smoothstep( alphaTest, alphaTest + fwidth( diffuseColor.a ), diffuseColor.a );
	if ( diffuseColor.a == 0.0 ) discard;
	#else
	if ( diffuseColor.a < alphaTest ) discard;
	#endif
#endif`,qd=`#ifdef USE_ALPHATEST
	uniform float alphaTest;
#endif`,Yd=`#ifdef USE_AOMAP
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
#endif`,Zd=`#ifdef USE_AOMAP
	uniform sampler2D aoMap;
	uniform float aoMapIntensity;
#endif`,$d=`#ifdef USE_BATCHING
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
#endif`,Jd=`#ifdef USE_BATCHING
	mat4 batchingMatrix = getBatchingMatrix( getIndirectIndex( gl_DrawID ) );
#endif`,Kd=`vec3 transformed = vec3( position );
#ifdef USE_ALPHAHASH
	vPosition = vec3( position );
#endif`,jd=`vec3 objectNormal = vec3( normal );
#ifdef USE_TANGENT
	vec3 objectTangent = vec3( tangent.xyz );
#endif`,Qd=`float G_BlinnPhong_Implicit( ) {
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
} // validated`,tf=`#ifdef USE_IRIDESCENCE
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
#endif`,ef=`#ifdef USE_BUMPMAP
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
#endif`,nf=`#if NUM_CLIPPING_PLANES > 0
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
#endif`,sf=`#if NUM_CLIPPING_PLANES > 0
	varying vec3 vClipPosition;
	uniform vec4 clippingPlanes[ NUM_CLIPPING_PLANES ];
#endif`,rf=`#if NUM_CLIPPING_PLANES > 0
	varying vec3 vClipPosition;
#endif`,of=`#if NUM_CLIPPING_PLANES > 0
	vClipPosition = - mvPosition.xyz;
#endif`,af=`#if defined( USE_COLOR_ALPHA )
	diffuseColor *= vColor;
#elif defined( USE_COLOR )
	diffuseColor.rgb *= vColor;
#endif`,cf=`#if defined( USE_COLOR_ALPHA )
	varying vec4 vColor;
#elif defined( USE_COLOR )
	varying vec3 vColor;
#endif`,lf=`#if defined( USE_COLOR_ALPHA )
	varying vec4 vColor;
#elif defined( USE_COLOR ) || defined( USE_INSTANCING_COLOR ) || defined( USE_BATCHING_COLOR )
	varying vec3 vColor;
#endif`,hf=`#if defined( USE_COLOR_ALPHA )
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
#endif`,uf=`#define PI 3.141592653589793
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
} // validated`,df=`#ifdef ENVMAP_TYPE_CUBE_UV
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
#endif`,ff=`vec3 transformedNormal = objectNormal;
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
#endif`,pf=`#ifdef USE_DISPLACEMENTMAP
	uniform sampler2D displacementMap;
	uniform float displacementScale;
	uniform float displacementBias;
#endif`,mf=`#ifdef USE_DISPLACEMENTMAP
	transformed += normalize( objectNormal ) * ( texture2D( displacementMap, vDisplacementMapUv ).x * displacementScale + displacementBias );
#endif`,gf=`#ifdef USE_EMISSIVEMAP
	vec4 emissiveColor = texture2D( emissiveMap, vEmissiveMapUv );
	#ifdef DECODE_VIDEO_TEXTURE_EMISSIVE
		emissiveColor = sRGBTransferEOTF( emissiveColor );
	#endif
	totalEmissiveRadiance *= emissiveColor.rgb;
#endif`,_f=`#ifdef USE_EMISSIVEMAP
	uniform sampler2D emissiveMap;
#endif`,xf="gl_FragColor = linearToOutputTexel( gl_FragColor );",yf=`vec4 LinearTransferOETF( in vec4 value ) {
	return value;
}
vec4 sRGBTransferEOTF( in vec4 value ) {
	return vec4( mix( pow( value.rgb * 0.9478672986 + vec3( 0.0521327014 ), vec3( 2.4 ) ), value.rgb * 0.0773993808, vec3( lessThanEqual( value.rgb, vec3( 0.04045 ) ) ) ), value.a );
}
vec4 sRGBTransferOETF( in vec4 value ) {
	return vec4( mix( pow( value.rgb, vec3( 0.41666 ) ) * 1.055 - vec3( 0.055 ), value.rgb * 12.92, vec3( lessThanEqual( value.rgb, vec3( 0.0031308 ) ) ) ), value.a );
}`,vf=`#ifdef USE_ENVMAP
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
#endif`,Mf=`#ifdef USE_ENVMAP
	uniform float envMapIntensity;
	uniform float flipEnvMap;
	uniform mat3 envMapRotation;
	#ifdef ENVMAP_TYPE_CUBE
		uniform samplerCube envMap;
	#else
		uniform sampler2D envMap;
	#endif
	
#endif`,Sf=`#ifdef USE_ENVMAP
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
#endif`,Ef=`#ifdef USE_ENVMAP
	#if defined( USE_BUMPMAP ) || defined( USE_NORMALMAP ) || defined( PHONG ) || defined( LAMBERT )
		#define ENV_WORLDPOS
	#endif
	#ifdef ENV_WORLDPOS
		
		varying vec3 vWorldPosition;
	#else
		varying vec3 vReflect;
		uniform float refractionRatio;
	#endif
#endif`,bf=`#ifdef USE_ENVMAP
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
#endif`,wf=`#ifdef USE_FOG
	vFogDepth = - mvPosition.z;
#endif`,Tf=`#ifdef USE_FOG
	varying float vFogDepth;
#endif`,Af=`#ifdef USE_FOG
	#ifdef FOG_EXP2
		float fogFactor = 1.0 - exp( - fogDensity * fogDensity * vFogDepth * vFogDepth );
	#else
		float fogFactor = smoothstep( fogNear, fogFar, vFogDepth );
	#endif
	gl_FragColor.rgb = mix( gl_FragColor.rgb, fogColor, fogFactor );
#endif`,Rf=`#ifdef USE_FOG
	uniform vec3 fogColor;
	varying float vFogDepth;
	#ifdef FOG_EXP2
		uniform float fogDensity;
	#else
		uniform float fogNear;
		uniform float fogFar;
	#endif
#endif`,Cf=`#ifdef USE_GRADIENTMAP
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
}`,If=`#ifdef USE_LIGHTMAP
	uniform sampler2D lightMap;
	uniform float lightMapIntensity;
#endif`,Pf=`LambertMaterial material;
material.diffuseColor = diffuseColor.rgb;
material.specularStrength = specularStrength;`,Lf=`varying vec3 vViewPosition;
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
#define RE_IndirectDiffuse		RE_IndirectDiffuse_Lambert`,Df=`uniform bool receiveShadow;
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
#endif`,Uf=`#ifdef USE_ENVMAP
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
#endif`,Ff=`ToonMaterial material;
material.diffuseColor = diffuseColor.rgb;`,Nf=`varying vec3 vViewPosition;
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
#define RE_IndirectDiffuse		RE_IndirectDiffuse_Toon`,Of=`BlinnPhongMaterial material;
material.diffuseColor = diffuseColor.rgb;
material.specularColor = specular;
material.specularShininess = shininess;
material.specularStrength = specularStrength;`,Bf=`varying vec3 vViewPosition;
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
#define RE_IndirectDiffuse		RE_IndirectDiffuse_BlinnPhong`,zf=`PhysicalMaterial material;
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
#endif`,kf=`struct PhysicalMaterial {
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
}`,Hf=`
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
#endif`,Vf=`#if defined( RE_IndirectDiffuse )
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
#endif`,Gf=`#if defined( RE_IndirectDiffuse )
	RE_IndirectDiffuse( irradiance, geometryPosition, geometryNormal, geometryViewDir, geometryClearcoatNormal, material, reflectedLight );
#endif
#if defined( RE_IndirectSpecular )
	RE_IndirectSpecular( radiance, iblIrradiance, clearcoatRadiance, geometryPosition, geometryNormal, geometryViewDir, geometryClearcoatNormal, material, reflectedLight );
#endif`,Wf=`#if defined( USE_LOGDEPTHBUF )
	gl_FragDepth = vIsPerspective == 0.0 ? gl_FragCoord.z : log2( vFragDepth ) * logDepthBufFC * 0.5;
#endif`,Xf=`#if defined( USE_LOGDEPTHBUF )
	uniform float logDepthBufFC;
	varying float vFragDepth;
	varying float vIsPerspective;
#endif`,qf=`#ifdef USE_LOGDEPTHBUF
	varying float vFragDepth;
	varying float vIsPerspective;
#endif`,Yf=`#ifdef USE_LOGDEPTHBUF
	vFragDepth = 1.0 + gl_Position.w;
	vIsPerspective = float( isPerspectiveMatrix( projectionMatrix ) );
#endif`,Zf=`#ifdef USE_MAP
	vec4 sampledDiffuseColor = texture2D( map, vMapUv );
	#ifdef DECODE_VIDEO_TEXTURE
		sampledDiffuseColor = sRGBTransferEOTF( sampledDiffuseColor );
	#endif
	diffuseColor *= sampledDiffuseColor;
#endif`,$f=`#ifdef USE_MAP
	uniform sampler2D map;
#endif`,Jf=`#if defined( USE_MAP ) || defined( USE_ALPHAMAP )
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
#endif`,Kf=`#if defined( USE_POINTS_UV )
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
#endif`,jf=`float metalnessFactor = metalness;
#ifdef USE_METALNESSMAP
	vec4 texelMetalness = texture2D( metalnessMap, vMetalnessMapUv );
	metalnessFactor *= texelMetalness.b;
#endif`,Qf=`#ifdef USE_METALNESSMAP
	uniform sampler2D metalnessMap;
#endif`,tp=`#ifdef USE_INSTANCING_MORPH
	float morphTargetInfluences[ MORPHTARGETS_COUNT ];
	float morphTargetBaseInfluence = texelFetch( morphTexture, ivec2( 0, gl_InstanceID ), 0 ).r;
	for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
		morphTargetInfluences[i] =  texelFetch( morphTexture, ivec2( i + 1, gl_InstanceID ), 0 ).r;
	}
#endif`,ep=`#if defined( USE_MORPHCOLORS )
	vColor *= morphTargetBaseInfluence;
	for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
		#if defined( USE_COLOR_ALPHA )
			if ( morphTargetInfluences[ i ] != 0.0 ) vColor += getMorph( gl_VertexID, i, 2 ) * morphTargetInfluences[ i ];
		#elif defined( USE_COLOR )
			if ( morphTargetInfluences[ i ] != 0.0 ) vColor += getMorph( gl_VertexID, i, 2 ).rgb * morphTargetInfluences[ i ];
		#endif
	}
#endif`,np=`#ifdef USE_MORPHNORMALS
	objectNormal *= morphTargetBaseInfluence;
	for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
		if ( morphTargetInfluences[ i ] != 0.0 ) objectNormal += getMorph( gl_VertexID, i, 1 ).xyz * morphTargetInfluences[ i ];
	}
#endif`,ip=`#ifdef USE_MORPHTARGETS
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
#endif`,sp=`#ifdef USE_MORPHTARGETS
	transformed *= morphTargetBaseInfluence;
	for ( int i = 0; i < MORPHTARGETS_COUNT; i ++ ) {
		if ( morphTargetInfluences[ i ] != 0.0 ) transformed += getMorph( gl_VertexID, i, 0 ).xyz * morphTargetInfluences[ i ];
	}
#endif`,rp=`float faceDirection = gl_FrontFacing ? 1.0 : - 1.0;
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
vec3 nonPerturbedNormal = normal;`,op=`#ifdef USE_NORMALMAP_OBJECTSPACE
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
#endif`,ap=`#ifndef FLAT_SHADED
	varying vec3 vNormal;
	#ifdef USE_TANGENT
		varying vec3 vTangent;
		varying vec3 vBitangent;
	#endif
#endif`,cp=`#ifndef FLAT_SHADED
	varying vec3 vNormal;
	#ifdef USE_TANGENT
		varying vec3 vTangent;
		varying vec3 vBitangent;
	#endif
#endif`,lp=`#ifndef FLAT_SHADED
	vNormal = normalize( transformedNormal );
	#ifdef USE_TANGENT
		vTangent = normalize( transformedTangent );
		vBitangent = normalize( cross( vNormal, vTangent ) * tangent.w );
	#endif
#endif`,hp=`#ifdef USE_NORMALMAP
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
#endif`,up=`#ifdef USE_CLEARCOAT
	vec3 clearcoatNormal = nonPerturbedNormal;
#endif`,dp=`#ifdef USE_CLEARCOAT_NORMALMAP
	vec3 clearcoatMapN = texture2D( clearcoatNormalMap, vClearcoatNormalMapUv ).xyz * 2.0 - 1.0;
	clearcoatMapN.xy *= clearcoatNormalScale;
	clearcoatNormal = normalize( tbn2 * clearcoatMapN );
#endif`,fp=`#ifdef USE_CLEARCOATMAP
	uniform sampler2D clearcoatMap;
#endif
#ifdef USE_CLEARCOAT_NORMALMAP
	uniform sampler2D clearcoatNormalMap;
	uniform vec2 clearcoatNormalScale;
#endif
#ifdef USE_CLEARCOAT_ROUGHNESSMAP
	uniform sampler2D clearcoatRoughnessMap;
#endif`,pp=`#ifdef USE_IRIDESCENCEMAP
	uniform sampler2D iridescenceMap;
#endif
#ifdef USE_IRIDESCENCE_THICKNESSMAP
	uniform sampler2D iridescenceThicknessMap;
#endif`,mp=`#ifdef OPAQUE
diffuseColor.a = 1.0;
#endif
#ifdef USE_TRANSMISSION
diffuseColor.a *= material.transmissionAlpha;
#endif
gl_FragColor = vec4( outgoingLight, diffuseColor.a );`,gp=`vec3 packNormalToRGB( const in vec3 normal ) {
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
}`,_p=`#ifdef PREMULTIPLIED_ALPHA
	gl_FragColor.rgb *= gl_FragColor.a;
#endif`,xp=`vec4 mvPosition = vec4( transformed, 1.0 );
#ifdef USE_BATCHING
	mvPosition = batchingMatrix * mvPosition;
#endif
#ifdef USE_INSTANCING
	mvPosition = instanceMatrix * mvPosition;
#endif
mvPosition = modelViewMatrix * mvPosition;
gl_Position = projectionMatrix * mvPosition;`,yp=`#ifdef DITHERING
	gl_FragColor.rgb = dithering( gl_FragColor.rgb );
#endif`,vp=`#ifdef DITHERING
	vec3 dithering( vec3 color ) {
		float grid_position = rand( gl_FragCoord.xy );
		vec3 dither_shift_RGB = vec3( 0.25 / 255.0, -0.25 / 255.0, 0.25 / 255.0 );
		dither_shift_RGB = mix( 2.0 * dither_shift_RGB, -2.0 * dither_shift_RGB, grid_position );
		return color + dither_shift_RGB;
	}
#endif`,Mp=`float roughnessFactor = roughness;
#ifdef USE_ROUGHNESSMAP
	vec4 texelRoughness = texture2D( roughnessMap, vRoughnessMapUv );
	roughnessFactor *= texelRoughness.g;
#endif`,Sp=`#ifdef USE_ROUGHNESSMAP
	uniform sampler2D roughnessMap;
#endif`,Ep=`#if NUM_SPOT_LIGHT_COORDS > 0
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
#endif`,bp=`#if NUM_SPOT_LIGHT_COORDS > 0
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
#endif`,wp=`#if ( defined( USE_SHADOWMAP ) && ( NUM_DIR_LIGHT_SHADOWS > 0 || NUM_POINT_LIGHT_SHADOWS > 0 ) ) || ( NUM_SPOT_LIGHT_COORDS > 0 )
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
#endif`,Tp=`float getShadowMask() {
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
}`,Ap=`#ifdef USE_SKINNING
	mat4 boneMatX = getBoneMatrix( skinIndex.x );
	mat4 boneMatY = getBoneMatrix( skinIndex.y );
	mat4 boneMatZ = getBoneMatrix( skinIndex.z );
	mat4 boneMatW = getBoneMatrix( skinIndex.w );
#endif`,Rp=`#ifdef USE_SKINNING
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
#endif`,Cp=`#ifdef USE_SKINNING
	vec4 skinVertex = bindMatrix * vec4( transformed, 1.0 );
	vec4 skinned = vec4( 0.0 );
	skinned += boneMatX * skinVertex * skinWeight.x;
	skinned += boneMatY * skinVertex * skinWeight.y;
	skinned += boneMatZ * skinVertex * skinWeight.z;
	skinned += boneMatW * skinVertex * skinWeight.w;
	transformed = ( bindMatrixInverse * skinned ).xyz;
#endif`,Ip=`#ifdef USE_SKINNING
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
#endif`,Pp=`float specularStrength;
#ifdef USE_SPECULARMAP
	vec4 texelSpecular = texture2D( specularMap, vSpecularMapUv );
	specularStrength = texelSpecular.r;
#else
	specularStrength = 1.0;
#endif`,Lp=`#ifdef USE_SPECULARMAP
	uniform sampler2D specularMap;
#endif`,Dp=`#if defined( TONE_MAPPING )
	gl_FragColor.rgb = toneMapping( gl_FragColor.rgb );
#endif`,Up=`#ifndef saturate
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
vec3 CustomToneMapping( vec3 color ) { return color; }`,Fp=`#ifdef USE_TRANSMISSION
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
#endif`,Np=`#ifdef USE_TRANSMISSION
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
#endif`,Op=`#if defined( USE_UV ) || defined( USE_ANISOTROPY )
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
#endif`,Bp=`#if defined( USE_UV ) || defined( USE_ANISOTROPY )
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
#endif`,zp=`#if defined( USE_UV ) || defined( USE_ANISOTROPY )
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
#endif`,kp=`#if defined( USE_ENVMAP ) || defined( DISTANCE ) || defined ( USE_SHADOWMAP ) || defined ( USE_TRANSMISSION ) || NUM_SPOT_LIGHT_COORDS > 0
	vec4 worldPosition = vec4( transformed, 1.0 );
	#ifdef USE_BATCHING
		worldPosition = batchingMatrix * worldPosition;
	#endif
	#ifdef USE_INSTANCING
		worldPosition = instanceMatrix * worldPosition;
	#endif
	worldPosition = modelMatrix * worldPosition;
#endif`,Hp=`varying vec2 vUv;
uniform mat3 uvTransform;
void main() {
	vUv = ( uvTransform * vec3( uv, 1 ) ).xy;
	gl_Position = vec4( position.xy, 1.0, 1.0 );
}`,Vp=`uniform sampler2D t2D;
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
}`,Gp=`varying vec3 vWorldDirection;
#include <common>
void main() {
	vWorldDirection = transformDirection( position, modelMatrix );
	#include <begin_vertex>
	#include <project_vertex>
	gl_Position.z = gl_Position.w;
}`,Wp=`#ifdef ENVMAP_TYPE_CUBE
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
}`,Xp=`varying vec3 vWorldDirection;
#include <common>
void main() {
	vWorldDirection = transformDirection( position, modelMatrix );
	#include <begin_vertex>
	#include <project_vertex>
	gl_Position.z = gl_Position.w;
}`,qp=`uniform samplerCube tCube;
uniform float tFlip;
uniform float opacity;
varying vec3 vWorldDirection;
void main() {
	vec4 texColor = textureCube( tCube, vec3( tFlip * vWorldDirection.x, vWorldDirection.yz ) );
	gl_FragColor = texColor;
	gl_FragColor.a *= opacity;
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
}`,Yp=`#include <common>
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
}`,Zp=`#if DEPTH_PACKING == 3200
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
}`,$p=`#define DISTANCE
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
}`,Jp=`#define DISTANCE
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
}`,Kp=`varying vec3 vWorldDirection;
#include <common>
void main() {
	vWorldDirection = transformDirection( position, modelMatrix );
	#include <begin_vertex>
	#include <project_vertex>
}`,jp=`uniform sampler2D tEquirect;
varying vec3 vWorldDirection;
#include <common>
void main() {
	vec3 direction = normalize( vWorldDirection );
	vec2 sampleUV = equirectUv( direction );
	gl_FragColor = texture2D( tEquirect, sampleUV );
	#include <tonemapping_fragment>
	#include <colorspace_fragment>
}`,Qp=`uniform float scale;
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
}`,tm=`uniform vec3 diffuse;
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
}`,em=`#include <common>
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
}`,nm=`uniform vec3 diffuse;
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
}`,im=`#define LAMBERT
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
}`,sm=`#define LAMBERT
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
}`,rm=`#define MATCAP
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
}`,om=`#define MATCAP
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
}`,am=`#define NORMAL
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
}`,cm=`#define NORMAL
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
}`,lm=`#define PHONG
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
}`,hm=`#define PHONG
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
}`,um=`#define STANDARD
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
}`,dm=`#define STANDARD
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
}`,fm=`#define TOON
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
}`,pm=`#define TOON
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
}`,mm=`uniform float size;
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
}`,gm=`uniform vec3 diffuse;
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
}`,_m=`#include <common>
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
}`,xm=`uniform vec3 color;
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
}`,ym=`uniform float rotation;
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
}`,vm=`uniform vec3 diffuse;
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
}`,Ot={alphahash_fragment:Hd,alphahash_pars_fragment:Vd,alphamap_fragment:Gd,alphamap_pars_fragment:Wd,alphatest_fragment:Xd,alphatest_pars_fragment:qd,aomap_fragment:Yd,aomap_pars_fragment:Zd,batching_pars_vertex:$d,batching_vertex:Jd,begin_vertex:Kd,beginnormal_vertex:jd,bsdfs:Qd,iridescence_fragment:tf,bumpmap_pars_fragment:ef,clipping_planes_fragment:nf,clipping_planes_pars_fragment:sf,clipping_planes_pars_vertex:rf,clipping_planes_vertex:of,color_fragment:af,color_pars_fragment:cf,color_pars_vertex:lf,color_vertex:hf,common:uf,cube_uv_reflection_fragment:df,defaultnormal_vertex:ff,displacementmap_pars_vertex:pf,displacementmap_vertex:mf,emissivemap_fragment:gf,emissivemap_pars_fragment:_f,colorspace_fragment:xf,colorspace_pars_fragment:yf,envmap_fragment:vf,envmap_common_pars_fragment:Mf,envmap_pars_fragment:Sf,envmap_pars_vertex:Ef,envmap_physical_pars_fragment:Uf,envmap_vertex:bf,fog_vertex:wf,fog_pars_vertex:Tf,fog_fragment:Af,fog_pars_fragment:Rf,gradientmap_pars_fragment:Cf,lightmap_pars_fragment:If,lights_lambert_fragment:Pf,lights_lambert_pars_fragment:Lf,lights_pars_begin:Df,lights_toon_fragment:Ff,lights_toon_pars_fragment:Nf,lights_phong_fragment:Of,lights_phong_pars_fragment:Bf,lights_physical_fragment:zf,lights_physical_pars_fragment:kf,lights_fragment_begin:Hf,lights_fragment_maps:Vf,lights_fragment_end:Gf,logdepthbuf_fragment:Wf,logdepthbuf_pars_fragment:Xf,logdepthbuf_pars_vertex:qf,logdepthbuf_vertex:Yf,map_fragment:Zf,map_pars_fragment:$f,map_particle_fragment:Jf,map_particle_pars_fragment:Kf,metalnessmap_fragment:jf,metalnessmap_pars_fragment:Qf,morphinstance_vertex:tp,morphcolor_vertex:ep,morphnormal_vertex:np,morphtarget_pars_vertex:ip,morphtarget_vertex:sp,normal_fragment_begin:rp,normal_fragment_maps:op,normal_pars_fragment:ap,normal_pars_vertex:cp,normal_vertex:lp,normalmap_pars_fragment:hp,clearcoat_normal_fragment_begin:up,clearcoat_normal_fragment_maps:dp,clearcoat_pars_fragment:fp,iridescence_pars_fragment:pp,opaque_fragment:mp,packing:gp,premultiplied_alpha_fragment:_p,project_vertex:xp,dithering_fragment:yp,dithering_pars_fragment:vp,roughnessmap_fragment:Mp,roughnessmap_pars_fragment:Sp,shadowmap_pars_fragment:Ep,shadowmap_pars_vertex:bp,shadowmap_vertex:wp,shadowmask_pars_fragment:Tp,skinbase_vertex:Ap,skinning_pars_vertex:Rp,skinning_vertex:Cp,skinnormal_vertex:Ip,specularmap_fragment:Pp,specularmap_pars_fragment:Lp,tonemapping_fragment:Dp,tonemapping_pars_fragment:Up,transmission_fragment:Fp,transmission_pars_fragment:Np,uv_pars_fragment:Op,uv_pars_vertex:Bp,uv_vertex:zp,worldpos_vertex:kp,background_vert:Hp,background_frag:Vp,backgroundCube_vert:Gp,backgroundCube_frag:Wp,cube_vert:Xp,cube_frag:qp,depth_vert:Yp,depth_frag:Zp,distanceRGBA_vert:$p,distanceRGBA_frag:Jp,equirect_vert:Kp,equirect_frag:jp,linedashed_vert:Qp,linedashed_frag:tm,meshbasic_vert:em,meshbasic_frag:nm,meshlambert_vert:im,meshlambert_frag:sm,meshmatcap_vert:rm,meshmatcap_frag:om,meshnormal_vert:am,meshnormal_frag:cm,meshphong_vert:lm,meshphong_frag:hm,meshphysical_vert:um,meshphysical_frag:dm,meshtoon_vert:fm,meshtoon_frag:pm,points_vert:mm,points_frag:gm,shadow_vert:_m,shadow_frag:xm,sprite_vert:ym,sprite_frag:vm},nt={common:{diffuse:{value:new Bt(16777215)},opacity:{value:1},map:{value:null},mapTransform:{value:new Ft},alphaMap:{value:null},alphaMapTransform:{value:new Ft},alphaTest:{value:0}},specularmap:{specularMap:{value:null},specularMapTransform:{value:new Ft}},envmap:{envMap:{value:null},envMapRotation:{value:new Ft},flipEnvMap:{value:-1},reflectivity:{value:1},ior:{value:1.5},refractionRatio:{value:.98}},aomap:{aoMap:{value:null},aoMapIntensity:{value:1},aoMapTransform:{value:new Ft}},lightmap:{lightMap:{value:null},lightMapIntensity:{value:1},lightMapTransform:{value:new Ft}},bumpmap:{bumpMap:{value:null},bumpMapTransform:{value:new Ft},bumpScale:{value:1}},normalmap:{normalMap:{value:null},normalMapTransform:{value:new Ft},normalScale:{value:new ut(1,1)}},displacementmap:{displacementMap:{value:null},displacementMapTransform:{value:new Ft},displacementScale:{value:1},displacementBias:{value:0}},emissivemap:{emissiveMap:{value:null},emissiveMapTransform:{value:new Ft}},metalnessmap:{metalnessMap:{value:null},metalnessMapTransform:{value:new Ft}},roughnessmap:{roughnessMap:{value:null},roughnessMapTransform:{value:new Ft}},gradientmap:{gradientMap:{value:null}},fog:{fogDensity:{value:25e-5},fogNear:{value:1},fogFar:{value:2e3},fogColor:{value:new Bt(16777215)}},lights:{ambientLightColor:{value:[]},lightProbe:{value:[]},directionalLights:{value:[],properties:{direction:{},color:{}}},directionalLightShadows:{value:[],properties:{shadowIntensity:1,shadowBias:{},shadowNormalBias:{},shadowRadius:{},shadowMapSize:{}}},directionalShadowMap:{value:[]},directionalShadowMatrix:{value:[]},spotLights:{value:[],properties:{color:{},position:{},direction:{},distance:{},coneCos:{},penumbraCos:{},decay:{}}},spotLightShadows:{value:[],properties:{shadowIntensity:1,shadowBias:{},shadowNormalBias:{},shadowRadius:{},shadowMapSize:{}}},spotLightMap:{value:[]},spotShadowMap:{value:[]},spotLightMatrix:{value:[]},pointLights:{value:[],properties:{color:{},position:{},decay:{},distance:{}}},pointLightShadows:{value:[],properties:{shadowIntensity:1,shadowBias:{},shadowNormalBias:{},shadowRadius:{},shadowMapSize:{},shadowCameraNear:{},shadowCameraFar:{}}},pointShadowMap:{value:[]},pointShadowMatrix:{value:[]},hemisphereLights:{value:[],properties:{direction:{},skyColor:{},groundColor:{}}},rectAreaLights:{value:[],properties:{color:{},position:{},width:{},height:{}}},ltc_1:{value:null},ltc_2:{value:null}},points:{diffuse:{value:new Bt(16777215)},opacity:{value:1},size:{value:1},scale:{value:1},map:{value:null},alphaMap:{value:null},alphaMapTransform:{value:new Ft},alphaTest:{value:0},uvTransform:{value:new Ft}},sprite:{diffuse:{value:new Bt(16777215)},opacity:{value:1},center:{value:new ut(.5,.5)},rotation:{value:0},map:{value:null},mapTransform:{value:new Ft},alphaMap:{value:null},alphaMapTransform:{value:new Ft},alphaTest:{value:0}}},dn={basic:{uniforms:Le([nt.common,nt.specularmap,nt.envmap,nt.aomap,nt.lightmap,nt.fog]),vertexShader:Ot.meshbasic_vert,fragmentShader:Ot.meshbasic_frag},lambert:{uniforms:Le([nt.common,nt.specularmap,nt.envmap,nt.aomap,nt.lightmap,nt.emissivemap,nt.bumpmap,nt.normalmap,nt.displacementmap,nt.fog,nt.lights,{emissive:{value:new Bt(0)}}]),vertexShader:Ot.meshlambert_vert,fragmentShader:Ot.meshlambert_frag},phong:{uniforms:Le([nt.common,nt.specularmap,nt.envmap,nt.aomap,nt.lightmap,nt.emissivemap,nt.bumpmap,nt.normalmap,nt.displacementmap,nt.fog,nt.lights,{emissive:{value:new Bt(0)},specular:{value:new Bt(1118481)},shininess:{value:30}}]),vertexShader:Ot.meshphong_vert,fragmentShader:Ot.meshphong_frag},standard:{uniforms:Le([nt.common,nt.envmap,nt.aomap,nt.lightmap,nt.emissivemap,nt.bumpmap,nt.normalmap,nt.displacementmap,nt.roughnessmap,nt.metalnessmap,nt.fog,nt.lights,{emissive:{value:new Bt(0)},roughness:{value:1},metalness:{value:0},envMapIntensity:{value:1}}]),vertexShader:Ot.meshphysical_vert,fragmentShader:Ot.meshphysical_frag},toon:{uniforms:Le([nt.common,nt.aomap,nt.lightmap,nt.emissivemap,nt.bumpmap,nt.normalmap,nt.displacementmap,nt.gradientmap,nt.fog,nt.lights,{emissive:{value:new Bt(0)}}]),vertexShader:Ot.meshtoon_vert,fragmentShader:Ot.meshtoon_frag},matcap:{uniforms:Le([nt.common,nt.bumpmap,nt.normalmap,nt.displacementmap,nt.fog,{matcap:{value:null}}]),vertexShader:Ot.meshmatcap_vert,fragmentShader:Ot.meshmatcap_frag},points:{uniforms:Le([nt.points,nt.fog]),vertexShader:Ot.points_vert,fragmentShader:Ot.points_frag},dashed:{uniforms:Le([nt.common,nt.fog,{scale:{value:1},dashSize:{value:1},totalSize:{value:2}}]),vertexShader:Ot.linedashed_vert,fragmentShader:Ot.linedashed_frag},depth:{uniforms:Le([nt.common,nt.displacementmap]),vertexShader:Ot.depth_vert,fragmentShader:Ot.depth_frag},normal:{uniforms:Le([nt.common,nt.bumpmap,nt.normalmap,nt.displacementmap,{opacity:{value:1}}]),vertexShader:Ot.meshnormal_vert,fragmentShader:Ot.meshnormal_frag},sprite:{uniforms:Le([nt.sprite,nt.fog]),vertexShader:Ot.sprite_vert,fragmentShader:Ot.sprite_frag},background:{uniforms:{uvTransform:{value:new Ft},t2D:{value:null},backgroundIntensity:{value:1}},vertexShader:Ot.background_vert,fragmentShader:Ot.background_frag},backgroundCube:{uniforms:{envMap:{value:null},flipEnvMap:{value:-1},backgroundBlurriness:{value:0},backgroundIntensity:{value:1},backgroundRotation:{value:new Ft}},vertexShader:Ot.backgroundCube_vert,fragmentShader:Ot.backgroundCube_frag},cube:{uniforms:{tCube:{value:null},tFlip:{value:-1},opacity:{value:1}},vertexShader:Ot.cube_vert,fragmentShader:Ot.cube_frag},equirect:{uniforms:{tEquirect:{value:null}},vertexShader:Ot.equirect_vert,fragmentShader:Ot.equirect_frag},distanceRGBA:{uniforms:Le([nt.common,nt.displacementmap,{referencePosition:{value:new P},nearDistance:{value:1},farDistance:{value:1e3}}]),vertexShader:Ot.distanceRGBA_vert,fragmentShader:Ot.distanceRGBA_frag},shadow:{uniforms:Le([nt.lights,nt.fog,{color:{value:new Bt(0)},opacity:{value:1}}]),vertexShader:Ot.shadow_vert,fragmentShader:Ot.shadow_frag}};dn.physical={uniforms:Le([dn.standard.uniforms,{clearcoat:{value:0},clearcoatMap:{value:null},clearcoatMapTransform:{value:new Ft},clearcoatNormalMap:{value:null},clearcoatNormalMapTransform:{value:new Ft},clearcoatNormalScale:{value:new ut(1,1)},clearcoatRoughness:{value:0},clearcoatRoughnessMap:{value:null},clearcoatRoughnessMapTransform:{value:new Ft},dispersion:{value:0},iridescence:{value:0},iridescenceMap:{value:null},iridescenceMapTransform:{value:new Ft},iridescenceIOR:{value:1.3},iridescenceThicknessMinimum:{value:100},iridescenceThicknessMaximum:{value:400},iridescenceThicknessMap:{value:null},iridescenceThicknessMapTransform:{value:new Ft},sheen:{value:0},sheenColor:{value:new Bt(0)},sheenColorMap:{value:null},sheenColorMapTransform:{value:new Ft},sheenRoughness:{value:1},sheenRoughnessMap:{value:null},sheenRoughnessMapTransform:{value:new Ft},transmission:{value:0},transmissionMap:{value:null},transmissionMapTransform:{value:new Ft},transmissionSamplerSize:{value:new ut},transmissionSamplerMap:{value:null},thickness:{value:0},thicknessMap:{value:null},thicknessMapTransform:{value:new Ft},attenuationDistance:{value:0},attenuationColor:{value:new Bt(0)},specularColor:{value:new Bt(1,1,1)},specularColorMap:{value:null},specularColorMapTransform:{value:new Ft},specularIntensity:{value:1},specularIntensityMap:{value:null},specularIntensityMapTransform:{value:new Ft},anisotropyVector:{value:new ut},anisotropyMap:{value:null},anisotropyMapTransform:{value:new Ft}}]),vertexShader:Ot.meshphysical_vert,fragmentShader:Ot.meshphysical_frag};var hr={r:0,b:0,g:0},ri=new mn,Mm=new he;function Sm(i,t,e,n,s,r,o){let a=new Bt(0),c=r===!0?0:1,u,f,p=null,h=0,d=null;function g(S){let b=S.isScene===!0?S.background:null;return b&&b.isTexture&&(b=(S.backgroundBlurriness>0?e:t).get(b)),b}function _(S){let b=!1,y=g(S);y===null?l(a,c):y&&y.isColor&&(l(y,1),b=!0);let I=i.xr.getEnvironmentBlendMode();I==="additive"?n.buffers.color.setClear(0,0,0,1,o):I==="alpha-blend"&&n.buffers.color.setClear(0,0,0,0,o),(i.autoClear||b)&&(n.buffers.depth.setTest(!0),n.buffers.depth.setMask(!0),n.buffers.color.setMask(!0),i.clear(i.autoClearColor,i.autoClearDepth,i.autoClearStencil))}function m(S,b){let y=g(b);y&&(y.isCubeTexture||y.mapping===eo)?(f===void 0&&(f=new _t(new He(1,1,1),new gn({name:"BackgroundCubeMaterial",uniforms:ts(dn.backgroundCube.uniforms),vertexShader:dn.backgroundCube.vertexShader,fragmentShader:dn.backgroundCube.fragmentShader,side:ke,depthTest:!1,depthWrite:!1,fog:!1})),f.geometry.deleteAttribute("normal"),f.geometry.deleteAttribute("uv"),f.onBeforeRender=function(I,T,R){this.matrixWorld.copyPosition(R.matrixWorld)},Object.defineProperty(f.material,"envMap",{get:function(){return this.uniforms.envMap.value}}),s.update(f)),ri.copy(b.backgroundRotation),ri.x*=-1,ri.y*=-1,ri.z*=-1,y.isCubeTexture&&y.isRenderTargetTexture===!1&&(ri.y*=-1,ri.z*=-1),f.material.uniforms.envMap.value=y,f.material.uniforms.flipEnvMap.value=y.isCubeTexture&&y.isRenderTargetTexture===!1?-1:1,f.material.uniforms.backgroundBlurriness.value=b.backgroundBlurriness,f.material.uniforms.backgroundIntensity.value=b.backgroundIntensity,f.material.uniforms.backgroundRotation.value.setFromMatrix4(Mm.makeRotationFromEuler(ri)),f.material.toneMapped=qt.getTransfer(y.colorSpace)!==Qt,(p!==y||h!==y.version||d!==i.toneMapping)&&(f.material.needsUpdate=!0,p=y,h=y.version,d=i.toneMapping),f.layers.enableAll(),S.unshift(f,f.geometry,f.material,0,0,null)):y&&y.isTexture&&(u===void 0&&(u=new _t(new Qn(2,2),new gn({name:"BackgroundMaterial",uniforms:ts(dn.background.uniforms),vertexShader:dn.background.vertexShader,fragmentShader:dn.background.fragmentShader,side:Jn,depthTest:!1,depthWrite:!1,fog:!1})),u.geometry.deleteAttribute("normal"),Object.defineProperty(u.material,"map",{get:function(){return this.uniforms.t2D.value}}),s.update(u)),u.material.uniforms.t2D.value=y,u.material.uniforms.backgroundIntensity.value=b.backgroundIntensity,u.material.toneMapped=qt.getTransfer(y.colorSpace)!==Qt,y.matrixAutoUpdate===!0&&y.updateMatrix(),u.material.uniforms.uvTransform.value.copy(y.matrix),(p!==y||h!==y.version||d!==i.toneMapping)&&(u.material.needsUpdate=!0,p=y,h=y.version,d=i.toneMapping),u.layers.enableAll(),S.unshift(u,u.geometry,u.material,0,0,null))}function l(S,b){S.getRGB(hr,Uh(i)),n.buffers.color.setClear(hr.r,hr.g,hr.b,b,o)}return{getClearColor:function(){return a},setClearColor:function(S,b=1){a.set(S),c=b,l(a,c)},getClearAlpha:function(){return c},setClearAlpha:function(S){c=S,l(a,c)},render:_,addToRenderList:m}}function Em(i,t){let e=i.getParameter(i.MAX_VERTEX_ATTRIBS),n={},s=h(null),r=s,o=!1;function a(v,C,H,k,W){let J=!1,X=p(k,H,C);r!==X&&(r=X,u(r.object)),J=d(v,k,H,W),J&&g(v,k,H,W),W!==null&&t.update(W,i.ELEMENT_ARRAY_BUFFER),(J||o)&&(o=!1,y(v,C,H,k),W!==null&&i.bindBuffer(i.ELEMENT_ARRAY_BUFFER,t.get(W).buffer))}function c(){return i.createVertexArray()}function u(v){return i.bindVertexArray(v)}function f(v){return i.deleteVertexArray(v)}function p(v,C,H){let k=H.wireframe===!0,W=n[v.id];W===void 0&&(W={},n[v.id]=W);let J=W[C.id];J===void 0&&(J={},W[C.id]=J);let X=J[k];return X===void 0&&(X=h(c()),J[k]=X),X}function h(v){let C=[],H=[],k=[];for(let W=0;W<e;W++)C[W]=0,H[W]=0,k[W]=0;return{geometry:null,program:null,wireframe:!1,newAttributes:C,enabledAttributes:H,attributeDivisors:k,object:v,attributes:{},index:null}}function d(v,C,H,k){let W=r.attributes,J=C.attributes,X=0,tt=H.getAttributes();for(let G in tt)if(tt[G].location>=0){let dt=W[G],bt=J[G];if(bt===void 0&&(G==="instanceMatrix"&&v.instanceMatrix&&(bt=v.instanceMatrix),G==="instanceColor"&&v.instanceColor&&(bt=v.instanceColor)),dt===void 0||dt.attribute!==bt||bt&&dt.data!==bt.data)return!0;X++}return r.attributesNum!==X||r.index!==k}function g(v,C,H,k){let W={},J=C.attributes,X=0,tt=H.getAttributes();for(let G in tt)if(tt[G].location>=0){let dt=J[G];dt===void 0&&(G==="instanceMatrix"&&v.instanceMatrix&&(dt=v.instanceMatrix),G==="instanceColor"&&v.instanceColor&&(dt=v.instanceColor));let bt={};bt.attribute=dt,dt&&dt.data&&(bt.data=dt.data),W[G]=bt,X++}r.attributes=W,r.attributesNum=X,r.index=k}function _(){let v=r.newAttributes;for(let C=0,H=v.length;C<H;C++)v[C]=0}function m(v){l(v,0)}function l(v,C){let H=r.newAttributes,k=r.enabledAttributes,W=r.attributeDivisors;H[v]=1,k[v]===0&&(i.enableVertexAttribArray(v),k[v]=1),W[v]!==C&&(i.vertexAttribDivisor(v,C),W[v]=C)}function S(){let v=r.newAttributes,C=r.enabledAttributes;for(let H=0,k=C.length;H<k;H++)C[H]!==v[H]&&(i.disableVertexAttribArray(H),C[H]=0)}function b(v,C,H,k,W,J,X){X===!0?i.vertexAttribIPointer(v,C,H,W,J):i.vertexAttribPointer(v,C,H,k,W,J)}function y(v,C,H,k){_();let W=k.attributes,J=H.getAttributes(),X=C.defaultAttributeValues;for(let tt in J){let G=J[tt];if(G.location>=0){let st=W[tt];if(st===void 0&&(tt==="instanceMatrix"&&v.instanceMatrix&&(st=v.instanceMatrix),tt==="instanceColor"&&v.instanceColor&&(st=v.instanceColor)),st!==void 0){let dt=st.normalized,bt=st.itemSize,zt=t.get(st);if(zt===void 0)continue;let ee=zt.buffer,Y=zt.type,et=zt.bytesPerElement,Mt=Y===i.INT||Y===i.UNSIGNED_INT||st.gpuType===Cc;if(st.isInterleavedBufferAttribute){let rt=st.data,Ct=rt.stride,Lt=st.offset;if(rt.isInstancedInterleavedBuffer){for(let kt=0;kt<G.locationSize;kt++)l(G.location+kt,rt.meshPerAttribute);v.isInstancedMesh!==!0&&k._maxInstanceCount===void 0&&(k._maxInstanceCount=rt.meshPerAttribute*rt.count)}else for(let kt=0;kt<G.locationSize;kt++)m(G.location+kt);i.bindBuffer(i.ARRAY_BUFFER,ee);for(let kt=0;kt<G.locationSize;kt++)b(G.location+kt,bt/G.locationSize,Y,dt,Ct*et,(Lt+bt/G.locationSize*kt)*et,Mt)}else{if(st.isInstancedBufferAttribute){for(let rt=0;rt<G.locationSize;rt++)l(G.location+rt,st.meshPerAttribute);v.isInstancedMesh!==!0&&k._maxInstanceCount===void 0&&(k._maxInstanceCount=st.meshPerAttribute*st.count)}else for(let rt=0;rt<G.locationSize;rt++)m(G.location+rt);i.bindBuffer(i.ARRAY_BUFFER,ee);for(let rt=0;rt<G.locationSize;rt++)b(G.location+rt,bt/G.locationSize,Y,dt,bt*et,bt/G.locationSize*rt*et,Mt)}}else if(X!==void 0){let dt=X[tt];if(dt!==void 0)switch(dt.length){case 2:i.vertexAttrib2fv(G.location,dt);break;case 3:i.vertexAttrib3fv(G.location,dt);break;case 4:i.vertexAttrib4fv(G.location,dt);break;default:i.vertexAttrib1fv(G.location,dt)}}}}S()}function I(){A();for(let v in n){let C=n[v];for(let H in C){let k=C[H];for(let W in k)f(k[W].object),delete k[W];delete C[H]}delete n[v]}}function T(v){if(n[v.id]===void 0)return;let C=n[v.id];for(let H in C){let k=C[H];for(let W in k)f(k[W].object),delete k[W];delete C[H]}delete n[v.id]}function R(v){for(let C in n){let H=n[C];if(H[v.id]===void 0)continue;let k=H[v.id];for(let W in k)f(k[W].object),delete k[W];delete H[v.id]}}function A(){E(),o=!0,r!==s&&(r=s,u(r.object))}function E(){s.geometry=null,s.program=null,s.wireframe=!1}return{setup:a,reset:A,resetDefaultState:E,dispose:I,releaseStatesOfGeometry:T,releaseStatesOfProgram:R,initAttributes:_,enableAttribute:m,disableUnusedAttributes:S}}function bm(i,t,e){let n;function s(u){n=u}function r(u,f){i.drawArrays(n,u,f),e.update(f,n,1)}function o(u,f,p){p!==0&&(i.drawArraysInstanced(n,u,f,p),e.update(f,n,p))}function a(u,f,p){if(p===0)return;t.get("WEBGL_multi_draw").multiDrawArraysWEBGL(n,u,0,f,0,p);let d=0;for(let g=0;g<p;g++)d+=f[g];e.update(d,n,1)}function c(u,f,p,h){if(p===0)return;let d=t.get("WEBGL_multi_draw");if(d===null)for(let g=0;g<u.length;g++)o(u[g],f[g],h[g]);else{d.multiDrawArraysInstancedWEBGL(n,u,0,f,0,h,0,p);let g=0;for(let _=0;_<p;_++)g+=f[_]*h[_];e.update(g,n,1)}}this.setMode=s,this.render=r,this.renderInstances=o,this.renderMultiDraw=a,this.renderMultiDrawInstances=c}function wm(i,t,e,n){let s;function r(){if(s!==void 0)return s;if(t.has("EXT_texture_filter_anisotropic")===!0){let R=t.get("EXT_texture_filter_anisotropic");s=i.getParameter(R.MAX_TEXTURE_MAX_ANISOTROPY_EXT)}else s=0;return s}function o(R){return!(R!==on&&n.convert(R)!==i.getParameter(i.IMPLEMENTATION_COLOR_READ_FORMAT))}function a(R){let A=R===Fs&&(t.has("EXT_color_buffer_half_float")||t.has("EXT_color_buffer_float"));return!(R!==In&&n.convert(R)!==i.getParameter(i.IMPLEMENTATION_COLOR_READ_TYPE)&&R!==Tn&&!A)}function c(R){if(R==="highp"){if(i.getShaderPrecisionFormat(i.VERTEX_SHADER,i.HIGH_FLOAT).precision>0&&i.getShaderPrecisionFormat(i.FRAGMENT_SHADER,i.HIGH_FLOAT).precision>0)return"highp";R="mediump"}return R==="mediump"&&i.getShaderPrecisionFormat(i.VERTEX_SHADER,i.MEDIUM_FLOAT).precision>0&&i.getShaderPrecisionFormat(i.FRAGMENT_SHADER,i.MEDIUM_FLOAT).precision>0?"mediump":"lowp"}let u=e.precision!==void 0?e.precision:"highp",f=c(u);f!==u&&(console.warn("THREE.WebGLRenderer:",u,"not supported, using",f,"instead."),u=f);let p=e.logarithmicDepthBuffer===!0,h=e.reverseDepthBuffer===!0&&t.has("EXT_clip_control"),d=i.getParameter(i.MAX_TEXTURE_IMAGE_UNITS),g=i.getParameter(i.MAX_VERTEX_TEXTURE_IMAGE_UNITS),_=i.getParameter(i.MAX_TEXTURE_SIZE),m=i.getParameter(i.MAX_CUBE_MAP_TEXTURE_SIZE),l=i.getParameter(i.MAX_VERTEX_ATTRIBS),S=i.getParameter(i.MAX_VERTEX_UNIFORM_VECTORS),b=i.getParameter(i.MAX_VARYING_VECTORS),y=i.getParameter(i.MAX_FRAGMENT_UNIFORM_VECTORS),I=g>0,T=i.getParameter(i.MAX_SAMPLES);return{isWebGL2:!0,getMaxAnisotropy:r,getMaxPrecision:c,textureFormatReadable:o,textureTypeReadable:a,precision:u,logarithmicDepthBuffer:p,reverseDepthBuffer:h,maxTextures:d,maxVertexTextures:g,maxTextureSize:_,maxCubemapSize:m,maxAttributes:l,maxVertexUniforms:S,maxVaryings:b,maxFragmentUniforms:y,vertexTextures:I,maxSamples:T}}function Tm(i){let t=this,e=null,n=0,s=!1,r=!1,o=new wn,a=new Ft,c={value:null,needsUpdate:!1};this.uniform=c,this.numPlanes=0,this.numIntersection=0,this.init=function(p,h){let d=p.length!==0||h||n!==0||s;return s=h,n=p.length,d},this.beginShadows=function(){r=!0,f(null)},this.endShadows=function(){r=!1},this.setGlobalState=function(p,h){e=f(p,h,0)},this.setState=function(p,h,d){let g=p.clippingPlanes,_=p.clipIntersection,m=p.clipShadows,l=i.get(p);if(!s||g===null||g.length===0||r&&!m)r?f(null):u();else{let S=r?0:n,b=S*4,y=l.clippingState||null;c.value=y,y=f(g,h,b,d);for(let I=0;I!==b;++I)y[I]=e[I];l.clippingState=y,this.numIntersection=_?this.numPlanes:0,this.numPlanes+=S}};function u(){c.value!==e&&(c.value=e,c.needsUpdate=n>0),t.numPlanes=n,t.numIntersection=0}function f(p,h,d,g){let _=p!==null?p.length:0,m=null;if(_!==0){if(m=c.value,g!==!0||m===null){let l=d+_*4,S=h.matrixWorldInverse;a.getNormalMatrix(S),(m===null||m.length<l)&&(m=new Float32Array(l));for(let b=0,y=d;b!==_;++b,y+=4)o.copy(p[b]).applyMatrix4(S,a),o.normal.toArray(m,y),m[y+3]=o.constant}c.value=m,c.needsUpdate=!0}return t.numPlanes=_,t.numIntersection=0,m}}function Am(i){let t=new WeakMap;function e(o,a){return a===da?o.mapping=$i:a===fa&&(o.mapping=Ji),o}function n(o){if(o&&o.isTexture){let a=o.mapping;if(a===da||a===fa)if(t.has(o)){let c=t.get(o).texture;return e(c,o.mapping)}else{let c=o.image;if(c&&c.height>0){let u=new Za(c.height);return u.fromEquirectangularTexture(i,o),t.set(o,u),o.addEventListener("dispose",s),e(u.texture,o.mapping)}else return null}}return o}function s(o){let a=o.target;a.removeEventListener("dispose",s);let c=t.get(a);c!==void 0&&(t.delete(a),c.dispose())}function r(){t=new WeakMap}return{get:n,dispose:r}}var Nr=class extends Ur{constructor(t=-1,e=1,n=1,s=-1,r=.1,o=2e3){super(),this.isOrthographicCamera=!0,this.type="OrthographicCamera",this.zoom=1,this.view=null,this.left=t,this.right=e,this.top=n,this.bottom=s,this.near=r,this.far=o,this.updateProjectionMatrix()}copy(t,e){return super.copy(t,e),this.left=t.left,this.right=t.right,this.top=t.top,this.bottom=t.bottom,this.near=t.near,this.far=t.far,this.zoom=t.zoom,this.view=t.view===null?null:Object.assign({},t.view),this}setViewOffset(t,e,n,s,r,o){this.view===null&&(this.view={enabled:!0,fullWidth:1,fullHeight:1,offsetX:0,offsetY:0,width:1,height:1}),this.view.enabled=!0,this.view.fullWidth=t,this.view.fullHeight=e,this.view.offsetX=n,this.view.offsetY=s,this.view.width=r,this.view.height=o,this.updateProjectionMatrix()}clearViewOffset(){this.view!==null&&(this.view.enabled=!1),this.updateProjectionMatrix()}updateProjectionMatrix(){let t=(this.right-this.left)/(2*this.zoom),e=(this.top-this.bottom)/(2*this.zoom),n=(this.right+this.left)/2,s=(this.top+this.bottom)/2,r=n-t,o=n+t,a=s+e,c=s-e;if(this.view!==null&&this.view.enabled){let u=(this.right-this.left)/this.view.fullWidth/this.zoom,f=(this.top-this.bottom)/this.view.fullHeight/this.zoom;r+=u*this.view.offsetX,o=r+u*this.view.width,a-=f*this.view.offsetY,c=a-f*this.view.height}this.projectionMatrix.makeOrthographic(r,o,a,c,this.near,this.far,this.coordinateSystem),this.projectionMatrixInverse.copy(this.projectionMatrix).invert()}toJSON(t){let e=super.toJSON(t);return e.object.zoom=this.zoom,e.object.left=this.left,e.object.right=this.right,e.object.top=this.top,e.object.bottom=this.bottom,e.object.near=this.near,e.object.far=this.far,this.view!==null&&(e.object.view=Object.assign({},this.view)),e}},Gi=4,kl=[.125,.215,.35,.446,.526,.582],li=20,qo=new Nr,Hl=new Bt,Yo=null,Zo=0,$o=0,Jo=!1,ai=(1+Math.sqrt(5))/2,Bi=1/ai,Vl=[new P(-ai,Bi,0),new P(ai,Bi,0),new P(-Bi,0,ai),new P(Bi,0,ai),new P(0,ai,-Bi),new P(0,ai,Bi),new P(-1,1,-1),new P(1,1,-1),new P(-1,1,1),new P(1,1,1)],Or=class{constructor(t){this._renderer=t,this._pingPongRenderTarget=null,this._lodMax=0,this._cubeSize=0,this._lodPlanes=[],this._sizeLods=[],this._sigmas=[],this._blurMaterial=null,this._cubemapMaterial=null,this._equirectMaterial=null,this._compileMaterial(this._blurMaterial)}fromScene(t,e=0,n=.1,s=100){Yo=this._renderer.getRenderTarget(),Zo=this._renderer.getActiveCubeFace(),$o=this._renderer.getActiveMipmapLevel(),Jo=this._renderer.xr.enabled,this._renderer.xr.enabled=!1,this._setSize(256);let r=this._allocateTargets();return r.depthBuffer=!0,this._sceneToCubeUV(t,n,s,r),e>0&&this._blur(r,0,0,e),this._applyPMREM(r),this._cleanup(r),r}fromEquirectangular(t,e=null){return this._fromTexture(t,e)}fromCubemap(t,e=null){return this._fromTexture(t,e)}compileCubemapShader(){this._cubemapMaterial===null&&(this._cubemapMaterial=Xl(),this._compileMaterial(this._cubemapMaterial))}compileEquirectangularShader(){this._equirectMaterial===null&&(this._equirectMaterial=Wl(),this._compileMaterial(this._equirectMaterial))}dispose(){this._dispose(),this._cubemapMaterial!==null&&this._cubemapMaterial.dispose(),this._equirectMaterial!==null&&this._equirectMaterial.dispose()}_setSize(t){this._lodMax=Math.floor(Math.log2(t)),this._cubeSize=Math.pow(2,this._lodMax)}_dispose(){this._blurMaterial!==null&&this._blurMaterial.dispose(),this._pingPongRenderTarget!==null&&this._pingPongRenderTarget.dispose();for(let t=0;t<this._lodPlanes.length;t++)this._lodPlanes[t].dispose()}_cleanup(t){this._renderer.setRenderTarget(Yo,Zo,$o),this._renderer.xr.enabled=Jo,t.scissorTest=!1,ur(t,0,0,t.width,t.height)}_fromTexture(t,e){t.mapping===$i||t.mapping===Ji?this._setSize(t.image.length===0?16:t.image[0].width||t.image[0].image.width):this._setSize(t.image.width/4),Yo=this._renderer.getRenderTarget(),Zo=this._renderer.getActiveCubeFace(),$o=this._renderer.getActiveMipmapLevel(),Jo=this._renderer.xr.enabled,this._renderer.xr.enabled=!1;let n=e||this._allocateTargets();return this._textureToCubeUV(t,n),this._applyPMREM(n),this._cleanup(n),n}_allocateTargets(){let t=3*Math.max(this._cubeSize,112),e=4*this._cubeSize,n={magFilter:pn,minFilter:pn,generateMipmaps:!1,type:Fs,format:on,colorSpace:ns,depthBuffer:!1},s=Gl(t,e,n);if(this._pingPongRenderTarget===null||this._pingPongRenderTarget.width!==t||this._pingPongRenderTarget.height!==e){this._pingPongRenderTarget!==null&&this._dispose(),this._pingPongRenderTarget=Gl(t,e,n);let{_lodMax:r}=this;({sizeLods:this._sizeLods,lodPlanes:this._lodPlanes,sigmas:this._sigmas}=Rm(r)),this._blurMaterial=Cm(r,t,e)}return s}_compileMaterial(t){let e=new _t(this._lodPlanes[0],t);this._renderer.compile(e,qo)}_sceneToCubeUV(t,e,n,s){let a=new Ue(90,1,e,n),c=[1,-1,1,1,1,1],u=[1,1,1,-1,-1,-1],f=this._renderer,p=f.autoClear,h=f.toneMapping;f.getClearColor(Hl),f.toneMapping=Zn,f.autoClear=!1;let d=new Je({name:"PMREM.Background",side:ke,depthWrite:!1,depthTest:!1}),g=new _t(new He,d),_=!1,m=t.background;m?m.isColor&&(d.color.copy(m),t.background=null,_=!0):(d.color.copy(Hl),_=!0);for(let l=0;l<6;l++){let S=l%3;S===0?(a.up.set(0,c[l],0),a.lookAt(u[l],0,0)):S===1?(a.up.set(0,0,c[l]),a.lookAt(0,u[l],0)):(a.up.set(0,c[l],0),a.lookAt(0,0,u[l]));let b=this._cubeSize;ur(s,S*b,l>2?b:0,b,b),f.setRenderTarget(s),_&&f.render(g,a),f.render(t,a)}g.geometry.dispose(),g.material.dispose(),f.toneMapping=h,f.autoClear=p,t.background=m}_textureToCubeUV(t,e){let n=this._renderer,s=t.mapping===$i||t.mapping===Ji;s?(this._cubemapMaterial===null&&(this._cubemapMaterial=Xl()),this._cubemapMaterial.uniforms.flipEnvMap.value=t.isRenderTargetTexture===!1?-1:1):this._equirectMaterial===null&&(this._equirectMaterial=Wl());let r=s?this._cubemapMaterial:this._equirectMaterial,o=new _t(this._lodPlanes[0],r),a=r.uniforms;a.envMap.value=t;let c=this._cubeSize;ur(e,0,0,3*c,2*c),n.setRenderTarget(e),n.render(o,qo)}_applyPMREM(t){let e=this._renderer,n=e.autoClear;e.autoClear=!1;let s=this._lodPlanes.length;for(let r=1;r<s;r++){let o=Math.sqrt(this._sigmas[r]*this._sigmas[r]-this._sigmas[r-1]*this._sigmas[r-1]),a=Vl[(s-r-1)%Vl.length];this._blur(t,r-1,r,o,a)}e.autoClear=n}_blur(t,e,n,s,r){let o=this._pingPongRenderTarget;this._halfBlur(t,o,e,n,s,"latitudinal",r),this._halfBlur(o,t,n,n,s,"longitudinal",r)}_halfBlur(t,e,n,s,r,o,a){let c=this._renderer,u=this._blurMaterial;o!=="latitudinal"&&o!=="longitudinal"&&console.error("blur direction must be either latitudinal or longitudinal!");let f=3,p=new _t(this._lodPlanes[s],u),h=u.uniforms,d=this._sizeLods[n]-1,g=isFinite(r)?Math.PI/(2*d):2*Math.PI/(2*li-1),_=r/g,m=isFinite(r)?1+Math.floor(f*_):li;m>li&&console.warn(`sigmaRadians, ${r}, is too large and will clip, as it requested ${m} samples when the maximum is set to ${li}`);let l=[],S=0;for(let R=0;R<li;++R){let A=R/_,E=Math.exp(-A*A/2);l.push(E),R===0?S+=E:R<m&&(S+=2*E)}for(let R=0;R<l.length;R++)l[R]=l[R]/S;h.envMap.value=t.texture,h.samples.value=m,h.weights.value=l,h.latitudinal.value=o==="latitudinal",a&&(h.poleAxis.value=a);let{_lodMax:b}=this;h.dTheta.value=g,h.mipInt.value=b-n;let y=this._sizeLods[s],I=3*y*(s>b-Gi?s-b+Gi:0),T=4*(this._cubeSize-y);ur(e,I,T,3*y,2*y),c.setRenderTarget(e),c.render(p,qo)}};function Rm(i){let t=[],e=[],n=[],s=i,r=i-Gi+1+kl.length;for(let o=0;o<r;o++){let a=Math.pow(2,s);e.push(a);let c=1/a;o>i-Gi?c=kl[o-i+Gi-1]:o===0&&(c=0),n.push(c);let u=1/(a-2),f=-u,p=1+u,h=[f,f,p,f,p,p,f,f,p,p,f,p],d=6,g=6,_=3,m=2,l=1,S=new Float32Array(_*g*d),b=new Float32Array(m*g*d),y=new Float32Array(l*g*d);for(let T=0;T<d;T++){let R=T%3*2/3-1,A=T>2?0:-1,E=[R,A,0,R+2/3,A,0,R+2/3,A+1,0,R,A,0,R+2/3,A+1,0,R,A+1,0];S.set(E,_*g*T),b.set(h,m*g*T);let v=[T,T,T,T,T,T];y.set(v,l*g*T)}let I=new Ee;I.setAttribute("position",new Ie(S,_)),I.setAttribute("uv",new Ie(b,m)),I.setAttribute("faceIndex",new Ie(y,l)),t.push(I),s>Gi&&s--}return{lodPlanes:t,sizeLods:e,sigmas:n}}function Gl(i,t,e){let n=new Pn(i,t,e);return n.texture.mapping=eo,n.texture.name="PMREM.cubeUv",n.scissorTest=!0,n}function ur(i,t,e,n,s){i.viewport.set(t,e,n,s),i.scissor.set(t,e,n,s)}function Cm(i,t,e){let n=new Float32Array(li),s=new P(0,1,0);return new gn({name:"SphericalGaussianBlur",defines:{n:li,CUBEUV_TEXEL_WIDTH:1/t,CUBEUV_TEXEL_HEIGHT:1/e,CUBEUV_MAX_MIP:`${i}.0`},uniforms:{envMap:{value:null},samples:{value:1},weights:{value:n},latitudinal:{value:!1},dTheta:{value:0},mipInt:{value:0},poleAxis:{value:s}},vertexShader:Fc(),fragmentShader:`

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
		`,blending:Yn,depthTest:!1,depthWrite:!1})}function Wl(){return new gn({name:"EquirectangularToCubeUV",uniforms:{envMap:{value:null}},vertexShader:Fc(),fragmentShader:`

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
		`,blending:Yn,depthTest:!1,depthWrite:!1})}function Xl(){return new gn({name:"CubemapToCubeUV",uniforms:{envMap:{value:null},flipEnvMap:{value:-1}},vertexShader:Fc(),fragmentShader:`

			precision mediump float;
			precision mediump int;

			uniform float flipEnvMap;

			varying vec3 vOutputDirection;

			uniform samplerCube envMap;

			void main() {

				gl_FragColor = textureCube( envMap, vec3( flipEnvMap * vOutputDirection.x, vOutputDirection.yz ) );

			}
		`,blending:Yn,depthTest:!1,depthWrite:!1})}function Fc(){return`

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
	`}function Im(i){let t=new WeakMap,e=null;function n(a){if(a&&a.isTexture){let c=a.mapping,u=c===da||c===fa,f=c===$i||c===Ji;if(u||f){let p=t.get(a),h=p!==void 0?p.texture.pmremVersion:0;if(a.isRenderTargetTexture&&a.pmremVersion!==h)return e===null&&(e=new Or(i)),p=u?e.fromEquirectangular(a,p):e.fromCubemap(a,p),p.texture.pmremVersion=a.pmremVersion,t.set(a,p),p.texture;if(p!==void 0)return p.texture;{let d=a.image;return u&&d&&d.height>0||f&&d&&s(d)?(e===null&&(e=new Or(i)),p=u?e.fromEquirectangular(a):e.fromCubemap(a),p.texture.pmremVersion=a.pmremVersion,t.set(a,p),a.addEventListener("dispose",r),p.texture):null}}}return a}function s(a){let c=0,u=6;for(let f=0;f<u;f++)a[f]!==void 0&&c++;return c===u}function r(a){let c=a.target;c.removeEventListener("dispose",r);let u=t.get(c);u!==void 0&&(t.delete(c),u.dispose())}function o(){t=new WeakMap,e!==null&&(e.dispose(),e=null)}return{get:n,dispose:o}}function Pm(i){let t={};function e(n){if(t[n]!==void 0)return t[n];let s;switch(n){case"WEBGL_depth_texture":s=i.getExtension("WEBGL_depth_texture")||i.getExtension("MOZ_WEBGL_depth_texture")||i.getExtension("WEBKIT_WEBGL_depth_texture");break;case"EXT_texture_filter_anisotropic":s=i.getExtension("EXT_texture_filter_anisotropic")||i.getExtension("MOZ_EXT_texture_filter_anisotropic")||i.getExtension("WEBKIT_EXT_texture_filter_anisotropic");break;case"WEBGL_compressed_texture_s3tc":s=i.getExtension("WEBGL_compressed_texture_s3tc")||i.getExtension("MOZ_WEBGL_compressed_texture_s3tc")||i.getExtension("WEBKIT_WEBGL_compressed_texture_s3tc");break;case"WEBGL_compressed_texture_pvrtc":s=i.getExtension("WEBGL_compressed_texture_pvrtc")||i.getExtension("WEBKIT_WEBGL_compressed_texture_pvrtc");break;default:s=i.getExtension(n)}return t[n]=s,s}return{has:function(n){return e(n)!==null},init:function(){e("EXT_color_buffer_float"),e("WEBGL_clip_cull_distance"),e("OES_texture_float_linear"),e("EXT_color_buffer_half_float"),e("WEBGL_multisampled_render_to_texture"),e("WEBGL_render_shared_exponent")},get:function(n){let s=e(n);return s===null&&Ms("THREE.WebGLRenderer: "+n+" extension not supported."),s}}}function Lm(i,t,e,n){let s={},r=new WeakMap;function o(p){let h=p.target;h.index!==null&&t.remove(h.index);for(let g in h.attributes)t.remove(h.attributes[g]);for(let g in h.morphAttributes){let _=h.morphAttributes[g];for(let m=0,l=_.length;m<l;m++)t.remove(_[m])}h.removeEventListener("dispose",o),delete s[h.id];let d=r.get(h);d&&(t.remove(d),r.delete(h)),n.releaseStatesOfGeometry(h),h.isInstancedBufferGeometry===!0&&delete h._maxInstanceCount,e.memory.geometries--}function a(p,h){return s[h.id]===!0||(h.addEventListener("dispose",o),s[h.id]=!0,e.memory.geometries++),h}function c(p){let h=p.attributes;for(let g in h)t.update(h[g],i.ARRAY_BUFFER);let d=p.morphAttributes;for(let g in d){let _=d[g];for(let m=0,l=_.length;m<l;m++)t.update(_[m],i.ARRAY_BUFFER)}}function u(p){let h=[],d=p.index,g=p.attributes.position,_=0;if(d!==null){let S=d.array;_=d.version;for(let b=0,y=S.length;b<y;b+=3){let I=S[b+0],T=S[b+1],R=S[b+2];h.push(I,T,T,R,R,I)}}else if(g!==void 0){let S=g.array;_=g.version;for(let b=0,y=S.length/3-1;b<y;b+=3){let I=b+0,T=b+1,R=b+2;h.push(I,T,T,R,R,I)}}else return;let m=new(Lh(h)?Dr:Lr)(h,1);m.version=_;let l=r.get(p);l&&t.remove(l),r.set(p,m)}function f(p){let h=r.get(p);if(h){let d=p.index;d!==null&&h.version<d.version&&u(p)}else u(p);return r.get(p)}return{get:a,update:c,getWireframeAttribute:f}}function Dm(i,t,e){let n;function s(h){n=h}let r,o;function a(h){r=h.type,o=h.bytesPerElement}function c(h,d){i.drawElements(n,d,r,h*o),e.update(d,n,1)}function u(h,d,g){g!==0&&(i.drawElementsInstanced(n,d,r,h*o,g),e.update(d,n,g))}function f(h,d,g){if(g===0)return;t.get("WEBGL_multi_draw").multiDrawElementsWEBGL(n,d,0,r,h,0,g);let m=0;for(let l=0;l<g;l++)m+=d[l];e.update(m,n,1)}function p(h,d,g,_){if(g===0)return;let m=t.get("WEBGL_multi_draw");if(m===null)for(let l=0;l<h.length;l++)u(h[l]/o,d[l],_[l]);else{m.multiDrawElementsInstancedWEBGL(n,d,0,r,h,0,_,0,g);let l=0;for(let S=0;S<g;S++)l+=d[S]*_[S];e.update(l,n,1)}}this.setMode=s,this.setIndex=a,this.render=c,this.renderInstances=u,this.renderMultiDraw=f,this.renderMultiDrawInstances=p}function Um(i){let t={geometries:0,textures:0},e={frame:0,calls:0,triangles:0,points:0,lines:0};function n(r,o,a){switch(e.calls++,o){case i.TRIANGLES:e.triangles+=a*(r/3);break;case i.LINES:e.lines+=a*(r/2);break;case i.LINE_STRIP:e.lines+=a*(r-1);break;case i.LINE_LOOP:e.lines+=a*r;break;case i.POINTS:e.points+=a*r;break;default:console.error("THREE.WebGLInfo: Unknown draw mode:",o);break}}function s(){e.calls=0,e.triangles=0,e.points=0,e.lines=0}return{memory:t,render:e,programs:null,autoReset:!0,reset:s,update:n}}function Fm(i,t,e){let n=new WeakMap,s=new pe;function r(o,a,c){let u=o.morphTargetInfluences,f=a.morphAttributes.position||a.morphAttributes.normal||a.morphAttributes.color,p=f!==void 0?f.length:0,h=n.get(a);if(h===void 0||h.count!==p){let E=function(){R.dispose(),n.delete(a),a.removeEventListener("dispose",E)};h!==void 0&&h.texture.dispose();let d=a.morphAttributes.position!==void 0,g=a.morphAttributes.normal!==void 0,_=a.morphAttributes.color!==void 0,m=a.morphAttributes.position||[],l=a.morphAttributes.normal||[],S=a.morphAttributes.color||[],b=0;d===!0&&(b=1),g===!0&&(b=2),_===!0&&(b=3);let y=a.attributes.position.count*b,I=1;y>t.maxTextureSize&&(I=Math.ceil(y/t.maxTextureSize),y=t.maxTextureSize);let T=new Float32Array(y*I*4*p),R=new Cr(T,y,I,p);R.type=Tn,R.needsUpdate=!0;let A=b*4;for(let v=0;v<p;v++){let C=m[v],H=l[v],k=S[v],W=y*I*4*v;for(let J=0;J<C.count;J++){let X=J*A;d===!0&&(s.fromBufferAttribute(C,J),T[W+X+0]=s.x,T[W+X+1]=s.y,T[W+X+2]=s.z,T[W+X+3]=0),g===!0&&(s.fromBufferAttribute(H,J),T[W+X+4]=s.x,T[W+X+5]=s.y,T[W+X+6]=s.z,T[W+X+7]=0),_===!0&&(s.fromBufferAttribute(k,J),T[W+X+8]=s.x,T[W+X+9]=s.y,T[W+X+10]=s.z,T[W+X+11]=k.itemSize===4?s.w:1)}}h={count:p,texture:R,size:new ut(y,I)},n.set(a,h),a.addEventListener("dispose",E)}if(o.isInstancedMesh===!0&&o.morphTexture!==null)c.getUniforms().setValue(i,"morphTexture",o.morphTexture,e);else{let d=0;for(let _=0;_<u.length;_++)d+=u[_];let g=a.morphTargetsRelative?1:1-d;c.getUniforms().setValue(i,"morphTargetBaseInfluence",g),c.getUniforms().setValue(i,"morphTargetInfluences",u)}c.getUniforms().setValue(i,"morphTargetsTexture",h.texture,e),c.getUniforms().setValue(i,"morphTargetsTextureSize",h.size)}return{update:r}}function Nm(i,t,e,n){let s=new WeakMap;function r(c){let u=n.render.frame,f=c.geometry,p=t.get(c,f);if(s.get(p)!==u&&(t.update(p),s.set(p,u)),c.isInstancedMesh&&(c.hasEventListener("dispose",a)===!1&&c.addEventListener("dispose",a),s.get(c)!==u&&(e.update(c.instanceMatrix,i.ARRAY_BUFFER),c.instanceColor!==null&&e.update(c.instanceColor,i.ARRAY_BUFFER),s.set(c,u))),c.isSkinnedMesh){let h=c.skeleton;s.get(h)!==u&&(h.update(),s.set(h,u))}return p}function o(){s=new WeakMap}function a(c){let u=c.target;u.removeEventListener("dispose",a),e.remove(u.instanceMatrix),u.instanceColor!==null&&e.remove(u.instanceColor)}return{update:r,dispose:o}}var Br=class extends Xe{constructor(t,e,n,s,r,o,a,c,u,f=Xi){if(f!==Xi&&f!==ji)throw new Error("DepthTexture format must be either THREE.DepthFormat or THREE.DepthStencilFormat");n===void 0&&f===Xi&&(n=di),n===void 0&&f===ji&&(n=Ki),super(null,s,r,o,a,c,f,n,u),this.isDepthTexture=!0,this.image={width:t,height:e},this.magFilter=a!==void 0?a:an,this.minFilter=c!==void 0?c:an,this.flipY=!1,this.generateMipmaps=!1,this.compareFunction=null}copy(t){return super.copy(t),this.compareFunction=t.compareFunction,this}toJSON(t){let e=super.toJSON(t);return this.compareFunction!==null&&(e.compareFunction=this.compareFunction),e}},Nh=new Xe,ql=new Br(1,1),Oh=new Cr,Bh=new qa,zh=new Fr,Yl=[],Zl=[],$l=new Float32Array(16),Jl=new Float32Array(9),Kl=new Float32Array(4);function is(i,t,e){let n=i[0];if(n<=0||n>0)return i;let s=t*e,r=Yl[s];if(r===void 0&&(r=new Float32Array(s),Yl[s]=r),t!==0){n.toArray(r,0);for(let o=1,a=0;o!==t;++o)a+=e,i[o].toArray(r,a)}return r}function ye(i,t){if(i.length!==t.length)return!1;for(let e=0,n=i.length;e<n;e++)if(i[e]!==t[e])return!1;return!0}function ve(i,t){for(let e=0,n=t.length;e<n;e++)i[e]=t[e]}function io(i,t){let e=Zl[t];e===void 0&&(e=new Int32Array(t),Zl[t]=e);for(let n=0;n!==t;++n)e[n]=i.allocateTextureUnit();return e}function Om(i,t){let e=this.cache;e[0]!==t&&(i.uniform1f(this.addr,t),e[0]=t)}function Bm(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y)&&(i.uniform2f(this.addr,t.x,t.y),e[0]=t.x,e[1]=t.y);else{if(ye(e,t))return;i.uniform2fv(this.addr,t),ve(e,t)}}function zm(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z)&&(i.uniform3f(this.addr,t.x,t.y,t.z),e[0]=t.x,e[1]=t.y,e[2]=t.z);else if(t.r!==void 0)(e[0]!==t.r||e[1]!==t.g||e[2]!==t.b)&&(i.uniform3f(this.addr,t.r,t.g,t.b),e[0]=t.r,e[1]=t.g,e[2]=t.b);else{if(ye(e,t))return;i.uniform3fv(this.addr,t),ve(e,t)}}function km(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z||e[3]!==t.w)&&(i.uniform4f(this.addr,t.x,t.y,t.z,t.w),e[0]=t.x,e[1]=t.y,e[2]=t.z,e[3]=t.w);else{if(ye(e,t))return;i.uniform4fv(this.addr,t),ve(e,t)}}function Hm(i,t){let e=this.cache,n=t.elements;if(n===void 0){if(ye(e,t))return;i.uniformMatrix2fv(this.addr,!1,t),ve(e,t)}else{if(ye(e,n))return;Kl.set(n),i.uniformMatrix2fv(this.addr,!1,Kl),ve(e,n)}}function Vm(i,t){let e=this.cache,n=t.elements;if(n===void 0){if(ye(e,t))return;i.uniformMatrix3fv(this.addr,!1,t),ve(e,t)}else{if(ye(e,n))return;Jl.set(n),i.uniformMatrix3fv(this.addr,!1,Jl),ve(e,n)}}function Gm(i,t){let e=this.cache,n=t.elements;if(n===void 0){if(ye(e,t))return;i.uniformMatrix4fv(this.addr,!1,t),ve(e,t)}else{if(ye(e,n))return;$l.set(n),i.uniformMatrix4fv(this.addr,!1,$l),ve(e,n)}}function Wm(i,t){let e=this.cache;e[0]!==t&&(i.uniform1i(this.addr,t),e[0]=t)}function Xm(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y)&&(i.uniform2i(this.addr,t.x,t.y),e[0]=t.x,e[1]=t.y);else{if(ye(e,t))return;i.uniform2iv(this.addr,t),ve(e,t)}}function qm(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z)&&(i.uniform3i(this.addr,t.x,t.y,t.z),e[0]=t.x,e[1]=t.y,e[2]=t.z);else{if(ye(e,t))return;i.uniform3iv(this.addr,t),ve(e,t)}}function Ym(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z||e[3]!==t.w)&&(i.uniform4i(this.addr,t.x,t.y,t.z,t.w),e[0]=t.x,e[1]=t.y,e[2]=t.z,e[3]=t.w);else{if(ye(e,t))return;i.uniform4iv(this.addr,t),ve(e,t)}}function Zm(i,t){let e=this.cache;e[0]!==t&&(i.uniform1ui(this.addr,t),e[0]=t)}function $m(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y)&&(i.uniform2ui(this.addr,t.x,t.y),e[0]=t.x,e[1]=t.y);else{if(ye(e,t))return;i.uniform2uiv(this.addr,t),ve(e,t)}}function Jm(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z)&&(i.uniform3ui(this.addr,t.x,t.y,t.z),e[0]=t.x,e[1]=t.y,e[2]=t.z);else{if(ye(e,t))return;i.uniform3uiv(this.addr,t),ve(e,t)}}function Km(i,t){let e=this.cache;if(t.x!==void 0)(e[0]!==t.x||e[1]!==t.y||e[2]!==t.z||e[3]!==t.w)&&(i.uniform4ui(this.addr,t.x,t.y,t.z,t.w),e[0]=t.x,e[1]=t.y,e[2]=t.z,e[3]=t.w);else{if(ye(e,t))return;i.uniform4uiv(this.addr,t),ve(e,t)}}function jm(i,t,e){let n=this.cache,s=e.allocateTextureUnit();n[0]!==s&&(i.uniform1i(this.addr,s),n[0]=s);let r;this.type===i.SAMPLER_2D_SHADOW?(ql.compareFunction=Ph,r=ql):r=Nh,e.setTexture2D(t||r,s)}function Qm(i,t,e){let n=this.cache,s=e.allocateTextureUnit();n[0]!==s&&(i.uniform1i(this.addr,s),n[0]=s),e.setTexture3D(t||Bh,s)}function tg(i,t,e){let n=this.cache,s=e.allocateTextureUnit();n[0]!==s&&(i.uniform1i(this.addr,s),n[0]=s),e.setTextureCube(t||zh,s)}function eg(i,t,e){let n=this.cache,s=e.allocateTextureUnit();n[0]!==s&&(i.uniform1i(this.addr,s),n[0]=s),e.setTexture2DArray(t||Oh,s)}function ng(i){switch(i){case 5126:return Om;case 35664:return Bm;case 35665:return zm;case 35666:return km;case 35674:return Hm;case 35675:return Vm;case 35676:return Gm;case 5124:case 35670:return Wm;case 35667:case 35671:return Xm;case 35668:case 35672:return qm;case 35669:case 35673:return Ym;case 5125:return Zm;case 36294:return $m;case 36295:return Jm;case 36296:return Km;case 35678:case 36198:case 36298:case 36306:case 35682:return jm;case 35679:case 36299:case 36307:return Qm;case 35680:case 36300:case 36308:case 36293:return tg;case 36289:case 36303:case 36311:case 36292:return eg}}function ig(i,t){i.uniform1fv(this.addr,t)}function sg(i,t){let e=is(t,this.size,2);i.uniform2fv(this.addr,e)}function rg(i,t){let e=is(t,this.size,3);i.uniform3fv(this.addr,e)}function og(i,t){let e=is(t,this.size,4);i.uniform4fv(this.addr,e)}function ag(i,t){let e=is(t,this.size,4);i.uniformMatrix2fv(this.addr,!1,e)}function cg(i,t){let e=is(t,this.size,9);i.uniformMatrix3fv(this.addr,!1,e)}function lg(i,t){let e=is(t,this.size,16);i.uniformMatrix4fv(this.addr,!1,e)}function hg(i,t){i.uniform1iv(this.addr,t)}function ug(i,t){i.uniform2iv(this.addr,t)}function dg(i,t){i.uniform3iv(this.addr,t)}function fg(i,t){i.uniform4iv(this.addr,t)}function pg(i,t){i.uniform1uiv(this.addr,t)}function mg(i,t){i.uniform2uiv(this.addr,t)}function gg(i,t){i.uniform3uiv(this.addr,t)}function _g(i,t){i.uniform4uiv(this.addr,t)}function xg(i,t,e){let n=this.cache,s=t.length,r=io(e,s);ye(n,r)||(i.uniform1iv(this.addr,r),ve(n,r));for(let o=0;o!==s;++o)e.setTexture2D(t[o]||Nh,r[o])}function yg(i,t,e){let n=this.cache,s=t.length,r=io(e,s);ye(n,r)||(i.uniform1iv(this.addr,r),ve(n,r));for(let o=0;o!==s;++o)e.setTexture3D(t[o]||Bh,r[o])}function vg(i,t,e){let n=this.cache,s=t.length,r=io(e,s);ye(n,r)||(i.uniform1iv(this.addr,r),ve(n,r));for(let o=0;o!==s;++o)e.setTextureCube(t[o]||zh,r[o])}function Mg(i,t,e){let n=this.cache,s=t.length,r=io(e,s);ye(n,r)||(i.uniform1iv(this.addr,r),ve(n,r));for(let o=0;o!==s;++o)e.setTexture2DArray(t[o]||Oh,r[o])}function Sg(i){switch(i){case 5126:return ig;case 35664:return sg;case 35665:return rg;case 35666:return og;case 35674:return ag;case 35675:return cg;case 35676:return lg;case 5124:case 35670:return hg;case 35667:case 35671:return ug;case 35668:case 35672:return dg;case 35669:case 35673:return fg;case 5125:return pg;case 36294:return mg;case 36295:return gg;case 36296:return _g;case 35678:case 36198:case 36298:case 36306:case 35682:return xg;case 35679:case 36299:case 36307:return yg;case 35680:case 36300:case 36308:case 36293:return vg;case 36289:case 36303:case 36311:case 36292:return Mg}}var $a=class{constructor(t,e,n){this.id=t,this.addr=n,this.cache=[],this.type=e.type,this.setValue=ng(e.type)}},Ja=class{constructor(t,e,n){this.id=t,this.addr=n,this.cache=[],this.type=e.type,this.size=e.size,this.setValue=Sg(e.type)}},Ka=class{constructor(t){this.id=t,this.seq=[],this.map={}}setValue(t,e,n){let s=this.seq;for(let r=0,o=s.length;r!==o;++r){let a=s[r];a.setValue(t,e[a.id],n)}}},Ko=/(\w+)(\])?(\[|\.)?/g;function jl(i,t){i.seq.push(t),i.map[t.id]=t}function Eg(i,t,e){let n=i.name,s=n.length;for(Ko.lastIndex=0;;){let r=Ko.exec(n),o=Ko.lastIndex,a=r[1],c=r[2]==="]",u=r[3];if(c&&(a=a|0),u===void 0||u==="["&&o+2===s){jl(e,u===void 0?new $a(a,i,t):new Ja(a,i,t));break}else{let p=e.map[a];p===void 0&&(p=new Ka(a),jl(e,p)),e=p}}}var Yi=class{constructor(t,e){this.seq=[],this.map={};let n=t.getProgramParameter(e,t.ACTIVE_UNIFORMS);for(let s=0;s<n;++s){let r=t.getActiveUniform(e,s),o=t.getUniformLocation(e,r.name);Eg(r,o,this)}}setValue(t,e,n,s){let r=this.map[e];r!==void 0&&r.setValue(t,n,s)}setOptional(t,e,n){let s=e[n];s!==void 0&&this.setValue(t,n,s)}static upload(t,e,n,s){for(let r=0,o=e.length;r!==o;++r){let a=e[r],c=n[a.id];c.needsUpdate!==!1&&a.setValue(t,c.value,s)}}static seqWithValue(t,e){let n=[];for(let s=0,r=t.length;s!==r;++s){let o=t[s];o.id in e&&n.push(o)}return n}};function Ql(i,t,e){let n=i.createShader(t);return i.shaderSource(n,e),i.compileShader(n),n}var bg=37297,wg=0;function Tg(i,t){let e=i.split(`
`),n=[],s=Math.max(t-6,0),r=Math.min(t+6,e.length);for(let o=s;o<r;o++){let a=o+1;n.push(`${a===t?">":" "} ${a}: ${e[o]}`)}return n.join(`
`)}var th=new Ft;function Ag(i){qt._getMatrix(th,qt.workingColorSpace,i);let t=`mat3( ${th.elements.map(e=>e.toFixed(4))} )`;switch(qt.getTransfer(i)){case no:return[t,"LinearTransferOETF"];case Qt:return[t,"sRGBTransferOETF"];default:return console.warn("THREE.WebGLProgram: Unsupported color space: ",i),[t,"LinearTransferOETF"]}}function eh(i,t,e){let n=i.getShaderParameter(t,i.COMPILE_STATUS),s=i.getShaderInfoLog(t).trim();if(n&&s==="")return"";let r=/ERROR: 0:(\d+)/.exec(s);if(r){let o=parseInt(r[1]);return e.toUpperCase()+`

`+s+`

`+Tg(i.getShaderSource(t),o)}else return s}function Rg(i,t){let e=Ag(t);return[`vec4 ${i}( vec4 value ) {`,`	return ${e[1]}( vec4( value.rgb * ${e[0]}, value.a ) );`,"}"].join(`
`)}function Cg(i,t){let e;switch(t){case Qu:e="Linear";break;case td:e="Reinhard";break;case ed:e="Cineon";break;case nd:e="ACESFilmic";break;case sd:e="AgX";break;case rd:e="Neutral";break;case id:e="Custom";break;default:console.warn("THREE.WebGLProgram: Unsupported toneMapping:",t),e="Linear"}return"vec3 "+i+"( vec3 color ) { return "+e+"ToneMapping( color ); }"}var dr=new P;function Ig(){qt.getLuminanceCoefficients(dr);let i=dr.x.toFixed(4),t=dr.y.toFixed(4),e=dr.z.toFixed(4);return["float luminance( const in vec3 rgb ) {",`	const vec3 weights = vec3( ${i}, ${t}, ${e} );`,"	return dot( weights, rgb );","}"].join(`
`)}function Pg(i){return[i.extensionClipCullDistance?"#extension GL_ANGLE_clip_cull_distance : require":"",i.extensionMultiDraw?"#extension GL_ANGLE_multi_draw : require":""].filter(Ss).join(`
`)}function Lg(i){let t=[];for(let e in i){let n=i[e];n!==!1&&t.push("#define "+e+" "+n)}return t.join(`
`)}function Dg(i,t){let e={},n=i.getProgramParameter(t,i.ACTIVE_ATTRIBUTES);for(let s=0;s<n;s++){let r=i.getActiveAttrib(t,s),o=r.name,a=1;r.type===i.FLOAT_MAT2&&(a=2),r.type===i.FLOAT_MAT3&&(a=3),r.type===i.FLOAT_MAT4&&(a=4),e[o]={type:r.type,location:i.getAttribLocation(t,o),locationSize:a}}return e}function Ss(i){return i!==""}function nh(i,t){let e=t.numSpotLightShadows+t.numSpotLightMaps-t.numSpotLightShadowsWithMaps;return i.replace(/NUM_DIR_LIGHTS/g,t.numDirLights).replace(/NUM_SPOT_LIGHTS/g,t.numSpotLights).replace(/NUM_SPOT_LIGHT_MAPS/g,t.numSpotLightMaps).replace(/NUM_SPOT_LIGHT_COORDS/g,e).replace(/NUM_RECT_AREA_LIGHTS/g,t.numRectAreaLights).replace(/NUM_POINT_LIGHTS/g,t.numPointLights).replace(/NUM_HEMI_LIGHTS/g,t.numHemiLights).replace(/NUM_DIR_LIGHT_SHADOWS/g,t.numDirLightShadows).replace(/NUM_SPOT_LIGHT_SHADOWS_WITH_MAPS/g,t.numSpotLightShadowsWithMaps).replace(/NUM_SPOT_LIGHT_SHADOWS/g,t.numSpotLightShadows).replace(/NUM_POINT_LIGHT_SHADOWS/g,t.numPointLightShadows)}function ih(i,t){return i.replace(/NUM_CLIPPING_PLANES/g,t.numClippingPlanes).replace(/UNION_CLIPPING_PLANES/g,t.numClippingPlanes-t.numClipIntersection)}var Ug=/^[ \t]*#include +<([\w\d./]+)>/gm;function ja(i){return i.replace(Ug,Ng)}var Fg=new Map;function Ng(i,t){let e=Ot[t];if(e===void 0){let n=Fg.get(t);if(n!==void 0)e=Ot[n],console.warn('THREE.WebGLRenderer: Shader chunk "%s" has been deprecated. Use "%s" instead.',t,n);else throw new Error("Can not resolve #include <"+t+">")}return ja(e)}var Og=/#pragma unroll_loop_start\s+for\s*\(\s*int\s+i\s*=\s*(\d+)\s*;\s*i\s*<\s*(\d+)\s*;\s*i\s*\+\+\s*\)\s*{([\s\S]+?)}\s+#pragma unroll_loop_end/g;function sh(i){return i.replace(Og,Bg)}function Bg(i,t,e,n){let s="";for(let r=parseInt(t);r<parseInt(e);r++)s+=n.replace(/\[\s*i\s*\]/g,"[ "+r+" ]").replace(/UNROLLED_LOOP_INDEX/g,r);return s}function rh(i){let t=`precision ${i.precision} float;
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
#define LOW_PRECISION`),t}function zg(i){let t="SHADOWMAP_TYPE_BASIC";return i.shadowMapType===xh?t="SHADOWMAP_TYPE_PCF":i.shadowMapType===Lu?t="SHADOWMAP_TYPE_PCF_SOFT":i.shadowMapType===bn&&(t="SHADOWMAP_TYPE_VSM"),t}function kg(i){let t="ENVMAP_TYPE_CUBE";if(i.envMap)switch(i.envMapMode){case $i:case Ji:t="ENVMAP_TYPE_CUBE";break;case eo:t="ENVMAP_TYPE_CUBE_UV";break}return t}function Hg(i){let t="ENVMAP_MODE_REFLECTION";if(i.envMap)switch(i.envMapMode){case Ji:t="ENVMAP_MODE_REFRACTION";break}return t}function Vg(i){let t="ENVMAP_BLENDING_NONE";if(i.envMap)switch(i.combine){case Rc:t="ENVMAP_BLENDING_MULTIPLY";break;case Ku:t="ENVMAP_BLENDING_MIX";break;case ju:t="ENVMAP_BLENDING_ADD";break}return t}function Gg(i){let t=i.envMapCubeUVHeight;if(t===null)return null;let e=Math.log2(t)-2,n=1/t;return{texelWidth:1/(3*Math.max(Math.pow(2,e),112)),texelHeight:n,maxMip:e}}function Wg(i,t,e,n){let s=i.getContext(),r=e.defines,o=e.vertexShader,a=e.fragmentShader,c=zg(e),u=kg(e),f=Hg(e),p=Vg(e),h=Gg(e),d=Pg(e),g=Lg(r),_=s.createProgram(),m,l,S=e.glslVersion?"#version "+e.glslVersion+`
`:"";e.isRawShaderMaterial?(m=["#define SHADER_TYPE "+e.shaderType,"#define SHADER_NAME "+e.shaderName,g].filter(Ss).join(`
`),m.length>0&&(m+=`
`),l=["#define SHADER_TYPE "+e.shaderType,"#define SHADER_NAME "+e.shaderName,g].filter(Ss).join(`
`),l.length>0&&(l+=`
`)):(m=[rh(e),"#define SHADER_TYPE "+e.shaderType,"#define SHADER_NAME "+e.shaderName,g,e.extensionClipCullDistance?"#define USE_CLIP_DISTANCE":"",e.batching?"#define USE_BATCHING":"",e.batchingColor?"#define USE_BATCHING_COLOR":"",e.instancing?"#define USE_INSTANCING":"",e.instancingColor?"#define USE_INSTANCING_COLOR":"",e.instancingMorph?"#define USE_INSTANCING_MORPH":"",e.useFog&&e.fog?"#define USE_FOG":"",e.useFog&&e.fogExp2?"#define FOG_EXP2":"",e.map?"#define USE_MAP":"",e.envMap?"#define USE_ENVMAP":"",e.envMap?"#define "+f:"",e.lightMap?"#define USE_LIGHTMAP":"",e.aoMap?"#define USE_AOMAP":"",e.bumpMap?"#define USE_BUMPMAP":"",e.normalMap?"#define USE_NORMALMAP":"",e.normalMapObjectSpace?"#define USE_NORMALMAP_OBJECTSPACE":"",e.normalMapTangentSpace?"#define USE_NORMALMAP_TANGENTSPACE":"",e.displacementMap?"#define USE_DISPLACEMENTMAP":"",e.emissiveMap?"#define USE_EMISSIVEMAP":"",e.anisotropy?"#define USE_ANISOTROPY":"",e.anisotropyMap?"#define USE_ANISOTROPYMAP":"",e.clearcoatMap?"#define USE_CLEARCOATMAP":"",e.clearcoatRoughnessMap?"#define USE_CLEARCOAT_ROUGHNESSMAP":"",e.clearcoatNormalMap?"#define USE_CLEARCOAT_NORMALMAP":"",e.iridescenceMap?"#define USE_IRIDESCENCEMAP":"",e.iridescenceThicknessMap?"#define USE_IRIDESCENCE_THICKNESSMAP":"",e.specularMap?"#define USE_SPECULARMAP":"",e.specularColorMap?"#define USE_SPECULAR_COLORMAP":"",e.specularIntensityMap?"#define USE_SPECULAR_INTENSITYMAP":"",e.roughnessMap?"#define USE_ROUGHNESSMAP":"",e.metalnessMap?"#define USE_METALNESSMAP":"",e.alphaMap?"#define USE_ALPHAMAP":"",e.alphaHash?"#define USE_ALPHAHASH":"",e.transmission?"#define USE_TRANSMISSION":"",e.transmissionMap?"#define USE_TRANSMISSIONMAP":"",e.thicknessMap?"#define USE_THICKNESSMAP":"",e.sheenColorMap?"#define USE_SHEEN_COLORMAP":"",e.sheenRoughnessMap?"#define USE_SHEEN_ROUGHNESSMAP":"",e.mapUv?"#define MAP_UV "+e.mapUv:"",e.alphaMapUv?"#define ALPHAMAP_UV "+e.alphaMapUv:"",e.lightMapUv?"#define LIGHTMAP_UV "+e.lightMapUv:"",e.aoMapUv?"#define AOMAP_UV "+e.aoMapUv:"",e.emissiveMapUv?"#define EMISSIVEMAP_UV "+e.emissiveMapUv:"",e.bumpMapUv?"#define BUMPMAP_UV "+e.bumpMapUv:"",e.normalMapUv?"#define NORMALMAP_UV "+e.normalMapUv:"",e.displacementMapUv?"#define DISPLACEMENTMAP_UV "+e.displacementMapUv:"",e.metalnessMapUv?"#define METALNESSMAP_UV "+e.metalnessMapUv:"",e.roughnessMapUv?"#define ROUGHNESSMAP_UV "+e.roughnessMapUv:"",e.anisotropyMapUv?"#define ANISOTROPYMAP_UV "+e.anisotropyMapUv:"",e.clearcoatMapUv?"#define CLEARCOATMAP_UV "+e.clearcoatMapUv:"",e.clearcoatNormalMapUv?"#define CLEARCOAT_NORMALMAP_UV "+e.clearcoatNormalMapUv:"",e.clearcoatRoughnessMapUv?"#define CLEARCOAT_ROUGHNESSMAP_UV "+e.clearcoatRoughnessMapUv:"",e.iridescenceMapUv?"#define IRIDESCENCEMAP_UV "+e.iridescenceMapUv:"",e.iridescenceThicknessMapUv?"#define IRIDESCENCE_THICKNESSMAP_UV "+e.iridescenceThicknessMapUv:"",e.sheenColorMapUv?"#define SHEEN_COLORMAP_UV "+e.sheenColorMapUv:"",e.sheenRoughnessMapUv?"#define SHEEN_ROUGHNESSMAP_UV "+e.sheenRoughnessMapUv:"",e.specularMapUv?"#define SPECULARMAP_UV "+e.specularMapUv:"",e.specularColorMapUv?"#define SPECULAR_COLORMAP_UV "+e.specularColorMapUv:"",e.specularIntensityMapUv?"#define SPECULAR_INTENSITYMAP_UV "+e.specularIntensityMapUv:"",e.transmissionMapUv?"#define TRANSMISSIONMAP_UV "+e.transmissionMapUv:"",e.thicknessMapUv?"#define THICKNESSMAP_UV "+e.thicknessMapUv:"",e.vertexTangents&&e.flatShading===!1?"#define USE_TANGENT":"",e.vertexColors?"#define USE_COLOR":"",e.vertexAlphas?"#define USE_COLOR_ALPHA":"",e.vertexUv1s?"#define USE_UV1":"",e.vertexUv2s?"#define USE_UV2":"",e.vertexUv3s?"#define USE_UV3":"",e.pointsUvs?"#define USE_POINTS_UV":"",e.flatShading?"#define FLAT_SHADED":"",e.skinning?"#define USE_SKINNING":"",e.morphTargets?"#define USE_MORPHTARGETS":"",e.morphNormals&&e.flatShading===!1?"#define USE_MORPHNORMALS":"",e.morphColors?"#define USE_MORPHCOLORS":"",e.morphTargetsCount>0?"#define MORPHTARGETS_TEXTURE_STRIDE "+e.morphTextureStride:"",e.morphTargetsCount>0?"#define MORPHTARGETS_COUNT "+e.morphTargetsCount:"",e.doubleSided?"#define DOUBLE_SIDED":"",e.flipSided?"#define FLIP_SIDED":"",e.shadowMapEnabled?"#define USE_SHADOWMAP":"",e.shadowMapEnabled?"#define "+c:"",e.sizeAttenuation?"#define USE_SIZEATTENUATION":"",e.numLightProbes>0?"#define USE_LIGHT_PROBES":"",e.logarithmicDepthBuffer?"#define USE_LOGDEPTHBUF":"",e.reverseDepthBuffer?"#define USE_REVERSEDEPTHBUF":"","uniform mat4 modelMatrix;","uniform mat4 modelViewMatrix;","uniform mat4 projectionMatrix;","uniform mat4 viewMatrix;","uniform mat3 normalMatrix;","uniform vec3 cameraPosition;","uniform bool isOrthographic;","#ifdef USE_INSTANCING","	attribute mat4 instanceMatrix;","#endif","#ifdef USE_INSTANCING_COLOR","	attribute vec3 instanceColor;","#endif","#ifdef USE_INSTANCING_MORPH","	uniform sampler2D morphTexture;","#endif","attribute vec3 position;","attribute vec3 normal;","attribute vec2 uv;","#ifdef USE_UV1","	attribute vec2 uv1;","#endif","#ifdef USE_UV2","	attribute vec2 uv2;","#endif","#ifdef USE_UV3","	attribute vec2 uv3;","#endif","#ifdef USE_TANGENT","	attribute vec4 tangent;","#endif","#if defined( USE_COLOR_ALPHA )","	attribute vec4 color;","#elif defined( USE_COLOR )","	attribute vec3 color;","#endif","#ifdef USE_SKINNING","	attribute vec4 skinIndex;","	attribute vec4 skinWeight;","#endif",`
`].filter(Ss).join(`
`),l=[rh(e),"#define SHADER_TYPE "+e.shaderType,"#define SHADER_NAME "+e.shaderName,g,e.useFog&&e.fog?"#define USE_FOG":"",e.useFog&&e.fogExp2?"#define FOG_EXP2":"",e.alphaToCoverage?"#define ALPHA_TO_COVERAGE":"",e.map?"#define USE_MAP":"",e.matcap?"#define USE_MATCAP":"",e.envMap?"#define USE_ENVMAP":"",e.envMap?"#define "+u:"",e.envMap?"#define "+f:"",e.envMap?"#define "+p:"",h?"#define CUBEUV_TEXEL_WIDTH "+h.texelWidth:"",h?"#define CUBEUV_TEXEL_HEIGHT "+h.texelHeight:"",h?"#define CUBEUV_MAX_MIP "+h.maxMip+".0":"",e.lightMap?"#define USE_LIGHTMAP":"",e.aoMap?"#define USE_AOMAP":"",e.bumpMap?"#define USE_BUMPMAP":"",e.normalMap?"#define USE_NORMALMAP":"",e.normalMapObjectSpace?"#define USE_NORMALMAP_OBJECTSPACE":"",e.normalMapTangentSpace?"#define USE_NORMALMAP_TANGENTSPACE":"",e.emissiveMap?"#define USE_EMISSIVEMAP":"",e.anisotropy?"#define USE_ANISOTROPY":"",e.anisotropyMap?"#define USE_ANISOTROPYMAP":"",e.clearcoat?"#define USE_CLEARCOAT":"",e.clearcoatMap?"#define USE_CLEARCOATMAP":"",e.clearcoatRoughnessMap?"#define USE_CLEARCOAT_ROUGHNESSMAP":"",e.clearcoatNormalMap?"#define USE_CLEARCOAT_NORMALMAP":"",e.dispersion?"#define USE_DISPERSION":"",e.iridescence?"#define USE_IRIDESCENCE":"",e.iridescenceMap?"#define USE_IRIDESCENCEMAP":"",e.iridescenceThicknessMap?"#define USE_IRIDESCENCE_THICKNESSMAP":"",e.specularMap?"#define USE_SPECULARMAP":"",e.specularColorMap?"#define USE_SPECULAR_COLORMAP":"",e.specularIntensityMap?"#define USE_SPECULAR_INTENSITYMAP":"",e.roughnessMap?"#define USE_ROUGHNESSMAP":"",e.metalnessMap?"#define USE_METALNESSMAP":"",e.alphaMap?"#define USE_ALPHAMAP":"",e.alphaTest?"#define USE_ALPHATEST":"",e.alphaHash?"#define USE_ALPHAHASH":"",e.sheen?"#define USE_SHEEN":"",e.sheenColorMap?"#define USE_SHEEN_COLORMAP":"",e.sheenRoughnessMap?"#define USE_SHEEN_ROUGHNESSMAP":"",e.transmission?"#define USE_TRANSMISSION":"",e.transmissionMap?"#define USE_TRANSMISSIONMAP":"",e.thicknessMap?"#define USE_THICKNESSMAP":"",e.vertexTangents&&e.flatShading===!1?"#define USE_TANGENT":"",e.vertexColors||e.instancingColor||e.batchingColor?"#define USE_COLOR":"",e.vertexAlphas?"#define USE_COLOR_ALPHA":"",e.vertexUv1s?"#define USE_UV1":"",e.vertexUv2s?"#define USE_UV2":"",e.vertexUv3s?"#define USE_UV3":"",e.pointsUvs?"#define USE_POINTS_UV":"",e.gradientMap?"#define USE_GRADIENTMAP":"",e.flatShading?"#define FLAT_SHADED":"",e.doubleSided?"#define DOUBLE_SIDED":"",e.flipSided?"#define FLIP_SIDED":"",e.shadowMapEnabled?"#define USE_SHADOWMAP":"",e.shadowMapEnabled?"#define "+c:"",e.premultipliedAlpha?"#define PREMULTIPLIED_ALPHA":"",e.numLightProbes>0?"#define USE_LIGHT_PROBES":"",e.decodeVideoTexture?"#define DECODE_VIDEO_TEXTURE":"",e.decodeVideoTextureEmissive?"#define DECODE_VIDEO_TEXTURE_EMISSIVE":"",e.logarithmicDepthBuffer?"#define USE_LOGDEPTHBUF":"",e.reverseDepthBuffer?"#define USE_REVERSEDEPTHBUF":"","uniform mat4 viewMatrix;","uniform vec3 cameraPosition;","uniform bool isOrthographic;",e.toneMapping!==Zn?"#define TONE_MAPPING":"",e.toneMapping!==Zn?Ot.tonemapping_pars_fragment:"",e.toneMapping!==Zn?Cg("toneMapping",e.toneMapping):"",e.dithering?"#define DITHERING":"",e.opaque?"#define OPAQUE":"",Ot.colorspace_pars_fragment,Rg("linearToOutputTexel",e.outputColorSpace),Ig(),e.useDepthPacking?"#define DEPTH_PACKING "+e.depthPacking:"",`
`].filter(Ss).join(`
`)),o=ja(o),o=nh(o,e),o=ih(o,e),a=ja(a),a=nh(a,e),a=ih(a,e),o=sh(o),a=sh(a),e.isRawShaderMaterial!==!0&&(S=`#version 300 es
`,m=[d,"#define attribute in","#define varying out","#define texture2D texture"].join(`
`)+`
`+m,l=["#define varying in",e.glslVersion===yl?"":"layout(location = 0) out highp vec4 pc_fragColor;",e.glslVersion===yl?"":"#define gl_FragColor pc_fragColor","#define gl_FragDepthEXT gl_FragDepth","#define texture2D texture","#define textureCube texture","#define texture2DProj textureProj","#define texture2DLodEXT textureLod","#define texture2DProjLodEXT textureProjLod","#define textureCubeLodEXT textureLod","#define texture2DGradEXT textureGrad","#define texture2DProjGradEXT textureProjGrad","#define textureCubeGradEXT textureGrad"].join(`
`)+`
`+l);let b=S+m+o,y=S+l+a,I=Ql(s,s.VERTEX_SHADER,b),T=Ql(s,s.FRAGMENT_SHADER,y);s.attachShader(_,I),s.attachShader(_,T),e.index0AttributeName!==void 0?s.bindAttribLocation(_,0,e.index0AttributeName):e.morphTargets===!0&&s.bindAttribLocation(_,0,"position"),s.linkProgram(_);function R(C){if(i.debug.checkShaderErrors){let H=s.getProgramInfoLog(_).trim(),k=s.getShaderInfoLog(I).trim(),W=s.getShaderInfoLog(T).trim(),J=!0,X=!0;if(s.getProgramParameter(_,s.LINK_STATUS)===!1)if(J=!1,typeof i.debug.onShaderError=="function")i.debug.onShaderError(s,_,I,T);else{let tt=eh(s,I,"vertex"),G=eh(s,T,"fragment");console.error("THREE.WebGLProgram: Shader Error "+s.getError()+" - VALIDATE_STATUS "+s.getProgramParameter(_,s.VALIDATE_STATUS)+`

Material Name: `+C.name+`
Material Type: `+C.type+`

Program Info Log: `+H+`
`+tt+`
`+G)}else H!==""?console.warn("THREE.WebGLProgram: Program Info Log:",H):(k===""||W==="")&&(X=!1);X&&(C.diagnostics={runnable:J,programLog:H,vertexShader:{log:k,prefix:m},fragmentShader:{log:W,prefix:l}})}s.deleteShader(I),s.deleteShader(T),A=new Yi(s,_),E=Dg(s,_)}let A;this.getUniforms=function(){return A===void 0&&R(this),A};let E;this.getAttributes=function(){return E===void 0&&R(this),E};let v=e.rendererExtensionParallelShaderCompile===!1;return this.isReady=function(){return v===!1&&(v=s.getProgramParameter(_,bg)),v},this.destroy=function(){n.releaseStatesOfProgram(this),s.deleteProgram(_),this.program=void 0},this.type=e.shaderType,this.name=e.shaderName,this.id=wg++,this.cacheKey=t,this.usedTimes=1,this.program=_,this.vertexShader=I,this.fragmentShader=T,this}var Xg=0,Qa=class{constructor(){this.shaderCache=new Map,this.materialCache=new Map}update(t){let e=t.vertexShader,n=t.fragmentShader,s=this._getShaderStage(e),r=this._getShaderStage(n),o=this._getShaderCacheForMaterial(t);return o.has(s)===!1&&(o.add(s),s.usedTimes++),o.has(r)===!1&&(o.add(r),r.usedTimes++),this}remove(t){let e=this.materialCache.get(t);for(let n of e)n.usedTimes--,n.usedTimes===0&&this.shaderCache.delete(n.code);return this.materialCache.delete(t),this}getVertexShaderID(t){return this._getShaderStage(t.vertexShader).id}getFragmentShaderID(t){return this._getShaderStage(t.fragmentShader).id}dispose(){this.shaderCache.clear(),this.materialCache.clear()}_getShaderCacheForMaterial(t){let e=this.materialCache,n=e.get(t);return n===void 0&&(n=new Set,e.set(t,n)),n}_getShaderStage(t){let e=this.shaderCache,n=e.get(t);return n===void 0&&(n=new tc(t),e.set(t,n)),n}},tc=class{constructor(t){this.id=Xg++,this.code=t,this.usedTimes=0}};function qg(i,t,e,n,s,r,o){let a=new Pr,c=new Qa,u=new Set,f=[],p=s.logarithmicDepthBuffer,h=s.vertexTextures,d=s.precision,g={MeshDepthMaterial:"depth",MeshDistanceMaterial:"distanceRGBA",MeshNormalMaterial:"normal",MeshBasicMaterial:"basic",MeshLambertMaterial:"lambert",MeshPhongMaterial:"phong",MeshToonMaterial:"toon",MeshStandardMaterial:"physical",MeshPhysicalMaterial:"physical",MeshMatcapMaterial:"matcap",LineBasicMaterial:"basic",LineDashedMaterial:"dashed",PointsMaterial:"points",ShadowMaterial:"shadow",SpriteMaterial:"sprite"};function _(E){return u.add(E),E===0?"uv":`uv${E}`}function m(E,v,C,H,k){let W=H.fog,J=k.geometry,X=E.isMeshStandardMaterial?H.environment:null,tt=(E.isMeshStandardMaterial?e:t).get(E.envMap||X),G=tt&&tt.mapping===eo?tt.image.height:null,st=g[E.type];E.precision!==null&&(d=s.getMaxPrecision(E.precision),d!==E.precision&&console.warn("THREE.WebGLProgram.getParameters:",E.precision,"not supported, using",d,"instead."));let dt=J.morphAttributes.position||J.morphAttributes.normal||J.morphAttributes.color,bt=dt!==void 0?dt.length:0,zt=0;J.morphAttributes.position!==void 0&&(zt=1),J.morphAttributes.normal!==void 0&&(zt=2),J.morphAttributes.color!==void 0&&(zt=3);let ee,Y,et,Mt;if(st){let jt=dn[st];ee=jt.vertexShader,Y=jt.fragmentShader}else ee=E.vertexShader,Y=E.fragmentShader,c.update(E),et=c.getVertexShaderID(E),Mt=c.getFragmentShaderID(E);let rt=i.getRenderTarget(),Ct=i.state.buffers.depth.getReversed(),Lt=k.isInstancedMesh===!0,kt=k.isBatchedMesh===!0,de=!!E.map,Wt=!!E.matcap,ge=!!tt,N=!!E.aoMap,Ye=!!E.lightMap,Ht=!!E.bumpMap,Vt=!!E.normalMap,Tt=!!E.displacementMap,oe=!!E.emissiveMap,wt=!!E.metalnessMap,w=!!E.roughnessMap,x=E.anisotropy>0,O=E.clearcoat>0,Z=E.dispersion>0,K=E.iridescence>0,q=E.sheen>0,St=E.transmission>0,ot=x&&!!E.anisotropyMap,ft=O&&!!E.clearcoatMap,Xt=O&&!!E.clearcoatNormalMap,j=O&&!!E.clearcoatRoughnessMap,pt=K&&!!E.iridescenceMap,Rt=K&&!!E.iridescenceThicknessMap,It=q&&!!E.sheenColorMap,mt=q&&!!E.sheenRoughnessMap,Gt=!!E.specularMap,Nt=!!E.specularColorMap,se=!!E.specularIntensityMap,D=St&&!!E.transmissionMap,it=St&&!!E.thicknessMap,V=!!E.gradientMap,$=!!E.alphaMap,lt=E.alphaTest>0,at=!!E.alphaHash,Dt=!!E.extensions,fe=Zn;E.toneMapped&&(rt===null||rt.isXRRenderTarget===!0)&&(fe=i.toneMapping);let Ae={shaderID:st,shaderType:E.type,shaderName:E.name,vertexShader:ee,fragmentShader:Y,defines:E.defines,customVertexShaderID:et,customFragmentShaderID:Mt,isRawShaderMaterial:E.isRawShaderMaterial===!0,glslVersion:E.glslVersion,precision:d,batching:kt,batchingColor:kt&&k._colorsTexture!==null,instancing:Lt,instancingColor:Lt&&k.instanceColor!==null,instancingMorph:Lt&&k.morphTexture!==null,supportsVertexTextures:h,outputColorSpace:rt===null?i.outputColorSpace:rt.isXRRenderTarget===!0?rt.texture.colorSpace:ns,alphaToCoverage:!!E.alphaToCoverage,map:de,matcap:Wt,envMap:ge,envMapMode:ge&&tt.mapping,envMapCubeUVHeight:G,aoMap:N,lightMap:Ye,bumpMap:Ht,normalMap:Vt,displacementMap:h&&Tt,emissiveMap:oe,normalMapObjectSpace:Vt&&E.normalMapType===ld,normalMapTangentSpace:Vt&&E.normalMapType===Ih,metalnessMap:wt,roughnessMap:w,anisotropy:x,anisotropyMap:ot,clearcoat:O,clearcoatMap:ft,clearcoatNormalMap:Xt,clearcoatRoughnessMap:j,dispersion:Z,iridescence:K,iridescenceMap:pt,iridescenceThicknessMap:Rt,sheen:q,sheenColorMap:It,sheenRoughnessMap:mt,specularMap:Gt,specularColorMap:Nt,specularIntensityMap:se,transmission:St,transmissionMap:D,thicknessMap:it,gradientMap:V,opaque:E.transparent===!1&&E.blending===Wi&&E.alphaToCoverage===!1,alphaMap:$,alphaTest:lt,alphaHash:at,combine:E.combine,mapUv:de&&_(E.map.channel),aoMapUv:N&&_(E.aoMap.channel),lightMapUv:Ye&&_(E.lightMap.channel),bumpMapUv:Ht&&_(E.bumpMap.channel),normalMapUv:Vt&&_(E.normalMap.channel),displacementMapUv:Tt&&_(E.displacementMap.channel),emissiveMapUv:oe&&_(E.emissiveMap.channel),metalnessMapUv:wt&&_(E.metalnessMap.channel),roughnessMapUv:w&&_(E.roughnessMap.channel),anisotropyMapUv:ot&&_(E.anisotropyMap.channel),clearcoatMapUv:ft&&_(E.clearcoatMap.channel),clearcoatNormalMapUv:Xt&&_(E.clearcoatNormalMap.channel),clearcoatRoughnessMapUv:j&&_(E.clearcoatRoughnessMap.channel),iridescenceMapUv:pt&&_(E.iridescenceMap.channel),iridescenceThicknessMapUv:Rt&&_(E.iridescenceThicknessMap.channel),sheenColorMapUv:It&&_(E.sheenColorMap.channel),sheenRoughnessMapUv:mt&&_(E.sheenRoughnessMap.channel),specularMapUv:Gt&&_(E.specularMap.channel),specularColorMapUv:Nt&&_(E.specularColorMap.channel),specularIntensityMapUv:se&&_(E.specularIntensityMap.channel),transmissionMapUv:D&&_(E.transmissionMap.channel),thicknessMapUv:it&&_(E.thicknessMap.channel),alphaMapUv:$&&_(E.alphaMap.channel),vertexTangents:!!J.attributes.tangent&&(Vt||x),vertexColors:E.vertexColors,vertexAlphas:E.vertexColors===!0&&!!J.attributes.color&&J.attributes.color.itemSize===4,pointsUvs:k.isPoints===!0&&!!J.attributes.uv&&(de||$),fog:!!W,useFog:E.fog===!0,fogExp2:!!W&&W.isFogExp2,flatShading:E.flatShading===!0,sizeAttenuation:E.sizeAttenuation===!0,logarithmicDepthBuffer:p,reverseDepthBuffer:Ct,skinning:k.isSkinnedMesh===!0,morphTargets:J.morphAttributes.position!==void 0,morphNormals:J.morphAttributes.normal!==void 0,morphColors:J.morphAttributes.color!==void 0,morphTargetsCount:bt,morphTextureStride:zt,numDirLights:v.directional.length,numPointLights:v.point.length,numSpotLights:v.spot.length,numSpotLightMaps:v.spotLightMap.length,numRectAreaLights:v.rectArea.length,numHemiLights:v.hemi.length,numDirLightShadows:v.directionalShadowMap.length,numPointLightShadows:v.pointShadowMap.length,numSpotLightShadows:v.spotShadowMap.length,numSpotLightShadowsWithMaps:v.numSpotLightShadowsWithMaps,numLightProbes:v.numLightProbes,numClippingPlanes:o.numPlanes,numClipIntersection:o.numIntersection,dithering:E.dithering,shadowMapEnabled:i.shadowMap.enabled&&C.length>0,shadowMapType:i.shadowMap.type,toneMapping:fe,decodeVideoTexture:de&&E.map.isVideoTexture===!0&&qt.getTransfer(E.map.colorSpace)===Qt,decodeVideoTextureEmissive:oe&&E.emissiveMap.isVideoTexture===!0&&qt.getTransfer(E.emissiveMap.colorSpace)===Qt,premultipliedAlpha:E.premultipliedAlpha,doubleSided:E.side===ze,flipSided:E.side===ke,useDepthPacking:E.depthPacking>=0,depthPacking:E.depthPacking||0,index0AttributeName:E.index0AttributeName,extensionClipCullDistance:Dt&&E.extensions.clipCullDistance===!0&&n.has("WEBGL_clip_cull_distance"),extensionMultiDraw:(Dt&&E.extensions.multiDraw===!0||kt)&&n.has("WEBGL_multi_draw"),rendererExtensionParallelShaderCompile:n.has("KHR_parallel_shader_compile"),customProgramCacheKey:E.customProgramCacheKey()};return Ae.vertexUv1s=u.has(1),Ae.vertexUv2s=u.has(2),Ae.vertexUv3s=u.has(3),u.clear(),Ae}function l(E){let v=[];if(E.shaderID?v.push(E.shaderID):(v.push(E.customVertexShaderID),v.push(E.customFragmentShaderID)),E.defines!==void 0)for(let C in E.defines)v.push(C),v.push(E.defines[C]);return E.isRawShaderMaterial===!1&&(S(v,E),b(v,E),v.push(i.outputColorSpace)),v.push(E.customProgramCacheKey),v.join()}function S(E,v){E.push(v.precision),E.push(v.outputColorSpace),E.push(v.envMapMode),E.push(v.envMapCubeUVHeight),E.push(v.mapUv),E.push(v.alphaMapUv),E.push(v.lightMapUv),E.push(v.aoMapUv),E.push(v.bumpMapUv),E.push(v.normalMapUv),E.push(v.displacementMapUv),E.push(v.emissiveMapUv),E.push(v.metalnessMapUv),E.push(v.roughnessMapUv),E.push(v.anisotropyMapUv),E.push(v.clearcoatMapUv),E.push(v.clearcoatNormalMapUv),E.push(v.clearcoatRoughnessMapUv),E.push(v.iridescenceMapUv),E.push(v.iridescenceThicknessMapUv),E.push(v.sheenColorMapUv),E.push(v.sheenRoughnessMapUv),E.push(v.specularMapUv),E.push(v.specularColorMapUv),E.push(v.specularIntensityMapUv),E.push(v.transmissionMapUv),E.push(v.thicknessMapUv),E.push(v.combine),E.push(v.fogExp2),E.push(v.sizeAttenuation),E.push(v.morphTargetsCount),E.push(v.morphAttributeCount),E.push(v.numDirLights),E.push(v.numPointLights),E.push(v.numSpotLights),E.push(v.numSpotLightMaps),E.push(v.numHemiLights),E.push(v.numRectAreaLights),E.push(v.numDirLightShadows),E.push(v.numPointLightShadows),E.push(v.numSpotLightShadows),E.push(v.numSpotLightShadowsWithMaps),E.push(v.numLightProbes),E.push(v.shadowMapType),E.push(v.toneMapping),E.push(v.numClippingPlanes),E.push(v.numClipIntersection),E.push(v.depthPacking)}function b(E,v){a.disableAll(),v.supportsVertexTextures&&a.enable(0),v.instancing&&a.enable(1),v.instancingColor&&a.enable(2),v.instancingMorph&&a.enable(3),v.matcap&&a.enable(4),v.envMap&&a.enable(5),v.normalMapObjectSpace&&a.enable(6),v.normalMapTangentSpace&&a.enable(7),v.clearcoat&&a.enable(8),v.iridescence&&a.enable(9),v.alphaTest&&a.enable(10),v.vertexColors&&a.enable(11),v.vertexAlphas&&a.enable(12),v.vertexUv1s&&a.enable(13),v.vertexUv2s&&a.enable(14),v.vertexUv3s&&a.enable(15),v.vertexTangents&&a.enable(16),v.anisotropy&&a.enable(17),v.alphaHash&&a.enable(18),v.batching&&a.enable(19),v.dispersion&&a.enable(20),v.batchingColor&&a.enable(21),E.push(a.mask),a.disableAll(),v.fog&&a.enable(0),v.useFog&&a.enable(1),v.flatShading&&a.enable(2),v.logarithmicDepthBuffer&&a.enable(3),v.reverseDepthBuffer&&a.enable(4),v.skinning&&a.enable(5),v.morphTargets&&a.enable(6),v.morphNormals&&a.enable(7),v.morphColors&&a.enable(8),v.premultipliedAlpha&&a.enable(9),v.shadowMapEnabled&&a.enable(10),v.doubleSided&&a.enable(11),v.flipSided&&a.enable(12),v.useDepthPacking&&a.enable(13),v.dithering&&a.enable(14),v.transmission&&a.enable(15),v.sheen&&a.enable(16),v.opaque&&a.enable(17),v.pointsUvs&&a.enable(18),v.decodeVideoTexture&&a.enable(19),v.decodeVideoTextureEmissive&&a.enable(20),v.alphaToCoverage&&a.enable(21),E.push(a.mask)}function y(E){let v=g[E.type],C;if(v){let H=dn[v];C=Fd.clone(H.uniforms)}else C=E.uniforms;return C}function I(E,v){let C;for(let H=0,k=f.length;H<k;H++){let W=f[H];if(W.cacheKey===v){C=W,++C.usedTimes;break}}return C===void 0&&(C=new Wg(i,v,E,r),f.push(C)),C}function T(E){if(--E.usedTimes===0){let v=f.indexOf(E);f[v]=f[f.length-1],f.pop(),E.destroy()}}function R(E){c.remove(E)}function A(){c.dispose()}return{getParameters:m,getProgramCacheKey:l,getUniforms:y,acquireProgram:I,releaseProgram:T,releaseShaderCache:R,programs:f,dispose:A}}function Yg(){let i=new WeakMap;function t(o){return i.has(o)}function e(o){let a=i.get(o);return a===void 0&&(a={},i.set(o,a)),a}function n(o){i.delete(o)}function s(o,a,c){i.get(o)[a]=c}function r(){i=new WeakMap}return{has:t,get:e,remove:n,update:s,dispose:r}}function Zg(i,t){return i.groupOrder!==t.groupOrder?i.groupOrder-t.groupOrder:i.renderOrder!==t.renderOrder?i.renderOrder-t.renderOrder:i.material.id!==t.material.id?i.material.id-t.material.id:i.z!==t.z?i.z-t.z:i.id-t.id}function oh(i,t){return i.groupOrder!==t.groupOrder?i.groupOrder-t.groupOrder:i.renderOrder!==t.renderOrder?i.renderOrder-t.renderOrder:i.z!==t.z?t.z-i.z:i.id-t.id}function ah(){let i=[],t=0,e=[],n=[],s=[];function r(){t=0,e.length=0,n.length=0,s.length=0}function o(p,h,d,g,_,m){let l=i[t];return l===void 0?(l={id:p.id,object:p,geometry:h,material:d,groupOrder:g,renderOrder:p.renderOrder,z:_,group:m},i[t]=l):(l.id=p.id,l.object=p,l.geometry=h,l.material=d,l.groupOrder=g,l.renderOrder=p.renderOrder,l.z=_,l.group=m),t++,l}function a(p,h,d,g,_,m){let l=o(p,h,d,g,_,m);d.transmission>0?n.push(l):d.transparent===!0?s.push(l):e.push(l)}function c(p,h,d,g,_,m){let l=o(p,h,d,g,_,m);d.transmission>0?n.unshift(l):d.transparent===!0?s.unshift(l):e.unshift(l)}function u(p,h){e.length>1&&e.sort(p||Zg),n.length>1&&n.sort(h||oh),s.length>1&&s.sort(h||oh)}function f(){for(let p=t,h=i.length;p<h;p++){let d=i[p];if(d.id===null)break;d.id=null,d.object=null,d.geometry=null,d.material=null,d.group=null}}return{opaque:e,transmissive:n,transparent:s,init:r,push:a,unshift:c,finish:f,sort:u}}function $g(){let i=new WeakMap;function t(n,s){let r=i.get(n),o;return r===void 0?(o=new ah,i.set(n,[o])):s>=r.length?(o=new ah,r.push(o)):o=r[s],o}function e(){i=new WeakMap}return{get:t,dispose:e}}function Jg(){let i={};return{get:function(t){if(i[t.id]!==void 0)return i[t.id];let e;switch(t.type){case"DirectionalLight":e={direction:new P,color:new Bt};break;case"SpotLight":e={position:new P,direction:new P,color:new Bt,distance:0,coneCos:0,penumbraCos:0,decay:0};break;case"PointLight":e={position:new P,color:new Bt,distance:0,decay:0};break;case"HemisphereLight":e={direction:new P,skyColor:new Bt,groundColor:new Bt};break;case"RectAreaLight":e={color:new Bt,position:new P,halfWidth:new P,halfHeight:new P};break}return i[t.id]=e,e}}}function Kg(){let i={};return{get:function(t){if(i[t.id]!==void 0)return i[t.id];let e;switch(t.type){case"DirectionalLight":e={shadowIntensity:1,shadowBias:0,shadowNormalBias:0,shadowRadius:1,shadowMapSize:new ut};break;case"SpotLight":e={shadowIntensity:1,shadowBias:0,shadowNormalBias:0,shadowRadius:1,shadowMapSize:new ut};break;case"PointLight":e={shadowIntensity:1,shadowBias:0,shadowNormalBias:0,shadowRadius:1,shadowMapSize:new ut,shadowCameraNear:1,shadowCameraFar:1e3};break}return i[t.id]=e,e}}}var jg=0;function Qg(i,t){return(t.castShadow?2:0)-(i.castShadow?2:0)+(t.map?1:0)-(i.map?1:0)}function t0(i){let t=new Jg,e=Kg(),n={version:0,hash:{directionalLength:-1,pointLength:-1,spotLength:-1,rectAreaLength:-1,hemiLength:-1,numDirectionalShadows:-1,numPointShadows:-1,numSpotShadows:-1,numSpotMaps:-1,numLightProbes:-1},ambient:[0,0,0],probe:[],directional:[],directionalShadow:[],directionalShadowMap:[],directionalShadowMatrix:[],spot:[],spotLightMap:[],spotShadow:[],spotShadowMap:[],spotLightMatrix:[],rectArea:[],rectAreaLTC1:null,rectAreaLTC2:null,point:[],pointShadow:[],pointShadowMap:[],pointShadowMatrix:[],hemi:[],numSpotLightShadowsWithMaps:0,numLightProbes:0};for(let u=0;u<9;u++)n.probe.push(new P);let s=new P,r=new he,o=new he;function a(u){let f=0,p=0,h=0;for(let E=0;E<9;E++)n.probe[E].set(0,0,0);let d=0,g=0,_=0,m=0,l=0,S=0,b=0,y=0,I=0,T=0,R=0;u.sort(Qg);for(let E=0,v=u.length;E<v;E++){let C=u[E],H=C.color,k=C.intensity,W=C.distance,J=C.shadow&&C.shadow.map?C.shadow.map.texture:null;if(C.isAmbientLight)f+=H.r*k,p+=H.g*k,h+=H.b*k;else if(C.isLightProbe){for(let X=0;X<9;X++)n.probe[X].addScaledVector(C.sh.coefficients[X],k);R++}else if(C.isDirectionalLight){let X=t.get(C);if(X.color.copy(C.color).multiplyScalar(C.intensity),C.castShadow){let tt=C.shadow,G=e.get(C);G.shadowIntensity=tt.intensity,G.shadowBias=tt.bias,G.shadowNormalBias=tt.normalBias,G.shadowRadius=tt.radius,G.shadowMapSize=tt.mapSize,n.directionalShadow[d]=G,n.directionalShadowMap[d]=J,n.directionalShadowMatrix[d]=C.shadow.matrix,S++}n.directional[d]=X,d++}else if(C.isSpotLight){let X=t.get(C);X.position.setFromMatrixPosition(C.matrixWorld),X.color.copy(H).multiplyScalar(k),X.distance=W,X.coneCos=Math.cos(C.angle),X.penumbraCos=Math.cos(C.angle*(1-C.penumbra)),X.decay=C.decay,n.spot[_]=X;let tt=C.shadow;if(C.map&&(n.spotLightMap[I]=C.map,I++,tt.updateMatrices(C),C.castShadow&&T++),n.spotLightMatrix[_]=tt.matrix,C.castShadow){let G=e.get(C);G.shadowIntensity=tt.intensity,G.shadowBias=tt.bias,G.shadowNormalBias=tt.normalBias,G.shadowRadius=tt.radius,G.shadowMapSize=tt.mapSize,n.spotShadow[_]=G,n.spotShadowMap[_]=J,y++}_++}else if(C.isRectAreaLight){let X=t.get(C);X.color.copy(H).multiplyScalar(k),X.halfWidth.set(C.width*.5,0,0),X.halfHeight.set(0,C.height*.5,0),n.rectArea[m]=X,m++}else if(C.isPointLight){let X=t.get(C);if(X.color.copy(C.color).multiplyScalar(C.intensity),X.distance=C.distance,X.decay=C.decay,C.castShadow){let tt=C.shadow,G=e.get(C);G.shadowIntensity=tt.intensity,G.shadowBias=tt.bias,G.shadowNormalBias=tt.normalBias,G.shadowRadius=tt.radius,G.shadowMapSize=tt.mapSize,G.shadowCameraNear=tt.camera.near,G.shadowCameraFar=tt.camera.far,n.pointShadow[g]=G,n.pointShadowMap[g]=J,n.pointShadowMatrix[g]=C.shadow.matrix,b++}n.point[g]=X,g++}else if(C.isHemisphereLight){let X=t.get(C);X.skyColor.copy(C.color).multiplyScalar(k),X.groundColor.copy(C.groundColor).multiplyScalar(k),n.hemi[l]=X,l++}}m>0&&(i.has("OES_texture_float_linear")===!0?(n.rectAreaLTC1=nt.LTC_FLOAT_1,n.rectAreaLTC2=nt.LTC_FLOAT_2):(n.rectAreaLTC1=nt.LTC_HALF_1,n.rectAreaLTC2=nt.LTC_HALF_2)),n.ambient[0]=f,n.ambient[1]=p,n.ambient[2]=h;let A=n.hash;(A.directionalLength!==d||A.pointLength!==g||A.spotLength!==_||A.rectAreaLength!==m||A.hemiLength!==l||A.numDirectionalShadows!==S||A.numPointShadows!==b||A.numSpotShadows!==y||A.numSpotMaps!==I||A.numLightProbes!==R)&&(n.directional.length=d,n.spot.length=_,n.rectArea.length=m,n.point.length=g,n.hemi.length=l,n.directionalShadow.length=S,n.directionalShadowMap.length=S,n.pointShadow.length=b,n.pointShadowMap.length=b,n.spotShadow.length=y,n.spotShadowMap.length=y,n.directionalShadowMatrix.length=S,n.pointShadowMatrix.length=b,n.spotLightMatrix.length=y+I-T,n.spotLightMap.length=I,n.numSpotLightShadowsWithMaps=T,n.numLightProbes=R,A.directionalLength=d,A.pointLength=g,A.spotLength=_,A.rectAreaLength=m,A.hemiLength=l,A.numDirectionalShadows=S,A.numPointShadows=b,A.numSpotShadows=y,A.numSpotMaps=I,A.numLightProbes=R,n.version=jg++)}function c(u,f){let p=0,h=0,d=0,g=0,_=0,m=f.matrixWorldInverse;for(let l=0,S=u.length;l<S;l++){let b=u[l];if(b.isDirectionalLight){let y=n.directional[p];y.direction.setFromMatrixPosition(b.matrixWorld),s.setFromMatrixPosition(b.target.matrixWorld),y.direction.sub(s),y.direction.transformDirection(m),p++}else if(b.isSpotLight){let y=n.spot[d];y.position.setFromMatrixPosition(b.matrixWorld),y.position.applyMatrix4(m),y.direction.setFromMatrixPosition(b.matrixWorld),s.setFromMatrixPosition(b.target.matrixWorld),y.direction.sub(s),y.direction.transformDirection(m),d++}else if(b.isRectAreaLight){let y=n.rectArea[g];y.position.setFromMatrixPosition(b.matrixWorld),y.position.applyMatrix4(m),o.identity(),r.copy(b.matrixWorld),r.premultiply(m),o.extractRotation(r),y.halfWidth.set(b.width*.5,0,0),y.halfHeight.set(0,b.height*.5,0),y.halfWidth.applyMatrix4(o),y.halfHeight.applyMatrix4(o),g++}else if(b.isPointLight){let y=n.point[h];y.position.setFromMatrixPosition(b.matrixWorld),y.position.applyMatrix4(m),h++}else if(b.isHemisphereLight){let y=n.hemi[_];y.direction.setFromMatrixPosition(b.matrixWorld),y.direction.transformDirection(m),_++}}}return{setup:a,setupView:c,state:n}}function ch(i){let t=new t0(i),e=[],n=[];function s(f){u.camera=f,e.length=0,n.length=0}function r(f){e.push(f)}function o(f){n.push(f)}function a(){t.setup(e)}function c(f){t.setupView(e,f)}let u={lightsArray:e,shadowsArray:n,camera:null,lights:t,transmissionRenderTarget:{}};return{init:s,state:u,setupLights:a,setupLightsView:c,pushLight:r,pushShadow:o}}function e0(i){let t=new WeakMap;function e(s,r=0){let o=t.get(s),a;return o===void 0?(a=new ch(i),t.set(s,[a])):r>=o.length?(a=new ch(i),o.push(a)):a=o[r],a}function n(){t=new WeakMap}return{get:e,dispose:n}}var ec=class extends Ln{static get type(){return"MeshDepthMaterial"}constructor(t){super(),this.isMeshDepthMaterial=!0,this.depthPacking=ad,this.map=null,this.alphaMap=null,this.displacementMap=null,this.displacementScale=1,this.displacementBias=0,this.wireframe=!1,this.wireframeLinewidth=1,this.setValues(t)}copy(t){return super.copy(t),this.depthPacking=t.depthPacking,this.map=t.map,this.alphaMap=t.alphaMap,this.displacementMap=t.displacementMap,this.displacementScale=t.displacementScale,this.displacementBias=t.displacementBias,this.wireframe=t.wireframe,this.wireframeLinewidth=t.wireframeLinewidth,this}},nc=class extends Ln{static get type(){return"MeshDistanceMaterial"}constructor(t){super(),this.isMeshDistanceMaterial=!0,this.map=null,this.alphaMap=null,this.displacementMap=null,this.displacementScale=1,this.displacementBias=0,this.setValues(t)}copy(t){return super.copy(t),this.map=t.map,this.alphaMap=t.alphaMap,this.displacementMap=t.displacementMap,this.displacementScale=t.displacementScale,this.displacementBias=t.displacementBias,this}},n0=`void main() {
	gl_Position = vec4( position, 1.0 );
}`,i0=`uniform sampler2D shadow_pass;
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
}`;function s0(i,t,e){let n=new As,s=new ut,r=new ut,o=new pe,a=new ec({depthPacking:cd}),c=new nc,u={},f=e.maxTextureSize,p={[Jn]:ke,[ke]:Jn,[ze]:ze},h=new gn({defines:{VSM_SAMPLES:8},uniforms:{shadow_pass:{value:null},resolution:{value:new ut},radius:{value:4}},vertexShader:n0,fragmentShader:i0}),d=h.clone();d.defines.HORIZONTAL_PASS=1;let g=new Ee;g.setAttribute("position",new Ie(new Float32Array([-1,-1,.5,3,-1,.5,-1,3,.5]),3));let _=new _t(g,h),m=this;this.enabled=!1,this.autoUpdate=!0,this.needsUpdate=!1,this.type=xh;let l=this.type;this.render=function(T,R,A){if(m.enabled===!1||m.autoUpdate===!1&&m.needsUpdate===!1||T.length===0)return;let E=i.getRenderTarget(),v=i.getActiveCubeFace(),C=i.getActiveMipmapLevel(),H=i.state;H.setBlending(Yn),H.buffers.color.setClear(1,1,1,1),H.buffers.depth.setTest(!0),H.setScissorTest(!1);let k=l!==bn&&this.type===bn,W=l===bn&&this.type!==bn;for(let J=0,X=T.length;J<X;J++){let tt=T[J],G=tt.shadow;if(G===void 0){console.warn("THREE.WebGLShadowMap:",tt,"has no shadow.");continue}if(G.autoUpdate===!1&&G.needsUpdate===!1)continue;s.copy(G.mapSize);let st=G.getFrameExtents();if(s.multiply(st),r.copy(G.mapSize),(s.x>f||s.y>f)&&(s.x>f&&(r.x=Math.floor(f/st.x),s.x=r.x*st.x,G.mapSize.x=r.x),s.y>f&&(r.y=Math.floor(f/st.y),s.y=r.y*st.y,G.mapSize.y=r.y)),G.map===null||k===!0||W===!0){let bt=this.type!==bn?{minFilter:an,magFilter:an}:{};G.map!==null&&G.map.dispose(),G.map=new Pn(s.x,s.y,bt),G.map.texture.name=tt.name+".shadowMap",G.camera.updateProjectionMatrix()}i.setRenderTarget(G.map),i.clear();let dt=G.getViewportCount();for(let bt=0;bt<dt;bt++){let zt=G.getViewport(bt);o.set(r.x*zt.x,r.y*zt.y,r.x*zt.z,r.y*zt.w),H.viewport(o),G.updateMatrices(tt,bt),n=G.getFrustum(),y(R,A,G.camera,tt,this.type)}G.isPointLightShadow!==!0&&this.type===bn&&S(G,A),G.needsUpdate=!1}l=this.type,m.needsUpdate=!1,i.setRenderTarget(E,v,C)};function S(T,R){let A=t.update(_);h.defines.VSM_SAMPLES!==T.blurSamples&&(h.defines.VSM_SAMPLES=T.blurSamples,d.defines.VSM_SAMPLES=T.blurSamples,h.needsUpdate=!0,d.needsUpdate=!0),T.mapPass===null&&(T.mapPass=new Pn(s.x,s.y)),h.uniforms.shadow_pass.value=T.map.texture,h.uniforms.resolution.value=T.mapSize,h.uniforms.radius.value=T.radius,i.setRenderTarget(T.mapPass),i.clear(),i.renderBufferDirect(R,null,A,h,_,null),d.uniforms.shadow_pass.value=T.mapPass.texture,d.uniforms.resolution.value=T.mapSize,d.uniforms.radius.value=T.radius,i.setRenderTarget(T.map),i.clear(),i.renderBufferDirect(R,null,A,d,_,null)}function b(T,R,A,E){let v=null,C=A.isPointLight===!0?T.customDistanceMaterial:T.customDepthMaterial;if(C!==void 0)v=C;else if(v=A.isPointLight===!0?c:a,i.localClippingEnabled&&R.clipShadows===!0&&Array.isArray(R.clippingPlanes)&&R.clippingPlanes.length!==0||R.displacementMap&&R.displacementScale!==0||R.alphaMap&&R.alphaTest>0||R.map&&R.alphaTest>0){let H=v.uuid,k=R.uuid,W=u[H];W===void 0&&(W={},u[H]=W);let J=W[k];J===void 0&&(J=v.clone(),W[k]=J,R.addEventListener("dispose",I)),v=J}if(v.visible=R.visible,v.wireframe=R.wireframe,E===bn?v.side=R.shadowSide!==null?R.shadowSide:R.side:v.side=R.shadowSide!==null?R.shadowSide:p[R.side],v.alphaMap=R.alphaMap,v.alphaTest=R.alphaTest,v.map=R.map,v.clipShadows=R.clipShadows,v.clippingPlanes=R.clippingPlanes,v.clipIntersection=R.clipIntersection,v.displacementMap=R.displacementMap,v.displacementScale=R.displacementScale,v.displacementBias=R.displacementBias,v.wireframeLinewidth=R.wireframeLinewidth,v.linewidth=R.linewidth,A.isPointLight===!0&&v.isMeshDistanceMaterial===!0){let H=i.properties.get(v);H.light=A}return v}function y(T,R,A,E,v){if(T.visible===!1)return;if(T.layers.test(R.layers)&&(T.isMesh||T.isLine||T.isPoints)&&(T.castShadow||T.receiveShadow&&v===bn)&&(!T.frustumCulled||n.intersectsObject(T))){T.modelViewMatrix.multiplyMatrices(A.matrixWorldInverse,T.matrixWorld);let k=t.update(T),W=T.material;if(Array.isArray(W)){let J=k.groups;for(let X=0,tt=J.length;X<tt;X++){let G=J[X],st=W[G.materialIndex];if(st&&st.visible){let dt=b(T,st,E,v);T.onBeforeShadow(i,T,R,A,k,dt,G),i.renderBufferDirect(A,null,k,dt,T,G),T.onAfterShadow(i,T,R,A,k,dt,G)}}}else if(W.visible){let J=b(T,W,E,v);T.onBeforeShadow(i,T,R,A,k,J,null),i.renderBufferDirect(A,null,k,J,T,null),T.onAfterShadow(i,T,R,A,k,J,null)}}let H=T.children;for(let k=0,W=H.length;k<W;k++)y(H[k],R,A,E,v)}function I(T){T.target.removeEventListener("dispose",I);for(let A in u){let E=u[A],v=T.target.uuid;v in E&&(E[v].dispose(),delete E[v])}}}var r0={[ra]:oa,[aa]:ha,[ca]:ua,[Zi]:la,[oa]:ra,[ha]:aa,[ua]:ca,[la]:Zi};function o0(i,t){function e(){let D=!1,it=new pe,V=null,$=new pe(0,0,0,0);return{setMask:function(lt){V!==lt&&!D&&(i.colorMask(lt,lt,lt,lt),V=lt)},setLocked:function(lt){D=lt},setClear:function(lt,at,Dt,fe,Ae){Ae===!0&&(lt*=fe,at*=fe,Dt*=fe),it.set(lt,at,Dt,fe),$.equals(it)===!1&&(i.clearColor(lt,at,Dt,fe),$.copy(it))},reset:function(){D=!1,V=null,$.set(-1,0,0,0)}}}function n(){let D=!1,it=!1,V=null,$=null,lt=null;return{setReversed:function(at){if(it!==at){let Dt=t.get("EXT_clip_control");it?Dt.clipControlEXT(Dt.LOWER_LEFT_EXT,Dt.ZERO_TO_ONE_EXT):Dt.clipControlEXT(Dt.LOWER_LEFT_EXT,Dt.NEGATIVE_ONE_TO_ONE_EXT);let fe=lt;lt=null,this.setClear(fe)}it=at},getReversed:function(){return it},setTest:function(at){at?rt(i.DEPTH_TEST):Ct(i.DEPTH_TEST)},setMask:function(at){V!==at&&!D&&(i.depthMask(at),V=at)},setFunc:function(at){if(it&&(at=r0[at]),$!==at){switch(at){case ra:i.depthFunc(i.NEVER);break;case oa:i.depthFunc(i.ALWAYS);break;case aa:i.depthFunc(i.LESS);break;case Zi:i.depthFunc(i.LEQUAL);break;case ca:i.depthFunc(i.EQUAL);break;case la:i.depthFunc(i.GEQUAL);break;case ha:i.depthFunc(i.GREATER);break;case ua:i.depthFunc(i.NOTEQUAL);break;default:i.depthFunc(i.LEQUAL)}$=at}},setLocked:function(at){D=at},setClear:function(at){lt!==at&&(it&&(at=1-at),i.clearDepth(at),lt=at)},reset:function(){D=!1,V=null,$=null,lt=null,it=!1}}}function s(){let D=!1,it=null,V=null,$=null,lt=null,at=null,Dt=null,fe=null,Ae=null;return{setTest:function(jt){D||(jt?rt(i.STENCIL_TEST):Ct(i.STENCIL_TEST))},setMask:function(jt){it!==jt&&!D&&(i.stencilMask(jt),it=jt)},setFunc:function(jt,tn,_n){(V!==jt||$!==tn||lt!==_n)&&(i.stencilFunc(jt,tn,_n),V=jt,$=tn,lt=_n)},setOp:function(jt,tn,_n){(at!==jt||Dt!==tn||fe!==_n)&&(i.stencilOp(jt,tn,_n),at=jt,Dt=tn,fe=_n)},setLocked:function(jt){D=jt},setClear:function(jt){Ae!==jt&&(i.clearStencil(jt),Ae=jt)},reset:function(){D=!1,it=null,V=null,$=null,lt=null,at=null,Dt=null,fe=null,Ae=null}}}let r=new e,o=new n,a=new s,c=new WeakMap,u=new WeakMap,f={},p={},h=new WeakMap,d=[],g=null,_=!1,m=null,l=null,S=null,b=null,y=null,I=null,T=null,R=new Bt(0,0,0),A=0,E=!1,v=null,C=null,H=null,k=null,W=null,J=i.getParameter(i.MAX_COMBINED_TEXTURE_IMAGE_UNITS),X=!1,tt=0,G=i.getParameter(i.VERSION);G.indexOf("WebGL")!==-1?(tt=parseFloat(/^WebGL (\d)/.exec(G)[1]),X=tt>=1):G.indexOf("OpenGL ES")!==-1&&(tt=parseFloat(/^OpenGL ES (\d)/.exec(G)[1]),X=tt>=2);let st=null,dt={},bt=i.getParameter(i.SCISSOR_BOX),zt=i.getParameter(i.VIEWPORT),ee=new pe().fromArray(bt),Y=new pe().fromArray(zt);function et(D,it,V,$){let lt=new Uint8Array(4),at=i.createTexture();i.bindTexture(D,at),i.texParameteri(D,i.TEXTURE_MIN_FILTER,i.NEAREST),i.texParameteri(D,i.TEXTURE_MAG_FILTER,i.NEAREST);for(let Dt=0;Dt<V;Dt++)D===i.TEXTURE_3D||D===i.TEXTURE_2D_ARRAY?i.texImage3D(it,0,i.RGBA,1,1,$,0,i.RGBA,i.UNSIGNED_BYTE,lt):i.texImage2D(it+Dt,0,i.RGBA,1,1,0,i.RGBA,i.UNSIGNED_BYTE,lt);return at}let Mt={};Mt[i.TEXTURE_2D]=et(i.TEXTURE_2D,i.TEXTURE_2D,1),Mt[i.TEXTURE_CUBE_MAP]=et(i.TEXTURE_CUBE_MAP,i.TEXTURE_CUBE_MAP_POSITIVE_X,6),Mt[i.TEXTURE_2D_ARRAY]=et(i.TEXTURE_2D_ARRAY,i.TEXTURE_2D_ARRAY,1,1),Mt[i.TEXTURE_3D]=et(i.TEXTURE_3D,i.TEXTURE_3D,1,1),r.setClear(0,0,0,1),o.setClear(1),a.setClear(0),rt(i.DEPTH_TEST),o.setFunc(Zi),Ht(!1),Vt(ul),rt(i.CULL_FACE),N(Yn);function rt(D){f[D]!==!0&&(i.enable(D),f[D]=!0)}function Ct(D){f[D]!==!1&&(i.disable(D),f[D]=!1)}function Lt(D,it){return p[D]!==it?(i.bindFramebuffer(D,it),p[D]=it,D===i.DRAW_FRAMEBUFFER&&(p[i.FRAMEBUFFER]=it),D===i.FRAMEBUFFER&&(p[i.DRAW_FRAMEBUFFER]=it),!0):!1}function kt(D,it){let V=d,$=!1;if(D){V=h.get(it),V===void 0&&(V=[],h.set(it,V));let lt=D.textures;if(V.length!==lt.length||V[0]!==i.COLOR_ATTACHMENT0){for(let at=0,Dt=lt.length;at<Dt;at++)V[at]=i.COLOR_ATTACHMENT0+at;V.length=lt.length,$=!0}}else V[0]!==i.BACK&&(V[0]=i.BACK,$=!0);$&&i.drawBuffers(V)}function de(D){return g!==D?(i.useProgram(D),g=D,!0):!1}let Wt={[ci]:i.FUNC_ADD,[Uu]:i.FUNC_SUBTRACT,[Fu]:i.FUNC_REVERSE_SUBTRACT};Wt[Nu]=i.MIN,Wt[Ou]=i.MAX;let ge={[Bu]:i.ZERO,[zu]:i.ONE,[ku]:i.SRC_COLOR,[ia]:i.SRC_ALPHA,[qu]:i.SRC_ALPHA_SATURATE,[Wu]:i.DST_COLOR,[Vu]:i.DST_ALPHA,[Hu]:i.ONE_MINUS_SRC_COLOR,[sa]:i.ONE_MINUS_SRC_ALPHA,[Xu]:i.ONE_MINUS_DST_COLOR,[Gu]:i.ONE_MINUS_DST_ALPHA,[Yu]:i.CONSTANT_COLOR,[Zu]:i.ONE_MINUS_CONSTANT_COLOR,[$u]:i.CONSTANT_ALPHA,[Ju]:i.ONE_MINUS_CONSTANT_ALPHA};function N(D,it,V,$,lt,at,Dt,fe,Ae,jt){if(D===Yn){_===!0&&(Ct(i.BLEND),_=!1);return}if(_===!1&&(rt(i.BLEND),_=!0),D!==Du){if(D!==m||jt!==E){if((l!==ci||y!==ci)&&(i.blendEquation(i.FUNC_ADD),l=ci,y=ci),jt)switch(D){case Wi:i.blendFuncSeparate(i.ONE,i.ONE_MINUS_SRC_ALPHA,i.ONE,i.ONE_MINUS_SRC_ALPHA);break;case dl:i.blendFunc(i.ONE,i.ONE);break;case fl:i.blendFuncSeparate(i.ZERO,i.ONE_MINUS_SRC_COLOR,i.ZERO,i.ONE);break;case pl:i.blendFuncSeparate(i.ZERO,i.SRC_COLOR,i.ZERO,i.SRC_ALPHA);break;default:console.error("THREE.WebGLState: Invalid blending: ",D);break}else switch(D){case Wi:i.blendFuncSeparate(i.SRC_ALPHA,i.ONE_MINUS_SRC_ALPHA,i.ONE,i.ONE_MINUS_SRC_ALPHA);break;case dl:i.blendFunc(i.SRC_ALPHA,i.ONE);break;case fl:i.blendFuncSeparate(i.ZERO,i.ONE_MINUS_SRC_COLOR,i.ZERO,i.ONE);break;case pl:i.blendFunc(i.ZERO,i.SRC_COLOR);break;default:console.error("THREE.WebGLState: Invalid blending: ",D);break}S=null,b=null,I=null,T=null,R.set(0,0,0),A=0,m=D,E=jt}return}lt=lt||it,at=at||V,Dt=Dt||$,(it!==l||lt!==y)&&(i.blendEquationSeparate(Wt[it],Wt[lt]),l=it,y=lt),(V!==S||$!==b||at!==I||Dt!==T)&&(i.blendFuncSeparate(ge[V],ge[$],ge[at],ge[Dt]),S=V,b=$,I=at,T=Dt),(fe.equals(R)===!1||Ae!==A)&&(i.blendColor(fe.r,fe.g,fe.b,Ae),R.copy(fe),A=Ae),m=D,E=!1}function Ye(D,it){D.side===ze?Ct(i.CULL_FACE):rt(i.CULL_FACE);let V=D.side===ke;it&&(V=!V),Ht(V),D.blending===Wi&&D.transparent===!1?N(Yn):N(D.blending,D.blendEquation,D.blendSrc,D.blendDst,D.blendEquationAlpha,D.blendSrcAlpha,D.blendDstAlpha,D.blendColor,D.blendAlpha,D.premultipliedAlpha),o.setFunc(D.depthFunc),o.setTest(D.depthTest),o.setMask(D.depthWrite),r.setMask(D.colorWrite);let $=D.stencilWrite;a.setTest($),$&&(a.setMask(D.stencilWriteMask),a.setFunc(D.stencilFunc,D.stencilRef,D.stencilFuncMask),a.setOp(D.stencilFail,D.stencilZFail,D.stencilZPass)),oe(D.polygonOffset,D.polygonOffsetFactor,D.polygonOffsetUnits),D.alphaToCoverage===!0?rt(i.SAMPLE_ALPHA_TO_COVERAGE):Ct(i.SAMPLE_ALPHA_TO_COVERAGE)}function Ht(D){v!==D&&(D?i.frontFace(i.CW):i.frontFace(i.CCW),v=D)}function Vt(D){D!==Iu?(rt(i.CULL_FACE),D!==C&&(D===ul?i.cullFace(i.BACK):D===Pu?i.cullFace(i.FRONT):i.cullFace(i.FRONT_AND_BACK))):Ct(i.CULL_FACE),C=D}function Tt(D){D!==H&&(X&&i.lineWidth(D),H=D)}function oe(D,it,V){D?(rt(i.POLYGON_OFFSET_FILL),(k!==it||W!==V)&&(i.polygonOffset(it,V),k=it,W=V)):Ct(i.POLYGON_OFFSET_FILL)}function wt(D){D?rt(i.SCISSOR_TEST):Ct(i.SCISSOR_TEST)}function w(D){D===void 0&&(D=i.TEXTURE0+J-1),st!==D&&(i.activeTexture(D),st=D)}function x(D,it,V){V===void 0&&(st===null?V=i.TEXTURE0+J-1:V=st);let $=dt[V];$===void 0&&($={type:void 0,texture:void 0},dt[V]=$),($.type!==D||$.texture!==it)&&(st!==V&&(i.activeTexture(V),st=V),i.bindTexture(D,it||Mt[D]),$.type=D,$.texture=it)}function O(){let D=dt[st];D!==void 0&&D.type!==void 0&&(i.bindTexture(D.type,null),D.type=void 0,D.texture=void 0)}function Z(){try{i.compressedTexImage2D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function K(){try{i.compressedTexImage3D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function q(){try{i.texSubImage2D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function St(){try{i.texSubImage3D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function ot(){try{i.compressedTexSubImage2D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function ft(){try{i.compressedTexSubImage3D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function Xt(){try{i.texStorage2D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function j(){try{i.texStorage3D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function pt(){try{i.texImage2D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function Rt(){try{i.texImage3D.apply(i,arguments)}catch(D){console.error("THREE.WebGLState:",D)}}function It(D){ee.equals(D)===!1&&(i.scissor(D.x,D.y,D.z,D.w),ee.copy(D))}function mt(D){Y.equals(D)===!1&&(i.viewport(D.x,D.y,D.z,D.w),Y.copy(D))}function Gt(D,it){let V=u.get(it);V===void 0&&(V=new WeakMap,u.set(it,V));let $=V.get(D);$===void 0&&($=i.getUniformBlockIndex(it,D.name),V.set(D,$))}function Nt(D,it){let $=u.get(it).get(D);c.get(it)!==$&&(i.uniformBlockBinding(it,$,D.__bindingPointIndex),c.set(it,$))}function se(){i.disable(i.BLEND),i.disable(i.CULL_FACE),i.disable(i.DEPTH_TEST),i.disable(i.POLYGON_OFFSET_FILL),i.disable(i.SCISSOR_TEST),i.disable(i.STENCIL_TEST),i.disable(i.SAMPLE_ALPHA_TO_COVERAGE),i.blendEquation(i.FUNC_ADD),i.blendFunc(i.ONE,i.ZERO),i.blendFuncSeparate(i.ONE,i.ZERO,i.ONE,i.ZERO),i.blendColor(0,0,0,0),i.colorMask(!0,!0,!0,!0),i.clearColor(0,0,0,0),i.depthMask(!0),i.depthFunc(i.LESS),o.setReversed(!1),i.clearDepth(1),i.stencilMask(4294967295),i.stencilFunc(i.ALWAYS,0,4294967295),i.stencilOp(i.KEEP,i.KEEP,i.KEEP),i.clearStencil(0),i.cullFace(i.BACK),i.frontFace(i.CCW),i.polygonOffset(0,0),i.activeTexture(i.TEXTURE0),i.bindFramebuffer(i.FRAMEBUFFER,null),i.bindFramebuffer(i.DRAW_FRAMEBUFFER,null),i.bindFramebuffer(i.READ_FRAMEBUFFER,null),i.useProgram(null),i.lineWidth(1),i.scissor(0,0,i.canvas.width,i.canvas.height),i.viewport(0,0,i.canvas.width,i.canvas.height),f={},st=null,dt={},p={},h=new WeakMap,d=[],g=null,_=!1,m=null,l=null,S=null,b=null,y=null,I=null,T=null,R=new Bt(0,0,0),A=0,E=!1,v=null,C=null,H=null,k=null,W=null,ee.set(0,0,i.canvas.width,i.canvas.height),Y.set(0,0,i.canvas.width,i.canvas.height),r.reset(),o.reset(),a.reset()}return{buffers:{color:r,depth:o,stencil:a},enable:rt,disable:Ct,bindFramebuffer:Lt,drawBuffers:kt,useProgram:de,setBlending:N,setMaterial:Ye,setFlipSided:Ht,setCullFace:Vt,setLineWidth:Tt,setPolygonOffset:oe,setScissorTest:wt,activeTexture:w,bindTexture:x,unbindTexture:O,compressedTexImage2D:Z,compressedTexImage3D:K,texImage2D:pt,texImage3D:Rt,updateUBOMapping:Gt,uniformBlockBinding:Nt,texStorage2D:Xt,texStorage3D:j,texSubImage2D:q,texSubImage3D:St,compressedTexSubImage2D:ot,compressedTexSubImage3D:ft,scissor:It,viewport:mt,reset:se}}function lh(i,t,e,n){let s=a0(n);switch(e){case Eh:return i*t;case wh:return i*t;case Th:return i*t*2;case Ah:return i*t/s.components*s.byteLength;case Lc:return i*t/s.components*s.byteLength;case Rh:return i*t*2/s.components*s.byteLength;case Dc:return i*t*2/s.components*s.byteLength;case bh:return i*t*3/s.components*s.byteLength;case on:return i*t*4/s.components*s.byteLength;case Uc:return i*t*4/s.components*s.byteLength;case vr:case Mr:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*8;case Sr:case Er:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*16;case ga:case xa:return Math.max(i,16)*Math.max(t,8)/4;case ma:case _a:return Math.max(i,8)*Math.max(t,8)/2;case ya:case va:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*8;case Ma:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*16;case Sa:return Math.floor((i+3)/4)*Math.floor((t+3)/4)*16;case Ea:return Math.floor((i+4)/5)*Math.floor((t+3)/4)*16;case ba:return Math.floor((i+4)/5)*Math.floor((t+4)/5)*16;case wa:return Math.floor((i+5)/6)*Math.floor((t+4)/5)*16;case Ta:return Math.floor((i+5)/6)*Math.floor((t+5)/6)*16;case Aa:return Math.floor((i+7)/8)*Math.floor((t+4)/5)*16;case Ra:return Math.floor((i+7)/8)*Math.floor((t+5)/6)*16;case Ca:return Math.floor((i+7)/8)*Math.floor((t+7)/8)*16;case Ia:return Math.floor((i+9)/10)*Math.floor((t+4)/5)*16;case Pa:return Math.floor((i+9)/10)*Math.floor((t+5)/6)*16;case La:return Math.floor((i+9)/10)*Math.floor((t+7)/8)*16;case Da:return Math.floor((i+9)/10)*Math.floor((t+9)/10)*16;case Ua:return Math.floor((i+11)/12)*Math.floor((t+9)/10)*16;case Fa:return Math.floor((i+11)/12)*Math.floor((t+11)/12)*16;case br:case Na:case Oa:return Math.ceil(i/4)*Math.ceil(t/4)*16;case Ch:case Ba:return Math.ceil(i/4)*Math.ceil(t/4)*8;case za:case ka:return Math.ceil(i/4)*Math.ceil(t/4)*16}throw new Error(`Unable to determine texture byte length for ${e} format.`)}function a0(i){switch(i){case In:case vh:return{byteLength:1,components:1};case Ts:case Mh:case Fs:return{byteLength:2,components:1};case Ic:case Pc:return{byteLength:2,components:4};case di:case Cc:case Tn:return{byteLength:4,components:1};case Sh:return{byteLength:4,components:3}}throw new Error(`Unknown texture type ${i}.`)}function c0(i,t,e,n,s,r,o){let a=t.has("WEBGL_multisampled_render_to_texture")?t.get("WEBGL_multisampled_render_to_texture"):null,c=typeof navigator>"u"?!1:/OculusBrowser/g.test(navigator.userAgent),u=new ut,f=new WeakMap,p,h=new WeakMap,d=!1;try{d=typeof OffscreenCanvas<"u"&&new OffscreenCanvas(1,1).getContext("2d")!==null}catch{}function g(w,x){return d?new OffscreenCanvas(w,x):Ar("canvas")}function _(w,x,O){let Z=1,K=wt(w);if((K.width>O||K.height>O)&&(Z=O/Math.max(K.width,K.height)),Z<1)if(typeof HTMLImageElement<"u"&&w instanceof HTMLImageElement||typeof HTMLCanvasElement<"u"&&w instanceof HTMLCanvasElement||typeof ImageBitmap<"u"&&w instanceof ImageBitmap||typeof VideoFrame<"u"&&w instanceof VideoFrame){let q=Math.floor(Z*K.width),St=Math.floor(Z*K.height);p===void 0&&(p=g(q,St));let ot=x?g(q,St):p;return ot.width=q,ot.height=St,ot.getContext("2d").drawImage(w,0,0,q,St),console.warn("THREE.WebGLRenderer: Texture has been resized from ("+K.width+"x"+K.height+") to ("+q+"x"+St+")."),ot}else return"data"in w&&console.warn("THREE.WebGLRenderer: Image in DataTexture is too big ("+K.width+"x"+K.height+")."),w;return w}function m(w){return w.generateMipmaps}function l(w){i.generateMipmap(w)}function S(w){return w.isWebGLCubeRenderTarget?i.TEXTURE_CUBE_MAP:w.isWebGL3DRenderTarget?i.TEXTURE_3D:w.isWebGLArrayRenderTarget||w.isCompressedArrayTexture?i.TEXTURE_2D_ARRAY:i.TEXTURE_2D}function b(w,x,O,Z,K=!1){if(w!==null){if(i[w]!==void 0)return i[w];console.warn("THREE.WebGLRenderer: Attempt to use non-existing WebGL internal format '"+w+"'")}let q=x;if(x===i.RED&&(O===i.FLOAT&&(q=i.R32F),O===i.HALF_FLOAT&&(q=i.R16F),O===i.UNSIGNED_BYTE&&(q=i.R8)),x===i.RED_INTEGER&&(O===i.UNSIGNED_BYTE&&(q=i.R8UI),O===i.UNSIGNED_SHORT&&(q=i.R16UI),O===i.UNSIGNED_INT&&(q=i.R32UI),O===i.BYTE&&(q=i.R8I),O===i.SHORT&&(q=i.R16I),O===i.INT&&(q=i.R32I)),x===i.RG&&(O===i.FLOAT&&(q=i.RG32F),O===i.HALF_FLOAT&&(q=i.RG16F),O===i.UNSIGNED_BYTE&&(q=i.RG8)),x===i.RG_INTEGER&&(O===i.UNSIGNED_BYTE&&(q=i.RG8UI),O===i.UNSIGNED_SHORT&&(q=i.RG16UI),O===i.UNSIGNED_INT&&(q=i.RG32UI),O===i.BYTE&&(q=i.RG8I),O===i.SHORT&&(q=i.RG16I),O===i.INT&&(q=i.RG32I)),x===i.RGB_INTEGER&&(O===i.UNSIGNED_BYTE&&(q=i.RGB8UI),O===i.UNSIGNED_SHORT&&(q=i.RGB16UI),O===i.UNSIGNED_INT&&(q=i.RGB32UI),O===i.BYTE&&(q=i.RGB8I),O===i.SHORT&&(q=i.RGB16I),O===i.INT&&(q=i.RGB32I)),x===i.RGBA_INTEGER&&(O===i.UNSIGNED_BYTE&&(q=i.RGBA8UI),O===i.UNSIGNED_SHORT&&(q=i.RGBA16UI),O===i.UNSIGNED_INT&&(q=i.RGBA32UI),O===i.BYTE&&(q=i.RGBA8I),O===i.SHORT&&(q=i.RGBA16I),O===i.INT&&(q=i.RGBA32I)),x===i.RGB&&O===i.UNSIGNED_INT_5_9_9_9_REV&&(q=i.RGB9_E5),x===i.RGBA){let St=K?no:qt.getTransfer(Z);O===i.FLOAT&&(q=i.RGBA32F),O===i.HALF_FLOAT&&(q=i.RGBA16F),O===i.UNSIGNED_BYTE&&(q=St===Qt?i.SRGB8_ALPHA8:i.RGBA8),O===i.UNSIGNED_SHORT_4_4_4_4&&(q=i.RGBA4),O===i.UNSIGNED_SHORT_5_5_5_1&&(q=i.RGB5_A1)}return(q===i.R16F||q===i.R32F||q===i.RG16F||q===i.RG32F||q===i.RGBA16F||q===i.RGBA32F)&&t.get("EXT_color_buffer_float"),q}function y(w,x){let O;return w?x===null||x===di||x===Ki?O=i.DEPTH24_STENCIL8:x===Tn?O=i.DEPTH32F_STENCIL8:x===Ts&&(O=i.DEPTH24_STENCIL8,console.warn("DepthTexture: 16 bit depth attachment is not supported with stencil. Using 24-bit attachment.")):x===null||x===di||x===Ki?O=i.DEPTH_COMPONENT24:x===Tn?O=i.DEPTH_COMPONENT32F:x===Ts&&(O=i.DEPTH_COMPONENT16),O}function I(w,x){return m(w)===!0||w.isFramebufferTexture&&w.minFilter!==an&&w.minFilter!==pn?Math.log2(Math.max(x.width,x.height))+1:w.mipmaps!==void 0&&w.mipmaps.length>0?w.mipmaps.length:w.isCompressedTexture&&Array.isArray(w.image)?x.mipmaps.length:1}function T(w){let x=w.target;x.removeEventListener("dispose",T),A(x),x.isVideoTexture&&f.delete(x)}function R(w){let x=w.target;x.removeEventListener("dispose",R),v(x)}function A(w){let x=n.get(w);if(x.__webglInit===void 0)return;let O=w.source,Z=h.get(O);if(Z){let K=Z[x.__cacheKey];K.usedTimes--,K.usedTimes===0&&E(w),Object.keys(Z).length===0&&h.delete(O)}n.remove(w)}function E(w){let x=n.get(w);i.deleteTexture(x.__webglTexture);let O=w.source,Z=h.get(O);delete Z[x.__cacheKey],o.memory.textures--}function v(w){let x=n.get(w);if(w.depthTexture&&(w.depthTexture.dispose(),n.remove(w.depthTexture)),w.isWebGLCubeRenderTarget)for(let Z=0;Z<6;Z++){if(Array.isArray(x.__webglFramebuffer[Z]))for(let K=0;K<x.__webglFramebuffer[Z].length;K++)i.deleteFramebuffer(x.__webglFramebuffer[Z][K]);else i.deleteFramebuffer(x.__webglFramebuffer[Z]);x.__webglDepthbuffer&&i.deleteRenderbuffer(x.__webglDepthbuffer[Z])}else{if(Array.isArray(x.__webglFramebuffer))for(let Z=0;Z<x.__webglFramebuffer.length;Z++)i.deleteFramebuffer(x.__webglFramebuffer[Z]);else i.deleteFramebuffer(x.__webglFramebuffer);if(x.__webglDepthbuffer&&i.deleteRenderbuffer(x.__webglDepthbuffer),x.__webglMultisampledFramebuffer&&i.deleteFramebuffer(x.__webglMultisampledFramebuffer),x.__webglColorRenderbuffer)for(let Z=0;Z<x.__webglColorRenderbuffer.length;Z++)x.__webglColorRenderbuffer[Z]&&i.deleteRenderbuffer(x.__webglColorRenderbuffer[Z]);x.__webglDepthRenderbuffer&&i.deleteRenderbuffer(x.__webglDepthRenderbuffer)}let O=w.textures;for(let Z=0,K=O.length;Z<K;Z++){let q=n.get(O[Z]);q.__webglTexture&&(i.deleteTexture(q.__webglTexture),o.memory.textures--),n.remove(O[Z])}n.remove(w)}let C=0;function H(){C=0}function k(){let w=C;return w>=s.maxTextures&&console.warn("THREE.WebGLTextures: Trying to use "+w+" texture units while this GPU supports only "+s.maxTextures),C+=1,w}function W(w){let x=[];return x.push(w.wrapS),x.push(w.wrapT),x.push(w.wrapR||0),x.push(w.magFilter),x.push(w.minFilter),x.push(w.anisotropy),x.push(w.internalFormat),x.push(w.format),x.push(w.type),x.push(w.generateMipmaps),x.push(w.premultiplyAlpha),x.push(w.flipY),x.push(w.unpackAlignment),x.push(w.colorSpace),x.join()}function J(w,x){let O=n.get(w);if(w.isVideoTexture&&Tt(w),w.isRenderTargetTexture===!1&&w.version>0&&O.__version!==w.version){let Z=w.image;if(Z===null)console.warn("THREE.WebGLRenderer: Texture marked for update but no image data found.");else if(Z.complete===!1)console.warn("THREE.WebGLRenderer: Texture marked for update but image is incomplete");else{Y(O,w,x);return}}e.bindTexture(i.TEXTURE_2D,O.__webglTexture,i.TEXTURE0+x)}function X(w,x){let O=n.get(w);if(w.version>0&&O.__version!==w.version){Y(O,w,x);return}e.bindTexture(i.TEXTURE_2D_ARRAY,O.__webglTexture,i.TEXTURE0+x)}function tt(w,x){let O=n.get(w);if(w.version>0&&O.__version!==w.version){Y(O,w,x);return}e.bindTexture(i.TEXTURE_3D,O.__webglTexture,i.TEXTURE0+x)}function G(w,x){let O=n.get(w);if(w.version>0&&O.__version!==w.version){et(O,w,x);return}e.bindTexture(i.TEXTURE_CUBE_MAP,O.__webglTexture,i.TEXTURE0+x)}let st={[Cn]:i.REPEAT,[hi]:i.CLAMP_TO_EDGE,[pa]:i.MIRRORED_REPEAT},dt={[an]:i.NEAREST,[od]:i.NEAREST_MIPMAP_NEAREST,[qs]:i.NEAREST_MIPMAP_LINEAR,[pn]:i.LINEAR,[So]:i.LINEAR_MIPMAP_NEAREST,[ui]:i.LINEAR_MIPMAP_LINEAR},bt={[hd]:i.NEVER,[gd]:i.ALWAYS,[ud]:i.LESS,[Ph]:i.LEQUAL,[dd]:i.EQUAL,[md]:i.GEQUAL,[fd]:i.GREATER,[pd]:i.NOTEQUAL};function zt(w,x){if(x.type===Tn&&t.has("OES_texture_float_linear")===!1&&(x.magFilter===pn||x.magFilter===So||x.magFilter===qs||x.magFilter===ui||x.minFilter===pn||x.minFilter===So||x.minFilter===qs||x.minFilter===ui)&&console.warn("THREE.WebGLRenderer: Unable to use linear filtering with floating point textures. OES_texture_float_linear not supported on this device."),i.texParameteri(w,i.TEXTURE_WRAP_S,st[x.wrapS]),i.texParameteri(w,i.TEXTURE_WRAP_T,st[x.wrapT]),(w===i.TEXTURE_3D||w===i.TEXTURE_2D_ARRAY)&&i.texParameteri(w,i.TEXTURE_WRAP_R,st[x.wrapR]),i.texParameteri(w,i.TEXTURE_MAG_FILTER,dt[x.magFilter]),i.texParameteri(w,i.TEXTURE_MIN_FILTER,dt[x.minFilter]),x.compareFunction&&(i.texParameteri(w,i.TEXTURE_COMPARE_MODE,i.COMPARE_REF_TO_TEXTURE),i.texParameteri(w,i.TEXTURE_COMPARE_FUNC,bt[x.compareFunction])),t.has("EXT_texture_filter_anisotropic")===!0){if(x.magFilter===an||x.minFilter!==qs&&x.minFilter!==ui||x.type===Tn&&t.has("OES_texture_float_linear")===!1)return;if(x.anisotropy>1||n.get(x).__currentAnisotropy){let O=t.get("EXT_texture_filter_anisotropic");i.texParameterf(w,O.TEXTURE_MAX_ANISOTROPY_EXT,Math.min(x.anisotropy,s.getMaxAnisotropy())),n.get(x).__currentAnisotropy=x.anisotropy}}}function ee(w,x){let O=!1;w.__webglInit===void 0&&(w.__webglInit=!0,x.addEventListener("dispose",T));let Z=x.source,K=h.get(Z);K===void 0&&(K={},h.set(Z,K));let q=W(x);if(q!==w.__cacheKey){K[q]===void 0&&(K[q]={texture:i.createTexture(),usedTimes:0},o.memory.textures++,O=!0),K[q].usedTimes++;let St=K[w.__cacheKey];St!==void 0&&(K[w.__cacheKey].usedTimes--,St.usedTimes===0&&E(x)),w.__cacheKey=q,w.__webglTexture=K[q].texture}return O}function Y(w,x,O){let Z=i.TEXTURE_2D;(x.isDataArrayTexture||x.isCompressedArrayTexture)&&(Z=i.TEXTURE_2D_ARRAY),x.isData3DTexture&&(Z=i.TEXTURE_3D);let K=ee(w,x),q=x.source;e.bindTexture(Z,w.__webglTexture,i.TEXTURE0+O);let St=n.get(q);if(q.version!==St.__version||K===!0){e.activeTexture(i.TEXTURE0+O);let ot=qt.getPrimaries(qt.workingColorSpace),ft=x.colorSpace===Xn?null:qt.getPrimaries(x.colorSpace),Xt=x.colorSpace===Xn||ot===ft?i.NONE:i.BROWSER_DEFAULT_WEBGL;i.pixelStorei(i.UNPACK_FLIP_Y_WEBGL,x.flipY),i.pixelStorei(i.UNPACK_PREMULTIPLY_ALPHA_WEBGL,x.premultiplyAlpha),i.pixelStorei(i.UNPACK_ALIGNMENT,x.unpackAlignment),i.pixelStorei(i.UNPACK_COLORSPACE_CONVERSION_WEBGL,Xt);let j=_(x.image,!1,s.maxTextureSize);j=oe(x,j);let pt=r.convert(x.format,x.colorSpace),Rt=r.convert(x.type),It=b(x.internalFormat,pt,Rt,x.colorSpace,x.isVideoTexture);zt(Z,x);let mt,Gt=x.mipmaps,Nt=x.isVideoTexture!==!0,se=St.__version===void 0||K===!0,D=q.dataReady,it=I(x,j);if(x.isDepthTexture)It=y(x.format===ji,x.type),se&&(Nt?e.texStorage2D(i.TEXTURE_2D,1,It,j.width,j.height):e.texImage2D(i.TEXTURE_2D,0,It,j.width,j.height,0,pt,Rt,null));else if(x.isDataTexture)if(Gt.length>0){Nt&&se&&e.texStorage2D(i.TEXTURE_2D,it,It,Gt[0].width,Gt[0].height);for(let V=0,$=Gt.length;V<$;V++)mt=Gt[V],Nt?D&&e.texSubImage2D(i.TEXTURE_2D,V,0,0,mt.width,mt.height,pt,Rt,mt.data):e.texImage2D(i.TEXTURE_2D,V,It,mt.width,mt.height,0,pt,Rt,mt.data);x.generateMipmaps=!1}else Nt?(se&&e.texStorage2D(i.TEXTURE_2D,it,It,j.width,j.height),D&&e.texSubImage2D(i.TEXTURE_2D,0,0,0,j.width,j.height,pt,Rt,j.data)):e.texImage2D(i.TEXTURE_2D,0,It,j.width,j.height,0,pt,Rt,j.data);else if(x.isCompressedTexture)if(x.isCompressedArrayTexture){Nt&&se&&e.texStorage3D(i.TEXTURE_2D_ARRAY,it,It,Gt[0].width,Gt[0].height,j.depth);for(let V=0,$=Gt.length;V<$;V++)if(mt=Gt[V],x.format!==on)if(pt!==null)if(Nt){if(D)if(x.layerUpdates.size>0){let lt=lh(mt.width,mt.height,x.format,x.type);for(let at of x.layerUpdates){let Dt=mt.data.subarray(at*lt/mt.data.BYTES_PER_ELEMENT,(at+1)*lt/mt.data.BYTES_PER_ELEMENT);e.compressedTexSubImage3D(i.TEXTURE_2D_ARRAY,V,0,0,at,mt.width,mt.height,1,pt,Dt)}x.clearLayerUpdates()}else e.compressedTexSubImage3D(i.TEXTURE_2D_ARRAY,V,0,0,0,mt.width,mt.height,j.depth,pt,mt.data)}else e.compressedTexImage3D(i.TEXTURE_2D_ARRAY,V,It,mt.width,mt.height,j.depth,0,mt.data,0,0);else console.warn("THREE.WebGLRenderer: Attempt to load unsupported compressed texture format in .uploadTexture()");else Nt?D&&e.texSubImage3D(i.TEXTURE_2D_ARRAY,V,0,0,0,mt.width,mt.height,j.depth,pt,Rt,mt.data):e.texImage3D(i.TEXTURE_2D_ARRAY,V,It,mt.width,mt.height,j.depth,0,pt,Rt,mt.data)}else{Nt&&se&&e.texStorage2D(i.TEXTURE_2D,it,It,Gt[0].width,Gt[0].height);for(let V=0,$=Gt.length;V<$;V++)mt=Gt[V],x.format!==on?pt!==null?Nt?D&&e.compressedTexSubImage2D(i.TEXTURE_2D,V,0,0,mt.width,mt.height,pt,mt.data):e.compressedTexImage2D(i.TEXTURE_2D,V,It,mt.width,mt.height,0,mt.data):console.warn("THREE.WebGLRenderer: Attempt to load unsupported compressed texture format in .uploadTexture()"):Nt?D&&e.texSubImage2D(i.TEXTURE_2D,V,0,0,mt.width,mt.height,pt,Rt,mt.data):e.texImage2D(i.TEXTURE_2D,V,It,mt.width,mt.height,0,pt,Rt,mt.data)}else if(x.isDataArrayTexture)if(Nt){if(se&&e.texStorage3D(i.TEXTURE_2D_ARRAY,it,It,j.width,j.height,j.depth),D)if(x.layerUpdates.size>0){let V=lh(j.width,j.height,x.format,x.type);for(let $ of x.layerUpdates){let lt=j.data.subarray($*V/j.data.BYTES_PER_ELEMENT,($+1)*V/j.data.BYTES_PER_ELEMENT);e.texSubImage3D(i.TEXTURE_2D_ARRAY,0,0,0,$,j.width,j.height,1,pt,Rt,lt)}x.clearLayerUpdates()}else e.texSubImage3D(i.TEXTURE_2D_ARRAY,0,0,0,0,j.width,j.height,j.depth,pt,Rt,j.data)}else e.texImage3D(i.TEXTURE_2D_ARRAY,0,It,j.width,j.height,j.depth,0,pt,Rt,j.data);else if(x.isData3DTexture)Nt?(se&&e.texStorage3D(i.TEXTURE_3D,it,It,j.width,j.height,j.depth),D&&e.texSubImage3D(i.TEXTURE_3D,0,0,0,0,j.width,j.height,j.depth,pt,Rt,j.data)):e.texImage3D(i.TEXTURE_3D,0,It,j.width,j.height,j.depth,0,pt,Rt,j.data);else if(x.isFramebufferTexture){if(se)if(Nt)e.texStorage2D(i.TEXTURE_2D,it,It,j.width,j.height);else{let V=j.width,$=j.height;for(let lt=0;lt<it;lt++)e.texImage2D(i.TEXTURE_2D,lt,It,V,$,0,pt,Rt,null),V>>=1,$>>=1}}else if(Gt.length>0){if(Nt&&se){let V=wt(Gt[0]);e.texStorage2D(i.TEXTURE_2D,it,It,V.width,V.height)}for(let V=0,$=Gt.length;V<$;V++)mt=Gt[V],Nt?D&&e.texSubImage2D(i.TEXTURE_2D,V,0,0,pt,Rt,mt):e.texImage2D(i.TEXTURE_2D,V,It,pt,Rt,mt);x.generateMipmaps=!1}else if(Nt){if(se){let V=wt(j);e.texStorage2D(i.TEXTURE_2D,it,It,V.width,V.height)}D&&e.texSubImage2D(i.TEXTURE_2D,0,0,0,pt,Rt,j)}else e.texImage2D(i.TEXTURE_2D,0,It,pt,Rt,j);m(x)&&l(Z),St.__version=q.version,x.onUpdate&&x.onUpdate(x)}w.__version=x.version}function et(w,x,O){if(x.image.length!==6)return;let Z=ee(w,x),K=x.source;e.bindTexture(i.TEXTURE_CUBE_MAP,w.__webglTexture,i.TEXTURE0+O);let q=n.get(K);if(K.version!==q.__version||Z===!0){e.activeTexture(i.TEXTURE0+O);let St=qt.getPrimaries(qt.workingColorSpace),ot=x.colorSpace===Xn?null:qt.getPrimaries(x.colorSpace),ft=x.colorSpace===Xn||St===ot?i.NONE:i.BROWSER_DEFAULT_WEBGL;i.pixelStorei(i.UNPACK_FLIP_Y_WEBGL,x.flipY),i.pixelStorei(i.UNPACK_PREMULTIPLY_ALPHA_WEBGL,x.premultiplyAlpha),i.pixelStorei(i.UNPACK_ALIGNMENT,x.unpackAlignment),i.pixelStorei(i.UNPACK_COLORSPACE_CONVERSION_WEBGL,ft);let Xt=x.isCompressedTexture||x.image[0].isCompressedTexture,j=x.image[0]&&x.image[0].isDataTexture,pt=[];for(let $=0;$<6;$++)!Xt&&!j?pt[$]=_(x.image[$],!0,s.maxCubemapSize):pt[$]=j?x.image[$].image:x.image[$],pt[$]=oe(x,pt[$]);let Rt=pt[0],It=r.convert(x.format,x.colorSpace),mt=r.convert(x.type),Gt=b(x.internalFormat,It,mt,x.colorSpace),Nt=x.isVideoTexture!==!0,se=q.__version===void 0||Z===!0,D=K.dataReady,it=I(x,Rt);zt(i.TEXTURE_CUBE_MAP,x);let V;if(Xt){Nt&&se&&e.texStorage2D(i.TEXTURE_CUBE_MAP,it,Gt,Rt.width,Rt.height);for(let $=0;$<6;$++){V=pt[$].mipmaps;for(let lt=0;lt<V.length;lt++){let at=V[lt];x.format!==on?It!==null?Nt?D&&e.compressedTexSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,lt,0,0,at.width,at.height,It,at.data):e.compressedTexImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,lt,Gt,at.width,at.height,0,at.data):console.warn("THREE.WebGLRenderer: Attempt to load unsupported compressed texture format in .setTextureCube()"):Nt?D&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,lt,0,0,at.width,at.height,It,mt,at.data):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,lt,Gt,at.width,at.height,0,It,mt,at.data)}}}else{if(V=x.mipmaps,Nt&&se){V.length>0&&it++;let $=wt(pt[0]);e.texStorage2D(i.TEXTURE_CUBE_MAP,it,Gt,$.width,$.height)}for(let $=0;$<6;$++)if(j){Nt?D&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,0,0,0,pt[$].width,pt[$].height,It,mt,pt[$].data):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,0,Gt,pt[$].width,pt[$].height,0,It,mt,pt[$].data);for(let lt=0;lt<V.length;lt++){let Dt=V[lt].image[$].image;Nt?D&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,lt+1,0,0,Dt.width,Dt.height,It,mt,Dt.data):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,lt+1,Gt,Dt.width,Dt.height,0,It,mt,Dt.data)}}else{Nt?D&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,0,0,0,It,mt,pt[$]):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,0,Gt,It,mt,pt[$]);for(let lt=0;lt<V.length;lt++){let at=V[lt];Nt?D&&e.texSubImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,lt+1,0,0,It,mt,at.image[$]):e.texImage2D(i.TEXTURE_CUBE_MAP_POSITIVE_X+$,lt+1,Gt,It,mt,at.image[$])}}}m(x)&&l(i.TEXTURE_CUBE_MAP),q.__version=K.version,x.onUpdate&&x.onUpdate(x)}w.__version=x.version}function Mt(w,x,O,Z,K,q){let St=r.convert(O.format,O.colorSpace),ot=r.convert(O.type),ft=b(O.internalFormat,St,ot,O.colorSpace),Xt=n.get(x),j=n.get(O);if(j.__renderTarget=x,!Xt.__hasExternalTextures){let pt=Math.max(1,x.width>>q),Rt=Math.max(1,x.height>>q);K===i.TEXTURE_3D||K===i.TEXTURE_2D_ARRAY?e.texImage3D(K,q,ft,pt,Rt,x.depth,0,St,ot,null):e.texImage2D(K,q,ft,pt,Rt,0,St,ot,null)}e.bindFramebuffer(i.FRAMEBUFFER,w),Vt(x)?a.framebufferTexture2DMultisampleEXT(i.FRAMEBUFFER,Z,K,j.__webglTexture,0,Ht(x)):(K===i.TEXTURE_2D||K>=i.TEXTURE_CUBE_MAP_POSITIVE_X&&K<=i.TEXTURE_CUBE_MAP_NEGATIVE_Z)&&i.framebufferTexture2D(i.FRAMEBUFFER,Z,K,j.__webglTexture,q),e.bindFramebuffer(i.FRAMEBUFFER,null)}function rt(w,x,O){if(i.bindRenderbuffer(i.RENDERBUFFER,w),x.depthBuffer){let Z=x.depthTexture,K=Z&&Z.isDepthTexture?Z.type:null,q=y(x.stencilBuffer,K),St=x.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT,ot=Ht(x);Vt(x)?a.renderbufferStorageMultisampleEXT(i.RENDERBUFFER,ot,q,x.width,x.height):O?i.renderbufferStorageMultisample(i.RENDERBUFFER,ot,q,x.width,x.height):i.renderbufferStorage(i.RENDERBUFFER,q,x.width,x.height),i.framebufferRenderbuffer(i.FRAMEBUFFER,St,i.RENDERBUFFER,w)}else{let Z=x.textures;for(let K=0;K<Z.length;K++){let q=Z[K],St=r.convert(q.format,q.colorSpace),ot=r.convert(q.type),ft=b(q.internalFormat,St,ot,q.colorSpace),Xt=Ht(x);O&&Vt(x)===!1?i.renderbufferStorageMultisample(i.RENDERBUFFER,Xt,ft,x.width,x.height):Vt(x)?a.renderbufferStorageMultisampleEXT(i.RENDERBUFFER,Xt,ft,x.width,x.height):i.renderbufferStorage(i.RENDERBUFFER,ft,x.width,x.height)}}i.bindRenderbuffer(i.RENDERBUFFER,null)}function Ct(w,x){if(x&&x.isWebGLCubeRenderTarget)throw new Error("Depth Texture with cube render targets is not supported");if(e.bindFramebuffer(i.FRAMEBUFFER,w),!(x.depthTexture&&x.depthTexture.isDepthTexture))throw new Error("renderTarget.depthTexture must be an instance of THREE.DepthTexture");let Z=n.get(x.depthTexture);Z.__renderTarget=x,(!Z.__webglTexture||x.depthTexture.image.width!==x.width||x.depthTexture.image.height!==x.height)&&(x.depthTexture.image.width=x.width,x.depthTexture.image.height=x.height,x.depthTexture.needsUpdate=!0),J(x.depthTexture,0);let K=Z.__webglTexture,q=Ht(x);if(x.depthTexture.format===Xi)Vt(x)?a.framebufferTexture2DMultisampleEXT(i.FRAMEBUFFER,i.DEPTH_ATTACHMENT,i.TEXTURE_2D,K,0,q):i.framebufferTexture2D(i.FRAMEBUFFER,i.DEPTH_ATTACHMENT,i.TEXTURE_2D,K,0);else if(x.depthTexture.format===ji)Vt(x)?a.framebufferTexture2DMultisampleEXT(i.FRAMEBUFFER,i.DEPTH_STENCIL_ATTACHMENT,i.TEXTURE_2D,K,0,q):i.framebufferTexture2D(i.FRAMEBUFFER,i.DEPTH_STENCIL_ATTACHMENT,i.TEXTURE_2D,K,0);else throw new Error("Unknown depthTexture format")}function Lt(w){let x=n.get(w),O=w.isWebGLCubeRenderTarget===!0;if(x.__boundDepthTexture!==w.depthTexture){let Z=w.depthTexture;if(x.__depthDisposeCallback&&x.__depthDisposeCallback(),Z){let K=()=>{delete x.__boundDepthTexture,delete x.__depthDisposeCallback,Z.removeEventListener("dispose",K)};Z.addEventListener("dispose",K),x.__depthDisposeCallback=K}x.__boundDepthTexture=Z}if(w.depthTexture&&!x.__autoAllocateDepthBuffer){if(O)throw new Error("target.depthTexture not supported in Cube render targets");Ct(x.__webglFramebuffer,w)}else if(O){x.__webglDepthbuffer=[];for(let Z=0;Z<6;Z++)if(e.bindFramebuffer(i.FRAMEBUFFER,x.__webglFramebuffer[Z]),x.__webglDepthbuffer[Z]===void 0)x.__webglDepthbuffer[Z]=i.createRenderbuffer(),rt(x.__webglDepthbuffer[Z],w,!1);else{let K=w.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT,q=x.__webglDepthbuffer[Z];i.bindRenderbuffer(i.RENDERBUFFER,q),i.framebufferRenderbuffer(i.FRAMEBUFFER,K,i.RENDERBUFFER,q)}}else if(e.bindFramebuffer(i.FRAMEBUFFER,x.__webglFramebuffer),x.__webglDepthbuffer===void 0)x.__webglDepthbuffer=i.createRenderbuffer(),rt(x.__webglDepthbuffer,w,!1);else{let Z=w.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT,K=x.__webglDepthbuffer;i.bindRenderbuffer(i.RENDERBUFFER,K),i.framebufferRenderbuffer(i.FRAMEBUFFER,Z,i.RENDERBUFFER,K)}e.bindFramebuffer(i.FRAMEBUFFER,null)}function kt(w,x,O){let Z=n.get(w);x!==void 0&&Mt(Z.__webglFramebuffer,w,w.texture,i.COLOR_ATTACHMENT0,i.TEXTURE_2D,0),O!==void 0&&Lt(w)}function de(w){let x=w.texture,O=n.get(w),Z=n.get(x);w.addEventListener("dispose",R);let K=w.textures,q=w.isWebGLCubeRenderTarget===!0,St=K.length>1;if(St||(Z.__webglTexture===void 0&&(Z.__webglTexture=i.createTexture()),Z.__version=x.version,o.memory.textures++),q){O.__webglFramebuffer=[];for(let ot=0;ot<6;ot++)if(x.mipmaps&&x.mipmaps.length>0){O.__webglFramebuffer[ot]=[];for(let ft=0;ft<x.mipmaps.length;ft++)O.__webglFramebuffer[ot][ft]=i.createFramebuffer()}else O.__webglFramebuffer[ot]=i.createFramebuffer()}else{if(x.mipmaps&&x.mipmaps.length>0){O.__webglFramebuffer=[];for(let ot=0;ot<x.mipmaps.length;ot++)O.__webglFramebuffer[ot]=i.createFramebuffer()}else O.__webglFramebuffer=i.createFramebuffer();if(St)for(let ot=0,ft=K.length;ot<ft;ot++){let Xt=n.get(K[ot]);Xt.__webglTexture===void 0&&(Xt.__webglTexture=i.createTexture(),o.memory.textures++)}if(w.samples>0&&Vt(w)===!1){O.__webglMultisampledFramebuffer=i.createFramebuffer(),O.__webglColorRenderbuffer=[],e.bindFramebuffer(i.FRAMEBUFFER,O.__webglMultisampledFramebuffer);for(let ot=0;ot<K.length;ot++){let ft=K[ot];O.__webglColorRenderbuffer[ot]=i.createRenderbuffer(),i.bindRenderbuffer(i.RENDERBUFFER,O.__webglColorRenderbuffer[ot]);let Xt=r.convert(ft.format,ft.colorSpace),j=r.convert(ft.type),pt=b(ft.internalFormat,Xt,j,ft.colorSpace,w.isXRRenderTarget===!0),Rt=Ht(w);i.renderbufferStorageMultisample(i.RENDERBUFFER,Rt,pt,w.width,w.height),i.framebufferRenderbuffer(i.FRAMEBUFFER,i.COLOR_ATTACHMENT0+ot,i.RENDERBUFFER,O.__webglColorRenderbuffer[ot])}i.bindRenderbuffer(i.RENDERBUFFER,null),w.depthBuffer&&(O.__webglDepthRenderbuffer=i.createRenderbuffer(),rt(O.__webglDepthRenderbuffer,w,!0)),e.bindFramebuffer(i.FRAMEBUFFER,null)}}if(q){e.bindTexture(i.TEXTURE_CUBE_MAP,Z.__webglTexture),zt(i.TEXTURE_CUBE_MAP,x);for(let ot=0;ot<6;ot++)if(x.mipmaps&&x.mipmaps.length>0)for(let ft=0;ft<x.mipmaps.length;ft++)Mt(O.__webglFramebuffer[ot][ft],w,x,i.COLOR_ATTACHMENT0,i.TEXTURE_CUBE_MAP_POSITIVE_X+ot,ft);else Mt(O.__webglFramebuffer[ot],w,x,i.COLOR_ATTACHMENT0,i.TEXTURE_CUBE_MAP_POSITIVE_X+ot,0);m(x)&&l(i.TEXTURE_CUBE_MAP),e.unbindTexture()}else if(St){for(let ot=0,ft=K.length;ot<ft;ot++){let Xt=K[ot],j=n.get(Xt);e.bindTexture(i.TEXTURE_2D,j.__webglTexture),zt(i.TEXTURE_2D,Xt),Mt(O.__webglFramebuffer,w,Xt,i.COLOR_ATTACHMENT0+ot,i.TEXTURE_2D,0),m(Xt)&&l(i.TEXTURE_2D)}e.unbindTexture()}else{let ot=i.TEXTURE_2D;if((w.isWebGL3DRenderTarget||w.isWebGLArrayRenderTarget)&&(ot=w.isWebGL3DRenderTarget?i.TEXTURE_3D:i.TEXTURE_2D_ARRAY),e.bindTexture(ot,Z.__webglTexture),zt(ot,x),x.mipmaps&&x.mipmaps.length>0)for(let ft=0;ft<x.mipmaps.length;ft++)Mt(O.__webglFramebuffer[ft],w,x,i.COLOR_ATTACHMENT0,ot,ft);else Mt(O.__webglFramebuffer,w,x,i.COLOR_ATTACHMENT0,ot,0);m(x)&&l(ot),e.unbindTexture()}w.depthBuffer&&Lt(w)}function Wt(w){let x=w.textures;for(let O=0,Z=x.length;O<Z;O++){let K=x[O];if(m(K)){let q=S(w),St=n.get(K).__webglTexture;e.bindTexture(q,St),l(q),e.unbindTexture()}}}let ge=[],N=[];function Ye(w){if(w.samples>0){if(Vt(w)===!1){let x=w.textures,O=w.width,Z=w.height,K=i.COLOR_BUFFER_BIT,q=w.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT,St=n.get(w),ot=x.length>1;if(ot)for(let ft=0;ft<x.length;ft++)e.bindFramebuffer(i.FRAMEBUFFER,St.__webglMultisampledFramebuffer),i.framebufferRenderbuffer(i.FRAMEBUFFER,i.COLOR_ATTACHMENT0+ft,i.RENDERBUFFER,null),e.bindFramebuffer(i.FRAMEBUFFER,St.__webglFramebuffer),i.framebufferTexture2D(i.DRAW_FRAMEBUFFER,i.COLOR_ATTACHMENT0+ft,i.TEXTURE_2D,null,0);e.bindFramebuffer(i.READ_FRAMEBUFFER,St.__webglMultisampledFramebuffer),e.bindFramebuffer(i.DRAW_FRAMEBUFFER,St.__webglFramebuffer);for(let ft=0;ft<x.length;ft++){if(w.resolveDepthBuffer&&(w.depthBuffer&&(K|=i.DEPTH_BUFFER_BIT),w.stencilBuffer&&w.resolveStencilBuffer&&(K|=i.STENCIL_BUFFER_BIT)),ot){i.framebufferRenderbuffer(i.READ_FRAMEBUFFER,i.COLOR_ATTACHMENT0,i.RENDERBUFFER,St.__webglColorRenderbuffer[ft]);let Xt=n.get(x[ft]).__webglTexture;i.framebufferTexture2D(i.DRAW_FRAMEBUFFER,i.COLOR_ATTACHMENT0,i.TEXTURE_2D,Xt,0)}i.blitFramebuffer(0,0,O,Z,0,0,O,Z,K,i.NEAREST),c===!0&&(ge.length=0,N.length=0,ge.push(i.COLOR_ATTACHMENT0+ft),w.depthBuffer&&w.resolveDepthBuffer===!1&&(ge.push(q),N.push(q),i.invalidateFramebuffer(i.DRAW_FRAMEBUFFER,N)),i.invalidateFramebuffer(i.READ_FRAMEBUFFER,ge))}if(e.bindFramebuffer(i.READ_FRAMEBUFFER,null),e.bindFramebuffer(i.DRAW_FRAMEBUFFER,null),ot)for(let ft=0;ft<x.length;ft++){e.bindFramebuffer(i.FRAMEBUFFER,St.__webglMultisampledFramebuffer),i.framebufferRenderbuffer(i.FRAMEBUFFER,i.COLOR_ATTACHMENT0+ft,i.RENDERBUFFER,St.__webglColorRenderbuffer[ft]);let Xt=n.get(x[ft]).__webglTexture;e.bindFramebuffer(i.FRAMEBUFFER,St.__webglFramebuffer),i.framebufferTexture2D(i.DRAW_FRAMEBUFFER,i.COLOR_ATTACHMENT0+ft,i.TEXTURE_2D,Xt,0)}e.bindFramebuffer(i.DRAW_FRAMEBUFFER,St.__webglMultisampledFramebuffer)}else if(w.depthBuffer&&w.resolveDepthBuffer===!1&&c){let x=w.stencilBuffer?i.DEPTH_STENCIL_ATTACHMENT:i.DEPTH_ATTACHMENT;i.invalidateFramebuffer(i.DRAW_FRAMEBUFFER,[x])}}}function Ht(w){return Math.min(s.maxSamples,w.samples)}function Vt(w){let x=n.get(w);return w.samples>0&&t.has("WEBGL_multisampled_render_to_texture")===!0&&x.__useRenderToTexture!==!1}function Tt(w){let x=o.render.frame;f.get(w)!==x&&(f.set(w,x),w.update())}function oe(w,x){let O=w.colorSpace,Z=w.format,K=w.type;return w.isCompressedTexture===!0||w.isVideoTexture===!0||O!==ns&&O!==Xn&&(qt.getTransfer(O)===Qt?(Z!==on||K!==In)&&console.warn("THREE.WebGLTextures: sRGB encoded textures have to use RGBAFormat and UnsignedByteType."):console.error("THREE.WebGLTextures: Unsupported texture color space:",O)),x}function wt(w){return typeof HTMLImageElement<"u"&&w instanceof HTMLImageElement?(u.width=w.naturalWidth||w.width,u.height=w.naturalHeight||w.height):typeof VideoFrame<"u"&&w instanceof VideoFrame?(u.width=w.displayWidth,u.height=w.displayHeight):(u.width=w.width,u.height=w.height),u}this.allocateTextureUnit=k,this.resetTextureUnits=H,this.setTexture2D=J,this.setTexture2DArray=X,this.setTexture3D=tt,this.setTextureCube=G,this.rebindTextures=kt,this.setupRenderTarget=de,this.updateRenderTargetMipmap=Wt,this.updateMultisampleRenderTarget=Ye,this.setupDepthRenderbuffer=Lt,this.setupFrameBufferTexture=Mt,this.useMultisampledRTT=Vt}function l0(i,t){function e(n,s=Xn){let r,o=qt.getTransfer(s);if(n===In)return i.UNSIGNED_BYTE;if(n===Ic)return i.UNSIGNED_SHORT_4_4_4_4;if(n===Pc)return i.UNSIGNED_SHORT_5_5_5_1;if(n===Sh)return i.UNSIGNED_INT_5_9_9_9_REV;if(n===vh)return i.BYTE;if(n===Mh)return i.SHORT;if(n===Ts)return i.UNSIGNED_SHORT;if(n===Cc)return i.INT;if(n===di)return i.UNSIGNED_INT;if(n===Tn)return i.FLOAT;if(n===Fs)return i.HALF_FLOAT;if(n===Eh)return i.ALPHA;if(n===bh)return i.RGB;if(n===on)return i.RGBA;if(n===wh)return i.LUMINANCE;if(n===Th)return i.LUMINANCE_ALPHA;if(n===Xi)return i.DEPTH_COMPONENT;if(n===ji)return i.DEPTH_STENCIL;if(n===Ah)return i.RED;if(n===Lc)return i.RED_INTEGER;if(n===Rh)return i.RG;if(n===Dc)return i.RG_INTEGER;if(n===Uc)return i.RGBA_INTEGER;if(n===vr||n===Mr||n===Sr||n===Er)if(o===Qt)if(r=t.get("WEBGL_compressed_texture_s3tc_srgb"),r!==null){if(n===vr)return r.COMPRESSED_SRGB_S3TC_DXT1_EXT;if(n===Mr)return r.COMPRESSED_SRGB_ALPHA_S3TC_DXT1_EXT;if(n===Sr)return r.COMPRESSED_SRGB_ALPHA_S3TC_DXT3_EXT;if(n===Er)return r.COMPRESSED_SRGB_ALPHA_S3TC_DXT5_EXT}else return null;else if(r=t.get("WEBGL_compressed_texture_s3tc"),r!==null){if(n===vr)return r.COMPRESSED_RGB_S3TC_DXT1_EXT;if(n===Mr)return r.COMPRESSED_RGBA_S3TC_DXT1_EXT;if(n===Sr)return r.COMPRESSED_RGBA_S3TC_DXT3_EXT;if(n===Er)return r.COMPRESSED_RGBA_S3TC_DXT5_EXT}else return null;if(n===ma||n===ga||n===_a||n===xa)if(r=t.get("WEBGL_compressed_texture_pvrtc"),r!==null){if(n===ma)return r.COMPRESSED_RGB_PVRTC_4BPPV1_IMG;if(n===ga)return r.COMPRESSED_RGB_PVRTC_2BPPV1_IMG;if(n===_a)return r.COMPRESSED_RGBA_PVRTC_4BPPV1_IMG;if(n===xa)return r.COMPRESSED_RGBA_PVRTC_2BPPV1_IMG}else return null;if(n===ya||n===va||n===Ma)if(r=t.get("WEBGL_compressed_texture_etc"),r!==null){if(n===ya||n===va)return o===Qt?r.COMPRESSED_SRGB8_ETC2:r.COMPRESSED_RGB8_ETC2;if(n===Ma)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ETC2_EAC:r.COMPRESSED_RGBA8_ETC2_EAC}else return null;if(n===Sa||n===Ea||n===ba||n===wa||n===Ta||n===Aa||n===Ra||n===Ca||n===Ia||n===Pa||n===La||n===Da||n===Ua||n===Fa)if(r=t.get("WEBGL_compressed_texture_astc"),r!==null){if(n===Sa)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_4x4_KHR:r.COMPRESSED_RGBA_ASTC_4x4_KHR;if(n===Ea)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_5x4_KHR:r.COMPRESSED_RGBA_ASTC_5x4_KHR;if(n===ba)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_5x5_KHR:r.COMPRESSED_RGBA_ASTC_5x5_KHR;if(n===wa)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_6x5_KHR:r.COMPRESSED_RGBA_ASTC_6x5_KHR;if(n===Ta)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_6x6_KHR:r.COMPRESSED_RGBA_ASTC_6x6_KHR;if(n===Aa)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_8x5_KHR:r.COMPRESSED_RGBA_ASTC_8x5_KHR;if(n===Ra)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_8x6_KHR:r.COMPRESSED_RGBA_ASTC_8x6_KHR;if(n===Ca)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_8x8_KHR:r.COMPRESSED_RGBA_ASTC_8x8_KHR;if(n===Ia)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_10x5_KHR:r.COMPRESSED_RGBA_ASTC_10x5_KHR;if(n===Pa)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_10x6_KHR:r.COMPRESSED_RGBA_ASTC_10x6_KHR;if(n===La)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_10x8_KHR:r.COMPRESSED_RGBA_ASTC_10x8_KHR;if(n===Da)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_10x10_KHR:r.COMPRESSED_RGBA_ASTC_10x10_KHR;if(n===Ua)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_12x10_KHR:r.COMPRESSED_RGBA_ASTC_12x10_KHR;if(n===Fa)return o===Qt?r.COMPRESSED_SRGB8_ALPHA8_ASTC_12x12_KHR:r.COMPRESSED_RGBA_ASTC_12x12_KHR}else return null;if(n===br||n===Na||n===Oa)if(r=t.get("EXT_texture_compression_bptc"),r!==null){if(n===br)return o===Qt?r.COMPRESSED_SRGB_ALPHA_BPTC_UNORM_EXT:r.COMPRESSED_RGBA_BPTC_UNORM_EXT;if(n===Na)return r.COMPRESSED_RGB_BPTC_SIGNED_FLOAT_EXT;if(n===Oa)return r.COMPRESSED_RGB_BPTC_UNSIGNED_FLOAT_EXT}else return null;if(n===Ch||n===Ba||n===za||n===ka)if(r=t.get("EXT_texture_compression_rgtc"),r!==null){if(n===br)return r.COMPRESSED_RED_RGTC1_EXT;if(n===Ba)return r.COMPRESSED_SIGNED_RED_RGTC1_EXT;if(n===za)return r.COMPRESSED_RED_GREEN_RGTC2_EXT;if(n===ka)return r.COMPRESSED_SIGNED_RED_GREEN_RGTC2_EXT}else return null;return n===Ki?i.UNSIGNED_INT_24_8:i[n]!==void 0?i[n]:null}return{convert:e}}var ic=class extends Ue{constructor(t=[]){super(),this.isArrayCamera=!0,this.cameras=t}},me=class extends Te{constructor(){super(),this.isGroup=!0,this.type="Group"}},h0={type:"move"},Es=class{constructor(){this._targetRay=null,this._grip=null,this._hand=null}getHandSpace(){return this._hand===null&&(this._hand=new me,this._hand.matrixAutoUpdate=!1,this._hand.visible=!1,this._hand.joints={},this._hand.inputState={pinching:!1}),this._hand}getTargetRaySpace(){return this._targetRay===null&&(this._targetRay=new me,this._targetRay.matrixAutoUpdate=!1,this._targetRay.visible=!1,this._targetRay.hasLinearVelocity=!1,this._targetRay.linearVelocity=new P,this._targetRay.hasAngularVelocity=!1,this._targetRay.angularVelocity=new P),this._targetRay}getGripSpace(){return this._grip===null&&(this._grip=new me,this._grip.matrixAutoUpdate=!1,this._grip.visible=!1,this._grip.hasLinearVelocity=!1,this._grip.linearVelocity=new P,this._grip.hasAngularVelocity=!1,this._grip.angularVelocity=new P),this._grip}dispatchEvent(t){return this._targetRay!==null&&this._targetRay.dispatchEvent(t),this._grip!==null&&this._grip.dispatchEvent(t),this._hand!==null&&this._hand.dispatchEvent(t),this}connect(t){if(t&&t.hand){let e=this._hand;if(e)for(let n of t.hand.values())this._getHandJoint(e,n)}return this.dispatchEvent({type:"connected",data:t}),this}disconnect(t){return this.dispatchEvent({type:"disconnected",data:t}),this._targetRay!==null&&(this._targetRay.visible=!1),this._grip!==null&&(this._grip.visible=!1),this._hand!==null&&(this._hand.visible=!1),this}update(t,e,n){let s=null,r=null,o=null,a=this._targetRay,c=this._grip,u=this._hand;if(t&&e.session.visibilityState!=="visible-blurred"){if(u&&t.hand){o=!0;for(let _ of t.hand.values()){let m=e.getJointPose(_,n),l=this._getHandJoint(u,_);m!==null&&(l.matrix.fromArray(m.transform.matrix),l.matrix.decompose(l.position,l.rotation,l.scale),l.matrixWorldNeedsUpdate=!0,l.jointRadius=m.radius),l.visible=m!==null}let f=u.joints["index-finger-tip"],p=u.joints["thumb-tip"],h=f.position.distanceTo(p.position),d=.02,g=.005;u.inputState.pinching&&h>d+g?(u.inputState.pinching=!1,this.dispatchEvent({type:"pinchend",handedness:t.handedness,target:this})):!u.inputState.pinching&&h<=d-g&&(u.inputState.pinching=!0,this.dispatchEvent({type:"pinchstart",handedness:t.handedness,target:this}))}else c!==null&&t.gripSpace&&(r=e.getPose(t.gripSpace,n),r!==null&&(c.matrix.fromArray(r.transform.matrix),c.matrix.decompose(c.position,c.rotation,c.scale),c.matrixWorldNeedsUpdate=!0,r.linearVelocity?(c.hasLinearVelocity=!0,c.linearVelocity.copy(r.linearVelocity)):c.hasLinearVelocity=!1,r.angularVelocity?(c.hasAngularVelocity=!0,c.angularVelocity.copy(r.angularVelocity)):c.hasAngularVelocity=!1));a!==null&&(s=e.getPose(t.targetRaySpace,n),s===null&&r!==null&&(s=r),s!==null&&(a.matrix.fromArray(s.transform.matrix),a.matrix.decompose(a.position,a.rotation,a.scale),a.matrixWorldNeedsUpdate=!0,s.linearVelocity?(a.hasLinearVelocity=!0,a.linearVelocity.copy(s.linearVelocity)):a.hasLinearVelocity=!1,s.angularVelocity?(a.hasAngularVelocity=!0,a.angularVelocity.copy(s.angularVelocity)):a.hasAngularVelocity=!1,this.dispatchEvent(h0)))}return a!==null&&(a.visible=s!==null),c!==null&&(c.visible=r!==null),u!==null&&(u.visible=o!==null),this}_getHandJoint(t,e){if(t.joints[e.jointName]===void 0){let n=new me;n.matrixAutoUpdate=!1,n.visible=!1,t.joints[e.jointName]=n,t.add(n)}return t.joints[e.jointName]}},u0=`
void main() {

	gl_Position = vec4( position, 1.0 );

}`,d0=`
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

}`,sc=class{constructor(){this.texture=null,this.mesh=null,this.depthNear=0,this.depthFar=0}init(t,e,n){if(this.texture===null){let s=new Xe,r=t.properties.get(s);r.__webglTexture=e.texture,(e.depthNear!=n.depthNear||e.depthFar!=n.depthFar)&&(this.depthNear=e.depthNear,this.depthFar=e.depthFar),this.texture=s}}getMesh(t){if(this.texture!==null&&this.mesh===null){let e=t.cameras[0].viewport,n=new gn({vertexShader:u0,fragmentShader:d0,uniforms:{depthColor:{value:this.texture},depthWidth:{value:e.z},depthHeight:{value:e.w}}});this.mesh=new _t(new Qn(20,20),n)}return this.mesh}reset(){this.texture=null,this.mesh=null}getDepthTexture(){return this.texture}},rc=class extends Kn{constructor(t,e){super();let n=this,s=null,r=1,o=null,a="local-floor",c=1,u=null,f=null,p=null,h=null,d=null,g=null,_=new sc,m=e.getContextAttributes(),l=null,S=null,b=[],y=[],I=new ut,T=null,R=new Ue;R.viewport=new pe;let A=new Ue;A.viewport=new pe;let E=[R,A],v=new ic,C=null,H=null;this.cameraAutoUpdate=!0,this.enabled=!1,this.isPresenting=!1,this.getController=function(Y){let et=b[Y];return et===void 0&&(et=new Es,b[Y]=et),et.getTargetRaySpace()},this.getControllerGrip=function(Y){let et=b[Y];return et===void 0&&(et=new Es,b[Y]=et),et.getGripSpace()},this.getHand=function(Y){let et=b[Y];return et===void 0&&(et=new Es,b[Y]=et),et.getHandSpace()};function k(Y){let et=y.indexOf(Y.inputSource);if(et===-1)return;let Mt=b[et];Mt!==void 0&&(Mt.update(Y.inputSource,Y.frame,u||o),Mt.dispatchEvent({type:Y.type,data:Y.inputSource}))}function W(){s.removeEventListener("select",k),s.removeEventListener("selectstart",k),s.removeEventListener("selectend",k),s.removeEventListener("squeeze",k),s.removeEventListener("squeezestart",k),s.removeEventListener("squeezeend",k),s.removeEventListener("end",W),s.removeEventListener("inputsourceschange",J);for(let Y=0;Y<b.length;Y++){let et=y[Y];et!==null&&(y[Y]=null,b[Y].disconnect(et))}C=null,H=null,_.reset(),t.setRenderTarget(l),d=null,h=null,p=null,s=null,S=null,ee.stop(),n.isPresenting=!1,t.setPixelRatio(T),t.setSize(I.width,I.height,!1),n.dispatchEvent({type:"sessionend"})}this.setFramebufferScaleFactor=function(Y){r=Y,n.isPresenting===!0&&console.warn("THREE.WebXRManager: Cannot change framebuffer scale while presenting.")},this.setReferenceSpaceType=function(Y){a=Y,n.isPresenting===!0&&console.warn("THREE.WebXRManager: Cannot change reference space type while presenting.")},this.getReferenceSpace=function(){return u||o},this.setReferenceSpace=function(Y){u=Y},this.getBaseLayer=function(){return h!==null?h:d},this.getBinding=function(){return p},this.getFrame=function(){return g},this.getSession=function(){return s},this.setSession=async function(Y){if(s=Y,s!==null){if(l=t.getRenderTarget(),s.addEventListener("select",k),s.addEventListener("selectstart",k),s.addEventListener("selectend",k),s.addEventListener("squeeze",k),s.addEventListener("squeezestart",k),s.addEventListener("squeezeend",k),s.addEventListener("end",W),s.addEventListener("inputsourceschange",J),m.xrCompatible!==!0&&await e.makeXRCompatible(),T=t.getPixelRatio(),t.getSize(I),s.renderState.layers===void 0){let et={antialias:m.antialias,alpha:!0,depth:m.depth,stencil:m.stencil,framebufferScaleFactor:r};d=new XRWebGLLayer(s,e,et),s.updateRenderState({baseLayer:d}),t.setPixelRatio(1),t.setSize(d.framebufferWidth,d.framebufferHeight,!1),S=new Pn(d.framebufferWidth,d.framebufferHeight,{format:on,type:In,colorSpace:t.outputColorSpace,stencilBuffer:m.stencil})}else{let et=null,Mt=null,rt=null;m.depth&&(rt=m.stencil?e.DEPTH24_STENCIL8:e.DEPTH_COMPONENT24,et=m.stencil?ji:Xi,Mt=m.stencil?Ki:di);let Ct={colorFormat:e.RGBA8,depthFormat:rt,scaleFactor:r};p=new XRWebGLBinding(s,e),h=p.createProjectionLayer(Ct),s.updateRenderState({layers:[h]}),t.setPixelRatio(1),t.setSize(h.textureWidth,h.textureHeight,!1),S=new Pn(h.textureWidth,h.textureHeight,{format:on,type:In,depthTexture:new Br(h.textureWidth,h.textureHeight,Mt,void 0,void 0,void 0,void 0,void 0,void 0,et),stencilBuffer:m.stencil,colorSpace:t.outputColorSpace,samples:m.antialias?4:0,resolveDepthBuffer:h.ignoreDepthValues===!1})}S.isXRRenderTarget=!0,this.setFoveation(c),u=null,o=await s.requestReferenceSpace(a),ee.setContext(s),ee.start(),n.isPresenting=!0,n.dispatchEvent({type:"sessionstart"})}},this.getEnvironmentBlendMode=function(){if(s!==null)return s.environmentBlendMode},this.getDepthTexture=function(){return _.getDepthTexture()};function J(Y){for(let et=0;et<Y.removed.length;et++){let Mt=Y.removed[et],rt=y.indexOf(Mt);rt>=0&&(y[rt]=null,b[rt].disconnect(Mt))}for(let et=0;et<Y.added.length;et++){let Mt=Y.added[et],rt=y.indexOf(Mt);if(rt===-1){for(let Lt=0;Lt<b.length;Lt++)if(Lt>=y.length){y.push(Mt),rt=Lt;break}else if(y[Lt]===null){y[Lt]=Mt,rt=Lt;break}if(rt===-1)break}let Ct=b[rt];Ct&&Ct.connect(Mt)}}let X=new P,tt=new P;function G(Y,et,Mt){X.setFromMatrixPosition(et.matrixWorld),tt.setFromMatrixPosition(Mt.matrixWorld);let rt=X.distanceTo(tt),Ct=et.projectionMatrix.elements,Lt=Mt.projectionMatrix.elements,kt=Ct[14]/(Ct[10]-1),de=Ct[14]/(Ct[10]+1),Wt=(Ct[9]+1)/Ct[5],ge=(Ct[9]-1)/Ct[5],N=(Ct[8]-1)/Ct[0],Ye=(Lt[8]+1)/Lt[0],Ht=kt*N,Vt=kt*Ye,Tt=rt/(-N+Ye),oe=Tt*-N;if(et.matrixWorld.decompose(Y.position,Y.quaternion,Y.scale),Y.translateX(oe),Y.translateZ(Tt),Y.matrixWorld.compose(Y.position,Y.quaternion,Y.scale),Y.matrixWorldInverse.copy(Y.matrixWorld).invert(),Ct[10]===-1)Y.projectionMatrix.copy(et.projectionMatrix),Y.projectionMatrixInverse.copy(et.projectionMatrixInverse);else{let wt=kt+Tt,w=de+Tt,x=Ht-oe,O=Vt+(rt-oe),Z=Wt*de/w*wt,K=ge*de/w*wt;Y.projectionMatrix.makePerspective(x,O,Z,K,wt,w),Y.projectionMatrixInverse.copy(Y.projectionMatrix).invert()}}function st(Y,et){et===null?Y.matrixWorld.copy(Y.matrix):Y.matrixWorld.multiplyMatrices(et.matrixWorld,Y.matrix),Y.matrixWorldInverse.copy(Y.matrixWorld).invert()}this.updateCamera=function(Y){if(s===null)return;let et=Y.near,Mt=Y.far;_.texture!==null&&(_.depthNear>0&&(et=_.depthNear),_.depthFar>0&&(Mt=_.depthFar)),v.near=A.near=R.near=et,v.far=A.far=R.far=Mt,(C!==v.near||H!==v.far)&&(s.updateRenderState({depthNear:v.near,depthFar:v.far}),C=v.near,H=v.far),R.layers.mask=Y.layers.mask|2,A.layers.mask=Y.layers.mask|4,v.layers.mask=R.layers.mask|A.layers.mask;let rt=Y.parent,Ct=v.cameras;st(v,rt);for(let Lt=0;Lt<Ct.length;Lt++)st(Ct[Lt],rt);Ct.length===2?G(v,R,A):v.projectionMatrix.copy(R.projectionMatrix),dt(Y,v,rt)};function dt(Y,et,Mt){Mt===null?Y.matrix.copy(et.matrixWorld):(Y.matrix.copy(Mt.matrixWorld),Y.matrix.invert(),Y.matrix.multiply(et.matrixWorld)),Y.matrix.decompose(Y.position,Y.quaternion,Y.scale),Y.updateMatrixWorld(!0),Y.projectionMatrix.copy(et.projectionMatrix),Y.projectionMatrixInverse.copy(et.projectionMatrixInverse),Y.isPerspectiveCamera&&(Y.fov=Ga*2*Math.atan(1/Y.projectionMatrix.elements[5]),Y.zoom=1)}this.getCamera=function(){return v},this.getFoveation=function(){if(!(h===null&&d===null))return c},this.setFoveation=function(Y){c=Y,h!==null&&(h.fixedFoveation=Y),d!==null&&d.fixedFoveation!==void 0&&(d.fixedFoveation=Y)},this.hasDepthSensing=function(){return _.texture!==null},this.getDepthSensingMesh=function(){return _.getMesh(v)};let bt=null;function zt(Y,et){if(f=et.getViewerPose(u||o),g=et,f!==null){let Mt=f.views;d!==null&&(t.setRenderTargetFramebuffer(S,d.framebuffer),t.setRenderTarget(S));let rt=!1;Mt.length!==v.cameras.length&&(v.cameras.length=0,rt=!0);for(let Lt=0;Lt<Mt.length;Lt++){let kt=Mt[Lt],de=null;if(d!==null)de=d.getViewport(kt);else{let ge=p.getViewSubImage(h,kt);de=ge.viewport,Lt===0&&(t.setRenderTargetTextures(S,ge.colorTexture,h.ignoreDepthValues?void 0:ge.depthStencilTexture),t.setRenderTarget(S))}let Wt=E[Lt];Wt===void 0&&(Wt=new Ue,Wt.layers.enable(Lt),Wt.viewport=new pe,E[Lt]=Wt),Wt.matrix.fromArray(kt.transform.matrix),Wt.matrix.decompose(Wt.position,Wt.quaternion,Wt.scale),Wt.projectionMatrix.fromArray(kt.projectionMatrix),Wt.projectionMatrixInverse.copy(Wt.projectionMatrix).invert(),Wt.viewport.set(de.x,de.y,de.width,de.height),Lt===0&&(v.matrix.copy(Wt.matrix),v.matrix.decompose(v.position,v.quaternion,v.scale)),rt===!0&&v.cameras.push(Wt)}let Ct=s.enabledFeatures;if(Ct&&Ct.includes("depth-sensing")){let Lt=p.getDepthInformation(Mt[0]);Lt&&Lt.isValid&&Lt.texture&&_.init(t,Lt,s.renderState)}}for(let Mt=0;Mt<b.length;Mt++){let rt=y[Mt],Ct=b[Mt];rt!==null&&Ct!==void 0&&Ct.update(rt,et,u||o)}bt&&bt(Y,et),et.detectedPlanes&&n.dispatchEvent({type:"planesdetected",data:et}),g=null}let ee=new Fh;ee.setAnimationLoop(zt),this.setAnimationLoop=function(Y){bt=Y},this.dispose=function(){}}},oi=new mn,f0=new he;function p0(i,t){function e(m,l){m.matrixAutoUpdate===!0&&m.updateMatrix(),l.value.copy(m.matrix)}function n(m,l){l.color.getRGB(m.fogColor.value,Uh(i)),l.isFog?(m.fogNear.value=l.near,m.fogFar.value=l.far):l.isFogExp2&&(m.fogDensity.value=l.density)}function s(m,l,S,b,y){l.isMeshBasicMaterial||l.isMeshLambertMaterial?r(m,l):l.isMeshToonMaterial?(r(m,l),p(m,l)):l.isMeshPhongMaterial?(r(m,l),f(m,l)):l.isMeshStandardMaterial?(r(m,l),h(m,l),l.isMeshPhysicalMaterial&&d(m,l,y)):l.isMeshMatcapMaterial?(r(m,l),g(m,l)):l.isMeshDepthMaterial?r(m,l):l.isMeshDistanceMaterial?(r(m,l),_(m,l)):l.isMeshNormalMaterial?r(m,l):l.isLineBasicMaterial?(o(m,l),l.isLineDashedMaterial&&a(m,l)):l.isPointsMaterial?c(m,l,S,b):l.isSpriteMaterial?u(m,l):l.isShadowMaterial?(m.color.value.copy(l.color),m.opacity.value=l.opacity):l.isShaderMaterial&&(l.uniformsNeedUpdate=!1)}function r(m,l){m.opacity.value=l.opacity,l.color&&m.diffuse.value.copy(l.color),l.emissive&&m.emissive.value.copy(l.emissive).multiplyScalar(l.emissiveIntensity),l.map&&(m.map.value=l.map,e(l.map,m.mapTransform)),l.alphaMap&&(m.alphaMap.value=l.alphaMap,e(l.alphaMap,m.alphaMapTransform)),l.bumpMap&&(m.bumpMap.value=l.bumpMap,e(l.bumpMap,m.bumpMapTransform),m.bumpScale.value=l.bumpScale,l.side===ke&&(m.bumpScale.value*=-1)),l.normalMap&&(m.normalMap.value=l.normalMap,e(l.normalMap,m.normalMapTransform),m.normalScale.value.copy(l.normalScale),l.side===ke&&m.normalScale.value.negate()),l.displacementMap&&(m.displacementMap.value=l.displacementMap,e(l.displacementMap,m.displacementMapTransform),m.displacementScale.value=l.displacementScale,m.displacementBias.value=l.displacementBias),l.emissiveMap&&(m.emissiveMap.value=l.emissiveMap,e(l.emissiveMap,m.emissiveMapTransform)),l.specularMap&&(m.specularMap.value=l.specularMap,e(l.specularMap,m.specularMapTransform)),l.alphaTest>0&&(m.alphaTest.value=l.alphaTest);let S=t.get(l),b=S.envMap,y=S.envMapRotation;b&&(m.envMap.value=b,oi.copy(y),oi.x*=-1,oi.y*=-1,oi.z*=-1,b.isCubeTexture&&b.isRenderTargetTexture===!1&&(oi.y*=-1,oi.z*=-1),m.envMapRotation.value.setFromMatrix4(f0.makeRotationFromEuler(oi)),m.flipEnvMap.value=b.isCubeTexture&&b.isRenderTargetTexture===!1?-1:1,m.reflectivity.value=l.reflectivity,m.ior.value=l.ior,m.refractionRatio.value=l.refractionRatio),l.lightMap&&(m.lightMap.value=l.lightMap,m.lightMapIntensity.value=l.lightMapIntensity,e(l.lightMap,m.lightMapTransform)),l.aoMap&&(m.aoMap.value=l.aoMap,m.aoMapIntensity.value=l.aoMapIntensity,e(l.aoMap,m.aoMapTransform))}function o(m,l){m.diffuse.value.copy(l.color),m.opacity.value=l.opacity,l.map&&(m.map.value=l.map,e(l.map,m.mapTransform))}function a(m,l){m.dashSize.value=l.dashSize,m.totalSize.value=l.dashSize+l.gapSize,m.scale.value=l.scale}function c(m,l,S,b){m.diffuse.value.copy(l.color),m.opacity.value=l.opacity,m.size.value=l.size*S,m.scale.value=b*.5,l.map&&(m.map.value=l.map,e(l.map,m.uvTransform)),l.alphaMap&&(m.alphaMap.value=l.alphaMap,e(l.alphaMap,m.alphaMapTransform)),l.alphaTest>0&&(m.alphaTest.value=l.alphaTest)}function u(m,l){m.diffuse.value.copy(l.color),m.opacity.value=l.opacity,m.rotation.value=l.rotation,l.map&&(m.map.value=l.map,e(l.map,m.mapTransform)),l.alphaMap&&(m.alphaMap.value=l.alphaMap,e(l.alphaMap,m.alphaMapTransform)),l.alphaTest>0&&(m.alphaTest.value=l.alphaTest)}function f(m,l){m.specular.value.copy(l.specular),m.shininess.value=Math.max(l.shininess,1e-4)}function p(m,l){l.gradientMap&&(m.gradientMap.value=l.gradientMap)}function h(m,l){m.metalness.value=l.metalness,l.metalnessMap&&(m.metalnessMap.value=l.metalnessMap,e(l.metalnessMap,m.metalnessMapTransform)),m.roughness.value=l.roughness,l.roughnessMap&&(m.roughnessMap.value=l.roughnessMap,e(l.roughnessMap,m.roughnessMapTransform)),l.envMap&&(m.envMapIntensity.value=l.envMapIntensity)}function d(m,l,S){m.ior.value=l.ior,l.sheen>0&&(m.sheenColor.value.copy(l.sheenColor).multiplyScalar(l.sheen),m.sheenRoughness.value=l.sheenRoughness,l.sheenColorMap&&(m.sheenColorMap.value=l.sheenColorMap,e(l.sheenColorMap,m.sheenColorMapTransform)),l.sheenRoughnessMap&&(m.sheenRoughnessMap.value=l.sheenRoughnessMap,e(l.sheenRoughnessMap,m.sheenRoughnessMapTransform))),l.clearcoat>0&&(m.clearcoat.value=l.clearcoat,m.clearcoatRoughness.value=l.clearcoatRoughness,l.clearcoatMap&&(m.clearcoatMap.value=l.clearcoatMap,e(l.clearcoatMap,m.clearcoatMapTransform)),l.clearcoatRoughnessMap&&(m.clearcoatRoughnessMap.value=l.clearcoatRoughnessMap,e(l.clearcoatRoughnessMap,m.clearcoatRoughnessMapTransform)),l.clearcoatNormalMap&&(m.clearcoatNormalMap.value=l.clearcoatNormalMap,e(l.clearcoatNormalMap,m.clearcoatNormalMapTransform),m.clearcoatNormalScale.value.copy(l.clearcoatNormalScale),l.side===ke&&m.clearcoatNormalScale.value.negate())),l.dispersion>0&&(m.dispersion.value=l.dispersion),l.iridescence>0&&(m.iridescence.value=l.iridescence,m.iridescenceIOR.value=l.iridescenceIOR,m.iridescenceThicknessMinimum.value=l.iridescenceThicknessRange[0],m.iridescenceThicknessMaximum.value=l.iridescenceThicknessRange[1],l.iridescenceMap&&(m.iridescenceMap.value=l.iridescenceMap,e(l.iridescenceMap,m.iridescenceMapTransform)),l.iridescenceThicknessMap&&(m.iridescenceThicknessMap.value=l.iridescenceThicknessMap,e(l.iridescenceThicknessMap,m.iridescenceThicknessMapTransform))),l.transmission>0&&(m.transmission.value=l.transmission,m.transmissionSamplerMap.value=S.texture,m.transmissionSamplerSize.value.set(S.width,S.height),l.transmissionMap&&(m.transmissionMap.value=l.transmissionMap,e(l.transmissionMap,m.transmissionMapTransform)),m.thickness.value=l.thickness,l.thicknessMap&&(m.thicknessMap.value=l.thicknessMap,e(l.thicknessMap,m.thicknessMapTransform)),m.attenuationDistance.value=l.attenuationDistance,m.attenuationColor.value.copy(l.attenuationColor)),l.anisotropy>0&&(m.anisotropyVector.value.set(l.anisotropy*Math.cos(l.anisotropyRotation),l.anisotropy*Math.sin(l.anisotropyRotation)),l.anisotropyMap&&(m.anisotropyMap.value=l.anisotropyMap,e(l.anisotropyMap,m.anisotropyMapTransform))),m.specularIntensity.value=l.specularIntensity,m.specularColor.value.copy(l.specularColor),l.specularColorMap&&(m.specularColorMap.value=l.specularColorMap,e(l.specularColorMap,m.specularColorMapTransform)),l.specularIntensityMap&&(m.specularIntensityMap.value=l.specularIntensityMap,e(l.specularIntensityMap,m.specularIntensityMapTransform))}function g(m,l){l.matcap&&(m.matcap.value=l.matcap)}function _(m,l){let S=t.get(l).light;m.referencePosition.value.setFromMatrixPosition(S.matrixWorld),m.nearDistance.value=S.shadow.camera.near,m.farDistance.value=S.shadow.camera.far}return{refreshFogUniforms:n,refreshMaterialUniforms:s}}function m0(i,t,e,n){let s={},r={},o=[],a=i.getParameter(i.MAX_UNIFORM_BUFFER_BINDINGS);function c(S,b){let y=b.program;n.uniformBlockBinding(S,y)}function u(S,b){let y=s[S.id];y===void 0&&(g(S),y=f(S),s[S.id]=y,S.addEventListener("dispose",m));let I=b.program;n.updateUBOMapping(S,I);let T=t.render.frame;r[S.id]!==T&&(h(S),r[S.id]=T)}function f(S){let b=p();S.__bindingPointIndex=b;let y=i.createBuffer(),I=S.__size,T=S.usage;return i.bindBuffer(i.UNIFORM_BUFFER,y),i.bufferData(i.UNIFORM_BUFFER,I,T),i.bindBuffer(i.UNIFORM_BUFFER,null),i.bindBufferBase(i.UNIFORM_BUFFER,b,y),y}function p(){for(let S=0;S<a;S++)if(o.indexOf(S)===-1)return o.push(S),S;return console.error("THREE.WebGLRenderer: Maximum number of simultaneously usable uniforms groups reached."),0}function h(S){let b=s[S.id],y=S.uniforms,I=S.__cache;i.bindBuffer(i.UNIFORM_BUFFER,b);for(let T=0,R=y.length;T<R;T++){let A=Array.isArray(y[T])?y[T]:[y[T]];for(let E=0,v=A.length;E<v;E++){let C=A[E];if(d(C,T,E,I)===!0){let H=C.__offset,k=Array.isArray(C.value)?C.value:[C.value],W=0;for(let J=0;J<k.length;J++){let X=k[J],tt=_(X);typeof X=="number"||typeof X=="boolean"?(C.__data[0]=X,i.bufferSubData(i.UNIFORM_BUFFER,H+W,C.__data)):X.isMatrix3?(C.__data[0]=X.elements[0],C.__data[1]=X.elements[1],C.__data[2]=X.elements[2],C.__data[3]=0,C.__data[4]=X.elements[3],C.__data[5]=X.elements[4],C.__data[6]=X.elements[5],C.__data[7]=0,C.__data[8]=X.elements[6],C.__data[9]=X.elements[7],C.__data[10]=X.elements[8],C.__data[11]=0):(X.toArray(C.__data,W),W+=tt.storage/Float32Array.BYTES_PER_ELEMENT)}i.bufferSubData(i.UNIFORM_BUFFER,H,C.__data)}}}i.bindBuffer(i.UNIFORM_BUFFER,null)}function d(S,b,y,I){let T=S.value,R=b+"_"+y;if(I[R]===void 0)return typeof T=="number"||typeof T=="boolean"?I[R]=T:I[R]=T.clone(),!0;{let A=I[R];if(typeof T=="number"||typeof T=="boolean"){if(A!==T)return I[R]=T,!0}else if(A.equals(T)===!1)return A.copy(T),!0}return!1}function g(S){let b=S.uniforms,y=0,I=16;for(let R=0,A=b.length;R<A;R++){let E=Array.isArray(b[R])?b[R]:[b[R]];for(let v=0,C=E.length;v<C;v++){let H=E[v],k=Array.isArray(H.value)?H.value:[H.value];for(let W=0,J=k.length;W<J;W++){let X=k[W],tt=_(X),G=y%I,st=G%tt.boundary,dt=G+st;y+=st,dt!==0&&I-dt<tt.storage&&(y+=I-dt),H.__data=new Float32Array(tt.storage/Float32Array.BYTES_PER_ELEMENT),H.__offset=y,y+=tt.storage}}}let T=y%I;return T>0&&(y+=I-T),S.__size=y,S.__cache={},this}function _(S){let b={boundary:0,storage:0};return typeof S=="number"||typeof S=="boolean"?(b.boundary=4,b.storage=4):S.isVector2?(b.boundary=8,b.storage=8):S.isVector3||S.isColor?(b.boundary=16,b.storage=12):S.isVector4?(b.boundary=16,b.storage=16):S.isMatrix3?(b.boundary=48,b.storage=48):S.isMatrix4?(b.boundary=64,b.storage=64):S.isTexture?console.warn("THREE.WebGLRenderer: Texture samplers can not be part of an uniforms group."):console.warn("THREE.WebGLRenderer: Unsupported uniform value type.",S),b}function m(S){let b=S.target;b.removeEventListener("dispose",m);let y=o.indexOf(b.__bindingPointIndex);o.splice(y,1),i.deleteBuffer(s[b.id]),delete s[b.id],delete r[b.id]}function l(){for(let S in s)i.deleteBuffer(s[S]);o=[],s={},r={}}return{bind:c,update:u,dispose:l}}var zr=class{constructor(t={}){let{canvas:e=xd(),context:n=null,depth:s=!0,stencil:r=!1,alpha:o=!1,antialias:a=!1,premultipliedAlpha:c=!0,preserveDrawingBuffer:u=!1,powerPreference:f="default",failIfMajorPerformanceCaveat:p=!1,reverseDepthBuffer:h=!1}=t;this.isWebGLRenderer=!0;let d;if(n!==null){if(typeof WebGLRenderingContext<"u"&&n instanceof WebGLRenderingContext)throw new Error("THREE.WebGLRenderer: WebGL 1 is not supported since r163.");d=n.getContextAttributes().alpha}else d=o;let g=new Uint32Array(4),_=new Int32Array(4),m=null,l=null,S=[],b=[];this.domElement=e,this.debug={checkShaderErrors:!0,onShaderError:null},this.autoClear=!0,this.autoClearColor=!0,this.autoClearDepth=!0,this.autoClearStencil=!0,this.sortObjects=!0,this.clippingPlanes=[],this.localClippingEnabled=!1,this._outputColorSpace=De,this.toneMapping=Zn,this.toneMappingExposure=1;let y=this,I=!1,T=0,R=0,A=null,E=-1,v=null,C=new pe,H=new pe,k=null,W=new Bt(0),J=0,X=e.width,tt=e.height,G=1,st=null,dt=null,bt=new pe(0,0,X,tt),zt=new pe(0,0,X,tt),ee=!1,Y=new As,et=!1,Mt=!1,rt=new he,Ct=new he,Lt=new P,kt=new pe,de={background:null,fog:null,environment:null,overrideMaterial:null,isScene:!0},Wt=!1;function ge(){return A===null?G:1}let N=n;function Ye(M,U){return e.getContext(M,U)}try{let M={alpha:!0,depth:s,stencil:r,antialias:a,premultipliedAlpha:c,preserveDrawingBuffer:u,powerPreference:f,failIfMajorPerformanceCaveat:p};if("setAttribute"in e&&e.setAttribute("data-engine","three.js r170"),e.addEventListener("webglcontextlost",$,!1),e.addEventListener("webglcontextrestored",lt,!1),e.addEventListener("webglcontextcreationerror",at,!1),N===null){let U="webgl2";if(N=Ye(U,M),N===null)throw Ye(U)?new Error("Error creating WebGL context with your selected attributes."):new Error("Error creating WebGL context.")}}catch(M){throw console.error("THREE.WebGLRenderer: "+M.message),M}let Ht,Vt,Tt,oe,wt,w,x,O,Z,K,q,St,ot,ft,Xt,j,pt,Rt,It,mt,Gt,Nt,se,D;function it(){Ht=new Pm(N),Ht.init(),Nt=new l0(N,Ht),Vt=new wm(N,Ht,t,Nt),Tt=new o0(N,Ht),Vt.reverseDepthBuffer&&h&&Tt.buffers.depth.setReversed(!0),oe=new Um(N),wt=new Yg,w=new c0(N,Ht,Tt,wt,Vt,Nt,oe),x=new Am(y),O=new Im(y),Z=new kd(N),se=new Em(N,Z),K=new Lm(N,Z,oe,se),q=new Nm(N,K,Z,oe),It=new Fm(N,Vt,w),j=new Tm(wt),St=new qg(y,x,O,Ht,Vt,se,j),ot=new p0(y,wt),ft=new $g,Xt=new e0(Ht),Rt=new Sm(y,x,O,Tt,q,d,c),pt=new s0(y,q,Vt),D=new m0(N,oe,Vt,Tt),mt=new bm(N,Ht,oe),Gt=new Dm(N,Ht,oe),oe.programs=St.programs,y.capabilities=Vt,y.extensions=Ht,y.properties=wt,y.renderLists=ft,y.shadowMap=pt,y.state=Tt,y.info=oe}it();let V=new rc(y,N);this.xr=V,this.getContext=function(){return N},this.getContextAttributes=function(){return N.getContextAttributes()},this.forceContextLoss=function(){let M=Ht.get("WEBGL_lose_context");M&&M.loseContext()},this.forceContextRestore=function(){let M=Ht.get("WEBGL_lose_context");M&&M.restoreContext()},this.getPixelRatio=function(){return G},this.setPixelRatio=function(M){M!==void 0&&(G=M,this.setSize(X,tt,!1))},this.getSize=function(M){return M.set(X,tt)},this.setSize=function(M,U,B=!0){if(V.isPresenting){console.warn("THREE.WebGLRenderer: Can't change size while VR device is presenting.");return}X=M,tt=U,e.width=Math.floor(M*G),e.height=Math.floor(U*G),B===!0&&(e.style.width=M+"px",e.style.height=U+"px"),this.setViewport(0,0,M,U)},this.getDrawingBufferSize=function(M){return M.set(X*G,tt*G).floor()},this.setDrawingBufferSize=function(M,U,B){X=M,tt=U,G=B,e.width=Math.floor(M*B),e.height=Math.floor(U*B),this.setViewport(0,0,M,U)},this.getCurrentViewport=function(M){return M.copy(C)},this.getViewport=function(M){return M.copy(bt)},this.setViewport=function(M,U,B,z){M.isVector4?bt.set(M.x,M.y,M.z,M.w):bt.set(M,U,B,z),Tt.viewport(C.copy(bt).multiplyScalar(G).round())},this.getScissor=function(M){return M.copy(zt)},this.setScissor=function(M,U,B,z){M.isVector4?zt.set(M.x,M.y,M.z,M.w):zt.set(M,U,B,z),Tt.scissor(H.copy(zt).multiplyScalar(G).round())},this.getScissorTest=function(){return ee},this.setScissorTest=function(M){Tt.setScissorTest(ee=M)},this.setOpaqueSort=function(M){st=M},this.setTransparentSort=function(M){dt=M},this.getClearColor=function(M){return M.copy(Rt.getClearColor())},this.setClearColor=function(){Rt.setClearColor.apply(Rt,arguments)},this.getClearAlpha=function(){return Rt.getClearAlpha()},this.setClearAlpha=function(){Rt.setClearAlpha.apply(Rt,arguments)},this.clear=function(M=!0,U=!0,B=!0){let z=0;if(M){let F=!1;if(A!==null){let Q=A.texture.format;F=Q===Uc||Q===Dc||Q===Lc}if(F){let Q=A.texture.type,ct=Q===In||Q===di||Q===Ts||Q===Ki||Q===Ic||Q===Pc,xt=Rt.getClearColor(),yt=Rt.getClearAlpha(),Pt=xt.r,Ut=xt.g,vt=xt.b;ct?(g[0]=Pt,g[1]=Ut,g[2]=vt,g[3]=yt,N.clearBufferuiv(N.COLOR,0,g)):(_[0]=Pt,_[1]=Ut,_[2]=vt,_[3]=yt,N.clearBufferiv(N.COLOR,0,_))}else z|=N.COLOR_BUFFER_BIT}U&&(z|=N.DEPTH_BUFFER_BIT),B&&(z|=N.STENCIL_BUFFER_BIT,this.state.buffers.stencil.setMask(4294967295)),N.clear(z)},this.clearColor=function(){this.clear(!0,!1,!1)},this.clearDepth=function(){this.clear(!1,!0,!1)},this.clearStencil=function(){this.clear(!1,!1,!0)},this.dispose=function(){e.removeEventListener("webglcontextlost",$,!1),e.removeEventListener("webglcontextrestored",lt,!1),e.removeEventListener("webglcontextcreationerror",at,!1),ft.dispose(),Xt.dispose(),wt.dispose(),x.dispose(),O.dispose(),q.dispose(),se.dispose(),D.dispose(),St.dispose(),V.dispose(),V.removeEventListener("sessionstart",il),V.removeEventListener("sessionend",sl),ti.stop()};function $(M){M.preventDefault(),console.log("THREE.WebGLRenderer: Context Lost."),I=!0}function lt(){console.log("THREE.WebGLRenderer: Context Restored."),I=!1;let M=oe.autoReset,U=pt.enabled,B=pt.autoUpdate,z=pt.needsUpdate,F=pt.type;it(),oe.autoReset=M,pt.enabled=U,pt.autoUpdate=B,pt.needsUpdate=z,pt.type=F}function at(M){console.error("THREE.WebGLRenderer: A WebGL context could not be created. Reason: ",M.statusMessage)}function Dt(M){let U=M.target;U.removeEventListener("dispose",Dt),fe(U)}function fe(M){Ae(M),wt.remove(M)}function Ae(M){let U=wt.get(M).programs;U!==void 0&&(U.forEach(function(B){St.releaseProgram(B)}),M.isShaderMaterial&&St.releaseShaderCache(M))}this.renderBufferDirect=function(M,U,B,z,F,Q){U===null&&(U=de);let ct=F.isMesh&&F.matrixWorld.determinant()<0,xt=Au(M,U,B,z,F);Tt.setMaterial(z,ct);let yt=B.index,Pt=1;if(z.wireframe===!0){if(yt=K.getWireframeAttribute(B),yt===void 0)return;Pt=2}let Ut=B.drawRange,vt=B.attributes.position,Yt=Ut.start*Pt,re=(Ut.start+Ut.count)*Pt;Q!==null&&(Yt=Math.max(Yt,Q.start*Pt),re=Math.min(re,(Q.start+Q.count)*Pt)),yt!==null?(Yt=Math.max(Yt,0),re=Math.min(re,yt.count)):vt!=null&&(Yt=Math.max(Yt,0),re=Math.min(re,vt.count));let ae=re-Yt;if(ae<0||ae===1/0)return;se.setup(F,z,xt,B,yt);let Be,Zt=mt;if(yt!==null&&(Be=Z.get(yt),Zt=Gt,Zt.setIndex(Be)),F.isMesh)z.wireframe===!0?(Tt.setLineWidth(z.wireframeLinewidth*ge()),Zt.setMode(N.LINES)):Zt.setMode(N.TRIANGLES);else if(F.isLine){let Et=z.linewidth;Et===void 0&&(Et=1),Tt.setLineWidth(Et*ge()),F.isLineSegments?Zt.setMode(N.LINES):F.isLineLoop?Zt.setMode(N.LINE_LOOP):Zt.setMode(N.LINE_STRIP)}else F.isPoints?Zt.setMode(N.POINTS):F.isSprite&&Zt.setMode(N.TRIANGLES);if(F.isBatchedMesh)if(F._multiDrawInstances!==null)Zt.renderMultiDrawInstances(F._multiDrawStarts,F._multiDrawCounts,F._multiDrawCount,F._multiDrawInstances);else if(Ht.get("WEBGL_multi_draw"))Zt.renderMultiDraw(F._multiDrawStarts,F._multiDrawCounts,F._multiDrawCount);else{let Et=F._multiDrawStarts,xn=F._multiDrawCounts,$t=F._multiDrawCount,en=yt?Z.get(yt).bytesPerElement:1,bi=wt.get(z).currentProgram.getUniforms();for(let Ve=0;Ve<$t;Ve++)bi.setValue(N,"_gl_DrawID",Ve),Zt.render(Et[Ve]/en,xn[Ve])}else if(F.isInstancedMesh)Zt.renderInstances(Yt,ae,F.count);else if(B.isInstancedBufferGeometry){let Et=B._maxInstanceCount!==void 0?B._maxInstanceCount:1/0,xn=Math.min(B.instanceCount,Et);Zt.renderInstances(Yt,ae,xn)}else Zt.render(Yt,ae)};function jt(M,U,B){M.transparent===!0&&M.side===ze&&M.forceSinglePass===!1?(M.side=ke,M.needsUpdate=!0,Xs(M,U,B),M.side=Jn,M.needsUpdate=!0,Xs(M,U,B),M.side=ze):Xs(M,U,B)}this.compile=function(M,U,B=null){B===null&&(B=M),l=Xt.get(B),l.init(U),b.push(l),B.traverseVisible(function(F){F.isLight&&F.layers.test(U.layers)&&(l.pushLight(F),F.castShadow&&l.pushShadow(F))}),M!==B&&M.traverseVisible(function(F){F.isLight&&F.layers.test(U.layers)&&(l.pushLight(F),F.castShadow&&l.pushShadow(F))}),l.setupLights();let z=new Set;return M.traverse(function(F){if(!(F.isMesh||F.isPoints||F.isLine||F.isSprite))return;let Q=F.material;if(Q)if(Array.isArray(Q))for(let ct=0;ct<Q.length;ct++){let xt=Q[ct];jt(xt,B,F),z.add(xt)}else jt(Q,B,F),z.add(Q)}),b.pop(),l=null,z},this.compileAsync=function(M,U,B=null){let z=this.compile(M,U,B);return new Promise(F=>{function Q(){if(z.forEach(function(ct){wt.get(ct).currentProgram.isReady()&&z.delete(ct)}),z.size===0){F(M);return}setTimeout(Q,10)}Ht.get("KHR_parallel_shader_compile")!==null?Q():setTimeout(Q,10)})};let tn=null;function _n(M){tn&&tn(M)}function il(){ti.stop()}function sl(){ti.start()}let ti=new Fh;ti.setAnimationLoop(_n),typeof self<"u"&&ti.setContext(self),this.setAnimationLoop=function(M){tn=M,V.setAnimationLoop(M),M===null?ti.stop():ti.start()},V.addEventListener("sessionstart",il),V.addEventListener("sessionend",sl),this.render=function(M,U){if(U!==void 0&&U.isCamera!==!0){console.error("THREE.WebGLRenderer.render: camera is not an instance of THREE.Camera.");return}if(I===!0)return;if(M.matrixWorldAutoUpdate===!0&&M.updateMatrixWorld(),U.parent===null&&U.matrixWorldAutoUpdate===!0&&U.updateMatrixWorld(),V.enabled===!0&&V.isPresenting===!0&&(V.cameraAutoUpdate===!0&&V.updateCamera(U),U=V.getCamera()),M.isScene===!0&&M.onBeforeRender(y,M,U,A),l=Xt.get(M,b.length),l.init(U),b.push(l),Ct.multiplyMatrices(U.projectionMatrix,U.matrixWorldInverse),Y.setFromProjectionMatrix(Ct),Mt=this.localClippingEnabled,et=j.init(this.clippingPlanes,Mt),m=ft.get(M,S.length),m.init(),S.push(m),V.enabled===!0&&V.isPresenting===!0){let Q=y.xr.getDepthSensingMesh();Q!==null&&Mo(Q,U,-1/0,y.sortObjects)}Mo(M,U,0,y.sortObjects),m.finish(),y.sortObjects===!0&&m.sort(st,dt),Wt=V.enabled===!1||V.isPresenting===!1||V.hasDepthSensing()===!1,Wt&&Rt.addToRenderList(m,M),this.info.render.frame++,et===!0&&j.beginShadows();let B=l.state.shadowsArray;pt.render(B,M,U),et===!0&&j.endShadows(),this.info.autoReset===!0&&this.info.reset();let z=m.opaque,F=m.transmissive;if(l.setupLights(),U.isArrayCamera){let Q=U.cameras;if(F.length>0)for(let ct=0,xt=Q.length;ct<xt;ct++){let yt=Q[ct];ol(z,F,M,yt)}Wt&&Rt.render(M);for(let ct=0,xt=Q.length;ct<xt;ct++){let yt=Q[ct];rl(m,M,yt,yt.viewport)}}else F.length>0&&ol(z,F,M,U),Wt&&Rt.render(M),rl(m,M,U);A!==null&&(w.updateMultisampleRenderTarget(A),w.updateRenderTargetMipmap(A)),M.isScene===!0&&M.onAfterRender(y,M,U),se.resetDefaultState(),E=-1,v=null,b.pop(),b.length>0?(l=b[b.length-1],et===!0&&j.setGlobalState(y.clippingPlanes,l.state.camera)):l=null,S.pop(),S.length>0?m=S[S.length-1]:m=null};function Mo(M,U,B,z){if(M.visible===!1)return;if(M.layers.test(U.layers)){if(M.isGroup)B=M.renderOrder;else if(M.isLOD)M.autoUpdate===!0&&M.update(U);else if(M.isLight)l.pushLight(M),M.castShadow&&l.pushShadow(M);else if(M.isSprite){if(!M.frustumCulled||Y.intersectsSprite(M)){z&&kt.setFromMatrixPosition(M.matrixWorld).applyMatrix4(Ct);let ct=q.update(M),xt=M.material;xt.visible&&m.push(M,ct,xt,B,kt.z,null)}}else if((M.isMesh||M.isLine||M.isPoints)&&(!M.frustumCulled||Y.intersectsObject(M))){let ct=q.update(M),xt=M.material;if(z&&(M.boundingSphere!==void 0?(M.boundingSphere===null&&M.computeBoundingSphere(),kt.copy(M.boundingSphere.center)):(ct.boundingSphere===null&&ct.computeBoundingSphere(),kt.copy(ct.boundingSphere.center)),kt.applyMatrix4(M.matrixWorld).applyMatrix4(Ct)),Array.isArray(xt)){let yt=ct.groups;for(let Pt=0,Ut=yt.length;Pt<Ut;Pt++){let vt=yt[Pt],Yt=xt[vt.materialIndex];Yt&&Yt.visible&&m.push(M,ct,Yt,B,kt.z,vt)}}else xt.visible&&m.push(M,ct,xt,B,kt.z,null)}}let Q=M.children;for(let ct=0,xt=Q.length;ct<xt;ct++)Mo(Q[ct],U,B,z)}function rl(M,U,B,z){let F=M.opaque,Q=M.transmissive,ct=M.transparent;l.setupLightsView(B),et===!0&&j.setGlobalState(y.clippingPlanes,B),z&&Tt.viewport(C.copy(z)),F.length>0&&Ws(F,U,B),Q.length>0&&Ws(Q,U,B),ct.length>0&&Ws(ct,U,B),Tt.buffers.depth.setTest(!0),Tt.buffers.depth.setMask(!0),Tt.buffers.color.setMask(!0),Tt.setPolygonOffset(!1)}function ol(M,U,B,z){if((B.isScene===!0?B.overrideMaterial:null)!==null)return;l.state.transmissionRenderTarget[z.id]===void 0&&(l.state.transmissionRenderTarget[z.id]=new Pn(1,1,{generateMipmaps:!0,type:Ht.has("EXT_color_buffer_half_float")||Ht.has("EXT_color_buffer_float")?Fs:In,minFilter:ui,samples:4,stencilBuffer:r,resolveDepthBuffer:!1,resolveStencilBuffer:!1,colorSpace:qt.workingColorSpace}));let Q=l.state.transmissionRenderTarget[z.id],ct=z.viewport||C;Q.setSize(ct.z,ct.w);let xt=y.getRenderTarget();y.setRenderTarget(Q),y.getClearColor(W),J=y.getClearAlpha(),J<1&&y.setClearColor(16777215,.5),y.clear(),Wt&&Rt.render(B);let yt=y.toneMapping;y.toneMapping=Zn;let Pt=z.viewport;if(z.viewport!==void 0&&(z.viewport=void 0),l.setupLightsView(z),et===!0&&j.setGlobalState(y.clippingPlanes,z),Ws(M,B,z),w.updateMultisampleRenderTarget(Q),w.updateRenderTargetMipmap(Q),Ht.has("WEBGL_multisampled_render_to_texture")===!1){let Ut=!1;for(let vt=0,Yt=U.length;vt<Yt;vt++){let re=U[vt],ae=re.object,Be=re.geometry,Zt=re.material,Et=re.group;if(Zt.side===ze&&ae.layers.test(z.layers)){let xn=Zt.side;Zt.side=ke,Zt.needsUpdate=!0,al(ae,B,z,Be,Zt,Et),Zt.side=xn,Zt.needsUpdate=!0,Ut=!0}}Ut===!0&&(w.updateMultisampleRenderTarget(Q),w.updateRenderTargetMipmap(Q))}y.setRenderTarget(xt),y.setClearColor(W,J),Pt!==void 0&&(z.viewport=Pt),y.toneMapping=yt}function Ws(M,U,B){let z=U.isScene===!0?U.overrideMaterial:null;for(let F=0,Q=M.length;F<Q;F++){let ct=M[F],xt=ct.object,yt=ct.geometry,Pt=z===null?ct.material:z,Ut=ct.group;xt.layers.test(B.layers)&&al(xt,U,B,yt,Pt,Ut)}}function al(M,U,B,z,F,Q){M.onBeforeRender(y,U,B,z,F,Q),M.modelViewMatrix.multiplyMatrices(B.matrixWorldInverse,M.matrixWorld),M.normalMatrix.getNormalMatrix(M.modelViewMatrix),F.onBeforeRender(y,U,B,z,M,Q),F.transparent===!0&&F.side===ze&&F.forceSinglePass===!1?(F.side=ke,F.needsUpdate=!0,y.renderBufferDirect(B,U,z,F,M,Q),F.side=Jn,F.needsUpdate=!0,y.renderBufferDirect(B,U,z,F,M,Q),F.side=ze):y.renderBufferDirect(B,U,z,F,M,Q),M.onAfterRender(y,U,B,z,F,Q)}function Xs(M,U,B){U.isScene!==!0&&(U=de);let z=wt.get(M),F=l.state.lights,Q=l.state.shadowsArray,ct=F.state.version,xt=St.getParameters(M,F.state,Q,U,B),yt=St.getProgramCacheKey(xt),Pt=z.programs;z.environment=M.isMeshStandardMaterial?U.environment:null,z.fog=U.fog,z.envMap=(M.isMeshStandardMaterial?O:x).get(M.envMap||z.environment),z.envMapRotation=z.environment!==null&&M.envMap===null?U.environmentRotation:M.envMapRotation,Pt===void 0&&(M.addEventListener("dispose",Dt),Pt=new Map,z.programs=Pt);let Ut=Pt.get(yt);if(Ut!==void 0){if(z.currentProgram===Ut&&z.lightsStateVersion===ct)return ll(M,xt),Ut}else xt.uniforms=St.getUniforms(M),M.onBeforeCompile(xt,y),Ut=St.acquireProgram(xt,yt),Pt.set(yt,Ut),z.uniforms=xt.uniforms;let vt=z.uniforms;return(!M.isShaderMaterial&&!M.isRawShaderMaterial||M.clipping===!0)&&(vt.clippingPlanes=j.uniform),ll(M,xt),z.needsLights=Cu(M),z.lightsStateVersion=ct,z.needsLights&&(vt.ambientLightColor.value=F.state.ambient,vt.lightProbe.value=F.state.probe,vt.directionalLights.value=F.state.directional,vt.directionalLightShadows.value=F.state.directionalShadow,vt.spotLights.value=F.state.spot,vt.spotLightShadows.value=F.state.spotShadow,vt.rectAreaLights.value=F.state.rectArea,vt.ltc_1.value=F.state.rectAreaLTC1,vt.ltc_2.value=F.state.rectAreaLTC2,vt.pointLights.value=F.state.point,vt.pointLightShadows.value=F.state.pointShadow,vt.hemisphereLights.value=F.state.hemi,vt.directionalShadowMap.value=F.state.directionalShadowMap,vt.directionalShadowMatrix.value=F.state.directionalShadowMatrix,vt.spotShadowMap.value=F.state.spotShadowMap,vt.spotLightMatrix.value=F.state.spotLightMatrix,vt.spotLightMap.value=F.state.spotLightMap,vt.pointShadowMap.value=F.state.pointShadowMap,vt.pointShadowMatrix.value=F.state.pointShadowMatrix),z.currentProgram=Ut,z.uniformsList=null,Ut}function cl(M){if(M.uniformsList===null){let U=M.currentProgram.getUniforms();M.uniformsList=Yi.seqWithValue(U.seq,M.uniforms)}return M.uniformsList}function ll(M,U){let B=wt.get(M);B.outputColorSpace=U.outputColorSpace,B.batching=U.batching,B.batchingColor=U.batchingColor,B.instancing=U.instancing,B.instancingColor=U.instancingColor,B.instancingMorph=U.instancingMorph,B.skinning=U.skinning,B.morphTargets=U.morphTargets,B.morphNormals=U.morphNormals,B.morphColors=U.morphColors,B.morphTargetsCount=U.morphTargetsCount,B.numClippingPlanes=U.numClippingPlanes,B.numIntersection=U.numClipIntersection,B.vertexAlphas=U.vertexAlphas,B.vertexTangents=U.vertexTangents,B.toneMapping=U.toneMapping}function Au(M,U,B,z,F){U.isScene!==!0&&(U=de),w.resetTextureUnits();let Q=U.fog,ct=z.isMeshStandardMaterial?U.environment:null,xt=A===null?y.outputColorSpace:A.isXRRenderTarget===!0?A.texture.colorSpace:ns,yt=(z.isMeshStandardMaterial?O:x).get(z.envMap||ct),Pt=z.vertexColors===!0&&!!B.attributes.color&&B.attributes.color.itemSize===4,Ut=!!B.attributes.tangent&&(!!z.normalMap||z.anisotropy>0),vt=!!B.morphAttributes.position,Yt=!!B.morphAttributes.normal,re=!!B.morphAttributes.color,ae=Zn;z.toneMapped&&(A===null||A.isXRRenderTarget===!0)&&(ae=y.toneMapping);let Be=B.morphAttributes.position||B.morphAttributes.normal||B.morphAttributes.color,Zt=Be!==void 0?Be.length:0,Et=wt.get(z),xn=l.state.lights;if(et===!0&&(Mt===!0||M!==v)){let Ze=M===v&&z.id===E;j.setState(z,M,Ze)}let $t=!1;z.version===Et.__version?(Et.needsLights&&Et.lightsStateVersion!==xn.state.version||Et.outputColorSpace!==xt||F.isBatchedMesh&&Et.batching===!1||!F.isBatchedMesh&&Et.batching===!0||F.isBatchedMesh&&Et.batchingColor===!0&&F.colorTexture===null||F.isBatchedMesh&&Et.batchingColor===!1&&F.colorTexture!==null||F.isInstancedMesh&&Et.instancing===!1||!F.isInstancedMesh&&Et.instancing===!0||F.isSkinnedMesh&&Et.skinning===!1||!F.isSkinnedMesh&&Et.skinning===!0||F.isInstancedMesh&&Et.instancingColor===!0&&F.instanceColor===null||F.isInstancedMesh&&Et.instancingColor===!1&&F.instanceColor!==null||F.isInstancedMesh&&Et.instancingMorph===!0&&F.morphTexture===null||F.isInstancedMesh&&Et.instancingMorph===!1&&F.morphTexture!==null||Et.envMap!==yt||z.fog===!0&&Et.fog!==Q||Et.numClippingPlanes!==void 0&&(Et.numClippingPlanes!==j.numPlanes||Et.numIntersection!==j.numIntersection)||Et.vertexAlphas!==Pt||Et.vertexTangents!==Ut||Et.morphTargets!==vt||Et.morphNormals!==Yt||Et.morphColors!==re||Et.toneMapping!==ae||Et.morphTargetsCount!==Zt)&&($t=!0):($t=!0,Et.__version=z.version);let en=Et.currentProgram;$t===!0&&(en=Xs(z,U,F));let bi=!1,Ve=!1,ds=!1,ce=en.getUniforms(),un=Et.uniforms;if(Tt.useProgram(en.program)&&(bi=!0,Ve=!0,ds=!0),z.id!==E&&(E=z.id,Ve=!0),bi||v!==M){Tt.buffers.depth.getReversed()?(rt.copy(M.projectionMatrix),vd(rt),Md(rt),ce.setValue(N,"projectionMatrix",rt)):ce.setValue(N,"projectionMatrix",M.projectionMatrix),ce.setValue(N,"viewMatrix",M.matrixWorldInverse);let On=ce.map.cameraPosition;On!==void 0&&On.setValue(N,Lt.setFromMatrixPosition(M.matrixWorld)),Vt.logarithmicDepthBuffer&&ce.setValue(N,"logDepthBufFC",2/(Math.log(M.far+1)/Math.LN2)),(z.isMeshPhongMaterial||z.isMeshToonMaterial||z.isMeshLambertMaterial||z.isMeshBasicMaterial||z.isMeshStandardMaterial||z.isShaderMaterial)&&ce.setValue(N,"isOrthographic",M.isOrthographicCamera===!0),v!==M&&(v=M,Ve=!0,ds=!0)}if(F.isSkinnedMesh){ce.setOptional(N,F,"bindMatrix"),ce.setOptional(N,F,"bindMatrixInverse");let Ze=F.skeleton;Ze&&(Ze.boneTexture===null&&Ze.computeBoneTexture(),ce.setValue(N,"boneTexture",Ze.boneTexture,w))}F.isBatchedMesh&&(ce.setOptional(N,F,"batchingTexture"),ce.setValue(N,"batchingTexture",F._matricesTexture,w),ce.setOptional(N,F,"batchingIdTexture"),ce.setValue(N,"batchingIdTexture",F._indirectTexture,w),ce.setOptional(N,F,"batchingColorTexture"),F._colorsTexture!==null&&ce.setValue(N,"batchingColorTexture",F._colorsTexture,w));let fs=B.morphAttributes;if((fs.position!==void 0||fs.normal!==void 0||fs.color!==void 0)&&It.update(F,B,en),(Ve||Et.receiveShadow!==F.receiveShadow)&&(Et.receiveShadow=F.receiveShadow,ce.setValue(N,"receiveShadow",F.receiveShadow)),z.isMeshGouraudMaterial&&z.envMap!==null&&(un.envMap.value=yt,un.flipEnvMap.value=yt.isCubeTexture&&yt.isRenderTargetTexture===!1?-1:1),z.isMeshStandardMaterial&&z.envMap===null&&U.environment!==null&&(un.envMapIntensity.value=U.environmentIntensity),Ve&&(ce.setValue(N,"toneMappingExposure",y.toneMappingExposure),Et.needsLights&&Ru(un,ds),Q&&z.fog===!0&&ot.refreshFogUniforms(un,Q),ot.refreshMaterialUniforms(un,z,G,tt,l.state.transmissionRenderTarget[M.id]),Yi.upload(N,cl(Et),un,w)),z.isShaderMaterial&&z.uniformsNeedUpdate===!0&&(Yi.upload(N,cl(Et),un,w),z.uniformsNeedUpdate=!1),z.isSpriteMaterial&&ce.setValue(N,"center",F.center),ce.setValue(N,"modelViewMatrix",F.modelViewMatrix),ce.setValue(N,"normalMatrix",F.normalMatrix),ce.setValue(N,"modelMatrix",F.matrixWorld),z.isShaderMaterial||z.isRawShaderMaterial){let Ze=z.uniformsGroups;for(let On=0,Bn=Ze.length;On<Bn;On++){let hl=Ze[On];D.update(hl,en),D.bind(hl,en)}}return en}function Ru(M,U){M.ambientLightColor.needsUpdate=U,M.lightProbe.needsUpdate=U,M.directionalLights.needsUpdate=U,M.directionalLightShadows.needsUpdate=U,M.pointLights.needsUpdate=U,M.pointLightShadows.needsUpdate=U,M.spotLights.needsUpdate=U,M.spotLightShadows.needsUpdate=U,M.rectAreaLights.needsUpdate=U,M.hemisphereLights.needsUpdate=U}function Cu(M){return M.isMeshLambertMaterial||M.isMeshToonMaterial||M.isMeshPhongMaterial||M.isMeshStandardMaterial||M.isShadowMaterial||M.isShaderMaterial&&M.lights===!0}this.getActiveCubeFace=function(){return T},this.getActiveMipmapLevel=function(){return R},this.getRenderTarget=function(){return A},this.setRenderTargetTextures=function(M,U,B){wt.get(M.texture).__webglTexture=U,wt.get(M.depthTexture).__webglTexture=B;let z=wt.get(M);z.__hasExternalTextures=!0,z.__autoAllocateDepthBuffer=B===void 0,z.__autoAllocateDepthBuffer||Ht.has("WEBGL_multisampled_render_to_texture")===!0&&(console.warn("THREE.WebGLRenderer: Render-to-texture extension was disabled because an external texture was provided"),z.__useRenderToTexture=!1)},this.setRenderTargetFramebuffer=function(M,U){let B=wt.get(M);B.__webglFramebuffer=U,B.__useDefaultFramebuffer=U===void 0},this.setRenderTarget=function(M,U=0,B=0){A=M,T=U,R=B;let z=!0,F=null,Q=!1,ct=!1;if(M){let yt=wt.get(M);if(yt.__useDefaultFramebuffer!==void 0)Tt.bindFramebuffer(N.FRAMEBUFFER,null),z=!1;else if(yt.__webglFramebuffer===void 0)w.setupRenderTarget(M);else if(yt.__hasExternalTextures)w.rebindTextures(M,wt.get(M.texture).__webglTexture,wt.get(M.depthTexture).__webglTexture);else if(M.depthBuffer){let vt=M.depthTexture;if(yt.__boundDepthTexture!==vt){if(vt!==null&&wt.has(vt)&&(M.width!==vt.image.width||M.height!==vt.image.height))throw new Error("WebGLRenderTarget: Attached DepthTexture is initialized to the incorrect size.");w.setupDepthRenderbuffer(M)}}let Pt=M.texture;(Pt.isData3DTexture||Pt.isDataArrayTexture||Pt.isCompressedArrayTexture)&&(ct=!0);let Ut=wt.get(M).__webglFramebuffer;M.isWebGLCubeRenderTarget?(Array.isArray(Ut[U])?F=Ut[U][B]:F=Ut[U],Q=!0):M.samples>0&&w.useMultisampledRTT(M)===!1?F=wt.get(M).__webglMultisampledFramebuffer:Array.isArray(Ut)?F=Ut[B]:F=Ut,C.copy(M.viewport),H.copy(M.scissor),k=M.scissorTest}else C.copy(bt).multiplyScalar(G).floor(),H.copy(zt).multiplyScalar(G).floor(),k=ee;if(Tt.bindFramebuffer(N.FRAMEBUFFER,F)&&z&&Tt.drawBuffers(M,F),Tt.viewport(C),Tt.scissor(H),Tt.setScissorTest(k),Q){let yt=wt.get(M.texture);N.framebufferTexture2D(N.FRAMEBUFFER,N.COLOR_ATTACHMENT0,N.TEXTURE_CUBE_MAP_POSITIVE_X+U,yt.__webglTexture,B)}else if(ct){let yt=wt.get(M.texture),Pt=U||0;N.framebufferTextureLayer(N.FRAMEBUFFER,N.COLOR_ATTACHMENT0,yt.__webglTexture,B||0,Pt)}E=-1},this.readRenderTargetPixels=function(M,U,B,z,F,Q,ct){if(!(M&&M.isWebGLRenderTarget)){console.error("THREE.WebGLRenderer.readRenderTargetPixels: renderTarget is not THREE.WebGLRenderTarget.");return}let xt=wt.get(M).__webglFramebuffer;if(M.isWebGLCubeRenderTarget&&ct!==void 0&&(xt=xt[ct]),xt){Tt.bindFramebuffer(N.FRAMEBUFFER,xt);try{let yt=M.texture,Pt=yt.format,Ut=yt.type;if(!Vt.textureFormatReadable(Pt)){console.error("THREE.WebGLRenderer.readRenderTargetPixels: renderTarget is not in RGBA or implementation defined format.");return}if(!Vt.textureTypeReadable(Ut)){console.error("THREE.WebGLRenderer.readRenderTargetPixels: renderTarget is not in UnsignedByteType or implementation defined type.");return}U>=0&&U<=M.width-z&&B>=0&&B<=M.height-F&&N.readPixels(U,B,z,F,Nt.convert(Pt),Nt.convert(Ut),Q)}finally{let yt=A!==null?wt.get(A).__webglFramebuffer:null;Tt.bindFramebuffer(N.FRAMEBUFFER,yt)}}},this.readRenderTargetPixelsAsync=async function(M,U,B,z,F,Q,ct){if(!(M&&M.isWebGLRenderTarget))throw new Error("THREE.WebGLRenderer.readRenderTargetPixels: renderTarget is not THREE.WebGLRenderTarget.");let xt=wt.get(M).__webglFramebuffer;if(M.isWebGLCubeRenderTarget&&ct!==void 0&&(xt=xt[ct]),xt){let yt=M.texture,Pt=yt.format,Ut=yt.type;if(!Vt.textureFormatReadable(Pt))throw new Error("THREE.WebGLRenderer.readRenderTargetPixelsAsync: renderTarget is not in RGBA or implementation defined format.");if(!Vt.textureTypeReadable(Ut))throw new Error("THREE.WebGLRenderer.readRenderTargetPixelsAsync: renderTarget is not in UnsignedByteType or implementation defined type.");if(U>=0&&U<=M.width-z&&B>=0&&B<=M.height-F){Tt.bindFramebuffer(N.FRAMEBUFFER,xt);let vt=N.createBuffer();N.bindBuffer(N.PIXEL_PACK_BUFFER,vt),N.bufferData(N.PIXEL_PACK_BUFFER,Q.byteLength,N.STREAM_READ),N.readPixels(U,B,z,F,Nt.convert(Pt),Nt.convert(Ut),0);let Yt=A!==null?wt.get(A).__webglFramebuffer:null;Tt.bindFramebuffer(N.FRAMEBUFFER,Yt);let re=N.fenceSync(N.SYNC_GPU_COMMANDS_COMPLETE,0);return N.flush(),await yd(N,re,4),N.bindBuffer(N.PIXEL_PACK_BUFFER,vt),N.getBufferSubData(N.PIXEL_PACK_BUFFER,0,Q),N.deleteBuffer(vt),N.deleteSync(re),Q}else throw new Error("THREE.WebGLRenderer.readRenderTargetPixelsAsync: requested read bounds are out of range.")}},this.copyFramebufferToTexture=function(M,U=null,B=0){M.isTexture!==!0&&(Ms("WebGLRenderer: copyFramebufferToTexture function signature has changed."),U=arguments[0]||null,M=arguments[1]);let z=Math.pow(2,-B),F=Math.floor(M.image.width*z),Q=Math.floor(M.image.height*z),ct=U!==null?U.x:0,xt=U!==null?U.y:0;w.setTexture2D(M,0),N.copyTexSubImage2D(N.TEXTURE_2D,B,0,0,ct,xt,F,Q),Tt.unbindTexture()},this.copyTextureToTexture=function(M,U,B=null,z=null,F=0){M.isTexture!==!0&&(Ms("WebGLRenderer: copyTextureToTexture function signature has changed."),z=arguments[0]||null,M=arguments[1],U=arguments[2],F=arguments[3]||0,B=null);let Q,ct,xt,yt,Pt,Ut,vt,Yt,re,ae=M.isCompressedTexture?M.mipmaps[F]:M.image;B!==null?(Q=B.max.x-B.min.x,ct=B.max.y-B.min.y,xt=B.isBox3?B.max.z-B.min.z:1,yt=B.min.x,Pt=B.min.y,Ut=B.isBox3?B.min.z:0):(Q=ae.width,ct=ae.height,xt=ae.depth||1,yt=0,Pt=0,Ut=0),z!==null?(vt=z.x,Yt=z.y,re=z.z):(vt=0,Yt=0,re=0);let Be=Nt.convert(U.format),Zt=Nt.convert(U.type),Et;U.isData3DTexture?(w.setTexture3D(U,0),Et=N.TEXTURE_3D):U.isDataArrayTexture||U.isCompressedArrayTexture?(w.setTexture2DArray(U,0),Et=N.TEXTURE_2D_ARRAY):(w.setTexture2D(U,0),Et=N.TEXTURE_2D),N.pixelStorei(N.UNPACK_FLIP_Y_WEBGL,U.flipY),N.pixelStorei(N.UNPACK_PREMULTIPLY_ALPHA_WEBGL,U.premultiplyAlpha),N.pixelStorei(N.UNPACK_ALIGNMENT,U.unpackAlignment);let xn=N.getParameter(N.UNPACK_ROW_LENGTH),$t=N.getParameter(N.UNPACK_IMAGE_HEIGHT),en=N.getParameter(N.UNPACK_SKIP_PIXELS),bi=N.getParameter(N.UNPACK_SKIP_ROWS),Ve=N.getParameter(N.UNPACK_SKIP_IMAGES);N.pixelStorei(N.UNPACK_ROW_LENGTH,ae.width),N.pixelStorei(N.UNPACK_IMAGE_HEIGHT,ae.height),N.pixelStorei(N.UNPACK_SKIP_PIXELS,yt),N.pixelStorei(N.UNPACK_SKIP_ROWS,Pt),N.pixelStorei(N.UNPACK_SKIP_IMAGES,Ut);let ds=M.isDataArrayTexture||M.isData3DTexture,ce=U.isDataArrayTexture||U.isData3DTexture;if(M.isRenderTargetTexture||M.isDepthTexture){let un=wt.get(M),fs=wt.get(U),Ze=wt.get(un.__renderTarget),On=wt.get(fs.__renderTarget);Tt.bindFramebuffer(N.READ_FRAMEBUFFER,Ze.__webglFramebuffer),Tt.bindFramebuffer(N.DRAW_FRAMEBUFFER,On.__webglFramebuffer);for(let Bn=0;Bn<xt;Bn++)ds&&N.framebufferTextureLayer(N.READ_FRAMEBUFFER,N.COLOR_ATTACHMENT0,wt.get(M).__webglTexture,F,Ut+Bn),M.isDepthTexture?(ce&&N.framebufferTextureLayer(N.DRAW_FRAMEBUFFER,N.COLOR_ATTACHMENT0,wt.get(U).__webglTexture,F,re+Bn),N.blitFramebuffer(yt,Pt,Q,ct,vt,Yt,Q,ct,N.DEPTH_BUFFER_BIT,N.NEAREST)):ce?N.copyTexSubImage3D(Et,F,vt,Yt,re+Bn,yt,Pt,Q,ct):N.copyTexSubImage2D(Et,F,vt,Yt,re+Bn,yt,Pt,Q,ct);Tt.bindFramebuffer(N.READ_FRAMEBUFFER,null),Tt.bindFramebuffer(N.DRAW_FRAMEBUFFER,null)}else ce?M.isDataTexture||M.isData3DTexture?N.texSubImage3D(Et,F,vt,Yt,re,Q,ct,xt,Be,Zt,ae.data):U.isCompressedArrayTexture?N.compressedTexSubImage3D(Et,F,vt,Yt,re,Q,ct,xt,Be,ae.data):N.texSubImage3D(Et,F,vt,Yt,re,Q,ct,xt,Be,Zt,ae):M.isDataTexture?N.texSubImage2D(N.TEXTURE_2D,F,vt,Yt,Q,ct,Be,Zt,ae.data):M.isCompressedTexture?N.compressedTexSubImage2D(N.TEXTURE_2D,F,vt,Yt,ae.width,ae.height,Be,ae.data):N.texSubImage2D(N.TEXTURE_2D,F,vt,Yt,Q,ct,Be,Zt,ae);N.pixelStorei(N.UNPACK_ROW_LENGTH,xn),N.pixelStorei(N.UNPACK_IMAGE_HEIGHT,$t),N.pixelStorei(N.UNPACK_SKIP_PIXELS,en),N.pixelStorei(N.UNPACK_SKIP_ROWS,bi),N.pixelStorei(N.UNPACK_SKIP_IMAGES,Ve),F===0&&U.generateMipmaps&&N.generateMipmap(Et),Tt.unbindTexture()},this.copyTextureToTexture3D=function(M,U,B=null,z=null,F=0){return M.isTexture!==!0&&(Ms("WebGLRenderer: copyTextureToTexture3D function signature has changed."),B=arguments[0]||null,z=arguments[1]||null,M=arguments[2],U=arguments[3],F=arguments[4]||0),Ms('WebGLRenderer: copyTextureToTexture3D function has been deprecated. Use "copyTextureToTexture" instead.'),this.copyTextureToTexture(M,U,B,z,F)},this.initRenderTarget=function(M){wt.get(M).__webglFramebuffer===void 0&&w.setupRenderTarget(M)},this.initTexture=function(M){M.isCubeTexture?w.setTextureCube(M,0):M.isData3DTexture?w.setTexture3D(M,0):M.isDataArrayTexture||M.isCompressedArrayTexture?w.setTexture2DArray(M,0):w.setTexture2D(M,0),Tt.unbindTexture()},this.resetState=function(){T=0,R=0,A=null,Tt.reset(),se.reset()},typeof __THREE_DEVTOOLS__<"u"&&__THREE_DEVTOOLS__.dispatchEvent(new CustomEvent("observe",{detail:this}))}get coordinateSystem(){return An}get outputColorSpace(){return this._outputColorSpace}set outputColorSpace(t){this._outputColorSpace=t;let e=this.getContext();e.drawingBufferColorspace=qt._getDrawingBufferColorSpace(t),e.unpackColorSpace=qt._getUnpackColorSpace()}};var kr=class i{constructor(t,e=1,n=1e3){this.isFog=!0,this.name="",this.color=new Bt(t),this.near=e,this.far=n}clone(){return new i(this.color,this.near,this.far)}toJSON(){return{type:"Fog",name:this.name,color:this.color.getHex(),near:this.near,far:this.far}}},Hr=class extends Te{constructor(){super(),this.isScene=!0,this.type="Scene",this.background=null,this.environment=null,this.fog=null,this.backgroundBlurriness=0,this.backgroundIntensity=1,this.backgroundRotation=new mn,this.environmentIntensity=1,this.environmentRotation=new mn,this.overrideMaterial=null,typeof __THREE_DEVTOOLS__<"u"&&__THREE_DEVTOOLS__.dispatchEvent(new CustomEvent("observe",{detail:this}))}copy(t,e){return super.copy(t,e),t.background!==null&&(this.background=t.background.clone()),t.environment!==null&&(this.environment=t.environment.clone()),t.fog!==null&&(this.fog=t.fog.clone()),this.backgroundBlurriness=t.backgroundBlurriness,this.backgroundIntensity=t.backgroundIntensity,this.backgroundRotation.copy(t.backgroundRotation),this.environmentIntensity=t.environmentIntensity,this.environmentRotation.copy(t.environmentRotation),t.overrideMaterial!==null&&(this.overrideMaterial=t.overrideMaterial.clone()),this.matrixAutoUpdate=t.matrixAutoUpdate,this}toJSON(t){let e=super.toJSON(t);return this.fog!==null&&(e.object.fog=this.fog.toJSON()),this.backgroundBlurriness>0&&(e.object.backgroundBlurriness=this.backgroundBlurriness),this.backgroundIntensity!==1&&(e.object.backgroundIntensity=this.backgroundIntensity),e.object.backgroundRotation=this.backgroundRotation.toArray(),this.environmentIntensity!==1&&(e.object.environmentIntensity=this.environmentIntensity),e.object.environmentRotation=this.environmentRotation.toArray(),e}},oc=class{constructor(t,e){this.isInterleavedBuffer=!0,this.array=t,this.stride=e,this.count=t!==void 0?t.length/e:0,this.usage=Va,this.updateRanges=[],this.version=0,this.uuid=$n()}onUploadCallback(){}set needsUpdate(t){t===!0&&this.version++}setUsage(t){return this.usage=t,this}addUpdateRange(t,e){this.updateRanges.push({start:t,count:e})}clearUpdateRanges(){this.updateRanges.length=0}copy(t){return this.array=new t.array.constructor(t.array),this.count=t.count,this.stride=t.stride,this.usage=t.usage,this}copyAt(t,e,n){t*=this.stride,n*=e.stride;for(let s=0,r=this.stride;s<r;s++)this.array[t+s]=e.array[n+s];return this}set(t,e=0){return this.array.set(t,e),this}clone(t){t.arrayBuffers===void 0&&(t.arrayBuffers={}),this.array.buffer._uuid===void 0&&(this.array.buffer._uuid=$n()),t.arrayBuffers[this.array.buffer._uuid]===void 0&&(t.arrayBuffers[this.array.buffer._uuid]=this.array.slice(0).buffer);let e=new this.array.constructor(t.arrayBuffers[this.array.buffer._uuid]),n=new this.constructor(e,this.stride);return n.setUsage(this.usage),n}onUpload(t){return this.onUploadCallback=t,this}toJSON(t){return t.arrayBuffers===void 0&&(t.arrayBuffers={}),this.array.buffer._uuid===void 0&&(this.array.buffer._uuid=$n()),t.arrayBuffers[this.array.buffer._uuid]===void 0&&(t.arrayBuffers[this.array.buffer._uuid]=Array.from(new Uint32Array(this.array.buffer))),{uuid:this.uuid,buffer:this.array.buffer._uuid,type:this.array.constructor.name,stride:this.stride}}},Pe=new P,Vr=class i{constructor(t,e,n,s=!1){this.isInterleavedBufferAttribute=!0,this.name="",this.data=t,this.itemSize=e,this.offset=n,this.normalized=s}get count(){return this.data.count}get array(){return this.data.array}set needsUpdate(t){this.data.needsUpdate=t}applyMatrix4(t){for(let e=0,n=this.data.count;e<n;e++)Pe.fromBufferAttribute(this,e),Pe.applyMatrix4(t),this.setXYZ(e,Pe.x,Pe.y,Pe.z);return this}applyNormalMatrix(t){for(let e=0,n=this.count;e<n;e++)Pe.fromBufferAttribute(this,e),Pe.applyNormalMatrix(t),this.setXYZ(e,Pe.x,Pe.y,Pe.z);return this}transformDirection(t){for(let e=0,n=this.count;e<n;e++)Pe.fromBufferAttribute(this,e),Pe.transformDirection(t),this.setXYZ(e,Pe.x,Pe.y,Pe.z);return this}getComponent(t,e){let n=this.array[t*this.data.stride+this.offset+e];return this.normalized&&(n=fn(n,this.array)),n}setComponent(t,e,n){return this.normalized&&(n=te(n,this.array)),this.data.array[t*this.data.stride+this.offset+e]=n,this}setX(t,e){return this.normalized&&(e=te(e,this.array)),this.data.array[t*this.data.stride+this.offset]=e,this}setY(t,e){return this.normalized&&(e=te(e,this.array)),this.data.array[t*this.data.stride+this.offset+1]=e,this}setZ(t,e){return this.normalized&&(e=te(e,this.array)),this.data.array[t*this.data.stride+this.offset+2]=e,this}setW(t,e){return this.normalized&&(e=te(e,this.array)),this.data.array[t*this.data.stride+this.offset+3]=e,this}getX(t){let e=this.data.array[t*this.data.stride+this.offset];return this.normalized&&(e=fn(e,this.array)),e}getY(t){let e=this.data.array[t*this.data.stride+this.offset+1];return this.normalized&&(e=fn(e,this.array)),e}getZ(t){let e=this.data.array[t*this.data.stride+this.offset+2];return this.normalized&&(e=fn(e,this.array)),e}getW(t){let e=this.data.array[t*this.data.stride+this.offset+3];return this.normalized&&(e=fn(e,this.array)),e}setXY(t,e,n){return t=t*this.data.stride+this.offset,this.normalized&&(e=te(e,this.array),n=te(n,this.array)),this.data.array[t+0]=e,this.data.array[t+1]=n,this}setXYZ(t,e,n,s){return t=t*this.data.stride+this.offset,this.normalized&&(e=te(e,this.array),n=te(n,this.array),s=te(s,this.array)),this.data.array[t+0]=e,this.data.array[t+1]=n,this.data.array[t+2]=s,this}setXYZW(t,e,n,s,r){return t=t*this.data.stride+this.offset,this.normalized&&(e=te(e,this.array),n=te(n,this.array),s=te(s,this.array),r=te(r,this.array)),this.data.array[t+0]=e,this.data.array[t+1]=n,this.data.array[t+2]=s,this.data.array[t+3]=r,this}clone(t){if(t===void 0){console.log("THREE.InterleavedBufferAttribute.clone(): Cloning an interleaved buffer attribute will de-interleave buffer data.");let e=[];for(let n=0;n<this.count;n++){let s=n*this.data.stride+this.offset;for(let r=0;r<this.itemSize;r++)e.push(this.data.array[s+r])}return new Ie(new this.array.constructor(e),this.itemSize,this.normalized)}else return t.interleavedBuffers===void 0&&(t.interleavedBuffers={}),t.interleavedBuffers[this.data.uuid]===void 0&&(t.interleavedBuffers[this.data.uuid]=this.data.clone(t)),new i(t.interleavedBuffers[this.data.uuid],this.itemSize,this.offset,this.normalized)}toJSON(t){if(t===void 0){console.log("THREE.InterleavedBufferAttribute.toJSON(): Serializing an interleaved buffer attribute will de-interleave buffer data.");let e=[];for(let n=0;n<this.count;n++){let s=n*this.data.stride+this.offset;for(let r=0;r<this.itemSize;r++)e.push(this.data.array[s+r])}return{itemSize:this.itemSize,type:this.array.constructor.name,array:e,normalized:this.normalized}}else return t.interleavedBuffers===void 0&&(t.interleavedBuffers={}),t.interleavedBuffers[this.data.uuid]===void 0&&(t.interleavedBuffers[this.data.uuid]=this.data.toJSON(t)),{isInterleavedBufferAttribute:!0,itemSize:this.itemSize,data:this.data.uuid,offset:this.offset,normalized:this.normalized}}},Rs=class extends Ln{static get type(){return"SpriteMaterial"}constructor(t){super(),this.isSpriteMaterial=!0,this.color=new Bt(16777215),this.map=null,this.alphaMap=null,this.rotation=0,this.sizeAttenuation=!0,this.transparent=!0,this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.alphaMap=t.alphaMap,this.rotation=t.rotation,this.sizeAttenuation=t.sizeAttenuation,this.fog=t.fog,this}},zi,xs=new P,ki=new P,Hi=new P,Vi=new ut,ys=new ut,kh=new he,fr=new P,vs=new P,pr=new P,hh=new ut,jo=new ut,uh=new ut,Gr=class extends Te{constructor(t=new Rs){if(super(),this.isSprite=!0,this.type="Sprite",zi===void 0){zi=new Ee;let e=new Float32Array([-.5,-.5,0,0,0,.5,-.5,0,1,0,.5,.5,0,1,1,-.5,.5,0,0,1]),n=new oc(e,5);zi.setIndex([0,1,2,0,2,3]),zi.setAttribute("position",new Vr(n,3,0,!1)),zi.setAttribute("uv",new Vr(n,2,3,!1))}this.geometry=zi,this.material=t,this.center=new ut(.5,.5)}raycast(t,e){t.camera===null&&console.error('THREE.Sprite: "Raycaster.camera" needs to be set in order to raycast against sprites.'),ki.setFromMatrixScale(this.matrixWorld),kh.copy(t.camera.matrixWorld),this.modelViewMatrix.multiplyMatrices(t.camera.matrixWorldInverse,this.matrixWorld),Hi.setFromMatrixPosition(this.modelViewMatrix),t.camera.isPerspectiveCamera&&this.material.sizeAttenuation===!1&&ki.multiplyScalar(-Hi.z);let n=this.material.rotation,s,r;n!==0&&(r=Math.cos(n),s=Math.sin(n));let o=this.center;mr(fr.set(-.5,-.5,0),Hi,o,ki,s,r),mr(vs.set(.5,-.5,0),Hi,o,ki,s,r),mr(pr.set(.5,.5,0),Hi,o,ki,s,r),hh.set(0,0),jo.set(1,0),uh.set(1,1);let a=t.ray.intersectTriangle(fr,vs,pr,!1,xs);if(a===null&&(mr(vs.set(-.5,.5,0),Hi,o,ki,s,r),jo.set(0,1),a=t.ray.intersectTriangle(fr,pr,vs,!1,xs),a===null))return;let c=t.ray.origin.distanceTo(xs);c<t.near||c>t.far||e.push({distance:c,point:xs.clone(),uv:qn.getInterpolation(xs,fr,vs,pr,hh,jo,uh,new ut),face:null,object:this})}copy(t,e){return super.copy(t,e),t.center!==void 0&&this.center.copy(t.center),this.material=t.material,this}};function mr(i,t,e,n,s,r){Vi.subVectors(i,e).addScalar(.5).multiply(n),s!==void 0?(ys.x=r*Vi.x-s*Vi.y,ys.y=s*Vi.x+r*Vi.y):ys.copy(Vi),i.copy(t),i.x+=ys.x,i.y+=ys.y,i.applyMatrix4(kh)}var Cs=class extends Ln{static get type(){return"PointsMaterial"}constructor(t){super(),this.isPointsMaterial=!0,this.color=new Bt(16777215),this.map=null,this.alphaMap=null,this.size=1,this.sizeAttenuation=!0,this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.alphaMap=t.alphaMap,this.size=t.size,this.sizeAttenuation=t.sizeAttenuation,this.fog=t.fog,this}},dh=new he,ac=new Ir,gr=new Qi,_r=new P,Wr=class extends Te{constructor(t=new Ee,e=new Cs){super(),this.isPoints=!0,this.type="Points",this.geometry=t,this.material=e,this.updateMorphTargets()}copy(t,e){return super.copy(t,e),this.material=Array.isArray(t.material)?t.material.slice():t.material,this.geometry=t.geometry,this}raycast(t,e){let n=this.geometry,s=this.matrixWorld,r=t.params.Points.threshold,o=n.drawRange;if(n.boundingSphere===null&&n.computeBoundingSphere(),gr.copy(n.boundingSphere),gr.applyMatrix4(s),gr.radius+=r,t.ray.intersectsSphere(gr)===!1)return;dh.copy(s).invert(),ac.copy(t.ray).applyMatrix4(dh);let a=r/((this.scale.x+this.scale.y+this.scale.z)/3),c=a*a,u=n.index,p=n.attributes.position;if(u!==null){let h=Math.max(0,o.start),d=Math.min(u.count,o.start+o.count);for(let g=h,_=d;g<_;g++){let m=u.getX(g);_r.fromBufferAttribute(p,m),fh(_r,m,c,s,t,e,this)}}else{let h=Math.max(0,o.start),d=Math.min(p.count,o.start+o.count);for(let g=h,_=d;g<_;g++)_r.fromBufferAttribute(p,g),fh(_r,g,c,s,t,e,this)}}updateMorphTargets(){let e=this.geometry.morphAttributes,n=Object.keys(e);if(n.length>0){let s=e[n[0]];if(s!==void 0){this.morphTargetInfluences=[],this.morphTargetDictionary={};for(let r=0,o=s.length;r<o;r++){let a=s[r].name||String(r);this.morphTargetInfluences.push(0),this.morphTargetDictionary[a]=r}}}}};function fh(i,t,e,n,s,r,o){let a=ac.distanceSqToPoint(i);if(a<e){let c=new P;ac.closestPointToPoint(i,c),c.applyMatrix4(n);let u=s.ray.origin.distanceTo(c);if(u<s.near||u>s.far)return;r.push({distance:u,distanceToRay:Math.sqrt(a),point:c,index:t,face:null,faceIndex:null,barycoord:null,object:o})}}var Xr=class extends Xe{constructor(t,e,n,s,r,o,a,c,u){super(t,e,n,s,r,o,a,c,u),this.isCanvasTexture=!0,this.needsUpdate=!0}},Ke=class{constructor(){this.type="Curve",this.arcLengthDivisions=200}getPoint(){return console.warn("THREE.Curve: .getPoint() not implemented."),null}getPointAt(t,e){let n=this.getUtoTmapping(t);return this.getPoint(n,e)}getPoints(t=5){let e=[];for(let n=0;n<=t;n++)e.push(this.getPoint(n/t));return e}getSpacedPoints(t=5){let e=[];for(let n=0;n<=t;n++)e.push(this.getPointAt(n/t));return e}getLength(){let t=this.getLengths();return t[t.length-1]}getLengths(t=this.arcLengthDivisions){if(this.cacheArcLengths&&this.cacheArcLengths.length===t+1&&!this.needsUpdate)return this.cacheArcLengths;this.needsUpdate=!1;let e=[],n,s=this.getPoint(0),r=0;e.push(0);for(let o=1;o<=t;o++)n=this.getPoint(o/t),r+=n.distanceTo(s),e.push(r),s=n;return this.cacheArcLengths=e,e}updateArcLengths(){this.needsUpdate=!0,this.getLengths()}getUtoTmapping(t,e){let n=this.getLengths(),s=0,r=n.length,o;e?o=e:o=t*n[r-1];let a=0,c=r-1,u;for(;a<=c;)if(s=Math.floor(a+(c-a)/2),u=n[s]-o,u<0)a=s+1;else if(u>0)c=s-1;else{c=s;break}if(s=c,n[s]===o)return s/(r-1);let f=n[s],h=n[s+1]-f,d=(o-f)/h;return(s+d)/(r-1)}getTangent(t,e){let s=t-1e-4,r=t+1e-4;s<0&&(s=0),r>1&&(r=1);let o=this.getPoint(s),a=this.getPoint(r),c=e||(o.isVector2?new ut:new P);return c.copy(a).sub(o).normalize(),c}getTangentAt(t,e){let n=this.getUtoTmapping(t);return this.getTangent(n,e)}computeFrenetFrames(t,e){let n=new P,s=[],r=[],o=[],a=new P,c=new he;for(let d=0;d<=t;d++){let g=d/t;s[d]=this.getTangentAt(g,new P)}r[0]=new P,o[0]=new P;let u=Number.MAX_VALUE,f=Math.abs(s[0].x),p=Math.abs(s[0].y),h=Math.abs(s[0].z);f<=u&&(u=f,n.set(1,0,0)),p<=u&&(u=p,n.set(0,1,0)),h<=u&&n.set(0,0,1),a.crossVectors(s[0],n).normalize(),r[0].crossVectors(s[0],a),o[0].crossVectors(s[0],r[0]);for(let d=1;d<=t;d++){if(r[d]=r[d-1].clone(),o[d]=o[d-1].clone(),a.crossVectors(s[d-1],s[d]),a.length()>Number.EPSILON){a.normalize();let g=Math.acos(we(s[d-1].dot(s[d]),-1,1));r[d].applyMatrix4(c.makeRotationAxis(a,g))}o[d].crossVectors(s[d],r[d])}if(e===!0){let d=Math.acos(we(r[0].dot(r[t]),-1,1));d/=t,s[0].dot(a.crossVectors(r[0],r[t]))>0&&(d=-d);for(let g=1;g<=t;g++)r[g].applyMatrix4(c.makeRotationAxis(s[g],d*g)),o[g].crossVectors(s[g],r[g])}return{tangents:s,normals:r,binormals:o}}clone(){return new this.constructor().copy(this)}copy(t){return this.arcLengthDivisions=t.arcLengthDivisions,this}toJSON(){let t={metadata:{version:4.6,type:"Curve",generator:"Curve.toJSON"}};return t.arcLengthDivisions=this.arcLengthDivisions,t.type=this.type,t}fromJSON(t){return this.arcLengthDivisions=t.arcLengthDivisions,this}},Is=class extends Ke{constructor(t=0,e=0,n=1,s=1,r=0,o=Math.PI*2,a=!1,c=0){super(),this.isEllipseCurve=!0,this.type="EllipseCurve",this.aX=t,this.aY=e,this.xRadius=n,this.yRadius=s,this.aStartAngle=r,this.aEndAngle=o,this.aClockwise=a,this.aRotation=c}getPoint(t,e=new ut){let n=e,s=Math.PI*2,r=this.aEndAngle-this.aStartAngle,o=Math.abs(r)<Number.EPSILON;for(;r<0;)r+=s;for(;r>s;)r-=s;r<Number.EPSILON&&(o?r=0:r=s),this.aClockwise===!0&&!o&&(r===s?r=-s:r=r-s);let a=this.aStartAngle+t*r,c=this.aX+this.xRadius*Math.cos(a),u=this.aY+this.yRadius*Math.sin(a);if(this.aRotation!==0){let f=Math.cos(this.aRotation),p=Math.sin(this.aRotation),h=c-this.aX,d=u-this.aY;c=h*f-d*p+this.aX,u=h*p+d*f+this.aY}return n.set(c,u)}copy(t){return super.copy(t),this.aX=t.aX,this.aY=t.aY,this.xRadius=t.xRadius,this.yRadius=t.yRadius,this.aStartAngle=t.aStartAngle,this.aEndAngle=t.aEndAngle,this.aClockwise=t.aClockwise,this.aRotation=t.aRotation,this}toJSON(){let t=super.toJSON();return t.aX=this.aX,t.aY=this.aY,t.xRadius=this.xRadius,t.yRadius=this.yRadius,t.aStartAngle=this.aStartAngle,t.aEndAngle=this.aEndAngle,t.aClockwise=this.aClockwise,t.aRotation=this.aRotation,t}fromJSON(t){return super.fromJSON(t),this.aX=t.aX,this.aY=t.aY,this.xRadius=t.xRadius,this.yRadius=t.yRadius,this.aStartAngle=t.aStartAngle,this.aEndAngle=t.aEndAngle,this.aClockwise=t.aClockwise,this.aRotation=t.aRotation,this}},cc=class extends Is{constructor(t,e,n,s,r,o){super(t,e,n,n,s,r,o),this.isArcCurve=!0,this.type="ArcCurve"}};function Nc(){let i=0,t=0,e=0,n=0;function s(r,o,a,c){i=r,t=a,e=-3*r+3*o-2*a-c,n=2*r-2*o+a+c}return{initCatmullRom:function(r,o,a,c,u){s(o,a,u*(a-r),u*(c-o))},initNonuniformCatmullRom:function(r,o,a,c,u,f,p){let h=(o-r)/u-(a-r)/(u+f)+(a-o)/f,d=(a-o)/f-(c-o)/(f+p)+(c-a)/p;h*=f,d*=f,s(o,a,h,d)},calc:function(r){let o=r*r,a=o*r;return i+t*r+e*o+n*a}}}var xr=new P,Qo=new Nc,ta=new Nc,ea=new Nc,lc=class extends Ke{constructor(t=[],e=!1,n="centripetal",s=.5){super(),this.isCatmullRomCurve3=!0,this.type="CatmullRomCurve3",this.points=t,this.closed=e,this.curveType=n,this.tension=s}getPoint(t,e=new P){let n=e,s=this.points,r=s.length,o=(r-(this.closed?0:1))*t,a=Math.floor(o),c=o-a;this.closed?a+=a>0?0:(Math.floor(Math.abs(a)/r)+1)*r:c===0&&a===r-1&&(a=r-2,c=1);let u,f;this.closed||a>0?u=s[(a-1)%r]:(xr.subVectors(s[0],s[1]).add(s[0]),u=xr);let p=s[a%r],h=s[(a+1)%r];if(this.closed||a+2<r?f=s[(a+2)%r]:(xr.subVectors(s[r-1],s[r-2]).add(s[r-1]),f=xr),this.curveType==="centripetal"||this.curveType==="chordal"){let d=this.curveType==="chordal"?.5:.25,g=Math.pow(u.distanceToSquared(p),d),_=Math.pow(p.distanceToSquared(h),d),m=Math.pow(h.distanceToSquared(f),d);_<1e-4&&(_=1),g<1e-4&&(g=_),m<1e-4&&(m=_),Qo.initNonuniformCatmullRom(u.x,p.x,h.x,f.x,g,_,m),ta.initNonuniformCatmullRom(u.y,p.y,h.y,f.y,g,_,m),ea.initNonuniformCatmullRom(u.z,p.z,h.z,f.z,g,_,m)}else this.curveType==="catmullrom"&&(Qo.initCatmullRom(u.x,p.x,h.x,f.x,this.tension),ta.initCatmullRom(u.y,p.y,h.y,f.y,this.tension),ea.initCatmullRom(u.z,p.z,h.z,f.z,this.tension));return n.set(Qo.calc(c),ta.calc(c),ea.calc(c)),n}copy(t){super.copy(t),this.points=[];for(let e=0,n=t.points.length;e<n;e++){let s=t.points[e];this.points.push(s.clone())}return this.closed=t.closed,this.curveType=t.curveType,this.tension=t.tension,this}toJSON(){let t=super.toJSON();t.points=[];for(let e=0,n=this.points.length;e<n;e++){let s=this.points[e];t.points.push(s.toArray())}return t.closed=this.closed,t.curveType=this.curveType,t.tension=this.tension,t}fromJSON(t){super.fromJSON(t),this.points=[];for(let e=0,n=t.points.length;e<n;e++){let s=t.points[e];this.points.push(new P().fromArray(s))}return this.closed=t.closed,this.curveType=t.curveType,this.tension=t.tension,this}};function ph(i,t,e,n,s){let r=(n-t)*.5,o=(s-e)*.5,a=i*i,c=i*a;return(2*e-2*n+r+o)*c+(-3*e+3*n-2*r-o)*a+r*i+e}function g0(i,t){let e=1-i;return e*e*t}function _0(i,t){return 2*(1-i)*i*t}function x0(i,t){return i*i*t}function bs(i,t,e,n){return g0(i,t)+_0(i,e)+x0(i,n)}function y0(i,t){let e=1-i;return e*e*e*t}function v0(i,t){let e=1-i;return 3*e*e*i*t}function M0(i,t){return 3*(1-i)*i*i*t}function S0(i,t){return i*i*i*t}function ws(i,t,e,n,s){return y0(i,t)+v0(i,e)+M0(i,n)+S0(i,s)}var qr=class extends Ke{constructor(t=new ut,e=new ut,n=new ut,s=new ut){super(),this.isCubicBezierCurve=!0,this.type="CubicBezierCurve",this.v0=t,this.v1=e,this.v2=n,this.v3=s}getPoint(t,e=new ut){let n=e,s=this.v0,r=this.v1,o=this.v2,a=this.v3;return n.set(ws(t,s.x,r.x,o.x,a.x),ws(t,s.y,r.y,o.y,a.y)),n}copy(t){return super.copy(t),this.v0.copy(t.v0),this.v1.copy(t.v1),this.v2.copy(t.v2),this.v3.copy(t.v3),this}toJSON(){let t=super.toJSON();return t.v0=this.v0.toArray(),t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t.v3=this.v3.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v0.fromArray(t.v0),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this.v3.fromArray(t.v3),this}},hc=class extends Ke{constructor(t=new P,e=new P,n=new P,s=new P){super(),this.isCubicBezierCurve3=!0,this.type="CubicBezierCurve3",this.v0=t,this.v1=e,this.v2=n,this.v3=s}getPoint(t,e=new P){let n=e,s=this.v0,r=this.v1,o=this.v2,a=this.v3;return n.set(ws(t,s.x,r.x,o.x,a.x),ws(t,s.y,r.y,o.y,a.y),ws(t,s.z,r.z,o.z,a.z)),n}copy(t){return super.copy(t),this.v0.copy(t.v0),this.v1.copy(t.v1),this.v2.copy(t.v2),this.v3.copy(t.v3),this}toJSON(){let t=super.toJSON();return t.v0=this.v0.toArray(),t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t.v3=this.v3.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v0.fromArray(t.v0),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this.v3.fromArray(t.v3),this}},Yr=class extends Ke{constructor(t=new ut,e=new ut){super(),this.isLineCurve=!0,this.type="LineCurve",this.v1=t,this.v2=e}getPoint(t,e=new ut){let n=e;return t===1?n.copy(this.v2):(n.copy(this.v2).sub(this.v1),n.multiplyScalar(t).add(this.v1)),n}getPointAt(t,e){return this.getPoint(t,e)}getTangent(t,e=new ut){return e.subVectors(this.v2,this.v1).normalize()}getTangentAt(t,e){return this.getTangent(t,e)}copy(t){return super.copy(t),this.v1.copy(t.v1),this.v2.copy(t.v2),this}toJSON(){let t=super.toJSON();return t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this}},uc=class extends Ke{constructor(t=new P,e=new P){super(),this.isLineCurve3=!0,this.type="LineCurve3",this.v1=t,this.v2=e}getPoint(t,e=new P){let n=e;return t===1?n.copy(this.v2):(n.copy(this.v2).sub(this.v1),n.multiplyScalar(t).add(this.v1)),n}getPointAt(t,e){return this.getPoint(t,e)}getTangent(t,e=new P){return e.subVectors(this.v2,this.v1).normalize()}getTangentAt(t,e){return this.getTangent(t,e)}copy(t){return super.copy(t),this.v1.copy(t.v1),this.v2.copy(t.v2),this}toJSON(){let t=super.toJSON();return t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this}},Zr=class extends Ke{constructor(t=new ut,e=new ut,n=new ut){super(),this.isQuadraticBezierCurve=!0,this.type="QuadraticBezierCurve",this.v0=t,this.v1=e,this.v2=n}getPoint(t,e=new ut){let n=e,s=this.v0,r=this.v1,o=this.v2;return n.set(bs(t,s.x,r.x,o.x),bs(t,s.y,r.y,o.y)),n}copy(t){return super.copy(t),this.v0.copy(t.v0),this.v1.copy(t.v1),this.v2.copy(t.v2),this}toJSON(){let t=super.toJSON();return t.v0=this.v0.toArray(),t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v0.fromArray(t.v0),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this}},dc=class extends Ke{constructor(t=new P,e=new P,n=new P){super(),this.isQuadraticBezierCurve3=!0,this.type="QuadraticBezierCurve3",this.v0=t,this.v1=e,this.v2=n}getPoint(t,e=new P){let n=e,s=this.v0,r=this.v1,o=this.v2;return n.set(bs(t,s.x,r.x,o.x),bs(t,s.y,r.y,o.y),bs(t,s.z,r.z,o.z)),n}copy(t){return super.copy(t),this.v0.copy(t.v0),this.v1.copy(t.v1),this.v2.copy(t.v2),this}toJSON(){let t=super.toJSON();return t.v0=this.v0.toArray(),t.v1=this.v1.toArray(),t.v2=this.v2.toArray(),t}fromJSON(t){return super.fromJSON(t),this.v0.fromArray(t.v0),this.v1.fromArray(t.v1),this.v2.fromArray(t.v2),this}},$r=class extends Ke{constructor(t=[]){super(),this.isSplineCurve=!0,this.type="SplineCurve",this.points=t}getPoint(t,e=new ut){let n=e,s=this.points,r=(s.length-1)*t,o=Math.floor(r),a=r-o,c=s[o===0?o:o-1],u=s[o],f=s[o>s.length-2?s.length-1:o+1],p=s[o>s.length-3?s.length-1:o+2];return n.set(ph(a,c.x,u.x,f.x,p.x),ph(a,c.y,u.y,f.y,p.y)),n}copy(t){super.copy(t),this.points=[];for(let e=0,n=t.points.length;e<n;e++){let s=t.points[e];this.points.push(s.clone())}return this}toJSON(){let t=super.toJSON();t.points=[];for(let e=0,n=this.points.length;e<n;e++){let s=this.points[e];t.points.push(s.toArray())}return t}fromJSON(t){super.fromJSON(t),this.points=[];for(let e=0,n=t.points.length;e<n;e++){let s=t.points[e];this.points.push(new ut().fromArray(s))}return this}},mh=Object.freeze({__proto__:null,ArcCurve:cc,CatmullRomCurve3:lc,CubicBezierCurve:qr,CubicBezierCurve3:hc,EllipseCurve:Is,LineCurve:Yr,LineCurve3:uc,QuadraticBezierCurve:Zr,QuadraticBezierCurve3:dc,SplineCurve:$r}),fc=class extends Ke{constructor(){super(),this.type="CurvePath",this.curves=[],this.autoClose=!1}add(t){this.curves.push(t)}closePath(){let t=this.curves[0].getPoint(0),e=this.curves[this.curves.length-1].getPoint(1);if(!t.equals(e)){let n=t.isVector2===!0?"LineCurve":"LineCurve3";this.curves.push(new mh[n](e,t))}return this}getPoint(t,e){let n=t*this.getLength(),s=this.getCurveLengths(),r=0;for(;r<s.length;){if(s[r]>=n){let o=s[r]-n,a=this.curves[r],c=a.getLength(),u=c===0?0:1-o/c;return a.getPointAt(u,e)}r++}return null}getLength(){let t=this.getCurveLengths();return t[t.length-1]}updateArcLengths(){this.needsUpdate=!0,this.cacheLengths=null,this.getCurveLengths()}getCurveLengths(){if(this.cacheLengths&&this.cacheLengths.length===this.curves.length)return this.cacheLengths;let t=[],e=0;for(let n=0,s=this.curves.length;n<s;n++)e+=this.curves[n].getLength(),t.push(e);return this.cacheLengths=t,t}getSpacedPoints(t=40){let e=[];for(let n=0;n<=t;n++)e.push(this.getPoint(n/t));return this.autoClose&&e.push(e[0]),e}getPoints(t=12){let e=[],n;for(let s=0,r=this.curves;s<r.length;s++){let o=r[s],a=o.isEllipseCurve?t*2:o.isLineCurve||o.isLineCurve3?1:o.isSplineCurve?t*o.points.length:t,c=o.getPoints(a);for(let u=0;u<c.length;u++){let f=c[u];n&&n.equals(f)||(e.push(f),n=f)}}return this.autoClose&&e.length>1&&!e[e.length-1].equals(e[0])&&e.push(e[0]),e}copy(t){super.copy(t),this.curves=[];for(let e=0,n=t.curves.length;e<n;e++){let s=t.curves[e];this.curves.push(s.clone())}return this.autoClose=t.autoClose,this}toJSON(){let t=super.toJSON();t.autoClose=this.autoClose,t.curves=[];for(let e=0,n=this.curves.length;e<n;e++){let s=this.curves[e];t.curves.push(s.toJSON())}return t}fromJSON(t){super.fromJSON(t),this.autoClose=t.autoClose,this.curves=[];for(let e=0,n=t.curves.length;e<n;e++){let s=t.curves[e];this.curves.push(new mh[s.type]().fromJSON(s))}return this}},pc=class extends fc{constructor(t){super(),this.type="Path",this.currentPoint=new ut,t&&this.setFromPoints(t)}setFromPoints(t){this.moveTo(t[0].x,t[0].y);for(let e=1,n=t.length;e<n;e++)this.lineTo(t[e].x,t[e].y);return this}moveTo(t,e){return this.currentPoint.set(t,e),this}lineTo(t,e){let n=new Yr(this.currentPoint.clone(),new ut(t,e));return this.curves.push(n),this.currentPoint.set(t,e),this}quadraticCurveTo(t,e,n,s){let r=new Zr(this.currentPoint.clone(),new ut(t,e),new ut(n,s));return this.curves.push(r),this.currentPoint.set(n,s),this}bezierCurveTo(t,e,n,s,r,o){let a=new qr(this.currentPoint.clone(),new ut(t,e),new ut(n,s),new ut(r,o));return this.curves.push(a),this.currentPoint.set(r,o),this}splineThru(t){let e=[this.currentPoint.clone()].concat(t),n=new $r(e);return this.curves.push(n),this.currentPoint.copy(t[t.length-1]),this}arc(t,e,n,s,r,o){let a=this.currentPoint.x,c=this.currentPoint.y;return this.absarc(t+a,e+c,n,s,r,o),this}absarc(t,e,n,s,r,o){return this.absellipse(t,e,n,n,s,r,o),this}ellipse(t,e,n,s,r,o,a,c){let u=this.currentPoint.x,f=this.currentPoint.y;return this.absellipse(t+u,e+f,n,s,r,o,a,c),this}absellipse(t,e,n,s,r,o,a,c){let u=new Is(t,e,n,s,r,o,a,c);if(this.curves.length>0){let p=u.getPoint(0);p.equals(this.currentPoint)||this.lineTo(p.x,p.y)}this.curves.push(u);let f=u.getPoint(1);return this.currentPoint.copy(f),this}copy(t){return super.copy(t),this.currentPoint.copy(t.currentPoint),this}toJSON(){let t=super.toJSON();return t.currentPoint=this.currentPoint.toArray(),t}fromJSON(t){return super.fromJSON(t),this.currentPoint.fromArray(t.currentPoint),this}},mc=class i extends Ee{constructor(t=[new ut(0,-.5),new ut(.5,0),new ut(0,.5)],e=12,n=0,s=Math.PI*2){super(),this.type="LatheGeometry",this.parameters={points:t,segments:e,phiStart:n,phiLength:s},e=Math.floor(e),s=we(s,0,Math.PI*2);let r=[],o=[],a=[],c=[],u=[],f=1/e,p=new P,h=new ut,d=new P,g=new P,_=new P,m=0,l=0;for(let S=0;S<=t.length-1;S++)switch(S){case 0:m=t[S+1].x-t[S].x,l=t[S+1].y-t[S].y,d.x=l*1,d.y=-m,d.z=l*0,_.copy(d),d.normalize(),c.push(d.x,d.y,d.z);break;case t.length-1:c.push(_.x,_.y,_.z);break;default:m=t[S+1].x-t[S].x,l=t[S+1].y-t[S].y,d.x=l*1,d.y=-m,d.z=l*0,g.copy(d),d.x+=_.x,d.y+=_.y,d.z+=_.z,d.normalize(),c.push(d.x,d.y,d.z),_.copy(g)}for(let S=0;S<=e;S++){let b=n+S*f*s,y=Math.sin(b),I=Math.cos(b);for(let T=0;T<=t.length-1;T++){p.x=t[T].x*y,p.y=t[T].y,p.z=t[T].x*I,o.push(p.x,p.y,p.z),h.x=S/e,h.y=T/(t.length-1),a.push(h.x,h.y);let R=c[3*T+0]*y,A=c[3*T+1],E=c[3*T+0]*I;u.push(R,A,E)}}for(let S=0;S<e;S++)for(let b=0;b<t.length-1;b++){let y=b+S*t.length,I=y,T=y+t.length,R=y+t.length+1,A=y+1;r.push(I,T,A),r.push(R,A,T)}this.setIndex(r),this.setAttribute("position",new Jt(o,3)),this.setAttribute("uv",new Jt(a,2)),this.setAttribute("normal",new Jt(u,3))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.points,t.segments,t.phiStart,t.phiLength)}},Ps=class i extends mc{constructor(t=1,e=1,n=4,s=8){let r=new pc;r.absarc(0,-e/2,t,Math.PI*1.5,0),r.absarc(0,e/2,t,0,Math.PI*.5),super(r.getPoints(n),s),this.type="CapsuleGeometry",this.parameters={radius:t,length:e,capSegments:n,radialSegments:s}}static fromJSON(t){return new i(t.radius,t.length,t.capSegments,t.radialSegments)}},Ls=class i extends Ee{constructor(t=1,e=32,n=0,s=Math.PI*2){super(),this.type="CircleGeometry",this.parameters={radius:t,segments:e,thetaStart:n,thetaLength:s},e=Math.max(3,e);let r=[],o=[],a=[],c=[],u=new P,f=new ut;o.push(0,0,0),a.push(0,0,1),c.push(.5,.5);for(let p=0,h=3;p<=e;p++,h+=3){let d=n+p/e*s;u.x=t*Math.cos(d),u.y=t*Math.sin(d),o.push(u.x,u.y,u.z),a.push(0,0,1),f.x=(o[h]/t+1)/2,f.y=(o[h+1]/t+1)/2,c.push(f.x,f.y)}for(let p=1;p<=e;p++)r.push(p,p+1,0);this.setIndex(r),this.setAttribute("position",new Jt(o,3)),this.setAttribute("normal",new Jt(a,3)),this.setAttribute("uv",new Jt(c,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.radius,t.segments,t.thetaStart,t.thetaLength)}},xe=class i extends Ee{constructor(t=1,e=1,n=1,s=32,r=1,o=!1,a=0,c=Math.PI*2){super(),this.type="CylinderGeometry",this.parameters={radiusTop:t,radiusBottom:e,height:n,radialSegments:s,heightSegments:r,openEnded:o,thetaStart:a,thetaLength:c};let u=this;s=Math.floor(s),r=Math.floor(r);let f=[],p=[],h=[],d=[],g=0,_=[],m=n/2,l=0;S(),o===!1&&(t>0&&b(!0),e>0&&b(!1)),this.setIndex(f),this.setAttribute("position",new Jt(p,3)),this.setAttribute("normal",new Jt(h,3)),this.setAttribute("uv",new Jt(d,2));function S(){let y=new P,I=new P,T=0,R=(e-t)/n;for(let A=0;A<=r;A++){let E=[],v=A/r,C=v*(e-t)+t;for(let H=0;H<=s;H++){let k=H/s,W=k*c+a,J=Math.sin(W),X=Math.cos(W);I.x=C*J,I.y=-v*n+m,I.z=C*X,p.push(I.x,I.y,I.z),y.set(J,R,X).normalize(),h.push(y.x,y.y,y.z),d.push(k,1-v),E.push(g++)}_.push(E)}for(let A=0;A<s;A++)for(let E=0;E<r;E++){let v=_[E][A],C=_[E+1][A],H=_[E+1][A+1],k=_[E][A+1];(t>0||E!==0)&&(f.push(v,C,k),T+=3),(e>0||E!==r-1)&&(f.push(C,H,k),T+=3)}u.addGroup(l,T,0),l+=T}function b(y){let I=g,T=new ut,R=new P,A=0,E=y===!0?t:e,v=y===!0?1:-1;for(let H=1;H<=s;H++)p.push(0,m*v,0),h.push(0,v,0),d.push(.5,.5),g++;let C=g;for(let H=0;H<=s;H++){let W=H/s*c+a,J=Math.cos(W),X=Math.sin(W);R.x=E*X,R.y=m*v,R.z=E*J,p.push(R.x,R.y,R.z),h.push(0,v,0),T.x=J*.5+.5,T.y=X*.5*v+.5,d.push(T.x,T.y),g++}for(let H=0;H<s;H++){let k=I+H,W=C+H;y===!0?f.push(W,W+1,k):f.push(W+1,W,k),A+=3}u.addGroup(l,A,y===!0?1:2),l+=A}}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.radiusTop,t.radiusBottom,t.height,t.radialSegments,t.heightSegments,t.openEnded,t.thetaStart,t.thetaLength)}},Ds=class i extends xe{constructor(t=1,e=1,n=32,s=1,r=!1,o=0,a=Math.PI*2){super(0,t,e,n,s,r,o,a),this.type="ConeGeometry",this.parameters={radius:t,height:e,radialSegments:n,heightSegments:s,openEnded:r,thetaStart:o,thetaLength:a}}static fromJSON(t){return new i(t.radius,t.height,t.radialSegments,t.heightSegments,t.openEnded,t.thetaStart,t.thetaLength)}};var Jr=class i extends Ee{constructor(t=.5,e=1,n=32,s=1,r=0,o=Math.PI*2){super(),this.type="RingGeometry",this.parameters={innerRadius:t,outerRadius:e,thetaSegments:n,phiSegments:s,thetaStart:r,thetaLength:o},n=Math.max(3,n),s=Math.max(1,s);let a=[],c=[],u=[],f=[],p=t,h=(e-t)/s,d=new P,g=new ut;for(let _=0;_<=s;_++){for(let m=0;m<=n;m++){let l=r+m/n*o;d.x=p*Math.cos(l),d.y=p*Math.sin(l),c.push(d.x,d.y,d.z),u.push(0,0,1),g.x=(d.x/e+1)/2,g.y=(d.y/e+1)/2,f.push(g.x,g.y)}p+=h}for(let _=0;_<s;_++){let m=_*(n+1);for(let l=0;l<n;l++){let S=l+m,b=S,y=S+n+1,I=S+n+2,T=S+1;a.push(b,y,T),a.push(y,I,T)}}this.setIndex(a),this.setAttribute("position",new Jt(c,3)),this.setAttribute("normal",new Jt(u,3)),this.setAttribute("uv",new Jt(f,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.innerRadius,t.outerRadius,t.thetaSegments,t.phiSegments,t.thetaStart,t.thetaLength)}};var qe=class i extends Ee{constructor(t=1,e=32,n=16,s=0,r=Math.PI*2,o=0,a=Math.PI){super(),this.type="SphereGeometry",this.parameters={radius:t,widthSegments:e,heightSegments:n,phiStart:s,phiLength:r,thetaStart:o,thetaLength:a},e=Math.max(3,Math.floor(e)),n=Math.max(2,Math.floor(n));let c=Math.min(o+a,Math.PI),u=0,f=[],p=new P,h=new P,d=[],g=[],_=[],m=[];for(let l=0;l<=n;l++){let S=[],b=l/n,y=0;l===0&&o===0?y=.5/e:l===n&&c===Math.PI&&(y=-.5/e);for(let I=0;I<=e;I++){let T=I/e;p.x=-t*Math.cos(s+T*r)*Math.sin(o+b*a),p.y=t*Math.cos(o+b*a),p.z=t*Math.sin(s+T*r)*Math.sin(o+b*a),g.push(p.x,p.y,p.z),h.copy(p).normalize(),_.push(h.x,h.y,h.z),m.push(T+y,1-b),S.push(u++)}f.push(S)}for(let l=0;l<n;l++)for(let S=0;S<e;S++){let b=f[l][S+1],y=f[l][S],I=f[l+1][S],T=f[l+1][S+1];(l!==0||o>0)&&d.push(b,y,T),(l!==n-1||c<Math.PI)&&d.push(y,I,T)}this.setIndex(d),this.setAttribute("position",new Jt(g,3)),this.setAttribute("normal",new Jt(_,3)),this.setAttribute("uv",new Jt(m,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.radius,t.widthSegments,t.heightSegments,t.phiStart,t.phiLength,t.thetaStart,t.thetaLength)}};var Us=class i extends Ee{constructor(t=1,e=.4,n=12,s=48,r=Math.PI*2){super(),this.type="TorusGeometry",this.parameters={radius:t,tube:e,radialSegments:n,tubularSegments:s,arc:r},n=Math.floor(n),s=Math.floor(s);let o=[],a=[],c=[],u=[],f=new P,p=new P,h=new P;for(let d=0;d<=n;d++)for(let g=0;g<=s;g++){let _=g/s*r,m=d/n*Math.PI*2;p.x=(t+e*Math.cos(m))*Math.cos(_),p.y=(t+e*Math.cos(m))*Math.sin(_),p.z=e*Math.sin(m),a.push(p.x,p.y,p.z),f.x=t*Math.cos(_),f.y=t*Math.sin(_),h.subVectors(p,f).normalize(),c.push(h.x,h.y,h.z),u.push(g/s),u.push(d/n)}for(let d=1;d<=n;d++)for(let g=1;g<=s;g++){let _=(s+1)*d+g-1,m=(s+1)*(d-1)+g-1,l=(s+1)*(d-1)+g,S=(s+1)*d+g;o.push(_,m,S),o.push(m,l,S)}this.setIndex(o),this.setAttribute("position",new Jt(a,3)),this.setAttribute("normal",new Jt(c,3)),this.setAttribute("uv",new Jt(u,2))}copy(t){return super.copy(t),this.parameters=Object.assign({},t.parameters),this}static fromJSON(t){return new i(t.radius,t.tube,t.radialSegments,t.tubularSegments,t.arc)}};var Fe=class extends Ln{static get type(){return"MeshLambertMaterial"}constructor(t){super(),this.isMeshLambertMaterial=!0,this.color=new Bt(16777215),this.map=null,this.lightMap=null,this.lightMapIntensity=1,this.aoMap=null,this.aoMapIntensity=1,this.emissive=new Bt(0),this.emissiveIntensity=1,this.emissiveMap=null,this.bumpMap=null,this.bumpScale=1,this.normalMap=null,this.normalMapType=Ih,this.normalScale=new ut(1,1),this.displacementMap=null,this.displacementScale=1,this.displacementBias=0,this.specularMap=null,this.alphaMap=null,this.envMap=null,this.envMapRotation=new mn,this.combine=Rc,this.reflectivity=1,this.refractionRatio=.98,this.wireframe=!1,this.wireframeLinewidth=1,this.wireframeLinecap="round",this.wireframeLinejoin="round",this.flatShading=!1,this.fog=!0,this.setValues(t)}copy(t){return super.copy(t),this.color.copy(t.color),this.map=t.map,this.lightMap=t.lightMap,this.lightMapIntensity=t.lightMapIntensity,this.aoMap=t.aoMap,this.aoMapIntensity=t.aoMapIntensity,this.emissive.copy(t.emissive),this.emissiveMap=t.emissiveMap,this.emissiveIntensity=t.emissiveIntensity,this.bumpMap=t.bumpMap,this.bumpScale=t.bumpScale,this.normalMap=t.normalMap,this.normalMapType=t.normalMapType,this.normalScale.copy(t.normalScale),this.displacementMap=t.displacementMap,this.displacementScale=t.displacementScale,this.displacementBias=t.displacementBias,this.specularMap=t.specularMap,this.alphaMap=t.alphaMap,this.envMap=t.envMap,this.envMapRotation.copy(t.envMapRotation),this.combine=t.combine,this.reflectivity=t.reflectivity,this.refractionRatio=t.refractionRatio,this.wireframe=t.wireframe,this.wireframeLinewidth=t.wireframeLinewidth,this.wireframeLinecap=t.wireframeLinecap,this.wireframeLinejoin=t.wireframeLinejoin,this.flatShading=t.flatShading,this.fog=t.fog,this}};function yr(i,t,e){return!i||!e&&i.constructor===t?i:typeof t.BYTES_PER_ELEMENT=="number"?new t(i):Array.prototype.slice.call(i)}function E0(i){return ArrayBuffer.isView(i)&&!(i instanceof DataView)}var es=class{constructor(t,e,n,s){this.parameterPositions=t,this._cachedIndex=0,this.resultBuffer=s!==void 0?s:new e.constructor(n),this.sampleValues=e,this.valueSize=n,this.settings=null,this.DefaultSettings_={}}evaluate(t){let e=this.parameterPositions,n=this._cachedIndex,s=e[n],r=e[n-1];n:{t:{let o;e:{i:if(!(t<s)){for(let a=n+2;;){if(s===void 0){if(t<r)break i;return n=e.length,this._cachedIndex=n,this.copySampleValue_(n-1)}if(n===a)break;if(r=s,s=e[++n],t<s)break t}o=e.length;break e}if(!(t>=r)){let a=e[1];t<a&&(n=2,r=a);for(let c=n-2;;){if(r===void 0)return this._cachedIndex=0,this.copySampleValue_(0);if(n===c)break;if(s=r,r=e[--n-1],t>=r)break t}o=n,n=0;break e}break n}for(;n<o;){let a=n+o>>>1;t<e[a]?o=a:n=a+1}if(s=e[n],r=e[n-1],r===void 0)return this._cachedIndex=0,this.copySampleValue_(0);if(s===void 0)return n=e.length,this._cachedIndex=n,this.copySampleValue_(n-1)}this._cachedIndex=n,this.intervalChanged_(n,r,s)}return this.interpolate_(n,r,t,s)}getSettings_(){return this.settings||this.DefaultSettings_}copySampleValue_(t){let e=this.resultBuffer,n=this.sampleValues,s=this.valueSize,r=t*s;for(let o=0;o!==s;++o)e[o]=n[r+o];return e}interpolate_(){throw new Error("call to abstract method")}intervalChanged_(){}},gc=class extends es{constructor(t,e,n,s){super(t,e,n,s),this._weightPrev=-0,this._offsetPrev=-0,this._weightNext=-0,this._offsetNext=-0,this.DefaultSettings_={endingStart:ml,endingEnd:ml}}intervalChanged_(t,e,n){let s=this.parameterPositions,r=t-2,o=t+1,a=s[r],c=s[o];if(a===void 0)switch(this.getSettings_().endingStart){case gl:r=t,a=2*e-n;break;case _l:r=s.length-2,a=e+s[r]-s[r+1];break;default:r=t,a=n}if(c===void 0)switch(this.getSettings_().endingEnd){case gl:o=t,c=2*n-e;break;case _l:o=1,c=n+s[1]-s[0];break;default:o=t-1,c=e}let u=(n-e)*.5,f=this.valueSize;this._weightPrev=u/(e-a),this._weightNext=u/(c-n),this._offsetPrev=r*f,this._offsetNext=o*f}interpolate_(t,e,n,s){let r=this.resultBuffer,o=this.sampleValues,a=this.valueSize,c=t*a,u=c-a,f=this._offsetPrev,p=this._offsetNext,h=this._weightPrev,d=this._weightNext,g=(n-e)/(s-e),_=g*g,m=_*g,l=-h*m+2*h*_-h*g,S=(1+h)*m+(-1.5-2*h)*_+(-.5+h)*g+1,b=(-1-d)*m+(1.5+d)*_+.5*g,y=d*m-d*_;for(let I=0;I!==a;++I)r[I]=l*o[f+I]+S*o[u+I]+b*o[c+I]+y*o[p+I];return r}},_c=class extends es{constructor(t,e,n,s){super(t,e,n,s)}interpolate_(t,e,n,s){let r=this.resultBuffer,o=this.sampleValues,a=this.valueSize,c=t*a,u=c-a,f=(n-e)/(s-e),p=1-f;for(let h=0;h!==a;++h)r[h]=o[u+h]*p+o[c+h]*f;return r}},xc=class extends es{constructor(t,e,n,s){super(t,e,n,s)}interpolate_(t){return this.copySampleValue_(t-1)}},cn=class{constructor(t,e,n,s){if(t===void 0)throw new Error("THREE.KeyframeTrack: track name is undefined");if(e===void 0||e.length===0)throw new Error("THREE.KeyframeTrack: no keyframes in track named "+t);this.name=t,this.times=yr(e,this.TimeBufferType),this.values=yr(n,this.ValueBufferType),this.setInterpolation(s||this.DefaultInterpolation)}static toJSON(t){let e=t.constructor,n;if(e.toJSON!==this.toJSON)n=e.toJSON(t);else{n={name:t.name,times:yr(t.times,Array),values:yr(t.values,Array)};let s=t.getInterpolation();s!==t.DefaultInterpolation&&(n.interpolation=s)}return n.type=t.ValueTypeName,n}InterpolantFactoryMethodDiscrete(t){return new xc(this.times,this.values,this.getValueSize(),t)}InterpolantFactoryMethodLinear(t){return new _c(this.times,this.values,this.getValueSize(),t)}InterpolantFactoryMethodSmooth(t){return new gc(this.times,this.values,this.getValueSize(),t)}setInterpolation(t){let e;switch(t){case wr:e=this.InterpolantFactoryMethodDiscrete;break;case Ha:e=this.InterpolantFactoryMethodLinear;break;case Eo:e=this.InterpolantFactoryMethodSmooth;break}if(e===void 0){let n="unsupported interpolation for "+this.ValueTypeName+" keyframe track named "+this.name;if(this.createInterpolant===void 0)if(t!==this.DefaultInterpolation)this.setInterpolation(this.DefaultInterpolation);else throw new Error(n);return console.warn("THREE.KeyframeTrack:",n),this}return this.createInterpolant=e,this}getInterpolation(){switch(this.createInterpolant){case this.InterpolantFactoryMethodDiscrete:return wr;case this.InterpolantFactoryMethodLinear:return Ha;case this.InterpolantFactoryMethodSmooth:return Eo}}getValueSize(){return this.values.length/this.times.length}shift(t){if(t!==0){let e=this.times;for(let n=0,s=e.length;n!==s;++n)e[n]+=t}return this}scale(t){if(t!==1){let e=this.times;for(let n=0,s=e.length;n!==s;++n)e[n]*=t}return this}trim(t,e){let n=this.times,s=n.length,r=0,o=s-1;for(;r!==s&&n[r]<t;)++r;for(;o!==-1&&n[o]>e;)--o;if(++o,r!==0||o!==s){r>=o&&(o=Math.max(o,1),r=o-1);let a=this.getValueSize();this.times=n.slice(r,o),this.values=this.values.slice(r*a,o*a)}return this}validate(){let t=!0,e=this.getValueSize();e-Math.floor(e)!==0&&(console.error("THREE.KeyframeTrack: Invalid value size in track.",this),t=!1);let n=this.times,s=this.values,r=n.length;r===0&&(console.error("THREE.KeyframeTrack: Track is empty.",this),t=!1);let o=null;for(let a=0;a!==r;a++){let c=n[a];if(typeof c=="number"&&isNaN(c)){console.error("THREE.KeyframeTrack: Time is not a valid number.",this,a,c),t=!1;break}if(o!==null&&o>c){console.error("THREE.KeyframeTrack: Out of order keys.",this,a,c,o),t=!1;break}o=c}if(s!==void 0&&E0(s))for(let a=0,c=s.length;a!==c;++a){let u=s[a];if(isNaN(u)){console.error("THREE.KeyframeTrack: Value is not a valid number.",this,a,u),t=!1;break}}return t}optimize(){let t=this.times.slice(),e=this.values.slice(),n=this.getValueSize(),s=this.getInterpolation()===Eo,r=t.length-1,o=1;for(let a=1;a<r;++a){let c=!1,u=t[a],f=t[a+1];if(u!==f&&(a!==1||u!==t[0]))if(s)c=!0;else{let p=a*n,h=p-n,d=p+n;for(let g=0;g!==n;++g){let _=e[p+g];if(_!==e[h+g]||_!==e[d+g]){c=!0;break}}}if(c){if(a!==o){t[o]=t[a];let p=a*n,h=o*n;for(let d=0;d!==n;++d)e[h+d]=e[p+d]}++o}}if(r>0){t[o]=t[r];for(let a=r*n,c=o*n,u=0;u!==n;++u)e[c+u]=e[a+u];++o}return o!==t.length?(this.times=t.slice(0,o),this.values=e.slice(0,o*n)):(this.times=t,this.values=e),this}clone(){let t=this.times.slice(),e=this.values.slice(),n=this.constructor,s=new n(this.name,t,e);return s.createInterpolant=this.createInterpolant,s}};cn.prototype.TimeBufferType=Float32Array;cn.prototype.ValueBufferType=Float32Array;cn.prototype.DefaultInterpolation=Ha;var pi=class extends cn{constructor(t,e,n){super(t,e,n)}};pi.prototype.ValueTypeName="bool";pi.prototype.ValueBufferType=Array;pi.prototype.DefaultInterpolation=wr;pi.prototype.InterpolantFactoryMethodLinear=void 0;pi.prototype.InterpolantFactoryMethodSmooth=void 0;var yc=class extends cn{};yc.prototype.ValueTypeName="color";var vc=class extends cn{};vc.prototype.ValueTypeName="number";var Mc=class extends es{constructor(t,e,n,s){super(t,e,n,s)}interpolate_(t,e,n,s){let r=this.resultBuffer,o=this.sampleValues,a=this.valueSize,c=(n-e)/(s-e),u=t*a;for(let f=u+a;u!==f;u+=4)jn.slerpFlat(r,0,o,u-a,o,u,c);return r}},Kr=class extends cn{InterpolantFactoryMethodLinear(t){return new Mc(this.times,this.values,this.getValueSize(),t)}};Kr.prototype.ValueTypeName="quaternion";Kr.prototype.InterpolantFactoryMethodSmooth=void 0;var mi=class extends cn{constructor(t,e,n){super(t,e,n)}};mi.prototype.ValueTypeName="string";mi.prototype.ValueBufferType=Array;mi.prototype.DefaultInterpolation=wr;mi.prototype.InterpolantFactoryMethodLinear=void 0;mi.prototype.InterpolantFactoryMethodSmooth=void 0;var Sc=class extends cn{};Sc.prototype.ValueTypeName="vector";var Ec=class{constructor(t,e,n){let s=this,r=!1,o=0,a=0,c,u=[];this.onStart=void 0,this.onLoad=t,this.onProgress=e,this.onError=n,this.itemStart=function(f){a++,r===!1&&s.onStart!==void 0&&s.onStart(f,o,a),r=!0},this.itemEnd=function(f){o++,s.onProgress!==void 0&&s.onProgress(f,o,a),o===a&&(r=!1,s.onLoad!==void 0&&s.onLoad())},this.itemError=function(f){s.onError!==void 0&&s.onError(f)},this.resolveURL=function(f){return c?c(f):f},this.setURLModifier=function(f){return c=f,this},this.addHandler=function(f,p){return u.push(f,p),this},this.removeHandler=function(f){let p=u.indexOf(f);return p!==-1&&u.splice(p,2),this},this.getHandler=function(f){for(let p=0,h=u.length;p<h;p+=2){let d=u[p],g=u[p+1];if(d.global&&(d.lastIndex=0),d.test(f))return g}return null}}},b0=new Ec,bc=class{constructor(t){this.manager=t!==void 0?t:b0,this.crossOrigin="anonymous",this.withCredentials=!1,this.path="",this.resourcePath="",this.requestHeader={}}load(){}loadAsync(t,e){let n=this;return new Promise(function(s,r){n.load(t,s,e,r)})}parse(){}setCrossOrigin(t){return this.crossOrigin=t,this}setWithCredentials(t){return this.withCredentials=t,this}setPath(t){return this.path=t,this}setResourcePath(t){return this.resourcePath=t,this}setRequestHeader(t){return this.requestHeader=t,this}};bc.DEFAULT_MATERIAL_NAME="__DEFAULT";var jr=class extends Te{constructor(t,e=1){super(),this.isLight=!0,this.type="Light",this.color=new Bt(t),this.intensity=e}dispose(){}copy(t,e){return super.copy(t,e),this.color.copy(t.color),this.intensity=t.intensity,this}toJSON(t){let e=super.toJSON(t);return e.object.color=this.color.getHex(),e.object.intensity=this.intensity,this.groundColor!==void 0&&(e.object.groundColor=this.groundColor.getHex()),this.distance!==void 0&&(e.object.distance=this.distance),this.angle!==void 0&&(e.object.angle=this.angle),this.decay!==void 0&&(e.object.decay=this.decay),this.penumbra!==void 0&&(e.object.penumbra=this.penumbra),this.shadow!==void 0&&(e.object.shadow=this.shadow.toJSON()),this.target!==void 0&&(e.object.target=this.target.uuid),e}},Qr=class extends jr{constructor(t,e,n){super(t,n),this.isHemisphereLight=!0,this.type="HemisphereLight",this.position.copy(Te.DEFAULT_UP),this.updateMatrix(),this.groundColor=new Bt(e)}copy(t,e){return super.copy(t,e),this.groundColor.copy(t.groundColor),this}},na=new he,gh=new P,_h=new P,wc=class{constructor(t){this.camera=t,this.intensity=1,this.bias=0,this.normalBias=0,this.radius=1,this.blurSamples=8,this.mapSize=new ut(512,512),this.map=null,this.mapPass=null,this.matrix=new he,this.autoUpdate=!0,this.needsUpdate=!1,this._frustum=new As,this._frameExtents=new ut(1,1),this._viewportCount=1,this._viewports=[new pe(0,0,1,1)]}getViewportCount(){return this._viewportCount}getFrustum(){return this._frustum}updateMatrices(t){let e=this.camera,n=this.matrix;gh.setFromMatrixPosition(t.matrixWorld),e.position.copy(gh),_h.setFromMatrixPosition(t.target.matrixWorld),e.lookAt(_h),e.updateMatrixWorld(),na.multiplyMatrices(e.projectionMatrix,e.matrixWorldInverse),this._frustum.setFromProjectionMatrix(na),n.set(.5,0,0,.5,0,.5,0,.5,0,0,.5,.5,0,0,0,1),n.multiply(na)}getViewport(t){return this._viewports[t]}getFrameExtents(){return this._frameExtents}dispose(){this.map&&this.map.dispose(),this.mapPass&&this.mapPass.dispose()}copy(t){return this.camera=t.camera.clone(),this.intensity=t.intensity,this.bias=t.bias,this.radius=t.radius,this.mapSize.copy(t.mapSize),this}clone(){return new this.constructor().copy(this)}toJSON(){let t={};return this.intensity!==1&&(t.intensity=this.intensity),this.bias!==0&&(t.bias=this.bias),this.normalBias!==0&&(t.normalBias=this.normalBias),this.radius!==1&&(t.radius=this.radius),(this.mapSize.x!==512||this.mapSize.y!==512)&&(t.mapSize=this.mapSize.toArray()),t.camera=this.camera.toJSON(!1).object,delete t.camera.matrix,t}};var Tc=class extends wc{constructor(){super(new Nr(-5,5,5,-5,.5,500)),this.isDirectionalLightShadow=!0}},to=class extends jr{constructor(t,e){super(t,e),this.isDirectionalLight=!0,this.type="DirectionalLight",this.position.copy(Te.DEFAULT_UP),this.updateMatrix(),this.target=new Te,this.shadow=new Tc}dispose(){this.shadow.dispose()}copy(t){return super.copy(t),this.target=t.target.clone(),this.shadow=t.shadow.clone(),this}};var Oc="\\[\\]\\.:\\/",w0=new RegExp("["+Oc+"]","g"),Bc="[^"+Oc+"]",T0="[^"+Oc.replace("\\.","")+"]",A0=/((?:WC+[\/:])*)/.source.replace("WC",Bc),R0=/(WCOD+)?/.source.replace("WCOD",T0),C0=/(?:\.(WC+)(?:\[(.+)\])?)?/.source.replace("WC",Bc),I0=/\.(WC+)(?:\[(.+)\])?/.source.replace("WC",Bc),P0=new RegExp("^"+A0+R0+C0+I0+"$"),L0=["material","materials","bones","map"],Ac=class{constructor(t,e,n){let s=n||le.parseTrackName(e);this._targetGroup=t,this._bindings=t.subscribe_(e,s)}getValue(t,e){this.bind();let n=this._targetGroup.nCachedObjects_,s=this._bindings[n];s!==void 0&&s.getValue(t,e)}setValue(t,e){let n=this._bindings;for(let s=this._targetGroup.nCachedObjects_,r=n.length;s!==r;++s)n[s].setValue(t,e)}bind(){let t=this._bindings;for(let e=this._targetGroup.nCachedObjects_,n=t.length;e!==n;++e)t[e].bind()}unbind(){let t=this._bindings;for(let e=this._targetGroup.nCachedObjects_,n=t.length;e!==n;++e)t[e].unbind()}},le=class i{constructor(t,e,n){this.path=e,this.parsedPath=n||i.parseTrackName(e),this.node=i.findNode(t,this.parsedPath.nodeName),this.rootNode=t,this.getValue=this._getValue_unbound,this.setValue=this._setValue_unbound}static create(t,e,n){return t&&t.isAnimationObjectGroup?new i.Composite(t,e,n):new i(t,e,n)}static sanitizeNodeName(t){return t.replace(/\s/g,"_").replace(w0,"")}static parseTrackName(t){let e=P0.exec(t);if(e===null)throw new Error("PropertyBinding: Cannot parse trackName: "+t);let n={nodeName:e[2],objectName:e[3],objectIndex:e[4],propertyName:e[5],propertyIndex:e[6]},s=n.nodeName&&n.nodeName.lastIndexOf(".");if(s!==void 0&&s!==-1){let r=n.nodeName.substring(s+1);L0.indexOf(r)!==-1&&(n.nodeName=n.nodeName.substring(0,s),n.objectName=r)}if(n.propertyName===null||n.propertyName.length===0)throw new Error("PropertyBinding: can not parse propertyName from trackName: "+t);return n}static findNode(t,e){if(e===void 0||e===""||e==="."||e===-1||e===t.name||e===t.uuid)return t;if(t.skeleton){let n=t.skeleton.getBoneByName(e);if(n!==void 0)return n}if(t.children){let n=function(r){for(let o=0;o<r.length;o++){let a=r[o];if(a.name===e||a.uuid===e)return a;let c=n(a.children);if(c)return c}return null},s=n(t.children);if(s)return s}return null}_getValue_unavailable(){}_setValue_unavailable(){}_getValue_direct(t,e){t[e]=this.targetObject[this.propertyName]}_getValue_array(t,e){let n=this.resolvedProperty;for(let s=0,r=n.length;s!==r;++s)t[e++]=n[s]}_getValue_arrayElement(t,e){t[e]=this.resolvedProperty[this.propertyIndex]}_getValue_toArray(t,e){this.resolvedProperty.toArray(t,e)}_setValue_direct(t,e){this.targetObject[this.propertyName]=t[e]}_setValue_direct_setNeedsUpdate(t,e){this.targetObject[this.propertyName]=t[e],this.targetObject.needsUpdate=!0}_setValue_direct_setMatrixWorldNeedsUpdate(t,e){this.targetObject[this.propertyName]=t[e],this.targetObject.matrixWorldNeedsUpdate=!0}_setValue_array(t,e){let n=this.resolvedProperty;for(let s=0,r=n.length;s!==r;++s)n[s]=t[e++]}_setValue_array_setNeedsUpdate(t,e){let n=this.resolvedProperty;for(let s=0,r=n.length;s!==r;++s)n[s]=t[e++];this.targetObject.needsUpdate=!0}_setValue_array_setMatrixWorldNeedsUpdate(t,e){let n=this.resolvedProperty;for(let s=0,r=n.length;s!==r;++s)n[s]=t[e++];this.targetObject.matrixWorldNeedsUpdate=!0}_setValue_arrayElement(t,e){this.resolvedProperty[this.propertyIndex]=t[e]}_setValue_arrayElement_setNeedsUpdate(t,e){this.resolvedProperty[this.propertyIndex]=t[e],this.targetObject.needsUpdate=!0}_setValue_arrayElement_setMatrixWorldNeedsUpdate(t,e){this.resolvedProperty[this.propertyIndex]=t[e],this.targetObject.matrixWorldNeedsUpdate=!0}_setValue_fromArray(t,e){this.resolvedProperty.fromArray(t,e)}_setValue_fromArray_setNeedsUpdate(t,e){this.resolvedProperty.fromArray(t,e),this.targetObject.needsUpdate=!0}_setValue_fromArray_setMatrixWorldNeedsUpdate(t,e){this.resolvedProperty.fromArray(t,e),this.targetObject.matrixWorldNeedsUpdate=!0}_getValue_unbound(t,e){this.bind(),this.getValue(t,e)}_setValue_unbound(t,e){this.bind(),this.setValue(t,e)}bind(){let t=this.node,e=this.parsedPath,n=e.objectName,s=e.propertyName,r=e.propertyIndex;if(t||(t=i.findNode(this.rootNode,e.nodeName),this.node=t),this.getValue=this._getValue_unavailable,this.setValue=this._setValue_unavailable,!t){console.warn("THREE.PropertyBinding: No target node found for track: "+this.path+".");return}if(n){let u=e.objectIndex;switch(n){case"materials":if(!t.material){console.error("THREE.PropertyBinding: Can not bind to material as node does not have a material.",this);return}if(!t.material.materials){console.error("THREE.PropertyBinding: Can not bind to material.materials as node.material does not have a materials array.",this);return}t=t.material.materials;break;case"bones":if(!t.skeleton){console.error("THREE.PropertyBinding: Can not bind to bones as node does not have a skeleton.",this);return}t=t.skeleton.bones;for(let f=0;f<t.length;f++)if(t[f].name===u){u=f;break}break;case"map":if("map"in t){t=t.map;break}if(!t.material){console.error("THREE.PropertyBinding: Can not bind to material as node does not have a material.",this);return}if(!t.material.map){console.error("THREE.PropertyBinding: Can not bind to material.map as node.material does not have a map.",this);return}t=t.material.map;break;default:if(t[n]===void 0){console.error("THREE.PropertyBinding: Can not bind to objectName of node undefined.",this);return}t=t[n]}if(u!==void 0){if(t[u]===void 0){console.error("THREE.PropertyBinding: Trying to bind to objectIndex of objectName, but is undefined.",this,t);return}t=t[u]}}let o=t[s];if(o===void 0){let u=e.nodeName;console.error("THREE.PropertyBinding: Trying to update property for track: "+u+"."+s+" but it wasn't found.",t);return}let a=this.Versioning.None;this.targetObject=t,t.needsUpdate!==void 0?a=this.Versioning.NeedsUpdate:t.matrixWorldNeedsUpdate!==void 0&&(a=this.Versioning.MatrixWorldNeedsUpdate);let c=this.BindingType.Direct;if(r!==void 0){if(s==="morphTargetInfluences"){if(!t.geometry){console.error("THREE.PropertyBinding: Can not bind to morphTargetInfluences because node does not have a geometry.",this);return}if(!t.geometry.morphAttributes){console.error("THREE.PropertyBinding: Can not bind to morphTargetInfluences because node does not have a geometry.morphAttributes.",this);return}t.morphTargetDictionary[r]!==void 0&&(r=t.morphTargetDictionary[r])}c=this.BindingType.ArrayElement,this.resolvedProperty=o,this.propertyIndex=r}else o.fromArray!==void 0&&o.toArray!==void 0?(c=this.BindingType.HasFromToArray,this.resolvedProperty=o):Array.isArray(o)?(c=this.BindingType.EntireArray,this.resolvedProperty=o):this.propertyName=s;this.getValue=this.GetterByBindingType[c],this.setValue=this.SetterByBindingTypeAndVersioning[c][a]}unbind(){this.node=null,this.getValue=this._getValue_unbound,this.setValue=this._setValue_unbound}};le.Composite=Ac;le.prototype.BindingType={Direct:0,EntireArray:1,ArrayElement:2,HasFromToArray:3};le.prototype.Versioning={None:0,NeedsUpdate:1,MatrixWorldNeedsUpdate:2};le.prototype.GetterByBindingType=[le.prototype._getValue_direct,le.prototype._getValue_array,le.prototype._getValue_arrayElement,le.prototype._getValue_toArray];le.prototype.SetterByBindingTypeAndVersioning=[[le.prototype._setValue_direct,le.prototype._setValue_direct_setNeedsUpdate,le.prototype._setValue_direct_setMatrixWorldNeedsUpdate],[le.prototype._setValue_array,le.prototype._setValue_array_setNeedsUpdate,le.prototype._setValue_array_setMatrixWorldNeedsUpdate],[le.prototype._setValue_arrayElement,le.prototype._setValue_arrayElement_setNeedsUpdate,le.prototype._setValue_arrayElement_setMatrixWorldNeedsUpdate],[le.prototype._setValue_fromArray,le.prototype._setValue_fromArray_setNeedsUpdate,le.prototype._setValue_fromArray_setMatrixWorldNeedsUpdate]];var d_=new Float32Array(1);typeof __THREE_DEVTOOLS__<"u"&&__THREE_DEVTOOLS__.dispatchEvent(new CustomEvent("register",{detail:{revision:"170"}}));typeof window<"u"&&(window.__THREE__?console.warn("WARNING: Multiple instances of Three.js being imported."):window.__THREE__="170");var Dn=[{name:"\uBAB8\uD480\uAE30",len:117.7,y0:0,y1:8,par:25e3,min:9e3},{name:"\uBC00\uCE58\uAE30",len:100,y0:8,y1:14,par:24e3,min:8500},{name:"\uD754\uB4E4 \uB2E4\uB9AC",len:100,y0:14,y1:20,par:22e3,min:8e3},{name:"\uBB34\uB108\uC9C0\uB294 \uAE38",len:103.4,y0:20,y1:24,par:22e3,min:7500},{name:"\uC815\uC0C1",len:88,y0:24,y1:34.5,par:26e3,min:9e3}],U0=[1e3,600,300],zc=480;function so(i,t,e,n,s,r,o){let a=i*1e3;for(let p=0;p<i;p++)a+=Math.max(0,Math.floor((Dn[p].par*1.6-t[p])/100));let c=Math.min(i,Dn.length-1),u=Dn[c],f=n?Dn[Dn.length-1].y1:u.y0+Math.min(1,Math.max(0,e/u.len))*(u.y1-u.y0);return a+=Math.floor(f*10),n&&(a+=(U0[s-1]||0)+(r?0:500)),a+Math.min(Math.max(o,0),20)*50}var kc=window.STEPUP_RACE||{},jh=/[?&]dev\b/.test(location.search),F0=-30,N0=11.2,O0=8.6,B0=60,z0=24,ne=.42,Vs=1.5,Hh=.4,k0=.1,os=.13,Ns=1/120,H0=.8,Qh=1.8,Vh=4.5,ue=40,Ne=7,gi=ue-Ne,V0=7,G0=-20,be=Math.PI*2,Wc=[{top:"#EAF4FF",stripe:"#D2E5FF",side:1784427,accent:1140479},{top:"#FFF1DE",stripe:"#FFD9A8",side:12730636,accent:16747008},{top:"#FFE6F1",stripe:"#FFC2DD",side:10295117,accent:16732043},{top:"#E3FBF4",stripe:"#B5F0DF",side:1013358,accent:1357990},{top:"#FFF7D1",stripe:"#FFE58A",side:5972406,accent:16761856}],Gh=16732043,tu=16752412,Wh=1140479,Xh=["\uD608\uC555 \uC8FC\uC758 \u{1FA78}","\uC544\uB798\uCE35 \uC190\uB2D8 \uC785\uC7A5~","\uB2E4\uC2DC \uC62C\uB77C\uAC00\uBA74 \uB418\uC8E0 \u{1F607}","\uADF8 \uC790\uB9AC\u2026 \uC544\uAE4C \uC9C0\uB098\uAC04 \uACF3 \uAC19\uC740\uB370\uC694?","\uAD1C\uCC2E\uC544\uC694, \uD0D1\uC740 \uC548 \uB3C4\uB9DD\uAC00\uC694","\uC2EC\uD638\uD761 \uD55C \uBC88 \u{1F9D8}"];function ls(i){return function(){i|=0,i=i+1831565813|0;let t=Math.imul(i^i>>>15,1|i);return t=t+Math.imul(t^t>>>7,61|t)^t,((t^t>>>14)>>>0)/4294967296}}var _i=(i,t)=>(i%t+t)%t;function Oe(i,t,e){let n=i/ue,s=ue+t;return{x:s*Math.cos(n),y:e,z:s*Math.sin(n),a:n}}function tl(i,t,e,n,s,r){let o=s-e,a=r-n,c=o*o+a*a,u=c?Math.max(0,Math.min(1,((i-e)*o+(t-n)*a)/c)):0,f=e+o*u,p=n+a*u;return[Math.hypot(i-f,t-p),f,p]}var Bs=class{constructor(t){Object.assign(this,{kind:"box",cx:0,cz:0,top:0,h:1.4,yaw:0,hx:2,hz:2,r:0,move:null,spin:0,belt:0,fall:!1,theme:0,color:null,ice:!1,mystery:!1,door:null,baseYaw:0},t),this.static=!this.move&&!this.spin&&!this.fall&&this.kind!=="tilt"&&!this.door,this.enabled=!0,this.state="idle",this.timer=0,this.drop=0,this.tilt=0,this.flash=0,this.warn=!1,this.baseX=this.cx,this.baseZ=this.cz,this.vx=0,this.vz=0,this.p={cx:this.cx,cz:this.cz,top:this.top,yaw:this.yaw,tilt:0},this.bounds()}bounds(){if(this.kind==="arc"){let t=1/0,e=-1/0,n=1/0,s=-1/0;for(let r=0;r<=8;r++){let o=this.a0+(this.a1-this.a0)*r/8;for(let a of[this.r0,this.r1]){let c=a*Math.cos(o),u=a*Math.sin(o);t=Math.min(t,c),e=Math.max(e,c),n=Math.min(n,u),s=Math.max(s,u)}}this.bb=[t,e,n,s,Math.min(this.y0,this.y1)-this.h,Math.max(this.y0,this.y1)]}else{let t=(this.kind==="disc"?this.r:Math.hypot(this.hx,this.hz))+(this.move?this.move.amp:0);this.bb=[this.cx-t,this.cx+t,this.cz-t,this.cz+t,this.top-this.h-30,this.top+3]}}near(t,e,n){let s=this.bb;return t>s[0]-1.5&&t<s[1]+1.5&&n>s[2]-1.5&&n<s[3]+1.5&&e>s[4]-2&&e<s[5]+3}save(){let t=this.p;t.cx=this.cx,t.cz=this.cz,t.top=this.top,t.yaw=this.yaw,t.tilt=this.tilt}update(t,e){if(this.move){let n=this.move,s=this.offset(t),r=this.offset(t+.01);if(this.cx=this.baseX+n.dir[0]*s,this.cz=this.baseZ+n.dir[1]*s,this.vx=n.dir[0]*(r-s)/.01,this.vz=n.dir[1]*(r-s)/.01,n.profile==="punch"){let o=_i(t/n.period+(n.phase||0),1);this.flash=o>.22&&o<.4?1:0}}if(this.spin&&(this.yaw=this.baseYaw+this.spin*t),this.fall&&(this.state==="shake"?(this.timer-=e,this.timer<=0&&(this.state="drop",this.timer=3.2,this.enabled=!1)):this.state==="drop"&&(this.timer-=e,this.drop+=e,this.timer<=0&&(this.state="idle",this.enabled=!0,this.drop=0))),this.door){let n=_i(t/this.door.period+(this.door.phase||0),1);this.enabled=n>=this.door.open,this.warn=n>.72,this.doorPh=n}}offset(t){let e=this.move;if(e.profile==="punch"){let n=_i(t/e.period+(e.phase||0),1);return n<.4?0:n<.47?e.amp*(n-.4)/.07:n<.7?e.amp:e.amp*(1-(n-.7)/.3)}return e.amp*Math.sin(be*t/e.period+(e.phase||0))}trigger(){this.fall&&this.state==="idle"&&(this.state="shake",this.timer=this.fake?.04:.32)}toLocal(t,e,n){let s=n||this,r=t-s.cx,o=e-s.cz,a=Math.cos(-s.yaw),c=Math.sin(-s.yaw);return[r*a-o*c,r*c+o*a]}toWorld(t,e,n){let s=n||this,r=Math.cos(s.yaw),o=Math.sin(s.yaw);return[s.cx+t*r-e*o,s.cz+t*o+e*r]}arcD(t,e){return _i(Math.atan2(e,t)-this.a0,be)}inside(t,e,n){if(this.kind==="disc"){let o=t-this.cx,a=e-this.cz;return o*o+a*a<=(this.r+n)**2}if(this.kind==="arc"){let o=Math.hypot(t,e);if(o<this.r0-n||o>this.r1+n)return!1;let a=this.arcD(t,e),c=this.a1-this.a0,u=n/o;return a<=c+u||a>=be-u}let[s,r]=this.toLocal(t,e);return Math.abs(s)<=this.hx+n&&Math.abs(r)<=this.hz+n}topAt(t,e){if(this.kind==="arc"){let n=this.a1-this.a0,s=this.arcD(t,e);return s>n&&(s=s>n+(be-n)/2?0:n),this.y0+(this.y1-this.y0)*s/n}if(this.kind==="tilt"){let[n]=this.toLocal(t,e);return this.top+Math.tan(this.tilt)*Math.max(-this.hx,Math.min(this.hx,n))}return this.top}bottom(){return(this.kind==="arc"?Math.min(this.y0,this.y1):this.top-(this.kind==="tilt"?Math.abs(Math.tan(this.tilt))*this.hx:0))-this.h}pushOut(t){if(this.kind==="disc"){let d=t.x-this.cx,g=t.z-this.cz,_=Math.hypot(d,g),m=this.r+ne;if(_>=m)return!1;let l=_>1e-5?m/_:1;return t.x=this.cx+(_>1e-5?d*l:m),t.z=this.cz+(_>1e-5?g*l:0),!0}if(this.kind==="arc"){let d=Math.hypot(t.x,t.z),g=this.arcD(t.x,t.z),_=this.a1-this.a0,m=E=>{let v=E/d;t.x*=v,t.z*=v},l=E=>{let v=Math.atan2(t.z,t.x)+E;t.x=d*Math.cos(v),t.z=d*Math.sin(v)};if(g<=_){if(d>=this.r0&&d<=this.r1){let E=[[d-this.r0,()=>m(this.r0-ne)],[this.r1-d,()=>m(this.r1+ne)],[g*d,()=>l(-(g+ne/d))],[(_-g)*d,()=>l(_-g+ne/d)]];return E.sort((v,C)=>v[0]-C[0]),E[0][1](),!0}return d<this.r0&&this.r0-d<ne?(m(this.r0-ne),!0):d>this.r1&&d-this.r1<ne?(m(this.r1+ne),!0):!1}let S=g>_+(be-_)/2?this.a0:this.a1,b=Math.cos(S),y=Math.sin(S),[I,T,R]=tl(t.x,t.z,this.r0*b,this.r0*y,this.r1*b,this.r1*y);if(I>=ne)return!1;let A=ne/Math.max(I,1e-4);return t.x=T+(t.x-T)*A,t.z=R+(t.z-R)*A,!0}let[e,n]=this.toLocal(t.x,t.z),s=Math.max(-this.hx,Math.min(this.hx,e)),r=Math.max(-this.hz,Math.min(this.hz,n)),o=e-s,a=n-r,c=Math.hypot(o,a),u,f;if(c>=ne)return!1;if(c>1e-5)u=s+o/c*ne,f=r+a/c*ne;else{let d=this.hx-Math.abs(e),g=this.hz-Math.abs(n);d<g?(u=Math.sign(e||1)*(this.hx+ne),f=n):(u=e,f=Math.sign(n||1)*(this.hz+ne))}let[p,h]=this.toWorld(u,f);return t.x=p,t.z=h,!0}carry(t){if(!(this.static||this.door)){if(this.kind==="disc"||this.spin){let e=t.x-this.p.cx,n=t.z-this.p.cz,s=this.yaw-this.p.yaw,r=Math.cos(s),o=Math.sin(s);t.x=this.cx+e*r-n*o,t.z=this.cz+e*o+n*r}else t.x+=this.cx-this.p.cx,t.z+=this.cz-this.p.cz;if(this.kind==="tilt"){let[e]=this.toLocal(t.x,t.z);t.y+=(Math.tan(this.tilt)-Math.tan(this.p.tilt))*e}}}},hs=class{constructor(t){Object.assign(this,t),this.bars=t.bars.map(e=>Object.assign({a:0},e))}update(t){for(let e of this.bars)e.a=e.spin*t+(e.phase||0)}hit(t){let e=t.pos;for(let n of this.bars){let s=this.base+n.y0,r=this.base+n.y1;if(e.y>r||e.y+Vs<s)continue;let o=Math.cos(n.a)*n.len,a=Math.sin(n.a)*n.len,[c,u,f]=tl(e.x,e.z,this.x-o,this.z-a,this.x+o,this.z+a);if(c>ne+.28)continue;let p=Math.sign((e.x-this.x)*-Math.sin(n.a)+(e.z-this.z)*Math.cos(n.a))||1,h=-Math.sin(n.a)*p,d=Math.cos(n.a)*p,g=(ne+.3)/Math.max(c,.001);return e.x=u+(e.x-u)*g,e.z=f+(e.z-f)*g,{vx:h*8.5,vz:d*8.5,vy:6,stun:.42}}return null}},us=class{constructor(t){Object.assign(this,{len:7,r:1.15,amp:.95,period:2.1,phase:0},t),this.update(0)}update(t){let e=be/this.period,n=this.amp*Math.sin(e*t+this.phase),s=this.len*Math.sin(n);this.f=n,this.v=this.amp*e*Math.cos(e*t+this.phase)*this.len*Math.cos(n),this.hx=this.x+this.n[0]*s,this.hz=this.z+this.n[1]*s,this.hy=this.py-this.len*Math.cos(n),this.lat=s}hit(t){let e=t.pos,n=Math.max(e.y+ne,Math.min(e.y+Vs-ne,this.hy));if(Math.hypot(e.x-this.hx,n-this.hy,e.z-this.hz)>this.r+ne)return null;let s=Math.sign(this.v)||1;return{vx:this.n[0]*s*14,vz:this.n[1]*s*14,vy:7.5,stun:.55}}},Xc=class{constructor(t){Object.assign(this,t),this.list=[],this.next=.3,this.rnd=ls(t.seed||7)}update(t,e,n,s){if(this.next-=e,this.next<=0&&!(s>this.sTop-7)){this.next=this.every*(.6+this.rnd()*.7);let r=1.1+this.rnd()*.6;this.list.push({s:this.sTop,u:(this.rnd()*2-1)*(Ne-r-.2),r,v:this.speed*(.85+this.rnd()*.35),spin:0,mesh:null})}for(let r of this.list){r.s-=r.v*e,r.spin+=r.v*e/r.r;let o=Oe(r.s,r.u,je(r.s)+r.r);r.x=o.x,r.y=o.y,r.z=o.z,r.a=o.a,r.s<this.sEnd&&(r.dead=!0)}for(let r of this.list)r.dead&&r.mesh&&(n.group.remove(r.mesh),r.mesh=null);this.list=this.list.filter(r=>!r.dead)}hit(t){let e=t.pos;for(let n of this.list){let s=Math.max(e.y+ne,Math.min(e.y+Vs-ne,n.y));if(Math.hypot(e.x-n.x,s-n.y,e.z-n.z)<n.r+ne){let r=-Math.sin(n.a),o=Math.cos(n.a);return{vx:-r*n.v*1.2,vz:-o*n.v*1.2,vy:7,stun:.5}}}return null}},qc=class{constructor(t){Object.assign(this,{period:4.2,dur:1.3,force:5},t)}phase(t){return _i(t+(this.offset||0),this.period)}active(t){return this.phase(t)<this.dur}warn(t){return this.phase(t)>this.period-.9}},oo=[];function je(i){let t=0;for(let[e,n,s,r]of oo){if(i<e)break;t=i<=n?s+(r-s)*(i-e)/(n-e):r}return t}function W0(){let i={solids:[],hazards:[],balls:null,winds:[],secs:[],gates:[],signs:[],group:new me,finishS:0,flags:[],tilts:[]};oo=[];let t=0,e=0,n=0,s=ls(4242),r=(h,d=0,g={})=>{let _=ue+(g.u0!=null?g.u0:-Ne),m=ue+(g.u1!=null?g.u1:Ne),l=new Bs({kind:"arc",r0:_,r1:m,a0:t/ue,a1:(t+h)/ue,y0:e,y1:e+d,h:g.h||1.6,theme:n,belt:g.belt||0,ice:!!g.ice,door:g.door||null,s0:t,len:h});return i.solids.push(l),oo.push([t,t+h,e,e+d]),t+=h,e+=d,l},o=h=>{oo.push([t,t+h,e,e]),t+=h},a=(h,d,g)=>{let _=Oe(t+h,d,0),m=new Bs(Object.assign({cx:_.x,cz:_.z,yaw:_.a,baseYaw:_.a,theme:n,s0:t+h,u:d},g));return i.solids.push(m),m},c=h=>{let d=(t+h)/ue;return[Math.cos(d),Math.sin(d)]},u=(h,d,g)=>{let _=Oe(t+h,0,0);i.hazards.push(new hs(Object.assign({x:_.x,z:_.z,base:e,bars:d,s:t+h},g||{})))},f=h=>{i.secs.push({s:t,y:e,name:h,theme:n}),i.gates.push({s:t,y:e,k:i.secs.length})};n=0,f("\uBAB8\uD480\uAE30"),r(14),r(16,3),r(34),u(-25,[{y0:.2,y1:.75,spin:1.6,len:7.3}]),u(-9,[{y0:.2,y1:.75,spin:-1.75,len:7.3},{y0:1.05,y1:1.55,spin:1.2,len:7.3,phase:2.1}]),r(8,1),o(3.2),r(8,.8,{u0:-5,u1:5}),o(3.5),r(8,.8,{u0:-4,u1:4}),o(3),r(6),r(14,2.4),n=1,f("\uBC00\uCE58\uAE30"),r(8),r(40),[[-32,0],[-22,2.2],[-12,4.1]].forEach(([h,d])=>a(h,0,{hx:2.6,hz:.7,top:e+2.3,h:2.3,move:{dir:c(h),amp:4.6,period:2.2,phase:d},color:Wh})),r(10,3),r(32),[[-27,1],[-21,-1],[-15,1],[-9,-1]].forEach(([h,d],g)=>{let _=c(h),m=[-_[0]*d,-_[1]*d];a(h,d*(Ne+1.2),{hx:3.2,hz:1.3,top:e+2.2,h:2.2,move:{dir:m,amp:5.4,period:2.4,phase:g*.27,profile:"punch"},color:tu,punch:!0})}),a(-2.5,4.4,{hx:1.5,hz:1.5,top:e+.15,h:.4,mystery:!0}),r(10,3),n=2,f("\uD754\uB4E4 \uB2E4\uB9AC"),r(8),r(38,2,{u0:-1.8,u1:1.8}),[[-30,0],[-19,1.6],[-8,3.3]].forEach(([h,d])=>{let g=Oe(t+h,0,0);i.hazards.push(new us({x:g.x,z:g.z,py:je(t+h)+8.3,n:c(h),phase:d,a:g.a,s:t+h}))}),r(6,0,{u0:-5,u1:5}),[4.5,11,17.5,24].forEach((h,d)=>a(h,0,{hx:2.3,hz:2.3,top:e,h:1,move:{dir:c(h),amp:4.4,period:3.1,phase:d%2?Math.PI:0},color:Wh})),o(28),r(8),r(12,4),n=3,f("\uBB34\uB108\uC9C0\uB294 \uAE38"),r(6),a(7.4,0,{kind:"disc",r:6.2,top:e+.3,h:1.2,spin:.9}),u(7.4,[{y0:.2,y1:.75,spin:1.8,len:5.8}],{base:e+.3}),a(21,0,{kind:"disc",r:6.2,top:e+.3,h:1.2,spin:-1.05}),o(28.4),r(6);for(let h=0;h<8;h++)for(let d=0;d<5;d++)a(1.95+h*2.9,-5.8+d*2.9,{hx:1.3,hz:1.3,top:e,h:.5,fall:!0,fake:s()<.15,tileColor:(h+d)%2});o(24.6),r(6),i.tilts.push(a(7.2,0,{kind:"tilt",hx:7,hz:6,top:e,h:.8,ice:!0})),o(14.4),r(8),r(10,4),n=4,f("\uC815\uC0C1"),r(6);let p=t;r(50,9),i.balls=new Xc({sTop:t-.5,sEnd:p-2,speed:11,every:.62,seed:11}),i.winds.push(new qc({s0:p,s1:t,period:4.2,dur:1.3,force:5})),r(14,0,{ice:!0}),u(-7,[{y0:.2,y1:.75,spin:2,len:7.3},{y0:1.05,y1:1.55,spin:-1.4,len:7.3,phase:1.2}]),r(3,.5),i.signs.push({s:t,y:e,text:"\uAC70\uC758 \uB2E4 \uC654\uC5B4\uC694 \u{1F607}"}),i.door=r(5,.5,{door:{period:3,open:.36}}),r(2,.5),i.finishS=t+8,i.finishY=e,r(18,0,{u0:-8,u1:8}),i.endS=t,i.topY=e;for(let h=6;h<i.endS-4;h+=11)i.flags.push(h);if(i.spawnS=4,jh){let h=be*ue,d=1/0;for(let _=0;_+h<i.endS;_+=2)d=Math.min(d,je(_+h)-je(_));let g=i.secs.map((_,m)=>+((m+1<i.secs.length?i.secs[m+1].s:i.finishS)-_.s).toFixed(1));console.log("[tower] \uCE35 \uAC04\uACA9 \uCD5C\uC18C",d.toFixed(1),"m \xB7 \uAD6C\uAC04 \uAE38\uC774",g.join(" "),"\xB7 \uB192\uC774",i.secs.map(_=>_.y).join(" "),"\u2192",i.finishY),g.forEach((_,m)=>{Math.abs(_-Dn[m].len)>.05&&console.warn("[tower] \uAD6C\uAC04",m+1,"\uAE38\uC774\uAC00 \uADDC\uCE59\uACFC \uB2E4\uB974\uB2E4",_,Dn[m].len)})}return i}var qh={};function At(i,t){let e=i+JSON.stringify(t||{});return qh[e]||(qh[e]=new Fe(Object.assign({color:i},t||{})))}function Fn(i,t,e){let n=document.createElement("canvas");n.width=i,n.height=t,e(n.getContext("2d"),i,t);let s=new Xr(n);return s.colorSpace=De,s.anisotropy=4,s}var X0=Wc.map(i=>{let t=Fn(128,128,(e,n,s)=>{e.fillStyle=i.top,e.fillRect(0,0,n,s),e.fillStyle=i.stripe;for(let r=0;r<4;r++)e.fillRect(0,r*32,n,16);e.fillStyle="rgba(255,255,255,.55)",e.fillRect(0,0,6,s),e.fillRect(n-6,0,6,s)});return t.wrapS=t.wrapT=Cn,t}),q0=(()=>{let i=Fn(128,128,(t,e,n)=>{t.fillStyle="#CFF4FF",t.fillRect(0,0,e,n),t.strokeStyle="rgba(255,255,255,.9)",t.lineWidth=3;for(let s=0;s<6;s++)t.beginPath(),t.moveTo(Math.random()*e,Math.random()*n),t.lineTo(Math.random()*e,Math.random()*n),t.stroke()});return i.wrapS=i.wrapT=Cn,i})(),Y0=["#6FA8FF","#5CE1C6"].map(i=>Fn(64,64,(t,e,n)=>{t.fillStyle=i,t.fillRect(0,0,e,n),t.strokeStyle="rgba(11,26,51,.32)",t.lineWidth=1.6,t.beginPath(),t.moveTo(10,8),t.lineTo(26,30),t.lineTo(20,46),t.moveTo(26,30),t.lineTo(48,36),t.lineTo(56,54),t.stroke()})),Z0=(()=>{let i=Fn(128,128,(t,e,n)=>{for(let s=-2;s<8;s++)t.fillStyle=s%2?"#111":"#FFD23F",t.beginPath(),t.moveTo(s*32,0),t.lineTo(s*32+32,0),t.lineTo(s*32+32-64,n),t.lineTo(s*32-64,n),t.fill()});return i.wrapS=i.wrapT=Cn,i})();function ro(i,t,e,n=512,s=128){return Fn(n,s,r=>{r.fillStyle=t,r.fillRect(0,0,n,s),r.fillStyle="rgba(255,255,255,.14)";for(let o=0;o<n/32;o+=2)r.fillRect(o*32,0,32,10),r.fillRect(o*32+32,s-10,32,10);r.fillStyle=e,r.font=`900 ${Math.round(s*.48)}px system-ui, sans-serif`,r.textAlign="center",r.textBaseline="middle",r.fillText(i,n/2,s/2+4)})}function $0(i){let t=Math.max(4,Math.ceil((i.a1-i.a0)*ue/2.5)),e=[],n=[],s=[],r=[],o=i.a1-i.a0,a=o*ue,c=(l,S,b,y,I)=>(e.push(l,S,b),n.push(y,I),e.length/3-1),u=(l,S,b,y)=>s.push(l,S,b,l,b,y),f=l=>i.y0+(i.y1-i.y0)*l/t,p=l=>i.a0+o*l/t,h=s.length,d=[];for(let l=0;l<=t;l++){let S=p(l),b=f(l),y=a*l/t/6;d.push([c(i.r0*Math.cos(S),b,i.r0*Math.sin(S),0,y),c(i.r1*Math.cos(S),b,i.r1*Math.sin(S),(i.r1-i.r0)/14,y)])}for(let l=0;l<t;l++)u(d[l][0],d[l+1][0],d[l+1][1],d[l][1]);r.push([h,s.length-h,0]),h=s.length;let g=(l,S)=>{let b=[],y=[];for(let I=0;I<=t;I++){let T=p(I),R=f(I);b.push(c(l*Math.cos(T),R,l*Math.sin(T),I/t,1)),y.push(c(l*Math.cos(T),R-i.h,l*Math.sin(T),I/t,0))}for(let I=0;I<t;I++)S?u(b[I],y[I],y[I+1],b[I+1]):u(b[I],b[I+1],y[I+1],y[I])};g(i.r1,!1),g(i.r0,!0);let _=[];for(let l=0;l<=t;l++){let S=p(l),b=f(l)-i.h;_.push([c(i.r0*Math.cos(S),b,i.r0*Math.sin(S),0,0),c(i.r1*Math.cos(S),b,i.r1*Math.sin(S),1,0)])}for(let l=0;l<t;l++)u(_[l][0],_[l][1],_[l+1][1],_[l+1][0]);for(let l of[0,t]){let S=p(l),b=f(l),y=Math.cos(S),I=Math.sin(S),T=[c(i.r0*y,b,i.r0*I,0,1),c(i.r1*y,b,i.r1*I,1,1),c(i.r1*y,b-i.h,i.r1*I,1,0),c(i.r0*y,b-i.h,i.r0*I,0,0)];l?u(T[0],T[1],T[2],T[3]):u(T[0],T[3],T[2],T[1])}r.push([h,s.length-h,1]);let m=new Ee;m.setAttribute("position",new Jt(e,3)),m.setAttribute("uv",new Jt(n,2)),m.setIndex(s);for(let[l,S,b]of r)m.addGroup(l,S,b);return m.computeVertexNormals(),m}function J0(i){let t=i.group;for(let h of i.solids){let d=Wc[h.theme],g;if(h.kind==="arc"){let _;h.door?(_=new Fe({map:Z0,emissive:0}),h.flashMat=_):h.ice?_=new Fe({map:q0}):_=new Fe({map:X0[h.theme]}),g=new _t($0(h),[_,At(d.side)]),t.add(g),h.mesh=g;continue}if(h.kind==="disc"){g=new _t(new xe(h.r,h.r,h.h,48),[At(d.side),At(12120319),At(d.side)]);let _=new _t(new Jr(h.r*.2,h.r*.95,48,1,0,Math.PI),At(8378623));_.rotation.x=-Math.PI/2,_.position.y=h.h/2+.01,g.add(_)}else{let _,m;if(h.fall?(_=h.fake?new Fe({map:Y0[h.tileColor]}):At(h.tileColor?6087110:7317759),m=At(2772096)):h.kind==="tilt"?(_=new Fe({map:ro("\u21C4 \uC5BC\uC74C \uC2DC\uC18C \u21C4","#BFF0FF","#0B4A6B",256,256)}),m=At(8378623)):h.mystery?(_=new Fe({map:ro("?","#A77BFF","#FFFFFF",128,128),emissive:2756704}),m=At(5972406)):h.punch?(_=new Fe({color:h.color,emissive:0}),m=_,h.flashMat=_):(_=At(h.color!=null?h.color:d.accent),m=At(h.move?739268:d.side)),g=new _t(new He(h.hx*2,h.h,h.hz*2),[m,m,_,m,m,m]),h.punch){let l=new _t(new qe(1.25,16,12),h.flashMat);l.scale.set(.7,1,1.35),l.position.x=0,g.add(l)}}t.add(g),h.mesh=g}for(let h of i.hazards)if(h instanceof hs){let d=new me,g=new _t(new xe(.5,.7,2,16),At(1784427));g.position.y=1,d.add(g);let _=new _t(new qe(.7,16,10),At(16765503));_.position.y=2.1,d.add(_),h.meshes=h.bars.map((m,l)=>{let S=new _t(new He(m.len*2,m.y1-m.y0,.5),At(l?tu:Gh));S.position.y=(m.y0+m.y1)/2;let b=new me;return b.add(S),d.add(b),b}),d.position.set(h.x,h.base,h.z),t.add(d)}else if(h instanceof us){let d=new me;d.position.set(h.x,h.py,h.z),d.rotation.y=-h.a,[-7.8,7.8].forEach(S=>{let b=new _t(new He(.6,12,.6),At(10295117));b.position.set(S,-4.2,0),d.add(b)});let g=new _t(new He(16.2,.6,.6),At(10295117));g.position.y=1.5,d.add(g);let _=new me;d.add(_);let m=new _t(new xe(.13,.13,h.len,8),At(13227242));m.position.y=-h.len/2,_.add(m);let l=new _t(new xe(h.r,h.r,h.r*2.4,22),[At(Gh),At(16777215),At(16777215)]);l.rotation.z=Math.PI/2,l.position.y=-h.len,_.add(l),h.piv=_,t.add(d)}let e=(h,d,g,_)=>{let m=Oe(h,0,d),l=new me;l.position.set(m.x,d,m.z),l.rotation.y=-m.a,[-Ne-.4,Ne+.4].forEach(y=>{let I=new _t(new xe(.45,.45,7,12),At(16777215));I.position.set(y,3.5,0),l.add(I);let T=new _t(new qe(.8,14,10),At(16765503));T.position.set(y,7.2,0),l.add(T)});let S=ro(g,_,"#FFFFFF"),b=new _t(new He(15.6,1.7,.5),[At(16777215),At(16777215),At(16777215),At(16777215),new Je({map:S}),new Je({map:S})]);return b.position.y=6.3,l.add(b),t.add(l),l};i.gates.forEach(h=>{h.k>1&&e(h.s,h.y,`STAGE ${h.k} \xB7 ${i.secs[h.k-1].name}`,"#"+Wc[h.k-1].accent.toString(16).padStart(6,"0"))}),i.signs.forEach(h=>e(h.s,h.y,h.text,"#FF4F8B")),e(i.finishS,i.finishY,"StepUP \uC815\uC0C1","#1166FF");{let h=Oe(i.finishS,0,i.finishY),d=new me;d.position.set(h.x,i.finishY,h.z),d.rotation.y=-h.a;let g=new _t(new Qn(16,1.4),new Je({map:Fn(256,32,_=>{for(let m=0;m<16;m++)for(let l=0;l<2;l++)_.fillStyle=(m+l)%2?"#111":"#fff",_.fillRect(m*16,l*16,16,16)})}));g.rotation.x=-Math.PI/2,g.position.y=.03,d.add(g),t.add(d)}{let h=new me,d=Oe(i.finishS+6,0,i.topY),g=new _t(new xe(1.6,1.9,1.3,6,1,!0),new Fe({color:16761856,side:ze,emissive:3811840}));h.add(g);for(let _=0;_<6;_++){let m=new _t(new Ds(.42,1.1,6),At(16761856)),l=_/6*be;m.position.set(Math.cos(l)*1.7,1.1,Math.sin(l)*1.7),h.add(m);let S=new _t(new qe(.22,8,6),At(_%2?16732043:2414847));S.position.set(Math.cos(l)*1.85,.1,Math.sin(l)*1.85),h.add(S)}h.position.set(d.x,i.topY+4,d.z),i.crown=h,t.add(h)}let n=i.topY+14;{let h=Fn(512,256,(m,l,S)=>{let b=["#1166FF","#FFFFFF","#24D8FF","#FFFFFF"];for(let y=0;y<20;y++)m.fillStyle=b[y%4],m.beginPath(),m.moveTo(y*32-64,0),m.lineTo(y*32-32,0),m.lineTo(y*32-96,S),m.lineTo(y*32-128,S),m.fill()});h.wrapS=h.wrapT=Cn,h.repeat.set(10,4);let d=new _t(new xe(gi-.05,gi+2,n+40,64,1,!0),new Fe({map:h,side:ze}));d.position.y=(n-40)/2,t.add(d);let g=new _t(new xe(gi+1,gi-.05,2,64),At(16765503));g.position.y=n+1,t.add(g);let _=ro("StepUP  \xB7  StepUP  \xB7  StepUP  \xB7  StepUP","#0B1A33","#FFFFFF",2048,128);_.wrapS=Cn,_.repeat.set(2,1);for(let m=12;m<n;m+=18){let l=new _t(new xe(gi+.15,gi+.15,3,64,1,!0),new Je({map:_}));l.position.y=m,t.add(l)}}i.solids.push(new Bs({kind:"disc",cx:0,cz:0,r:gi-.4,top:n,h:n+60,pillar:!0}));let s=[16732043,16765503,2414847,8191922,16752412,10976255];i.flagMeshes=[],i.flags.forEach((h,d)=>{let g=Oe(h,Ne+.9,je(h)),_=new me;_.position.set(g.x,g.y-1.6,g.z);let m=new _t(new xe(.08,.08,6.5,6),At(16777215));m.position.y=3.25,_.add(m);let l=new _t(new Qn(1.8,1.1),new Fe({color:s[d%s.length],side:ze}));l.position.set(.9,5.8,0),_.add(l),_.rotation.y=-g.a+Math.PI/2,t.add(_),i.flagMeshes.push({fl:l,s:h})});let r=ls(9);i.balloons=[];for(let h=0;h<46;h++){let d=r()*be,g=ue+Ne+6+r()*30,_=-4+r()*(i.topY+18),m=new _t(new qe(1,14,12),At(s[h%s.length]));m.scale.set(1,1.2,1);let l=new _t(new xe(.02,.02,2.6,4),At(16777215));l.position.y=-2.4,m.add(l),m.position.set(Math.cos(d)*g,_,Math.sin(d)*g),m.userData={y0:_,ph:r()*6},t.add(m),i.balloons.push(m)}let o=new _t(new Ls(700,64),new Fe({color:3116031,emissive:666234}));o.rotation.x=-Math.PI/2,o.position.y=-24,t.add(o);let a=new _t(new xe(70,82,6,64),At(16769955));a.position.y=-24,t.add(a);let c=new _t(new xe(62,66,6.5,64),At(8182638));c.position.y=-23.6,t.add(c);for(let h=0;h<28;h++){let d=h/28*be,g=new me;for(let _=0;_<4;_++){let m=new _t(new He(34,4,6),At(s[(h+_)%s.length]));m.position.set(0,_*4,_*6),g.add(m)}g.position.set(Math.cos(d)*150,-22,Math.sin(d)*150),g.rotation.y=-d-Math.PI/2,t.add(g)}for(let h=0;h<22;h++){let d=new me,g=3+(r()*3|0);for(let l=0;l<g;l++){let S=new _t(new qe(3+r()*4,12,10),At(16777215));S.position.set(l*4,r()*2,r()*2),d.add(S)}let _=r()*be,m=110+r()*120;d.position.set(Math.cos(_)*m,10+r()*60,Math.sin(_)*m),t.add(d)}let u=new me,f=new _t(new xe(.07,.07,3.4,6),At(16777215));f.position.y=1.7,u.add(f);let p=new _t(new Qn(1.4,.9),new Je({color:16765503,side:ze,transparent:!0,opacity:.85}));p.position.set(.7,3,0),u.add(p),u.visible=!1,i.bestFlag=u,t.add(u)}function K0(i,t){for(let s of i.solids){let r=s.mesh;if(!r)continue;if(s.kind==="arc"){s.door&&(r.visible=s.enabled,s.flashMat.emissive.setHex(s.warn&&Math.floor(t/90)%2?6693376:0));continue}let o=s.fall&&s.state==="shake"?Math.sin(t/16)*.07:0;r.position.set(s.cx+o,s.top-s.h/2-(s.fall?s.drop*s.drop*9:0),s.cz),r.rotation.set(0,-s.yaw,s.kind==="tilt"?s.tilt:0),r.visible=!(s.fall&&s.drop>1.6),s.flashMat&&s.flashMat.emissive.setHex(s.flash&&Math.floor(t/90)%2?16720384:0)}for(let s of i.hazards)s instanceof hs?s.bars.forEach((r,o)=>{s.meshes[o].rotation.y=-r.a}):s instanceof us&&(s.piv.rotation.z=s.f);if(i.balls)for(let s of i.balls.list){if(!s.mesh){s.mesh=new _t(new qe(s.r,20,14),At(16739133));let r=new _t(new Us(s.r*.98,.14,6,24),At(16777215));r.rotation.y=Math.PI/2,s.mesh.add(r),i.group.add(s.mesh)}s.mesh.position.set(s.x,s.y,s.z),s.mesh.rotation.set(0,-s.a,0),s.mesh.children[0].rotation.x=s.spin}i.crown&&(i.crown.rotation.y+=.02,i.crown.position.y=i.topY+4+Math.sin(t/500)*.4);let e=i.winds[0],n=e&&e.active(L.t);i.flagMeshes.forEach((s,r)=>{let o=e&&s.s>=e.s0&&s.s<=e.s1;s.fl.rotation.y=Math.sin(t/(o&&(n||e.warn(L.t))?60:300)+r)*(o&&n?.9:.35)}),i.balloons.forEach(s=>{s.position.y=s.userData.y0+Math.sin(t/1400+s.userData.ph)*1.2})}function Yh(i,t){let e=new me,n=new me;e.add(n);let s=new _t(new Ps(.34,.36,6,14),At(i));s.position.y=.82,n.add(s);let r=new _t(new xe(.355,.355,.1,16),At(16777215));r.position.y=.84,n.add(r);let o=new _t(new qe(.33,18,14),At(16767416));o.position.y=1.36,n.add(o);let a=new _t(new qe(.345,18,10,0,be,0,Math.PI*.42),At(t?2759442:[2759442,7028255,14725194,1776411][i%4]));a.position.y=1.38,n.add(a);let c=new _t(new Us(.335,.055,6,22),At(t?16765503:i));c.rotation.x=Math.PI/2,c.position.y=1.45,n.add(c),[-.11,.11].forEach(p=>{let h=new _t(new qe(.045,8,6),At(727603));h.position.set(p,1.38,-.3),n.add(h)});let u=[];[-.17,.17].forEach(p=>{let h=new me;h.position.set(p,.5,0);let d=new _t(new xe(.08,.08,.34,6),At(727603));d.position.y=-.2,h.add(d);let g=new _t(new He(.26,.17,.5),At(16777215));g.position.set(0,-.4,-.08),h.add(g);let _=new _t(new He(.27,.06,.52),At(i));_.position.set(0,-.49,-.08),h.add(_),n.add(h),u.push(h)});let f=[];if([-.43,.43].forEach(p=>{let h=new _t(new Ps(.075,.26,4,8),At(i));h.position.set(p,.86,0),n.add(h),f.push(h)}),t){let p=new _t(new Ds(.22,.4,4),new Je({color:16765503}));p.rotation.x=Math.PI,p.position.y=2.2,e.add(p),e.userData.arrow=p}return Object.assign(e.userData,{body:n,legs:u,arms:f,phase:0}),e}function j0(i,t){let e=Fn(256,64,(s,r,o)=>{s.fillStyle="rgba(11,26,51,.72)",s.beginPath(),s.roundRect?s.roundRect(4,6,r-8,o-12,26):s.rect(4,6,r-8,o-12),s.fill(),s.fillStyle=t,s.font="800 30px system-ui, sans-serif",s.textAlign="center",s.textBaseline="middle",s.fillText(i,r/2,o/2+1)}),n=new Gr(new Rs({map:e,depthTest:!1,transparent:!0}));return n.scale.set(1.3,.33,1),n.position.y=2.1,n.renderOrder=5,n}function Q0(i,t,e,n,s){let r=i.userData;r.phase+=n*(4+t*1.4);let o=e?.25:Math.min(1,t/6),a=Math.sin(r.phase)*.9*o;r.legs[0].rotation.x=a,r.legs[1].rotation.x=-a,r.arms[0].rotation.x=-a,r.arms[1].rotation.x=a,r.body.position.y=e?0:Math.abs(Math.sin(r.phase))*.08*o,r.body.rotation.x=s==="dive"?-1.25:s==="shove"?-.45:0,r.body.position.z=s==="dive"?-.3:0,s==="shove"&&(r.arms[0].rotation.x=-1.5,r.arms[1].rotation.x=-1.5),r.arrow&&(r.arrow.position.y=2.2+Math.sin(performance.now()/200)*.12)}var eu=[{key:"rush",name:"\uB3CC\uC9C4\uC655",speed:1,wait:0,sloppy:.35,color:16732043},{key:"bully",name:"\uBC29\uD574\uAFBC",speed:.93,wait:.5,sloppy:.12,bully:1,color:14753096},{key:"block",name:"\uAE38\uB9C9\uC774",speed:.82,wait:.6,sloppy:.1,block:1,color:16752412},{key:"copy",name:"\uB530\uB77C\uC7C1\uC774",speed:.92,wait:.5,sloppy:.12,copy:1,color:10976255},{key:"shy",name:"\uAC81\uC7C1\uC774",speed:.9,wait:1,sloppy:.05,shy:1,color:1357990},{key:"goof",name:"\uC5C9\uB6B1\uC774",speed:.88,wait:.4,sloppy:.25,goof:1,color:16761856},{key:"gamble",name:"\uB3C4\uBC15\uC0AC",speed:.97,wait:.2,sloppy:.3,gamble:1,color:4054148}],Zh=["rush","bully","block","copy","shy","goof","gamble","rush","bully","copy","shy","goof","gamble","block","rush"],uo=class{constructor(t){Object.assign(this,{pos:new P,vel:new P,ground:null,onGround:!1,coyote:0,jumpBuf:0,stun:0,safe:0,yaw:0,theta:0,lastA:0,lives:2,out:!1,deadT:0,shoveCd:0,shoveT:0,diveUsed:!1,diving:!1,sec:0,maxS:0,finished:!1,lastShover:null,lastShoveAt:-9,ix:0,iz:0,airS:0,speedK:1},t)}place(t,e){let n=je(t),s=Oe(t,e,n);this.pos.set(s.x,n+.02,s.z),this.vel.set(0,0,0),this.theta=t/ue,this.lastA=Math.atan2(s.z,s.x),this.ground=null,this.onGround=!1,this.stun=0,this.safe=1,this.diving=!1,this.airS=t,this.yaw=Math.atan2(Math.sin(s.a),-Math.cos(s.a))}get s(){return this.theta*ue}get u(){return Math.hypot(this.pos.x,this.pos.z)-ue}},L={mode:"title",world:null,t:0,acc:0,playT:0,bodies:[],me:null,secT0:0,secMs:[],place:0,finishers:0,ko:0,revived:!1,run:null,daily:!1,paused:!1,shovesOnMe:0};function nu(){L.world&&(hn.remove(L.world.group),L.world.group.traverse(t=>{t.geometry&&t.geometry.dispose()}));let i=W0();J0(i),hn.add(i.group),L.world=i,L.t=0;for(let t of i.solids)t.update(0,0),t.save();for(let t of i.hazards)t.update(0);return i}function t_(i){let t=L.world,e=0;for(let n=0;n<t.secs.length;n++)i>=t.secs[n].s&&(e=n);return e}function cs(i,t){!t||i.safe>0||(i.onGround&&(i.airS=i.s),i.vel.x=t.vx,i.vel.z=t.vz,i.vel.y=Math.max(i.vel.y,t.vy),i.stun=t.stun,i.safe=.35,i.onGround=!1,i.ground=null,i.diving=!1,i===L.me&&Qe("hit"))}function iu(i,t){let e=L.world,n=i.pos,s=i.vel;i.ground&&i.ground.enabled&&i.ground.carry(n);let r=i.ix,o=i.iz,a=Math.hypot(r,o);a>1&&(r/=a,o/=a);let c=O0*i.speedK,u=i.onGround&&i.ground&&i.ground.ice,f=i.diving?.08:i.stun>0?.12:u?.13:1,p=(i.onGround?B0:z0)*f,h=r*c,d=o*c,g=h-s.x,_=d-s.z,m=Math.hypot(g,_),l=p*t;if(m>l?(s.x+=g/m*l,s.z+=_/m*l):(s.x=h,s.z=d),i.onGround&&i.ground&&i.ground.kind==="tilt"){let A=i.ground,E=-Math.sin(A.tilt)*11*t;n.x+=Math.cos(A.yaw)*E,n.z+=Math.sin(A.yaw)*E}for(let A of e.winds)if(A.active(L.t)&&i.s>=A.s0&&i.s<=A.s1){let E=Math.atan2(n.z,n.x);n.x+=Math.cos(E)*A.force*t,n.z+=Math.sin(E)*A.force*t}if(i.coyote=i.onGround?k0:Math.max(0,i.coyote-t),i.jumpBuf=Math.max(0,i.jumpBuf-t),i.jumpBuf>0&&i.coyote>0&&i.stun<=0)s.y=N0,i.coyote=0,i.jumpBuf=0,i.onGround=!1,i.ground=null,i.diveUsed=!1,i===L.me&&Qe("jump");else if(i.jumpBuf>0&&!i.onGround&&!i.diveUsed&&i.stun<=0&&i.coyote<=0){let A=[-Math.sin(i.yaw),-Math.cos(i.yaw)];s.x=A[0]*(c+Vh),s.z=A[1]*(c+Vh),s.y=Math.max(s.y,3),i.diveUsed=!0,i.diving=!0,i.jumpBuf=0,i===L.me&&Qe("dive")}s.y=Math.max(-45,s.y+F0*t);let S=n.y;n.x+=s.x*t,n.y+=s.y*t,n.z+=s.z*t,i.stun=Math.max(0,i.stun-t),i.safe=Math.max(0,i.safe-t),i.shoveCd=Math.max(0,i.shoveCd-t),i.shoveT=Math.max(0,i.shoveT-t);let b=null,y=-1/0;if(s.y<=.01)for(let A of e.solids){if(!A.enabled||!A.near(n.x,n.y,n.z))continue;let E=A.topAt(n.x,n.z);if(!A.inside(n.x,n.z,E>S+.01?ne:ne*.5))continue;let v=A.kind==="arc"||A.kind==="tilt"||A===i.ground?Hh+.3:Hh;n.y<=E+.02&&S>=E-v&&E>y&&(b=A,y=E)}let I=i.onGround;i.onGround=!1,b?(n.y=y,s.y=0,i.onGround=!0,i.ground=b,b.trigger(),i.diving&&(i.diving=!1,i.stun=.3),b.mystery?n_(i):I||e_(i)):(i.ground=null,I&&(i.airS=i.s));for(let A of e.solids){if(!A.enabled||A===b||!A.near(n.x,n.y,n.z))continue;let E=A.topAt(n.x,n.z);if(!(n.y>=E-.02||n.y+Vs<=A.bottom())&&A.pushOut(n)&&A.move)if(A.move.profile==="punch"&&Math.hypot(A.vx,A.vz)>3){let v=Math.hypot(A.vx,A.vz),C=Math.min(1,10/v);cs(i,{vx:A.vx*C,vz:A.vz*C,vy:6,stun:.45})}else s.x+=(A.vx-s.x)*.35,s.z+=(A.vz-s.z)*.35}if(i.safe<=0){for(let A of e.hazards){let E=A.hit(i);if(E){cs(i,E);break}}e.balls&&i.safe<=0&&cs(i,e.balls.hit(i))}let T=Math.atan2(n.z,n.x),R=T-i.lastA;R>Math.PI&&(R-=be),R<-Math.PI&&(R+=be),i.theta+=R,i.lastA=T}function e_(i){for(;i.theta>be&&je(i.s)-i.pos.y>4;)i.theta-=be;let t=i.airS-i.s;i===L.me&&(Qe("land"),t>25&&(Nn(`\u2212${Math.round(t)}m \xB7 ${Xh[Math.random()*Xh.length|0]}`,2.4),Qe("fall"))),i.airS=i.s}function n_(i){let t=i.s/ue,e=[-Math.sin(t),Math.cos(t)],n=Math.random()<.55;i.vel.set(e[0]*(n?11:-10),n?16:14,e[1]*(n?11:-10)),i.onGround=!1,i.ground=null,i.airS=i.s,i.stun=.25,i===L.me&&(Qe("boing"),Nn(n?"\uB7ED\uD0A4! \u{1F340}":"\uC544\u2026 \uB4A4\uB85C \u{1F635}"))}function i_(){let i=L.bodies.filter(t=>!t.out&&t.deadT<=0&&!t.finished);for(let t=0;t<i.length;t++)for(let e=t+1;e<i.length;e++){let n=i[t],s=i[e],r=s.pos.x-n.pos.x,o=s.pos.z-n.pos.z;if(Math.abs(r)>1||Math.abs(o)>1||Math.abs(n.pos.y-s.pos.y)>Vs)continue;let a=Math.hypot(r,o),c=ne*2;if(a>=c||a<1e-4)continue;let u=(c-a)/2,f=r/a,p=o/a;if(!n.isMe&&!s.isMe){let d=Math.atan2(n.pos.z,n.pos.x),g=-Math.sin(d),_=Math.cos(d),m=r*g+o*_||.01;if(f=g*Math.sign(m),p=_*Math.sign(m),u=(c-Math.abs(m))/2,u<=0)continue}n.pos.x-=f*u,n.pos.z-=p*u,s.pos.x+=f*u,s.pos.z+=p*u;let h=(s.vel.x-n.vel.x)*f+(s.vel.z-n.vel.z)*p;if(h<0){let d=h*.5;n.vel.x+=f*d,n.vel.z+=p*d,s.vel.x-=f*d,s.vel.z-=p*d}n.diving&&s.safe<=0&&(cs(s,{vx:f*8,vz:p*8,vy:4,stun:.35}),s.lastShover=n,s.lastShoveAt=L.t),s.diving&&n.safe<=0&&(cs(n,{vx:-f*8,vz:-p*8,vy:4,stun:.35}),n.lastShover=s,n.lastShoveAt=L.t)}}function _o(i){if(i.shoveCd>0||i.stun>0||i.out||i.deadT>0)return!1;i.shoveCd=H0,i.shoveT=.25;let t=[-Math.sin(i.yaw),-Math.cos(i.yaw)],e=!1;for(let n of L.bodies){if(n===i||n.out||n.deadT>0||n.finished)continue;let s=n.pos.x-i.pos.x,r=n.pos.z-i.pos.z,o=Math.hypot(s,r);if(o>Qh||Math.abs(n.pos.y-i.pos.y)>1.2||(s*t[0]+r*t[1])/Math.max(o,.001)<.35)continue;if(n===L.me&&i!==L.me){if(L.shovesOnMe>=2+L.me.sec)continue;L.shovesOnMe++}let a=9.5/Math.max(o,.5);cs(n,{vx:s*a,vz:r*a,vy:4.5,stun:.4}),n.lastShover=i,n.lastShoveAt=L.t,e=!0}return i===L.me&&Qe(e?"shove":"whiff"),e}function Os(i,t,e){return(_i(Math.atan2(t,i)-_i(e/ue,be)+Math.PI,be)-Math.PI)*ue}function su(i,t){let e=L.world,n=i.pos,s=i.s,r=i.u,o=i.persona,a=i.rnd,c=s/ue,u=[-Math.sin(c),Math.cos(c)],f=[Math.cos(c),Math.sin(c)];if(i.stun>0){i.ix=0,i.iz=0;return}if(i.goofT=Math.max(0,(i.goofT||0)-t),o.goof&&!i.goofT&&a()<t*.22&&(i.goofT=.6+a()*.8,i.goofKind=["stop","back","hop","spin"][a()*4|0]),i.goofT>0){if(i.goofKind==="stop"){i.ix=i.iz=0;return}if(i.goofKind==="back"){i.ix=-u[0]*.6,i.iz=-u[1]*.6;return}if(i.goofKind==="spin"){let l=L.t*9;i.ix=Math.cos(l),i.iz=Math.sin(l);return}i.goofKind==="hop"&&i.onGround&&(i.jumpBuf=os)}let p=i.lane||0,h=1,d=null;for(let l of e.solids){if(!l.enabled||l.pillar||!l.near(n.x,n.y,n.z)&&l.kind!=="arc")continue;if(l.kind==="arc"){if(s+3.4>=l.s0&&s+3.4<=l.s0+l.len&&Math.abs(l.y0-n.y)<3&&l.r1-l.r0<13){let y=(l.r1-l.r0)/2-.7;d={u:(l.r0+l.r1)/2-ue,half:Math.max(.2,y)}}continue}if(l.fall||l.mystery&&!o.gamble)continue;let S=l.topAt(l.cx,l.cz);if(S>n.y+.7||S<n.y-2.5)continue;let b=Os(l.cx,l.cz,s);b<.5||b>7.5||(!d||d.ds==null||b<d.ds)&&(d={u:Math.hypot(l.cx,l.cz)-ue,half:l.kind==="disc"?l.r*.45:l.hx*.55,ds:b})}let g=!1;d&&(p=d.u+Math.max(-d.half,Math.min(d.half,p-d.u)),g=d.ds==null&&Math.abs(r-d.u)>d.half+.4,g&&(h=Math.max(.2,1-(Math.abs(r-d.u)-d.half)/2.5))),p=s_(i,s,r,p,n);for(let l of e.solids){if(!l.move||l.kind!=="box"||!l.near(n.x,n.y,n.z)||l.top<=n.y+.7)continue;let S=Os(l.cx,l.cz,s);if(S<-1||S>5.5)continue;let b=Math.hypot(l.cx,l.cz)-ue;if(l.punch){p=-Math.sign(l.u||b)*3.6,l.flash&&S<3.2&&i.patient&&(h=0);continue}p=-5;let y=b-l.hx+(l.vx*f[0]+l.vz*f[1])*.3;S<2.2&&S>0&&y<r+.7&&(i.patient||a()<.5)&&(h=0)}for(let l of e.hazards)if(l instanceof us){let S=Os(l.x,l.z,s),b=Math.sign(l.v)===Math.sign(l.lat-r)||Math.abs(l.lat-r)>4.5;S>-1.2&&S<4.2&&!(b&&Math.abs(l.lat-r)>1.6)&&i.patient&&(h=S<.4?1:0)}if(e.door&&e.door.enabled===!1||e.door&&e.door.warn){let l=e.door.s0-s;l>0&&l<3.5&&i.patient&&(h=0)}for(let l of e.winds)s>=l.s0&&s<=l.s1&&(l.active(L.t)||l.warn(L.t))&&(p=Math.min(p,-2.5));if(e.balls)for(let l of e.balls.list){let S=l.s-s;S>-1&&S<8&&Math.abs(l.u-p)<l.r+1&&(p=l.u>r?l.u-l.r-1.3:l.u+l.r+1.3)}let _=L.me;if(_&&!_.out&&_.deadT<=0&&!_.finished){let l=Math.hypot(_.pos.x-n.x,_.pos.z-n.z);if(o.bully&&l<5&&Math.abs(_.pos.y-n.y)<1.2){p=_.u;let S=_.s-s;if(S>-1&&S<4&&(h=1,l<Qh-.1&&a()<t*4)){i.yaw=Math.atan2(-(_.pos.x-n.x),-(_.pos.z-n.z)),_o(i);return}}o.copy&&l<6&&_.s>s&&(p=_.u,!_.onGround&&i.onGround&&_.vel.y>5&&(i.jumpBuf=os))}o.block&&d&&d.half<2&&(p=d.u+Math.sin(L.t*1.7+i.seed)*1),o.shy&&(h===0?i.shyT=1:i.shyT>0&&(i.shyT-=t,i.shyT>.4&&(h=0))),p=Math.max(-Ne+.8,Math.min(Ne-1.6,p));let m=Math.max(-1,Math.min(1,(p-r)*.9));if(i.ix=u[0]*h+f[0]*m,i.iz=u[1]*h+f[1]*m,i.onGround&&h>0){let l=1.25+(a()-.5)*o.sloppy*2+(o.gamble?.5:0),S=Oe(s+l,r+m*.4,0),b=Oe(s+.9,r,0),y=zs(S.x,S.z,n.y+.5),I=zs(b.x,b.z,n.y+1.2),T=y===-1/0||y<n.y-.6||I>n.y+.35;for(let R of e.hazards)if(R instanceof hs&&Math.abs(R.base-n.y)<.8)for(let A of R.bars){let E=Math.cos(A.a)*A.len,v=Math.sin(A.a)*A.len,C=Oe(s+1.3+(a()-.5)*o.sloppy*1.6,r,0),[H]=tl(C.x,C.z,R.x-E,R.z-v,R.x+E,R.z+v);H<.75&&(T=!0)}T&&!(g&&(y===-1/0||y<n.y-.6))&&(i.jumpBuf=os),o.gamble&&a()<t*.08&&(i.jumpBuf=os)}i.yaw=Math.atan2(-i.ix,-i.iz)}function s_(i,t,e,n,s){let r=u=>{let f=Oe(t+1.8,u,0);for(let p of L.world.solids){if(!p.enabled||p.pillar||!p.near(f.x,s.y,f.z)||!p.inside(f.x,f.z,0))continue;let h=p.topAt(f.x,f.z);if(Math.abs(h-s.y)<1.2&&!(i.patient&&p.fake)&&p.state!=="shake")return!0}return!1};if(!L.world.solids.some(u=>u.fall&&u.near(s.x,s.y,s.z)&&Math.abs(Os(u.cx,u.cz,t))<4)||r(n))return n;let a=n,c=1/0;for(let u of[-2.9,2.9,-5.8,5.8,-1.45,1.45]){let f=Math.max(-Ne+.8,Math.min(Ne-.8,e+u));r(f)&&Math.abs(u)<c&&(c=Math.abs(u),a=f)}return a}function zs(i,t,e){let n=-1/0;for(let s of L.world.solids)if(s.enabled&&!s.pillar&&s.near(i,e,t)&&s.inside(i,t,.1)){let r=s.topAt(i,t);r<=e+.05&&r>n&&(n=r)}return n}var ao=[];function ru(i){nu(),Object.assign(L,{bodies:[],secMs:[],place:0,finishers:0,ko:0,revived:!1,playT:0,secT0:0,shovesOnMe:0});for(let r of ao)hn.remove(r);ao.length=0;let t=ls(i??Date.now()&16777215^24301),e=[];for(let r=0;r<4;r++)for(let o=0;o<4;o++)e.push([2+r*2.4,-4.8+o*3.2]);let n=5;L.me=new uo({name:"\uB098",isMe:!0}),L.me.place(e[n][0],e[n][1]),L.me.mesh=Yh(1140479,!0),L.bodies.push(L.me);let s=0;e.forEach((r,o)=>{if(o===n)return;let a=eu.find(u=>u.key===Zh[s%Zh.length]);s++;let c=new uo({lives:3,persona:a,name:a.name,rnd:ls(t()*1e9|0),seed:t()*6,lane:(t()*2-1)*2.5,patient:t()<.45+a.wait*.5,speedK:a.speed*(.93+t()*.09)});c.place(r[0],r[1]),c.mesh=Yh(a.color,!1),c.tag=j0(a.name,"#"+a.color.toString(16).padStart(6,"0")),c.mesh.add(c.tag),L.bodies.push(c)});for(let r of L.bodies)hn.add(r.mesh),ao.push(r.mesh),r.maxS=r.s}function r_(i){if(i.out||i.deadT>0||i.finished||i.onGround)return;let t=je(Math.min(i.s,L.world.endS));if(!(i.pos.y>G0&&(i.pos.y>t-V0||zs(i.pos.x,i.pos.z,i.pos.y)>-1/0))){if(i!==L.me&&i.lastShover===L.me&&L.t-i.lastShoveAt<3&&(L.ko++,Nn("\uBC00\uC5B4\uC11C \uB5A8\uC5B4\uB728\uB9BC +50")),i.lives--,i===L.me&&Qe("splash"),i.lives<=0){i.out=!0,i.mesh.visible=!1,i===L.me&&Yc(!1,"dead");return}i.deadT=1.2,i.mesh.visible=!1,i===L.me&&(L.revived=!0,Nn("\uD48D\uB369! \uBD80\uD65C \u2014 \uB9C8\uC9C0\uB9C9 \uBAA9\uC228\uC774\uC5D0\uC694",2))}}function o_(i,t){if(i.deadT-=t,i.deadT<=0){let e=L.world.secs[i.sec];i.place(e.s+2.5,i===L.me?0:(i.lane||0)*.5),i.mesh.visible=!0}}function ou(i){let t=L.world;L.t+=i;for(let n of t.solids)n.save();for(let n of t.tilts){let s=0;for(let o of L.bodies)if(o.ground===n&&!o.out){let[a]=n.toLocal(o.pos.x,o.pos.z);s+=a}let r=Math.max(-.3,Math.min(.3,-s*.035));n.tilt+=(r-n.tilt)*Math.min(1,i*2.2)}for(let n of t.solids)n.update(L.t,i);for(let n of t.hazards)n.update(L.t);let e=0;for(let n of L.bodies)n.out||(e=Math.max(e,n.s));t.balls&&t.balls.update(L.t,i,t,L.me&&!L.me.out?L.me.s:e);for(let n of L.bodies)if(!(n.out||n.finished)){if(n.deadT>0){o_(n,i);continue}n.isMe||su(n,i),iu(n,i)}i_();for(let n of L.bodies)if(!(n.out||n.finished||n.deadT>0)){if(n.onGround){n.maxS=Math.max(n.maxS,n.s);let s=t_(n.s);s>n.sec&&(n.sec=s,n===L.me&&$h(s))}if(n.s>=t.finishS&&n.pos.y>t.finishY-1.5&&(n.finished=!0,L.finishers++,n.place=L.finishers,n===L.me)){L.place=L.finishers,$h(t.secs.length),Yc(!0);return}if(r_(n),L.mode!=="play"&&L.mode!=="sim")return}L.playT>zc&&L.mode==="play"&&Yc(!1,"time")}function $h(i){let t=Math.round((L.playT-L.secT0)*1e3);L.secMs.push(t),L.secT0=L.playT,L.mode!=="sim"&&(Qe("win"),i<L.world.secs.length&&(fo(`<small>STAGE ${i+1}</small><b>${L.world.secs[i].name}</b>`,"good",1.6),hu(L.me.pos,60)),L.daily&&L.run&&Ei("tower_ping",{p_run:L.run,p_section:L.secMs.length,p_ms:t}).catch(()=>null))}var ht=i=>document.getElementById(i),xo=new zr({canvas:ht("stage"),antialias:(window.devicePixelRatio||1)<2,powerPreference:"high-performance"});xo.setPixelRatio(Math.min(window.devicePixelRatio||1,1.75));xo.outputColorSpace=De;var hn=new Hr;hn.background=Fn(4,256,(i,t,e)=>{let n=i.createLinearGradient(0,0,0,e);n.addColorStop(0,"#3B8EFF"),n.addColorStop(.55,"#9ED4FF"),n.addColorStop(1,"#E8F6FF"),i.fillStyle=n,i.fillRect(0,0,t,e)});hn.fog=new kr(12575743,120,320);hn.add(new Qr(15398143,4872827,1.7));var au=new to(16777215,1.6);au.position.set(30,60,20);hn.add(au);var Mi=new Ue(60,1,.1,900),xi=new _t(new Ls(.5,20),new Je({color:0,transparent:!0,opacity:.28,depthWrite:!1}));xi.rotation.x=-Math.PI/2;hn.add(xi);var Gs=400,yo=new Ee,yi=new Float32Array(Gs*3),cu=new Float32Array(Gs*3),el=[];for(let i=0;i<Gs;i++){yi[i*3+1]=-999,el.push([0,0,0,0]);let t=new Bt([16732043,16765503,2414847,8191922,16777215][i%5]);cu.set([t.r,t.g,t.b],i*3)}yo.setAttribute("position",new Ie(yi,3));yo.setAttribute("color",new Ie(cu,3));var lu=new Wr(yo,new Cs({size:.35,vertexColors:!0}));lu.frustumCulled=!1;hn.add(lu);var a_=0;function hu(i,t){for(let e=0;e<t;e++){let n=a_++%Gs;yi.set([i.x,i.y+2,i.z],n*3),el[n]=[(Math.random()-.5)*10,6+Math.random()*8,(Math.random()-.5)*10,2.5]}}function c_(i){for(let t=0;t<Gs;t++){let e=el[t];e[3]<=0||(e[3]-=i,e[1]-=14*i,yi[t*3]+=e[0]*i,yi[t*3+1]+=e[1]*i,yi[t*3+2]+=e[2]*i,e[3]<=0&&(yi[t*3+1]=-999))}yo.attributes.position.needsUpdate=!0}var gt={title:ht("title"),hud:ht("hud"),stage:ht("hudStage"),score:ht("hudScore"),time:ht("hudTime"),pos:ht("hudPos"),lives:ht("hudLives"),banner:ht("banner"),over:ht("over"),board:ht("board"),toast:ht("toast"),controls:ht("controls"),join:ht("join"),sns:ht("sns"),wind:ht("hudWind")};function uu(i){let t=Math.max(0,i)/1e3,e=Math.floor(t/60),n=t-e*60;return(e?e+":"+(n<10?"0":""):"")+n.toFixed(1)}var ie=(i,t)=>{i.hidden=!t},co=0,lo=0;function Nn(i,t){L.mode!=="sim"&&(gt.toast.textContent=i,gt.toast.hidden=!1,co=t||1.3)}function fo(i,t,e){gt.banner.innerHTML=i,gt.banner.className="banner "+(t||""),gt.banner.hidden=!1,lo=e||0}function du(i){return String(i??"").replace(/[&<>"']/g,t=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"})[t])}function fu(){let i=L.world,t=L.secMs.length;return t<i.secs.length?Math.max(0,Math.round(L.me.maxS-i.secs[t].s)):0}function pu(){let i=L.me,t=L.world;gt.stage.textContent=`STAGE ${Math.min(i.sec+1,t.secs.length)} \xB7 ${t.secs[Math.min(i.sec,t.secs.length-1)].name}`,gt.score.textContent=so(L.secMs.length,L.secMs,fu(),!1,0,L.revived,L.ko).toLocaleString("ko-KR")+"\uC810",gt.time.textContent=uu((zc-L.playT)*1e3);let e=0;for(let s of L.bodies)s!==i&&!s.out&&(s.finished||s.s>i.s)&&e++;gt.pos.textContent=`${e+1}\uC704 \xB7 \uB0A8\uC740 ${L.bodies.filter(s=>!s.out).length}\uBA85`,gt.lives.textContent=i.lives>=2?"\u2764\uFE0F\u2764\uFE0F":"\u2764\uFE0F\u{1F90D}";let n=t.winds.find(s=>i.s>=s.s0-6&&i.s<=s.s1);if(gt.wind.hidden=!(n&&(n.active(L.t)||n.warn(L.t))),t.bestFlag.visible=i.maxS-i.s>15,t.bestFlag.visible){let s=Oe(i.maxS,-Ne+1,je(i.maxS));t.bestFlag.position.set(s.x,s.y,s.z),t.bestFlag.rotation.y=-s.a}}var Kt={email:"",name:"",status:null};try{Kt.email=localStorage.getItem("tower_email")||"",Kt.name=localStorage.getItem("tower_name")||""}catch{}async function Ei(i,t){if(!kc.url)throw new Error("no server");let e=await fetch(kc.url+"/rest/v1/rpc/"+i,{method:"POST",headers:{"Content-Type":"application/json",apikey:kc.key},body:JSON.stringify(t),cache:"no-store"});if(!e.ok)throw new Error("http "+e.status);return e.json()}var l_=/^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 _.\-]{2,12}$/,vi={email:"\uC774\uBA54\uC77C \uC8FC\uC18C\uB97C \uB2E4\uC2DC \uD655\uC778\uD574 \uC8FC\uC138\uC694.",not_registered:"\uC0AC\uC804 \uB4F1\uB85D\uD55C \uC774\uBA54\uC77C\uC774 \uC544\uB2C8\uC5D0\uC694.",name:"\uB2C9\uB124\uC784\uC740 2~12\uC790, \uD55C\uAE00 \xB7 \uC601\uBB38 \xB7 \uC22B\uC790\uB85C \uC801\uC5B4 \uC8FC\uC138\uC694.",busy:"\uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.",no_runs:"\uC624\uB298 \uAE30\uD68C\uB97C \uB2E4 \uC37C\uC5B4\uC694. SNS\uC5D0 \uC62C\uB9AC\uBA74 \uD55C \uBC88 \uB354 \uD560 \uC218 \uC788\uC5B4\uC694.",url:"X \xB7 \uC778\uC2A4\uD0C0 \xB7 \uC2A4\uB808\uB4DC \uAC8C\uC2DC\uBB3C \uB9C1\uD06C\uAC00 \uC544\uB2C8\uC5D0\uC694.",account:"\uACC4\uC815 \uC544\uC774\uB514\uB97C \uC601\uBB38 \xB7 \uC22B\uC790 \xB7 \uBC11\uC904 \xB7 \uC810\uC73C\uB85C \uC801\uC5B4 \uC8FC\uC138\uC694.",duplicate:"\uC774\uBBF8 \uB0B8 \uAC8C\uC2DC\uBB3C\uC774\uC5D0\uC694. \uC0C8 \uAC8C\uC2DC\uBB3C\uC744 \uC62C\uB824 \uC8FC\uC138\uC694.",day_limit:"\uC624\uB298\uC740 SNS\uB85C \uBC1B\uC744 \uC218 \uC788\uB294 \uAE30\uD68C(3\uBC88)\uB97C \uB2E4 \uC37C\uC5B4\uC694. 00\uC2DC\uC5D0 \uB2E4\uC2DC \uCC44\uC6CC\uC838\uC694.",consent:"\uB3D9\uC758\uC5D0 \uCCB4\uD06C\uD574 \uC8FC\uC138\uC694.",closed:"\uC9C0\uAE08\uC740 \uAC8C\uC784\uC744 \uC26C\uACE0 \uC788\uC5B4\uC694."};function ks(){let i=Kt.status,t=ht("whoLine"),e=ht("dailyBtn");if(!i){t.innerHTML=Kt.email?"":'<span class="muted">\uC0AC\uC804 \uB4F1\uB85D\uD55C \uC774\uBA54\uC77C\uB85C \uCC38\uAC00\uD574\uC694</span>',e.textContent="\uC624\uB298\uC758 \uB3C4\uC804";return}t.innerHTML=`<b>${du(i.name)}</b> \xB7 \uC624\uB298 \uCD5C\uACE0 ${i.best_today!=null?(+i.best_today).toLocaleString("ko-KR")+"\uC810":"\u2014"} <button class="link" id="switchAcc" type="button">\uBC14\uAFB8\uAE30</button>`,ht("switchAcc").onclick=()=>po(!0),e.textContent=i.runs_left>0?`\uC624\uB298\uC758 \uB3C4\uC804 (${i.runs_left}\uBC88 \uB0A8\uC74C)`:"SNS \uC62C\uB9AC\uACE0 \uD55C \uBC88 \uB354 \uD558\uAE30"}async function nl(){if(!Kt.email){Kt.status=null,ks();return}try{let i=await Ei("tower_status",{p_email:Kt.email});i&&i.ok?Kt.status=i:i&&(i.code==="not_joined"||i.code==="not_registered")&&(Kt.status=null)}catch{}ks()}function po(){ie(gt.title,!1),ie(gt.join,!0),ht("joinEmail").value=Kt.email,ht("joinName").value=Kt.name,ht("joinErr").textContent=""}ht("joinForm").onsubmit=async i=>{i.preventDefault();let t=ht("joinEmail").value.trim().toLowerCase(),e=ht("joinName").value.replace(/\s+/g," ").trim(),n=ht("joinErr"),s=ht("joinBtn");if(n.textContent="",!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(t)){n.textContent=vi.email;return}if(!l_.test(e)){n.textContent=vi.name;return}if(!ht("joinConsent").checked){n.textContent=vi.consent;return}s.disabled=!0,s.textContent="\uD655\uC778 \uC911\u2026";try{let r=await Ei("tower_join",{p_email:t,p_name:e,p_trap:ht("joinTrap").value});if(r&&r.ok){Kt.email=t,Kt.name=e,Kt.status=r;try{localStorage.setItem("tower_email",t),localStorage.setItem("tower_name",e)}catch{}ie(gt.join,!1),ie(gt.title,!0),ks()}else n.textContent=vi[r&&r.code]||"\uD655\uC778\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.",r&&r.code==="not_registered"&&n.insertAdjacentHTML("beforeend",' <a href="/?utm_source=game&amp;utm_medium=web#join">\uC0AC\uC804 \uB4F1\uB85D\uD558\uAE30</a>')}catch{n.textContent="\uC5F0\uACB0\uC774 \uB04A\uACBC\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."}s.disabled=!1,s.textContent="\uCC38\uAC00\uD558\uAE30"};ht("joinBack").onclick=()=>{ie(gt.join,!1),ie(gt.title,!0)};async function mu(){if(!Kt.email)return po();if(Kt.status&&Kt.status.runs_left<=0)return Zc();let i=ht("dailyBtn");i.disabled=!0;try{let t=await Ei("tower_start",{p_email:Kt.email});if(t&&t.ok)L.daily=!0,L.run=t.run,Kt.status&&(Kt.status.runs_left=t.runs_left),_u();else{if(t&&t.code==="not_registered")return i.disabled=!1,po();t&&t.code==="no_runs"?(Kt.status&&(Kt.status.runs_left=0,Kt.status.sns_left=t.sns_left),ks(),Zc()):Nn(vi[t&&t.code]||"\uC2DC\uC791\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694.",2)}}catch{Nn("\uC5F0\uACB0\uC774 \uB04A\uACA8 \uC624\uB298\uC758 \uB3C4\uC804\uC744 \uC2DC\uC791\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694.",2)}i.disabled=!1}function gu(){L.daily=!1,L.run=null,_u()}function _u(){bu(),ru(),L.mode="intro",L.intro=3.6,[gt.title,gt.over,gt.board,gt.sns,gt.join].forEach(i=>ie(i,!1)),ie(gt.hud,!0),ie(gt.controls,!0),fo(`<small>${L.daily?"\uC624\uB298\uC758 \uB3C4\uC804":"\uC5F0\uC2B5 \xB7 \uC810\uC218 \uC5C6\uC74C"}</small><b>StepUP \uD0C0\uC6CC</b><span>\uC815\uC0C1\uAE4C\uC9C0 \uB2E4\uC12F \uAD6C\uAC04 \xB7 \uBB3C\uC5D0 \uBE60\uC9C0\uBA74 \uBD80\uD65C 1\uBC88</span>`,"",0),pu()}async function Yc(i,t){if(L.mode==="sim"){L.simEnd={finished:i,why:t},L.mode="simdone";return}L.mode="over",ie(gt.controls,!1),gt.banner.hidden=!0,gt.wind.hidden=!0;let e=L.me,n=L.secMs.length,s=i?0:fu(),r=so(n,L.secMs,s,i,L.place,L.revived,L.ko);Qe(i?"win":"out"),i&&hu(e.pos,220);let o=i?L.place===1?"\u{1F3C6} \uC815\uC0C1 1\uB4F1!":"\uC815\uC0C1 \uB3C4\uCC29!":t==="time"?"\uC2DC\uAC04 \uB05D!":"\uD0C8\uB77D\u2026 \u{1FA78}";if(gt.over.innerHTML=`<h2>${o}</h2><p class="big">${r.toLocaleString("ko-KR")}\uC810</p>
    <p class="sub">${i?`${L.place}\uC704\uB85C \uB3C4\uCC29`:`STAGE ${Math.min(n+1,5)}\uAE4C\uC9C0`} \xB7 ${uu(L.playT*1e3)}${L.ko?` \xB7 \uBC00\uC5B4 \uB5A8\uC5B4\uB728\uB9B0 AI ${L.ko}\uBA85`:""}${L.revived?" \xB7 \uBD80\uD65C \uC0AC\uC6A9":""}</p>
    <p class="sub" id="overNote">${L.daily?"\uC810\uC218\uB97C \uAE30\uB85D\uD558\uB294 \uC911\u2026":"\uC5F0\uC2B5 \uD310\uC740 \uC810\uC218\uAC00 \uB0A8\uC9C0 \uC54A\uC544\uC694."}</p>
    <button class="btn" id="againDaily">${L.daily?"\uB2E4\uC2DC \uB3C4\uC804":"\uC624\uB298\uC758 \uB3C4\uC804"}</button>
    <div class="row2"><button class="btn ghost" id="againPractice">\uC5F0\uC2B5 \uD55C \uD310</button><button class="btn ghost" id="toBoard">\uC21C\uC704 \uBCF4\uAE30</button></div>
    <button class="btn ghost" id="toSns">SNS \uC62C\uB9AC\uACE0 \uD55C \uBC88 \uB354</button><button class="link" id="toTitle" type="button">\uCC98\uC74C\uC73C\uB85C</button>`,ie(gt.over,!0),ht("againDaily").onclick=()=>{ie(gt.over,!1),mu()},ht("againPractice").onclick=gu,ht("toBoard").onclick=()=>yu(),ht("toTitle").onclick=h_,ht("toSns").onclick=Zc,!L.daily||!L.run)return;let a=L.run;L.run=null;try{let c=await Ei("tower_end",{p_run:a,p_progress:s,p_revived:L.revived,p_ko:L.ko,p_place:i?L.place:0});ht("overNote").textContent=c&&c.ok?`\uAE30\uB85D\uD588\uC5B4\uC694 \xB7 \uC624\uB298 ${c.rank_today}\uC704 \xB7 \uC5ED\uB300 ${c.rank_all}\uC704${c.score!==r?` (\uC11C\uBC84 \uC810\uC218 ${(+c.score).toLocaleString("ko-KR")}\uC810)`:""}`:"\uC810\uC218\uB97C \uAE30\uB85D\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694."}catch{ht("overNote").textContent="\uC5F0\uACB0\uC774 \uB04A\uACA8 \uB9C8\uC9C0\uB9C9\uC73C\uB85C \uD1B5\uACFC\uD55C \uAD6C\uAC04\uAE4C\uC9C0\uB9CC \uAE30\uB85D\uB410\uC5B4\uC694."}nl()}function h_(){L.mode="title",[gt.over,gt.board,gt.sns,gt.join,gt.hud,gt.controls].forEach(i=>ie(i,!1)),ie(gt.title,!0);for(let i of ao)i.visible=!1;L.me=null,xi.visible=!1,nl()}var vo=`\uB098\uB294 StepUP \uD0C0\uC6CC \uC815\uC0C1\uAE4C\uC9C0 \uAC08 \uC218 \uC788\uC744\uAE4C? \u{1F3C3} \uD608\uC555 \uC8FC\uC758 \u{1FA78}
https://stepupcrew.com/
#StepUP\uCC4C\uB9B0\uC9C0 #\uC2A4\uD15D\uC5C5 #StepUp`;function Zc(){[gt.title,gt.over].forEach(t=>ie(t,!1)),ie(gt.sns,!0),ht("snsErr").textContent="",ht("snsUrl").value="",ht("snsText").textContent=vo;let i=Kt.status;ht("snsLeft").textContent=i?`\uC624\uB298 SNS\uB85C \uBC1B\uC744 \uC218 \uC788\uB294 \uAE30\uD68C ${i.sns_left}\uBC88 \uB0A8\uC74C \xB7 00\uC2DC\uC5D0 \uB2E4\uC2DC 3\uBC88`:"\uBA3C\uC800 \uC0AC\uC804 \uB4F1\uB85D\uD55C \uC774\uBA54\uC77C\uB85C \uCC38\uAC00\uD574 \uC8FC\uC138\uC694."}ht("snsCopy").onclick=()=>{(navigator.clipboard?navigator.clipboard.writeText(vo):Promise.reject(new Error("x"))).then(()=>Nn("\uAE00\uC744 \uBCF5\uC0AC\uD588\uC5B4\uC694. SNS\uC5D0 \uBD99\uC5EC \uB123\uC5B4 \uC62C\uB824 \uC8FC\uC138\uC694.",2),()=>Nn("\uBCF5\uC0AC\uAC00 \uC548 \uB3FC\uC694. \uAE00\uC744 \uAE38\uAC8C \uB20C\uB7EC \uBCF5\uC0AC\uD574 \uC8FC\uC138\uC694.",2))};ht("snsX").href="https://x.com/intent/post?text="+encodeURIComponent(vo);ht("snsThreads").href="https://www.threads.net/intent/post?text="+encodeURIComponent(vo);ht("snsForm").onsubmit=async i=>{i.preventDefault();let t=ht("snsErr"),e=ht("snsSend");if(t.textContent="",!Kt.email)return ie(gt.sns,!1),po();if(!ht("snsConsent").checked){t.textContent=vi.consent;return}e.disabled=!0,e.textContent="\uD655\uC778 \uC911\u2026";try{let n=await Ei("tower_sns_submit",{p_email:Kt.email,p_url:ht("snsUrl").value.trim(),p_account:ht("snsAcct").value.trim(),p_consent:!0,p_trap:""});n&&n.ok?(Kt.status&&(Kt.status.runs_left=n.runs_left,Kt.status.sns_left=n.sns_left),ks(),ie(gt.sns,!1),ie(gt.title,!0),L.mode="title",Nn("\uAE30\uD68C +1! \uBC14\uB85C \uB3C4\uC804\uD574 \uBCF4\uC138\uC694.",2)):t.textContent=vi[n&&n.code]||"\uD655\uC778\uD558\uC9C0 \uBABB\uD588\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."}catch{t.textContent="\uC5F0\uACB0\uC774 \uB04A\uACBC\uC5B4\uC694. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574 \uC8FC\uC138\uC694."}e.disabled=!1,e.textContent="\uB9C1\uD06C \uB0B4\uACE0 \uD55C \uBC88 \uB354"};ht("snsUrl").addEventListener("input",()=>{ht("acctRow").hidden=!/instagram\.com|(x|twitter)\.com\/i\//i.test(ht("snsUrl").value)});ht("snsBack").onclick=()=>{ie(gt.sns,!1),L.mode==="over"?ie(gt.over,!0):ie(gt.title,!0)};var rs="today",xu=null;async function yu(){[gt.over,gt.title,gt.controls].forEach(i=>ie(i,!1)),ie(gt.board,!0),ht("boardList").innerHTML='<li class="muted">\uBD88\uB7EC\uC624\uB294 \uC911\u2026</li>',ht("boardMe").textContent="";try{xu=await Ei("tower_board",{p_email:Kt.email||""}),vu()}catch{ht("boardList").innerHTML='<li class="muted">\uC21C\uC704\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC5B4\uC694.</li>'}}function vu(){let i=xu||{},t=(rs==="today"?i.today:i.all)||[],e=rs==="today"?i.me_today:i.me_all;[].forEach.call(document.querySelectorAll("[data-board]"),n=>n.setAttribute("aria-selected",String(n.dataset.board===rs))),ht("boardList").innerHTML=t.length?t.map(n=>`<li class="${n.me?"me":""}"><b>${n.rank}</b><span>${du(n.name)}<small>${n.cleared>=5?"\uC815\uC0C1":"STAGE "+(n.cleared+1)}</small></span><em>${(+n.score).toLocaleString("ko-KR")}</em></li>`).join(""):`<li class="muted">${rs==="today"?"\uC624\uB298\uC740 \uC544\uC9C1 \uAE30\uB85D\uC774 \uC5C6\uC5B4\uC694. \uCCAB \uBC88\uC9F8 \uC8FC\uC778\uACF5\uC774 \uB418\uC5B4 \uBCF4\uC138\uC694.":"\uC544\uC9C1 \uAE30\uB85D\uC774 \uC5C6\uC5B4\uC694."}</li>`,ht("boardMe").textContent=e?`\uB0B4 ${rs==="today"?"\uC624\uB298":"\uC5ED\uB300"} \uCD5C\uACE0 ${(+e.score).toLocaleString("ko-KR")}\uC810 \xB7 ${e.rank}\uC704`:i.players?`\uCC38\uAC00 ${i.players}\uBA85`:""}[].forEach.call(document.querySelectorAll("[data-board]"),i=>{i.onclick=()=>{rs=i.dataset.board,vu()}});ht("boardClose").onclick=()=>{ie(gt.board,!1),L.mode==="over"?ie(gt.over,!0):ie(gt.title,!0)};ht("dailyBtn").onclick=mu;ht("practiceBtn").onclick=gu;ht("boardBtn").onclick=()=>yu();var Un={keys:new Set,stickX:0,stickZ:0},$c={ArrowUp:[0,-1],KeyW:[0,-1],ArrowDown:[0,1],KeyS:[0,1],ArrowLeft:[-1,0],KeyA:[-1,0],ArrowRight:[1,0],KeyD:[1,0]};addEventListener("keydown",i=>{i.target&&/INPUT|TEXTAREA/.test(i.target.tagName)||($c[i.code]&&(Un.keys.add(i.code),i.preventDefault()),i.code==="Space"&&(!i.repeat&&L.me&&(L.me.jumpBuf=os),i.preventDefault()),(i.code==="ShiftLeft"||i.code==="ShiftRight"||i.code==="KeyJ")&&L.mode==="play"&&L.me&&_o(L.me))});addEventListener("keyup",i=>Un.keys.delete(i.code));var as=0;function Jh(){let i=L.me;if(!i)return;let t=0,e=0;for(let r of Un.keys)t+=$c[r][0],e+=$c[r][1];t=Math.max(-1,Math.min(1,t+Un.stickX)),e=Math.max(-1,Math.min(1,e+Un.stickZ));let n=[-Math.sin(as),-Math.cos(as)],s=[-n[1],n[0]];i.ix=n[0]*-e+s[0]*t,i.iz=n[1]*-e+s[1]*t}var ho=ht("stick"),Mu=ht("knob"),mo=ht("jumpBtn"),go=ht("shoveBtn"),Hs=null,Jc=0,Kc=0,ss=56;gt.controls.addEventListener("pointerdown",i=>{i.target===mo||i.target===go||Hs!==null||i.clientX>innerWidth*.6||(Hs=i.pointerId,Jc=i.clientX,Kc=i.clientY,ho.style.left=Jc+"px",ho.style.top=Kc+"px",ho.classList.add("on"),gt.controls.setPointerCapture(i.pointerId),i.preventDefault())});gt.controls.addEventListener("pointermove",i=>{if(i.pointerId!==Hs)return;let t=i.clientX-Jc,e=i.clientY-Kc,n=Math.hypot(t,e);n>ss&&(t=t/n*ss,e=e/n*ss),Mu.style.transform=`translate(${t}px, ${e}px)`;let s=Math.min(1,n/ss),r=s<.12?0:(s-.12)/.88/Math.max(s,.001);Un.stickX=t/ss*r,Un.stickZ=e/ss*r});function Su(i){i.pointerId===Hs&&(Hs=null,Un.stickX=Un.stickZ=0,Mu.style.transform="",ho.classList.remove("on"))}gt.controls.addEventListener("pointerup",Su);gt.controls.addEventListener("pointercancel",Su);mo.addEventListener("pointerdown",i=>{L.me&&(L.me.jumpBuf=os),mo.classList.add("on"),i.preventDefault(),i.stopPropagation()});go.addEventListener("pointerdown",i=>{L.mode==="play"&&L.me&&_o(L.me),go.classList.add("on"),i.preventDefault(),i.stopPropagation()});[mo,go].forEach(i=>["pointerup","pointercancel","pointerleave"].forEach(t=>i.addEventListener(t,()=>i.classList.remove("on"))));document.addEventListener("visibilitychange",()=>{L.paused=document.hidden,Qc=performance.now()});var ln=null,Si=!1;try{Si=localStorage.getItem("race_mute")==="1"}catch{}var jc=ht("muteBtn");function Eu(){jc.textContent=Si?"\u{1F507}":"\u{1F50A}",jc.setAttribute("aria-label",Si?"\uC18C\uB9AC \uCF1C\uAE30":"\uC18C\uB9AC \uB044\uAE30")}Eu();jc.onclick=()=>{Si=!Si,Eu();try{localStorage.setItem("race_mute",Si?"1":"0")}catch{}bu()};function bu(){if(!ln)try{ln=new(window.AudioContext||window.webkitAudioContext)}catch{ln=null}ln&&ln.state==="suspended"&&ln.resume()}function Me(i,t,e,n="sine",s=.12,r=0){if(!ln||Si||L.mode==="sim")return;let o=ln.currentTime+r,a=ln.createOscillator(),c=ln.createGain();a.type=n,a.frequency.setValueAtTime(i,o),a.frequency.exponentialRampToValueAtTime(Math.max(30,t),o+e),c.gain.setValueAtTime(s,o),c.gain.exponentialRampToValueAtTime(.001,o+e),a.connect(c).connect(ln.destination),a.start(o),a.stop(o+e+.02)}function Qe(i){i==="jump"?Me(420,760,.12,"square",.05):i==="land"?Me(160,90,.06,"sine",.06):i==="hit"?Me(220,60,.22,"sawtooth",.09):i==="dive"?Me(600,300,.18,"triangle",.08):i==="shove"?(Me(180,90,.12,"square",.1),Me(500,700,.08,"triangle",.06,.05)):i==="whiff"?Me(300,220,.08,"sine",.05):i==="boing"?Me(300,1100,.25,"triangle",.1):i==="fall"?(Me(700,90,.7,"triangle",.08),Me(220,110,.5,"sawtooth",.04,.3)):i==="splash"?(Me(900,60,.6,"sawtooth",.06),Me(200,40,.5,"square",.05,.1)):i==="beep"?Me(520,520,.12,"square",.06):i==="go"?Me(880,880,.3,"square",.07):i==="win"?(Me(523,523,.12,"triangle",.1),Me(659,659,.12,"triangle",.1,.12),Me(784,784,.25,"triangle",.1,.24)):i==="out"&&(Me(392,330,.25,"triangle",.1),Me(330,220,.4,"triangle",.1,.25))}function wu(){let i=innerWidth,t=innerHeight;xo.setSize(i,t,!1),Mi.aspect=i/t,Mi.fov=i/t<.8?72:60,Mi.updateProjectionMatrix()}addEventListener("resize",wu);wu();var Hc=new P(0,30,95),Vc=new P(0,14,0);function u_(i){let t=Mi.aspect<.8,e=L.me;if(!e||e.out){let n=L.t*.08,s=new P(Math.cos(n)*95,32,Math.sin(n)*95);Hc.lerp(s,1-Math.exp(-i*1.2)),Vc.lerp(new P(0,16,0),1-Math.exp(-i*1.2))}else{let n=Math.min(e.s,L.world.endS),s=n/ue,r=[-Math.sin(s),Math.cos(s)],o=[Math.cos(s),Math.sin(s)],c=Math.atan2(-r[0],-r[1])-as;c=Math.atan2(Math.sin(c),Math.cos(c)),as+=c*Math.min(1,i*5);let u=t?10.5:9,f=t?6.6:5.4,p=[-Math.sin(as),-Math.cos(as)],h=e.pos,d=e.deadT>0?je(n):Math.max(h.y,je(n)-8),g=new P(h.x-p[0]*u+o[0]*1.5,d+f,h.z-p[1]*u+o[1]*1.5),_=1-Math.exp(-i*7);Hc.lerp(g,_),Vc.lerp(new P(h.x+p[0]*6,d+1,h.z+p[1]*6),_)}Mi.position.copy(Hc),Mi.lookAt(Vc)}function Gc(i){for(let n of L.bodies){if(n.out)continue;let s=n.mesh,r=Math.hypot(n.vel.x,n.vel.z);s.position.copy(n.pos),r>.5&&!n.diving&&n.stun<=0&&n.shoveT<=0&&(n.yaw=Math.atan2(-n.vel.x,-n.vel.z));let o=n.yaw-s.rotation.y;if(o=Math.atan2(Math.sin(o),Math.cos(o)),s.rotation.y+=o*Math.min(1,i*14),s.rotation.z=n.stun>0?Math.sin(n.stun*30)*.3:0,n.deadT<=0&&(s.visible=!(n.safe>.4&&Math.floor(n.safe*12)%2)),Q0(s,r,!n.onGround,i,n.diving?"dive":n.shoveT>0?"shove":""),n.tag){let a=L.me?Math.hypot(n.pos.x-L.me.pos.x,n.pos.z-L.me.pos.z):99;n.tag.visible=a>2.5&&a<11&&Math.abs(n.pos.y-L.me.pos.y)<4}}let t=L.me;if(!t)return;let e=zs(t.pos.x,t.pos.z,t.pos.y);if(xi.visible=e>-1/0&&t.deadT<=0&&!t.out,xi.visible){xi.position.set(t.pos.x,e+.03,t.pos.z);let n=Math.max(.35,1-(t.pos.y-e)*.12);xi.scale.set(n,n,n)}}function Kh(i){let t=L.world;L.t+=i;for(let e of t.solids)e.save(),e.update(L.t,i);for(let e of t.hazards)e.update(L.t);t.balls&&t.balls.update(L.t,i,t,0)}var Qc=performance.now();function Tu(i){requestAnimationFrame(Tu);let t=Math.min(.05,(i-Qc)/1e3);if(Qc=i,!(L.paused||L.mode==="sim"||L.mode==="simdone")){if(co>0&&(co-=t,co<=0&&(gt.toast.hidden=!0)),lo>0&&(lo-=t,lo<=0&&(gt.banner.hidden=!0)),L.mode==="intro"){let e=Math.ceil(L.intro-.6);L.intro-=t;let n=Math.ceil(L.intro-.6);L.intro<3&&n>0&&(fo(`<b class="num">${n}</b>`,"count",0),e!==n&&Qe("beep")),Kh(t),Jh(),Gc(t),L.intro<=.6&&(L.mode="play",fo('<b class="num">GO!</b>',"count",.7),Qe("go"),L.t=0)}else if(L.mode==="play"){for(Jh(),L.acc+=t;L.acc>=Ns&&L.mode==="play";)L.acc-=Ns,L.playT+=Ns,ou(Ns);Gc(t),L.mode==="play"&&pu()}else Kh(t),L.me&&Gc(t);u_(t),K0(L.world,i),c_(t),xo.render(hn,Mi)}}nu();L.mode="title";nl();requestAnimationFrame(Tu);jh&&(window.__tower={S:L,P:Oe,RT:ue,HW:Ne,trackY:je,simStep:ou,setupRace:ru,stepBody:iu,groundTop:zs,DT:Ns,SECTIONS:Dn,towerScore:so,shove:_o,aiThink:su,PERSONAS:eu,mulberry:ls,relS:Os});
/*! Bundled license information:

three/build/three.module.js:
  (**
   * @license
   * Copyright 2010-2024 Three.js Authors
   * SPDX-License-Identifier: MIT
   *)
*/
