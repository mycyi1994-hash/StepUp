/* 11.11 워킹데이 방향 B: 화면 전체가 막대 표면. 걸음 합계만큼 위에서 초콜릿이 내려온다.
   데이터는 한 곳(readTotal)에서만 읽는다. WALKINGDAY.url이 비면 서버 호출 없음. */
(function () {
  'use strict';
  var CFG = window.WALKINGDAY || {};
  var PREVIEW = location.hash === '#preview' || CFG.preview === true;
  var T1 = 1111111, T2 = 2222222, EXAMPLE = 642180;
  var START = Date.UTC(2026, 10, 6, 15, 0), CLOSE = Date.UTC(2026, 10, 12, 15, 0); // 11/7 00:00 KST, 11/12 23:59 KST 마감
  var REDUCED = matchMedia('(prefers-reduced-motion: reduce)').matches;
  var A = 'assets/';
  var $ = function (id) { return document.getElementById(id); };
  var fmt = function (n) { return Math.round(n).toLocaleString('en-US'); };
  var clamp = function (v, a, b) { return v < a ? a : v > b ? b : v; };
  var sstep = function (a, b, x) { var t = clamp((x - a) / (b - a), 0, 1); return t * t * (3 - 2 * t); };

  /* ── 글자: 경계로 자르지 않는다. 숫자 칸은 통째로 비스킷 위(진한 갈색) 또는 초콜릿 위(크림색) ── */
  var hero = $('hero'), head = $('head');
  function rr(k) { return [hero.querySelector('[data-r="' + k + '"]')]; }
  var elNum = rr('num'), elSub = rr('sub'), elLb = rr('lb'), elTag = rr('tag'), elCount = rr('count'), bub = $('bub');
  function both(arr, fn) { fn(arr[0]); }
  function setText(arr, s) { if (arr[0].textContent !== s) arr[0].textContent = s; }

  /* ── 상태 ── */
  var total = 0, cur = 0, vel = 0, pour = 0, phaseOverride = null, t0 = performance.now(), om = 3.0, tNow = 0;
  var rev = [0, 0, 0];
  var RESULT = Date.UTC(2026, 10, 15, 15, 0), BEFORE_FILL = T1 * .5;
  function phase() { if (phaseOverride) return phaseOverride; var n = Date.now(); return n < START ? 'before' : n < CLOSE ? 'live' : n < RESULT ? 'count' : 'result'; }

  /* ── 장면 치수 ── */
  var S = { W: 390, H: 844, U: 390, bead: 26, bow: 12, wk: .7, bowE: 12, wkE: .7, wide: false, lim: 1e5, yA: 1e5, covY: 1e5, calmY: 1e5, topY: -1e4, dripCap: 1e5, cH: 0 };
  var drips = [], cellsR = [[0, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0]], cells = [[0, 0, 0, 0], [0, 0, 0, 0], [0, 0, 0, 0]], cellsReady = false, relRect = [0, 0, 1, 1];
  /* 합계 → 초콜릿 위치. 숫자 칸 위에서 멈췄다가 222만에 칸 전체를 한 번에 덮는다. 글자 칸 기준으로 layout에서 채움 */
  var FK = [[0, 160], [T2, 860]], revAt = [T1 * .27, T1 * .54, T1], fy = 160, covOn = false;
  function frontBase(s) { for (var i = 1; i < FK.length; i++) if (s < FK[i][0]) { var a = FK[i - 1], b = FK[i]; return a[1] + (b[1] - a[1]) * Math.max(0, s - a[0]) / (b[0] - a[0]); } return FK[FK.length - 1][1]; }
  function wob(x, t, U) { U = U || S.U; return U * (.026 * Math.sin(x / (U * .47) + 1.3 + t * .05) + .012 * Math.sin(x / (U * .19) + .4 - t * .037) + .006 * Math.sin(x / (U * .083) + 2.1 + t * .03)); }
  function sheetY(x, t, base) { var u = (x - S.W / 2) / (S.W / 2); return (base === undefined ? fy : base) + S.bowE * (1 - u * u) + S.wkE * wob(x, t); }
  var D8 = [[.045, .07, .046], [.165, .24, .040], [.30, .05, .052], [.395, .13, .044], [.545, .37, .036], [.655, .055, .05], [.785, .19, .040], [.93, .09, .044]];
  var D14 = [[.03, .10, .042], [.085, .17, .034], [.16, .05, .046], [.215, .22, .030], [.29, .07, .044], [.365, .13, .036], [.43, .045, .048], [.50, .24, .032], [.575, .07, .044], [.63, .42, .030], [.71, .06, .046], [.79, .30, .032], [.86, .10, .042], [.95, .19, .036]];
  function buildDrips() {
    var tb = S.W / S.U < 1.3 ? D8 : D14; drips = [];
    for (var i = 0; i < tb.length; i++) drips.push({ x: tb[i][0] * S.W, len: tb[i][1] * S.U * (S.wide ? .62 : 1), r: tb[i][2] * S.U * (S.wide ? 1.08 : 1), ph: i * 1.9, y0: 0, y1: 0 });
  }
  function updateDrips(t) {
    var lim = S.lim + Math.max(0, fy - S.yA) * 3, pw = clamp((fy - FK[0][1]) / Math.max(1, FK[1][1] - FK[0][1]), 0, 1), lk = .22 + .78 * pw;
    S.bowE = S.bow * (.35 + .65 * pw); S.wkE = S.wk * (.4 + .6 * pw);
    for (var i = 0; i < drips.length; i++) {
      var d = drips[i], b = sheetY(d.x, t), L = Math.min(d.len * lk * (1 + .85 * pour), S.dripCap + Math.max(0, fy - S.yA) * 3) + S.U * .006 * Math.sin(t * .23 + d.ph), room = lim - b, re = Math.min(d.r, Math.max(.6, room * .55));
      if (cellsReady) for (var q = 0; q < 3; q++) { var c = cells[q]; if (d.x > c[0] - d.r && d.x < c[2] + d.r) { var r0 = rev[q], up = Math.max(S.U * .02, c[1] - 6 - re * 1.12 - b); L = L * r0 + Math.min(L, up) * (1 - r0); re *= .7 + .3 * r0; } }
      d.re = re; d.y1 = b + Math.min(L, Math.max(0, room - re * 1.12)); d.y0 = b - S.bead * 1.15;
    }
  }
  function frontY(x, t) {
    var y = sheetY(x, t), sk = 0, i, d, dx;
    for (i = 0; i < drips.length; i++) { d = drips[i]; dx = (x - d.x) / (d.re * 2.2); if (dx > -3 && dx < 3) { var L0 = d.y1 - d.y0 - S.bead * 1.15; sk += Math.min(L0 * .85, d.re * 1.75) * Math.exp(-dx * dx); } }
    y += sk;
    for (i = 0; i < drips.length; i++) { d = drips[i]; dx = x - d.x; var rr0 = d.re * 1.12; if (dx > -rr0 && dx < rr0) { var yd = d.y1 + Math.sqrt(rr0 * rr0 - dx * dx); if (yd > y) y = yd; } }
    return y;
  }

  /* ── 초콜릿·비스킷 셰이더 ── */
  var VS = 'attribute vec2 a;uniform vec2 uR;varying vec2 vP;void main(){vP=vec2(a.x*.5+.5,.5-a.y*.5)*uR;gl_Position=vec4(a,0.,1.);}';
  var FS = [
    '#ifdef GL_FRAGMENT_PRECISION_HIGH', 'precision highp float;', '#else', 'precision mediump float;', '#endif',
    'varying vec2 vP;uniform vec2 uR;uniform float uWave,uT,uU,uF,uBow,uBead,uTop,uPx,uPlate,uDr,uExpo,uBisK,uBU,uTopOp,uWK,uCalmY,uCalmK,uChK,uTopY,uGhost;uniform vec2 uOff;uniform vec4 uD[14];uniform vec4 uRelR,uC0,uC1,uC2;uniform float uSa;uniform vec3 uRev,uAlbD,uAlbW,uFlat,uSc;uniform sampler2D uB,uRel;',
    'float wob(float x){float U=uU;return U*(.026*sin(x/(U*.47)+1.3+uT*.05)+.012*sin(x/(U*.19)+.4-uT*.037)+.006*sin(x/(U*.083)+2.1+uT*.03));}',
    'float wobD(float x){float U=uU;return .0553*cos(x/(U*.47)+1.3+uT*.05)+.0632*cos(x/(U*.19)+.4-uT*.037)+.0723*cos(x/(U*.083)+2.1+uT*.03);}',
    'float inR(vec2 p,vec4 c){return step(c.x,p.x)*step(p.x,c.z)*step(c.y,p.y)*step(p.y,c.w);}',
    'float gs(float x,float c,float w){x=(x-c)/w;return exp(-x*x);}',
    /* 녹은 표면 높이: 흐름 방향(세로)으로 늘어진 넓은 물결 + 드문 주름 + 턱 뒤 둔덕 + 턱과 나란한 잔물결 */
    'float surf(vec2 p){vec2 q=p/uU;vec2 a=vec2(q.x*1.9,q.y*1.35-uT*.012);a+=.42*vec2(sin(a.y*1.7+.8+.6*sin(a.x*.9)),sin(a.x*1.3-.6+uT*.015));',
    'float w1=sin(a.x*1.25+.8*sin(a.y*1.1));float w2=sin(a.x*2.6+1.4*sin(a.y*.8+w1)+1.7);float w3=sin(a.x*4.7+a.y*1.1+2.1*w1);return .50*w1+.17*w2+.05*sqrt(w3*w3+.02);}',
    'void main(){vec2 p=vP;float hw=.5*uR.x,u=(p.x-hw)/hw,Rb=uBead,c=7.;',
    'float sk=0.,skx=0.;for(int j=0;j<14;j++){vec4 D=uD[j];if(D.w<=0.)continue;float L0=D.z-D.y-Rb*1.15;float A0=min(L0*.85,D.w*1.75);float W0=D.w*2.2;float xx=(p.x-D.x)/W0;if(abs(xx)<3.){float g0=exp(-xx*xx);sk+=A0*g0;skx+=A0*g0*(-2.*xx/W0);}}',
    'float f=uF+uBow*(1.-u*u)+wob(p.x)*uWK+sk;float fx=-2.*uBow*u/hw+wobD(p.x)*uWK+skx;float nr=sqrt(1.+fx*fx);float d=(f-p.y)/nr;vec2 gd=vec2(fx,-1.)/nr;',
    'float Fs,dFs;if(d>=Rb){Fs=1.;dFs=0.;}else if(d>0.){float k=1.-d/Rb;Fs=1.-k*k;dFs=2.*k/Rb;}else{Fs=max(2.*d/Rb,-3.);dFs=2./Rb;}',
    'float e0=exp(c*Fs);float E=e0+1e-9;vec2 G0=e0*dFs*gd;vec2 G=G0*(Rb*.92);',
    'vec2 so=vec2(uU*.010,uU*.034);float ds=d+so.y;float Fz;if(ds>=Rb){Fz=1.;}else if(ds>0.){float k2=1.-ds/Rb;Fz=1.-k2*k2;}else{Fz=max(2.*ds/Rb,-3.);}float E2=exp(c*Fz)+1e-9;',
    'for(int i=0;i<14;i++){vec4 D=uD[i];if(D.w<=0.)continue;float ky=clamp((p.y-D.y)/(D.z-D.y),0.,1.);float t0=clamp(ky/.34,0.,1.),t3=clamp((ky-.60)/.40,0.,1.);float r=D.w*(1.28-.46*t0*t0*(3.-2.*t0)+.30*t3*t3*(3.-2.*t3));float rp=D.w*(.30*6.*t3*(1.-t3)/.40-.46*6.*t0*(1.-t0)/.34)/(D.z-D.y);vec2 pa=vec2(p.x-D.x,p.y-mix(D.y,D.z,ky));float ir=1./(r*r);float q0=dot(pa,pa)*ir;if(q0<3.6){float w=exp(c*(1.-q0))*(1.-smoothstep(Rb*.1,Rb*1.3,d));E+=w;vec2 gg=w*(-2.*pa*ir);gg.y+=w*2.*q0/r*rp;G0+=gg;G+=gg*r;}vec2 ps=pa-so;float q2=dot(ps,ps)*ir;if(q2<5.){E2+=exp(c*(1.-q2));}}',
    'float S=log(E)/c;vec2 gS=G0/E;vec2 gA=G/E;float S2=log(E2)/c;',
    'if(uTop>-9000.){float ft=uTop+wob(p.x*1.31+77.)*1.1;float dt=p.y-ft;float Ft,dF;if(dt>=Rb){Ft=1.;dF=0.;}else if(dt>0.){float k3=1.-dt/Rb;Ft=1.-k3*k3;dF=2.*k3/Rb;}else{Ft=max(2.*dt/Rb,-3.);dF=2./Rb;}float a0=exp(-c*S),b0=exp(-c*Ft);float ab=a0+b0;S=-log(ab)/c;vec2 gt=dF*vec2(-wobD(p.x*1.31+77.)*1.44,1.);gS=(a0*gS+b0*gt)/ab;gA=(a0*gA+b0*gt*(Rb*.92))/ab;S2=min(S2,Ft-1.6);}',
    /* 비스킷 */
    'vec2 pb=p+uOff;vec2 tb=pb*uBisK;vec2 tb2=vec2(.8*tb.x-.6*tb.y,.6*tb.x+.8*tb.y)*.83+vec2(.37,.11);vec3 t1=texture2D(uB,tb).rgb,t2=texture2D(uB,tb2).rgb;',
    'float lf=.5+.5*sin(pb.x/(uBU*.53)+1.3*sin(pb.y/(uBU*.71)))*sin(pb.y/(uBU*.47)+.7+.6*sin(pb.x/(uBU*.9)));vec3 bis=mix(t1,t2,smoothstep(.40,.60,lf));',
    'bis*=(1.-.20*u*u)*(1.03-.10*u)*(1.-.06*sin(pb.y/(uBU*.37)+2.*sin(pb.x/(uBU*.6))));',
    /* 글자 자리 비스킷은 구멍 무늬를 눌러 잔잔하게 */
    'float cz=smoothstep(uCalmY-uU*.12,uCalmY,p.y)*uCalmK;vec3 bisC=vec3(.93,.80,.60)*(1.+.10*(dot(t1,vec3(.33))-.62));bis=mix(bis,bisC,cz);',
    'float sh=smoothstep(-2.5,.25,S2)*(1.-.6*cz*(1.-uPlate));float cont=smoothstep(-.7,.0,S);vec3 bisS=bis*mix(vec3(1.),vec3(.36,.22,.13),sh*.86);bisS*=1.-.42*cont;',
    /* 양각 */
    'vec2 ruv=(p-uRelR.xy)/(uRelR.zw-uRelR.xy);vec4 rl=texture2D(uRel,ruv);float rv0=uRev.x*inR(p,uC0)+uRev.y*inR(p,uC1)+uRev.z*inR(p,uC2);float rv=rv0*smoothstep(.72,1.,S);',
    'vec2 sR=(rl.rg-.50196)*2.5*rv;float rH=rl.b*1.28-.28;float rTop=clamp(rH,0.,1.)*rv,rTr=clamp(-rH/.28,0.,1.)*rv;',
    /* 빼빼로 글자는 어느 상태에서도 읽힌다: 비스킷 위 진한 갈색, 초콜릿 위 크림색 절반, 칸을 다 덮으면 크림색 */
    'float cAny=max(max(inR(p,uC0),inR(p,uC1)),inR(p,uC2));float gh=smoothstep(.02,.28,rH)*cAny*uGhost;bisS=mix(bisS,vec3(.26,.135,.06),gh*.9);',
    /* 표면 기울기 */
    'float e=1.5;float h0=surf(p),hx=surf(p+vec2(e,0.)),hy=surf(p+vec2(0.,e));vec2 sW=vec2(hx-h0,hy-h0)/e*uU*.1;sW*=uWave;float dd=max(d,0.);float crE=exp(-dd/(Rb*1.7));float riE=exp(-dd/(Rb*5.));float riP=dd/(Rb*1.2)+1.4*sin(p.x/(uU*.33));sW+=gd*uU*.1*(-.30/(Rb*1.7)*crE+.12*riE*(cos(riP)/(Rb*1.2)-sin(riP)/(Rb*5.)))*smoothstep(0.,Rb*.55,d);float wm=smoothstep(.0,.9,S)*(1.-.72*rTop);',
    'float Sc=clamp(S,.018,1.3);float Hh=sqrt(Sc);vec2 sL=gA*.5/Hh;vec2 sC=vec2(-u*.27,0.);vec2 mic=vec2(t1.r-t2.g,t2.r-t1.g)*.012;',
    'vec2 s=sC+sW*wm+sL+sR+mic;vec3 n=normalize(vec3(-s,1.));vec3 V=normalize(vec3((.5*uR-p)/(uR.y*1.7),1.));vec3 Rf=reflect(-V,n);vec2 q=Rf.xy/(abs(Rf.z)+.45);',
    /* 스튜디오 조명: 큰 소프트박스, 오른쪽 띠, 위쪽 띠, 턱을 잡는 낮은 띠, 작은 점광. 천천히 흐른다 */
    'float rg=max(1.-smoothstep(0.,Rb*1.4,d),smoothstep(.03,.33,length(sR)));',
    'float cx=-.31+.03*sin(uDr*.7);float vw=1.-smoothstep(.75,1.3,abs(q.y+.05+.08*sin(uDr*.31)));',
    /* 소프트박스 반사: 한쪽은 또렷한 경계, 반대쪽으로 번진다 */
    'float cxx=mix(cx,-.62,rg);float ed=mix(.016,.13,rg);float ql=q.x-(cxx-.10);float k1=smoothstep(-ed,ed,ql)*exp(-pow(max(ql,0.)/mix(.095,.22,rg),mix(1.15,1.6,rg)))*vw;float k1c=gs(q.x,cxx-.07,.035)*vw*(1.-.55*rg);',
    'float qr=(mix(.41,.88,rg)-.02*sin(uDr*.5))-q.x;float k2=smoothstep(-ed,ed,qr)*exp(-pow(max(qr,0.)/mix(.045,.17,rg),mix(1.15,1.6,rg)))*(1.-smoothstep(.7,1.2,abs(q.y)));',
    'float qt=q.y+.72;float k3=smoothstep(-ed,ed,qt)*exp(-pow(max(qt,0.)/mix(.07,.26,rg),mix(1.15,1.6,rg)))*(1.-smoothstep(.6,1.4,abs(q.x+.04-.10*sin(uDr*.23))));',
    'float prox=1.-smoothstep(0.,Rb*2.2,d);',
    'float k4=gs(length(q-vec2(-.42+.02*sin(uDr*.4),-.48)),0.,mix(.05,.13,rg));',
    'float zf=smoothstep(-.55,.15,Rf.z);float k6=gs(q.y,.17,.09)*zf*prox*smoothstep(0.,Rb*.5,d);',
    'float kw=uPlate>.5?.6:(uR.x/uR.y>1.1?.8:1.);vec3 env=(vec3(1.,.68,.46)*(k1*mix(7.5*kw,8.8,rg)+k2*mix(3.2,3.8,rg)+k3*mix(3.6,6.,rg))+vec3(1.,.90,.80)*(k1c*mix(2.6,3.2,rg)+k4*mix(16.,10.,rg)))*zf+vec3(1.,.80,.62)*k6*1.5;',
    'env+=(vec3(.80,.34,.14)*gs(q.x,cx,.34)*vw*.8+vec3(.36,.15,.07)*smoothstep(1.,-1.,q.y)*.42)*zf;',
    'env+=vec3(1.,.46,.14)*pow(smoothstep(.45,2.2,q.y),1.2)*1.5*prox;if(uPlate>.5)env*=.22+.78*prox;',
    /* 재질 */
    'float ndv=max(dot(n,V),0.);float F=min(.045+.955*pow(1.-ndv,5.),.6);vec3 Ld=normalize(vec3(-.55,-.62,.56));float dif=max(dot(n,Ld),0.);float wr=dot(n,Ld)*.5+.5;',
    'vec3 alb=mix(uAlbW,uAlbD,smoothstep(.05,.36,Hh));',
    'vec3 ch=alb*(.34+1.05*wr*wr+.42*dif);',
    'ch=ch*(1.-F)+env*F;ch*=1.+.42*rTop;ch*=1.-.62*rTr;ch*=(1.-.12*u*u);',
    'ch=1.-exp(-ch*uExpo);ch=pow(ch,vec3(.4545));float lm=dot(ch,vec3(.3,.59,.11));ch=mix(vec3(lm),ch,1.16);ch=pow(clamp(ch,0.,1.),vec3(.97,1.09,1.24));',
    /* 글자가 놓이는 띠는 번들거림을 눌러 크림색 글자 대비 확보 */
    'float tz=1.-smoothstep(uTopY,uTopY+uU*.10,p.y);float cc=max(cz,tz*.6)*uChK;ch=mix(ch,uFlat+ch*.30,cc);',
    'ch=mix(ch,vec3(.973,.895,.76)*(.84+.26*dif),gh*mix(.58,.96,rv0));',
    /* 합치기: 가장자리는 비스킷 결을 따라 미세하게 들쭉날쭉 */
    'float jit=(dot(t1,vec3(.33))-.62)*.16;float aa=max(length(gS)*uPx,.004);float cov=smoothstep(-aa,aa,S-jit);',
    'float gr=fract(sin(dot(gl_FragCoord.xy,vec2(12.9898,78.233)))*43758.5453)-.5;',
    'if(uPlate>.5){if(uTopOp>.5&&p.y<uR.y*.5){vec3 o=mix(bis*(1.-.34*cont),ch,cov)+gr*.012;gl_FragColor=vec4(o,1.);}else{float sa=smoothstep(-1.15,.1,S2)*uSa*(1.-cov);vec3 sc=uSc;vec3 o=(ch+gr*.012)*cov+sc*sa;gl_FragColor=vec4(o,cov+sa);}}',
    'else{vec3 o=mix(bisS,ch,cov)+gr*.012;gl_FragColor=vec4(o,1.);}}'
  ].join('\n');
  var UN = ['uSa', 'uSc', 'uAlbD', 'uAlbW', 'uFlat', 'uWK', 'uCalmY', 'uCalmK', 'uChK', 'uTopY', 'uGhost', 'uWave', 'uOff', 'uBU', 'uTopOp', 'uR', 'uT', 'uU', 'uF', 'uBow', 'uBead', 'uTop', 'uPx', 'uPlate', 'uDr', 'uExpo', 'uBisK', 'uD', 'uRelR', 'uC0', 'uC1', 'uC2', 'uRev', 'uB', 'uRel'];
  function mkGL(canvas, attr) {
    var gl = canvas.getContext('webgl', attr) || canvas.getContext('experimental-webgl', attr); if (!gl) return null;
    function sh(type, src) { var s = gl.createShader(type); gl.shaderSource(s, src); gl.compileShader(s); if (!gl.getShaderParameter(s, gl.COMPILE_STATUS)) { console.warn(gl.getShaderInfoLog(s)); return null; } return s; }
    var v = sh(gl.VERTEX_SHADER, VS), f = sh(gl.FRAGMENT_SHADER, FS); if (!v || !f) return null;
    var pr = gl.createProgram(); gl.attachShader(pr, v); gl.attachShader(pr, f); gl.linkProgram(pr); if (!gl.getProgramParameter(pr, gl.LINK_STATUS)) { console.warn(gl.getProgramInfoLog(pr)); return null; }
    gl.useProgram(pr);
    var b = gl.createBuffer(); gl.bindBuffer(gl.ARRAY_BUFFER, b); gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, 1, 1]), gl.STATIC_DRAW);
    var al = gl.getAttribLocation(pr, 'a'); gl.enableVertexAttribArray(al); gl.vertexAttribPointer(al, 2, gl.FLOAT, false, 0, 0);
    var u = {}; UN.forEach(function (n) { u[n] = gl.getUniformLocation(pr, n); });
    gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
    var tb = gl.createTexture(), tr = gl.createTexture();
    gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, tb); gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 1, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array([214, 168, 98, 255])); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR);
    gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, tr); gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, 1, 1, 0, gl.RGBA, gl.UNSIGNED_BYTE, new Uint8Array([128, 128, 56, 255])); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.uniform1i(u.uB, 0); gl.uniform1i(u.uRel, 1);
    return { gl: gl, u: u, tb: tb, tr: tr, canvas: canvas };
  }
  function setTile(G, img) {
    var gl = G.gl; gl.activeTexture(gl.TEXTURE0); gl.bindTexture(gl.TEXTURE_2D, G.tb); gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGB, gl.RGB, gl.UNSIGNED_BYTE, img); gl.generateMipmap(gl.TEXTURE_2D);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR_MIPMAP_LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.REPEAT); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.REPEAT);
  }

  /* ── 양각: 글자를 높이로 바꿔 기울기를 텍스처에 담는다 ── */
  function gauss(Src, w, h, sig) {
    var r = Math.max(1, Math.round(sig)), T = new Float32Array(Src), O = new Float32Array(Src.length), iv = 1 / (2 * r + 1), x, y, i, s;
    for (var p = 0; p < 3; p++) {
      for (y = 0; y < h; y++) { var o = y * w; s = T[o] * (r + 1); for (i = 1; i <= r; i++) s += T[o + Math.min(i, w - 1)]; for (x = 0; x < w; x++) { O[o + x] = s * iv; s += T[o + Math.min(x + r + 1, w - 1)] - T[o + Math.max(x - r, 0)]; } }
      for (x = 0; x < w; x++) { s = O[x] * (r + 1); for (i = 1; i <= r; i++) s += O[x + Math.min(i, h - 1) * w]; for (y = 0; y < h; y++) { T[x + y * w] = s * iv; s += O[x + Math.min(y + r + 1, h - 1) * w] - O[x + Math.max(y - r, 0) * w]; } }
    }
    return T;
  }
  function reliefH(w, h, draw, rs, bevel, trench) {
    var c = document.createElement('canvas'); c.width = w; c.height = h;
    var x = c.getContext('2d', { willReadFrequently: true }); draw(x);
    var d; try { d = x.getImageData(0, 0, w, h).data; } catch (e) { return null; }
    var n = w * h, Av = new Float32Array(n), i; for (i = 0; i < n; i++) Av[i] = d[i * 4 + 3] / 255;
    var B1 = gauss(Av, w, h, bevel * rs), B2 = gauss(Av, w, h, bevel * rs * 3.0), H = new Float32Array(n);
    for (i = 0; i < n; i++) { var b = B1[i]; H[i] = -trench * sstep(.22, .5, b) + (1 + trench) * sstep(.5, .94, b) + .16 * sstep(.6, 1, B2[i]); }
    return gauss(H, w, h, Math.max(1, rs * 1.1));
  }
  function uploadRelief(G, H, w, h, rect, rs, amp) {
    var n = w * h, out = new Uint8Array(n * 4), kk = rs * amp * .5 / 1.25 * 127, i, yy, xx;
    for (yy = 0; yy < h; yy++) for (xx = 0; xx < w; xx++) {
      i = yy * w + xx; var l = H[yy * w + Math.max(xx - 1, 0)], r = H[yy * w + Math.min(xx + 1, w - 1)], u = H[Math.max(yy - 1, 0) * w + xx], dn = H[Math.min(yy + 1, h - 1) * w + xx], o = i * 4;
      var dx = (r - l) * kk, dy = (dn - u) * kk, nz = Math.min(1, (Math.abs(dx) + Math.abs(dy)) * .6); out[o] = clamp(128 + dx + (Math.random() - .5) * nz, 0, 255); out[o + 1] = clamp(128 + dy + (Math.random() - .5) * nz, 0, 255); out[o + 2] = clamp((Math.min(H[i], 1) + .28) / 1.28 * 255, 0, 255); out[o + 3] = 255;
    }
    var gl = G.gl; gl.activeTexture(gl.TEXTURE1); gl.bindTexture(gl.TEXTURE_2D, G.tr); gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, w, h, 0, gl.RGBA, gl.UNSIGNED_BYTE, out);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE); gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.uniform4f(G.u.uRelR, rect[0], rect[1], rect[2], rect[3]);
  }
  function buildRelief(G, items, rect, rs, bevel, amp) {
    var w = Math.max(2, Math.round((rect[2] - rect[0]) * rs)), h = Math.max(2, Math.round((rect[3] - rect[1]) * rs));
    var H = reliefH(w, h, function (x) { items.forEach(function (it) { x.drawImage(it.img, (it.x - rect[0]) * rs, (it.y - rect[1]) * rs, it.w * rs, it.h * rs); }); }, rs, bevel, .28);
    if (H) uploadRelief(G, H, w, h, rect, rs, amp);
  }
  var imgCache = {};
  function loadImg(src) { if (!imgCache[src]) imgCache[src] = new Promise(function (res, rej) { var i = new Image(); i.onload = function () { res(i); }; i.onerror = rej; i.src = src; }); return imgCache[src]; }

  /* ── 첫 화면 그리기 ── */
  var AC0 = new Float32Array(4), AC1 = new Float32Array(4), AC2 = new Float32Array(4), AREV = new Float32Array(3);
  var cv = $('gl'), G = null, ctx2 = null, tile = new Image(), scale = 1, DU = new Float32Array(56), lastDraw = 0, heroOn = true, fps = [], lowered = 0;
  var TONE = { dark: [[.030, .0095, .0040], [.15, .052, .017], [.165, .075, .035]], milk: [[.225, .125, .066], [.31, .178, .094], [.36, .238, .148]], cream: [[1.75, 1.26, .84], [1.55, 1.08, .68], [.70, .64, .50]] };
  function applyScene(g, o) {
    var gl = g.gl, u = g.u, tn = TONE[o.tone || 'dark']; gl.uniform3fv(u.uAlbD, tn[0]); gl.uniform3fv(u.uAlbW, tn[1]); gl.uniform3fv(u.uFlat, tn[2]); var dk = (o.tone || 'dark') === 'dark'; gl.uniform1f(u.uSa, dk ? .62 : .72); gl.uniform3fv(u.uSc, dk ? [.20, .09, .035] : [.12, .05, .018]); gl.uniform2f(u.uR, o.W, o.H); gl.uniform1f(u.uT, o.t); gl.uniform1f(u.uU, o.U); gl.uniform1f(u.uF, o.f); gl.uniform1f(u.uBow, o.bow); gl.uniform1f(u.uBead, o.bead); gl.uniform1f(u.uTop, o.top); gl.uniform1f(u.uPx, o.px); gl.uniform1f(u.uPlate, o.plate); gl.uniform1f(u.uDr, o.dr); gl.uniform1f(u.uExpo, o.expo); gl.uniform1f(u.uBisK, o.bk); gl.uniform1f(u.uWave, o.wave === undefined ? 1 : o.wave); gl.uniform2f(u.uOff, o.ox || 0, o.oy || 0); gl.uniform1f(u.uBU, o.bu || o.U); gl.uniform1f(u.uTopOp, o.topOp || 0); gl.uniform1f(u.uWK, o.wk === undefined ? 1 : o.wk); gl.uniform1f(u.uCalmY, o.calmY === undefined ? -1e4 : o.calmY); gl.uniform1f(u.uCalmK, o.calmk || 0); gl.uniform1f(u.uChK, o.chk || 0); gl.uniform1f(u.uTopY, o.topY === undefined ? -1e4 : o.topY); gl.uniform1f(u.uGhost, o.ghost || 0);
    gl.uniform4fv(u.uD, o.D); gl.uniform4fv(u.uC0, o.c0); gl.uniform4fv(u.uC1, o.c1); gl.uniform4fv(u.uC2, o.c2); gl.uniform3fv(u.uRev, o.rev);
  }
  function drawHero(now) {
    var t = REDUCED ? 8 : (now - t0) / 1000; tNow = t; updateDrips(t);
    if (!G) { draw2D(t); return; }
    for (var i = 0; i < 14; i++) { var d = drips[i], o = i * 4; if (d) { DU[o] = d.x; DU[o + 1] = d.y0; DU[o + 2] = d.y1; DU[o + 3] = d.re; } else { DU[o + 3] = 0; } }
    AC0.set(cellsR[0]); AC1.set(cellsR[1]); AC2.set(cellsR[2]); AREV.set(rev);
    var gl = G.gl; gl.viewport(0, 0, cv.width, cv.height);
    applyScene(G, { wk: S.wkE, calmY: S.calmY, calmk: 1, chk: 1, topY: S.topY, ghost: 1, W: S.W, H: S.H, t: t, U: S.U, f: fy, bow: S.bowE, bead: S.bead, top: -1e4, px: 1 / scale, plate: 0, dr: t * .55, expo: S.wide ? 1.06 : 1.18, bk: 1 / (S.wide ? 780 : 620), bu: S.U, D: DU, c0: AC0, c1: AC1, c2: AC2, rev: AREV });
    gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
  }
  function draw2D(t) {
    if (!ctx2) return; var W = S.W, H = S.H, dp = Math.min(devicePixelRatio || 1, 1.5), x = ctx2; if (cv.width !== Math.round(W * dp)) { cv.width = Math.round(W * dp); cv.height = Math.round(H * dp); }
    x.setTransform(dp, 0, 0, dp, 0, 0); x.fillStyle = '#D6A862'; x.fillRect(0, 0, W, H); if (tile.complete && tile.naturalWidth) { x.fillStyle = x.createPattern(tile, 'repeat'); x.fillRect(0, 0, W, H); }
    var cy = Math.min(S.calmY, H), cg = x.createLinearGradient(0, cy - S.U * .12, 0, cy); cg.addColorStop(0, 'rgba(237,204,153,0)'); cg.addColorStop(1, '#EDCC99'); x.fillStyle = cg; x.fillRect(0, cy - S.U * .12, W, H);
    if (glyphs && cellsReady) cells.forEach(function (c, q) { x.drawImage(glyph2D(q, '#4E2A12'), c[0], c[1], c[2] - c[0], c[3] - c[1]); });
    var path = new Path2D(); path.moveTo(-2, -4); path.lineTo(W + 2, -4); for (var xx = W + 2; xx >= -2; xx -= 3) path.lineTo(xx, frontY(xx, t));
    path.closePath(); x.save(); x.shadowColor = 'rgba(58,24,6,.6)'; x.shadowBlur = 20; x.shadowOffsetX = 3; x.shadowOffsetY = 12;
    var g = x.createLinearGradient(0, 0, W, 0); g.addColorStop(0, '#190A03'); g.addColorStop(.1, '#3E1D0D'); g.addColorStop(.17, '#8A5A3D'); g.addColorStop(.24, '#3C1C0C'); g.addColorStop(.62, '#2A1208'); g.addColorStop(.82, '#4F2815'); g.addColorStop(.86, '#7A4C33'); g.addColorStop(.9, '#2E1509'); g.addColorStop(1, '#170902');
    x.fillStyle = g; x.fill(path); x.restore();
    if (glyphs && cellsReady) { x.save(); x.clip(path); cells.forEach(function (c, q) { x.globalAlpha = rev[q] > .5 ? 1 : .55; x.drawImage(glyph2D(q, '#F8EBD3'), c[0], c[1], c[2] - c[0], c[3] - c[1]); }); x.restore(); }
  }
  var g2D = {};
  function glyph2D(q, col) { var k = q + col, w = 400, h = 400; if (!g2D[k]) g2D[k] = tint(glyphs[q === 2 ? 1 : 0], w, h, col); return g2D[k]; }

  /* ── 움직임 ── */
  var lastStep = 0, raf = 0;
  function revTargets() { for (var q = 0; q < 3; q++) revT[q] = cur >= revAt[q] ? 1 : 0; }
  /* 숫자 칸 색은 칸 가운데를 지날 때 통째로 바뀐다 */
  function renderInk() { var on = fy > S.covY; if (on !== covOn) { covOn = on; hero.classList.toggle('cov', on); } }
  var revT = [0, 0, 0];
  function snap(now) {
    cur = total; vel = 0; pour = 0; fy = frontBase(cur); revTargets(); rev = revT.slice(); renderNumber(cur); renderInk(); drawHero(now);
  }
  function frame(now) {
    raf = requestAnimationFrame(frame);
    var real = Math.min(1.2, (now - lastStep) / 1000 || .016); lastStep = now;
    var moving = Math.abs(total - cur) > .5 || Math.abs(vel) > 1 || pour > .004;
    if (moving) {
      for (var n = Math.ceil(real / .016), h = real / n, j = 0; j < n; j++) { vel += (-2 * om * vel - om * om * (cur - total)) * h; cur += vel * h; }
      if (Math.abs(total - cur) < 60 && Math.abs(vel) < 300) { cur = total; vel = 0; }
      var rate = Math.min(1, Math.abs(vel) / T2 * 3.4); pour += (rate - pour) * Math.min(1, real * (rate > pour ? 7 : .9));
      renderNumber(cur);
    }
    var ft = frontBase(cur), fyMoving = Math.abs(ft - fy) > .2; if (fyMoving) { fy += (ft - fy) * Math.min(1, real * 8); } else fy = ft; renderInk();
    revTargets(); var revMoving = false;
    for (var q = 0; q < 3; q++) { if (Math.abs(revT[q] - rev[q]) > .003) { rev[q] += (revT[q] - rev[q]) * Math.min(1, real * 3.2); revMoving = true; } else rev[q] = revT[q]; }
    if (!heroOn) return;
    var busy = moving || revMoving || fyMoving, gap = busy ? 0 : 33;
    if (now - lastDraw < gap) return;
    if (G && !lowered0 && lastDraw) sample(now - lastDraw, busy);
    lastDraw = now; drawHero(now);
  }
  /* 저사양: 첫 붓기 동안 프레임을 재서 해상도를 낮춘다 */
  var lowered0 = false, samp = 0, sampN = 0;
  function sample(dt, busy) {
    if (document.hidden || dt > 240 || !dt) return; samp += dt; sampN++;
    if (sampN >= 24) { var avg = samp / sampN, lim = busy ? 40 : 52; samp = 0; sampN = 0; if (avg > lim && scale > .75) { scale = Math.max(.75, scale * .72); sizeGL(); lowered++; } if (lowered > 3) lowered0 = true; }
  }
  function sizeGL() { if (!G) return; cv.width = Math.round(S.W * scale); cv.height = Math.round(S.H * scale); }

  /* ── 숫자 ── */
  var D = { w: 1923, h: 292, map: { '0': [0, 158], '1': [160, 97], '2': [259, 158], '3': [419, 164], '4': [585, 175], '5': [762, 164], '6': [928, 158], '7': [1088, 151], '8': [1241, 164], '9': [1407, 158], ',': [1567, 72], '-': [1641, 108], 'D': [1751, 170] } };
  var GAP = D.h * .036, numLen = 0, numK = 1, numCells = [[]], NUMLEN = 9;
  function sizeNum(len) {
    numLen = len; var col = elCount[0].clientWidth, maxH = S.wide ? Math.min(180, S.H * .2) : S.W >= 600 ? Math.min(180, S.H * .17) : Math.min(96, S.H * .105);
    var digits = len - Math.floor((len - 1) / 4), commas = len - digits, w = digits * D.map['3'][1] + commas * D.map[','][1] + (len - 1) * GAP; numK = Math.min(col / w, maxH / D.h);
    both(elNum, function (e) { e.style.height = (D.h * numK).toFixed(1) + 'px'; e.textContent = ''; }); numCells = [[]];
  }
  function setStr(s, sizeLen) {
    if (numLen !== sizeLen || !numCells[0].length) sizeNum(sizeLen);
    var k = numK;
    for (var L = 0; L < 1; L++) {
      var arr = numCells[L], host = elNum[L];
      while (arr.length < s.length) { var i = document.createElement('i'), sz = (D.w * k).toFixed(1) + 'px ' + (D.h * k).toFixed(1) + 'px'; i.style.webkitMaskSize = sz; i.style.maskSize = sz; i.style.height = (D.h * k).toFixed(1) + 'px'; i.style.marginRight = (GAP * k).toFixed(2) + 'px'; host.appendChild(i); arr.push({ el: i, ch: '' }); }
      while (arr.length > s.length) host.removeChild(arr.pop().el);
      for (var n = 0; n < s.length; n++) { var c = arr[n]; if (c.ch !== s[n]) { var m = D.map[s[n]], pos = (-m[0] * k).toFixed(2) + 'px 0'; c.el.style.width = (m[1] * k).toFixed(2) + 'px'; c.el.style.webkitMaskPosition = pos; c.el.style.maskPosition = pos; c.ch = s[n]; } }
    }
  }
  function renderNumber(v) { if (phase() === 'before') return; setStr(fmt(v), NUMLEN); }

  /* ── 상태 표시 ── */
  function daysLeft() { var ref = phaseOverride === 'before' ? Date.UTC(2026, 10, 4, 15) : Date.now(); return Math.max(1, Math.ceil((START - ref) / 864e5)); }
  function renderState() {
    var p = phase(), v = total, msg;
    setText(elLb, p === 'before' ? '시작까지' : p === 'count' ? '집계 중' : p === 'result' ? '최종 걸음' : '다 같이 걸은 걸음');
    if (p === 'before') { var dn = 'D-' + daysLeft(); setStr(dn, NUMLEN); both(elNum, function (e) { e.setAttribute('aria-label', dn); }); msg = '11/7(토) 시작 · 사전 등록하면 응모권 1장'; }
    else {
      renderNumber(cur); both(elNum, function (e) { e.setAttribute('aria-label', fmt(v) + ' 걸음'); });
      if (p === 'count') msg = '11/16(월) 결과 발표';
      else if (p === 'result') msg = '당첨자 발표 11/16(월)';
      else if (v >= T2) msg = '222만 걸음 달성, 막대 완성';
      else if (v >= T1) msg = '111만 걸음 달성 · 222만까지 ' + fmt(T2 - v) + ' 남음';
      else msg = '111만 걸음까지 ' + fmt(T1 - v) + ' 남음';
    }
    if (p !== 'before' && !PREVIEW && !CFG.url && typeof CFG.total !== 'number') { msg = '집계 준비 중 · 매일 밤 9시 갱신'; both(elNum, function (e) { e.style.visibility = 'hidden'; e.style.height = '0'; }); }
    setText(elSub, msg);
    var showTag = PREVIEW && p !== 'before' && demoShown; both(elTag, function (e) { e.hidden = !showTag; });
  }
  var bubT = 0;
  var BUB = { '+11,111걸음': ['x_bBub.webp', 540, 118] };
  function say(text) {
    if (REDUCED) return; var b = BUB[text];
    if (b) { bub.innerHTML = '<span class="g" style="--m:url(' + A + b[0] + ');aspect-ratio:' + b[1] + '/' + b[2] + '" role="img" aria-label="' + text + '"></span>'; } else bub.textContent = text;
    bub.hidden = false; var xb = S.W * .66, yb = frontY(xb, tNow) + (frontBase(total) - fy) + 14, cTop = elCount[0].getBoundingClientRect().top - hero.getBoundingClientRect().top; yb = clamp(yb, 96, Math.max(96, cTop - 46));
    bub.style.left = (xb - bub.offsetWidth / 2) + 'px'; bub.style.top = yb + 'px';
    requestAnimationFrame(function () { bub.classList.add('on'); }); clearTimeout(bubT); bubT = setTimeout(function () { bub.classList.remove('on'); setTimeout(function () { bub.hidden = true; }, 260); }, 2200);
  }
  function setTotal(v, quiet) {
    var prev = total; total = Math.max(0, v); if (phase() === 'before') total = Math.max(total, BEFORE_FILL);
    if (REDUCED) snap(performance.now());
    if (!quiet && total > prev) { if (prev < T2 && total >= T2) say('222만 걸음 달성'); else if (prev < T1 && total >= T1) say('111만 걸음 달성'); else say('+' + fmt(total - prev) + '걸음'); }
    renderState();
  }

  /* ── 배치 ── */
  var lastW = 0, lastH = 0, glyphs = null;
  function layout() {
    var W = hero.clientWidth, H = hero.clientHeight, sameW = W === lastW; if (sameW && H === lastH) { numLen = 0; renderState(); return; } lastW = W; lastH = H;
    S.W = W; S.H = H; S.wide = W >= 900; S.U = Math.min(W, H * .62); S.bead = S.U * (S.wide ? .068 : .082); S.bow = S.U * (S.wide ? .07 : .03); S.wk = S.wide ? 1 : .7;
    numLen = 0; renderState();
    var heroTop = hero.getBoundingClientRect().top, hr = head.getBoundingClientRect(), hb = hr.bottom - heroTop, cr0 = elCount[0].getBoundingClientRect(), cTop = cr0.top - heroTop, cBot = cr0.bottom - heroTop;
    buildDrips(); var gx = parseFloat(getComputedStyle(head).left) || 16, gap = Math.max(13, S.U * .034), wm = .044 * S.U * S.wk, lim = cTop - 6, slack = Math.max(8, H * .018);
    S.lim = lim; S.yA = lim - S.bow - wm; S.covY = (cTop + cBot) * .5; S.calmY = cTop - 2; S.topY = hb; S.cH = cBot - cTop;
    /* 글자 칸: 숫자 칸 위 남은 높이 안에. 111만에서 셋 다 덮이도록 아래 끝을 초콜릿 위치에 맞춘다 */
    var ar = [822 / 841, 822 / 841, 832 / 814], bot = S.yA - slack - wm - 3 + S.bow * .1;
    if (!S.wide) {
      var top = hb + 10, all = Math.max(60, bot - top), cwA = (W - gx * 2 - gap * 2) / 3, chA = cwA / ar[0], stag = S.U * .035, gap2 = Math.max(gap, wm * 2 + S.bow + 5), availH = (all - gap2 - stag) / 2, cwB = Math.min((W - gx * 2 - gap) / 2, availH * ar[0]);
      if (chA > all) { chA = all; cwA = chA * ar[0]; }
      /* 빼빼로는 한 줄. 제목 바로 아래에 두어 초콜릿이 내려올 자리를 남김 */
      S.two = false;
      var chr = cwA / ar[2], rowH = Math.max(chA, chr), rowTop = top + Math.min(S.U * .012, Math.max(0, all - rowH) * .2), rowBot = rowTop + rowH;
      cells[0] = [gx, rowBot - chA, gx + cwA, rowBot]; cells[1] = [gx + cwA + gap, rowBot - chA, gx + cwA * 2 + gap, rowBot]; cells[2] = [gx + (cwA + gap) * 2, rowBot - chr, gx + (cwA + gap) * 2 + cwA, rowBot];
    } else {
      S.two = false; var x0 = hr.right + S.U * .07, right = W - gx, top2 = Math.max(hr.top - heroTop - S.U * .06, 12), all2 = bot - top2, cw2 = Math.min((right - x0 - gap * 2) / 3, all2 * .62 * ar[0]), ch2 = cw2 / ar[0], step2 = Math.min(ch2 * .5, (all2 - ch2) / 2), xr = right - cw2 * 3 - gap * 2, chr2 = cw2 / ar[2];
      cells[0] = [xr, bot - step2 * 2 - ch2, xr + cw2, bot - step2 * 2]; cells[1] = [xr + cw2 + gap, bot - step2 - ch2, xr + cw2 * 2 + gap, bot - step2]; cells[2] = [right - cw2, bot - chr2, right, bot];
    }
    cellsReady = true; cellsR = cells.map(function (c) { var m = gap * .5; return [c[0] - m, c[1] - m, c[2] + m, c[3] + m]; });
    function cover(c) { var u = Math.max(Math.abs(c[0] - W / 2), Math.abs(c[2] - W / 2)) / (W / 2); return c[3] + 3 + wm - S.bow * (1 - u * u); }
    var y0 = hb + 2 + wm * .4, f0 = Math.max(y0, cover(cells[0])), f1 = Math.max(f0, cover(cells[1])), f2 = Math.max(f1, cover(cells[2])); S.yA = Math.max(f2, S.yA);
    S.dripCap = S.wide ? 1e5 : Math.max(30, (lim - y0) * .42); if (S.wide) FK = [[0, y0], [revAt[0], f0], [revAt[1], f1], [T1, f2], [T2 - 1, S.yA], [T2, H + S.bead * 2.2 + S.U * .03]];
    else { revAt = [T1 * .1, T1 * .1, T1 * .1]; FK = [[0, y0], [T1 * .5, f2], [T1, f2 + (S.yA - f2) * .5], [T2 - 1, S.yA], [T2, H + S.bead * 2.2 + S.U * .03]]; }
    fy = frontBase(cur); renderInk();
    if (G) { scale = pickScale(); sizeGL(); } if (glyphs && !(sameW && lastRelH === H)) makeHeroRelief();
    renderState(); revTargets(); drawHero(performance.now());
  }
  function pickScale() {
    var dp = Math.min(devicePixelRatio || 1, 2), cap = /Mobi|Android/i.test(navigator.userAgent) ? 1.9e6 : 3.2e6, sc = dp; if (S.W * S.H * sc * sc > cap) sc = Math.sqrt(cap / (S.W * S.H));
    for (var i = 0; i < lowered; i++) sc *= .72; return Math.max(.75, sc);
  }
  var lastRelH = 0;
  function makeHeroRelief() {
    if (!G || !glyphs) return; lastRelH = S.H;
    var x0 = 1e9, y0 = 1e9, x1 = -1e9, y1 = -1e9, items = cells.map(function (c, i) { x0 = Math.min(x0, c[0]); y0 = Math.min(y0, c[1]); x1 = Math.max(x1, c[2]); y1 = Math.max(y1, c[3]); return { img: i === 2 ? glyphs[1] : glyphs[0], x: c[0], y: c[1], w: c[2] - c[0], h: c[3] - c[1] }; });
    var pad = 16, rect = [x0 - pad, y0 - pad, x1 + pad, y1 + pad], ch = cells[0][3] - cells[0][1], rs = Math.min(2, Math.max(1, 900 / Math.max(rect[2] - rect[0], rect[3] - rect[1])));
    buildRelief(G, items, rect, rs, ch * .032, ch * .034);
  }

  /* ── 아래 초콜릿 판: 같은 셰이더로 한 번만 굽는다 ── */
  var PG = null, plateTimer = 0;
  function plateGL() {
    if (PG) return PG; var c = document.createElement('canvas'); PG = mkGL(c, { alpha: true, premultipliedAlpha: true, preserveDrawingBuffer: true, antialias: false }); if (PG && tile.complete && tile.naturalWidth) setTile(PG, tile); return PG;
  }
  var plates = [{ key: 'auth', tone: 'dark', top: 1, sec: $('s1'), cv: $('pl1'), done: 0, t: 21 }, { key: 'tk', tone: 'cream', sec: $('s2'), cv: $('pl2'), done: 0, t: 33 }, { key: 'gift', tone: 'milk', sec: $('s3'), cv: $('pl3'), done: 0, t: 47 }, { key: 'date', tone: 'cream', sec: $('s4'), cv: $('pl4'), done: 0, t: 61 }, { key: 'end', tone: 'dark', sec: $('s5'), cv: $('pl5'), done: 0, t: 47 }];
  function renderPlate(pl) {
    var g = plateGL(); if (!g) return Promise.resolve();
    var r = pl.cv.getBoundingClientRect(), W = r.width, H = r.height; if (W < 10 || H < 10) return Promise.resolve();
    var ps = Math.min(devicePixelRatio || 1, 2); if (W * H * ps * ps > 3.8e6) ps = Math.sqrt(3.8e6 / (W * H)); ps = Math.max(1, ps);
    var U = Math.min(W, 880), jobs = [], items = [], isEnd = pl.key === 'end';
    if (isEnd) {
      var seal = $('seal').getBoundingClientRect(), gx = parseFloat(getComputedStyle(pl.sec.querySelector('.in')).paddingLeft) || 16, gw = 92, gl0 = (W - gw * 3 - U * .02) / 2, ty = seal.top - r.top + 14;
      jobs.push(loadImg(A + 'x_bPp.webp').then(function (im) { items.push({ img: im, x: gl0, y: ty, w: gw, h: gw * 841 / 822 }, { img: im, x: gl0 + gw + U * .01, y: ty, w: gw, h: gw * 841 / 822 }); }));
      jobs.push(loadImg(A + 'x_bPr.webp').then(function (im) { items.push({ img: im, x: gl0 + (gw + U * .01) * 2, y: ty, w: gw, h: gw * 814 / 832 }); }));
    }
    return Promise.all(jobs).then(function () {
      var off = g.canvas, gl = g.gl; off.width = Math.round(W * ps); off.height = Math.round(H * ps); gl.viewport(0, 0, off.width, off.height); gl.clearColor(0, 0, 0, 0); gl.clear(gl.COLOR_BUFFER_BIT);
      var big = 0; items.forEach(function (i) { big = Math.max(big, i.h); });
      var rect = items.length ? [0, Math.min.apply(null, items.map(function (i) { return i.y; })) - 14, W, Math.min(H, Math.max.apply(null, items.map(function (i) { return i.y + i.h; })) + 14)] : [0, 0, W, 64];
      var sc = Math.min(2, 1100 / Math.max(rect[2] - rect[0], rect[3] - rect[1], 1)); items.sort(function (a, b) { return a.y - b.y; });
      buildRelief(g, items, rect, Math.max(.9, sc), (big || 60) * .017, (big || 60) * .042);
      var bead = U * .062, D = new Float32Array(56), over = parseFloat(getComputedStyle(pl.sec).getPropertyValue('--drip')) || 0, front = over ? H - over - bead * .4 : H + 900, t = pl.t;
      if (over) {
        var tb = D8; for (var i = 0; i < tb.length; i++) { var ti = (i + pl.t) % tb.length, dx = (pl.tone === 'dark' ? 1 - tb[i][0] : tb[i][0]) * W, L = Math.min(tb[ti][1] * U * 1.1, over * .62), base = front + U * (.026 * Math.sin(dx / (U * .47) + 1.3 + t * .05) + .012 * Math.sin(dx / (U * .19) + .4 - t * .037) + .006 * Math.sin(dx / (U * .083) + 2.1 + t * .03)); D[i * 4] = dx; D[i * 4 + 1] = base - bead * 1.15; D[i * 4 + 2] = base + L; D[i * 4 + 3] = tb[i][2] * U; }
      }
      var cream = pl.tone === 'cream';
      applyScene(g, { tone: pl.tone, calmk: 1, chk: cream ? .78 : pl.tone === 'milk' ? .86 : isEnd ? .66 : .78, ghost: isEnd ? 1 : 0, W: W, H: H, t: t, U: U, f: front, bow: 0, bead: bead, top: pl.top ? 46 : -1e4, topOp: pl.top ? 1 : 0, ox: 0, oy: S.H, bu: S.U, px: 1 / ps, plate: 1, wave: cream ? .55 : .38, dr: pl.t * .13, expo: .96, bk: 1 / 620, D: D, c0: new Float32Array([0, 0, W, H]), c1: new Float32Array([0, 0, 0, 0]), c2: new Float32Array([0, 0, 0, 0]), rev: new Float32Array([1, 0, 0]) });
      gl.drawArrays(gl.TRIANGLE_STRIP, 0, 4);
      pl.cv.width = off.width; pl.cv.height = off.height; pl.cv.getContext('2d').drawImage(off, 0, 0); pl.done = W;
    }).catch(function (e) { console.warn(e); });
  }
  function tint(img, w, h, col) { var c = document.createElement('canvas'); c.width = Math.max(1, Math.round(w)); c.height = Math.max(1, Math.round(h)); var x = c.getContext('2d'); x.drawImage(img, 0, 0, c.width, c.height); x.globalCompositeOperation = 'source-in'; x.fillStyle = col; x.fillRect(0, 0, c.width, c.height); return c; }
  var plateObs = null;
  function watchPlates() {
    if (!window.IntersectionObserver) { plates.forEach(renderPlate); return; }
    plateObs = new IntersectionObserver(function (es) { es.forEach(function (e) { if (e.isIntersecting) { var pl = plates.filter(function (p) { return p.sec === e.target; })[0]; if (pl && pl.done !== pl.cv.getBoundingClientRect().width) renderPlate(pl); } }); }, { rootMargin: '150% 0px' });
    plates.forEach(function (p) { plateObs.observe(p.sec); });
  }

  /* ── 데이터 ── */
  function readTotal() {
    if (!CFG.url) return;
    fetch(CFG.url + '/rest/v1/rpc/walkingday_total', { method: 'POST', headers: { 'Content-Type': 'application/json', apikey: CFG.key }, body: '{}', cache: 'no-store' })
      .then(function (r) { return r.ok ? r.json() : null; })
      .then(function (d) { var row = Array.isArray(d) ? d[0] : d; if (row && isFinite(+row.total)) setTotal(+row.total); })
      .catch(function () { /* 다음 주기에 다시 읽음. 화면은 마지막 숫자 유지 */ });
  }

  /* ── 사전 등록 링크: 들어온 주소의 ref·utm_* 를 덮어써서 QR 유입 값 유지 ── */
  var ctas = [].slice.call(document.querySelectorAll('a.cta'));
  try {
    var qs = new URLSearchParams(location.search), keep = [];
    qs.forEach(function (v, k) { if (v && (k === 'ref' || /^utm_/i.test(k))) keep.push([k, v]); });
    if (keep.length) ctas.forEach(function (a) { var u = new URL(a.href); keep.forEach(function (p) { u.searchParams.set(p[0], p[1]); }); a.href = u.toString(); });
  } catch (e) { /* 기본 주소 유지 */ }

  /* 11/7~11/12 걸음 인증 양식이 정해지면(config.js의 proof) 주 버튼이 인증으로 바뀜 */
  if (CFG.proof && phase() === 'live') ctas.forEach(function (a) { a.href = CFG.proof; if (a.firstChild && a.firstChild.nodeType === 3) a.firstChild.nodeValue = '걸음 인증하기'; });

  /* ── 하단 고정 버튼: 폰에서 첫 화면 버튼이 나가면 표시, 끝 부분 버튼이 보이면 숨김 ── */
  (function () {
    var st = $('sticky'), fd = $('stFade'), hc = hero.querySelector('.cta'), eh = document.querySelector('#s5 h2'); if (!st || !hc || !eh) return;
    var faces = [['s1', '#241006'], ['s2', '#F6E6CB'], ['s3', '#7E4D2B'], ['s4', '#F6E6CB'], ['s5', '#241006']];
    function apply() { var vh = innerHeight, on = hc.getBoundingClientRect().bottom < 8 && eh.getBoundingClientRect().top > vh - 130; st.classList.toggle('on', on); fd.classList.toggle('on', on); if (on) { var c = faces[0][1]; faces.forEach(function (f) { if ($(f[0]).getBoundingClientRect().top < vh - 60) c = f[1]; }); fd.style.setProperty('--fc', c); } }
    addEventListener('scroll', apply, { passive: true }); addEventListener('resize', apply);
    setTimeout(function () { st.classList.add('ready'); apply(); }, 400);
  })();

  /* ── 복사 ── */
  $('copy').addEventListener('click', function () {
    var b = this, url = CFG.share || location.href.split('#')[0], done = function () { b.textContent = '복사됨'; setTimeout(function () { b.textContent = '링크 복사'; }, 1600); };
    if (navigator.clipboard) navigator.clipboard.writeText(url).then(done, function () { b.textContent = url; }); else b.textContent = url;
  });

  /* ── 제품 사진 대체 ── */
  

  /* ── 시안 조작(#preview일 때만) ── */
  var demoShown = false;
  function setupPreview() {
    var btn = $('dockBtn'), dock = $('dock'), r = $('dR'), o = $('dO');
    btn.hidden = false;
    btn.addEventListener('click', function () { dock.hidden = !dock.hidden; btn.setAttribute('aria-expanded', String(!dock.hidden)); });
    var sync = function () { r.value = total; o.textContent = fmt(total) + '걸음'; };
    r.addEventListener('input', function () { phaseOverride = 'live'; demoShown = true; setTotal(+r.value, true); o.textContent = fmt(total) + '걸음'; });
    dock.addEventListener('click', function (e) {
      var a = e.target.getAttribute && e.target.getAttribute('data-a'); if (!a) return; demoShown = true;
      if (a === 'before') { phaseOverride = 'before'; setTotal(0, true); }
      else if (a === 'plus') { phaseOverride = 'live'; setTotal(total + 11111); }
      else if (a === 'closed') { phaseOverride = 'result'; setTotal(Math.max(total, 1), true); }
      else { phaseOverride = 'live'; setTotal(+a); }
      sync();
    });
    var Q = new URLSearchParams(location.search); phaseOverride = Q.get('wdphase') || 'live'; demoShown = true; total = 0; sync(); return function () { var qt = Q.get('wdtotal'); setTotal(phaseOverride === 'before' ? BEFORE_FILL : qt !== null ? +qt : EXAMPLE, true); sync(); };
  }

  /* ── 시작 ── */
  var startup = null;
  if (PREVIEW) startup = setupPreview();
  G = mkGL(cv, { alpha: false, antialias: false, powerPreference: 'high-performance' });
  if (!G) { document.documentElement.classList.add('nogl'); ctx2 = cv.getContext('2d'); }
  cv.addEventListener('webglcontextlost', function (e) { e.preventDefault(); G = null; var n = document.createElement('canvas'); n.id = 'gl'; n.setAttribute('role', 'img'); n.setAttribute('aria-label', cv.getAttribute('aria-label')); cv.parentNode.replaceChild(n, cv); cv = n; ctx2 = cv.getContext('2d'); document.documentElement.classList.add('nogl'); draw2D(tNow); });
  tile.onload = function () { if (G) setTile(G, tile); drawHero(performance.now()); if (PG) setTile(PG, tile); watchPlates(); };
  tile.onerror = function () { drawHero(performance.now()); };
  tile.src = A + 'b_biscuit.webp';
  Promise.all([loadImg(A + 'x_bPp.webp'), loadImg(A + 'x_bPr.webp')]).then(function (g) { glyphs = g; makeHeroRelief(); drawHero(performance.now()); }).catch(function () {});
  var rsz = 0;
  addEventListener('resize', function () { layout(); clearTimeout(rsz); rsz = setTimeout(function () { plates.reduce(function (q, p) { return q.then(function () { if (p.done && p.done !== p.cv.getBoundingClientRect().width) return renderPlate(p); }); }, Promise.resolve()); }, 220); });
  layout();
  if (window.IntersectionObserver) new IntersectionObserver(function (es) { heroOn = es[0].isIntersecting; }).observe(hero);
  if (startup) { setTimeout(startup, REDUCED ? 0 : 500); }
  else { if (phase() === 'before') total = BEFORE_FILL; else if (typeof CFG.total === 'number') total = CFG.total; else if (!CFG.url) total = BEFORE_FILL; renderState(); readTotal(); if (CFG.url) { setInterval(readTotal, 60000); document.addEventListener('visibilitychange', function () { if (!document.hidden) readTotal(); }); } }
  if (REDUCED) { snap(performance.now()); } else {
    raf = requestAnimationFrame(frame);
    /* 화면 갱신이 멈춘 환경(썸네일·숨은 탭)에서도 숫자와 초콜릿은 맞게 */
    setInterval(function () { var n = performance.now(); if (n - lastStep > 900 && (cur !== total || rev[0] !== revT[0] || rev[1] !== revT[1] || rev[2] !== revT[2])) snap(n); }, 700);
  }
  setInterval(renderState, 30000); /* 날짜가 넘어가면 상태도 넘어감 */
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(function () { if (Math.abs(elCount[0].offsetHeight - S.cH) > 1) { lastW = 0; layout(); } else renderState(); plates.forEach(function (p) { if (p.done) renderPlate(p); }); });
})();
