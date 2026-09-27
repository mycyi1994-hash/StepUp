// StepUp editable design package v7. Creates native nodes; never imports screenshots.
// Run once in a NEW blank Figma Design file. Three pages are created.
const nativeFigma=figma;
const figmaApi=Object.create(nativeFigma);
figmaApi.createAutoLayout=(direction='HORIZONTAL')=>{const n=nativeFigma.createFrame();n.layoutMode=direction;n.primaryAxisSizingMode='AUTO';n.counterAxisSizingMode='AUTO';return n;};

(async function(figma){
async function foundations(){
 const DS={pages:{},vars:{},styles:{},colors:{"bg":"#050911","surface":"#0D1522","raised":"#152237","border":"#25364C","text":"#F1F3FA","muted":"#A1AEC3","blue":"#3D83FF","softBlue":"#AACBFF","primary":"#F2F4FA","ink":"#080D17","danger":"#FF9B95","dangerBg":"#341B25","success":"#96D5BA"},catalog:{},icons:{}};
 const page=figma.currentPage;page.name='StepUp · 00 작업 안내 & 스타일';DS.pages.guide=page.id;
 for(const [key,name]of [['components','StepUp · 01 컴포넌트'],['screens','StepUp · 02 화면 & 플로우']]){const p=figma.createPage();p.name=name;DS.pages[key]=p.id;}
 const rgb=h=>({r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255});
 const primitive=figma.variables.createVariableCollection('StepUp · Primitives');primitive.renameMode(primitive.defaultModeId,'Base');
 const semantic=figma.variables.createVariableCollection('StepUp · Semantic');semantic.renameMode(semantic.defaultModeId,'Dark');
 for(const [key,hex]of Object.entries(DS.colors)){const p=figma.variables.createVariable('color/'+key,primitive,'COLOR');p.scopes=['FRAME_FILL','SHAPE_FILL','TEXT_FILL','STROKE_COLOR'];p.setValueForMode(primitive.defaultModeId,rgb(hex));p.setVariableCodeSyntax('WEB','var(--stepup-color-'+key+')');const v=figma.variables.createVariable('color/'+key,semantic,'COLOR');v.scopes=['FRAME_FILL','SHAPE_FILL','TEXT_FILL','STROKE_COLOR'];v.setValueForMode(semantic.defaultModeId,{type:'VARIABLE_ALIAS',id:p.id});v.setVariableCodeSyntax('WEB','var(--stepup-'+key+')');DS.vars[key]=v.id;}
 for(const n of [4,8,12,16,20,24,32,48,56,64,72]){const v=figma.variables.createVariable('space/'+n,primitive,'FLOAT');v.scopes=['GAP','WIDTH_HEIGHT','CORNER_RADIUS'];v.setValueForMode(primitive.defaultModeId,n);v.setVariableCodeSyntax('WEB','var(--stepup-space-'+n+')');DS.vars['s'+n]=v.id;}
 for(const [name,size,line,style]of [['Display',48,62,'Regular'],['Title',28,38,'Medium'],['Section',20,30,'Medium'],['Button',18,28,'Medium'],['Body',16,26,'Regular'],['Caption',14,22,'Regular'],['Micro',12,18,'Regular']]){const t=figma.createTextStyle();t.name='StepUp/'+name;t.fontName={family:'Noto Sans KR',style};t.fontSize=size;t.lineHeight={unit:'PIXELS',value:line};t.letterSpacing={unit:'PIXELS',value:name==='Title'?-.6:-.2};DS.styles[name]=t.id;}
 return DS;
}
async function buildAtomic(DS){
await figma.loadFontAsync({family:"Noto Sans KR",style:"Regular"});
await figma.loadFontAsync({family:"Noto Sans KR",style:"Medium"});
const V={};for(const [k,id] of Object.entries(DS.vars)) V[k]=await figma.variables.getVariableByIdAsync(id);
const c=h=>({r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255});
function paint(k){return figma.variables.setBoundVariableForPaint({type:"SOLID",color:c(DS.colors[k])},"color",V[k]);}
function fill(n,k){n.fills=[paint(k)];}
function gap(n,v){n.itemSpacing=v;if(V["s"+v])n.setBoundVariable("itemSpacing",V["s"+v]);}
function pad(n,v){for(const p of ["paddingTop","paddingBottom","paddingLeft","paddingRight"]){n[p]=v;if(V["s"+v])n.setBoundVariable(p,V["s"+v]);}}
function radius(n,v){n.cornerRadius=v;if(V["s"+v])n.setBoundVariable("cornerRadius",V["s"+v]);}
function border(n,k="border"){n.strokes=[paint(k)];n.strokeWeight=1;}
function al(name,dir="VERTICAL",w=342,g=12){const n=figma.createAutoLayout(dir);n.name=name;n.fills=[];n.resize(w,1);n.primaryAxisSizingMode=dir==="VERTICAL"?"AUTO":"FIXED";n.counterAxisSizingMode=dir==="VERTICAL"?"FIXED":"AUTO";gap(n,g);return n;}
async function txt(parent,name,str,style="Body",color="text",width){const t=figma.createText();t.name=name;await t.setTextStyleIdAsync(DS.styles[style]);t.characters=str;fill(t,color);parent.appendChild(t);t.textAutoResize="HEIGHT";t.resize(width||parent.width,1);if("layoutMode" in parent&&parent.layoutMode!=="NONE")t.layoutSizingVertical="HUG";return t;}
function prop(comp,node,name){const key=comp.addComponentProperty(name,"TEXT",node.characters);node.componentPropertyReferences={characters:key};return key;}
function comp(name,w=342){const n=figma.createComponent();n.name=name;n.layoutMode="VERTICAL";n.resize(w,1);n.primaryAxisSizingMode="AUTO";n.counterAxisSizingMode="FIXED";n.fills=[];return n;}
function allIds(roots){const out=[];const rec=n=>{out.push(n.id);if("children" in n)for(const a of n.children)rec(a);};roots.forEach(rec);return out;}

async function inst(family,variant,values={},parent){const f=DS.catalog[family];const master=await figma.getNodeByIdAsync(variant?f.variants[variant]:f.id);const n=master.createInstance();n.x=0;n.y=0;if(parent)parent.appendChild(n);const props={};for(const [key,value]of Object.entries(values))if(f.props&&f.props[key]){const wanted=f.props[key].split("#")[0];const actual=Object.keys(n.componentProperties).find(k=>k.split("#")[0]===wanted);if(actual)props[actual]=value;}if(Object.keys(props).length)n.setProperties(props);return n;}
async function btn(parent,label,tone="Primary",w){const n=await inst("Button",tone+"/Default",{label},parent);if(w){n.resize(w,56);const label=n.findOne(z=>z.type==="TEXT");if(label)label.resize(w-32,label.height);}n.name="Button / "+label;return n;}


const page=await figma.getNodeByIdAsync(DS.pages.components);await figma.setCurrentPageAsync(page);
const roots=[],catalog={},icons={};let cy=100;
function register(name,n,meta={}){n.x=100;n.y=cy;cy+=n.height+100;roots.push(n);catalog[name]={id:n.id,...meta};return n;}
function variants(name,items,cols=4){const s=figma.combineAsVariants(items,page);s.name="StepUp / "+name;let maxh=Math.max(...items.map(n=>n.height));items.forEach((n,i)=>{n.x=24+(i%cols)*(n.width+24);n.y=48+Math.floor(i/cols)*(maxh+36);});s.resize(Math.max(...items.map(n=>n.x+n.width))+24,Math.max(...items.map(n=>n.y+n.height))+24);fill(s,"bg");border(s);radius(s,16);s.description="StepUp 편집용 컴포넌트. 인스턴스에서 텍스트와 상태를 변경하세요.";return s;}
const paths={
back:"M15 5 L8 12 L15 19",close:"M6 6 L18 18 M18 6 L6 18",check:"M5 12 L10 17 L19 7",chevron:"M9 5 L16 12 L9 19",
play:"M8 5 L19 12 L8 19 Z",pause:"M8 5 L8 19 M16 5 L16 19",flag:"M5 21 L5 3 Q9 1 13 4 Q17 7 21 4 L21 15 Q17 18 13 15 Q9 12 5 14",
run:"M14 3 A2 2 0 1 0 14 7 A2 2 0 1 0 14 3 M5 11 L9 8 L14 10 L18 12 L21 10 M14 10 L11 15 L16 18 L16 22 M11 15 L7 19 L3 19",
pin:"M12 22 C8 17 4 13 4 9 A8 8 0 1 1 20 9 C20 13 16 17 12 22 Z M12 6 A3 3 0 1 0 12 12 A3 3 0 1 0 12 6",
walk:"M13 2 A2 2 0 1 0 13 6 A2 2 0 1 0 13 2 M7 13 L9 8 L13 8 L16 12 L20 14 M12 8 L11 15 L16 21 M11 15 L7 22",
refresh:"M20 9 A8 8 0 1 0 20 16 M20 3 L20 9 L14 9",locate:"M12 1 L12 5 M12 19 L12 23 M1 12 L5 12 M19 12 L23 12 M12 5 A7 7 0 1 0 12 19 A7 7 0 1 0 12 5 M12 9 A3 3 0 1 0 12 15 A3 3 0 1 0 12 9",
clock:"M12 2 A10 10 0 1 0 12 22 A10 10 0 1 0 12 2 M12 6 L12 12 L16 15",info:"M12 2 A10 10 0 1 0 12 22 A10 10 0 1 0 12 2 M12 10 L12 17 M12 6 L12 7",
edit:"M4 20 L5 14 L17 2 L22 7 L10 19 Z M14 5 L19 10",trash:"M4 7 L20 7 M9 7 L9 3 L15 3 L15 7 M6 7 L7 21 L17 21 L18 7 M10 10 L10 18 M14 10 L14 18"
};
const iconrow=al("Icons / 24px · stroke 1.6","HORIZONTAL",16*56,32);
for(const [key,path]of Object.entries(paths)){const n=figma.createComponent();n.name="StepUp / Icon / "+key;n.resize(24,24);n.fills=[];const v=figma.createNodeFromSvg('<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path d="'+path+'" fill="none" stroke="#AACBFF" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>');v.name="Editable path";n.appendChild(v);for(const z of v.findAll(q=>q.type==="VECTOR")){if(z.fills.length)z.fills=[paint("softBlue")];if(z.strokes.length)z.strokes=[paint("softBlue")];}n.description="24px 선 아이콘. 벡터 경로를 직접 편집할 수 있습니다.";iconrow.appendChild(n);icons[key]=n.id;}
register("Icons",iconrow,{icons});
const b=comp("Tone=Primary, State=Default");b.layoutMode="HORIZONTAL";b.resize(342,56);b.primaryAxisSizingMode="FIXED";b.counterAxisSizingMode="FIXED";b.primaryAxisAlignItems="CENTER";b.counterAxisAlignItems="CENTER";gap(b,8);pad(b,16);radius(b,16);fill(b,"primary");
const label=await txt(b,"Label","러닝 시작","Button","ink",246);label.textAlignHorizontal="CENTER";
const pLabel=prop(b,label,"Label");const bi=(await figma.getNodeByIdAsync(icons.play)).createInstance();b.appendChild(bi);const pIcon=b.addComponentProperty("Icon","INSTANCE_SWAP",icons.play);const pShow=b.addComponentProperty("Show icon","BOOLEAN",false);bi.componentPropertyReferences={mainComponent:pIcon,visible:pShow};bi.visible=false;
const bs=[];const bm={};
for(const tone of ["Primary","Secondary","Ghost","Danger"])for(const state of ["Default","Pressed","Disabled","Loading"]){const n=bs.length?b.clone():b;n.name="Tone="+tone+", State="+state;fill(n,tone==="Primary"?"primary":tone==="Danger"?"dangerBg":tone==="Ghost"?"bg":"surface");fill(n.findOne(z=>z.type==="TEXT"),tone==="Primary"?"ink":tone==="Danger"?"danger":"text");if(tone==="Secondary")border(n);if(state==="Pressed")n.opacity=.72;if(state==="Disabled")n.opacity=.36;if(state==="Loading")n.findOne(z=>z.type==="TEXT").characters="처리 중…";bs.push(n);bm[tone+"/"+state]=n.id;}
register("Button",variants("Button",bs),{variants:bm,props:{label:pLabel,icon:pIcon,showIcon:pShow}});
const card=comp("Tone=Secondary, State=Default");card.layoutMode="HORIZONTAL";card.resize(342,88);card.primaryAxisSizingMode="FIXED";card.counterAxisSizingMode="FIXED";pad(card,20);gap(card,12);radius(card,16);fill(card,"surface");border(card);
const ct=al("Copy","VERTICAL",254,4);card.appendChild(ct);const t1=await txt(ct,"Title","러닝 챌린지","Section");const t2=await txt(ct,"Description","준비된 목표에 도전하기","Caption","muted");const cp={title:prop(card,t1,"Title"),description:prop(card,t2,"Description")};
const ci=(await figma.getNodeByIdAsync(icons.flag)).createInstance();card.appendChild(ci);card.counterAxisAlignItems="CENTER";cp.icon=card.addComponentProperty("Icon","INSTANCE_SWAP",icons.flag);ci.componentPropertyReferences={mainComponent:cp.icon};
const cs=[],cm={};for(const tone of ["Primary","Secondary"])for(const state of ["Default","Pressed","Disabled"]){const n=cs.length?card.clone():card;n.name="Tone="+tone+", State="+state;fill(n,tone==="Primary"?"primary":"surface");for(const t of n.findAll(z=>z.type==="TEXT"))fill(t,tone==="Primary"?"ink":t.name==="Title"?"text":"muted");n.opacity=state==="Disabled"?.36:state==="Pressed"?.72:1;cs.push(n);cm[tone+"/"+state]=n.id;}
register("Action Card",variants("Action Card",cs,3),{variants:cm,props:cp});
const inp=comp("State=Empty",165);pad(inp,16);gap(inp,4);fill(inp,"surface");border(inp);radius(inp,16);
const it=await txt(inp,"Label","키","Body","text",133);const valrow=al("Value row","HORIZONTAL",133,8);inp.appendChild(valrow);valrow.counterAxisAlignItems="CENTER";const iv=await txt(valrow,"Value","—","Title","text",87);const iu=await txt(valrow,"Unit","cm","Caption","muted",38);const help=await txt(inp,"Helper","키를 입력해주세요","Micro","danger",133);help.visible=false;
const ip={label:prop(inp,it,"Label"),value:prop(inp,iv,"Value"),unit:prop(inp,iu,"Unit"),helper:prop(inp,help,"Helper")};const ih=inp.addComponentProperty("Show helper","BOOLEAN",false);help.componentPropertyReferences={characters:ip.helper,visible:ih};ip.showHelper=ih;
const ins=[],im={};for(const s of ["Empty","Focused","Filled","Error","Disabled"]){const n=ins.length?inp.clone():inp;n.name="State="+s;if(s==="Focused")border(n,"blue");if(s==="Error"){border(n,"danger");n.findOne(z=>z.name==="Helper").visible=true;}if(s==="Filled")n.findOne(z=>z.name==="Value").characters="170";if(s==="Disabled")n.opacity=.4;ins.push(n);im[s]=n.id;}
register("Number Field",variants("Number Field",ins,5),{variants:im,props:ip});
const opt=comp("State=Default");opt.layoutMode="HORIZONTAL";opt.resize(342,72);opt.primaryAxisSizingMode="FIXED";opt.counterAxisSizingMode="FIXED";pad(opt,16);gap(opt,12);radius(opt,16);fill(opt,"surface");border(opt);opt.counterAxisAlignItems="CENTER";
const ot=al("Copy","VERTICAL",274,2);opt.appendChild(ot);const o1=await txt(ot,"Label","처음이에요","Button");const o2=await txt(ot,"Description","아직 러닝 경험이 없어요","Micro","muted");const oi=(await figma.getNodeByIdAsync(icons.check)).createInstance();opt.appendChild(oi);oi.opacity=0;const op={label:prop(opt,o1,"Label"),description:prop(opt,o2,"Description")};
const os=[opt,opt.clone()];os[1].name="State=Selected";border(os[1],"blue");os[1].children[1].opacity=1;
register("Choice",variants("Choice",os,2),{variants:{Default:os[0].id,Selected:os[1].id},props:op});
const met=comp("Metric",155);gap(met,2);const mt=await txt(met,"Label","러닝 시간","Caption","muted");const mv=await txt(met,"Value","12:34","Title");register("Metric",met,{props:{label:prop(met,mt,"Label"),value:prop(met,mv,"Value")}});
const header=comp("Header",390);header.layoutMode="HORIZONTAL";header.resize(390,64);header.primaryAxisSizingMode="FIXED";header.counterAxisSizingMode="FIXED";pad(header,8);header.counterAxisAlignItems="CENTER";gap(header,8);const backWrap=al("Back · 48px touch","HORIZONTAL",48,0);backWrap.resize(48,48);backWrap.primaryAxisSizingMode="FIXED";backWrap.counterAxisSizingMode="FIXED";backWrap.primaryAxisAlignItems="CENTER";backWrap.counterAxisAlignItems="CENTER";backWrap.appendChild((await figma.getNodeByIdAsync(icons.back)).createInstance());header.appendChild(backWrap);const ht=await txt(header,"Title","러닝 시작","Body","text",270);register("Header",header,{props:{title:prop(header,ht,"Title")}});
const status=comp("Status Bar",390);status.layoutMode="HORIZONTAL";status.resize(390,44);status.primaryAxisSizingMode="FIXED";status.counterAxisSizingMode="FIXED";status.primaryAxisAlignItems="SPACE_BETWEEN";status.counterAxisAlignItems="CENTER";status.paddingLeft=24;status.paddingRight=24;await txt(status,"Time","9:41","Caption","text",60);const sys=figma.createNodeFromSvg('<svg xmlns="http://www.w3.org/2000/svg" width="80" height="20" viewBox="0 0 80 20"><g fill="#F1F3FA"><rect x="0" y="12" width="3" height="6" rx="1"/><rect x="5" y="9" width="3" height="9" rx="1"/><rect x="10" y="5" width="3" height="13" rx="1"/><rect x="15" y="2" width="3" height="16" rx="1"/></g><path d="M28 7 Q37 -1 46 7 M31 11 Q37 5 43 11 M34 15 Q37 12 40 15" fill="none" stroke="#F1F3FA" stroke-width="2.5" stroke-linecap="round"/><rect x="54" y="4" width="22" height="12" rx="3" fill="none" stroke="#F1F3FA" stroke-width="1.5"/><rect x="57" y="7" width="16" height="6" rx="1" fill="#F1F3FA"/><path d="M78 8 L78 12" stroke="#F1F3FA" stroke-width="2"/></svg>');sys.name='System indicators / editable vectors';status.appendChild(sys);register("Status Bar",status);
const hi=figma.createComponent();hi.name="StepUp / Home Indicator";hi.resize(390,24);hi.fills=[];const hl=figma.createRectangle();hi.appendChild(hl);hl.name="Indicator";hl.resize(134,5);hl.x=128;hl.y=12;radius(hl,4);fill(hl,"text");register("Home Indicator",hi);
return {createdNodeIds:allIds(roots),mutatedNodeIds:[],catalog,icons,bounds:roots.map(n=>({id:n.id,name:n.name,x:n.x,y:n.y,w:n.width,h:n.height})),counts:{components:page.findAllWithCriteria({types:["COMPONENT"]}).length,sets:page.findAllWithCriteria({types:["COMPONENT_SET"]}).length}};


}
async function buildMolecules(DS){
await figma.loadFontAsync({family:"Noto Sans KR",style:"Regular"});
await figma.loadFontAsync({family:"Noto Sans KR",style:"Medium"});
const V={};for(const [k,id] of Object.entries(DS.vars)) V[k]=await figma.variables.getVariableByIdAsync(id);
const c=h=>({r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255});
function paint(k){return figma.variables.setBoundVariableForPaint({type:"SOLID",color:c(DS.colors[k])},"color",V[k]);}
function fill(n,k){n.fills=[paint(k)];}
function gap(n,v){n.itemSpacing=v;if(V["s"+v])n.setBoundVariable("itemSpacing",V["s"+v]);}
function pad(n,v){for(const p of ["paddingTop","paddingBottom","paddingLeft","paddingRight"]){n[p]=v;if(V["s"+v])n.setBoundVariable(p,V["s"+v]);}}
function radius(n,v){n.cornerRadius=v;if(V["s"+v])n.setBoundVariable("cornerRadius",V["s"+v]);}
function border(n,k="border"){n.strokes=[paint(k)];n.strokeWeight=1;}
function al(name,dir="VERTICAL",w=342,g=12){const n=figma.createAutoLayout(dir);n.name=name;n.fills=[];n.resize(w,1);n.primaryAxisSizingMode=dir==="VERTICAL"?"AUTO":"FIXED";n.counterAxisSizingMode=dir==="VERTICAL"?"FIXED":"AUTO";gap(n,g);return n;}
async function txt(parent,name,str,style="Body",color="text",width){const t=figma.createText();t.name=name;await t.setTextStyleIdAsync(DS.styles[style]);t.characters=str;fill(t,color);parent.appendChild(t);t.textAutoResize="HEIGHT";t.resize(width||parent.width,1);if("layoutMode" in parent&&parent.layoutMode!=="NONE")t.layoutSizingVertical="HUG";return t;}
function prop(comp,node,name){const key=comp.addComponentProperty(name,"TEXT",node.characters);node.componentPropertyReferences={characters:key};return key;}
function comp(name,w=342){const n=figma.createComponent();n.name=name;n.layoutMode="VERTICAL";n.resize(w,1);n.primaryAxisSizingMode="AUTO";n.counterAxisSizingMode="FIXED";n.fills=[];return n;}
function allIds(roots){const out=[];const rec=n=>{out.push(n.id);if("children" in n)for(const a of n.children)rec(a);};roots.forEach(rec);return out;}

async function inst(family,variant,values={},parent){const f=DS.catalog[family];const master=await figma.getNodeByIdAsync(variant?f.variants[variant]:f.id);const n=master.createInstance();n.x=0;n.y=0;if(parent)parent.appendChild(n);const props={};for(const [key,value]of Object.entries(values))if(f.props&&f.props[key]){const wanted=f.props[key].split("#")[0];const actual=Object.keys(n.componentProperties).find(k=>k.split("#")[0]===wanted);if(actual)props[actual]=value;}if(Object.keys(props).length)n.setProperties(props);return n;}
async function btn(parent,label,tone="Primary",w){const n=await inst("Button",tone+"/Default",{label},parent);if(w){n.resize(w,56);const label=n.findOne(z=>z.type==="TEXT");if(label)label.resize(w-32,label.height);}n.name="Button / "+label;return n;}


const page=await figma.getNodeByIdAsync(DS.pages.components);await figma.setCurrentPageAsync(page);
const roots=[],updated=[],catalog={};
const ic=await figma.getNodeByIdAsync(DS.catalog.Icons.id);ic.primaryAxisSizingMode="FIXED";ic.counterAxisSizingMode="AUTO";ic.resize(864,24);updated.push(ic.id);
for(const [key,id]of Object.entries(DS.catalog["Action Card"].variants)){if(key.startsWith("Secondary")){const n=await figma.getNodeByIdAsync(id);fill(n,"surface");for(const t of n.findAll(z=>z.type==="TEXT"))fill(t,t.name==="Title"?"text":"muted");updated.push(n.id,...n.findAll(z=>z.type==="TEXT").map(z=>z.id));}}
let cy=2220;
function reg(name,n,meta={}){n.x=100;n.y=cy;cy+=n.height+100;roots.push(n);catalog[name]={id:n.id,...meta};return n;}
const modal=comp("Kind=Confirm",342);pad(modal,24);gap(modal,12);fill(modal,"raised");radius(modal,24);border(modal);
const mt=await txt(modal,"Title","러닝을 마칠까요?","Section","text",294);
const mb=await txt(modal,"Body","지금까지 달린 기록을 저장할 수 있어요.","Body","muted",294);
const ma=al("Actions","VERTICAL",294,8);modal.appendChild(ma);ma.paddingTop=12;
const primary=await btn(ma,"저장하고 마치기","Primary",294);const secondary=await btn(ma,"계속 달리기","Secondary",294);const tertiary=await btn(ma,"기록 없이 끝내기","Ghost",294);
const mp={title:prop(modal,mt,"Title"),body:prop(modal,mb,"Body")};
primary.isExposedInstance=true;secondary.isExposedInstance=true;tertiary.isExposedInstance=true;primary.name="Action / Primary";secondary.name="Action / Secondary";tertiary.name="Action / Tertiary";mp.showTertiary=modal.addComponentProperty("Show tertiary","BOOLEAN",false);tertiary.componentPropertyReferences={visible:mp.showTertiary};tertiary.visible=false;
const mods=[modal];const mm={Confirm:modal.id};
for(const kind of ["Danger","Info","Error"]){const n=modal.clone();n.name="Kind="+kind;mods.push(n);mm[kind]=n.id;if(kind==="Danger"){const bi=n.findOne(z=>z.type==="INSTANCE"&&z.name==="Action / Primary");bi.swapComponent(await figma.getNodeByIdAsync(DS.catalog.Button.variants["Danger/Default"]));}}
const set=figma.combineAsVariants(mods,page);set.name="StepUp / Dialog";mods.forEach((n,i)=>{n.x=24+i*366;n.y=48;});set.resize(1488,mods[0].height+72);fill(set,"bg");border(set);set.description="텍스트 속성으로 제목·본문·버튼 문구를 교체합니다. Show tertiary로 세 번째 버튼을 켭니다. 모달 확인 중 운동은 일시정지합니다.";reg("Dialog",set,{props:mp,variants:mm});
const toast=comp("Toast");pad(toast,16);radius(toast,16);fill(toast,"raised");border(toast);const tt=await txt(toast,"Message","기록을 저장했어요","Body","text",310);reg("Toast",toast,{props:{message:prop(toast,tt,"Message")}});
const note=comp("Notice");pad(note,16);gap(note,4);radius(note,16);fill(note,"surface");const nt=await txt(note,"Title","내 위치에서 출발해요","Body","softBlue",310);const nb=await txt(note,"Body","출발한 곳으로 돌아오는 코스예요.","Caption","muted",310);reg("Notice",note,{props:{title:prop(note,nt,"Title"),body:prop(note,nb,"Body")}});
const progress=figma.createComponent();progress.name="StepUp / Progress";progress.resize(342,6);progress.fills=[];const track=figma.createRectangle();track.name="Track";track.resize(342,6);radius(track,4);fill(track,"border");progress.appendChild(track);const prog=figma.createRectangle();prog.name="Progress · resize width";prog.resize(171,6);radius(prog,4);fill(prog,"blue");progress.appendChild(prog);progress.description="진행률은 Progress 레이어의 너비로 편집합니다. 0–342 px.";reg("Progress",progress);
const map=figma.createComponent();map.name="StepUp / Route Map";map.resize(342,268);radius(map,24);fill(map,"surface");map.clipsContent=true;
function vector(name,path,color,width){const v=figma.createVector();v.name=name;v.vectorPaths=[{windingRule:"NONZERO",data:path}];v.fills=[];v.strokes=[paint(color)];v.strokeWeight=width;v.strokeCap="ROUND";v.strokeJoin="ROUND";map.appendChild(v);return v;}
const water=figma.createRectangle();water.name="River";water.resize(440,85);water.x=-40;water.y=5;water.rotation=-12;fill(water,"raised");map.appendChild(water);
vector("Road / North","M-10 96 L350 32","border",5);vector("Road / South","M-10 246 L350 182","border",5);vector("Road / West","M72 -10 L110 280","border",5);vector("Road / East","M255 -10 L290 280","border",5);
const park=figma.createRectangle();park.name="Park";park.resize(122,101);park.x=125;park.y=110;radius(park,20);fill(park,"raised");map.appendChild(park);
vector("Recommended route · editable","M102 214 L85 103 C125 95 175 83 262 75 L281 179 C216 192 153 204 102 214","blue",4);
const dot=figma.createEllipse();dot.name="Start and finish / current location";dot.resize(16,16);dot.x=94;dot.y=206;fill(dot,"primary");dot.strokes=[paint("blue")];dot.strokeWeight=4;map.appendChild(dot);
const lab=await txt(map,"Current location","내 위치","Micro","text",80);lab.x=110;lab.y=217;
const river=await txt(map,"River label","한강","Micro","muted",70);river.x=170;river.y=30;
map.description="위치 기반 왕복 코스를 설명하는 편집용 지도입니다. 실제 경로·실측 위치가 아닌 예시 도형입니다. 현재 위치는 출발·도착 지점으로 동일합니다.";reg("Route Map",map);
const keypad=comp("Numeric keypad",390);pad(keypad,12);gap(keypad,8);fill(keypad,"surface");
for(const row of [["1","2","3"],["4","5","6"],["7","8","9"],[".","0","⌫"]]){const r=al("Keys","HORIZONTAL",366,8);keypad.appendChild(r);for(const k of row){const cell=al("Key "+k,"VERTICAL",116,0);cell.resize(116,44);cell.primaryAxisSizingMode="FIXED";cell.counterAxisSizingMode="FIXED";cell.primaryAxisAlignItems="CENTER";radius(cell,8);fill(cell,"raised");const t=await txt(cell,"Digit",k,"Section","text",116);t.textAlignHorizontal="CENTER";r.appendChild(cell);}}
keypad.description="숫자 키패드의 앱 내 시각 참고용. 실제 구현은 OS 숫자 키보드를 사용합니다.";reg("Numeric Keypad",keypad);
return {createdNodeIds:allIds(roots),mutatedNodeIds:updated,catalog,bounds:roots.map(n=>({id:n.id,name:n.name,x:n.x,y:n.y,w:n.width,h:n.height})),counts:{components:page.findAllWithCriteria({types:["COMPONENT"]}).length,sets:page.findAllWithCriteria({types:["COMPONENT_SET"]}).length}};


}
async function buildScreens(DS){
await figma.loadFontAsync({family:"Noto Sans KR",style:"Regular"});
await figma.loadFontAsync({family:"Noto Sans KR",style:"Medium"});
const V={};for(const [k,id] of Object.entries(DS.vars)) V[k]=await figma.variables.getVariableByIdAsync(id);
const c=h=>({r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255});
function paint(k){return figma.variables.setBoundVariableForPaint({type:"SOLID",color:c(DS.colors[k])},"color",V[k]);}
function fill(n,k){n.fills=[paint(k)];}
function gap(n,v){n.itemSpacing=v;if(V["s"+v])n.setBoundVariable("itemSpacing",V["s"+v]);}
function pad(n,v){for(const p of ["paddingTop","paddingBottom","paddingLeft","paddingRight"]){n[p]=v;if(V["s"+v])n.setBoundVariable(p,V["s"+v]);}}
function radius(n,v){n.cornerRadius=v;if(V["s"+v])n.setBoundVariable("cornerRadius",V["s"+v]);}
function border(n,k="border"){n.strokes=[paint(k)];n.strokeWeight=1;}
function al(name,dir="VERTICAL",w=342,g=12){const n=figma.createAutoLayout(dir);n.name=name;n.fills=[];n.resize(w,1);n.primaryAxisSizingMode=dir==="VERTICAL"?"AUTO":"FIXED";n.counterAxisSizingMode=dir==="VERTICAL"?"FIXED":"AUTO";gap(n,g);return n;}
async function txt(parent,name,str,style="Body",color="text",width){const t=figma.createText();t.name=name;await t.setTextStyleIdAsync(DS.styles[style]);t.characters=str;fill(t,color);parent.appendChild(t);t.textAutoResize="HEIGHT";t.resize(width||parent.width,1);if("layoutMode" in parent&&parent.layoutMode!=="NONE")t.layoutSizingVertical="HUG";return t;}
function prop(comp,node,name){const key=comp.addComponentProperty(name,"TEXT",node.characters);node.componentPropertyReferences={characters:key};return key;}
function comp(name,w=342){const n=figma.createComponent();n.name=name;n.layoutMode="VERTICAL";n.resize(w,1);n.primaryAxisSizingMode="AUTO";n.counterAxisSizingMode="FIXED";n.fills=[];return n;}
function allIds(roots){const out=[];const rec=n=>{out.push(n.id);if("children" in n)for(const a of n.children)rec(a);};roots.forEach(rec);return out;}

async function inst(family,variant,values={},parent){const f=DS.catalog[family];const master=await figma.getNodeByIdAsync(variant?f.variants[variant]:f.id);const n=master.createInstance();n.x=0;n.y=0;if(parent)parent.appendChild(n);const props={};for(const [key,value]of Object.entries(values))if(f.props&&f.props[key]){const wanted=f.props[key].split("#")[0];const actual=Object.keys(n.componentProperties).find(k=>k.split("#")[0]===wanted);if(actual)props[actual]=value;}if(Object.keys(props).length)n.setProperties(props);return n;}
async function btn(parent,label,tone="Primary",w){const n=await inst("Button",tone+"/Default",{label},parent);if(w){n.resize(w,56);const label=n.findOne(z=>z.type==="TEXT");if(label)label.resize(w-32,label.height);}n.name="Button / "+label;return n;}


const page=await figma.getNodeByIdAsync(DS.pages.screens);await figma.setCurrentPageAsync(page);
const roots=[],screens={},links=[],regions={};
function route(node,dest,label,kind="CLICK"){links.push({node:node.id,dest,label,kind});}
async function screen(id,name,header,back="U01"){const root=figma.createFrame();root.name=id+" · "+name;root.resize(390,844);fill(root,"bg");root.clipsContent=true;roots.push(root);screens[id]=root.id;
const glow=figma.createRectangle();glow.name="Atmosphere / blue gradient";root.appendChild(glow);glow.resize(390,340);glow.fills=[{type:"GRADIENT_RADIAL",gradientTransform:[[0,1,-.35],[-1,0,1.1]],gradientStops:[{position:0,color:{r:.03,g:.16,b:.36,a:.85}},{position:1,color:{r:.02,g:.035,b:.067,a:0}}]}];
const status=await inst("Status Bar",null,{},root);status.y=0;
const h=await inst("Header",null,{title:header},root);h.y=44;route(h.findOne(n=>n.name==="Back · 48px touch"),back,"뒤로");
const home=await inst("Home Indicator",null,{},root);home.y=820;
const body=al("Content","VERTICAL",342,20);root.appendChild(body);body.x=24;body.y=120;
return {id,root,body};}
async function heading(s,title,sub,center=false){const group=al("Heading","VERTICAL",342,8);s.body.appendChild(group);const t=await txt(group,"Title",title,"Title");if(center)t.textAlignHorizontal="CENTER";if(sub){const d=await txt(group,"Description",sub,"Body","muted");if(center)d.textAlignHorizontal="CENTER";}return group;}
async function action(parent,label,dest,tone="Primary"){const b=await btn(parent,label,tone);if(dest)route(b,dest,label);return b;}
async function dock(s,actions,note){const d=al("Bottom actions","VERTICAL",342,8);s.root.appendChild(d);for(const a of actions)await action(d,a[0],a[1],a[2]||"Primary");if(note){const t=await txt(d,"Footnote",note,"Micro","muted");t.textAlignHorizontal="CENTER";}d.x=24;d.y=800-d.height;regions[s.id]={contentBottom:s.body.y+s.body.height,dockTop:d.y};return d;}
async function notice(p,title,body){return await inst("Notice",null,{title,body},p);}
async function metrics(p,values){const row=al("Metrics","HORIZONTAL",342,24);p.appendChild(row);for(const [label,value]of values){await inst("Metric",null,{label,value},row);}return row;}
async function card(p,title,description,icon,dest,tone="Secondary"){const n=await inst("Action Card",tone+"/Default",{title,description,icon:DS.icons[icon]},p);route(n,dest,title);return n;}
async function timer(s,caption,time,sub){const g=al("Live metric","VERTICAL",342,8);s.body.appendChild(g);g.paddingTop=28;g.paddingBottom=28;const c=await txt(g,"Caption",caption,"Caption","softBlue");c.textAlignHorizontal="CENTER";const t=await txt(g,"Timer",time,"Display");t.fontSize=64;t.lineHeight={unit:"PIXELS",value:82};t.textAlignHorizontal="CENTER";if(sub){const d=await txt(g,"Progress note",sub,"Body","muted");d.textAlignHorizontal="CENTER";}return g;}
async function progress(s,width=180){const p=await inst("Progress",null,{},s.body);p.findOne(n=>n.name==="Progress · resize width").resize(width,6);return p;}
async function map(s,large=false){const m=await inst("Route Map",null,{},s.body);if(large)m.resize(342,450);return m;}
async function fields(s,state="Empty"){const row=al("Body information","HORIZONTAL",342,12);s.body.appendChild(row);const h=await inst("Number Field",state,{label:"키",value:state==="Filled"||state==="Focused"?"170":"—",unit:"cm",helper:"키를 입력해주세요",showHelper:state==="Error"},row);const w=await inst("Number Field",state==="Error"?"Filled":state,{label:"몸무게",value:state==="Filled"||state==="Focused"||state==="Error"?"70":"—",unit:"kg",helper:"몸무게를 입력해주세요"},row);route(h,"D01","키 입력");route(w,"D01","몸무게 입력");return row;}
async function experience(s,selected=false){const g=al("Experience · single select","VERTICAL",342,8);s.body.appendChild(g);await txt(g,"Label","러닝 경험","Section");for(const [i,label,description]of [[0,"처음이에요","아직 러닝 경험이 없어요"],[1,"가끔 달려요","한 달에 몇 번 정도 달려요"],[2,"꾸준히 달려요","주 2회 이상, 꾸준히 달려요"]]){const n=await inst("Choice",selected&&i===0?"Selected":"Default",{label,description},g);route(n,"@choice:"+i,label);}return g;}
async function result(id,name,title,sub,vals,actions,note){const s=await screen(id,name,name);await heading(s,title,sub);const icon=(await figma.getNodeByIdAsync(DS.icons.check)).createInstance();s.body.appendChild(icon);icon.resize(48,48);await metrics(s.body,vals);await notice(s.body,"오늘의 기록",note||"지금까지의 움직임을 기록했어요.");await dock(s,actions);return s;}
async function loading(id,name,title,sub,dest,cancel){const s=await screen(id,name,name);s.body.y=200;await heading(s,title,sub,true);await timer(s,"잠시만 기다려주세요","···");await dock(s,[["취소",cancel,"Secondary"]]);route(s.root,dest,"시안 자동 전환","TIME");return s;}
function finish(start){
 const contexts={R03:'R02',R04:'R02',R06:'R05',R07:'R02',C01:'U03',L01:'U01',L04:'K04',K05:'K04',K08:'K04',D03:'U05',D12:'D08',S01:'D10'};
 for(const [id,sourceId]of Object.entries(contexts)){const root=roots.find(n=>n.id===screens[id]),source=roots.find(n=>n.id===screens[sourceId]);if(!root||!source)continue;for(const child of [...root.children])if(child.name!=='Overlay / scrim'&&child.type!=='INSTANCE'||child.type==='INSTANCE'&&!child.name.startsWith('Kind='))child.remove();const bg=source.clone();root.insertChild(0,bg);bg.name='Context / '+sourceId;bg.x=0;bg.y=0;}
 roots.forEach((n,i)=>{const pos=start+i;n.x=100+(pos%6)*454;n.y=120+Math.floor(pos/6)*940;});return {createdNodeIds:allIds(roots),mutatedNodeIds:[],screens,links,regions,counts:{screens:roots.length},bounds:roots.map(n=>({id:n.id,name:n.name,x:n.x,y:n.y,w:n.width,h:n.height}))};}

const modals={};
async function modalScreen(id,name,title,body,kind,actions,time="12:34"){
const s=await screen(id,name,"러닝 중","@resume");await heading(s,"내 페이스로 달려요","움직인 만큼 기록하고 있어요.");await timer(s,"러닝 시간",time);await metrics(s.body,[["달린 거리","1.62 km"],["평균 페이스","7′45″"]]);await dock(s,[["일시정지",null],["종료하기",null,"Ghost"]]);
const scrim=figma.createRectangle();s.root.appendChild(scrim);scrim.name="Overlay / scrim";scrim.resize(390,844);scrim.fills=[{type:"SOLID",color:{r:0,g:0,b:0},opacity:.66}];
const dialog=await inst("Dialog",kind,{title,body,showTertiary:actions.length>2},s.root);dialog.x=24;dialog.y=(844-dialog.height)/2;
const slots=["Primary","Secondary","Tertiary"];actions.forEach((a,i)=>{const b=dialog.findOne(n=>n.type==="INSTANCE"&&n.name==="Action / "+slots[i]);if(b){const key=Object.keys(b.componentProperties).find(k=>k.startsWith("Label#"));b.setProperties({[key]:a[0]});route(b,a[1],a[0]);}});
modals[id]={dialog:dialog.id,actions,kind,title,body};return s;}


let s=await screen("U01","시작 메뉴","러닝 시작");
s.body.y=136;const tag=await txt(s.body,"Eyebrow","↗  오늘의 러닝","Caption","blue");tag.textAlignHorizontal="CENTER";await heading(s,"내 페이스로\n시작해요",null,true);
const menu=al("Four running modes","VERTICAL",342,12);menu.paddingTop=20;s.body.appendChild(menu);
await card(menu,"자유 러닝","목표 없이 바로 시작","run","R01","Primary");
await card(menu,"러닝 챌린지","준비된 목표에 도전하기","flag","U02");
await card(menu,"추천 코스","내 위치에서 출발하는 코스","pin","L01");
await card(menu,"다이어트 모드","내게 맞는 러닝 방법 찾기","walk","U05");
s=await screen("U02","챌린지 목록","러닝 챌린지");await heading(s,"오늘의 러닝 목표","준비된 목표로 가볍게 도전해요.");
for(const [title,sub,dest]of [["10분 러닝","시간에 맞춰 가볍게 달려요","@start:U03"],["1km 러닝","처음 도전하는 거리","@start:C04"],["3km 러닝","조금 더 길게 달려요","@start:C05"]]){
const box=al("Challenge / "+title,"VERTICAL",342,12);pad(box,16);radius(box,16);fill(box,"surface");border(box);s.body.appendChild(box);
await txt(box,"Title",title,"Section","text",310);await txt(box,"Description",sub,"Caption","muted",310);const b=await btn(box,title+" 시작","Primary",310);route(b,dest,title+" 시작");}
s.body.itemSpacing=12;await dock(s,[["지난 도전 보기","C03","Ghost"]]);
s=await screen("U03","10분 챌린지 진행","10분 러닝","R04");await heading(s,"천천히, 꾸준하게","10분 목표에 도전하고 있어요.");await timer(s,"달린 시간","06:24","목표까지 3분 36초");await progress(s,219);await metrics(s.body,[["달린 거리","0.82 km"],["평균 페이스","7′48″"]]);await dock(s,[["일시정지","R03","Primary"],["종료하기","R04","Ghost"]]);
s=await screen("U04","내 위치 기준 추천 코스","추천 코스");await heading(s,"내 위치에서 한 바퀴","내 위치에서 출발하고 돌아오는 코스예요.");s.body.itemSpacing=16;await map(s);await metrics(s.body,[["코스 거리","2.4 km"],["예상 시간","약 20분"]]);await notice(s.body,"한강 가볍게 한 바퀴","평탄한 길 · 현재 위치 출발 / 도착");await dock(s,[["이 코스로 시작","@start:K04"],["다른 코스 추천","K01","Secondary"]]);
s=await screen("U05","다이어트 모드 입력","다이어트 모드","D03");s.body.itemSpacing=16;await heading(s,"나에게 맞게 시작해요","몸 정보와 러닝 경험을 알려주세요.");await fields(s);await txt(s.body,"Data use","키·몸무게는 변화 기록에 사용해요.","Caption","muted");await experience(s);await dock(s,[["러닝 방법 추천받기","D02"]],"몸 정보는 나중에 수정할 수 있어요.");
s=await screen("U06","러닝 방법 추천","다이어트 모드");await heading(s,"걷기부터 가볍게","처음 시작하는 분을 위한 15분 루틴이에요.");await notice(s.body,"걷기와 짧은 러닝을 함께","속도보다 편안하게 이어가는 데 집중해요.");
const routine=al("Routine steps","VERTICAL",342,0);pad(routine,16);radius(routine,16);fill(routine,"surface");s.body.appendChild(routine);
for(const [a,b]of [["준비 걷기","3분"],["달리기 1분 + 걷기 2분","3회"],["마무리 걷기","3분"]]){const r=al("Step","HORIZONTAL",310,8);r.paddingTop=12;r.paddingBottom=12;routine.appendChild(r);await txt(r,"Step label",a,"Body","text",240);await txt(r,"Duration",b,"Body","softBlue",62);}
await metrics(s.body,[["총 러닝","3분"],["총 걷기","12분"]]);await action(s.body,"몸 정보·경험 수정","D06","Ghost");await dock(s,[["이 방법으로 시작","@start:D07"]]);



s=await screen("R01","시작 카운트다운","자유 러닝");s.body.y=230;await heading(s,"이제 시작해요","편안한 속도로 달려주세요.",true);await timer(s,"준비","3");await dock(s,[["시작 취소","U01","Secondary"]]);route(s.root,"R02","카운트다운 완료","TIME3");
s=await screen("R02","자유 러닝 진행","자유 러닝","R04");await heading(s,"내 페이스로 달려요","목표 없이 편안하게 이어가세요.");await timer(s,"러닝 시간","12:34");await metrics(s.body,[["달린 거리","1.62 km"],["평균 페이스","7′45″"]]);await notice(s.body,"지금처럼 천천히","힘들면 잠깐 걸어도 괜찮아요.");await dock(s,[["일시정지","R03"],["종료하기","R04","Ghost"]]);
await modalScreen("R03","일시정지","잠깐 쉬어가요","지금까지의 기록은 그대로 유지돼요.","Confirm",[["다시 달리기","@resume"],["종료하기","R04"]],"12:34");
await modalScreen("R04","종료 확인","러닝을 마칠까요?","지금까지 달린 기록을 저장할 수 있어요.","Confirm",[["저장하고 마치기","R05"],["계속 달리기","@resume"],["기록 없이 끝내기","R07"]],"12:34");
await result("R05","러닝 결과","오늘도 잘 달렸어요","러닝 기록을 저장했어요.",[["러닝 시간","12:34"],["달린 거리","1.62 km"]],[["처음 화면으로","U01"],["기록 삭제","R06","Ghost"]]);
await modalScreen("R06","기록 삭제 확인","이 기록을 삭제할까요?","삭제한 러닝 기록은 되돌릴 수 없어요.","Danger",[["삭제하기","U01"],["취소","R05"]]);
await modalScreen("R07","기록 없이 종료 확인","저장 없이 끝낼까요?","이번 러닝 기록이 저장되지 않아요.","Danger",[["저장 없이 끝내기","U01"],["돌아가기","R04"]]);
await modalScreen("C01","목표 달성","목표를 달성했어요","조금 더 달리거나 기록을 저장할 수 있어요.","Info",[["저장하고 마치기","R05"],["계속 달리기","U03"]],"10:00");
await result("C02","중간 종료 결과","오늘은 여기까지","목표에 닿지 않아도 움직인 만큼 남아요.",[["러닝 시간","06:24"],["달린 거리","0.82 km"]],[["챌린지로 돌아가기","U02"]],"10분 챌린지 · 6분 24초의 기록을 저장했어요.");
s=await screen("C03","지난 도전 상세","지난 도전","U02");await heading(s,"차곡차곡 쌓인 도전","내가 달린 만큼 남아 있어요.");await notice(s.body,"오늘 · 10분 러닝 달성","10:00 · 1.28 km");await metrics(s.body,[["평균 페이스","7′48″"],["달성한 목표","10분"]]);await notice(s.body,"어제 · 1km 러닝 달성","08:12 · 1.00 km");await dock(s,[["챌린지로 돌아가기","U02"]]);
for(const [id,title,time,dist,left,w]of [["C04","1km 러닝","04:12","0.52 km","목표까지 0.48 km",178],["C05","3km 러닝","12:34","1.62 km","목표까지 1.38 km",184]]){s=await screen(id,title+" 진행",title,"R04");await heading(s,"한 걸음씩 가까워져요","내 속도로 목표까지 달려보세요.");await timer(s,"달린 거리",dist,left);await progress(s,w);await metrics(s.body,[["러닝 시간",time],["목표 거리",id==="C04"?"1.00 km":"3.00 km"]]);await dock(s,[["일시정지","R03"],["종료하기","R04","Ghost"]]);}
await modalScreen("L01","위치 사용 안내","내 위치에서 코스를 찾아요","현재 위치를 사용해 출발하고 돌아오는 코스를 추천해요.","Info",[["위치 허용하기","L03"],["나중에","U01"]]);
s=await screen("L02","위치 권한 꺼짐","추천 코스");s.body.y=210;await heading(s,"위치를 확인할 수 없어요","설정에서 위치 사용을 허용해주세요.",true);await notice(s.body,"위치 없이도 달릴 수 있어요","자유 러닝은 시간만 기록할 수 있어요.");await dock(s,[["설정 열기","L03"],["자유 러닝 시작","@start:R02_TIME","Secondary"]]);
await loading("L03","위치 확인 중","내 위치를\n확인하고 있어요","잠시만 기다려주세요.","K01","U01");
await modalScreen("L04","위치 신호 끊김","위치 신호가 약해요","시간은 계속 기록하고 있어요. 위치가 돌아오면 거리를 다시 측정해요.","Info",[["위치 다시 확인","K04"],["시간만 기록하기","R02_TIME"]]);
await loading("K01","코스 찾는 중","가까운 코스를 찾고 있어요","내 위치에서 돌아오는 길을 살펴보고 있어요.","U04","U01");
s=await screen("K02","다른 코스 추천","추천 코스");s.body.itemSpacing=16;await heading(s,"이번 코스는 어때요?","내 위치에서 출발하고 돌아와요.");await map(s);await metrics(s.body,[["코스 거리","1.8 km"],["예상 시간","약 15분"]]);await notice(s.body,"공원 가볍게 한 바퀴","평탄한 길 · 현재 위치 출발 / 도착");await dock(s,[["이 코스로 시작","@start:K04"],["다른 코스 추천","K01","Secondary"]]);
s=await screen("K03","추천 가능한 코스 없음","추천 코스");s.body.y=210;await heading(s,"지금은 코스를\n찾기 어려워요","위치와 연결 상태를 확인하고 다시 찾아보세요.",true);await dock(s,[["다시 찾기","L03"],["자유 러닝 시작","R01","Secondary"]]);
s=await screen("K04","코스 러닝 중","코스 러닝","R04");await heading(s,"내 위치에서 한 바퀴","한강 가볍게 한 바퀴 · 2.4 km");await map(s);await metrics(s.body,[["달린 거리","1.20 km"],["남은 거리","1.20 km"]]);await progress(s,171);await action(s.body,"지도 크게 보기","K07","Secondary");await dock(s,[["일시정지","R03"],["종료하기","R04","Ghost"]]);
await modalScreen("K05","코스 이탈 안내","코스에서 조금 벗어났어요","기록은 계속 이어지고 있어요. 지도를 확인하거나 자유 러닝으로 달려보세요.","Info",[["지도 크게 보기","K07"],["자유 러닝으로 전환","K08"]]);
await result("K06","코스 완료","한 바퀴 완주했어요","출발한 곳으로 돌아왔어요.",[["코스 거리","2.40 km"],["러닝 시간","20:16"]],[["처음 화면으로","U01"]],"코스와 러닝 기록을 저장했어요.");
s=await screen("K07","전체 지도","전체 지도","K04");s.body.y=112;await map(s,true);await notice(s.body,"내 위치 · 출발 / 도착","파란 선을 따라 한 바퀴 돌아와요.");await dock(s,[["내 위치로 지도 맞추기","K07","Secondary"],["운동 화면으로","K04"]]);
await modalScreen("K08","자유 러닝 전환 확인","자유 러닝으로 바꿀까요?","지금까지의 시간과 거리는 유지하고 코스 안내만 끝내요.","Confirm",[["전환하기","R02"],["취소","K05"]]);
s=await screen("D01","숫자 입력","다이어트 모드","D03");await heading(s,"몸 정보를 입력해주세요","숫자를 입력하고 완료를 눌러주세요.");await fields(s,"Focused");await notice(s.body,"키 · 몸무게","몸 정보는 변화 기록에 사용해요.");const kb=await inst("Numeric Keypad",null,{},s.root);kb.y=596;const done=al("Keyboard done","VERTICAL",342,0);s.root.appendChild(done);done.x=24;done.y=526;await action(done,"입력 완료","D06");
s=await screen("D02","입력 확인","다이어트 모드","D03");s.body.itemSpacing=16;await heading(s,"입력을 확인해주세요","키를 입력하면 추천을 받을 수 있어요.");await fields(s,"Error");await experience(s,true);await dock(s,[["입력 수정하기","D06"]]);
await modalScreen("D03","입력 취소 확인","입력을 그만할까요?","작성 중인 내용은 저장되지 않아요.","Confirm",[["계속 입력하기","U05"],["나가기","U01"]]);
await loading("D04","러닝 방법 준비","러닝 방법을\n준비하고 있어요","입력한 러닝 경험을 참고하고 있어요.","U06","D06");
s=await screen("D05","추천 실패","다이어트 모드");s.body.y=210;await heading(s,"추천을 가져오지\n못했어요","입력한 정보는 그대로 있어요. 잠시 후 다시 시도해주세요.",true);await dock(s,[["다시 시도","D04"],["입력 화면으로","D06","Secondary"]]);
s=await screen("D06","몸 정보·경험 수정","몸 정보·경험 수정","D03");s.body.itemSpacing=16;await heading(s,"나에게 맞게 바꿔요","몸 정보와 러닝 경험을 확인해주세요.");await fields(s,"Filled");await experience(s,true);await dock(s,[["수정하고 다시 추천","D04"],["취소","U06","Ghost"]]);
for(const [id,name,title,remain,sub,next,elapsed]of [["D07","준비 걷기","가볍게 걸어주세요","02:16","준비 걷기 · 3분","다음 · 달리기 1분","00:44"],["D08","러닝 구간","편안하게 달려주세요","00:42","달리기 · 1회 / 3회","다음 · 걷기 2분","03:18"],["D09","걷기 구간","숨을 고르며 걸어요","01:28","걷기 · 1회 / 3회","다음 · 달리기 1분","04:32"],["D11","마무리 걷기","천천히 마무리해요","02:04","마무리 걷기 · 3분","오늘의 루틴을 거의 마쳤어요","12:56"]]){
s=await screen(id,name,"다이어트 모드","R04");await heading(s,title,sub);await timer(s,"남은 구간 시간",remain);await progress(s,id==="D11"?290:id==="D09"?104:id==="D08"?75:17);await notice(s.body,next,"구간 시간이 끝나면 자동으로 바뀌어요.");await metrics(s.body,[["전체 운동",elapsed],["예정 운동","15:00"]]);await dock(s,[["일시정지","D12"],["종료하기","R04","Ghost"]]);}
s=await screen("D10","루틴 완료","다이어트 모드");await heading(s,"15분, 끝까지 해냈어요","걷기와 달리기를 모두 마쳤어요.");await timer(s,"총 운동 시간","15:00");await metrics(s.body,[["총 러닝","3분"],["총 걷기","12분"]]);await dock(s,[["기록 저장하고 마치기","S02"]]);
await modalScreen("D12","다이어트 일시정지","잠깐 쉬어가요","현재 구간과 남은 시간을 그대로 이어갈 수 있어요.","Confirm",[["이어서 시작","@resume"],["종료하기","R04"]],"00:42");
await result("D13","중간 종료 결과","움직인 만큼 남았어요","오늘은 여기까지 해도 괜찮아요.",[["전체 운동","06:32"],["완료 구간","2 / 8"]],[["처음 화면으로","U01"],["같은 방법 다시 하기","U06","Secondary"]],"진행한 시간만 러닝 기록에 저장했어요.");
await modalScreen("S01","기록 저장 실패","아직 저장되지 않았어요","러닝 기록은 이 화면에 남아 있어요. 연결을 확인하고 다시 저장해주세요.","Error",[["다시 저장","S02"],["화면에 머무르기","D10"]]);
await result("S02","저장 완료","기록을 저장했어요","오늘의 움직임이 차곡차곡 쌓였어요.",[["전체 운동","15:00"],["러닝 + 걷기","3분 + 12분"]],[["처음 화면으로","U01"],["같은 방법 다시 하기","U06","Secondary"]]);


return {...finish(0),modals};
}
async function wire(DS){
await figma.loadFontAsync({family:"Noto Sans KR",style:"Regular"});
await figma.loadFontAsync({family:"Noto Sans KR",style:"Medium"});
const V={};for(const [k,id] of Object.entries(DS.vars)) V[k]=await figma.variables.getVariableByIdAsync(id);
const c=h=>({r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255});
function paint(k){return figma.variables.setBoundVariableForPaint({type:"SOLID",color:c(DS.colors[k])},"color",V[k]);}
function fill(n,k){n.fills=[paint(k)];}
function gap(n,v){n.itemSpacing=v;if(V["s"+v])n.setBoundVariable("itemSpacing",V["s"+v]);}
function pad(n,v){for(const p of ["paddingTop","paddingBottom","paddingLeft","paddingRight"]){n[p]=v;if(V["s"+v])n.setBoundVariable(p,V["s"+v]);}}
function radius(n,v){n.cornerRadius=v;if(V["s"+v])n.setBoundVariable("cornerRadius",V["s"+v]);}
function border(n,k="border"){n.strokes=[paint(k)];n.strokeWeight=1;}
function al(name,dir="VERTICAL",w=342,g=12){const n=figma.createAutoLayout(dir);n.name=name;n.fills=[];n.resize(w,1);n.primaryAxisSizingMode=dir==="VERTICAL"?"AUTO":"FIXED";n.counterAxisSizingMode=dir==="VERTICAL"?"FIXED":"AUTO";gap(n,g);return n;}
async function txt(parent,name,str,style="Body",color="text",width){const t=figma.createText();t.name=name;await t.setTextStyleIdAsync(DS.styles[style]);t.characters=str;fill(t,color);parent.appendChild(t);t.textAutoResize="HEIGHT";t.resize(width||parent.width,1);if("layoutMode" in parent&&parent.layoutMode!=="NONE")t.layoutSizingVertical="HUG";return t;}
function prop(comp,node,name){const key=comp.addComponentProperty(name,"TEXT",node.characters);node.componentPropertyReferences={characters:key};return key;}
function comp(name,w=342){const n=figma.createComponent();n.name=name;n.layoutMode="VERTICAL";n.resize(w,1);n.primaryAxisSizingMode="AUTO";n.counterAxisSizingMode="FIXED";n.fills=[];return n;}
function allIds(roots){const out=[];const rec=n=>{out.push(n.id);if("children" in n)for(const a of n.children)rec(a);};roots.forEach(rec);return out;}

async function inst(family,variant,values={},parent){const f=DS.catalog[family];const master=await figma.getNodeByIdAsync(variant?f.variants[variant]:f.id);const n=master.createInstance();n.x=0;n.y=0;if(parent)parent.appendChild(n);const props={};for(const [key,value]of Object.entries(values))if(f.props&&f.props[key]){const wanted=f.props[key].split("#")[0];const actual=Object.keys(n.componentProperties).find(k=>k.split("#")[0]===wanted);if(actual)props[actual]=value;}if(Object.keys(props).length)n.setProperties(props);return n;}
async function btn(parent,label,tone="Primary",w){const n=await inst("Button",tone+"/Default",{label},parent);if(w){n.resize(w,56);const label=n.findOne(z=>z.type==="TEXT");if(label)label.resize(w-32,label.height);}n.name="Button / "+label;return n;}


const page=await figma.getNodeByIdAsync(DS.pages.screens);await figma.setCurrentPageAsync(page);
const state=DS.screenState, created=[],mutated=[],overlayCache={},countdowns={},connections=[];
const screenIds=Object.values(state.screens);let overlayIndex=0;
function top(n){let p=n;while(p&&p.parent&&p.parent.type!=='PAGE')p=p.parent;return p;}
function codeOf(n){const t=top(n);return Object.keys(state.screens).find(k=>state.screens[k]===t.id)||'U01';}
async function connect(node,destination,kind='CLICK',navigation='NAVIGATE'){
 const trigger=kind==='TIME3'?{type:'AFTER_TIMEOUT',timeout:3}:kind==='TIME'?{type:'AFTER_TIMEOUT',timeout:1.5}:{type:'ON_CLICK'};
 const action={type:'NODE',destinationId:destination,navigation,transition:{type:'DISSOLVE',easing:{type:'EASE_OUT'},duration:.18}};
 if(navigation==='OVERLAY')action.overlayRelativePosition={x:0,y:0};
 await node.setReactionsAsync([{trigger,actions:[action]}]);mutated.push(node.id);connections.push({node:node.id,destination,kind,navigation});
}
async function overlay(id,origin='R02'){
 const key=id+'_'+origin;if(overlayCache[key])return overlayCache[key];
 const data=state.modals[id];if(!data)return state.screens[id];
 const root=figma.createFrame();root.name='Overlay / '+key;root.resize(390,844);root.fills=[];root.x=3000+(overlayIndex%6)*430;root.y=120+Math.floor(overlayIndex/6)*920;overlayIndex++;created.push(root);overlayCache[key]=root.id;
 const dim=figma.createRectangle();root.appendChild(dim);dim.name='Scrim · dismiss cancels';dim.resize(390,844);dim.fills=[{type:'SOLID',color:{r:0,g:0,b:0},opacity:.66}];await dim.setReactionsAsync([{trigger:{type:'ON_CLICK'},actions:[{type:'CLOSE'}]}]);
 const dialog=(await figma.getNodeByIdAsync(data.dialog)).clone();root.appendChild(dialog);dialog.x=24;dialog.y=(844-dialog.height)/2;
 for(let i=0;i<data.actions.length;i++){
  let dest=data.actions[i][1];const b=dialog.findOne(n=>n.type==='INSTANCE'&&n.name==='Action / '+['Primary','Secondary','Tertiary'][i]);if(!b)continue;
  if(dest==='@resume')dest=origin;
  if(id==='R04'&&dest==='R05'&&['U03','C04','C05'].includes(origin))dest='C02';
  if(id==='R04'&&dest==='R05'&&['D07','D08','D09','D11','D12'].includes(origin))dest='D13';
  if(id==='D03'&&origin==='D06')dest=dest==='U05'?'D06':dest==='U01'?'U06':dest;
  if(state.modals[dest])await connect(b,await overlay(dest,origin),'CLICK','OVERLAY');else if(state.screens[dest])await connect(b,state.screens[dest]);
 }
 return root.id;
}
// Additional time-only state preserves unknown distance/pace rather than displaying zero.
const timeOnly=(await figma.getNodeByIdAsync(state.screens.R02)).clone();timeOnly.name='Support / R02_TIME · 위치 없이 시간 기록';timeOnly.x=3000;timeOnly.y=11000;created.push(timeOnly);state.screens.R02_TIME=timeOnly.id;
for(const t of timeOnly.findAll(n=>n.type==='TEXT')){if(t.characters==='1.62 km'||t.characters==='7′45″')t.characters='—';if(t.characters==='목표 없이 편안하게 이어가세요.')t.characters='위치 없이 시간만 기록하고 있어요.';}
for(const b of timeOnly.findAll(n=>n.type==='INSTANCE'&&n.name.startsWith('Button / '))){const id=b.name.includes('일시정지')?'R03':'R04';await connect(b,await overlay(id,'R02_TIME'),'CLICK','OVERLAY');}
async function countdown(target,origin){const key=target+'_'+origin;if(countdowns[key])return countdowns[key];const source=await figma.getNodeByIdAsync(state.screens.R01);const n=source.clone();n.name='Support / Countdown → '+target;n.x=3000+Object.keys(countdowns).length*430;n.y=10000;created.push(n);countdowns[key]=n.id;await connect(n,state.screens[target],'TIME3');for(const b of n.findAll(z=>z.type==='INSTANCE'&&z.name==='Button / 시작 취소'))await connect(b,state.screens[origin]);return n.id;}
// Three exclusive, filled examples for input demonstration. Actual text entry is supplied by the OS in implementation.
const choices=[];for(let k=0;k<3;k++){const n=(await figma.getNodeByIdAsync(state.screens.D06)).clone();n.name='Support / Experience '+(k+1);n.x=4300+k*430;n.y=11000;created.push(n);choices.push(n);const c=n.findOne(z=>z.name==='Experience · single select');let i=0;for(const o of c.children.filter(z=>z.type==='INSTANCE')){const values={...o.componentProperties};o.swapComponent(await figma.getNodeByIdAsync(DS.catalog.Choice.variants[i===k?'Selected':'Default']));const vals={};for(const [key,v]of Object.entries(values))if(v.type==='TEXT')vals[key]=v.value;o.setProperties(vals);i++;}}
for(const n of choices){const group=n.findOne(z=>z.name==='Experience · single select');const items=group.children.filter(z=>z.type==='INSTANCE');for(let k=0;k<items.length;k++)await connect(items[k],choices[k].id);for(const b of n.findAll(z=>z.type==='INSTANCE'&&z.name.startsWith('Button / ')))await connect(b,state.screens[b.name.includes('취소')?'U06':'D04']);}
for(const link of state.links){const node=await figma.getNodeByIdAsync(link.node);if(!node)continue;const origin=codeOf(node);let dest=link.dest;if(!dest)continue;if(dest.startsWith('@choice:')){await connect(node,choices[Number(dest.split(':')[1])].id);continue;}if(dest.startsWith('@start:')){await connect(node,await countdown(dest.split(':')[1],origin));continue;}if(dest==='@resume')dest='R02';if(state.modals[dest])await connect(node,await overlay(dest,origin),link.kind,'OVERLAY');else if(state.screens[dest])await connect(node,state.screens[dest],link.kind);}
// Separate QA launcher exposes error and completion states without adding debug controls to product screens.
const qa=al('REVIEW · 모든 화면과 오류 상태','VERTICAL',780,12);pad(qa,24);fill(qa,'bg');qa.x=3000;qa.y=12000;created.push(qa);await txt(qa,'Title','상태별 화면 검토','Title','text',732);await txt(qa,'Description','제품 화면이 아닌 디자이너용 검토 메뉴입니다.','Body','muted',732);
for(const [id,nid]of Object.entries(state.screens)){if(id==='R02_TIME')continue;const target=await figma.getNodeByIdAsync(nid);const b=await btn(qa,target.name,'Secondary',732);await connect(b,nid);}
page.flowStartingPoints=[{nodeId:state.screens.U01,name:'StepUp · 시작 메뉴'},{nodeId:qa.id,name:'디자이너 · 모든 상태 검토'}];
return {createdNodeIds:allIds(created),mutatedNodeIds:mutated,connections,overlayCount:Object.keys(overlayCache).length,countdowns:Object.keys(countdowns).length,reviewFrame:qa.id};

}
async function guide(DS){
await figma.loadFontAsync({family:"Noto Sans KR",style:"Regular"});
await figma.loadFontAsync({family:"Noto Sans KR",style:"Medium"});
const V={};for(const [k,id] of Object.entries(DS.vars)) V[k]=await figma.variables.getVariableByIdAsync(id);
const c=h=>({r:parseInt(h.slice(1,3),16)/255,g:parseInt(h.slice(3,5),16)/255,b:parseInt(h.slice(5,7),16)/255});
function paint(k){return figma.variables.setBoundVariableForPaint({type:"SOLID",color:c(DS.colors[k])},"color",V[k]);}
function fill(n,k){n.fills=[paint(k)];}
function gap(n,v){n.itemSpacing=v;if(V["s"+v])n.setBoundVariable("itemSpacing",V["s"+v]);}
function pad(n,v){for(const p of ["paddingTop","paddingBottom","paddingLeft","paddingRight"]){n[p]=v;if(V["s"+v])n.setBoundVariable(p,V["s"+v]);}}
function radius(n,v){n.cornerRadius=v;if(V["s"+v])n.setBoundVariable("cornerRadius",V["s"+v]);}
function border(n,k="border"){n.strokes=[paint(k)];n.strokeWeight=1;}
function al(name,dir="VERTICAL",w=342,g=12){const n=figma.createAutoLayout(dir);n.name=name;n.fills=[];n.resize(w,1);n.primaryAxisSizingMode=dir==="VERTICAL"?"AUTO":"FIXED";n.counterAxisSizingMode=dir==="VERTICAL"?"FIXED":"AUTO";gap(n,g);return n;}
async function txt(parent,name,str,style="Body",color="text",width){const t=figma.createText();t.name=name;await t.setTextStyleIdAsync(DS.styles[style]);t.characters=str;fill(t,color);parent.appendChild(t);t.textAutoResize="HEIGHT";t.resize(width||parent.width,1);if("layoutMode" in parent&&parent.layoutMode!=="NONE")t.layoutSizingVertical="HUG";return t;}
function prop(comp,node,name){const key=comp.addComponentProperty(name,"TEXT",node.characters);node.componentPropertyReferences={characters:key};return key;}
function comp(name,w=342){const n=figma.createComponent();n.name=name;n.layoutMode="VERTICAL";n.resize(w,1);n.primaryAxisSizingMode="AUTO";n.counterAxisSizingMode="FIXED";n.fills=[];return n;}
function allIds(roots){const out=[];const rec=n=>{out.push(n.id);if("children" in n)for(const a of n.children)rec(a);};roots.forEach(rec);return out;}

async function inst(family,variant,values={},parent){const f=DS.catalog[family];const master=await figma.getNodeByIdAsync(variant?f.variants[variant]:f.id);const n=master.createInstance();n.x=0;n.y=0;if(parent)parent.appendChild(n);const props={};for(const [key,value]of Object.entries(values))if(f.props&&f.props[key]){const wanted=f.props[key].split("#")[0];const actual=Object.keys(n.componentProperties).find(k=>k.split("#")[0]===wanted);if(actual)props[actual]=value;}if(Object.keys(props).length)n.setProperties(props);return n;}
async function btn(parent,label,tone="Primary",w){const n=await inst("Button",tone+"/Default",{label},parent);if(w){n.resize(w,56);const label=n.findOne(z=>z.type==="TEXT");if(label)label.resize(w-32,label.height);}n.name="Button / "+label;return n;}


const page=await figma.getNodeByIdAsync(DS.pages.guide);await figma.setCurrentPageAsync(page);
const roots=[];let y=100;
async function board(title,paragraphs){const n=al(title,'VERTICAL',1000,20);pad(n,40);fill(n,'bg');radius(n,24);n.x=100;n.y=y;roots.push(n);await txt(n,'Title',title,'Title','text',920);for(const p of paragraphs)await txt(n,'Guide',p,'Body','muted',920);y+=n.height+60;return n;}
await board('StepUp / Editable UI Kit',[
'45개 기본·상세 화면 + 독립 모달 + 재사용 컴포넌트. 390 × 844 기준. 모든 텍스트·도형·버튼은 편집할 수 있습니다.',
'01 컴포넌트에서 마스터를 수정하면 화면의 인스턴스에 반영됩니다. 새 화면에서는 마스터를 복사하지 말고 인스턴스를 사용하세요.',
'Button: Tone / State / Label / Icon / Show icon. Action Card: Title / Description / Icon. Number Field: State / Label / Value / Unit / Helper. Choice: Default / Selected. Dialog: Title / Body / Show tertiary, 중첩된 버튼의 Label을 변경하세요.',
'기존 Android 앱은 Pretendard를 사용합니다. 이번 Figma 파일은 현재 연결에서 사용 가능한 Noto Sans KR Regular·Medium으로 구성했습니다. Pretendard 적용 시 StepUp 텍스트 스타일의 글꼴을 교체하고 줄바꿈을 확인하세요.'
]);
const colors=await board('Color / Dark',['의미별 색상은 Semantic 변수에서 바꿉니다. Primitives는 원본 값입니다.']);
for(const [key,hex]of Object.entries(DS.colors)){const row=al('Color '+key,'HORIZONTAL',920,16);colors.appendChild(row);const sw=figma.createRectangle();sw.name=key;sw.resize(48,32);fill(sw,key);radius(sw,8);row.appendChild(sw);await txt(row,'Token',key+'  '+hex,'Body','text',850);}y=colors.y+colors.height+60;
const type=await board('Typography / Noto Sans KR',['Display 48/62 · Title 28/38 · Section 20/30 · Button 18/28 · Body 16/26 · Caption 14/22 · Micro 12/18']);
for(const name of Object.keys(DS.styles))await txt(type,'Sample '+name,name+'  내 페이스로 시작해요',name,'text',920);y=type.y+type.height+60;
await board('Layout / 작업 규칙',[
'390 × 844 화면 · 좌우 여백 24 · 간격 8 / 12 / 16 / 20 / 24 · 기본 버튼 높이 56 · 카드 반경 16 · 모달 반경 24. 주요 묶음은 Auto Layout으로 구성합니다.',
'버튼은 행동을 문장으로 표시합니다. 아이콘만으로 기능을 전달하지 않습니다. 경험 선택은 한 항목만 선택하며 체크 상태를 사용합니다.',
'화면의 지도는 편집 가능한 벡터 예시입니다. 실제 지도를 사용한 경로 탐색이나 출발점 길찾기가 아닙니다. 출발과 도착은 현재 위치입니다.',
'미측정 거리·페이스는 0이 아닌 — 로 표시합니다. 저장 실패 시 기록을 보존하고, 저장 성공을 확인한 뒤 완료 문구를 표시합니다.'
]);
await board('Prototype / 구현 전달',[
'시작 메뉴의 네 버튼에서 주요 흐름을 확인하세요. 상세 오류·완료 상태는 별도의 REVIEW 프레임에서 열 수 있습니다. 모달은 독립 Overlay 프레임으로 연결했습니다.',
'프로토타입의 숫자 입력·위치 권한·운동 시간·저장은 예시 상태 전환입니다. 실제 입력 검증·OS 권한·위치·운동 추천·저장 처리는 앱 구현이 필요합니다.',
'다이어트 예시: 준비 걷기 3분 + (달리기 1분 + 걷기 2분) × 3회 + 마무리 걷기 3분 = 총 15분. 총 러닝 3분 / 걷기 12분. 키·몸무게는 변화 기록이며, 러닝 경험은 루틴 구성 참고값입니다.',
'45개 화면은 상태 명세입니다. 모달과 카운트다운의 보조 프레임은 모드별 연결을 위해 추가됩니다. 원본 85개 동작 정의는 동봉된 interaction-spec.md에 있습니다.',
'이 파일은 연결된 사이트나 앱을 배포하지 않습니다. 원본 시안과 다르게 새 기능을 추가하지 않았습니다. 컴포넌트 API와 화면의 텍스트·수치는 수정할 수 있습니다.'
]);
return {createdNodeIds:allIds(roots),mutatedNodeIds:[]};

}
try{await figma.loadFontAsync({family:'Noto Sans KR',style:'Regular'});await figma.loadFontAsync({family:'Noto Sans KR',style:'Medium'});const DS=await foundations();const a=await buildAtomic(DS);Object.assign(DS.catalog,a.catalog);DS.icons=a.icons;const b=await buildMolecules(DS);Object.assign(DS.catalog,b.catalog);const screens=await buildScreens(DS);DS.screenState=screens;const connections=await wire(DS);DS.connections=connections;await guide(DS);await figma.setCurrentPageAsync(await figma.getNodeByIdAsync(DS.pages.screens));figma.viewport.scrollAndZoomIntoView([await figma.getNodeByIdAsync(screens.screens.U01)]);if(typeof globalThis.__STEPUP_CAPTURE__==='function')globalThis.__STEPUP_CAPTURE__(DS);figma.closePlugin('StepUp: 45 screens + editable components created');}catch(error){figma.closePlugin('StepUp generation stopped: '+String(error));throw error;}
})(figmaApi);